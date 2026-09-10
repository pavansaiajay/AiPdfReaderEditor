package pavansaiajayx.aipdfreadereditor.core.ai

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pavansaiajayx.aipdfreadereditor.core.ai.client.GeminiClient
import pavansaiajayx.aipdfreadereditor.core.ai.exception.InsufficientCreditsException
import pavansaiajayx.aipdfreadereditor.core.ai.model.AiGenerationResult
import pavansaiajayx.aipdfreadereditor.core.common.result.Result
import pavansaiajayx.aipdfreadereditor.core.datastore.economy.CreditManager

private class TestPreferencesDataStore(initial: Preferences = emptyPreferences()) : DataStore<Preferences> {
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

private class FakeGeminiClient : GeminiClient {
    var generateSummaryCallCount = 0
    var sendChatMessageCallCount = 0

    var summaryResponse = AiGenerationResult(text = "Summary text", promptTokens = 100, candidateTokens = 50)
    var chatResponse = AiGenerationResult(text = "Chat answer", promptTokens = 150, candidateTokens = 60)

    override suspend fun generateSummary(prompt: String): AiGenerationResult {
        generateSummaryCallCount++
        return summaryResponse
    }

    override suspend fun sendChatMessage(systemContext: String, userMessage: String): AiGenerationResult {
        sendChatMessageCallCount++
        return chatResponse
    }
}

class AiEnginePreflightTest {

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var creditManager: CreditManager
    private lateinit var fakeGeminiClient: FakeGeminiClient
    private lateinit var aiEngine: AiEngine
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        dataStore = TestPreferencesDataStore()
        creditManager = CreditManager(dataStore)
        fakeGeminiClient = FakeGeminiClient()
        aiEngine = AiEngine(
            geminiClient = fakeGeminiClient,
            creditManager = creditManager,
            ioDispatcher = testDispatcher
        )
    }

    @Test
    fun generateSummaryFailsImmediatelyWhenZeroCreditsAndMakesNoApiCall() = runTest(testDispatcher) {
        // Balance is 0 by default
        val result = aiEngine.generateSummary("Sample document text to summarize")

        assertTrue("Expected Result.Error for 0 credits", result is Result.Error)
        val error = result as Result.Error
        assertTrue(
            "Expected InsufficientCreditsException or cause",
            error.cause is InsufficientCreditsException
        )
        assertEquals("Gemini API should NOT be called", 0, fakeGeminiClient.generateSummaryCallCount)
    }

    @Test
    fun sendChatMessageFailsImmediatelyWhenZeroCreditsAndMakesNoApiCall() = runTest(testDispatcher) {
        // Balance is 0 by default
        val result = aiEngine.sendChatMessage(
            systemContext = "PDF Document Context",
            userMessage = "What is the key takeaway?"
        )

        assertTrue("Expected Result.Error for 0 credits", result is Result.Error)
        val error = result as Result.Error
        assertTrue(
            "Expected InsufficientCreditsException or cause",
            error.cause is InsufficientCreditsException
        )
        assertEquals("Gemini API should NOT be called", 0, fakeGeminiClient.sendChatMessageCallCount)
    }
}
