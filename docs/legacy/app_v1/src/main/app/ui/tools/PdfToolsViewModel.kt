package pavansaiajayx.aipdfreadereditor.app.ui.tools

import android.app.Activity
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import pavansaiajayx.aipdfreadereditor.app.core.ads.AdManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pavansaiajayx.aipdfreadereditor.app.core.analytics.AnalyticsManager
import pavansaiajayx.aipdfreadereditor.app.core.economy.CreditManager
import pavansaiajayx.aipdfreadereditor.app.core.pdf.PdfEngine
import pavansaiajayx.aipdfreadereditor.app.data.local.DocumentDao
import pavansaiajayx.aipdfreadereditor.app.data.local.DocumentEntity
import java.io.File
import javax.inject.Inject

// ── MVI State ──

sealed interface PdfToolsState {
    data object Idle : PdfToolsState
    data object Processing : PdfToolsState
    data class Success(val toolName: String, val outputFiles: List<File>) : PdfToolsState
    data class Error(val message: String) : PdfToolsState
    data object InsufficientCredits : PdfToolsState
}

// ── MVI Intent ──

sealed interface PdfToolsIntent {
    data class MergeClicked(val uris: List<Uri>) : PdfToolsIntent
    data class SplitClicked(val uri: Uri) : PdfToolsIntent
    data class CompressClicked(val uri: Uri) : PdfToolsIntent
    data class SaveFile(val sourceFile: File, val destinationUri: Uri) : PdfToolsIntent
    data class SaveFiles(val sourceFiles: List<File>, val folderUri: Uri) : PdfToolsIntent
    data class EncryptClicked(val uri: Uri, val password: String) : PdfToolsIntent
    data class DecryptClicked(val uri: Uri, val password: String) : PdfToolsIntent
    data class ImagesToPdfClicked(val uris: List<Uri>) : PdfToolsIntent
    data class PdfToImagesClicked(val uri: Uri) : PdfToolsIntent
    data class AddWatermarkClicked(val uri: Uri, val text: String) : PdfToolsIntent
    data class ExtractTextClicked(val uri: Uri) : PdfToolsIntent
    data class OcrImageClicked(val uri: Uri) : PdfToolsIntent
    data class ScanDocumentCompleted(val pdfUri: String) : PdfToolsIntent
    data class DeletePagesClicked(val uri: Uri, val pagesText: String) : PdfToolsIntent
    data class ReorderPagesClicked(val uri: Uri, val orderText: String) : PdfToolsIntent
    data class RotatePdfClicked(val uri: Uri, val rotationDegrees: Int) : PdfToolsIntent
    data class ExtractPageClicked(val uri: Uri, val page: Int) : PdfToolsIntent
    data class FlattenPdfClicked(val uri: Uri) : PdfToolsIntent
    data class HtmlToPdfClicked(val html: String) : PdfToolsIntent
    data class EarnCreditsAdClicked(val activity: Activity) : PdfToolsIntent
    data class RequestReview(val activity: Activity) : PdfToolsIntent
    data object DismissState : PdfToolsIntent
}

// ── ViewModel ──

@HiltViewModel
class PdfToolsViewModel @Inject constructor(
    private val pdfEngine: PdfEngine,
    private val creditManager: CreditManager,
    private val adManager: AdManager,
    private val analyticsManager: AnalyticsManager,
    private val documentDao: DocumentDao,
    private val appReviewManager: pavansaiajayx.aipdfreadereditor.app.core.reviews.AppReviewManager
) : ViewModel() {

    private val _state = MutableStateFlow<PdfToolsState>(PdfToolsState.Idle)
    val state: StateFlow<PdfToolsState> = _state.asStateFlow()

    val credits: StateFlow<Int> = creditManager.creditsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun onIntent(intent: PdfToolsIntent) {
        when (intent) {
            is PdfToolsIntent.MergeClicked -> handleMerge(intent.uris)
            is PdfToolsIntent.SplitClicked -> handleSplit(intent.uri)
            is PdfToolsIntent.CompressClicked -> handleCompress(intent.uri)
            is PdfToolsIntent.EncryptClicked -> handleEncrypt(intent.uri, intent.password)
            is PdfToolsIntent.DecryptClicked -> handleDecrypt(intent.uri, intent.password)
            is PdfToolsIntent.ImagesToPdfClicked -> handleImagesToPdf(intent.uris)
            is PdfToolsIntent.PdfToImagesClicked -> handlePdfToImages(intent.uri)
            is PdfToolsIntent.AddWatermarkClicked -> handleAddWatermark(intent.uri, intent.text)
            is PdfToolsIntent.ExtractTextClicked -> handleExtractText(intent.uri)
            is PdfToolsIntent.OcrImageClicked -> handleOcrImage(intent.uri)
            is PdfToolsIntent.ScanDocumentCompleted -> handleScanDocumentCompleted(intent.pdfUri)
            is PdfToolsIntent.DeletePagesClicked -> handleDeletePages(intent.uri, intent.pagesText)
            is PdfToolsIntent.ReorderPagesClicked -> handleReorderPages(intent.uri, intent.orderText)
            is PdfToolsIntent.RotatePdfClicked -> handleRotatePdf(intent.uri, intent.rotationDegrees)
            is PdfToolsIntent.ExtractPageClicked -> handleExtractPage(intent.uri, intent.page)
            is PdfToolsIntent.FlattenPdfClicked -> handleFlattenPdf(intent.uri)
            is PdfToolsIntent.HtmlToPdfClicked -> handleHtmlToPdf(intent.html)
            is PdfToolsIntent.EarnCreditsAdClicked -> watchAdForCredits(intent.activity)
            is PdfToolsIntent.SaveFile -> handleSaveFile(intent.sourceFile, intent.destinationUri)
            is PdfToolsIntent.SaveFiles -> handleSaveFiles(intent.sourceFiles, intent.folderUri)
            is PdfToolsIntent.RequestReview -> appReviewManager.showReviewDialog(intent.activity)
            is PdfToolsIntent.DismissState -> _state.value = PdfToolsState.Idle
        }
    }

    private fun handleMerge(uris: List<Uri>) {
        executeFreeOperation("merge") {
            val outputFile = File(pdfEngine.cacheDir, "merged_${System.currentTimeMillis()}.pdf")
            pdfEngine.mergePdfs(uris, outputFile).map { listOf(it) }
        }
    }

    private fun handleSplit(uri: Uri) {
        executeFreeOperation("split") {
            val outputFolder = File(pdfEngine.cacheDir, "split_${System.currentTimeMillis()}")
            pdfEngine.splitPdf(uri, outputFolder)
        }
    }

    private fun handleCompress(uri: Uri) {
        executeFreeOperation("compress") {
            val outputFile =
                File(pdfEngine.cacheDir, "compressed_${System.currentTimeMillis()}.pdf")
            pdfEngine.compressPdf(uri, outputFile).map { listOf(it) }
        }
    }

    private fun handleEncrypt(uri: Uri, password: String) {
        executeFreeOperation("encrypt") {
            val outputFile = File(pdfEngine.cacheDir, "encrypted_${System.currentTimeMillis()}.pdf")
            pdfEngine.encryptPdf(uri, password, outputFile).map { listOf(it) }
        }
    }

    private fun handleDecrypt(uri: Uri, password: String) {
        executeFreeOperation("decrypt") {
            val outputFile = File(pdfEngine.cacheDir, "decrypted_${System.currentTimeMillis()}.pdf")
            pdfEngine.decryptPdf(uri, password, outputFile).map { listOf(it) }
        }
    }

    private fun handleImagesToPdf(uris: List<Uri>) {
        executeFreeOperation("images_to_pdf") {
            val outputFile = File(pdfEngine.cacheDir, "images_to_pdf_${System.currentTimeMillis()}.pdf")
            pdfEngine.imagesToPdf(uris, outputFile).map { listOf(it) }
        }
    }

    private fun handlePdfToImages(uri: Uri) {
        executeFreeOperation("pdf_to_images") {
            val outputFolder = File(pdfEngine.cacheDir, "pdf_to_images_${System.currentTimeMillis()}")
            pdfEngine.pdfToImages(uri, outputFolder)
        }
    }

    private fun handleAddWatermark(uri: Uri, text: String) {
        executeFreeOperation("add_watermark") {
            val outputFile = File(pdfEngine.cacheDir, "watermark_${System.currentTimeMillis()}.pdf")
            pdfEngine.addWatermark(uri, text, outputFile).map { listOf(it) }
        }
    }

    private fun handleExtractText(uri: Uri) {
        executeFreeOperation("extract_text") {
            val outputFile = File(pdfEngine.cacheDir, "extracted_${System.currentTimeMillis()}.txt")
            pdfEngine.extractText(uri, outputFile).map { listOf(it) }
        }
    }

    private fun handleOcrImage(uri: Uri) {
        viewModelScope.launch {
            val currentCredits = creditManager.creditsFlow.first()
            if (currentCredits < 1) {
                _state.value = PdfToolsState.InsufficientCredits
                return@launch
            }

            _state.value = PdfToolsState.Processing
            analyticsManager.logEvent("pdf_tool_started", mapOf("tool" to "ocr_image"))

            val result = pdfEngine.extractTextFromImage(uri)
            result.fold(
                onSuccess = { extractedText ->
                    val outputFile = File(pdfEngine.cacheDir, "ocr_${System.currentTimeMillis()}.txt")
                    outputFile.writeText(extractedText)
                    
                    creditManager.deductCredits(1)
                    analyticsManager.logEvent(
                        "pdf_tool_success",
                        mapOf("tool" to "ocr_image", "output_count" to "1")
                    )
                    _state.value = PdfToolsState.Success("ocr_image", listOf(outputFile))
                },
                onFailure = { error ->
                    analyticsManager.logEvent(
                        "pdf_tool_error",
                        mapOf("tool" to "ocr_image", "error" to (error.message ?: "unknown"))
                    )
                    _state.value = PdfToolsState.Error(error.message ?: "Unknown error")
                }
            )
        }
    }

    private fun handleScanDocumentCompleted(pdfUri: String) {
        viewModelScope.launch {
            analyticsManager.logEvent(
                "pdf_tool_success",
                mapOf("tool" to "scan_document", "output_count" to "1")
            )
            documentDao.insert(
                DocumentEntity(
                    fileName = "Scanned_Doc_${System.currentTimeMillis()}.pdf",
                    uri = pdfUri,
                    timestamp = System.currentTimeMillis()
                )
            )
            _state.value = PdfToolsState.Idle
        }
    }

    private fun handleDeletePages(uri: Uri, pagesText: String) {
        executeFreeOperation("delete_pages") {
            val pagesToRemove = pagesText.split(",")
                .mapNotNull { it.trim().toIntOrNull()?.minus(1) }
            val outputFile = File(pdfEngine.cacheDir, "deleted_pages_${System.currentTimeMillis()}.pdf")
            pdfEngine.deletePages(uri, pagesToRemove, outputFile).map { listOf(it) }
        }
    }

    private fun handleReorderPages(uri: Uri, orderText: String) {
        executeFreeOperation("reorder_pages") {
            val newOrder = orderText.split(",")
                .mapNotNull { it.trim().toIntOrNull()?.minus(1) }
            val outputFile = File(pdfEngine.cacheDir, "reordered_${System.currentTimeMillis()}.pdf")
            pdfEngine.reorderPages(uri, newOrder, outputFile).map { listOf(it) }
        }
    }

    private fun handleRotatePdf(uri: Uri, rotationDegrees: Int) {
        executeFreeOperation("rotate_pdf") {
            val outputFile = File(pdfEngine.cacheDir, "rotated_${System.currentTimeMillis()}.pdf")
            pdfEngine.rotatePages(uri, rotationDegrees, outputFile).map { listOf(it) }
        }
    }

    private fun handleExtractPage(uri: Uri, page: Int) {
        executeFreeOperation("extract_page") {
            val outputFile = File(pdfEngine.cacheDir, "extracted_page_${System.currentTimeMillis()}.pdf")
            pdfEngine.extractSinglePage(uri, page, outputFile).map { listOf(it) }
        }
    }

    private fun handleFlattenPdf(uri: Uri) {
        executeFreeOperation("flatten_pdf") {
            val outputFile = File(pdfEngine.cacheDir, "flattened_${System.currentTimeMillis()}.pdf")
            pdfEngine.flattenPdf(uri, outputFile).map { listOf(it) }
        }
    }

    private fun handleHtmlToPdf(html: String) {
        executeFreeOperation("html_to_pdf") {
            val outputFile = File(pdfEngine.cacheDir, "html_to_pdf_${System.currentTimeMillis()}.pdf")
            pdfEngine.htmlToPdf(html, outputFile).map { listOf(it) }
        }
    }

    private fun executeFreeOperation(
        toolName: String,
        operation: suspend () -> Result<List<File>>
    ) {
        viewModelScope.launch {
            _state.value = PdfToolsState.Processing
            analyticsManager.logEvent("pdf_tool_started", mapOf("tool" to toolName))

            val result = operation()
            result.fold(
                onSuccess = { files ->
                    analyticsManager.logEvent(
                        "pdf_tool_success",
                        mapOf("tool" to toolName, "output_count" to files.size.toString())
                    )
                    _state.value = PdfToolsState.Success(toolName, files)
                },
                onFailure = { error ->
                    analyticsManager.logEvent(
                        "pdf_tool_error",
                        mapOf("tool" to toolName, "error" to (error.message ?: "unknown"))
                    )
                    _state.value = PdfToolsState.Error(error.message ?: "Unknown error")
                }
            )
        }
    }

    private fun handleSaveFile(sourceFile: File, destinationUri: Uri) {
        viewModelScope.launch {
            val result = pdfEngine.copyToUri(sourceFile, destinationUri)
            result.fold(
                onSuccess = {
                    documentDao.insert(
                        DocumentEntity(
                            fileName = sourceFile.name,
                            uri = destinationUri.toString(),
                            timestamp = System.currentTimeMillis()
                        )
                    )
                    _state.value = PdfToolsState.Idle
                },
                onFailure = { error ->
                    _state.value = PdfToolsState.Error(error.message ?: "Failed to save file")
                }
            )
        }
    }

    private fun handleSaveFiles(sourceFiles: List<File>, folderUri: Uri) {
        viewModelScope.launch {
            val result = pdfEngine.copyToFolder(sourceFiles, folderUri)
            result.fold(
                onSuccess = { copiedFiles ->
                    val now = System.currentTimeMillis()
                    for ((fileName, fileUri) in copiedFiles) {
                        documentDao.insert(
                            DocumentEntity(
                                fileName = fileName,
                                uri = fileUri.toString(),
                                timestamp = now
                            )
                        )
                    }
                    _state.value = PdfToolsState.Idle
                },
                onFailure = { error ->
                    _state.value = PdfToolsState.Error(error.message ?: "Failed to save files")
                }
            )
        }
    }

    init {
        adManager.loadRewardedAd {}
    }

    fun dismissDialog() {
        _state.value = PdfToolsState.Idle
    }

    fun watchAdForCredits(activity: Activity) {
        val shown = adManager.showRewardedAd(activity) {
            viewModelScope.launch {
                creditManager.addCredits(5)
                // Reset state back to Idle so the dialog dismisses
                _state.value = PdfToolsState.Idle
            }
        }
        if (!shown) {
            _state.value = PdfToolsState.Error("No ads available right now. Please try again later.")
        }
        adManager.loadRewardedAd {}
    }
}
