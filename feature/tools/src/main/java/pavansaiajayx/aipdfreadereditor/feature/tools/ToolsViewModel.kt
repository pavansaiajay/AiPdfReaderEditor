package pavansaiajayx.aipdfreadereditor.feature.tools

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pavansaiajayx.aipdfreadereditor.core.database.dao.DocumentDao
import pavansaiajayx.aipdfreadereditor.core.database.model.DocumentEntity
import pavansaiajayx.aipdfreadereditor.core.pdf.engine.PdfEngine
import java.io.File
import java.util.UUID
import javax.inject.Inject

enum class ToolType {
    Merge,
    Split,
    Compress,
    ImagesToPdf,
    PdfToImages,
    Encrypt,
    Decrypt,
    Watermark,
    ExtractText,
    Rotate
}

enum class ToolCategory {
    Organize,
    Convert,
    Security
}

data class ToolDefinition(
    val type: ToolType,
    @StringRes val titleRes: Int,
    @StringRes val descRes: Int,
    val category: ToolCategory,
    val requiresMultiple: Boolean = false,
    val mimeType: String = "application/pdf"
)

data class ToolsUiState(
    val isProcessing: Boolean = false,
    val progress: Int = 0,
    val activeTool: ToolType? = null,
    val selectedUris: List<String> = emptyList(),
    val resultMessage: String? = null,
    val resultUri: String? = null,
    val errorMessage: String? = null,
    val showPasswordDialog: Boolean = false,
    val showWatermarkDialog: Boolean = false,
    val showRotationDialog: Boolean = false
)

sealed interface ToolsAction {
    data class SelectTool(val tool: ToolType) : ToolsAction
    data class FilesSelected(val uris: List<String>) : ToolsAction
    data class PasswordSubmitted(val password: String) : ToolsAction
    data class WatermarkSubmitted(val text: String) : ToolsAction
    data class RotationSubmitted(val degrees: Int) : ToolsAction
    data object DismissDialogs : ToolsAction
    data object DismissResult : ToolsAction
    data object DismissError : ToolsAction
    data object BackClicked : ToolsAction
}

sealed interface ToolsEvent {
    data object NavigateBack : ToolsEvent
    data class LaunchFilePicker(val tool: ToolType, val isMultiple: Boolean, val mimeType: String) : ToolsEvent
    data class NavigateToViewer(val uri: String) : ToolsEvent
}

@HiltViewModel
class ToolsViewModel @Inject constructor(
    private val pdfEngine: PdfEngine,
    private val documentDao: DocumentDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ToolsUiState())
    val uiState: StateFlow<ToolsUiState> = _uiState.asStateFlow()

    private val _events = Channel<ToolsEvent>(Channel.BUFFERED)
    val events: Flow<ToolsEvent> = _events.receiveAsFlow()

    fun onAction(action: ToolsAction) {
        when (action) {
            is ToolsAction.SelectTool -> handleToolSelected(action.tool)
            is ToolsAction.FilesSelected -> handleFilesSelected(action.uris)
            is ToolsAction.PasswordSubmitted -> handlePasswordSubmitted(action.password)
            is ToolsAction.WatermarkSubmitted -> handleWatermarkSubmitted(action.text)
            is ToolsAction.RotationSubmitted -> handleRotationSubmitted(action.degrees)
            is ToolsAction.DismissDialogs -> dismissAllDialogs()
            is ToolsAction.DismissResult -> _uiState.update { it.copy(resultMessage = null, resultUri = null) }
            is ToolsAction.DismissError -> _uiState.update { it.copy(errorMessage = null) }
            is ToolsAction.BackClicked -> {
                viewModelScope.launch {
                    _events.send(ToolsEvent.NavigateBack)
                }
            }
        }
    }

    private fun handleToolSelected(tool: ToolType) {
        _uiState.update { it.copy(activeTool = tool, errorMessage = null, resultMessage = null, resultUri = null) }
        val isMultiple = tool == ToolType.Merge || tool == ToolType.ImagesToPdf
        val mimeType = if (tool == ToolType.ImagesToPdf) "image/*" else "application/pdf"
        viewModelScope.launch {
            _events.send(ToolsEvent.LaunchFilePicker(tool = tool, isMultiple = isMultiple, mimeType = mimeType))
        }
    }

    private fun handleFilesSelected(uris: List<String>) {
        if (uris.isEmpty()) return
        _uiState.update { it.copy(selectedUris = uris) }

        val activeTool = _uiState.value.activeTool ?: return
        when (activeTool) {
            ToolType.Encrypt, ToolType.Decrypt -> {
                _uiState.update { it.copy(showPasswordDialog = true) }
            }
            ToolType.Watermark -> {
                _uiState.update { it.copy(showWatermarkDialog = true) }
            }
            ToolType.Rotate -> {
                _uiState.update { it.copy(showRotationDialog = true) }
            }
            ToolType.Merge -> executeMerge(uris)
            ToolType.Split -> executeSplit(uris.first())
            ToolType.Compress -> executeCompress(uris.first())
            ToolType.ImagesToPdf -> executeImagesToPdf(uris)
            ToolType.PdfToImages -> executePdfToImages(uris.first())
            ToolType.ExtractText -> executeExtractText(uris.first())
        }
    }

    private fun handlePasswordSubmitted(password: String) {
        val tool = _uiState.value.activeTool ?: return
        val uriStr = _uiState.value.selectedUris.firstOrNull() ?: return
        dismissAllDialogs()
        if (tool == ToolType.Encrypt) {
            executeEncrypt(uriStr, password)
        } else if (tool == ToolType.Decrypt) {
            executeDecrypt(uriStr, password)
        }
    }

    private fun handleWatermarkSubmitted(text: String) {
        val uriStr = _uiState.value.selectedUris.firstOrNull() ?: return
        dismissAllDialogs()
        executeWatermark(uriStr, text)
    }

    private fun handleRotationSubmitted(degrees: Int) {
        val uriStr = _uiState.value.selectedUris.firstOrNull() ?: return
        dismissAllDialogs()
        executeRotate(uriStr, degrees)
    }

    private fun dismissAllDialogs() {
        _uiState.update {
            it.copy(
                showPasswordDialog = false,
                showWatermarkDialog = false,
                showRotationDialog = false
            )
        }
    }

    private fun executeMerge(uriStrings: List<String>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, progress = 0) }
            val outputFile = File(pdfEngine.cacheDir, "merged_${System.currentTimeMillis()}.pdf")
            val result = pdfEngine.mergePdfUris(uriStrings, outputFile) { progress ->
                _uiState.update { it.copy(progress = progress) }
            }
            handleFileResult(result, "PDFs merged successfully")
        }
    }

    private fun executeSplit(uriString: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, progress = 50) }
            val outputFolder = File(pdfEngine.cacheDir, "split_${System.currentTimeMillis()}")
            val result = pdfEngine.splitPdfUri(uriString, outputFolder)
            _uiState.update { it.copy(isProcessing = false) }
            result.onSuccess { files ->
                val count = files.size
                _uiState.update {
                    it.copy(
                        resultMessage = "Successfully split into $count pages",
                        resultUri = files.firstOrNull()?.toURI()?.toString()
                    )
                }
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.message ?: "Failed to split PDF") }
            }
        }
    }

    private fun executeCompress(uriString: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, progress = 50) }
            val outputFile = File(pdfEngine.cacheDir, "compressed_${System.currentTimeMillis()}.pdf")
            val result = pdfEngine.compressPdfUri(uriString, outputFile)
            handleFileResult(result, "PDF compressed successfully")
        }
    }

    private fun executeImagesToPdf(uriStrings: List<String>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, progress = 50) }
            val outputFile = File(pdfEngine.cacheDir, "images_converted_${System.currentTimeMillis()}.pdf")
            val result = pdfEngine.imagesToPdfUris(uriStrings, outputFile)
            handleFileResult(result, "Images converted to PDF successfully")
        }
    }

    private fun executePdfToImages(uriString: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, progress = 50) }
            val outputFolder = File(pdfEngine.cacheDir, "pages_${System.currentTimeMillis()}")
            val result = pdfEngine.pdfToImagesUri(uriString, outputFolder)
            _uiState.update { it.copy(isProcessing = false) }
            result.onSuccess { files ->
                _uiState.update {
                    it.copy(
                        resultMessage = "Exported ${files.size} pages as images",
                        resultUri = files.firstOrNull()?.toURI()?.toString()
                    )
                }
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.message ?: "Failed to export images") }
            }
        }
    }

    private fun executeEncrypt(uriString: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, progress = 50) }
            val outputFile = File(pdfEngine.cacheDir, "protected_${System.currentTimeMillis()}.pdf")
            val result = pdfEngine.encryptPdfUri(uriString, password, outputFile)
            handleFileResult(result, "PDF protected with password", isEncrypted = true)
        }
    }

    private fun executeDecrypt(uriString: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, progress = 50) }
            val outputFile = File(pdfEngine.cacheDir, "unlocked_${System.currentTimeMillis()}.pdf")
            val result = pdfEngine.decryptPdfUri(uriString, password, outputFile)
            handleFileResult(result, "Password removed successfully")
        }
    }

    private fun executeWatermark(uriString: String, text: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, progress = 50) }
            val outputFile = File(pdfEngine.cacheDir, "watermarked_${System.currentTimeMillis()}.pdf")
            val result = pdfEngine.addWatermarkUri(uriString, text, outputFile)
            handleFileResult(result, "Watermark applied successfully")
        }
    }

    private fun executeRotate(uriString: String, degrees: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, progress = 50) }
            val outputFile = File(pdfEngine.cacheDir, "rotated_${System.currentTimeMillis()}.pdf")
            val result = pdfEngine.rotatePagesUri(uriString, degrees, outputFile)
            handleFileResult(result, "Pages rotated successfully")
        }
    }

    private fun executeExtractText(uriString: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, progress = 50) }
            val outputFile = File(pdfEngine.cacheDir, "extracted_${System.currentTimeMillis()}.txt")
            val result = pdfEngine.extractTextUri(uriString, outputFile)
            _uiState.update { it.copy(isProcessing = false) }
            result.onSuccess { file ->
                val uri = file.toURI().toString()
                _uiState.update {
                    it.copy(
                        resultMessage = "Text extracted successfully (${file.length()} bytes)",
                        resultUri = uri
                    )
                }
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.message ?: "Failed to extract text") }
            }
        }
    }

    private suspend fun handleFileResult(
        result: kotlin.Result<File>,
        successMessage: String,
        isEncrypted: Boolean = false
    ) {
        _uiState.update { it.copy(isProcessing = false) }
        result.onSuccess { file ->
            val uri = file.toURI().toString()
            documentDao.insert(
                DocumentEntity(
                    fileName = file.name,
                    uri = uri,
                    fileSizeBytes = file.length(),
                    isEncrypted = isEncrypted
                )
            )
            _uiState.update {
                it.copy(
                    resultMessage = successMessage,
                    resultUri = uri
                )
            }
        }.onFailure { error ->
            _uiState.update {
                it.copy(errorMessage = error.message ?: "Operation failed")
            }
        }
    }
}
