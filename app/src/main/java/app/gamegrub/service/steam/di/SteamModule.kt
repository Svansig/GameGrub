package app.gamegrub.service.steam.di

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
    fun provideSteamCloudClient(): SteamCloudClient = SteamCloudClientAdapter()

    @Provides
    @Singleton
    fun provideGameEventEmitter(): GameEventEmitter = GameEventEmitterAdapter()

    @Provides
    @Singleton
    fun provideSteamPreferences(): SteamPreferences = SteamPreferencesAdapter()
}
