package app.gamegrub.data.repository

import app.gamegrub.data.GameSource
import app.gamegrub.data.LibraryItem
import app.gamegrub.data.UnifiedGame
import app.gamegrub.db.dao.GameDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameRepository @Inject constructor(
    private val gameDao: GameDao
) {
    fun getAllGames(): Flow<List<UnifiedGame>> = gameDao.getAll()

    fun getGamesBySource(source: GameSource): Flow<List<UnifiedGame>> = 
        gameDao.getBySource(source)

    fun getInstalledGames(): Flow<List<UnifiedGame>> = 
        gameDao.getByInstallStatus(true)

    fun getInstalledGamesBySource(source: GameSource): Flow<List<UnifiedGame>> =
        gameDao.getBySourceAndInstallStatus(source, true)

    fun searchGames(query: String): Flow<List<UnifiedGame>> =
        gameDao.searchByName(query)

    fun searchGamesBySource(query: String, source: GameSource): Flow<List<UnifiedGame>> =
        gameDao.searchByNameAndSource(query, source)

    suspend fun getGameById(id: Int): UnifiedGame? = gameDao.getById(id)

    suspend fun getGameByAppId(appId: String): UnifiedGame? = gameDao.getByAppId(appId)

    suspend fun insertGame(game: UnifiedGame) = gameDao.insert(game)

    suspend fun insertGames(games: List<UnifiedGame>) = gameDao.insertAll(games)

    suspend fun updateGame(game: UnifiedGame) = gameDao.update(game)

    suspend fun deleteGame(id: Int) = gameDao.delete(id)

    suspend fun updateInstallStatus(id: Int, isInstalled: Boolean, installPath: String) =
        gameDao.updateInstallStatus(id, isInstalled, installPath)

    suspend fun markAsUninstalled(id: Int) = gameDao.markAsUninstalled(id)

    suspend fun updatePlayTime(id: Int, timestamp: Long, additionalPlayTime: Long) =
        gameDao.updatePlayTime(id, timestamp, additionalPlayTime)

    suspend fun upsertGamesPreservingInstallStatus(games: List<UnifiedGame>) =
        gameDao.upsertPreservingInstallStatus(games)

    fun getGameCount(): Flow<Int> = gameDao.getCount()

    fun getGameCountBySource(source: GameSource): Flow<Int> = gameDao.getCountBySource(source)
}
