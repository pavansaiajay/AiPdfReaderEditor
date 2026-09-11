package pavansaiajayx.aipdfreadereditor.feature.viewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pavansaiajayx.aipdfreadereditor.core.pdf.renderer.PdfRendererPool
import javax.inject.Inject

data class ViewerUiState(
    val documentUri: String = "",
    val fileName: String = "",
    val currentPage: Int = 0,
    val pageCount: Int = 0,
    val zoomLevel: Float = 1.0f,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed interface ViewerAction {
    data class LoadDocument(val uri: String, val fileName: String = "", val pageCount: Int = 0) : ViewerAction
    data class PageChanged(val page: Int) : ViewerAction
    data class ZoomChanged(val zoom: Float) : ViewerAction
    data object ResetZoom : ViewerAction
    data object BackClicked : ViewerAction
    data object ChatClicked : ViewerAction
    data object ToolsClicked : ViewerAction
    data object DismissError : ViewerAction
}

sealed interface ViewerEvent {
    data object NavigateBack : ViewerEvent
    data class NavigateToChat(val documentUri: String) : ViewerEvent
    data object NavigateToTools : ViewerEvent
    data class ScrollToPage(val page: Int) : ViewerEvent
}

@HiltViewModel
class ViewerViewModel @Inject constructor(
    val pdfRendererPool: PdfRendererPool
) : ViewModel() {

    private val _uiState = MutableStateFlow(ViewerUiState())
    val uiState: StateFlow<ViewerUiState> = _uiState.asStateFlow()

    private val _events = Channel<ViewerEvent>(Channel.BUFFERED)
    val events: Flow<ViewerEvent> = _events.receiveAsFlow()

    fun onAction(action: ViewerAction) {
        when (action) {
            is ViewerAction.LoadDocument -> {
                _uiState.update {
                    it.copy(
                        documentUri = action.uri,
                        fileName = action.fileName,
                        pageCount = action.pageCount,
                        isLoading = false
                    )
                }
            }
            is ViewerAction.PageChanged -> {
                _uiState.update { it.copy(currentPage = action.page) }
            }
            is ViewerAction.ZoomChanged -> {
                val clamped = action.zoom.coerceIn(1.0f, 5.0f)
                _uiState.update { it.copy(zoomLevel = clamped) }
            }
            is ViewerAction.ResetZoom -> {
                _uiState.update { it.copy(zoomLevel = 1.0f) }
            }
            is ViewerAction.BackClicked -> {
                viewModelScope.launch {
                    _events.send(ViewerEvent.NavigateBack)
                }
            }
            is ViewerAction.ChatClicked -> {
                viewModelScope.launch {
                    _events.send(ViewerEvent.NavigateToChat(_uiState.value.documentUri))
                }
            }
            is ViewerAction.ToolsClicked -> {
                viewModelScope.launch {
                    _events.send(ViewerEvent.NavigateToTools)
                }
            }
            is ViewerAction.DismissError -> {
                _uiState.update { it.copy(errorMessage = null) }
            }
        }
    }
}
