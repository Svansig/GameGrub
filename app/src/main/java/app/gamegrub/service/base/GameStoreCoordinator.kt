package app.gamegrub.service.base

import android.content.Context
import app.gamegrub.data.DownloadInfo
import app.gamegrub.data.GameSource
import com.winlator.container.Container
import dagger.hilt.android.qualifiers.ApplicationContext

/**
 * Unified injectable base for all game store business logic.
 *
 * Each store provides a @Singleton subclass that implements this contract.
 * Android Services remain as thin lifecycle shells and delegate to their
 * coordinator for all business operations.
 *
 * Callers inject [GameStoreCoordinator] (or the full Set) instead of
 * reaching through static service companions.
 *
 * appId throughout is the internal LibraryItem.gameId (numeric DB primary key).
 * Coordinators handle the mapping to store-specific identifiers internally.
 */
abstract class GameStoreCoordinator(
    @ApplicationContext protected val context: Context,
) {
    abstract val gameSource: GameSource

    // ── Service lifecycle ──────────────────────────────────────────────────────

    /** Start the store's foreground service (idempotent). */
    abstract fun start()

    /** Stop the store's foreground service. */
    abstract fun stop()

    /** True while the store's foreground service is running. */
    abstract val isRunning: Boolean

    /** Trigger a manual library sync, bypassing throttle. */
    abstract fun triggerLibrarySync()

    // ── Auth ───────────────────────────────────────────────────────────────────

    abstract fun hasStoredCredentials(): Boolean

    abstract suspend fun logout(): Result<Unit>

    // ── Install state ──────────────────────────────────────────────────────────

    abstract suspend fun isGameInstalled(appId: Int): Boolean

    abstract suspend fun getInstallPath(appId: Int): String?

    // ── Downloads ──────────────────────────────────────────────────────────────

    abstract fun getDownloadInfo(appId: Int): DownloadInfo?

    abstract fun hasActiveDownload(): Boolean

    abstract fun cancelDownload(appId: Int): Boolean

    open fun hasActiveOperations(): Boolean = hasActiveDownload()

    // ── Launch ─────────────────────────────────────────────────────────────────

    abstract suspend fun getLaunchExecutable(containerId: String, container: Container): String
}
