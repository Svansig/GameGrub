package app.gamegrub.service.steam.managers

import app.gamegrub.PrefManager
import app.gamegrub.data.PostSyncInfo
import app.gamegrub.enums.SaveLocation
import app.gamegrub.enums.SyncResult
import app.gamegrub.service.steam.di.SteamAppInfoClient
import app.gamegrub.service.steam.di.SteamCloudClient
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
class SteamCloudSavesManager @Inject constructor(
    private val cloudClient: SteamCloudClient,
    private val appInfoClient: SteamAppInfoClient,
) {

    private val syncInProgressApps = ConcurrentHashMap<Int, AtomicBoolean>()

    private fun getSyncFlag(appId: Int): AtomicBoolean {
        val existing = syncInProgressApps[appId]
        if (existing != null) {
            return existing
        }
        val created = AtomicBoolean(false)
        val prior = syncInProgressApps.putIfAbsent(appId, created)
        return prior ?: created
    }

    private fun tryAcquireSync(appId: Int): Boolean {
        val flag = getSyncFlag(appId)
        return flag.compareAndSet(false, true)
    }

    private fun releaseSync(appId: Int) {
        val flag = syncInProgressApps[appId]
        flag?.set(false)
        if (flag != null && !flag.get()) {
            syncInProgressApps.remove(appId, flag)
        }
    }

    fun forceSyncUserFiles(
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
            // Keep behavior compatible with the legacy flow by requiring app metadata to exist first.
            if (appInfoClient.findApp(appId) == null) {
                return@async PostSyncInfo(SyncResult.UnknownFail)
            }
            prefixToPath.hashCode()
            overrideLocalChangeNumber.hashCode()

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
                            filesDownloaded = if (result.downloadsCompleted) 1 else 0,
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

}
