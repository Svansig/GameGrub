package app.gamegrub.domain.usecase

import app.gamegrub.data.GameFilter
import app.gamegrub.data.GameSource
import app.gamegrub.data.LibraryItem
import app.gamegrub.data.SortOption
import app.gamegrub.gateway.LibraryGateway
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetGamesUseCase @Inject constructor(
    private val libraryGateway: LibraryGateway,
) {
    operator fun invoke(filter: GameFilter = GameFilter()): Flow<List<LibraryItem>> {
        val gamesFlow = when {
            filter.source != null -> libraryGateway.getGamesBySource(filter.source)
            filter.isInstalled == true -> libraryGateway.getInstalledGames()
            else -> libraryGateway.getAllGames()
        }

        return gamesFlow.map { games ->
            var result = games

            if (filter.searchQuery.isNotEmpty()) {
                result = result.filter { it.name.contains(filter.searchQuery, ignoreCase = true) }
            }

            result = when (filter.sortBy) {
                SortOption.NAME_ASC -> result.sortedBy { it.name.lowercase() }
                SortOption.NAME_DESC -> result.sortedByDescending { it.name.lowercase() }
                SortOption.LAST_PLAYED -> result.sortedByDescending { it.isInstalled }
                SortOption.PLAY_TIME -> result.sortedByDescending { it.isInstalled }
                SortOption.INSTALL_SIZE -> result.sortedByDescending { it.sizeBytes }
                SortOption.RELEASE_DATE -> result.sortedByDescending { it.isInstalled }
            }

            result
        }
    }
}

class GetGameByIdUseCase @Inject constructor(
    private val libraryGateway: LibraryGateway,
) {
    suspend operator fun invoke(appId: String): LibraryItem? {
        return libraryGateway.getGameByAppId(appId)
    }
}

class RefreshLibraryUseCase @Inject constructor(
    private val libraryGateway: LibraryGateway,
) {
    suspend operator fun invoke(source: GameSource): Result<Unit> {
        return libraryGateway.refreshLibrary(source)
    }
}

class SearchGamesUseCase @Inject constructor(
    private val libraryGateway: LibraryGateway,
) {
    operator fun invoke(query: String): Flow<List<LibraryItem>> {
        return libraryGateway.searchGames(query)
    }
}
