package pavansaiajayx.aipdfreadereditor.feature.tools

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
import java.io.File
import java.nio.file.Files

@OptIn(ExperimentalCoroutinesApi::class)
class ToolsViewModelTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
    private lateinit var tempDir: File
    private lateinit var fakePdfEngine: FakePdfEngine
    private lateinit var fakeDocumentDao: FakeDocumentDao
    private lateinit var viewModel: ToolsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        tempDir = Files.createTempDirectory("tools_test").toFile()
        fakePdfEngine = FakePdfEngine(tempDir)
        fakeDocumentDao = FakeDocumentDao()
        viewModel = ToolsViewModel(
            pdfEngine = fakePdfEngine,
            documentDao = fakeDocumentDao
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        tempDir.deleteRecursively()
    }

    @Test
    fun onAction_selectTool_emitsLaunchFilePicker() = runTest(testDispatcher) {
        val events = mutableListOf<ToolsEvent>()
        val job = launch {
            viewModel.events.collect { events.add(it) }
        }

        viewModel.onAction(ToolsAction.SelectTool(ToolType.Merge))
        advanceUntilIdle()

        assertEquals(1, events.size)
        val event = events.first() as ToolsEvent.LaunchFilePicker
        assertEquals(ToolType.Merge, event.tool)
        assertTrue(event.isMultiple)
        assertEquals("application/pdf", event.mimeType)
        job.cancel()
    }

    @Test
    fun onAction_filesSelected_forMerge_executesMergeAndInsertsInDao() = runTest(testDispatcher) {
        viewModel.onAction(ToolsAction.SelectTool(ToolType.Merge))
        viewModel.onAction(ToolsAction.FilesSelected(listOf("file:///doc1.pdf", "file:///doc2.pdf")))
        advanceUntilIdle()

        assertTrue(fakePdfEngine.mergeCalled)
        val state = viewModel.uiState.value
        assertFalse(state.isProcessing)
        assertEquals("PDFs merged successfully", state.resultMessage)
        assertNotNull(state.resultUri)

        val inserted = fakeDocumentDao.getDocumentByUri(state.resultUri!!)
        assertNotNull(inserted)
    }

    @Test
    fun onAction_filesSelected_forSplit_executesSplit() = runTest(testDispatcher) {
        viewModel.onAction(ToolsAction.SelectTool(ToolType.Split))
        viewModel.onAction(ToolsAction.FilesSelected(listOf("file:///doc.pdf")))
        advanceUntilIdle()

        assertTrue(fakePdfEngine.splitCalled)
        val state = viewModel.uiState.value
        assertFalse(state.isProcessing)
        assertEquals("Successfully split into 2 pages", state.resultMessage)
    }

    @Test
    fun onAction_filesSelected_forCompress_executesCompress() = runTest(testDispatcher) {
        viewModel.onAction(ToolsAction.SelectTool(ToolType.Compress))
        viewModel.onAction(ToolsAction.FilesSelected(listOf("file:///large.pdf")))
        advanceUntilIdle()

        assertTrue(fakePdfEngine.compressCalled)
        val state = viewModel.uiState.value
        assertEquals("PDF compressed successfully", state.resultMessage)
    }

    @Test
    fun onAction_encrypt_showsPasswordDialog_andExecutesOnPasswordSubmitted() = runTest(testDispatcher) {
        viewModel.onAction(ToolsAction.SelectTool(ToolType.Encrypt))
        viewModel.onAction(ToolsAction.FilesSelected(listOf("file:///doc.pdf")))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showPasswordDialog)

        viewModel.onAction(ToolsAction.PasswordSubmitted("safe123"))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showPasswordDialog)
        assertTrue(fakePdfEngine.encryptCalled)
        assertEquals("PDF protected with password", viewModel.uiState.value.resultMessage)
    }

    @Test
    fun onAction_watermark_showsWatermarkDialog_andExecutesOnWatermarkSubmitted() = runTest(testDispatcher) {
        viewModel.onAction(ToolsAction.SelectTool(ToolType.Watermark))
        viewModel.onAction(ToolsAction.FilesSelected(listOf("file:///doc.pdf")))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showWatermarkDialog)

        viewModel.onAction(ToolsAction.WatermarkSubmitted("CONFIDENTIAL"))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showWatermarkDialog)
        assertTrue(fakePdfEngine.watermarkCalled)
        assertEquals("Watermark applied successfully", viewModel.uiState.value.resultMessage)
    }

    @Test
    fun onAction_rotate_showsRotationDialog_andExecutesOnRotationSubmitted() = runTest(testDispatcher) {
        viewModel.onAction(ToolsAction.SelectTool(ToolType.Rotate))
        viewModel.onAction(ToolsAction.FilesSelected(listOf("file:///doc.pdf")))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showRotationDialog)

        viewModel.onAction(ToolsAction.RotationSubmitted(90))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showRotationDialog)
        assertTrue(fakePdfEngine.rotateCalled)
        assertEquals("Pages rotated successfully", viewModel.uiState.value.resultMessage)
    }

    @Test
    fun onAction_backClicked_emitsNavigateBack() = runTest(testDispatcher) {
        val events = mutableListOf<ToolsEvent>()
        val job = launch {
            viewModel.events.collect { events.add(it) }
        }

        viewModel.onAction(ToolsAction.BackClicked)
        advanceUntilIdle()

        assertEquals(1, events.size)
        assertEquals(ToolsEvent.NavigateBack, events.first())
        job.cancel()
    }

    @Test
    fun onOperationFailure_setsErrorMessage() = runTest(testDispatcher) {
        fakePdfEngine.shouldFail = true

        viewModel.onAction(ToolsAction.SelectTool(ToolType.Merge))
        viewModel.onAction(ToolsAction.FilesSelected(listOf("file:///doc1.pdf", "file:///doc2.pdf")))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isProcessing)
        assertEquals("Merge failed", state.errorMessage)
        assertNull(state.resultMessage)
    }
}
