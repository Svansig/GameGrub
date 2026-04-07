package app.gamegrub.domain.usecase

import app.gamegrub.data.GameSource
import app.gamegrub.data.LibraryItem
import app.gamegrub.gateway.AuthStateGateway
import app.gamegrub.gateway.LibraryGateway
import app.gamegrub.gateway.LibrarySourceSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class RefreshLibraryOrchestrationUseCaseTest {
    @Test
    fun invoke_refreshesSteamAndCredentialedStores() = runBlocking {
        val libraryGateway = FakeLibraryGateway()
        val authStateGateway = FakeAuthStateGateway(
            stored = setOf(GameSource.GOG, GameSource.AMAZON),
        )

        val useCase = RefreshLibraryOrchestrationUseCase(libraryGateway, authStateGateway)
        useCase()

        assertEquals(
            listOf(GameSource.STEAM, GameSource.GOG, GameSource.AMAZON),
            libraryGateway.refreshedSources,
        )
    }

    @Test
    fun invoke_withoutStoredCredentials_refreshesOnlySteam() = runBlocking {
        val libraryGateway = FakeLibraryGateway()
        val authStateGateway = FakeAuthStateGateway(stored = emptySet())

        val useCase = RefreshLibraryOrchestrationUseCase(libraryGateway, authStateGateway)
        useCase()

        assertEquals(listOf(GameSource.STEAM), libraryGateway.refreshedSources)
    }

    private class FakeLibraryGateway : LibraryGateway {
        val refreshedSources = mutableListOf<GameSource>()

        override fun observeSourceSnapshot(): Flow<LibrarySourceSnapshot> = emptyFlow()

        override fun getAllGames(): Flow<List<LibraryItem>> = emptyFlow()

        override fun getGamesBySource(source: GameSource): Flow<List<LibraryItem>> = emptyFlow()

        override fun getInstalledGames(): Flow<List<LibraryItem>> = emptyFlow()

        override fun searchGames(query: String): Flow<List<LibraryItem>> = emptyFlow()

        override suspend fun refreshLibrary(source: GameSource): Result<Unit> {
            refreshedSources.add(source)
            return Result.success(Unit)
        }

        override suspend fun getGameByAppId(appId: String): LibraryItem? = null

        override fun getGameCount(source: GameSource?): Flow<Int> = emptyFlow()
    }

    private class FakeAuthStateGateway(
        private val stored: Set<GameSource>,
    ) : AuthStateGateway {
        override fun hasStoredCredentials(source: GameSource): Boolean = source in stored

        override fun isLoggedIn(source: GameSource): Boolean = source in stored

        override fun getLoggedInStores(): Set<GameSource> = stored
    }
}

