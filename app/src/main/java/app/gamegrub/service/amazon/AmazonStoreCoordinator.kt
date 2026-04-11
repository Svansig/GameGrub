package app.gamegrub.service.amazon

import android.content.Context
import android.content.Intent
import app.gamegrub.data.AmazonGame
import app.gamegrub.data.DownloadInfo
import app.gamegrub.data.GameSource
import app.gamegrub.enums.Marker
import app.gamegrub.events.AndroidEvent
import app.gamegrub.service.base.GameStoreCoordinator
import app.gamegrub.service.base.GameStoreService
import app.gamegrub.storage.StorageManager
import app.gamegrub.ui.runtime.XServerRuntime
import app.gamegrub.ui.utils.SnackbarManager
import app.gamegrub.utils.container.ContainerUtils
import com.winlator.container.Container
import dagger.hilt.android.qualifiers.ApplicationContext
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
import javax.inject.Singleton

@Singleton
class AmazonStoreCoordinator @Inject constructor(
    @ApplicationContext context: Context,
    private val amazonManager: AmazonManager,
    private val amazonDownloadManager: AmazonDownloadManager,
) : GameStoreCoordinator(context) {

    override val gameSource = GameSource.AMAZON

    private val coordinatorScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Download state (previously lived in AmazonService companion)
    private val activeDownloads = ConcurrentHashMap<String, DownloadInfo>()
    private val activeDownloadPaths = ConcurrentHashMap<String, String>()

    @Volatile
    private var _isRunning = false
    override val isRunning: Boolean get() = _isRunning

    // Called by AmazonService lifecycle
    internal fun onServiceStarted() {
        _isRunning = true
    }

    internal fun onServiceStopped() {
        _isRunning = false
    }

    // ── Service lifecycle ──────────────────────────────────────────────────────

    override fun start() {
        if (_isRunning) return
        val intent = Intent(context, AmazonService::class.java).apply {
            action = GameStoreService.ACTION_SYNC_LIBRARY
        }
        context.startForegroundService(intent)
    }

    override fun stop() {
        context.stopService(Intent(context, AmazonService::class.java))
    }

    override fun triggerLibrarySync() {
        val intent = Intent(context, AmazonService::class.java).apply {
            action = GameStoreService.ACTION_MANUAL_SYNC
        }
        context.startForegroundService(intent)
    }

    // ── Auth ───────────────────────────────────────────────────────────────────

    override fun hasStoredCredentials(): Boolean =
        AmazonAuthManager.hasStoredCredentials(context)

    override suspend fun logout(): Result<Unit> = runCatching {
        AmazonAuthManager.logout(context)
        amazonManager.deleteAllNonInstalledGames()
        stop()
    }

    // ── Install state ──────────────────────────────────────────────────────────

    override suspend fun isGameInstalled(appId: Int): Boolean {
        val game = amazonManager.getGameByAppId(appId) ?: return false
        if (game.isInstalled && game.installPath.isNotEmpty()) {
            return StorageManager.hasMarker(game.installPath, Marker.DOWNLOAD_COMPLETE_MARKER)
        }
        val installPath = game.installPath.takeIf { it.isNotEmpty() }
            ?: game.title.takeIf { it.isNotEmpty() }?.let {
                AmazonConstants.getGameInstallPath(context, it)
            }
            ?: return false
        return StorageManager.hasMarker(installPath, Marker.DOWNLOAD_COMPLETE_MARKER) &&
                !StorageManager.hasMarker(installPath, Marker.DOWNLOAD_IN_PROGRESS_MARKER)
    }

    override suspend fun getInstallPath(appId: Int): String? {
        val game = amazonManager.getGameByAppId(appId) ?: return null
        return if (game.isInstalled && game.installPath.isNotEmpty()) game.installPath else null
    }

    fun getExpectedInstallPath(appId: Int): String? {
        val game = runBlocking { amazonManager.getGameByAppId(appId) } ?: return null
        activeDownloadPaths[game.productId]?.let { return it }
        return game.title.takeIf { it.isNotBlank() }?.let {
            AmazonConstants.getGameInstallPath(context, it)
        }
    }

    suspend fun getBearerToken(): String? = amazonManager.getBearerToken()

    // ── Downloads ──────────────────────────────────────────────────────────────

    override fun getDownloadInfo(appId: Int): DownloadInfo? {
        val productId = runBlocking { amazonManager.getGameByAppId(appId) }?.productId ?: return null
        return activeDownloads[productId]
    }

    fun getDownloadInfoByProductId(productId: String): DownloadInfo? = activeDownloads[productId]

    override fun hasActiveDownload(): Boolean = activeDownloads.isNotEmpty()

    override fun cancelDownload(appId: Int): Boolean {
        val productId = runBlocking { amazonManager.getGameByAppId(appId) }?.productId ?: return false
        return cancelDownloadByProductId(productId)
    }

    fun cancelDownloadByProductId(productId: String): Boolean {
        val downloadInfo = activeDownloads[productId] ?: return false
        downloadInfo.cancel()
        return true
    }

    suspend fun downloadGame(productId: String, installPath: String): Result<DownloadInfo> {
        activeDownloads[productId]?.let { return Result.success(it) }

        val game = withContext(Dispatchers.IO) { amazonManager.getGameById(productId) }
            ?: return Result.failure(Exception("Game not found: $productId"))

        val downloadInfo = DownloadInfo(
            jobCount = 1,
            gameId = game.appId,
            downloadingAppIds = CopyOnWriteArrayList(),
        )
        downloadInfo.setPersistencePath(installPath)
        val persistedBytes = downloadInfo.loadPersistedBytesDownloaded(installPath)
        if (persistedBytes > 0L) downloadInfo.initializeBytesDownloaded(persistedBytes)

        downloadInfo.setActive(true)
        activeDownloads[productId] = downloadInfo
        activeDownloadPaths[productId] = installPath

        StorageManager.removeMarker(installPath, Marker.DOWNLOAD_COMPLETE_MARKER)
        XServerRuntime.get().events.emitJava(AndroidEvent.DownloadStatusChanged(game.appId, true))

        val job = coordinatorScope.launch {
            try {
                val result = amazonDownloadManager.downloadGame(
                    context = context,
                    game = game,
                    installPath = installPath,
                    downloadInfo = downloadInfo,
                )
                if (result.isSuccess) {
                    downloadInfo.setActive(false)
                    downloadInfo.clearPersistedBytesDownloaded(installPath)
                    SnackbarManager.show("Download completed: ${game.title}")
                    XServerRuntime.get().events.emitJava(
                        AndroidEvent.LibraryInstallStatusChanged(game.appId),
                    )
                } else {
                    val error = result.exceptionOrNull()
                    Timber.tag("Amazon").e(error, "Download failed for $productId")
                    downloadInfo.setActive(false)
                    cleanupFailedInstall(game, installPath)
                    SnackbarManager.show("Download failed: ${error?.message ?: "Unknown error"}")
                }
            } catch (e: Exception) {
                if (e !is java.util.concurrent.CancellationException) {
                    Timber.tag("Amazon").e(e, "Download exception for $productId")
                    cleanupFailedInstall(game, installPath)
                }
                downloadInfo.setActive(false)
            } finally {
                activeDownloads.remove(productId)
                activeDownloadPaths.remove(productId)
                XServerRuntime.get().events.emitJava(
                    AndroidEvent.DownloadStatusChanged(game.appId, false),
                )
            }
        }
        downloadInfo.setDownloadJob(job)
        return Result.success(downloadInfo)
    }

    private suspend fun cleanupFailedInstall(game: AmazonGame, installPath: String) {
        withContext(Dispatchers.IO) {
            StorageManager.removeMarker(installPath, Marker.DOWNLOAD_COMPLETE_MARKER)
            StorageManager.removeMarker(installPath, Marker.DOWNLOAD_IN_PROGRESS_MARKER)
            runCatching {
                val dir = File(installPath)
                if (dir.exists()) dir.deleteRecursively()
            }.onFailure { Timber.tag("Amazon").w(it, "Failed to clean partial install dir for ${game.productId}") }
            runCatching {
                amazonManager.markUninstalled(game.productId)
            }.onFailure { Timber.tag("Amazon").w(it, "Failed to mark game uninstalled: ${game.productId}") }
        }
        withContext(Dispatchers.Main) {
            ContainerUtils.deleteContainer(context, "AMAZON_${game.appId}")
        }
        XServerRuntime.get().events.emitJava(AndroidEvent.LibraryInstallStatusChanged(game.appId))
    }

    // ── Launch ─────────────────────────────────────────────────────────────────

    override suspend fun getLaunchExecutable(containerId: String, container: Container): String =
        AmazonService.getLaunchExecutable(containerId)
}
