package pavansaiajayx.aipdfreadereditor.app

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pavansaiajayx.aipdfreadereditor.core.datastore.preferences.UserPreferencesRepository
import pavansaiajayx.aipdfreadereditor.core.navigation.Screen

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
class MainViewModelTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var userPreferencesRepository: UserPreferencesRepository
    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        dataStore = InMemoryPreferencesDataStore()
        userPreferencesRepository = UserPreferencesRepository(dataStore)
        viewModel = MainViewModel(userPreferencesRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialStartDestinationIsOnboardingWhenPreferenceIsFalse() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Ready state", state is MainUiState.Ready)
        val readyState = state as MainUiState.Ready
        assertEquals(Screen.Onboarding, readyState.startDestination)
        assertNull(readyState.initialPdfUri)

        collectJob.cancel()
    }

    @Test
    fun initialStartDestinationIsHomeWhenOnboardingHasBeenSeen() = runTest {
        userPreferencesRepository.setHasSeenOnboarding(true)

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Ready state", state is MainUiState.Ready)
        val readyState = state as MainUiState.Ready
        assertEquals(Screen.Home, readyState.startDestination)
        assertNull(readyState.initialPdfUri)

        collectJob.cancel()
    }

    @Test
    fun deepLinkUriIsReflectedInUiState() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.setDeepLinkUri("content://com.example.provider/sample.pdf")
        advanceUntilIdle()

        val state = viewModel.uiState.value as MainUiState.Ready
        assertEquals("content://com.example.provider/sample.pdf", state.initialPdfUri)

        viewModel.clearDeepLink()
        advanceUntilIdle()

        val clearedState = viewModel.uiState.value as MainUiState.Ready
        assertNull(clearedState.initialPdfUri)

        collectJob.cancel()
    }
}
