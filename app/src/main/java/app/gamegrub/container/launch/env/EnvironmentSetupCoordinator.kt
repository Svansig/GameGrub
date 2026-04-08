package app.gamegrub.container.launch.env

import android.content.Context
import androidx.compose.runtime.MutableState
import app.gamegrub.GameGrubApp
import app.gamegrub.PrefManager
import app.gamegrub.container.launch.manager.ContainerLaunchManager
import app.gamegrub.container.launch.preinstall.PreInstallChainExecutor
import app.gamegrub.container.launch.preinstall.PreInstallSteps
import app.gamegrub.container.launch.unpack.UnpackExecutionCoordinator
import app.gamegrub.data.GameSource
import app.gamegrub.data.LaunchInfo
import app.gamegrub.events.AndroidEvent
import app.gamegrub.gamefixes.GameFixesRegistry
import app.gamegrub.service.steam.AchievementWatcher
import app.gamegrub.service.steam.SteamService
import app.gamegrub.service.steam.managers.SteamSessionContext
import app.gamegrub.ui.data.XServerState
import app.gamegrub.launch.ActiveSessionStore
import app.gamegrub.utils.container.ContainerUtils
import com.winlator.PrefManager as WinlatorPrefManager
import com.winlator.alsaserver.ALSAClient
import com.winlator.container.Container
import com.winlator.contents.ContentsManager
import com.winlator.core.FileUtils
import com.winlator.core.KeyValueSet
import com.winlator.core.ProcessHelper
import com.winlator.fexcore.FEXCoreManager
import com.winlator.winhandler.WinHandler.PreferredInputApi
import com.winlator.xconnector.UnixSocketConfig
import com.winlator.xenvironment.ImageFs
import com.winlator.xenvironment.XEnvironment
import com.winlator.xenvironment.components.ALSAServerComponent
import com.winlator.xenvironment.components.BionicProgramLauncherComponent
import com.winlator.xenvironment.components.GlibcProgramLauncherComponent
import com.winlator.xenvironment.components.GuestProgramLauncherComponent
import com.winlator.xenvironment.components.NetworkInfoUpdateComponent
import com.winlator.xenvironment.components.PulseAudioComponent
import com.winlator.xenvironment.components.SteamClientComponent
import com.winlator.xenvironment.components.SysVSharedMemoryComponent
import com.winlator.xenvironment.components.VirGLRendererComponent
import com.winlator.xenvironment.components.VortekRendererComponent
import com.winlator.xenvironment.components.WineRequestComponent
import com.winlator.xenvironment.components.XServerComponent
import com.winlator.xserver.XServer
import java.io.File
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Builds and starts the runtime XEnvironment used to launch a game container.
 *
 * This coordinator intentionally preserves side effects from the original
 * XServerScreen flow (env var mutation, component setup, splash text updates,
 * telemetry-related state, and post-launch watchers).
 */
internal object EnvironmentSetupCoordinator {
    fun setupXEnvironment(
        context: Context,
        appId: String,
        bootToContainer: Boolean,
        testGraphics: Boolean,
        launchManager: ContainerLaunchManager,
        xServerState: MutableState<XServerState>,
        envVars: com.winlator.core.envvars.EnvVars,
        container: Container?,
        appLaunchInfo: LaunchInfo?,
        xServer: XServer,
        containerVariantChanged: Boolean,
        onGameLaunchError: ((String) -> Unit)? = null,
        navigateBack: () -> Unit,
    ): XEnvironment {
        val gameSource = ContainerUtils.extractGameSourceFromContainerId(appId)
        val nonNullContainer = container ?: throw IllegalArgumentException("Container is required for XEnvironment setup")
        val lcAll = nonNullContainer.lC_ALL
        val imageFs = ImageFs.find(context)
        Timber.i("ImageFs paths:")
        Timber.i("- rootDir: ${imageFs.rootDir.absolutePath}")
        Timber.i("- winePath: ${imageFs.winePath}")
        Timber.i("- home_path: ${imageFs.home_path}")
        Timber.i("- wineprefix: ${imageFs.wineprefix}")

        val contentsManager = ContentsManager(context)
        contentsManager.syncContents()
        envVars.put("LC_ALL", lcAll)
        envVars.put("MESA_DEBUG", "silent")
        envVars.put("MESA_NO_ERROR", "1")
        envVars.put("WINEPREFIX", imageFs.wineprefix)

        if (nonNullContainer.isSdlControllerAPI) {
            when (nonNullContainer.inputType) {
                PreferredInputApi.XINPUT.ordinal, PreferredInputApi.AUTO.ordinal,
                -> {
                    envVars.put("SDL_XINPUT_ENABLED", "1")
                    envVars.put("SDL_DIRECTINPUT_ENABLED", "0")
                    envVars.put("SDL_JOYSTICK_HIDAPI", "1")
                }

                PreferredInputApi.DINPUT.ordinal -> {
                    envVars.put("SDL_XINPUT_ENABLED", "0")
                    envVars.put("SDL_DIRECTINPUT_ENABLED", "1")
                    envVars.put("SDL_JOYSTICK_HIDAPI", "0")
                }

                PreferredInputApi.BOTH.ordinal -> {
                    envVars.put("SDL_XINPUT_ENABLED", "1")
                    envVars.put("SDL_DIRECTINPUT_ENABLED", "1")
                    envVars.put("SDL_JOYSTICK_HIDAPI", "1")
                }
            }
            envVars.put("SDL_JOYSTICK_WGI", "0")
            envVars.put("SDL_JOYSTICK_RAWINPUT", "0")
            envVars.put("SDL_JOYSTICK_ALLOW_BACKGROUND_EVENTS", "1")
            envVars.put("SDL_HINT_FORCE_RAISEWINDOW", "0")
            envVars.put("SDL_ALLOW_TOPMOST", "0")
            envVars.put("SDL_MOUSE_FOCUS_CLICKTHROUGH", "1")
        }

        ProcessHelper.removeAllDebugCallbacks()
        val enableWineDebug = PrefManager.enableWineDebug
        val enableBox86Logs = WinlatorPrefManager.getBoolean("enable_box86_64_logs", false)
        val wineDebugChannels = PrefManager.wineDebugChannels
        envVars.put(
            "WINEDEBUG",
            if (enableWineDebug && wineDebugChannels.isNotEmpty()) {
                "+" + wineDebugChannels.replace(",", ",+")
            } else {
                "-all"
            },
        )

        var logFile: File? = null
        val captureLogs = enableWineDebug || enableBox86Logs
        if (captureLogs) {
            val wineLogDir = File(context.getExternalFilesDir(null), "wine_logs")
            wineLogDir.mkdirs()
            logFile = File(wineLogDir, "wine_debug.log")
            if (logFile.exists()) {
                logFile.delete()
            }
        }

        ProcessHelper.addDebugCallback { line ->
            if (captureLogs) {
                logFile?.appendText(line + "\n")
            }
        }

        val rootPath = imageFs.rootDir.path
        FileUtils.clear(imageFs.tmpDir)

        val useGlibc = nonNullContainer.containerVariant.equals(Container.GLIBC, ignoreCase = true)
        val guestProgramLauncherComponent: GuestProgramLauncherComponent = if (useGlibc) {
            Timber.i("Setting guestProgramLauncherComponent to GlibcProgramLauncherComponent")
            GlibcProgramLauncherComponent(
                contentsManager,
                contentsManager.getProfileByEntryName(nonNullContainer.wineVersion),
            )
        } else {
            Timber.i("Setting guestProgramLauncherComponent to BionicProgramLauncherComponent")
            BionicProgramLauncherComponent(
                contentsManager,
                contentsManager.getProfileByEntryName(nonNullContainer.wineVersion),
            )
        }

        var preInstallCommands: List<PreInstallSteps.PreInstallCommand> = emptyList()
        var gameExecutable = ""

        try {
            GameFixesRegistry.applyFor(context, appId, nonNullContainer)
        } catch (e: Exception) {
            Timber.tag("GameFixes").w(e, "Game fixes failed before launch")
        }

        if (nonNullContainer.startupSelection == Container.STARTUP_SELECTION_AGGRESSIVE) {
            if (nonNullContainer.containerVariant.equals(Container.BIONIC)) {
                Timber.d("Incorrect startup selection detected. Reverting to essential startup selection")
                nonNullContainer.startupSelection = Container.STARTUP_SELECTION_ESSENTIAL
                nonNullContainer.putExtra("startupSelection", Container.STARTUP_SELECTION_ESSENTIAL.toString())
                nonNullContainer.saveData()
            } else {
                xServer.winHandler.killProcess("services.exe")
            }
        }

        val wow64Mode = nonNullContainer.isWoW64Mode
        guestProgramLauncherComponent.container = nonNullContainer
        guestProgramLauncherComponent.wineInfo = xServerState.value.wineInfo
        gameExecutable =
            "wine explorer /desktop=shell," + xServer.screenInfo + " " +
            launchManager.buildWineStartCommand(
                context = context,
                appId = appId,
                container = nonNullContainer,
                bootToContainer = bootToContainer,
                testGraphics = testGraphics,
                appLaunchInfo = appLaunchInfo,
                envVars = envVars,
                guestProgramLauncherComponent = guestProgramLauncherComponent,
                gameSource = gameSource,
            ) +
            (if (nonNullContainer.execArgs.isNotEmpty()) " " + nonNullContainer.execArgs else "")

        preInstallCommands = PreInstallSteps.getPreInstallCommands(
            nonNullContainer,
            appId,
            gameSource,
            xServer.screenInfo.toString(),
            containerVariantChanged,
        )

        guestProgramLauncherComponent.guestExecutable = preInstallCommands.firstOrNull()?.executable ?: gameExecutable
        guestProgramLauncherComponent.isWoW64Mode = wow64Mode
        guestProgramLauncherComponent.setSteamType(nonNullContainer.steamType)

        envVars.putAll(nonNullContainer.envVars)
        applySessionEnvPlan(envVars)
        if (!envVars.has("WINEESYNC")) {
            envVars.put("WINEESYNC", "1")
        }

        val graphicsDriverConfig = KeyValueSet(nonNullContainer.graphicsDriverConfig)
        if (graphicsDriverConfig.get("version").lowercase(Locale.getDefault()).contains("gen8")) {
            var tuDebug = envVars.get("TU_DEBUG")
            if (!tuDebug.contains("nolrz")) {
                tuDebug = (if (!tuDebug.isEmpty()) "$tuDebug," else "") + "nolrz"
            }
            envVars.put("TU_DEBUG", tuDebug)
        }

        val bindingPaths = mutableListOf<String>()
        for (drive in nonNullContainer.drivesIterator()) {
            Timber.i("Binding drive ${drive[0]} with path of ${drive[1]}")
            bindingPaths.add(drive[1])
        }

        guestProgramLauncherComponent.bindingPaths = bindingPaths.toTypedArray()
        guestProgramLauncherComponent.box64Version = nonNullContainer.box64Version
        guestProgramLauncherComponent.box86Version = nonNullContainer.box86Version
        guestProgramLauncherComponent.box86Preset = nonNullContainer.box86Preset
        guestProgramLauncherComponent.box64Preset = nonNullContainer.box64Preset

        if (guestProgramLauncherComponent is BionicProgramLauncherComponent) {
            guestProgramLauncherComponent.setFEXCorePreset(nonNullContainer.fexCorePreset)
        }

        guestProgramLauncherComponent.setPreUnpack {
            UnpackExecutionCoordinator.unpackExecutableFile(
                context = context,
                needsUnpacking = nonNullContainer.isNeedsUnpacking,
                container = nonNullContainer,
                appId = appId,
                guestProgramLauncherComponent = guestProgramLauncherComponent,
                containerVariantChanged = containerVariantChanged,
                onError = onGameLaunchError,
            )
            if (preInstallCommands.isNotEmpty()) {
                GameGrubApp.events.emit(AndroidEvent.SetBootingSplashText("Installing prerequisites..."))
            } else {
                GameGrubApp.events.emit(AndroidEvent.SetBootingSplashText("Launching game..."))
            }
        }

        if (nonNullContainer.isGstreamerWorkaround) {
            for (envVar in Container.MEDIACONV_ENV_VARS) {
                val parts = envVar.split("=", limit = 2)
                if (parts.size == 2) {
                    envVars.put(parts[0], parts[1])
                }
            }
        }

        val environment = XEnvironment(context, imageFs)
        environment.addComponent(
            SysVSharedMemoryComponent(
                xServer,
                UnixSocketConfig.createSocket(rootPath, UnixSocketConfig.SYSVSHM_SERVER_PATH),
            ),
        )
        environment.addComponent(
            XServerComponent(
                xServer,
                UnixSocketConfig.createSocket(rootPath, UnixSocketConfig.XSERVER_PATH),
            ),
        )
        environment.addComponent(NetworkInfoUpdateComponent())

        if (!nonNullContainer.isLaunchRealSteam) {
            environment.addComponent(SteamClientComponent())
        }

        if (xServerState.value.audioDriver == "alsa") {
            envVars.put("ANDROID_ALSA_SERVER", imageFs.rootDir.path + UnixSocketConfig.ALSA_SERVER_PATH)
            envVars.put("ANDROID_ASERVER_USE_SHM", "true")
            val options = ALSAClient.Options.fromKeyValueSet(null)
            environment.addComponent(
                ALSAServerComponent(
                    UnixSocketConfig.createSocket(imageFs.rootDir.path, UnixSocketConfig.ALSA_SERVER_PATH),
                    options,
                ),
            )
        } else if (xServerState.value.audioDriver == "pulseaudio") {
            envVars.put("PULSE_SERVER", imageFs.rootDir.path + UnixSocketConfig.PULSE_SERVER_PATH)
            environment.addComponent(
                PulseAudioComponent(
                    UnixSocketConfig.createSocket(imageFs.rootDir.path, UnixSocketConfig.PULSE_SERVER_PATH),
                ),
            )
        }

        if (xServerState.value.graphicsDriver == "virgl") {
            environment.addComponent(
                VirGLRendererComponent(
                    xServer,
                    UnixSocketConfig.createSocket(rootPath, UnixSocketConfig.VIRGL_SERVER_PATH),
                ),
            )
        } else if (
            xServerState.value.graphicsDriver == "vortek" ||
            xServerState.value.graphicsDriver == "adreno" ||
            xServerState.value.graphicsDriver == "sd-8-elite"
        ) {
            Timber.i("Adding VortekRendererComponent to Environment")
            val gcfg = KeyValueSet(nonNullContainer.graphicsDriverConfig)
            val graphicsDriver = xServerState.value.graphicsDriver
            if (graphicsDriver == "sd-8-elite" || graphicsDriver == "adreno") {
                gcfg.put("adrenotoolsDriver", "vulkan.adreno.so")
                nonNullContainer.graphicsDriverConfig = gcfg.toString()
            }
            val options = VortekRendererComponent.Options.fromKeyValueSet(context, gcfg)
            environment.addComponent(
                VortekRendererComponent(
                    xServer,
                    UnixSocketConfig.createSocket(rootPath, UnixSocketConfig.VORTEK_SERVER_PATH),
                    options,
                    context,
                ),
            )
        }

        guestProgramLauncherComponent.envVars = envVars

        PreInstallChainExecutor.configure(
            container = nonNullContainer,
            preInstallCommands = preInstallCommands,
            gameExecutable = gameExecutable,
            guestProgramLauncherComponent = guestProgramLauncherComponent,
            onGameLaunchError = onGameLaunchError,
        )

        environment.addComponent(guestProgramLauncherComponent)
        environment.addComponent(WineRequestComponent())

        FEXCoreManager.ensureAppConfigOverrides(context)

        if (nonNullContainer.isLaunchRealSteam) {
            val steamService = SteamService.instance
                ?: throw IllegalStateException("SteamService must be running before launching real Steam")
            val session = SteamSessionContext(
                steamId64 = SteamService.getSteamId64()?.toString() ?: PrefManager.steamUserSteamId64.toString(),
                account = PrefManager.username,
                refreshToken = PrefManager.refreshToken,
                accessToken = PrefManager.accessToken,
                personaName = steamService.localPersona.value.name.ifBlank { PrefManager.username },
            )
            steamService.sessionDomain.setupRealSteamSessionFiles(
                session = session,
                imageFs = imageFs,
                guestProgramLauncherComponent = guestProgramLauncherComponent,
            )
        }

        Timber.i("---- Launching Container ----")
        Timber.i("ID: ${nonNullContainer.id}")
        Timber.i("Name: ${nonNullContainer.name}")
        Timber.i("Screen Size: ${nonNullContainer.screenSize}")
        Timber.i("Graphics Driver: ${nonNullContainer.graphicsDriver}")
        Timber.i("DX Wrapper: ${nonNullContainer.dxWrapper} (Config: '${nonNullContainer.dxWrapperConfig}')")
        Timber.i("Audio Driver: ${nonNullContainer.audioDriver}")
        Timber.i("WoW64 Mode: ${nonNullContainer.isWoW64Mode}")
        Timber.i("Box64 Version: ${nonNullContainer.box64Version}")
        Timber.i("Box64 Preset: ${nonNullContainer.box64Preset}")
        Timber.i("Box86 Version: ${nonNullContainer.box86Version}")
        Timber.i("Box86 Preset: ${nonNullContainer.box86Preset}")
        Timber.i("FEXCore Preset: ${nonNullContainer.fexCorePreset}")
        Timber.i("CPU List: ${nonNullContainer.cpuList}")
        Timber.i("CPU List WoW64: ${nonNullContainer.cpuListWoW64}")
        Timber.i("Env Vars (Container Base): ${nonNullContainer.envVars}")
        Timber.i("Env Vars (Final Guest): $envVars")
        Timber.i("Guest Executable: ${guestProgramLauncherComponent.guestExecutable}")
        Timber.i("---------------------------")

        val isCustomGame = gameSource == GameSource.CUSTOM_GAME
        val gameIdForTicket = ContainerUtils.extractGameIdFromContainerId(appId)
        if (!bootToContainer && !isCustomGame && !nonNullContainer.isLaunchRealSteam) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val ticket = SteamService.instance?.getEncryptedAppTicket(gameIdForTicket)
                    if (ticket != null) {
                        Timber.i("Successfully retrieved encrypted app ticket for app $gameIdForTicket")
                    } else {
                        Timber.w("Failed to retrieve encrypted app ticket for app $gameIdForTicket")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error requesting encrypted app ticket for app $gameIdForTicket")
                }
            }
        }

        try {
            environment.startEnvironmentComponents()
        } catch (e: Exception) {
            Timber.e(e, "Failed to start environment components, cleaning up")
            ActiveSessionStore.clearActiveSession()
            try {
                environment.stopEnvironmentComponents()
            } catch (cleanupEx: Exception) {
                Timber.e(cleanupEx, "Error during environment cleanup")
            }
            throw e
        }
        ActiveSessionStore.clearActiveSession()

        if (gameSource == GameSource.STEAM) {
            val gameIdInt = ContainerUtils.extractGameIdFromContainerId(appId)
            val cloudStats = SteamService.instance?.cloudStatsDomain
            val achAppId = cloudStats?.cachedAchievementsAppId?.value
            if (achAppId != null) {
                val watchDirs = SteamService.getGseSaveDirs(context, gameIdInt)
                val displayNameMap = cloudStats.cachedAchievements.value?.associate { ach ->
                    ach.name to (
                        ach.displayName?.get(nonNullContainer.language)
                            ?: ach.displayName?.get("english")
                            ?: ach.name
                        )
                } ?: emptyMap()
                val iconUrlMap = cloudStats.cachedAchievements.value?.associate { ach ->
                    ach.name to ach.icon?.let {
                        "https://steamcdn-a.akamaihd.net/steamcommunity/public/images/apps/$achAppId/$it"
                    }
                } ?: emptyMap()
                GameGrubApp.achievementWatcher = AchievementWatcher(watchDirs, displayNameMap, iconUrlMap)
                    .also { it.start() }
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            xServer.winHandler.start()
        }

        envVars.clear()
        xServerState.value = xServerState.value.copy(dxwrapperConfig = null)
        return environment
    }

    /**
     * Keys managed exclusively by existing EnvironmentSetupCoordinator logic.
     *
     * These are excluded from session plan application to avoid conflicting with
     * the legacy ImageFs and container-config-driven env var management. Remove
     * entries from this list as the corresponding paths are migrated to SessionPlan.
     */
    private val SESSION_ENV_DENYLIST = setOf(
        "WINEPREFIX",   // managed by imageFs.wineprefix — migration in progress
        "WINEDEBUG",    // managed by PrefManager.enableWineDebug
        "LC_ALL",       // managed by container.lC_ALL
        "MESA_DEBUG",   // hardcoded
        "MESA_NO_ERROR", // hardcoded
    )

    /**
     * Applies non-conflicting env vars from the active session plan to [envVars].
     *
     * Called after container-base env vars are applied so that session plan vars
     * (cache paths, driver hints, etc.) can supplement or override container defaults.
     * Keys in [SESSION_ENV_DENYLIST] are skipped to preserve legacy behavior during
     * the ImageFs-to-SessionPlan migration.
     */
    private fun applySessionEnvPlan(envVars: com.winlator.core.envvars.EnvVars) {
        val envPlan = ActiveSessionStore.getActiveSession()?.envPlan ?: return
        var applied = 0
        for ((key, value) in envPlan.environmentVariables) {
            if (key !in SESSION_ENV_DENYLIST && value.isNotEmpty()) {
                envVars.put(key, value)
                applied++
            }
        }
        if (applied > 0) Timber.d("SessionEnvPlan: applied $applied env var(s) from active session")
    }
}
