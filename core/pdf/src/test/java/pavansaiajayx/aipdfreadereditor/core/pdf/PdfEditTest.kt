package pavansaiajayx.aipdfreadereditor.core.pdf

import android.graphics.Path
import android.graphics.RectF
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import pavansaiajayx.aipdfreadereditor.core.pdf.edit.PdfEdit

class PdfEditTest {

    @Test
    fun pdfEditDrawProperties() {
        val path = Path()
        val draw = PdfEdit.Draw(path = path, color = 0xFFFF0000.toInt(), strokeWidth = 5.0f)

        assertEquals(0xFFFF0000.toInt(), draw.color)
        assertEquals(5.0f, draw.strokeWidth, 0.001f)
        assertSame(path, draw.path)
    }

    @Test
    fun pdfEditHighlightProperties() {
        val rect = RectF(10f, 20f, 100f, 50f)
        val highlight = PdfEdit.Highlight(rect = rect, color = 0x80FFFF00.toInt())

        assertSame(rect, highlight.rect)
        assertEquals(0x80FFFF00.toInt(), highlight.color)
    }

    @Test
    fun pdfEditTextProperties() {
        val text = PdfEdit.Text(text = "Approved", x = 150f, y = 300f, color = 0xFF000000.toInt(), size = 16f)

        assertEquals("Approved", text.text)
        assertEquals(150f, text.x, 0.001f)
        assertEquals(300f, text.y, 0.001f)
        assertEquals(16f, text.size, 0.001f)
    }
}
