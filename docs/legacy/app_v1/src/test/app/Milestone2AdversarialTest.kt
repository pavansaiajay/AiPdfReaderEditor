package pavansaiajayx.aipdfreadereditor.app

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import pavansaiajayx.aipdfreadereditor.app.data.local.DocumentDao
import pavansaiajayx.aipdfreadereditor.app.data.local.DocumentEntity
import pavansaiajayx.aipdfreadereditor.app.ui.tools.PdfToolsIntent
import pavansaiajayx.aipdfreadereditor.app.ui.tools.PdfToolsViewModel
import pavansaiajayx.aipdfreadereditor.app.ui.tools.grid.SplitPdfViewModel
import pavansaiajayx.aipdfreadereditor.app.ui.viewer.PdfViewerViewModel
import java.io.File

/**
 * Fake DocumentDao for testing synchronization logic without Room SQLite runtime dependencies.
 */
class FakeDocumentDao : DocumentDao {
    private val _docs = MutableStateFlow<List<DocumentEntity>>(emptyList())

    override suspend fun insert(document: DocumentEntity) {
        val current = _docs.value.filter { it.id != document.id || (it.id == 0L && it.uri != document.uri) }
        val generatedId = if (document.id == 0L) (current.maxOfOrNull { it.id } ?: 0L) + 1 else document.id
        _docs.value = listOf(document.copy(id = generatedId)) + current
    }

    override fun getAllDocuments(): Flow<List<DocumentEntity>> = _docs.asStateFlow()

    override suspend fun delete(document: DocumentEntity) {
        _docs.value = _docs.value.filter { it.id != document.id && it.uri != document.uri }
    }
}

class Milestone2AdversarialTest {

    @Test
    fun testDocumentEntityCreation() {
        val timestamp = System.currentTimeMillis()
        val doc = DocumentEntity(
            fileName = "sample_output.pdf",
            uri = "content://com.android.providers.media.documents/document/123",
            timestamp = timestamp
        )
        assertEquals("sample_output.pdf", doc.fileName)
        assertEquals("content://com.android.providers.media.documents/document/123", doc.uri)
        assertEquals(timestamp, doc.timestamp)
        assertNull(doc.thumbnailPath)
    }

    @Test
    fun testFakeDocumentDaoInsertAndRetrieve() = runBlocking {
        val dao = FakeDocumentDao()
        val timestamp = System.currentTimeMillis()
        val doc1 = DocumentEntity(fileName = "doc1.pdf", uri = "content://doc/1", timestamp = timestamp)
        val doc2 = DocumentEntity(fileName = "doc2.pdf", uri = "content://doc/2", timestamp = timestamp + 1000)

        dao.insert(doc1)
        dao.insert(doc2)

        val list = dao.getAllDocuments().first()
        assertEquals("Should contain 2 documents", 2, list.size)
        assertEquals("Latest document should be at index 0", "doc2.pdf", list[0].fileName)
        assertEquals("doc1.pdf", list[1].fileName)

        dao.delete(list[0])
        val listAfterDelete = dao.getAllDocuments().first()
        assertEquals(1, listAfterDelete.size)
        assertEquals("doc1.pdf", listAfterDelete[0].fileName)
    }

    @Test
    fun verifyPdfViewerViewModelConstructorInjectsDocumentDao() {
        val clazz = PdfViewerViewModel::class.java
        val constructors = clazz.declaredConstructors
        var hasDocumentDao = false
        for (constructor in constructors) {
            if (constructor.parameterTypes.contains(DocumentDao::class.java)) {
                hasDocumentDao = true
                break
            }
        }
        assertTrue("PdfViewerViewModel must inject DocumentDao for annotation sync", hasDocumentDao)
    }

    @Test
    fun verifySplitPdfViewModelConstructorInjectsDocumentDao() {
        val clazz = SplitPdfViewModel::class.java
        val constructors = clazz.declaredConstructors
        var hasDocumentDao = false
        for (constructor in constructors) {
            if (constructor.parameterTypes.contains(DocumentDao::class.java)) {
                hasDocumentDao = true
                break
            }
        }
        assertTrue("SplitPdfViewModel must inject DocumentDao for batch split sync", hasDocumentDao)
    }

    @Test
    fun verifyPdfToolsViewModelConstructorInjectsDocumentDao() {
        val clazz = PdfToolsViewModel::class.java
        val constructors = clazz.declaredConstructors
        var hasDocumentDao = false
        for (constructor in constructors) {
            if (constructor.parameterTypes.contains(DocumentDao::class.java)) {
                hasDocumentDao = true
                break
            }
        }
        assertTrue("PdfToolsViewModel must inject DocumentDao for single & batch output sync", hasDocumentDao)
    }

    @Test
    fun testPdfToolsSaveFilesIntentContract() {
        val clazz = PdfToolsIntent.SaveFiles::class.java
        val fields = clazz.declaredFields.map { it.name to it.type }.toMap()
        assertTrue("SaveFiles intent must contain sourceFiles field", fields.containsKey("sourceFiles"))
        assertTrue("SaveFiles intent must contain folderUri field", fields.containsKey("folderUri"))
        assertEquals(java.util.List::class.java, fields["sourceFiles"])
        assertEquals(android.net.Uri::class.java, fields["folderUri"])
    }

    @Test
    fun testPdfToolsSaveFileIntentContract() {
        val clazz = PdfToolsIntent.SaveFile::class.java
        val fields = clazz.declaredFields.map { it.name to it.type }.toMap()
        assertTrue("SaveFile intent must contain sourceFile field", fields.containsKey("sourceFile"))
        assertTrue("SaveFile intent must contain destinationUri field", fields.containsKey("destinationUri"))
        assertEquals(File::class.java, fields["sourceFile"])
        assertEquals(android.net.Uri::class.java, fields["destinationUri"])
    }

    @Test
    fun testSplitPdfViewModelSelectionMethodsExist() {
        val clazz = SplitPdfViewModel::class.java
        val methodNames = clazz.declaredMethods.map { it.name }
        assertTrue("SplitPdfViewModel must have splitPdfToFolder method", methodNames.contains("splitPdfToFolder"))
        assertTrue("SplitPdfViewModel must have selectRange method", methodNames.contains("selectRange"))
        assertTrue("SplitPdfViewModel must have selectAll method", methodNames.contains("selectAll"))
        assertTrue("SplitPdfViewModel must have clearSelection method", methodNames.contains("clearSelection"))
    }
}
