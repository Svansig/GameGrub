package app.gamegrub.service.steam.managers

import app.gamegrub.data.DepotInfo
import app.gamegrub.data.OwnedGames
import app.gamegrub.data.SteamApp
import app.gamegrub.data.SteamLicense
import app.gamegrub.db.dao.AppInfoDao
import app.gamegrub.db.dao.CachedLicenseDao
import app.gamegrub.db.dao.DownloadingAppInfoDao
import app.gamegrub.db.dao.SteamAppDao
import app.gamegrub.db.dao.SteamLicenseDao
import app.gamegrub.enums.Marker
import app.gamegrub.service.steam.SteamService
import app.gamegrub.service.steam.di.SteamConnection
import app.gamegrub.service.steam.di.SteamLibraryClient
import app.gamegrub.utils.steam.LicenseSerializer
import app.gamegrub.utils.storage.MarkerUtils
import `in`.dragonbra.javasteam.steam.handlers.steamapps.License
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SteamLibraryManager @Inject constructor(
    private val libraryClient: SteamLibraryClient,
    private val connection: SteamConnection,
    private val appDao: SteamAppDao,
    private val licenseDao: SteamLicenseDao,
    private val appInfoDao: AppInfoDao,
    private val cachedLicenseDao: CachedLicenseDao,
    private val downloadingAppInfoDao: DownloadingAppInfoDao,
) {
    private inline fun <T> blockingDb(crossinline block: suspend () -> T): T = runBlocking(Dispatchers.IO) {
        block()
    }

    // App DAO operations
    fun findApp(appId: Int): SteamApp? = blockingDb { appDao.findApp(appId) }
    fun getAllAppIds(): List<Int> = blockingDb { appDao.getAllAppIds() }
    fun deleteAllApps() = blockingDb { appDao.deleteAll() }
    fun insertAllApps(apps: List<SteamApp>) = blockingDb { appDao.insertAll(apps) }
    fun insertApp(app: SteamApp) = blockingDb { appDao.insert(app) }
    fun updateApp(app: SteamApp) = blockingDb { appDao.update(app) }

    fun getAppInfoOf(appId: Int): SteamApp? = findApp(appId)
    fun getDownloadableDlcAppsOf(appId: Int): List<SteamApp>? = blockingDb { appDao.findDownloadableDLCApps(appId) }
    fun getHiddenDlcAppsOf(appId: Int): List<SteamApp>? = blockingDb { appDao.findHiddenDLCApps(appId) }

    // License DAO operations
    fun deleteAllLicenses() = blockingDb { licenseDao.deleteAll() }
    fun insertAllLicenses(licenses: List<SteamLicense>) = blockingDb { licenseDao.insertAll(licenses) }
    fun findStaleLicenses(packageIds: List<Int>): List<SteamLicense> = blockingDb { licenseDao.findStaleLicences(packageIds) }
    fun deleteStaleLicenses(packageIds: List<Int>) = blockingDb { licenseDao.deleteStaleLicenses(packageIds) }
    fun getAllLicenses(): List<SteamLicense> = blockingDb { licenseDao.getAllLicenses() }
    fun findLicense(packageId: Int): SteamLicense? = blockingDb { licenseDao.findLicense(packageId) }
    fun updateLicenseApps(packageId: Int, appIds: List<Int>) = blockingDb { licenseDao.updateApps(packageId, appIds) }
    fun updateLicenseDepots(packageId: Int, depotIds: List<Int>) = blockingDb { licenseDao.updateDepots(packageId, depotIds) }

    // Cached license operations
    fun deleteAllCachedLicenses() = blockingDb { cachedLicenseDao.deleteAll() }
    fun insertAllCachedLicenses(licenses: List<app.gamegrub.data.CachedLicense>) = blockingDb { cachedLicenseDao.insertAll(licenses) }

    // App info operations
    fun getInstalledApp(appId: Int) = blockingDb { appInfoDao.getInstalledApp(appId) }
    fun deleteAppInfo(appId: Int) = blockingDb { appInfoDao.deleteApp(appId) }

    // Downloading app info operations
    fun getDownloadingAppInfo(appId: Int) = blockingDb { downloadingAppInfoDao.getDownloadingApp(appId) }
    fun getDownloadingAppInfoOf(appId: Int) = getDownloadingAppInfo(appId)
    fun getAllDownloadingApps() = blockingDb { downloadingAppInfoDao.getAll() }
    fun deleteDownloadingApp(appId: Int) = blockingDb { downloadingAppInfoDao.deleteApp(appId) }
    fun deleteAllDownloadingApps() = blockingDb { downloadingAppInfoDao.deleteAll() }

    // Composite operations
    fun clearAllLibraryData() {
        blockingDb {
            appDao.deleteAll()
            licenseDao.deleteAll()
            cachedLicenseDao.deleteAll()
        }
    }

    // Library client operations
    suspend fun refreshOwnedGamesFromServer(): Int = withContext(Dispatchers.IO) {
        val steamId = connection.steamId ?: return@withContext 0
        try {
            val games = libraryClient.getOwnedGames(steamId)
            val steamApps = games.map { game ->
                SteamApp(
                    id = game.appId,
                    name = game.name,
                    iconHash = game.iconUrl,
                    logoHash = game.logoUrl,
                )
            }
            appDao.insertAll(steamApps)
            steamApps.size
        } catch (e: Exception) {
            Timber.e(e, "Failed to refresh owned games")
            0
        }
    }

    suspend fun getOwnedGames(friendId: Long): List<OwnedGames> = withContext(Dispatchers.IO) {
        try {
            val steamId = `in`.dragonbra.javasteam.types.SteamID(friendId)
            libraryClient.getOwnedGames(steamId).map { game ->
                OwnedGames(
                    appId = game.appId,
                    name = game.name,
                    playtimeTwoWeeks = game.playtimeMinutes,
                    playtimeForever = game.playtimeMinutes,
                    imgIconUrl = game.iconUrl,
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get owned games")
            emptyList()
        }
    }

    suspend fun checkDlcOwnership(dlcAppIds: Set<Int>): Set<Int> = withContext(Dispatchers.IO) {
        libraryClient.checkDlcOwnership(dlcAppIds)
    }

    suspend fun getLicensesFromDb(): List<License> = withContext(Dispatchers.IO) {
        cachedLicenseDao.getAll().mapNotNull { LicenseSerializer.deserializeLicense(it.licenseJson) }
    }

    fun getPkgInfoOf(appId: Int): SteamLicense? {
        val app = findApp(appId) ?: return null
        return findLicense(app.packageId)
    }

    fun hasPartialDownload(appId: Int): Boolean {
        if (getDownloadingAppInfo(appId) != null) {
            return true
        }
        val dirPath = SteamService.getAppDirPath(appId)
        return File(dirPath).exists() && !MarkerUtils.hasMarker(dirPath, Marker.DOWNLOAD_COMPLETE_MARKER)
    }

    fun getMainAppDepots(appId: Int, language: String): Map<Int, DepotInfo> {
        language.hashCode() // keep signature stable while using DB-backed depots.
        return findApp(appId)?.depots.orEmpty()
    }

    fun getDownloadableDepots(appId: Int): Map<Int, DepotInfo> = getMainAppDepots(appId, "english")
    fun getAppDlc(appId: Int): Map<Int, DepotInfo> =
        getMainAppDepots(appId, "english").filterValues { depot ->
            depot.dlcAppId != SteamService.INVALID_APP_ID
        }
}
