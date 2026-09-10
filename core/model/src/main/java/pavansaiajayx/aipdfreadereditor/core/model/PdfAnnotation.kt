package pavansaiajayx.aipdfreadereditor.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Point(
    val x: Float,
    val y: Float
)

@Serializable
sealed interface PdfAnnotation {
    val id: String
    val pageIndex: Int

    @Serializable
    @SerialName("freehand")
    data class Freehand(
        override val id: String,
        override val pageIndex: Int,
        val points: List<Point>,
        val colorArgb: Int,
        val strokeWidthDp: Float,
        val isHighlighter: Boolean = false
    ) : PdfAnnotation

    @Serializable
    @SerialName("text_stamp")
    data class TextStamp(
        override val id: String,
        override val pageIndex: Int,
        val text: String,
        val xRatio: Float,
        val yRatio: Float,
        val colorArgb: Int,
        val fontSizeSp: Float
    ) : PdfAnnotation

    @Serializable
    @SerialName("signature_stamp")
    data class SignatureStamp(
        override val id: String,
        override val pageIndex: Int,
        val points: List<Point>,
        val xRatio: Float,
        val yRatio: Float,
        val scale: Float = 1.0f
    ) : PdfAnnotation
}
