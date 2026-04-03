package app.gamegrub.gateway

import java.io.File

interface StorageGateway {
    fun getGameInstallDirectory(source: app.gamegrub.data.GameSource): File

    fun getGameDataDirectory(source: app.gamegrub.data.GameSource): File

    fun getDownloadCacheDirectory(): File

    fun getCloudSavesDirectory(): File

    suspend fun ensureDirectoryExists(path: File): Result<Unit>

    fun getAvailableSpace(path: File): Long

    fun getTotalSpace(path: File): Long

    fun isPathAccessible(path: String): Boolean

    suspend fun deleteGameData(appId: String): Result<Unit>

    suspend fun getGameSize(appId: String): Long
}
