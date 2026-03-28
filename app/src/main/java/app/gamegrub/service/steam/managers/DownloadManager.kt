package app.gamegrub.service.steam.managers

import app.gamegrub.data.DownloadInfo
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
    fun getDownloadingAppInfo(appId: Int): DownloadingAppInfo? = downloadingAppInfoDao.getDownloadingApp(appId)
    fun getAllDownloadingApps(): List<DownloadingAppInfo> = downloadingAppInfoDao.getAll()
    fun insertDownloadingApp(info: DownloadingAppInfo) = downloadingAppInfoDao.insert(info)
    fun deleteDownloadingApp(appId: Int) = downloadingAppInfoDao.deleteApp(appId)
    fun deleteAllDownloadingApps() = downloadingAppInfoDao.deleteAll()

    fun getInstalledApp(appId: Int) = appInfoDao.getInstalledApp(appId)
    fun insertAppInfo(info: app.gamegrub.data.AppInfo) = appInfoDao.insert(info)
    fun updateAppInfo(info: app.gamegrub.data.AppInfo) = appInfoDao.update(info)
    fun deleteAppInfo(appId: Int) = appInfoDao.deleteApp(appId)

    fun deleteChangeNumbersByApp(appId: Int) = changeNumbersDao.deleteByAppId(appId)
    fun deleteFileChangeListsByApp(appId: Int) = fileChangeListsDao.deleteByAppId(appId)

    fun clearAll() {
        appInfoDao.deleteAll()
        changeNumbersDao.deleteAll()
        fileChangeListsDao.deleteAll()
        downloadingAppInfoDao.deleteAll()
    }
}
