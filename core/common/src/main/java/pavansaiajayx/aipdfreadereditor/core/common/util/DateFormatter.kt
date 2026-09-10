package pavansaiajayx.aipdfreadereditor.core.common.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateFormatter {
    private val standardDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val standardTimeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    fun formatStandardDate(epochMs: Long): String {
        if (epochMs <= 0L) return ""
        return standardDateFormat.format(Date(epochMs))
    }

    fun formatRelative(epochMs: Long, now: Long = System.currentTimeMillis()): String {
        if (epochMs <= 0L) return ""
        val diff = now - epochMs
        if (diff < 60_000L) return "Just now"
        if (diff < 3_600_000L) {
            val minutes = diff / 60_000L
            return if (minutes == 1L) "1 minute ago" else "$minutes minutes ago"
        }
        if (diff < 86_400_000L) {
            val hours = diff / 3_600_000L
            return if (hours == 1L) "1 hour ago" else "$hours hours ago"
        }
        if (diff < 172_800_000L) {
            return "Yesterday, ${standardTimeFormat.format(Date(epochMs))}"
        }
        return formatStandardDate(epochMs)
    }
}
