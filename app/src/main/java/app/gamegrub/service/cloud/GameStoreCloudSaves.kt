package app.gamegrub.service.cloud

import app.gamegrub.data.GameSource

enum class CloudSyncStatus {
    IDLE,
    SYNCING,
    CONFLICT,
    ERROR,
}

data class CloudSaveInfo(
    val gameId: String,
    val localTimestamp: Long,
    val cloudTimestamp: Long,
    val status: CloudSyncStatus,
    val conflictMessage: String? = null,
)

interface GameStoreCloudSaves {
    val gameSource: GameSource

    suspend fun sync(gameId: String): Result<Unit>

    suspend fun getStatus(gameId: String): CloudSaveInfo

    suspend fun resolveConflict(gameId: String, resolution: ConflictResolution): Result<Unit>

    suspend fun getCloudSaves(gameId: String): List<String>

    suspend fun downloadCloudSave(gameId: String, saveName: String): Result<Unit>

    suspend fun uploadCloudSave(gameId: String, savePath: String): Result<Unit>
}

enum class ConflictResolution {
    KEEP_LOCAL,
    KEEP_CLOUD,
    KEEP_BOTH,
}
