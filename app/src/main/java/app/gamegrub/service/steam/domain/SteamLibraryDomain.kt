package app.gamegrub.service.steam.domain

import app.gamegrub.data.AppInfo
import app.gamegrub.data.CachedLicense
import app.gamegrub.data.DepotInfo
import app.gamegrub.data.DownloadingAppInfo
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
import app.gamegrub.service.steam.managers.DownloadManager
import app.gamegrub.service.steam.managers.PicsChangesManager
import app.gamegrub.utils.steam.LicenseSerializer
import app.gamegrub.utils.storage.MarkerUtils
import `in`.dragonbra.javasteam.enums.ELicenseFlags
import `in`.dragonbra.javasteam.steam.handlers.steamapps.License
import `in`.dragonbra.javasteam.steam.handlers.steamapps.PICSRequest
import java.io.File
import java.util.EnumSet
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Library domain: game metadata, PICS sync, download state, and app info.
 * Coordinates: SteamLibraryManager, PicsChangesManager, DownloadManager, CatalogManager
 */
@Singleton
class SteamLibraryDomain @Inject constructor(
    private val libraryClient: SteamLibraryClient,
    private val connection: SteamConnection,
    private val appDao: SteamAppDao,
    private val licenseDao: SteamLicenseDao,
    private val appInfoDao: AppInfoDao,
    private val cachedLicenseDao: CachedLicenseDao,
    private val downloadingAppInfoDao: DownloadingAppInfoDao,
    private val picsChangesManager: PicsChangesManager,
    private val downloadManager: DownloadManager,
) {
    data class LicenseSyncResult(
        val addedCount: Int,
        val removedCount: Int,
        val packageRequests: List<PICSRequest>,
    )

    // App DAO operations
    suspend fun findApp(appId: Int): SteamApp? = appDao.findApp(appId)
    suspend fun getAllAppIds(): List<Int> = appDao.getAllAppIds()
    suspend fun deleteAllApps() = appDao.deleteAll()
    suspend fun insertAllApps(apps: List<SteamApp>) = appDao.insertAll(apps)
    suspend fun insertApp(app: SteamApp) = appDao.insert(app)
    suspend fun updateApp(app: SteamApp) = appDao.update(app)

    suspend fun getAppInfoOf(appId: Int): SteamApp? = findApp(appId)
    suspend fun getDownloadableDlcAppsOf(appId: Int): List<SteamApp> = appDao.findDownloadableDLCApps(appId)
    suspend fun getHiddenDlcAppsOf(appId: Int): List<SteamApp> = appDao.findHiddenDLCApps(appId)

    // License DAO operations
    suspend fun deleteAllLicenses() = licenseDao.deleteAll()
    suspend fun insertAllLicenses(licenses: List<SteamLicense>) = licenseDao.insertAll(licenses)
    suspend fun findStaleLicenses(packageIds: List<Int>): List<SteamLicense> = licenseDao.findStaleLicences(packageIds)
    suspend fun deleteStaleLicenses(packageIds: List<Int>) = licenseDao.deleteStaleLicenses(packageIds)
    suspend fun getAllLicenses(): List<SteamLicense> = licenseDao.getAllLicenses()
    suspend fun findLicense(packageId: Int): SteamLicense? = licenseDao.findLicense(packageId)
    suspend fun updateLicenseApps(packageId: Int, appIds: List<Int>) = licenseDao.updateApps(packageId, appIds)
    suspend fun updateLicenseDepots(packageId: Int, depotIds: List<Int>) = licenseDao.updateDepots(packageId, depotIds)

    // Cached license operations
    suspend fun deleteAllCachedLicenses() = cachedLicenseDao.deleteAll()
    suspend fun insertAllCachedLicenses(licenses: List<CachedLicense>) = cachedLicenseDao.insertAll(licenses)

    suspend fun syncLicensesForPics(
        callbackLicenses: List<License>,
        preferredOwnerAccountId: Int?,
    ): LicenseSyncResult {
        deleteAllCachedLicenses()
        insertAllCachedLicenses(
            callbackLicenses.map { license ->
                CachedLicense(licenseJson = LicenseSerializer.serializeLicense(license))
            },
        )

        val licensesToAdd = callbackLicenses
            .groupBy { it.packageID }
            .map { licensesEntry ->
                val preferred = licensesEntry.value.firstOrNull {
                    it.ownerAccountID == preferredOwnerAccountId
                } ?: licensesEntry.value.first()

                SteamLicense(
                    packageId = licensesEntry.key,
                    lastChangeNumber = preferred.lastChangeNumber,
                    timeCreated = preferred.timeCreated,
                    timeNextProcess = preferred.timeNextProcess,
                    minuteLimit = preferred.minuteLimit,
                    minutesUsed = preferred.minutesUsed,
                    paymentMethod = preferred.paymentMethod,
                    licenseFlags = licensesEntry.value
                        .map { it.licenseFlags }
                        .reduceOrNull { first, second ->
                            val combined = EnumSet.copyOf(first)
                            combined.addAll(second)
                            combined
                        } ?: EnumSet.noneOf(ELicenseFlags::class.java),
                    purchaseCode = preferred.purchaseCode,
                    licenseType = preferred.licenseType,
                    territoryCode = preferred.territoryCode,
                    accessToken = preferred.accessToken,
                    ownerAccountId = licensesEntry.value.map { it.ownerAccountID },
                    masterPackageID = preferred.masterPackageID,
                )
            }

        if (licensesToAdd.isNotEmpty()) {
            insertAllLicenses(licensesToAdd)
        }

        val licensesToRemove = findStaleLicenses(callbackLicenses.map { it.packageID })
        if (licensesToRemove.isNotEmpty()) {
            deleteStaleLicenses(licensesToRemove.map { it.packageId })
        }

        return LicenseSyncResult(
            addedCount = licensesToAdd.size,
            removedCount = licensesToRemove.size,
            packageRequests = getAllLicenses().map { license ->
                PICSRequest(license.packageId, license.accessToken)
            },
        )
    }

    // App info operations
    suspend fun getInstalledApp(appId: Int) = appInfoDao.getInstalledApp(appId)
    suspend fun deleteAppInfo(appId: Int) = appInfoDao.deleteApp(appId)
    suspend fun upsertInstalledAppDownloadState(
        appId: Int,
        entitledDepotIds: List<Int>,
        selectedDlcAppIds: List<Int>,
    ) {
        val existing = appInfoDao.getInstalledApp(appId)
        if (existing != null) {
            val updatedDownloadedDepots = (existing.downloadedDepots + entitledDepotIds).distinct().sorted()
            val updatedDlcDepots = (existing.dlcDepots + selectedDlcAppIds).distinct().sorted()
            appInfoDao.update(
                AppInfo(
                    id = appId,
                    isDownloaded = true,
                    downloadedDepots = updatedDownloadedDepots,
                    dlcDepots = updatedDlcDepots,
                ),
            )
        } else {
            appInfoDao.insert(
                AppInfo(
                    id = appId,
                    isDownloaded = true,
                    downloadedDepots = entitledDepotIds.sorted(),
                    dlcDepots = selectedDlcAppIds.sorted(),
                ),
            )
        }
    }

    // Downloading app info operations
    suspend fun getDownloadingAppInfo(appId: Int) = downloadingAppInfoDao.getDownloadingApp(appId)
    suspend fun getDownloadingAppInfoOf(appId: Int) = getDownloadingAppInfo(appId)
    suspend fun saveDownloadingAppInfo(appId: Int, dlcAppIds: List<Int>) {
        downloadingAppInfoDao.insert(
            DownloadingAppInfo(
                appId = appId,
                dlcAppIds = dlcAppIds,
            ),
        )
    }

    suspend fun getAllDownloadingApps() = downloadingAppInfoDao.getAll()
    suspend fun deleteDownloadingApp(appId: Int) = downloadingAppInfoDao.deleteApp(appId)
    suspend fun deleteAllDownloadingApps() = downloadingAppInfoDao.deleteAll()

    // Composite operations
    suspend fun clearAllLibraryData() {
        deleteAllApps()
        deleteAllLicenses()
        deleteAllCachedLicenses()
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

    suspend fun getPkgInfoOf(appId: Int): SteamLicense? {
        val app = findApp(appId) ?: return null
        return findLicense(app.packageId)
    }

    suspend fun hasPartialDownload(appId: Int): Boolean {
        if (getDownloadingAppInfo(appId) != null) {
            return true
        }
        val dirPath = SteamService.getAppDirPath(appId)
        return File(dirPath).exists() && !MarkerUtils.hasMarker(dirPath, Marker.DOWNLOAD_COMPLETE_MARKER)
    }

    suspend fun getMainAppDepots(appId: Int, language: String): Map<Int, DepotInfo> {
        language.hashCode()
        return findApp(appId)?.depots.orEmpty()
    }

    suspend fun getDownloadableDepots(appId: Int): Map<Int, DepotInfo> = getMainAppDepots(appId, "english")
    suspend fun getAppDlc(appId: Int): Map<Int, DepotInfo> =
        getMainAppDepots(appId, "english").filterValues { depot ->
            depot.dlcAppId != SteamService.INVALID_APP_ID
        }

    // Download state operations - encapsulate in domain
    suspend fun deleteAppData(appId: Int) = downloadManager.deleteAppData(appId)
    suspend fun clearDownloadState() = downloadManager.clearAll()

    // PICS changes operations - encapsulate in domain
    suspend fun deleteAllPicsChanges() = picsChangesManager.deleteAllChanges()
}
