package pavansaiajayx.aipdfreadereditor.core.pdf.di

import org.junit.Assert.assertNotNull
import org.junit.Test

class PdfModuleTest {

    @Test
    fun providePdfRendererPoolReturnsNonNull() {
        val pool = PdfModule.providePdfRendererPool()
        assertNotNull(pool)
    }
}
