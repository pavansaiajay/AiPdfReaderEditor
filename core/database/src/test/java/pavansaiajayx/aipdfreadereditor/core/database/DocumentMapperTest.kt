package pavansaiajayx.aipdfreadereditor.core.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pavansaiajayx.aipdfreadereditor.core.database.mapper.asEntity
import pavansaiajayx.aipdfreadereditor.core.database.mapper.asExternalModel
import pavansaiajayx.aipdfreadereditor.core.database.model.DocumentEntity
import pavansaiajayx.aipdfreadereditor.core.model.Document

class DocumentMapperTest {

    @Test
    fun entityMapsToExternalModelCorrectly() {
        val entity = DocumentEntity(
            id = 42L,
            fileName = "sample.pdf",
            uri = "content://media/external/files/42",
            pageCount = 15,
            fileSizeBytes = 2048576L,
            thumbnailUri = "content://media/external/thumb/42",
            isFavorite = true,
            isEncrypted = false,
            createdAt = 1000L,
            lastAccessedAt = 2000L
        )

        val model = entity.asExternalModel()

        assertEquals("42", model.id)
        assertEquals("sample.pdf", model.fileName)
        assertEquals("content://media/external/files/42", model.uri)
        assertEquals(15, model.pageCount)
        assertEquals(2048576L, model.sizeBytes)
        assertEquals("content://media/external/thumb/42", model.thumbnailUri)
        assertTrue(model.isFavorite)
        assertFalse(model.isEncrypted)
        assertEquals(1000L, model.createdAtEpochMs)
        assertEquals(2000L, model.lastAccessedEpochMs)
    }

    @Test
    fun externalModelMapsToEntityCorrectly() {
        val model = Document(
            id = "99",
            fileName = "invoice.pdf",
            uri = "content://media/external/files/99",
            sizeBytes = 1024L,
            pageCount = 2,
            lastAccessedEpochMs = 5000L,
            createdAtEpochMs = 4000L,
            thumbnailUri = null,
            isFavorite = false,
            isEncrypted = true
        )

        val entity = model.asEntity()

        assertEquals(99L, entity.id)
        assertEquals("invoice.pdf", entity.fileName)
        assertEquals("content://media/external/files/99", entity.uri)
        assertEquals(2, entity.pageCount)
        assertEquals(1024L, entity.fileSizeBytes)
        assertNull(entity.thumbnailUri)
        assertFalse(entity.isFavorite)
        assertTrue(entity.isEncrypted)
        assertEquals(4000L, entity.createdAt)
        assertEquals(5000L, entity.lastAccessedAt)
    }

    @Test
    fun externalModelWithNonNumericIdDefaultsToZeroEntityId() {
        val model = Document(
            id = "uuid-abc-123",
            fileName = "contract.pdf",
            uri = "content://media/external/files/doc",
            sizeBytes = 500L
        )

        val entity = model.asEntity()

        assertEquals(0L, entity.id)
    }
}
