package app.gamegrub.utils.steam

import android.content.Context
import app.gamegrub.PrefManager
import app.gamegrub.enums.SpecialGameSaveMapping
import app.gamegrub.service.steam.SteamService
import java.io.File
import java.nio.file.Files
import timber.log.Timber

object SteamSaveLocationManager {

    fun ensureSaveLocationsForGames(context: Context, steamAppId: Int) {
        val mapping = SpecialGameSaveMapping.registry.find { it.appId == steamAppId } ?: return

        try {
            val service = SteamService.instance
            val accountId = service?.userManager?.getSteam3AccountId()
                ?: PrefManager.steamUserAccountId.toLong().takeIf { it > 0L }
                ?: 0L
            val steamId64 = service?.userManager?.getSteamId64()?.toString() ?: "0"
            val steam3AccountId = accountId.toString()

            val basePath = mapping.pathType.toAbsPath(context, steamAppId, accountId)

            val sourceRelativePath = service?.userManager?.substituteSteamIdTokens(mapping.sourceRelativePath)
                ?: mapping.sourceRelativePath
                .replace("{64BitSteamID}", steamId64)
                .replace("{Steam3AccountID}", steam3AccountId)
            val targetRelativePath = service?.userManager?.substituteSteamIdTokens(mapping.targetRelativePath)
                ?: mapping.targetRelativePath
                .replace("{64BitSteamID}", steamId64)
                .replace("{Steam3AccountID}", steam3AccountId)

            val sourcePath = File(basePath, sourceRelativePath)
            val targetPath = File(basePath, targetRelativePath)

            if (!sourcePath.exists()) {
                Timber.i("[${mapping.description}] Source save folder does not exist yet: ${sourcePath.absolutePath}")
                return
            }

            if (targetPath.exists()) {
                if (Files.isSymbolicLink(targetPath.toPath())) {
                    Timber.i("[${mapping.description}] Symlink already exists: ${targetPath.absolutePath}")
                    return
                } else {
                    Timber.w("[${mapping.description}] Target path exists but is not a symlink: ${targetPath.absolutePath}")
                    return
                }
            }

            targetPath.parentFile?.mkdirs()
            Files.createSymbolicLink(targetPath.toPath(), sourcePath.toPath())
            Timber.i("[${mapping.description}] Created symlink: ${targetPath.absolutePath} -> ${sourcePath.absolutePath}")
        } catch (e: Exception) {
            Timber.e(e, "[${mapping.description}] Failed to create save location symlink")
        }
    }
}

