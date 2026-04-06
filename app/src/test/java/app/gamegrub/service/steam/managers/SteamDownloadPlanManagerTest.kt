package app.gamegrub.service.steam.managers

import app.gamegrub.data.DepotInfo
import app.gamegrub.data.ManifestInfo
import app.gamegrub.data.SteamApp
import app.gamegrub.enums.OS
import app.gamegrub.enums.OSArch
import app.gamegrub.service.steam.SteamService
import java.util.EnumSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SteamDownloadPlanManagerTest {
    @Test
    fun buildPlan_selectsMainAndDlcDepots_andComputesDownloadingIds() {
        val appId = 1
        val selectedDlcId = 99
        val mainDepots = mapOf(
            10 to depot(10, SteamService.INVALID_APP_ID),
            11 to depot(11, selectedDlcId),
        )
        val downloadableDepots = mainDepots + mapOf(
            20 to depot(20, selectedDlcId),
        )

        val plan = SteamDownloadPlanManager.buildPlan(
            appId = appId,
            downloadableDepots = downloadableDepots,
            userSelectedDlcAppIds = listOf(selectedDlcId),
            mainDepots = mainDepots,
            downloadableDlcApps = listOf(SteamApp(id = selectedDlcId)),
            installedDownloadedDepots = null,
            isUpdateOrVerify = false,
            invalidAppId = SteamService.INVALID_APP_ID,
            initialMainAppDlcIds = emptyList(),
        )

        assertEquals(setOf(10, 11, 20), plan.selectedDepots.keys)
        assertEquals(listOf(selectedDlcId, appId), plan.downloadingAppIds)
        assertEquals(listOf(selectedDlcId), plan.calculatedDlcAppIds)
    }

    @Test
    fun buildPlan_excludesInstalledMainDepots_whenNotUpdateOrVerify() {
        val appId = 1
        val mainDepots = mapOf(
            10 to depot(10, SteamService.INVALID_APP_ID),
            11 to depot(11, SteamService.INVALID_APP_ID),
        )

        val plan = SteamDownloadPlanManager.buildPlan(
            appId = appId,
            downloadableDepots = mainDepots,
            userSelectedDlcAppIds = emptyList(),
            mainDepots = mainDepots,
            downloadableDlcApps = emptyList(),
            installedDownloadedDepots = listOf(10),
            isUpdateOrVerify = false,
            invalidAppId = SteamService.INVALID_APP_ID,
            initialMainAppDlcIds = emptyList(),
        )

        assertEquals(setOf(11), plan.mainAppDepots.keys)
        assertEquals(setOf(11), plan.selectedDepots.keys)
    }

    @Test
    fun buildPlan_mainOnlyFallback_whenNoDlcDepots() {
        val appId = 1
        val selectedDlcId = 99
        val mainDepots = mapOf(
            10 to depot(10, SteamService.INVALID_APP_ID),
            11 to depot(11, selectedDlcId),
        )

        val plan = SteamDownloadPlanManager.buildPlan(
            appId = appId,
            downloadableDepots = mainDepots,
            userSelectedDlcAppIds = listOf(selectedDlcId),
            mainDepots = mainDepots,
            downloadableDlcApps = emptyList(),
            installedDownloadedDepots = null,
            isUpdateOrVerify = false,
            invalidAppId = SteamService.INVALID_APP_ID,
            initialMainAppDlcIds = mutableListOf(123),
        )

        assertEquals(listOf(appId), plan.downloadingAppIds)
        assertTrue(plan.calculatedDlcAppIds.isEmpty())
        assertEquals(listOf(123, selectedDlcId), plan.mainAppDlcIds)
    }

    @Test
    fun buildPlan_returnsEmptySelectedDepots_whenNoEligibleDepots() {
        val appId = 1
        val depotWithoutManifest = depot(
            depotId = 10,
            dlcAppId = 99,
            hasManifest = false,
        )

        val plan = SteamDownloadPlanManager.buildPlan(
            appId = appId,
            downloadableDepots = mapOf(10 to depotWithoutManifest),
            userSelectedDlcAppIds = listOf(99),
            mainDepots = emptyMap(),
            downloadableDlcApps = emptyList(),
            installedDownloadedDepots = null,
            isUpdateOrVerify = false,
            invalidAppId = SteamService.INVALID_APP_ID,
            initialMainAppDlcIds = emptyList(),
        )

        assertTrue(plan.selectedDepots.isEmpty())
    }

    private fun depot(
        depotId: Int,
        dlcAppId: Int,
        hasManifest: Boolean = true,
    ): DepotInfo {
        val manifests = if (hasManifest) {
            mapOf(
                "public" to ManifestInfo(
                    name = "public",
                    gid = 1L,
                    size = 10L,
                    download = 10L,
                ),
            )
        } else {
            emptyMap()
        }

        return DepotInfo(
            depotId = depotId,
            dlcAppId = dlcAppId,
            depotFromApp = 1,
            sharedInstall = false,
            osList = EnumSet.of(OS.windows),
            osArch = OSArch.Unknown,
            manifests = manifests,
            encryptedManifests = emptyMap(),
            language = "english",
        )
    }
}
