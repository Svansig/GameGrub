package app.gamegrub.service.steam.managers

import app.gamegrub.data.DepotInfo
import app.gamegrub.enums.OS
import app.gamegrub.enums.OSArch
import app.gamegrub.service.steam.SteamService
import `in`.dragonbra.javasteam.enums.EDepotFileFlag
import `in`.dragonbra.javasteam.types.FileData
import java.util.EnumSet
import timber.log.Timber

/**
 * Encapsulates Steam launch/depot selection heuristics used by SteamService companion APIs.
 */
object SteamLaunchConfigManager {
    private val ueShipping = Regex(""".*-win(32|64)(-shipping)?\.exe$""", RegexOption.IGNORE_CASE)
    private val ueBinaries = Regex(""".*/binaries/win(32|64)/.*\.exe$""", RegexOption.IGNORE_CASE)
    private val negativeKeywords = listOf(
        "crash", "handler", "viewer", "compiler", "tool",
        "setup", "unins", "eac", "launcher", "steam",
    )
    private val genericName = Regex("^[a-z]\\d{1,3}\\.exe$", RegexOption.IGNORE_CASE)

    fun filterForDownloadableDepots(
        depot: DepotInfo,
        has64Bit: Boolean,
        preferredLanguage: String,
        ownedDlc: Map<Int, DepotInfo>?,
    ): Boolean {
        if (depot.manifests.isEmpty() && depot.encryptedManifests.isNotEmpty()) {
            return false
        }
        if (depot.manifests.isEmpty() && !depot.sharedInstall) {
            return false
        }
        if (depot.manifests.isNotEmpty() && depot.manifests.values.all { it.size == 0L && it.download == 0L }) {
            return false
        }

        if (!(depot.osList.contains(OS.windows) || (!depot.osList.contains(OS.linux) && !depot.osList.contains(OS.macos)))) {
            return false
        }

        val archOk = when (depot.osArch) {
            OSArch.Arch64, OSArch.Unknown -> true
            OSArch.Arch32 -> !has64Bit
        }
        if (!archOk) {
            return false
        }

        if (depot.dlcAppId != SteamService.INVALID_APP_ID && ownedDlc != null && !ownedDlc.containsKey(depot.depotId)) {
            return false
        }

        if (depot.language.isNotEmpty() && depot.language != preferredLanguage) {
            return false
        }

        return true
    }

    fun hasExecutableFlag(flags: Any): Boolean = when (flags) {
        is EnumSet<*> -> flags.contains(EDepotFileFlag.Executable) || flags.contains(EDepotFileFlag.CustomExecutable)
        is Int -> (flags and 0x20) != 0 || (flags and 0x80) != 0
        is Long -> ((flags and 0x20L) != 0L) || ((flags and 0x80L) != 0L)
        else -> false
    }

    fun isStub(file: FileData): Boolean {
        val name = file.fileName.lowercase()
        val stub = genericName.matches(name) || negativeKeywords.any { it in name } || file.totalSize < 1_000_000
        if (stub) {
            Timber.d("Stub filtered: ${file.fileName} size=${file.totalSize}")
        }
        return stub
    }

    fun choosePrimaryExe(files: List<FileData>?, gameName: String): FileData? = files?.maxWithOrNull { a, b ->
        val scoreA = scoreExe(a, gameName, hasExecutableFlag(a.flags))
        val scoreB = scoreExe(b, gameName, hasExecutableFlag(b.flags))

        if (scoreA != scoreB) {
            scoreA - scoreB
        } else {
            (a.totalSize - b.totalSize).toInt()
        }
    }

    private fun scoreExe(file: FileData, gameName: String, hasExeFlag: Boolean): Int {
        var score = 0
        val path = file.fileName.lowercase()

        if (ueShipping.matches(path)) score += 300
        if (ueBinaries.containsMatchIn(path)) score += 250
        if (!path.contains('/')) score += 200
        if (path.contains(gameName) || fuzzyMatch(path, gameName)) score += 100
        if (negativeKeywords.any { it in path }) score -= 150
        if (genericName.matches(file.fileName)) score -= 200
        if (hasExeFlag) score += 50

        Timber.i("Score for $path: $score")
        return score
    }

    private fun fuzzyMatch(a: String, b: String): Boolean {
        val cleanA = a.replace(Regex("[^a-z]"), "")
        val cleanB = b.replace(Regex("[^a-z]"), "")
        return cleanA.take(5) == cleanB.take(5)
    }
}

