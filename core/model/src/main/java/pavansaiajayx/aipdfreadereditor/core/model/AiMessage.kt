package pavansaiajayx.aipdfreadereditor.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class AiRole {
    User,
    Model,
    System
}

@Serializable
data class TokenUsage(
    val promptTokens: Int,
    val candidateTokens: Int,
    val creditCost: Int
)

@Serializable
data class AiCitation(
    val pageNumber: Int,
    val snippetText: String,
    val confidence: Float = 1.0f
)

@Serializable
data class AiMessage(
    val id: String,
    val role: AiRole,
    val content: String,
    val timestampEpochMs: Long = System.currentTimeMillis(),
    val tokenUsage: TokenUsage? = null,
    val citations: List<AiCitation> = emptyList(),
    val isPending: Boolean = false,
    val errorMessage: String? = null
)
