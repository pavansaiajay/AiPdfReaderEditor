package pavansaiajayx.aipdfreadereditor.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Document(
    val id: String,
    val title: String,
    val uri: String,
    val sizeBytes: Long,
    val pageCount: Int = 0,
    val lastModifiedEpochMs: Long = System.currentTimeMillis(),
    val thumbnailUri: String? = null
)
