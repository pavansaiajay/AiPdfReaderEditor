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
import pavansaiajayx.aipdfreadereditor.core.model.AiRole

private class ChatTestDataStore(initial: Preferences = emptyPreferences()) : DataStore<Preferences> {
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

private class CapturingGeminiClient : GeminiClient {
    var lastSummaryPrompt: String? = null
    var lastSystemContext: String? = null
    var lastUserMessage: String? = null

    override suspend fun generateSummary(prompt: String): AiGenerationResult {
        lastSummaryPrompt = prompt
        return AiGenerationResult(text = "Summary Output", promptTokens = 200, candidateTokens = 100)
    }

    override suspend fun sendChatMessage(systemContext: String, userMessage: String): AiGenerationResult {
        lastSystemContext = systemContext
        lastUserMessage = userMessage
        return AiGenerationResult(text = "Answer: $userMessage", promptTokens = 150, candidateTokens = 80)
    }
}

class AiEngineChatTest {

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var creditManager: CreditManager
    private lateinit var capturingClient: CapturingGeminiClient
    private lateinit var aiEngine: AiEngine
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        dataStore = ChatTestDataStore()
        creditManager = CreditManager(dataStore)
        capturingClient = CapturingGeminiClient()
        aiEngine = AiEngine(
            geminiClient = capturingClient,
            creditManager = creditManager,
            ioDispatcher = testDispatcher
        )
    }

    @Test
    fun generateSummaryFormatsPromptCorrectly() = runTest(testDispatcher) {
        creditManager.addCredits(5)
        val sampleText = "The quick brown fox jumps over the lazy dog."

        val result = aiEngine.generateSummary(sampleText)
        assertTrue(result is Result.Success)

        val promptSent = capturingClient.lastSummaryPrompt
        assertTrue("Prompt must include document text", promptSent?.contains(sampleText) == true)
        assertTrue("Prompt must include summarizer role instruction", promptSent?.contains("expert document analyzer") == true)
    }

    @Test
    fun createChatSessionInitializesWithContextAndEmptyHistory() {
        val session = aiEngine.createChatSession("Context: Quarterly Financials 2026")
        assertEquals("Context: Quarterly Financials 2026", session.documentContext)
        assertTrue("Initial history must be empty", session.getMessages().isEmpty())
    }

    @Test
    fun chatSessionAppendsUserAndModelMessagesOnSuccess() = runTest(testDispatcher) {
        creditManager.addCredits(5)
        val session = aiEngine.createChatSession("Context: Engineering Spec")

        val result = session.sendMessage("What is the concurrency model?")
        assertTrue(result is Result.Success)

        val messages = session.getMessages()
        assertEquals(2, messages.size)

        val userMsg = messages[0]
        assertEquals(AiRole.User, userMsg.role)
        assertEquals("What is the concurrency model?", userMsg.content)

        val modelMsg = messages[1]
        assertEquals(AiRole.Model, modelMsg.role)
        assertEquals("Answer: What is the concurrency model?", modelMsg.content)
        assertEquals(1, modelMsg.tokenUsage?.creditCost)

        assertEquals("Context: Engineering Spec", capturingClient.lastSystemContext)
        assertEquals("What is the concurrency model?", capturingClient.lastUserMessage)
    }

    @Test
    fun chatSessionFailsWhenZeroCreditsWithoutAlteringHistory() = runTest(testDispatcher) {
        // Balance is 0
        val session = aiEngine.createChatSession("Context: Engineering Spec")

        val result = session.sendMessage("Hello")
        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.cause is InsufficientCreditsException)

        assertTrue("History must remain empty on preflight failure", session.getMessages().isEmpty())
        assertEquals(null, capturingClient.lastUserMessage)
    }
}
