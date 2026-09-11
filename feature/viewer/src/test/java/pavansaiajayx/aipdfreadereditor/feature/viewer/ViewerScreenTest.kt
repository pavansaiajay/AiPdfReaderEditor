package pavansaiajayx.aipdfreadereditor.feature.viewer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ViewerScreenTest {

    @Test
    fun viewerUiState_defaultsAreValid() {
        val state = ViewerUiState()
        assertEquals("", state.documentUri)
        assertEquals("", state.fileName)
        assertEquals(0, state.currentPage)
        assertEquals(0, state.pageCount)
        assertEquals(1.0f, state.zoomLevel, 0.001f)
        assertEquals(false, state.isLoading)
        assertEquals(null, state.errorMessage)
    }

    @Test
    fun viewerUiState_copyPreservesInvariants() {
        val state = ViewerUiState(
            documentUri = "content://doc/1",
            fileName = "sample.pdf",
            currentPage = 3,
            pageCount = 10,
            zoomLevel = 2.0f,
            isLoading = true
        )
        assertEquals("content://doc/1", state.documentUri)
        assertEquals("sample.pdf", state.fileName)
        assertEquals(3, state.currentPage)
        assertEquals(10, state.pageCount)
        assertEquals(2.0f, state.zoomLevel, 0.001f)
        assertEquals(true, state.isLoading)
    }

    @Test
    fun viewerActions_instantiateCorrectly() {
        val load = ViewerAction.LoadDocument("uri", "name", 5)
        val page = ViewerAction.PageChanged(2)
        val zoom = ViewerAction.ZoomChanged(1.5f)

        assertEquals("uri", load.uri)
        assertEquals("name", load.fileName)
        assertEquals(5, load.pageCount)
        assertEquals(2, page.page)
        assertEquals(1.5f, zoom.zoom, 0.001f)

        assertNotNull(ViewerAction.ResetZoom)
        assertNotNull(ViewerAction.BackClicked)
        assertNotNull(ViewerAction.ChatClicked)
        assertNotNull(ViewerAction.ToolsClicked)
        assertNotNull(ViewerAction.DismissError)
    }

    @Test
    fun viewerEvents_instantiateCorrectly() {
        assertNotNull(ViewerEvent.NavigateBack)
        val chatEvent = ViewerEvent.NavigateToChat("uri")
        assertEquals("uri", chatEvent.documentUri)
        assertNotNull(ViewerEvent.NavigateToTools)
        val scrollEvent = ViewerEvent.ScrollToPage(3)
        assertEquals(3, scrollEvent.page)
    }
}
