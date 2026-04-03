package app.gamegrub.gateway

import app.gamegrub.data.GameSource
import app.gamegrub.data.LibraryItem
import kotlinx.coroutines.flow.Flow

interface LibraryGateway {
    fun getAllGames(): Flow<List<LibraryItem>>

    fun getGamesBySource(source: GameSource): Flow<List<LibraryItem>>

    fun getInstalledGames(): Flow<List<LibraryItem>>

    fun searchGames(query: String): Flow<List<LibraryItem>>

    suspend fun refreshLibrary(source: GameSource): Result<Unit>

    suspend fun getGameByAppId(appId: String): LibraryItem?

    fun getGameCount(source: GameSource? = null): Flow<Int>
}
