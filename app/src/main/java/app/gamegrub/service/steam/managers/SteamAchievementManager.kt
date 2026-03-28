package app.gamegrub.service.steam.managers

import android.content.Context
import app.gamegrub.service.steam.di.SteamUserClient
import app.gamegrub.statsgen.Achievement
import app.gamegrub.statsgen.StatsAchievementsGenerator
import app.gamegrub.utils.steam.SteamUtils
import com.winlator.xenvironment.ImageFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SteamAchievementManager @Inject constructor(
    private val userClient: SteamUserClient,
) {
    private val _cachedAchievements = MutableStateFlow<List<Achievement>?>(null)
    val cachedAchievements: StateFlow<List<Achievement>?> = _cachedAchievements.asStateFlow()

    private val _cachedAchievementsAppId = MutableStateFlow<Int?>(null)
    val cachedAchievementsAppId: StateFlow<Int?> = _cachedAchievementsAppId.asStateFlow()

    fun clearCachedAchievements() {
        _cachedAchievements.value = null
        _cachedAchievementsAppId.value = null
    }

    suspend fun generateAchievements(appId: Int, configDirectory: String) {
        withContext(Dispatchers.IO) {
            try {
                Timber.d("Generating achievements for app $appId")
            } catch (e: Exception) {
                Timber.e(e, "Failed to generate achievements for $appId")
            }
        }
    }

    fun getGseSaveDirs(context: Context, appId: Int): List<File> {
        val imageFs = ImageFs.find(context)
        return listOf(
            File(imageFs.rootDir, "${ImageFs.WINEPREFIX}/drive_c/users/xuser/AppData/Roaming/GSE Saves/$appId"),
        ).filter { it.exists() }
    }
}
