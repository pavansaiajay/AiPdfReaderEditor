package pavansaiajayx.aipdfreadereditor.feature.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ToolsScreenTest {

    @Test
    fun toolsUiState_defaultsAreValid() {
        val state = ToolsUiState()
        assertEquals(false, state.isProcessing)
        assertEquals(0, state.progress)
        assertNull(state.activeTool)
        assertEquals(0, state.selectedUris.size)
        assertNull(state.resultMessage)
        assertNull(state.resultUri)
        assertNull(state.errorMessage)
        assertEquals(false, state.showPasswordDialog)
        assertEquals(false, state.showWatermarkDialog)
        assertEquals(false, state.showRotationDialog)
    }

    @Test
    fun toolsUiState_copyPreservesInvariants() {
        val state = ToolsUiState(
            isProcessing = true,
            progress = 75,
            activeTool = ToolType.Merge,
            selectedUris = listOf("uri1", "uri2"),
            resultMessage = "Merged successfully",
            resultUri = "file:///cache/merged.pdf",
            errorMessage = "None",
            showPasswordDialog = true,
            showWatermarkDialog = true,
            showRotationDialog = true
        )

        assertEquals(true, state.isProcessing)
        assertEquals(75, state.progress)
        assertEquals(ToolType.Merge, state.activeTool)
        assertEquals(2, state.selectedUris.size)
        assertEquals("Merged successfully", state.resultMessage)
        assertEquals("file:///cache/merged.pdf", state.resultUri)
        assertEquals("None", state.errorMessage)
        assertEquals(true, state.showPasswordDialog)
        assertEquals(true, state.showWatermarkDialog)
        assertEquals(true, state.showRotationDialog)
    }

    @Test
    fun toolsActions_instantiateCorrectly() {
        val select = ToolsAction.SelectTool(ToolType.Compress)
        val files = ToolsAction.FilesSelected(listOf("u1", "u2"))
        val pwd = ToolsAction.PasswordSubmitted("secret123")
        val wm = ToolsAction.WatermarkSubmitted("DRAFT")
        val rot = ToolsAction.RotationSubmitted(90)

        assertEquals(ToolType.Compress, select.tool)
        assertEquals(listOf("u1", "u2"), files.uris)
        assertEquals("secret123", pwd.password)
        assertEquals("DRAFT", wm.text)
        assertEquals(90, rot.degrees)

        assertNotNull(ToolsAction.DismissDialogs)
        assertNotNull(ToolsAction.DismissResult)
        assertNotNull(ToolsAction.DismissError)
        assertNotNull(ToolsAction.BackClicked)
    }

    @Test
    fun toolsEvents_instantiateCorrectly() {
        val picker = ToolsEvent.LaunchFilePicker(ToolType.Merge, true, "application/pdf")
        val nav = ToolsEvent.NavigateToViewer("file:///out.pdf")

        assertEquals(ToolType.Merge, picker.tool)
        assertEquals(true, picker.isMultiple)
        assertEquals("application/pdf", picker.mimeType)
        assertEquals("file:///out.pdf", nav.uri)
        assertNotNull(ToolsEvent.NavigateBack)
    }
}
