package app.gamegrub.service.steam.managers

import app.gamegrub.db.dao.ChangeNumbersDao
import app.gamegrub.db.dao.FileChangeListsDao
import app.gamegrub.service.steam.di.SteamPicsClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PicsChangesManager @Inject constructor(
    private val changeNumbersDao: ChangeNumbersDao,
    private val fileChangeListsDao: FileChangeListsDao,
    private val picsClient: SteamPicsClient,
) {
    private companion object {
        // One shared change number row for global PICS sync progress.
        const val GLOBAL_PICS_APP_ID = 0
    }

    suspend fun checkForChanges(): PicsChangesResult = withContext(Dispatchers.IO) {
        val currentChangeNumber = getChangeNumber()
        runCatching {
            val result = picsClient.getChangesSince(currentChangeNumber)

            setChangeNumber(result.currentChangeNumber)

            PicsChangesResult(
                success = true,
                currentChangeNumber = result.currentChangeNumber,
                appChanges = result.appChanges,
                packageChanges = result.packageChanges,
                needsFullUpdate = result.needsFullUpdate,
            )
        }.getOrElse {
            PicsChangesResult(success = false, currentChangeNumber = currentChangeNumber)
        }
    }

    suspend fun getChangeNumber(): Long {
        return changeNumbersDao.getByAppId(GLOBAL_PICS_APP_ID)?.changeNumber ?: 0L
    }

    suspend fun setChangeNumber(changeNumber: Long) {
        changeNumbersDao.insert(GLOBAL_PICS_APP_ID, changeNumber)
    }

    suspend fun deleteAllChanges() {
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
