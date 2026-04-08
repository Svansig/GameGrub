package app.gamegrub.cache.manifest

import kotlinx.serialization.Serializable

@Serializable
enum class CacheType {
    SHADER,
    TRANSLATOR,
    PROBE,
    TEMP,
    SESSION,
}

@Serializable
data class CacheManifest(
    val id: String,
    val cacheType: CacheType,
    val createdAt: Long = System.currentTimeMillis(),
    val lastAccessed: Long = System.currentTimeMillis(),
    val baseId: String? = null,
    val runtimeId: String? = null,
    val driverId: String? = null,
    val profileId: String? = null,
    val exeHash: String? = null,
    val sizeBytes: Long = 0L,
    val path: String,
    val metadata: Map<String, String> = emptyMap(),
) {
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        if (id.isBlank()) errors.add("CacheManifest.id cannot be blank")
        if (path.isBlank()) errors.add("CacheManifest.path cannot be blank")
        return errors
    }

    fun isValid(): Boolean = validate().isEmpty()

    fun computeCacheKey(): String {
        val parts = listOfNotNull(
            baseId,
            runtimeId,
            driverId,
            profileId,
            exeHash,
        )
        return parts.joinToString("-")
    }
}

@Serializable
data class CacheManifestStore(
    val version: Int = 1,
    val caches: Map<String, CacheManifest> = emptyMap(),
    val totalSizeBytes: Long = 0L,
    val lastUpdated: Long = System.currentTimeMillis(),
) {
    fun addCache(cache: CacheManifest): CacheManifestStore {
        val newCaches = caches + (cache.id to cache)
        val newSize = totalSizeBytes + cache.sizeBytes
        return copy(
            caches = newCaches,
            totalSizeBytes = newSize,
            lastUpdated = System.currentTimeMillis(),
        )
    }

    fun removeCache(cacheId: String): CacheManifestStore {
        val removed = caches[cacheId] ?: return this
        val newCaches = caches - cacheId
        return copy(
            caches = newCaches,
            totalSizeBytes = (totalSizeBytes - removed.sizeBytes).coerceAtLeast(0L),
            lastUpdated = System.currentTimeMillis(),
        )
    }

    fun findByKey(key: String): List<CacheManifest> {
        return caches.values.filter { it.computeCacheKey() == key }
    }
}