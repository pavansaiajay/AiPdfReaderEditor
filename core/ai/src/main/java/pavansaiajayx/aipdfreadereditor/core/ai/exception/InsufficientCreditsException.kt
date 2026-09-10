package pavansaiajayx.aipdfreadereditor.core.ai.exception

class InsufficientCreditsException(
    val currentBalance: Int = 0,
    val requiredCredits: Int = 1,
    message: String = "Insufficient credits ($currentBalance). At least $requiredCredits credit required to perform this AI operation."
) : Exception(message)
