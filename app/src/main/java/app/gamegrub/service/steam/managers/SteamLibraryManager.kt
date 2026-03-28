package app.gamegrub.service.steam.managers

import android.content.Context
import app.gamegrub.data.AppInfo
import app.gamegrub.data.DepotInfo
import app.gamegrub.data.DownloadInfo
import app.gamegrub.data.DownloadingAppInfo
import app.gamegrub.data.OwnedGames
import app.gamegrub.data.SteamApp
import app.gamegrub.data.SteamLicense
import app.gamegrub.db.dao.AppInfoDao
import app.gamegrub.db.dao.CachedLicenseDao
import app.gamegrub.db.dao.DownloadingAppInfoDao
import app.gamegrub.db.dao.SteamAppDao
import app.gamegrub.db.dao.SteamLicenseDao
import app.gamegrub.enums.SaveLocation
import app.gamegrub.utils.steam.LicenseSerializer
import app.gamegrub.utils.steam.SteamUtils
import `in`.dragonbra.javasteam.enums.ELicenseFlags
import `in`.dragonbra.javasteam.steam.handlers.steamapps.License
import `in`.dragonbra.javasteam.steam.handlers.steamapps.PICSRequest
import `in`.dragonbra.javasteam.steam.handlers.steamapps.SteamApps
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.SteamFriends
import `in`.dragonbra.javasteam.types.KeyValue
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Manages Steam library operations including game discovery, license management,
 * and depot information.
 *
 * Responsibilities:
 * - Fetching owned games from Steam
 * - Managing game licenses
 * - Tracking depot/installation information
 * - DLC discovery and management
 */
class SteamLibraryManager(
    private val appDao: SteamAppDao,
    private val licenseDao: SteamLicenseDao,
    private val appInfoDao: AppInfoDao,
    private val cachedLicenseDao: CachedLicenseDao,
    private val downloadingAppInfoDao: DownloadingAppInfoDao,
    private val getService: () -> SteamServiceAccess?,
) {

    /**
     * Refresh owned games from Steam server.
     */
    suspend fun refreshOwnedGamesFromServer(): Int = withContext(Dispatchers.IO) {
        val service = getService() ?: return@withContext 0
        val steamApps = service.steamApps ?: return@withContext 0

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
            Timber.i("Refreshed ${steamAppsList.size} owned games from server")
            steamAppsList.size
        } catch (e: Exception) {
            Timber.e(e, "Failed to refresh owned games")
            0
        }
    }

    /**
     * Get owned games for a friend.
     */
    suspend fun getOwnedGames(friendID: Long): List<OwnedGames> = withContext(Dispatchers.IO) {
        val service = getService() ?: return@withContext emptyList()
        val steamFriends = service.steamFriends ?: return@withContext emptyList()

        try {
            val result = steamFriends.getOwnedGames(friendID).await()
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
            Timber.e(e, "Failed to get owned games for friend $friendID")
            emptyList()
        }
    }

    /**
     * Check DLC ownership via PICS batch request.
     */
    suspend fun checkDlcOwnershipViaPICSBatch(dlcAppIds: Set<Int>): Set<Int> = withContext(Dispatchers.IO) {
        val service = getService() ?: return@withContext emptySet()
        val steamApps = service.steamApps ?: return@withContext emptySet()

        if (dlcAppIds.isEmpty()) return@withContext emptySet()

        try {
            val requests = dlcAppIds.map { PICSRequest(appId = it) }
            val result = steamApps.getPICSProductInfo(requests, emptyList()).await()

            val ownedDlc = mutableSetOf<Int>()
            result?.apps?.forEach { (appId, appInfo) ->
                if (appInfo.changenumber > 0) {
                    ownedDlc.add(appId)
                }
            }

            Timber.d("Checked ${dlcAppIds.size} DLC apps, found ${ownedDlc.size} owned")
            ownedDlc
        } catch (e: Exception) {
            Timber.e(e, "Failed to check DLC ownership via PICS batch")
            emptySet()
        }
    }

    /**
     * Get licenses from database for use with DepotDownloader.
     */
    suspend fun getLicensesFromDb(): List<License> = withContext(Dispatchers.IO) {
        val cached = cachedLicenseDao.getAll()
        cached.mapNotNull { cachedLicense ->
            LicenseSerializer.deserializeLicense(cachedLicense.licenseJson)
        }
    }

    /**
     * Get package info for an app.
     */
    fun getPkgInfoOf(appId: Int): SteamLicense? {
        return kotlinx.coroutines.runBlocking(Dispatchers.IO) {
            val app = appDao.findApp(appId)
            if (app != null) {
                licenseDao.findLicense(app.packageId)
            } else {
                null
            }
        }
    }

    /**
     * Get app info from database.
     */
    fun getAppInfoOf(appId: Int): SteamApp? {
        return kotlinx.coroutines.runBlocking(Dispatchers.IO) {
            appDao.findApp(appId)
        }
    }

    /**
     * Get downloading app info.
     */
    fun getDownloadingAppInfoOf(appId: Int): DownloadingAppInfo? {
        return kotlinx.coroutines.runBlocking(Dispatchers.IO) {
            downloadingAppInfoDao.getDownloadingApp(appId)
        }
    }

    /**
     * Get downloadable DLC apps for a game.
     */
    fun getDownloadableDlcAppsOf(appId: Int): List<SteamApp>? {
        return kotlinx.coroutines.runBlocking(Dispatchers.IO) {
            appDao.findDownloadableDLCApps(appId)
        }
    }

    /**
     * Get hidden DLC apps.
     */
    fun getHiddenDlcAppsOf(appId: Int): List<SteamApp>? {
        return kotlinx.coroutines.runBlocking(Dispatchers.IO) {
            appDao.findHiddenDLCApps(appId)
        }
    }

    /**
     * Get installed app info.
     */
    fun getInstalledApp(appId: Int): AppInfo? {
        return kotlinx.coroutines.runBlocking(Dispatchers.IO) {
            appInfoDao.getInstalledApp(appId)
        }
    }

    /**
     * Get main app depot IDs without proper depot DLC IDs.
     */
    fun getMainAppDlcIdsWithoutProperDepotDlcIds(appId: Int): MutableList<Int> {
        val mainAppDepots = getMainAppDepots(appId, "english")
        val depotDlcIds = mainAppDepots.values.mapNotNull { it.dlcAppId }.toSet()
        val downloadableDlcApps = getDownloadableDlcAppsOf(appId) ?: return mutableListOf()

        return downloadableDlcApps
            .filter { it.appId !in depotDlcIds }
            .map { it.appId }
            .toMutableList()
    }

    /**
     * Get main app depots.
     */
    fun getMainAppDepots(appId: Int, containerLanguage: String): Map<Int, DepotInfo> {
        return try {
            val appDirPath = SteamUtils.getAppDirPath(appId)
            val depotsFile = File(appDirPath, "depots_${appId}.vdf")
            if (!depotsFile.exists()) return emptyMap()

            val kv = KeyValue.loadFromFile(depotsFile) ?: return emptyMap()
            val depots = mutableMapOf<Int, DepotInfo>()

            kv["depots"].children?.forEach { depotKv ->
                val depotId = depotKv.name?.toIntOrNull() ?: return@forEach
                val manifests = depotKv["manifests"]
                val language = depotKv["config"]["oslist"].value ?: "english"

                if (language == containerLanguage || language.isEmpty()) {
                    val depotInfo = DepotInfo(
                        depotId = depotId,
                        size = depotKv["maxsize"].value?.toLongOrNull() ?: 0L,
                        dlcAppId = depotKv["dlc"].value?.toIntOrNull(),
                    )
                    depots[depotId] = depotInfo
                }
            }

            depots
        } catch (e: Exception) {
            Timber.e(e, "Failed to get main app depots for $appId")
            emptyMap()
        }
    }

    /**
     * Get downloadable depots.
     */
    fun getDownloadableDepots(appId: Int): Map<Int, DepotInfo> {
        return getDownloadableDepots(appId, "english")
    }

    /**
     * Get downloadable depots for a specific language.
     */
    fun getDownloadableDepots(appId: Int, preferredLanguage: String): Map<Int, DepotInfo> {
        return try {
            val appDirPath = SteamUtils.getAppDirPath(appId)
            val depotsFile = File(appDirPath, "depots_${appId}.vdf")
            if (!depotsFile.exists()) return emptyMap()

            val kv = KeyValue.loadFromFile(depotsFile) ?: return emptyMap()
            val depots = mutableMapOf<Int, DepotInfo>()

            kv["depots"].children?.forEach { depotKv ->
                val depotId = depotKv.name?.toIntOrNull() ?: return@forEach

                val depotInfo = DepotInfo(
                    depotId = depotId,
                    size = depotKv["maxsize"].value?.toLongOrNull() ?: 0L,
                    dlcAppId = depotKv["dlc"].value?.toIntOrNull(),
                )
                depots[depotId] = depotInfo
            }

            depots
        } catch (e: Exception) {
            Timber.e(e, "Failed to get downloadable depots for $appId")
            emptyMap()
        }
    }

    /**
     * Check if there is an incomplete download on disk.
     */
    fun hasPartialDownload(appId: Int): Boolean {
        val downloadingApp = getDownloadingAppInfoOf(appId)
        if (downloadingApp != null) return true

        val dirPath = SteamUtils.getAppDirPath(appId)
        return File(dirPath).exists() && !app.gamegrub.utils.storage.MarkerUtils.hasMarker(
            dirPath,
            app.gamegrub.enums.Marker.DOWNLOAD_COMPLETE_MARKER,
        )
    }

    /**
     * Is app installed.
     */
    fun isAppInstalled(appId: Int): Boolean {
        return getInstalledApp(appId) != null
    }

    /**
     * Get app DLC map.
     */
    fun getAppDlc(appId: Int): Map<Int, DepotInfo> {
        val depots = getMainAppDepots(appId, "english")
        return depots.filter { it.value.dlcAppId != null }
    }

    /**
     * Get owned app DLC.
     */
    suspend fun getOwnedAppDlc(appId: Int): Map<Int, DepotInfo> {
        val dlc = getAppDlc(appId)
        val dlcAppIds = dlc.values.mapNotNull { it.dlcAppId }.toSet()
        val ownedDlcIds = checkDlcOwnershipViaPICSBatch(dlcAppIds)

        return dlc.filter { (depotId, info) ->
            info.dlcAppId in ownedDlcIds
        }
    }
}
