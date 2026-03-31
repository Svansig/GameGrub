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

class SteamDlcDepotManagerTest {
    @Test
    fun resolveMainAppDlcIdsWithoutProperDepotDlcIds_returnsEmpty_whenAppInfoMissing() {
        val result = SteamDlcDepotManager.resolveMainAppDlcIdsWithoutProperDepotDlcIds(
            appInfo = null,
            hiddenDlcAppIds = listOf(42),
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun resolveMainAppDlcIdsWithoutProperDepotDlcIds_returnsHiddenPlaceholderDlcId() {
        val dlcAppId = 42
        val appInfo = createApp(
            mapOf(
                100 to createDepot(
                    depotId = 100,
                    dlcAppId = dlcAppId,
                    osList = EnumSet.of(OS.none),
                    manifests = emptyMap(),
                ),
            ),
        )

        val result = SteamDlcDepotManager.resolveMainAppDlcIdsWithoutProperDepotDlcIds(
            appInfo = appInfo,
            hiddenDlcAppIds = listOf(dlcAppId),
        )

        assertEquals(listOf(dlcAppId), result)
    }

    @Test
    fun resolveMainAppDlcIdsWithoutProperDepotDlcIds_returnsEmpty_whenDlcHasMultipleDepots() {
        val dlcAppId = 42
        val appInfo = createApp(
            mapOf(
                100 to createDepot(100, dlcAppId, EnumSet.of(OS.none), emptyMap()),
                101 to createDepot(101, dlcAppId, EnumSet.of(OS.none), emptyMap()),
            ),
        )

        val result = SteamDlcDepotManager.resolveMainAppDlcIdsWithoutProperDepotDlcIds(
            appInfo = appInfo,
            hiddenDlcAppIds = listOf(dlcAppId),
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun resolveMainAppDlcIdsWithoutProperDepotDlcIds_returnsEmpty_whenDepotNotPlaceholderOrNotHidden() {
        val dlcAppId = 99
        val appInfo = createApp(
            mapOf(
                100 to createDepot(
                    depotId = 100,
                    dlcAppId = dlcAppId,
                    osList = EnumSet.of(OS.windows),
                    manifests = mapOf(
                        "public" to ManifestInfo(
                            name = "public",
                            gid = 1L,
                            size = 10L,
                            download = 10L,
                        ),
                    ),
                ),
            ),
        )

        val result = SteamDlcDepotManager.resolveMainAppDlcIdsWithoutProperDepotDlcIds(
            appInfo = appInfo,
            hiddenDlcAppIds = emptyList(),
        )

        assertTrue(result.isEmpty())
    }

    private fun createApp(depots: Map<Int, DepotInfo>): SteamApp {
        return SteamApp(
            id = 1,
            depots = depots,
            name = "Test Game",
        )
    }

    private fun createDepot(
        depotId: Int,
        dlcAppId: Int,
        osList: EnumSet<OS>,
        manifests: Map<String, ManifestInfo>,
    ): DepotInfo {
        return DepotInfo(
            depotId = depotId,
            dlcAppId = dlcAppId,
            depotFromApp = 1,
            sharedInstall = false,
            osList = osList,
            osArch = OSArch.Unknown,
            manifests = manifests,
            encryptedManifests = emptyMap(),
            language = "english",
            optionalDlcId = SteamService.INVALID_APP_ID,
        )
    }
}

