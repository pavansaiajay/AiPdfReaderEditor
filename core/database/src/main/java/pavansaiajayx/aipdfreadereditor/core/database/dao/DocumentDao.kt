package pavansaiajayx.aipdfreadereditor.core.database.dao

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow
import pavansaiajayx.aipdfreadereditor.core.database.model.DocumentEntity

@Dao
interface DocumentDao {

    @Query("SELECT * FROM documents ORDER BY last_accessed_at DESC")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE is_favorite = 1 ORDER BY last_accessed_at DESC")
    fun getFavoriteDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getDocumentById(id: Long): DocumentEntity?

    @Query("SELECT * FROM documents WHERE uri = :uri LIMIT 1")
    suspend fun getDocumentByUri(uri: String): DocumentEntity?

    @Query("SELECT * FROM documents WHERE file_name LIKE '%' || :query || '%' ORDER BY last_accessed_at DESC")
    fun searchDocuments(query: String): Flow<List<DocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(document: DocumentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(documents: List<DocumentEntity>): List<Long>

    @Update
    suspend fun update(document: DocumentEntity)

    @Delete
    suspend fun delete(document: DocumentEntity)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE documents SET is_favorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE documents SET last_accessed_at = :timestamp WHERE id = :id")
    suspend fun updateLastAccessed(id: Long, timestamp: Long)

    @Query("DELETE FROM documents")
    suspend fun clearAll()
}
