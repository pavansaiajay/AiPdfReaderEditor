package pavansaiajayx.aipdfreadereditor.core.common.economy

import kotlin.math.ceil

object TokenCalculator {
    const val DEFAULT_WEIGHT_IN = 1.0
    const val DEFAULT_WEIGHT_OUT = 3.0
    const val DEFAULT_TOKEN_RATE = 2500

    fun calculateCreditCost(
        promptTokens: Int,
        candidateTokens: Int,
        weightIn: Double = DEFAULT_WEIGHT_IN,
        weightOut: Double = DEFAULT_WEIGHT_OUT,
        tokenRate: Int = DEFAULT_TOKEN_RATE
    ): Int {
        if (promptTokens <= 0 && candidateTokens <= 0) return 1
        val effectiveTokens = promptTokens * weightIn + candidateTokens * weightOut
        val calculated = ceil(effectiveTokens / tokenRate).toInt()
        return maxOf(1, calculated)
    }
}
