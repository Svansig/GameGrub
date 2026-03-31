package app.gamegrub.service.steam.managers

import app.gamegrub.data.DepotInfo
import `in`.dragonbra.javasteam.steam.handlers.steamapps.PICSRequest
import timber.log.Timber

/**
 * Encapsulates DLC ownership heuristics used when deciding which DLC depots are downloadable.
 */
object SteamDlcOwnershipManager {
    suspend fun filterOwnedAppDlc(
        appDlcDepots: Map<Int, DepotInfo>,
        invalidAppId: Int,
        ownedGameIds: Set<Int>,
        hasLicense: suspend (Int) -> Boolean,
        hasPicsApp: suspend (Int) -> Boolean,
    ): Map<Int, DepotInfo> {
        return appDlcDepots.filter { (_, depot) ->
            when {
                // Base-game depots always download.
                depot.dlcAppId == invalidAppId -> true

                hasLicense(depot.dlcAppId) -> true

                hasPicsApp(depot.dlcAppId) -> true

                depot.dlcAppId in ownedGameIds -> true

                else -> false
            }
        }
    }

    suspend fun checkDlcOwnershipViaPICSBatch(
        dlcAppIds: Set<Int>,
        loadAccessTokens: suspend (List<Int>) -> Map<Int, Long>,
        loadPicsProductInfo: suspend (List<PICSRequest>) -> Set<Int>,
        chunkSize: Int = 100,
    ): Set<Int> {
        if (dlcAppIds.isEmpty()) {
            return emptySet()
        }

        return try {
            val tokens = loadAccessTokens(dlcAppIds.toList())
            val ownedAppIds = tokens.keys.filter { it in dlcAppIds }.toSet()
            if (ownedAppIds.isEmpty()) {
                Timber.w("No owned DLCs found via access tokens")
                return emptySet()
            }

            val picsRequests = ownedAppIds.mapNotNull { appId ->
                val token = tokens[appId] ?: return@mapNotNull null
                PICSRequest(id = appId, accessToken = token)
            }
            if (picsRequests.isEmpty()) {
                return emptySet()
            }

            val ownedFromPics = mutableSetOf<Int>()
            picsRequests.chunked(chunkSize).forEach { chunk ->
                ownedFromPics.addAll(loadPicsProductInfo(chunk))
            }

            ownedFromPics
        } catch (e: Exception) {
            Timber.e(e, "Failed to check DLC ownership via PICS batch for ${dlcAppIds.size} appIds")
            emptySet()
        }
    }
}
