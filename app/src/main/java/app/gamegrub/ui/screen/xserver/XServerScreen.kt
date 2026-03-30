package app.gamegrub.ui.screen.xserver

import android.content.Context
import android.graphics.Color
import android.hardware.input.InputManager
import android.view.Gravity
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.gamegrub.GameGrubApp
import app.gamegrub.PrefManager
import app.gamegrub.R
import app.gamegrub.container.launch.env.EnvironmentSetupCoordinator
import app.gamegrub.container.launch.manager.ContainerLaunchManager
import app.gamegrub.container.launch.manager.ContainerLaunchManagerFactory
import app.gamegrub.container.launch.prep.LaunchPreparationCoordinator
import app.gamegrub.container.manager.ContainerRuntimeManagerFactory
import app.gamegrub.data.LaunchInfo
import app.gamegrub.events.AndroidEvent
import app.gamegrub.externaldisplay.ExternalDisplayInputController
import app.gamegrub.externaldisplay.ExternalDisplaySwapController
import app.gamegrub.externaldisplay.SwapInputOverlayView
import app.gamegrub.service.steam.SteamService
import app.gamegrub.ui.component.QuickMenu
import app.gamegrub.ui.data.PerformanceHudConfig
import app.gamegrub.ui.data.XServerState
import app.gamegrub.ui.enums.Orientation
import app.gamegrub.ui.utils.SnackbarManager
import app.gamegrub.ui.widget.PerformanceHudView
import app.gamegrub.utils.container.ContainerUtils
import com.winlator.container.Container
import com.winlator.contents.ContentsManager
import com.winlator.core.Callback
import com.winlator.core.DXVKHelper
import com.winlator.core.OnExtractFileListener
import com.winlator.core.ProcessHelper
import com.winlator.core.Win32AppWorkarounds
import com.winlator.core.WineInfo
import com.winlator.core.envvars.EnvVars
import com.winlator.inputcontrols.ControllerManager
import com.winlator.inputcontrols.ControlsProfile
import com.winlator.inputcontrols.ExternalController
import com.winlator.inputcontrols.InputControlsManager
import com.winlator.widget.FrameRating
import com.winlator.widget.InputControlsView
import com.winlator.widget.TouchpadView
import com.winlator.widget.XServerView
import com.winlator.winhandler.WinHandler
import com.winlator.winhandler.WinHandler.PreferredInputApi
import com.winlator.xenvironment.ImageFs
import com.winlator.xenvironment.XEnvironment
import com.winlator.xenvironment.components.XServerComponent
import com.winlator.xserver.Keyboard
import com.winlator.xserver.ScreenInfo
import com.winlator.xserver.Window
import com.winlator.xserver.XServer
import java.io.File
import java.nio.file.Paths
import java.util.EnumSet
import java.util.Locale
import kotlin.io.path.name
import kotlinx.coroutines.Job
import timber.log.Timber

// Always re-extract drivers and DXVK on every launch to handle cases of container corruption
// where games randomly stop working. Set to false once corruption issues are resolved.
private const val ALWAYS_REEXTRACT = true

private const val EXIT_PROCESS_TIMEOUT_MS = 30_000L
private const val EXIT_PROCESS_POLL_INTERVAL_MS = 1_000L
private const val EXIT_PROCESS_RESPONSE_TIMEOUT_MS = 2_000L

private data class XServerViewReleaseBinding(
    val xServerView: XServerView,
    val windowLifecycleCoordinator: XServerWindowLifecycleCoordinator,
)

// TODO logs in composables are 'unstable' which can cause recomposition (performance issues)

@Composable
@OptIn(ExperimentalComposeUiApi::class)
fun XServerScreen(
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    appId: String,
    bootToContainer: Boolean,
    testGraphics: Boolean = false,
    registerBackAction: (() -> Unit) -> Unit,
    navigateBack: () -> Unit,
    onExit: (onComplete: (() -> Unit)?) -> Unit,
    onWindowMapped: ((Context, Window) -> Unit)? = null,
    onWindowUnmapped: ((Window) -> Unit)? = null,
    onGameLaunchError: ((String) -> Unit)? = null,
) {
    Timber.i("Starting up XServerScreen")
    val context = LocalContext.current
    val view = LocalView.current
    val imm = remember(context) {
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }

    // GameGrubApp.events.emit(AndroidEvent.SetAppBarVisibility(false))
    GameGrubApp.events.emit(AndroidEvent.SetSystemUIVisibility(false))

    // seems to be used to indicate when a custom wine is being installed (intent extra "generate_wineprefix")
    // val generateWinePrefix = false
    var firstTimeBoot = false
    var containerVariantChanged = false
    var frameRating by remember { mutableStateOf<FrameRating?>(null) }
    var vkbasaltConfig = ""
    var taskAffinityMask = 0
    var taskAffinityMaskWoW64 = 0

    LaunchedEffect(appId) {
        XServerExitCoordinator.resetExitGuard()
    }

    val container = remember(appId) {
        ContainerUtils.getContainer(context, appId)
    }

    val suspendPolicy = remember(container.id) { container.suspendPolicy }
    val neverSuspend = suspendPolicy.equals(Container.SUSPEND_POLICY_NEVER, ignoreCase = true)
    val manualResumeMode = suspendPolicy.equals(Container.SUSPEND_POLICY_MANUAL, ignoreCase = true)

    SideEffect {
        GameGrubApp.setActiveSuspendPolicy(suspendPolicy)
    }

    GameGrubApp.events.emit(
        AndroidEvent.SetAllowedOrientation(
            if (container.isPortraitMode) {
                EnumSet.of(Orientation.PORTRAIT)
            } else {
                PrefManager.allowedOrientation
            },
        ),
    )

    val xServerState = rememberSaveable(stateSaver = XServerState.Saver) {
        mutableStateOf(
            XServerState(
                graphicsDriver = container.graphicsDriver,
                graphicsDriverVersion = container.graphicsDriverVersion,
                audioDriver = container.audioDriver,
                dxwrapper = container.dxWrapper,
                dxwrapperConfig = DXVKHelper.parseConfig(container.dxWrapperConfig),
                screenSize = container.screenSize,
            ),
        )
    }

    // val xServer by remember {
    //     val result = mutableStateOf(XServer(ScreenInfo(xServerState.value.screenSize)))
    //     Log.d("XServerScreen", "Remembering xServer as $result")
    //     result
    // }
    // var xEnvironment: XEnvironment? by remember {
    //     val result = mutableStateOf<XEnvironment?>(null)
    //     Log.d("XServerScreen", "Remembering xEnvironment as $result")
    //     result
    // }
    // TouchMouse is initialized on XServer but no longer used as mutable screen state.
    var keyboard by remember { mutableStateOf<Keyboard?>(null) }
    // var pointerEventListener by remember { mutableStateOf<Callback<MotionEvent>?>(null) }

    val gameId = ContainerUtils.extractGameIdFromContainerId(appId)
    val appLaunchInfo = SteamService.getAppInfoOf(gameId)?.let {
        SteamService.getWindowsLaunchInfos(gameId).firstOrNull()
    }

    val currentAppInfo = SteamService.getAppInfoOf(gameId)

    var xServerView: XServerView? by remember {
        val result = mutableStateOf<XServerView?>(null)
        Timber.i("Remembering xServerView as $result")
        result
    }

    var swapInputOverlay: SwapInputOverlayView? by remember { mutableStateOf(null) }
    var imeInputReceiver: app.gamegrub.externaldisplay.IMEInputReceiver? by remember { mutableStateOf(null) }

    var win32AppWorkarounds: Win32AppWorkarounds? by remember { mutableStateOf(null) }
    var physicalControllerHandler: PhysicalControllerHandler? by remember { mutableStateOf(null) }
    var exitWatchJob: Job? by remember { mutableStateOf(null) }
    val currentOnWindowMapped by rememberUpdatedState(onWindowMapped)
    val currentOnWindowUnmapped by rememberUpdatedState(onWindowUnmapped)
    val currentOnExit by rememberUpdatedState(onExit)
    val currentNavigateBack by rememberUpdatedState(navigateBack)
    val exitRequestCoordinator = XServerExitRequestCoordinator(
        winHandlerProvider = { xServerView?.getxServer()?.winHandler },
        environmentProvider = { GameGrubApp.xEnvironment },
        frameRatingProvider = { frameRating },
        appInfoProvider = { currentAppInfo },
        container = container,
        appId = appId,
        onExit = { onComplete -> currentOnExit(onComplete) },
        navigateBack = { currentNavigateBack() },
    )
    val windowLifecycleCoordinator = remember(container.id, appId, context) {
        XServerWindowLifecycleCoordinator(
            context = context,
            container = container,
            appId = appId,
            appInfo = currentAppInfo,
            xServerState = xServerState,
            frameRatingProvider = { frameRating },
            currentExitWatchJobProvider = { exitWatchJob },
            updateExitWatchJob = { exitWatchJob = it },
            onFirstApplicationWindowStarted = {
                if (!container.isDisableMouseInput && !container.isTouchscreenMode) {
                    xServerView?.renderer?.setCursorVisible(true)
                }
            },
            win32AppWorkaroundsProvider = { win32AppWorkarounds },
            onWindowMapped = { window ->
                currentOnWindowMapped?.invoke(context, window)
            },
            onWindowUnmapped = { window ->
                currentOnWindowUnmapped?.invoke(window)
            },
            onExit = { onComplete ->
                currentOnExit(onComplete)
            },
            navigateBack = {
                currentNavigateBack()
            },
            processTimeoutMs = EXIT_PROCESS_TIMEOUT_MS,
            pollIntervalMs = EXIT_PROCESS_POLL_INTERVAL_MS,
            responseTimeoutMs = EXIT_PROCESS_RESPONSE_TIMEOUT_MS,
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            physicalControllerHandler?.cleanup()
            physicalControllerHandler = null
            windowLifecycleCoordinator.cancelActiveExitWatch()
        }
    }
    var areControlsVisible by remember { mutableStateOf(false) }
    var isEditMode by remember { mutableStateOf(false) }
    var gameRoot by remember { mutableStateOf<View?>(null) }
    // Snapshot of element positions before entering edit mode (for cancel behavior)
    var elementPositionsSnapshot by remember { mutableStateOf<Map<com.winlator.inputcontrols.ControlElement, Pair<Int, Int>>>(emptyMap()) }
    var showElementEditor by remember { mutableStateOf(false) }
    var elementToEdit by remember { mutableStateOf<com.winlator.inputcontrols.ControlElement?>(null) }
    var showPhysicalControllerDialog by remember { mutableStateOf(false) }
    var keyboardRequestedFromOverlay by remember { mutableStateOf(false) }
    var showQuickMenu by remember { mutableStateOf(false) }
    var hasPhysicalController by remember { mutableStateOf(false) }
    var keepPausedForEditor by remember { mutableStateOf(false) }
    var hasPhysicalKeyboard by remember { mutableStateOf(false) }
    var hasPhysicalMouse by remember { mutableStateOf(false) }
    var hasInternalTouchpad by remember { mutableStateOf(false) }
    var hasUpdatedScreenGamepad by remember { mutableStateOf(false) }
    var isPerformanceHudEnabled by remember { mutableStateOf(PrefManager.showFps) }

    var performanceHudConfig by remember { mutableStateOf(XServerPerformanceHudConfigStore.load()) }
    var performanceHudView by remember { mutableStateOf<PerformanceHudView?>(null) }
    var performanceHudHost by remember { mutableStateOf<FrameLayout?>(null) }
    var isDraggingPerformanceHud by remember { mutableStateOf(false) }
    var isTrackingPerformanceHudTouch by remember { mutableStateOf(false) }
    var performanceHudTouchDownRawX by remember { mutableStateOf(0f) }
    var performanceHudTouchDownRawY by remember { mutableStateOf(0f) }
    var performanceHudDragOffsetX by remember { mutableStateOf(0f) }
    var performanceHudDragOffsetY by remember { mutableStateOf(0f) }
    val performanceHudTouchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()

    fun applyPerformanceHudConfig(config: PerformanceHudConfig) {
        performanceHudConfig = config
        XServerPerformanceHudConfigStore.persist(config)
        performanceHudView?.setConfig(config)
    }

    fun restorePerformanceHudPosition() {
        val host = performanceHudHost ?: return
        val hud = performanceHudView ?: return
        if (host.width <= 0 || host.height <= 0 || hud.width <= 0 || hud.height <= 0) return

        val maxX = (host.width - hud.width).coerceAtLeast(0).toFloat()
        val maxY = (host.height - hud.height).coerceAtLeast(0).toFloat()
        val margin = 12 * context.resources.displayMetrics.density
        val savedX = PrefManager.performanceHudXFraction
        val savedY = PrefManager.performanceHudYFraction

        hud.x = if (savedX in 0f..1f) maxX * savedX else margin.coerceAtMost(maxX)
        hud.y = if (savedY in 0f..1f) maxY * savedY else margin.coerceAtMost(maxY)

        PrefManager.performanceHudXFraction = if (maxX > 0f) hud.x / maxX else 0f
        PrefManager.performanceHudYFraction = if (maxY > 0f) hud.y / maxY else 0f
    }

    fun movePerformanceHud(rawX: Float, rawY: Float, save: Boolean) {
        val host = performanceHudHost ?: return
        val hud = performanceHudView ?: return
        if (host.width <= 0 || host.height <= 0 || hud.width <= 0 || hud.height <= 0) return

        val hostLocation = IntArray(2)
        host.getLocationOnScreen(hostLocation)
        val maxX = (host.width - hud.width).coerceAtLeast(0).toFloat()
        val maxY = (host.height - hud.height).coerceAtLeast(0).toFloat()

        hud.x = (rawX - hostLocation[0] - performanceHudDragOffsetX).coerceIn(0f, maxX)
        hud.y = (rawY - hostLocation[1] - performanceHudDragOffsetY).coerceIn(0f, maxY)

        if (save) {
            PrefManager.performanceHudXFraction = if (maxX > 0f) hud.x / maxX else 0f
            PrefManager.performanceHudYFraction = if (maxY > 0f) hud.y / maxY else 0f
        }
    }

    fun removePerformanceHud() {
        isDraggingPerformanceHud = false
        isTrackingPerformanceHudTouch = false
        performanceHudView?.let { hud ->
            (hud.parent as? ViewGroup)?.removeView(hud)
        }
        performanceHudView = null
    }

    fun togglePerformanceHudLayout() {
        val hud = performanceHudView ?: return
        val compactMode = !hud.isCompactMode()
        hud.setCompactMode(compactMode)
        PrefManager.performanceHudCompactMode = compactMode
        hud.post {
            if (performanceHudView === hud && !isDraggingPerformanceHud) {
                restorePerformanceHudPosition()
            }
        }
    }

    fun updatePerformanceHud(show: Boolean) {
        if (!show) {
            removePerformanceHud()
            return
        }
        if (performanceHudView != null) {
            return
        }

        val targetLayout = performanceHudHost ?: return
        val hud = PerformanceHudView(
            context = context,
            fpsProvider = {
                frameRating?.currentFPS ?: 0f
            },
            initialConfig = performanceHudConfig,
            initialCompactMode = PrefManager.performanceHudCompactMode,
        )
        val layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        targetLayout.addView(hud, layoutParams)
        performanceHudView = hud
        hud.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            if (!isDraggingPerformanceHud) restorePerformanceHudPosition()
        }
        targetLayout.post {
            if (performanceHudView === hud) restorePerformanceHudPosition()
        }
        hud.bringToFront()
    }

    fun resumeIfAllowedAfterOverlay() {
        XServerOverlayActionCoordinator.resumeIfAllowedAfterOverlay(
            neverSuspend = neverSuspend,
            manualResumeMode = manualResumeMode,
        )
    }

    fun resumeFromManualButton() {
        XServerOverlayActionCoordinator.resumeFromManualButton(
            neverSuspend = neverSuspend,
            onKeepPausedForEditorChanged = { keepPausedForEditor = it },
        )
    }

    val tryCapturePointer: () -> Boolean = {
        // Only recapture when we have a physical mouse plugged in (or internal touchpad),
        // no menus are open and we're not in Touchscreen mode
        if ((hasPhysicalMouse || hasInternalTouchpad) &&
            !showElementEditor &&
            !keepPausedForEditor &&
            !showQuickMenu &&
            !isEditMode &&
            !container.isTouchscreenMode
        ) {
            GameGrubApp.touchpadView?.postDelayed(
                {
                    val view = GameGrubApp.touchpadView
                    if (view != null) {
                        view.requestFocus()
                        view.requestPointerCapture()
                    }
                },
                100,
            )
            true
        } else {
            false
        }
    }

    fun scanForExternalDevices() {
        val deviceIds = InputDevice.getDeviceIds()
        val detectedPhysicalKeyboard = deviceIds.any { id ->
            val device = InputDevice.getDevice(id) ?: return@any false
            val isExternal = device.isExternal
            Keyboard.isKeyboardDevice(device) && !device.isVirtual && isExternal
        }
        val detectedPhysicalMouse = deviceIds.any { id ->
            val device = InputDevice.getDevice(id) ?: return@any false
            val isMouse = device.supportsSource(InputDevice.SOURCE_MOUSE) ||
                device.supportsSource(InputDevice.SOURCE_MOUSE_RELATIVE) ||
                device.supportsSource(InputDevice.SOURCE_TOUCHPAD)
            val isExternal = device.isExternal
            isMouse && !device.isVirtual && isExternal
        }
        val controllerManager = ControllerManager.getInstance()
        controllerManager.scanForDevices()
        val presence = XServerInputDeviceCoordinator.DevicePresence(
            hasPhysicalKeyboard = detectedPhysicalKeyboard,
            hasPhysicalMouse = detectedPhysicalMouse,
            hasPhysicalController = controllerManager.detectedDevices.isNotEmpty(),
        )
        hasPhysicalKeyboard = presence.hasPhysicalKeyboard
        hasPhysicalMouse = presence.hasPhysicalMouse
        hasPhysicalController = presence.hasPhysicalController

        if (XServerInputDeviceCoordinator.shouldEvaluateAutoShow(
                hasInternalTouchpad = hasInternalTouchpad,
                presence = presence,
                isTouchscreenMode = container.isTouchscreenMode,
            )
        ) {
            val manager = GameGrubApp.inputControlsManager
            val targetProfile = XServerControlsProfileResolver.resolveCurrentOrFallbackProfile(
                manager = manager,
                container = container,
            )

            if (targetProfile != null) {
                if (!showElementEditor && !keepPausedForEditor && !showQuickMenu && !isEditMode) {
                    Timber.d("No external devices attached, showing on-screen controls")
                    if (!areControlsVisible) {
                        XServerInputControlsOverlayHelper.showInputControls(
                            targetProfile,
                            xServerView!!.getxServer().winHandler,
                            container,
                        )
                        areControlsVisible = true
                    }

                    GameGrubApp.touchpadView?.postDelayed(
                        {
                            val view = GameGrubApp.touchpadView
                            if (view != null) {
                                // Delay technically not required for the function to work but this can
                                // race against tryCapturePointer() and end up capturing after release
                                // was already called
                                view.releasePointerCapture()
                            }
                        },
                        100,
                    )
                }
                hasUpdatedScreenGamepad = false
            }
        }
    }

    fun evaluateDevice(device: InputDevice) {
        // Some devices advertise all its capabilities on onInputDeviceAdded callback
        // but some can also do basic advertise on onInputDeviceAdded and only expand on onInputDeviceChanged
        val uiGuardState = XServerInputDeviceCoordinator.UiGuardState(
            showElementEditor = showElementEditor,
            keepPausedForEditor = keepPausedForEditor,
            showQuickMenu = showQuickMenu,
            isEditMode = isEditMode,
            isTouchscreenMode = container.isTouchscreenMode,
            hasUpdatedScreenGamepad = hasUpdatedScreenGamepad,
        )
        val isExternal = device.isExternal
        if (!device.isVirtual && isExternal) {
            if (Keyboard.isKeyboardDevice(device)) {
                hasPhysicalKeyboard = true
                if (XServerInputDeviceCoordinator.shouldHideForExternalKeyboard(uiGuardState)) {
                    hasUpdatedScreenGamepad = true

                    XServerInputControlsOverlayHelper.hideInputControls()
                    areControlsVisible = false
                }
            }
            val isMouse = device.supportsSource(InputDevice.SOURCE_MOUSE) ||
                device.supportsSource(InputDevice.SOURCE_MOUSE_RELATIVE) ||
                device.supportsSource(InputDevice.SOURCE_TOUCHPAD)
            if (isMouse) {
                hasPhysicalMouse = true
                if (XServerInputDeviceCoordinator.shouldHideForExternalMouse(uiGuardState) && tryCapturePointer()) {
                    hasUpdatedScreenGamepad = true

                    XServerInputControlsOverlayHelper.hideInputControls()
                    areControlsVisible = false
                }
            }
        }
        val isGamepad = ExternalController.isGameController(device)
        if (isGamepad) {
            if (XServerInputDeviceCoordinator.shouldHideForExternalGamepad(uiGuardState)) {
                hasUpdatedScreenGamepad = true

                XServerInputControlsOverlayHelper.hideInputControls()
                areControlsVisible = false
            }
        }
    }

    fun openControlsEditor() {
        val manager = GameGrubApp.inputControlsManager ?: InputControlsManager(context)
        val activeProfile = XServerControlsProfileResolver.getOrCreateContainerProfile(
            manager = manager,
            container = container,
            gameName = currentAppInfo?.name ?: container.name,
            suffix = "Controls",
            onProfileCreated = { profile ->
                GameGrubApp.inputControlsView?.profile = profile
            },
        )

        if (activeProfile != null) {
            val profile = GameGrubApp.inputControlsView?.profile
            if (profile != null) {
                val snapshot = mutableMapOf<com.winlator.inputcontrols.ControlElement, Pair<Int, Int>>()
                profile.elements.forEach { element ->
                    snapshot[element] = Pair(element.x.toInt(), element.y.toInt())
                }
                elementPositionsSnapshot = snapshot
            }

            isEditMode = true
            GameGrubApp.inputControlsView?.setEditMode(true)
            GameGrubApp.inputControlsView?.let { icView ->
                icView.post {
                    activeProfile.loadElements(icView)
                }
            }

            if (!areControlsVisible) {
                XServerInputControlsOverlayHelper.showInputControls(
                    activeProfile,
                    xServerView!!.getxServer().winHandler,
                    container,
                )
                areControlsVisible = true
            }
        }
    }

    val dismissOverlayMenu: () -> Unit = {
        XServerOverlayActionCoordinator.dismissOverlayMenu(
            imeInputReceiver = imeInputReceiver,
            keyboardRequestedFromOverlay = keyboardRequestedFromOverlay,
            keepPausedForEditor = keepPausedForEditor,
            manualResumeMode = manualResumeMode,
            neverSuspend = neverSuspend,
            onKeyboardRequestedFromOverlayChanged = { keyboardRequestedFromOverlay = it },
            onShowQuickMenuChanged = { showQuickMenu = it },
        )
    }

    val onQuickMenuItemSelected: (Int) -> Boolean = { itemId ->
        XServerOverlayActionCoordinator.handleQuickMenuAction(
            itemId = itemId,
            anchorView = view,
            imm = imm,
            imeInputReceiver = imeInputReceiver,
            container = container,
            appId = appId,
            xServerView = xServerView,
            areControlsVisible = areControlsVisible,
            onAreControlsVisibleChanged = { areControlsVisible = it },
            isPerformanceHudEnabled = isPerformanceHudEnabled,
            onPerformanceHudEnabledChanged = { isPerformanceHudEnabled = it },
            neverSuspend = neverSuspend,
            onKeepPausedForEditorChanged = { keepPausedForEditor = it },
            onKeyboardRequestedFromOverlayChanged = { keyboardRequestedFromOverlay = it },
            onTogglePerformanceHud = ::updatePerformanceHud,
            onEditControlsRequested = ::openControlsEditor,
            onEditPhysicalControllerRequested = { showPhysicalControllerDialog = true },
            onExitRequested = {
                exitRequestCoordinator.requestExit()
            },
        )
    }

    val gameBack: () -> Unit = {
        XServerOverlayActionCoordinator.handleBack(
            anchorView = view,
            imeInputReceiver = imeInputReceiver,
            showQuickMenu = showQuickMenu,
            neverSuspend = neverSuspend,
            onDismissOverlayMenu = dismissOverlayMenu,
            onKeyboardRequestedFromOverlayChanged = { keyboardRequestedFromOverlay = it },
            onShowQuickMenuChanged = { showQuickMenu = it },
            onHasPhysicalControllerChanged = { hasPhysicalController = it },
        )
    }

    DisposableEffect(Unit) {
        val inputManager = context.getSystemService(Context.INPUT_SERVICE) as InputManager

        val deviceListener = object : InputManager.InputDeviceListener {
            override fun onInputDeviceAdded(deviceId: Int) {
                val device = InputDevice.getDevice(deviceId) ?: return
                evaluateDevice(device)
            }

            override fun onInputDeviceRemoved(deviceId: Int) {
                // Re-scan since we don't know which type was removed
                scanForExternalDevices()
            }

            override fun onInputDeviceChanged(deviceId: Int) {
                scanForExternalDevices()
                val device = InputDevice.getDevice(deviceId) ?: return
                evaluateDevice(device)
            }
        }

        inputManager.registerInputDeviceListener(deviceListener, null)
        scanForExternalDevices()

        onDispose {
            inputManager.unregisterInputDeviceListener(deviceListener)
        }
    }

    DisposableEffect(container) {
        registerBackAction(gameBack)
        onDispose {
            Timber.d("XServerScreen leaving, clearing back action")
            removePerformanceHud()
            performanceHudHost = null
            imeInputReceiver?.hideKeyboard()
            imeInputReceiver = null
            if (!SteamService.keepAlive) {
                GameGrubApp.clearActiveSuspendState()
            } else if (!manualResumeMode) {
                GameGrubApp.isOverlayPaused = false
            }
            registerBackAction { }
        } // preserve suspend state across activity recreation while a game is still running
    }

    DisposableEffect(lifecycleOwner, container) {
        val onActivityDestroyed: (AndroidEvent.ActivityDestroyed) -> Unit = {
            Timber.i("onActivityDestroyed")
            exitRequestCoordinator.requestExit()
        }
        val onKeyEvent: (AndroidEvent.KeyEvent) -> Boolean = {
            val event = it.event
            val uiGuardState = XServerInputDeviceCoordinator.UiGuardState(
                showElementEditor = showElementEditor,
                keepPausedForEditor = keepPausedForEditor,
                showQuickMenu = showQuickMenu,
                isEditMode = isEditMode,
                isTouchscreenMode = container.isTouchscreenMode,
                hasUpdatedScreenGamepad = hasUpdatedScreenGamepad,
            )
            val manualResumeState = XServerInputEventDispatchCoordinator.ManualResumeState(
                manualResumeMode = manualResumeMode,
                isOverlayPaused = GameGrubApp.isOverlayPaused,
                showQuickMenu = showQuickMenu,
                keepPausedForEditor = keepPausedForEditor,
            )

            XServerInputEventDispatchCoordinator.handleKeyEvent(
                event = event,
                isKeyboard = Keyboard.isKeyboardDevice(event.device),
                isGamepad = ExternalController.isGameController(event.device),
                uiGuardState = uiGuardState,
                manualResumeState = manualResumeState,
                onResumeFromManualButton = ::resumeFromManualButton,
                dispatchGamepadKeyRaw = { keyEvent ->
                    requireNotNull(xServerView).getxServer().winHandler.onKeyEvent(keyEvent)
                },
                dispatchGamepadKeyPhysicalController = { keyEvent ->
                    physicalControllerHandler?.onKeyEvent(keyEvent) == true
                },
                dispatchGamepadKeyInputControls = { keyEvent ->
                    GameGrubApp.inputControlsView?.onKeyEvent(keyEvent) == true
                },
                dispatchKeyboardKey = { keyEvent ->
                    keyboard?.onKeyEvent(keyEvent) == true
                },
            )
        }
        val onMotionEvent: (AndroidEvent.MotionEvent) -> Boolean = {
            val event = it.event
            val uiGuardState = XServerInputDeviceCoordinator.UiGuardState(
                showElementEditor = showElementEditor,
                keepPausedForEditor = keepPausedForEditor,
                showQuickMenu = showQuickMenu,
                isEditMode = isEditMode,
                isTouchscreenMode = container.isTouchscreenMode,
                hasUpdatedScreenGamepad = hasUpdatedScreenGamepad,
            )

            XServerInputEventDispatchCoordinator.handleMotionEvent(
                event = event,
                isGamepad = ExternalController.isGameController(event?.device),
                uiGuardState = uiGuardState,
                isOverlayPaused = GameGrubApp.isOverlayPaused,
                hasPointerCapture = GameGrubApp.touchpadView?.hasPointerCapture() == true,
                dispatchGamepadMotionRaw = { motionEvent ->
                    requireNotNull(xServerView).getxServer().winHandler.onGenericMotionEvent(motionEvent)
                },
                dispatchGamepadMotionPhysicalController = { motionEvent ->
                    physicalControllerHandler?.onGenericMotionEvent(motionEvent) == true
                },
                dispatchGamepadMotionInputControls = { motionEvent ->
                    GameGrubApp.inputControlsView?.onGenericMotionEvent(motionEvent) == true
                },
                onInternalTouchpadDetected = {
                    hasInternalTouchpad = true
                },
                onHideInputControlsRequested = {
                    hasUpdatedScreenGamepad = true
                    XServerInputControlsOverlayHelper.hideInputControls()
                    areControlsVisible = false
                },
                onTryCapturePointer = {
                    tryCapturePointer()
                },
            )
        }
        val requestExitFromEvent: (XServerExitTriggerEventCoordinator.ExitTrigger) -> Unit = {
            exitRequestCoordinator.requestExit()
        }
        val exitEventHandlers = XServerExitTriggerEventCoordinator.createHandlers(
            onRequestExit = requestExitFromEvent,
        )
        val debugCallback = Callback<String> { outputLine ->
            Timber.i(outputLine ?: "")
        }
        val disposeLifecycleBindings = XServerLifecycleEventBindings.register(
            onActivityDestroyed = onActivityDestroyed,
            onKeyEvent = onKeyEvent,
            onMotionEvent = onMotionEvent,
            onGuestProgramTerminated = exitEventHandlers.onGuestProgramTerminated,
            onForceCloseApp = exitEventHandlers.onForceCloseApp,
            debugCallback = debugCallback,
        )

        onDispose {
            disposeLifecycleBindings()
        }
    }

    DisposableEffect(lifecycleOwner, xServerView) {
        val currentXServerView = xServerView
        if (currentXServerView == null) {
            onDispose { }
        } else {
            val disposeRendererLifecycleBinding = XServerRendererLifecycleCoordinator.register(
                lifecycleOwner = lifecycleOwner,
                xServerView = currentXServerView,
            )
            onDispose {
                disposeRendererLifecycleBinding()
            }
        }
    }

    val isPortrait = container.isPortraitMode
    // var launchedView by rememberSaveable { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxSize()) {
        key(isPortrait) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerHoverIcon(PointerIcon.Default)
                    .pointerInteropFilter { event ->
                        val hud = performanceHudView
                        if (hud != null) {
                            when (event.actionMasked) {
                                MotionEvent.ACTION_DOWN -> {
                                    if (hud.isShown && hud.width > 0 && hud.height > 0) {
                                        val hudLocation = IntArray(2)
                                        hud.getLocationOnScreen(hudLocation)
                                        val insideHud =
                                            event.rawX >= hudLocation[0] &&
                                                event.rawX <= hudLocation[0] + hud.width &&
                                                event.rawY >= hudLocation[1] &&
                                                event.rawY <= hudLocation[1] + hud.height
                                        if (insideHud) {
                                            performanceHudTouchDownRawX = event.rawX
                                            performanceHudTouchDownRawY = event.rawY
                                            performanceHudDragOffsetX = event.rawX - hudLocation[0]
                                            performanceHudDragOffsetY = event.rawY - hudLocation[1]
                                            isTrackingPerformanceHudTouch = true
                                            isDraggingPerformanceHud = false
                                            return@pointerInteropFilter true
                                        }
                                    }
                                }

                                MotionEvent.ACTION_MOVE -> {
                                    if (isTrackingPerformanceHudTouch) {
                                        if (!isDraggingPerformanceHud) {
                                            val deltaX = event.rawX - performanceHudTouchDownRawX
                                            val deltaY = event.rawY - performanceHudTouchDownRawY
                                            val distanceSquared = (deltaX * deltaX) + (deltaY * deltaY)
                                            if (distanceSquared >= performanceHudTouchSlop * performanceHudTouchSlop) {
                                                isDraggingPerformanceHud = true
                                            }
                                        }
                                        if (isDraggingPerformanceHud) {
                                            movePerformanceHud(event.rawX, event.rawY, save = false)
                                            return@pointerInteropFilter true
                                        }
                                    }
                                }

                                MotionEvent.ACTION_POINTER_DOWN,
                                MotionEvent.ACTION_POINTER_UP,
                                -> {
                                    if (isTrackingPerformanceHudTouch || isDraggingPerformanceHud) {
                                        isTrackingPerformanceHudTouch = false
                                        isDraggingPerformanceHud = false
                                        return@pointerInteropFilter true
                                    }
                                }

                                MotionEvent.ACTION_UP -> {
                                    if (isTrackingPerformanceHudTouch) {
                                        if (isDraggingPerformanceHud) {
                                            movePerformanceHud(event.rawX, event.rawY, save = true)
                                        } else {
                                            hud.performClick()
                                            togglePerformanceHudLayout()
                                        }
                                        isTrackingPerformanceHudTouch = false
                                        isDraggingPerformanceHud = false
                                        return@pointerInteropFilter true
                                    }
                                }

                                MotionEvent.ACTION_CANCEL -> {
                                    if (isTrackingPerformanceHudTouch || isDraggingPerformanceHud) {
                                        if (isDraggingPerformanceHud) {
                                            movePerformanceHud(event.rawX, event.rawY, save = true)
                                        }
                                        isTrackingPerformanceHudTouch = false
                                        isDraggingPerformanceHud = false
                                        return@pointerInteropFilter true
                                    }
                                }
                            }
                        }

                        val overlayHandled = swapInputOverlay
                            ?.takeIf { it.visibility == View.VISIBLE }
                            ?.dispatchTouchEvent(event) == true
                        if (overlayHandled) return@pointerInteropFilter true

                        if (isPortrait) {
                            gameRoot?.dispatchTouchEvent(event)
                        } else {
                            val controlsHandled = if (areControlsVisible) {
                                GameGrubApp.inputControlsView?.onTouchEvent(event) ?: false
                            } else {
                                false
                            }
                            if (!controlsHandled) {
                                GameGrubApp.touchpadView?.onTouchEvent(event)
                            }
                        }
                        true
                    },
                factory = { context ->
                    Timber.i("Creating XServerView and XServer")
                    val dm = context.resources.displayMetrics
                    val screenWidth = if (isPortrait) minOf(dm.widthPixels, dm.heightPixels) else dm.widthPixels
                    val controlsHeightPortrait = screenWidth * 9 / 16
                    val mainRoot = if (isPortrait) {
                        LinearLayout(context).apply {
                            orientation = LinearLayout.VERTICAL
                            setBackgroundColor(Color.TRANSPARENT)
                        }
                    } else {
                        FrameLayout(context)
                    }
                    val frameLayout = if (isPortrait) {
                        val top = FrameLayout(context)
                        mainRoot.addView(top, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
                        top
                    } else {
                        mainRoot as FrameLayout
                    }
                    performanceHudHost = frameLayout
                    val appId = appId
                    val existingXServer =
                        GameGrubApp.xEnvironment
                            ?.getComponent<XServerComponent>(XServerComponent::class.java)
                            ?.xServer
                    val xServerToUse = existingXServer ?: XServer(ScreenInfo(xServerState.value.screenSize))
                    val xServerView = XServerView(
                        context,
                        xServerToUse,
                    ).apply {
                        xServerView = this
                        val renderer = this.renderer
                        renderer.isCursorVisible = false
                        getxServer().renderer = renderer
                        GameGrubApp.touchpadView =
                            TouchpadView(context, getxServer(), PrefManager.getBoolean("capture_pointer_on_external_mouse", true))
                        frameLayout.addView(GameGrubApp.touchpadView)
                        GameGrubApp.touchpadView?.setMoveCursorToTouchpoint(PrefManager.getBoolean("move_cursor_to_touchpoint", false))

                        // Add invisible IME receiver to capture system keyboard input when keyboard is on external display
                        val imeDisplayContext = context.display?.let { display ->
                            context.createDisplayContext(display)
                        } ?: context

                        val imeReceiver = app.gamegrub.externaldisplay.IMEInputReceiver(
                            context = context,
                            displayContext = imeDisplayContext,
                            xServer = getxServer(),
                        ).apply {
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                            alpha = 0f
                            isClickable = false
                        }
                        frameLayout.addView(imeReceiver)
                        imeInputReceiver = imeReceiver

                        getxServer().winHandler = WinHandler(getxServer(), this)
                        win32AppWorkarounds = Win32AppWorkarounds(getxServer())
                        keyboard = Keyboard(getxServer())
                        if (!bootToContainer) {
                            renderer.setUnviewableWMClasses("explorer.exe")
                            // TODO: make 'force fullscreen' be an option of the app being launched
                            if (container.executablePath.isNotBlank()) {
                                renderer.forceFullscreenWMClass = Paths.get(container.executablePath).name
                            }
                        }
                        windowLifecycleCoordinator.attach(this)
                        mainRoot.tag = XServerViewReleaseBinding(this, windowLifecycleCoordinator)

                        if (GameGrubApp.xEnvironment == null) {
                            // Launch all blocking wine setup operations on a background thread to avoid blocking main thread
                            val setupExecutor = java.util.concurrent.Executors.newSingleThreadExecutor { r ->
                                Thread(r, "WineSetup-Thread").apply { isDaemon = false }
                            }

                            setupExecutor.submit {
                                try {
                                    val containerManager = ContainerRuntimeManagerFactory.create(context)
                                    val launchManager = ContainerLaunchManagerFactory.create()
                                    // Configure WinHandler with container's input API settings
                                    val handler = getxServer().winHandler
                                    if (container.inputType !in 0..3) {
                                        container.inputType = PreferredInputApi.BOTH.ordinal
                                        container.saveData()
                                    }
                                    handler.setPreferredInputApi(PreferredInputApi.values()[container.inputType])
                                    handler.setDInputMapperType(container.dinputMapperType)
                                    if (container.isDisableMouseInput) {
                                        GameGrubApp.touchpadView?.setTouchscreenMouseDisabled(true)
                                    } else if (container.isTouchscreenMode) {
                                        GameGrubApp.touchpadView?.setTouchscreenMode(true)
                                        // Apply per-game gesture configuration
                                        val gestureConfig = app.gamegrub.data.TouchGestureConfig.fromJson(container.gestureConfig)
                                        GameGrubApp.touchpadView?.gestureConfig = gestureConfig
                                    }
                                    Timber.d(
                                        "WinHandler configured: preferredInputApi=%s, dinputMapperType=0x%02x",
                                        PreferredInputApi.values()[container.inputType],
                                        container.dinputMapperType,
                                    )
                                    // Timber.d("1 Container drives: ${container.drives}")
                                    containerManager.activate(container)
                                    // Timber.d("2 Container drives: ${container.drives}")
                                    val imageFs = ImageFs.find(context)

                                    taskAffinityMask = ProcessHelper.getAffinityMask(container.getCPUList(true)).toShort().toInt()
                                    taskAffinityMaskWoW64 = ProcessHelper.getAffinityMask(container.getCPUListWoW64(true)).toShort().toInt()
                                    win32AppWorkarounds?.setTaskAffinityMasks(taskAffinityMask, taskAffinityMaskWoW64)
                                    containerVariantChanged = container.containerVariant != imageFs.variant
                                    firstTimeBoot = container.getExtra("appVersion").isEmpty() || containerVariantChanged
                                    Timber.i("First time boot: $firstTimeBoot")

                                    val wineVersion = container.wineVersion
                                    Timber.i("Wine version is: $wineVersion")
                                    val contentsManager = ContentsManager(context)
                                    contentsManager.syncContents()
                                    Timber.i("Wine info is: " + WineInfo.fromIdentifier(context, contentsManager, wineVersion))
                                    xServerState.value = xServerState.value.copy(
                                        wineInfo = WineInfo.fromIdentifier(context, contentsManager, wineVersion),
                                    )
                                    Timber.i("xServerState.value.wineInfo is: " + xServerState.value.wineInfo)
                                    Timber.i("WineInfo.MAIN_WINE_VERSION is: " + WineInfo.MAIN_WINE_VERSION)
                                    Timber.i("Wine path for wineinfo is " + xServerState.value.wineInfo.path)

                                    if (!xServerState.value.wineInfo.isMainWineVersion()) {
                                        Timber.i("Settings wine path to: ${xServerState.value.wineInfo.path}")
                                        imageFs.setWinePath(xServerState.value.wineInfo.path)
                                    } else {
                                        imageFs.setWinePath(imageFs.rootDir.path + "/opt/wine")
                                    }

                                    val onExtractFileListener = if (!xServerState.value.wineInfo.isWin64) {
                                        object : OnExtractFileListener {
                                            override fun onExtractFile(destination: File?, size: Long): File? {
                                                return destination?.path?.let {
                                                    if (it.contains("system32/")) {
                                                        null
                                                    } else {
                                                        File(it.replace("syswow64/", "system32/"))
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        null
                                    }

                                    val sharpnessEffect: String = container.getExtra("sharpnessEffect", "None")
                                    if (sharpnessEffect != "None") {
                                        val sharpnessLevel = container.getExtra("sharpnessLevel", "100").toDouble()
                                        val sharpnessDenoise = container.getExtra("sharpnessDenoise", "100").toDouble()
                                        vkbasaltConfig =
                                            "effects=" + sharpnessEffect.lowercase(Locale.getDefault()) + ";" + "casSharpness=" +
                                            sharpnessLevel / 100 +
                                            ";" +
                                            "dlsSharpness=" +
                                            sharpnessLevel / 100 +
                                            ";" +
                                            "dlsDenoise=" +
                                            sharpnessDenoise / 100 +
                                            ";" +
                                            "enableOnLaunch=True"
                                    }

                                    Timber.i("Doing things once")
                                    val envVars = EnvVars()

                                    LaunchPreparationCoordinator.prepareLaunchArtifacts(
                                        context = context,
                                        firstTimeBoot = firstTimeBoot,
                                        screenInfo = xServerView!!.getxServer().screenInfo,
                                        xServerState = xServerState,
                                        container = container,
                                        containerManager = containerManager,
                                        envVars = envVars,
                                        contentsManager = contentsManager,
                                        onExtractFileListener = onExtractFileListener,
                                        vkbasaltConfig = vkbasaltConfig,
                                        alwaysReextract = ALWAYS_REEXTRACT,
                                    )
                                    GameGrubApp.xEnvironment = setupXEnvironment(
                                        context,
                                        appId,
                                        bootToContainer,
                                        testGraphics,
                                        launchManager,
                                        xServerState,
                                        envVars,
                                        container,
                                        appLaunchInfo,
                                        xServerView!!.getxServer(),
                                        containerVariantChanged,
                                        onGameLaunchError,
                                        navigateBack,
                                    )
                                    if (!GameGrubApp.isActivityInForeground && !neverSuspend) {
                                        GameGrubApp.xEnvironment?.onPause()
                                        if (manualResumeMode) {
                                            view.post {
                                                GameGrubApp.isOverlayPaused = true
                                                Timber.d("Game paused after environment setup while app was backgrounded (manual resume required)")
                                            }
                                        } else {
                                            Timber.d("Game paused after environment setup while app was backgrounded")
                                        }
                                    }
                                } catch (e: Exception) {
                                    Timber.e(e, "Error during wine setup operations")
                                    try {
                                        GameGrubApp.xEnvironment?.stopEnvironmentComponents()
                                    } catch (cleanupEx: Exception) {
                                        Timber.e(cleanupEx, "Error cleaning up environment after setup failure")
                                    }
                                    GameGrubApp.xEnvironment = null
                                    onGameLaunchError?.invoke("Failed to setup wine: ${e.message}")
                                } finally {
                                    setupExecutor.shutdown()
                                }
                            }
                        }
                    }
                    GameGrubApp.xServerView = xServerView

                    val gameHost = FrameLayout(context).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                    }
                    frameLayout.addView(gameHost)
                    gameHost.addView(xServerView)

                    GameGrubApp.inputControlsManager = InputControlsManager(context)

                    // Store the loaded profile for auto-show logic later (declared outside apply block)
                    var loadedProfile: ControlsProfile? = null

                    // Create InputControlsView and add to FrameLayout
                    val manager = GameGrubApp.inputControlsManager
                    val startupProfile = XServerPhysicalControllerCoordinator.resolveStartupProfile(
                        manager = manager,
                        container = container,
                    )
                    loadedProfile = startupProfile

                    val icView = InputControlsView(context).apply {
                        setXServer(xServerView.getxServer())
                        touchpadView = GameGrubApp.touchpadView
                        PrefManager.init(context)
                        profile = startupProfile
                        XServerPhysicalControllerCoordinator.applyInputControlsViewDefaults(this, container)
                    }

                    if (startupProfile != null) {
                        physicalControllerHandler = XServerPhysicalControllerCoordinator.createOrUpdateHandler(
                            existingHandler = physicalControllerHandler,
                            profile = startupProfile,
                            xServer = xServerView.getxServer(),
                            onOpenNavigationMenu = gameBack,
                        )
                    }

                    GameGrubApp.inputControlsView = icView

                    xServerView.getxServer().winHandler.setInputControlsView(GameGrubApp.inputControlsView)

                    // Add InputControlsView (portrait: inside fixed-height container at bottom; landscape: overlay)
                    if (isPortrait) {
                        val controlsContainer = FrameLayout(context).apply {
                            setBackgroundColor(Color.BLACK)
                        }
                        mainRoot.addView(
                            controlsContainer,
                            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, controlsHeightPortrait),
                        )
                        controlsContainer.addView(icView)
                    } else {
                        frameLayout.addView(icView)
                    }
                    val configuredExternalMode = ExternalDisplayInputController.fromConfig(container.externalDisplayMode)
                    val swapEnabled = container.isExternalDisplaySwap

                    val overlay = SwapInputOverlayView(context, xServerView.getxServer()).apply {
                        visibility = View.GONE
                        setMode(ExternalDisplayInputController.Mode.OFF)
                    }
                    frameLayout.addView(overlay)
                    swapInputOverlay = overlay

                    val externalDisplayController =
                        if (!swapEnabled && configuredExternalMode != ExternalDisplayInputController.Mode.OFF) {
                            ExternalDisplayInputController(
                                context = context,
                                xServer = xServerView.getxServer(),
                                touchpadViewProvider = { GameGrubApp.touchpadView },
                            ).apply {
                                setMode(configuredExternalMode)
                                start()
                            }
                        } else {
                            null
                        }

                    val swapController =
                        if (swapEnabled) {
                            val surfaceBg = ContextCompat.getColor(context, R.color.external_display_surface_background)
                            ExternalDisplaySwapController(
                                context = context,
                                xServerViewProvider = { xServerView },
                                internalGameHostProvider = { gameHost },
                                onGameOnExternalChanged = { gameOnExternal ->
                                    if (gameOnExternal) {
                                        GameGrubApp.touchpadView?.setBackgroundColor(surfaceBg)
                                        when (configuredExternalMode) {
                                            ExternalDisplayInputController.Mode.KEYBOARD,
                                            ExternalDisplayInputController.Mode.HYBRID,
                                            -> {
                                                overlay.visibility = View.VISIBLE
                                                overlay.setMode(configuredExternalMode)
                                            }

                                            else -> {
                                                overlay.visibility = View.GONE
                                                overlay.setMode(ExternalDisplayInputController.Mode.OFF)
                                            }
                                        }
                                    } else {
                                        GameGrubApp.touchpadView?.setBackgroundColor(Color.TRANSPARENT)
                                        overlay.visibility = View.GONE
                                        overlay.setMode(ExternalDisplayInputController.Mode.OFF)
                                    }
                                },
                            ).apply {
                                setSwapEnabled(true)
                                start()
                            }
                        } else {
                            null
                        }
                    mainRoot.addOnAttachStateChangeListener(
                        object : View.OnAttachStateChangeListener {
                            override fun onViewAttachedToWindow(v: View) {}

                            override fun onViewDetachedFromWindow(v: View) {
                                externalDisplayController?.stop()
                                swapController?.stop()
                            }
                        },
                    )
                    // Don't call hideInputControls() here - let the auto-show logic below handle visibility
                    // so that the view gets measured/laid out and has valid dimensions for element loading

                    // Auto-show on-screen controls after the view has been laid out and has proper dimensions
                    icView.post {
                        Timber.d("Auto-show logic running - view dimensions: ${icView.width}x${icView.height}")
                        loadedProfile?.let { profile ->
                            // Load elements if not already loaded (view has dimensions now)
                            if (!profile.isElementsLoaded) {
                                Timber.d("Loading profile elements for auto-show")
                                profile.loadElements(icView)
                            }

                            // Only auto-show if profile has on-screen elements
                            Timber.d("Profile has ${profile.elements.size} elements loaded")
                            if (profile.elements.isNotEmpty()) {
                                // Check for ACTUAL physically connected controllers, not just saved bindings
                                val controllerManager = ControllerManager.getInstance()
                                controllerManager.scanForDevices()
                                val hasPhysicalController = controllerManager.detectedDevices.isNotEmpty()

                                val shouldShowControls = XServerPhysicalControllerCoordinator.shouldShowOnScreenControls(
                                    container = container,
                                    hasPhysicalController = hasPhysicalController,
                                    hasPhysicalKeyboard = hasPhysicalKeyboard,
                                    hasPhysicalMouse = hasPhysicalMouse,
                                )

                                if (shouldShowControls) {
                                    Timber.d("Auto-showing onscreen controls")
                                    XServerInputControlsOverlayHelper.showInputControls(
                                        profile,
                                        xServerView.getxServer().winHandler,
                                        container,
                                    )
                                    areControlsVisible = true
                                } else {
                                    Timber.d("Hiding onscreen controls")
                                    XServerInputControlsOverlayHelper.hideInputControls()
                                    areControlsVisible = false
                                }
                            } else {
                                Timber.w("Profile has no elements - cannot auto-show controls")
                            }
                        }
                    }
                    frameRating = FrameRating(context)
                    frameRating?.visibility = View.GONE

                    if (isPerformanceHudEnabled) {
                        frameLayout.post {
                            updatePerformanceHud(true)
                        }
                    }

                    if (container.isDisableMouseInput) {
                        GameGrubApp.touchpadView?.setTouchscreenMouseDisabled(true)
                    }

                    mainRoot

                    // } else {
                    //     Log.d("XServerScreen", "Creating XServerView without creating XServer")
                    //     xServerView = XServerView(context, PluviaApp.xServer)
                    // }
                    // xServerView
                },
                update = { view ->
                    gameRoot = view
                },
                onRelease = { view ->
                    gameRoot = null
                    removePerformanceHud()
                    performanceHudHost = null

                    val releaseBinding = view.tag as? XServerViewReleaseBinding
                    releaseBinding?.let { binding ->
                        binding.windowLifecycleCoordinator.detach()
                        if (GameGrubApp.xServerView === binding.xServerView) {
                            GameGrubApp.xServerView = null
                        }
                    }
                    view.tag = null
                },
            )
        }

        // Floating toolbar for edit mode (always visible in edit mode)
        if (isEditMode && areControlsVisible) {
            EditModeToolbar(
                onAdd = {
                    if (GameGrubApp.inputControlsView?.addElement() == true) {
                        // Element was added, refresh the view
                        GameGrubApp.inputControlsView?.invalidate()
                    }
                },
                onEdit = {
                    val selectedElement = GameGrubApp.inputControlsView?.selectedElement
                    if (selectedElement != null) {
                        elementToEdit = selectedElement
                        showElementEditor = true
                    }
                },
                onDelete = {
                    GameGrubApp.inputControlsView?.removeElement()
                },
                onSave = {
                    // Save profile changes
                    GameGrubApp.inputControlsView?.profile?.save()
                    // Clear snapshot since changes were accepted
                    elementPositionsSnapshot = emptyMap()
                    // Exit edit mode
                    isEditMode = false
                    GameGrubApp.inputControlsView?.setEditMode(false)
                    // Force redraw on next frame to ensure grid is removed
                    GameGrubApp.inputControlsView?.post {
                        GameGrubApp.inputControlsView?.invalidate()
                    }
                    keepPausedForEditor = false
                    resumeIfAllowedAfterOverlay()
                },
                onClose = {
                    // Restore element positions from snapshot (cancel behavior)
                    if (elementPositionsSnapshot.isNotEmpty()) {
                        elementPositionsSnapshot.forEach { (element, position) ->
                            element.setX(position.first)
                            element.setY(position.second)
                        }
                        elementPositionsSnapshot = emptyMap()
                    }

                    // Exit edit mode without saving
                    isEditMode = false
                    GameGrubApp.inputControlsView?.setEditMode(false)
                    // Force redraw on next frame to ensure grid is removed
                    GameGrubApp.inputControlsView?.post {
                        GameGrubApp.inputControlsView?.profile?.loadElements(GameGrubApp.inputControlsView)
                        GameGrubApp.inputControlsView?.profile?.save()
                        GameGrubApp.inputControlsView?.invalidate()
                    }
                    keepPausedForEditor = false
                    resumeIfAllowedAfterOverlay()
                },
                onDuplicate = { id ->
                    val manager = GameGrubApp.inputControlsManager
                    val profile = manager?.getProfile(id)
                    val currentProfile = GameGrubApp.inputControlsView?.profile
                    if (profile != null && currentProfile != null) {
                        // Wait for view to be laid out before loading elements
                        GameGrubApp.inputControlsView?.let { icView ->
                            icView.post {
                                // Load Profile 0 elements (with valid dimensions)
                                profile.loadElements(icView)

                                // Clear current profile elements and copy from Profile 0
                                val elementsToRemove = currentProfile.elements.toList()
                                elementsToRemove.forEach { currentProfile.removeElement(it) }

                                profile.elements.forEach { element ->
                                    val newElement = com.winlator.inputcontrols.ControlElement(icView)
                                    newElement.setType(element.type)
                                    newElement.setShape(element.shape)
                                    newElement.setX(element.x.toInt())
                                    newElement.setY(element.y.toInt())
                                    newElement.setScale(element.scale)
                                    newElement.text = element.text
                                    newElement.setIconId(element.iconId.toInt())
                                    newElement.isToggleSwitch = element.isToggleSwitch
                                    // Copy range button properties — must set binding count
                                    // BEFORE copying bindings, because setBindingCount resets
                                    // the bindings array to NONE.
                                    if (element.type == com.winlator.inputcontrols.ControlElement.Type.RANGE_BUTTON) {
                                        newElement.range = element.range
                                        newElement.setOrientation(element.orientation)
                                        newElement.setBindingCount(element.bindingCount)
                                        newElement.isScrollLocked = element.isScrollLocked
                                    }
                                    for (i in 0 until element.bindingCount) {
                                        newElement.setBindingAt(i, element.getBindingAt(i))
                                    }
                                    // Copy shooter mode properties
                                    if (element.type == com.winlator.inputcontrols.ControlElement.Type.SHOOTER_MODE) {
                                        newElement.shooterMovementType = element.shooterMovementType
                                        newElement.shooterLookType = element.shooterLookType
                                        newElement.shooterLookSensitivity = element.shooterLookSensitivity
                                        newElement.shooterJoystickSize = element.shooterJoystickSize
                                    }
                                    currentProfile.addElement(newElement)
                                }

                                icView.invalidate()
                                SnackbarManager.show(context.getString(R.string.toast_controls_reset))
                            }
                        }
                    }
                },
            )
        }

        QuickMenu(
            isVisible = showQuickMenu,
            onDismiss = dismissOverlayMenu,
            onItemSelected = onQuickMenuItemSelected,
            renderer = xServerView?.renderer,
            isPerformanceHudEnabled = isPerformanceHudEnabled,
            performanceHudConfig = performanceHudConfig,
            onPerformanceHudConfigChanged = ::applyPerformanceHudConfig,
            hasPhysicalController = hasPhysicalController,
        )

        if (manualResumeMode && GameGrubApp.isOverlayPaused && !showQuickMenu && !keepPausedForEditor) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            color = androidx.compose.ui.graphics.Color.White,
                            shape = androidx.compose.foundation.shape.CircleShape,
                        )
                        .clickable(onClick = ::resumeFromManualButton),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.resume_game),
                        tint = androidx.compose.ui.graphics.Color.Black,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
        }
    }

    // Element Editor Dialog
    if (showElementEditor && elementToEdit != null && GameGrubApp.inputControlsView != null) {
        app.gamegrub.ui.component.dialog.ElementEditorDialog(
            element = elementToEdit!!,
            view = GameGrubApp.inputControlsView!!,
            onDismiss = {
                showElementEditor = false
                // Keep edit mode active so user can edit other elements
            },
            onSave = {
                showElementEditor = false
                // Keep edit mode active so user can edit other elements
            },
        )
    }

    // Physical Controller Config Dialog
    if (showPhysicalControllerDialog) {
        // Get profile from container settings, not from InputControlsView
        // (InputControlsView.profile is null when on-screen controls are hidden)
        val manager = GameGrubApp.inputControlsManager ?: InputControlsManager(context)
        val profile = XServerPhysicalControllerCoordinator.resolveDialogProfile(
            manager = manager,
            container = container,
            gameName = currentAppInfo?.name ?: container.name,
        )

        if (profile != null) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = {
                    showPhysicalControllerDialog = false
                    keepPausedForEditor = false
                    resumeIfAllowedAfterOverlay()
                },
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.95f)),
                ) {
                    app.gamegrub.ui.component.dialog.PhysicalControllerConfigSection(
                        profile = profile,
                        onDismiss = {
                            showPhysicalControllerDialog = false
                            keepPausedForEditor = false
                            resumeIfAllowedAfterOverlay()
                        },
                        onSave = {
                            XServerPhysicalControllerCoordinator.persistDialogProfileChanges(profile, container)

                            // Update handler with reloaded profile if on-screen controls are shown
                            if (GameGrubApp.inputControlsView?.profile != null) {
                                GameGrubApp.inputControlsView?.profile = profile
                            }
                            physicalControllerHandler = XServerPhysicalControllerCoordinator.createOrUpdateHandler(
                                existingHandler = physicalControllerHandler,
                                profile = profile,
                                xServer = xServerView?.getxServer(),
                                onOpenNavigationMenu = gameBack,
                            )
                            showPhysicalControllerDialog = false
                            keepPausedForEditor = false
                            resumeIfAllowedAfterOverlay()
                        },
                    )
                }
            }
        }
    }

    // var ranSetup by rememberSaveable { mutableStateOf(false) }
    // LaunchedEffect(lifecycleOwner) {
    //     if (!ranSetup) {
    //         ranSetup = true
    //
    //
    //     }
    // }
}

private fun setupXEnvironment(
    context: Context,
    appId: String,
    bootToContainer: Boolean,
    testGraphics: Boolean,
    launchManager: ContainerLaunchManager,
    xServerState: MutableState<XServerState>,
    // xServerViewModel: XServerViewModel,
    envVars: EnvVars,
    // generateWinePrefix: Boolean,
    container: Container?,
    appLaunchInfo: LaunchInfo?,
    // shortcut: Shortcut?,
    xServer: XServer,
    containerVariantChanged: Boolean,
    onGameLaunchError: ((String) -> Unit)? = null,
    navigateBack: () -> Unit,
): XEnvironment {
    return EnvironmentSetupCoordinator.setupXEnvironment(
        context = context,
        appId = appId,
        bootToContainer = bootToContainer,
        testGraphics = testGraphics,
        launchManager = launchManager,
        xServerState = xServerState,
        envVars = envVars,
        container = container,
        appLaunchInfo = appLaunchInfo,
        xServer = xServer,
        containerVariantChanged = containerVariantChanged,
        onGameLaunchError = onGameLaunchError,
        navigateBack = navigateBack,
    )
}
