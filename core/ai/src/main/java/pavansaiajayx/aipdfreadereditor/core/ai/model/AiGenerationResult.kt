package pavansaiajayx.aipdfreadereditor.core.ai.model

data class AiGenerationResult(
    val text: String,
    val promptTokens: Int,
    val candidateTokens: Int
)
