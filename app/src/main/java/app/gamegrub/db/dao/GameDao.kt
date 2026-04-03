package app.gamegrub.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import app.gamegrub.data.GameSource
import app.gamegrub.data.UnifiedGame
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(game: UnifiedGame)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(games: List<UnifiedGame>)

    @Update
    suspend fun update(game: UnifiedGame)

    @Query("DELETE FROM games WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("SELECT * FROM games WHERE id = :id")
    suspend fun getById(id: Int): UnifiedGame?

    @Query("SELECT * FROM games WHERE app_id = :appId")
    suspend fun getByAppId(appId: String): UnifiedGame?

    @Query("SELECT * FROM games ORDER BY LOWER(name) ASC")
    fun getAll(): Flow<List<UnifiedGame>>

    @Query("SELECT * FROM games WHERE game_source = :source ORDER BY LOWER(name) ASC")
    fun getBySource(source: GameSource): Flow<List<UnifiedGame>>

    @Query("SELECT * FROM games WHERE is_installed = :isInstalled ORDER BY LOWER(name) ASC")
    fun getByInstallStatus(isInstalled: Boolean): Flow<List<UnifiedGame>>

    @Query("SELECT * FROM games WHERE game_source = :source AND is_installed = :isInstalled ORDER BY LOWER(name) ASC")
    fun getBySourceAndInstallStatus(source: GameSource, isInstalled: Boolean): Flow<List<UnifiedGame>>

    @Query("SELECT * FROM games WHERE name LIKE '%' || :query || '%' ORDER BY LOWER(name) ASC")
    fun searchByName(query: String): Flow<List<UnifiedGame>>

    @Query("SELECT * FROM games WHERE game_source = :source AND name LIKE '%' || :query || '%' ORDER BY LOWER(name) ASC")
    fun searchByNameAndSource(query: String, source: GameSource): Flow<List<UnifiedGame>>

    @Query("SELECT COUNT(*) FROM games")
    fun getCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM games WHERE game_source = :source")
    fun getCountBySource(source: GameSource): Flow<Int>

    @Query("UPDATE games SET is_installed = :isInstalled, install_path = :installPath WHERE id = :id")
    suspend fun updateInstallStatus(id: Int, isInstalled: Boolean, installPath: String)

    @Query("UPDATE games SET is_installed = 0, install_path = '' WHERE id = :id")
    suspend fun markAsUninstalled(id: Int)

    @Query("UPDATE games SET last_played = :timestamp, play_time = play_time + :additionalPlayTime WHERE id = :id")
    suspend fun updatePlayTime(id: Int, timestamp: Long, additionalPlayTime: Long)

    @Transaction
    suspend fun upsertPreservingInstallStatus(games: List<UnifiedGame>) {
        games.forEach { newGame ->
            val existingGame = getByAppId(newGame.appId)
            if (existingGame != null) {
                val gameToInsert = newGame.copy(
                    id = existingGame.id,
                    isInstalled = existingGame.isInstalled,
                    installPath = existingGame.installPath,
                    installSize = existingGame.installSize,
                    lastPlayed = existingGame.lastPlayed,
                    playTime = existingGame.playTime,
                )
                insert(gameToInsert)
            } else {
                insert(newGame)
            }
        }
    }
}
