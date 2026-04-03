package app.gamegrub.ui.launch

import android.content.Context
import app.gamegrub.AppOptionMenuType
import app.gamegrub.GameGrubApp
import app.gamegrub.LaunchRequestManager
import app.gamegrub.PrefManager
import app.gamegrub.R
import app.gamegrub.api.config.BestConfigService
import app.gamegrub.data.GameSource
import app.gamegrub.enums.PathType
import app.gamegrub.enums.SaveLocation
import app.gamegrub.enums.SyncResult
import app.gamegrub.service.amazon.AmazonService
import app.gamegrub.service.epic.EpicCloudSavesManager
import app.gamegrub.service.epic.EpicService
import app.gamegrub.service.gog.GOGService
import app.gamegrub.service.steam.SteamService
import app.gamegrub.ui.enums.DialogType
import app.gamegrub.ui.model.MainViewModel
import app.gamegrub.ui.component.dialog.state.MessageDialogState
import app.gamegrub.ui.utils.SnackbarManager
import app.gamegrub.utils.container.ContainerUtils
import app.gamegrub.utils.container.LaunchDependencies
import app.gamegrub.utils.game.CustomGameScanner
import app.gamegrub.utils.general.IntentLaunchManager
import app.gamegrub.utils.manifest.ManifestInstaller
import com.google.android.play.core.splitcompat.SplitCompat
import com.posthog.PostHog
import com.winlator.container.Container
import com.winlator.container.ContainerManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesClientObjects.ECloudPendingRemoteOperation
import java.io.File
import java.util.Date
import kotlin.reflect.KFunction2
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import timber.log.Timber

@EntryPoint
@InstallIn(SingletonComponent::class)
private interface BestConfigServiceEntryPoint {
    fun bestConfigService(): BestConfigService
}

sealed class GameResolutionResult {
    data class Success(
        val finalAppId: String,
        val gameId: Int,
        val isSteamInstalled: Boolean,
        val isCustomGame: Boolean,
    ) : GameResolutionResult()

    data class NotFound(
        val gameId: Int,
        val originalAppId: String,
    ) : GameResolutionResult()
}

fun resolveGameAppId(context: Context, appId: String): GameResolutionResult {
    val gameSource = ContainerUtils.extractGameSourceFromContainerId(appId)
    val gameId = ContainerUtils.extractGameIdFromContainerId(appId)
    val isInstalled = when (gameSource) {
        GameSource.STEAM -> {
            if (SteamService.getAppInfoOf(gameId) != null) {
                SteamService.isAppInstalled(gameId)
            } else {
                ContainerUtils.hasContainer(context, appId)
            }
        }

        GameSource.GOG -> {
            GOGService.isGameInstalled(gameId.toString())
        }

        GameSource.EPIC -> {
            EpicService.isGameInstalled(context, gameId)
        }

        GameSource.AMAZON -> {
            AmazonService.isGameInstalledByAppIdSync(context, gameId)
        }

        GameSource.CUSTOM_GAME -> {
            CustomGameScanner.get().isGameInstalled(gameId)
        }
    }

    if (!isInstalled) {
        return GameResolutionResult.NotFound(
            gameId = gameId,
            originalAppId = appId,
        )
    }

    val isSteamInstalled = gameSource == GameSource.STEAM
    val isCustomGame = gameSource == GameSource.CUSTOM_GAME

    return GameResolutionResult.Success(
        finalAppId = appId,
        gameId = gameId,
        isSteamInstalled = isSteamInstalled,
        isCustomGame = isCustomGame,
    )
}

fun needsSteamLogin(context: Context, appId: String): Boolean {
    val gameSource = ContainerUtils.extractGameSourceFromContainerId(appId)
    if (gameSource != GameSource.STEAM || SteamService.isLoggedIn) return false
    return try {
        !ContainerUtils.getContainer(context, appId).isSteamOfflineMode
    } catch (_: Exception) {
        true
    }
}

fun consumePendingLaunchWithError(context: Context) {
    val request = LaunchRequestManager.peekPendingLaunchRequest() ?: return
    if (!needsSteamLogin(context, request.appId)) return
    LaunchRequestManager.consumePendingLaunchRequest()
    SnackbarManager.show(context.getString(R.string.intent_launch_steam_login_failed))
}

fun trackGameLaunched(appId: String) {
    val gameSource = ContainerUtils.extractGameSourceFromContainerId(appId)
    val gameName = ContainerUtils.resolveGameName(appId)
    PostHog.capture(
        event = "game_launched",
        properties = mapOf(
            "game_name" to gameName,
            "game_store" to gameSource.name,
            "key_attestation_available" to PrefManager.keyAttestationAvailable,
            "play_integrity_available" to PrefManager.playIntegrityAvailable,
        ),
    )
}

fun showGameNotInstalledDialog(
    context: Context,
    originalAppId: String,
    requestAppId: String,
    setMessageDialogState: (MessageDialogState) -> Unit,
    logTag: String,
) {
    val appName = ContainerUtils.resolveGameName(originalAppId)
    Timber.tag(logTag).w("Game not installed: $appName ($requestAppId)")
    setMessageDialogState(
        MessageDialogState(
            visible = true,
            type = DialogType.SYNC_FAIL,
            title = context.getString(R.string.game_not_installed_title),
            message = context.getString(R.string.game_not_installed_message, appName),
            dismissBtnText = context.getString(R.string.ok),
        ),
    )
}

fun handleExternalLaunchSuccess(
    context: Context,
    appId: String,
    useTemporaryOverride: Boolean,
    viewModel: MainViewModel,
    setMessageDialogState: (MessageDialogState) -> Unit,
) {
    LaunchRequestManager.markAsExternalLaunch()
    trackGameLaunched(appId)
    viewModel.setLaunchedAppId(appId)
    viewModel.setBootToContainer(false)
    preLaunchApp(
        context = context,
        appId = appId,
        useTemporaryOverride = useTemporaryOverride,
        setLoadingDialogVisible = viewModel::setLoadingDialogVisible,
        setLoadingProgress = viewModel::setLoadingDialogProgress,
        setLoadingMessage = viewModel::setLoadingDialogMessage,
        setMessageDialogState = setMessageDialogState,
        onSuccess = viewModel::launchApp,
    )
}

fun preLaunchApp(
    context: Context,
    appId: String,
    ignorePendingOperations: Boolean = false,
    preferredSave: SaveLocation = SaveLocation.None,
    useTemporaryOverride: Boolean = false,
    skipCloudSync: Boolean = false,
    setLoadingDialogVisible: (Boolean) -> Unit,
    setLoadingProgress: (Float) -> Unit,
    setLoadingMessage: (String) -> Unit,
    setMessageDialogState: (MessageDialogState) -> Unit,
    onSuccess: KFunction2<Context, String, Unit>,
    retryCount: Int = 0,
    isOffline: Boolean = false,
    bootToContainer: Boolean = false,
) {
    setLoadingDialogVisible(true)

    val gameId = ContainerUtils.extractGameIdFromContainerId(appId)

    CoroutineScope(Dispatchers.IO).launch {
        val containerManager = ContainerManager(context)
        val container = if (useTemporaryOverride) {
            ContainerUtils.getOrCreateContainerWithOverride(context, appId)
        } else {
            ContainerUtils.getOrCreateContainer(context, appId)
        }

        container.clearSessionMetadata()

        val gameSource = ContainerUtils.extractGameSourceFromContainerId(appId)

        if (!bootToContainer) {
            val effectiveExe = when (gameSource) {
                GameSource.STEAM -> SteamService.getLaunchExecutable(appId, container)
                GameSource.GOG -> GOGService.getLaunchExecutable(appId, container)
                GameSource.EPIC -> EpicService.getLaunchExecutable(appId)
                GameSource.CUSTOM_GAME -> CustomGameScanner.get().getLaunchExecutable(container)
                GameSource.AMAZON -> AmazonService.getLaunchExecutable(appId)
            }
            if (effectiveExe.isBlank()) {
                Timber.tag("preLaunchApp").w("Cannot launch $appId: no executable found (game source: $gameSource)")
                setLoadingDialogVisible(false)
                setMessageDialogState(
                    MessageDialogState(
                        visible = true,
                        type = DialogType.EXECUTABLE_NOT_FOUND,
                        title = context.getString(R.string.game_executable_not_found_title),
                        message = context.getString(R.string.game_executable_not_found),
                        dismissBtnText = context.getString(R.string.ok),
                        actionBtnText = AppOptionMenuType.EditContainer.text,
                    ),
                )
                return@launch
            }
        }

        if (gameSource == GameSource.STEAM) {
            try {
                val configJson = Json.parseToJsonElement(container.containerJson).jsonObject
                val missingRequests = EntryPointAccessors
                    .fromApplication(context.applicationContext, BestConfigServiceEntryPoint::class.java)
                    .bestConfigService()
                    .resolveMissingManifestInstallRequests(
                        context, configJson, "exact_gpu_match",
                    )
                for (request in missingRequests) {
                    setLoadingMessage(context.getString(R.string.main_downloading_entry, request.entry.name))
                    try {
                        ManifestInstaller.installManifestEntry(
                            context, request.entry, request.isDriver, request.contentType,
                        ) { progress -> setLoadingProgress(progress.coerceIn(0f, 1f)) }
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to install ${request.entry.name}, continuing")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to install manifest components")
                setLoadingDialogVisible(false)
                return@launch
            }
        }

        val isCustomGame = gameSource == GameSource.CUSTOM_GAME

        SplitCompat.install(context)
        try {
            if (!SteamService.isImageFsInstallable(context, container.containerVariant)) {
                setLoadingMessage("Downloading first-time files")
                SteamService.downloadImageFs(
                    onDownloadProgress = { setLoadingProgress(it / 1.0f) },
                    this,
                    variant = container.containerVariant,
                    context = context,
                ).await()
            }
            if (container.containerVariant.equals(Container.GLIBC) &&
                !SteamService.isFileInstallable(context, "imagefs_patches_gamenative.tzst")
            ) {
                setLoadingMessage("Downloading Wine")
                SteamService.downloadImageFsPatches(
                    onDownloadProgress = { setLoadingProgress(it / 1.0f) },
                    this,
                    context = context,
                ).await()
            } else {
                if (container.wineVersion.contains("proton-9.0-arm64ec") &&
                    !SteamService.isFileInstallable(context, "proton-9.0-arm64ec.txz")
                ) {
                    setLoadingMessage("Downloading arm64ec Proton")
                    SteamService.downloadFile(
                        onDownloadProgress = { setLoadingProgress(it / 1.0f) },
                        this,
                        context = context,
                        "proton-9.0-arm64ec.txz",
                    ).await()
                } else if (container.wineVersion.contains("proton-9.0-x86_64") &&
                    !SteamService.isFileInstallable(context, "proton-9.0-x86_64.txz")
                ) {
                    setLoadingMessage("Downloading x86_64 Proton")
                    SteamService.downloadFile(
                        onDownloadProgress = { setLoadingProgress(it / 1.0f) },
                        this,
                        context = context,
                        "proton-9.0-x86_64.txz",
                    ).await()
                }
                if (container.wineVersion.contains("proton-9.0-x86_64") || container.wineVersion.contains("proton-9.0-arm64ec")) {
                    val protonVersion = container.wineVersion
                    val imageFs = ImageFs.find(context)
                    val outFile = File(imageFs.rootDir, "/opt/$protonVersion")
                    val binDir = File(outFile, "bin")
                    if (!binDir.exists() || !binDir.isDirectory) {
                        Timber.i("Extracting $protonVersion to /opt/")
                        setLoadingMessage("Extracting $protonVersion")
                        setLoadingProgress(-1f)
                        val downloaded = File(imageFs.filesDir, "$protonVersion.txz")
                        TarCompressorUtils.extract(
                            TarCompressorUtils.Type.XZ,
                            downloaded,
                            outFile,
                        )
                    }
                }
            }

            if (!container.isUseLegacyDRM &&
                !container.isLaunchRealSteam &&
                !SteamService.isFileInstallable(context, "experimental-drm-20260116.tzst")
            ) {
                setLoadingMessage("Downloading extras")
                SteamService.downloadFile(
                    onDownloadProgress = { setLoadingProgress(it / 1.0f) },
                    this,
                    context = context,
                    "experimental-drm-20260116.tzst",
                ).await()
            }
            if (container.isLaunchRealSteam && !SteamService.isFileInstallable(context, "steam.tzst")) {
                setLoadingMessage(context.getString(R.string.main_downloading_steam))
                SteamService.downloadSteam(
                    onDownloadProgress = { setLoadingProgress(it / 1.0f) },
                    this,
                    context = context,
                ).await()
            }
            if (container.isLaunchRealSteam && !SteamService.isFileInstallable(context, "steam-token.tzst")) {
                setLoadingMessage("Downloading steam-token")
                SteamService.downloadFile(
                    onDownloadProgress = { setLoadingProgress(it / 1.0f) },
                    this,
                    context = context,
                    "steam-token.tzst",
                ).await()
            }
        } catch (e: Exception) {
            Timber.tag("preLaunchApp").e(e, "File download failed")
            setLoadingDialogVisible(false)
            setMessageDialogState(
                MessageDialogState(
                    visible = true,
                    type = DialogType.SYNC_FAIL,
                    title = context.getString(R.string.download_failed_title),
                    message = e.message ?: context.getString(R.string.download_failed_message),
                    dismissBtnText = context.getString(R.string.ok),
                ),
            )
            return@launch
        }

        try {
            LaunchDependencies().ensureLaunchDependencies(
                context = context,
                container = container,
                gameSource = gameSource,
                gameId = gameId,
                setLoadingMessage = setLoadingMessage,
                setLoadingProgress = setLoadingProgress,
            )
        } catch (e: Exception) {
            Timber.tag("preLaunchApp").e(e, "ensureLaunchDependencies failed")
            setLoadingDialogVisible(false)
            setMessageDialogState(
                MessageDialogState(
                    visible = true,
                    type = DialogType.SYNC_FAIL,
                    title = context.getString(R.string.launch_dependency_failed_title),
                    message = e.message ?: context.getString(R.string.launch_dependency_failed_message),
                    dismissBtnText = context.getString(R.string.ok),
                ),
            )
            return@launch
        }

        val loadingMessage = if (container.containerVariant.equals(Container.GLIBC)) {
            context.getString(R.string.main_installing_glibc)
        } else {
            context.getString(R.string.main_installing_bionic)
        }
        setLoadingMessage(loadingMessage)
        val imageFsInstallSuccess =
            ImageFsInstaller.installIfNeededFuture(context, context.assets, container) { progress ->
                setLoadingProgress(progress / 100f)
            }.get()

        if (!imageFsInstallSuccess) {
            Timber.tag("preLaunchApp").e("ImageFS installation failed")
            setLoadingDialogVisible(false)
            setMessageDialogState(
                MessageDialogState(
                    visible = true,
                    type = DialogType.SYNC_FAIL,
                    title = context.getString(R.string.install_failed_title),
                    message = context.getString(R.string.install_failed_message),
                    dismissBtnText = context.getString(R.string.ok),
                ),
            )
            return@launch
        }

        setLoadingMessage(context.getString(R.string.main_loading))
        setLoadingProgress(-1f)

        containerManager.activateContainer(container)

        val isSteamGame = gameSource == GameSource.STEAM
        if (isSteamGame) {
            try {
                val currentPlaying = SteamService.getSelfCurrentlyPlayingAppId()
                if (!isOffline && currentPlaying != null && currentPlaying != gameId) {
                    val otherGameName = SteamService.getAppInfoOf(currentPlaying)?.name ?: "another game"
                    setLoadingDialogVisible(false)
                    setMessageDialogState(
                        MessageDialogState(
                            visible = true,
                            type = DialogType.ACCOUNT_SESSION_ACTIVE,
                            title = context.getString(R.string.main_app_running_title),
                            message = context.getString(R.string.main_app_running_message, otherGameName),
                            confirmBtnText = context.getString(R.string.main_play_anyway),
                            dismissBtnText = context.getString(R.string.cancel),
                        ),
                    )
                    return@launch
                }
            } catch (_: Exception) {
                /* ignore persona read errors */
            }
        }

        if (isCustomGame) {
            Timber.tag("preLaunchApp").i("Custom Game detected for $appId - skipping Steam Cloud sync and launching container")
            setLoadingDialogVisible(false)
            onSuccess(context, appId)
            return@launch
        }

        val isGOGGame = gameSource == GameSource.GOG
        if (isGOGGame) {
            Timber.tag("GOG").i("[Cloud Saves] GOG Game detected for $appId - syncing cloud saves before launch")
            Timber.tag("GOG").d("[Cloud Saves] Starting pre-game download sync for $appId")
            val syncSuccess = GOGService.syncCloudSaves(
                context = context,
                appId = appId,
            )

            if (!syncSuccess) {
                Timber.tag("GOG").w("[Cloud Saves] Download sync failed for $appId, proceeding with launch anyway")
            } else {
                Timber.tag("GOG").i("[Cloud Saves] Download sync completed successfully for $appId")
            }

            setLoadingDialogVisible(false)
            onSuccess(context, appId)
            return@launch
        }

        val isAmazonGame = gameSource == GameSource.AMAZON
        if (isAmazonGame) {
            Timber.tag("preLaunchApp").i("Amazon Game detected for $appId - skipping cloud sync and launching container")
            setLoadingDialogVisible(false)
            onSuccess(context, appId)
            return@launch
        }

        val isEpicGame = gameSource == GameSource.EPIC
        if (isEpicGame) {
            Timber.tag("Epic").i("[Cloud Saves] Epic Game detected for $appId - syncing cloud saves before launch")
            Timber.tag("Epic").d("[Cloud Saves] Starting pre-game download sync for $appId")
            val syncSuccess = EpicCloudSavesManager.syncCloudSaves(
                context = context,
                appId = gameId,
            )

            if (!syncSuccess) {
                Timber.tag("Epic").w("[Cloud Saves] Download sync failed for $appId, proceeding with launch anyway")
            } else {
                Timber.tag("Epic").i("[Cloud Saves] Download sync completed successfully for $appId")
            }

            Timber.tag("Epic").i("[Ownership Tokens] Cleaning up launch tokens for Epic games...")
            EpicService.cleanupLaunchTokens(context)

            setLoadingDialogVisible(false)
            onSuccess(context, appId)
            return@launch
        }

        if (skipCloudSync) {
            Timber.tag("preLaunchApp").w("Skipping Steam Cloud sync for $appId by user request")
            setLoadingDialogVisible(false)
            onSuccess(context, appId)
            return@launch
        }

        val prefixToPath: (String) -> String = { prefix ->
            val accountId = SteamService.getSteam3AccountId() ?: 0L
            PathType.from(prefix).toAbsPath(context, gameId, accountId)
        }
        setLoadingMessage("Syncing cloud saves")
        setLoadingProgress(-1f)
        val postSyncInfo = SteamService.beginLaunchApp(
            appId = gameId,
            prefixToPath = prefixToPath,
            ignorePendingOperations = ignorePendingOperations,
            preferredSave = preferredSave,
            parentScope = this,
            isOffline = isOffline,
            onProgress = { message, progress ->
                setLoadingMessage(message)
                setLoadingProgress(if (progress < 0) -1f else progress)
            },
        ).await()

        setLoadingDialogVisible(false)

        when (postSyncInfo.syncResult) {
            SyncResult.Conflict -> {
                setMessageDialogState(
                    MessageDialogState(
                        visible = true,
                        type = DialogType.SYNC_CONFLICT,
                        title = context.getString(R.string.main_save_conflict_title),
                        message = context.getString(
                            R.string.main_save_conflict_message,
                            Date(postSyncInfo.localTimestamp).toString(),
                            Date(postSyncInfo.remoteTimestamp).toString(),
                        ),
                        dismissBtnText = context.getString(R.string.main_keep_local),
                        confirmBtnText = context.getString(R.string.main_keep_remote),
                    ),
                )
            }

            SyncResult.InProgress -> {
                if (useTemporaryOverride && retryCount < 5) {
                    Timber.i("Sync in progress for intent launch, retrying in 2 seconds... (attempt ${retryCount + 1}/5)")
                    delay(2000)
                    preLaunchApp(
                        context = context,
                        appId = appId,
                        ignorePendingOperations = ignorePendingOperations,
                        preferredSave = preferredSave,
                        useTemporaryOverride = useTemporaryOverride,
                        setLoadingDialogVisible = setLoadingDialogVisible,
                        setLoadingProgress = setLoadingProgress,
                        setLoadingMessage = setLoadingMessage,
                        setMessageDialogState = setMessageDialogState,
                        onSuccess = onSuccess,
                        retryCount = retryCount + 1,
                        bootToContainer = bootToContainer,
                    )
                } else {
                    setMessageDialogState(
                        MessageDialogState(
                            visible = true,
                            type = DialogType.SYNC_IN_PROGRESS,
                            title = context.getString(R.string.sync_error_title),
                            message = context.getString(R.string.main_sync_in_progress_launch_anyway_message),
                            confirmBtnText = context.getString(R.string.main_launch_anyway),
                            dismissBtnText = context.getString(R.string.main_wait),
                        ),
                    )
                }
            }

            SyncResult.UnknownFail,
            SyncResult.DownloadFail,
            SyncResult.UpdateFail,
            -> {
                setMessageDialogState(
                    MessageDialogState(
                        visible = true,
                        type = DialogType.SYNC_FAIL,
                        title = context.getString(R.string.sync_error_title),
                        message = context.getString(R.string.main_sync_failed, postSyncInfo.syncResult.toString()),
                        dismissBtnText = context.getString(R.string.ok),
                    ),
                )
            }

            SyncResult.PendingOperations -> {
                Timber.i(
                    "Pending remote operations:${
                        postSyncInfo.pendingRemoteOperations.joinToString("\n") { pro ->
                            "\n\tmachineName: ${pro.machineName}" +
                                "\n\ttimestamp: ${Date(pro.timeLastUpdated * 1000L)}" +
                                "\n\toperation: ${pro.operation}"
                        }
                    }",
                )
                if (postSyncInfo.pendingRemoteOperations.size == 1) {
                    val pro = postSyncInfo.pendingRemoteOperations.first()
                    val gameName = SteamService.getAppInfoOf(ContainerUtils.extractGameIdFromContainerId(appId))?.name ?: ""
                    val dateStr = Date(pro.timeLastUpdated * 1000L).toString()
                    when (pro.operation) {
                        ECloudPendingRemoteOperation.k_ECloudPendingRemoteOperationUploadInProgress -> {
                            setMessageDialogState(
                                MessageDialogState(
                                    visible = true,
                                    type = DialogType.PENDING_UPLOAD_IN_PROGRESS,
                                    title = context.getString(R.string.main_upload_in_progress_title),
                                    message = context.getString(
                                        R.string.main_upload_in_progress_message,
                                        gameName,
                                        pro.machineName,
                                        dateStr,
                                    ),
                                    dismissBtnText = context.getString(R.string.ok),
                                ),
                            )
                        }

                        ECloudPendingRemoteOperation.k_ECloudPendingRemoteOperationUploadPending -> {
                            setMessageDialogState(
                                MessageDialogState(
                                    visible = true,
                                    type = DialogType.PENDING_UPLOAD,
                                    title = context.getString(R.string.main_pending_upload_title),
                                    message = context.getString(
                                        R.string.main_pending_upload_message,
                                        gameName,
                                        pro.machineName,
                                        dateStr,
                                    ),
                                    confirmBtnText = context.getString(R.string.main_play_anyway),
                                    dismissBtnText = context.getString(R.string.cancel),
                                ),
                            )
                        }

                        ECloudPendingRemoteOperation.k_ECloudPendingRemoteOperationAppSessionActive -> {
                            setMessageDialogState(
                                MessageDialogState(
                                    visible = true,
                                    type = DialogType.APP_SESSION_ACTIVE,
                                    title = context.getString(R.string.main_app_running_title),
                                    message = context.getString(
                                        R.string.main_app_running_other_device,
                                        pro.machineName,
                                        gameName,
                                        dateStr,
                                    ),
                                    confirmBtnText = context.getString(R.string.main_play_anyway),
                                    dismissBtnText = context.getString(R.string.cancel),
                                ),
                            )
                        }

                        ECloudPendingRemoteOperation.k_ECloudPendingRemoteOperationAppSessionSuspended -> {
                            setMessageDialogState(
                                MessageDialogState(
                                    visible = true,
                                    type = DialogType.APP_SESSION_SUSPENDED,
                                    title = context.getString(R.string.sync_error_title),
                                    message = context.getString(R.string.main_app_session_suspended),
                                    dismissBtnText = context.getString(R.string.ok),
                                ),
                            )
                        }

                        ECloudPendingRemoteOperation.k_ECloudPendingRemoteOperationNone -> {
                            setMessageDialogState(
                                MessageDialogState(
                                    visible = true,
                                    type = DialogType.PENDING_OPERATION_NONE,
                                    title = context.getString(R.string.sync_error_title),
                                    message = context.getString(R.string.main_pending_operation_none),
                                    dismissBtnText = context.getString(R.string.ok),
                                ),
                            )
                        }
                    }
                } else {
                    setMessageDialogState(
                        MessageDialogState(
                            visible = true,
                            type = DialogType.MULTIPLE_PENDING_OPERATIONS,
                            title = context.getString(R.string.sync_error_title),
                            message = context.getString(R.string.main_multiple_pending_operations),
                            dismissBtnText = context.getString(R.string.ok),
                        ),
                    )
                }
            }

            SyncResult.UpToDate,
            SyncResult.Success,
            -> onSuccess(context, appId)
        }
    }
}

