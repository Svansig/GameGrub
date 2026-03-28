package app.gamegrub.service.steam.managers

import android.content.Context
import app.gamegrub.PrefManager
import app.gamegrub.data.PostSyncInfo
import app.gamegrub.enums.SaveLocation
import app.gamegrub.enums.SyncResult
import app.gamegrub.service.steam.di.CloudSyncResult
import app.gamegrub.service.steam.di.SteamAppInfoClient
import app.gamegrub.service.steam.di.SteamCloudClient
import app.gamegrub.service.steam.di.SteamConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SteamCloudSavesManager @Inject constructor(
    private val cloudClient: SteamCloudClient,
    private val appInfoClient: SteamAppInfoClient,
    private val connection: SteamConnection,
) {

    private val syncInProgressApps = ConcurrentHashMap<Int, AtomicBoolean>()

    private fun tryAcquireSync(appId: Int): Boolean {
        val flag = syncInProgressApps.getOrPut(appId) { AtomicBoolean(false) }
        return flag.compareAndSet(false, true)
    }

    private fun releaseSync(appId: Int) {
        syncInProgressApps[appId]?.set(false)
        syncInProgressApps.remove(appId)
    }

    suspend fun forceSyncUserFiles(
        appId: Int,
        prefixToPath: (String) -> String,
        preferredSave: SaveLocation = SaveLocation.None,
        parentScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
        overrideLocalChangeNumber: Long? = null,
    ): Deferred<PostSyncInfo> = parentScope.async {
        if (!tryAcquireSync(appId)) {
            Timber.w("Sync already in progress for $appId")
            return@async PostSyncInfo(SyncResult.InProgress)
        }

        try {
            val clientId = PrefManager.clientId ?: return@async PostSyncInfo(SyncResult.UnknownFail)
            val appInfo = appInfoClient.findApp(appId) ?: return@async PostSyncInfo(SyncResult.UnknownFail)

            var syncResult = PostSyncInfo(SyncResult.UnknownFail)

            for (attempt in 1..3) {
                try {
                    val result = cloudClient.syncUserFiles(
                        appId = appId,
                        clientId = clientId.toString(),
                        preferredSave = preferredSave.name,
                    )

                    if (result.success) {
                        syncResult = PostSyncInfo(
                            SyncResult.Success,
                            uploadsCompleted = result.uploadsCompleted,
                            downloadsCompleted = result.downloadsCompleted,
                        )
                    }
                    break
                } catch (e: Exception) {
                    if (attempt == 3) Timber.e(e, "Cloud sync failed after 3 attempts")
                    else delay(1000L * attempt)
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
    ) = withContext(Dispatchers.IO) {
        if (isOffline || !isConnected) return@withContext
        if (!tryAcquireSync(appId)) return@withContext

        try {
            val clientId = PrefManager.clientId?.toString() ?: return@withContext

            for (attempt in 1..3) {
                try {
                    val result = cloudClient.syncUserFiles(
                        appId = appId,
                        clientId = clientId,
                    )

                    cloudClient.signalAppExitSyncDone(
                        appId = appId,
                        clientId = clientId,
                        uploadsCompleted = result.uploadsCompleted,
                    )
                    break
                } catch (e: Exception) {
                    if (attempt == 3) Timber.e(e, "Cloud sync failed after 3 attempts")
                    else delay(1000L * attempt)
                }
            }
        } finally {
            releaseSync(appId)
        }
    }
}
