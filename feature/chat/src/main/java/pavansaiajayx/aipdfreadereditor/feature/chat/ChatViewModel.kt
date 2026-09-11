package pavansaiajayx.aipdfreadereditor.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pavansaiajayx.aipdfreadereditor.core.ai.AiEngine
import pavansaiajayx.aipdfreadereditor.core.common.result.Result
import pavansaiajayx.aipdfreadereditor.core.datastore.economy.CreditManager
import pavansaiajayx.aipdfreadereditor.core.model.AiMessage
import pavansaiajayx.aipdfreadereditor.core.model.AiRole
import java.util.UUID
import javax.inject.Inject

data class ChatUiState(
    val documentUri: String = "",
    val fileName: String = "",
    val messages: List<AiMessage> = emptyList(),
    val credits: Int = 0,
    val inputQuery: String = "",
    val isAiResponding: Boolean = false,
    val showInsufficientCreditsDialog: Boolean = false,
    val errorMessage: String? = null
)

sealed interface ChatAction {
    data class LoadDocument(val uri: String, val fileName: String = "") : ChatAction
    data class InputQueryChanged(val query: String) : ChatAction
    data object SendMessage : ChatAction
    data class QuickPromptSelected(val prompt: String) : ChatAction
    data object BackClicked : ChatAction
    data object EarnCreditsClicked : ChatAction
    data object DismissCreditsDialog : ChatAction
    data object DismissError : ChatAction
}

sealed interface ChatEvent {
    data object NavigateBack : ChatEvent
    data class ShowToast(val message: String) : ChatEvent
    data object ScrollToBottom : ChatEvent
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val aiEngine: AiEngine,
    private val creditManager: CreditManager
) : ViewModel() {

    private val _documentUri = MutableStateFlow("")
    private val _fileName = MutableStateFlow("")
    private val _messages = MutableStateFlow<List<AiMessage>>(emptyList())
    private val _inputQuery = MutableStateFlow("")
    private val _isAiResponding = MutableStateFlow(false)
    private val _showCreditsDialog = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    private val _events = Channel<ChatEvent>(Channel.BUFFERED)
    val events: Flow<ChatEvent> = _events.receiveAsFlow()

    val uiState: StateFlow<ChatUiState> = combine(
        creditManager.creditsFlow,
        _documentUri,
        _fileName,
        _messages,
        _inputQuery,
        _isAiResponding,
        _showCreditsDialog,
        _errorMessage
    ) { args: Array<Any?> ->
        val credits = args[0] as Int
        val uri = args[1] as String
        val name = args[2] as String
        @Suppress("UNCHECKED_CAST")
        val msgs = args[3] as List<AiMessage>
        val query = args[4] as String
        val isResponding = args[5] as Boolean
        val showDialog = args[6] as Boolean
        val error = args[7] as String?

        ChatUiState(
            credits = credits,
            documentUri = uri,
            fileName = name,
            messages = msgs,
            inputQuery = query,
            isAiResponding = isResponding,
            showInsufficientCreditsDialog = showDialog,
            errorMessage = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ChatUiState()
    )

    fun onAction(action: ChatAction) {
        when (action) {
            is ChatAction.LoadDocument -> {
                _documentUri.value = action.uri
                _fileName.value = action.fileName
            }
            is ChatAction.InputQueryChanged -> {
                _inputQuery.value = action.query
            }
            is ChatAction.SendMessage -> {
                sendQuery(_inputQuery.value)
            }
            is ChatAction.QuickPromptSelected -> {
                sendQuery(action.prompt)
            }
            is ChatAction.BackClicked -> {
                viewModelScope.launch {
                    _events.send(ChatEvent.NavigateBack)
                }
            }
            is ChatAction.EarnCreditsClicked -> {
                _showCreditsDialog.value = true
            }
            is ChatAction.DismissCreditsDialog -> {
                _showCreditsDialog.value = false
            }
            is ChatAction.DismissError -> {
                _errorMessage.value = null
            }
        }
    }

    private fun sendQuery(queryText: String) {
        if (queryText.isBlank() || _isAiResponding.value) return

        val currentBalance = uiState.value.credits
        if (currentBalance < 1) {
            _showCreditsDialog.value = true
            return
        }

        val userMessage = AiMessage(
            id = UUID.randomUUID().toString(),
            role = AiRole.User,
            content = queryText
        )
        _messages.update { it + userMessage }
        _inputQuery.value = ""
        _isAiResponding.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            val result = aiEngine.sendChatMessage(
                systemContext = "Document: ${_fileName.value}",
                userMessage = queryText
            )
            _isAiResponding.value = false
            when (result) {
                is Result.Success -> {
                    _messages.update { it + result.data }
                    _events.send(ChatEvent.ScrollToBottom)
                }
                is Result.Error -> {
                    _errorMessage.value = result.cause?.message ?: "Failed to generate AI response"
                }
                is Result.Loading -> {
                    // Progress tracked via _isAiResponding
                }
            }
        }
    }
}
