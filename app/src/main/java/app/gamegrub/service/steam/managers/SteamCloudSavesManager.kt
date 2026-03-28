package app.gamegrub.service.steam.managers

import android.content.Context
import app.gamegrub.data.PostSyncInfo
import app.gamegrub.data.SteamApp
import app.gamegrub.enums.SaveLocation
import app.gamegrub.enums.SyncResult
import app.gamegrub.service.steam.SteamAutoCloud
import `in`.dragonbra.javasteam.steam.handlers.steamcloud.SteamCloud
import `in`.dragonbra.javasteam.steam.steamclient.AsyncJobFailedException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import timber.log.Timber

class SteamCloudSavesManager(
    private val getService: () -> SteamServiceAccess?,
    private val getAppInfo: (Int) -> SteamApp?,
    private val getClientId: () -> String?,
) {

    private val syncInProgressApps = ConcurrentHashMap<Int, AtomicBoolean>()

    private fun getSyncFlag(appId: Int): AtomicBoolean {
        val existing = syncInProgressApps[appId]
        if (existing != null) return existing
        val created = AtomicBoolean(false)
        val prior = syncInProgressApps.putIfAbsent(appId, created)
        return prior ?: created
    }

    private fun tryAcquireSync(appId: Int): Boolean {
        return getSyncFlag(appId).compareAndSet(false, true)
    }

    private fun releaseSync(appId: Int) {
        val flag = syncInProgressApps[appId]
        flag?.set(false)
        if (flag != null && !flag.get()) {
            syncInProgressApps.remove(appId, flag)
        }
    }

    suspend fun forceSyncUserFiles(
        appId: Int,
        prefixToPath: (String) -> String,
        preferredSave: SaveLocation = SaveLocation.None,
        parentScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
        overrideLocalChangeNumber: Long? = null,
    ): Deferred<PostSyncInfo> = parentScope.async {
        if (!tryAcquireSync(appId)) {
            Timber.w("Cannot force sync when sync already in progress for appId=$appId")
            return@async PostSyncInfo(SyncResult.InProgress)
        }

        try {
            var syncResult = PostSyncInfo(SyncResult.UnknownFail)
            val clientId = getClientId() ?: return@async syncResult
            val service = getService() ?: return@async syncResult
            val appInfo = getAppInfo(appId) ?: return@async syncResult
            val steamCloud = service.steamCloud ?: return@async syncResult

            val maxAttempts = 3
            for (attempt in 1..maxAttempts) {
                try {
                    val postSyncInfo = SteamAutoCloud.syncUserFiles(
                        appInfo = appInfo,
                        clientId = clientId,
                        steamInstance = service,
                        steamCloud = steamCloud,
                        preferredSave = preferredSave,
                        parentScope = parentScope,
                        prefixToPath = prefixToPath,
                        overrideLocalChangeNumber = overrideLocalChangeNumber,
                    ).await()

                    postSyncInfo?.let { info ->
                        syncResult = info
                        Timber.i("Force cloud sync completed for app $appId with result: ${info.syncResult}")
                    }
                    break
                } catch (e: AsyncJobFailedException) {
                    if (attempt == maxAttempts) {
                        Timber.e(e, "Force cloud sync failed after $maxAttempts attempts")
                    } else {
                        Timber.w("Force cloud sync attempt $attempt failed, retrying...")
                        delay(1000L * attempt)
                    }
                }
            }

            syncResult
        } finally {
            releaseSync(appId)
        }
    }

    suspend fun syncAndClose(
        context: Context,
        appId: Int,
        isOffline: Boolean,
        isConnected: Boolean,
        prefixToPath: (String) -> String,
        syncAchievements: suspend (Context, Int) -> Unit,
    ) = withContext(Dispatchers.IO) {
        if (isOffline || !isConnected) return@withContext

        if (!tryAcquireSync(appId)) {
            Timber.w("Cannot close app when sync already in progress for appId=$appId")
            return@withContext
        }

        try {
            try {
                syncAchievements(context, appId)
            } catch (e: Exception) {
                Timber.e(e, "Achievement sync failed for appId=$appId, continuing with cloud save sync")
            }

            val clientId = getClientId()
            val service = getService()
            val appInfo = getAppInfo(appId)
            val steamCloud = service?.steamCloud

            if (clientId != null && service != null && appInfo != null && steamCloud != null) {
                val maxAttempts = 3
                for (attempt in 1..maxAttempts) {
                    try {
                        val postSyncInfo = SteamAutoCloud.syncUserFiles(
                            appInfo = appInfo,
                            clientId = clientId,
                            steamInstance = service,
                            steamCloud = steamCloud,
                            parentScope = this,
                            prefixToPath = prefixToPath,
                        ).await()

                        steamCloud.signalAppExitSyncDone(
                            appId = appId,
                            clientId = clientId,
                            uploadsCompleted = postSyncInfo?.uploadsCompleted == true,
                        )
                        break
                    } catch (e: Exception) {
                        if (attempt == maxAttempts) {
                            Timber.e(e, "Cloud sync failed after $maxAttempts attempts for appId=$appId")
                        } else {
                            delay(1000L * attempt)
                        }
                    }
                }
            }
        } finally {
            releaseSync(appId)
        }
    }
}
