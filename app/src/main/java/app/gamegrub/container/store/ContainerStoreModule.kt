package app.gamegrub.container.store

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * DI module for ContainerStore.
 *
 * ContainerStore is constructed via @Inject constructor(rootDir: File).
 * The File binding is provided centrally by RuntimeStoreModule (all stores
 * share context.filesDir as their root and use distinct subdirectory names).
 */
@Module
@InstallIn(SingletonComponent::class)
object ContainerStoreModule
