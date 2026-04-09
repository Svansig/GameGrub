package app.gamegrub.api.compatibility

import android.content.Context
import androidx.compose.ui.graphics.Color
import app.gamegrub.Constants
import app.gamegrub.R
import app.gamegrub.network.NetworkManager
import app.gamegrub.service.auth.KeyAttestationHelper
import app.gamegrub.service.auth.PlayIntegrity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

@Singleton
class GameCompatibilityService @Inject constructor(
    private val cache: GameCompatibilityCache,
    @param:ApplicationContext private val context: Context,
) {
    private val httpClient = NetworkManager.http

    data class GameCompatibilityResponse(
        val gameName: String,
        val totalPlayableCount: Int,
        val gpuPlayableCount: Int,
        val avgRating: Float,
        val hasBeenTried: Boolean,
        val isNotWorking: Boolean,
    )

    data class CompatibilityMessage(
        val text: String,
        val color: Color,
    )

    fun getCompatibilityMessageFromResponse(context: Context, response: GameCompatibilityResponse): CompatibilityMessage {
        return when {
            response.totalPlayableCount > 0 && response.gpuPlayableCount > 0 ->
                CompatibilityMessage(context.getString(R.string.best_config_exact_gpu_match), Color.Green)

            response.gpuPlayableCount == 0 && response.totalPlayableCount > 0 ->
                CompatibilityMessage(context.getString(R.string.best_config_fallback_match), Color.Yellow)

            response.isNotWorking ->
                CompatibilityMessage(context.getString(R.string.library_not_compatible), Color.Red)

            else ->
                CompatibilityMessage(context.getString(R.string.library_compatibility_unknown), Color.Gray)
        }
    }

    suspend fun getCompatibilityMessageForGame(
        context: Context,
        gameName: String,
        gpuName: String,
    ): CompatibilityMessage? {
        if (gameName.isBlank() || gpuName.isBlank() || gpuName == "Unknown GPU") {
            return null
        }

        val responses = getCompatibility(listOf(gameName), gpuName)
        val response = responses[gameName] ?: responses.values.firstOrNull {
            it.gameName.equals(gameName, ignoreCase = true)
        }

        return response?.let { getCompatibilityMessageFromResponse(context, it) }
    }

    suspend fun getCompatibility(
        gameNames: List<String>,
        gpuName: String,
    ): Map<String, GameCompatibilityResponse> = withContext(Dispatchers.IO) {
        if (gameNames.isEmpty()) {
            return@withContext emptyMap()
        }

        val result = mutableMapOf<String, GameCompatibilityResponse>()

        for (gameName in gameNames) {
            cache.getCached(gameName)?.let { cached ->
                result[gameName] = cached
            }
        }

        val uncached = gameNames.filter { it !in result }
        if (uncached.isEmpty()) {
            Timber.tag("GameCompatibilityService").d("All ${gameNames.size} games from cache")
            return@withContext result
        }

        Timber.tag("GameCompatibilityService").d("Cache hit: ${result.size}, fetching: ${uncached.size}")

        val fetched = fetchFromApi(uncached, gpuName)
        if (fetched != null) {
            cache.cacheAll(fetched)
            result.putAll(fetched)
        }

        result
    }

    fun clearCache() = cache.clear()

    private suspend fun fetchFromApi(
        gameNames: List<String>,
        gpuName: String,
    ): Map<String, GameCompatibilityResponse>? = withContext(Dispatchers.IO) {
        try {
            val requestBody = JSONObject().apply {
                put("gameNames", JSONArray(gameNames))
                put("gpuName", gpuName)
            }

            val attestation = KeyAttestationHelper.getAttestationFields(
                context = context,
                baseUrl = "https://api.gamenative.app",
            )
            if (attestation != null) {
                requestBody.put("nonce", attestation.first)
                requestBody.put("attestationChain", JSONArray(attestation.second))
            }

            val mediaType = Constants.Protocol.MIME_APPLICATION_JSON.toMediaType()
            val bodyString = requestBody.toString()
            val body = bodyString.toRequestBody(mediaType)

            val integrityToken = PlayIntegrity.requestToken(bodyString.toByteArray())

            val requestBuilder = Request.Builder()
                .url(API_BASE_URL)
                .post(body)
                .header("Content-Type", Constants.Protocol.MIME_APPLICATION_JSON)
            if (integrityToken != null) {
                requestBuilder.header("X-Integrity-Token", integrityToken)
            }
            val request = requestBuilder.build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Timber.tag("GameCompatibilityService").w("API request failed - HTTP ${response.code}")
                    return@withContext null
                }

                val responseBody = response.body.string()
                val jsonResponse = JSONObject(responseBody)

                val result = mutableMapOf<String, GameCompatibilityResponse>()
                val keys = jsonResponse.keys()

                while (keys.hasNext()) {
                    val gameName = keys.next()
                    val gameData = jsonResponse.getJSONObject(gameName)

                    val compatibilityResponse = GameCompatibilityResponse(
                        gameName = gameName,
                        totalPlayableCount = gameData.optInt("totalPlayableCount", 0),
                        gpuPlayableCount = gameData.optInt("gpuPlayableCount", 0),
                        avgRating = gameData.optDouble("avgRating", 0.0).toFloat(),
                        hasBeenTried = gameData.optBoolean("hasBeenTried", false),
                        isNotWorking = gameData.optBoolean("isNotWorking", false),
                    )

                    result[gameName] = compatibilityResponse
                }

                Timber.tag("GameCompatibilityService").d("Fetched compatibility for ${result.size} games")
                result
            }
        } catch (e: Exception) {
            Timber.tag("GameCompatibilityService").e(e, "Error fetching compatibility data: ${e.message}")
            null
        }
    }

    companion object {
        private const val API_BASE_URL = "https://api.gamenative.app/api/game-runs"
    }
}
