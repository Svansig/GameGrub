package app.gamegrub.service.gog

import android.content.Context
import android.content.Intent
import app.gamegrub.data.DownloadInfo
import app.gamegrub.data.GameSource
import app.gamegrub.events.AndroidEvent
import app.gamegrub.service.base.GameStoreCoordinator
import app.gamegrub.service.base.GameStoreService
import app.gamegrub.ui.runtime.XServerRuntime
import app.gamegrub.ui.utils.SnackbarManager
import com.winlator.container.Container
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

@Singleton
class GOGStoreCoordinator @Inject constructor(
    @ApplicationContext context: Context,
    private val gogManager: GOGManager,
    private val gogDownloadManager: GOGDownloadManager,
) : GameStoreCoordinator(context) {

    override val gameSource = GameSource.GOG

    private val coordinatorScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Download state (previously lived in GOGService companion)
    private val activeDownloads = ConcurrentHashMap<String, DownloadInfo>()

    @Volatile
    private var _isRunning = false
    override val isRunning: Boolean get() = _isRunning

    // Called by GOGService lifecycle
    internal fun onServiceStarted() {
        _isRunning = true
    }
    internal fun onServiceStopped() {
        _isRunning = false
    }

    // ── Service lifecycle ──────────────────────────────────────────────────────

    override fun start() {
        if (_isRunning) return
        val intent = Intent(context, GOGService::class.java).apply {
            action = GameStoreService.ACTION_SYNC_LIBRARY
        }
        context.startForegroundService(intent)
    }

    override fun stop() {
        context.stopService(Intent(context, GOGService::class.java))
    }

    override fun triggerLibrarySync() {
        val intent = Intent(context, GOGService::class.java).apply {
            action = GameStoreService.ACTION_MANUAL_SYNC
        }
        context.startForegroundService(intent)
    }

    // ── Auth ───────────────────────────────────────────────────────────────────

    override fun hasStoredCredentials(): Boolean =
        GOGAuthManager.hasStoredCredentials(context)

    override suspend fun logout(): Result<Unit> = runCatching {
        GOGAuthManager.clearStoredCredentials(context)
        gogManager.deleteAllNonInstalledGames()
        stop()
    }

    // ── Install state ──────────────────────────────────────────────────────────

    override suspend fun isGameInstalled(appId: Int): Boolean {
        val game = gogManager.getGameFromDbById(appId.toString()) ?: return false
        if (!game.isInstalled) return false
        val (isValid, _) = gogManager.verifyInstallation(appId.toString())
        return isValid
    }

    override suspend fun getInstallPath(appId: Int): String? {
        val game = gogManager.getGameFromDbById(appId.toString()) ?: return null
        return if (game.isInstalled) game.installPath else null
    }

    // ── Downloads ──────────────────────────────────────────────────────────────

    override fun getDownloadInfo(appId: Int): DownloadInfo? = activeDownloads[appId.toString()]

    fun getDownloadInfoByGameId(gameId: String): DownloadInfo? = activeDownloads[gameId]

    fun getCurrentlyDownloadingGame(): String? = activeDownloads.keys.firstOrNull()

    override fun hasActiveDownload(): Boolean = activeDownloads.isNotEmpty()

    override fun cancelDownload(appId: Int): Boolean = cancelDownloadByGameId(appId.toString())

    fun cancelDownloadByGameId(gameId: String): Boolean {
        val downloadInfo = activeDownloads[gameId] ?: run {
            Timber.w("No active download found for game: $gameId")
            return false
        }
        downloadInfo.cancel()
        activeDownloads.remove(gameId)
        return true
    }

    fun cleanupDownload(gameId: String) {
        activeDownloads.remove(gameId)
    }

    fun downloadGame(
        gameId: String,
        installPath: String,
        containerLanguage: String,
    ): Result<DownloadInfo?> {
        val downloadInfo = DownloadInfo(
            jobCount = 1,
            gameId = 0,
            downloadingAppIds = CopyOnWriteArrayList(),
        )
        activeDownloads[gameId] = downloadInfo

        val job = coordinatorScope.launch {
            try {
                val commonRedistDir = File(installPath, "_CommonRedist")
                val result = gogDownloadManager.downloadGame(
                    gameId,
                    File(installPath),
                    downloadInfo,
                    containerLanguage,
                    true,
                    commonRedistDir,
                )
                if (result.isFailure) {
                    Timber.e(result.exceptionOrNull(), "[GOG] Download failed for $gameId")
                    downloadInfo.setProgress(-1.0f)
                    SnackbarManager.show("Download failed: ${result.exceptionOrNull()?.message ?: "Unknown error"}")
                } else {
                    downloadInfo.setProgress(1.0f)
                    SnackbarManager.show("Download completed successfully!")
                }
            } catch (e: Exception) {
                Timber.e(e, "[GOG] Download exception for $gameId")
                downloadInfo.setProgress(-1.0f)
                SnackbarManager.show("Download error: ${e.message ?: "Unknown error"}")
            } finally {
                downloadInfo.setActive(false)
                activeDownloads.remove(gameId)
            }
        }
        downloadInfo.setDownloadJob(job)
        return Result.success(downloadInfo)
    }

    suspend fun downloadDependencies(
        gameId: String,
        dependencies: List<String>,
        gameDir: File,
        supportDir: File,
        onProgress: (Float) -> Unit = {},
    ): Result<Unit> = gogDownloadManager.downloadDependenciesWithProgress(
        gameId = gameId,
        dependencies = dependencies,
        gameDir = gameDir,
        supportDir = supportDir,
        onProgress = onProgress,
    )

    // ── Launch ─────────────────────────────────────────────────────────────────

    override suspend fun getLaunchExecutable(containerId: String, container: Container): String =
        gogManager.getLaunchExecutable(containerId, container)

    suspend fun syncCloudSaves(appId: String, preferredAction: String = "none"): Boolean =
        GOGService.syncCloudSaves(context, appId, preferredAction)

    fun getScriptInterpreterPartsForLaunch(appId: String): List<String>? =
        gogManager.getScriptInterpreterPartsForLaunchSync(appId)
}
