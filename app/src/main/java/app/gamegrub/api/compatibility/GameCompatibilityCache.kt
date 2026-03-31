package app.gamegrub.api.compatibility

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber

@Singleton
class GameCompatibilityCache @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val CACHE_TTL_MS = 6 * 60 * 60 * 1000L
        private const val CACHE_FILE_NAME = "compatibility_cache.json"
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    private val cache = ConcurrentHashMap<String, CachedEntry>()
    private val loaded = AtomicBoolean(false)

    private val cacheFile: File
        get() = File(context.cacheDir, CACHE_FILE_NAME)

    @Serializable
    data class CachedEntry(
        val gameName: String,
        val totalPlayableCount: Int,
        val gpuPlayableCount: Int,
        val avgRating: Float,
        val hasBeenTried: Boolean,
        val isNotWorking: Boolean,
        val timestamp: Long,
    )

    private fun loadCache() {
        if (loaded.get()) return

        try {
            if (!cacheFile.exists()) {
                loaded.set(true)
                return
            }

            val cacheJson = cacheFile.readText()
            if (cacheJson.isBlank()) {
                loaded.set(true)
                return
            }

            val rawCache = Json.decodeFromString<Map<String, CachedEntry>>(cacheJson)
            val now = System.currentTimeMillis()

            rawCache.forEach { (gameName, entry) ->
                if (now - entry.timestamp < CACHE_TTL_MS) {
                    cache[gameName] = entry
                }
            }

            val expiredCount = rawCache.size - cache.size
            if (expiredCount > 0) {
                Timber.tag("GameCompatibilityCache").d("Loaded ${cache.size} valid entries ($expiredCount expired)")
            } else {
                Timber.tag("GameCompatibilityCache").d("Loaded ${cache.size} entries from storage")
            }

            if (expiredCount > 0) {
                persistCache()
            }
        } catch (e: Exception) {
            Timber.tag("GameCompatibilityCache").e(e, "Failed to load cache")
        } finally {
            loaded.set(true)
        }
    }

    private fun persistCache() {
        scope.launch {
            try {
                val cacheSnapshot: Map<String, CachedEntry> = cache.toMap()
                val cacheJson = Json.encodeToString(cacheSnapshot)
                cacheFile.writeText(cacheJson)
                Timber.tag("GameCompatibilityCache").d("Persisted ${cache.size} entries")
            } catch (e: Exception) {
                Timber.tag("GameCompatibilityCache").e(e, "Failed to persist cache")
            }
        }
    }

    fun getCached(gameName: String): GameCompatibilityService.GameCompatibilityResponse? {
        loadCache()

        val entry = cache[gameName] ?: return null

        if (System.currentTimeMillis() - entry.timestamp >= CACHE_TTL_MS) {
            cache.remove(gameName)
            Timber.tag("GameCompatibilityCache").d("Expired entry removed on access: $gameName")
            persistCache()
            return null
        }

        return GameCompatibilityService.GameCompatibilityResponse(
            gameName = entry.gameName,
            totalPlayableCount = entry.totalPlayableCount,
            gpuPlayableCount = entry.gpuPlayableCount,
            avgRating = entry.avgRating,
            hasBeenTried = entry.hasBeenTried,
            isNotWorking = entry.isNotWorking,
        )
    }

    fun cache(gameName: String, response: GameCompatibilityService.GameCompatibilityResponse) {
        loadCache()
        cache[gameName] = CachedEntry(
            gameName = response.gameName,
            totalPlayableCount = response.totalPlayableCount,
            gpuPlayableCount = response.gpuPlayableCount,
            avgRating = response.avgRating,
            hasBeenTried = response.hasBeenTried,
            isNotWorking = response.isNotWorking,
            timestamp = System.currentTimeMillis(),
        )
        persistCache()
    }

    fun cacheAll(responses: Map<String, GameCompatibilityService.GameCompatibilityResponse>) {
        loadCache()
        val now = System.currentTimeMillis()
        responses.forEach { (gameName, response) ->
            cache[gameName] = CachedEntry(
                gameName = response.gameName,
                totalPlayableCount = response.totalPlayableCount,
                gpuPlayableCount = response.gpuPlayableCount,
                avgRating = response.avgRating,
                hasBeenTried = response.hasBeenTried,
                isNotWorking = response.isNotWorking,
                timestamp = now,
            )
        }
        persistCache()
    }

    fun isCached(gameName: String): Boolean = getCached(gameName) != null

    fun clear() {
        cache.clear()
        if (cacheFile.exists()) {
            cacheFile.delete()
        }
        Timber.tag("GameCompatibilityCache").d("Cache cleared")
    }

    fun size(): Int {
        loadCache()
        return cache.size
    }
}
