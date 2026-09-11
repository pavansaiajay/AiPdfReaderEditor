package pavansaiajayx.aipdfreadereditor.feature.home

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import pavansaiajayx.aipdfreadereditor.core.database.dao.DocumentDao
import pavansaiajayx.aipdfreadereditor.core.database.model.DocumentEntity

internal class FakeDocumentDao : DocumentDao {

    private val documentsState = MutableStateFlow<Map<Long, DocumentEntity>>(emptyMap())
    private var nextId = 1L

    override fun getAllDocuments(): Flow<List<DocumentEntity>> {
        return documentsState.map { map ->
            map.values.sortedByDescending { it.lastAccessedAt }
        }
    }

    override fun getFavoriteDocuments(): Flow<List<DocumentEntity>> {
        return documentsState.map { map ->
            map.values
                .filter { it.isFavorite }
                .sortedByDescending { it.lastAccessedAt }
        }
    }

    override suspend fun getDocumentById(id: Long): DocumentEntity? {
        return documentsState.value[id]
    }

    override suspend fun getDocumentByUri(uri: String): DocumentEntity? {
        return documentsState.value.values.firstOrNull { it.uri == uri }
    }

    override fun searchDocuments(query: String): Flow<List<DocumentEntity>> {
        return documentsState.map { map ->
            map.values
                .filter { it.fileName.contains(query, ignoreCase = true) }
                .sortedByDescending { it.lastAccessedAt }
        }
    }

    override suspend fun insert(document: DocumentEntity): Long {
        val current = documentsState.value.toMutableMap()
        val assignedId = if (document.id == 0L) nextId++ else document.id
        val entity = document.copy(id = assignedId)
        current[assignedId] = entity
        documentsState.value = current
        return assignedId
    }

    override suspend fun insertAll(documents: List<DocumentEntity>): List<Long> {
        val ids = mutableListOf<Long>()
        val current = documentsState.value.toMutableMap()
        for (doc in documents) {
            val assignedId = if (doc.id == 0L) nextId++ else doc.id
            current[assignedId] = doc.copy(id = assignedId)
            ids.add(assignedId)
        }
        documentsState.value = current
        return ids
    }

    override suspend fun update(document: DocumentEntity) {
        val current = documentsState.value.toMutableMap()
        if (current.containsKey(document.id)) {
            current[document.id] = document
            documentsState.value = current
        }
    }

    override suspend fun delete(document: DocumentEntity) {
        val current = documentsState.value.toMutableMap()
        current.remove(document.id)
        documentsState.value = current
    }

    override suspend fun deleteById(id: Long) {
        val current = documentsState.value.toMutableMap()
        current.remove(id)
        documentsState.value = current
    }

    override suspend fun updateFavorite(id: Long, isFavorite: Boolean) {
        val current = documentsState.value.toMutableMap()
        val existing = current[id]
        if (existing != null) {
            current[id] = existing.copy(isFavorite = isFavorite)
            documentsState.value = current
        }
    }

    override suspend fun updateLastAccessed(id: Long, timestamp: Long) {
        val current = documentsState.value.toMutableMap()
        val existing = current[id]
        if (existing != null) {
            current[id] = existing.copy(lastAccessedAt = timestamp)
            documentsState.value = current
        }
    }

    override suspend fun clearAll() {
        documentsState.value = emptyMap()
    }
}
