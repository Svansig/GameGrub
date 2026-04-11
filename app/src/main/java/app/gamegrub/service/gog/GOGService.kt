package app.gamegrub.service.gog

import android.content.Context
import android.content.Intent
import android.os.IBinder
import app.gamegrub.data.DownloadInfo
import app.gamegrub.data.GOGCredentials
import app.gamegrub.data.GOGGame
import app.gamegrub.data.LaunchInfo
import app.gamegrub.data.LibraryItem
import app.gamegrub.events.AndroidEvent
import app.gamegrub.service.NotificationHelper
import app.gamegrub.service.base.GameStoreService
import app.gamegrub.ui.runtime.XServerRuntime
import app.gamegrub.utils.container.ContainerUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * GOG Service - thin abstraction layer that delegates to managers.
 *
 * Architecture:
 * - GOGApiClient: Api Layer for interacting with GOG's APIs
 * - GOGDownloadManager: Handles Download Logic for Games
 * - GOGConstants: Shared Constants for our GOG-related data
 * - GOGCloudSavesManager: Handler for Cloud Saves
 * - GOGAuthManager: Authentication and account management
 * - GOGManager: Game library, downloads, and installation
 * - GOGManifestParser: Parses and has utils for parsing/extracting/decompressing manifests.
 * - GOGDataMdoels: Data Models for GOG-related Data types such as API responses
 *
 */
@AndroidEntryPoint
class GOGService : GameStoreService() {

    companion object {
        private var instance: GOGService? = null

        val isRunning: Boolean
            get() = instance != null

        fun start(context: Context) {
            if (instance != null) {
                Timber.tag("GOG").d("[GOGService] Service already running, skipping start")
                return
            }
            val intent = Intent(context, GOGService::class.java).apply {
                action = ACTION_SYNC_LIBRARY
            }
            context.startForegroundService(intent)
        }

        fun triggerLibrarySync(context: Context) {
            if (instance == null) {
                return
            }
            val intent = Intent(context, GOGService::class.java).apply {
                action = ACTION_MANUAL_SYNC
            }
            context.startForegroundService(intent)
        }

        fun stop() {
            instance?.stopSelf()
        }

        fun hasActiveOperations(): Boolean {
            return instance?.syncInProgress ?: false || hasActiveDownload()
        }

        // ==========================================================================
        // AUTHENTICATION - Delegate to GOGAuthManager
        // ==========================================================================

        suspend fun authenticateWithCode(context: Context, authorizationCode: String): Result<GOGCredentials> {
            return GOGAuthManager.authenticateWithCode(context, authorizationCode)
        }

        fun hasStoredCredentials(context: Context): Boolean {
            return GOGAuthManager.hasStoredCredentials(context)
        }

        suspend fun getStoredCredentials(context: Context): Result<GOGCredentials> {
            return GOGAuthManager.getStoredCredentials(context)
        }

        suspend fun validateCredentials(context: Context): Result<Boolean> {
            return GOGAuthManager.validateCredentials(context)
        }

        fun clearStoredCredentials(context: Context): Boolean {
            return GOGAuthManager.clearStoredCredentials(context)
        }

        /**
         * Logout from GOG - clears credentials, database, and stops service
         */
        suspend fun logout(context: Context): Result<Unit> {
            return withContext(Dispatchers.IO) {
                try {
                    Timber.i("[GOGService] Logging out from GOG...")

                    // Get instance first before stopping the service
                    val instance = getInstance()
                    if (instance == null) {
                        Timber.w("[GOGService] Service instance not available during logout")
                        return@withContext Result.failure(Exception("Service not running"))
                    }

                    // Clear stored credentials
                    val credentialsCleared = clearStoredCredentials(context)
                    if (!credentialsCleared) {
                        Timber.w("[GOGService] Failed to clear credentials during logout")
                    }

                    // Clear all non-installed GOG games from database
                    instance.gogManager.deleteAllNonInstalledGames()
                    Timber.i("[GOGService] All non-installed GOG games removed from database")

                    // Stop the service
                    stop()

                    Timber.i("[GOGService] Logout completed successfully")
                    Result.success(Unit)
                } catch (e: Exception) {
                    Timber.e(e, "[GOGService] Error during logout")
                    Result.failure(e)
                }
            }
        }

        // ==========================================================================
        // SYNC & OPERATIONS
        // ==========================================================================

        internal fun getInstance(): GOGService? = instance

        // Set by onCreate/onDestroy to give companion access to coordinator's download state.
        @Volatile
        internal var coordinator: GOGStoreCoordinator? = null

        fun getScriptInterpreterPartsForLaunch(appId: String): List<String>? =
            getInstance()?.gogManager?.getScriptInterpreterPartsForLaunchSync(appId)

        suspend fun downloadDependencies(
            gameId: String,
            dependencies: List<String>,
            gameDir: File,
            supportDir: File,
            onProgress: ((Float) -> Unit) = {},
        ): Result<Unit> = getInstance()?.gogDownloadManager?.downloadDependenciesWithProgress(
            gameId = gameId,
            dependencies = dependencies,
            gameDir = gameDir,
            supportDir = supportDir,
            onProgress = onProgress,
        ) ?: Result.failure(Exception("GOG service not available"))

        // ==========================================================================
        // DOWNLOAD OPERATIONS - Delegate to GOGStoreCoordinator
        // ==========================================================================

        fun hasActiveDownload(): Boolean = coordinator?.hasActiveDownload() ?: false

        fun getCurrentlyDownloadingGame(): String? = coordinator?.getCurrentlyDownloadingGame()

        fun getDownloadInfo(gameId: String): DownloadInfo? = coordinator?.getDownloadInfoByGameId(gameId)

        fun cleanupDownload(gameId: String) = coordinator?.cleanupDownload(gameId)

        fun cancelDownload(gameId: String): Boolean =
            coordinator?.cancelDownloadByGameId(gameId) ?: false

        fun downloadGame(context: Context, gameId: String, installPath: String, containerLanguage: String): Result<DownloadInfo?> =
            coordinator?.downloadGame(gameId, installPath, containerLanguage)
                ?: Result.failure(Exception("GOG service not available"))

        // ==========================================================================
        // GAME & LIBRARY OPERATIONS - Delegate to instance GOGManager
        // ==========================================================================

        fun getGOGGameOf(gameId: String): GOGGame? {
            return runBlocking(Dispatchers.IO) {
                getInstance()?.gogManager?.getGameFromDbById(gameId)
            }
        }

        suspend fun updateGOGGame(game: GOGGame) {
            getInstance()?.gogManager?.updateGame(game)
        }

        fun isGameInstalled(gameId: String): Boolean {
            return runBlocking(Dispatchers.IO) {
                val game = getInstance()?.gogManager?.getGameFromDbById(gameId)
                if (game?.isInstalled != true) {
                    return@runBlocking false
                }

                // Verify the installation is actually valid
                val (isValid, errorMessage) = getInstance()?.gogManager?.verifyInstallation(gameId)
                    ?: Pair(false, "Service not available")
                if (!isValid) {
                    Timber.w("Game $gameId marked as installed but verification failed: $errorMessage")
                }
                isValid
            }
        }

        fun getInstallPath(gameId: String): String? {
            return runBlocking(Dispatchers.IO) {
                val game = getInstance()?.gogManager?.getGameFromDbById(gameId)
                if (game?.isInstalled == true) game.installPath else null
            }
        }

//        fun verifyInstallation(gameId: String): Pair<Boolean, String?> {
//            return getInstance()?.gogManager?.verifyInstallation(gameId)
//                ?: Pair(false, "Service not available")
//        }

        suspend fun getInstalledExe(libraryItem: LibraryItem): String {
            return getInstance()?.gogManager?.getInstalledExe(libraryItem)
                ?: ""
        }

        /**
         * Resolves the effective launch executable for a GOG game (container config or auto-detected).
         * Returns empty string if no executable can be found.
         */
        suspend fun getLaunchExecutable(appId: String, container: com.winlator.container.Container): String {
            return getInstance()?.gogManager?.getLaunchExecutable(appId, container) ?: ""
        }

        suspend fun getGogWineStartCommand(
            libraryItem: LibraryItem,
            container: com.winlator.container.Container,
            bootToContainer: Boolean,
            appLaunchInfo: LaunchInfo?,
            envVars: com.winlator.core.envvars.EnvVars,
            guestProgramLauncherComponent: com.winlator.xenvironment.components.GuestProgramLauncherComponent,
            gameId: Int,
        ): String {
            return getInstance()?.gogManager?.getGogWineStartCommand(
                libraryItem, container, bootToContainer, appLaunchInfo, envVars, guestProgramLauncherComponent, gameId,
            ) ?: "\"explorer.exe\""
        }

        fun getGogWineStartCommandSync(
            libraryItem: LibraryItem,
            container: com.winlator.container.Container,
            bootToContainer: Boolean,
            appLaunchInfo: LaunchInfo?,
            envVars: com.winlator.core.envvars.EnvVars,
            guestProgramLauncherComponent: com.winlator.xenvironment.components.GuestProgramLauncherComponent,
            gameId: Int,
        ): String = runBlocking(Dispatchers.IO) {
            getGogWineStartCommand(
                libraryItem, container, bootToContainer, appLaunchInfo, envVars, guestProgramLauncherComponent, gameId,
            )
        }

        suspend fun refreshLibrary(context: Context): Result<Int> {
            return getInstance()?.gogManager?.refreshLibrary(context)
                ?: Result.failure(Exception("Service not available"))
        }

        suspend fun refreshSingleGame(gameId: String, context: Context): Result<GOGGame?> {
            return getInstance()?.gogManager?.refreshSingleGame(gameId, context)
                ?: Result.failure(Exception("Service not available"))
        }

        /**
         * Delete/uninstall a GOG game
         * Delegates to GOGManager.deleteGame
         */
        suspend fun deleteGame(context: Context, libraryItem: LibraryItem): Result<Unit> {
            return getInstance()?.gogManager?.deleteGame(context, libraryItem)
                ?: Result.failure(Exception("Service not available"))
        }

        /**
         * Sync GOG cloud saves for a game
         * @param context Android context
         * @param appId Game app ID (e.g., "gog_123456")
         * @param preferredAction Preferred sync action: "download", "upload", or "none"
         * @return true if sync succeeded, false otherwise
         */
        suspend fun syncCloudSaves(
            context: Context,
            appId: String,
            preferredAction: String = "none",
        ): Boolean = withContext(Dispatchers.IO) {
            try {
                Timber.tag("GOG").d("[Cloud Saves] syncCloudSaves called for $appId with action: $preferredAction")

                // Check if there's already a sync in progress for this appId
                val serviceInstance = getInstance()
                if (serviceInstance == null) {
                    Timber.tag("GOG").e("[Cloud Saves] Service instance not available for sync start")
                    return@withContext false
                }

                if (!serviceInstance.gogManager.startSync(appId)) {
                    Timber.tag("GOG").w("[Cloud Saves] Sync already in progress for $appId, skipping duplicate sync")
                    return@withContext false
                }

                try {
                    val instance = getInstance()
                    if (instance == null) {
                        Timber.tag("GOG").e("[Cloud Saves] Service instance not available")
                        return@withContext false
                    }

                    if (!GOGAuthManager.hasStoredCredentials(context)) {
                        Timber.tag("GOG").e("[Cloud Saves] Cannot sync saves: not authenticated")
                        return@withContext false
                    }

                    val authConfigPath = GOGAuthManager.getAuthConfigPath(context)
                    Timber.tag("GOG").d("[Cloud Saves] Using auth config path: $authConfigPath")

                    // Get game info
                    val gameId = ContainerUtils.extractGameIdFromContainerId(appId)
                    Timber.tag("GOG").d("[Cloud Saves] Extracted game ID: $gameId from appId: $appId")
                    val game = instance.gogManager.getGameFromDbById(gameId.toString())

                    if (game == null) {
                        Timber.tag("GOG").e("[Cloud Saves] Game not found for appId: $appId")
                        return@withContext false
                    }
                    Timber.tag("GOG").d("[Cloud Saves] Found game: ${game.title}")

                    // Get save directory paths (Android runs games through Wine, so always Windows)
                    Timber.tag("GOG").d("[Cloud Saves] Resolving save directory paths for $appId")
                    val saveLocations = instance.gogManager.getSaveDirectoryPath(context, appId, game.title)

                    if (saveLocations.isNullOrEmpty()) {
                        Timber.tag("GOG").w("[Cloud Saves] No save locations found for game $appId (cloud saves may not be enabled)")
                        return@withContext false
                    }
                    Timber.tag("GOG").i("[Cloud Saves] Found ${saveLocations.size} save location(s) for $appId")

                    var allSucceeded = true

                    // Sync each save location
                    for ((index, location) in saveLocations.withIndex()) {
                        try {
                            Timber.tag("GOG").d("[Cloud Saves] Processing location ${index + 1}/${saveLocations.size}: '${location.name}'")

                            // Log directory state BEFORE sync
                            try {
                                val saveDir = File(location.location)
                                Timber.tag("GOG").d("[Cloud Saves] [BEFORE] Checking directory: ${location.location}")
                                Timber.tag("GOG")
                                    .d("[Cloud Saves] [BEFORE] Directory exists: ${saveDir.exists()}, isDirectory: ${saveDir.isDirectory}")
                                if (saveDir.exists() && saveDir.isDirectory) {
                                    val filesBefore = saveDir.listFiles()
                                    if (filesBefore != null && filesBefore.isNotEmpty()) {
                                        Timber.tag("GOG").i(
                                            "[Cloud Saves] [BEFORE] ${filesBefore.size} files in '${location.name}': ${
                                                filesBefore.joinToString(", ") {
                                                    it.name
                                                }
                                            }",
                                        )
                                    } else {
                                        Timber.tag("GOG").i("[Cloud Saves] [BEFORE] Directory '${location.name}' is empty")
                                    }
                                } else {
                                    Timber.tag("GOG").i("[Cloud Saves] [BEFORE] Directory '${location.name}' does not exist yet")
                                }
                            } catch (e: Exception) {
                                Timber.tag("GOG").e(e, "[Cloud Saves] [BEFORE] Failed to check directory")
                            }

                            // Get stored timestamp for this location
                            val timestampStr = instance.gogManager.getCloudSaveSyncTimestamp(appId, location.name)
                            val timestamp = timestampStr.toLongOrNull() ?: 0L

                            Timber.tag("GOG")
                                .i("[Cloud Saves] Syncing '${location.name}' for game $gameId (clientId: ${location.clientId}, path: ${location.location}, timestamp: $timestamp, action: $preferredAction)")

                            // Validate clientSecret is available
                            if (location.clientSecret.isEmpty()) {
                                Timber.tag("GOG").e("[Cloud Saves] Missing clientSecret for '${location.name}', skipping sync")
                                continue
                            }

                            val cloudSavesManager = GOGCloudSavesManager(context)
                            val newTimestamp = cloudSavesManager.syncSaves(
                                clientId = location.clientId,
                                clientSecret = location.clientSecret,
                                localPath = location.location,
                                dirname = location.name,
                                lastSyncTimestamp = timestamp,
                                preferredAction = preferredAction,
                            )

                            if (newTimestamp > 0) {
                                // Success - store new timestamp
                                instance.gogManager.setCloudSaveSyncTimestamp(appId, location.name, newTimestamp.toString())
                                Timber.tag("GOG").d("[Cloud Saves] Updated timestamp for '${location.name}': $newTimestamp")

                                // Log the save files in the directory after sync
                                try {
                                    val saveDir = File(location.location)
                                    if (saveDir.exists() && saveDir.isDirectory) {
                                        val files = saveDir.listFiles()
                                        if (files != null && files.isNotEmpty()) {
                                            val fileList = files.joinToString(", ") { it.name }
                                            Timber.tag("GOG")
                                                .i("[Cloud Saves] [$preferredAction] Files in '${location.name}': $fileList (${files.size} files)")

                                            // Log detailed file info
                                            files.forEach { file ->
                                                val size = if (file.isFile) "${file.length()} bytes" else "directory"
                                                Timber.tag("GOG").d("[Cloud Saves] [$preferredAction]   - ${file.name} ($size)")
                                            }
                                        } else {
                                            Timber.tag("GOG")
                                                .w("[Cloud Saves] [$preferredAction] Directory '${location.name}' is empty at: ${location.location}")
                                        }
                                    } else {
                                        Timber.tag("GOG").w("[Cloud Saves] [$preferredAction] Directory not found: ${location.location}")
                                    }
                                } catch (e: Exception) {
                                    Timber.tag("GOG").e(e, "[Cloud Saves] Failed to list files in directory: ${location.location}")
                                }

                                Timber.tag("GOG").i("[Cloud Saves] Successfully synced save location '${location.name}' for game $gameId")
                            } else {
                                Timber.tag("GOG")
                                    .e("[Cloud Saves] Failed to sync save location '${location.name}' for game $gameId (timestamp: $newTimestamp)")
                                allSucceeded = false
                            }
                        } catch (e: Exception) {
                            Timber.tag("GOG").e(e, "[Cloud Saves] Exception syncing save location '${location.name}' for game $gameId")
                            allSucceeded = false
                        }
                    }

                    if (allSucceeded) {
                        Timber.tag("GOG").i("[Cloud Saves] All save locations synced successfully for $appId")
                        return@withContext true
                    } else {
                        Timber.tag("GOG").w("[Cloud Saves] Some save locations failed to sync for $appId")
                        return@withContext false
                    }
                } finally {
                    // Always end the sync, even if an exception occurred
                    getInstance()?.gogManager?.endSync(appId)
                    Timber.tag("GOG").d("[Cloud Saves] Sync completed and lock released for $appId")
                }
            } catch (e: Exception) {
                Timber.tag("GOG").e(e, "[Cloud Saves] Failed to sync cloud saves for App ID: $appId")
                return@withContext false
            }
        }
    }

    private lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var gogManager: GOGManager

    @Inject
    lateinit var gogDownloadManager: GOGDownloadManager

    @Inject
    lateinit var gogStoreCoordinator: GOGStoreCoordinator

    private val onEndProcess: (AndroidEvent.EndProcess) -> Unit = { stop() }

    override fun onCreate() {
        super.onCreate()
        instance = this
        coordinator = gogStoreCoordinator
        gogStoreCoordinator.onServiceStarted()

        // Initialize notification helper for foreground service
        notificationHelper = NotificationHelper(applicationContext)
        XServerRuntime.get().events.on<AndroidEvent.EndProcess, Unit>(onEndProcess)
    }

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
        gogStoreCoordinator.onServiceStopped()
        coordinator = null
        instance = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun getServiceTag(): String = "GOG"

    override suspend fun performSync(context: Context, isManual: Boolean) {
        gogManager.startBackgroundSync(context)
    }

    override fun getNotificationTitle(): String = "GOG"

    override fun getNotificationContent(): String = "Connected"
}
