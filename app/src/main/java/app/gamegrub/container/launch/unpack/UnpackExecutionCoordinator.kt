package app.gamegrub.container.launch.unpack

import app.gamegrub.GameGrubApp
import app.gamegrub.events.AndroidEvent
import app.gamegrub.service.steam.SteamService
import app.gamegrub.utils.container.ContainerUtils
import com.winlator.container.Container
import com.winlator.xenvironment.ImageFs
import com.winlator.xenvironment.components.GuestProgramLauncherComponent
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import timber.log.Timber

/**
 * Runs launch-time unpacking and redistributable preparation.
 *
 * This preserves the previous XServerScreen behavior:
 * - optional mono install when unpacking is required,
 * - redistributable presence checks for Steam shared depots,
 * - Steamless execution and .unpacked.exe promotion,
 * - final wineserver restart and persistence of `isNeedsUnpacking`.
 */
internal object UnpackExecutionCoordinator {
    fun unpackExecutableFile(
        context: android.content.Context,
        needsUnpacking: Boolean,
        container: Container,
        appId: String,
        guestProgramLauncherComponent: GuestProgramLauncherComponent,
        containerVariantChanged: Boolean,
        onError: ((String) -> Unit)? = null,
    ) {
        val imageFs = ImageFs.find(context)
        var output = StringBuilder()

        if (needsUnpacking || containerVariantChanged) {
            try {
                GameGrubApp.events.emit(AndroidEvent.SetBootingSplashText("Installing Mono..."))
                val monoCmd = "wine msiexec /i Z:\\opt\\mono-gecko-offline\\wine-mono-9.0.0-x86.msi && wineserver -k"
                Timber.i("Install mono command $monoCmd")
                val monoOutput = guestProgramLauncherComponent.execShellCommand(monoCmd)
                output.append(monoOutput)
                Timber.i("Result of mono command $output")
            } catch (e: Exception) {
                Timber.e("Error during mono installation: $e")
            }

            try {
                installRedistributables(appId)
            } catch (e: Exception) {
                Timber.tag("installRedist").e(e, "Error installing redistributables: ${e.message}")
            }
        }

        if (!needsUnpacking) {
            return
        }

        try {
            try {
                GameGrubApp.events.emit(AndroidEvent.SetBootingSplashText("Handling DRM..."))
                val origTxtFile = File("${imageFs.wineprefix}/dosdevices/a:/orig_dll_path.txt")

                if (origTxtFile.exists()) {
                    val relDllPaths = origTxtFile.readLines().map { it.trim() }.filter { it.isNotBlank() }
                    if (relDllPaths.isNotEmpty()) {
                        Timber.i("Found ${relDllPaths.size} DLL path(s) in orig_dll_path.txt")
                        for (relDllPath in relDllPaths) {
                            try {
                                val origDll = File("${imageFs.wineprefix}/dosdevices/a:/$relDllPath")
                                if (origDll.exists()) {
                                    val genCmd = "wine z:\\generate_interfaces_file.exe A:\\" + relDllPath.replace('/', '\\')
                                    Timber.i("Running generate_interfaces_file $genCmd")
                                    val genOutput = guestProgramLauncherComponent.execShellCommand(genCmd)

                                    val origSteamInterfaces = File("${imageFs.wineprefix}/dosdevices/z:/steam_interfaces.txt")
                                    if (origSteamInterfaces.exists()) {
                                        val finalSteamInterfaces = File(origDll.parent, "steam_interfaces.txt")
                                        try {
                                            Files.copy(
                                                origSteamInterfaces.toPath(),
                                                finalSteamInterfaces.toPath(),
                                                REPLACE_EXISTING,
                                            )
                                            Timber.i("Copied steam_interfaces.txt to ${finalSteamInterfaces.absolutePath}")
                                        } catch (ioe: IOException) {
                                            Timber.w(ioe, "Failed to copy steam_interfaces.txt for $relDllPath")
                                        }
                                    } else {
                                        Timber.w("steam_interfaces.txt not found at $origSteamInterfaces for $relDllPath")
                                    }

                                    Timber.i("Result of generate_interfaces_file command $genOutput")
                                } else {
                                    Timber.w("DLL specified in orig_dll_path.txt not found: $origDll")
                                }
                            } catch (e: Exception) {
                                Timber.w(e, "Failed to process DLL path $relDllPath, continuing with next path")
                            }
                        }
                    } else {
                        Timber.i("orig_dll_path.txt is empty; skipping interface generation")
                    }
                } else {
                    Timber.i("orig_dll_path.txt not present; skipping interface generation")
                }
            } catch (e: Exception) {
                Timber.e("Error running generate_interfaces_file: $e")
            }

            output = StringBuilder()

            if (!container.isLaunchRealSteam) {
                val exePaths = if (container.isUnpackFiles) {
                    val scanned = ContainerUtils.scanExecutablesInADrive(container.drives)
                    val filtered = ContainerUtils.filterExesForUnpacking(scanned)
                    filtered.ifEmpty { listOf(container.executablePath).filter { it.isNotEmpty() } }
                } else {
                    listOf(container.executablePath).filter { it.isNotEmpty() }
                }

                if (exePaths.isEmpty()) {
                    Timber.w("No executable path set, skipping Steamless")
                } else {
                    GameGrubApp.events.emit(AndroidEvent.SetBootingSplashText("Handling DRM..."))
                    for ((index, executablePath) in exePaths.withIndex()) {
                        if (exePaths.size > 1) {
                            GameGrubApp.events.emit(
                                AndroidEvent.SetBootingSplashText("Handling DRM (${index + 1}/${exePaths.size})"),
                            )
                        }

                        var batchFile: File? = null
                        try {
                            val normalizedPath = executablePath.replace('/', '\\')
                            val windowsPath = "A:\\$normalizedPath"

                            batchFile = File(imageFs.rootDir, "tmp/steamless_wrapper.bat")
                            batchFile.parentFile?.mkdirs()
                            batchFile.writeText("@echo off\r\nz:\\Steamless\\Steamless.CLI.exe \"$windowsPath\"\r\n")

                            val slCmd = "wine z:\\tmp\\steamless_wrapper.bat"
                            val slOutput = guestProgramLauncherComponent.execShellCommand(slCmd)
                            output.append(slOutput)
                            Timber.i("Finished processing executable. Result: $output")
                        } catch (e: Exception) {
                            Timber.e(e, "Error running Steamless on $executablePath")
                            output.append("Error processing $executablePath: ${e.message}\n")
                        } finally {
                            batchFile?.delete()
                        }

                        try {
                            val unixPath = executablePath.replace('\\', '/')
                            val exe = File(imageFs.wineprefix + "/dosdevices/a:/" + unixPath)
                            val unpackedExe = File(imageFs.wineprefix + "/dosdevices/a:/" + unixPath + ".unpacked.exe")
                            val originalExe = File(imageFs.wineprefix + "/dosdevices/a:/" + unixPath + ".original.exe")

                            val windowsPathForLog = "A:\\${executablePath.replace('/', '\\')}"
                            Timber.i("Moving files for $windowsPathForLog")
                            if (exe.exists() && unpackedExe.exists()) {
                                if (originalExe.exists()) {
                                    Timber.i("Original backup exists for $windowsPathForLog; skipping overwrite")
                                } else {
                                    Files.copy(exe.toPath(), originalExe.toPath(), REPLACE_EXISTING)
                                }
                                Files.copy(unpackedExe.toPath(), exe.toPath(), REPLACE_EXISTING)
                                Timber.i("Successfully moved files for $windowsPathForLog")
                            } else {
                                val errorMsg = "Either exe or unpacked exe does not exist for $windowsPathForLog"
                                Timber.w(errorMsg)
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "Error moving files for $executablePath")
                        }
                    }
                }
            } else {
                Timber.i(
                    "Skipping Steamless (launchRealSteam=${container.isLaunchRealSteam}, " +
                        "useLegacyDRM=${container.isUseLegacyDRM}, unpackFiles=${container.isUnpackFiles})",
                )
            }

            output = StringBuilder()
            try {
                val wsOutput = guestProgramLauncherComponent.execShellCommand("wineserver -k")
                output.append(wsOutput)
                Timber.i("Result of wineserver -k command $output")
            } catch (e: Exception) {
                Timber.e("Error running wineserver: $e")
            }

            container.isNeedsUnpacking = false
            Timber.d("Setting needs unpacking to false")
            container.saveData()
        } catch (e: Exception) {
            Timber.e("Error during unpacking: $e")
            onError?.invoke("Error during unpacking: ${e.message}")
        }
    }

    private fun installRedistributables(appId: String) {
        try {
            val steamAppId = ContainerUtils.extractGameIdFromContainerId(appId)
            val downloadableDepots = SteamService.getDownloadableDepots(steamAppId)
            val sharedDepots = downloadableDepots.filter { (_, depotInfo) ->
                val manifest = depotInfo.manifests["public"]
                manifest == null || manifest.gid == 0L
            }

            if (sharedDepots.isEmpty()) {
                Timber.tag("installRedist").i("No shared depots found, skipping redistributable installation")
                return
            }

            Timber.tag("installRedist").i("Found ${sharedDepots.size} shared depot(s), checking for redistributables")

            val steamAppDirPath = SteamService.getAppDirPath(steamAppId)
            val commonRedistDir = File(steamAppDirPath, "_CommonRedist")
            if (!commonRedistDir.exists() || !commonRedistDir.isDirectory) {
                Timber.tag("installRedist").i("_CommonRedist directory not found at ${commonRedistDir.absolutePath}")
                return
            }

            Timber.tag("installRedist").i("Finished checking for redistributables")
        } catch (e: Exception) {
            Timber.tag("installRedist").e(e, "Error in installRedistributables: ${e.message}")
        }
    }
}
