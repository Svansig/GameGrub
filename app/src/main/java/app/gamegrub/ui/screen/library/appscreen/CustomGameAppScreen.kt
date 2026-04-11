package app.gamegrub.ui.screen.library.appscreen

import android.content.Context
import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.gamegrub.PrefManager
import app.gamegrub.R
import app.gamegrub.api.steamGridDB.SteamGridDB
import app.gamegrub.data.LibraryItem
import app.gamegrub.domain.customgame.CustomGameScanner
import app.gamegrub.domain.customgame.ExeIconExtractor
import app.gamegrub.domain.customgame.GameMetadataManager
import app.gamegrub.events.AndroidEvent
import app.gamegrub.storage.StorageManager
import app.gamegrub.ui.data.AppMenuOption
import app.gamegrub.ui.data.GameDisplayInfo
import app.gamegrub.ui.enums.AppOptionMenuType
import app.gamegrub.ui.runtime.XServerRuntime
import app.gamegrub.ui.utils.SnackbarManager
import app.gamegrub.utils.container.ContainerUtils
import com.winlator.container.ContainerData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

/**
 * Custom Game-specific implementation of BaseAppScreen
 */
class CustomGameAppScreen(
    private val steamGridDB: SteamGridDB,
) : BaseAppScreen() {
    companion object {
        // Shared state for delete dialog - list of appIds that should show the dialog
        private val deleteDialogAppIds = mutableStateListOf<String>()

        fun showDeleteDialog(appId: String) {
            if (!deleteDialogAppIds.contains(appId)) {
                deleteDialogAppIds.add(appId)
            }
        }

        fun hideDeleteDialog(appId: String) {
            deleteDialogAppIds.remove(appId)
        }

        fun shouldShowDeleteDialog(appId: String): Boolean {
            return deleteDialogAppIds.contains(appId)
        }
    }

    @Composable
    override fun getGameDisplayInfo(
        context: Context,
        libraryItem: LibraryItem,
    ): GameDisplayInfo {
        val gameFolderPath = remember(libraryItem.appId) {
            CustomGameScanner.get().getFolderPathFromAppId(libraryItem.appId)
        }

        // Helper function to find SteamGridDB images in the game folder
        fun findSteamGridDBImage(folder: File, imageType: String): String? {
            return folder.listFiles()?.firstOrNull { file ->
                file.name.startsWith("steamgriddb_$imageType") &&
                        (
                                file.name.endsWith(".png", ignoreCase = true) ||
                                        file.name.endsWith(".jpg", ignoreCase = true) ||
                                        file.name.endsWith(".webp", ignoreCase = true)
                                )
            }?.let { Uri.fromFile(it).toString() }
        }

        // Check for all SteamGridDB images in the game folder
        // Hero view uses horizontal grid (grid_hero)
        val heroImageUrl = remember(gameFolderPath) {
            gameFolderPath?.let { path ->
                val folder = File(path)
                findSteamGridDBImage(folder, "grid_hero")
            }
        }

        // Capsule view uses vertical grid (grid_capsule)
        val capsuleUrl = remember(gameFolderPath) {
            gameFolderPath?.let { path ->
                val folder = File(path)
                findSteamGridDBImage(folder, "grid_capsule")
            }
        }

        // Header view uses heroes endpoint (hero, but not grid_hero)
        val headerUrl = remember(gameFolderPath) {
            gameFolderPath?.let { path ->
                val folder = File(path)
                // Find hero image but exclude grid_hero
                folder.listFiles()?.firstOrNull { file ->
                    file.name.startsWith("steamgriddb_hero") &&
                            !file.name.contains("grid") &&
                            (
                                    file.name.endsWith(".png", ignoreCase = true) ||
                                            file.name.endsWith(".jpg", ignoreCase = true) ||
                                            file.name.endsWith(".webp", ignoreCase = true)
                                    )
                }?.let { Uri.fromFile(it).toString() }
            }
        }

        val logoUrl = remember(gameFolderPath) {
            gameFolderPath?.let { path ->
                val folder = File(path)
                findSteamGridDBImage(folder, "logo")
            }
        }

        // Note: iconUrl is intentionally null - we extract icons from exe files
        // and don't use SteamGridDB icons

        // Try to get release date from .gamenative metadata if available
        var releaseDate by remember { mutableLongStateOf(0L) }
        LaunchedEffect(gameFolderPath) {
            gameFolderPath?.let { path ->
                val folder = File(path)
                // Get release date from metadata
                val metadata = GameMetadataManager.read(folder)
                releaseDate = metadata?.releaseDate ?: 0L
            }
        }

        // Calculate folder size on disk (async, will update via state)
        var sizeOnDisk by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(gameFolderPath) {
            gameFolderPath?.let { path ->
                withContext(Dispatchers.IO) {
                    try {
                        val folderSize = StorageManager.getFolderSize(path)
                        val formattedSize = StorageManager.formatBinarySize(folderSize)
                        sizeOnDisk = formattedSize
                    } catch (e: Exception) {
                        // Ignore errors, sizeOnDisk will remain null
                    }
                }
            }
        }

        // Custom Games don't have Steam metadata, so we use basic info
        return GameDisplayInfo(
            name = libraryItem.name,
            developer = context.getString(R.string.custom_game_unknown_developer), // Custom Games don't have developer info
            releaseDate = releaseDate,
            heroImageUrl = heroImageUrl,
            iconUrl = null, // Icons are extracted from exe files, not from SteamGridDB
            gameId = libraryItem.gameId,
            appId = libraryItem.appId,
            installLocation = gameFolderPath,
            sizeOnDisk = sizeOnDisk, // Calculated folder size
            sizeFromStore = null, // No store size info
            lastPlayedText = null, // Not tracked
            playtimeText = null, // Not tracked
            logoUrl = logoUrl,
            capsuleUrl = capsuleUrl,
            headerUrl = headerUrl,
        )
    }

    override fun isInstalled(context: Context, libraryItem: LibraryItem): Boolean {
        // Custom Games are always considered "installed" since they're external
        return true
    }

    override fun isValidToDownload(context: Context, libraryItem: LibraryItem): Boolean {
        // Custom Games cannot be downloaded through the app
        return false
    }

    override fun isDownloading(context: Context, libraryItem: LibraryItem): Boolean {
        // Custom Games don't have downloads
        return false
    }

    override fun getDownloadProgress(context: Context, libraryItem: LibraryItem): Float {
        return 0f
    }

    override fun isUpdatePending(context: Context, libraryItem: LibraryItem): Boolean {
        return false
    }

    override fun onDownloadInstallClick(
        context: Context,
        libraryItem: LibraryItem,
        onClickPlay: (Boolean) -> Unit,
    ) {
        // Launch the game; preLaunchApp will show EXECUTABLE_NOT_FOUND if no exe (getLaunchExecutable blank)
        XServerRuntime.get().events.emit(AndroidEvent.ExternalGameLaunch(libraryItem.appId))
    }

    override fun onPauseResumeClick(context: Context, libraryItem: LibraryItem) {
        // Not applicable for Custom Games
    }

    override fun onDeleteDownloadClick(context: Context, libraryItem: LibraryItem) {
        // Show delete confirmation dialog for Custom Games
        showDeleteDialog(libraryItem.appId)
    }

    override fun onUpdateClick(context: Context, libraryItem: LibraryItem) {
        // Not applicable for Custom Games
    }

    override fun getInstallPath(context: Context, libraryItem: LibraryItem): String? {
        // Custom Games are always "installed" (they're external folders)
        return CustomGameScanner.get().getFolderPathFromAppId(libraryItem.appId)
    }

    override fun getGameFolderPathForImageFetch(context: Context, libraryItem: LibraryItem): String? {
        // For Custom Games, the install path is the same as the folder path
        val path = getInstallPath(context, libraryItem)
        Timber.tag("CustomGameAppScreen").d("getGameFolderPathForImageFetch - appId: ${libraryItem.appId}, path: ${path ?: "null"}")
        return path
    }

    override fun onAfterFetchImages(context: Context, libraryItem: LibraryItem, gameFolderPath: String) {
        // Extract icon from executable after fetching images
        Timber.tag("CustomGameAppScreen").d("onAfterFetchImages called - appId: ${libraryItem.appId}, gameFolderPath: $gameFolderPath")

        // Verify the path was resolved correctly, but use gameFolderPath as fallback
        val resolvedPath = getInstallPath(context, libraryItem)
        Timber.tag("CustomGameAppScreen").d("Resolved install path: ${resolvedPath ?: "null"}")

        // Use resolvedPath if available, otherwise fall back to gameFolderPath (which was validated by caller)
        val actualGameFolderPath = resolvedPath ?: gameFolderPath
        if (resolvedPath != gameFolderPath && resolvedPath != null) {
            Timber.tag("CustomGameAppScreen")
                .w("Path mismatch! gameFolderPath: $gameFolderPath, resolvedPath: $resolvedPath, using: $actualGameFolderPath")
        } else if (resolvedPath == null) {
            Timber.tag("CustomGameAppScreen").w("Could not resolve install path, using provided gameFolderPath: $gameFolderPath")
        }

        val gameFolder = File(actualGameFolderPath)
        if (gameFolder.exists() && gameFolder.isDirectory) {
            Timber.tag("CustomGameAppScreen").i("Extracting icon from executable after fetching images")
            try {
                // Check if icon already exists by using the same method the UI uses
                val existingIconPath = CustomGameScanner.get().findIconFileForCustomGame(context, libraryItem.appId)
                val hasExtractedIcon = existingIconPath != null && existingIconPath.endsWith(".extracted.ico", ignoreCase = true)

                Timber.tag("CustomGameAppScreen")
                    .d("Icon check - existingIconPath: ${existingIconPath ?: "null"}, hasExtractedIcon: $hasExtractedIcon")

                // Also check if there's an extracted icon file anywhere in the folder (limited depth search)
                // Limit search to 2 levels deep to avoid performance issues with large game folders
                fun findExtractedIconLimited(dir: File, depth: Int = 0, maxDepth: Int = 2): File? {
                    if (depth > maxDepth) return null
                    dir.listFiles()?.forEach { file ->
                        if (file.isDirectory) {
                            val found = findExtractedIconLimited(file, depth + 1, maxDepth)
                            if (found != null) return found
                        } else if (file.name.endsWith(".extracted.ico", ignoreCase = true)) {
                            return file
                        }
                    }
                    return null
                }

                val extractedIconFile = findExtractedIconLimited(gameFolder)
                Timber.tag("CustomGameAppScreen").d("Recursive search found extracted icon: ${extractedIconFile?.absolutePath ?: "null"}")

                // If findIconFileForCustomGame didn't find an extracted icon, but one exists, we should still try to extract
                // (maybe the container path isn't set or the icon is in a different location)
                val shouldExtract = !hasExtractedIcon || (extractedIconFile != null && existingIconPath != extractedIconFile.absolutePath)

                if (shouldExtract) {
                    // First, try using the container's selected executable if available
                    val containerManager = com.winlator.container.ContainerManager(context)
                    val hasContainer = containerManager.hasContainer(libraryItem.appId)
                    Timber.tag("CustomGameAppScreen").d("Container exists: $hasContainer")

                    if (hasContainer) {
                        val container = containerManager.getContainerById(libraryItem.appId)
                        val relExe = container.executablePath
                        Timber.tag("CustomGameAppScreen").d("Container executable path: ${relExe ?: "null"}")

                        if (!relExe.isNullOrEmpty()) {
                            val exeFile = File(gameFolder, relExe.replace('/', File.separatorChar))
                            Timber.tag("CustomGameAppScreen")
                                .d("Checking executable file: ${exeFile.absolutePath}, exists: ${exeFile.exists()}")

                            if (exeFile.exists()) {
                                val outIco = File(exeFile.parentFile, exeFile.nameWithoutExtension + ".extracted.ico")
                                Timber.tag("CustomGameAppScreen").d("Attempting to extract icon to: ${outIco.absolutePath}")
                                val extracted = ExeIconExtractor.tryExtractMainIcon(exeFile, outIco)
                                Timber.tag("CustomGameAppScreen").d("Icon extraction result: $extracted")

                                if (extracted) {
                                    Timber.tag("CustomGameAppScreen").d("Extracted icon from selected executable: ${exeFile.name}")
                                } else {
                                    Timber.tag("CustomGameAppScreen").w("Failed to extract icon from selected executable: ${exeFile.name}")
                                }
                            }
                        }
                    }

                    // If that didn't work, try finding a unique executable
                    val uniqueExeRel = CustomGameScanner.get().findUniqueExeRelativeToFolder(gameFolder)
                    Timber.tag("CustomGameAppScreen").d("Unique executable found: ${uniqueExeRel ?: "null"}")

                    if (!uniqueExeRel.isNullOrEmpty()) {
                        val exeFile = File(gameFolder, uniqueExeRel.replace('/', File.separatorChar))
                        Timber.tag("CustomGameAppScreen")
                            .d("Checking unique executable file: ${exeFile.absolutePath}, exists: ${exeFile.exists()}")

                        if (exeFile.exists()) {
                            val outIco = File(exeFile.parentFile, exeFile.nameWithoutExtension + ".extracted.ico")
                            // Only extract if we haven't already extracted from the selected exe
                            if (!outIco.exists()) {
                                Timber.tag("CustomGameAppScreen").d("Attempting to extract icon to: ${outIco.absolutePath}")
                                val extracted = ExeIconExtractor.tryExtractMainIcon(exeFile, outIco)
                                Timber.tag("CustomGameAppScreen").d("Icon extraction result: $extracted")

                                if (extracted) {
                                    Timber.tag("CustomGameAppScreen").d("Extracted icon from unique executable: ${exeFile.name}")
                                } else {
                                    Timber.tag("CustomGameAppScreen").w("Failed to extract icon from unique executable: ${exeFile.name}")
                                }
                            } else {
                                Timber.tag("CustomGameAppScreen").d("Icon file already exists: ${outIco.absolutePath}")
                            }
                        }
                    } else {
                        Timber.tag("CustomGameAppScreen").w("No unique executable found in folder: $gameFolderPath")
                    }
                } else {
                    Timber.tag("CustomGameAppScreen").d("Icon already exists, skipping extraction")
                }
            } catch (e: Exception) {
                Timber.tag("CustomGameAppScreen").e(e, "Failed to extract icon from executable")
                // Silently continue - icon extraction is optional
            }
        } else {
            Timber.tag("CustomGameAppScreen").e("Game folder does not exist: $gameFolderPath")
        }
    }

    /**
     * Override Reset Container to show confirmation dialog and preserve drives
     */
    @Composable
    override fun getResetContainerOption(
        context: Context,
        libraryItem: LibraryItem,
    ): AppMenuOption {
        var showResetConfirmDialog by remember { mutableStateOf(false) }

        if (showResetConfirmDialog) {
            ResetConfirmDialog(
                onConfirm = {
                    resetContainerToDefaults(context, libraryItem)
                },
                onDismiss = { },
            )
        }

        return AppMenuOption(
            optionType = AppOptionMenuType.ResetToDefaults,
            onClick = { },
        )
    }

    /**
     * Custom Game-specific menu options
     */
    @Composable
    override fun getSourceSpecificMenuOptions(
        context: Context,
        libraryItem: LibraryItem,
        onEditContainer: () -> Unit,
        onBack: () -> Unit,
        onClickPlay: (Boolean) -> Unit,
        isInstalled: Boolean,
    ): List<AppMenuOption> {
        val options = mutableListOf<AppMenuOption>()

        // Fetch images from SteamGridDB for Custom Games
        options.add(getFetchImagesOption(context, libraryItem))

        return options
    }

    @Composable
    private fun getFetchImagesOption(context: Context, libraryItem: LibraryItem): AppMenuOption {
        return AppMenuOption(
            optionType = AppOptionMenuType.FetchSteamGridDBImages,
            onClick = {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val gameName = libraryItem.name
                        val gameFolderPath = getGameFolderPathForImageFetch(context, libraryItem)

                        if (gameFolderPath != null) {
                            val folder = File(gameFolderPath)
                            val appId = libraryItem.gameId
                            GameMetadataManager.update(
                                folder = folder,
                                appId = appId,
                                steamgriddbFetched = false,
                            )

                            steamGridDB.fetchGameImages(gameName, gameFolderPath)
                            XServerRuntime.get().events.emit(AndroidEvent.CustomGameImagesFetched(libraryItem.appId))
                            onAfterFetchImages(context, libraryItem, gameFolderPath)

                            SnackbarManager.show(context.getString(R.string.base_app_images_fetched))
                        } else {
                            SnackbarManager.show(context.getString(R.string.base_app_game_folder_not_found))
                        }
                    } catch (e: Exception) {
                        SnackbarManager.show(context.getString(R.string.base_app_images_fetch_failed, e.message ?: ""))
                    }
                }
            },
        )
    }

    override fun loadContainerData(context: Context, libraryItem: LibraryItem): ContainerData {
        val container = ContainerUtils.getOrCreateContainer(context, libraryItem.appId)
        return ContainerUtils.toContainerData(container)
    }

    override fun saveContainerConfig(context: Context, libraryItem: LibraryItem, config: ContainerData) {
        ContainerUtils.applyToContainer(context, libraryItem.appId, config)
    }

    override fun supportsContainerConfig(): Boolean = true

    override fun getExportFileExtension(): String = ".pcgame"

    @Composable
    override fun AdditionalDialogs(
        libraryItem: LibraryItem,
        onDismiss: () -> Unit,
        onEditContainer: () -> Unit,
        onBack: () -> Unit,
    ) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        // Track delete dialog state
        var showDeleteDialog by remember { mutableStateOf(shouldShowDeleteDialog(libraryItem.appId)) }

        LaunchedEffect(libraryItem.appId) {
            snapshotFlow { shouldShowDeleteDialog(libraryItem.appId) }
                .collect { shouldShow ->
                    showDeleteDialog = shouldShow
                }
        }

        // Delete confirmation dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = {
                    hideDeleteDialog(libraryItem.appId)
                },
                title = { Text(stringResource(R.string.custom_game_delete_title)) },
                text = {
                    Text(text = stringResource(R.string.custom_game_delete_message, libraryItem.name))
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            hideDeleteDialog(libraryItem.appId)

                            // Delete the game folder and container
                            scope.launch {
                                try {
                                    // Delete the container first (needs to be on main thread)
                                    withContext(Dispatchers.Main) {
                                        ContainerUtils.deleteContainer(context, libraryItem.appId)
                                    }

                                    // Remove from manual folders list and invalidate cache
                                    withContext(Dispatchers.IO) {
                                        val folderPath = CustomGameScanner.get().getFolderPathFromAppId(libraryItem.appId)
                                        if (folderPath != null) {
                                            val manualFolders = PrefManager.customGameManualFolders.toMutableSet()
                                            manualFolders.remove(folderPath)
                                            PrefManager.customGameManualFolders = manualFolders
                                        }
                                        CustomGameScanner.get().invalidateCache()
                                    }

                                    // Navigate back and show notification
                                    SnackbarManager.show("\"${libraryItem.name}\" has been deleted")

                                    withContext(Dispatchers.Main) {
                                        // Small delay to ensure file system updates are complete
                                        // before navigating back (list will auto-refresh when displayed)
                                        delay(100)

                                        // Navigate back to game list
                                        onBack()
                                    }
                                } catch (e: Exception) {
                                    SnackbarManager.show("Failed to delete game: ${e.message}")
                                }
                            }
                        },
                    ) {
                        Text("Delete", color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            hideDeleteDialog(libraryItem.appId)
                        },
                    ) {
                        Text("Cancel")
                    }
                },
            )
        }
    }
}
