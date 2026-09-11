package pavansaiajayx.aipdfreadereditor.feature.chat

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pavansaiajayx.aipdfreadereditor.core.ai.AiEngine
import pavansaiajayx.aipdfreadereditor.core.ai.client.GeminiClient
import pavansaiajayx.aipdfreadereditor.core.ai.model.AiGenerationResult
import pavansaiajayx.aipdfreadereditor.core.datastore.economy.CreditManager
import pavansaiajayx.aipdfreadereditor.core.model.AiRole
import pavansaiajayx.aipdfreadereditor.core.model.CreditReason

private class InMemoryPreferencesDataStore(initial: Preferences = emptyPreferences()) : DataStore<Preferences> {
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
    var chatCallCount = 0
    var shouldFail = false
    var chatResponse = AiGenerationResult(text = "AI document insights", promptTokens = 200, candidateTokens = 100)

    override suspend fun generateSummary(prompt: String): AiGenerationResult {
        if (shouldFail) throw RuntimeException("API error")
        return chatResponse
    }

    override suspend fun sendChatMessage(systemContext: String, userMessage: String): AiGenerationResult {
        if (shouldFail) throw RuntimeException("API error")
        chatCallCount++
        return chatResponse
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var creditManager: CreditManager
    private lateinit var fakeGeminiClient: FakeGeminiClient
    private lateinit var aiEngine: AiEngine
    private lateinit var viewModel: ChatViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        dataStore = InMemoryPreferencesDataStore()
        creditManager = CreditManager(dataStore)
        fakeGeminiClient = FakeGeminiClient()
        aiEngine = AiEngine(
            geminiClient = fakeGeminiClient,
            creditManager = creditManager,
            ioDispatcher = testDispatcher
        )
        viewModel = ChatViewModel(
            aiEngine = aiEngine,
            creditManager = creditManager
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_hasEmptyMessagesAndZeroCredits() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("", state.documentUri)
        assertEquals(0, state.credits)
        assertTrue(state.messages.isEmpty())
        assertFalse(state.isAiResponding)
        assertFalse(state.showInsufficientCreditsDialog)
    }

    @Test
    fun onAction_loadDocument_setsDocumentInfo() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        viewModel.onAction(ChatAction.LoadDocument("content://docs/report.pdf", "report.pdf"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("content://docs/report.pdf", state.documentUri)
        assertEquals("report.pdf", state.fileName)
    }

    @Test
    fun onAction_inputQueryChanged_updatesQuery() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        viewModel.onAction(ChatAction.InputQueryChanged("What are the conclusions?"))
        advanceUntilIdle()

        assertEquals("What are the conclusions?", viewModel.uiState.value.inputQuery)
    }

    @Test
    fun onAction_sendMessage_whenInsufficientCredits_showsCreditsDialogAndDoesNotCallAi() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        // Default balance is 0 credits (< 1 required)
        viewModel.onAction(ChatAction.InputQueryChanged("Summarize this"))
        viewModel.onAction(ChatAction.SendMessage)
        advanceUntilIdle()

        assertEquals(0, fakeGeminiClient.chatCallCount)
        assertTrue(viewModel.uiState.value.showInsufficientCreditsDialog)
        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }

    @Test
    fun onAction_sendMessage_whenSufficientCredits_postsMessagesAndDeductsCredits() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        // Fund user with 5 credits
        creditManager.addCredits(5, CreditReason.RewardedAdWatch)
        advanceUntilIdle()

        viewModel.onAction(ChatAction.InputQueryChanged("Summarize key points"))
        viewModel.onAction(ChatAction.SendMessage)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, fakeGeminiClient.chatCallCount)
        assertEquals(2, state.messages.size)
        assertEquals(AiRole.User, state.messages[0].role)
        assertEquals("Summarize key points", state.messages[0].content)
        assertEquals(AiRole.Model, state.messages[1].role)
        assertEquals("AI document insights", state.messages[1].content)
        assertFalse(state.isAiResponding)
        // 5 credits - 1 credit cost = 4 credits
        assertEquals(4, state.credits)
    }

    @Test
    fun onAction_backClicked_emitsNavigateBack() = runTest(testDispatcher) {
        val events = mutableListOf<ChatEvent>()
        val job = launch { viewModel.events.collect { events.add(it) } }

        viewModel.onAction(ChatAction.BackClicked)
        advanceUntilIdle()

        assertEquals(1, events.size)
        assertEquals(ChatEvent.NavigateBack, events.first())
        job.cancel()
    }

    @Test
    fun invariant_failedAiCallNeverDeductsCredits() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        creditManager.addCredits(5, CreditReason.RewardedAdWatch)
        fakeGeminiClient.shouldFail = true
        advanceUntilIdle()

        viewModel.onAction(ChatAction.InputQueryChanged("Fail this request"))
        viewModel.onAction(ChatAction.SendMessage)
        advanceUntilIdle()

        // Balance MUST remain exactly 5 credits on failure!
        assertEquals(5, viewModel.uiState.value.credits)
        assertNotNull(viewModel.uiState.value.errorMessage)
    }
}
