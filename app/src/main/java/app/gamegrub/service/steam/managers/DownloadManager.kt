package app.gamegrub.service.steam.managers

import app.gamegrub.data.AppInfo
import app.gamegrub.data.DownloadingAppInfo
import app.gamegrub.db.dao.AppInfoDao
import app.gamegrub.db.dao.ChangeNumbersDao
import app.gamegrub.db.dao.DownloadingAppInfoDao
import app.gamegrub.db.dao.FileChangeListsDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadManager @Inject constructor(
    private val downloadingAppInfoDao: DownloadingAppInfoDao,
    private val appInfoDao: AppInfoDao,
    private val changeNumbersDao: ChangeNumbersDao,
    private val fileChangeListsDao: FileChangeListsDao,
) {
    // DownloadingAppInfo operations
    suspend fun getDownloadingAppInfo(appId: Int): DownloadingAppInfo? = downloadingAppInfoDao.getDownloadingApp(appId)
    suspend fun getAllDownloadingApps(): List<DownloadingAppInfo> = downloadingAppInfoDao.getAll()
    suspend fun insertDownloadingApp(info: DownloadingAppInfo) = downloadingAppInfoDao.insert(info)
    suspend fun deleteDownloadingApp(appId: Int) = downloadingAppInfoDao.deleteApp(appId)
    suspend fun deleteAllDownloadingApps() = downloadingAppInfoDao.deleteAll()

    // AppInfo operations
    suspend fun getInstalledApp(appId: Int): AppInfo? = appInfoDao.getInstalledApp(appId)
    suspend fun insertAppInfo(info: AppInfo) = appInfoDao.insert(info)
    suspend fun updateAppInfo(info: AppInfo) = appInfoDao.update(info)
    suspend fun deleteAppInfo(appId: Int) = appInfoDao.deleteApp(appId)

    // Change numbers operations
    suspend fun deleteChangeNumbersByApp(appId: Int) = changeNumbersDao.deleteByAppId(appId)
    suspend fun deleteAllChangeNumbers() = changeNumbersDao.deleteAll()

    // File change lists operations
    suspend fun deleteFileChangeListsByApp(appId: Int) = fileChangeListsDao.deleteByAppId(appId)
    suspend fun deleteAllFileChangeLists() = fileChangeListsDao.deleteAll()

    // Composite: delete all data for an app (used when deleting app)
    suspend fun deleteAppData(appId: Int) {
        appInfoDao.deleteApp(appId)
        changeNumbersDao.deleteByAppId(appId)
        fileChangeListsDao.deleteByAppId(appId)
        downloadingAppInfoDao.deleteApp(appId)
    }

    // Composite: clear all download-related data (used on logout)
    suspend fun clearAll() {
        appInfoDao.deleteAll()
        changeNumbersDao.deleteAll()
        fileChangeListsDao.deleteAll()
        downloadingAppInfoDao.deleteAll()
    }
}
