package pavansaiajayx.aipdfreadereditor.core.common.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

class DateFormatterTest {

    @Test
    fun formatsStandardDate() {
        val formatted = DateFormatter.formatStandardDate(1700000000000L)
        assertNotNull(formatted)
        assertFalse(formatted.isBlank())
    }

    @Test
    fun formatsRelativeDate() {
        val now = System.currentTimeMillis()
        val justNow = DateFormatter.formatRelative(now - 1000L)
        assertNotNull(justNow)
        assertFalse(justNow.isBlank())
    }
}
