package app.gamegrub.di

import app.gamegrub.gateway.AuthGateway
import app.gamegrub.gateway.AuthStateGateway
import app.gamegrub.gateway.CloudSavesGateway
import app.gamegrub.gateway.DownloadGateway
import app.gamegrub.gateway.LaunchGateway
import app.gamegrub.gateway.LaunchRequestGateway
import app.gamegrub.gateway.LibraryGateway
import app.gamegrub.gateway.PreferencesGateway
import app.gamegrub.gateway.StorageGateway
import app.gamegrub.gateway.impl.AuthGatewayImpl
import app.gamegrub.gateway.impl.AuthStateGatewayImpl
import app.gamegrub.gateway.impl.CloudSavesGatewayImpl
import app.gamegrub.gateway.impl.DownloadGatewayImpl
import app.gamegrub.gateway.impl.LaunchGatewayImpl
import app.gamegrub.gateway.impl.LaunchRequestGatewayImpl
import app.gamegrub.gateway.impl.LibraryGatewayImpl
import app.gamegrub.gateway.impl.PreferencesGatewayImpl
import app.gamegrub.gateway.impl.StorageGatewayImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
abstract class GatewayModule {

    @Binds
    @Singleton
    abstract fun bindLibraryGateway(impl: LibraryGatewayImpl): LibraryGateway

    @Binds
    @Singleton
    abstract fun bindAuthGateway(impl: AuthGatewayImpl): AuthGateway

    @Binds
    @Singleton
    abstract fun bindAuthStateGateway(impl: AuthStateGatewayImpl): AuthStateGateway

    @Binds
    @Singleton
    abstract fun bindLaunchGateway(impl: LaunchGatewayImpl): LaunchGateway

    @Binds
    @Singleton
    abstract fun bindLaunchRequestGateway(impl: LaunchRequestGatewayImpl): LaunchRequestGateway

    @Binds
    @Singleton
    abstract fun bindDownloadGateway(impl: DownloadGatewayImpl): DownloadGateway

    @Binds
    @Singleton
    abstract fun bindCloudSavesGateway(impl: CloudSavesGatewayImpl): CloudSavesGateway

    @Binds
    @Singleton
    abstract fun bindPreferencesGateway(impl: PreferencesGatewayImpl): PreferencesGateway

    @Binds
    @Singleton
    abstract fun bindStorageGateway(impl: StorageGatewayImpl): StorageGateway
}
