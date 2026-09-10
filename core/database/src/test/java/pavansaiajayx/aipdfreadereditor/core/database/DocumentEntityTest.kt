package pavansaiajayx.aipdfreadereditor.core.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pavansaiajayx.aipdfreadereditor.core.database.model.DocumentEntity

class DocumentEntityTest {

    @Test
    fun defaultValuesAreCorrectlyInitialized() {
        val before = System.currentTimeMillis()
        val entity = DocumentEntity(
            fileName = "test.pdf",
            uri = "content://media/external/files/10"
        )
        val after = System.currentTimeMillis()

        assertEquals(0L, entity.id)
        assertEquals("test.pdf", entity.fileName)
        assertEquals("content://media/external/files/10", entity.uri)
        assertEquals(0, entity.pageCount)
        assertEquals(0L, entity.fileSizeBytes)
        assertNull(entity.thumbnailUri)
        assertFalse(entity.isFavorite)
        assertFalse(entity.isEncrypted)
        assertTrue(entity.createdAt in before..after)
        assertTrue(entity.lastAccessedAt in before..after)
    }

    @Test
    fun entityCopyPreservesAndUpdatesFields() {
        val entity = DocumentEntity(
            id = 1L,
            fileName = "doc.pdf",
            uri = "content://media/external/files/1",
            isFavorite = false
        )

        val updated = entity.copy(isFavorite = true, lastAccessedAt = 9999L)

        assertEquals(1L, updated.id)
        assertTrue(updated.isFavorite)
        assertEquals(9999L, updated.lastAccessedAt)
    }
}
