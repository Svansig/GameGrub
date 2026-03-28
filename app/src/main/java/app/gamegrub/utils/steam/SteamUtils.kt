package app.gamegrub.utils.steam

import android.content.Context
import app.gamegrub.PrefManager
import app.gamegrub.api.steamMetadata.SteamMetadataFetcher
import app.gamegrub.data.ManifestInfo
import app.gamegrub.data.SteamApp
import app.gamegrub.enums.Marker
import app.gamegrub.service.steam.SteamService
import app.gamegrub.service.steam.SteamService.Companion.getAppInfoOf
import app.gamegrub.utils.container.ContainerUtils
import app.gamegrub.utils.storage.FileUtils
import app.gamegrub.utils.storage.MarkerUtils
import com.winlator.container.Container
import com.winlator.core.TarCompressorUtils
import com.winlator.core.WineRegistryEditor
import com.winlator.xenvironment.ImageFs
import `in`.dragonbra.javasteam.types.KeyValue
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import kotlin.io.path.absolutePathString
import kotlin.io.path.name
import kotlinx.coroutines.runBlocking
import timber.log.Timber

object SteamUtils {

    fun getDownloadBytes(manifest: ManifestInfo?): Long = SteamFormatUtils.getDownloadBytes(manifest)

    internal val http = SteamMetadataFetcher.http

    /**
     * Converts steam time to actual time
     * @return a string in the 'MMM d - h:mm a' format.
     */
    // Note: Mostly correct, has a slight skew when near another minute
    fun fromSteamTime(rtime: Int): String = SteamFormatUtils.fromSteamTime(rtime)

    /**
     * Converts steam time from the playtime of a friend into an approximate double representing hours.
     * @return A string representing how many hours were played, ie: 1.5 hrs
     */
    fun formatPlayTime(time: Int): String = SteamFormatUtils.formatPlayTime(time)

    private fun generateInterfacesFile(dllPath: Path) {
        val outFile = dllPath.parent.resolve("steam_interfaces.txt")
        if (Files.exists(outFile)) return // already generated on a previous boot

        // -------- read DLL into memory ----------------------------------------
        val bytes = Files.readAllBytes(dllPath)
        val strings = mutableSetOf<String>()

        val sb = StringBuilder()
        fun flush() {
            if (sb.length >= 10) { // only consider reasonably long strings
                val candidate = sb.toString()
                if (candidate.matches(Regex("^Steam[A-Za-z]+[0-9]{3}\$", RegexOption.IGNORE_CASE))) {
                    strings += candidate
                }
            }
            sb.setLength(0)
        }

        for (b in bytes) {
            val ch = b.toInt() and 0xFF
            if (ch in 0x20..0x7E) { // printable ASCII
                sb.append(ch.toChar())
            } else {
                flush()
            }
        }
        flush() // catch trailing string

        if (strings.isEmpty()) {
            Timber.w("No Steam interface strings found in ${dllPath.fileName}")
            return
        }

        val sorted = strings.sorted()
        Files.write(outFile, sorted)
        Timber.i("Generated steam_interfaces.txt (${sorted.size} interfaces)")
    }

    private fun copyOriginalSteamDll(dllPath: Path, appDirPath: String): String? {
        // 1️⃣  back-up next to the original DLL
        val backup = dllPath.parent.resolve("${dllPath.fileName}.orig")
        if (Files.notExists(backup)) {
            try {
                Files.copy(dllPath, backup)
                Timber.i("Copied original ${dllPath.fileName} to $backup")
            } catch (e: IOException) {
                Timber.w(e, "Failed to back up ${dllPath.fileName}")
                return null
            }
        }
        // 2️⃣  return the relative path inside the app directory (even if backup already existed)
        return try {
            val relPath = Paths.get(appDirPath).relativize(backup)
            relPath.toString()
        } catch (e: Exception) {
            Timber.w(e, "Failed to compute relative path for ${dllPath.fileName}")
            null
        }
    }

    /**
     * Replaces any existing `steam_api.dll` or `steam_api64.dll` in the app directory
     * with our pipe dll stored in assets
     */
    suspend fun replaceSteamApi(context: Context, appId: String, isOffline: Boolean = false) {
        val steamAppId = ContainerUtils.extractGameIdFromContainerId(appId)
        val appDirPath = SteamService.getAppDirPath(steamAppId)
        if (MarkerUtils.hasMarker(appDirPath, Marker.STEAM_DLL_REPLACED)) {
            return
        }
        MarkerUtils.removeMarker(appDirPath, Marker.STEAM_DLL_RESTORED)
        MarkerUtils.removeMarker(appDirPath, Marker.STEAM_COLDCLIENT_USED)
        Timber.i("Starting replaceSteamApi for appId: $appId")
        Timber.i("Checking directory: $appDirPath")
        var replaced32Count = 0
        var replaced64Count = 0
        val backupPaths = mutableSetOf<String>()
        val imageFs = ImageFs.find(context)
        autoLoginUserChanges(imageFs)
        setupLightweightSteamConfig(imageFs, SteamService.getSteamId64()?.toString())

        val rootPath = Paths.get(appDirPath)
        // Get ticket once for all DLLs
        val ticketBase64 = SteamService.instance?.getEncryptedAppTicketBase64(steamAppId)

        rootPath.toFile().walkTopDown().maxDepth(10).forEach { file ->
            val path = file.toPath()
            if (!file.isFile || !path.name.startsWith("steam_api", ignoreCase = true)) return@forEach

            val is64Bit = path.name.equals("steam_api64.dll", ignoreCase = true)
            val is32Bit = path.name.equals("steam_api.dll", ignoreCase = true)

            if (is64Bit || is32Bit) {
                val dllName = if (is64Bit) "steam_api64.dll" else "steam_api.dll"
                Timber.i("Found $dllName at ${path.absolutePathString()}, replacing...")
                generateInterfacesFile(path)
                val relPath = copyOriginalSteamDll(path, appDirPath)
                if (relPath != null) {
                    backupPaths.add(relPath)
                }
                Files.delete(path)
                Files.createFile(path)
                FileOutputStream(path.absolutePathString()).use { fos ->
                    context.assets.open("steampipe/$dllName").use { fs ->
                        fs.copyTo(fos)
                    }
                }
                Timber.i("Replaced $dllName")
                if (is64Bit) replaced64Count++ else replaced32Count++
                ensureSteamSettings(context, path, appId, ticketBase64, isOffline)
            }
        }

        // Write all collected backup paths to orig_dll_path.txt
        if (backupPaths.isNotEmpty()) {
            try {
                Files.write(
                    Paths.get(appDirPath).resolve("orig_dll_path.txt"),
                    backupPaths.sorted(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                )
                Timber.i("Wrote ${backupPaths.size} DLL backup paths to orig_dll_path.txt")
            } catch (e: IOException) {
                Timber.w(e, "Failed to write orig_dll_path.txt")
            }
        }

        Timber.i("Finished replaceSteamApi for appId: $appId. Replaced 32bit: $replaced32Count, Replaced 64bit: $replaced64Count")

        // Restore unpacked executable if it exists (for DRM-free mode)
        restoreUnpackedExecutable(context, steamAppId)

        // Restore original steamclient.dll files if they exist
        restoreSteamclientFiles(context, steamAppId)

        // Create Steam ACF manifest for real Steam compatibility
        createAppManifest(context, steamAppId)

        // Game-specific Handling
        ensureSaveLocationsForGames(context, steamAppId)

        // Generate achievements.json
        generateAchievementsFile(rootPath.resolve("steam_settings"), appId)

        MarkerUtils.addMarker(appDirPath, Marker.STEAM_DLL_REPLACED)
    }

    /**
     * Replaces any existing `steamclient.dll` or `steamclient64.dll` in the Steam directory
     */
    suspend fun replaceSteamclientDll(context: Context, appId: String, isOffline: Boolean = false) {
        val steamAppId = ContainerUtils.extractGameIdFromContainerId(appId)
        val appDirPath = SteamService.getAppDirPath(steamAppId)
        val container = ContainerUtils.getContainer(context, appId)

        if (MarkerUtils.hasMarker(appDirPath, Marker.STEAM_COLDCLIENT_USED) &&
            File(container.rootDir, ".wine/drive_c/Program Files (x86)/Steam/steamclient_loader_x64.dll").exists()
        ) {
            return
        }
        MarkerUtils.removeMarker(appDirPath, Marker.STEAM_DLL_REPLACED)
        MarkerUtils.removeMarker(appDirPath, Marker.STEAM_DLL_RESTORED)

        // Make a backup before extracting
        backupSteamclientFiles(context, steamAppId)

        // Delete extra_dlls folder before extraction to prevent conflicts
        val extraDllDir = File(container.rootDir, ".wine/drive_c/Program Files (x86)/Steam/extra_dlls")
        if (extraDllDir.exists()) {
            extraDllDir.deleteRecursively()
            Timber.i("Deleted extra_dlls directory before extraction for appId: $steamAppId")
        }

        val imageFs = ImageFs.find(context)
        val downloaded = File(imageFs.filesDir, "experimental-drm-20260116.tzst")
        TarCompressorUtils.extract(
            TarCompressorUtils.Type.ZSTD,
            downloaded,
            imageFs.rootDir,
        )
        putBackSteamDlls(appDirPath)
        restoreUnpackedExecutable(context, steamAppId)

        // Get ticket and pass to ensureSteamSettings
        val ticketBase64 = SteamService.instance?.getEncryptedAppTicketBase64(steamAppId)
        val path = File(container.rootDir, ".wine/drive_c/Program Files (x86)/Steam/steamclient.dll").toPath()
        ensureSteamSettings(context, path, appId, ticketBase64, isOffline)
        generateAchievementsFile(path, appId)

        // Game-specific Handling
        ensureSaveLocationsForGames(context, steamAppId)

        MarkerUtils.addMarker(appDirPath, Marker.STEAM_COLDCLIENT_USED)
    }

    fun steamClientFiles(): Array<String> {
        return SteamClientFilesManager.steamClientFiles()
    }

    fun backupSteamclientFiles(context: Context, steamAppId: Int) {
        SteamClientFilesManager.backupSteamclientFiles(context, steamAppId)
    }

    fun restoreSteamclientFiles(context: Context, steamAppId: Int) {
        SteamClientFilesManager.restoreSteamclientFiles(context, steamAppId)
    }

    internal fun writeColdClientIni(steamAppId: Int, container: Container) {
        SteamClientFilesManager.writeColdClientIni(steamAppId, container)
    }

    fun autoLoginUserChanges(imageFs: ImageFs) {
        val service = SteamService.instance ?: throw IllegalStateException("SteamService must be running to apply Steam session files")
        service.sessionFilesManager.applyAutoLoginUserChanges(
            imageFs = imageFs,
            steamId64 = SteamService.getSteamId64()?.toString() ?: "0",
            account = PrefManager.username,
            refreshToken = PrefManager.refreshToken,
            accessToken = PrefManager.accessToken,
            personaName = service.localPersona.value.name.ifBlank { PrefManager.username },
        )
    }

    /**
     * Creates configuration files that make Steam run in lightweight mode
     * with reduced resource usage and disabled community features
     */
    private fun setupLightweightSteamConfig(imageFs: ImageFs, steamId64: String?) {
        Timber.i("Setting up lightweight steam configs")
        try {
            val steamPath = File(imageFs.wineprefix, "drive_c/Program Files (x86)/Steam")

            // Create necessary directories
            val userDataPath = File(steamPath, "userdata/$steamId64")
            val configPath = File(userDataPath, "config")
            val remotePath = File(userDataPath, "7/remote")

            configPath.mkdirs()
            remotePath.mkdirs()

            // Create localconfig.vdf for small mode and low resource usage
            val localConfigContent = """
                "UserLocalConfigStore"
                {
                  "Software"
                  {
                    "Valve"
                    {
                      "Steam"
                      {
                        "SmallMode"                      "1"
                        "LibraryDisableCommunityContent" "1"
                        "LibraryLowBandwidthMode"        "1"
                        "LibraryLowPerfMode"             "1"
                      }
                    }
                  }
                  "friends"
                  {
                    "SignIntoFriends" "0"
                  }
                }
            """.trimIndent()

            // Create sharedconfig.vdf for additional optimizations
            val sharedConfigContent = """
                "UserRoamingConfigStore"
                {
                  "Software"
                  {
                    "Valve"
                    {
                      "Steam"
                      {
                        "SteamDefaultDialog" "#app_games"
                        "FriendsUI"
                        {
                          "FriendsUIJSON" "{\"bSignIntoFriends\":false,\"bAnimatedAvatars\":false,\"PersonaNotifications\":0,\"bDisableRoomEffects\":true}"
                        }
                      }
                    }
                  }
                }
            """.trimIndent()

            // Write the configuration files if they don't exist
            val localConfigFile = File(configPath, "localconfig.vdf")
            val sharedConfigFile = File(remotePath, "sharedconfig.vdf")

            if (!localConfigFile.exists()) {
                localConfigFile.writeText(localConfigContent)
                Timber.i("Created lightweight Steam localconfig.vdf")
            }

            if (!sharedConfigFile.exists()) {
                sharedConfigFile.writeText(sharedConfigContent)
                Timber.i("Created lightweight Steam sharedconfig.vdf")
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to setup lightweight Steam configuration")
        }
    }

    /**
     * Restores the unpacked executable (.unpacked.exe) if it exists and is different from current .exe
     * This ensures we use the DRM-free version when not using real Steam
     */
    private fun restoreUnpackedExecutable(context: Context, steamAppId: Int) {
        try {
            val imageFs = ImageFs.find(context)
            val appDirPath = SteamService.getAppDirPath(steamAppId)

            // Convert to Wine path format
            val container = ContainerUtils.getContainer(context, "STEAM_$steamAppId")
            val executablePath = container.executablePath
            val drives = container.drives
            val driveIndex = drives.indexOf(appDirPath)
            val drive = if (driveIndex > 1) {
                drives[driveIndex - 2]
            } else {
                Timber.e("Could not locate game drive")
                'D'
            }
            val executableFile = "$drive:\\$executablePath"

            val exe = File(imageFs.wineprefix + "/dosdevices/" + executableFile.replace("A:", "a:").replace('\\', '/'))
            val unpackedExe = File(
                imageFs.wineprefix + "/dosdevices/" + executableFile.replace("A:", "a:").replace('\\', '/') + ".unpacked.exe",
            )

            if (unpackedExe.exists()) {
                // Check if files are different (compare size and last modified time for efficiency)
                val areFilesDifferent = !exe.exists() ||
                    exe.length() != unpackedExe.length() ||
                    exe.lastModified() != unpackedExe.lastModified()

                if (areFilesDifferent) {
                    Files.copy(unpackedExe.toPath(), exe.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    Timber.i("Restored unpacked executable from ${unpackedExe.name} to ${exe.name}")
                } else {
                    Timber.i("Unpacked executable is already current, no restore needed")
                }
            } else {
                Timber.i("No unpacked executable found, using current executable")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to restore unpacked executable for appId $steamAppId")
        }
    }

    /**
     * Creates a Steam ACF (Application Cache File) manifest for the given app
     * This allows real Steam to detect the game as installed
     */
    private fun createAppManifest(context: Context, steamAppId: Int) {
        SteamManifestInstaller.createAppManifest(context, steamAppId)
    }

    /**
     * Restores the original steam_api.dll and steam_api64.dll files from their .orig backups
     * if they exist. Does not error if backup files are not found.
     */
    fun restoreSteamApi(context: Context, appId: String) {
        Timber.i("Starting restoreSteamApi for appId: $appId")
        val steamAppId = ContainerUtils.extractGameIdFromContainerId(appId)
        val imageFs = ImageFs.find(context)
        val container = ContainerUtils.getOrCreateContainer(context, appId)
        val cfgFile = File(imageFs.wineprefix, "drive_c/Program Files (x86)/Steam/steam.cfg")
        if (!cfgFile.exists()) {
            cfgFile.parentFile?.mkdirs()
            Files.createFile(cfgFile.toPath())
            cfgFile.writeText("BootStrapperInhibitAll=Enable\nBootStrapperForceSelfUpdate=False")
        }

        // Update or modify localconfig.vdf
        val accountId = SteamService.getSteam3AccountId()?.toString() ?: "0"
        updateOrModifyLocalConfig(imageFs, container, steamAppId.toString(), accountId)

        skipFirstTimeSteamSetup(imageFs.rootDir)
        val appDirPath = SteamService.getAppDirPath(steamAppId)
        if (MarkerUtils.hasMarker(appDirPath, Marker.STEAM_DLL_RESTORED)) {
            return
        }
        MarkerUtils.removeMarker(appDirPath, Marker.STEAM_DLL_REPLACED)
        MarkerUtils.removeMarker(appDirPath, Marker.STEAM_COLDCLIENT_USED)
        Timber.i("Checking directory: $appDirPath")

        autoLoginUserChanges(imageFs)
        setupLightweightSteamConfig(imageFs, accountId)

        putBackSteamDlls(appDirPath)

        Timber.i("Finished restoreSteamApi for appId: $appId")

        // Restore original executable if it exists (for real Steam mode)
        restoreOriginalExecutable(context, steamAppId)

        // Restore original steamclient.dll files if they exist
        restoreSteamclientFiles(context, steamAppId)

        // Create Steam ACF manifest for real Steam compatibility
        createAppManifest(context, steamAppId)

        // Game-specific Handling
        ensureSaveLocationsForGames(context, steamAppId)

        MarkerUtils.addMarker(appDirPath, Marker.STEAM_DLL_RESTORED)
    }

    fun findSteamApiDllRootFile(file: File, depth: Int): File? {
        return SteamClientFilesManager.findSteamApiDllRootFile(file, depth)
    }

    fun putBackSteamDlls(appDirPath: String) {
        SteamClientFilesManager.putBackSteamDlls(appDirPath)
    }

    /**
     * Restores the original executable files from their .original.exe backups
     * if they exist. Does not error if backup files are not found.
     */
    fun restoreOriginalExecutable(context: Context, steamAppId: Int) {
        SteamClientFilesManager.restoreOriginalExecutable(context, steamAppId)
    }

    /**
     * Sibling folder "steam_settings" + empty "offline.txt" file, no-ops if they already exist.
     */
    private fun ensureSteamSettings(
        context: Context,
        dllPath: Path,
        appId: String,
        ticketBase64: String? = null,
        isOffline: Boolean = false,
    ) {
        val steamAppId = ContainerUtils.extractGameIdFromContainerId(appId)
        val steamDir = dllPath.parent
        Files.createDirectories(steamDir)
        val appIdFileUpper = dllPath.parent.resolve("steam_appid.txt")
        if (Files.notExists(appIdFileUpper)) {
            Files.createFile(appIdFileUpper)
            appIdFileUpper.toFile().writeText(steamAppId.toString())
        }
        val settingsDir = dllPath.parent.resolve("steam_settings")
        if (Files.notExists(settingsDir)) {
            Files.createDirectories(settingsDir)
        }
        val appIdFile = settingsDir.resolve("steam_appid.txt")
        if (Files.notExists(appIdFile)) {
            Files.createFile(appIdFile)
            appIdFile.toFile().writeText(steamAppId.toString())
        }
        val depotsFile = settingsDir.resolve("depots.txt")
        if (Files.exists(depotsFile)) {
            Files.delete(depotsFile)
        }
        SteamService.getInstalledDepotsOf(steamAppId)?.sorted()?.let { depotsList ->
            Files.createFile(depotsFile)
            depotsFile.toFile().writeText(depotsList.joinToString(System.lineSeparator()))
        }

        val configsIni = settingsDir.resolve("configs.user.ini")
        val accountName = PrefManager.username
        val accountSteamId = SteamService.getSteamId64()?.toString()
            ?: PrefManager.steamUserSteamId64.takeIf { it != 0L }?.toString()
            ?: "0"
        val accountId = SteamService.getSteam3AccountId()
            ?: PrefManager.steamUserAccountId.takeIf { it != 0 }?.toLong()
            ?: 0L
        val container = ContainerUtils.getOrCreateContainer(context, appId)
        val language = runCatching {
            (
                container.getExtra("language", null)
                    ?: container.javaClass.getMethod("getLanguage").invoke(container) as? String
                )
                ?: "english"
        }.getOrDefault("english").lowercase()
        val useSteamInput = container.getExtra("useSteamInput", "false").toBoolean()

        // Get appInfo to check if saveFilePatterns exist (used for both user and app configs)
        val appInfo = getAppInfoOf(steamAppId)
        val hasSaveFilePatterns = appInfo?.ufs?.saveFilePatterns?.isNotEmpty() == true

        val iniContent = buildString {
            appendLine("[user::general]")
            appendLine("account_name=$accountName")
            appendLine("account_steamid=$accountSteamId")
            appendLine("language=$language")
            if (!ticketBase64.isNullOrEmpty()) {
                appendLine("ticket=$ticketBase64")
            }

            // Only add [user::saves] section if no saveFilePatterns are defined
            if (!hasSaveFilePatterns) {
                val steamUserDataPath = "C:\\Program Files (x86)\\Steam\\userdata\\$accountId"
                appendLine()
                appendLine("[user::saves]")
                appendLine("local_save_path=$steamUserDataPath")
            }
        }

        if (Files.notExists(configsIni)) Files.createFile(configsIni)
        configsIni.toFile().writeText(iniContent)

        val appIni = settingsDir.resolve("configs.app.ini")
        val dlcIds = SteamService.getInstalledDlcDepotsOf(steamAppId)
        val dlcApps = SteamService.getDownloadableDlcAppsOf(steamAppId)
        val hiddenDlcApps = SteamService.getHiddenDlcAppsOf(steamAppId)
        val appendedDlcIds = mutableListOf<Int>()

        val forceDlc = container.isForceDlc
        val appIniContent = buildString {
            appendLine("[app::dlcs]")
            appendLine("unlock_all=${if (forceDlc) 1 else 0}")
            dlcIds?.sorted()?.forEach {
                appendLine("$it=dlc$it")
                appendedDlcIds.add(it)
            }

            dlcApps?.forEach { dlcApp ->
                val installedDlcApp = SteamService.getInstalledApp(dlcApp.id)
                if (installedDlcApp != null && !appendedDlcIds.contains(dlcApp.id)) {
                    appendLine("${dlcApp.id}=dlc${dlcApp.id}")
                    appendedDlcIds.add(dlcApp.id)
                }
            }

            // only add hidden dlc apps if not found in appendedDlcIds
            hiddenDlcApps?.forEach { hiddenDlcApp ->
                if (!appendedDlcIds.contains(hiddenDlcApp.id) &&
                    // only add hidden dlc apps if it is not a DLC of the main app
                    appInfo!!.depots.filter { (_, depot) -> depot.dlcAppId == hiddenDlcApp.id }.size <= 1
                ) {
                    appendLine("${hiddenDlcApp.id}=dlc${hiddenDlcApp.id}")
                }
            }

            // Add cloud save config sections if appInfo exists
            if (appInfo != null) {
                appendLine()
                append(generateCloudSaveConfig(appInfo))
            }
        }

        if (Files.notExists(appIni)) Files.createFile(appIni)
        appIni.toFile().writeText(appIniContent)

        val mainIni = settingsDir.resolve("configs.main.ini")

        val steamOfflineMode = container.isSteamOfflineMode
        val useOfflineConfig = steamOfflineMode || isOffline
        val mainIniContent = buildString {
            appendLine("[main::connectivity]")
            appendLine("disable_lan_only=${if (useOfflineConfig) 0 else 1}")
            if (useOfflineConfig) {
                appendLine("offline=1")
            }
        }

        if (Files.notExists(mainIni)) Files.createFile(mainIni)
        mainIni.toFile().writeText(mainIniContent)

        val controllerDir = settingsDir.resolve("controller")
        if (useSteamInput) {
            val controllerVdfText = SteamService.resolveSteamControllerVdfText(steamAppId)
            if (!controllerVdfText.isNullOrEmpty()) {
                runCatching {
                    SteamControllerVdfUtils.generateControllerConfig(controllerVdfText, controllerDir)
                }.onFailure { error ->
                    Timber.w(error, "Failed to generate controller config for $steamAppId")
                }
            }
        } else {
            runCatching {
                if (Files.exists(controllerDir)) {
                    controllerDir.toFile().deleteRecursively()
                }
            }.onFailure { error ->
                Timber.w(error, "Failed to delete controller config for $steamAppId")
            }
        }

        // Write supported languages list
        val supportedLanguagesFile = settingsDir.resolve("supported_languages.txt")
        if (Files.notExists(supportedLanguagesFile)) {
            Files.createFile(supportedLanguagesFile)
        }
        val supportedLanguages = listOf(
            "arabic",
            "bulgarian",
            "schinese",
            "tchinese",
            "czech",
            "danish",
            "dutch",
            "english",
            "finnish",
            "french",
            "german",
            "greek",
            "hungarian",
            "italian",
            "japanese",
            "koreana",
            "norwegian",
            "polish",
            "portuguese",
            "brazilian",
            "romanian",
            "russian",
            "spanish",
            "latam",
            "swedish",
            "thai",
            "turkish",
            "ukrainian",
            "vietnamese",
        )
        supportedLanguagesFile.toFile().writeText(supportedLanguages.joinToString("\n"))
    }

    /**
     * Generates cloud save configuration sections for configs.app.ini
     * Returns empty string if no Windows save patterns are found
     */
    private fun generateCloudSaveConfig(appInfo: SteamApp): String {
        // Filter to only Windows save patterns
        val windowsPatterns = appInfo.ufs.saveFilePatterns.filter { it.root.isWindows }

        return buildString {
            if (windowsPatterns.isNotEmpty()) {
                appendLine("[app::cloud_save::general]")
                appendLine("create_default_dir=1")
                appendLine("create_specific_dirs=1")
                appendLine()
                appendLine("[app::cloud_save::win]")
                val uniqueDirs = LinkedHashSet<String>()
                windowsPatterns.forEach { pattern ->
                    val root = if (pattern.root.name == "GameInstall") "gameinstall" else pattern.root.name
                    val path = pattern.path
                        .replace("{64BitSteamID}", "{::64BitSteamID::}")
                        .replace("{Steam3AccountID}", "{::Steam3AccountID::}")
                    uniqueDirs.add("{::$root::}/$path")
                }

                uniqueDirs.forEachIndexed { index, dir ->
                    appendLine("dir${index + 1}=$dir")
                }
            }
        }
    }


    private fun skipFirstTimeSteamSetup(rootDir: File?) {
        val systemRegFile = File(rootDir, ImageFs.WINEPREFIX + "/system.reg")
        val redistributables = listOf(
            "DirectX\\Jun2010" to "DXSetup", // DirectX Jun 2010
            ".NET\\3.5" to "3.5 SP1", // .NET 3.5
            ".NET\\3.5 Client Profile" to "3.5 Client Profile SP1",
            ".NET\\4.0" to "4.0", // .NET 4.0
            ".NET\\4.0 Client Profile" to "4.0 Client Profile",
            ".NET\\4.5.2" to "4.5.2",
            ".NET\\4.6" to "4.6",
            ".NET\\4.7" to "4.7",
            ".NET\\4.8" to "4.8",
            "XNA\\3.0" to "3.0", // XNA 3.0
            "XNA\\3.1" to "3.1",
            "XNA\\4.0" to "4.0",
            "OpenAL\\2.0.7.0" to "2.0.7.0", // OpenAL 2.0.7.0
            ".NET\\4.5.1" to "4.5.1", // some Unity 5 titles
            ".NET\\4.6.1" to "4.6.1", // Space Engineers, Far Cry 5 :contentReference[oaicite:1]{index=1}
            ".NET\\4.6.2" to "4.6.2",
            ".NET\\4.7.1" to "4.7.1",
            ".NET\\4.7.2" to "4.7.2", // common fix loops :contentReference[oaicite:2]{index=2}
            ".NET\\4.8.1" to "4.8.1",
        )

        WineRegistryEditor(systemRegFile).use { registryEditor ->
            redistributables.forEach { (subPath, valueName) ->
                registryEditor.setDwordValue(
                    "Software\\Valve\\Steam\\Apps\\CommonRedist\\$subPath",
                    valueName,
                    1,
                )
                registryEditor.setDwordValue(
                    "Software\\Wow6432Node\\Valve\\Steam\\Apps\\CommonRedist\\$subPath",
                    valueName,
                    1,
                )
            }
        }
    }

    fun fetchDirect3DMajor(steamAppId: Int, callback: (Int) -> Unit) {
        SteamMetadataFetcher.fetchDirect3DMajor(steamAppId, callback)
    }

    fun updateOrModifyLocalConfig(imageFs: ImageFs, container: Container, appId: String, steamUserId64: String) {
        try {
            val exeCommandLine = container.execArgs

            val steamPath = File(imageFs.wineprefix, "drive_c/Program Files (x86)/Steam")

            // Create necessary directories
            val userDataPath = File(steamPath, "userdata/$steamUserId64")
            val configPath = File(userDataPath, "config")
            configPath.mkdirs()

            val localConfigFile = File(configPath, "localconfig.vdf")

            if (localConfigFile.exists()) {
                val vdfContent = FileUtils.readFileAsString(localConfigFile.absolutePath)
                val vdfData = KeyValue.loadFromString(vdfContent!!)!!
                val app = vdfData["Software"]["Valve"]["Steam"]["apps"][appId]
                val option = app.children.firstOrNull { it.name == "LaunchOptions" }
                if (option != null) {
                    option.value = exeCommandLine.orEmpty()
                } else {
                    app.children.add(KeyValue("LaunchOptions", exeCommandLine))
                }

                vdfData.saveToFile(localConfigFile, false)
            } else {
                val vdfData = KeyValue(name = "UserLocalConfigStore")
                val option = KeyValue("LaunchOptions", exeCommandLine)
                val software = KeyValue("Software")
                val valve = KeyValue("Valve")
                val steam = KeyValue("Steam")
                val apps = KeyValue("apps")
                val app = KeyValue(appId)

                app.children.add(option)
                apps.children.add(app)
                steam.children.add(apps)
                valve.children.add(steam)
                software.children.add(valve)
                vdfData.children.add(software)

                vdfData.saveToFile(localConfigFile, false)
            }

            val userLanguage = container.language
            val steamappsDir = File(steamPath, "steamapps")
            val appManifestFile = File(steamappsDir, "appmanifest_$appId.acf")

            if (appManifestFile.exists()) {
                val manifestContent = FileUtils.readFileAsString(appManifestFile.absolutePath)
                val manifestData = KeyValue.loadFromString(manifestContent!!)!!

                val userConfig = manifestData.children.firstOrNull { it.name == "UserConfig" }
                if (userConfig != null) {
                    val languageKey = userConfig.children.firstOrNull { it.name == "language" }
                    if (languageKey != null) {
                        languageKey.value = userLanguage
                    } else {
                        userConfig.children.add(KeyValue("language", userLanguage))
                    }
                } else {
                    val newUserConfig = KeyValue("UserConfig")
                    newUserConfig.children.add(KeyValue("language", userLanguage))
                    manifestData.children.add(newUserConfig)
                }

                val mountedConfig = manifestData.children.firstOrNull { it.name == "MountedConfig" }
                if (mountedConfig != null) {
                    val languageKey = mountedConfig.children.firstOrNull { it.name == "language" }
                    if (languageKey != null) {
                        languageKey.value = userLanguage
                    } else {
                        mountedConfig.children.add(KeyValue("language", userLanguage))
                    }
                } else {
                    val newMountedConfig = KeyValue("MountedConfig")
                    newMountedConfig.children.add(KeyValue("language", userLanguage))
                    manifestData.children.add(newMountedConfig)
                }

                manifestData.saveToFile(appManifestFile, false)
                Timber.i("Updated app manifest language to $userLanguage for appId $appId")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to update or modify local config")
        }
    }

    fun getSteamId64(): Long? {
        return SteamService.getSteamId64()
    }

    fun getSteam3AccountId(): Long? {
        return SteamService.getSteam3AccountId()
    }

    /**
     * Ensures save locations for games that require special handling (e.g., symlinks)
     * This function checks if the current game needs any save location mappings
     * and applies them automatically.
     *
     * Supports placeholders in paths:
     * - {64BitSteamID} - Replaced with the user's 64-bit Steam ID
     * - {Steam3AccountID} - Replaced with the user's Steam3 account ID
     */
    fun ensureSaveLocationsForGames(context: Context, steamAppId: Int) {
        SteamSaveLocationManager.ensureSaveLocationsForGames(context, steamAppId)
    }

    fun generateAchievementsFile(dllPath: Path, appId: String) {
        if (!SteamService.isLoggedIn) {
            Timber.w("Skipping achievements generation for $appId — Steam not logged in")
            return
        }

        val steamAppId = ContainerUtils.extractGameIdFromContainerId(appId)
        val settingsDir = dllPath.parent.resolve("steam_settings")
        if (Files.notExists(settingsDir)) {
            Files.createDirectories(settingsDir)
        }

        try {
            runBlocking {
                SteamService.generateAchievements(steamAppId, settingsDir.absolutePathString())
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to generate achievements for $appId")
        }
    }
}
