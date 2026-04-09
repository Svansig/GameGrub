package app.gamegrub.service.steam

import app.gamegrub.PrefManager
import app.gamegrub.data.SteamApp
import app.gamegrub.enums.Marker
import app.gamegrub.service.DownloadService
import app.gamegrub.storage.StorageManager
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.pathString
import timber.log.Timber

object SteamPaths {

    val serverListPath: String
        get() {
            // Priority 1: Try cache directory first (preferred location)
            val cachePath = DownloadService.baseCacheDirPath
            if (cachePath.isNotBlank()) {
                val cacheDir = File(cachePath)
                if (cacheDir.exists() && cacheDir.canWrite()) {
                    Timber.d("Using cache directory for server list: $cachePath")
                    return Paths.get(cachePath, "server_list.bin").pathString
                }
            }

            // Priority 2: Fall back to data directory (guaranteed writable)
            val dataPath = DownloadService.baseDataDirPath
            if (dataPath.isNotBlank()) {
                val dataDir = File(dataPath)
                if (dataDir.exists() && dataDir.canWrite()) {
                    Timber.w("Cache directory not writable, using data directory for server list")
                    return Paths.get(dataPath, "server_list.bin").pathString
                }
            }

            // Priority 3: Try external app directory if available
            val externalAppPath = DownloadService.baseExternalAppDirPath
            if (externalAppPath.isNotBlank()) {
                val extDir = File(externalAppPath)
                if (extDir.exists() && extDir.canWrite()) {
                    Timber.w("Using external app directory for server list: $externalAppPath")
                    return Paths.get(externalAppPath, "server_list.bin").pathString
                }
            }

            // Final fallback: return cache path and let error occur upstream (better than silent failure)
            Timber.e("No writable directory found for server_list.bin. Will attempt cache directory anyway.")
            return Paths.get(cachePath.takeIf { it.isNotBlank() } ?: "/cache", "server_list.bin").pathString
        }

    private val internalAppInstallPath: String
        get() = Paths.get(DownloadService.baseDataDirPath, "Steam", "steamapps", "common").pathString

    private val externalAppInstallPath: String
        get() = Paths.get(PrefManager.externalStoragePath, "Steam", "steamapps", "common").pathString

    val allInstallPaths: List<String>
        get() {
            val paths = mutableListOf(internalAppInstallPath)
            if (PrefManager.externalStoragePath.isNotBlank()) {
                paths += externalAppInstallPath
            }
            for (volPath in DownloadService.externalVolumePaths) {
                if (volPath.isNotBlank()) {
                    paths += Paths.get(volPath, "Steam", "steamapps", "common").pathString
                }
            }
            return paths.distinct()
        }

    private val internalAppStagingPath: String
        get() = Paths.get(DownloadService.baseDataDirPath, "Steam", "steamapps", "staging").pathString

    private val externalAppStagingPath: String
        get() = Paths.get(PrefManager.externalStoragePath, "Steam", "steamapps", "staging").pathString

    val defaultStoragePath: String
        get() {
            // Priority 1: External storage if configured and accessible
            val externalPath = PrefManager.externalStoragePath
            if (PrefManager.useExternalStorage && externalPath.isNotBlank() && File(externalPath).exists()) {
                Timber.i("Using external storage path: $externalPath")
                return externalPath
            }

            // Priority 2: Internal app data directory
            val internalPath = DownloadService.baseDataDirPath
            if (internalPath.isNotBlank() && File(internalPath).exists()) {
                Timber.i("Using internal storage path: $internalPath")
                return internalPath
            }

            // Priority 3: Fallback to external app files directory
            val externalAppPath = DownloadService.baseExternalAppDirPath
            if (externalAppPath.isNotBlank() && File(externalAppPath).exists()) {
                Timber.w("Falling back to external app directory: $externalAppPath")
                return externalAppPath
            }

            // Final fallback: create and return internal path if it exists but warn
            Timber.e("No valid storage paths available. Returning internal path: $internalPath")
            return internalPath.takeIf { it.isNotBlank() } ?: "/data/data"
        }

    val defaultAppInstallPath: String
        get() {
            return if (PrefManager.useExternalStorage && File(PrefManager.externalStoragePath).exists()) {
                Timber.i("Using external storage")
                Timber.i("install path for external storage is $externalAppInstallPath")
                externalAppInstallPath
            } else {
                Timber.i("Using internal storage")
                internalAppInstallPath
            }
        }

    val defaultAppStagingPath: String
        get() = if (PrefManager.useExternalStorage) externalAppStagingPath else internalAppStagingPath

    fun getAppDirName(app: SteamApp?): String {
        val appName = app?.config?.installDir.orEmpty()
        return appName.ifEmpty { app?.name.orEmpty() }
    }

    fun resolveExistingAppDir(installPaths: List<String>, names: List<String>): String? {
        var firstExisting: String? = null
        for (basePath in installPaths) {
            for (name in names) {
                if (name.isEmpty()) continue
                val path = Paths.get(basePath, name)
                if (Files.isDirectory(path)) {
                    if (StorageManager.hasMarker(path.pathString, Marker.DOWNLOAD_COMPLETE_MARKER)) {
                        return path.pathString
                    }
                    if (firstExisting == null) firstExisting = path.pathString
                }
            }
        }
        return firstExisting
    }

    fun getAppDirPath(gameId: Int): String {
        val info = SteamService.getAppInfoOf(gameId)
        val appName = getAppDirName(info)
        val oldName = info?.name.orEmpty()
        val names = if (oldName.isNotEmpty() && oldName != appName) listOf(appName, oldName) else listOf(appName)

        val resolved = resolveExistingAppDir(allInstallPaths, names)
        if (resolved != null) return resolved

        return Paths.get(
            if (PrefManager.useExternalStorage) externalAppInstallPath else internalAppInstallPath,
            appName,
        ).pathString
    }
}
