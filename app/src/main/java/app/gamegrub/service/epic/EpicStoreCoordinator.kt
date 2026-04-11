package app.gamegrub.service.epic

import android.content.Context
import android.content.Intent
import app.gamegrub.data.DownloadInfo
import app.gamegrub.data.GameSource
import app.gamegrub.enums.Marker
import app.gamegrub.service.base.GameStoreCoordinator
import app.gamegrub.service.base.GameStoreService
import app.gamegrub.storage.StorageManager
import app.gamegrub.ui.utils.SnackbarManager
import com.winlator.container.Container
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpicStoreCoordinator @Inject constructor(
    @ApplicationContext context: Context,
    private val epicManager: EpicManager,
    private val epicDownloadManager: EpicDownloadManager,
) : GameStoreCoordinator(context) {

    override val gameSource = GameSource.EPIC

    private val coordinatorScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Download state (previously lived in EpicService companion)
    private val activeDownloads = ConcurrentHashMap<Int, DownloadInfo>()

    @Volatile
    private var _isRunning = false
    override val isRunning: Boolean get() = _isRunning

    // Called by EpicService lifecycle
    internal fun onServiceStarted() {
        _isRunning = true
    }

    internal fun onServiceStopped() {
        _isRunning = false
    }

    // ── Service lifecycle ──────────────────────────────────────────────────────

    override fun start() {
        if (_isRunning) return
        val intent = Intent(context, EpicService::class.java).apply {
            action = GameStoreService.ACTION_SYNC_LIBRARY
        }
        context.startForegroundService(intent)
    }

    override fun stop() {
        context.stopService(Intent(context, EpicService::class.java))
    }

    override fun triggerLibrarySync() {
        val intent = Intent(context, EpicService::class.java).apply {
            action = GameStoreService.ACTION_MANUAL_SYNC
        }
        context.startForegroundService(intent)
    }

    // ── Auth ───────────────────────────────────────────────────────────────────

    override fun hasStoredCredentials(): Boolean =
        EpicAuthManager.hasStoredCredentials(context)

    override suspend fun logout(): Result<Unit> = runCatching {
        EpicAuthManager.clearStoredCredentials(context)
        epicManager.deleteAllNonInstalledGames()
        stop()
    }

    // ── Install state ──────────────────────────────────────────────────────────

    override suspend fun isGameInstalled(appId: Int): Boolean {
        val game = epicManager.getGameById(appId) ?: return false
        if (game.isInstalled && game.installPath.isNotEmpty()) {
            return StorageManager.hasMarker(game.installPath, Marker.DOWNLOAD_COMPLETE_MARKER)
        }
        val installPath = game.installPath.takeIf { it.isNotEmpty() }
            ?: game.appName.takeIf { it.isNotEmpty() }?.let {
                EpicConstants.getGameInstallPath(context, it)
            }
            ?: return false
        return StorageManager.hasMarker(installPath, Marker.DOWNLOAD_COMPLETE_MARKER) &&
                !StorageManager.hasMarker(installPath, Marker.DOWNLOAD_IN_PROGRESS_MARKER)
    }

    override suspend fun getInstallPath(appId: Int): String? {
        val game = epicManager.getGameById(appId) ?: return null
        return if (game.isInstalled && game.installPath.isNotEmpty()) game.installPath else null
    }

    // ── Downloads ──────────────────────────────────────────────────────────────

    override fun getDownloadInfo(appId: Int): DownloadInfo? = activeDownloads[appId]

    override fun hasActiveDownload(): Boolean = activeDownloads.isNotEmpty()

    override fun cancelDownload(appId: Int): Boolean {
        val downloadInfo = activeDownloads[appId] ?: return false
        downloadInfo.cancel()
        activeDownloads.remove(appId)
        return true
    }

    suspend fun cleanupDownload(context: Context, appId: Int) {
        epicManager.getGameById(appId)?.let { game ->
            val path = EpicConstants.getGameInstallPath(context, game.appName)
            StorageManager.removeMarker(path, Marker.DOWNLOAD_IN_PROGRESS_MARKER)
        }
        activeDownloads.remove(appId)
    }

    fun downloadGame(
        appId: Int,
        dlcGameIds: List<Int>,
        installPath: String,
        containerLanguage: String,
    ): Result<DownloadInfo> {
        val game = runBlocking { epicManager.getGameById(appId) }
            ?: return Result.failure(Exception("Game not found for appId: $appId"))
        val gameId = game.id

        activeDownloads[appId]?.let { return Result.success(it) }

        val downloadInfo = DownloadInfo(
            jobCount = 1,
            gameId = appId,
            downloadingAppIds = CopyOnWriteArrayList(),
        )
        activeDownloads[appId] = downloadInfo
        downloadInfo.setActive(true)

        val job = coordinatorScope.launch {
            try {
                val commonRedistDir = File(installPath, "_CommonRedist")
                val result = epicDownloadManager.downloadGame(
                    context, game, installPath, downloadInfo, containerLanguage,
                    dlcGameIds, commonRedistDir,
                )
                if (result.isSuccess) {
                    downloadInfo.setProgress(1.0f)
                    SnackbarManager.show("Download completed successfully!")
                } else {
                    val error = result.exceptionOrNull()
                    Timber.e(error, "[Epic] Download failed for $gameId")
                    downloadInfo.setProgress(-1.0f)
                    SnackbarManager.show("Download failed: ${error?.message ?: "Unknown error"}")
                }
            } catch (e: Exception) {
                Timber.e(e, "[Epic] Download exception for $gameId")
                downloadInfo.setProgress(-1.0f)
                SnackbarManager.show("Download error: ${e.message ?: "Unknown error"}")
            } finally {
                downloadInfo.setActive(false)
                activeDownloads.remove(appId)
            }
        }
        downloadInfo.setDownloadJob(job)
        return Result.success(downloadInfo)
    }

    // ── Launch ─────────────────────────────────────────────────────────────────

    override suspend fun getLaunchExecutable(containerId: String, container: Container): String =
        EpicService.getLaunchExecutable(containerId)

    suspend fun getInstalledExe(gameId: Int): String =
        epicManager.getInstalledExe(gameId)
}
