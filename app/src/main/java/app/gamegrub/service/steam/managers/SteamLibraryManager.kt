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
import app.gamegrub.service.steam.di.OwnedGame
import app.gamegrub.service.steam.di.SteamConnection
import app.gamegrub.service.steam.di.SteamLibraryClient
import app.gamegrub.utils.steam.LicenseSerializer
import app.gamegrub.utils.steam.SteamUtils
import app.gamegrub.utils.storage.MarkerUtils
import `in`.dragonbra.javasteam.steam.handlers.steamapps.License
import `in`.dragonbra.javasteam.types.KeyValue
import kotlinx.coroutines.Dispatchers
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

    suspend fun refreshOwnedGames(): Int = withContext(Dispatchers.IO) {
        val steamId = connection.steamId ?: return@withContext 0
        try {
            val games = libraryClient.getOwnedGames(steamId)
            val steamApps = games.map { game ->
                SteamApp(
                    appId = game.appId,
                    name = game.name,
                    iconHash = game.iconUrl,
                    logoHash = game.logoUrl,
                    playtime = game.playtimeMinutes,
                    hasCloudEnabled = true,
                    packageName = null,
                    changeNumber = 0,
                    downloadComplete = false,
                    isDlc = false,
                    parentAppId = null,
                    parentPackageName = null,
                    installDirName = null,
                )
            }
            appDao.insertAll(steamApps)
            Timber.i("Refreshed ${steamApps.size} owned games")
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
                    playtime = game.playtimeMinutes,
                    iconUrl = game.iconUrl,
                    logoUrl = game.logoUrl,
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
        val app = appDao.findApp(appId) ?: return null
        return licenseDao.findLicense(app.packageId)
    }

    fun getAppInfo(appId: Int): SteamApp? = appDao.findApp(appId)
    fun getDownloadingAppInfo(appId: Int) = downloadingAppInfoDao.getDownloadingApp(appId)
    fun getDownloadableDlcApps(appId: Int) = appDao.findDownloadableDLCApps(appId)
    fun getHiddenDlcApps(appId: Int) = appDao.findHiddenDLCApps(appId)
    fun getInstalledApp(appId: Int) = appInfoDao.getInstalledApp(appId)
    fun isAppInstalled(appId: Int) = getInstalledApp(appId) != null

    fun hasPartialDownload(appId: Int): Boolean {
        if (getDownloadingAppInfo(appId) != null) return true
        val dirPath = SteamUtils.getAppDirPath(appId)
        return File(dirPath).exists() && !MarkerUtils.hasMarker(dirPath, Marker.DOWNLOAD_COMPLETE_MARKER)
    }

    fun getMainAppDepots(appId: Int, language: String): Map<Int, DepotInfo> {
        return try {
            val depotsFile = File(SteamUtils.getAppDirPath(appId), "depots_${appId}.vdf")
            if (!depotsFile.exists()) return emptyMap()
            val kv = KeyValue.loadFromFile(depotsFile) ?: return emptyMap()
            val depots = mutableMapOf<Int, DepotInfo>()
            kv["depots"].children?.forEach { depotKv ->
                val depotId = depotKv.name?.toIntOrNull() ?: return@forEach
                depots[depotId] = DepotInfo(
                    depotId = depotId,
                    size = depotKv["maxsize"].value?.toLongOrNull() ?: 0L,
                    dlcAppId = depotKv["dlc"].value?.toIntOrNull(),
                )
            }
            depots
        } catch (e: Exception) {
            Timber.e(e, "Failed to get depots for $appId")
            emptyMap()
        }
    }

    fun getDownloadableDepots(appId: Int): Map<Int, DepotInfo> = getMainAppDepots(appId, "english")
    fun getAppDlc(appId: Int): Map<Int, DepotInfo> = getMainAppDepots(appId, "english").filter { it.value.dlcAppId != null }
}
