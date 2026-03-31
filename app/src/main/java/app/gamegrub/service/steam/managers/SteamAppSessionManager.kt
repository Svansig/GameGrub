package app.gamegrub.service.steam.managers

import app.gamegrub.data.GameProcessInfo
import app.gamegrub.data.PostSyncInfo
import app.gamegrub.enums.SyncResult
import `in`.dragonbra.javasteam.steam.handlers.steamapps.GamePlayedInfo
import `in`.dragonbra.javasteam.steam.handlers.steamcloud.PendingRemoteOperation
import `in`.dragonbra.javasteam.steam.steamclient.AsyncJobFailedException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber

@Singleton
class SteamAppSessionManager @Inject constructor() {
    @Volatile
    var keepAlive: Boolean = false

    @Volatile
    var autoStopWhenIdle: Boolean = false

    private val sessionOpsInProgress = ConcurrentHashMap<Int, AtomicBoolean>()
    private val _isPlayingBlocked = MutableStateFlow(false)

    private fun getSyncFlag(appId: Int): AtomicBoolean {
        val existing = sessionOpsInProgress[appId]
        if (existing != null) {
            return existing
        }
        val created = AtomicBoolean(false)
        val prior = sessionOpsInProgress.putIfAbsent(appId, created)
        return prior ?: created
    }

    fun tryAcquireSync(appId: Int): Boolean {
        val flag = getSyncFlag(appId)
        return flag.compareAndSet(false, true)
    }

    fun releaseSync(appId: Int) {
        val flag = sessionOpsInProgress[appId]
        flag?.set(false)
        if (flag != null && !flag.get()) {
            sessionOpsInProgress.remove(appId, flag)
        }
    }

    fun hasActiveSessionOperations(): Boolean {
        return sessionOpsInProgress.values.any { it.get() }
    }

    fun updatePlayingSessionState(isBlocked: Boolean) {
        _isPlayingBlocked.value = isBlocked
    }

    suspend fun kickPlayingSession(
        onlyGame: Boolean,
        kickSession: suspend (Boolean) -> Unit,
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            _isPlayingBlocked.value = true
            kickSession(onlyGame)

            // Wait for PlayingSessionStateCallback to indicate unblocked.
            val deadline = System.currentTimeMillis() + 5000
            while (System.currentTimeMillis() < deadline) {
                if (_isPlayingBlocked.value == false) {
                    return@withContext true
                }
                delay(100)
            }
            false
        } catch (_: Exception) {
            false
        }
    }

    suspend fun notifyRunningProcesses(
        gameProcesses: Array<out GameProcessInfo>,
        isConnected: Boolean,
        resolvePlayedInfo: suspend (GameProcessInfo) -> GamePlayedInfo?,
        notifyGamesPlayed: suspend (List<GamePlayedInfo>) -> Unit,
    ) = withContext(Dispatchers.IO) {
        if (!isConnected) {
            return@withContext
        }

        val gamesPlayed = gameProcesses.mapNotNull { resolvePlayedInfo(it) }

        Timber.i(
            "GameProcessInfo:%s",
            gamesPlayed.joinToString("\n") { game ->
                """
                |   processId: ${game.processId}
                |   gameId: ${game.gameId}
                |   processes: ${
                    game.processIdList.joinToString("\n") { process ->
                        """
                        |   processId: ${process.processId}
                        |   processIdParent: ${process.processIdParent}
                        |   parentIsSteam: ${process.parentIsSteam}
                        """.trimMargin()
                    }
                }
                """.trimMargin()
            },
        )

        notifyGamesPlayed(gamesPlayed)
    }

    fun beginLaunchApp(
        appId: Int,
        isOffline: Boolean,
        isConnected: Boolean,
        ignorePendingOperations: Boolean,
        parentScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
        syncUserFiles: suspend () -> PostSyncInfo?,
        signalLaunchIntent: suspend () -> LaunchIntentResult,
        kickExistingPlayingSession: suspend () -> Unit,
    ): Deferred<PostSyncInfo> = parentScope.async {
        if (isOffline || !isConnected) {
            return@async PostSyncInfo(SyncResult.UpToDate)
        }
        if (!tryAcquireSync(appId)) {
            Timber.w("Cannot launch app when sync already in progress for appId=$appId")
            return@async PostSyncInfo(SyncResult.InProgress)
        }

        try {
            var syncResult = PostSyncInfo(SyncResult.UnknownFail)
            val maxAttempts = 3
            for (attempt in 1..maxAttempts) {
                try {
                    val postSyncInfo = syncUserFiles()
                    syncResult = postSyncInfo ?: PostSyncInfo(SyncResult.UnknownFail)

                    if (syncResult.syncResult == SyncResult.Success || syncResult.syncResult == SyncResult.UpToDate) {
                        val launchIntentResult = signalLaunchIntent()
                        if (launchIntentResult.pendingRemoteOperations.isNotEmpty() && !ignorePendingOperations) {
                            syncResult = PostSyncInfo(
                                syncResult = SyncResult.PendingOperations,
                                pendingRemoteOperations = launchIntentResult.pendingRemoteOperations,
                            )
                        } else if (ignorePendingOperations && launchIntentResult.hasAppSessionActiveOperation) {
                            kickExistingPlayingSession()
                        }
                    }

                    break
                } catch (e: AsyncJobFailedException) {
                    if (attempt == maxAttempts) {
                        Timber.e(e, "Cloud sync failed after $maxAttempts attempts")
                        syncResult = PostSyncInfo(SyncResult.UnknownFail)
                    } else {
                        Timber.w("Cloud sync attempt $attempt failed (AsyncJobFailedException), retrying...")
                        delay(1000L * attempt)
                    }
                }
            }

            syncResult
        } finally {
            releaseSync(appId)
        }
    }

    fun closeApp(
        appId: Int,
        isOffline: Boolean,
        isConnected: Boolean,
        parentScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
        syncAchievements: suspend () -> Unit,
        syncUserFiles: suspend () -> PostSyncInfo?,
        signalExitSyncDone: suspend (PostSyncInfo?) -> Unit,
    ): Deferred<Unit> = parentScope.async {
        if (isOffline || !isConnected) {
            return@async
        }

        if (!tryAcquireSync(appId)) {
            Timber.w("Cannot close app when sync already in progress for appId=$appId")
            return@async
        }

        try {
            try {
                syncAchievements()
            } catch (e: Exception) {
                Timber.e(e, "Achievement sync failed for appId=$appId, continuing with cloud save sync")
            }

            val maxAttempts = 3
            for (attempt in 1..maxAttempts) {
                try {
                    val postSyncInfo = syncUserFiles()
                    signalExitSyncDone(postSyncInfo)
                    break
                } catch (e: AsyncJobFailedException) {
                    if (attempt == maxAttempts) {
                        Timber.e(e, "Close app sync failed after $maxAttempts attempts")
                    } else {
                        Timber.w("Close app sync attempt $attempt failed (AsyncJobFailedException), retrying...")
                        delay(1000L * attempt)
                    }
                }
            }
        } finally {
            releaseSync(appId)
        }
    }
}

data class LaunchIntentResult(
    val pendingRemoteOperations: List<PendingRemoteOperation> = emptyList(),
    val hasAppSessionActiveOperation: Boolean = false,
)
