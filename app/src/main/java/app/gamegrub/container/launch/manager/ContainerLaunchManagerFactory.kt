package app.gamegrub.container.launch.manager

/**
 * Temporary factory until launch manager wiring moves to DI.
 */
internal object ContainerLaunchManagerFactory {
    fun create(): ContainerLaunchManager {
        return StoreResolverContainerLaunchManager()
    }
}
