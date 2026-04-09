package app.gamegrub.container.store

import app.gamegrub.container.manifest.ContainerManifest
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File

object ContainerStoreLayout {
    private const val CONTAINER_DIR = "containers"

    fun containersDir(rootDir: File): File = File(rootDir, CONTAINER_DIR)

    fun containerDir(rootDir: File, containerId: String): File = File(containersDir(rootDir), containerId)

    fun containerManifestPath(rootDir: File, containerId: String): File = File(containerDir(rootDir, containerId), "manifest.json")

    fun prefixDir(rootDir: File, containerId: String): File = File(containerDir(rootDir, containerId), "prefix")

    fun installDir(rootDir: File, containerId: String): File = File(containerDir(rootDir, containerId), "install")

    fun savesDir(rootDir: File, containerId: String): File = File(containerDir(rootDir, containerId), "saves")

    fun overridesDir(rootDir: File, containerId: String): File = File(containerDir(rootDir, containerId), "overrides")

    fun cacheDir(rootDir: File, containerId: String): File = File(containerDir(rootDir, containerId), "cache")
}

object ContainerStoreSchema {
    private val json = Json { prettyPrint = true }

    fun verifyDirectoryStructure(rootDir: File): Boolean {
        return ContainerStoreLayout.containersDir(rootDir).exists()
    }

    fun createDirectoryStructure(rootDir: File): Boolean {
        return try {
            ContainerStoreLayout.containersDir(rootDir).mkdirs()
            Timber.i("ContainerStore directory structure created at ${rootDir.path}/containers")
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to create ContainerStore directory structure")
            false
        }
    }

    fun ensureInitialized(rootDir: File): Boolean {
        if (!verifyDirectoryStructure(rootDir)) {
            return createDirectoryStructure(rootDir)
        }
        return true
    }

    fun writeManifest(manifest: ContainerManifest, rootDir: File): Boolean {
        return try {
            val path = ContainerStoreLayout.containerManifestPath(rootDir, manifest.id)
            path.parentFile?.mkdirs()
            path.writeText(json.encodeToString(manifest))
            Timber.d("Written ContainerManifest for ${manifest.id}")
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to write ContainerManifest for ${manifest.id}")
            false
        }
    }

    fun readManifest(rootDir: File, containerId: String): ContainerManifest? {
        return try {
            val path = ContainerStoreLayout.containerManifestPath(rootDir, containerId)
            if (path.exists()) {
                json.decodeFromString<ContainerManifest>(path.readText())
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to read ContainerManifest for $containerId")
            null
        }
    }

    fun listContainers(rootDir: File): List<ContainerManifest> {
        return ContainerStoreLayout.containersDir(rootDir).listFiles()?.mapNotNull { dir ->
            readManifest(rootDir, dir.name)
        } ?: emptyList()
    }

    fun deleteContainer(rootDir: File, containerId: String): Boolean {
        return try {
            val dir = ContainerStoreLayout.containerDir(rootDir, containerId)
            if (dir.exists()) {
                dir.deleteRecursively()
                Timber.i("Deleted container: $containerId")
            }
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete container: $containerId")
            false
        }
    }

    fun ensureContainerDirectories(rootDir: File, containerId: String): Boolean {
        return try {
            ContainerStoreLayout.prefixDir(rootDir, containerId).mkdirs()
            ContainerStoreLayout.installDir(rootDir, containerId).mkdirs()
            ContainerStoreLayout.savesDir(rootDir, containerId).mkdirs()
            ContainerStoreLayout.overridesDir(rootDir, containerId).mkdirs()
            ContainerStoreLayout.cacheDir(rootDir, containerId).mkdirs()
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to create container directories for: $containerId")
            false
        }
    }
}
