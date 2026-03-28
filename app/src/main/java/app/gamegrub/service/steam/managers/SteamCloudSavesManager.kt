package app.gamegrub.service.steam.managers

import app.gamegrub.service.steam.di.SteamCloudClient
import app.gamegrub.service.steam.di.SteamConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SteamCloudSavesManager @Inject constructor(
    private val cloudClient: SteamCloudClient,
    private val connection: SteamConnection,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun signalAppExitComplete(appId: Int, clientId: String, uploadsCompleted: Boolean) {
        withContext(Dispatchers.IO) {
            try {
                cloudClient.signalAppExitSyncDone(appId, clientId, uploadsCompleted)
                Timber.i("Cloud sync complete for app $appId")
            } catch (e: Exception) {
                Timber.e(e, "Failed to signal app exit sync for $appId")
            }
        }
    }

    val isConnected: Boolean get() = connection.isConnected
}
