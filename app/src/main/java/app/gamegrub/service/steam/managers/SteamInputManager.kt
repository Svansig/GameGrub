package app.gamegrub.service.steam.managers

import app.gamegrub.data.SteamControllerConfigDetail
import java.io.File

/**
 * High-level Steam Input domain manager for template routing and config resolution.
 */
object SteamInputManager {
    fun selectSteamControllerConfig(details: List<SteamControllerConfigDetail>): SteamControllerConfigDetail? {
        return SteamControllerConfigManager.selectSteamControllerConfig(details)
    }

    fun resolveSteamInputManifestFile(appDirPath: String, manifestPath: String): File? {
        return SteamControllerConfigManager.resolveSteamInputManifestFile(appDirPath, manifestPath)
    }

    fun loadConfigFromManifest(manifestFile: File): String? {
        return SteamControllerConfigManager.loadConfigFromManifest(manifestFile)
    }

    fun routeFor(templateIndex: Int): SteamControllerTemplateRoutingManager.TemplateRoute {
        return SteamControllerTemplateRoutingManager.routeFor(templateIndex)
    }

    fun requiresWorkshopDownload(templateIndex: Int): Boolean {
        return SteamControllerTemplateRoutingManager.requiresWorkshopDownload(templateIndex)
    }

    fun tryDownloadWorkshopControllerConfig(
        templateIndex: Int,
        configDetails: List<SteamControllerConfigDetail>,
        appDirPath: String,
        configFileName: String,
        fetchPublishedFileDetailsJson: (Long) -> String?,
        downloadFileBytes: (String) -> ByteArray?,
    ): Boolean {
        return SteamControllerWorkshopDownloadManager.tryDownloadWorkshopControllerConfig(
            templateIndex = templateIndex,
            configDetails = configDetails,
            appDirPath = appDirPath,
            configFileName = configFileName,
            requiresWorkshopDownload = ::requiresWorkshopDownload,
            selectConfig = ::selectSteamControllerConfig,
            fetchPublishedFileDetailsJson = fetchPublishedFileDetailsJson,
            downloadFileBytes = downloadFileBytes,
        )
    }
}
