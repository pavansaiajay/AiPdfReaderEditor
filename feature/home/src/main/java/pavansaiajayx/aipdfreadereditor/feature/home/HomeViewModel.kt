package pavansaiajayx.aipdfreadereditor.feature.home

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
import kotlinx.coroutines.launch
import pavansaiajayx.aipdfreadereditor.core.database.dao.DocumentDao
import pavansaiajayx.aipdfreadereditor.core.database.model.DocumentEntity
import pavansaiajayx.aipdfreadereditor.core.datastore.economy.CreditManager
import javax.inject.Inject

data class HomeUiState(
    val credits: Int = 0,
    val searchQuery: String = "",
    val recentDocuments: List<DocumentEntity> = emptyList(),
    val isLoading: Boolean = false,
    val showInsufficientCreditsDialog: Boolean = false
)

sealed interface HomeAction {
    data class SearchQueryChanged(val query: String) : HomeAction
    data class DocumentClicked(val uri: String) : HomeAction
    data class DeleteDocument(val document: DocumentEntity) : HomeAction
    data class OpenPdfUri(val uri: String, val fileName: String) : HomeAction
    data object OpenToolsClicked : HomeAction
    data object EarnCreditsClicked : HomeAction
    data object DismissCreditsDialog : HomeAction
}

sealed interface HomeEvent {
    data class NavigateToViewer(val uri: String) : HomeEvent
    data object NavigateToTools : HomeEvent
    data object LaunchFilePicker : HomeEvent
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val creditManager: CreditManager,
    private val documentDao: DocumentDao
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _isLoading = MutableStateFlow(false)
    private val _showCreditsDialog = MutableStateFlow(false)

    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    val events: Flow<HomeEvent> = _events.receiveAsFlow()

    val uiState: StateFlow<HomeUiState> = combine(
        creditManager.creditsFlow,
        documentDao.getAllDocuments(),
        _searchQuery,
        _isLoading,
        _showCreditsDialog
    ) { credits, documents, query, isLoading, showCreditsDialog ->
        val filteredDocuments = if (query.isBlank()) {
            documents
        } else {
            documents.filter { it.fileName.contains(query, ignoreCase = true) }
        }
        HomeUiState(
            credits = credits,
            searchQuery = query,
            recentDocuments = filteredDocuments,
            isLoading = isLoading,
            showInsufficientCreditsDialog = showCreditsDialog
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    fun onAction(action: HomeAction) {
        when (action) {
            is HomeAction.SearchQueryChanged -> {
                _searchQuery.value = action.query
            }
            is HomeAction.DocumentClicked -> {
                viewModelScope.launch {
                    val existing = documentDao.getDocumentByUri(action.uri)
                    if (existing != null) {
                        documentDao.updateLastAccessed(existing.id, System.currentTimeMillis())
                    }
                    _events.send(HomeEvent.NavigateToViewer(action.uri))
                }
            }
            is HomeAction.DeleteDocument -> {
                viewModelScope.launch {
                    documentDao.delete(action.document)
                }
            }
            is HomeAction.OpenPdfUri -> {
                viewModelScope.launch {
                    val existing = documentDao.getDocumentByUri(action.uri)
                    if (existing != null) {
                        documentDao.updateLastAccessed(existing.id, System.currentTimeMillis())
                    } else {
                        documentDao.insert(
                            DocumentEntity(
                                fileName = action.fileName,
                                uri = action.uri,
                                lastAccessedAt = System.currentTimeMillis()
                            )
                        )
                    }
                    _events.send(HomeEvent.NavigateToViewer(action.uri))
                }
            }
            is HomeAction.OpenToolsClicked -> {
                viewModelScope.launch {
                    _events.send(HomeEvent.NavigateToTools)
                }
            }
            is HomeAction.EarnCreditsClicked -> {
                _showCreditsDialog.value = true
            }
            is HomeAction.DismissCreditsDialog -> {
                _showCreditsDialog.value = false
            }
        }
    }
}
