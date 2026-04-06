package app.gamegrub.service.steam.managers

import app.gamegrub.data.DepotInfo
import app.gamegrub.data.ManifestInfo
import app.gamegrub.enums.OS
import app.gamegrub.enums.OSArch
import app.gamegrub.service.steam.SteamService
import java.util.EnumSet
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SteamDlcOwnershipManagerTest {
    @Test
    fun filterOwnedAppDlc_includesDepotWhenLicenseExists() = runBlocking {
        val depot = createDepot(depotId = 10, dlcAppId = 100)
        val result = SteamDlcOwnershipManager.filterOwnedAppDlc(
            appDlcDepots = mapOf(depot.depotId to depot),
            invalidAppId = SteamService.INVALID_APP_ID,
            ownedGameIds = emptySet(),
            hasLicense = { appId -> appId == 100 },
            hasPicsApp = { false },
        )
        assertEquals(setOf(10), result.keys)
    }

    @Test
    fun filterOwnedAppDlc_includesDepotWhenPicsAppExists() = runBlocking {
        val depot = createDepot(depotId = 11, dlcAppId = 200)
        val result = SteamDlcOwnershipManager.filterOwnedAppDlc(
            appDlcDepots = mapOf(depot.depotId to depot),
            invalidAppId = SteamService.INVALID_APP_ID,
            ownedGameIds = emptySet(),
            hasLicense = { false },
            hasPicsApp = { appId -> appId == 200 },
        )
        assertEquals(setOf(11), result.keys)
    }

    @Test
    fun filterOwnedAppDlc_includesDepotWhenOwnedGamesContainsDlcAppId() = runBlocking {
        val depot = createDepot(depotId = 12, dlcAppId = 300)
        val result = SteamDlcOwnershipManager.filterOwnedAppDlc(
            appDlcDepots = mapOf(depot.depotId to depot),
            invalidAppId = SteamService.INVALID_APP_ID,
            ownedGameIds = setOf(300),
            hasLicense = { false },
            hasPicsApp = { false },
        )
        assertEquals(setOf(12), result.keys)
    }

    @Test
    fun filterOwnedAppDlc_excludesDepotWhenNoOwnershipSignals() = runBlocking {
        val depot = createDepot(depotId = 13, dlcAppId = 400)
        val result = SteamDlcOwnershipManager.filterOwnedAppDlc(
            appDlcDepots = mapOf(depot.depotId to depot),
            invalidAppId = SteamService.INVALID_APP_ID,
            ownedGameIds = emptySet(),
            hasLicense = { false },
            hasPicsApp = { false },
        )
        assertEquals(emptySet<Int>(), result.keys)
    }

    @Test
    fun filterOwnedAppDlc_includesInvalidAppIdDepot() = runBlocking {
        val depot = createDepot(depotId = 14, dlcAppId = SteamService.INVALID_APP_ID)
        val result = SteamDlcOwnershipManager.filterOwnedAppDlc(
            appDlcDepots = mapOf(depot.depotId to depot),
            invalidAppId = SteamService.INVALID_APP_ID,
            ownedGameIds = emptySet(),
            hasLicense = { false },
            hasPicsApp = { false },
        )
        assertEquals(setOf(14), result.keys)
    }

    @Test
    fun checkDlcOwnershipViaPICSBatch_returnsEmpty_whenInputEmpty() = runBlocking {
        val result = SteamDlcOwnershipManager.checkDlcOwnershipViaPICSBatch(
            dlcAppIds = emptySet(),
            loadAccessTokens = { error("Should not be called") },
            loadPicsProductInfo = { error("Should not be called") },
        )

        assertEquals(emptySet<Int>(), result)
    }

    @Test
    fun checkDlcOwnershipViaPICSBatch_returnsEmpty_whenNoTokensGranted() = runBlocking {
        val result = SteamDlcOwnershipManager.checkDlcOwnershipViaPICSBatch(
            dlcAppIds = setOf(10, 11),
            loadAccessTokens = { emptyMap() },
            loadPicsProductInfo = { error("Should not be called") },
        )

        assertEquals(emptySet<Int>(), result)
    }

    @Test
    fun checkDlcOwnershipViaPICSBatch_aggregatesAcrossChunks() = runBlocking {
        val chunkSizes = mutableListOf<Int>()
        val result = SteamDlcOwnershipManager.checkDlcOwnershipViaPICSBatch(
            dlcAppIds = setOf(1, 2, 3),
            loadAccessTokens = { ids -> ids.associateWith { it.toLong() } },
            loadPicsProductInfo = { requests ->
                chunkSizes.add(requests.size)
                requests.map { it.id }.toSet()
            },
            chunkSize = 2,
        )

        assertEquals(listOf(2, 1), chunkSizes)
        assertEquals(setOf(1, 2, 3), result)
    }

    @Test
    fun checkDlcOwnershipViaPICSBatch_returnsEmpty_onException() = runBlocking {
        val result = SteamDlcOwnershipManager.checkDlcOwnershipViaPICSBatch(
            dlcAppIds = setOf(1),
            loadAccessTokens = { throw IllegalStateException("boom") },
            loadPicsProductInfo = { emptySet() },
        )

        assertEquals(emptySet<Int>(), result)
    }

    private fun createDepot(depotId: Int, dlcAppId: Int): DepotInfo {
        return DepotInfo(
            depotId = depotId,
            dlcAppId = dlcAppId,
            depotFromApp = 1,
            sharedInstall = false,
            osList = EnumSet.of(OS.windows),
            osArch = OSArch.Unknown,
            manifests = mapOf(
                "public" to ManifestInfo(name = "public", gid = 1L, size = 1L, download = 1L),
            ),
            encryptedManifests = emptyMap(),
            language = "english",
        )
    }
}
