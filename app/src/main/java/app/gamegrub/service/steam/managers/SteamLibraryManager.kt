package app.gamegrub.service.steam.managers

import app.gamegrub.data.SteamLicense
import app.gamegrub.db.dao.AppInfoDao
import app.gamegrub.db.dao.CachedLicenseDao
import app.gamegrub.db.dao.DownloadingAppInfoDao
import app.gamegrub.db.dao.SteamAppDao
import app.gamegrub.db.dao.SteamLicenseDao
import app.gamegrub.service.steam.di.OwnedGame
import app.gamegrub.service.steam.di.SteamLibraryClient
import app.gamegrub.utils.steam.LicenseSerializer
import `in`.dragonbra.javasteam.steam.handlers.steamapps.License
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SteamLibraryManager @Inject constructor(
    private val libraryClient: SteamLibraryClient,
    private val appDao: SteamAppDao,
    private val licenseDao: SteamLicenseDao,
    private val appInfoDao: AppInfoDao,
    private val cachedLicenseDao: CachedLicenseDao,
    private val downloadingAppInfoDao: DownloadingAppInfoDao,
) {

    suspend fun refreshOwnedGames(): Int = withContext(Dispatchers.IO) {
        try {
            Timber.i("Refreshing owned games")
            0
        } catch (e: Exception) {
            Timber.e(e, "Failed to refresh owned games")
            0
        }
    }

    suspend fun getOwnedGames(friendId: Long): List<OwnedGame> = withContext(Dispatchers.IO) {
        emptyList()
    }

    suspend fun getLicensesFromDb(): List<License> = withContext(Dispatchers.IO) {
        cachedLicenseDao.getAll().mapNotNull {
            LicenseSerializer.deserializeLicense(it.licenseJson)
        }
    }

    fun getPkgInfoOf(appId: Int): SteamLicense? {
        val app = appDao.findApp(appId) ?: return null
        return licenseDao.findLicense(app.packageId)
    }

    fun getAppInfo(appId: Int) = appDao.findApp(appId)
    fun getDownloadingAppInfo(appId: Int) = downloadingAppInfoDao.getDownloadingApp(appId)
    fun getDownloadableDlcApps(appId: Int) = appDao.findDownloadableDLCApps(appId)
    fun getHiddenDlcApps(appId: Int) = appDao.findHiddenDLCApps(appId)
    fun getInstalledApp(appId: Int) = appInfoDao.getInstalledApp(appId)
    fun isAppInstalled(appId: Int) = getInstalledApp(appId) != null
}
