package pavansaiajayx.aipdfreadereditor.core.model

import kotlinx.serialization.Serializable

@Serializable
data class PdfPage(
    val pageIndex: Int,
    val widthPoints: Float,
    val heightPoints: Float,
    val aspectRatio: Float = if (widthPoints > 0) heightPoints / widthPoints else 1.0f,
    val rotationDegrees: Int = 0
)
