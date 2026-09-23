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

    val isConfigured: Boolean get() = BuildConfig.AI_ENDPOINT.isNotBlank()
    val endpoint: String get() = BuildConfig.AI_ENDPOINT
    val model: String get() = BuildConfig.AI_MODEL

    private val jsonType = "application/json; charset=utf-8".toMediaType()

    suspend fun checkServer(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val base = ollamaBaseUrl() ?: error("Local Ollama endpoint is not configured")
            val request = Request.Builder().url(base.trimEnd('/') + "/api/tags").get().build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) error("HTTP ${response.code}")
                val models = JSONObject(body).optJSONArray("models") ?: JSONArray()
                val names = (0 until models.length()).mapNotNull { i ->
                    models.optJSONObject(i)?.optString("name")?.takeIf { it.isNotBlank() }
                }
                if (names.isNotEmpty() && names.none { it == model }) {
                    error("Ollama is online, but model '$model' is not installed")
                }
                "Ollama online • $model"
            }
        }
    }

    suspend fun ask(question: String, systemContext: String): Result<String> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext Result.failure(IllegalStateException("Local LLM endpoint is not configured"))
        if (question.isBlank()) return@withContext Result.failure(IllegalArgumentException("Question is empty"))

        runCatching {
            val messages = JSONArray()
                .put(JSONObject().apply {
                    put("role", "system")
                    put(
                        "content",
                        "You are a workshop-manual technical assistant. Use the supplied offline reference context when relevant. Do not invent vehicle-specific specifications. Clearly state uncertainty. Context:\n$systemContext",
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
                    if (BuildConfig.AI_API_KEY.isNotBlank()) {
                        header("Authorization", "Bearer ${BuildConfig.AI_API_KEY}")
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
        }.getOrDefault("")
    }
}
