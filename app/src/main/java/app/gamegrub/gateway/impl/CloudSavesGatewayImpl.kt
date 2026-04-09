package app.gamegrub.gateway.impl

import app.gamegrub.gateway.CloudSaveResolution
import app.gamegrub.gateway.CloudSaveSyncStatus
import app.gamegrub.gateway.CloudSavesGateway
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

@Singleton
class CloudSavesGatewayImpl @Inject constructor() : CloudSavesGateway {

    private val syncStatuses = mutableMapOf<String, MutableStateFlow<CloudSaveSyncStatus>>()

    override suspend fun syncSaves(gameId: String): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun getSyncStatus(gameId: String): CloudSaveSyncStatus {
        return syncStatuses[gameId]?.value ?: CloudSaveSyncStatus(
            gameId = gameId,
            isSyncing = false,
            lastSyncTime = 0,
        )
    }

    override suspend fun resolveConflict(gameId: String, resolution: CloudSaveResolution): Result<Unit> {
        return Result.success(Unit)
    }

    override fun observeSyncStatus(gameId: String): Flow<CloudSaveSyncStatus> {
        return syncStatuses.getOrPut(gameId) {
            MutableStateFlow(CloudSaveSyncStatus(gameId, false, 0))
        }
    }
}
