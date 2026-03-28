package app.gamegrub.service.steam.managers

import app.gamegrub.data.PostSyncInfo
import app.gamegrub.enums.SyncResult
import `in`.dragonbra.javasteam.steam.steamclient.AsyncJobFailedException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SteamAppSessionManager @Inject constructor() {
    private val sessionOpsInProgress = ConcurrentHashMap<Int, AtomicBoolean>()

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

    fun beginLaunchApp(
        appId: Int,
        isOffline: Boolean,
        isConnected: Boolean,
        parentScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
        executeSyncAndSignal: suspend () -> PostSyncInfo,
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
                    syncResult = executeSyncAndSignal()
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
        executeSyncAndSignal: suspend () -> Unit,
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
                    executeSyncAndSignal()
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

