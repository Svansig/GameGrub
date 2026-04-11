package app.gamegrub.service.steam.domain

import app.gamegrub.data.GameProcessInfo
import app.gamegrub.data.PostSyncInfo
import com.winlator.xenvironment.ImageFs
import com.winlator.xenvironment.components.GuestProgramLauncherComponent
import `in`.dragonbra.javasteam.steam.handlers.steamapps.GamePlayedInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Session domain: launch/close app flow, session files, and ticket lifecycle.
 * Coordinates: SteamAppSessionManager, SteamSessionFilesManager, SteamTicketManager
 */
@Singleton
class SteamSessionDomain @Inject constructor(
    val appSessionManager: app.gamegrub.service.steam.managers.SteamAppSessionManager,
    val sessionFilesManager: app.gamegrub.service.steam.managers.SteamSessionFilesManager,
    val ticketManager: app.gamegrub.service.steam.managers.SteamTicketManager,
) {
    fun getKeepAlive(): Boolean = appSessionManager.keepAlive

    fun setKeepAlive(value: Boolean) {
        appSessionManager.keepAlive = value
    }

    fun getAutoStopWhenIdle(): Boolean = appSessionManager.autoStopWhenIdle

    fun setAutoStopWhenIdle(value: Boolean) {
        appSessionManager.autoStopWhenIdle = value
    }

    fun hasActiveSessionOperations(): Boolean = appSessionManager.hasActiveSessionOperations()

    fun updatePlayingSessionState(isBlocked: Boolean) {
        appSessionManager.updatePlayingSessionState(isBlocked)
    }

    suspend fun kickPlayingSession(
        onlyGame: Boolean,
        kickSession: suspend (Boolean) -> Unit,
    ): Boolean = appSessionManager.kickPlayingSession(onlyGame, kickSession)

    suspend fun notifyRunningProcesses(
        gameProcesses: Array<out GameProcessInfo>,
        isConnected: Boolean,
        resolvePlayedInfo: suspend (GameProcessInfo) -> GamePlayedInfo?,
        notifyGamesPlayed: suspend (List<GamePlayedInfo>) -> Unit,
    ) = appSessionManager.notifyRunningProcesses(
        gameProcesses = gameProcesses,
        isConnected = isConnected,
        resolvePlayedInfo = resolvePlayedInfo,
        notifyGamesPlayed = notifyGamesPlayed,
    )

    fun beginLaunchApp(
        appId: Int,
        isOffline: Boolean,
        isConnected: Boolean,
        ignorePendingOperations: Boolean,
        parentScope: CoroutineScope,
        syncUserFiles: suspend () -> PostSyncInfo?,
        signalLaunchIntent: suspend () -> app.gamegrub.service.steam.managers.LaunchIntentResult,
        kickExistingPlayingSession: suspend () -> Unit,
    ): Deferred<PostSyncInfo> = appSessionManager.beginLaunchApp(
        appId = appId,
        isOffline = isOffline,
        isConnected = isConnected,
        ignorePendingOperations = ignorePendingOperations,
        parentScope = parentScope,
        syncUserFiles = syncUserFiles,
        signalLaunchIntent = signalLaunchIntent,
        kickExistingPlayingSession = kickExistingPlayingSession,
    )

    fun closeApp(
        appId: Int,
        isOffline: Boolean,
        isConnected: Boolean,
        parentScope: CoroutineScope,
        syncAchievements: suspend () -> Unit,
        syncUserFiles: suspend () -> PostSyncInfo?,
        signalExitSyncDone: suspend (PostSyncInfo?) -> Unit,
    ): Deferred<Unit> = appSessionManager.closeApp(
        appId = appId,
        isOffline = isOffline,
        isConnected = isConnected,
        parentScope = parentScope,
        syncAchievements = syncAchievements,
        syncUserFiles = syncUserFiles,
        signalExitSyncDone = signalExitSyncDone,
    )

    suspend fun getEncryptedAppTicket(appId: Int): ByteArray? = ticketManager.getEncryptedAppTicket(appId)

    suspend fun getEncryptedAppTicketBase64(appId: Int): String? = ticketManager.getEncryptedAppTicketBase64(appId)

    fun clearAllTickets() {
        ticketManager.clearAllTickets()
    }

    fun applyAutoLoginUserChanges(
        imageFs: ImageFs,
        session: app.gamegrub.service.steam.managers.SteamSessionContext,
    ) {
        sessionFilesManager.applyAutoLoginUserChanges(
            imageFs = imageFs,
            session = session,
        )
    }

    fun setupRealSteamSessionFiles(
        session: app.gamegrub.service.steam.managers.SteamSessionContext,
        imageFs: ImageFs,
        guestProgramLauncherComponent: GuestProgramLauncherComponent,
    ) {
        sessionFilesManager.setupRealSteamSessionFiles(
            session = session,
            imageFs = imageFs,
            guestProgramLauncherComponent = guestProgramLauncherComponent,
        )
    }
}
