package app.gamegrub.content.manifest

import android.content.Context
import android.net.Uri
import app.gamegrub.R
import app.gamegrub.service.steam.SteamService
import com.winlator.contents.AdrenotoolsManager
import com.winlator.contents.ContentProfile
import com.winlator.contents.ContentsManager
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

/**
 * Result of a manifest-based install operation.
 *
 * @param success Whether the install succeeded
 * @param message User-facing status message
 */
data class ManifestInstallResult(
    val success: Boolean,
    val message: String,
)

/**
 * Handles downloading and installing components from the manifest.
 *
 * Supports both driver installations (via AdrenotoolsManager) and content
 * installations (DXVK, VKD3D, Wine, Proton, etc. via ContentsManager).
 *
 * Ownership: Belongs to `content/manifest` package. Migrated from
 * `utils/manifest` as part of COH-028.
 */
object ManifestInstaller {
    suspend fun downloadAndInstallDriver(
        context: Context,
        entry: ManifestEntry,
        onProgress: (Float) -> Unit = {},
    ): ManifestInstallResult = withContext(Dispatchers.IO) {
        var destFile: File? = null
        try {
            destFile = File(context.cacheDir, entry.url.substringAfterLast("/"))
            SteamService.fetchFile(entry.url, destFile, onProgress)
            val uri = Uri.fromFile(destFile)
            val name = AdrenotoolsManager(context).installDriver(uri)
            if (name.isEmpty()) {
                return@withContext ManifestInstallResult(
                    success = false,
                    message = context.getString(R.string.manifest_install_failed, entry.name),
                )
            }
            return@withContext ManifestInstallResult(
                success = true,
                message = context.getString(R.string.manifest_install_success, entry.name),
            )
        } catch (e: Exception) {
            Timber.e(e, "ManifestInstaller: driver install failed")
            return@withContext ManifestInstallResult(
                success = false,
                message = context.getString(R.string.manifest_download_failed, e.message ?: e.javaClass.simpleName),
            )
        } finally {
            destFile?.delete()
        }
    }

    suspend fun installManifestEntry(
        context: Context,
        entry: ManifestEntry,
        isDriver: Boolean,
        contentType: ContentProfile.ContentType? = null,
        onProgress: (Float) -> Unit = {},
    ): ManifestInstallResult {
        return if (isDriver) {
            downloadAndInstallDriver(context, entry, onProgress)
        } else {
            val type = contentType
                ?: throw IllegalArgumentException("contentType must be provided when installing manifest content")
            downloadAndInstallContent(context, entry, type, onProgress)
        }
    }

    suspend fun downloadAndInstallContent(
        context: Context,
        entry: ManifestEntry,
        expectedType: ContentProfile.ContentType,
        onProgress: (Float) -> Unit = {},
    ): ManifestInstallResult = withContext(Dispatchers.IO) {
        var destFile: File? = null
        try {
            destFile = File(context.cacheDir, entry.url.substringAfterLast("/"))
            SteamService.fetchFile(entry.url, destFile, onProgress)
            val uri = Uri.fromFile(destFile)
            val mgr = ContentsManager(context)

            val (profile, _, _) = extractContent(mgr, uri)
            if (profile == null) {
                return@withContext ManifestInstallResult(
                    success = false,
                    message = context.getString(R.string.manifest_install_failed, entry.name),
                )
            }

            val installed = finishInstall(mgr, profile)
            if (!installed) {
                return@withContext ManifestInstallResult(
                    success = false,
                    message = context.getString(R.string.manifest_install_failed, entry.name),
                )
            }

            return@withContext ManifestInstallResult(
                success = true,
                message = context.getString(R.string.manifest_install_success, entry.name),
            )
        } catch (e: Exception) {
            Timber.e(e, "ManifestInstaller: content install failed")
            return@withContext ManifestInstallResult(
                success = false,
                message = context.getString(R.string.manifest_download_failed, e.message ?: e.javaClass.simpleName),
            )
        } finally {
            destFile?.delete()
        }
    }

    private suspend fun extractContent(
        mgr: ContentsManager,
        uri: Uri,
    ): Triple<ContentProfile?, ContentsManager.InstallFailedReason?, Exception?> = withContext(Dispatchers.IO) {
        val deferred = CompletableDeferred<Triple<ContentProfile?, ContentsManager.InstallFailedReason?, Exception?>>()
        try {
            mgr.extraContentFile(
                uri,
                object : ContentsManager.OnInstallFinishedCallback {
                    override fun onFailed(reason: ContentsManager.InstallFailedReason, e: Exception) {
                        deferred.complete(Triple(null, reason, e))
                    }

                    override fun onSucceed(profileArg: ContentProfile) {
                        deferred.complete(Triple(profileArg, null, null))
                    }
                },
            )
        } catch (e: Exception) {
            deferred.complete(Triple(null, null, e))
        }
        val result = withTimeoutOrNull(240_000L) {
            deferred.await()
        } ?: Triple(null, null, Exception("Installation timed out"))
        result
    }

    private suspend fun finishInstall(
        mgr: ContentsManager,
        profile: ContentProfile,
    ): Boolean = withContext(Dispatchers.IO) {
        val deferred = CompletableDeferred<Boolean>()
        try {
            mgr.finishInstallContent(
                profile,
                object : ContentsManager.OnInstallFinishedCallback {
                    override fun onFailed(reason: ContentsManager.InstallFailedReason, e: Exception) {
                        deferred.complete(false)
                    }

                    override fun onSucceed(profileArg: ContentProfile) {
                        deferred.complete(true)
                    }
                },
            )
        } catch (_: Exception) {
            deferred.complete(false)
        }
        val result = withTimeoutOrNull(240_000L) {
            deferred.await()
        } ?: run {
            Timber.w("ManifestInstaller: finishInstall timed out after 240 seconds")
            false
        }
        result
    }
}
