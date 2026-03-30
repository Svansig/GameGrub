package app.gamegrub.service.steam.managers

import app.gamegrub.data.DepotInfo
import app.gamegrub.data.ManifestInfo
import app.gamegrub.data.SteamApp
import app.gamegrub.enums.OS
import app.gamegrub.enums.OSArch
import app.gamegrub.service.steam.SteamService
import java.util.EnumSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SteamDepotSelectionManagerTest {
    @Test
    fun getMainAppDepots_passesHas64BitAndFilters() {
        val appInfo = createApp(
            appId = 1,
            depots = mapOf(
                10 to createDepot(depotId = 10, osArch = OSArch.Arch64),
                11 to createDepot(depotId = 11, osArch = OSArch.Arch32),
            ),
        )
        val has64BitArgs = mutableListOf<Boolean>()

        val result = SteamDepotSelectionManager.getMainAppDepots(
            appInfo = appInfo,
            containerLanguage = "english",
            ownedDlc = emptyMap(),
            isDownloadableDepot = { depot, has64Bit, _, _ ->
                has64BitArgs.add(has64Bit)
                depot.depotId == 10
            },
        )

        assertEquals(setOf(10), result.keys)
        assertTrue(has64BitArgs.isNotEmpty())
        assertTrue(has64BitArgs.all { it })
    }

    @Test
    fun getDownloadableDepots_mergesIndirectDlcAndRewritesDlcAppId() {
        val mainApp = createApp(
            appId = 1,
            depots = mapOf(10 to createDepot(depotId = 10, dlcAppId = SteamService.INVALID_APP_ID)),
        )
        val dlcApp = createApp(
            appId = 99,
            depots = mapOf(50 to createDepot(depotId = 50, dlcAppId = SteamService.INVALID_APP_ID)),
        )

        val result = SteamDepotSelectionManager.getDownloadableDepots(
            appInfo = mainApp,
            preferredLanguage = "english",
            ownedDlc = emptyMap(),
            indirectDlcApps = listOf(dlcApp),
            isDownloadableDepot = { _, _, _, _ -> true },
        )

        assertEquals(setOf(10, 50), result.keys)
        assertEquals(99, result[50]?.dlcAppId)
    }

    @Test
    fun getDownloadableDepots_passesOwnedDlcForMainAndNullForIndirect() {
        val mainDepot = createDepot(depotId = 10)
        val dlcDepot = createDepot(depotId = 50)
        val mainApp = createApp(appId = 1, depots = mapOf(mainDepot.depotId to mainDepot))
        val dlcApp = createApp(appId = 99, depots = mapOf(dlcDepot.depotId to dlcDepot))
        val ownedDlc = mapOf(mainDepot.depotId to mainDepot)
        val seenOwnedDlcByDepot = mutableMapOf<Int, Map<Int, DepotInfo>?>()

        SteamDepotSelectionManager.getDownloadableDepots(
            appInfo = mainApp,
            preferredLanguage = "english",
            ownedDlc = ownedDlc,
            indirectDlcApps = listOf(dlcApp),
            isDownloadableDepot = { depot, _, _, ownedMap ->
                seenOwnedDlcByDepot[depot.depotId] = ownedMap
                true
            },
        )

        assertNotNull(seenOwnedDlcByDepot[10])
        assertEquals(ownedDlc, seenOwnedDlcByDepot[10])
        assertNull(seenOwnedDlcByDepot[50])
    }

    private fun createApp(appId: Int, depots: Map<Int, DepotInfo>): SteamApp {
        return SteamApp(
            id = appId,
            depots = depots,
            name = "Test App $appId",
        )
    }

    private fun createDepot(
        depotId: Int,
        dlcAppId: Int = SteamService.INVALID_APP_ID,
        osArch: OSArch = OSArch.Unknown,
    ): DepotInfo {
        return DepotInfo(
            depotId = depotId,
            dlcAppId = dlcAppId,
            depotFromApp = 1,
            sharedInstall = false,
            osList = EnumSet.of(OS.windows),
            osArch = osArch,
            manifests = mapOf(
                "public" to ManifestInfo(
                    name = "public",
                    gid = 1L,
                    size = 2L,
                    download = 2L,
                ),
            ),
            encryptedManifests = emptyMap(),
            language = "english",
        )
    }
}

