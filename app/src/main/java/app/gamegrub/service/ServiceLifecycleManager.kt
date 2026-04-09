package app.gamegrub.service

import android.content.Context
import app.gamegrub.data.GameSource
import app.gamegrub.service.base.GameStoreCoordinator
import app.gamegrub.service.steam.SteamService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * Manages lifecycle of background services (Steam, GOG, Epic).
 * Handles starting, stopping, and auto-stop logic based on app state
 * and active operations.
 */
@Singleton
class ServiceLifecycleManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storeCoordinators: Set<@JvmSuppressWildcards GameStoreCoordinator>,
) {

    private val nonSteamCoordinators: List<GameStoreCoordinator>
        get() = storeCoordinators.filter { it.gameSource != GameSource.STEAM }

    private fun getCoordinator(gameSource: GameSource): GameStoreCoordinator? {
        return storeCoordinators.find { it.gameSource == gameSource }
    }

    /**
     * Stop services when app is being destroyed.
     * Only stops if not changing configuration (e.g., rotation).
     */
    fun onDestroy(isChangingConfigurations: Boolean) {
        if (SteamService.isConnected && !SteamService.isLoggedIn &&
            !isChangingConfigurations && !SteamService.keepAlive
        ) {
            Timber.i("Stopping Steam Service")
            SteamService.stop()
        }

        nonSteamCoordinators.forEach { coordinator ->
            if (coordinator.isRunning && !isChangingConfigurations) {
                Timber.i("Stopping ${coordinator.gameSource} Service")
                coordinator.stop()
            }
        }
    }

    /**
     * Handle app going to background.
     * Stops services with no active operations to conserve resources.
     */
    fun onStop(isChangingConfigurations: Boolean) {
        // Stop SteamService only if no downloads or sync are in progress
        if (!isChangingConfigurations &&
            SteamService.isConnected &&
            !SteamService.hasActiveOperations() &&
            !SteamService.isLoginInProgress &&
            !SteamService.keepAlive &&
            !SteamService.isImporting
        ) {
            Timber.i("Stopping SteamService - no active operations")
            SteamService.stop()
        }

        // Stop non-Steam coordinators if running and no active operations
        nonSteamCoordinators.forEach { coordinator ->
            if (coordinator.isRunning && !isChangingConfigurations) {
                if (!coordinator.hasActiveOperations()) {
                    Timber.i("Stopping ${coordinator.gameSource} Service - no active operations")
                    coordinator.stop()
                } else {
                    Timber.d("${coordinator.gameSource} Service kept running - has active operations")
                }
            }
        }
    }

    /**
     * Handle app coming to foreground.
     * Restarts services that may have been stopped while backgrounded.
     */
    fun onResume() {
        // Restart non-Steam services if they went down but have stored credentials
        nonSteamCoordinators.forEach { coordinator ->
            val hasCredentials = coordinator.hasStoredCredentials()
            val isRunning = coordinator.isRunning
            if (hasCredentials && !isRunning) {
                Timber.i("${coordinator.gameSource} service was down on resume - restarting")
                coordinator.start()
            }
        }
    }

    /**
     * Log current service state for debugging.
     */
    fun logState() {
        Timber.d(
            "Steam: connected=%b, loggedIn=%b, keepAlive=%b, importing=%b",
            SteamService.isConnected,
            SteamService.isLoggedIn,
            SteamService.keepAlive,
            SteamService.isImporting,
        )
    }
}
