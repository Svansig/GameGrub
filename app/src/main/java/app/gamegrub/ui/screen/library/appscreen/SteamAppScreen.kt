package app.gamegrub.ui.screen.library.appscreen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.storage.StorageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.gamegrub.GameGrubApp
import app.gamegrub.PrefManager
import app.gamegrub.R
import app.gamegrub.api.compatibility.GameCompatibilityService
import app.gamegrub.api.config.BestConfigService
import app.gamegrub.data.LibraryItem
import app.gamegrub.enums.Marker
import app.gamegrub.enums.PathType
import app.gamegrub.enums.SyncResult
import app.gamegrub.events.AndroidEvent
import app.gamegrub.service.DownloadService
import app.gamegrub.service.steam.SteamPaths
import app.gamegrub.service.steam.SteamService
import app.gamegrub.service.steam.SteamService.Companion.getAppDirPath
import app.gamegrub.ui.model.SteamAppScreenViewModel
import app.gamegrub.ui.component.dialog.GameManagerDialog
import app.gamegrub.ui.component.dialog.LoadingDialog
import app.gamegrub.ui.component.dialog.MessageDialog
import app.gamegrub.ui.component.dialog.state.GameManagerDialogState
import app.gamegrub.ui.component.dialog.state.MessageDialogState
import app.gamegrub.ui.data.AppMenuOption
import app.gamegrub.ui.data.GameDisplayInfo
import app.gamegrub.ui.enums.AppOptionMenuType
import app.gamegrub.ui.enums.DialogType
import app.gamegrub.ui.screen.library.GameMigrationDialog
import app.gamegrub.ui.utils.SnackbarManager
import app.gamegrub.ui.utils.StoragePermissionGate
import app.gamegrub.utils.container.ContainerUtils
import app.gamegrub.utils.container.ContainerUtils.getContainer
import app.gamegrub.utils.manifest.ManifestInstaller
import app.gamegrub.utils.steam.SteamUtils
import app.gamegrub.utils.storage.MarkerUtils
import app.gamegrub.utils.storage.StorageUtils
import com.google.android.play.core.splitcompat.SplitCompat
import com.posthog.PostHog
import com.winlator.container.ContainerData
import com.winlator.container.ContainerManager
import com.winlator.core.GPUInformation
import com.winlator.xenvironment.ImageFsInstaller
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.nio.file.Paths
import kotlin.io.path.pathString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

@EntryPoint
@InstallIn(SingletonComponent::class)
private interface GameCompatibilityServiceEntryPoint {
    fun gameCompatibilityService(): GameCompatibilityService
}

@EntryPoint
@InstallIn(SingletonComponent::class)
private interface BestConfigServiceEntryPoint {
    fun bestConfigService(): BestConfigService
}

private fun getSteamInstallDomain(context: Context): app.gamegrub.service.steam.domain.SteamInstallDomain {
    return EntryPointAccessors
        .fromApplication(context.applicationContext, SteamInstallDomainEntryPoint::class.java)
        .steamInstallDomain()
}

private data class InstallSizeInfo(
    val downloadSize: String,
    val installSize: String,
    val availableSpace: String,
    val installBytes: Long,
    val availableBytes: Long,
)

data class KnownConfigInstallState(
    val visible: Boolean,
    val progress: Float,
    val label: String,
)

private suspend fun installMissingComponentsForConfig(
    context: Context,
    gameId: Int,
    configJson: kotlinx.serialization.json.JsonObject,
    matchType: String,
    uiScope: CoroutineScope,
): Boolean {
    val missingRequests = EntryPointAccessors
        .fromApplication(context.applicationContext, BestConfigServiceEntryPoint::class.java)
        .bestConfigService()
        .resolveMissingManifestInstallRequests(
            context,
            configJson,
            matchType,
        )
    if (missingRequests.isEmpty()) return true

    uiScope.launch(Dispatchers.Main.immediate) {
        SteamAppScreen.showKnownConfigInstallState(
            gameId,
            KnownConfigInstallState(
                visible = true,
                progress = -1f,
                label = missingRequests.first().entry.name,
            ),
        )
    }

    for (request in missingRequests) {
        val label = request.entry.id
        uiScope.launch(Dispatchers.Main.immediate) {
            SteamAppScreen.showKnownConfigInstallState(
                gameId,
                KnownConfigInstallState(
                    visible = true,
                    progress = -1f,
                    label = label,
                ),
            )
        }
        val result = ManifestInstaller.installManifestEntry(
            context = context,
            entry = request.entry,
            isDriver = request.isDriver,
            contentType = request.contentType,
            onProgress = { progress ->
                val clamped = progress.coerceIn(0f, 1f)
                uiScope.launch(Dispatchers.Main.immediate) {
                    SteamAppScreen.showKnownConfigInstallState(
                        gameId,
                        KnownConfigInstallState(
                            visible = true,
                            progress = clamped,
                            label = label,
                        ),
                    )
                }
            },
        )
        SnackbarManager.show(result.message)
        if (!result.success) {
            uiScope.launch(Dispatchers.Main.immediate) { SteamAppScreen.hideKnownConfigInstallState(gameId) }
            return false
        }
    }

    uiScope.launch(Dispatchers.Main.immediate) { SteamAppScreen.hideKnownConfigInstallState(gameId) }
    return true
}

private suspend fun applyConfigForContainer(
    context: Context,
    gameId: Int,
    appId: String,
    configJson: kotlinx.serialization.json.JsonObject,
    matchType: String,
    uiScope: CoroutineScope,
): Boolean {
    return try {
        val installsOk = installMissingComponentsForConfig(
            context,
            gameId,
            configJson,
            matchType,
            uiScope,
        )
        if (!installsOk) return false

        val container = ContainerUtils.getOrCreateContainer(context, appId)
        val containerData = ContainerUtils.toContainerData(container)
        val bestConfigService = EntryPointAccessors
            .fromApplication(context.applicationContext, BestConfigServiceEntryPoint::class.java)
            .bestConfigService()
        val parsedConfig = bestConfigService.parseConfigToContainerData(
            context,
            configJson,
            matchType,
            true,
        )
        val missingContentDescription = bestConfigService.consumeLastMissingContentDescription()
        if (parsedConfig != null && parsedConfig.isNotEmpty()) {
            val updatedContainerData = ContainerUtils.applyBestConfigMapToContainerData(
                containerData,
                parsedConfig,
            )
            ContainerUtils.applyToContainer(context, container, updatedContainerData)
            SnackbarManager.show(context.getString(R.string.best_config_applied_successfully))
        } else {
            val message = if (missingContentDescription != null) {
                context.getString(R.string.best_config_missing_content, missingContentDescription)
            } else {
                context.getString(R.string.best_config_known_config_invalid)
            }
            SnackbarManager.show(message)
        }
        true
    } catch (e: Exception) {
        Timber.w(e, "Failed to apply config: ${e.message}")
        withContext(Dispatchers.Main) {
            SteamAppScreen.hideKnownConfigInstallState(gameId)
        }
        SnackbarManager.show(context.getString(R.string.best_config_apply_failed, e.message ?: "Unknown error"))
        false
    }
}

private fun buildInstallPromptState(context: Context, info: InstallSizeInfo): MessageDialogState {
    val message = context.getString(
        R.string.steam_install_space_prompt,
        info.downloadSize,
        info.installSize,
        info.availableSpace,
    )
    return MessageDialogState(
        visible = true,
        type = DialogType.INSTALL_APP,
        title = context.getString(R.string.download_prompt_title),
        message = message,
        confirmBtnText = context.getString(R.string.proceed),
        dismissBtnText = context.getString(R.string.cancel),
    )
}

private fun buildNotEnoughSpaceState(context: Context, info: InstallSizeInfo): MessageDialogState {
    val message = context.getString(
        R.string.steam_install_not_enough_space,
        info.installSize,
        info.availableSpace,
    )
    return MessageDialogState(
        visible = true,
        type = DialogType.NOT_ENOUGH_SPACE,
        title = context.getString(R.string.not_enough_space),
        message = message,
        confirmBtnText = context.getString(R.string.acknowledge),
    )
}

/**
 * Steam-specific implementation of BaseAppScreen
 */
class SteamAppScreen(
    private val viewModel: SteamAppScreenViewModel,
) : BaseAppScreen() {
    companion object {
        // Shared state for uninstall dialog - list of appIds that should show the dialog
        private val uninstallDialogAppIds = mutableStateListOf<String>()

        fun showUninstallDialog(appId: String) {
            if (!uninstallDialogAppIds.contains(appId)) {
                uninstallDialogAppIds.add(appId)
            }
        }

        fun hideUninstallDialog(appId: String) {
            uninstallDialogAppIds.remove(appId)
        }

        fun shouldShowUninstallDialog(appId: String): Boolean {
            return uninstallDialogAppIds.contains(appId)
        }

        // Shared state for install dialog - map of gameId to MessageDialogState
        private val installDialogStates = mutableStateMapOf<Int, MessageDialogState>()

        fun showInstallDialog(gameId: Int, state: MessageDialogState) {
            installDialogStates[gameId] = state
        }

        fun hideInstallDialog(gameId: Int) {
            installDialogStates.remove(gameId)
        }

        fun getInstallDialogState(gameId: Int): MessageDialogState? {
            return installDialogStates[gameId]
        }

        private val knownConfigInstallStates = mutableStateMapOf<Int, KnownConfigInstallState>()

        fun showKnownConfigInstallState(gameId: Int, state: KnownConfigInstallState) {
            knownConfigInstallStates[gameId] = state
        }

        fun hideKnownConfigInstallState(gameId: Int) {
            knownConfigInstallStates.remove(gameId)
        }

        fun getKnownConfigInstallState(gameId: Int): KnownConfigInstallState? {
            return knownConfigInstallStates[gameId]
        }

        private val gameManagerDialogStates = mutableStateMapOf<Int, GameManagerDialogState>()

        fun showGameManagerDialog(gameId: Int, state: GameManagerDialogState) {
            gameManagerDialogStates[gameId] = state
        }

        fun hideGameManagerDialog(gameId: Int) {
            gameManagerDialogStates.remove(gameId)
        }

        fun getGameManagerDialogState(gameId: Int): GameManagerDialogState? {
            return gameManagerDialogStates[gameId]
        }

        // Shared state for update/verify operation - map of gameId to AppOptionMenuType
        private val pendingUpdateVerifyOperations = mutableStateMapOf<Int, AppOptionMenuType>()

        fun setPendingUpdateVerifyOperation(gameId: Int, operation: AppOptionMenuType?) {
            if (operation != null) {
                pendingUpdateVerifyOperations[gameId] = operation
            } else {
                pendingUpdateVerifyOperations.remove(gameId)
            }
        }

        fun getPendingUpdateVerifyOperation(gameId: Int): AppOptionMenuType? {
            return pendingUpdateVerifyOperations[gameId]
        }
    }

    @Composable
    override fun getGameDisplayInfo(
        context: Context,
        libraryItem: LibraryItem,
    ): GameDisplayInfo {
        val gameId = libraryItem.gameId
        val gameCompatibilityService = remember(context.applicationContext) {
            EntryPointAccessors
                .fromApplication(context.applicationContext, GameCompatibilityServiceEntryPoint::class.java)
                .gameCompatibilityService()
        }
        val appInfo = remember(libraryItem.appId) {
            SteamService.getAppInfoOf(gameId)
        } ?: return GameDisplayInfo(
            name = libraryItem.name,
            developer = "",
            releaseDate = 0L,
            heroImageUrl = null,
            iconUrl = null,
            gameId = gameId,
            appId = libraryItem.appId,
        )

        var isInstalled by remember(libraryItem.appId) {
            mutableStateOf(SteamService.isAppInstalled(gameId))
        }

        DisposableEffect(gameId) {
            val listener: (AndroidEvent.LibraryInstallStatusChanged) -> Unit = { event ->
                if (event.appId == gameId) {
                    isInstalled = SteamService.isAppInstalled(gameId)
                }
            }
            GameGrubApp.events.on<AndroidEvent.LibraryInstallStatusChanged, Unit>(listener)
            onDispose {
                GameGrubApp.events.off<AndroidEvent.LibraryInstallStatusChanged, Unit>(listener)
            }
        }

        // Get hero image URL
        val heroImageUrl = remember(appInfo.id) {
            appInfo.getHeroUrl()
        }

        // Get icon URL
        val iconUrl = remember(appInfo.id) {
            appInfo.iconUrl
        }

        // Get install location
        val installLocation = remember(isInstalled, gameId) {
            if (isInstalled) {
                getAppDirPath(gameId)
            } else {
                null
            }
        }

        // Get size on disk (async, will update via state)
        var sizeOnDisk by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(isInstalled, gameId) {
            if (isInstalled) {
                DownloadService.getSizeOnDiskDisplay(gameId) {
                    sizeOnDisk = it
                }
            } else {
                sizeOnDisk = null
            }
        }

        // Get size from store (async, will update via state)
        var sizeFromStore by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(isInstalled, gameId) {
            if (!isInstalled) {
                // Load size from store on IO, assign on Main to respect Compose threading
                val size = withContext(Dispatchers.IO) {
                    DownloadService.getSizeFromStoreDisplay(gameId)
                }
                sizeFromStore = size
            } else {
                sizeFromStore = null
            }
        }

        // Get last played text
        val lastPlayedText = remember(isInstalled, gameId) {
            if (isInstalled) {
                val path = getAppDirPath(gameId)
                val file = File(path)
                if (file.exists()) {
                    SteamUtils.fromSteamTime((file.lastModified() / 1000).toInt())
                } else {
                    context.getString(R.string.steam_never)
                }
            } else {
                context.getString(R.string.steam_never)
            }
        }

        // Get playtime text
        var playtimeText by remember { mutableStateOf("0 hrs") }
        LaunchedEffect(gameId) {
            val steamID = SteamService.getSteamId64()
            if (steamID != null) {
                val games = SteamService.getOwnedGames(steamID)
                val game = games.firstOrNull { it.appId == gameId }
                playtimeText = if (game != null) {
                    SteamUtils.formatPlayTime(game.playtimeForever) + " hrs"
                } else {
                    "0 hrs"
                }
            }
        }

        // Fetch compatibility via service (cache/network handled internally)
        var compatibilityMessage by remember { mutableStateOf<String?>(null) }
        var compatibilityColor by remember { mutableStateOf<ULong?>(null) }
        LaunchedEffect(isInstalled, gameId, appInfo.name) {
            try {
                val gpuName = GPUInformation.getRenderer(context)

                val message = gameCompatibilityService.getCompatibilityMessageForGame(
                    context = context,
                    gameName = appInfo.name,
                    gpuName = gpuName,
                )

                if (message != null) {
                    compatibilityMessage = message.text
                    compatibilityColor = message.color.value
                } else {
                    compatibilityMessage = null
                    compatibilityColor = null
                }
            } catch (e: Exception) {
                Timber.tag("SteamAppScreen").e(e, "Failed to get compatibility data")
                compatibilityMessage = null
                compatibilityColor = null
            }
        }

        return GameDisplayInfo(
            name = appInfo.name,
            developer = appInfo.developer,
            releaseDate = appInfo.releaseDate,
            heroImageUrl = heroImageUrl,
            iconUrl = iconUrl,
            gameId = gameId,
            appId = libraryItem.appId,
            installLocation = installLocation,
            sizeOnDisk = sizeOnDisk,
            sizeFromStore = sizeFromStore,
            lastPlayedText = lastPlayedText,
            playtimeText = playtimeText,
            compatibilityMessage = compatibilityMessage,
            compatibilityColor = compatibilityColor,
        )
    }

    override fun isInstalled(context: Context, libraryItem: LibraryItem): Boolean {
        return SteamService.isAppInstalled(libraryItem.gameId)
    }

    override fun isValidToDownload(context: Context, libraryItem: LibraryItem): Boolean {
        val appInfo = SteamService.getAppInfoOf(libraryItem.gameId) ?: return false
        return appInfo.branches.isNotEmpty() && appInfo.depots.isNotEmpty()
    }

    override fun isDownloading(context: Context, libraryItem: LibraryItem): Boolean {
        // download job is removed on completion, so non-null means actively downloading
        val installDomain = getSteamInstallDomain(context)
        return installDomain.getAppDownloadInfo(libraryItem.gameId) != null
    }

    override fun getDownloadProgress(context: Context, libraryItem: LibraryItem): Float {
        val installDomain = getSteamInstallDomain(context)
        val downloadInfo = installDomain.getAppDownloadInfo(libraryItem.gameId)
        return downloadInfo?.getProgress() ?: 0f
    }

    override fun hasPartialDownload(context: Context, libraryItem: LibraryItem): Boolean {
        // Use Steam's more accurate check that looks for marker files
        return SteamService.hasPartialDownload(libraryItem.gameId)
    }

    override fun observeGameState(
        context: Context,
        libraryItem: LibraryItem,
        onStateChanged: () -> Unit,
        onProgressChanged: (Float) -> Unit,
        onHasPartialDownloadChanged: ((Boolean) -> Unit)?,
    ): () -> Unit {
        val appId = libraryItem.gameId
        val disposables = mutableListOf<() -> Unit>()

        var progressDisposer = attachDownloadProgressListener(context, appId, onProgressChanged)

        val installListener: (AndroidEvent.LibraryInstallStatusChanged) -> Unit = { event ->
            if (event.appId == appId) {
                onStateChanged()
            }
        }
        GameGrubApp.events.on<AndroidEvent.LibraryInstallStatusChanged, Unit>(installListener)
        disposables += { GameGrubApp.events.off<AndroidEvent.LibraryInstallStatusChanged, Unit>(installListener) }

        val downloadStatusListener: (AndroidEvent.DownloadStatusChanged) -> Unit = { event ->
            if (event.appId == appId) {
                if (event.isDownloading) {
                    progressDisposer?.invoke()
                    progressDisposer = attachDownloadProgressListener(context, appId, onProgressChanged)
                    onHasPartialDownloadChanged?.invoke(true)
                } else {
                    progressDisposer?.invoke()
                    progressDisposer = null
                    if (SteamService.isAppInstalled(appId)) {
                        onHasPartialDownloadChanged?.invoke(false)
                    }
                }
                onStateChanged()
            }
        }
        GameGrubApp.events.on<AndroidEvent.DownloadStatusChanged, Unit>(downloadStatusListener)
        disposables += { GameGrubApp.events.off<AndroidEvent.DownloadStatusChanged, Unit>(downloadStatusListener) }

        val connectivityListener: (AndroidEvent.DownloadPausedDueToConnectivity) -> Unit = { event ->
            if (event.appId == appId) {
                onStateChanged()
            }
        }
        GameGrubApp.events.on<AndroidEvent.DownloadPausedDueToConnectivity, Unit>(connectivityListener)
        disposables += { GameGrubApp.events.off<AndroidEvent.DownloadPausedDueToConnectivity, Unit>(connectivityListener) }

        return {
            progressDisposer?.invoke()
            disposables.forEach { it() }
        }
    }

    private fun attachDownloadProgressListener(
        context: Context,
        appId: Int,
        onProgressChanged: (Float) -> Unit,
    ): (() -> Unit)? {
        val installDomain = getSteamInstallDomain(context)
        val downloadInfo = installDomain.getAppDownloadInfo(appId) ?: return null
        val listener: (Float) -> Unit = { progress ->
            onProgressChanged(progress)
        }
        downloadInfo.addProgressListener(listener)
        onProgressChanged(downloadInfo.getProgress())
        return { downloadInfo.removeProgressListener(listener) }
    }

    override suspend fun isUpdatePendingSuspend(context: Context, libraryItem: LibraryItem): Boolean {
        return SteamService.isUpdatePending(libraryItem.gameId)
    }

    override fun getInstallPath(context: Context, libraryItem: LibraryItem): String? {
        // Only return path if game is installed
        if (isInstalled(context, libraryItem)) {
            return getAppDirPath(libraryItem.gameId)
        }
        return null
    }

    override fun onRunContainerClick(
        context: Context,
        libraryItem: LibraryItem,
        onClickPlay: (Boolean) -> Unit,
    ) {
        val gameId = libraryItem.gameId
        val appInfo = SteamService.getAppInfoOf(gameId)
        PostHog.capture(
            event = "container_opened",
            properties = mapOf("game_name" to (appInfo?.name ?: "")),
        )
        super.onRunContainerClick(context, libraryItem, onClickPlay)
    }

    override fun onDownloadInstallClick(
        context: Context,
        libraryItem: LibraryItem,
        onClickPlay: (Boolean) -> Unit,
    ) {
        val gameId = libraryItem.gameId
        val installDomain = getSteamInstallDomain(context)
        val downloadInfo = installDomain.getAppDownloadInfo(gameId)
        val isDownloading = downloadInfo != null && (downloadInfo.getProgress() ?: 0f) < 1f
        val isInstalled = SteamService.isAppInstalled(gameId)

        if (isDownloading) {
            // Show cancel download dialog
            showInstallDialog(
                gameId,
                MessageDialogState(
                    visible = true,
                    type = DialogType.CANCEL_APP_DOWNLOAD,
                    title = context.getString(R.string.cancel_download_prompt_title),
                    message = context.getString(R.string.steam_cancel_download_message),
                    confirmBtnText = context.getString(R.string.yes),
                    dismissBtnText = context.getString(R.string.no),
                ),
            )
        } else if (SteamService.hasPartialDownload(gameId)) {
            viewModel.downloadApp(gameId) { result -> /* fire and forget */ }
        } else if (!isInstalled) {
            // Request storage permissions first, then show install dialog
            // This will be handled by the permission launcher in AdditionalDialogs
            showGameManagerDialog(
                gameId,
                GameManagerDialogState(
                    visible = true,
                ),
            )
        } else {
            onClickPlay(false)
        }
    }

    override fun onPauseResumeClick(context: Context, libraryItem: LibraryItem) {
        val gameId = libraryItem.gameId
        val installDomain = getSteamInstallDomain(context)
        val downloadInfo = installDomain.getAppDownloadInfo(gameId)

        if (downloadInfo != null) {
            downloadInfo.cancel()
        } else {
            viewModel.downloadApp(gameId) { result -> /* fire and forget */ }
        }
    }

    override fun onDeleteDownloadClick(context: Context, libraryItem: LibraryItem) {
        val gameId = libraryItem.gameId
        val isInstalled = SteamService.isAppInstalled(gameId)
        val installDomain = getSteamInstallDomain(context)
        val downloadInfo = installDomain.getAppDownloadInfo(gameId)
        val isDownloading = downloadInfo != null && (downloadInfo.getProgress() ?: 0f) < 1f

        if (isDownloading || SteamService.hasPartialDownload(gameId)) {
            // Show cancel download dialog when downloading
            showInstallDialog(
                gameId,
                MessageDialogState(
                    visible = true,
                    type = DialogType.CANCEL_APP_DOWNLOAD,
                    title = context.getString(R.string.cancel_download_prompt_title),
                    message = context.getString(R.string.steam_delete_download_message),
                    confirmBtnText = context.getString(R.string.yes),
                    dismissBtnText = context.getString(R.string.no),
                ),
            )
        } else if (isInstalled) {
            // Show uninstall dialog when installed
            showUninstallDialog(libraryItem.appId)
        }
    }

    override fun onUpdateClick(context: Context, libraryItem: LibraryItem) {
        viewModel.downloadApp(libraryItem.gameId) { result -> /* fire and forget */ }
    }

    /**
     * Override Edit Container to check for ImageFS installation first
     */
    @Composable
    override fun getEditContainerOption(
        context: Context,
        libraryItem: LibraryItem,
        onEditContainer: () -> Unit,
    ): AppMenuOption {
        val gameId = libraryItem.gameId
        val appId = libraryItem.appId

        return AppMenuOption(
            optionType = AppOptionMenuType.EditContainer,
            onClick = {
                val container = ContainerUtils.getOrCreateContainer(context, appId)
                val variant = container.containerVariant

                if (!SteamService.isImageFsInstalled(context)) {
                    if (!SteamService.isImageFsInstallable(context, variant)) {
                        showInstallDialog(
                            gameId,
                            MessageDialogState(
                                visible = true,
                                type = DialogType.INSTALL_IMAGEFS,
                                title = context.getString(R.string.steam_imagefs_download_install_title),
                                message = context.getString(R.string.steam_imagefs_download_install_message),
                                confirmBtnText = context.getString(R.string.proceed),
                                dismissBtnText = context.getString(R.string.cancel),
                            ),
                        )
                    } else {
                        showInstallDialog(
                            gameId,
                            MessageDialogState(
                                visible = true,
                                type = DialogType.INSTALL_IMAGEFS,
                                title = context.getString(R.string.steam_imagefs_install_title),
                                message = context.getString(R.string.steam_imagefs_install_message),
                                confirmBtnText = context.getString(R.string.proceed),
                                dismissBtnText = context.getString(R.string.cancel),
                            ),
                        )
                    }
                } else {
                    onEditContainer()
                }
            },
        )
    }

    /**
     * Override Reset Container to show confirmation dialog
     */
    @Composable
    override fun getResetContainerOption(
        context: Context,
        libraryItem: LibraryItem,
    ): AppMenuOption {
        libraryItem.gameId
        var showResetConfirmDialog by remember { mutableStateOf(false) }

        if (showResetConfirmDialog) {
            ResetConfirmDialog(
                onConfirm = {
                    showResetConfirmDialog = false
                    resetContainerToDefaults(context, libraryItem)
                },
                onDismiss = { showResetConfirmDialog = false },
            )
        }

        return AppMenuOption(
            AppOptionMenuType.ResetToDefaults,
            onClick = { showResetConfirmDialog = true },
        )
    }

    @Composable
    override fun getSourceSpecificMenuOptions(
        context: Context,
        libraryItem: LibraryItem,
        onEditContainer: () -> Unit,
        onBack: () -> Unit,
        onClickPlay: (Boolean) -> Unit,
        isInstalled: Boolean,
    ): List<AppMenuOption> {
        val gameId = libraryItem.gameId
        val appId = libraryItem.appId
        val appInfo = SteamService.getAppInfoOf(gameId) ?: return emptyList()
        val isDownloadInProgress = SteamService.getDownloadingAppInfoOf(gameId) != null
        val scope = rememberCoroutineScope()

        val options = mutableListOf<AppMenuOption>(
            AppMenuOption(
                AppOptionMenuType.BrowseOnlineSaves,
                onClick = {
                    val browserIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://store.steampowered.com/account/remotestorageapp/?appid=$gameId"),
                    )
                    context.startActivity(browserIntent)
                },
            ),
        )

        if (!isInstalled || isDownloadInProgress) {
            return options
        }

        // Steam-specific options that only make sense once the game is installed.
        options += listOf(
            AppMenuOption(
                AppOptionMenuType.ResetDrm,
                onClick = {
                    val container = ContainerUtils.getOrCreateContainer(context, appId)
                    MarkerUtils.removeMarker(getAppDirPath(gameId), Marker.STEAM_DLL_REPLACED)
                    MarkerUtils.removeMarker(getAppDirPath(gameId), Marker.STEAM_DLL_RESTORED)
                    MarkerUtils.removeMarker(getAppDirPath(gameId), Marker.STEAM_COLDCLIENT_USED)
                    container.isNeedsUnpacking = true
                    container.saveData()
                },
            ),
            AppMenuOption(
                AppOptionMenuType.ManageGameContent,
                onClick = {
                    showGameManagerDialog(
                        gameId,
                        GameManagerDialogState(
                            visible = true,
                        ),
                    )
                },
            ),
            AppMenuOption(
                AppOptionMenuType.VerifyFiles,
                onClick = {
                    // Show confirmation dialog before verifying
                    setPendingUpdateVerifyOperation(gameId, AppOptionMenuType.VerifyFiles)
                    showInstallDialog(
                        gameId,
                        MessageDialogState(
                            visible = true,
                            type = DialogType.UPDATE_VERIFY_CONFIRM,
                            title = context.getString(R.string.steam_verify_files_title),
                            message = context.getString(R.string.steam_verify_files_message),
                            confirmBtnText = context.getString(R.string.steam_continue),
                            dismissBtnText = context.getString(R.string.cancel),
                        ),
                    )
                },
            ),
            AppMenuOption(
                AppOptionMenuType.Update,
                onClick = {
                    // Show confirmation dialog before updating
                    setPendingUpdateVerifyOperation(gameId, AppOptionMenuType.Update)
                    showInstallDialog(
                        gameId,
                        MessageDialogState(
                            visible = true,
                            type = DialogType.UPDATE_VERIFY_CONFIRM,
                            title = context.getString(R.string.steam_update_title),
                            message = context.getString(R.string.steam_update_message),
                            confirmBtnText = context.getString(R.string.steam_continue),
                            dismissBtnText = context.getString(R.string.cancel),
                        ),
                    )
                },
            ),
            // Uninstall option removed from menu - now handled by delete button next to play button
            // The button uses onDeleteDownloadClick which shows the uninstall dialog
            AppMenuOption(
                AppOptionMenuType.ForceCloudSync,
                onClick = {
                    PostHog.capture(
                        event = "cloud_sync_forced",
                        properties = mapOf("game_name" to appInfo.name),
                    )
                    viewModel.syncUserCloudFiles(appId, gameId) { syncResult ->
                        when (syncResult) {
                            SyncResult.Success -> {
                                SnackbarManager.show(context.getString(R.string.steam_cloud_sync_success))
                            }
                            SyncResult.UpToDate -> {
                                SnackbarManager.show(context.getString(R.string.steam_cloud_sync_up_to_date))
                            }
                            else -> {
                                SnackbarManager.show(
                                    context.getString(
                                        R.string.steam_cloud_sync_failed,
                                        syncResult,
                                    ),
                                )
                            }
                        }
                    }
                },
            ),
            AppMenuOption(
                AppOptionMenuType.UseKnownConfig,
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        try {
                            val gameName = appInfo.name
                            val gpuName = GPUInformation.getRenderer(context)

                            val bestConfig = EntryPointAccessors
                                .fromApplication(context.applicationContext, BestConfigServiceEntryPoint::class.java)
                                .bestConfigService()
                                .fetchBestConfig(gameName, gpuName)
                            if (bestConfig == null) {
                                SnackbarManager.show(context.getString(R.string.best_config_fetch_failed))
                            } else if (bestConfig.matchType == "no_match") {
                                SnackbarManager.show(context.getString(R.string.best_config_no_config_available))
                            } else {
                                applyConfigForContainer(
                                    context,
                                    gameId,
                                    appId,
                                    bestConfig.bestConfig,
                                    bestConfig.matchType,
                                    scope,
                                )
                            }
                        } catch (e: Exception) {
                            Timber.w(e, "Failed to apply known config: ${e.message}")
                            withContext(Dispatchers.Main) {
                                hideKnownConfigInstallState(gameId)
                            }
                            SnackbarManager.show(context.getString(R.string.best_config_apply_failed, e.message ?: "Unknown error"))
                        }
                    }
                },
            ),
        )

        return options
    }

    override fun loadContainerData(context: Context, libraryItem: LibraryItem): ContainerData {
        val container = ContainerUtils.getOrCreateContainer(context, libraryItem.appId)
        return ContainerUtils.toContainerData(container)
    }

    override fun saveContainerConfig(context: Context, libraryItem: LibraryItem, config: ContainerData) {
        val container = getContainer(context, libraryItem.appId)
        ContainerUtils.applyToContainer(context, libraryItem.appId, config)

        if (container.language != config.language) {
            viewModel.downloadApp(libraryItem.gameId) { result -> /* fire and forget */ }
        }
    }

    override fun supportsContainerConfig(): Boolean = true

    override fun getExportFileExtension(): String = ".steam"

    @Composable
    override fun AdditionalDialogs(
        libraryItem: LibraryItem,
        onDismiss: () -> Unit,
        onEditContainer: () -> Unit,
        onBack: () -> Unit,
    ) {
        val context = LocalContext.current
        val gameId = libraryItem.gameId
        val appInfo = remember(libraryItem.appId) {
            SteamService.getAppInfoOf(gameId)
        }

        // Track uninstall dialog state
        var showUninstallDialog by remember { mutableStateOf(shouldShowUninstallDialog(libraryItem.appId)) }

        LaunchedEffect(libraryItem.appId) {
            snapshotFlow { shouldShowUninstallDialog(libraryItem.appId) }
                .collect { shouldShow ->
                    showUninstallDialog = shouldShow
                }
        }

        // Track install dialog state
        var installDialogState by remember(gameId) {
            mutableStateOf(getInstallDialogState(gameId) ?: MessageDialogState(false))
        }

        LaunchedEffect(gameId) {
            snapshotFlow { getInstallDialogState(gameId) }
                .collect { state ->
                    installDialogState = state ?: MessageDialogState(false)
                }
        }

        var knownConfigInstallState by remember(gameId) {
            mutableStateOf(getKnownConfigInstallState(gameId) ?: KnownConfigInstallState(false, -1f, ""))
        }

        LaunchedEffect(gameId) {
            snapshotFlow { getKnownConfigInstallState(gameId) }
                .collect { state ->
                    knownConfigInstallState = state ?: KnownConfigInstallState(false, -1f, "")
                }
        }

        var gameManagerDialogState by remember(gameId) {
            mutableStateOf(getGameManagerDialogState(gameId) ?: GameManagerDialogState(false))
        }

        LaunchedEffect(gameId) {
            snapshotFlow { getGameManagerDialogState(gameId) }
                .collect { state ->
                    gameManagerDialogState = state ?: GameManagerDialogState(false)
                }
        }

        // Migration state
        val scope = rememberCoroutineScope()
        var showMoveDialog by remember { mutableStateOf(false) }
        var showStorageLocationDialog by remember { mutableStateOf(false) }
        var storageLocationConfirmedForInstall by remember(gameId) { mutableStateOf(false) }
        var pendingInstallDlcIds by remember(gameId) { mutableStateOf<List<Int>?>(null) }
        var current by remember { mutableStateOf("") }
        var progress by remember { mutableFloatStateOf(0f) }
        var moved by remember { mutableIntStateOf(0) }
        var total by remember { mutableIntStateOf(0) }
        val sm = context.getSystemService(StorageManager::class.java)
        val externalStorageDirs = remember {
            context.getExternalFilesDirs(null)
                .filterNotNull()
                .filter { Environment.getExternalStorageState(it) == Environment.MEDIA_MOUNTED }
                .filter { sm.getStorageVolume(it)?.isPrimary != true }
        }
        val oldGamesDirectory = remember {
            Paths.get(SteamPaths.defaultAppInstallPath).pathString
        }
        var hasStoragePermission by remember {
            mutableStateOf<Boolean>(StoragePermissionGate.hasStorageAccess(context, SteamPaths.defaultStoragePath))
        }
        var installSizeInfo by remember(gameId) { mutableStateOf<InstallSizeInfo?>(null) }
        fun launchPendingInstall(selectedDlcIds: List<Int>) {
            val installDomain = getSteamInstallDomain(context)
            val installedApp = SteamService.getInstalledApp(gameId)
            if (installedApp != null) {
                // Remove markers if the app is already installed
                MarkerUtils.removeMarker(getAppDirPath(gameId), Marker.STEAM_DLL_REPLACED)
                MarkerUtils.removeMarker(getAppDirPath(gameId), Marker.STEAM_DLL_RESTORED)
                MarkerUtils.removeMarker(getAppDirPath(gameId), Marker.STEAM_COLDCLIENT_USED)
            }

            PostHog.capture(
                event = "game_install_started",
                properties = mapOf("game_name" to (appInfo?.name ?: "")),
            )
            storageLocationConfirmedForInstall = false
            viewModel.downloadAppWithDlc(gameId, selectedDlcIds) { result -> /* fire and forget */ }
        }

        fun showPendingInstallDialog() {
            showInstallDialog(
                gameId,
                MessageDialogState(
                    visible = true,
                    type = DialogType.INSTALL_APP_PENDING,
                ),
            )
        }

        // Permission launcher for game migration
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
            onResult = { _ ->
                scope.launch {
                    showMoveDialog = true
                    StorageUtils.moveGamesFromOldPath(
                        Paths.get(Environment.getExternalStorageDirectory().absolutePath, "GameNative", "Steam").pathString,
                        oldGamesDirectory,
                        onProgressUpdate = { currentFile, fileProgress, movedFiles, totalFiles ->
                            current = currentFile
                            progress = fileProgress
                            moved = movedFiles
                            total = totalFiles
                        },
                        onComplete = {
                            showMoveDialog = false
                        },
                    )
                }
            },
        )

        val requestManageStorageLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) {
            val granted = StoragePermissionGate.hasStorageAccess(context, SteamPaths.defaultStoragePath)
            hasStoragePermission = granted
            if (!granted) {
                SnackbarManager.show(context.getString(R.string.steam_storage_permission_required))
                hideInstallDialog(gameId)
                hideGameManagerDialog(gameId)
                pendingInstallDlcIds = null
            } else {
                showPendingInstallDialog()
            }
        }

        fun continuePendingInstallIfAuthorized() {
            val selectedDlcIds = pendingInstallDlcIds ?: return
            val targetPath = SteamPaths.defaultStoragePath
            val granted = StoragePermissionGate.hasStorageAccess(context, targetPath)
            hasStoragePermission = granted

            if (granted) {
                pendingInstallDlcIds = null
                launchPendingInstall(selectedDlcIds)
                return
            }

            val intent = StoragePermissionGate.createManageStoragePermissionIntent(context)
            if (intent != null) {
                requestManageStorageLauncher.launch(intent)
            } else {
                pendingInstallDlcIds = null
                SnackbarManager.show(context.getString(R.string.steam_storage_permission_required))
            }
        }

        LaunchedEffect(gameId, hasStoragePermission) {
            if (hasStoragePermission != true) {
                installSizeInfo = null
                return@LaunchedEffect
            }
            try {
                val info = withContext(Dispatchers.IO) {
                    val depots = SteamService.getDownloadableDepots(gameId)
                    Timber.i("There are ${depots.size} depots belonging to ${libraryItem.appId}")
                    val availableBytes = StorageUtils.getAvailableSpace(SteamPaths.defaultStoragePath)
                    val downloadBytes = depots.values.sumOf {
                        SteamUtils.getDownloadBytes(it.manifests["public"])
                    }
                    val installBytes = depots.values.sumOf { it.manifests["public"]?.size ?: 0 }
                    InstallSizeInfo(
                        downloadSize = StorageUtils.formatBinarySize(downloadBytes),
                        installSize = StorageUtils.formatBinarySize(installBytes),
                        availableSpace = StorageUtils.formatBinarySize(availableBytes),
                        installBytes = installBytes,
                        availableBytes = availableBytes,
                    )
                }
                installSizeInfo = info
            } catch (e: Exception) {
                Timber.e(e, "Failed to calculate install sizes for gameId=$gameId")
                installSizeInfo = null
            }
        }

        LaunchedEffect(installDialogState.visible, installDialogState.type, hasStoragePermission, installSizeInfo) {
            if (!installDialogState.visible) return@LaunchedEffect
            if (installDialogState.type != DialogType.INSTALL_APP_PENDING) return@LaunchedEffect

            val path = SteamPaths.defaultStoragePath
            val hasAccessNow = StoragePermissionGate.hasStorageAccess(context, path)
            hasStoragePermission = hasAccessNow

            if (!hasAccessNow) {
                if (StoragePermissionGate.shouldRequestManageStoragePermission(context, path)) {
                    val intent = StoragePermissionGate.createManageStoragePermissionIntent(context)
                    if (intent != null) {
                        requestManageStorageLauncher.launch(intent)
                    } else {
                        SnackbarManager.show(context.getString(R.string.steam_storage_permission_required))
                        hideInstallDialog(gameId)
                    }
                } else {
                    SnackbarManager.show(context.getString(R.string.steam_storage_permission_required))
                    hideInstallDialog(gameId)
                }
            } else {
                val info = installSizeInfo ?: return@LaunchedEffect
                val state = if (info.availableBytes < info.installBytes) {
                    buildNotEnoughSpaceState(context, info)
                } else {
                    buildInstallPromptState(context, info)
                }
                showInstallDialog(gameId, state)
            }
        }

        LaunchedEffect(gameManagerDialogState.visible, hasStoragePermission) {
            if (!gameManagerDialogState.visible) return@LaunchedEffect

            val isFreshInstall = !SteamService.isAppInstalled(gameId)
            if (isFreshInstall && !storageLocationConfirmedForInstall && pendingInstallDlcIds == null) {
                hideGameManagerDialog(gameId)
                if (externalStorageDirs.isNotEmpty()) {
                    showStorageLocationDialog = true
                } else {
                    PrefManager.useExternalStorage = false
                    storageLocationConfirmedForInstall = true
                    showGameManagerDialog(gameId, GameManagerDialogState(visible = true))
                }
                return@LaunchedEffect
            }

            val path = SteamPaths.defaultStoragePath
            val hasAccessNow = StoragePermissionGate.hasStorageAccess(context, path)
            hasStoragePermission = hasAccessNow

            if (!hasAccessNow) {
                if (StoragePermissionGate.shouldRequestManageStoragePermission(context, path)) {
                    val intent = StoragePermissionGate.createManageStoragePermissionIntent(context)
                    if (intent != null) {
                        requestManageStorageLauncher.launch(intent)
                    } else {
                        SnackbarManager.show(context.getString(R.string.steam_storage_permission_required))
                    }
                }
            }
        }

        LoadingDialog(
            visible = knownConfigInstallState.visible,
            progress = knownConfigInstallState.progress,
            message = if (knownConfigInstallState.label.isNotEmpty()) {
                context.getString(R.string.manifest_downloading_item, knownConfigInstallState.label)
            } else {
                context.getString(R.string.working)
            },
        )

        // Install dialog (INSTALL_APP, NOT_ENOUGH_SPACE, CANCEL_APP_DOWNLOAD)
        if (installDialogState.visible) {
            val onDismissRequest: () -> Unit = {
                hideInstallDialog(gameId)
            }
            val onDismissClick: () -> Unit = {
                hideInstallDialog(gameId)
            }
            val onConfirmClick: (() -> Unit)? = when (installDialogState.type) {
                DialogType.INSTALL_APP_PENDING -> null

                DialogType.INSTALL_APP -> {
                    {
                        hideInstallDialog(gameId)
                        if (pendingInstallDlcIds != null) {
                            continuePendingInstallIfAuthorized()
                        } else {
                            val installDomain = getSteamInstallDomain(context)
                            PostHog.capture(
                                event = "game_install_started",
                                properties = mapOf("game_name" to (appInfo?.name ?: "")),
                            )
            viewModel.downloadApp(gameId) { result -> /* fire and forget */ }
                        }
                    }
                }

                DialogType.NOT_ENOUGH_SPACE -> {
                    {
                        hideInstallDialog(gameId)
                    }
                }

                DialogType.CANCEL_APP_DOWNLOAD -> {
                    {
                        val installDomain = getSteamInstallDomain(context)
                        PostHog.capture(
                            event = "game_install_cancelled",
                            properties = mapOf("game_name" to (appInfo?.name ?: "")),
                        )
                        val downloadInfo = installDomain.getAppDownloadInfo(gameId)
                        downloadInfo?.cancel()
                        viewModel.deleteApp(gameId) {
                            hideInstallDialog(gameId)
                        }
                    }
                }

                DialogType.UPDATE_VERIFY_CONFIRM -> {
                    {
                        hideInstallDialog(gameId)
                        val installDomain = getSteamInstallDomain(context)
                        val operation = getPendingUpdateVerifyOperation(gameId)
                        setPendingUpdateVerifyOperation(gameId, null)

                        if (operation != null) {
                            viewModel.verifyAppWithCloudSync(
                                appId = gameId,
                                shouldSyncCloud = (operation == AppOptionMenuType.VerifyFiles),
                                onComplete = { /* No additional UI actions needed */ },
                            )
                        }
                    }
                }

                DialogType.INSTALL_IMAGEFS -> {
                    {
                        hideInstallDialog(gameId)
                        // Install ImageFS with loading progress
                        // Note: This should ideally show a loading dialog, but for now we'll do it in the background
                        viewModel.installImageFs(gameId) {
                            // ImageFS installation completed
                        }
                    }
                }

                else -> null
            }

            MessageDialog(
                visible = installDialogState.visible,
                onDismissRequest = onDismissRequest,
                onConfirmClick = onConfirmClick,
                onDismissClick = onDismissClick,
                confirmBtnText = installDialogState.confirmBtnText,
                dismissBtnText = installDialogState.dismissBtnText,
                title = installDialogState.title,
                message = installDialogState.message,
            )
        }

        // Uninstall confirmation dialog
        if (showUninstallDialog) {
            AlertDialog(
                onDismissRequest = {
                    hideUninstallDialog(libraryItem.appId)
                },
                title = { Text(stringResource(R.string.steam_uninstall_game_title)) },
                text = {
                    Text(
                        text = stringResource(
                            R.string.steam_uninstall_confirmation_message,
                            appInfo?.name ?: libraryItem.name,
                        ),
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            hideUninstallDialog(libraryItem.appId)

                            viewModel.deleteAppWithContainerCleanup(gameId) { result ->
                                result.fold(
                                    onSuccess = {
                                        GameGrubApp.events.emit(AndroidEvent.LibraryInstallStatusChanged(gameId))
                                        SnackbarManager.show(
                                            context.getString(
                                                R.string.steam_uninstall_success,
                                                appInfo?.name ?: libraryItem.name,
                                            ),
                                        )
                                        PostHog.capture(
                                            event = "game_uninstalled",
                                            properties = mapOf("game_name" to (appInfo?.name ?: "")),
                                        )
                                    },
                                    onFailure = {
                                        SnackbarManager.show(context.getString(R.string.steam_uninstall_failed))
                                    },
                                )
                            }
                        },
                    ) {
                        Text(stringResource(R.string.uninstall), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            hideUninstallDialog(libraryItem.appId)
                        },
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                },
            )
        }

        if (showMoveDialog) {
            GameMigrationDialog(
                progress = progress,
                currentFile = current,
                movedFiles = moved,
                totalFiles = total,
            )
        }

        if (showStorageLocationDialog) {
            AlertDialog(
                onDismissRequest = {
                    showStorageLocationDialog = false
                    storageLocationConfirmedForInstall = false
                    pendingInstallDlcIds = null
                },
                title = { Text(stringResource(R.string.steam_storage_location_title)) },
                text = { Text(stringResource(R.string.steam_storage_location_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            PrefManager.useExternalStorage = false
                            storageLocationConfirmedForInstall = true
                            showStorageLocationDialog = false
                            if (pendingInstallDlcIds == null) {
                                showGameManagerDialog(gameId, GameManagerDialogState(visible = true))
                            } else {
                                showPendingInstallDialog()
                            }
                        },
                    ) {
                        Text(stringResource(R.string.steam_storage_location_internal))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            if (externalStorageDirs.isEmpty()) {
                                SnackbarManager.show(context.getString(R.string.settings_interface_no_external_storage))
                                return@TextButton
                            }

                            PrefManager.useExternalStorage = true
                            val currentPath = PrefManager.externalStoragePath
                            if (currentPath.isBlank() || externalStorageDirs.none { it.absolutePath == currentPath }) {
                                PrefManager.externalStoragePath = externalStorageDirs.first().absolutePath
                            }

                            storageLocationConfirmedForInstall = true
                            showStorageLocationDialog = false
                            if (pendingInstallDlcIds == null) {
                                showGameManagerDialog(gameId, GameManagerDialogState(visible = true))
                            } else {
                                showPendingInstallDialog()
                            }
                        },
                    ) {
                        Text(stringResource(R.string.steam_storage_location_external))
                    }
                },
            )
        }

        if (gameManagerDialogState.visible) {
            GameManagerDialog(
                visible = true,
                onGetDisplayInfo = { context ->
                    return@GameManagerDialog getGameDisplayInfo(context, libraryItem)
                },
                onInstall = { dlcAppIds ->
                    hideGameManagerDialog(gameId)

                    pendingInstallDlcIds = dlcAppIds
                    if (storageLocationConfirmedForInstall) {
                        showPendingInstallDialog()
                    } else if (externalStorageDirs.isNotEmpty()) {
                        showStorageLocationDialog = true
                    } else {
                        PrefManager.useExternalStorage = false
                        showPendingInstallDialog()
                    }
                },
                onDismissRequest = {
                    storageLocationConfirmedForInstall = false
                    pendingInstallDlcIds = null
                    hideGameManagerDialog(gameId)
                },
            )
        }
    }
}
