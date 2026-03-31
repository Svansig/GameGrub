package app.gamegrub.service.steam.domain

import app.gamegrub.GameGrubApp
import app.gamegrub.data.DepotInfo
import app.gamegrub.data.DownloadInfo
import app.gamegrub.data.ManifestInfo
import app.gamegrub.data.SteamApp
import app.gamegrub.data.SteamControllerConfigDetail
import app.gamegrub.events.AndroidEvent
import app.gamegrub.service.steam.managers.SteamInputManager
import app.gamegrub.service.steam.managers.SteamInstalledExeManager
import `in`.dragonbra.javasteam.types.FileData
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SteamInstallDomain @Inject constructor(
    val inputManager: SteamInputManager,
    val libraryDomain: SteamLibraryDomain,
) {
    val downloadJobs = ConcurrentHashMap<Int, DownloadInfo>()

    fun getAppDownloadInfo(appId: Int): DownloadInfo? = downloadJobs[appId]

    private fun notifyDownloadStarted(appId: Int) {
        GameGrubApp.events.emit(AndroidEvent.DownloadStatusChanged(appId, true))
    }

    private fun notifyDownloadStopped(appId: Int) {
        GameGrubApp.events.emit(AndroidEvent.DownloadStatusChanged(appId, false))
    }

    fun removeDownloadJob(appId: Int) {
        val removed = downloadJobs.remove(appId)
        if (removed != null) {
            notifyDownloadStopped(appId)
        }
    }

    suspend fun completeAppDownload(
        downloadInfo: DownloadInfo,
        downloadingAppId: Int,
        entitledDepotIds: List<Int>,
        selectedDlcAppIds: List<Int>,
        appDirPath: String,
    ) {
        libraryDomain.upsertInstalledAppDownloadState(
            appId = downloadingAppId,
            entitledDepotIds = entitledDepotIds,
            selectedDlcAppIds = selectedDlcAppIds,
        )

        downloadInfo.downloadingAppIds.removeIf { it == downloadingAppId }

        if (downloadInfo.downloadingAppIds.isEmpty()) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                app.gamegrub.utils.storage.MarkerUtils.addMarker(appDirPath, app.gamegrub.enums.Marker.DOWNLOAD_COMPLETE_MARKER)
                app.gamegrub.utils.storage.MarkerUtils.removeMarker(appDirPath, app.gamegrub.enums.Marker.STEAM_DLL_REPLACED)
                app.gamegrub.utils.storage.MarkerUtils.removeMarker(appDirPath, app.gamegrub.enums.Marker.STEAM_COLDCLIENT_USED)
            }

            libraryDomain.deleteDownloadingApp(downloadInfo.gameId)
            GameGrubApp.events.emit(AndroidEvent.LibraryInstallStatusChanged(downloadInfo.gameId))
            downloadInfo.clearPersistedBytesDownloaded(appDirPath)
        }
    }
    fun buildDownloadPlan(
        appId: Int,
        downloadableDepots: Map<Int, DepotInfo>,
        userSelectedDlcAppIds: List<Int>,
        mainDepots: Map<Int, DepotInfo>,
        downloadableDlcApps: List<SteamApp>,
        installedDownloadedDepots: List<Int>?,
        isUpdateOrVerify: Boolean,
        invalidAppId: Int,
        initialMainAppDlcIds: List<Int>,
    ) = app.gamegrub.service.steam.managers.SteamInstallManager.buildDownloadPlan(
        appId = appId,
        downloadableDepots = downloadableDepots,
        userSelectedDlcAppIds = userSelectedDlcAppIds,
        mainDepots = mainDepots,
        downloadableDlcApps = downloadableDlcApps,
        installedDownloadedDepots = installedDownloadedDepots,
        isUpdateOrVerify = isUpdateOrVerify,
        invalidAppId = invalidAppId,
        initialMainAppDlcIds = initialMainAppDlcIds,
    )

    fun choosePrimaryExe(files: List<FileData>?, gameName: String): FileData? =
        app.gamegrub.service.steam.managers.SteamInstallManager.choosePrimaryExe(files, gameName)

    fun hasExecutableFlag(flags: Any): Boolean = app.gamegrub.service.steam.managers.SteamInstallManager.hasExecutableFlag(flags)

    fun isStub(file: FileData): Boolean = app.gamegrub.service.steam.managers.SteamInstallManager.isStub(file)

    fun <T> resolveInstalledExe(
        appInfo: SteamApp?,
        canQueryManifests: Boolean,
        fallbackExecutable: () -> String,
        loadManifestCandidates: (depotId: Int, manifest: ManifestInfo) -> List<SteamInstalledExeManager.ExecutableCandidate<T>>?,
        choosePrimary: (List<SteamInstalledExeManager.ExecutableCandidate<T>>, String) -> SteamInstalledExeManager.ExecutableCandidate<T>?,
    ): String = app.gamegrub.service.steam.managers.SteamInstallManager.resolveInstalledExe(
        appInfo = appInfo,
        canQueryManifests = canQueryManifests,
        fallbackExecutable = fallbackExecutable,
        loadManifestCandidates = loadManifestCandidates,
        choosePrimary = choosePrimary,
    )

    fun selectSteamControllerConfig(details: List<SteamControllerConfigDetail>): SteamControllerConfigDetail? =
        inputManager.selectSteamControllerConfig(details)

    fun resolveSteamInputManifestFile(appDirPath: String, manifestPath: String): File? =
        inputManager.resolveSteamInputManifestFile(appDirPath, manifestPath)

    fun loadConfigFromManifest(manifestFile: File): String? =
        inputManager.loadConfigFromManifest(manifestFile)

    fun routeFor(templateIndex: Int): app.gamegrub.service.steam.managers.SteamControllerTemplateRoutingManager.TemplateRoute =
        inputManager.routeFor(templateIndex)

    fun requiresWorkshopDownload(templateIndex: Int): Boolean =
        inputManager.requiresWorkshopDownload(templateIndex)

    fun tryDownloadWorkshopControllerConfig(
        templateIndex: Int,
        configDetails: List<SteamControllerConfigDetail>,
        appDirPath: String,
        configFileName: String,
        fetchPublishedFileDetailsJson: (Long) -> String?,
        downloadFileBytes: (String) -> ByteArray?,
    ): Boolean = inputManager.tryDownloadWorkshopControllerConfig(
        templateIndex = templateIndex,
        configDetails = configDetails,
        appDirPath = appDirPath,
        configFileName = configFileName,
        fetchPublishedFileDetailsJson = fetchPublishedFileDetailsJson,
        downloadFileBytes = downloadFileBytes,
    )
}
