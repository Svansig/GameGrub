package app.gamegrub.utils

import java.text.DecimalFormat

object FormatUtils {
    private val sizeUnits = arrayOf("B", "KB", "MB", "GB", "TB")

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"

        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val df = DecimalFormat("#,##0.##")

        return "${df.format(bytes / Math.pow(1024.0, digitGroups.toDouble()))} ${sizeUnits[digitGroups]}"
    }

    fun formatPlayTime(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "< 1m"
        }
    }

    fun formatLastPlayed(timestamp: Long): String {
        if (timestamp <= 0) return "Never"

        val now = System.currentTimeMillis()
        val diff = now - timestamp

        val days = diff / (24 * 60 * 60 * 1000)
        val hours = diff / (60 * 60 * 1000)
        val minutes = diff / (60 * 1000)

        return when {
            days > 30 -> "${days / 30} months ago"
            days > 0 -> "$days days ago"
            hours > 0 -> "$hours hours ago"
            minutes > 0 -> "$minutes minutes ago"
            else -> "Just now"
        }
    }
}
