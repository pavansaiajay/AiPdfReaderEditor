package pavansaiajayx.aipdfreadereditor.core.ai.client

import pavansaiajayx.aipdfreadereditor.core.ai.model.AiGenerationResult

interface GeminiClient {
    suspend fun generateSummary(prompt: String): AiGenerationResult
    suspend fun sendChatMessage(systemContext: String, userMessage: String): AiGenerationResult
}
