package app.gamegrub.service.steam.managers

import app.gamegrub.data.SteamApp
import app.gamegrub.enums.OS
import app.gamegrub.service.steam.SteamService

/**
 * Encapsulates heuristics for detecting DLC app IDs represented as placeholder depots
 * in a main app's depot list.
 */
object SteamDlcDepotManager {
    fun resolveMainAppDlcIdsWithoutProperDepotDlcIds(
        appInfo: SteamApp?,
        hiddenDlcAppIds: List<Int>,
    ): MutableList<Int> {
        if (appInfo == null) {
            return mutableListOf()
        }

        val mainAppDlcIds = mutableListOf<Int>()
        val checkingAppDlcIds = appInfo.depots
            .filter { it.value.dlcAppId != SteamService.INVALID_APP_ID }
            .map { it.value.dlcAppId }
            .distinct()

        checkingAppDlcIds.forEach { checkingDlcId ->
            val checkMap = appInfo.depots.filter { it.value.dlcAppId == checkingDlcId }
            if (checkMap.size == 1) {
                val depotInfo = checkMap[checkMap.keys.first()] ?: return@forEach
                if (depotInfo.osList.contains(OS.none) &&
                    depotInfo.manifests.isEmpty() &&
                    hiddenDlcAppIds.isNotEmpty() &&
                    hiddenDlcAppIds.contains(checkingDlcId)
                ) {
                    mainAppDlcIds.add(checkingDlcId)
                }
            }
        }

        return mainAppDlcIds
    }
}
