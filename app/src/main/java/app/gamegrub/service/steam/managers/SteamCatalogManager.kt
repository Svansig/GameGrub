package app.gamegrub.service.steam.managers

import app.gamegrub.data.DepotInfo
import app.gamegrub.data.SteamApp
import `in`.dragonbra.javasteam.steam.handlers.steamapps.PICSRequest

/**
 * High-level catalog domain manager: ownership, DLC shaping, and depot selection.
 */
object SteamCatalogManager {
    suspend fun filterOwnedAppDlc(
        appDlcDepots: Map<Int, DepotInfo>,
        invalidAppId: Int,
        ownedGameIds: Set<Int>,
        hasLicense: suspend (Int) -> Boolean,
        hasPicsApp: suspend (Int) -> Boolean,
    ): Map<Int, DepotInfo> = SteamDlcOwnershipManager.filterOwnedAppDlc(
        appDlcDepots = appDlcDepots,
        invalidAppId = invalidAppId,
        ownedGameIds = ownedGameIds,
        hasLicense = hasLicense,
        hasPicsApp = hasPicsApp,
    )

    fun resolveMainAppDlcIdsWithoutProperDepotDlcIds(
        appInfo: SteamApp?,
        hiddenDlcAppIds: List<Int>,
    ): MutableList<Int> = SteamDlcDepotManager.resolveMainAppDlcIdsWithoutProperDepotDlcIds(
        appInfo = appInfo,
        hiddenDlcAppIds = hiddenDlcAppIds,
    )

    fun filterForDownloadableDepots(
        depot: DepotInfo,
        has64Bit: Boolean,
        preferredLanguage: String,
        ownedDlc: Map<Int, DepotInfo>?,
    ): Boolean = SteamLaunchConfigManager.filterForDownloadableDepots(
        depot = depot,
        has64Bit = has64Bit,
        preferredLanguage = preferredLanguage,
        ownedDlc = ownedDlc,
    )

    fun getMainAppDepots(
        appInfo: SteamApp?,
        containerLanguage: String,
        ownedDlc: Map<Int, DepotInfo>,
    ): Map<Int, DepotInfo> = SteamDepotSelectionManager.getMainAppDepots(
        appInfo = appInfo,
        containerLanguage = containerLanguage,
        ownedDlc = ownedDlc,
        isDownloadableDepot = ::filterForDownloadableDepots,
    )

    fun getDownloadableDepots(
        appInfo: SteamApp?,
        preferredLanguage: String,
        ownedDlc: Map<Int, DepotInfo>,
        indirectDlcApps: List<SteamApp>,
    ): Map<Int, DepotInfo> = SteamDepotSelectionManager.getDownloadableDepots(
        appInfo = appInfo,
        preferredLanguage = preferredLanguage,
        ownedDlc = ownedDlc,
        indirectDlcApps = indirectDlcApps,
        isDownloadableDepot = ::filterForDownloadableDepots,
    )

    suspend fun checkDlcOwnershipViaPICSBatch(
        dlcAppIds: Set<Int>,
        loadAccessTokens: suspend (List<Int>) -> Map<Int, Long>,
        loadPicsProductInfo: suspend (List<PICSRequest>) -> Set<Int>,
    ): Set<Int> = SteamDlcOwnershipManager.checkDlcOwnershipViaPICSBatch(
        dlcAppIds = dlcAppIds,
        loadAccessTokens = loadAccessTokens,
        loadPicsProductInfo = loadPicsProductInfo,
    )
}
