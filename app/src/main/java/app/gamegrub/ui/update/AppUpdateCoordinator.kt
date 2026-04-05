package app.gamegrub.ui.update

import android.content.Context
import app.gamegrub.BuildConfig
import app.gamegrub.update.UpdateInstaller
import app.gamegrub.update.UpdateChecker
import app.gamegrub.update.UpdateInfo
import timber.log.Timber

/**
 * Coordinates the app update workflow: checking for updates, downloading, and installing.
 *
 * This class encapsulates the logic for:
 * - Checking if an app update is available
 * - Downloading the update
 * - Installing the update
 *
 * The coordinator preserves all existing update behavior from GameGrubMain while
 * moving it into a testable boundary that doesn't live in composable effects.
 *
 * @param context Application context used for update operations.
 */
class AppUpdateCoordinator(
    private val context: Context,
) {
    /**
     * Checks for app updates and returns update info if available.
     *
     * @return [UpdateInfo] if an update is available and newer than current version,
     *         null otherwise.
     */
    suspend fun checkForUpdate(): UpdateInfo? {
        val checkedUpdateInfo = UpdateChecker.checkForUpdate(context)
        if (checkedUpdateInfo != null) {
            val appVersionCode = BuildConfig.VERSION_CODE
            val serverVersionCode = checkedUpdateInfo.versionCode
            Timber.i("Update check: app versionCode=$appVersionCode, server versionCode=$serverVersionCode")
            if (appVersionCode < serverVersionCode) {
                return checkedUpdateInfo
            }
        }
        return null
    }

    /**
     * Downloads and installs the provided update.
     *
     * This is a suspending operation that handles the download and install workflow.
     *
     * @param updateInfo The update info containing download URL and version details.
     * @param onProgress Callback invoked with download progress (0.0 to 1.0).
     * @return true if the update was successfully downloaded and installed, false otherwise.
     */
    suspend fun downloadAndInstall(
        updateInfo: UpdateInfo,
        onProgress: (Float) -> Unit,
    ): Boolean {
        return UpdateInstaller.downloadAndInstall(
            context = context,
            downloadUrl = updateInfo.downloadUrl,
            versionName = updateInfo.versionName,
            onProgress = onProgress,
        )
    }
}
