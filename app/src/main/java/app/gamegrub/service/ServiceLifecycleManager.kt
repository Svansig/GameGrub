package app.gamegrub.service

import android.content.Context
import app.gamegrub.GameGrubApp
import app.gamegrub.service.epic.EpicService
import app.gamegrub.service.gog.GOGService
import app.gamegrub.service.steam.SteamService
import timber.log.Timber

/**
 * Manages lifecycle of background services (Steam, GOG, Epic).
 * Handles starting, stopping, and auto-stop logic based on app state
 * and active operations.
 */
object ServiceLifecycleManager {

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

        if (GOGService.isRunning && !isChangingConfigurations) {
            Timber.i("Stopping GOG Service")
            GOGService.stop()
        }

        if (EpicService.isRunning && !isChangingConfigurations) {
            Timber.i("Stopping EpicService - app destroyed")
            EpicService.stop()
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

        // Stop GOGService if running and no downloads in progress
        if (GOGService.isRunning && !isChangingConfigurations) {
            if (!GOGService.hasActiveOperations()) {
                Timber.i("Stopping GOG Service - no active operations")
                GOGService.stop()
            }
        }

        // Stop EpicService if running, unless there are active downloads or sync operations
        if (EpicService.isRunning && !isChangingConfigurations) {
            if (!EpicService.hasActiveOperations()) {
                Timber.i("Stopping EpicService - no active operations")
                EpicService.stop()
            } else {
                Timber.d("EpicService kept running - has active operations")
            }
        }
    }

    /**
     * Handle app coming to foreground.
     * Restarts services that may have been stopped while backgrounded.
     */
    fun onResume(context: Context) {
        // Restart GOG service if it went down
        if (GOGService.hasStoredCredentials(context) && !GOGService.isRunning) {
            Timber.i("GOG service was down on resume - restarting")
            GOGService.start(context)
        }

        // Restart EpicService if it went down and user is authenticated
        if (EpicService.hasStoredCredentials(context) && !EpicService.isRunning) {
            Timber.i("EpicService was down on resume - restarting")
            EpicService.start(context)
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
