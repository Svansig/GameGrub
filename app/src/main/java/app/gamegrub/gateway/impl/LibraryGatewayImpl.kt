package app.gamegrub.gateway.impl

import app.gamegrub.data.GameSource
import app.gamegrub.data.LibraryItem
import app.gamegrub.data.extension.toLibraryItem
import app.gamegrub.data.repository.GameRepository
import app.gamegrub.gateway.LibraryGateway
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryGatewayImpl @Inject constructor(
    private val gameRepository: GameRepository,
) : LibraryGateway {

    override fun getAllGames(): Flow<List<LibraryItem>> {
        return gameRepository.getAllGames().map { games ->
            games.map { it.toLibraryItem() }
        }
    }

    override fun getGamesBySource(source: GameSource): Flow<List<LibraryItem>> {
        return gameRepository.getGamesBySource(source).map { games ->
            games.map { it.toLibraryItem() }
        }
    }

    override fun getInstalledGames(): Flow<List<LibraryItem>> {
        return gameRepository.getInstalledGames().map { games ->
            games.map { it.toLibraryItem() }
        }
    }

    override fun searchGames(query: String): Flow<List<LibraryItem>> {
        return gameRepository.searchGames(query).map { games ->
            games.map { it.toLibraryItem() }
        }
    }

    override suspend fun refreshLibrary(source: GameSource): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun getGameByAppId(appId: String): LibraryItem? {
        return gameRepository.getGameByAppId(appId)?.toLibraryItem()
    }

    override fun getGameCount(source: GameSource?): Flow<Int> {
        return if (source != null) {
            gameRepository.getGameCountBySource(source)
        } else {
            gameRepository.getGameCount()
        }
    }
}
