package app.gamegrub.service.steam

import android.content.Context
import android.content.Intent
import app.gamegrub.PrefManager
import app.gamegrub.data.DownloadInfo
import app.gamegrub.data.GameSource
import app.gamegrub.enums.Marker
import app.gamegrub.service.base.GameStoreCoordinator
import app.gamegrub.storage.StorageManager
import com.winlator.container.Container
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Steam implementation of [GameStoreCoordinator].
 *
 * Steam's service is significantly more complex than the other stores
 * (PICS, depot management, family sharing, etc.) and is not yet fully
 * migrated to the coordinator pattern. This class wraps the SteamService
 * companion API so callers can treat Steam uniformly with other stores.
 *
 * Full Steam migration is tracked separately.
 */
@Singleton
class SteamStoreCoordinator @Inject constructor(
    @ApplicationContext context: Context,
) : GameStoreCoordinator(context) {

    override val gameSource = GameSource.STEAM

    // ── Service lifecycle ──────────────────────────────────────────────────────

    override fun start() {
        context.startForegroundService(Intent(context, SteamService::class.java))
    }

    override fun stop() {
        SteamService.stop()
    }

    override val isRunning: Boolean get() = SteamService.isConnected

    override fun triggerLibrarySync() {
        // Steam syncs automatically on connection; no manual trigger needed
    }

    // ── Auth ───────────────────────────────────────────────────────────────────

    /**
     * True if Steam credentials are stored locally (user has previously logged in).
     * Uses persisted Steam ID as a proxy since full auth state requires a live connection.
     */
    override fun hasStoredCredentials(): Boolean =
        SteamService.isLoggedIn || PrefManager.steamUserSteamId64 > 0L

    override suspend fun logout(): Result<Unit> = runCatching {
        SteamService.logOut()
    }

    // ── Install state ──────────────────────────────────────────────────────────

    override suspend fun isGameInstalled(appId: Int): Boolean =
        SteamService.isAppInstalled(appId)

    override suspend fun getInstallPath(appId: Int): String? {
        val path = SteamPaths.getAppDirPath(appId)
        return if (StorageManager.hasMarker(path, Marker.DOWNLOAD_COMPLETE_MARKER)) path else null
    }

    // ── Downloads ──────────────────────────────────────────────────────────────

    // Steam manages downloads internally via its own installDomain and does not
    // expose a DownloadInfo handle through the companion API. Return null/false
    // here; callers that need Steam-specific progress should use SteamService directly.
    override fun getDownloadInfo(appId: Int): DownloadInfo? = null

    override fun hasActiveDownload(): Boolean =
        SteamService.hasActiveOperations()

    override fun cancelDownload(appId: Int): Boolean = false

    override fun hasActiveOperations(): Boolean =
        SteamService.hasActiveOperations()

    // ── Launch ─────────────────────────────────────────────────────────────────

    override suspend fun getLaunchExecutable(containerId: String, container: Container): String =
        SteamService.getLaunchExecutable(containerId, container)
}
