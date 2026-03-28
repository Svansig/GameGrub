package app.gamegrub.service.steam.managers

import android.content.Context
import app.gamegrub.statsgen.Achievement
import app.gamegrub.statsgen.StatsAchievementsGenerator
import app.gamegrub.utils.container.ContainerUtils
import app.gamegrub.utils.steam.SteamUtils
import com.winlator.xenvironment.ImageFs
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.steam.handlers.steamuserstats.SteamUserStats
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

class SteamAchievementManager(
    private val getService: () -> SteamServiceAccess?,
) {

    var cachedAchievements: List<Achievement>? = null
        private set
    var cachedAchievementsAppId: Int? = null
        private set

    fun clearCachedAchievements() {
        cachedAchievements = null
        cachedAchievementsAppId = null
    }

    suspend fun generateAchievements(appId: Int, configDirectory: String) {
        val service = getService() ?: return
        val steamUser = service.steamUser ?: return
        val userStats = service.steamUserStats ?: return

        val userStatsResult = userStats.getUserStats(appId, steamUser.steamID!!).await()
        val schemaArray = userStatsResult.schema.toByteArray()
        val generator = StatsAchievementsGenerator()
        val result = generator.generateStatsAchievements(schemaArray, configDirectory)

        cachedAchievements = result.achievements
        cachedAchievementsAppId = appId

        val nameToBlockBit = result.nameToBlockBit
        Timber.d("nameToBlockBit size=${nameToBlockBit.size} for appId=$appId")

        if (nameToBlockBit.isNotEmpty()) {
            val configDir = File(configDirectory)
            if (!configDir.exists()) configDir.mkdirs()

            val mappingJson = JSONObject()
            nameToBlockBit.forEach { (name, pair) ->
                mappingJson.put(name, JSONArray(listOf(pair.first, pair.second)))
            }
            File(configDir, "achievement_name_to_block.json").writeText(mappingJson.toString(), Charsets.UTF_8)
        }
    }

    fun getGseSaveDirs(context: Context, appId: Int): List<File> {
        val imageFs = ImageFs.find(context)
        val dirs = mutableListOf<File>()

        dirs.add(
            File(
                imageFs.rootDir,
                "${ImageFs.WINEPREFIX}/drive_c/users/xuser/AppData/Roaming/GSE Saves/$appId",
            ),
        )

        val accountId = getService()?.steamClient?.steamID?.accountID?.toInt()
            ?: app.gamegrub.PrefManager.steamUserAccountId.takeIf { it != 0 }

        if (accountId != null) {
            dirs.add(
                File(
                    imageFs.rootDir,
                    "${ImageFs.WINEPREFIX}/drive_c/Program Files (x86)/Steam/userdata/$accountId/$appId",
                ),
            )
        }

        return dirs
    }

    suspend fun syncAchievementsFromGoldberg(context: Context, appId: Int) = withContext(Dispatchers.IO) {
        val gseSaveDirs = getGseSaveDirs(context, appId).filter { it.isDirectory }
        if (gseSaveDirs.isEmpty()) {
            Timber.d("No GSE save directory found for appId=$appId")
            return@withContext
        }

        val unlockedNames = mutableSetOf<String>()
        var gseStatsDir: File? = null

        for (gseSaveDir in gseSaveDirs) {
            val goldbergAchFile = File(gseSaveDir, "achievements.json")
            if (goldbergAchFile.exists()) {
                try {
                    val json = JSONObject(goldbergAchFile.readText(Charsets.UTF_8))
                    for (name in json.keys()) {
                        val entry = json.optJSONObject(name) ?: continue
                        if (entry.optBoolean("earned", false)) {
                            unlockedNames.add(name)
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse Goldberg achievements.json for appId=$appId")
                }
            }

            val statsDir = File(gseSaveDir, "stats")
            if (gseStatsDir == null && statsDir.isDirectory && (statsDir.listFiles()?.isNotEmpty() == true)) {
                gseStatsDir = statsDir
            }
        }

        val hasStats = gseStatsDir != null
        if (unlockedNames.isEmpty() && !hasStats) {
            Timber.d("No earned achievements or stats found in Goldberg output for appId=$appId")
            return@withContext
        }

        val configDirectory = findSteamSettingsDir(context, appId)
        if (configDirectory == null) {
            Timber.w("Could not find steam_settings directory for appId=$appId")
            return@withContext
        }

        Timber.i("Found ${unlockedNames.size} earned achievements for appId=$appId, syncing to Steam")
        val result = storeAchievementUnlocks(appId, configDirectory, unlockedNames, gseStatsDir ?: gseSaveDirs.first().resolve("stats"))
        result.onSuccess {
            Timber.i("Successfully synced achievements and stats to Steam for appId=$appId")
        }.onFailure { e ->
            Timber.e(e, "Failed to sync achievements and stats to Steam for appId=$appId")
        }
    }

    private fun findSteamSettingsDir(context: Context, appId: Int): String? {
        val appDirPath = SteamUtils.getAppDirPath(appId)
        val appDirSettings = File(appDirPath, "steam_settings")
        if (File(appDirSettings, "achievement_name_to_block.json").exists()) {
            return appDirSettings.absolutePath
        }

        val container = ContainerUtils.getContainer(context, "STEAM_$appId")
        val coldclientSettings = File(
            container.rootDir,
            ".wine/drive_c/Program Files (x86)/Steam/steam_settings",
        )
        if (File(coldclientSettings, "achievement_name_to_block.json").exists()) {
            return coldclientSettings.absolutePath
        }

        return null
    }

    suspend fun storeAchievementUnlocks(
        appId: Int,
        configDirectory: String,
        unlockedNames: Set<String>,
        gseStatsDir: File,
    ): Result<Unit> = runCatching {
        val service = getService() ?: throw IllegalStateException("Service not available")
        val steamUser = service.steamUser ?: throw IllegalStateException("SteamUser not available")
        val userStats = service.steamUserStats ?: throw IllegalStateException("SteamUserStats not available")

        val userStatsResult = userStats.getUserStats(appId, steamUser.steamID!!).await()
        if (userStatsResult.result != EResult.OK) {
            throw IllegalStateException("getUserStats failed: ${userStatsResult.result}")
        }

        val allStats = mutableMapOf<Int, Int>()

        val mappingFile = File(configDirectory, "achievement_name_to_block.json")
        if (mappingFile.exists() && unlockedNames.isNotEmpty()) {
            val mappingJson = JSONObject(mappingFile.readText(Charsets.UTF_8))
            val nameToBlockBit = mutableMapOf<String, Pair<Int, Int>>()

            for (key in mappingJson.keys()) {
                val arr = mappingJson.optJSONArray(key) ?: continue
                if (arr.length() >= 2) {
                    nameToBlockBit[key] = Pair(arr.getInt(0), arr.getInt(1))
                }
            }

            for (block in userStats.achievementBlocks ?: emptyList()) {
                val blockId = (block.achievementId as? Number)?.toInt() ?: continue
                var bitmask = 0
                val unlockTimes = block.unlockTime ?: emptyList()
                for (i in unlockTimes.indices) {
                    val t = unlockTimes[i]
                    if (t > 0) bitmask = bitmask or (1 shl i)
                }
                allStats[blockId] = bitmask
            }

            for (name in unlockedNames) {
                val (blockId, bit) = nameToBlockBit[name] ?: continue
                val current = allStats[blockId] ?: 0
                allStats[blockId] = current or bit
            }

            userStats.storeStats(appId, allStats, emptyMap()).await()
        }

        if (gseStatsDir.isDirectory) {
            Timber.d("Processing GSE stats directory for appId=$appId")
        }
    }
}
