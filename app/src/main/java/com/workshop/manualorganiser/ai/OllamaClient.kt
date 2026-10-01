package com.workshop.manualorganiser.ai

import com.workshop.manualorganiser.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object OllamaClient {
    private val http by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    suspend fun chat(history: List<Pair<Boolean, String>>, thinking: Boolean): String {
        val messages = JSONArray()
        messages.put(JSONObject().put("role", "system").put(
            "content",
            "You are Workshop Manual Organiser's local technical assistant. Prefer information from the user's local workshop manuals when supplied. Never invent a manual citation."
        ))
        history.forEach { (isUser, text) ->
            messages.put(JSONObject().put("role", if (isUser) "user" else "assistant").put("content", text))
        }

        val body = JSONObject()
            .put("model", BuildConfig.AI_MODEL)
            .put("messages", messages)
            .put("stream", false)
            .put("options", JSONObject().put("temperature", if (thinking) 0.2 else 0.4))
            .toString()

        val request = Request.Builder()
            .url(BuildConfig.AI_ENDPOINT)
            .header("Content-Type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        http.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("Ollama HTTP ${response.code}: ${raw.take(300)}")
            val text = JSONObject(raw).optJSONObject("message")?.optString("content").orEmpty()
            if (text.isBlank()) error("Ollama returned no message content")
            return text
        }
    }

    suspend fun check(): Result<String> = runCatching {
        val request = Request.Builder().url(BuildConfig.AI_ENDPOINT.replace("/api/chat", "/api/tags")).get().build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Ollama HTTP ${response.code}")
            "Ollama online"
        }
    }
}
