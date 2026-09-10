package pavansaiajayx.aipdfreadereditor.app.ui.tools.grid

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pavansaiajayx.aipdfreadereditor.app.core.pdf.PdfEngine
import pavansaiajayx.aipdfreadereditor.app.data.local.DocumentDao
import java.io.File
import javax.inject.Inject

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

@HiltViewModel
class DeletePagesViewModel @Inject constructor(
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

    fun deletePages() {
        val sourceUri = _selectedPdfUri.value ?: return
        val pagesToRemove = _selectedPages.value.toList().sorted()
        if (pagesToRemove.isEmpty()) {
            return
        }

        viewModelScope.launch {
            _isProcessing.value = true
            
            val tempFile = File(pdfEngine.cacheDir, "deleted_${System.currentTimeMillis()}.pdf")
            
            val result = pdfEngine.deletePages(sourceUri, pagesToRemove, tempFile)
            result.fold(
                onSuccess = { file ->
                    try {
                        val resolver = context.contentResolver
                        val fileName = "deleted_${System.currentTimeMillis()}.pdf"
                        val contentValues = android.content.ContentValues().apply {
                            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOCUMENTS + "/AiPdfReaderEditor")
                            put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
                        }

                        val collection = android.provider.MediaStore.Files.getContentUri("external")
                        val uri = resolver.insert(collection, contentValues)
                            ?: throw IllegalStateException("Failed to create MediaStore record")

                        resolver.openOutputStream(uri)?.use { outStream ->
                            file.inputStream().use { inStream ->
                                inStream.copyTo(outStream)
                            }
                        }

                        contentValues.clear()
                        contentValues.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
                        resolver.update(uri, contentValues, null, null)

                        documentDao.insert(
                            pavansaiajayx.aipdfreadereditor.app.data.local.DocumentEntity(
                                fileName = fileName,
                                uri = uri.toString(),
                                timestamp = System.currentTimeMillis()
                            )
                        )
                        _successUri.value = uri
                    } catch (e: Exception) {
                        _error.value = e.message ?: "Failed to save deleted PDF"
                    }
                },
                onFailure = { error ->
                    _error.value = error.message ?: "Failed to delete pages"
                }
            )
            
            _isProcessing.value = false
        }
    }
}
