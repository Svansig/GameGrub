package app.gamegrub.service.steam.managers

import app.gamegrub.data.DepotInfo
import app.gamegrub.data.ManifestInfo
import app.gamegrub.data.SteamApp
import `in`.dragonbra.javasteam.types.FileData

/**
 * High-level install domain manager: depot planning and executable resolution.
 */
object SteamInstallManager {
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
    ): SteamDownloadPlanManager.DownloadPlan = SteamDownloadPlanManager.buildPlan(
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

    fun choosePrimaryExe(files: List<FileData>?, gameName: String): FileData? {
        return SteamLaunchConfigManager.choosePrimaryExe(files, gameName)
    }

    fun hasExecutableFlag(flags: Any): Boolean = SteamLaunchConfigManager.hasExecutableFlag(flags)

    fun isStub(file: FileData): Boolean = SteamLaunchConfigManager.isStub(file)

    fun <T> resolveInstalledExe(
        appInfo: SteamApp?,
        canQueryManifests: Boolean,
        fallbackExecutable: () -> String,
        loadManifestCandidates: (depotId: Int, manifest: ManifestInfo) -> List<SteamInstalledExeManager.ExecutableCandidate<T>>?,
        choosePrimary: (List<SteamInstalledExeManager.ExecutableCandidate<T>>, String) -> SteamInstalledExeManager.ExecutableCandidate<T>?,
    ): String = SteamInstalledExeManager.resolveInstalledExe(
        appInfo = appInfo,
        canQueryManifests = canQueryManifests,
        fallbackExecutable = fallbackExecutable,
        loadManifestCandidates = loadManifestCandidates,
        choosePrimary = choosePrimary,
    )
}

