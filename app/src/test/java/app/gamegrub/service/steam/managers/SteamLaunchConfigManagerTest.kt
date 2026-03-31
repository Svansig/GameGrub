package app.gamegrub.service.steam.managers

import app.gamegrub.data.DepotInfo
import app.gamegrub.data.ManifestInfo
import app.gamegrub.enums.OS
import app.gamegrub.enums.OSArch
import app.gamegrub.service.steam.SteamService
import java.util.EnumSet
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SteamLaunchConfigManagerTest {
    @Test
    fun filterForDownloadableDepots_returnsFalse_forUnsupportedOs() {
        val depot = createDepot(osList = EnumSet.of(OS.linux))

        val allowed = SteamLaunchConfigManager.filterForDownloadableDepots(
            depot = depot,
            has64Bit = false,
            preferredLanguage = "english",
            ownedDlc = emptyMap(),
        )

        assertFalse(allowed)
    }

    @Test
    fun filterForDownloadableDepots_returnsFalse_forUnownedDlc() {
        val depot = createDepot(dlcAppId = 1000)

        val allowed = SteamLaunchConfigManager.filterForDownloadableDepots(
            depot = depot,
            has64Bit = false,
            preferredLanguage = "english",
            ownedDlc = emptyMap(),
        )

        assertFalse(allowed)
    }

    @Test
    fun filterForDownloadableDepots_returnsTrue_forSupportedOwnedDepot() {
        val depot = createDepot(depotId = 22, dlcAppId = 1000)

        val allowed = SteamLaunchConfigManager.filterForDownloadableDepots(
            depot = depot,
            has64Bit = false,
            preferredLanguage = "english",
            ownedDlc = mapOf(depot.depotId to depot),
        )

        assertTrue(allowed)
    }

    @Test
    fun hasExecutableFlag_returnsTrue_forBitmaskFlag() {
        assertTrue(SteamLaunchConfigManager.hasExecutableFlag(0x20))
        assertTrue(SteamLaunchConfigManager.hasExecutableFlag(0x80L))
    }

    private fun createDepot(
        depotId: Int = 10,
        dlcAppId: Int = SteamService.INVALID_APP_ID,
        osList: EnumSet<OS> = EnumSet.of(OS.windows),
    ): DepotInfo {
        return DepotInfo(
            depotId = depotId,
            dlcAppId = dlcAppId,
            depotFromApp = 1,
            sharedInstall = false,
            osList = osList,
            osArch = OSArch.Unknown,
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

