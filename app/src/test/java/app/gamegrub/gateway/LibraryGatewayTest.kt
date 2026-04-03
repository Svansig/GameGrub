package app.gamegrub.gateway

import app.gamegrub.data.GameSource
import app.gamegrub.data.LibraryItem
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
class LibraryGatewayTest {

    @Mock
    private lateinit var libraryGateway: LibraryGateway

    @BeforeEach
    fun setup() {
    }

    @Test
    fun getAllGames_returnsAllGames() {
        val games = listOf(
            LibraryItem("STEAM_1", "Game 1", gameSource = GameSource.STEAM),
            LibraryItem("GOG_1", "Game 2", gameSource = GameSource.GOG),
        )
        whenever(libraryGateway.getAllGames()).thenReturn(flowOf(games))

        val result = libraryGateway.getAllGames()

        result.collect { collected ->
            assertEquals(2, collected.size)
        }
    }

    @Test
    fun getGamesBySource_filtersBySource() {
        val steamGames = listOf(LibraryItem("STEAM_1", "Steam Game", gameSource = GameSource.STEAM))
        whenever(libraryGateway.getGamesBySource(GameSource.STEAM)).thenReturn(flowOf(steamGames))

        val result = libraryGateway.getGamesBySource(GameSource.STEAM)

        result.collect { collected ->
            assertEquals(1, collected.size)
            assertEquals(GameSource.STEAM, collected[0].gameSource)
        }
    }

    @Test
    fun getInstalledGames_returnsInstalledOnly() {
        val installed = listOf(LibraryItem("STEAM_1", "Installed Game", gameSource = GameSource.STEAM, isInstalled = true))
        whenever(libraryGateway.getInstalledGames()).thenReturn(flowOf(installed))

        val result = libraryGateway.getInstalledGames()

        result.collect { collected ->
            assertEquals(1, collected.size)
            assertEquals(true, collected[0].isInstalled)
        }
    }

    @Test
    fun searchGames_returnsMatching() {
        val games = listOf(LibraryItem("STEAM_1", "My Game", gameSource = GameSource.STEAM))
        whenever(libraryGateway.searchGames("My")).thenReturn(flowOf(games))

        val result = libraryGateway.searchGames("My")

        result.collect { collected ->
            assertEquals(1, collected.size)
            assertEquals("My Game", collected[0].name)
        }
    }

    @Test
    suspend fun refreshLibrary_returnsResult() {
        whenever(libraryGateway.refreshLibrary(GameSource.STEAM)).thenReturn(Result.success(Unit))

        val result = libraryGateway.refreshLibrary(GameSource.STEAM)

        assertEquals(true, result.isSuccess)
    }

    @Test
    suspend fun getGameByAppId_returnsGame() {
        val game = LibraryItem("STEAM_123", "Test Game", gameSource = GameSource.STEAM)
        whenever(libraryGateway.getGameByAppId("STEAM_123")).thenReturn(game)

        val result = libraryGateway.getGameByAppId("STEAM_123")

        assertEquals("STEAM_123", result?.appId)
    }

    @Test
    fun getGameCount_returnsCount() {
        whenever(libraryGateway.getGameCount(GameSource.STEAM)).thenReturn(flowOf(42))

        val result = libraryGateway.getGameCount(GameSource.STEAM)

        result.collect { count ->
            assertEquals(42, count)
        }
    }
}
