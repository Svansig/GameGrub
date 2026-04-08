package app.gamegrub.launch

import app.gamegrub.container.store.ContainerStore
import app.gamegrub.runtime.store.RuntimeStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LaunchEngineModule {

    @Provides
    @Singleton
    fun provideLaunchEngine(
        runtimeStore: RuntimeStore,
        containerStore: ContainerStore,
    ): LaunchEngine {
        return LaunchEngine(runtimeStore, containerStore)
    }
}
