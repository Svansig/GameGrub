package app.gamegrub.container.store

import app.gamegrub.container.manifest.ContainerConfiguration
import app.gamegrub.container.manifest.ContainerManifest
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Service for managing per-game container state.
 *
 * Provides container lifecycle management including creation, retrieval,
 * update, and deletion. Each container stores mutable game-specific state
 * including prefixes, installations, saves, and overrides.
 *
 * @property rootDir Root directory for container storage
 */
@Singleton
class ContainerStore @Inject constructor(
    private val rootDir: File,
) {
    init {
        ContainerStoreSchema.ensureInitialized(rootDir)
    }

    fun getRootDir(): File = rootDir

    suspend fun createContainer(
        gameId: String,
        gamePlatform: String,
        baseId: String,
        runtimeId: String,
        driverId: String? = null,
        profileId: String? = null,
        configuration: ContainerConfiguration = ContainerConfiguration(),
        name: String = "Container $gameId",
    ): Result<ContainerManifest> = withContext(Dispatchers.IO) {
        try {
            val containerId = "${gamePlatform.lowercase()}_${gameId}_${UUID.randomUUID().toString().take(8)}"
            val manifest = ContainerManifest(
                id = containerId,
                name = name,
                gameId = gameId,
                gamePlatform = gamePlatform,
                baseId = baseId,
                runtimeId = runtimeId,
                driverId = driverId,
                profileId = profileId,
                configuration = configuration,
            )

            if (!ContainerStoreSchema.ensureContainerDirectories(rootDir, containerId)) {
                return@withContext Result.failure(IllegalStateException("Failed to create container directories"))
            }

            if (!ContainerStoreSchema.writeManifest(manifest, rootDir)) {
                return@withContext Result.failure(IllegalStateException("Failed to write container manifest"))
            }

            Timber.i("Created container: $containerId for game $gamePlatform:$gameId")
            Result.success(manifest)
        } catch (e: Exception) {
            Timber.e(e, "Failed to create container for game $gamePlatform:$gameId")
            Result.failure(e)
        }
    }

    fun getContainer(containerId: String): ContainerManifest? {
        return ContainerStoreSchema.readManifest(rootDir, containerId)
    }

    fun getContainerByGame(gamePlatform: String, gameId: String): ContainerManifest? {
        return listContainers().find { it.gamePlatform == gamePlatform && it.gameId == gameId }
    }

    fun listContainers(): List<ContainerManifest> {
        return ContainerStoreSchema.listContainers(rootDir)
    }

    fun listContainersByPlatform(gamePlatform: String): List<ContainerManifest> {
        return listContainers().filter { it.gamePlatform == gamePlatform }
    }

    suspend fun updateContainer(manifest: ContainerManifest): Result<ContainerManifest> = withContext(Dispatchers.IO) {
        try {
            val updated = manifest.copy(lastModified = System.currentTimeMillis())
            if (!ContainerStoreSchema.writeManifest(updated, rootDir)) {
                return@withContext Result.failure(IllegalStateException("Failed to update container manifest"))
            }
            Timber.i("Updated container: ${manifest.id}")
            Result.success(updated)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update container: ${manifest.id}")
            Result.failure(e)
        }
    }

    suspend fun updateConfiguration(containerId: String, configuration: ContainerConfiguration): Result<ContainerManifest> =
        withContext(Dispatchers.IO) {
            val existing = getContainer(containerId)
                ?: return@withContext Result.failure(IllegalArgumentException("Container not found: $containerId"))
            val updated = existing.copy(
                configuration = configuration,
                lastModified = System.currentTimeMillis(),
            )
            if (!ContainerStoreSchema.writeManifest(updated, rootDir)) {
                return@withContext Result.failure(IllegalStateException("Failed to update configuration"))
            }
            Result.success(updated)
        }

    suspend fun addUserOverride(containerId: String, key: String, value: String): Result<ContainerManifest> = withContext(Dispatchers.IO) {
        val existing =
            getContainer(containerId) ?: return@withContext Result.failure(IllegalArgumentException("Container not found: $containerId"))
        val updated = existing.copy(
            userOverrides = existing.userOverrides + (key to value),
            lastModified = System.currentTimeMillis(),
        )
        if (!ContainerStoreSchema.writeManifest(updated, rootDir)) {
            return@withContext Result.failure(IllegalStateException("Failed to add user override"))
        }
        Result.success(updated)
    }

    suspend fun removeUserOverride(containerId: String, key: String): Result<ContainerManifest> = withContext(Dispatchers.IO) {
        val existing =
            getContainer(containerId) ?: return@withContext Result.failure(IllegalArgumentException("Container not found: $containerId"))
        val updated = existing.copy(
            userOverrides = existing.userOverrides - key,
            lastModified = System.currentTimeMillis(),
        )
        if (!ContainerStoreSchema.writeManifest(updated, rootDir)) {
            return@withContext Result.failure(IllegalStateException("Failed to remove user override"))
        }
        Result.success(updated)
    }

    fun getPrefixDir(containerId: String): File = ContainerStoreLayout.prefixDir(rootDir, containerId)

    fun getInstallDir(containerId: String): File = ContainerStoreLayout.installDir(rootDir, containerId)

    fun getSavesDir(containerId: String): File = ContainerStoreLayout.savesDir(rootDir, containerId)

    fun getOverridesDir(containerId: String): File = ContainerStoreLayout.overridesDir(rootDir, containerId)

    fun getCacheDir(containerId: String): File = ContainerStoreLayout.cacheDir(rootDir, containerId)

    suspend fun deleteContainer(containerId: String): Boolean = withContext(Dispatchers.IO) {
        ContainerStoreSchema.deleteContainer(rootDir, containerId)
    }

    suspend fun clearCache(containerId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val cacheDir = getCacheDir(containerId)
            if (cacheDir.exists()) {
                cacheDir.deleteRecursively()
                cacheDir.mkdirs()
            }
            Timber.i("Cleared cache for container: $containerId")
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear cache for container: $containerId")
            false
        }
    }
}
