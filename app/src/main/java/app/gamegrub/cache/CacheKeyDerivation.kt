package app.gamegrub.cache

import app.gamegrub.cache.manifest.CacheManifest
import app.gamegrub.cache.manifest.CacheType
import timber.log.Timber
import java.io.File

data class CacheKey(
    val baseId: String,
    val runtimeId: String,
    val driverId: String?,
    val profileId: String?,
    val exeHash: String?,
) {
    fun toPathString(): String = listOfNotNull(
        baseId,
        runtimeId,
        driverId,
        profileId,
        exeHash,
    ).joinToString("-")

    companion object {
        fun fromContainer(
            baseId: String,
            runtimeId: String,
            driverId: String?,
            profileId: String?,
            exeHash: String? = null,
        ): CacheKey = CacheKey(
            baseId = baseId,
            runtimeId = runtimeId,
            driverId = driverId,
            profileId = profileId,
            exeHash = exeHash,
        )
    }
}

sealed class InvalidationPolicy {
    data class TimeBased(val maxAgeMs: Long) : InvalidationPolicy()
    data class SizeBased(val maxSizeBytes: Long) : InvalidationPolicy()
    data class VersionBased(val expectedVersion: String) : InvalidationPolicy()
    data object Manual : InvalidationPolicy()
    data object Never : InvalidationPolicy()
}

object CacheKeyDerivation {
    private val hashRegex = Regex("^[a-fA-F0-9]{8,64}$")

    fun deriveKey(
        baseId: String,
        runtimeId: String,
        driverId: String? = null,
        profileId: String? = null,
        exeHash: String? = null,
    ): CacheKey {
        return CacheKey(
            baseId = requireNotBlank(baseId, "baseId"),
            runtimeId = requireNotBlank(runtimeId, "runtimeId"),
            driverId = driverId?.takeIf { it.isNotBlank() },
            profileId = profileId?.takeIf { it.isNotBlank() },
            exeHash = exeHash?.takeIf { hashRegex.matches(it) },
        )
    }

    fun deriveShaderCacheKey(
        baseId: String,
        runtimeId: String,
        driverId: String,
    ): CacheKey = CacheKey(
        baseId = baseId,
        runtimeId = runtimeId,
        driverId = driverId,
        profileId = null,
        exeHash = null,
    )

    fun deriveTranslatorCacheKey(
        baseId: String,
        runtimeId: String,
        profileId: String,
        exeHash: String,
    ): CacheKey = CacheKey(
        baseId = baseId,
        runtimeId = runtimeId,
        driverId = null,
        profileId = profileId,
        exeHash = exeHash,
    )

    fun deriveProbeCacheKey(
        baseId: String,
        runtimeId: String,
    ): CacheKey = CacheKey(
        baseId = baseId,
        runtimeId = runtimeId,
        driverId = null,
        profileId = null,
        exeHash = null,
    )

    private fun requireNotBlank(value: String, name: String): String {
        check(value.isNotBlank()) { "$name cannot be blank" }
        return value
    }
}

object CacheInvalidationPolicy {
    fun shouldInvalidate(
        manifest: CacheManifest,
        policy: InvalidationPolicy,
    ): Boolean {
        return when (policy) {
            is InvalidationPolicy.TimeBased -> {
                val age = System.currentTimeMillis() - manifest.lastAccessed
                age > policy.maxAgeMs
            }
            is InvalidationPolicy.SizeBased -> {
                manifest.sizeBytes > policy.maxSizeBytes
            }
            is InvalidationPolicy.VersionBased -> {
                manifest.metadata["version"] != policy.expectedVersion
            }
            is InvalidationPolicy.Manual -> true
            is InvalidationPolicy.Never -> false
        }
    }

    fun computeInvalidation(
        manifests: List<CacheManifest>,
        policy: InvalidationPolicy,
    ): List<CacheManifest> {
        return manifests.filter { shouldInvalidate(it, policy) }
    }
}

class CacheGarbageCollector(
    private val maxTotalSizeBytes: Long = 5L * 1024 * 1024 * 1024,
    private val maxAgeMs: Long = 30L * 24 * 60 * 60 * 1000,
) {
    data class GcResult(
        val evictedCaches: List<CacheManifest>,
        val freedBytes: Long,
        val remainingSizeBytes: Long,
    )

    fun runGc(manifests: List<CacheManifest>): GcResult {
        val sortedByAge = manifests.sortedBy { it.lastAccessed }
        
        var freedBytes = 0L
        val evicted = mutableListOf<CacheManifest>()
        var currentSize = manifests.sumOf { it.sizeBytes }
        
        val toEvict = mutableListOf<CacheManifest>()

        for (manifest in sortedByAge) {
            if (manifest.lastAccessed < System.currentTimeMillis() - maxAgeMs) {
                toEvict.add(manifest)
            }
        }

        if (currentSize > maxTotalSizeBytes) {
            val targetFreed = currentSize - maxTotalSizeBytes
            var needed = targetFreed
            
            for (manifest in sortedByAge) {
                if (needed <= 0) break
                if (manifest !in toEvict) {
                    toEvict.add(manifest)
                    needed -= manifest.sizeBytes
                }
            }
        }

        for (manifest in toEvict) {
            freedBytes += manifest.sizeBytes
            evicted.add(manifest)
            currentSize -= manifest.sizeBytes
        }

        if (evicted.isNotEmpty()) {
            Timber.i("GC: evicted ${evicted.size} caches, freed ${freedBytes / 1024 / 1024}MB")
        }

        return GcResult(
            evictedCaches = evicted,
            freedBytes = freedBytes,
            remainingSizeBytes = currentSize,
        )
    }
}