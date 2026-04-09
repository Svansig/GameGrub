package app.gamegrub.domain.usecase

import app.gamegrub.gateway.CloudSaveResolution
import app.gamegrub.gateway.CloudSaveSyncStatus
import app.gamegrub.gateway.CloudSavesGateway
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class SyncCloudSavesUseCase @Inject constructor(
    private val cloudSavesGateway: CloudSavesGateway,
) {
    suspend operator fun invoke(gameId: String): Result<Unit> {
        return cloudSavesGateway.syncSaves(gameId)
    }
}

class GetCloudSaveSyncStatusUseCase @Inject constructor(
    private val cloudSavesGateway: CloudSavesGateway,
) {
    suspend operator fun invoke(gameId: String): CloudSaveSyncStatus {
        return cloudSavesGateway.getSyncStatus(gameId)
    }
}

class ResolveCloudSaveConflictUseCase @Inject constructor(
    private val cloudSavesGateway: CloudSavesGateway,
) {
    suspend operator fun invoke(gameId: String, resolution: CloudSaveResolution): Result<Unit> {
        return cloudSavesGateway.resolveConflict(gameId, resolution)
    }
}

class ObserveCloudSaveSyncStatusUseCase @Inject constructor(
    private val cloudSavesGateway: CloudSavesGateway,
) {
    operator fun invoke(gameId: String): Flow<CloudSaveSyncStatus> {
        return cloudSavesGateway.observeSyncStatus(gameId)
    }
}
