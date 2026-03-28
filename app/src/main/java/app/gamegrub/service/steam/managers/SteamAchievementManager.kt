package app.gamegrub.service.steam.managers

import android.content.Context
import app.gamegrub.PrefManager
import app.gamegrub.statsgen.Achievement
import app.gamegrub.statsgen.StatsAchievementsGenerator
import app.gamegrub.utils.container.ContainerUtils
import app.gamegrub.utils.steam.SteamUtils
import com.winlator.xenvironment.ImageFs
import `in`.dragonbra.javasteam.enums.EResult
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
class SteamAchievementManager @Inject constructor() {

    private val _cachedAchievements = MutableStateFlow<List<Achievement>?>(null)
    val cachedAchievements: StateFlow<List<Achievement>?> = _cachedAchievements.asStateFlow()

    private val _cachedAchievementsAppId = MutableStateFlow<Int?>(null)
    val cachedAchievementsAppId: StateFlow<Int?> = _cachedAchievementsAppId.asStateFlow()

    fun clearCachedAchievements() {
        _cachedAchievements.value = null
        _cachedAchievementsAppId.value = null
    }

    suspend fun generateAchievements(appId: Int, configDirectory: String) {
        val service = SteamService.instance ?: return
        val steamUser = service._steamUser ?: return
        val userStats = service._steamUserStats ?: return

        val result = userStats.getUserStats(appId, steamUser.steamID!!).await()
        val schemaArray = result.schema.toByteArray()
        val generator = StatsAchievementsGenerator()
        val genResult = generator.generateStatsAchievements(schemaArray, configDirectory)

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
    }

    fun getGseSaveDirs(context: Context, appId: Int): List<File> {
        val imageFs = ImageFs.find(context)
        val dirs = mutableListOf<File>()
        dirs.add(File(imageFs.rootDir, "${ImageFs.WINEPREFIX}/drive_c/users/xuser/AppData/Roaming/GSE Saves/$appId"))
        val accountId = PrefManager.steamUserAccountId.takeIf { it != 0 }
        if (accountId != null) {
            dirs.add(File(imageFs.rootDir, "${ImageFs.WINEPREFIX}/drive_c/Program Files (x86)/Steam/userdata/$accountId/$appId"))
        }
        return dirs
    }

    suspend fun syncAchievementsFromGoldberg(context: Context, appId: Int) = withContext(Dispatchers.IO) {
        val gseSaveDirs = getGseSaveDirs(context, appId).filter { it.isDirectory }
        if (gseSaveDirs.isEmpty()) {
            Timber.d("No GSE save directory for appId=$appId")
            return@withContext
        }

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
        val appDir = File(SteamUtils.getAppDirPath(appId), "steam_settings")
        if (File(appDir, "achievement_name_to_block.json").exists()) return appDir.absolutePath
        val container = ContainerUtils.getContainer(context, "STEAM_$appId")
        val coldClient = File(container.rootDir, ".wine/drive_c/Program Files (x86)/Steam/steam_settings")
        if (File(coldClient, "achievement_name_to_block.json").exists()) return coldClient.absolutePath
        return null
    }

    suspend fun storeAchievementUnlocks(
        appId: Int,
        configDirectory: String,
        unlockedNames: Set<String>,
        gseStatsDir: File,
    ): Result<Unit> = runCatching {
        val service = SteamService.instance ?: throw IllegalStateException("Service not available")
        val steamUser = service._steamUser ?: throw IllegalStateException("SteamUser not available")
        val userStats = service._steamUserStats ?: throw IllegalStateException("SteamUserStats not available")

        val result = userStats.getUserStats(appId, steamUser.steamID!!).await()
        if (result.result != EResult.OK) throw IllegalStateException("getUserStats failed: ${result.result}")

        val allStats = mutableMapOf<Int, Int>()
        val mappingFile = File(configDirectory, "achievement_name_to_block.json")
        if (mappingFile.exists() && unlockedNames.isNotEmpty()) {
            val mappingJson = JSONObject(mappingFile.readText())
            val nameToBlockBit = mutableMapOf<String, Pair<Int, Int>>()
            for (key in mappingJson.keys()) {
                val arr = mappingJson.optJSONArray(key) ?: continue
                if (arr.length() >= 2) nameToBlockBit[key] = Pair(arr.getInt(0), arr.getInt(1))
            }
            for (block in result.achievementBlocks ?: emptyList()) {
                val blockId = (block.achievementId as? Number)?.toInt() ?: continue
                var bitmask = 0
                for (i in (block.unlockTime ?: emptyList()).indices) {
                    if ((block.unlockTime ?: emptyList())[i] > 0) bitmask = bitmask or (1 shl i)
                }
                allStats[blockId] = bitmask
            }
            for (name in unlockedNames) {
                val (blockId, bit) = nameToBlockBit[name] ?: continue
                allStats[blockId] = (allStats[blockId] ?: 0) or bit
            }
        }
        userStats.storeStats(appId, allStats, emptyMap()).await()
    }
}
