package app.gamegrub.utils.network

import android.content.Context
import app.gamegrub.BuildConfig
import app.gamegrub.Constants
import app.gamegrub.network.NetworkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber

@Serializable
data class UpdateInfo(
    val updateAvailable: Boolean,
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val releaseNotes: String? = null,
)

object UpdateChecker {
    private val httpClient = NetworkManager.createHttpClient(
        connectTimeoutSeconds = 10,
        readTimeoutSeconds = 10,
        writeTimeoutSeconds = 10,
        callTimeoutMinutes = 1,
    )

    suspend fun checkForUpdate(context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val url = "${Constants.Misc.UPDATE_CHECK_URL}?versionCode=${BuildConfig.VERSION_CODE}&versionName=${BuildConfig.VERSION_NAME}"
            val request = NetworkManager.buildGetRequest(url)

            val response = httpClient.newCall(request).execute()

            if (response.isSuccessful) {
                val body = response.body.string()
                val updateInfo = Json.decodeFromString<UpdateInfo>(body)
                Timber.i("Update check: updateAvailable=${updateInfo.updateAvailable}, versionCode=${updateInfo.versionCode}")
                return@withContext updateInfo
            } else {
                Timber.w("Update check failed: HTTP ${response.code}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error checking for updates")
        }
        return@withContext null
    }
}
