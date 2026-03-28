package app.gamegrub.data

import app.gamegrub.enums.PathType
import app.gamegrub.service.steam.SteamService
import java.io.File
import kotlinx.serialization.Serializable

@Serializable
data class SaveFilePattern(
    val root: PathType,
    val path: String,
    val pattern: String,
    val recursive: Int = 0,
) {
    val prefix: String
        get() = "%${root.name}%$path"
            .replace("{64BitSteamID}", SteamService.getSteamId64().toString())
            .replace("{Steam3AccountID}", SteamService.getSteam3AccountId().toString())

    val substitutedPath: String
        get() = path
            .replace("{64BitSteamID}", SteamService.getSteamId64().toString())
            .replace("{Steam3AccountID}", SteamService.getSteam3AccountId().toString())
            .replace("\\", File.separator)
}
