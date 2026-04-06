package app.gamegrub.service.steam.managers

import app.gamegrub.data.DepotInfo
import app.gamegrub.data.ManifestInfo
import app.gamegrub.enums.OS
import app.gamegrub.enums.OSArch
import app.gamegrub.service.steam.SteamService
import java.util.EnumSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SteamDomainManagersSmokeTest {
    @Test
    fun catalogManager_filterForDownloadableDepots_allowsBasicWindowsDepot() {
        val depot = depot(10, SteamService.INVALID_APP_ID)

        val allowed = SteamCatalogManager.filterForDownloadableDepots(
            depot = depot,
            has64Bit = false,
            preferredLanguage = "english",
            ownedDlc = emptyMap(),
        )

        assertTrue(allowed)
    }

    @Test
    fun installManager_buildDownloadPlan_includesMainAppIdWhenMainDepotsExist() {
        val mainDepots = mapOf(10 to depot(10, SteamService.INVALID_APP_ID))
        val plan = SteamInstallManager.buildDownloadPlan(
            appId = 1,
            downloadableDepots = mainDepots,
            userSelectedDlcAppIds = emptyList(),
            mainDepots = mainDepots,
            downloadableDlcApps = emptyList(),
            installedDownloadedDepots = null,
            isUpdateOrVerify = false,
            invalidAppId = SteamService.INVALID_APP_ID,
            initialMainAppDlcIds = emptyList(),
        )

        assertEquals(listOf(1), plan.downloadingAppIds)
    }

    @Test
    fun inputManager_routeFor_mapsKnownTemplateIndex() {
        val route = SteamInputManager.routeFor(13)
        assertEquals(SteamControllerTemplateRoutingManager.TemplateSource.Manifest, route.source)
    }

    @Test
    fun installManager_buildDownloadPlan_handlesDifferentDepotShape() {
        val mainDepots = mapOf(22 to depot(22, 55))

        val plan = SteamInstallManager.buildDownloadPlan(
            appId = 7,
            downloadableDepots = mainDepots,
            userSelectedDlcAppIds = listOf(55),
            mainDepots = mainDepots,
            downloadableDlcApps = emptyList(),
            installedDownloadedDepots = null,
            isUpdateOrVerify = false,
            invalidAppId = SteamService.INVALID_APP_ID,
            initialMainAppDlcIds = emptyList(),
        )

        assertEquals(listOf(7), plan.downloadingAppIds)
    }

    private fun depot(depotId: Int, dlcAppId: Int): DepotInfo {
        return DepotInfo(
            depotId = depotId,
            dlcAppId = dlcAppId,
            depotFromApp = 1,
            sharedInstall = false,
            osList = EnumSet.of(OS.windows),
            osArch = OSArch.Unknown,
            manifests = mapOf(
                "public" to ManifestInfo(name = "public", gid = 1L, size = 10L, download = 10L),
            ),
            encryptedManifests = emptyMap(),
            language = "english",
        )
    }
}
