package app.gamegrub.service.steam.managers

import app.gamegrub.data.DepotInfo
import app.gamegrub.data.SteamApp
import app.gamegrub.enums.OSArch

/**
 * Encapsulates rules that build downloadable depot maps for base apps and DLC apps.
 */
object SteamDepotSelectionManager {
    fun getMainAppDepots(
        appInfo: SteamApp?,
        containerLanguage: String,
        ownedDlc: Map<Int, DepotInfo>,
        isDownloadableDepot: (DepotInfo, Boolean, String, Map<Int, DepotInfo>?) -> Boolean,
    ): Map<Int, DepotInfo> {
        if (appInfo == null) {
            return emptyMap()
        }

        val has64Bit = appInfo.depots.values.any { it.osArch == OSArch.Arch64 }
        return appInfo.depots
            .asSequence()
            .filter { (_, depot) ->
                isDownloadableDepot(depot, has64Bit, containerLanguage, ownedDlc)
            }
            .associate { it.toPair() }
    }

    fun getDownloadableDepots(
        appInfo: SteamApp?,
        preferredLanguage: String,
        ownedDlc: Map<Int, DepotInfo>,
        indirectDlcApps: List<SteamApp>,
        isDownloadableDepot: (DepotInfo, Boolean, String, Map<Int, DepotInfo>?) -> Boolean,
    ): Map<Int, DepotInfo> {
        if (appInfo == null) {
            return emptyMap()
        }

        val has64Bit = appInfo.depots.values.any { it.osArch == OSArch.Arch64 }

        val map = appInfo.depots
            .asSequence()
            .filter { (_, depot) ->
                isDownloadableDepot(depot, has64Bit, preferredLanguage, ownedDlc)
            }
            .associate { it.toPair() }
            .toMutableMap()

        indirectDlcApps.forEach { dlcApp ->
            dlcApp.depots
                .asSequence()
                .filter { (_, depot) ->
                    isDownloadableDepot(depot, has64Bit, preferredLanguage, null)
                }
                .associate { it.toPair() }
                .forEach { (depotId, depot) ->
                    map[depotId] = DepotInfo(
                        depotId = depot.depotId,
                        dlcAppId = dlcApp.id,
                        optionalDlcId = depot.optionalDlcId,
                        depotFromApp = depot.depotFromApp,
                        sharedInstall = depot.sharedInstall,
                        osList = depot.osList,
                        osArch = depot.osArch,
                        language = depot.language,
                        manifests = depot.manifests,
                        encryptedManifests = depot.encryptedManifests,
                    )
                }
        }

        return map
    }
}

