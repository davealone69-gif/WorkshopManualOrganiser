package com.workshop.manualorganiser.ai

import com.workshop.manualorganiser.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Optional Hermes gateway.
 *
 * This does not pretend Hermes is an Android library. If a real Hermes
 * OpenAI-compatible HTTP endpoint is configured, Workshop can route through it.
 * If it is blank, the caller must use Ollama directly.
 */
object HermesGatewayClient {
    private val http by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    suspend fun chat(history: List<Pair<Boolean, String>>, thinking: Boolean): Result<String> = runCatching {
        val endpoint = BuildConfig.HERMES_ENDPOINT.trim()
        if (endpoint.isBlank()) error("Hermes endpoint is not configured")

        val messages = JSONArray()
        messages.put(
            JSONObject()
                .put("role", "system")
                .put(
                    "content",
                    "You are the Hermes orchestration layer for Workshop Manual Organiser. Use connected tools only when actually available and never fabricate tool results."
                )
        )
        history.forEach { (isUser, text) ->
            messages.put(
                JSONObject()
                    .put("role", if (isUser) "user" else "assistant")
                    .put("content", text)
            )
        }

        val body = JSONObject()
            .put("model", BuildConfig.HERMES_MODEL)
            .put("messages", messages)
            .put("stream", false)
            .put("temperature", if (thinking) 0.2 else 0.4)
            .toString()

        val requestBuilder = Request.Builder()
            .url(endpoint)
            .header("Content-Type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))

        if (BuildConfig.HERMES_API_KEY.isNotBlank()) {
            requestBuilder.header("Authorization", "Bearer ${BuildConfig.HERMES_API_KEY}")
        }

        http.newCall(requestBuilder.build()).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("Hermes HTTP ${response.code}: ${raw.take(300)}")
            val text = JSONObject(raw)
                .optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content")
                .orEmpty()
            if (text.isBlank()) error("Hermes returned no message content")
            text
        }
    }
}
