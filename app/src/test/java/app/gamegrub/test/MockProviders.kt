package app.gamegrub.test

import app.gamegrub.data.GameSource
import app.gamegrub.data.LibraryItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

object MockProviders {

    fun provideLibraryGateway(
        games: List<LibraryItem> = emptyList(),
    ): app.gamegrub.gateway.LibraryGateway {
        return object : app.gamegrub.gateway.LibraryGateway {
            override fun getAllGames(): Flow<List<LibraryItem>> = flowOf(games)
            override fun getGamesBySource(source: GameSource): Flow<List<LibraryItem>> = flowOf(games.filter { it.gameSource == source })
            override fun getInstalledGames(): Flow<List<LibraryItem>> = flowOf(games.filter { it.isInstalled })
            override fun searchGames(query: String): Flow<List<LibraryItem>> = flowOf(games.filter { it.name.contains(query) })
            override suspend fun refreshLibrary(source: GameSource) = Result.success(Unit)
            override suspend fun getGameByAppId(appId: String) = games.find { it.appId == appId }
            override fun getGameCount(source: GameSource?) = flowOf(games.size)
        }
    }

    fun provideAuthGateway(
        loggedInStores: Set<GameSource> = emptySet(),
    ): app.gamegrub.gateway.AuthGateway {
        return object : app.gamegrub.gateway.AuthGateway {
            override suspend fun login(source: GameSource) = Result.success(Unit)
            override suspend fun logout(source: GameSource) = Result.success(Unit)
            override fun isLoggedIn(source: GameSource) = source in loggedInStores
            override fun getAuthState(source: GameSource) = if (source in loggedInStores) {
                app.gamegrub.gateway.AuthState.LoggedIn(source, "TestUser")
            } else {
                app.gamegrub.gateway.AuthState.NotLoggedIn
            }
            override fun getLoginUrl(source: GameSource) = "https://login.${source.name.lowercase()}.com"
            override suspend fun handleAuthCallback(source: GameSource, callbackUrl: String) = Result.success(Unit)
            override fun getLoggedInStores() = loggedInStores
        }
    }

    fun provideDownloadGateway(): app.gamegrub.gateway.DownloadGateway {
        return object : app.gamegrub.gateway.DownloadGateway {
            override suspend fun startDownload(libraryItem: LibraryItem) = Result.success(Unit)
            override suspend fun pauseDownload(gameId: String) = Result.success(Unit)
            override suspend fun resumeDownload(gameId: String) = Result.success(Unit)
            override suspend fun cancelDownload(gameId: String) = Result.success(Unit)
            override fun getDownloadProgress(gameId: String) = flowOf(0f)
            override fun getDownloadInfo(gameId: String) = flowOf(null)
            override fun isDownloading(gameId: String) = false
            override fun getActiveDownloads() = emptyList()
        }
    }

    fun provideLaunchGateway(): app.gamegrub.gateway.LaunchGateway {
        return object : app.gamegrub.gateway.LaunchGateway {
            override suspend fun launchGame(libraryItem: LibraryItem) = Result.success(Unit)
            override suspend fun prepareLaunch(libraryItem: LibraryItem) = Result.success(Unit)
            override fun getLaunchState(appId: String) = app.gamegrub.gateway.LaunchState.Idle
            override fun cancelLaunch(appId: String) {}
            override fun getActiveLaunchCount() = 0
        }
    }
}
