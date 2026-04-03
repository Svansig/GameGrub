package app.gamegrub.gateway

import kotlinx.coroutines.flow.Flow

interface CloudSavesGateway {
    suspend fun syncSaves(gameId: String): Result<Unit>

    suspend fun getSyncStatus(gameId: String): CloudSaveSyncStatus

    suspend fun resolveConflict(gameId: String, resolution: CloudSaveResolution): Result<Unit>

    fun observeSyncStatus(gameId: String): Flow<CloudSaveSyncStatus>
}

data class CloudSaveSyncStatus(
    val gameId: String,
    val isSyncing: Boolean,
    val lastSyncTime: Long,
    val hasConflict: Boolean = false,
    val conflictMessage: String? = null,
    val errorMessage: String? = null,
)

enum class CloudSaveResolution {
    KEEP_LOCAL,
    KEEP_CLOUD,
    KEEP_BOTH,
}
