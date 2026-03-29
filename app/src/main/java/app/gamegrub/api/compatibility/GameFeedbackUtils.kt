package app.gamegrub.api.compatibility

import android.content.Context
import android.os.Build
import app.gamegrub.BuildConfig
import app.gamegrub.api.ApiResult
import app.gamegrub.data.GameSource
import app.gamegrub.service.amazon.AmazonService
import app.gamegrub.service.epic.EpicService
import app.gamegrub.service.gog.GOGService
import app.gamegrub.service.steam.SteamService
import app.gamegrub.utils.container.ContainerUtils
import app.gamegrub.utils.device.HardwareUtils
import app.gamegrub.utils.game.CustomGameScanner
import com.winlator.core.GPUInformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber

object GameFeedbackUtils {

    suspend fun submitGameFeedback(
        context: Context,
        appId: String,
        rating: Int,
        tags: List<String>,
        notes: String?,
    ): Boolean = withContext(Dispatchers.IO) {
        Timber.Forest.d("GameFeedbackUtils: Starting submitGameFeedback method with rating=$rating")
        try {
            val gameSource = ContainerUtils.extractGameSourceFromContainerId(appId)
            val gameId = ContainerUtils.extractGameIdFromContainerId(appId)
            val container = ContainerUtils.getContainer(context, appId)
            val configJson = JSONObject(container.containerJson)
            Timber.Forest.d("config string is: " + container.containerJson)
            Timber.Forest.d("configJson: $configJson")

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
                    val productId = AmazonService.Companion.getProductIdByAppId(gameId)
                    if (!productId.isNullOrBlank()) {
                        AmazonService.Companion.getAmazonGameOf(productId)?.title ?: ""
                    } else {
                        ""
                    }
                }

                GameSource.EPIC -> {
                    val game = EpicService.Companion.getEpicGameOf(gameId)
                    game?.title ?: ""
                }

                GameSource.GOG -> {
                    val game = GOGService.Companion.getGOGGameOf(gameId.toString())
                    game?.title ?: ""
                }

                GameSource.STEAM -> {
                    val appInfo = SteamService.Companion.getAppInfoOf(gameId)
                    appInfo?.name ?: ""
                }
            }

            if (gameName.isEmpty()) {
                Timber.Forest.e("SubmitGameFeedback: Could not get game name for appId $appId")
                return@withContext false
            }
            Timber.Forest.d("GameFeedbackUtils: Game name: $gameName")

            // Get GPU info if available
            val gpu = try {
                Timber.Forest.d("GameFeedbackUtils: About to get GPU info")
                val gpuInfo = GPUInformation.getRenderer(context)
                Timber.Forest.d("GameFeedbackUtils: GPU info: $gpuInfo")
                gpuInfo
            } catch (e: Exception) {
                Timber.Forest.e(e, "GameFeedbackUtils: Failed to get GPU info: ${e.message}")
                "Unknown GPU"
            }

            // Get SOC identifier
            val soc = try {
                val socName = HardwareUtils.getSOCName()
                Timber.Forest.d("GameFeedbackUtils: SOC info: $socName")
                socName
            } catch (e: Exception) {
                Timber.Forest.e(e, "GameFeedbackUtils: Failed to get SOC info: ${e.message}")
                null
            }

            // Get Android version
            val androidVer = Build.VERSION.RELEASE
            Timber.Forest.d("GameFeedbackUtils: Android version: $androidVer")

            // Get app version
            val appVersion = BuildConfig.VERSION_NAME
            Timber.Forest.d("GameFeedbackUtils: App version: $appVersion")

            // Retrieve session data from container metadata
            val avgFpsStr = container.getSessionMetadata("avg_fps", "")
            val sessionLengthSecStr = container.getSessionMetadata("session_length_sec", "")
            val avgFps = if (avgFpsStr.isNotEmpty()) {
                try {
                    avgFpsStr.toFloat()
                } catch (e: NumberFormatException) {
                    Timber.Forest.w("GameFeedbackUtils: Failed to parse avg_fps: $avgFpsStr")
                    null
                }
            } else {
                null
            }
            val sessionLengthSec = if (sessionLengthSecStr.isNotEmpty()) {
                try {
                    sessionLengthSecStr.toInt()
                } catch (e: NumberFormatException) {
                    Timber.Forest.w("GameFeedbackUtils: Failed to parse session_length_sec: $sessionLengthSecStr")
                    null
                }
            } else {
                null
            }

            Timber.Forest.i("GameFeedbackUtils: Submitting game feedback: game=$gameName, device=${Build.MODEL}, rating=$rating, tags=${tags.joinToString()}, avgFps=$avgFps, sessionLengthSec=$sessionLengthSec")

            val result = GameRunApi.submit(
                gameName = gameName,
                deviceModel = Build.MODEL,
                deviceManufacturer = Build.MANUFACTURER,
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
                    Timber.Forest.i("GameFeedbackUtils: Game feedback submitted successfully (id=${result.data.id})")
                    true
                }

                is ApiResult.HttpError -> {
                    Timber.Forest.e("GameFeedbackUtils: HTTP error ${result.code}: ${result.message}")
                    false
                }

                is ApiResult.NetworkError -> {
                    Timber.Forest.e(
                        result.exception,
                        "GameFeedbackUtils: Network error during submission",
                    )
                    false
                }
            }
        } catch (e: Exception) {
            Timber.Forest.e(e, "GameFeedbackUtils: Failed to prepare game feedback: ${e.message}")
            false
        }
    }
}
