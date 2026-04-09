package app.gamegrub.ui

import android.content.Intent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import app.gamegrub.BuildConfig
import app.gamegrub.Constants
import app.gamegrub.LaunchRequestManager
import app.gamegrub.PrefManager
import app.gamegrub.R
import app.gamegrub.enums.AppTheme
import app.gamegrub.enums.LoginResult
import app.gamegrub.enums.SaveLocation
import app.gamegrub.events.AndroidEvent
import app.gamegrub.launch.GameResolutionResult
import app.gamegrub.launch.IntentLaunchManager
import app.gamegrub.launch.needsSteamLogin
import app.gamegrub.launch.resolveGameAppId
import app.gamegrub.launch.trackGameLaunched
import app.gamegrub.service.amazon.AmazonService
import app.gamegrub.service.epic.EpicService
import app.gamegrub.service.gog.GOGService
import app.gamegrub.service.steam.SteamService
import app.gamegrub.ui.component.AchievementOverlay
import app.gamegrub.ui.component.BootingSplash
import app.gamegrub.ui.component.ConnectionStatusBanner
import app.gamegrub.ui.component.dialog.ContainerConfigDialog
import app.gamegrub.ui.component.dialog.GameFeedbackDialog
import app.gamegrub.ui.component.dialog.LoadingDialog
import app.gamegrub.ui.component.dialog.MessageDialog
import app.gamegrub.ui.component.dialog.state.GameFeedbackDialogState
import app.gamegrub.ui.component.dialog.state.MessageDialogState
import app.gamegrub.ui.container.ContainerConfigCoordinator
import app.gamegrub.ui.enums.DialogType
import app.gamegrub.ui.feedback.GameFeedbackCoordinator
import app.gamegrub.ui.launch.consumePendingLaunchWithError
import app.gamegrub.ui.launch.handleExternalLaunchSuccess
import app.gamegrub.ui.launch.preLaunchApp
import app.gamegrub.ui.launch.showGameNotInstalledDialog
import app.gamegrub.ui.model.MainViewModel
import app.gamegrub.ui.orientation.OrientationPolicy
import app.gamegrub.ui.runtime.XServerRuntime
import app.gamegrub.ui.screen.GameGrubScreen
import app.gamegrub.ui.screen.HomeScreen
import app.gamegrub.ui.screen.login.UserLoginScreen
import app.gamegrub.ui.screen.settings.SettingsScreen
import app.gamegrub.ui.screen.xserver.XServerScreen
import app.gamegrub.ui.service.getServiceStartupCoordinator
import app.gamegrub.ui.theme.GameGrubTheme
import app.gamegrub.ui.update.AppUpdateCoordinator
import app.gamegrub.ui.utils.SnackbarManager
import app.gamegrub.update.UpdateInfo
import app.gamegrub.utils.container.ContainerUtils
import com.winlator.container.ContainerData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

private const val BOOTING_SPLASH_Z_INDEX = 10f
private const val CONNECTION_BANNER_Z_INDEX = 5f
private val SNACKBAR_BOTTOM_PADDING = 16.dp
private val SNACKBAR_CORNER_RADIUS = 24.dp
private val SNACKBAR_SHADOW_ELEVATION = 4.dp
private val SNACKBAR_HORIZONTAL_PADDING = 24.dp
private val SNACKBAR_VERTICAL_PADDING = 12.dp

private fun NavHostController.navigateFromLoginIfNeeded(
    targetRoute: String,
    logTag: String = "GameGrubMain",
) {
    val currentRoute = currentDestination?.route
    if (currentRoute == GameGrubScreen.LoginUser.route) {
        Timber.tag(logTag).i("Navigating from LoginUser to $targetRoute")
        navigate(targetRoute) {
            popUpTo(GameGrubScreen.LoginUser.route) {
                inclusive = true
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GameGrubMain(
    viewModel: MainViewModel = hiltViewModel(),
    navController: NavHostController = rememberNavController(),
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val pendingLaunchMessage = stringResource(R.string.intent_launch_steam_pending)
    val discordSupportTitle = stringResource(R.string.main_discord_support_title)
    val discordSupportMessage = stringResource(R.string.main_discord_support_message)
    val openDiscordText = stringResource(R.string.main_open_discord)
    val closeText = stringResource(R.string.close)
    val saveContainerTitle = stringResource(R.string.save_container_settings_title)
    val saveContainerMessage = stringResource(R.string.save_container_settings_message)
    val saveText = stringResource(R.string.save)
    val discardText = stringResource(R.string.discard)
    val mainShareText = stringResource(R.string.main_share_text)
    val mainShareLabel = stringResource(R.string.main_share)
    val updateFailedTitle = stringResource(R.string.main_update_failed_title)
    val updateFailedMessage = stringResource(R.string.main_update_failed_message)
    val okText = stringResource(R.string.ok)
    val containerConfigTitle = stringResource(R.string.container_config_title)
    val downloadingUpdateMessage = stringResource(R.string.main_downloading_update)
    val feedbackSubmitFailed = stringResource(R.string.game_feedback_submit_failed)
    val isGoldBuild = BuildConfig.BUILD_TYPE.contains("gold", ignoreCase = true)

    val state by viewModel.state.collectAsStateWithLifecycle()

    var msgDialogState by rememberSaveable(stateSaver = MessageDialogState.Saver) {
        mutableStateOf(MessageDialogState(false))
    }
    val setMessageDialogState: (MessageDialogState) -> Unit = { msgDialogState = it }

    var gameFeedbackState by rememberSaveable(stateSaver = GameFeedbackDialogState.Saver) {
        mutableStateOf(GameFeedbackDialogState(false))
    }

    var hasBack by rememberSaveable { mutableStateOf(navController.previousBackStackEntry?.destination?.route != null) }

    var isConnecting by rememberSaveable { mutableStateOf(false) }
    var shownPendingLaunchSnackbar by rememberSaveable { mutableStateOf(false) }

    var gameBackAction by remember { mutableStateOf<() -> Unit?>({}) }

    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }

    var openContainerConfigForAppId by rememberSaveable { mutableStateOf<String?>(null) }

    // Track if connection banner was dismissed by user
    var connectionBannerDismissed by rememberSaveable { mutableStateOf(false) }

    // Track previous connection state to detect actual changes (not just recomposition)
    val previousConnectionState = remember { mutableStateOf(state.connectionState) }

    // Reset dismissed state only when connection state actually changes
    LaunchedEffect(state.connectionState) {
        if (previousConnectionState.value != state.connectionState) {
            connectionBannerDismissed = false
            previousConnectionState.value = state.connectionState
        }
    }

    // Check for updates on app start
    val appUpdateCoordinator = remember { AppUpdateCoordinator(context) }
    val containerConfigCoordinator = remember { ContainerConfigCoordinator(context) }
    val gameFeedbackCoordinator = remember { GameFeedbackCoordinator(context, viewModel.viewModelScope) }
    LaunchedEffect(Unit) {
        val checkedUpdateInfo = appUpdateCoordinator.checkForUpdate()
        if (checkedUpdateInfo != null) {
            updateInfo = checkedUpdateInfo
            viewModel.setUpdateInfo(checkedUpdateInfo)
        }
    }

    // process pending launch request from cold start (event bus has no replay)
    LaunchedEffect(Unit) {
        LaunchRequestManager.consumePendingLaunchRequest()?.let { launchRequest ->
            Timber.i("[GameGrubMain]: Processing pending launch request for app ${launchRequest.appId}")
            // Steam games needing login will be handled by OnLogonEnded/SteamDisconnected.
            // consume+requeue is safe: both calls are non-suspending, so no other coroutine
            // can interleave between them on the main dispatcher (cooperative scheduling).
            if (needsSteamLogin(context, launchRequest.appId)) {
                LaunchRequestManager.setPendingLaunchRequest(launchRequest)
                shownPendingLaunchSnackbar = false
                // stay quiet on first pass; snackbar shows after a failure
                if (SteamService.isConnected) {
                    shownPendingLaunchSnackbar = true
                    SnackbarManager.show(pendingLaunchMessage)
                }
                return@let
            }
            when (val resolution = resolveGameAppId(context, launchRequest.appId)) {
                is GameResolutionResult.Success -> {
                    if (launchRequest.containerConfig != null) {
                        IntentLaunchManager.applyTemporaryConfigOverride(
                            context, launchRequest.appId, launchRequest.containerConfig,
                        )
                    }
                    handleExternalLaunchSuccess(
                        context = context,
                        appId = resolution.finalAppId,
                        useTemporaryOverride = launchRequest.containerConfig != null,
                        viewModel = viewModel,
                        setMessageDialogState = setMessageDialogState,
                    )
                }

                is GameResolutionResult.NotFound -> {
                    showGameNotInstalledDialog(
                        context = context,
                        originalAppId = resolution.originalAppId,
                        requestAppId = launchRequest.appId,
                        setMessageDialogState = setMessageDialogState,
                        logTag = "GameGrubMain",
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                MainViewModel.MainUiEvent.LaunchApp -> {
                    navController.navigate(GameGrubScreen.XServer.route)
                }

                is MainViewModel.MainUiEvent.ExternalGameLaunch -> {
                    Timber.i("[GameGrubMain]: Received ExternalGameLaunch UI event for app ${event.appId}")

                    // Steam games need login before launch (cloud sync uses userSteamId)
                    if (needsSteamLogin(context, event.appId)) {
                        // preserve any container config override already applied by handleLaunchIntent
                        LaunchRequestManager.setPendingLaunchRequest(
                            IntentLaunchManager.LaunchRequest(
                                appId = event.appId,
                                containerConfig = IntentLaunchManager.getTemporaryOverride(event.appId),
                            ),
                        )
                        shownPendingLaunchSnackbar = false
                        if (SteamService.isConnected) {
                            shownPendingLaunchSnackbar = true
                            SnackbarManager.show(pendingLaunchMessage)
                        }
                        return@collect
                    }

                    when (val resolution = resolveGameAppId(context, event.appId)) {
                        is GameResolutionResult.Success -> {
                            Timber.i(
                                "[GameGrubMain]: Using appId=%s (original=%s, isSteamInstalled=%s, isCustomGame=%s)",
                                resolution.finalAppId,
                                event.appId,
                                resolution.isSteamInstalled,
                                resolution.isCustomGame,
                            )

                            handleExternalLaunchSuccess(
                                context = context,
                                appId = resolution.finalAppId,
                                useTemporaryOverride = IntentLaunchManager.hasTemporaryOverride(resolution.finalAppId),
                                viewModel = viewModel,
                                setMessageDialogState = setMessageDialogState,
                            )
                        }

                        is GameResolutionResult.NotFound -> {
                            showGameNotInstalledDialog(
                                context = context,
                                originalAppId = resolution.originalAppId,
                                requestAppId = event.appId,
                                setMessageDialogState = setMessageDialogState,
                                logTag = "GameGrubMain",
                            )
                        }
                    }
                }

                MainViewModel.MainUiEvent.OnBackPressed -> {
                    if (SteamService.keepAlive) {
                        gameBackAction.invoke() ?: navController.popBackStack()
                    } else if (hasBack) {
                        // TODO: check if back leads to log out and present confidence modal
                        navController.popBackStack()
                    } else {
                        // TODO: quit app?
                    }
                }

                MainViewModel.MainUiEvent.OnLoggedOut -> {
                    // Clear persisted route so next login starts fresh from Home
                    viewModel.clearPersistedRoute()
                    // Pop stack and go back to login
                    navController.popBackStack(
                        route = GameGrubScreen.LoginUser.route,
                        inclusive = false,
                        saveState = false,
                    )
                }

                is MainViewModel.MainUiEvent.OnLogonEnded -> {
                    when (event.result) {
                        LoginResult.Success -> {
                            if (LaunchRequestManager.hasPendingLaunchRequest()) {
                                LaunchRequestManager.consumePendingLaunchRequest()?.let { launchRequest ->
                                    Timber.tag("IntentLaunch")
                                        .i("Processing pending launch request for app ${launchRequest.appId} (user is now logged in)")
                                    when (val resolution = resolveGameAppId(context, launchRequest.appId)) {
                                        is GameResolutionResult.NotFound -> {
                                            showGameNotInstalledDialog(
                                                context = context,
                                                originalAppId = resolution.originalAppId,
                                                requestAppId = launchRequest.appId,
                                                setMessageDialogState = setMessageDialogState,
                                                logTag = "IntentLaunch",
                                            )
                                            return@let
                                        }

                                        is GameResolutionResult.Success -> {
                                            if (launchRequest.containerConfig != null) {
                                                IntentLaunchManager.applyTemporaryConfigOverride(
                                                    context,
                                                    launchRequest.appId,
                                                    launchRequest.containerConfig,
                                                )
                                                Timber.tag("IntentLaunch")
                                                    .i("Applied container config override for app ${launchRequest.appId}")
                                            }

                                            // Navigate to Home if not already there (for pending launch requests)
                                            if (navController.currentDestination?.route != GameGrubScreen.Home.route) {
                                                navController.navigate(GameGrubScreen.Home.route) {
                                                    popUpTo(navController.graph.startDestinationId) {
                                                        saveState = false
                                                    }
                                                }
                                            }

                                            handleExternalLaunchSuccess(
                                                context = context,
                                                appId = launchRequest.appId,
                                                useTemporaryOverride = launchRequest.containerConfig != null,
                                                viewModel = viewModel,
                                                setMessageDialogState = setMessageDialogState,
                                            )
                                        }
                                    }
                                }
                            } else if (XServerRuntime.get().xEnvironment == null) {
                                val currentRoute = navController.currentDestination?.route
                                val targetRoute = viewModel.getPersistedRoute() ?: GameGrubScreen.Home.route
                                if (currentRoute == GameGrubScreen.LoginUser.route) {
                                    navController.navigateFromLoginIfNeeded(targetRoute, "LogonEnded")
                                } else if (currentRoute == GameGrubScreen.Home.route + "?offline={offline}") {
                                    val isCurrentlyOffline = navController.currentBackStackEntry
                                        ?.arguments?.getBoolean("offline") ?: false
                                    if (isCurrentlyOffline) {
                                        navController.navigate(GameGrubScreen.Home.route + "?offline=false") {
                                            popUpTo(GameGrubScreen.Home.route + "?offline={offline}") {
                                                inclusive = true
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        LoginResult.Failed -> {
                            Timber.i("Login failed: ${event.result}")
                            consumePendingLaunchWithError(context)
                        }

                        else -> {
                            Timber.i("Received non-result: ${event.result}")
                        }
                    }
                }

                is MainViewModel.MainUiEvent.SteamDisconnected -> {
                    if (event.isTerminal) {
                        shownPendingLaunchSnackbar = false
                        consumePendingLaunchWithError(context)
                    } else if (!shownPendingLaunchSnackbar) {
                        val appId = LaunchRequestManager.peekPendingLaunchRequest()?.appId
                        if (appId != null && needsSteamLogin(context, appId)) {
                            shownPendingLaunchSnackbar = true
                            SnackbarManager.show(pendingLaunchMessage)
                        }
                    }
                }

                MainViewModel.MainUiEvent.ShowDiscordSupportDialog -> {
                    msgDialogState = MessageDialogState(
                        visible = true,
                        type = DialogType.DISCORD,
                        title = discordSupportTitle,
                        message = discordSupportMessage,
                        confirmBtnText = openDiscordText,
                        dismissBtnText = closeText,
                    )
                }

                is MainViewModel.MainUiEvent.ShowGameFeedbackDialog -> {
                    gameFeedbackState = GameFeedbackDialogState(
                        visible = true,
                        appId = event.appId,
                    )
                }
            }
        }
    }

    LaunchedEffect(navController) {
        Timber.i("navController changed")

        if (!state.hasLaunched) {
            viewModel.setHasLaunched(true)

            Timber.i("Creating on destination changed listener")

            XServerRuntime.get().onDestinationChangedListener = NavController.OnDestinationChangedListener { _, destination, _ ->
                Timber.i("onDestinationChanged to ${destination.route}")
                // in order not to trigger the screen changed launch effect
                viewModel.setCurrentScreen(destination.route)
            }
        } else {
            XServerRuntime.get().onDestinationChangedListener?.let {
                navController.removeOnDestinationChangedListener(it)
            }
        }

        XServerRuntime.get().onDestinationChangedListener?.let {
            navController.addOnDestinationChangedListener(it)
        }
    }

    // TODO: merge to VM?
    LaunchedEffect(state.currentScreen) {
        // do the following each time we navigate to a new screen
        if (state.resettedScreen != state.currentScreen) {
            viewModel.setScreen()
            // Log.d("GameGrubMain", "Screen changed to $currentScreen, resetting some values")
            // TODO: remove this if statement once XServerScreen orientation change bug is fixed
            if (state.currentScreen != GameGrubScreen.XServer) {
                // Hide or show status bar based on if in game or not
                val shouldShowStatusBar = !PrefManager.hideStatusBarWhenNotInGame
                XServerRuntime.get().events.emit(AndroidEvent.SetSystemUIVisibility(shouldShowStatusBar))

                // reset system ui visibility based on user preference
                // Keep non-game routes fully rotatable.
                XServerRuntime.get().events.emit(
                    AndroidEvent.SetOrientationPolicy(
                        OrientationPolicy.unrestricted(PrefManager.allowedOrientation),
                    ),
                )
            }
            // find out if back is available
            hasBack = navController.previousBackStackEntry?.destination?.route != null
        }
    }

    LaunchedEffect(Unit) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            val coordinator = context.getServiceStartupCoordinator()
            coordinator.evaluateAndStartServices(
                viewModel = viewModel,
                navController = navController,
                state = state,
                isConnecting = isConnecting,
                onConnectingChanged = { isConnecting = it },
                onNavigation = null,
            )
        }
    }

    // Listen for connection state changes - reset local isConnecting flag
    LaunchedEffect(state.isSteamConnected) {
        if (state.isSteamConnected) {
            isConnecting = false
        }
    }

    // Listen for save container config prompt
    var pendingSaveAppId by rememberSaveable { mutableStateOf<String?>(null) }
    val onPromptSaveConfig: (AndroidEvent.PromptSaveContainerConfig) -> Unit = { event ->
        pendingSaveAppId = event.appId
        msgDialogState = MessageDialogState(
            visible = true,
            type = DialogType.SAVE_CONTAINER_CONFIG,
            title = saveContainerTitle,
            message = saveContainerMessage,
            confirmBtnText = saveText,
            dismissBtnText = discardText,
        )
    }

    // Listen for game feedback request
    val onShowGameFeedback: (AndroidEvent.ShowGameFeedback) -> Unit = { event ->
        gameFeedbackState = GameFeedbackDialogState(
            visible = true,
            appId = event.appId,
        )
    }

    LaunchedEffect(Unit) {
        XServerRuntime.get().events.on<AndroidEvent.PromptSaveContainerConfig, Unit>(onPromptSaveConfig)
        XServerRuntime.get().events.on<AndroidEvent.ShowGameFeedback, Unit>(onShowGameFeedback)
    }

    DisposableEffect(Unit) {
        onDispose {
            XServerRuntime.get().events.off<AndroidEvent.PromptSaveContainerConfig, Unit>(onPromptSaveConfig)
            XServerRuntime.get().events.off<AndroidEvent.ShowGameFeedback, Unit>(onShowGameFeedback)
        }
    }

    val onDismissRequest: (() -> Unit)?
    val onDismissClick: (() -> Unit)?
    val onConfirmClick: (() -> Unit)?
    var onActionClick: (() -> Unit)? = null
    when (msgDialogState.type) {
        DialogType.DISCORD -> {
            onConfirmClick = {
                setMessageDialogState(MessageDialogState(false))
                uriHandler.openUri(Constants.Links.DISCORD_INVITE)
            }
            onDismissClick = {
                setMessageDialogState(MessageDialogState(false))
            }
            onDismissRequest = {
                setMessageDialogState(MessageDialogState(false))
            }
        }

        DialogType.SUPPORT -> {
            onConfirmClick = {
                uriHandler.openUri(Constants.Misc.KO_FI_LINK)
                PrefManager.tipped = true
                msgDialogState = MessageDialogState(visible = false)
            }
            onDismissRequest = {
                msgDialogState = MessageDialogState(visible = false)
            }
            onDismissClick = {
                msgDialogState = MessageDialogState(visible = false)
            }
            onActionClick = {
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, mainShareText)
                    type = Constants.Protocol.MIME_TEXT_PLAIN
                }
                context.startActivity(Intent.createChooser(shareIntent, mainShareLabel))
            }
        }

        DialogType.SYNC_CONFLICT -> {
            onConfirmClick = {
                preLaunchApp(
                    scope = viewModel.viewModelScope,
                    context = context,
                    appId = state.launchedAppId,
                    preferredSave = SaveLocation.Remote,
                    setLoadingDialogVisible = viewModel::setLoadingDialogVisible,
                    setLoadingProgress = viewModel::setLoadingDialogProgress,
                    setLoadingMessage = viewModel::setLoadingDialogMessage,
                    setMessageDialogState = setMessageDialogState,
                    onSuccess = viewModel::launchApp,
                )
                msgDialogState = MessageDialogState(false)
            }
            onDismissClick = {
                preLaunchApp(
                    scope = viewModel.viewModelScope,
                    context = context,
                    appId = state.launchedAppId,
                    preferredSave = SaveLocation.Local,
                    setLoadingDialogVisible = viewModel::setLoadingDialogVisible,
                    setLoadingProgress = viewModel::setLoadingDialogProgress,
                    setLoadingMessage = viewModel::setLoadingDialogMessage,
                    setMessageDialogState = setMessageDialogState,
                    onSuccess = viewModel::launchApp,
                )
                msgDialogState = MessageDialogState(false)
            }
            onDismissRequest = {
                msgDialogState = MessageDialogState(false)
            }
        }

        DialogType.SYNC_FAIL -> {
            onConfirmClick = null
            onDismissClick = {
                setMessageDialogState(MessageDialogState(false))
            }
            onDismissRequest = {
                setMessageDialogState(MessageDialogState(false))
            }
        }

        DialogType.EXECUTABLE_NOT_FOUND -> {
            onConfirmClick = null
            onDismissClick = {
                setMessageDialogState(MessageDialogState(false))
            }
            onDismissRequest = {
                setMessageDialogState(MessageDialogState(false))
            }
            onActionClick = {
                setMessageDialogState(MessageDialogState(false))
                openContainerConfigForAppId = state.launchedAppId
            }
        }

        DialogType.SYNC_IN_PROGRESS -> {
            onConfirmClick = {
                setMessageDialogState(MessageDialogState(false))
                preLaunchApp(
                    scope = viewModel.viewModelScope,
                    context = context,
                    appId = state.launchedAppId,
                    skipCloudSync = true,
                    setLoadingDialogVisible = viewModel::setLoadingDialogVisible,
                    setLoadingProgress = viewModel::setLoadingDialogProgress,
                    setLoadingMessage = viewModel::setLoadingDialogMessage,
                    setMessageDialogState = setMessageDialogState,
                    onSuccess = viewModel::launchApp,
                    isOffline = viewModel.isOffline.value,
                )
            }
            onDismissClick = {
                setMessageDialogState(MessageDialogState(false))
            }
            onDismissRequest = {
                setMessageDialogState(MessageDialogState(false))
            }
        }

        DialogType.PENDING_UPLOAD_IN_PROGRESS -> {
            onDismissClick = {
                setMessageDialogState(MessageDialogState(false))
            }
            onDismissRequest = {
                setMessageDialogState(MessageDialogState(false))
            }
            onConfirmClick = null
        }

        DialogType.PENDING_UPLOAD -> {
            onConfirmClick = {
                setMessageDialogState(MessageDialogState(false))
                preLaunchApp(
                    scope = viewModel.viewModelScope,
                    context = context,
                    appId = state.launchedAppId,
                    ignorePendingOperations = true,
                    setLoadingDialogVisible = viewModel::setLoadingDialogVisible,
                    setLoadingProgress = viewModel::setLoadingDialogProgress,
                    setLoadingMessage = viewModel::setLoadingDialogMessage,
                    setMessageDialogState = setMessageDialogState,
                    onSuccess = viewModel::launchApp,
                )
            }
            onDismissClick = {
                setMessageDialogState(MessageDialogState(false))
            }
            onDismissRequest = {
                setMessageDialogState(MessageDialogState(false))
            }
        }

        DialogType.APP_SESSION_ACTIVE -> {
            onConfirmClick = {
                setMessageDialogState(MessageDialogState(false))
                preLaunchApp(
                    scope = viewModel.viewModelScope,
                    context = context,
                    appId = state.launchedAppId,
                    ignorePendingOperations = true,
                    setLoadingDialogVisible = viewModel::setLoadingDialogVisible,
                    setLoadingProgress = viewModel::setLoadingDialogProgress,
                    setLoadingMessage = viewModel::setLoadingDialogMessage,
                    setMessageDialogState = setMessageDialogState,
                    onSuccess = viewModel::launchApp,
                )
            }
            onDismissClick = {
                setMessageDialogState(MessageDialogState(false))
            }
            onDismissRequest = {
                setMessageDialogState(MessageDialogState(false))
            }
        }

        DialogType.ACCOUNT_SESSION_ACTIVE -> {
            onConfirmClick = {
                setMessageDialogState(MessageDialogState(false))
                viewModel.viewModelScope.launch {
                    // Kick only the game on the other device and wait briefly for confirmation
                    SteamService.kickPlayingSession(onlyGame = true)
                    preLaunchApp(
                        scope = viewModel.viewModelScope,
                        context = context,
                        appId = state.launchedAppId,
                        setLoadingDialogVisible = viewModel::setLoadingDialogVisible,
                        setLoadingProgress = viewModel::setLoadingDialogProgress,
                        setLoadingMessage = viewModel::setLoadingDialogMessage,
                        setMessageDialogState = setMessageDialogState,
                        onSuccess = viewModel::launchApp,
                        isOffline = viewModel.isOffline.value,
                    )
                }
            }
            onDismissClick = {
                setMessageDialogState(MessageDialogState(false))
            }
            onDismissRequest = {
                setMessageDialogState(MessageDialogState(false))
            }
        }

        DialogType.APP_SESSION_SUSPENDED -> {
            onDismissClick = {
                setMessageDialogState(MessageDialogState(false))
            }
            onDismissRequest = {
                setMessageDialogState(MessageDialogState(false))
            }
            onConfirmClick = null
        }

        DialogType.PENDING_OPERATION_NONE -> {
            onDismissClick = {
                setMessageDialogState(MessageDialogState(false))
            }
            onDismissRequest = {
                setMessageDialogState(MessageDialogState(false))
            }
            onConfirmClick = null
        }

        DialogType.MULTIPLE_PENDING_OPERATIONS -> {
            onDismissClick = {
                setMessageDialogState(MessageDialogState(false))
            }
            onDismissRequest = {
                setMessageDialogState(MessageDialogState(false))
            }
            onConfirmClick = null
        }

        DialogType.CRASH -> {
            onDismissClick = null
            onDismissRequest = {
                viewModel.setHasCrashedLastStart(false)
                setMessageDialogState(MessageDialogState(false))
            }
            onConfirmClick = {
                viewModel.setHasCrashedLastStart(false)
                setMessageDialogState(MessageDialogState(false))
            }
        }

        DialogType.SAVE_CONTAINER_CONFIG -> {
            onConfirmClick = {
                pendingSaveAppId?.let { appId ->
                    containerConfigCoordinator.saveConfig(appId)
                }
                pendingSaveAppId = null
                setMessageDialogState(MessageDialogState(false))
            }
            onDismissClick = {
                pendingSaveAppId?.let { appId ->
                    containerConfigCoordinator.discardConfig(appId)
                }
                pendingSaveAppId = null
                setMessageDialogState(MessageDialogState(false))
            }
            onDismissRequest = {
                pendingSaveAppId?.let { appId ->
                    containerConfigCoordinator.discardConfig(appId)
                }
                pendingSaveAppId = null
                setMessageDialogState(MessageDialogState(false))
            }
        }

        DialogType.APP_UPDATE -> {
            onConfirmClick = {
                setMessageDialogState(MessageDialogState(false))
                val updateInfo = viewModel.updateInfo.value
                if (updateInfo != null) {
                    scope.launch {
                        viewModel.setLoadingDialogVisible(true)
                        viewModel.setLoadingDialogMessage(downloadingUpdateMessage)
                        viewModel.setLoadingDialogProgress(0f)

                        val success = appUpdateCoordinator.downloadAndInstall(
                            updateInfo = updateInfo,
                            onProgress = { progress ->
                                viewModel.setLoadingDialogProgress(progress)
                            },
                        )

                        viewModel.setLoadingDialogVisible(false)
                        if (!success) {
                            msgDialogState = MessageDialogState(
                                visible = true,
                                type = DialogType.SYNC_FAIL,
                                title = updateFailedTitle,
                                message = updateFailedMessage,
                                dismissBtnText = okText,
                            )
                        }
                    }
                }
            }
            onDismissClick = {
                setMessageDialogState(MessageDialogState(false))
            }
            onDismissRequest = {
                setMessageDialogState(MessageDialogState(false))
            }
        }

        else -> {
            onDismissRequest = null
            onDismissClick = null
            onConfirmClick = null
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        SnackbarManager.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    GameGrubTheme(
        isDark = when (state.appTheme) {
            AppTheme.AUTO -> isSystemInDarkTheme()
            AppTheme.DAY -> false
            AppTheme.NIGHT -> true
            AppTheme.AMOLED -> true
        },
        isAmoled = (state.appTheme == AppTheme.AMOLED),
        style = state.paletteStyle,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LoadingDialog(
                visible = state.loadingDialogVisible,
                progress = state.loadingDialogProgress,
                message = state.loadingDialogMessage,
            )

            MessageDialog(
                visible = msgDialogState.visible,
                onDismissRequest = onDismissRequest,
                onConfirmClick = onConfirmClick,
                confirmBtnText = msgDialogState.confirmBtnText,
                onDismissClick = onDismissClick,
                dismissBtnText = msgDialogState.dismissBtnText,
                onActionClick = onActionClick,
                actionBtnText = msgDialogState.actionBtnText,
                icon = msgDialogState.type.icon,
                title = msgDialogState.title,
                message = msgDialogState.message,
            )

            val scope = rememberCoroutineScope()
            var containerConfigForDialog by remember(openContainerConfigForAppId) { mutableStateOf<ContainerData?>(null) }
            LaunchedEffect(openContainerConfigForAppId) {
                val appId = openContainerConfigForAppId
                if (appId == null) {
                    containerConfigForDialog = null
                    return@LaunchedEffect
                }
                containerConfigForDialog = withContext(Dispatchers.IO) {
                    val container = ContainerUtils.getOrCreateContainer(context, appId)
                    ContainerUtils.toContainerData(container)
                }
            }
            openContainerConfigForAppId?.let { appId ->
                containerConfigForDialog?.let { config ->
                    ContainerConfigDialog(
                        visible = true,
                        title = containerConfigTitle,
                        initialConfig = config,
                        onDismissRequest = { openContainerConfigForAppId = null },
                        onSave = { newConfig ->
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    ContainerUtils.applyToContainer(context, appId, newConfig)
                                }
                                openContainerConfigForAppId = null
                            }
                        },
                    )
                }
            }

            GameFeedbackDialog(
                state = gameFeedbackState,
                onStateChange = { gameFeedbackState = it },
                onSubmit = { feedbackState ->
                    Timber.d(
                        "GameFeedback: onSubmit called with rating=${feedbackState.rating}, tags=${feedbackState.selectedTags}, text=${
                            feedbackState.feedbackText.take(
                                20,
                            )
                        }",
                    )
                    try {
                        gameFeedbackCoordinator.submitFeedback(
                            feedbackState = feedbackState,
                            onComplete = {
                                gameFeedbackState = GameFeedbackDialogState(visible = false)
                            },
                        )
                    } catch (e: Exception) {
                        Timber.e(e, "GameFeedback: Error preparing game feedback")
                        SnackbarManager.show(feedbackSubmitFailed)
                        gameFeedbackState = GameFeedbackDialogState(visible = false)
                    }
                },
                onDismiss = {
                    gameFeedbackState = GameFeedbackDialogState(visible = false)
                },
                onDiscordSupport = {
                    uriHandler.openUri(Constants.Links.DISCORD_INVITE)
                },
            )

            Box(modifier = Modifier.zIndex(BOOTING_SPLASH_Z_INDEX)) {
                BootingSplash(
                    visible = state.showBootingSplash,
                    text = state.bootingSplashText,
                )
            }

            // Connection status banner (overlay) - dismissible so users can access navigation
            if (state.currentScreen != GameGrubScreen.LoginUser &&
                !connectionBannerDismissed &&
                !SteamService.isConnected &&
                PrefManager.refreshToken.isNotEmpty() &&
                PrefManager.username.isNotEmpty()
            ) {
                Box(modifier = Modifier.zIndex(CONNECTION_BANNER_Z_INDEX)) {
                    ConnectionStatusBanner(
                        connectionState = state.connectionState,
                        connectionMessage = state.connectionMessage,
                        timeoutSeconds = state.connectionTimeoutSeconds,
                        onContinueOffline = {
                            viewModel.continueOffline()
                        },
                        onRetry = {
                            viewModel.retryConnection()
                            context.startForegroundService(Intent(context, SteamService::class.java))
                        },
                        onDismiss = {
                            connectionBannerDismissed = true
                        },
                    )
                }
            }

            val startDestination = rememberSaveable {
                when {
                    !PrefManager.onboardingCompleted -> GameGrubScreen.Onboarding.route

                    SteamService.isLoggedIn -> GameGrubScreen.Home.route + "?offline=false"

                    // skip login screen if any service has stored credentials
                    (PrefManager.username.isNotEmpty() && PrefManager.refreshToken.isNotEmpty()) ||
                            GOGService.hasStoredCredentials(context) ||
                            EpicService.hasStoredCredentials(context) ||
                            AmazonService.hasStoredCredentials(context) ->
                        GameGrubScreen.Home.route + "?offline=true"

                    else -> GameGrubScreen.LoginUser.route
                }
            }

            NavHost(
                navController = navController,
                startDestination = startDestination,
            ) {
                // Onboarding
                composable(route = GameGrubScreen.Onboarding.route) {
                    app.gamegrub.ui.screen.onboarding.OnboardingScreen(
                        onComplete = {
                            PrefManager.onboardingCompleted = true
                            navController.navigate(GameGrubScreen.LoginUser.route) {
                                popUpTo(GameGrubScreen.Onboarding.route) { inclusive = true }
                            }
                        },
                        onSkip = {
                            PrefManager.onboardingCompleted = true
                            navController.navigate(GameGrubScreen.LoginUser.route) {
                                popUpTo(GameGrubScreen.Onboarding.route) { inclusive = true }
                            }
                        },
                        onLoginWithSteam = {
                            PrefManager.onboardingCompleted = true
                            navController.navigate(GameGrubScreen.LoginUser.route) {
                                popUpTo(GameGrubScreen.Onboarding.route) { inclusive = true }
                            }
                        },
                    )
                }
                // Login
                composable(route = GameGrubScreen.LoginUser.route) {
                    UserLoginScreen(
                        connectionState = state.connectionState,
                        onRetryConnection = viewModel::retryConnection,
                        onContinueOffline = {
                            navController.navigate(GameGrubScreen.Home.route + "?offline=true")
                        },
                        onPlatformSignedIn = {
                            navController.navigate(GameGrubScreen.Home.route + "?offline=false") {
                                popUpTo(GameGrubScreen.LoginUser.route) { inclusive = true }
                            }
                        },
                    )
                }
                // Library, Downloads, Friends
                composable(
                    route = GameGrubScreen.Home.route + "?offline={offline}",
                    deepLinks = listOf(navDeepLink { uriPattern = "pluvia://home" }),
                    arguments = listOf(
                        navArgument("offline") {
                            type = NavType.BoolType
                            defaultValue = false // default when the query param isn’t present
                        },
                    ),
                ) { backStackEntry ->
                    val isOffline = backStackEntry.arguments?.getBoolean("offline") ?: false
                    val updateAvailableTitle = stringResource(R.string.main_update_available_title)
                    val updateAvailableMessage = updateInfo?.let {
                        stringResource(
                            R.string.main_update_available_message,
                            it.versionName,
                            it.releaseNotes?.let { notes -> "\n\n$notes" } ?: "",
                        )
                    }
                    val updateNowText = stringResource(R.string.main_update_button)
                    val laterText = stringResource(R.string.main_later_button)
                    val recentCrashTitle = stringResource(R.string.main_recent_crash_title)
                    val recentCrashMessage = stringResource(R.string.main_recent_crash_message)
                    val thankYouTitle = stringResource(R.string.main_thank_you_title)
                    val thankYouMessage = stringResource(R.string.main_thank_you_message)
                    val joinKofiText = stringResource(R.string.main_join_kofi)

                    // Show update/crash/support dialogs when Home is first displayed
                    // Skip when offline with Steam credentials (avoid flash when Steam reconnects)
                    LaunchedEffect(Unit) {
                        val hasSteamCredentials = PrefManager.refreshToken.isNotEmpty() && PrefManager.username.isNotEmpty()
                        val shouldShowDialogs = !isOffline || !hasSteamCredentials

                        if (shouldShowDialogs &&
                            !state.annoyingDialogShown &&
                            XServerRuntime.get().xEnvironment == null &&
                            !SteamService.keepAlive &&
                            !LaunchRequestManager.wasLaunchedViaExternalIntent
                        ) {
                            val currentUpdateInfo = updateInfo
                            if (currentUpdateInfo != null) {
                                viewModel.setAnnoyingDialogShown(true)
                                msgDialogState = MessageDialogState(
                                    visible = true,
                                    type = DialogType.APP_UPDATE,
                                    title = updateAvailableTitle,
                                    message = updateAvailableMessage ?: "",
                                    confirmBtnText = updateNowText,
                                    dismissBtnText = laterText,
                                )
                            } else if (state.hasCrashedLastStart) {
                                viewModel.setAnnoyingDialogShown(true)
                                msgDialogState = MessageDialogState(
                                    visible = true,
                                    type = DialogType.CRASH,
                                    title = recentCrashTitle,
                                    message = recentCrashMessage,
                                    confirmBtnText = okText,
                                )
                            } else if (!(PrefManager.tipped || isGoldBuild)) {
                                viewModel.setAnnoyingDialogShown(true)
                                msgDialogState = MessageDialogState(
                                    visible = true,
                                    type = DialogType.SUPPORT,
                                    title = thankYouTitle,
                                    message = thankYouMessage,
                                    confirmBtnText = joinKofiText,
                                    dismissBtnText = closeText,
                                    actionBtnText = mainShareLabel,
                                )
                            }
                        }
                    }

                    HomeScreen(
                        onClickPlay = { appId, asContainer ->
                            trackGameLaunched(appId)
                            viewModel.setLaunchedAppId(appId)
                            viewModel.setBootToContainer(asContainer)
                            viewModel.setTestGraphics(false)
                            viewModel.setOffline(isOffline)
                            preLaunchApp(
                                scope = viewModel.viewModelScope,
                                context = context,
                                appId = appId,
                                setLoadingDialogVisible = viewModel::setLoadingDialogVisible,
                                setLoadingProgress = viewModel::setLoadingDialogProgress,
                                setLoadingMessage = viewModel::setLoadingDialogMessage,
                                setMessageDialogState = { msgDialogState = it },
                                onSuccess = viewModel::launchApp,
                                isOffline = isOffline,
                                bootToContainer = asContainer,
                            )
                        },
                        onTestGraphics = { appId ->
                            viewModel.setLaunchedAppId(appId)
                            viewModel.setBootToContainer(true)
                            viewModel.setTestGraphics(true)
                            viewModel.setOffline(isOffline)
                            preLaunchApp(
                                scope = viewModel.viewModelScope,
                                context = context,
                                appId = appId,
                                setLoadingDialogVisible = viewModel::setLoadingDialogVisible,
                                setLoadingProgress = viewModel::setLoadingDialogProgress,
                                setLoadingMessage = viewModel::setLoadingDialogMessage,
                                setMessageDialogState = { msgDialogState = it },
                                onSuccess = viewModel::launchApp,
                                isOffline = isOffline,
                                bootToContainer = true,
                            )
                        },
                        onClickExit = {
                            XServerRuntime.get().events.emit(AndroidEvent.EndProcess)
                        },
                        onChat = {
                            navController.navigate(GameGrubScreen.Chat.route(it))
                        },
                        onNavigateRoute = {
                            navController.navigate(it)
                        },
                        onLogout = {
                            SteamService.logOut()
                        },
                        onGoOnline = {
                            navController.navigate(
                                if (!SteamService.isLoggedIn) {
                                    GameGrubScreen.LoginUser.route
                                } else {
                                    GameGrubScreen.Home.route
                                },
                            )
                        },
                        isOffline = isOffline,
                    )
                }

                // Full Screen Chat
                // Chat feature temporarily disabled - screen component removed
                /* composable(
                    route = "chat/{id}",
                    arguments = listOf(
                        navArgument(PluviaScreen.Chat.ARG_ID) {
                            type = NavType.LongType
                        },
                    ),
                ) {
                    val id = it.arguments?.getLong(PluviaScreen.Chat.ARG_ID) ?: throw RuntimeException("Unable to get ID to chat")
                    ChatScreen(
                        friendId = id,
                        onBack = {
                            CoroutineScope(Dispatchers.Main).launch {
                                navController.popBackStack()
                            }
                        },
                    )
                } */

                // Game Screen
                composable(route = GameGrubScreen.XServer.route) {
                    XServerScreen(
                        appId = state.launchedAppId,
                        bootToContainer = state.bootToContainer,
                        testGraphics = state.testGraphics,
                        registerBackAction = { cb ->
                            Timber.d("registerBackAction called: $cb")
                            gameBackAction = cb
                        },
                        navigateBack = {
                            CoroutineScope(Dispatchers.Main).launch {
                                val currentRoute = navController.currentBackStackEntry
                                    ?.destination
                                    ?.route

                                if (currentRoute == GameGrubScreen.XServer.route) {
                                    if (LaunchRequestManager.wasLaunchedViaExternalIntent) {
                                        Timber.d("[IntentLaunch]: Finishing activity to return to external launcher")
                                        LaunchRequestManager.clearExternalLaunchFlag()
                                        (context as? android.app.Activity)?.finish()
                                    } else {
                                        navController.popBackStack()
                                    }
                                }
                            }
                        },
                        onWindowMapped = { context, window ->
                            viewModel.onWindowMapped(context, window, state.launchedAppId)
                        },
                        onExit = { onComplete ->
                            viewModel.exitSteamApp(context, state.launchedAppId, onComplete)
                        },
                        onGameLaunchError = { error ->
                            viewModel.onGameLaunchError(error)
                        },
                    )
                }

                // Settings
                composable(route = GameGrubScreen.Settings.route) {
                    SettingsScreen(
                        appTheme = state.appTheme,
                        paletteStyle = state.paletteStyle,
                        onAppTheme = viewModel::setTheme,
                        onPaletteStyle = viewModel::setPalette,
                        onBack = { navController.navigateUp() },
                    )
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBarsIgnoringVisibility)
                    .padding(bottom = SNACKBAR_BOTTOM_PADDING),
            ) { data ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Surface(
                        shape = RoundedCornerShape(SNACKBAR_CORNER_RADIUS),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shadowElevation = SNACKBAR_SHADOW_ELEVATION,
                    ) {
                        Text(
                            text = data.visuals.message,
                            modifier = Modifier.padding(
                                horizontal = SNACKBAR_HORIZONTAL_PADDING,
                                vertical = SNACKBAR_VERTICAL_PADDING,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            AchievementOverlay()
        }
    }
}
