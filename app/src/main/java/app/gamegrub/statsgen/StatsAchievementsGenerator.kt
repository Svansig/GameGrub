package app.gamegrub.statsgen

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File

/**
 * Parses Steam stat/achievement schema data and writes normalized JSON config files.
 *
 * The generator preserves legacy output structure and key ordering for compatibility with
 * existing config consumers.
 */
class StatsAchievementsGenerator {
    private val vdfParser = VdfParser()
    private val jsonWriter = Json {
        prettyPrint = true
        explicitNulls = false
    }

    /**
     * Generates in-memory stats/achievements and writes `achievements.json` / `stats.json`.
     *
     * Compatibility notes:
     * - `achievements.json` is always written (empty array when no achievements exist).
     * - `stats.json` is only written when at least one stat exists.
     * - Stat normalization matches legacy behavior and throws when no numeric fallback exists.
     */
    fun generateStatsAchievements(schema: ByteArray, configDirectory: String): ProcessingResult {
        val parsedData = parseSchema(schema)
        val outputData = buildOutputData(parsedData.achievements, parsedData.stats)
        val configDir = createConfigDirectory(configDirectory)

        writeAchievementsJson(configDir, outputData.achievements)
        writeStatsJson(configDir, outputData.stats)

        return ProcessingResult(
            achievements = parsedData.achievements,
            stats = parsedData.stats,
            copyDefaultUnlockedImg = outputData.copyDefaultUnlockedImg,
            copyDefaultLockedImg = outputData.copyDefaultLockedImg,
            nameToBlockBit = parsedData.nameToBlockBit,
        )
    }

    private fun parseSchema(schema: ByteArray): ParsedSchemaData {
        val parsedSchema = vdfParser.binaryLoads(schema)
        val achievementsOut = mutableListOf<Achievement>()
        val statsOut = mutableListOf<Stat>()
        val nameToBlockBit = mutableMapOf<String, Pair<Int, Int>>()

        for (appData in parsedSchema.values) {
            val schemaMap = appData.asStringAnyMap() ?: continue
            val statInfo = schemaMap["stats"].asStringAnyMap() ?: continue

            for ((statKey, statData) in statInfo) {
                val statMap = statData.asStringAnyMap() ?: continue
                val statType = statMap["type"]?.toString() ?: continue

                if (isAchievementStatType(statType)) {
                    achievementsOut += parseAchievements(statKey, statMap, nameToBlockBit)
                } else {
                    statsOut += parseStat(statKey, statMap, statType)
                }
            }
        }

        return ParsedSchemaData(
            achievements = achievementsOut,
            stats = statsOut,
            nameToBlockBit = nameToBlockBit,
        )
    }

    private fun parseAchievements(
        statKey: String,
        stat: Map<String, Any?>,
        nameToBlockBit: MutableMap<String, Pair<Int, Int>>,
    ): List<Achievement> {
        val bits = stat["bits"].asStringAnyMap() ?: return emptyList()
        return bits.mapNotNull { (achievementIndex, achievementData) ->
            val achievementMap = achievementData.asStringAnyMap() ?: return@mapNotNull null
            val displayMap = achievementMap["display"].asStringAnyMap() ?: emptyMap()
            val parsedDisplay = parseDisplay(displayMap)

            val achievementName = achievementMap["name"]?.toString().orEmpty()
            rememberAchievementBitIndex(statKey, achievementIndex, achievementName, nameToBlockBit)

            Achievement(
                name = achievementName,
                displayName = parsedDisplay.displayName,
                description = parsedDisplay.description,
                hidden = parsedDisplay.hidden,
                icon = parsedDisplay.icon,
                iconGray = parsedDisplay.iconGray,
                icongray = parsedDisplay.icongray,
                progress = achievementMap["progress"].asStringAnyMapWithoutNullValues(),
            )
        }
    }

    private fun parseDisplay(display: Map<String, Any?>): ParsedDisplay {
        var displayName: Map<String, String>? = null
        var description: Map<String, String>? = null
        var hidden = 0

        for ((key, value) in display) {
            when (key.lowercase()) {
                "name" -> displayName = toLocalizedText(value)
                "desc" -> description = toLocalizedText(value)
                "hidden" -> hidden = toHiddenInt(value)
            }
        }

        return ParsedDisplay(
            displayName = displayName,
            description = description,
            hidden = hidden,
            icon = display["icon"]?.toString(),
            iconGray = display["icon_gray"]?.toString(),
            icongray = display["icongray"]?.toString(),
        )
    }

    private fun parseStat(statKey: String, stat: Map<String, Any?>, statType: String): Stat {
        val normalizedType = normalizeStatType(statType)
        val defaultValue = when {
            stat.containsKey("Default") -> stat["Default"]
            stat.containsKey("default") -> stat["default"]
            else -> "0"
        }

        return Stat(
            id = statKey,
            name = stat["name"]?.toString().orEmpty(),
            type = normalizedType,
            default = defaultValue?.toString() ?: "0",
            global = "0",
            min = stat["min"]?.toString(),
        )
    }

    private fun buildOutputData(achievements: List<Achievement>, stats: List<Stat>): OutputData {
        var copyDefaultUnlockedImg = false
        var copyDefaultLockedImg = false

        val outputAchievements = achievements.map { achievement ->
            val iconPath = if (!achievement.icon.isNullOrEmpty()) {
                "img/${achievement.icon}"
            } else {
                copyDefaultUnlockedImg = true
                "img/steam_default_icon_unlocked.jpg"
            }

            val iconGrayPath = if (!achievement.iconGray.isNullOrEmpty()) {
                "img/${achievement.iconGray}"
            } else {
                copyDefaultLockedImg = true
                "img/steam_default_icon_locked.jpg"
            }

            OutputAchievement(
                hidden = achievement.hidden,
                displayName = achievement.displayName ?: emptyMap(),
                description = achievement.description ?: emptyMap(),
                icon = iconPath,
                iconGray = iconGrayPath,
                name = achievement.name,
                unlocked = achievement.unlocked,
                unlockTimestamp = achievement.unlockTimestamp,
                formattedUnlockTime = achievement.formattedUnlockTime,
            )
        }

        val outputStats = stats.map { stat ->
            val (defaultNum, globalNum) = normalizeNumericDefaults(stat)
            OutputStat(
                id = stat.id,
                default = defaultNum,
                global = globalNum,
                name = stat.name,
                type = stat.type,
            )
        }

        return OutputData(
            achievements = outputAchievements,
            stats = outputStats,
            copyDefaultUnlockedImg = copyDefaultUnlockedImg,
            copyDefaultLockedImg = copyDefaultLockedImg,
        )
    }

    /**
     * Normalizes stat numeric defaults to the same shape expected by existing stats.json readers.
     */
    private fun normalizeNumericDefaults(stat: Stat): Pair<String, String> {
        if (stat.type.lowercase() == "int") {
            val intValues = runCatching {
                stat.default.toInt().toString() to stat.global.toInt().toString()
            }.getOrNull()
            if (intValues != null) {
                return intValues
            }

            val floatValues = runCatching {
                stat.default.toFloat().toInt().toString() to stat.global.toFloat().toInt().toString()
            }.getOrNull()
            if (floatValues != null) {
                return floatValues
            }

            val minValue = stat.min?.toIntOrNull()
            if (minValue != null) {
                return minValue.toString() to "0"
            }

            throw IllegalArgumentException("min not exist in stat and no way to get the data. please report with the appid")
        }

        return stat.default.toFloat().toString() to stat.global.toFloat().toString()
    }

    private fun createConfigDirectory(configDirectory: String): File {
        val configDir = File(configDirectory)
        if (!configDir.exists()) {
            configDir.mkdirs()
        }
        return configDir
    }

    private fun writeAchievementsJson(configDir: File, achievements: List<OutputAchievement>) {
        val achievementsFile = File(configDir, "achievements.json")
        if (achievementsFile.exists()) {
            achievementsFile.delete()
        }

        val achievementsJsonContent = jsonWriter.encodeToString(buildAchievementsJson(achievements))

        achievementsFile.writeText(achievementsJsonContent, Charsets.UTF_8)
    }

    private fun buildAchievementsJson(achievements: List<OutputAchievement>): JsonArray {
        return buildJsonArray {
            for (achievement in achievements) {
                add(
                    buildJsonObject {
                        put("hidden", achievement.hidden)
                        put("displayName", achievement.displayName.toJsonObject())
                        put("description", achievement.description.toJsonObject())
                        put("icon", achievement.icon)
                        put("icon_gray", achievement.iconGray)
                        put("name", achievement.name)

                        achievement.unlocked?.let { put("unlocked", it) }
                        achievement.unlockTimestamp?.let { put("unlockTimestamp", it) }
                        achievement.formattedUnlockTime?.let { put("formattedUnlockTime", it) }
                    },
                )
            }
        }
    }

    private fun writeStatsJson(configDir: File, stats: List<OutputStat>) {
        if (stats.isEmpty()) {
            return
        }

        val statsFile = File(configDir, "stats.json")
        if (statsFile.exists()) {
            statsFile.delete()
        }

        val statsJsonContent = jsonWriter.encodeToString(buildStatsJson(stats))

        statsFile.writeText(statsJsonContent, Charsets.UTF_8)
    }

    private fun buildStatsJson(stats: List<OutputStat>): JsonArray {
        return buildJsonArray {
            for (stat in stats) {
                add(
                    buildJsonObject {
                        put("id", stat.id)
                        put("default", stat.default)
                        put("global", stat.global)
                        put("name", stat.name)
                        put("type", stat.type)
                    },
                )
            }
        }
    }

    private fun Map<String, String>.toJsonObject(): JsonObject {
        return buildJsonObject {
            for ((key, value) in this@toJsonObject) {
                put(key, JsonPrimitive(value))
            }
        }
    }

    private fun isAchievementStatType(statType: String): Boolean {
        return statType == StatType.STAT_TYPE_BITS || statType == StatType.ACHIEVEMENTS
    }

    private fun normalizeStatType(statType: String): String {
        return when (statType) {
            StatType.INT, StatType.STAT_TYPE_INT -> "int"
            StatType.FLOAT, StatType.STAT_TYPE_FLOAT -> "float"
            StatType.AVGRATE, StatType.STAT_TYPE_AVGRATE -> "avgrate"
            else -> "int"
        }
    }

    private fun rememberAchievementBitIndex(
        statKey: String,
        achievementIndex: String,
        achievementName: String,
        nameToBlockBit: MutableMap<String, Pair<Int, Int>>,
    ) {
        if (achievementName.isEmpty()) {
            return
        }

        val blockId = statKey.toIntOrNull() ?: return
        val bitIndex = achievementIndex.toIntOrNull() ?: return
        nameToBlockBit[achievementName] = blockId to bitIndex
    }

    private fun toLocalizedText(value: Any?): Map<String, String> {
        val valueMap = value.asStringAnyMap()
        if (valueMap != null) {
            return valueMap.mapValues { (_, text) -> text.toString() }
        }
        return mapOf("english" to value.toString())
    }

    private fun toHiddenInt(value: Any?): Int {
        return when (value) {
            is Number -> value.toInt()
            else -> value?.toString()?.toIntOrNull() ?: 0
        }
    }

    private fun Any?.asStringAnyMap(): Map<String, Any?>? {
        val rawMap = this as? Map<*, *> ?: return null
        return rawMap.entries.associate { (key, value) -> key.toString() to value }
    }

    private fun Any?.asStringAnyMapWithoutNullValues(): Map<String, Any>? {
        val rawMap = this as? Map<*, *> ?: return null
        return rawMap.entries
            .filter { (_, value) -> value != null }
            .associate { (key, value) -> key.toString() to (value as Any) }
    }

    private data class ParsedSchemaData(
        val achievements: List<Achievement>,
        val stats: List<Stat>,
        val nameToBlockBit: Map<String, Pair<Int, Int>>,
    )

    private data class ParsedDisplay(
        val displayName: Map<String, String>?,
        val description: Map<String, String>?,
        val hidden: Int,
        val icon: String?,
        val iconGray: String?,
        val icongray: String?,
    )

    private data class OutputAchievement(
        val hidden: Int,
        val displayName: Map<String, String>,
        val description: Map<String, String>,
        val icon: String,
        val iconGray: String,
        val name: String,
        val unlocked: Boolean?,
        val unlockTimestamp: Int?,
        val formattedUnlockTime: String?,
    )

    private data class OutputStat(
        val id: String,
        val default: String,
        val global: String,
        val name: String,
        val type: String,
    )

    private data class OutputData(
        val achievements: List<OutputAchievement>,
        val stats: List<OutputStat>,
        val copyDefaultUnlockedImg: Boolean,
        val copyDefaultLockedImg: Boolean,
    )
}
