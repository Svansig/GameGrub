package app.gamegrub.data.sync

import app.gamegrub.data.GameSource
import app.gamegrub.data.UnifiedGame
import app.gamegrub.db.dao.AmazonGameDao
import app.gamegrub.db.dao.EpicGameDao
import app.gamegrub.db.dao.GOGGameDao
import app.gamegrub.db.dao.GameDao
import app.gamegrub.db.dao.SteamAppDao
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameSyncManager @Inject constructor(
    private val gameDao: GameDao,
    private val steamAppDao: SteamAppDao,
    private val gogGameDao: GOGGameDao,
    private val epicGameDao: EpicGameDao,
    private val amazonGameDao: AmazonGameDao,
) {
    suspend fun syncFromLegacyTables(): Result<Int> {
        return try {
            var syncedCount = 0

            // Sync Steam apps
            val steamApps = steamAppDao.getAllOwnedApps().first()
            steamApps.forEach { app ->
                if (app.installDir.isNotEmpty()) {
                    val unified = UnifiedGame(
                        appId = "${GameSource.STEAM.name}_${app.id}",
                        gameSource = GameSource.STEAM,
                        name = app.name,
                        iconUrl = app.iconUrl,
                        headerUrl = app.headerUrl,
                        isInstalled = app.installDir.isNotEmpty(),
                        installPath = app.installDir,
                        type = app.type,
                        developer = app.developer,
                        publisher = app.publisher,
                    )
                    gameDao.insert(unified)
                    syncedCount++
                }
            }

            // Sync GOG games
            val gogGames = gogGameDao.getAllAsList()
            gogGames.forEach { game ->
                val unified = UnifiedGame(
                    appId = "${GameSource.GOG.name}_${game.id}",
                    gameSource = GameSource.GOG,
                    name = game.title,
                    iconUrl = game.iconUrl,
                    headerUrl = game.imageUrl,
                    isInstalled = game.isInstalled,
                    installPath = game.installPath,
                    installSize = game.installSize,
                    downloadSize = game.downloadSize,
                    lastPlayed = game.lastPlayed,
                    playTime = game.playTime,
                    type = game.type,
                    description = game.description,
                    developer = game.developer,
                    publisher = game.publisher,
                    releaseDate = game.releaseDate,
                )
                gameDao.insert(unified)
                syncedCount++
            }

            // Sync Epic games
            val epicGames = epicGameDao.getAll().first()
            epicGames.forEach { game ->
                val unified = UnifiedGame(
                    appId = "${GameSource.EPIC.name}_${game.id}",
                    gameSource = GameSource.EPIC,
                    name = game.title,
                    iconUrl = game.iconUrl,
                    headerUrl = game.artCover,
                    isInstalled = game.isInstalled,
                    installPath = game.installPath,
                    installSize = game.installSize,
                    downloadSize = game.downloadSize,
                    lastPlayed = game.lastPlayed,
                    playTime = game.playTime,
                    type = game.type,
                    description = game.description,
                    developer = game.developer,
                    publisher = game.publisher,
                    releaseDate = game.releaseDate,
                )
                gameDao.insert(unified)
                syncedCount++
            }

            // Sync Amazon games
            val amazonGames = amazonGameDao.getAllAsList()
            amazonGames.forEach { game ->
                val unified = UnifiedGame(
                    appId = "${GameSource.AMAZON.name}_${game.productId}",
                    gameSource = GameSource.AMAZON,
                    name = game.title,
                    iconUrl = game.artUrl,
                    headerUrl = game.heroUrl,
                    isInstalled = game.isInstalled,
                    installPath = game.installPath,
                    installSize = game.installSize,
                    downloadSize = game.downloadSize,
                    lastPlayed = game.lastPlayed,
                    playTime = game.playTimeMinutes,
                    developer = game.developer,
                    publisher = game.publisher,
                    releaseDate = game.releaseDate,
                )
                gameDao.insert(unified)
                syncedCount++
            }

            Timber.i("[GameSyncManager] Synced $syncedCount games from legacy tables")
            Result.success(syncedCount)
        } catch (e: Exception) {
            Timber.e(e, "[GameSyncManager] Failed to sync from legacy tables")
            Result.failure(e)
        }
    }

    suspend fun migrateToUnifiedTable(): Result<Int> {
        return syncFromLegacyTables()
    }
}
