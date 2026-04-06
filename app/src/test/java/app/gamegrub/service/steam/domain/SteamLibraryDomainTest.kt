package app.gamegrub.service.steam.domain

import app.gamegrub.data.SteamApp
import app.gamegrub.db.dao.AppInfoDao
import app.gamegrub.db.dao.CachedLicenseDao
import app.gamegrub.db.dao.DownloadingAppInfoDao
import app.gamegrub.db.dao.SteamAppDao
import app.gamegrub.db.dao.SteamLicenseDao
import app.gamegrub.enums.AppType
import app.gamegrub.service.steam.di.OwnedGame
import app.gamegrub.service.steam.di.SteamConnection
import app.gamegrub.service.steam.di.SteamLibraryClient
import app.gamegrub.service.steam.managers.DownloadManager
import app.gamegrub.service.steam.managers.PicsChangesManager
import `in`.dragonbra.javasteam.types.SteamID
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SteamLibraryDomainTest {
    private val libraryClient = mockk<SteamLibraryClient>()
    private val connection = mockk<SteamConnection>()
    private val appDao = mockk<SteamAppDao>()
    private val licenseDao = mockk<SteamLicenseDao>(relaxed = true)
    private val appInfoDao = mockk<AppInfoDao>(relaxed = true)
    private val cachedLicenseDao = mockk<CachedLicenseDao>(relaxed = true)
    private val downloadingAppInfoDao = mockk<DownloadingAppInfoDao>(relaxed = true)
    private val picsChangesManager = mockk<PicsChangesManager>(relaxed = true)
    private val downloadManager = mockk<DownloadManager>(relaxed = true)

    private val domain = SteamLibraryDomain(
        libraryClient = libraryClient,
        connection = connection,
        appDao = appDao,
        licenseDao = licenseDao,
        appInfoDao = appInfoDao,
        cachedLicenseDao = cachedLicenseDao,
        downloadingAppInfoDao = downloadingAppInfoDao,
        picsChangesManager = picsChangesManager,
        downloadManager = downloadManager,
    )

    @Test
    fun refreshOwnedGamesFromServer_preservesExistingOwnershipMetadata() = runBlocking {
        val steamId = SteamID(76561198000000000L)
        val existingApp = SteamApp(
            id = 10,
            packageId = 999,
            ownerAccountId = listOf(12345),
            type = AppType.game,
            receivedPICS = true,
            name = "Old Name",
            iconHash = "oldIcon",
            logoHash = "oldLogo",
        )
        val insertedAppsSlot = slot<List<SteamApp>>()

        every { connection.steamId } returns steamId
        coEvery { libraryClient.getOwnedGames(steamId) } returns listOf(
            OwnedGame(
                appId = 10,
                name = "New Name",
                playtimeMinutes = 0,
                iconUrl = "newIcon",
                logoUrl = "newLogo",
            ),
        )
        coEvery { appDao.findApp(10) } returns existingApp
        coEvery { appDao.insertAll(capture(insertedAppsSlot)) } returns Unit

        val refreshed = domain.refreshOwnedGamesFromServer()

        assertEquals(1, refreshed)
        assertEquals(1, insertedAppsSlot.captured.size)
        val inserted = insertedAppsSlot.captured.first()
        assertEquals(999, inserted.packageId)
        assertEquals(listOf(12345), inserted.ownerAccountId)
        assertEquals(AppType.game, inserted.type)
        assertTrue(inserted.receivedPICS)
        assertEquals("New Name", inserted.name)
        assertEquals("newIcon", inserted.iconHash)
        assertEquals("newLogo", inserted.logoHash)
    }

    @Test
    fun refreshOwnedGamesFromServer_insertsNewAppsWhenNoExistingRow() = runBlocking {
        val steamId = SteamID(76561198000000001L)
        val insertedAppsSlot = slot<List<SteamApp>>()

        every { connection.steamId } returns steamId
        coEvery { libraryClient.getOwnedGames(steamId) } returns listOf(
            OwnedGame(
                appId = 77,
                name = "Brand New Game",
                playtimeMinutes = 5,
                iconUrl = "icon77",
                logoUrl = "logo77",
            ),
        )
        coEvery { appDao.findApp(77) } returns null
        coEvery { appDao.insertAll(capture(insertedAppsSlot)) } returns Unit

        val refreshed = domain.refreshOwnedGamesFromServer()

        assertEquals(1, refreshed)
        val inserted = insertedAppsSlot.captured.first()
        assertEquals(77, inserted.id)
        assertEquals("Brand New Game", inserted.name)
        assertEquals("icon77", inserted.iconHash)
        assertEquals("logo77", inserted.logoHash)
        coVerify(exactly = 1) { appDao.insertAll(any()) }
    }
}

