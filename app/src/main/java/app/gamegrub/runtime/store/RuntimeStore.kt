package app.gamegrub.runtime.store

import app.gamegrub.runtime.manifest.BaseManifest
import app.gamegrub.runtime.manifest.DriverManifest
import app.gamegrub.runtime.manifest.RuntimeManifest
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Service for managing immutable runtime bundles.
 *
 * Provides registration, verification, and retrieval of base, runtime, and driver
 * bundles. Each bundle is identified by a unique ID and verified via content hash.
 * Supports concurrent access and atomic operations.
 *
 * @property rootDir Root directory for the runtime store
 */
@Singleton
class RuntimeStore @Inject constructor(
    private val rootDir: File,
) {
    init {
        RuntimeStoreSchema.ensureInitialized(rootDir)
    }

    fun getRootDir(): File = rootDir

    suspend fun registerBase(manifest: BaseManifest, contentDir: File): Result<BaseManifest> = withContext(Dispatchers.IO) {
        try {
            if (!manifest.isValid()) {
                return@withContext Result.failure(IllegalArgumentException("Invalid BaseManifest: ${manifest.validate()}"))
            }

            val bundleDir = RuntimeStoreLayout.baseDir(rootDir, manifest.id)
            if (bundleDir.exists()) {
                Timber.w("Base bundle already exists: ${manifest.id}")
                return@withContext Result.failure(IllegalStateException("Base bundle already exists: ${manifest.id}"))
            }

            bundleDir.mkdirs()

            val contentHash = computeDirectoryHash(contentDir)
            if (contentHash != manifest.contentHash) {
                bundleDir.deleteRecursively()
                return@withContext Result.failure(IllegalStateException("Content hash mismatch: expected ${manifest.contentHash}, got $contentHash"))
            }

            contentDir.copyRecursively(File(bundleDir, "rootfs"))

            if (!RuntimeStoreSchema.writeManifest(manifest, rootDir)) {
                bundleDir.deleteRecursively()
                return@withContext Result.failure(IllegalStateException("Failed to write manifest"))
            }

            Timber.i("Registered base bundle: ${manifest.id}")
            Result.success(manifest)
        } catch (e: Exception) {
            Timber.e(e, "Failed to register base bundle: ${manifest.id}")
            Result.failure(e)
        }
    }

    suspend fun registerRuntime(manifest: RuntimeManifest, contentDir: File): Result<RuntimeManifest> = withContext(Dispatchers.IO) {
        try {
            if (!manifest.isValid()) {
                return@withContext Result.failure(IllegalArgumentException("Invalid RuntimeManifest: ${manifest.validate()}"))
            }

            val bundleDir = RuntimeStoreLayout.runtimeDir(rootDir, manifest.id)
            if (bundleDir.exists()) {
                Timber.w("Runtime bundle already exists: ${manifest.id}")
                return@withContext Result.failure(IllegalStateException("Runtime bundle already exists: ${manifest.id}"))
            }

            bundleDir.mkdirs()

            val contentHash = computeDirectoryHash(contentDir)
            if (contentHash != manifest.contentHash) {
                bundleDir.deleteRecursively()
                return@withContext Result.failure(IllegalStateException("Content hash mismatch: expected ${manifest.contentHash}, got $contentHash"))
            }

            contentDir.copyRecursively(bundleDir)

            if (!RuntimeStoreSchema.writeManifest(manifest, rootDir)) {
                bundleDir.deleteRecursively()
                return@withContext Result.failure(IllegalStateException("Failed to write manifest"))
            }

            Timber.i("Registered runtime bundle: ${manifest.id}")
            Result.success(manifest)
        } catch (e: Exception) {
            Timber.e(e, "Failed to register runtime bundle: ${manifest.id}")
            Result.failure(e)
        }
    }

    suspend fun registerDriver(manifest: DriverManifest, contentDir: File): Result<DriverManifest> = withContext(Dispatchers.IO) {
        try {
            if (!manifest.isValid()) {
                return@withContext Result.failure(IllegalArgumentException("Invalid DriverManifest: ${manifest.validate()}"))
            }

            val bundleDir = RuntimeStoreLayout.driverDir(rootDir, manifest.id)
            if (bundleDir.exists()) {
                Timber.w("Driver bundle already exists: ${manifest.id}")
                return@withContext Result.failure(IllegalStateException("Driver bundle already exists: ${manifest.id}"))
            }

            bundleDir.mkdirs()

            val contentHash = computeDirectoryHash(contentDir)
            if (contentHash != manifest.contentHash) {
                bundleDir.deleteRecursively()
                return@withContext Result.failure(IllegalStateException("Content hash mismatch: expected ${manifest.contentHash}, got $contentHash"))
            }

            contentDir.copyRecursively(bundleDir)

            if (!RuntimeStoreSchema.writeManifest(manifest, rootDir)) {
                bundleDir.deleteRecursively()
                return@withContext Result.failure(IllegalStateException("Failed to write manifest"))
            }

            Timber.i("Registered driver bundle: ${manifest.id}")
            Result.success(manifest)
        } catch (e: Exception) {
            Timber.e(e, "Failed to register driver bundle: ${manifest.id}")
            Result.failure(e)
        }
    }

    suspend fun verifyBundle(bundleType: BundleType, bundleId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            when (bundleType) {
                is BundleType.Base -> {
                    val manifest = RuntimeStoreSchema.readBaseManifest(rootDir, bundleId) ?: return@withContext false
                    val contentDir = RuntimeStoreLayout.baseDir(rootDir, bundleId)
                    val rootfsDir = File(contentDir, "rootfs")
                    if (!rootfsDir.exists()) return@withContext false
                    computeDirectoryHash(rootfsDir) == manifest.contentHash
                }

                is BundleType.Runtime -> {
                    val manifest = RuntimeStoreSchema.readRuntimeManifest(rootDir, bundleId) ?: return@withContext false
                    val contentDir = RuntimeStoreLayout.runtimeDir(rootDir, bundleId)
                    if (!contentDir.exists()) return@withContext false
                    computeDirectoryHash(contentDir) == manifest.contentHash
                }

                is BundleType.Driver -> {
                    val manifest = RuntimeStoreSchema.readDriverManifest(rootDir, bundleId) ?: return@withContext false
                    val contentDir = RuntimeStoreLayout.driverDir(rootDir, bundleId)
                    if (!contentDir.exists()) return@withContext false
                    computeDirectoryHash(contentDir) == manifest.contentHash
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to verify bundle: $bundleType/$bundleId")
            false
        }
    }

    fun getBase(baseId: String): BaseManifest? = RuntimeStoreSchema.readBaseManifest(rootDir, baseId)

    fun getRuntime(runtimeId: String): RuntimeManifest? = RuntimeStoreSchema.readRuntimeManifest(rootDir, runtimeId)

    fun getDriver(driverId: String): DriverManifest? = RuntimeStoreSchema.readDriverManifest(rootDir, driverId)

    fun listBases(): List<BaseManifest> = RuntimeStoreSchema.listBases(rootDir)

    fun listRuntimes(): List<RuntimeManifest> = RuntimeStoreSchema.listRuntimes(rootDir)

    fun listDrivers(): List<DriverManifest> = RuntimeStoreSchema.listDrivers(rootDir)

    suspend fun deleteBase(baseId: String): Boolean = withContext(Dispatchers.IO) {
        RuntimeStoreSchema.deleteBase(rootDir, baseId)
    }

    suspend fun deleteRuntime(runtimeId: String): Boolean = withContext(Dispatchers.IO) {
        RuntimeStoreSchema.deleteRuntime(rootDir, runtimeId)
    }

    suspend fun deleteDriver(driverId: String): Boolean = withContext(Dispatchers.IO) {
        RuntimeStoreSchema.deleteDriver(rootDir, driverId)
    }

    private fun computeDirectoryHash(dir: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        dir.walkTopDown()
            .filter { it.isFile }
            .sortedBy { it.relativeTo(dir).path }
            .forEach { file ->
                digest.update(file.relativeTo(dir).path.toString().toByteArray())
                digest.update(file.readBytes())
            }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
