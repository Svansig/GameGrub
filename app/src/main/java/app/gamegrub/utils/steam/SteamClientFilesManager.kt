package app.gamegrub.utils.steam

import android.content.Context
import app.gamegrub.service.steam.SteamService
import app.gamegrub.service.steam.SteamService.Companion.getAppDirName
import app.gamegrub.service.steam.SteamService.Companion.getAppInfoOf
import com.winlator.container.Container
import com.winlator.xenvironment.ImageFs
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.io.path.absolutePathString
import kotlin.io.path.name

object SteamClientFilesManager {

    fun steamClientFiles(): Array<String> {
        return arrayOf(
            "GameOverlayRenderer.dll",
            "GameOverlayRenderer64.dll",
            "steamclient.dll",
            "steamclient64.dll",
            "steamclient_loader_x32.exe",
            "steamclient_loader_x64.exe",
        )
    }

    fun backupSteamclientFiles(context: Context, steamAppId: Int) {
        val imageFs = ImageFs.find(context)
        var backupCount = 0

        val backupDir = File(imageFs.wineprefix, "drive_c/Program Files (x86)/Steam/steamclient_backup")
        backupDir.mkdirs()

        steamClientFiles().forEach { fileName ->
            val dll = File(imageFs.wineprefix, "drive_c/Program Files (x86)/Steam/$fileName")
            if (dll.exists()) {
                Files.copy(dll.toPath(), File(backupDir, "$fileName.orig").toPath(), StandardCopyOption.REPLACE_EXISTING)
                backupCount++
            }
        }

        Timber.i("Finished backupSteamclientFiles for appId: $steamAppId. Backed up $backupCount file(s)")
    }

    fun restoreSteamclientFiles(context: Context, steamAppId: Int) {
        val imageFs = ImageFs.find(context)
        var restoredCount = 0

        val origDir = File(imageFs.wineprefix, "drive_c/Program Files (x86)/Steam")
        val backupDir = File(imageFs.wineprefix, "drive_c/Program Files (x86)/Steam/steamclient_backup")

        if (backupDir.exists()) {
            steamClientFiles().forEach { fileName ->
                val dll = File(backupDir, "$fileName.orig")
                if (dll.exists()) {
                    Files.copy(dll.toPath(), File(origDir, fileName).toPath(), StandardCopyOption.REPLACE_EXISTING)
                    restoredCount++
                }
            }
        }

        val extraDllDir = File(imageFs.wineprefix, "drive_c/Program Files (x86)/Steam/extra_dlls")
        if (extraDllDir.exists()) {
            extraDllDir.deleteRecursively()
        }

        Timber.i("Finished restoreSteamclientFiles for appId: $steamAppId. Restored $restoredCount file(s)")
    }

    fun writeColdClientIni(steamAppId: Int, container: Container) {
        val gameName = getAppDirName(getAppInfoOf(steamAppId))
        val executablePath = container.executablePath.replace("/", "\\")
        val exePath = "steamapps\\common\\$gameName\\$executablePath"
        val exeRunDir = "steamapps\\common\\$gameName"
        val exeCommandLine = container.execArgs
        val iniFile = File(container.rootDir, ".wine/drive_c/Program Files (x86)/Steam/ColdClientLoader.ini")
        iniFile.parentFile?.mkdirs()

        val injectionSection = if (container.isUnpackFiles) {
            """
                [Injection]
                IgnoreLoaderArchDifference=1
                DllsToInjectFolder=extra_dlls
            """
        } else {
            """
                [Injection]
                IgnoreLoaderArchDifference=1
            """
        }

        iniFile.writeText(
            """
                [SteamClient]

                Exe=$exePath
                ExeRunDir=$exeRunDir
                ExeCommandLine=$exeCommandLine
                AppId=$steamAppId

                # path to the steamclient dlls, both must be set, absolute paths or relative to the loader directory
                SteamClientDll=steamclient.dll
                SteamClient64Dll=steamclient64.dll

                $injectionSection
            """.trimIndent(),
        )
    }

    fun findSteamApiDllRootFile(file: File, depth: Int): File? {
        if (depth < 0) return null
        val (files, directories) = file.walkTopDown().maxDepth(1).partition { it.isFile }

        val steamApi = files.firstOrNull {
            it.toPath().name.startsWith("steam_api", true) &&
                    (
                            it.toPath().name.endsWith(".dll", true) ||
                                    it.toPath().name.endsWith(".dll.orig", true)
                            )
        }

        if (steamApi != null) {
            return steamApi.parentFile
        }

        return directories.filter { it != file }.firstNotNullOfOrNull { findSteamApiDllRootFile(it, depth - 1) }
    }

    fun putBackSteamDlls(appDirPath: String) {
        val dllRootFile = findSteamApiDllRootFile(File(appDirPath), 10)

        if (dllRootFile == null) {
            Timber.w("Failed to find steam_api.dll/steam_api64.dll on a Steam game")
            return
        }

        dllRootFile.walkTopDown().maxDepth(1).forEach { file ->
            val path = file.toPath()
            if (!file.isFile ||
                !path.name.startsWith("steam_api", ignoreCase = true) ||
                !path.name.endsWith(".orig", ignoreCase = true)
            ) {
                return@forEach
            }

            val is64Bit = path.name.equals("steam_api64.dll.orig", ignoreCase = true)
            val is32Bit = path.name.equals("steam_api.dll.orig", ignoreCase = true)
            if (!is32Bit && !is64Bit) {
                return@forEach
            }

            try {
                val dllName = if (is64Bit) "steam_api64.dll" else "steam_api.dll"
                val originalPath = path.parent.resolve(dllName)
                Timber.i("Found ${path.name} at ${path.absolutePathString()}, restoring...")

                if (Files.exists(originalPath)) {
                    Files.delete(originalPath)
                }
                Files.copy(path, originalPath)
                Timber.i("Restored $dllName from backup")
            } catch (e: IOException) {
                Timber.w(e, "Failed to restore ${path.name} from backup")
            }
        }
    }

    fun restoreOriginalExecutable(context: Context, steamAppId: Int) {
        Timber.i("Starting restoreOriginalExecutable for appId: $steamAppId")
        val appDirPath = SteamService.getAppDirPath(steamAppId)
        Timber.i("Checking directory: $appDirPath")
        var restoredCount = 0

        val imageFs = ImageFs.find(context)
        val dosDevicesPath = File(imageFs.wineprefix, "dosdevices/a:")

        dosDevicesPath.walkTopDown().maxDepth(10)
            .filter { it.isFile && it.name.endsWith(".original.exe", ignoreCase = true) }
            .forEach { file ->
                try {
                    val origPath = file.toPath()
                    val originalPath = origPath.parent.resolve(origPath.name.removeSuffix(".original.exe"))
                    Timber.i("Found ${origPath.name} at ${origPath.absolutePathString()}, restoring...")

                    if (Files.exists(originalPath)) {
                        Files.delete(originalPath)
                    }

                    Files.copy(origPath, originalPath)

                    Timber.i("Restored ${originalPath.fileName} from backup")
                    restoredCount++
                } catch (e: IOException) {
                    Timber.w(e, "Failed to restore ${file.name} from backup")
                }
            }

        Timber.i("Finished restoreOriginalExecutable for appId: $steamAppId. Restored $restoredCount executable(s)")
    }
}
