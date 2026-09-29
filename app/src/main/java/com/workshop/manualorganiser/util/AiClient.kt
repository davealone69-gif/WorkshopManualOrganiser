package com.workshop.manualorganiser.util

import com.workshop.manualorganiser.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Real local LLM client. The default target is Ollama running on the same
 * Android device at 127.0.0.1:11434 using the configured model.
 *
 * There are no canned or fabricated answers. A successful answer always comes
 * from the HTTP response returned by the configured LLM server.
 */
object AiClient {
    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val hermesConfigured: Boolean get() = BuildConfig.HERMES_ENDPOINT.isNotBlank()
    val isConfigured: Boolean get() = hermesConfigured || BuildConfig.AI_ENDPOINT.isNotBlank()
    val endpoint: String get() = if (hermesConfigured) BuildConfig.HERMES_ENDPOINT else BuildConfig.AI_ENDPOINT
    val model: String get() = if (hermesConfigured) BuildConfig.HERMES_MODEL else BuildConfig.AI_MODEL
    val backendName: String get() = if (hermesConfigured) "Hermes Agent" else "Local Ollama"

    private val jsonType = "application/json; charset=utf-8".toMediaType()

    suspend fun checkServer(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (hermesConfigured) {
                val base = endpoint.trimEnd('/').removeSuffix("/v1")
                val request = Request.Builder().url(base + "/health").get().apply {
                    if (BuildConfig.HERMES_API_KEY.isNotBlank()) header("Authorization", "Bearer ${BuildConfig.HERMES_API_KEY}")
                }.build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) error("Hermes HTTP ${response.code}")
                    "Hermes Agent online • $model"
                }
            } else {
                val base = ollamaBaseUrl() ?: error("Local Ollama endpoint is not configured")
                val request = Request.Builder().url(base.trimEnd('/') + "/api/tags").get().build()
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful) error("HTTP ${response.code}")
                    val models = JSONObject(body).optJSONArray("models") ?: JSONArray()
                    val names = (0 until models.length()).mapNotNull { i ->
                        models.optJSONObject(i)?.optString("name")?.takeIf { it.isNotBlank() }
                    }
                    require(names.contains(model)) {
                        if (names.isEmpty()) "Ollama is online, but no models are installed"
                        else "Ollama is online, but model '$model' is not installed"
                    }
                    "Ollama online • $model"
                }
            }
        }
    }

    suspend fun ask(question: String, systemContext: String): Result<String> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext Result.failure(IllegalStateException("No AI backend is configured"))
        if (question.isBlank()) return@withContext Result.failure(IllegalArgumentException("Question is empty"))

        runCatching {
            val messages = JSONArray()
                .put(JSONObject().apply {
                    put("role", "system")
                    put(
                        "content",
                        "You are Hermes, the Workshop Manual Master diagnostic research agent. Use supplied manual context first, then use your configured research tools when needed. Never invent vehicle-specific specifications. For fault finding, identify shared dependencies before naming parts, verify model/year applicability, distinguish manual evidence from internet reports, and state uncertainty plainly. If evidence conflicts, show the conflict instead of arguing. Be humble and fact-check. Music control is forbidden unless the user explicitly commands a music action. Context:\n$systemContext",
                    )
                })
                .put(JSONObject().apply {
                    put("role", "user")
                    put("content", question.trim())
                })

            val payload = JSONObject().apply {
                put("model", model)
                put("messages", messages)
                put("stream", false)
            }

            val request = Request.Builder()
                .url(endpoint)
                .post(payload.toString().toRequestBody(jsonType))
                .header("Content-Type", "application/json")
                .apply {
                    val key = if (hermesConfigured) BuildConfig.HERMES_API_KEY else BuildConfig.AI_API_KEY
                    if (key.isNotBlank()) header("Authorization", "Bearer $key")
                    if (hermesConfigured) {
                        header("X-Hermes-Session-Id", "workshop-manual-master")
                        header("X-Hermes-Session-Key", "workshop-manual-master")
                    }
                }
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) error("HTTP ${response.code}: ${body.take(200)}")
                val text = extractText(body).trim()
                if (text.isBlank()) error("LLM returned an empty response")
                text
            }
        }
    }

    private fun ollamaBaseUrl(): String? =
        endpoint.takeIf { it.endsWith("/api/chat") }?.removeSuffix("/api/chat")

    private fun extractText(body: String): String {
        if (body.isBlank()) return ""
        return runCatching {
            val json = JSONObject(body)
            json.optJSONObject("message")?.optString("content").orEmpty()
                .ifBlank { json.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content").orEmpty() }
        }.getOrDefault("")
    }
}
