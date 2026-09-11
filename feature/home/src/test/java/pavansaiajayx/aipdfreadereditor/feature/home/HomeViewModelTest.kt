package pavansaiajayx.aipdfreadereditor.feature.home

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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pavansaiajayx.aipdfreadereditor.core.database.model.DocumentEntity
import pavansaiajayx.aipdfreadereditor.core.datastore.economy.CreditManager
import pavansaiajayx.aipdfreadereditor.core.model.CreditReason

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
class HomeViewModelTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var creditManager: CreditManager
    private lateinit var documentDao: FakeDocumentDao
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        dataStore = InMemoryPreferencesDataStore()
        creditManager = CreditManager(dataStore)
        documentDao = FakeDocumentDao()
        viewModel = HomeViewModel(
            creditManager = creditManager,
            documentDao = documentDao
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_observesCreditsAndDocuments() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        creditManager.addCredits(10, CreditReason.RewardedAdWatch)
        documentDao.insert(
            DocumentEntity(
                fileName = "invoice.pdf",
                uri = "content://docs/invoice"
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(10, state.credits)
        assertEquals(1, state.recentDocuments.size)
        assertEquals("invoice.pdf", state.recentDocuments.first().fileName)
    }

    @Test
    fun onAction_searchQueryChanged_filtersDocumentsInMemory() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        documentDao.insert(DocumentEntity(fileName = "Invoice_2026.pdf", uri = "content://doc/1"))
        documentDao.insert(DocumentEntity(fileName = "Annual_Report.pdf", uri = "content://doc/2"))
        advanceUntilIdle()

        viewModel.onAction(HomeAction.SearchQueryChanged("invoice"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("invoice", state.searchQuery)
        assertEquals(1, state.recentDocuments.size)
        assertEquals("Invoice_2026.pdf", state.recentDocuments.first().fileName)
    }

    @Test
    fun onAction_deleteDocument_removesFromDao() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val docId = documentDao.insert(DocumentEntity(fileName = "temp.pdf", uri = "content://doc/temp"))
        advanceUntilIdle()

        val doc = documentDao.getDocumentById(docId)!!
        viewModel.onAction(HomeAction.DeleteDocument(doc))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.recentDocuments.isEmpty())
    }

    @Test
    fun onAction_documentClicked_emitsNavigateToViewer() = runTest(testDispatcher) {
        val events = mutableListOf<HomeEvent>()
        val job = launch {
            viewModel.events.collect { events.add(it) }
        }

        viewModel.onAction(HomeAction.DocumentClicked("content://doc/target"))
        advanceUntilIdle()

        assertEquals(1, events.size)
        assertEquals(HomeEvent.NavigateToViewer("content://doc/target"), events.first())
        job.cancel()
    }

    @Test
    fun onAction_openToolsClicked_emitsNavigateToTools() = runTest(testDispatcher) {
        val events = mutableListOf<HomeEvent>()
        val job = launch {
            viewModel.events.collect { events.add(it) }
        }

        viewModel.onAction(HomeAction.OpenToolsClicked)
        advanceUntilIdle()

        assertEquals(1, events.size)
        assertEquals(HomeEvent.NavigateToTools, events.first())
        job.cancel()
    }

    @Test
    fun onAction_openPdfUri_insertsDocumentAndEmitsNavigateToViewer() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val events = mutableListOf<HomeEvent>()
        val job = launch {
            viewModel.events.collect { events.add(it) }
        }

        viewModel.onAction(HomeAction.OpenPdfUri(uri = "content://doc/selected.pdf", fileName = "selected.pdf"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.recentDocuments.size)
        assertEquals("selected.pdf", state.recentDocuments.first().fileName)
        assertTrue(events.contains(HomeEvent.NavigateToViewer("content://doc/selected.pdf")))
        job.cancel()
    }
}
