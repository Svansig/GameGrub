package app.gamegrub.service.epic

import android.content.Context
import android.content.Intent
import android.os.IBinder
import app.gamegrub.data.DownloadInfo
import app.gamegrub.data.EpicCredentials
import app.gamegrub.data.EpicGame
import app.gamegrub.enums.Marker
import app.gamegrub.events.AndroidEvent
import app.gamegrub.service.NotificationHelper
import app.gamegrub.service.base.GameStoreService
import app.gamegrub.storage.StorageManager
import app.gamegrub.ui.runtime.XServerRuntime
import app.gamegrub.utils.container.ContainerUtils
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Epic Games Service - thin coordinator that delegates to other Epic managers.
 */
@AndroidEntryPoint
class EpicService : GameStoreService() {

    companion object {
        private var instance: EpicService? = null

        val isRunning: Boolean
            get() = instance != null

        fun start(context: Context) {
            if (instance != null) {
                Timber.tag("EPIC").d("[EpicService] Service already running, skipping start")
                return
            }
            val intent = Intent(context, EpicService::class.java).apply {
                action = ACTION_SYNC_LIBRARY
            }
            context.startForegroundService(intent)
        }

        fun triggerLibrarySync(context: Context) {
            if (instance != null) {
                val intent = Intent(context, EpicService::class.java).apply {
                    action = ACTION_MANUAL_SYNC
                }
                context.startForegroundService(intent)
            }
        }

        fun stop() {
            instance?.stopSelf()
        }

        // ==========================================================================
        // AUTHENTICATION - Delegate to EpicAuthManager
        // ==========================================================================

        suspend fun authenticateWithCode(context: Context, authorizationCode: String): Result<EpicCredentials> {
            return EpicAuthManager.authenticateWithCode(context, authorizationCode)
        }

        fun hasStoredCredentials(context: Context): Boolean {
            return EpicAuthManager.hasStoredCredentials(context)
        }

        suspend fun getStoredCredentials(context: Context): Result<EpicCredentials> {
            return EpicAuthManager.getStoredCredentials(context)
        }

        /**
         * Logout from Epic - clears credentials, database, and stops service
         */
        suspend fun logout(context: Context): Result<Unit> {
            return withContext(Dispatchers.IO) {
                try {
                    Timber.tag("EPIC").i("Logging out from Epic...")

                    // Clear stored credentials first, regardless of service state
                    val credentialsCleared = EpicAuthManager.clearStoredCredentials(context)
                    if (!credentialsCleared) {
                        Timber.tag("Epic").e("Failed to clear credentials during logout")
                        return@withContext Result.failure(Exception("Failed to clear stored credentials"))
                    }

                    // Get instance to clean up service-specific data
                    val instance = getInstance()
                    if (instance != null) {
                        // Clear all nonInstalled Epic games from database
                        instance.epicManager.deleteAllNonInstalledGames()
                        Timber.tag("Epic").i("All Non-installed Epic games removed from database")

                        // Stop the service
                        stop()
                    } else {
                        Timber.tag("Epic").w("Service not running during logout, but credentials were cleared")
                    }

                    Timber.tag("Epic").i("Logout completed successfully")
                    Result.success(Unit)
                } catch (e: Exception) {
                    Timber.tag("Epic").e(e, "Error during logout")
                    Result.failure(e)
                }
            }
        }

        // ==========================================================================
        // SYNC & OPERATIONS
        // ==========================================================================

        fun hasActiveOperations(): Boolean {
            return instance?.syncInProgress ?: false || hasActiveDownload()
        }

        internal fun getInstance(): EpicService? = instance

        // Set by onCreate/onDestroy to give companion access to coordinator's download state.
        @Volatile
        internal var coordinator: EpicStoreCoordinator? = null

        suspend fun getInstalledExe(gameId: Int): String =
            getInstance()?.epicManager?.getInstalledExe(gameId) ?: ""

        // ==========================================================================
        // DOWNLOAD OPERATIONS - Delegate to EpicStoreCoordinator
        // ==========================================================================

        fun hasActiveDownload(): Boolean = coordinator?.hasActiveDownload() ?: false

        fun getDownloadInfo(appId: Int): DownloadInfo? = coordinator?.getDownloadInfo(appId)

        suspend fun deleteGame(context: Context, appId: Int): Result<Unit> {
            val instance = getInstance() ?: return Result.failure(Exception("Service not available"))

            return try {
                // Get the game to find its install path
                val game = instance.epicManager.getGameById(appId) ?: return Result.failure(Exception("Game not found: $appId"))

                val path = game.installPath.ifEmpty { EpicConstants.getGameInstallPath(context, game.appName) }
                if (File(path).exists()) {
                    Timber.tag("Epic").i("Deleting installation folder: $path")
                    val deleted = File(path).deleteRecursively()
                    if (deleted) {
                        Timber.tag("Epic").i("Successfully deleted installation folder")
                    } else {
                        Timber.tag("Epic").w("Failed to delete some files in installation folder")
                    }
                    StorageManager.removeMarker(path, Marker.DOWNLOAD_COMPLETE_MARKER)
                    StorageManager.removeMarker(path, Marker.DOWNLOAD_IN_PROGRESS_MARKER)
                }

                // Uninstall from database (keeps the entry but marks as not installed)
                instance.epicManager.uninstall(appId)

                // Delete container
                // Use game.id (the auto-generated numeric Room DB primary key) to match the container
                // ID format used at creation time: "EPIC_${libraryItem.gameId}" = "EPIC_${game.id}".
                // Previously used game.appName (the Legendary identifier, e.g. a UUID) which never
                // matched the stored container ID, causing orphaned containers.
                withContext(Dispatchers.Main) {
                    ContainerUtils.deleteContainer(context, "EPIC_${game.id}")
                }

                // Trigger library refresh event
                XServerRuntime.get().events.emitJava(
                    AndroidEvent.LibraryInstallStatusChanged(appId),
                )

                Timber.tag("Epic").i("Game uninstalled: $appId")
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.tag("Epic").e(e, "Failed to uninstall game: $appId")
                Result.failure(e)
            }
        }

        suspend fun cleanupDownload(context: Context, appId: Int) {
            withContext(Dispatchers.IO) {
                getInstance()?.epicManager?.getGameById(appId)?.let { game ->
                    val path = EpicConstants.getGameInstallPath(context, game.appName)
                    StorageManager.removeMarker(path, Marker.DOWNLOAD_IN_PROGRESS_MARKER)
                }
            }
            coordinator?.cleanupDownload(context, appId)
        }

        // ==========================================================================
        // GAME & LIBRARY OPERATIONS
        // ==========================================================================

        fun getEpicGameOf(appId: Int): EpicGame? {
            return runBlocking(Dispatchers.IO) {
                getInstance()?.epicManager?.getGameById(appId)
            }
        }

        fun getDLCForGame(appId: Int): List<EpicGame> {
            return runBlocking(Dispatchers.IO) {
                getInstance()?.epicManager?.getDLCForTitle(appId) ?: emptyList()
            }
        }

        suspend fun updateEpicGame(game: EpicGame) {
            getInstance()?.epicManager?.updateGame(game)
        }

        fun isGameInstalled(context: Context, appId: Int): Boolean {
            val game = getEpicGameOf(appId) ?: return false

            if (game.isInstalled && game.installPath.isNotEmpty()) {
                return StorageManager.hasMarker(game.installPath, Marker.DOWNLOAD_COMPLETE_MARKER)
            }

            val installPath = game.installPath.takeIf { it.isNotEmpty() }
                ?: game.appName.takeIf { it.isNotEmpty() }?.let {
                    EpicConstants.getGameInstallPath(context, it)
                }
                ?: return false

            val isDownloadComplete = StorageManager.hasMarker(installPath, Marker.DOWNLOAD_COMPLETE_MARKER)
            val isDownloadInProgress = StorageManager.hasMarker(installPath, Marker.DOWNLOAD_IN_PROGRESS_MARKER)
            if (isDownloadComplete && !isDownloadInProgress) {
                val updatedGame = game.copy(
                    isInstalled = true,
                    installPath = installPath,
                )
                runBlocking(Dispatchers.IO) {
                    getInstance()?.epicManager?.updateGame(updatedGame)
                }
                return true
            }

            return false
        }

        fun getInstallPath(appId: Int): String? {
            val game = getEpicGameOf(appId)
            return if (game?.isInstalled == true && game.installPath.isNotEmpty()) {
                game.installPath
            } else {
                null
            }
        }

        /**
         * Resolves the effective launch executable for an Epic game.
         * Container id is expected to be "EPIC_&lt;numericId&gt;" (from library). Returns empty if
         * game is not installed, no executable can be found, or containerId cannot be parsed.
         */
        suspend fun getLaunchExecutable(containerId: String): String {
            val gameId = try {
                ContainerUtils.extractGameIdFromContainerId(containerId)
            } catch (e: Exception) {
                Timber.tag("Epic").e(e, "Failed to parse Epic containerId: $containerId")
                return ""
            }
            return getInstance()?.epicManager?.getLaunchExecutable(gameId) ?: ""
        }

        suspend fun fetchManifestSizes(context: Context, appId: Int): EpicManager.ManifestSizes {
            return getInstance()?.epicManager?.fetchManifestSizes(context, appId)
                ?: EpicManager.ManifestSizes(installSize = 0L, downloadSize = 0L)
        }

        fun downloadGame(
            context: Context,
            appId: Int,
            dlcGameIds: List<Int>,
            installPath: String,
            containerLanguage: String,
        ): Result<DownloadInfo> =
            coordinator?.downloadGame(appId, dlcGameIds, installPath, containerLanguage)
                ?: Result.failure(Exception("Epic service not available"))

        // ==========================================================================
        // Game Launcher Helpers
        // ==========================================================================

        suspend fun buildLaunchParameters(
            context: Context,
            game: EpicGame,
            offline: Boolean = false,
            languageCode: String = "en-US",
        ): Result<List<String>> {
            return EpicGameLauncher.buildLaunchParameters(context, game, offline, languageCode)
        }

        fun cleanupLaunchTokens(context: Context) {
            EpicGameLauncher.cleanupOwnershipTokens(context)
        }

        // ==========================================================================
        // CLOUD SAVES HELPERS
        // ==========================================================================
    }

    private lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var epicManager: EpicManager

    @Inject
    lateinit var epicDownloadManager: EpicDownloadManager

    @Inject
    lateinit var epicStoreCoordinator: EpicStoreCoordinator

    private val onEndProcess: (AndroidEvent.EndProcess) -> Unit = { stop() }

    override fun onCreate() {
        super.onCreate()
        instance = this
        coordinator = epicStoreCoordinator
        epicStoreCoordinator.onServiceStarted()
        Timber.tag("Epic").i("[EpicService] Service created")

        // Initialize notification helper for foreground service
        notificationHelper = NotificationHelper(applicationContext)
        XServerRuntime.get().events.on<AndroidEvent.EndProcess, Unit>(onEndProcess)
    }

    override fun getServiceTag(): String = "EPIC"

    override suspend fun performSync(context: Context, isManual: Boolean) {
        val result = epicManager.startBackgroundSync(context)
        if (result.isFailure) {
            Timber.tag("EPIC").w("Background sync failed: ${result.exceptionOrNull()?.message}")
        }
    }

    override fun getNotificationTitle(): String = "Epic Games"

    override fun getNotificationContent(): String = "Connected"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, notificationHelper.createForegroundNotification("Connected"))
        handleStartCommand(intent)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        XServerRuntime.get().events.off<AndroidEvent.EndProcess, Unit>(onEndProcess)
        stopForeground(STOP_FOREGROUND_REMOVE)
        notificationHelper.cancel()
        epicStoreCoordinator.onServiceStopped()
        coordinator = null
        instance = null
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
