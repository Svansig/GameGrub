package app.gamegrub.runtime.store

import app.gamegrub.runtime.manifest.BaseManifest
import app.gamegrub.runtime.manifest.DriverManifest
import app.gamegrub.runtime.manifest.RuntimeManifest
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File

object RuntimeStoreLayout {
    private const val BASE_DIR = "runtime-store"
    private const val BASES_SUBDIR = "bases"
    private const val RUNTIMES_SUBDIR = "runtimes"
    private const val DRIVERS_SUBDIR = "drivers"
    private const val INDEX_DIR = "index"

    fun basesDir(rootDir: File): File = File(File(rootDir, BASE_DIR), BASES_SUBDIR)
    fun runtimesDir(rootDir: File): File = File(File(rootDir, BASE_DIR), RUNTIMES_SUBDIR)
    fun driversDir(rootDir: File): File = File(File(rootDir, BASE_DIR), DRIVERS_SUBDIR)
    fun indexDir(rootDir: File): File = File(File(rootDir, BASE_DIR), INDEX_DIR)

    fun baseDir(rootDir: File, baseId: String): File = File(basesDir(rootDir), baseId)
    fun runtimeDir(rootDir: File, runtimeId: String): File = File(runtimesDir(rootDir), runtimeId)
    fun driverDir(rootDir: File, driverId: String): File = File(driversDir(rootDir), driverId)

    fun baseManifestPath(rootDir: File, baseId: String): File = File(baseDir(rootDir, baseId), "manifest.json")
    fun runtimeManifestPath(rootDir: File, runtimeId: String): File = File(runtimeDir(rootDir, runtimeId), "manifest.json")
    fun driverManifestPath(rootDir: File, driverId: String): File = File(driverDir(rootDir, driverId), "manifest.json")

    fun basesIndexPath(rootDir: File): File = File(indexDir(rootDir), "bases.json")
    fun runtimesIndexPath(rootDir: File): File = File(indexDir(rootDir), "runtimes.json")
    fun driversIndexPath(rootDir: File): File = File(indexDir(rootDir), "drivers.json")
}

sealed class BundleType {
    data object Base : BundleType()
    data object Runtime : BundleType()
    data object Driver : BundleType()
}

object RuntimeStoreSchema {
    private val json = Json {
        prettyPrint = true
        prettyPrint = true
    }

    fun verifyDirectoryStructure(rootDir: File): Boolean {
        val bases = RuntimeStoreLayout.basesDir(rootDir)
        val runtimes = RuntimeStoreLayout.runtimesDir(rootDir)
        val drivers = RuntimeStoreLayout.driversDir(rootDir)
        val index = RuntimeStoreLayout.indexDir(rootDir)

        return bases.exists() && runtimes.exists() && drivers.exists() && index.exists()
    }

    fun createDirectoryStructure(rootDir: File): Boolean {
        return try {
            RuntimeStoreLayout.basesDir(rootDir).mkdirs()
            RuntimeStoreLayout.runtimesDir(rootDir).mkdirs()
            RuntimeStoreLayout.driversDir(rootDir).mkdirs()
            RuntimeStoreLayout.indexDir(rootDir).mkdirs()
            Timber.i("RuntimeStore directory structure created at ${rootDir.path}/runtime-store")
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to create RuntimeStore directory structure")
            false
        }
    }

    fun ensureInitialized(rootDir: File): Boolean {
        if (!verifyDirectoryStructure(rootDir)) {
            return createDirectoryStructure(rootDir)
        }
        return true
    }

    fun writeManifest(manifest: BaseManifest, rootDir: File): Boolean {
        return try {
            val path = RuntimeStoreLayout.baseManifestPath(rootDir, manifest.id)
            path.parentFile?.mkdirs()
            path.writeText(json.encodeToString(manifest))
            Timber.d("Written BaseManifest for ${manifest.id}")
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to write BaseManifest for ${manifest.id}")
            false
        }
    }

    fun writeManifest(manifest: RuntimeManifest, rootDir: File): Boolean {
        return try {
            val path = RuntimeStoreLayout.runtimeManifestPath(rootDir, manifest.id)
            path.parentFile?.mkdirs()
            path.writeText(json.encodeToString(manifest))
            Timber.d("Written RuntimeManifest for ${manifest.id}")
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to write RuntimeManifest for ${manifest.id}")
            false
        }
    }

    fun writeManifest(manifest: DriverManifest, rootDir: File): Boolean {
        return try {
            val path = RuntimeStoreLayout.driverManifestPath(rootDir, manifest.id)
            path.parentFile?.mkdirs()
            path.writeText(json.encodeToString(manifest))
            Timber.d("Written DriverManifest for ${manifest.id}")
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to write DriverManifest for ${manifest.id}")
            false
        }
    }

    fun readBaseManifest(rootDir: File, baseId: String): BaseManifest? {
        return try {
            val path = RuntimeStoreLayout.baseManifestPath(rootDir, baseId)
            if (path.exists()) {
                json.decodeFromString<BaseManifest>(path.readText())
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to read BaseManifest for $baseId")
            null
        }
    }

    fun readRuntimeManifest(rootDir: File, runtimeId: String): RuntimeManifest? {
        return try {
            val path = RuntimeStoreLayout.runtimeManifestPath(rootDir, runtimeId)
            if (path.exists()) {
                json.decodeFromString<RuntimeManifest>(path.readText())
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to read RuntimeManifest for $runtimeId")
            null
        }
    }

    fun readDriverManifest(rootDir: File, driverId: String): DriverManifest? {
        return try {
            val path = RuntimeStoreLayout.driverManifestPath(rootDir, driverId)
            if (path.exists()) {
                json.decodeFromString<DriverManifest>(path.readText())
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to read DriverManifest for $driverId")
            null
        }
    }

    fun listBases(rootDir: File): List<BaseManifest> {
        return RuntimeStoreLayout.basesDir(rootDir).listFiles()?.mapNotNull { dir ->
            readBaseManifest(rootDir, dir.name)
        } ?: emptyList()
    }

    fun listRuntimes(rootDir: File): List<RuntimeManifest> {
        return RuntimeStoreLayout.runtimesDir(rootDir).listFiles()?.mapNotNull { dir ->
            readRuntimeManifest(rootDir, dir.name)
        } ?: emptyList()
    }

    fun listDrivers(rootDir: File): List<DriverManifest> {
        return RuntimeStoreLayout.driversDir(rootDir).listFiles()?.mapNotNull { dir ->
            readDriverManifest(rootDir, dir.name)
        } ?: emptyList()
    }

    fun deleteBase(rootDir: File, baseId: String): Boolean {
        return try {
            val dir = RuntimeStoreLayout.baseDir(rootDir, baseId)
            if (dir.exists()) {
                dir.deleteRecursively()
                Timber.i("Deleted base bundle: $baseId")
            }
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete base bundle: $baseId")
            false
        }
    }

    fun deleteRuntime(rootDir: File, runtimeId: String): Boolean {
        return try {
            val dir = RuntimeStoreLayout.runtimeDir(rootDir, runtimeId)
            if (dir.exists()) {
                dir.deleteRecursively()
                Timber.i("Deleted runtime bundle: $runtimeId")
            }
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete runtime bundle: $runtimeId")
            false
        }
    }

    fun deleteDriver(rootDir: File, driverId: String): Boolean {
        return try {
            val dir = RuntimeStoreLayout.driverDir(rootDir, driverId)
            if (dir.exists()) {
                dir.deleteRecursively()
                Timber.i("Deleted driver bundle: $driverId")
            }
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete driver bundle: $driverId")
            false
        }
    }
}
