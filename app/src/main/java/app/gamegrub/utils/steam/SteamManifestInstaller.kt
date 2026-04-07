package app.gamegrub.utils.steam

import android.content.Context
import app.gamegrub.data.DepotInfo
import app.gamegrub.service.steam.SteamService
import app.gamegrub.service.steam.SteamService.Companion.getAppInfoOf
import com.winlator.xenvironment.ImageFs
import timber.log.Timber
import java.io.File
import java.nio.file.Files

object SteamManifestInstaller {

    fun createAppManifest(context: Context, steamAppId: Int) {
        try {
            Timber.i("Attempting to createAppManifest for appId: $steamAppId")
            val appInfo = getAppInfoOf(steamAppId)
            if (appInfo == null) {
                Timber.w("No app info found for appId: $steamAppId")
                return
            }

            val imageFs = ImageFs.find(context)

            val steamappsDir = File(imageFs.wineprefix, "drive_c/Program Files (x86)/Steam/steamapps")
            if (!steamappsDir.exists()) {
                steamappsDir.mkdirs()
            }

            val commonDir = File(steamappsDir, "common")
            if (!commonDir.exists()) {
                commonDir.mkdirs()
            }

            val gameDir = File(SteamService.getAppDirPath(steamAppId))
            val gameName = gameDir.name
            val sizeOnDisk = calculateDirectorySize(gameDir)

            val steamGameLink = File(commonDir, gameName)
            if (!steamGameLink.exists()) {
                Files.createSymbolicLink(steamGameLink.toPath(), gameDir.toPath())
                Timber.i("Created symlink from ${steamGameLink.absolutePath} to ${gameDir.absolutePath}")
            }

            val buildId = appInfo.branches["public"]?.buildId ?: 0L
            val downloadableDepots = SteamService.getDownloadableDepots(steamAppId)

            val regularDepots = mutableMapOf<Int, DepotInfo>()
            val sharedDepots = mutableMapOf<Int, DepotInfo>()

            downloadableDepots.forEach { (depotId, depotInfo) ->
                val manifest = depotInfo.manifests["public"]
                if (manifest != null && manifest.gid != 0L) {
                    regularDepots[depotId] = depotInfo
                } else {
                    sharedDepots[depotId] = depotInfo
                }
            }

            val acfContent = buildString {
                appendLine("\"AppState\"")
                appendLine("{")
                appendLine("\t\"appid\"\t\t\"$steamAppId\"")
                appendLine("\t\"Universe\"\t\t\"1\"")
                appendLine("\t\"name\"\t\t\"${escapeString(appInfo.name)}\"")
                appendLine("\t\"StateFlags\"\t\t\"4\"")
                appendLine("\t\"LastUpdated\"\t\t\"${System.currentTimeMillis() / 1000}\"")
                appendLine("\t\"SizeOnDisk\"\t\t\"$sizeOnDisk\"")
                appendLine("\t\"buildid\"\t\t\"$buildId\"")

                val actualInstallDir = appInfo.config.installDir.ifEmpty { gameName }
                appendLine("\t\"installdir\"\t\t\"${escapeString(actualInstallDir)}\"")

                appendLine("\t\"LastOwner\"\t\t\"0\"")
                appendLine("\t\"BytesToDownload\"\t\t\"0\"")
                appendLine("\t\"BytesDownloaded\"\t\t\"0\"")
                appendLine("\t\"AutoUpdateBehavior\"\t\t\"0\"")
                appendLine("\t\"AllowOtherDownloadsWhileRunning\"\t\t\"0\"")
                appendLine("\t\"ScheduledAutoUpdate\"\t\t\"0\"")

                if (regularDepots.isNotEmpty()) {
                    appendLine("\t\"InstalledDepots\"")
                    appendLine("\t{")
                    regularDepots.forEach { (depotId, depotInfo) ->
                        val manifest = depotInfo.manifests["public"]
                        appendLine("\t\t\"$depotId\"")
                        appendLine("\t\t{")
                        appendLine("\t\t\t\"manifest\"\t\t\"${manifest?.gid ?: "0"}\"")
                        appendLine("\t\t\t\"size\"\t\t\"${manifest?.size ?: 0}\"")
                        appendLine("\t\t}")
                    }
                    appendLine("\t}")
                }

                appendLine("\t\"UserConfig\" { \"language\" \"english\" }")
                appendLine("\t\"MountedConfig\" { \"language\" \"english\" }")
                appendLine("}")
            }

            val acfFile = File(steamappsDir, "appmanifest_$steamAppId.acf")
            acfFile.writeText(acfContent)
            Timber.i("Created ACF manifest for ${appInfo.name} at ${acfFile.absolutePath}")

            if (sharedDepots.isNotEmpty()) {
                val steamworksAcfContent = buildString {
                    appendLine("\"AppState\"")
                    appendLine("{")
                    appendLine("\t\"appid\"\t\t\"228980\"")
                    appendLine("\t\"Universe\"\t\t\"1\"")
                    appendLine("\t\"name\"\t\t\"Steamworks Common Redistributables\"")
                    appendLine("\t\"StateFlags\"\t\t\"4\"")
                    appendLine("\t\"installdir\"\t\t\"Steamworks Shared\"")
                    appendLine("\t\"buildid\"\t\t\"1\"")
                    appendLine("\t\"BytesToDownload\"\t\t\"0\"")
                    appendLine("\t\"BytesDownloaded\"\t\t\"0\"")
                    appendLine("}")
                }

                val steamworksAcfFile = File(steamappsDir, "appmanifest_228980.acf")
                steamworksAcfFile.writeText(steamworksAcfContent)
                Timber.i("Created Steamworks Common Redistributables ACF manifest at ${steamworksAcfFile.absolutePath}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to create ACF manifest for appId $steamAppId")
        }
    }

    private fun escapeString(input: String?): String {
        if (input == null) return ""
        return input.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")
    }

    private fun calculateDirectorySize(directory: File): Long {
        if (!directory.exists() || !directory.isDirectory) {
            return 0L
        }

        var size = 0L
        try {
            directory.walkTopDown().forEach { file ->
                if (file.isFile) {
                    size += file.length()
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Error calculating directory size")
        }

        return size
    }
}
