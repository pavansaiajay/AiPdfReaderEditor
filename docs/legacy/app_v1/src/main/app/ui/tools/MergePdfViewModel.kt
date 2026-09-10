package pavansaiajayx.aipdfreadereditor.app.ui.tools

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pavansaiajayx.aipdfreadereditor.app.core.pdf.PdfEngine
import java.io.File
import javax.inject.Inject
import java.util.Collections

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext

import pavansaiajayx.aipdfreadereditor.app.data.local.DocumentDao
import pavansaiajayx.aipdfreadereditor.app.data.local.DocumentEntity

sealed interface MergePdfIntent {
    data class AddPdfs(val uris: List<Uri>) : MergePdfIntent
    data class RemovePdf(val uri: Uri) : MergePdfIntent
    data class ReorderPdfs(val from: Int, val to: Int) : MergePdfIntent
    data class UpdateList(val uris: List<Uri>) : MergePdfIntent
    data object MergeClicked : MergePdfIntent
    data object ResetResult : MergePdfIntent
}

sealed interface ResultState {
    data class Success(val uri: Uri) : ResultState
    data class Error(val message: String) : ResultState
}

data class MergePdfState(
    val selectedPdfs: List<Uri> = emptyList(),
    val isMerging: Boolean = false,
    val mergeProgress: Int = 0,
    val mergeResult: ResultState? = null
)

@HiltViewModel
class MergePdfViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pdfEngine: PdfEngine,
    private val documentDao: DocumentDao
) : ViewModel() {

    private val _state = MutableStateFlow(MergePdfState())
    val state: StateFlow<MergePdfState> = _state.asStateFlow()

    fun onIntent(intent: MergePdfIntent) {
        when (intent) {
            is MergePdfIntent.AddPdfs -> {
                val current = _state.value.selectedPdfs
                // Prevent adding duplicates
                val newUris = intent.uris.filter { !current.contains(it) }
                _state.value = _state.value.copy(
                    selectedPdfs = current + newUris
                )
            }
            is MergePdfIntent.RemovePdf -> {
                val current = _state.value.selectedPdfs
                _state.value = _state.value.copy(
                    selectedPdfs = current.filter { it != intent.uri }
                )
            }
            is MergePdfIntent.ReorderPdfs -> {
                val current = _state.value.selectedPdfs.toMutableList()
                if (intent.from in current.indices && intent.to in current.indices) {
                    Collections.swap(current, intent.from, intent.to)
                    _state.value = _state.value.copy(selectedPdfs = current)
                }
            }
            is MergePdfIntent.UpdateList -> {
                _state.value = _state.value.copy(selectedPdfs = intent.uris)
            }
            is MergePdfIntent.MergeClicked -> {
                val uris = _state.value.selectedPdfs
                if (uris.size >= 2) {
                    mergePdfs(uris)
                }
            }
            is MergePdfIntent.ResetResult -> {
                _state.value = _state.value.copy(mergeResult = null, isMerging = false)
            }
        }
    }

    private fun mergePdfs(uris: List<Uri>) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isMerging = true, mergeResult = null, mergeProgress = 0)
            val outputFile = File(pdfEngine.cacheDir, "merged_${System.currentTimeMillis()}.pdf")
            val result = pdfEngine.mergePdfs(uris, outputFile) { progress ->
                _state.value = _state.value.copy(mergeProgress = progress)
            }
            
            result.fold(
                onSuccess = { file ->
                    try {
                        val resolver = context.contentResolver
                        val fileName = "merged_${System.currentTimeMillis()}.pdf"
                        val contentValues = ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/AiPdfReaderEditor")
                            put(MediaStore.MediaColumns.IS_PENDING, 1)
                        }

                        val collection = MediaStore.Files.getContentUri("external")

                        val uri = resolver.insert(collection, contentValues)
                            ?: throw IllegalStateException("Failed to create MediaStore record")

                        resolver.openOutputStream(uri)?.use { outStream ->
                            file.inputStream().use { inStream ->
                                inStream.copyTo(outStream)
                            }
                        }

                        contentValues.clear()
                        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        resolver.update(uri, contentValues, null, null)

                        documentDao.insert(
                            DocumentEntity(
                                fileName = fileName,
                                uri = uri.toString(),
                                timestamp = System.currentTimeMillis()
                            )
                        )
                        
                        _state.value = _state.value.copy(
                            isMerging = false,
                            mergeResult = ResultState.Success(uri)
                        )
                    } catch (e: Exception) {
                        _state.value = _state.value.copy(
                            isMerging = false,
                            mergeResult = ResultState.Error(e.message ?: "Failed to save merged PDF")
                        )
                    }
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        isMerging = false,
                        mergeResult = ResultState.Error(error.message ?: "Failed to merge PDFs")
                    )
                }
            )
        }
    }
}
