package app.gamegrub.service.steam

import app.gamegrub.PrefManager
import app.gamegrub.data.SteamApp
import app.gamegrub.enums.Marker
import app.gamegrub.service.DownloadService
import app.gamegrub.utils.MarkerUtils
import timber.log.Timber
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.pathString

object SteamPaths {

    val serverListPath: String
        get() = Paths.get(DownloadService.baseCacheDirPath, "server_list.bin").pathString

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
            return if (PrefManager.useExternalStorage && File(PrefManager.externalStoragePath).exists()) {
                Timber.i("External storage path is ${PrefManager.externalStoragePath}")
                PrefManager.externalStoragePath
            } else {
                if (SteamService.instance != null) {
                    return DownloadService.baseDataDirPath
                }
                ""
            }
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
                    if (MarkerUtils.hasMarker(path.pathString, Marker.DOWNLOAD_COMPLETE_MARKER)) {
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
