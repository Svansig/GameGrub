package app.gamegrub.session

import app.gamegrub.cache.CacheController
import app.gamegrub.container.store.ContainerStore
import app.gamegrub.runtime.store.RuntimeStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SessionAssemblerModule {

    @Provides
    @Singleton
    fun provideSessionAssembler(
        runtimeStore: RuntimeStore,
        containerStore: ContainerStore,
        cacheController: CacheController,
    ): SessionAssembler {
        return SessionAssembler(runtimeStore, containerStore, cacheController)
    }
}
