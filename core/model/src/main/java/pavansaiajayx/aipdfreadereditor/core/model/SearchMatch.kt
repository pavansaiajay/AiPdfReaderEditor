package pavansaiajayx.aipdfreadereditor.core.model

import kotlinx.serialization.Serializable

@Serializable
data class NormalizedRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

@Serializable
data class SearchMatch(
    val pageIndex: Int,
    val matchText: String,
    val snippet: String,
    val boundingBoxes: List<NormalizedRect> = emptyList()
)
