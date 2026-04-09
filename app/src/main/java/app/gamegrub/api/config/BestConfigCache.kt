package app.gamegrub.api.config

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import timber.log.Timber
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BestConfigCache @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    companion object {
        private const val CACHE_TTL_MS = 24 * 60 * 60 * 1000L
        private const val CACHE_FILE_NAME = "best_config_cache.json"
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    private val cache = ConcurrentHashMap<String, CachedEntry>()
    private val loaded = AtomicBoolean(false)

    private val cacheFile: File
        get() = File(context.cacheDir, CACHE_FILE_NAME)

    @Serializable
    data class CachedEntry(
        val bestConfigJson: String,
        val matchType: String,
        val matchedGpu: String,
        val matchedDeviceId: Int,
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

            rawCache.forEach { (key, entry) ->
                if (now - entry.timestamp < CACHE_TTL_MS) {
                    cache[key] = entry
                }
            }

            val expiredCount = rawCache.size - cache.size
            if (expiredCount > 0) {
                Timber.tag("BestConfigCache").d("Loaded ${cache.size} valid entries ($expiredCount expired)")
            } else {
                Timber.tag("BestConfigCache").d("Loaded ${cache.size} entries from storage")
            }

            if (expiredCount > 0) {
                persistCache()
            }
        } catch (e: Exception) {
            Timber.tag("BestConfigCache").e(e, "Failed to load cache")
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
                Timber.tag("BestConfigCache").d("Persisted ${cache.size} entries")
            } catch (e: Exception) {
                Timber.tag("BestConfigCache").e(e, "Failed to persist cache")
            }
        }
    }

    fun getCached(key: String): BestConfigService.BestConfigResponse? {
        loadCache()

        val entry = cache[key] ?: return null

        if (System.currentTimeMillis() - entry.timestamp >= CACHE_TTL_MS) {
            cache.remove(key)
            Timber.tag("BestConfigCache").d("Expired entry removed on access: $key")
            persistCache()
            return null
        }

        return BestConfigService.BestConfigResponse(
            bestConfig = Json.parseToJsonElement(entry.bestConfigJson) as JsonObject,
            matchType = entry.matchType,
            matchedGpu = entry.matchedGpu,
            matchedDeviceId = entry.matchedDeviceId,
        )
    }

    fun cache(key: String, response: BestConfigService.BestConfigResponse) {
        loadCache()
        cache[key] = CachedEntry(
            bestConfigJson = response.bestConfig.toString(),
            matchType = response.matchType,
            matchedGpu = response.matchedGpu,
            matchedDeviceId = response.matchedDeviceId,
            timestamp = System.currentTimeMillis(),
        )
        persistCache()
    }

    fun clear() {
        cache.clear()
        if (cacheFile.exists()) {
            cacheFile.delete()
        }
        Timber.tag("BestConfigCache").d("Cache cleared")
    }
}
