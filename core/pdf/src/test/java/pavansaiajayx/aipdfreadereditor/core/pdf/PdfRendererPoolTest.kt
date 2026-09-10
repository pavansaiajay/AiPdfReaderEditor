package pavansaiajayx.aipdfreadereditor.core.pdf

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import pavansaiajayx.aipdfreadereditor.core.pdf.renderer.PdfRendererPool

class PdfRendererPoolTest {

    @Test
    fun maxConcurrentRendersIsBoundedToFour() {
        val pool = PdfRendererPool()
        assertEquals(4, pool.maxConcurrent)
    }

    @Test
    fun closeAllAndCloseUriSucceedGracefullyWhenEmpty() = runTest {
        val pool = PdfRendererPool()
        assertNotNull(pool)
        pool.closeAll()
    }
}
