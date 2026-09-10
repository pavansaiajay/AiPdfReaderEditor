package pavansaiajayx.aipdfreadereditor.app.ui.tools.grid

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pavansaiajayx.aipdfreadereditor.app.core.pdf.PdfEngine
import pavansaiajayx.aipdfreadereditor.app.data.local.DocumentDao
import pavansaiajayx.aipdfreadereditor.app.data.local.DocumentEntity
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SplitPdfViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pdfEngine: PdfEngine,
    private val documentDao: DocumentDao
) : ViewModel() {

    private val _selectedPdfUri = MutableStateFlow<Uri?>(null)
    val selectedPdfUri = _selectedPdfUri.asStateFlow()

    private val _pageCount = MutableStateFlow(0)
    val pageCount = _pageCount.asStateFlow()

    private val _selectedPages = MutableStateFlow<Set<Int>>(emptySet())
    val selectedPages = _selectedPages.asStateFlow()
    
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    // Since split returns multiple files, we track the first created URI or folder URI on success
    private val _successUri = MutableStateFlow<Uri?>(null)
    val successUri = _successUri.asStateFlow()

    fun dismissError() {
        _error.value = null
    }

    fun dismissSuccess() {
        _successUri.value = null
    }

    fun loadPdf(uri: Uri) {
        viewModelScope.launch {
            _selectedPdfUri.value = uri
            val result = pdfEngine.getPageCount(uri)
            result.onSuccess { count ->
                _pageCount.value = count
            }
        }
    }

    fun togglePageSelection(pageIndex: Int) {
        val current = _selectedPages.value.toMutableSet()
        if (current.contains(pageIndex)) {
            current.remove(pageIndex)
        } else {
            current.add(pageIndex)
        }
        _selectedPages.value = current
    }

    fun selectRange(start: Int, end: Int) {
        val min = minOf(start, end)
        val max = maxOf(start, end)
        val range = (min..max).toSet()
        _selectedPages.value = _selectedPages.value + range
    }

    fun selectAll() {
        _selectedPages.value = (0 until _pageCount.value).toSet()
    }

    fun clearSelection() {
        _selectedPages.value = emptySet()
    }

    /**
     * Splits selected pages into cache files, then saves them into the user-selected
     * SAF directory tree folder, synchronizing all output records into DocumentDao.
     */
    fun splitPdfToFolder(folderUri: Uri) {
        val sourceUri = _selectedPdfUri.value ?: return
        val pages = _selectedPages.value.toList().sorted()
        if (pages.isEmpty()) {
            return
        }

        viewModelScope.launch {
            _isProcessing.value = true
            
            val tempFolder = File(pdfEngine.cacheDir, "split_output_${System.currentTimeMillis()}")
            val splitResult = pdfEngine.splitSelectedPages(sourceUri, pages, tempFolder)
            
            splitResult.fold(
                onSuccess = { files ->
                    val copyResult = pdfEngine.copyToFolder(files, folderUri)
                    copyResult.fold(
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
                            _successUri.value = copiedFiles.firstOrNull()?.second ?: folderUri
                        },
                        onFailure = { error ->
                            _error.value = error.message ?: "Failed to save split PDFs to folder"
                        }
                    )
                },
                onFailure = { error ->
                    _error.value = error.message ?: "Failed to split PDF"
                }
            )
            
            _isProcessing.value = false
        }
    }
}
