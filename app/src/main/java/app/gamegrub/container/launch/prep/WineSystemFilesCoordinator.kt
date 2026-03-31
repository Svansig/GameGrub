package app.gamegrub.container.launch.prep

import android.content.Context
import androidx.compose.runtime.MutableState
import app.gamegrub.container.manager.ContainerRuntimeManager
import app.gamegrub.ui.data.XServerState
import com.winlator.PrefManager as WinlatorPrefManager
import com.winlator.container.Container
import com.winlator.contents.ContentProfile
import com.winlator.contents.ContentsManager
import com.winlator.core.AppUtils
import com.winlator.core.DefaultVersion
import com.winlator.core.FileUtils
import com.winlator.core.GPUHelper
import com.winlator.core.KeyValueSet
import com.winlator.core.OnExtractFileListener
import com.winlator.core.TarCompressorUtils
import com.winlator.core.WineInfo
import com.winlator.core.WineStartMenuCreator
import com.winlator.core.WineThemeManager
import com.winlator.core.WineUtils
import com.winlator.core.envvars.EnvVars
import com.winlator.xenvironment.ImageFs
import com.winlator.xserver.ScreenInfo
import java.io.File
import java.util.Arrays
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

/**
 * Prepares Wine system files and container metadata before environment startup.
 *
 * The implementation keeps the existing sequencing from XServerScreen but
 * receives file-extraction operations as callbacks to minimize migration risk.
 */
internal object WineSystemFilesCoordinator {
    fun setupWineSystemFiles(
        context: Context,
        firstTimeBoot: Boolean,
        screenInfo: ScreenInfo,
        xServerState: MutableState<XServerState>,
        container: Container,
        containerManager: ContainerRuntimeManager,
        envVars: EnvVars,
        contentsManager: ContentsManager,
        onExtractFileListener: OnExtractFileListener?,
        alwaysReextract: Boolean,
    ) {
        val imageFs = ImageFs.find(context)
        val appVersion = AppUtils.getVersionCode(context).toString()
        val imgVersion = imageFs.getVersion().toString()
        val wineVersion = imageFs.getArch()
        val variant = imageFs.getVariant()
        var containerDataChanged = false

        if (!container.getExtra("appVersion").equals(appVersion) ||
            !container.getExtra("imgVersion").equals(imgVersion) ||
            container.containerVariant != variant ||
            (container.containerVariant == variant && container.wineVersion != wineVersion)
        ) {
            applyGeneralPatches(context, container, imageFs, xServerState.value.wineInfo, containerManager, onExtractFileListener)
            container.putExtra("appVersion", appVersion)
            container.putExtra("imgVersion", imgVersion)
            containerDataChanged = true
        }

        refreshComponentsFiles(context)

        if (xServerState.value.dxwrapper == "dxvk") {
            xServerState.value = xServerState.value.copy(
                dxwrapper = "dxvk-" + xServerState.value.dxwrapperConfig?.get("version"),
            )
        }

        if (xServerState.value.dxwrapper == "vkd3d") {
            xServerState.value = xServerState.value.copy(
                dxwrapper = "vkd3d-" + xServerState.value.dxwrapperConfig?.get("vkd3dVersion"),
            )
        }

        val needReextract =
            alwaysReextract || xServerState.value.dxwrapper != container.getExtra("dxwrapper") || container.wineVersion != wineVersion

        Timber.i("needReextract is %s", needReextract)
        Timber.i("xServerState.value.dxwrapper is %s", xServerState.value.dxwrapper)
        Timber.i("container.getExtra(\"dxwrapper\") is %s", container.getExtra("dxwrapper"))

        if (needReextract) {
            extractDXWrapperFiles(
                context,
                firstTimeBoot,
                container,
                containerManager,
                xServerState.value.dxwrapper,
                imageFs,
                contentsManager,
                onExtractFileListener,
            )
            container.putExtra("dxwrapper", xServerState.value.dxwrapper)
            containerDataChanged = true
        }

        if (xServerState.value.dxwrapper == "cnc-ddraw") {
            envVars.put("CNC_DDRAW_CONFIG_FILE", "C:\\ProgramData\\cnc-ddraw\\ddraw.ini")
        }

        val wincomponents = container.winComponents
        if (!wincomponents.equals(container.getExtra("wincomponents"))) {
            extractWinComponentFiles(context, firstTimeBoot, imageFs, container, containerManager, onExtractFileListener)
            container.putExtra("wincomponents", wincomponents)
            containerDataChanged = true
        }

        if (container.isLaunchRealSteam) {
            extractSteamFiles(context, onExtractFileListener)
        }

        val desktopTheme = container.desktopTheme
        if ((desktopTheme + "," + screenInfo) != container.getExtra("desktopTheme")) {
            WineThemeManager.apply(context, WineThemeManager.ThemeInfo(desktopTheme), screenInfo)
            container.putExtra("desktopTheme", desktopTheme + "," + screenInfo)
            containerDataChanged = true
        }

        WineStartMenuCreator.create(context, container)
        WineUtils.createDosdevicesSymlinks(container)

        val startupSelection = container.startupSelection.toString()
        if (startupSelection != container.getExtra("startupSelection")) {
            WineUtils.changeServicesStatus(container, container.startupSelection != Container.STARTUP_SELECTION_NORMAL)
            container.putExtra("startupSelection", startupSelection)
            containerDataChanged = true
        }

        if (containerDataChanged) {
            container.saveData()
        }
    }

    private fun applyGeneralPatches(
        context: Context,
        container: Container,
        imageFs: ImageFs,
        wineInfo: WineInfo,
        containerManager: ContainerRuntimeManager,
        onExtractFileListener: OnExtractFileListener?,
    ) {
        Timber.i("Applying general patches")
        val rootDir = imageFs.rootDir
        val contentsManager = ContentsManager(context)
        if (container.containerVariant.equals(Container.GLIBC)) {
            FileUtils.delete(File(rootDir, "/opt/apps"))
            val downloaded = File(imageFs.filesDir, "imagefs_patches_gamenative.tzst")
            Timber.i("Extracting imagefs_patches_gamenative.tzst")
            if (Arrays.asList<String?>(*context.assets.list("")).contains("imagefs_patches_gamenative.tzst") == true) {
                TarCompressorUtils.extract(
                    TarCompressorUtils.Type.ZSTD,
                    context.assets,
                    "imagefs_patches_gamenative.tzst",
                    rootDir,
                    onExtractFileListener,
                )
            } else if (downloaded.exists()) {
                TarCompressorUtils.extract(
                    TarCompressorUtils.Type.ZSTD,
                    downloaded,
                    rootDir,
                    onExtractFileListener,
                )
            }
        } else {
            Timber.i("Extracting container_pattern_common.tzst")
            TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, context.assets, "container_pattern_common.tzst", rootDir)
            Timber.i("Attempting to extract _container_pattern.tzst with wine version %s", container.wineVersion)
        }
        containerManager.extractPattern(container.wineVersion, contentsManager, container.rootDir, null)
        WineUtils.applySystemTweaks(context, wineInfo)
        container.putExtra("graphicsDriver", null)
        container.putExtra("desktopTheme", null)
        WinlatorPrefManager.init(context)
        WinlatorPrefManager.putString("current_box64_version", "")
    }

    private fun refreshComponentsFiles(context: Context) {
        TarCompressorUtils.extract(
            TarCompressorUtils.Type.ZSTD,
            context.assets,
            "pulseaudio-gamenative.tzst",
            File(context.filesDir, "pulseaudio"),
        )
    }

    private fun extractDXWrapperFiles(
        context: Context,
        firstTimeBoot: Boolean,
        container: Container,
        containerManager: ContainerRuntimeManager,
        dxwrapper: String,
        imageFs: ImageFs,
        contentsManager: ContentsManager,
        onExtractFileListener: OnExtractFileListener?,
    ) {
        val dlls = arrayOf(
            "d3d10.dll",
            "d3d10_1.dll",
            "d3d10core.dll",
            "d3d11.dll",
            "d3d12.dll",
            "d3d12core.dll",
            "d3d8.dll",
            "d3d9.dll",
            "dxgi.dll",
            "ddraw.dll",
        )
        val splitDxWrapper = dxwrapper.split("-")[0]
        if (firstTimeBoot && splitDxWrapper != "vkd3d") {
            cloneOriginalDllFiles(imageFs, *dlls)
        }
        val rootDir = imageFs.rootDir
        val windowsDir = File(rootDir, ImageFs.WINEPREFIX + "/drive_c/windows")

        when (splitDxWrapper) {
            "wined3d" -> {
                restoreOriginalDllFiles(context, container, containerManager, imageFs, *dlls)
            }

            "cnc-ddraw" -> {
                restoreOriginalDllFiles(context, container, containerManager, imageFs, *dlls)
                val assetDir = "dxwrapper/cnc-ddraw-" + DefaultVersion.CNC_DDRAW
                val configFile = File(rootDir, ImageFs.WINEPREFIX + "/drive_c/ProgramData/cnc-ddraw/ddraw.ini")
                if (!configFile.isFile) {
                    FileUtils.copy(context, "$assetDir/ddraw.ini", configFile)
                }
                val shadersDir = File(rootDir, ImageFs.WINEPREFIX + "/drive_c/ProgramData/cnc-ddraw/Shaders")
                FileUtils.delete(shadersDir)
                FileUtils.copy(context, "$assetDir/Shaders", shadersDir)
                TarCompressorUtils.extract(
                    TarCompressorUtils.Type.ZSTD,
                    context.assets,
                    "$assetDir/ddraw.tzst",
                    windowsDir,
                    onExtractFileListener,
                )
            }

            "vkd3d" -> {
                Timber.i("Extracting VKD3D D3D12 DLLs for dxwrapper: $dxwrapper")
                val profile: ContentProfile? = contentsManager.getProfileByEntryName(dxwrapper)
                val vortekLike =
                    container.graphicsDriver == "vortek" || container.graphicsDriver == "adreno" || container.graphicsDriver == "sd-8-elite"
                val dxvkVersionForVkd3d = if (vortekLike &&
                    GPUHelper.vkGetApiVersionSafe() < GPUHelper.vkMakeVersion(1, 3, 0)
                ) {
                    "1.10.3"
                } else {
                    "2.4.1"
                }
                Timber.i("Extracting VKD3D DX version for dxwrapper: $dxvkVersionForVkd3d")
                TarCompressorUtils.extract(
                    TarCompressorUtils.Type.ZSTD,
                    context.assets,
                    "dxwrapper/dxvk-$dxvkVersionForVkd3d.tzst",
                    windowsDir,
                    onExtractFileListener,
                )
                if (profile != null) {
                    Timber.d("Applying user-defined VKD3D content profile: $dxwrapper")
                    contentsManager.applyContent(profile)
                } else {
                    Timber.i("Extracting VKD3D D3D12 DLLs version: $dxwrapper")
                    TarCompressorUtils.extract(
                        TarCompressorUtils.Type.ZSTD,
                        context.assets,
                        "dxwrapper/$dxwrapper.tzst",
                        windowsDir,
                        onExtractFileListener,
                    )
                }
            }

            else -> {
                val profile: ContentProfile? = contentsManager.getProfileByEntryName(dxwrapper)
                Timber.i("Extracting DXVK/D8VK DLLs for dxwrapper: $dxwrapper")
                restoreOriginalDllFiles(context, container, containerManager, imageFs, "d3d12.dll", "d3d12core.dll", "ddraw.dll")
                if (profile != null) {
                    Timber.d("Applying user-defined DXVK content profile: $dxwrapper")
                    contentsManager.applyContent(profile)
                } else {
                    TarCompressorUtils.extract(
                        TarCompressorUtils.Type.ZSTD,
                        context.assets,
                        "dxwrapper/$dxwrapper.tzst",
                        windowsDir,
                        onExtractFileListener,
                    )
                }
                TarCompressorUtils.extract(
                    TarCompressorUtils.Type.ZSTD,
                    context.assets,
                    "dxwrapper/d8vk-${DefaultVersion.D8VK}.tzst",
                    windowsDir,
                    onExtractFileListener,
                )
            }
        }
    }

    private fun cloneOriginalDllFiles(imageFs: ImageFs, vararg dlls: String) {
        val rootDir = imageFs.rootDir
        val cacheDir = File(rootDir, ImageFs.CACHE_PATH + "/original_dlls")
        if (!cacheDir.isDirectory) {
            cacheDir.mkdirs()
        }
        val windowsDir = File(rootDir, ImageFs.WINEPREFIX + "/drive_c/windows")
        val dirnames = arrayOf("system32", "syswow64")

        for (dll in dlls) {
            for (dirname in dirnames) {
                val dllFile = File(windowsDir, "$dirname/$dll")
                if (dllFile.isFile) {
                    FileUtils.copy(dllFile, File(cacheDir, "$dirname/$dll"))
                }
            }
        }
    }

    private fun restoreOriginalDllFiles(
        context: Context,
        container: Container,
        containerManager: ContainerRuntimeManager,
        imageFs: ImageFs,
        vararg dlls: String,
    ) {
        val rootDir = imageFs.rootDir
        if (container.containerVariant.equals(Container.GLIBC)) {
            val cacheDir = File(rootDir, ImageFs.CACHE_PATH + "/original_dlls")
            val contentsManager = ContentsManager(context)
            if (cacheDir.isDirectory) {
                val windowsDir = File(rootDir, ImageFs.WINEPREFIX + "/drive_c/windows")
                val dirnames = cacheDir.list()
                var filesCopied = 0

                for (dll in dlls) {
                    var success = false
                    for (dirname in dirnames!!) {
                        val srcFile = File(cacheDir, "$dirname/$dll")
                        val dstFile = File(windowsDir, "$dirname/$dll")
                        if (FileUtils.copy(srcFile, dstFile)) {
                            success = true
                        }
                    }
                    if (success) {
                        filesCopied++
                    }
                }

                if (filesCopied == dlls.size) {
                    return
                }
            }

            containerManager.extractPattern(
                container.wineVersion,
                contentsManager,
                container.rootDir,
                object : OnExtractFileListener {
                    override fun onExtractFile(file: File, size: Long): File? {
                        val path = file.path
                        if (path.contains("system32/") || path.contains("syswow64/")) {
                            for (dll in dlls) {
                                if (path.endsWith("system32/$dll") || path.endsWith("syswow64/$dll")) {
                                    return file
                                }
                            }
                        }
                        return null
                    }
                },
            )

            cloneOriginalDllFiles(imageFs, *dlls)
        } else {
            val windowsDir = File(rootDir, ImageFs.WINEPREFIX + "/drive_c/windows")
            val system32dlls = if (container.wineVersion.contains("arm64ec")) {
                File(imageFs.getWinePath() + "/lib/wine/aarch64-windows")
            } else {
                File(imageFs.getWinePath() + "/lib/wine/x86_64-windows")
            }

            val syswow64dlls = File(imageFs.getWinePath() + "/lib/wine/i386-windows")

            for (dll in dlls) {
                var srcFile = File(system32dlls, dll)
                var dstFile = File(windowsDir, "system32/$dll")
                FileUtils.copy(srcFile, dstFile)
                srcFile = File(syswow64dlls, dll)
                dstFile = File(windowsDir, "syswow64/$dll")
                FileUtils.copy(srcFile, dstFile)
            }
        }
    }

    private fun extractWinComponentFiles(
        context: Context,
        firstTimeBoot: Boolean,
        imageFs: ImageFs,
        container: Container,
        containerManager: ContainerRuntimeManager,
        onExtractFileListener: OnExtractFileListener?,
    ) {
        val rootDir = imageFs.rootDir
        val windowsDir = File(rootDir, ImageFs.WINEPREFIX + "/drive_c/windows")
        val systemRegFile = File(rootDir, ImageFs.WINEPREFIX + "/system.reg")

        try {
            val wincomponentsJSONObject = JSONObject(FileUtils.readString(context, "wincomponents/wincomponents.json"))
            val dlls = mutableListOf<String>()
            val wincomponents = container.winComponents

            if (firstTimeBoot) {
                for (wincomponent in KeyValueSet(wincomponents)) {
                    val dlnames = wincomponentsJSONObject.getJSONArray(wincomponent[0])
                    for (i in 0 until dlnames.length()) {
                        val dlname = dlnames.getString(i)
                        dlls.add(if (!dlname.endsWith(".exe")) "$dlname.dll" else dlname)
                    }
                }

                cloneOriginalDllFiles(imageFs, *dlls.toTypedArray())
                dlls.clear()
            }

            val oldWinComponentsIter = KeyValueSet(container.getExtra("wincomponents", Container.FALLBACK_WINCOMPONENTS)).iterator()

            for (wincomponent in KeyValueSet(wincomponents)) {
                try {
                    if (wincomponent[1].equals(oldWinComponentsIter.next()[1]) && !firstTimeBoot) {
                        continue
                    }
                } catch (_: StringIndexOutOfBoundsException) {
                    Timber.d("Wincomponent ${wincomponent[0]} does not exist in oldwincomponents, skipping")
                }
                val identifier = wincomponent[0]
                val useNative = wincomponent[1].equals("1")

                if (!container.wineVersion.contains("arm64ec") && identifier.contains("opengl") && useNative) {
                    continue
                }

                if (useNative) {
                    TarCompressorUtils.extract(
                        TarCompressorUtils.Type.ZSTD,
                        context.assets,
                        "wincomponents/$identifier.tzst",
                        windowsDir,
                        onExtractFileListener,
                    )
                } else {
                    val dlnames = wincomponentsJSONObject.getJSONArray(identifier)
                    for (i in 0 until dlnames.length()) {
                        val dlname = dlnames.getString(i)
                        dlls.add(if (!dlname.endsWith(".exe")) "$dlname.dll" else dlname)
                    }
                }
                WineUtils.overrideWinComponentDlls(context, container, identifier, useNative)
                WineUtils.setWinComponentRegistryKeys(systemRegFile, identifier, useNative)
            }

            if (dlls.isNotEmpty()) {
                restoreOriginalDllFiles(context, container, containerManager, imageFs, *dlls.toTypedArray())
            }
        } catch (e: JSONException) {
            Timber.e("Failed to read JSON: $e")
        }
    }

    private fun extractSteamFiles(
        context: Context,
        onExtractFileListener: OnExtractFileListener?,
    ) {
        val imageFs = ImageFs.find(context)
        if (File(
                imageFs.rootDir.absolutePath,
                ImageFs.WINEPREFIX + "/drive_c/Program Files (x86)/Steam/steam.exe",
            ).exists()
        ) {
            return
        }
        val downloaded = File(imageFs.filesDir, "steam.tzst")
        Timber.i("Extracting steam.tzst")
        TarCompressorUtils.extract(
            TarCompressorUtils.Type.ZSTD,
            downloaded,
            imageFs.rootDir,
            onExtractFileListener,
        )
    }
}
