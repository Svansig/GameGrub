package app.gamegrub.di

import app.gamegrub.device.AndroidDeviceQueryManager
import app.gamegrub.device.DeviceQueryGateway
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency module for device/hardware query abstractions.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DeviceQueryModule {
    /**
     * Bind [AndroidDeviceQueryManager] as the canonical [DeviceQueryGateway] implementation.
     *
     * @param impl Concrete Android-backed device query manager.
     * @return Gateway interface used by application layers.
     */
    @Binds
    @Singleton
    abstract fun bindDeviceQueryGateway(impl: AndroidDeviceQueryManager): DeviceQueryGateway
}

