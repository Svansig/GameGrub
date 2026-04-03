package app.gamegrub.test

import app.gamegrub.data.GameSource
import app.gamegrub.data.LibraryItem

object FakeData {
    fun createLibraryItem(
        appId: String = "STEAM_12345",
        name: String = "Test Game",
        source: GameSource = GameSource.STEAM,
        isInstalled: Boolean = false,
    ): LibraryItem {
        return LibraryItem(
            appId = appId,
            name = name,
            iconHash = "https://example.com/icon.png",
            headerImageUrl = "https://example.com/header.png",
            gameSource = source,
            sizeBytes = 1024 * 1024 * 1024L,
            isInstalled = isInstalled,
        )
    }

    fun createLibraryItemList(count: Int = 10, source: GameSource = GameSource.STEAM): List<LibraryItem> {
        return (1..count).map { i ->
            createLibraryItem(
                appId = "${source.name}_$i",
                name = "Game $i",
                source = source,
                isInstalled = i % 2 == 0,
            )
        }
    }
}

class MockLibraryGateway : app.gamegrub.gateway.LibraryGateway {
    private val games = mutableListOf<LibraryItem>()

    fun setGames(list: List<LibraryItem>) {
        games.clear()
        games.addAll(list)
    }

    override fun getAllGames() = kotlinx.coroutines.flow.flowOf(games.toList())

    override fun getGamesBySource(source: GameSource) = flowOf(games.filter { it.gameSource == source })

    override fun getInstalledGames() = flowOf(games.filter { it.isInstalled })

    override fun searchGames(query: String) = flowOf(games.filter { it.name.contains(query) })

    override suspend fun refreshLibrary(source: GameSource): Result<Unit> = Result.success(Unit)

    override suspend fun getGameByAppId(appId: String): LibraryItem? = games.find { it.appId == appId }

    override fun getGameCount(source: GameSource?) = flowOf(if (source != null) games.count { it.gameSource == source } else games.size)

    private fun <T> flowOf(value: T) = kotlinx.coroutines.flow.flowOf(value)
}

class MockAuthGateway : app.gamegrub.gateway.AuthGateway {
    private val loggedInStores = mutableSetOf<GameSource>()

    fun setLoggedIn(stores: Set<GameSource>) {
        loggedInStores.clear()
        loggedInStores.addAll(stores)
    }

    override suspend fun login(source: GameSource): Result<Unit> {
        loggedInStores.add(source)
        return Result.success(Unit)
    }

    override suspend fun logout(source: GameSource): Result<Unit> {
        loggedInStores.remove(source)
        return Result.success(Unit)
    }

    override fun isLoggedIn(source: GameSource): Boolean = source in loggedInStores

    override fun getAuthState(source: GameSource) = when (source) {
        in loggedInStores -> app.gamegrub.gateway.AuthState.LoggedIn(source, "TestUser")
        else -> app.gamegrub.gateway.AuthState.NotLoggedIn
    }

    override fun getLoginUrl(source: GameSource) = "https://login.${source.name.lowercase()}.com/oauth"

    override suspend fun handleAuthCallback(source: GameSource, callbackUrl: String): Result<Unit> {
        loggedInStores.add(source)
        return Result.success(Unit)
    }

    override fun getLoggedInStores() = loggedInStores.toSet()
}
