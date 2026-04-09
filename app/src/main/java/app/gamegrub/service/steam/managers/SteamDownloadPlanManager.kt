package app.gamegrub.service.steam.managers

import app.gamegrub.data.DepotInfo
import app.gamegrub.data.SteamApp

/**
 * Builds deterministic depot/app-id download plans before network/download execution starts.
 */
object SteamDownloadPlanManager {
    data class DownloadPlan(
        val mainAppDepots: Map<Int, DepotInfo>,
        val dlcAppDepots: Map<Int, DepotInfo>,
        val selectedDepots: Map<Int, DepotInfo>,
        val downloadingAppIds: List<Int>,
        val calculatedDlcAppIds: List<Int>,
        val mainAppDlcIds: List<Int>,
    )

    fun buildPlan(
        appId: Int,
        downloadableDepots: Map<Int, DepotInfo>,
        userSelectedDlcAppIds: List<Int>,
        mainDepots: Map<Int, DepotInfo>,
        downloadableDlcApps: List<SteamApp>,
        installedDownloadedDepots: List<Int>?,
        isUpdateOrVerify: Boolean,
        invalidAppId: Int,
        initialMainAppDlcIds: List<Int>,
    ): DownloadPlan {
        var mainAppDepots = mainDepots.filter { (_, depot) ->
            depot.dlcAppId == invalidAppId
        } + mainDepots.filter { (_, depot) ->
            userSelectedDlcAppIds.contains(depot.dlcAppId) && depot.manifests.isNotEmpty()
        }

        val dlcAppDepots = downloadableDepots.filter { (_, depot) ->
            !mainAppDepots.keys.contains(depot.depotId) &&
                    userSelectedDlcAppIds.contains(depot.dlcAppId) &&
                    downloadableDlcApps.any { it.id == depot.dlcAppId } &&
                    depot.manifests.isNotEmpty()
        }

        if (installedDownloadedDepots != null && !isUpdateOrVerify) {
            mainAppDepots = mainAppDepots.filter { it.key !in installedDownloadedDepots }
        }

        val selectedDepots = mainAppDepots + dlcAppDepots

        val downloadingAppIds = mutableListOf<Int>()
        val calculatedDlcAppIds = mutableListOf<Int>()

        userSelectedDlcAppIds.forEach { dlcAppId ->
            if (dlcAppDepots.any { (_, depot) -> depot.dlcAppId == dlcAppId }) {
                downloadingAppIds.add(dlcAppId)
                calculatedDlcAppIds.add(dlcAppId)
            }
        }

        if (mainAppDepots.isNotEmpty()) {
            downloadingAppIds.add(appId)
        }

        val mainAppDlcIds = initialMainAppDlcIds.toMutableList()
        if (dlcAppDepots.isEmpty()) {
            mainAppDlcIds.addAll(
                mainAppDepots
                    .filter { it.value.dlcAppId != invalidAppId }
                    .map { it.value.dlcAppId }
                    .distinct(),
            )

            calculatedDlcAppIds.clear()
            downloadingAppIds.clear()
            downloadingAppIds.add(appId)
        }

        return DownloadPlan(
            mainAppDepots = mainAppDepots,
            dlcAppDepots = dlcAppDepots,
            selectedDepots = selectedDepots,
            downloadingAppIds = downloadingAppIds,
            calculatedDlcAppIds = calculatedDlcAppIds,
            mainAppDlcIds = mainAppDlcIds,
        )
    }
}
