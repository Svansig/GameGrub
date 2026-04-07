package app.gamegrub.service.epic

import android.content.Context
import android.content.Intent
import android.os.IBinder
import app.gamegrub.GameGrubApp
import app.gamegrub.data.DownloadInfo
import app.gamegrub.data.EpicCredentials
import app.gamegrub.data.EpicGame
import app.gamegrub.enums.Marker
import app.gamegrub.events.AndroidEvent
import app.gamegrub.service.NotificationHelper
import app.gamegrub.service.base.GameStoreService
import app.gamegrub.storage.StorageManager
import app.gamegrub.ui.utils.SnackbarManager
import app.gamegrub.utils.container.ContainerUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject

/**
 * Epic Games Service - thin coordinator that delegates to other Epic managers.
 */
@AndroidEntryPoint
class EpicService : GameStoreService() {

    companion object {
        private var instance: EpicService? = null
        private var isSyncInProgress: Boolean = false

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
            return isSyncInProgress || hasActiveDownload()
        }

        fun getInstance(): EpicService? = instance

        // ==========================================================================
        // DOWNLOAD OPERATIONS - Delegate to instance EpicManager
        // ==========================================================================

        fun hasActiveDownload(): Boolean {
            return getInstance()?.activeDownloads?.isNotEmpty() ?: false
        }

        fun getDownloadInfo(appId: Int): DownloadInfo? {
            return getInstance()?.activeDownloads?.get(appId)
        }

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
                GameGrubApp.events.emitJava(
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
            getInstance()?.activeDownloads?.remove(appId)
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
        ): Result<DownloadInfo> {
            val instance = getInstance() ?: return Result.failure(Exception("Service not available"))

            val game = runBlocking { instance.epicManager.getGameById(appId) }
                ?: return Result.failure(Exception("Game not found for appId: $appId"))
            val gameId = game.id

            // Check if already downloading
            if (instance.activeDownloads.containsKey(appId)) {
                Timber.tag("Epic").w("Download already in progress for $appId")
                return Result.success(instance.activeDownloads[appId]!!)
            }

            // Create DownloadInfo before launching coroutine to avoid race condition
            val downloadInfo = DownloadInfo(
                jobCount = 1,
                gameId = appId,
                downloadingAppIds = CopyOnWriteArrayList<Int>(),
            )

            instance.activeDownloads[appId] = downloadInfo
            downloadInfo.setActive(true)

            // Start download in background
            val job = instance.scope.launch {
                try {
                    val commonRedistDir = File(installPath, "_CommonRedist")
                    Timber.tag("Epic").i("Starting download for game: ${game.title}, gameId: ${game.id}")

                    val result = instance.epicDownloadManager.downloadGame(
                        context,
                        game,
                        installPath,
                        downloadInfo,
                        containerLanguage,
                        dlcGameIds,
                        commonRedistDir,
                    )

                    Timber.tag("Epic")
                        .d("Download result: ${if (result.isSuccess) "SUCCESS" else "FAILURE: ${result.exceptionOrNull()?.message}"}")

                    if (result.isSuccess) {
                        Timber.i("[Download] Completed successfully for game $gameId")
                        downloadInfo.setProgress(1.0f)
                        downloadInfo.setActive(false)

                        SnackbarManager.show("Download completed successfully!")
                    } else {
                        val error = result.exceptionOrNull()
                        Timber.e(error, "[Download] Failed for game $gameId")
                        downloadInfo.setProgress(-1.0f)
                        downloadInfo.setActive(false)

                        SnackbarManager.show("Download failed: ${error?.message ?: "Unknown error"}")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "[Download] Exception for game $gameId")
                    downloadInfo.setProgress(-1.0f)
                    downloadInfo.setActive(false)

                    SnackbarManager.show("Download error: ${e.message ?: "Unknown error"}")
                } finally {
                    instance.activeDownloads.remove(appId)
                    Timber.d("[Download] Finished for game $gameId, progress: ${downloadInfo.getProgress()}, active: ${downloadInfo.isActive()}")
                }
            }
            downloadInfo.setDownloadJob(job)

            // Return the DownloadInfo immediately so caller can track progress
            return Result.success(downloadInfo)
        }

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

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Track active downloads by GameNative Int ID
    private val activeDownloads = ConcurrentHashMap<Int, DownloadInfo>()

    private val onEndProcess: (AndroidEvent.EndProcess) -> Unit = { stop() }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Timber.tag("Epic").i("[EpicService] Service created")

        // Initialize notification helper for foreground service
        notificationHelper = NotificationHelper(applicationContext)
        GameGrubApp.events.on<AndroidEvent.EndProcess, Unit>(onEndProcess)
    }

    override fun getServiceTag(): String = "EPIC"

    override fun performSync(context: Context, isManual: Boolean) {
        Timber.tag("EPIC").i("Starting library sync (manual=$isManual)")
        if (isSyncInProgress) {
            Timber.tag("EPIC").d("Sync already in progress, skipping")
            return
        }
        isSyncInProgress = true
        val result = runBlocking(Dispatchers.IO) { epicManager.startBackgroundSync(context) }
        if (result.isFailure) {
            Timber.w("Background sync failed: ${result.exceptionOrNull()?.message}")
        } else {
            Timber.tag("EPIC").i("Background library sync completed successfully")
        }
        isSyncInProgress = false
    }

    override fun getNotificationTitle(): String = "Epic Games"

    override fun getNotificationContent(): String = "Connected"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.tag("EPIC").d("onStartCommand() - action: ${intent?.action}")

        getInstance()
        startForeground(1, notificationHelper.createForegroundNotification("Connected"))

        handleStartCommand(intent)

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.tag("Epic").i("[EpicService] Service destroyed")
        GameGrubApp.events.off<AndroidEvent.EndProcess, Unit>(onEndProcess)
        stopForeground(STOP_FOREGROUND_REMOVE)
        notificationHelper.cancel()
        instance = null
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
