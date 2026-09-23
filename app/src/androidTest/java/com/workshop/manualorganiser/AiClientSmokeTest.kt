package com.workshop.manualorganiser

import com.workshop.manualorganiser.util.AiClient
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class AiClientSmokeTest {
    @Test
    fun realOllamaRoundTripWorksWhenCiEndpointIsConfigured() = runBlocking {
        if (!BuildConfig.AI_ENDPOINT.contains("10.0.2.2")) return@runBlocking

        val status = AiClient.checkServer().getOrThrow()
        assertTrue(status.contains(AiClient.model))

        val answer = AiClient.ask(
            question = "Reply with exactly the word OK.",
            systemContext = "This is a connectivity smoke test.",
        ).getOrThrow()

        assertTrue("Ollama returned an empty answer", answer.isNotBlank())
    }
}
