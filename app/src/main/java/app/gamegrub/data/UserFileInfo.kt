package app.gamegrub.data

import app.gamegrub.enums.PathType
import app.gamegrub.service.steam.SteamService
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.pathString
import kotlinx.serialization.Serializable

@Serializable
data class UserFileInfo(
    val root: PathType,
    val path: String,
    val filename: String,
    val timestamp: Long,
    val sha: ByteArray,
) {
    private fun substituteSteamTokens(value: String): String {
        val steamId64 = SteamService.getSteamId64()?.toString() ?: "0"
        val steam3AccountId = SteamService.getSteam3AccountId()?.toString() ?: "0"
        return value
            .replace("{64BitSteamID}", steamId64)
            .replace("{Steam3AccountID}", steam3AccountId)
    }

    // "." and blank path both mean "root of path type" per Steam manifest.
    val prefix: String
        get() {
            val pathForPrefix = when {
                path.isBlank() || path == "." -> ""
                else -> path
            }
            return substituteSteamTokens(Paths.get("%${root.name}%$pathForPrefix").pathString)
        }

    // Bare placeholder (%GameInstall%) expects no slash before filename; path with folder uses Paths.get.
    val prefixPath: String
        get() = when {
            path.isBlank() || path == "." -> "$prefix$filename"
            else -> Paths.get(prefix, filename).pathString
        }.let { substituteSteamTokens(it) }

    val substitutedPath: String
        get() = substituteSteamTokens(path).replace("\\", File.separator)

    fun getAbsPath(prefixToPath: (String) -> String): Path {
        return Paths.get(prefixToPath(root.toString()), substitutedPath, filename)
    }
}
