package app.gamegrub.api.compatibility

import android.content.Context
import app.gamegrub.BuildConfig
import app.gamegrub.api.ApiResult
import app.gamegrub.device.DeviceQueryProvider
import app.gamegrub.data.GameSource
import app.gamegrub.service.amazon.AmazonService
import app.gamegrub.service.epic.EpicService
import app.gamegrub.service.gog.GOGService
import app.gamegrub.service.steam.SteamService
import app.gamegrub.utils.container.ContainerUtils
import app.gamegrub.utils.game.CustomGameScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber

/**
 * Compatibility feedback helper responsible for submitting post-session telemetry.
 */
object GameFeedbackUtils {

    /**
     * Submit user feedback and contextual device/config metadata for a game session.
     *
     * @param context Android context used to resolve data sources and submit API requests.
     * @param appId Container-backed application identifier.
     * @param rating User rating value.
     * @param tags User-selected feedback tags.
     * @param notes Optional free-form feedback text.
     * @return `true` when submission succeeds, otherwise `false`.
     */
    suspend fun submitGameFeedback(
        context: Context,
        appId: String,
        rating: Int,
        tags: List<String>,
        notes: String?,
    ): Boolean = withContext(Dispatchers.IO) {
        Timber.d("GameFeedbackUtils: Starting submitGameFeedback method with rating=$rating")
        try {
            val gameSource = ContainerUtils.extractGameSourceFromContainerId(appId)
            val gameId = ContainerUtils.extractGameIdFromContainerId(appId)
            val container = ContainerUtils.getContainer(context, appId)
            val configJson = JSONObject(container.containerJson)
            Timber.d("config string is: %s", container.containerJson)
            Timber.d("configJson: $configJson")

            val deviceQueryGateway = DeviceQueryProvider.from(context)
            val identitySnapshot = deviceQueryGateway.getIdentitySnapshot()

            // Get the game name from container or use a fallback
            val gameName = when (gameSource) {
                GameSource.CUSTOM_GAME -> {
                    val folderPath = CustomGameScanner.get().findCustomGameById(gameId) ?: ""
                    if (folderPath.isNotEmpty()) {
                        val game = CustomGameScanner.get().createLibraryItemFromFolder(folderPath)
                        game?.name ?: ""
                    } else {
                        ""
                    }
                }

                GameSource.AMAZON -> {
                    val productId = AmazonService.getProductIdByAppId(gameId)
                    if (!productId.isNullOrBlank()) {
                        AmazonService.getAmazonGameOf(productId)?.title ?: ""
                    } else {
                        ""
                    }
                }

                GameSource.EPIC -> {
                    val game = EpicService.getEpicGameOf(gameId)
                    game?.title ?: ""
                }

                GameSource.GOG -> {
                    val game = GOGService.getGOGGameOf(gameId.toString())
                    game?.title ?: ""
                }

                GameSource.STEAM -> {
                    val appInfo = SteamService.getAppInfoOf(gameId)
                    appInfo?.name ?: ""
                }
            }

            if (gameName.isEmpty()) {
                Timber.e("SubmitGameFeedback: Could not get game name for appId $appId")
                return@withContext false
            }
            Timber.d("GameFeedbackUtils: Game name: $gameName")

            // Get GPU info if available
            val gpu: String = try {
                Timber.d("GameFeedbackUtils: About to get GPU info")
                val gpuInfo = deviceQueryGateway.getGpuRendererOrUnknown() ?: "Unknown GPU"
                Timber.d("GameFeedbackUtils: GPU info: $gpuInfo")
                gpuInfo
            } catch (e: Exception) {
                Timber.e(e, "GameFeedbackUtils: Failed to get GPU info: ${e.message}")
                "Unknown GPU"
            }

            // Get SOC identifier
            val soc = try {
                val socName = deviceQueryGateway.getSocName()
                Timber.d("GameFeedbackUtils: SOC info: $socName")
                socName
            } catch (e: Exception) {
                Timber.e(e, "GameFeedbackUtils: Failed to get SOC info: ${e.message}")
                null
            }

            // Get Android version
            val androidVer = identitySnapshot.androidVersion
            Timber.d("GameFeedbackUtils: Android version: $androidVer")

            // Get app version
            val appVersion = BuildConfig.VERSION_NAME
            Timber.d("GameFeedbackUtils: App version: $appVersion")

            // Retrieve session data from container metadata
            val avgFpsStr = container.getSessionMetadata("avg_fps", "")
            val sessionLengthSecStr = container.getSessionMetadata("session_length_sec", "")
            val avgFps = if (avgFpsStr.isNotEmpty()) {
                try {
                    avgFpsStr.toFloat()
                } catch (e: NumberFormatException) {
                    Timber.w("GameFeedbackUtils: Failed to parse avg_fps: $avgFpsStr")
                    null
                }
            } else {
                null
            }
            val sessionLengthSec = if (sessionLengthSecStr.isNotEmpty()) {
                try {
                    sessionLengthSecStr.toInt()
                } catch (e: NumberFormatException) {
                    Timber.w("GameFeedbackUtils: Failed to parse session_length_sec: $sessionLengthSecStr")
                    null
                }
            } else {
                null
            }

            Timber.i(
                "GameFeedbackUtils: Submitting game feedback: game=%s, device=%s, rating=%d, tags=%s, avgFps=%s, sessionLengthSec=%s",
                gameName,
                identitySnapshot.model,
                rating,
                tags.joinToString(),
                avgFps,
                sessionLengthSec,
            )

            val result = GameRunApi.submit(
                gameName = gameName,
                deviceModel = identitySnapshot.model,
                deviceManufacturer = identitySnapshot.manufacturer,
                gpuName = gpu,
                socName = soc,
                androidVersion = androidVer,
                appVersion = appVersion,
                configs = configJson,
                rating = rating,
                tags = tags,
                notes = notes,
                avgFps = avgFps,
                sessionLengthSec = sessionLengthSec,
            )

            when (result) {
                is ApiResult.Success -> {
                    Timber.i("GameFeedbackUtils: Game feedback submitted successfully (id=${result.data.id})")
                    true
                }

                is ApiResult.HttpError -> {
                    Timber.e("GameFeedbackUtils: HTTP error ${result.code}: ${result.message}")
                    false
                }

                is ApiResult.NetworkError -> {
                    Timber.e(
                        result.exception,
                        "GameFeedbackUtils: Network error during submission",
                    )
                    false
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "GameFeedbackUtils: Failed to prepare game feedback: ${e.message}")
            false
        }
    }
}
