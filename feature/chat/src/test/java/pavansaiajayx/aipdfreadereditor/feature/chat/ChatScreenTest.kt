package pavansaiajayx.aipdfreadereditor.feature.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import pavansaiajayx.aipdfreadereditor.core.model.AiMessage
import pavansaiajayx.aipdfreadereditor.core.model.AiRole

class ChatScreenTest {

    @Test
    fun chatUiState_defaultsAreValid() {
        val state = ChatUiState()
        assertEquals("", state.documentUri)
        assertEquals("", state.fileName)
        assertEquals(0, state.messages.size)
        assertEquals(0, state.credits)
        assertEquals("", state.inputQuery)
        assertEquals(false, state.isAiResponding)
        assertEquals(false, state.showInsufficientCreditsDialog)
        assertNull(state.errorMessage)
    }

    @Test
    fun chatUiState_copyPreservesInvariants() {
        val message = AiMessage(
            id = "m1",
            role = AiRole.User,
            content = "Summarize page 1"
        )
        val state = ChatUiState(
            documentUri = "content://pdf/1",
            fileName = "contract.pdf",
            messages = listOf(message),
            credits = 10,
            inputQuery = "What is the date?",
            isAiResponding = true,
            showInsufficientCreditsDialog = false,
            errorMessage = "Sample error"
        )

        assertEquals("content://pdf/1", state.documentUri)
        assertEquals("contract.pdf", state.fileName)
        assertEquals(1, state.messages.size)
        assertEquals("Summarize page 1", state.messages.first().content)
        assertEquals(10, state.credits)
        assertEquals("What is the date?", state.inputQuery)
        assertEquals(true, state.isAiResponding)
        assertEquals(false, state.showInsufficientCreditsDialog)
        assertEquals("Sample error", state.errorMessage)
    }

    @Test
    fun chatActions_instantiateCorrectly() {
        val load = ChatAction.LoadDocument("uri", "doc.pdf")
        val input = ChatAction.InputQueryChanged("test query")
        val prompt = ChatAction.QuickPromptSelected("Summarize Key Points")

        assertEquals("uri", load.uri)
        assertEquals("doc.pdf", load.fileName)
        assertEquals("test query", input.query)
        assertEquals("Summarize Key Points", prompt.prompt)

        assertNotNull(ChatAction.SendMessage)
        assertNotNull(ChatAction.BackClicked)
        assertNotNull(ChatAction.EarnCreditsClicked)
        assertNotNull(ChatAction.DismissCreditsDialog)
        assertNotNull(ChatAction.DismissError)
    }

    @Test
    fun chatEvents_instantiateCorrectly() {
        val toast = ChatEvent.ShowToast("Copied")
        assertEquals("Copied", toast.message)
        assertNotNull(ChatEvent.NavigateBack)
        assertNotNull(ChatEvent.ScrollToBottom)
    }
}
