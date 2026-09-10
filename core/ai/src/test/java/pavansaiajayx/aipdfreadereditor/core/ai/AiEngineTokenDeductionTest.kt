package pavansaiajayx.aipdfreadereditor.core.ai

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pavansaiajayx.aipdfreadereditor.core.ai.client.GeminiClient
import pavansaiajayx.aipdfreadereditor.core.ai.model.AiGenerationResult
import pavansaiajayx.aipdfreadereditor.core.common.economy.TokenCalculator
import pavansaiajayx.aipdfreadereditor.core.common.result.Result
import pavansaiajayx.aipdfreadereditor.core.datastore.economy.CreditManager
import java.io.IOException

private class DeductionTestDataStore(initial: Preferences = emptyPreferences()) : DataStore<Preferences> {
    private val state = MutableStateFlow(initial)
    private val mutex = Mutex()

    override val data: Flow<Preferences> = state.asStateFlow()

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        return mutex.withLock {
            val updated = transform(state.value)
            state.value = updated
            updated
        }
    }
}

private class ConfigurableFakeGeminiClient : GeminiClient {
    var generateSummaryResult: () -> AiGenerationResult = {
        AiGenerationResult(text = "Summary", promptTokens = 2500, candidateTokens = 2500)
    }
    var sendChatMessageResult: () -> AiGenerationResult = {
        AiGenerationResult(text = "Chat Answer", promptTokens = 1000, candidateTokens = 500)
    }

    override suspend fun generateSummary(prompt: String): AiGenerationResult {
        return generateSummaryResult()
    }

    override suspend fun sendChatMessage(systemContext: String, userMessage: String): AiGenerationResult {
        return sendChatMessageResult()
    }
}

class AiEngineTokenDeductionTest {

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var creditManager: CreditManager
    private lateinit var fakeGeminiClient: ConfigurableFakeGeminiClient
    private lateinit var aiEngine: AiEngine
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        dataStore = DeductionTestDataStore()
        creditManager = CreditManager(dataStore)
        fakeGeminiClient = ConfigurableFakeGeminiClient()
        aiEngine = AiEngine(
            geminiClient = fakeGeminiClient,
            creditManager = creditManager,
            ioDispatcher = testDispatcher
        )
    }

    @Test
    fun successfulSummaryDeductsExactCalculatedTokensFromBalance() = runTest(testDispatcher) {
        creditManager.addCredits(10)
        assertEquals(10, creditManager.creditsFlow.first())

        // 2500 in * 1.0 + 2500 out * 3.0 = 10,000 / 2500 = 4 credits
        fakeGeminiClient.generateSummaryResult = {
            AiGenerationResult(text = "Executive Summary", promptTokens = 2500, candidateTokens = 2500)
        }

        val result = aiEngine.generateSummary("Large document...")
        assertTrue("Expected Result.Success", result is Result.Success)
        val success = result as Result.Success
        assertEquals(4, success.data.tokenUsage?.creditCost)

        val finalBalance = creditManager.creditsFlow.first()
        assertEquals("Balance must decrease by exactly 4 credits", 6, finalBalance)
    }

    @Test
    fun successfulChatDeductsExactCalculatedTokensFromBalance() = runTest(testDispatcher) {
        creditManager.addCredits(10)

        // 1000 in * 1.0 + 500 out * 3.0 = 2500 / 2500 = 1 credit
        fakeGeminiClient.sendChatMessageResult = {
            AiGenerationResult(text = "Response", promptTokens = 1000, candidateTokens = 500)
        }

        val result = aiEngine.sendChatMessage("Context", "Question")
        assertTrue("Expected Result.Success", result is Result.Success)
        val success = result as Result.Success
        assertEquals(1, success.data.tokenUsage?.creditCost)

        val finalBalance = creditManager.creditsFlow.first()
        assertEquals("Balance must decrease by exactly 1 credit", 9, finalBalance)
    }

    @Test
    fun failedGenerationDeductsZeroCreditsFromBalance() = runTest(testDispatcher) {
        creditManager.addCredits(10)

        fakeGeminiClient.generateSummaryResult = {
            throw IOException("Network timeout connecting to Gemini API")
        }

        val result = aiEngine.generateSummary("Document text")
        assertTrue("Expected Result.Error", result is Result.Error)

        val finalBalance = creditManager.creditsFlow.first()
        assertEquals("Balance must remain strictly unchanged on failure", 10, finalBalance)
    }

    @Test
    fun minimumOneCreditDeductedForTinyResponse() = runTest(testDispatcher) {
        creditManager.addCredits(10)

        fakeGeminiClient.sendChatMessageResult = {
            AiGenerationResult(text = "Hi", promptTokens = 5, candidateTokens = 5)
        }

        val result = aiEngine.sendChatMessage("Ctx", "Hi")
        assertTrue("Expected Result.Success", result is Result.Success)
        val success = result as Result.Success
        assertEquals(1, success.data.tokenUsage?.creditCost)

        val finalBalance = creditManager.creditsFlow.first()
        assertEquals("Minimum 1 credit deducted", 9, finalBalance)
    }
}
