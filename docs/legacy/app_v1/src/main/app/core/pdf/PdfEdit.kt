package pavansaiajayx.aipdfreadereditor.app.core.pdf

import android.graphics.Path
import android.graphics.RectF

sealed interface PdfEdit {
    data class Draw(val path: Path, val color: Int, val strokeWidth: Float) : PdfEdit
    data class Highlight(val rect: RectF, val color: Int) : PdfEdit
    data class Text(val text: String, val x: Float, val y: Float, val color: Int, val size: Float) : PdfEdit
}
