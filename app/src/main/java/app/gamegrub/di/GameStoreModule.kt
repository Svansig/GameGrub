package app.gamegrub.di

import app.gamegrub.service.amazon.AmazonStoreCoordinator
import app.gamegrub.service.base.GameStoreCoordinator
import app.gamegrub.service.epic.EpicStoreCoordinator
import app.gamegrub.service.gog.GOGStoreCoordinator
import app.gamegrub.service.steam.SteamStoreCoordinator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@InstallIn(SingletonComponent::class)
@Module
abstract class GameStoreModule {

    @Binds
    @IntoSet
    abstract fun bindAmazonCoordinator(coordinator: AmazonStoreCoordinator): GameStoreCoordinator

    @Binds
    @IntoSet
    abstract fun bindEpicCoordinator(coordinator: EpicStoreCoordinator): GameStoreCoordinator

    @Binds
    @IntoSet
    abstract fun bindGOGCoordinator(coordinator: GOGStoreCoordinator): GameStoreCoordinator

    @Binds
    @IntoSet
    abstract fun bindSteamCoordinator(coordinator: SteamStoreCoordinator): GameStoreCoordinator
}
