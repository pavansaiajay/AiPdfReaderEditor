package pavansaiajayx.aipdfreadereditor.core.model

import kotlinx.serialization.Serializable

@Serializable
data class PdfMetadata(
    val title: String? = null,
    val author: String? = null,
    val subject: String? = null,
    val keywords: List<String> = emptyList(),
    val creator: String? = null,
    val producer: String? = null,
    val creationDateEpochMs: Long? = null,
    val modificationDateEpochMs: Long? = null,
    val pageCount: Int = 0,
    val isEncrypted: Boolean = false,
    val pdfVersion: String? = null
)
