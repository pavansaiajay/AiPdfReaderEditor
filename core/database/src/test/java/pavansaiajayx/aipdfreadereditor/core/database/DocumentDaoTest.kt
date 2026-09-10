package pavansaiajayx.aipdfreadereditor.core.database

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pavansaiajayx.aipdfreadereditor.core.database.dao.DocumentDao
import pavansaiajayx.aipdfreadereditor.core.database.model.DocumentEntity

class DocumentDaoTest {

    private lateinit var dao: DocumentDao

    @Before
    fun setup() {
        dao = FakeDocumentDao()
    }

    @Test
    fun getAllDocumentsOrdersByLastAccessedAtDescending() = runTest {
        val doc1 = DocumentEntity(
            fileName = "older.pdf",
            uri = "content://doc/1",
            lastAccessedAt = 1000L
        )
        val doc2 = DocumentEntity(
            fileName = "newer.pdf",
            uri = "content://doc/2",
            lastAccessedAt = 3000L
        )
        val doc3 = DocumentEntity(
            fileName = "middle.pdf",
            uri = "content://doc/3",
            lastAccessedAt = 2000L
        )

        dao.insertAll(listOf(doc1, doc2, doc3))

        val results = dao.getAllDocuments().first()
        assertEquals(3, results.size)
        assertEquals("newer.pdf", results[0].fileName)
        assertEquals("middle.pdf", results[1].fileName)
        assertEquals("older.pdf", results[2].fileName)
    }

    @Test
    fun getFavoriteDocumentsFiltersOnlyFavorites() = runTest {
        val fav1 = DocumentEntity(
            fileName = "fav1.pdf",
            uri = "content://doc/1",
            isFavorite = true,
            lastAccessedAt = 1000L
        )
        val regular = DocumentEntity(
            fileName = "regular.pdf",
            uri = "content://doc/2",
            isFavorite = false,
            lastAccessedAt = 2000L
        )
        val fav2 = DocumentEntity(
            fileName = "fav2.pdf",
            uri = "content://doc/3",
            isFavorite = true,
            lastAccessedAt = 3000L
        )

        dao.insertAll(listOf(fav1, regular, fav2))

        val favorites = dao.getFavoriteDocuments().first()
        assertEquals(2, favorites.size)
        assertEquals("fav2.pdf", favorites[0].fileName)
        assertEquals("fav1.pdf", favorites[1].fileName)
    }

    @Test
    fun searchDocumentsFiltersByFileName() = runTest {
        val docA = DocumentEntity(fileName = "Report_2026_Q1.pdf", uri = "content://doc/1")
        val docB = DocumentEntity(fileName = "Invoice_March.pdf", uri = "content://doc/2")
        val docC = DocumentEntity(fileName = "Annual_Report.pdf", uri = "content://doc/3")

        dao.insertAll(listOf(docA, docB, docC))

        val searchReport = dao.searchDocuments("Report").first()
        assertEquals(2, searchReport.size)
        assertTrue(searchReport.all { it.fileName.contains("Report") })

        val searchInvoice = dao.searchDocuments("Invoice").first()
        assertEquals(1, searchInvoice.size)
        assertEquals("Invoice_March.pdf", searchInvoice[0].fileName)

        val searchNone = dao.searchDocuments("NonExistent").first()
        assertTrue(searchNone.isEmpty())
    }

    @Test
    fun getDocumentByIdAndByUri() = runTest {
        val id = dao.insert(
            DocumentEntity(
                fileName = "contract.pdf",
                uri = "content://doc/unique_contract"
            )
        )

        val byId = dao.getDocumentById(id)
        assertNotNull(byId)
        assertEquals("contract.pdf", byId?.fileName)

        val byUri = dao.getDocumentByUri("content://doc/unique_contract")
        assertNotNull(byUri)
        assertEquals(id, byUri?.id)

        assertNull(dao.getDocumentById(9999L))
        assertNull(dao.getDocumentByUri("content://doc/missing"))
    }

    @Test
    fun updateFavoriteAndLastAccessedMutatesDocument() = runTest {
        val id = dao.insert(
            DocumentEntity(
                fileName = "notes.pdf",
                uri = "content://doc/notes",
                isFavorite = false,
                lastAccessedAt = 1000L
            )
        )

        dao.updateFavorite(id, true)
        var updated = dao.getDocumentById(id)
        assertTrue(updated?.isFavorite == true)

        dao.updateLastAccessed(id, 5555L)
        updated = dao.getDocumentById(id)
        assertEquals(5555L, updated?.lastAccessedAt)
    }

    @Test
    fun deleteAndClearAllRemovesDocuments() = runTest {
        val id1 = dao.insert(DocumentEntity(fileName = "a.pdf", uri = "content://doc/a"))
        val id2 = dao.insert(DocumentEntity(fileName = "b.pdf", uri = "content://doc/b"))

        assertEquals(2, dao.getAllDocuments().first().size)

        dao.deleteById(id1)
        val remaining = dao.getAllDocuments().first()
        assertEquals(1, remaining.size)
        assertEquals(id2, remaining[0].id)

        dao.clearAll()
        assertTrue(dao.getAllDocuments().first().isEmpty())
    }
}
