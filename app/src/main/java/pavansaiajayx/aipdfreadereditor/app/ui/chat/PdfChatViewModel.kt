package pavansaiajayx.aipdfreadereditor.app.ui.chat

import android.app.Activity
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.ai.Chat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pavansaiajayx.aipdfreadereditor.app.core.ads.AdManager
import pavansaiajayx.aipdfreadereditor.app.core.ai.AiEngine
import pavansaiajayx.aipdfreadereditor.app.core.economy.CreditManager
import pavansaiajayx.aipdfreadereditor.app.core.pdf.PdfEngine
import javax.inject.Inject

data class ChatMessage(val sender: String, val text: String)

data class PdfChatState(
    val chatHistory: List<ChatMessage> = emptyList(),
    val isExtractingText: Boolean = false,
    val isAiTyping: Boolean = false,
    val error: String? = null,
    val showInsufficientCreditsDialog: Boolean = false
)

@HiltViewModel
class PdfChatViewModel @Inject constructor(
    private val aiEngine: AiEngine,
    private val pdfEngine: PdfEngine,
    private val creditManager: CreditManager,
    private val adManager: AdManager
) : ViewModel() {
    private val _state = MutableStateFlow(PdfChatState())
    val state: StateFlow<PdfChatState> = _state.asStateFlow()

    private var chatSession: Chat? = null

    init {
        adManager.loadRewardedAd {}
    }

    fun initializeChat(uri: Uri) {
        if (chatSession != null) return
        viewModelScope.launch {
            _state.update { it.copy(isExtractingText = true, error = null) }
            val textResult = pdfEngine.extractTextToString(uri)
            textResult.fold(
                onSuccess = { text ->
                    chatSession = aiEngine.startChatSession(text)
                    _state.update { it.copy(isExtractingText = false) }
                },
                onFailure = { err ->
                    _state.update { it.copy(isExtractingText = false, error = err.message ?: "Failed to extract text") }
                }
            )
        }
    }

    fun sendMessage(text: String) {
        val currentChat = chatSession ?: return
        viewModelScope.launch {
            val credits = creditManager.creditsFlow.first()
            if (credits < 1) {
                _state.update { it.copy(showInsufficientCreditsDialog = true) }
                return@launch
            }
            
            _state.update { it.copy(
                chatHistory = it.chatHistory + ChatMessage("user", text),
                isAiTyping = true,
                error = null
            ) }

            val result = aiEngine.sendMessage(currentChat, text)
            result.fold(
                onSuccess = { response ->
                    creditManager.deductCredits(1)
                    _state.update { it.copy(
                        chatHistory = it.chatHistory + ChatMessage("ai", response),
                        isAiTyping = false
                    ) }
                },
                onFailure = { err ->
                    _state.update { it.copy(isAiTyping = false, error = err.message ?: "Failed to send message") }
                }
            )
        }
    }

    fun dismissInsufficientCreditsDialog() {
        _state.update { it.copy(showInsufficientCreditsDialog = false) }
    }

    fun watchAdForCredits(activity: Activity) {
        val shown = adManager.showRewardedAd(activity) {
            viewModelScope.launch {
                creditManager.addCredits(5)
                _state.update { it.copy(showInsufficientCreditsDialog = false) }
            }
        }
        if (!shown) {
            _state.update { it.copy(error = "No ads available right now. Please try again later.", showInsufficientCreditsDialog = false) }
        }
        adManager.loadRewardedAd {}
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
