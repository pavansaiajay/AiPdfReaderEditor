package pavansaiajayx.aipdfreadereditor.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Document(
    val id: String,
    val fileName: String,
    val uri: String,
    val sizeBytes: Long,
    val pageCount: Int = 0,
    val lastAccessedEpochMs: Long = System.currentTimeMillis(),
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val thumbnailUri: String? = null,
    val isFavorite: Boolean = false,
    val isEncrypted: Boolean = false
)
