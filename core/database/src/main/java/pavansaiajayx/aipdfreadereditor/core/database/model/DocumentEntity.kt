package pavansaiajayx.aipdfreadereditor.core.database.model

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "file_name")
    val fileName: String,
    @ColumnInfo(name = "uri")
    val uri: String,
    @ColumnInfo(name = "page_count")
    val pageCount: Int = 0,
    @ColumnInfo(name = "file_size_bytes")
    val fileSizeBytes: Long = 0,
    @ColumnInfo(name = "thumbnail_uri")
    val thumbnailUri: String? = null,
    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,
    @ColumnInfo(name = "is_encrypted")
    val isEncrypted: Boolean = false,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "last_accessed_at")
    val lastAccessedAt: Long = System.currentTimeMillis()
)
