package app.gamegrub.gateway.impl

import android.content.Context
import app.gamegrub.data.GameSource
import app.gamegrub.data.LibraryItem
import app.gamegrub.data.extension.toLibraryItem
import app.gamegrub.data.repository.GameRepository
import app.gamegrub.db.dao.AmazonGameDao
import app.gamegrub.db.dao.AppInfoDao
import app.gamegrub.db.dao.EpicGameDao
import app.gamegrub.db.dao.GOGGameDao
import app.gamegrub.db.dao.SteamAppDao
import app.gamegrub.gateway.LibraryGateway
import app.gamegrub.gateway.LibrarySourceSnapshot
import app.gamegrub.service.amazon.AmazonService
import app.gamegrub.service.epic.EpicService
import app.gamegrub.service.gog.GOGService
import app.gamegrub.service.steam.SteamService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryGatewayImpl @Inject constructor(
    private val gameRepository: GameRepository,
    private val steamAppDao: SteamAppDao,
    private val gogGameDao: GOGGameDao,
    private val epicGameDao: EpicGameDao,
    private val amazonGameDao: AmazonGameDao,
    private val appInfoDao: AppInfoDao,
    @param:ApplicationContext private val context: Context,
) : LibraryGateway {

    override fun observeSourceSnapshot(): Flow<LibrarySourceSnapshot> {
        return combine(
            steamAppDao.getAllOwnedLibraryApps(),
            gogGameDao.getAll(),
            epicGameDao.getAll(),
            amazonGameDao.getAll(),
            appInfoDao.observeDownloadedAppIds(),
        ) { steamApps, gogGames, epicGames, amazonGames, downloadedIds ->
            LibrarySourceSnapshot(
                steamApps = steamApps,
                gogGames = gogGames,
                epicGames = epicGames,
                amazonGames = amazonGames,
                downloadedSteamAppIds = downloadedIds.toSet(),
            )
        }
    }

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
        return runCatching {
            when (source) {
                GameSource.STEAM -> {
                    SteamService.refreshOwnedGamesFromServer()
                }

                GameSource.GOG -> {
                    if (GOGService.hasStoredCredentials(context)) {
                        GOGService.triggerLibrarySync(context)
                    }
                }

                GameSource.EPIC -> {
                    if (EpicService.hasStoredCredentials(context)) {
                        EpicService.triggerLibrarySync(context)
                    }
                }

                GameSource.AMAZON -> {
                    if (AmazonService.hasStoredCredentials(context)) {
                        AmazonService.triggerLibrarySync(context)
                    }
                }

                GameSource.CUSTOM_GAME -> Unit
            }
        }
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
