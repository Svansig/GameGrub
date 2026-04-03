package app.gamegrub.di

import app.gamegrub.data.repository.GameRepository
import app.gamegrub.data.sync.GameSyncManager
import app.gamegrub.db.dao.GameDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object RepositoryModule {

    @Provides
    @Singleton
    fun provideGameRepository(gameDao: GameDao): GameRepository {
        return GameRepository(gameDao)
    }

    @Provides
    @Singleton
    fun provideGameSyncManager(
        gameDao: GameDao,
        steamAppDao: app.gamegrub.db.dao.SteamAppDao,
        gogGameDao: app.gamegrub.db.dao.GOGGameDao,
        epicGameDao: app.gamegrub.db.dao.EpicGameDao,
        amazonGameDao: app.gamegrub.db.dao.AmazonGameDao,
    ): GameSyncManager {
        return GameSyncManager(
            gameDao = gameDao,
            steamAppDao = steamAppDao,
            gogGameDao = gogGameDao,
            epicGameDao = epicGameDao,
            amazonGameDao = amazonGameDao,
        )
    }
}
