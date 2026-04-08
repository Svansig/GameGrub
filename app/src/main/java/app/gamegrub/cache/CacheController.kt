package app.gamegrub.cache

import app.gamegrub.cache.manifest.CacheManifest
import app.gamegrub.cache.manifest.CacheManifestStore
import app.gamegrub.cache.manifest.CacheType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CacheController @Inject constructor(
    private val rootDir: File,
) {
    private val json = Json { prettyPrint = true }
    private val cacheStoreFile: File by lazy { File(rootDir, "cache/cache-store.json") }
    private var cacheStore: CacheManifestStore = loadCacheStore()

    companion object {
        private const val CACHE_BASE_DIR = "cache"
        private const val SHADER_DIR = "shader"
        private const val TRANSLATOR_DIR = "translator"
        private const val PROBE_DIR = "probe"
        private const val SESSION_DIR = "session"
    }

    init {
        ensureCacheDirectoryExists()
    }

    private fun ensureCacheDirectoryExists() {
        File(rootDir, CACHE_BASE_DIR).mkdirs()
    }

    private fun loadCacheStore(): CacheManifestStore {
        return try {
            if (cacheStoreFile.exists()) {
                json.decodeFromString<CacheManifestStore>(cacheStoreFile.readText())
            } else {
                CacheManifestStore()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load cache store, starting fresh")
            CacheManifestStore()
        }
    }

    private fun saveCacheStore() {
        try {
            cacheStoreFile.parentFile?.mkdirs()
            cacheStoreFile.writeText(json.encodeToString(cacheStore))
        } catch (e: Exception) {
            Timber.e(e, "Failed to save cache store")
        }
    }

    fun getCacheDir(cacheType: CacheType, key: CacheKey): File {
        val subDir = when (cacheType) {
            CacheType.SHADER -> SHADER_DIR
            CacheType.TRANSLATOR -> TRANSLATOR_DIR
            CacheType.PROBE -> PROBE_DIR
            CacheType.TEMP, CacheType.SESSION -> SESSION_DIR
        }
        return File(File(rootDir, CACHE_BASE_DIR), "$subDir/${key.toPathString()}")
    }

    suspend fun createCache(
        cacheType: CacheType,
        key: CacheKey,
        metadata: Map<String, String> = emptyMap(),
    ): Result<CacheManifest> = withContext(Dispatchers.IO) {
        try {
            val cacheDir = getCacheDir(cacheType, key)
            cacheDir.mkdirs()

            val manifest = CacheManifest(
                id = UUID.randomUUID().toString(),
                cacheType = cacheType,
                baseId = key.baseId,
                runtimeId = key.runtimeId,
                driverId = key.driverId,
                profileId = key.profileId,
                exeHash = key.exeHash,
                sizeBytes = 0L,
                path = cacheDir.absolutePath,
                metadata = metadata,
            )

            cacheStore = cacheStore.addCache(manifest)
            saveCacheStore()

            Timber.i("Created cache: ${manifest.id} type=${cacheType.name} key=${key.toPathString()}")
            Result.success(manifest)
        } catch (e: Exception) {
            Timber.e(e, "Failed to create cache")
            Result.failure(e)
        }
    }

    suspend fun getCache(cacheType: CacheType, key: CacheKey): CacheManifest? {
        return cacheStore.findByKey(key.toPathString()).firstOrNull { it.cacheType == cacheType }
    }

    suspend fun getCacheById(cacheId: String): CacheManifest? {
        return cacheStore.caches[cacheId]
    }

    fun listCaches(cacheType: CacheType? = null): List<CacheManifest> {
        return if (cacheType != null) {
            cacheStore.caches.values.filter { it.cacheType == cacheType }
        } else {
            cacheStore.caches.values.toList()
        }
    }

    fun listShaderCaches(): List<CacheManifest> = listCaches(CacheType.SHADER)

    fun listTranslatorCaches(): List<CacheManifest> = listCaches(CacheType.TRANSLATOR)

    fun listProbeCaches(): List<CacheManifest> = listCaches(CacheType.PROBE)

    suspend fun updateAccessTime(cacheId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val existing = cacheStore.caches[cacheId] ?: return@withContext false
            val updated = existing.copy(lastAccessed = System.currentTimeMillis())
            cacheStore = cacheStore.removeCache(cacheId).addCache(updated)
            saveCacheStore()
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to update access time for cache: $cacheId")
            false
        }
    }

    suspend fun updateSize(cacheId: String, sizeBytes: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val existing = cacheStore.caches[cacheId] ?: return@withContext false
            val updated = existing.copy(sizeBytes = sizeBytes)
            cacheStore = cacheStore.removeCache(cacheId).addCache(updated)
            saveCacheStore()
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to update size for cache: $cacheId")
            false
        }
    }

    suspend fun deleteCache(cacheId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val existing = cacheStore.caches[cacheId] ?: return@withContext false
            
            val cacheDir = File(existing.path)
            if (cacheDir.exists()) {
                cacheDir.deleteRecursively()
            }

            cacheStore = cacheStore.removeCache(cacheId)
            saveCacheStore()

            Timber.i("Deleted cache: $cacheId")
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete cache: $cacheId")
            false
        }
    }

    suspend fun invalidateByPolicy(policy: InvalidationPolicy): Int = withContext(Dispatchers.IO) {
        val toInvalidate = CacheInvalidationPolicy.computeInvalidation(
            cacheStore.caches.values.toList(),
            policy,
        )

        var count = 0
        for (manifest in toInvalidate) {
            if (deleteCache(manifest.id)) {
                count++
            }
        }

        Timber.i("Invalidated $count caches by policy")
        count
    }

    suspend fun runGarbageCollection(
        maxTotalSizeBytes: Long = 5L * 1024 * 1024 * 1024,
        maxAgeMs: Long = 30L * 24 * 60 * 60 * 1000,
    ): CacheGarbageCollector.GcResult = withContext(Dispatchers.IO) {
        val gc = CacheGarbageCollector(maxTotalSizeBytes, maxAgeMs)
        val result = gc.runGc(cacheStore.caches.values.toList())

        for (manifest in result.evictedCaches) {
            deleteCache(manifest.id)
        }

        result
    }

    fun getTotalCacheSize(): Long = cacheStore.totalSizeBytes

    fun getCacheCount(): Int = cacheStore.caches.size

    suspend fun clearAllCaches(): Boolean = withContext(Dispatchers.IO) {
        try {
            for (manifest in cacheStore.caches.values.toList()) {
                deleteCache(manifest.id)
            }
            Timber.i("Cleared all caches")
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear all caches")
            false
        }
    }
}