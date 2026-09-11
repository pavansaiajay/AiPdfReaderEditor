package pavansaiajayx.aipdfreadereditor.feature.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import pavansaiajayx.aipdfreadereditor.core.database.model.DocumentEntity

class HomeScreenTest {

    @Test
    fun homeUiState_defaultsAreValid() {
        val state = HomeUiState()
        assertEquals(0, state.credits)
        assertEquals("", state.searchQuery)
        assertEquals(0, state.recentDocuments.size)
        assertEquals(false, state.isLoading)
        assertEquals(false, state.showInsufficientCreditsDialog)
    }

    @Test
    fun homeUiState_copyPreservesInvariants() {
        val doc = DocumentEntity(fileName = "doc.pdf", uri = "content://doc/1")
        val state = HomeUiState(
            credits = 15,
            searchQuery = "test",
            recentDocuments = listOf(doc),
            isLoading = true,
            showInsufficientCreditsDialog = true
        )
        assertEquals(15, state.credits)
        assertEquals("test", state.searchQuery)
        assertEquals(1, state.recentDocuments.size)
        assertEquals(true, state.isLoading)
        assertEquals(true, state.showInsufficientCreditsDialog)
    }

    @Test
    fun homeActions_instantiateCorrectly() {
        val doc = DocumentEntity(fileName = "doc.pdf", uri = "content://doc/1")
        val search = HomeAction.SearchQueryChanged("q")
        val click = HomeAction.DocumentClicked("uri")
        val delete = HomeAction.DeleteDocument(doc)
        val open = HomeAction.OpenPdfUri("uri", "doc.pdf")

        assertEquals("q", search.query)
        assertEquals("uri", click.uri)
        assertEquals(doc, delete.document)
        assertEquals("uri", open.uri)
        assertEquals("doc.pdf", open.fileName)

        assertNotNull(HomeAction.OpenToolsClicked)
        assertNotNull(HomeAction.EarnCreditsClicked)
        assertNotNull(HomeAction.DismissCreditsDialog)
    }

    @Test
    fun homeEvents_instantiateCorrectly() {
        val viewerEvent = HomeEvent.NavigateToViewer("uri")
        assertEquals("uri", viewerEvent.uri)
        assertNotNull(HomeEvent.NavigateToTools)
        assertNotNull(HomeEvent.LaunchFilePicker)
    }
}
