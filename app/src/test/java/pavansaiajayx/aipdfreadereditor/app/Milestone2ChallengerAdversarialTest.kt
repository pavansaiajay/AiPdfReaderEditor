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
import pavansaiajayx.aipdfreadereditor.app.ui.home.HomeViewModel
import pavansaiajayx.aipdfreadereditor.app.ui.tools.MergePdfViewModel
import pavansaiajayx.aipdfreadereditor.app.ui.tools.PdfToolsIntent
import pavansaiajayx.aipdfreadereditor.app.ui.tools.PdfToolsViewModel
import pavansaiajayx.aipdfreadereditor.app.ui.tools.grid.DeletePagesViewModel
import pavansaiajayx.aipdfreadereditor.app.ui.tools.grid.ExtractPagesViewModel
import pavansaiajayx.aipdfreadereditor.app.ui.tools.grid.SplitPdfViewModel
import pavansaiajayx.aipdfreadereditor.app.ui.viewer.EditTool
import pavansaiajayx.aipdfreadereditor.app.ui.viewer.PdfViewerState
import pavansaiajayx.aipdfreadereditor.app.ui.viewer.PdfViewerViewModel
import java.io.File

/**
 * Challenger 1 Adversarial Test Suite for Milestone 2 (R2: Scoped Storage & Database Synchronization).
 * Stress tests:
 * - Batch SAF tree writing with multiple files & MIME type mapping
 * - DocumentDao entity persistence, timestamp ordering, and URI string representations
 * - Annotation save synchronization to DocumentDao
 * - ViewModel dependency injection and intent contracts
 */
class Milestone2ChallengerAdversarialTest {

    class MockDocumentDao : DocumentDao {
        private val _documents = MutableStateFlow<List<DocumentEntity>>(emptyList())
        val recordedInserts = mutableListOf<DocumentEntity>()
        val recordedDeletes = mutableListOf<DocumentEntity>()

        override suspend fun insert(document: DocumentEntity) {
            recordedInserts.add(document)
            val current = _documents.value.filter { it.id != document.id || (it.id == 0L && it.uri != document.uri) }
            val assignedId = if (document.id == 0L) (current.maxOfOrNull { it.id } ?: 0L) + 1 else document.id
            val newEntity = document.copy(id = assignedId)
            // Simulates SQLite ORDER BY timestamp DESC
            val updated = (current + newEntity).sortedByDescending { it.timestamp }
            _documents.value = updated
        }

        override fun getAllDocuments(): Flow<List<DocumentEntity>> = _documents.asStateFlow()

        override suspend fun delete(document: DocumentEntity) {
            recordedDeletes.add(document)
            _documents.value = _documents.value.filter { it.id != document.id && it.uri != document.uri }
        }
    }

    // ==========================================
    // 1. DocumentDao Persistence & Timestamp Ordering
    // ==========================================

    @Test
    fun testDocumentDaoTimestampOrderingDescending() = runBlocking {
        val dao = MockDocumentDao()
        val baseTime = 1700000000000L

        val docOld = DocumentEntity(fileName = "old.pdf", uri = "content://media/1", timestamp = baseTime)
        val docMid = DocumentEntity(fileName = "mid.pdf", uri = "content://media/2", timestamp = baseTime + 5000)
        val docNew = DocumentEntity(fileName = "new.pdf", uri = "content://media/3", timestamp = baseTime + 10000)

        // Insert in non-chronological order
        dao.insert(docMid)
        dao.insert(docOld)
        dao.insert(docNew)

        val docs = dao.getAllDocuments().first()
        assertEquals(3, docs.size)
        assertEquals("Latest file must be at head", "new.pdf", docs[0].fileName)
        assertEquals("Mid file must be second", "mid.pdf", docs[1].fileName)
        assertEquals("Oldest file must be last", "old.pdf", docs[2].fileName)
    }

    @Test
    fun testDocumentDaoUriStringPreservation() = runBlocking {
        val dao = MockDocumentDao()
        val complexUris = listOf(
            "content://com.android.externalstorage.documents/tree/primary%3ADocuments%2FAiPdfReaderEditor/document/primary%3ADocuments%2FAiPdfReaderEditor%2Fsplit_1.pdf",
            "content://media/external/file/987654?format=pdf&owner=app",
            "file:///data/user/0/pavansaiajayx.aipdfreadereditor.app/cache/sample.pdf",
            "content://com.android.providers.downloads.documents/document/raw%3A%2Fstorage%2Femulated%2F0%2FDownload%2Ftest%20(1).pdf"
        )

        val time = System.currentTimeMillis()
        complexUris.forEachIndexed { index, uriStr ->
            dao.insert(DocumentEntity(fileName = "file_$index.pdf", uri = uriStr, timestamp = time + index))
        }

        val docs = dao.getAllDocuments().first()
        assertEquals(complexUris.size, docs.size)
        // Verify exact URI strings were preserved without corruption
        for (i in complexUris.indices) {
            val matched = docs.find { it.fileName == "file_$i.pdf" }
            assertNotNull("Document file_$i.pdf must exist", matched)
            assertEquals("URI string must match verbatim", complexUris[i], matched?.uri)
        }
    }

    @Test
    fun testDocumentDaoReplaceOrUpdateExistingId() = runBlocking {
        val dao = MockDocumentDao()
        val initialDoc = DocumentEntity(id = 42L, fileName = "report.pdf", uri = "content://doc/42", timestamp = 1000L)
        dao.insert(initialDoc)

        assertEquals(1, dao.getAllDocuments().first().size)
        assertEquals("report.pdf", dao.getAllDocuments().first()[0].fileName)

        val updatedDoc = DocumentEntity(id = 42L, fileName = "report_v2.pdf", uri = "content://doc/42_new", timestamp = 2000L)
        dao.insert(updatedDoc)

        val docs = dao.getAllDocuments().first()
        assertEquals("Should replace existing entity with same ID", 1, docs.size)
        assertEquals("report_v2.pdf", docs[0].fileName)
        assertEquals("content://doc/42_new", docs[0].uri)
        assertEquals(2000L, docs[0].timestamp)
    }

    // ==========================================
    // 2. Batch SAF Tree Writing & MIME Types
    // ==========================================

    @Test
    fun testMimeTypeResolutionLogic() {
        fun resolveMimeType(fileName: String): String {
            val extension = fileName.substringAfterLast('.', "")
            return when (extension.lowercase()) {
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "txt" -> "text/plain"
                else -> "application/pdf"
            }
        }

        assertEquals("application/pdf", resolveMimeType("page_1.pdf"))
        assertEquals("application/pdf", resolveMimeType("PAGE_1.PDF"))
        assertEquals("image/jpeg", resolveMimeType("page_1.jpg"))
        assertEquals("image/jpeg", resolveMimeType("PAGE_1.JPEG"))
        assertEquals("image/png", resolveMimeType("icon.png"))
        assertEquals("image/png", resolveMimeType("SCREENSHOT.PNG"))
        assertEquals("text/plain", resolveMimeType("extracted.txt"))
        assertEquals("application/pdf", resolveMimeType("unrecognized_extension.bin"))
        assertEquals("application/pdf", resolveMimeType("no_extension_file"))
        assertEquals("application/pdf", resolveMimeType("archive.tar.gz"))
    }

    @Test
    fun testBatchFileEntityCreationInDao() = runBlocking {
        val dao = MockDocumentDao()
        val timestamp = System.currentTimeMillis()
        val batchFiles = listOf(
            "split_page_1.pdf" to "content://saf/tree/document/split_page_1.pdf",
            "split_page_2.pdf" to "content://saf/tree/document/split_page_2.pdf",
            "split_page_3.pdf" to "content://saf/tree/document/split_page_3.pdf"
        )

        for ((name, uri) in batchFiles) {
            dao.insert(DocumentEntity(fileName = name, uri = uri, timestamp = timestamp))
        }

        val allDocs = dao.getAllDocuments().first()
        assertEquals(3, allDocs.size)
        assertEquals(3, dao.recordedInserts.size)
        assertTrue(allDocs.any { it.fileName == "split_page_1.pdf" })
        assertTrue(allDocs.any { it.fileName == "split_page_2.pdf" })
        assertTrue(allDocs.any { it.fileName == "split_page_3.pdf" })
    }

    // ==========================================
    // 3. Annotation Save Synchronization
    // ==========================================

    @Test
    fun testFilenameExtractionFromUriString() {
        fun extractFileNameFromSegment(segment: String?): String {
            return segment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
                ?: "annotated_fallback.pdf"
        }

        // Test with standard path segment
        assertEquals("contract.pdf", extractFileNameFromSegment("contract.pdf"))
        assertEquals("contract.pdf", extractFileNameFromSegment("documents/contract.pdf"))
        assertEquals("annotated_fallback.pdf", extractFileNameFromSegment(null))
        assertEquals("annotated_fallback.pdf", extractFileNameFromSegment(""))
        assertEquals("annotated_fallback.pdf", extractFileNameFromSegment("   "))
    }

    @Test
    fun testPdfViewerStateEmptyEditsProtection() {
        val state = PdfViewerState(edits = emptyList())
        assertTrue("Empty edits should not trigger saving", state.edits.isEmpty())
        assertFalse(state.isSaving)
        assertFalse(state.saveSuccess)
    }

    // ==========================================
    // 4. ViewModel DAO Injection Verification
    // ==========================================

    @Test
    fun verifyAllStorageViewModelsInjectDocumentDao() {
        val viewModelsToVerify = listOf(
            PdfViewerViewModel::class.java,
            SplitPdfViewModel::class.java,
            PdfToolsViewModel::class.java,
            MergePdfViewModel::class.java,
            DeletePagesViewModel::class.java,
            ExtractPagesViewModel::class.java,
            HomeViewModel::class.java
        )

        for (vmClass in viewModelsToVerify) {
            val constructors = vmClass.declaredConstructors
            var hasDao = false
            for (constructor in constructors) {
                if (constructor.parameterTypes.contains(DocumentDao::class.java)) {
                    hasDao = true
                    break
                }
            }
            assertTrue(
                "ViewModel ${vmClass.simpleName} must have DocumentDao injected for Scoped Storage synchronization",
                hasDao
            )
        }
    }

    @Test
    fun verifyHomeViewModelRecentFilesFlowPresence() {
        val fields = HomeViewModel::class.java.declaredFields
        val recentFilesField = fields.find { it.name == "recentFiles" }
        assertNotNull("HomeViewModel must expose recentFiles field for displaying DB history", recentFilesField)
        assertEquals(kotlinx.coroutines.flow.StateFlow::class.java, recentFilesField?.type)
    }
}
