package pavansaiajayx.aipdfreadereditor.feature.onboarding

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pavansaiajayx.aipdfreadereditor.core.datastore.economy.CreditManager
import pavansaiajayx.aipdfreadereditor.core.datastore.preferences.UserPreferencesRepository

internal class InMemoryPreferencesDataStore(initial: Preferences = emptyPreferences()) : DataStore<Preferences> {
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

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var userPreferencesRepository: UserPreferencesRepository
    private lateinit var creditManager: CreditManager
    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        dataStore = InMemoryPreferencesDataStore()
        userPreferencesRepository = UserPreferencesRepository(dataStore)
        creditManager = CreditManager(dataStore)
        viewModel = OnboardingViewModel(
            userPreferencesRepository = userPreferencesRepository,
            creditManager = creditManager
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_hasDefaultValues() = runTest(testDispatcher) {
        val state = viewModel.uiState.value
        assertEquals(0, state.currentPage)
        assertEquals(3, state.totalPages)
        assertFalse(state.isCompleting)
    }

    @Test
    fun onAction_pageChanged_updatesCurrentPage() = runTest(testDispatcher) {
        viewModel.onAction(OnboardingAction.PageChanged(1))
        assertEquals(1, viewModel.uiState.value.currentPage)

        viewModel.onAction(OnboardingAction.PageChanged(2))
        assertEquals(2, viewModel.uiState.value.currentPage)
    }

    @Test
    fun onAction_nextClicked_onIntermediatePage_emitsScrollToPage() = runTest(testDispatcher) {
        val emittedEvents = mutableListOf<OnboardingEvent>()
        val job = launch {
            viewModel.events.collect { emittedEvents.add(it) }
        }

        viewModel.onAction(OnboardingAction.NextClicked)
        advanceUntilIdle()

        assertEquals(1, emittedEvents.size)
        assertEquals(OnboardingEvent.ScrollToPage(1), emittedEvents.first())
        job.cancel()
    }

    @Test
    fun onAction_completeOnboarding_persistsPreferenceAndAwardsCreditsAndNavigates() = runTest(testDispatcher) {
        val emittedEvents = mutableListOf<OnboardingEvent>()
        val job = launch {
            viewModel.events.collect { emittedEvents.add(it) }
        }

        viewModel.onAction(OnboardingAction.CompleteOnboarding)
        advanceUntilIdle()

        assertTrue(userPreferencesRepository.hasSeenOnboardingFlow.first())
        assertEquals(5, creditManager.creditsFlow.first())
        assertTrue(emittedEvents.contains(OnboardingEvent.NavigateToHome))
        job.cancel()
    }

    @Test
    fun onAction_skipClicked_completesOnboardingAndNavigates() = runTest(testDispatcher) {
        val emittedEvents = mutableListOf<OnboardingEvent>()
        val job = launch {
            viewModel.events.collect { emittedEvents.add(it) }
        }

        viewModel.onAction(OnboardingAction.SkipClicked)
        advanceUntilIdle()

        assertTrue(userPreferencesRepository.hasSeenOnboardingFlow.first())
        assertEquals(5, creditManager.creditsFlow.first())
        assertTrue(emittedEvents.contains(OnboardingEvent.NavigateToHome))
        job.cancel()
    }

    @Test
    fun onAction_nextClicked_onLastPage_triggersCompletion() = runTest(testDispatcher) {
        val emittedEvents = mutableListOf<OnboardingEvent>()
        val job = launch {
            viewModel.events.collect { emittedEvents.add(it) }
        }

        viewModel.onAction(OnboardingAction.PageChanged(2))
        viewModel.onAction(OnboardingAction.NextClicked)
        advanceUntilIdle()

        assertTrue(userPreferencesRepository.hasSeenOnboardingFlow.first())
        assertEquals(5, creditManager.creditsFlow.first())
        assertTrue(emittedEvents.contains(OnboardingEvent.NavigateToHome))
        job.cancel()
    }
}
