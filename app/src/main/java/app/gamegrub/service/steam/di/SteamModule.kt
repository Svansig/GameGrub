package app.gamegrub.service.steam.di

import app.gamegrub.db.dao.AppInfoDao
import app.gamegrub.db.dao.SteamAppDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SteamModule {

    @Provides
    @Singleton
    fun provideSteamClientProvider(): SteamClientProvider = SteamClientProvider()

    @Provides
    @Singleton
    fun provideSteamConnection(provider: SteamClientProvider): SteamConnection = SteamConnectionAdapter(provider)

    @Provides
    @Singleton
    fun provideSteamAuthClient(provider: SteamClientProvider): SteamAuthClient = SteamAuthClientAdapter(provider)

    @Provides
    @Singleton
    fun provideSteamUserClient(provider: SteamClientProvider): SteamUserClient = SteamUserClientAdapter(provider)

    @Provides
    @Singleton
    fun provideSteamLibraryClient(): SteamLibraryClient = SteamLibraryClientAdapter()

    @Provides
    @Singleton
    fun provideSteamCloudClient(provider: SteamClientProvider): SteamCloudClient = SteamCloudClientAdapter(provider)

    @Provides
    @Singleton
    fun provideSteamStatsClient(provider: SteamClientProvider, appInfoClient: SteamAppInfoClient): SteamStatsClient = SteamStatsClientAdapter(provider, appInfoClient)

    @Provides
    @Singleton
    fun provideSteamAppInfoClient(appDao: SteamAppDao, appInfoDao: AppInfoDao): SteamAppInfoClient = SteamAppInfoClientAdapter(appDao, appInfoDao)

    @Provides
    @Singleton
    fun provideGameEventEmitter(): GameEventEmitter = GameEventEmitterAdapter()

    @Provides
    @Singleton
    fun provideSteamPreferences(): SteamPreferences = SteamPreferencesAdapter()
}
