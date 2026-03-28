package app.gamegrub.service.steam.managers

import android.content.Context
import app.gamegrub.PrefManager
import app.gamegrub.data.PostSyncInfo
import app.gamegrub.data.SteamApp
import app.gamegrub.enums.SaveLocation
import app.gamegrub.enums.SyncResult
import app.gamegrub.service.steam.SteamAutoCloud
import `in`.dragonbra.javasteam.steam.steamclient.AsyncJobFailedException
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
class SteamCloudSavesManager @Inject constructor() {

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
            var syncResult = PostSyncInfo(SyncResult.UnknownFail)
            val service = SteamService.instance ?: return@async syncResult
            val clientId = PrefManager.clientId ?: return@async syncResult
            val appInfo = service.appDao?.findApp(appId) ?: return@async syncResult
            val steamCloud = service._steamCloud ?: return@async syncResult

            for (attempt in 1..3) {
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
                    postSyncInfo?.let { syncResult = it }
                    break
                } catch (e: AsyncJobFailedException) {
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
            val service = SteamService.instance ?: return@withContext
            val clientId = PrefManager.clientId ?: return@withContext
            val appInfo = service.appDao?.findApp(appId) ?: return@withContext
            val steamCloud = service._steamCloud ?: return@withContext

            try {
                service._steamUserStats?.let { userStats ->
                    service._steamUser?.steamID?.let { steamId ->
                        userStats.getUserStats(appId, steamId).await()
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Achievement sync failed for $appId")
            }

            for (attempt in 1..3) {
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
                    if (attempt == 3) Timber.e(e, "Cloud sync failed after 3 attempts")
                    else delay(1000L * attempt)
                }
            }
        } finally {
            releaseSync(appId)
        }
    }
}
