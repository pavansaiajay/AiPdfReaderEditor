package pavansaiajayx.aipdfreadereditor.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class CreditReason {
    DailyWelcomeBonus,
    RewardedAdWatch,
    AiChatQuery,
    AiDocumentSummary,
    AiOcrExtraction,
    ManualAdjustment
}

@Serializable
data class CreditBalance(
    val currentCredits: Int = 10,
    val lastBonusEpochMs: Long = 0L,
    val totalCreditsEarned: Int = 10,
    val totalCreditsSpent: Int = 0
)

@Serializable
data class CreditTransaction(
    val id: String,
    val timestampEpochMs: Long,
    val amountDelta: Int,
    val reason: CreditReason,
    val inputTokens: Int = 0,
    val outputTokens: Int = 0,
    val resultingBalance: Int
)
