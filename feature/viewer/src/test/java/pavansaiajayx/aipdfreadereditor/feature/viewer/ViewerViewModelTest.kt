package pavansaiajayx.aipdfreadereditor.feature.viewer

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pavansaiajayx.aipdfreadereditor.core.pdf.renderer.PdfRendererPool

@OptIn(ExperimentalCoroutinesApi::class)
class ViewerViewModelTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: ViewerViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ViewerViewModel(
            pdfRendererPool = PdfRendererPool()
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_hasDefaultValues() = runTest(testDispatcher) {
        val state = viewModel.uiState.value
        assertEquals("", state.documentUri)
        assertEquals("", state.fileName)
        assertEquals(0, state.currentPage)
        assertEquals(0, state.pageCount)
        assertEquals(1.0f, state.zoomLevel, 0.001f)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun onAction_loadDocument_updatesDocumentMetadata() = runTest(testDispatcher) {
        viewModel.onAction(ViewerAction.LoadDocument(uri = "content://docs/contract.pdf", fileName = "contract.pdf", pageCount = 12))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("content://docs/contract.pdf", state.documentUri)
        assertEquals("contract.pdf", state.fileName)
        assertEquals(12, state.pageCount)
        assertFalse(state.isLoading)
    }

    @Test
    fun onAction_pageChanged_updatesCurrentPage() = runTest(testDispatcher) {
        viewModel.onAction(ViewerAction.LoadDocument(uri = "content://docs/doc.pdf", fileName = "doc.pdf", pageCount = 10))
        viewModel.onAction(ViewerAction.PageChanged(4))
        advanceUntilIdle()

        assertEquals(4, viewModel.uiState.value.currentPage)
    }

    @Test
    fun onAction_zoomChanged_clampsZoomBetweenOneAndFive() = runTest(testDispatcher) {
        viewModel.onAction(ViewerAction.ZoomChanged(2.5f))
        assertEquals(2.5f, viewModel.uiState.value.zoomLevel, 0.001f)

        // Below 1.0f should clamp to 1.0f
        viewModel.onAction(ViewerAction.ZoomChanged(0.5f))
        assertEquals(1.0f, viewModel.uiState.value.zoomLevel, 0.001f)

        // Above 5.0f should clamp to 5.0f
        viewModel.onAction(ViewerAction.ZoomChanged(8.0f))
        assertEquals(5.0f, viewModel.uiState.value.zoomLevel, 0.001f)
    }

    @Test
    fun onAction_resetZoom_resetsToDefaultOne() = runTest(testDispatcher) {
        viewModel.onAction(ViewerAction.ZoomChanged(3.0f))
        assertEquals(3.0f, viewModel.uiState.value.zoomLevel, 0.001f)

        viewModel.onAction(ViewerAction.ResetZoom)
        assertEquals(1.0f, viewModel.uiState.value.zoomLevel, 0.001f)
    }

    @Test
    fun onAction_backClicked_emitsNavigateBack() = runTest(testDispatcher) {
        val events = mutableListOf<ViewerEvent>()
        val job = launch { viewModel.events.collect { events.add(it) } }

        viewModel.onAction(ViewerAction.BackClicked)
        advanceUntilIdle()

        assertEquals(1, events.size)
        assertEquals(ViewerEvent.NavigateBack, events.first())
        job.cancel()
    }

    @Test
    fun onAction_chatClicked_emitsNavigateToChat() = runTest(testDispatcher) {
        viewModel.onAction(ViewerAction.LoadDocument(uri = "content://docs/sample.pdf", fileName = "sample.pdf", pageCount = 5))
        val events = mutableListOf<ViewerEvent>()
        val job = launch { viewModel.events.collect { events.add(it) } }

        viewModel.onAction(ViewerAction.ChatClicked)
        advanceUntilIdle()

        assertEquals(1, events.size)
        assertEquals(ViewerEvent.NavigateToChat("content://docs/sample.pdf"), events.first())
        job.cancel()
    }

    @Test
    fun onAction_toolsClicked_emitsNavigateToTools() = runTest(testDispatcher) {
        val events = mutableListOf<ViewerEvent>()
        val job = launch { viewModel.events.collect { events.add(it) } }

        viewModel.onAction(ViewerAction.ToolsClicked)
        advanceUntilIdle()

        assertEquals(1, events.size)
        assertEquals(ViewerEvent.NavigateToTools, events.first())
        job.cancel()
    }

    @Test
    fun invariant_offlineViewerNeverReferencesCreditManager() {
        val fields = ViewerViewModel::class.java.declaredFields
        val hasCreditManager = fields.any { it.type.name.contains("CreditManager") }
        assertFalse("ViewerViewModel must never inject or reference CreditManager", hasCreditManager)
    }
}
