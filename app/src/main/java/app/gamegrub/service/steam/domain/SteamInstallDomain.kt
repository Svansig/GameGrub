package app.gamegrub.service.steam.domain

import app.gamegrub.data.DepotInfo
import app.gamegrub.data.ManifestInfo
import app.gamegrub.data.SteamApp
import app.gamegrub.service.steam.managers.SteamCatalogManager
import app.gamegrub.service.steam.managers.SteamInstallManager
import app.gamegrub.service.steam.managers.SteamInputManager
import `in`.dragonbra.javasteam.types.FileData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SteamInstallDomain @Inject constructor(
    val catalogManager: SteamCatalogManager,
    val inputManager: SteamInputManager,
) {
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
    ) = SteamInstallManager.buildDownloadPlan(
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
        SteamInstallManager.choosePrimaryExe(files, gameName)

    fun hasExecutableFlag(flags: Any): Boolean = SteamInstallManager.hasExecutableFlag(flags)

    fun isStub(file: FileData): Boolean = SteamInstallManager.isStub(file)

    fun <T> resolveInstalledExe(
        appInfo: SteamApp?,
        canQueryManifests: Boolean,
        fallbackExecutable: () -> String,
        loadManifestCandidates: (depotId: Int, manifest: ManifestInfo) -> List<app.gamegrub.service.steam.managers.SteamInstalledExeManager.ExecutableCandidate<T>>?,
        choosePrimary: (List<app.gamegrub.service.steam.managers.SteamInstalledExeManager.ExecutableCandidate<T>>, String) -> app.gamegrub.service.steam.managers.SteamInstalledExeManager.ExecutableCandidate<T>?,
    ): String = SteamInstallManager.resolveInstalledExe(
        appInfo = appInfo,
        canQueryManifests = canQueryManifests,
        fallbackExecutable = fallbackExecutable,
        loadManifestCandidates = loadManifestCandidates,
        choosePrimary = choosePrimary,
    )
}