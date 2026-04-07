package app.gamegrub.startup

import android.content.Context
import app.gamegrub.CrashHandler
import app.gamegrub.PrefManager
import app.gamegrub.launch.IntentLaunchManager
import app.gamegrub.network.NetworkManager
import app.gamegrub.utils.container.ContainerMigrator
import timber.log.Timber

/**
 * Coordinates app startup initialization in a well-defined order.
 * Each initializer handles a specific aspect of app startup.
 */
class StartupCoordinator {

    private val initializers: List<AppInitializer> = listOf(
        NetworkInitializer(),
        CrashHandlerInitializer(),
        PreferencesInitializer(),
        ContainerMigrationInitializer(),
        LaunchCleanupInitializer(),
    )

    fun initialize(context: Context) {
        Timber.d("[StartupCoordinator] Starting app initialization...")
        
        for (initializer in initializers) {
            try {
                Timber.d("[StartupCoordinator] Running: ${initializer::class.java.simpleName}")
                initializer.initialize(context)
            } catch (e: Exception) {
                Timber.e(e, "[StartupCoordinator] Failed: ${initializer::class.java.simpleName}")
                initializer.onFailure(context, e)
            }
        }
        
        Timber.d("[StartupCoordinator] App initialization complete")
    }
}

interface AppInitializer {
    fun initialize(context: Context)
    fun onFailure(context: Context, error: Exception) {
        Timber.e(error, "Initializer ${this::class.java.simpleName} failed")
    }
}

class NetworkInitializer : AppInitializer {
    override fun initialize(context: Context) {
        NetworkManager.init(context)
    }
}

class CrashHandlerInitializer : AppInitializer {
    override fun initialize(context: Context) {
        CrashHandler.initialize(context)
    }
}

class PreferencesInitializer : AppInitializer {
    override fun initialize(context: Context) {
        PrefManager.init(context)
    }
}

class ContainerMigrationInitializer : AppInitializer {
    override fun initialize(context: Context) {
        ContainerMigrator.migrateLegacyContainersIfNeeded(
            context = context,
            onProgressUpdate = null,
            onComplete = null,
        )
    }
}

class LaunchCleanupInitializer : AppInitializer {
    override fun initialize(context: Context) {
        app.gamegrub.launch.IntentLaunchManager.clearAllTemporaryOverrides()
    }
}