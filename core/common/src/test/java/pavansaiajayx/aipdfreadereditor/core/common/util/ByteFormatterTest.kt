package pavansaiajayx.aipdfreadereditor.core.common.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ByteFormatterTest {

    @Test
    fun formatsZeroBytes() {
        assertEquals("0 B", ByteFormatter.format(0L))
    }

    @Test
    fun formatsBytes() {
        assertEquals("512 B", ByteFormatter.format(512L))
    }

    @Test
    fun formatsKilobytes() {
        assertEquals("1.0 KB", ByteFormatter.format(1024L))
        assertEquals("500.0 KB", ByteFormatter.format(512000L))
    }

    @Test
    fun formatsMegabytes() {
        assertEquals("1.0 MB", ByteFormatter.format(1048576L))
        assertEquals("2.5 MB", ByteFormatter.format(2621440L))
    }

    @Test
    fun formatsGigabytes() {
        assertEquals("1.0 GB", ByteFormatter.format(1073741824L))
    }
}
