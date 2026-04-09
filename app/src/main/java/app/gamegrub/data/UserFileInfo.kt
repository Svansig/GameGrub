package app.gamegrub.data

import app.gamegrub.enums.PathType
import app.gamegrub.service.steam.SteamService
import kotlinx.serialization.Serializable
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.pathString

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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserFileInfo

        if (timestamp != other.timestamp) return false
        if (root != other.root) return false
        if (path != other.path) return false
        if (filename != other.filename) return false
        if (!sha.contentEquals(other.sha)) return false
        if (prefix != other.prefix) return false
        if (prefixPath != other.prefixPath) return false
        if (substitutedPath != other.substitutedPath) return false

        return true
    }

    override fun hashCode(): Int {
        var result = timestamp.hashCode()
        result = 31 * result + root.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + filename.hashCode()
        result = 31 * result + sha.contentHashCode()
        result = 31 * result + prefix.hashCode()
        result = 31 * result + prefixPath.hashCode()
        result = 31 * result + substitutedPath.hashCode()
        return result
    }
}
