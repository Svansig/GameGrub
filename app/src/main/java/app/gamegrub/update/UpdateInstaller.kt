package app.gamegrub.update

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

object UpdateInstaller {
    suspend fun downloadAndInstall(
        context: Context,
        downloadUrl: String,
        versionName: String,
        onProgress: (Float) -> Unit,
    ): Boolean = withContext(Dispatchers.IO) {
        context.hashCode()
        onProgress.hashCode()
        Timber.w(
            "Update installer is temporarily paused; skipping update download/install (url=%s, version=%s)",
            downloadUrl,
            versionName,
        )
        false
    }

    /*
     * Legacy implementation intentionally paused.
     * Keep this block for easier re-enable when update delivery is ready again.
     *
     * suspend fun downloadAndInstall(
     *     context: Context,
     *     downloadUrl: String,
     *     versionName: String,
     *     onProgress: (Float) -> Unit,
     * ): Boolean = withContext(Dispatchers.IO) {
     *     try {
     *         val apkFileName = "gamenative-v$versionName.apk"
     *         val destFile = File(context.cacheDir, apkFileName)
     *         val fileName = Uri.parse(downloadUrl).lastPathSegment
     *             ?.takeIf { it.isNotBlank() }
     *             ?: apkFileName
     *
     *         SteamService.fetchFileWithFallback(
     *             fileName = fileName,
     *             dest = destFile,
     *             onProgress = onProgress,
     *         )
     *
     *         if (!destFile.exists() || destFile.length() == 0L) {
     *             return@withContext false
     *         }
     *
     *         withContext(Dispatchers.Main) {
     *             installApk(context, destFile)
     *         }
     *         true
     *     } catch (e: Exception) {
     *         Timber.e(e, "Error downloading/installing update")
     *         false
     *     }
     * }
     *
     * @SuppressLint("QueryPermissionsNeeded")
     * private fun installApk(context: Context, apkFile: File) {
     *     // ...legacy installer intent + FileProvider flow...
     * }
     */
}
