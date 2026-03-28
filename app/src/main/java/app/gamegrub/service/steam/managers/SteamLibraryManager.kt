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
import app.gamegrub.utils.steam.LicenseSerializer
import app.gamegrub.utils.steam.SteamUtils
import app.gamegrub.utils.storage.MarkerUtils
import `in`.dragonbra.javasteam.enums.ELicenseFlags
import `in`.dragonbra.javasteam.steam.handlers.steamapps.License
import `in`.dragonbra.javasteam.steam.handlers.steamapps.PICSRequest
import `in`.dragonbra.javasteam.types.KeyValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SteamLibraryManager @Inject constructor(
    private val appDao: SteamAppDao,
    private val licenseDao: SteamLicenseDao,
    private val appInfoDao: AppInfoDao,
    private val cachedLicenseDao: CachedLicenseDao,
    private val downloadingAppInfoDao: DownloadingAppInfoDao,
) {

    suspend fun refreshOwnedGames(): Int = withContext(Dispatchers.IO) {
        val service = SteamService.instance ?: return@withContext 0
        val steamApps = service._steamApps ?: return@withContext 0

        try {
            val result = steamApps.getOwnedGames(service.steamClient?.steamID ?: return@withContext 0).await()
            val games = result?.games ?: return@withContext 0

            val steamAppsList = games.map { game ->
                SteamApp(
                    appId = game.appID,
                    name = game.name ?: "Unknown Game",
                    iconHash = game.imgIconUrl ?: "",
                    logoHash = game.imgLogoUrl ?: "",
                    playtime = game.playtimeForever,
                    hasCloudEnabled = game.hasCommunityVisibleStats,
                    packageName = null,
                    changeNumber = 0,
                    downloadComplete = false,
                    isDlc = false,
                    parentAppId = null,
                    parentPackageName = null,
                    installDirName = null,
                )
            }

            appDao.insertAll(steamAppsList)
            Timber.i("Refreshed ${steamAppsList.size} owned games")
            steamAppsList.size
        } catch (e: Exception) {
            Timber.e(e, "Failed to refresh owned games")
            0
        }
    }

    suspend fun getOwnedGames(friendId: Long): List<OwnedGames> = withContext(Dispatchers.IO) {
        val service = SteamService.instance ?: return@withContext emptyList()
        val steamFriends = service._steamFriends ?: return@withContext emptyList()

        try {
            val result = steamFriends.getOwnedGames(friendId).await()
            result?.games?.map { game ->
                OwnedGames(
                    appId = game.appID,
                    name = game.name ?: "Unknown",
                    playtime = game.playtimeForever,
                    iconUrl = game.imgIconUrl ?: "",
                    logoUrl = game.imgLogoUrl ?: "",
                )
            } ?: emptyList()
        } catch (e: Exception) {
            Timber.e(e, "Failed to get owned games")
            emptyList()
        }
    }

    suspend fun checkDlcOwnership(dlcAppIds: Set<Int>): Set<Int> = withContext(Dispatchers.IO) {
        val service = SteamService.instance ?: return@withContext emptySet()
        val steamApps = service._steamApps ?: return@withContext emptySet()
        if (dlcAppIds.isEmpty()) return@withContext emptySet()

        try {
            val requests = dlcAppIds.map { PICSRequest(appId = it) }
            val result = steamApps.getPICSProductInfo(requests, emptyList()).await()
            result?.apps?.keys ?: emptySet()
        } catch (e: Exception) {
            Timber.e(e, "Failed to check DLC ownership")
            emptySet()
        }
    }

    suspend fun getLicensesFromDb(): List<License> = withContext(Dispatchers.IO) {
        cachedLicenseDao.getAll().mapNotNull { LicenseSerializer.deserializeLicense(it.licenseJson) }
    }

    fun getPkgInfoOf(appId: Int): SteamLicense? = kotlinx.coroutines.runBlocking(Dispatchers.IO) {
        val app = appDao.findApp(appId) ?: return@runBlocking null
        licenseDao.findLicense(app.packageId)
    }

    fun getAppInfo(appId: Int): SteamApp? = kotlinx.coroutines.runBlocking(Dispatchers.IO) {
        appDao.findApp(appId)
    }

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
