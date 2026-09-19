package com.workshop.manualorganiser.util

import com.workshop.manualorganiser.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Optional remote enrichment for AI Technical Help.
 *
 * The app is fully functional without this: if no endpoint is configured the
 * local [KnowledgeBase] answers. Configure with `-Pai.endpoint=…` and
 * `-Pai.apiKey=…` at build time so no credential ever lands in source control.
 */
object AiClient {

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    val isConfigured: Boolean get() = BuildConfig.AI_ENDPOINT.isNotBlank()

    private val jsonType = "application/json; charset=utf-8".toMediaType()

    /**
     * Asks the configured endpoint a question.
     * Returns a failure when unconfigured or unreachable so the caller can fall
     * back to the offline knowledge base.
     */
    suspend fun ask(question: String, systemContext: String): Result<String> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext Result.failure(IllegalStateException("not configured"))
        runCatching {
            val payload = JSONObject().apply {
                put("prompt", question)
                put("context", systemContext)
            }
            val builder = Request.Builder()
                .url(BuildConfig.AI_ENDPOINT)
                .post(payload.toString().toRequestBody(jsonType))
                .header("Content-Type", "application/json")
            if (BuildConfig.AI_API_KEY.isNotBlank()) {
                builder.header("Authorization", "Bearer ${BuildConfig.AI_API_KEY}")
            }
            client.newCall(builder.build()).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    error("HTTP ${response.code}")
                }
                extractText(body)
            }
        }
    }

    /** Tolerates the common response shapes of hosted LLM endpoints. */
    private fun extractText(body: String): String {
        if (body.isBlank()) return ""
        return runCatching {
            val json = JSONObject(body)
            when {
                json.has("answer") -> json.getString("answer")
                json.has("output") -> json.getString("output")
                json.has("text") -> json.getString("text")
                json.has("response") -> json.getString("response")
                json.has("completion") -> json.getString("completion")
                else -> body
            }
        }.getOrDefault(body)
    }
}
