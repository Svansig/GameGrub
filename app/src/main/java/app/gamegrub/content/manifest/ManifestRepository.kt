package app.gamegrub.content.manifest

import android.content.Context
import app.gamegrub.Constants
import app.gamegrub.PrefManager
import app.gamegrub.network.NetworkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Request
import timber.log.Timber

/**
 * Repository for fetching and caching component manifest data.
 *
 * This object provides access to the remote component manifest (list of available
 * DXVK, VKD3D, Wine, Proton, etc. versions) with in-memory caching and
 * one-day staleness semantics.
 *
 * Ownership: This class belongs to the `content/manifest` package, representing
 * the content/component management domain. It was migrated from `utils/manifest`
 * as part of the COH-028 utils ownership consolidation effort.
 */
object ManifestRepository {
    /** Cache validity duration: 24 hours */
    private const val ONE_DAY_MS = 24 * 60 * 60 * 1000L

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Loads manifest data, using cached version if available and not stale.
     *
     * @param context Android context for network operations
     * @return Parsed ManifestData, or empty manifest if fetch fails
     */
    suspend fun loadManifest(context: Context): ManifestData {
        val cachedJson = PrefManager.componentManifestJson
        val cachedManifest = parseManifest(cachedJson) ?: ManifestData.empty()
        val lastFetchedAt = PrefManager.componentManifestFetchedAt
        val isStale = System.currentTimeMillis() - lastFetchedAt >= ONE_DAY_MS

        if (cachedJson.isNotEmpty() && !isStale) {
            return cachedManifest
        }

        val fetched = fetchManifestJson()
        if (fetched != null) {
            val parsed = parseManifest(fetched)
            if (parsed != null) {
                val now = System.currentTimeMillis()
                PrefManager.componentManifestJson = fetched
                PrefManager.componentManifestFetchedAt = now
                return parsed
            }
        }

        return cachedManifest
    }

    /**
     * Fetches fresh manifest JSON from the remote endpoint.
     *
     * @return Raw JSON string, or null if fetch fails
     */
    private suspend fun fetchManifestJson(): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(Constants.Api.LEGACY_COMPONENT_MANIFEST_URL).build()
            NetworkManager.http.newCall(request).execute().use { response ->
                response.takeIf { it.isSuccessful }?.body?.string()
            }
        } catch (e: Exception) {
            Timber.e(e, "ManifestRepository: fetch failed")
            null
        }
    }

    /**
     * Parses manifest JSON string into domain model.
     *
     * @param jsonString Raw JSON to parse
     * @return Parsed ManifestData, or null if JSON is invalid
     */
    fun parseManifest(jsonString: String?): ManifestData? {
        if (jsonString.isNullOrBlank()) return null
        return try {
            json.decodeFromString<ManifestData>(jsonString)
        } catch (e: Exception) {
            Timber.e(e, "ManifestRepository: parse failed")
            null
        }
    }
}
