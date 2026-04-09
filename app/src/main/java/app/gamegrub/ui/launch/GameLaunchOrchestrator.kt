package app.gamegrub.ui.launch

import android.content.Context
import androidx.lifecycle.viewModelScope
import app.gamegrub.LaunchRequestManager
import app.gamegrub.R
import app.gamegrub.api.config.BestConfigService
import app.gamegrub.container.launch.dependency.LaunchDependencies
import app.gamegrub.content.manifest.ManifestInstaller
import app.gamegrub.data.GameSource
import app.gamegrub.domain.customgame.CustomGameScanner
import app.gamegrub.enums.PathType
import app.gamegrub.enums.SaveLocation
import app.gamegrub.enums.SyncResult
import app.gamegrub.launch.LaunchEngine
import app.gamegrub.launch.LaunchOptions
import app.gamegrub.launch.LaunchResult
import app.gamegrub.launch.trackGameLaunched
import app.gamegrub.service.amazon.AmazonService
import app.gamegrub.service.epic.EpicCloudSavesManager
import app.gamegrub.service.epic.EpicService
import app.gamegrub.service.gog.GOGService
import app.gamegrub.service.steam.SteamService
import app.gamegrub.session.SessionAssembler
import app.gamegrub.telemetry.record.LaunchOutcome
import app.gamegrub.telemetry.record.LaunchRecordStore
import app.gamegrub.telemetry.record.SessionMilestone
import app.gamegrub.telemetry.session.LaunchFingerprint
import app.gamegrub.telemetry.session.LaunchFingerprintEmitter
import app.gamegrub.telemetry.session.LaunchMilestone
import app.gamegrub.telemetry.session.MilestoneEmitter
import app.gamegrub.ui.component.dialog.state.MessageDialogState
import app.gamegrub.ui.enums.AppOptionMenuType
import app.gamegrub.ui.enums.DialogType
import app.gamegrub.ui.model.MainViewModel
import app.gamegrub.utils.container.ContainerUtils
import com.google.android.play.core.splitcompat.SplitCompat
import com.winlator.container.Container
import com.winlator.container.ContainerManager
import com.winlator.core.TarCompressorUtils
import com.winlator.xenvironment.ImageFs
import com.winlator.xenvironment.ImageFsInstaller
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesClientObjects.ECloudPendingRemoteOperation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import timber.log.Timber
import java.io.File
import java.util.Date
import kotlin.reflect.KFunction2

@EntryPoint
@InstallIn(SingletonComponent::class)
private interface BestConfigServiceEntryPoint {
    fun bestConfigService(): BestConfigService
}

@EntryPoint
@InstallIn(SingletonComponent::class)
private interface SessionEntryPoint {
    fun sessionAssembler(): SessionAssembler
    fun launchEngine(): LaunchEngine
    fun launchRecordStore(): LaunchRecordStore
}

private const val SYNC_IN_PROGRESS_MAX_RETRIES = 5
private const val SYNC_IN_PROGRESS_RETRY_DELAY_MS = 2000L

internal fun shouldRetrySyncInProgress(
    useTemporaryOverride: Boolean,
    retryCount: Int,
): Boolean {
    return useTemporaryOverride && retryCount < SYNC_IN_PROGRESS_MAX_RETRIES
}

/**
 * Handles a resolved external launch by setting ViewModel state and running pre-launch setup.
 * @param context App context.
 * @param appId Encoded app id to launch.
 * @param useTemporaryOverride Whether the temporary container override is used.
 * @param viewModel Main screen ViewModel used for launch state updates.
 * @param setMessageDialogState UI callback for dialog updates.
 */
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
        scope = viewModel.viewModelScope,
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

/**
 * Prepares runtime dependencies, sync state, and container activation before launching a title.
 *
 * This function runs on `Dispatchers.IO` and updates UI state through callback parameters.
 *
 * @param scope Parent coroutine scope used to launch the preparation job.
 * @param context App context.
 * @param appId Encoded app id being launched.
 * @param ignorePendingOperations Whether Steam should ignore pending remote operations.
 * @param preferredSave Preferred save conflict resolution strategy.
 * @param useTemporaryOverride Whether to use temporary container override values.
 * @param skipCloudSync Whether Steam cloud sync should be skipped.
 * @param setLoadingDialogVisible UI callback for loading dialog visibility.
 * @param setLoadingProgress UI callback for loading progress updates.
 * @param setLoadingMessage UI callback for loading status text.
 * @param setMessageDialogState UI callback for error/conflict dialog updates.
 * @param onSuccess Callback invoked when launch can continue.
 * @param retryCount Retry count for in-progress sync handling.
 * @param isOffline Whether launch should avoid online Steam session checks.
 * @param bootToContainer Whether launch targets container boot instead of game executable.
 */
fun preLaunchApp(
    scope: CoroutineScope,
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

    scope.launch(Dispatchers.IO) {
        val containerManager = ContainerManager(context)
        val container = if (useTemporaryOverride) {
            ContainerUtils.getOrCreateContainerWithOverride(context, appId)
        } else {
            ContainerUtils.getOrCreateContainer(context, appId)
        }

        container.clearSessionMetadata()

        val gameSource = ContainerUtils.extractGameSourceFromContainerId(appId)

        val fingerprint = LaunchFingerprint(
            containerId = appId,
            containerPath = container.rootDir.absolutePath,
            gamePlatform = gameSource.name,
            gameTitle = ContainerUtils.resolveGameName(appId),
            wineVersion = container.wineVersion,
            dxwrapper = container.getExtra("dxwrapper"),
            containerVariant = container.containerVariant,
        )
        LaunchFingerprintEmitter.emit(fingerprint)
        fingerprint.logAtMilestone("LAUNCH_REQUEST_RECEIVED")
        MilestoneEmitter.startSession(fingerprint.sessionId)
        MilestoneEmitter.record(LaunchMilestone.LAUNCH_REQUEST_QUEUED, mapOf("appId" to appId))
        MilestoneEmitter.record(LaunchMilestone.ASSEMBLY_START, mapOf("wineVersion" to container.wineVersion))

        val sessionAssembler = EntryPointAccessors
            .fromApplication(context.applicationContext, SessionEntryPoint::class.java)
            .sessionAssembler()
        val launchEngine = EntryPointAccessors
            .fromApplication(context.applicationContext, SessionEntryPoint::class.java)
            .launchEngine()
        val launchRecordStore = EntryPointAccessors
            .fromApplication(context.applicationContext, SessionEntryPoint::class.java)
            .launchRecordStore()

        val gameId = ContainerUtils.extractGameIdFromContainerId(appId)

        val sessionPlan = sessionAssembler.assemble(
            gameId = gameId.toString(),
            gameTitle = ContainerUtils.resolveGameName(appId),
            gamePlatform = gameSource.name,
        ).getOrElse {
            Timber.e(it, "Failed to assemble session for $appId")
            MilestoneEmitter.record(LaunchMilestone.LAUNCH_FAILED, mapOf("reason" to it.message.orEmpty()))
            setLoadingDialogVisible(false)
            setMessageDialogState(
                MessageDialogState(
                    visible = true,
                    type = DialogType.EXECUTABLE_NOT_FOUND,
                    title = context.getString(R.string.game_launch_failed),
                    message = it.message ?: "Failed to assemble launch session",
                    dismissBtnText = context.getString(R.string.ok),
                ),
            )
            return@launch
        }

        MilestoneEmitter.record(
            LaunchMilestone.ASSEMBLY_COMPLETE,
            mapOf(
                "sessionId" to sessionPlan.sessionId,
                "runtimeId" to (sessionPlan.composition as? app.gamegrub.session.model.SessionComposition.Full)?.runtime?.id.orEmpty(),
                "baseId" to (sessionPlan.composition as? app.gamegrub.session.model.SessionComposition.Full)?.base?.id.orEmpty(),
            ),
        )

        if (!bootToContainer) {
            // Resolve a concrete executable before expensive setup work so we can fail fast.
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
                // Ensure manifest-provisioned components requested by best config are installed.
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
            // Download and unpack runtime payloads (ImageFS, Wine/Proton, DRM, Steam assets) on demand.
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
            fingerprint.logAtMilestone("IMAGEFS_INSTALL_FAILED")
            MilestoneEmitter.record(LaunchMilestone.LAUNCH_FAILED, mapOf("reason" to "imagefs_install_failed"))
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
        fingerprint.logAtMilestone("CONTAINER_ACTIVATED")
        MilestoneEmitter.record(LaunchMilestone.CONTAINER_READY, mapOf("containerPath" to container.rootDir.absolutePath))

        val isSteamGame = gameSource == GameSource.STEAM
        if (isSteamGame) {
            try {
                // Guard against concurrent active session conflicts on another running Steam title.
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
            // Custom games do not participate in store cloud flows.
            Timber.tag("preLaunchApp").i("Custom Game detected for $appId - skipping Steam Cloud sync and launching container")
            fingerprint.logAtMilestone("LAUNCH_SUCCESS")
            MilestoneEmitter.record(LaunchMilestone.PROCESS_SPAWNED)
            setLoadingDialogVisible(false)
            onSuccess(context, appId)
            return@launch
        }

        val isGOGGame = gameSource == GameSource.GOG
        if (isGOGGame) {
            // GOG cloud saves are best-effort; launch continues even if pre-sync fails.
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

            fingerprint.logAtMilestone("LAUNCH_SUCCESS")
            MilestoneEmitter.record(LaunchMilestone.PROCESS_SPAWNED)
            setLoadingDialogVisible(false)
            onSuccess(context, appId)
            return@launch
        }

        val isAmazonGame = gameSource == GameSource.AMAZON
        if (isAmazonGame) {
            // Amazon titles currently launch without cloud sync.
            Timber.tag("preLaunchApp").i("Amazon Game detected for $appId - skipping cloud sync and launching container")
            fingerprint.logAtMilestone("LAUNCH_SUCCESS")
            MilestoneEmitter.record(LaunchMilestone.PROCESS_SPAWNED)
            setLoadingDialogVisible(false)
            onSuccess(context, appId)
            return@launch
        }

        val isEpicGame = gameSource == GameSource.EPIC
        if (isEpicGame) {
            // Epic uses pre-launch cloud sync plus token cleanup before entering the container.
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

            fingerprint.logAtMilestone("LAUNCH_SUCCESS")
            MilestoneEmitter.record(LaunchMilestone.PROCESS_SPAWNED)
            setLoadingDialogVisible(false)
            onSuccess(context, appId)
            return@launch
        }

        if (skipCloudSync) {
            Timber.tag("preLaunchApp").w("Skipping Steam Cloud sync for $appId by user request")
            fingerprint.logAtMilestone("LAUNCH_SUCCESS")
            MilestoneEmitter.record(LaunchMilestone.PROCESS_SPAWNED)
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

        // Explicitly route each sync outcome to UI conflict dialogs or launch continuation.
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
                if (shouldRetrySyncInProgress(useTemporaryOverride, retryCount)) {
                    Timber.i(
                        "Sync in progress for intent launch, retrying in 2 seconds... (attempt ${retryCount + 1}/$SYNC_IN_PROGRESS_MAX_RETRIES)",
                    )
                    delay(SYNC_IN_PROGRESS_RETRY_DELAY_MS)
                    preLaunchApp(
                        scope = scope,
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
                -> {
                MilestoneEmitter.record(LaunchMilestone.ASSEMBLY_COMPLETE, mapOf("sessionId" to sessionPlan.sessionId))

                val launchStartTime = System.currentTimeMillis()
                val launchResult = launchEngine.execute(sessionPlan, LaunchOptions())
                val launchEndTime = System.currentTimeMillis()

                when (launchResult) {
                    is LaunchResult.Success -> {
                        fingerprint.logAtMilestone("LAUNCH_SUCCESS")
                        MilestoneEmitter.record(LaunchMilestone.PROCESS_SPAWNED)
                        MilestoneEmitter.record(LaunchMilestone.GAME_INTERACTIVE)

                        CoroutineScope(Dispatchers.IO).launch {
                            saveLaunchRecord(
                                recordStore = launchRecordStore,
                                sessionId = sessionPlan.sessionId,
                                titleId = gameId.toString(),
                                titleName = sessionPlan.metadata.gameTitle,
                                sessionPlan = sessionPlan,
                                outcome = LaunchOutcome.SUCCESS,
                                exitCode = launchResult.processId?.toInt(),
                                startTime = launchStartTime,
                                endTime = launchEndTime,
                            )
                        }
                        onSuccess(context, appId)
                    }

                    is LaunchResult.Failure -> {
                        Timber.e("Launch failed: ${launchResult.reason}")
                        MilestoneEmitter.record(
                            LaunchMilestone.LAUNCH_FAILED,
                            mapOf(
                                "reason" to launchResult.reason,
                                "exitCode" to (launchResult.exitCode?.toString() ?: "none"),
                            ),
                        )

                        CoroutineScope(Dispatchers.IO).launch {
                            saveLaunchRecord(
                                recordStore = launchRecordStore,
                                sessionId = sessionPlan.sessionId,
                                titleId = gameId.toString(),
                                titleName = sessionPlan.metadata.gameTitle,
                                sessionPlan = sessionPlan,
                                outcome = LaunchOutcome.FAILURE,
                                exitCode = launchResult.exitCode,
                                startTime = launchStartTime,
                                endTime = launchEndTime,
                                errorMessage = launchResult.reason,
                            )
                        }
                        setLoadingDialogVisible(false)
                        setMessageDialogState(
                            MessageDialogState(
                                visible = true,
                                type = DialogType.GAME_LAUNCH_FAILED,
                                title = context.getString(R.string.game_launch_failed),
                                message = launchResult.reason,
                                dismissBtnText = context.getString(R.string.ok),
                            ),
                        )
                    }

                    is LaunchResult.Cancelled -> {
                        MilestoneEmitter.record(LaunchMilestone.LAUNCH_FAILED, mapOf("reason" to "Launch cancelled"))

                        CoroutineScope(Dispatchers.IO).launch {
                            saveLaunchRecord(
                                recordStore = launchRecordStore,
                                sessionId = sessionPlan.sessionId,
                                titleId = gameId.toString(),
                                titleName = sessionPlan.metadata.gameTitle,
                                sessionPlan = sessionPlan,
                                outcome = LaunchOutcome.CANCELLED,
                                startTime = launchStartTime,
                                endTime = launchEndTime,
                            )
                        }
                    }
                }
            }
        }
    }
}

private suspend fun saveLaunchRecord(
    recordStore: LaunchRecordStore,
    sessionId: String,
    titleId: String,
    titleName: String,
    sessionPlan: app.gamegrub.session.model.SessionPlan,
    outcome: LaunchOutcome,
    exitCode: Int? = null,
    startTime: Long,
    endTime: Long,
    errorMessage: String? = null,
) {
    val milestones = MilestoneEmitter.getRecorder().getMilestones().map { record ->
        SessionMilestone(
            milestone = record.milestone.name,
            timestamp = record.timestamp,
            metadata = record.metadata,
        )
    }

    val fullComposition = sessionPlan.composition as? app.gamegrub.session.model.SessionComposition.Full

    val record = app.gamegrub.telemetry.record.LaunchSessionRecord(
        sessionId = sessionId,
        titleId = titleId,
        titleName = titleName,
        deviceClass = android.os.Build.MODEL,
        baseId = fullComposition?.base?.id,
        runtimeId = fullComposition?.runtime?.id,
        driverId = fullComposition?.driver?.id,
        profileId = null,
        outcome = outcome,
        exitCode = exitCode,
        startTime = startTime,
        endTime = endTime,
        durationMs = endTime - startTime,
        milestones = milestones,
        errorMessage = errorMessage,
    )

    recordStore.saveRecord(record)
}
