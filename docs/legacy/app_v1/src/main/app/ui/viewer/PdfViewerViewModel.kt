package pavansaiajayx.aipdfreadereditor.app.ui.viewer

import android.app.Activity
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import pavansaiajayx.aipdfreadereditor.app.core.pdf.PdfEdit
import pavansaiajayx.aipdfreadereditor.app.core.pdf.PdfEngine
import pavansaiajayx.aipdfreadereditor.app.data.local.DocumentDao
import pavansaiajayx.aipdfreadereditor.app.data.local.DocumentEntity
import java.io.File
import javax.inject.Inject

enum class EditTool { None, Pen, Highlight, Text, Eraser, Signature }

data class PdfViewerState(
    val edits: List<PdfEdit> = emptyList(),
    val currentTool: EditTool = EditTool.None,
    val currentColor: Color = Color.Red,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null,
    val searchResults: List<Int>? = null,
    val aiSummary: String? = null,
    val isAiProcessing: Boolean = false,
    val showInsufficientCreditsDialog: Boolean = false
)

@HiltViewModel
class PdfViewerViewModel @Inject constructor(
    private val pdfEngine: PdfEngine,
    private val creditManager: CreditManager,
    private val aiEngine: AiEngine,
    private val adManager: AdManager,
    private val documentDao: DocumentDao
) : ViewModel() {

    private val _state = MutableStateFlow(PdfViewerState())
    val state: StateFlow<PdfViewerState> = _state.asStateFlow()

    fun setTool(tool: EditTool) {
        _state.update { it.copy(currentTool = tool) }
    }

    fun setColor(color: Color) {
        _state.update { it.copy(currentColor = color) }
    }

    fun addEdit(edit: PdfEdit) {
        _state.update { it.copy(edits = it.edits + edit) }
    }

    fun removeEdit(edit: PdfEdit) {
        _state.update { it.copy(edits = it.edits - edit) }
    }

    fun clearEdits() {
        _state.update { it.copy(edits = emptyList()) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun resetSaveSuccess() {
        _state.update { it.copy(saveSuccess = false) }
    }

    fun saveEdits(fileUri: String, pageIndex: Int = 0) {
        viewModelScope.launch {
            if (_state.value.edits.isEmpty()) return@launch
            
            _state.update { it.copy(isSaving = true, error = null, saveSuccess = false) }

            try {
                val uri = Uri.parse(fileUri)
                val outputFile = File(pdfEngine.cacheDir, "annotated_${System.currentTimeMillis()}.pdf")
                
                val result = pdfEngine.applyAnnotations(uri, _state.value.edits, pageIndex, outputFile)
                result.fold(
                    onSuccess = { file ->
                        pdfEngine.copyToUri(file, uri)
                        val fileName = uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
                            ?: "annotated_${System.currentTimeMillis()}.pdf"
                        documentDao.insert(
                            DocumentEntity(
                                fileName = fileName,
                                uri = fileUri,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                        _state.update { it.copy(isSaving = false, saveSuccess = true, edits = emptyList(), currentTool = EditTool.None) }
                    },
                    onFailure = { err ->
                        _state.update { it.copy(isSaving = false, error = err.message ?: "Failed to save") }
                    }
                )
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, error = e.message ?: "Unknown error") }
            }
        }
    }

    fun searchInPdf(uri: Uri, query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                _state.update { it.copy(searchResults = null) }
                return@launch
            }
            try {
                val result = pdfEngine.searchInPdf(uri, query)
                result.fold(
                    onSuccess = { matches ->
                        _state.update { it.copy(searchResults = matches) }
                    },
                    onFailure = { err ->
                        _state.update { it.copy(error = err.message ?: "Failed to search") }
                    }
                )
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Unknown error") }
            }
        }
    }

    fun clearSearchResults() {
        _state.update { it.copy(searchResults = null) }
    }

    fun summarizePdf(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isAiProcessing = true, error = null, aiSummary = null) }
            try {
                val currentCredits = creditManager.creditsFlow.first()
                android.util.Log.d("AiViewModel_Debug", "Current credits: $currentCredits")
                if (currentCredits < 5) {
                    _state.update { it.copy(isAiProcessing = false, showInsufficientCreditsDialog = true) }
                    return@launch
                }

                val textResult = pdfEngine.extractTextToString(uri)
                android.util.Log.d("AiViewModel_Debug", "Text extraction success: ${textResult.isSuccess}")
                textResult.fold(
                    onSuccess = { extractedText ->
                        val summaryResult = aiEngine.generateSummary(extractedText)
                        summaryResult.fold(
                            onSuccess = { summary ->
                                creditManager.deductCredits(5)
                                _state.update { it.copy(isAiProcessing = false, aiSummary = summary) }
                            },
                            onFailure = { err ->
                                android.util.Log.e("AiViewModel_Debug", "Summarization failed in ViewModel: ${err.message}", err)
                                err.printStackTrace()
                                _state.update { it.copy(isAiProcessing = false, error = err.message ?: "Failed to generate summary") }
                            }
                        )
                    },
                    onFailure = { err ->
                        _state.update { it.copy(isAiProcessing = false, error = err.message ?: "Failed to extract text") }
                    }
                )
            } catch (e: Exception) {
                _state.update { it.copy(isAiProcessing = false, error = e.message ?: "Unknown error") }
            }
        }
    }

    fun clearAiSummary() {
        _state.update { it.copy(aiSummary = null) }
    }

    fun dismissDialog() {
        _state.update { it.copy(showInsufficientCreditsDialog = false) }
    }

    fun watchAdForCredits(activity: Activity) {
        val shown = adManager.showRewardedAd(activity) {
            viewModelScope.launch {
                creditManager.addCredits(5)
                // Reset state back to Idle so the dialog dismisses
                _state.update { it.copy(showInsufficientCreditsDialog = false) }
            }
        }
        if (!shown) {
            _state.update { it.copy(error = "No ads available right now. Please try again later.", showInsufficientCreditsDialog = false) }
        }
        adManager.loadRewardedAd {}
    }

    init {
        adManager.loadRewardedAd {}
    }
}
