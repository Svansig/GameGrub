package app.gamegrub.container.launch.prep

import android.content.Context
import app.gamegrub.device.DeviceQueryProvider
import com.winlator.container.Container
import com.winlator.contents.AdrenotoolsManager
import com.winlator.core.DXVKHelper
import com.winlator.core.DefaultVersion
import com.winlator.core.FileUtils
import com.winlator.core.GPUHelper
import com.winlator.core.KeyValueSet
import com.winlator.core.TarCompressorUtils
import com.winlator.core.envvars.EnvVars
import com.winlator.xconnector.UnixSocketConfig
import com.winlator.xenvironment.ImageFs
import java.io.File
import java.util.Locale
import timber.log.Timber

/**
 * Handles graphics-driver payload extraction and related runtime env vars.
 */
internal object GraphicsDriverPreparationCoordinator {
    fun extractGraphicsDriverFiles(
        context: Context,
        graphicsDriver: String,
        dxwrapper: String,
        dxwrapperConfig: KeyValueSet,
        container: Container,
        envVars: EnvVars,
        firstTimeBoot: Boolean,
        vkbasaltConfig: String,
        alwaysReextract: Boolean,
    ) {
        val deviceQueryGateway = DeviceQueryProvider.from(context)
        if (container.containerVariant.equals(Container.GLIBC)) {
            val turnipVersion =
                container.graphicsDriverVersion.takeIf { it.isNotEmpty() && graphicsDriver == "turnip" } ?: DefaultVersion.TURNIP
            val virglVersion =
                container.graphicsDriverVersion.takeIf { it.isNotEmpty() && graphicsDriver == "virgl" } ?: DefaultVersion.VIRGL
            val zinkVersion =
                container.graphicsDriverVersion.takeIf { it.isNotEmpty() && graphicsDriver == "zink" } ?: DefaultVersion.ZINK
            val adrenoVersion =
                container.graphicsDriverVersion.takeIf { it.isNotEmpty() && graphicsDriver == "adreno" } ?: DefaultVersion.ADRENO
            val sd8EliteVersion =
                container.graphicsDriverVersion.takeIf { it.isNotEmpty() && graphicsDriver == "sd-8-elite" } ?: DefaultVersion.SD8ELITE

            var cacheId = graphicsDriver
            if (graphicsDriver == "turnip") {
                cacheId += "-$turnipVersion-$zinkVersion"
                if (turnipVersion == "25.2.0" || turnipVersion == "25.3.0") {
                    if (deviceQueryGateway.isAdreno710720732()) {
                        envVars.put("TU_DEBUG", "gmem")
                    } else {
                        envVars.put("TU_DEBUG", "sysmem")
                    }
                }
            } else if (graphicsDriver == "virgl") {
                cacheId += "-" + DefaultVersion.VIRGL
            } else if (graphicsDriver == "vortek" || graphicsDriver == "adreno" || graphicsDriver == "sd-8-elite") {
                cacheId += "-" + DefaultVersion.VORTEK
            }

            val imageFs = ImageFs.find(context)
            val configDir = imageFs.configDir
            val sentinel = File(configDir, ".current_graphics_driver")
            val onDiskId = sentinel.takeIf { it.exists() }?.readText() ?: ""
            val changed = alwaysReextract || cacheId != container.getExtra("graphicsDriver") || cacheId != onDiskId
            Timber.i("Changed is %s will re-extract drivers accordingly.", changed)
            val rootDir = imageFs.rootDir
            envVars.put("vblank_mode", "0")

            if (changed) {
                FileUtils.delete(File(imageFs.lib32Dir, "libvulkan_freedreno.so"))
                FileUtils.delete(File(imageFs.lib64Dir, "libvulkan_freedreno.so"))
                FileUtils.delete(File(imageFs.lib64Dir, "libvulkan_vortek.so"))
                FileUtils.delete(File(imageFs.lib32Dir, "libvulkan_vortek.so"))
                FileUtils.delete(File(imageFs.lib32Dir, "libGL.so.1.7.0"))
                FileUtils.delete(File(imageFs.lib64Dir, "libGL.so.1.7.0"))
                val vulkanICDDir = File(rootDir, "/usr/share/vulkan/icd.d")
                FileUtils.delete(vulkanICDDir)
                vulkanICDDir.mkdirs()
                container.putExtra("graphicsDriver", cacheId)
                container.saveData()
                if (!sentinel.exists()) {
                    sentinel.parentFile?.mkdirs()
                    sentinel.createNewFile()
                }
                sentinel.writeText(cacheId)
            }
            if (dxwrapper.contains("dxvk")) {
                DXVKHelper.setEnvVars(context, dxwrapperConfig, envVars)
            } else if (dxwrapper.contains("vkd3d")) {
                DXVKHelper.setVKD3DEnvVars(context, dxwrapperConfig, envVars)
            }

            if (graphicsDriver == "turnip") {
                envVars.put("GALLIUM_DRIVER", "zink")
                envVars.put("TU_OVERRIDE_HEAP_SIZE", "4096")
                if (!envVars.has("MESA_VK_WSI_PRESENT_MODE")) {
                    envVars.put("MESA_VK_WSI_PRESENT_MODE", "mailbox")
                }
                envVars.put("vblank_mode", "0")

                if (!deviceQueryGateway.isAdreno6xx() && !deviceQueryGateway.isAdreno710720732()) {
                    val userEnvVars = EnvVars(container.envVars)
                    val tuDebug = userEnvVars.get("TU_DEBUG")
                    if (!tuDebug.contains("sysmem")) {
                        userEnvVars.put("TU_DEBUG", (if (!tuDebug.isEmpty()) "$tuDebug," else "") + "sysmem")
                    }
                    container.envVars = userEnvVars.toString()
                }

                if (changed) {
                    TarCompressorUtils.extract(
                        TarCompressorUtils.Type.ZSTD,
                        context.assets,
                        "graphics_driver/turnip-$turnipVersion.tzst",
                        rootDir,
                    )
                    TarCompressorUtils.extract(
                        TarCompressorUtils.Type.ZSTD,
                        context.assets,
                        "graphics_driver/zink-$zinkVersion.tzst",
                        rootDir,
                    )
                }
            } else if (graphicsDriver == "virgl") {
                envVars.put("GALLIUM_DRIVER", "virpipe")
                envVars.put("VIRGL_NO_READBACK", "true")
                envVars.put("VIRGL_SERVER_PATH", imageFs.rootDir.path + UnixSocketConfig.VIRGL_SERVER_PATH)
                envVars.put("MESA_EXTENSION_OVERRIDE", "-GL_EXT_vertex_array_bgra")
                envVars.put("MESA_GL_VERSION_OVERRIDE", "3.1")
                envVars.put("vblank_mode", "0")
                if (changed) {
                    TarCompressorUtils.extract(
                        TarCompressorUtils.Type.ZSTD,
                        context.assets,
                        "graphics_driver/virgl-$virglVersion.tzst",
                        rootDir,
                    )
                }
            } else if (graphicsDriver == "vortek") {
                Timber.i("Setting Vortek env vars")
                envVars.put("GALLIUM_DRIVER", "zink")
                envVars.put("ZINK_CONTEXT_THREADED", "1")
                envVars.put("MESA_GL_VERSION_OVERRIDE", "3.3")
                envVars.put("WINEVKUSEPLACEDADDR", "1")
                envVars.put("VORTEK_SERVER_PATH", imageFs.rootDir.path + UnixSocketConfig.VORTEK_SERVER_PATH)
                Timber.i("dxwrapper is %s", dxwrapper)
                if (dxwrapper.contains("dxvk")) {
                    envVars.put("WINE_D3D_CONFIG", "renderer=gdi")
                }
                if (changed) {
                    TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, context.assets, "graphics_driver/vortek-2.1.tzst", rootDir)
                    TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, context.assets, "graphics_driver/zink-22.2.5.tzst", rootDir)
                }
            } else if (graphicsDriver == "adreno" || graphicsDriver == "sd-8-elite") {
                val assetZip = if (graphicsDriver == "adreno") "Adreno_${adrenoVersion}_adpkg.zip" else "SD8Elite_$sd8EliteVersion.zip"

                val componentRoot = com.winlator.core.GeneralComponents.getComponentDir(
                    com.winlator.core.GeneralComponents.Type.ADRENOTOOLS_DRIVER,
                    context,
                )

                val identifier = FileUtils.readZipManifestNameFromAssets(context, assetZip) ?: assetZip.substringBeforeLast('.')

                val adrenoCacheId = "$graphicsDriver-$identifier"
                val needsExtract = changed || adrenoCacheId != container.getExtra("graphicsDriverAdreno")

                if (needsExtract) {
                    val destinationDir = File(componentRoot.toString())
                    if (destinationDir.isDirectory) {
                        FileUtils.delete(destinationDir)
                    }
                    destinationDir.mkdirs()
                    FileUtils.extractZipFromAssets(context, assetZip, destinationDir)
                    container.putExtra("graphicsDriverAdreno", adrenoCacheId)
                    container.saveData()
                }
                envVars.put("GALLIUM_DRIVER", "zink")
                envVars.put("ZINK_CONTEXT_THREADED", "1")
                envVars.put("MESA_GL_VERSION_OVERRIDE", "3.3")
                envVars.put("WINEVKUSEPLACEDADDR", "1")
                envVars.put("VORTEK_SERVER_PATH", imageFs.rootDir.path + UnixSocketConfig.VORTEK_SERVER_PATH)
                Timber.i("dxwrapper is %s", dxwrapper)
                if (dxwrapper.contains("dxvk")) {
                    envVars.put("WINE_D3D_CONFIG", "renderer=gdi")
                }
                if (changed) {
                    TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, context.assets, "graphics_driver/vortek-2.1.tzst", rootDir)
                    TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, context.assets, "graphics_driver/zink-22.2.5.tzst", rootDir)
                }
            }
        } else {
            var adrenoToolsDriverId: String? = null
            val selectedDriverVersion: String?
            val graphicsDriverConfig = KeyValueSet(container.graphicsDriverConfig)
            val imageFs = ImageFs.find(context)

            val currentWrapperVersion: String? = graphicsDriverConfig.get("version", DefaultVersion.WRAPPER)
            val isAdrenotoolsTurnip: String? = graphicsDriverConfig.get("adrenotoolsTurnip", "1")

            selectedDriverVersion = currentWrapperVersion

            adrenoToolsDriverId =
                if (selectedDriverVersion!!.contains(DefaultVersion.WRAPPER)) DefaultVersion.WRAPPER else selectedDriverVersion
            Timber.tag("GraphicsDriverExtraction").d("Adrenotools DriverID: %s", adrenoToolsDriverId)

            val rootDir: File? = imageFs.rootDir

            if (dxwrapper.contains("dxvk")) {
                DXVKHelper.setEnvVars(context, dxwrapperConfig, envVars)
                val version = dxwrapperConfig.get("version")
                if (version == "1.11.1-sarek") {
                    Timber.tag("GraphicsDriverExtraction").d("Disabling Wrapper PATCH_OPCONSTCOMP SPIR-V pass")
                    envVars.put("WRAPPER_NO_PATCH_OPCONSTCOMP", "1")
                }
            } else if (dxwrapper.contains("vkd3d")) {
                DXVKHelper.setVKD3DEnvVars(context, dxwrapperConfig, envVars)
            }

            val useDRI3: Boolean = container.isUseDRI3
            if (!useDRI3) {
                envVars.put("MESA_VK_WSI_DEBUG", "sw")
            }

            if (currentWrapperVersion.lowercase(Locale.getDefault())
                    .contains("turnip") &&
                isAdrenotoolsTurnip == "0"
            ) {
                envVars.put("VK_ICD_FILENAMES", imageFs.shareDir.path + "/vulkan/icd.d/freedreno_icd.aarch64.json")
            } else {
                envVars.put("VK_ICD_FILENAMES", imageFs.shareDir.path + "/vulkan/icd.d/wrapper_icd.aarch64.json")
            }
            envVars.put("GALLIUM_DRIVER", "zink")
            envVars.put("LIBGL_KOPPER_DISABLE", "true")

            val mainWrapperSelection: String = graphicsDriver
            val lastInstalledMainWrapper = container.getExtra("lastInstalledMainWrapper")

            if (alwaysReextract || firstTimeBoot || mainWrapperSelection != lastInstalledMainWrapper) {
                if (mainWrapperSelection.lowercase(Locale.getDefault()).startsWith("wrapper")) {
                    val assetPath = "graphics_driver/" + mainWrapperSelection.lowercase(Locale.getDefault()) + ".tzst"
                    Timber.tag("GraphicsDriverExtraction").d(
                        "WRAPPER selection changed or first boot. Extracting: %s",
                        assetPath,
                    )
                    val success: Boolean = TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, context.assets, assetPath, rootDir)
                    if (success) {
                        container.putExtra("lastInstalledMainWrapper", mainWrapperSelection)
                        container.saveData()
                    }
                    Timber.tag("XServerDisplayActivity").d("First time container boot, extracting extra_libs.tzst")
                    TarCompressorUtils.extract(
                        TarCompressorUtils.Type.ZSTD,
                        context.assets,
                        "graphics_driver/extra_libs.tzst",
                        rootDir,
                    )
                    val renderer = deviceQueryGateway.getActiveDriverRenderer()
                    if (container.wineVersion.contains("arm64ec") && renderer?.contains("Mali") != true) {
                        TarCompressorUtils.extract(
                            TarCompressorUtils.Type.ZSTD,
                            context.assets,
                            "graphics_driver/zink_dlls.tzst",
                            File(rootDir, ImageFs.WINEPREFIX + "/drive_c/windows"),
                        )
                    }
                }
            }

            if (adrenoToolsDriverId !== "System") {
                val adrenotoolsManager = AdrenotoolsManager(context)
                adrenotoolsManager.setDriverById(envVars, imageFs, adrenoToolsDriverId)
            }

            var vulkanVersion = graphicsDriverConfig.get("vulkanVersion") ?: "1.0"
            val vulkanVersionPatch = GPUHelper.vkVersionPatch()

            vulkanVersion = "$vulkanVersion.$vulkanVersionPatch"
            envVars.put("WRAPPER_VK_VERSION", vulkanVersion)

            val blacklistedExtensions: String? = graphicsDriverConfig.get("blacklistedExtensions")
            envVars.put("WRAPPER_EXTENSION_BLACKLIST", blacklistedExtensions)

            val gpuName = graphicsDriverConfig.get("gpuName")
            if (gpuName != "Device") {
                envVars.put("WRAPPER_DEVICE_NAME", gpuName)
                envVars.put("WRAPPER_DEVICE_ID", deviceQueryGateway.getDeviceIdFromGpuName(gpuName))
                envVars.put("WRAPPER_VENDOR_ID", deviceQueryGateway.getVendorIdFromGpuName(gpuName))
            }

            val maxDeviceMemory: String? = graphicsDriverConfig.get("maxDeviceMemory", "0")
            if (maxDeviceMemory != null && maxDeviceMemory.toInt() > 0) {
                envVars.put("WRAPPER_VMEM_MAX_SIZE", maxDeviceMemory)
            }

            val presentMode = graphicsDriverConfig.get("presentMode")
            if (presentMode.contains("immediate")) {
                envVars.put("WRAPPER_MAX_IMAGE_COUNT", "1")
            }
            envVars.put("MESA_VK_WSI_PRESENT_MODE", presentMode)

            val resourceType = graphicsDriverConfig.get("resourceType")
            envVars.put("WRAPPER_RESOURCE_TYPE", resourceType)

            val syncFrame = graphicsDriverConfig.get("syncFrame")
            if (syncFrame == "1") {
                envVars.put("MESA_VK_WSI_DEBUG", "forcesync")
            }

            val disablePresentWait = graphicsDriverConfig.get("disablePresentWait")
            envVars.put("WRAPPER_DISABLE_PRESENT_WAIT", disablePresentWait)

            val bcnEmulation = graphicsDriverConfig.get("bcnEmulation")
            val bcnEmulationType = graphicsDriverConfig.get("bcnEmulationType")
            when (bcnEmulation) {
                "auto" -> {
                    if (bcnEmulationType.equals("compute") && deviceQueryGateway.getActiveDriverVendorId() != 0x5143) {
                        envVars.put("ENABLE_BCN_COMPUTE", "1")
                        envVars.put("BCN_COMPUTE_AUTO", "1")
                    }
                    envVars.put("WRAPPER_EMULATE_BCN", "3")
                }

                "full" -> {
                    if (bcnEmulationType.equals("compute") && deviceQueryGateway.getActiveDriverVendorId() != 0x5143) {
                        envVars.put("ENABLE_BCN_COMPUTE", "1")
                        envVars.put("BCN_COMPUTE_AUTO", "0")
                    }
                    envVars.put("WRAPPER_EMULATE_BCN", "2")
                }

                "none" -> envVars.put("WRAPPER_EMULATE_BCN", "0")

                else -> envVars.put("WRAPPER_EMULATE_BCN", "1")
            }

            val bcnEmulationCache = graphicsDriverConfig.get("bcnEmulationCache")
            envVars.put("WRAPPER_USE_BCN_CACHE", bcnEmulationCache)

            if (!vkbasaltConfig.isEmpty()) {
                envVars.put("ENABLE_VKBASALT", "1")
                envVars.put("VKBASALT_CONFIG", vkbasaltConfig)
            }
        }
    }
}
