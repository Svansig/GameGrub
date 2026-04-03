package app.gamegrub.data.repository

import app.gamegrub.data.GameSource
import app.gamegrub.data.UnifiedGame
import app.gamegrub.db.dao.GameDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class GameRepositoryTest {

    @Mock
    private lateinit var gameDao: GameDao

    private lateinit var repository: GameRepository

    @BeforeEach
    fun setup() {
        repository = GameRepository(gameDao)
    }

    @Test
    fun getAllGames_returnsFlowFromDao() {
        val games = listOf(createUnifiedGame(1, "STEAM_1"), createUnifiedGame(2, "STEAM_2"))
        whenever(gameDao.getAll()).thenReturn(flowOf(games))

        val result = repository.getAllGames()

        result.collect { collected ->
            assertEquals(2, collected.size)
            assertEquals("STEAM_1", collected[0].appId)
        }
    }

    @Test
    fun getGamesBySource_filtersBySource() {
        val steamGames = listOf(createUnifiedGame(1, "STEAM_1"))
        whenever(gameDao.getBySource(GameSource.STEAM)).thenReturn(flowOf(steamGames))

        val result = repository.getGamesBySource(GameSource.STEAM)

        result.collect { collected ->
            assertEquals(1, collected.size)
            assertEquals(GameSource.STEAM, collected[0].gameSource)
        }
    }

    @Test
    fun getInstalledGames_returnsOnlyInstalled() {
        val installed = createUnifiedGame(1, "STEAM_1", isInstalled = true)
        whenever(gameDao.getByInstallStatus(true)).thenReturn(flowOf(listOf(installed)))

        val result = repository.getInstalledGames()

        result.collect { collected ->
            assertEquals(1, collected.size)
            assertEquals(true, collected[0].isInstalled)
        }
    }

    @Test
    fun searchGames_returnsMatchingGames() {
        val games = listOf(createUnifiedGame(1, "STEAM_1", name = "Test Game"))
        whenever(gameDao.searchByName("Test")).thenReturn(flowOf(games))

        val result = repository.searchGames("Test")

        result.collect { collected ->
            assertEquals(1, collected.size)
            assertEquals("Test Game", collected[0].name)
        }
    }

    @Test
    suspend fun getGameById_returnsGame() {
        val game = createUnifiedGame(1, "STEAM_1")
        whenever(gameDao.getById(1)).thenReturn(game)

        val result = repository.getGameById(1)

        assertEquals("STEAM_1", result?.appId)
    }

    @Test
    suspend fun getGameByAppId_returnsGame() {
        val game = createUnifiedGame(1, "STEAM_123")
        whenever(gameDao.getByAppId("STEAM_123")).thenReturn(game)

        val result = repository.getGameByAppId("STEAM_123")

        assertEquals(1, result?.id)
    }

    @Test
    suspend fun insertGame_callsDao() {
        val game = createUnifiedGame(0, "STEAM_NEW")

        repository.insertGame(game)
    }

    @Test
    suspend fun updateGame_callsDao() {
        val game = createUnifiedGame(1, "STEAM_UPDATED")

        repository.updateGame(game)
    }

    @Test
    suspend fun deleteGame_callsDao() {
        repository.deleteGame(1)
    }

    @Test
    suspend fun updateInstallStatus_callsDao() {
        repository.updateInstallStatus(1, true, "/path/to/game")
    }

    @Test
    fun getGameCount_returnsCount() {
        whenever(gameDao.getCount()).thenReturn(flowOf(42))

        val result = repository.getGameCount()

        result.collect { count ->
            assertEquals(42, count)
        }
    }

    @Test
    fun getGameCountBySource_returnsCount() {
        whenever(gameDao.getCountBySource(GameSource.GOG)).thenReturn(flowOf(10))

        val result = repository.getGameCountBySource(GameSource.GOG)

        result.collect { count ->
            assertEquals(10, count)
        }
    }

    private fun createUnifiedGame(
        id: Int = 0,
        appId: String = "STEAM_1",
        name: String = "Test Game",
        gameSource: GameSource = GameSource.STEAM,
        isInstalled: Boolean = false,
    ): UnifiedGame {
        return UnifiedGame(
            id = id,
            appId = appId,
            name = name,
            gameSource = gameSource,
            isInstalled = isInstalled,
            installPath = "",
            headerImageUrl = "",
            iconUrl = "",
            sizeBytes = 0,
            lastPlayed = 0,
            totalPlaytime = 0,
        )
    }
}
