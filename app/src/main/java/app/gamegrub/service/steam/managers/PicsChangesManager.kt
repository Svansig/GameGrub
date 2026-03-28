package app.gamegrub.service.steam.managers

import app.gamegrub.db.dao.ChangeNumbersDao
import app.gamegrub.db.dao.FileChangeListsDao
import app.gamegrub.service.steam.di.PicsChanges
import app.gamegrub.service.steam.di.SteamPicsClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PicsChangesManager @Inject constructor(
    private val changeNumbersDao: ChangeNumbersDao,
    private val fileChangeListsDao: FileChangeListsDao,
    private val picsClient: SteamPicsClient,
) {
    suspend fun checkForChanges(): PicsChangesResult = withContext(Dispatchers.IO) {
        val currentChangeNumber = getChangeNumber()
        val result = picsClient.getChangesSince(currentChangeNumber)

        if (result.success) {
            setChangeNumber(result.currentChangeNumber)
        }

        PicsChangesResult(
            success = result.success,
            currentChangeNumber = result.currentChangeNumber,
            appChanges = result.appChanges,
            packageChanges = result.packageChanges,
            needsFullUpdate = result.needsFullUpdate,
        )
    }

    fun getChangeNumber(): Long = changeNumbersDao.getChangeNumber()
    fun setChangeNumber(changeNumber: Long) = changeNumbersDao.setChangeNumber(changeNumber)
    fun deleteAllChanges() {
        changeNumbersDao.deleteAll()
        fileChangeListsDao.deleteAll()
    }
}

data class PicsChangesResult(
    val success: Boolean,
    val currentChangeNumber: Long = 0,
    val appChanges: Set<Int> = emptySet(),
    val packageChanges: Set<Int> = emptySet(),
    val needsFullUpdate: Boolean = false,
)
