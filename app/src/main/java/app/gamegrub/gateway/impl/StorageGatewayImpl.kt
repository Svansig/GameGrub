package app.gamegrub.gateway.impl

import app.gamegrub.data.GameSource
import app.gamegrub.gateway.StorageGateway
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageGatewayImpl @Inject constructor() : StorageGateway {

    private fun getBaseDir(source: GameSource): File {
        return when (source) {
            GameSource.STEAM -> File("/data/data/com.gamegrub.steam")
            GameSource.GOG -> File("/data/data/com.gamegrub.gog")
            GameSource.EPIC -> File("/data/data/com.gamegrub.epic")
            GameSource.AMAZON -> File("/data/data/com.gamegrub.amazon")
            GameSource.CUSTOM_GAME -> File("/data/data/com.gamegrub.custom")
        }
    }

    override fun getGameInstallDirectory(source: GameSource): File {
        return File(getBaseDir(source), "games")
    }

    override fun getGameDataDirectory(source: GameSource): File {
        return File(getBaseDir(source), "data")
    }

    override fun getDownloadCacheDirectory(): File {
        return File("/cache/downloads")
    }

    override fun getCloudSavesDirectory(): File {
        return File("/data/cloudsaves")
    }

    override suspend fun ensureDirectoryExists(path: File): Result<Unit> {
        return try {
            if (!path.exists()) {
                path.mkdirs()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getAvailableSpace(path: File): Long {
        return path.freeSpace
    }

    override fun getTotalSpace(path: File): Long {
        return path.totalSpace
    }

    override fun isPathAccessible(path: String): Boolean {
        return try {
            File(path).canRead()
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun deleteGameData(appId: String): Result<Unit> {
        return try {
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getGameSize(appId: String): Long {
        return 0L
    }
}
