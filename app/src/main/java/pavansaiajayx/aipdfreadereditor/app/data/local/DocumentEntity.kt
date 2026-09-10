package pavansaiajayx.aipdfreadereditor.app.data.local

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,
    val uri: String,
    val timestamp: Long,
    val thumbnailPath: String? = null
)
