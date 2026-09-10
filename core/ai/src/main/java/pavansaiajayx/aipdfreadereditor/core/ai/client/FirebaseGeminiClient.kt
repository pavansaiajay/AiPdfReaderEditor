package pavansaiajayx.aipdfreadereditor.core.ai.client

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import kotlinx.coroutines.delay
import pavansaiajayx.aipdfreadereditor.core.ai.model.AiGenerationResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseGeminiClient @Inject constructor() : GeminiClient {
    private val primaryModel by lazy { Firebase.ai.generativeModel("gemini-3.7-flash") }
    private val fallbackModel by lazy { Firebase.ai.generativeModel("gemini-3.5-flash-lite") }

    override suspend fun generateSummary(prompt: String): AiGenerationResult {
        var attempt = 1
        while (attempt <= 2) {
            try {
                val response = primaryModel.generateContent(prompt)
                val usage = response.usageMetadata
                val promptTokens = usage?.promptTokenCount ?: (prompt.length / 4)
                val candidateTokens = usage?.candidatesTokenCount ?: ((response.text?.length ?: 0) / 4)
                return AiGenerationResult(
                    text = response.text ?: "No summary generated.",
                    promptTokens = maxOf(1, promptTokens),
                    candidateTokens = maxOf(1, candidateTokens)
                )
            } catch (e: Exception) {
                if (attempt == 1) {
                    delay(1500)
                }
            }
            attempt++
        }

        val fallbackResponse = fallbackModel.generateContent(prompt)
        val fallbackUsage = fallbackResponse.usageMetadata
        val promptTokens = fallbackUsage?.promptTokenCount ?: (prompt.length / 4)
        val candidateTokens = fallbackUsage?.candidatesTokenCount ?: ((fallbackResponse.text?.length ?: 0) / 4)
        return AiGenerationResult(
            text = fallbackResponse.text ?: "No summary generated.",
            promptTokens = maxOf(1, promptTokens),
            candidateTokens = maxOf(1, candidateTokens)
        )
    }

    override suspend fun sendChatMessage(systemContext: String, userMessage: String): AiGenerationResult {
        var attempt = 1
        val fullPrompt = "Context:\n$systemContext\n\nUser Question:\n$userMessage"
        while (attempt <= 2) {
            try {
                val response = primaryModel.generateContent(fullPrompt)
                val usage = response.usageMetadata
                val promptTokens = usage?.promptTokenCount ?: (fullPrompt.length / 4)
                val candidateTokens = usage?.candidatesTokenCount ?: ((response.text?.length ?: 0) / 4)
                return AiGenerationResult(
                    text = response.text ?: "No response generated.",
                    promptTokens = maxOf(1, promptTokens),
                    candidateTokens = maxOf(1, candidateTokens)
                )
            } catch (e: Exception) {
                if (attempt == 1) {
                    delay(1500)
                }
            }
            attempt++
        }

        val fallbackResponse = fallbackModel.generateContent(fullPrompt)
        val fallbackUsage = fallbackResponse.usageMetadata
        val promptTokens = fallbackUsage?.promptTokenCount ?: (fullPrompt.length / 4)
        val candidateTokens = fallbackUsage?.candidatesTokenCount ?: ((fallbackResponse.text?.length ?: 0) / 4)
        return AiGenerationResult(
            text = fallbackResponse.text ?: "No response generated.",
            promptTokens = maxOf(1, promptTokens),
            candidateTokens = maxOf(1, candidateTokens)
        )
    }
}
