package pavansaiajayx.aipdfreadereditor.core.common.economy

import org.junit.Assert.assertEquals
import org.junit.Test

class TokenCalculatorTest {

    @Test
    fun zeroTokensReturnsMinimumOneCredit() {
        val cost = TokenCalculator.calculateCreditCost(promptTokens = 0, candidateTokens = 0)
        assertEquals(1, cost)
    }

    @Test
    fun smallQueriesCostFloorOfOneCredit() {
        // (100 * 1.0 + 50 * 3.0) = 250 tokens -> 250 / 2500 = 0.1 -> ceil = 1
        val cost = TokenCalculator.calculateCreditCost(promptTokens = 100, candidateTokens = 50)
        assertEquals(1, cost)
    }

    @Test
    fun boundaryAtExactRateReturnsOneCredit() {
        // 2500 * 1.0 = 2500 -> 2500 / 2500 = 1
        val cost = TokenCalculator.calculateCreditCost(promptTokens = 2500, candidateTokens = 0)
        assertEquals(1, cost)
    }

    @Test
    fun boundaryJustOverRateCeilsToNextCredit() {
        // 2501 * 1.0 = 2501 -> 2501 / 2500 = 1.0004 -> ceil = 2
        val cost = TokenCalculator.calculateCreditCost(promptTokens = 2501, candidateTokens = 0)
        assertEquals(2, cost)
    }

    @Test
    fun outputTokensWeightedAtThreeTimes() {
        // (1000 * 1.0 + 1000 * 3.0) = 4000 -> 4000 / 2500 = 1.6 -> ceil = 2
        val cost = TokenCalculator.calculateCreditCost(promptTokens = 1000, candidateTokens = 1000)
        assertEquals(2, cost)
    }

    @Test
    fun largeSummaryCalculation() {
        // (5000 * 1.0 + 2000 * 3.0) = 11000 -> 11000 / 2500 = 4.4 -> ceil = 5
        val cost = TokenCalculator.calculateCreditCost(promptTokens = 5000, candidateTokens = 2000)
        assertEquals(5, cost)
    }
}
