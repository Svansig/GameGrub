package app.gamegrub.utils.steam

import app.gamegrub.data.ManifestInfo
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object SteamFormatUtils {

    private val steamDateFormat by lazy {
        SimpleDateFormat("MMM d - h:mm a", Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }
    }

    fun getDownloadBytes(manifest: ManifestInfo?): Long {
        if (manifest == null) {
            return 0L
        }
        return if (manifest.download > 0L) manifest.download else manifest.size
    }

    fun fromSteamTime(rtime: Int): String = steamDateFormat.format(rtime * 1000L)

    fun formatPlayTime(time: Int): String {
        val hours = time / 60.0
        return if (hours % 1 == 0.0) {
            hours.toInt().toString()
        } else {
            String.format(Locale.getDefault(), "%.1f", hours)
        }
    }
}
