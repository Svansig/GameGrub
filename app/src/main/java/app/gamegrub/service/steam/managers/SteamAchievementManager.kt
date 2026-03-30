package app.gamegrub.service.steam.managers

import android.content.Context
import app.gamegrub.PrefManager
import app.gamegrub.service.steam.SteamService
import app.gamegrub.service.steam.di.SteamAppInfoClient
import app.gamegrub.service.steam.di.SteamConnection
import app.gamegrub.service.steam.di.SteamStatsClient
import app.gamegrub.service.steam.di.SteamUserClient
import app.gamegrub.statsgen.Achievement
import app.gamegrub.statsgen.StatsAchievementsGenerator
import app.gamegrub.utils.container.ContainerUtils
import com.winlator.xenvironment.ImageFs
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

@Singleton
class SteamAchievementManager @Inject constructor(
    private val userClient: SteamUserClient,
    private val statsClient: SteamStatsClient,
    private val appInfoClient: SteamAppInfoClient,
    private val connection: SteamConnection,
) {

    private val _cachedAchievements = MutableStateFlow<List<Achievement>?>(null)
    val cachedAchievements: StateFlow<List<Achievement>?> = _cachedAchievements.asStateFlow()

    private val _cachedAchievementsAppId = MutableStateFlow<Int?>(null)
    val cachedAchievementsAppId: StateFlow<Int?> = _cachedAchievementsAppId.asStateFlow()

    fun clearCachedAchievements() {
        _cachedAchievements.value = null
        _cachedAchievementsAppId.value = null
    }

    suspend fun generateAchievements(appId: Int, configDirectory: String) = withContext(Dispatchers.IO) {
        val steamId = connection.steamId ?: return@withContext

        try {
            val result = statsClient.getUserStats(appId, steamId)
            if (!result.success) {
                Timber.e("Failed to get user stats for $appId")
                return@withContext
            }

            val generator = StatsAchievementsGenerator()
            val genResult = generator.generateStatsAchievements(result.schema, configDirectory)

            _cachedAchievements.value = genResult.achievements
            _cachedAchievementsAppId.value = appId

            if (genResult.nameToBlockBit.isNotEmpty()) {
                val configDir = File(configDirectory)
                if (!configDir.exists()) configDir.mkdirs()
                val mappingJson = JSONObject()
                genResult.nameToBlockBit.forEach { (name, pair) ->
                    mappingJson.put(name, JSONArray(listOf(pair.first, pair.second)))
                }
                File(configDir, "achievement_name_to_block.json").writeText(mappingJson.toString(), Charsets.UTF_8)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to generate achievements for $appId")
        }
    }

    fun getGseSaveDirs(context: Context, appId: Int): List<File> {
        val imageFs = ImageFs.find(context)
        val dirs = mutableListOf<File>()
        dirs.add(File(imageFs.rootDir, "${ImageFs.WINEPREFIX}/drive_c/users/xuser/AppData/Roaming/GSE Saves/$appId"))
        val accountId = PrefManager.steamUserAccountId.takeIf { it != 0 }
        if (accountId != null) {
            dirs.add(File(imageFs.rootDir, "${ImageFs.WINEPREFIX}/drive_c/Program Files (x86)/Steam/userdata/$accountId/$appId"))
        }
        return dirs.filter { it.exists() }
    }

    suspend fun syncAchievementsFromGoldberg(context: Context, appId: Int) = withContext(Dispatchers.IO) {
        val gseSaveDirs = getGseSaveDirs(context, appId).filter { it.isDirectory }
        if (gseSaveDirs.isEmpty()) return@withContext

        val unlockedNames = mutableSetOf<String>()
        var gseStatsDir: File? = null

        for (gseSaveDir in gseSaveDirs) {
            val goldbergFile = File(gseSaveDir, "achievements.json")
            if (goldbergFile.exists()) {
                try {
                    val json = JSONObject(goldbergFile.readText())
                    for (name in json.keys()) {
                        val entry = json.optJSONObject(name) ?: continue
                        if (entry.optBoolean("earned", false)) unlockedNames.add(name)
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse Goldberg achievements for $appId")
                }
            }
            val statsDir = File(gseSaveDir, "stats")
            if (gseStatsDir == null && statsDir.isDirectory) gseStatsDir = statsDir
        }

        if (unlockedNames.isEmpty() && gseStatsDir == null) return@withContext

        val configDir = findSteamSettingsDir(context, appId) ?: return@withContext
        storeAchievementUnlocks(appId, configDir, unlockedNames, gseStatsDir ?: gseSaveDirs.first().resolve("stats"))
    }

    private fun findSteamSettingsDir(context: Context, appId: Int): String? {
        val appDir: File = File(SteamService.getAppDirPath(appId), "steam_settings")
        val appMapping: File = File(appDir, "achievement_name_to_block.json")
        if (appMapping.isFile) {
            return appDir.path
        }
        val container = ContainerUtils.getContainer(context, "STEAM_$appId")
        val coldClient: File = File(container.rootDir, ".wine/drive_c/Program Files (x86)/Steam/steam_settings")
        if (File(coldClient, "achievement_name_to_block.json").isFile) {
            return coldClient.path
        }
        return null
    }

    suspend fun storeAchievementUnlocks(
        appId: Int,
        configDirectory: String,
        unlockedNames: Set<String>,
        gseStatsDir: File,
    ): Result<Unit> = runCatching {
        val steamId = connection.steamId ?: throw IllegalStateException("Not logged in")

        val result = statsClient.getUserStats(appId, steamId)
        if (!result.success) throw IllegalStateException("Failed to get user stats")

        val allStats = mutableMapOf<Int, Int>()
        val mappingFile = File(configDirectory, "achievement_name_to_block.json")
        if (mappingFile.exists() && unlockedNames.isNotEmpty()) {
            val mappingJson = JSONObject(mappingFile.readText())
            val nameToBlockBit = mutableMapOf<String, Pair<Int, Int>>()
            for (key in mappingJson.keys()) {
                val arr = mappingJson.optJSONArray(key) ?: continue
                if (arr.length() >= 2) nameToBlockBit[key] = Pair(arr.getInt(0), arr.getInt(1))
            }
            for (block in result.achievementBlocks) {
                var bitmask = 0
                for (i in block.unlockTimes.indices) {
                    if (block.unlockTimes[i] > 0) bitmask = bitmask or (1 shl i)
                }
                allStats[block.achievementId] = bitmask
            }
            for (name in unlockedNames) {
                val (blockId, bit) = nameToBlockBit[name] ?: continue
                allStats[blockId] = (allStats[blockId] ?: 0) or bit
            }
        }
        statsClient.storeStats(appId, allStats, emptyMap())
    }
}
