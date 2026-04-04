package app.gamegrub.utils.manifest

import android.content.Context
import app.gamegrub.Constants
import app.gamegrub.PrefManager
import app.gamegrub.network.NetworkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Request
import timber.log.Timber

object ManifestRepository {
    private const val ONE_DAY_MS = 24 * 60 * 60 * 1000L
    private val json = Json { ignoreUnknownKeys = true }

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
