package pavansaiajayx.aipdfreadereditor.core.common.util

import java.util.Locale

object ByteFormatter {
    private const val KB = 1024L
    private const val MB = 1024L * 1024L
    private const val GB = 1024L * 1024L * 1024L

    fun format(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        return when {
            bytes >= GB -> String.format(Locale.US, "%.1f GB", bytes.toDouble() / GB)
            bytes >= MB -> String.format(Locale.US, "%.1f MB", bytes.toDouble() / MB)
            bytes >= KB -> String.format(Locale.US, "%.1f KB", bytes.toDouble() / KB)
            else -> "$bytes B"
        }
    }
}
