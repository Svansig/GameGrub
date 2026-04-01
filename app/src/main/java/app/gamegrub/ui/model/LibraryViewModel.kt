package app.gamegrub.ui.model

import android.content.Context
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.gamegrub.GameGrubApp
import app.gamegrub.PrefManager
import app.gamegrub.R
import app.gamegrub.api.compatibility.GameCompatibilityService
import app.gamegrub.data.AmazonGame
import app.gamegrub.data.EpicGame
import app.gamegrub.data.GOGGame
import app.gamegrub.data.GameCompatibilityStatus
import app.gamegrub.data.GameSource
import app.gamegrub.data.LibraryItem
import app.gamegrub.data.SteamLibraryApp
import app.gamegrub.db.dao.AmazonGameDao
import app.gamegrub.db.dao.EpicGameDao
import app.gamegrub.db.dao.GOGGameDao
import app.gamegrub.db.dao.SteamAppDao
import app.gamegrub.events.AndroidEvent
import app.gamegrub.service.DownloadService
import app.gamegrub.service.amazon.AmazonService
import app.gamegrub.service.epic.EpicService
import app.gamegrub.service.gog.GOGService
import app.gamegrub.service.steam.SteamService
import app.gamegrub.ui.data.LibraryState
import app.gamegrub.ui.enums.AppFilter
import app.gamegrub.ui.enums.LibraryTab
import app.gamegrub.ui.enums.LibraryTab.Companion.next
import app.gamegrub.ui.enums.LibraryTab.Companion.previous
import app.gamegrub.ui.enums.SortOption
import app.gamegrub.utils.game.CustomGameScanner
import app.gamegrub.utils.general.unaccent
import com.winlator.core.GPUInformation
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.EnumSet
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val steamAppDao: SteamAppDao,
    private val gogGameDao: GOGGameDao,
    private val epicGameDao: EpicGameDao,
    private val amazonGameDao: AmazonGameDao,
    private val gameCompatibilityService: GameCompatibilityService,
    private val customGameScanner: CustomGameScanner,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    private data class PlatformAuthState(
        val steamLoggedIn: Boolean,
        val gogLoggedIn: Boolean,
        val epicLoggedIn: Boolean,
        val amazonLoggedIn: Boolean,
    )

    private val _state = MutableStateFlow(LibraryState(isLoading = true))
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    // Keep the library scroll state. This will last longer as the VM will stay alive.
    var listState: LazyGridState by mutableStateOf(LazyGridState(0, 0))

    private val onInstallStatusChanged: (AndroidEvent.LibraryInstallStatusChanged) -> Unit = {
        onFilterApps(paginationCurrentPage)
    }

    private val onCustomGameImagesFetched: (AndroidEvent.CustomGameImagesFetched) -> Unit = {
        // Increment refresh counter and refresh the library list to pick up newly fetched images
        _state.update { it.copy(imageRefreshCounter = it.imageRefreshCounter + 1) }
        onFilterApps(paginationCurrentPage)
    }

    // How many items loaded on one page of results
    @Volatile
    private var paginationCurrentPage: Int = 0

    @Volatile
    private var lastPageInCurrentFilter: Int = 0

    // Complete and unfiltered app list
    private var appList: List<SteamLibraryApp> = emptyList()
    private var gogGameList: List<GOGGame> = emptyList()
    private var epicGameList: List<EpicGame> = emptyList()
    private var amazonGameList: List<AmazonGame> = emptyList()

    // Track if this is the first load to apply minimum load time
    private var isFirstLoad = true

    // Track debounce job for search
    private var searchDebounceJob: Job? = null
    private val searchDebounceMs = 500L // 500ms debounce

    // Cache GPU name to avoid repeated calls
    private val gpuName: String by lazy {
        try {
            val gpu = GPUInformation.getRenderer(context)
            if (gpu.isNullOrEmpty()) {
                Timber.tag("LibraryViewModel").w("GPU name is null or empty")
                "Unknown GPU"
            } else {
                Timber.tag("LibraryViewModel").d("Retrieved GPU name: $gpu")
                gpu
            }
        } catch (e: Exception) {
            Timber.tag("LibraryViewModel").e(e, "Failed to get GPU name")
            "Unknown GPU"
        }
    }

    init {
        refreshPlatformAuthState()

        viewModelScope.launch(Dispatchers.IO) {
            steamAppDao.getAllOwnedLibraryApps(
                // ownerIds = SteamService.familyMembers.ifEmpty { listOf((SteamService.getSteam3AccountId() ?: 0L).toInt()) },
            ).collect { apps ->
                // Room can emit frequently during sync; only react when payload actually changes.
                if (appList != apps) {
                    Timber.tag("LibraryViewModel").d("Collecting ${apps.size} apps")
                    appList = apps
                    onFilterApps(paginationCurrentPage)
                }
            }
        }

        // Collect GOG games
        viewModelScope.launch(Dispatchers.IO) {
            gogGameDao.getAll().collect { games ->
                Timber.tag("LibraryViewModel").d("Collecting ${games.size} GOG games")
                // Check if the list has actually changed before triggering a re-filter
                if (gogGameList != games) {
                    gogGameList = games
                    onFilterApps(paginationCurrentPage)
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            epicGameDao.getAll().collect { games ->
                Timber.tag("LibraryViewModel").d("Collecting ${games.size} Epic games")

                val hasChanges = epicGameList.size != games.size || epicGameList != games
                epicGameList = games

                if (hasChanges) {
                    onFilterApps(paginationCurrentPage)
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            amazonGameDao.getAll().collect { games ->
                Timber.tag("LibraryViewModel").d("Collecting ${games.size} Amazon games")
                val hasChanges = amazonGameList.size != games.size || amazonGameList != games
                amazonGameList = games
                if (hasChanges) {
                    onFilterApps(paginationCurrentPage)
                }
            }
        }

        GameGrubApp.events.on<AndroidEvent.LibraryInstallStatusChanged, Unit>(onInstallStatusChanged)
        GameGrubApp.events.on<AndroidEvent.CustomGameImagesFetched, Unit>(onCustomGameImagesFetched)
    }

    override fun onCleared() {
        searchDebounceJob?.cancel()
        GameGrubApp.events.off<AndroidEvent.LibraryInstallStatusChanged, Unit>(onInstallStatusChanged)
        GameGrubApp.events.off<AndroidEvent.CustomGameImagesFetched, Unit>(onCustomGameImagesFetched)
        super.onCleared()
    }

    fun onModalBottomSheet(value: Boolean) {
        _state.update { it.copy(modalBottomSheet = value) }
    }

    fun onIsSearching(value: Boolean) {
        _state.update { it.copy(isSearching = value) }
        if (!value) {
            onSearchQuery("")
        }
    }

    fun onSourceToggle(source: GameSource) {
        val current = _state.value
        when (source) {
            GameSource.STEAM -> {
                val newValue = !current.showSteamInLibrary
                PrefManager.showSteamInLibrary = newValue
                _state.update { it.copy(showSteamInLibrary = newValue) }
            }

            GameSource.CUSTOM_GAME -> {
                val newValue = !current.showCustomGamesInLibrary
                PrefManager.showCustomGamesInLibrary = newValue
                _state.update { it.copy(showCustomGamesInLibrary = newValue) }
            }

            GameSource.GOG -> {
                val newValue = !current.showGOGInLibrary
                PrefManager.showGOGInLibrary = newValue
                _state.update { it.copy(showGOGInLibrary = newValue) }
            }

            GameSource.EPIC -> {
                val newValue = !current.showEpicInLibrary
                PrefManager.showEpicInLibrary = newValue
                _state.update { it.copy(showEpicInLibrary = newValue) }
            }

            GameSource.AMAZON -> {
                val newValue = !current.showAmazonInLibrary
                PrefManager.showAmazonInLibrary = newValue
                _state.update { it.copy(showAmazonInLibrary = newValue) }
            }
        }
        onFilterApps(paginationCurrentPage)
    }

    fun onSortOptionChanged(sortOption: SortOption) {
        PrefManager.librarySortOption = sortOption
        _state.update { it.copy(currentSortOption = sortOption) }
        onFilterApps()
    }

    fun onOptionsPanelToggle(isOpen: Boolean) {
        _state.update { it.copy(isOptionsPanelOpen = isOpen) }
    }

    fun onTabChanged(tab: LibraryTab) {
        _state.update { it.copy(currentTab = tab) }
        onFilterApps(0) // Reset to first page and refresh
    }

    fun onNextTab() {
        _state.update { currentState ->
            val nextTab = currentState.currentTab.next()
            Timber.tag("LibraryViewModel").d("Tab next via bumper: ${currentState.currentTab} -> $nextTab")
            currentState.copy(currentTab = nextTab)
        }
        onFilterApps(0)
    }

    fun onPreviousTab() {
        _state.update { currentState ->
            val previousTab = currentState.currentTab.previous()
            Timber.tag("LibraryViewModel").d("Tab previous via bumper: ${currentState.currentTab} -> $previousTab")
            currentState.copy(currentTab = previousTab)
        }
        onFilterApps(0)
    }

    fun onSearchQuery(value: String) {
        // Update UI immediately for responsive typing
        _state.update { it.copy(searchQuery = value) }

        // Cancel previous debounce job
        searchDebounceJob?.cancel()

        // Start new debounce job
        searchDebounceJob = viewModelScope.launch {
            delay(searchDebounceMs)
            // Only trigger filter after user stops typing
            onFilterApps()
        }
    }

    // TODO: include other sort types
    fun onFilterChanged(value: AppFilter) {
        _state.update { currentState ->
            val updatedFilter = EnumSet.copyOf(currentState.appInfoSortType)

            if (updatedFilter.contains(value)) {
                updatedFilter.remove(value)
            } else {
                updatedFilter.add(value)
            }

            PrefManager.libraryFilter = updatedFilter

            currentState.copy(appInfoSortType = updatedFilter)
        }

        onFilterApps()
    }

    fun onPageChange(pageIncrement: Int) {
        // Amount to change by
        var toPage = max(0, paginationCurrentPage + pageIncrement)
        toPage = min(toPage, lastPageInCurrentFilter)
        onFilterApps(toPage)
    }

    fun onRefresh() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }

            refreshPlatformAuthState()

            // Clear compatibility cache on manual refresh to get fresh data
            gameCompatibilityService.clearCache()

            try {
                val newApps = SteamService.refreshOwnedGamesFromServer()
                if (newApps > 0) {
                    Timber.tag("LibraryViewModel").i("Queued $newApps newly owned games for PICS sync")
                } else {
                    Timber.tag("LibraryViewModel").d("No newly owned games discovered during refresh")
                }
                if (GOGService.hasStoredCredentials(context)) {
                    Timber.tag("LibraryViewModel").i("Triggering GOG library refresh")
                    GOGService.triggerLibrarySync(context)
                }
                if (AmazonService.hasStoredCredentials(context)) {
                    Timber.tag("LibraryViewModel").i("Triggering Amazon library refresh")
                    AmazonService.triggerLibrarySync(context)
                }
            } catch (e: Exception) {
                Timber.tag("LibraryViewModel").e(e, "Failed to refresh owned games from server")
            } finally {
                onFilterApps(0).join()
                // Fetch compatibility for current page after refresh
                val currentPageGames = _state.value.appInfoList.map { it.name }
                if (currentPageGames.isNotEmpty()) {
                    fetchCompatibilityForPage(currentPageGames)
                }
                _state.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun addCustomGameFolder(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val normalizedPath = File(path).absolutePath
            val libraryItem = customGameScanner.createLibraryItemFromFolder(normalizedPath)
            if (libraryItem == null) {
                Timber.tag("LibraryViewModel").w("Selected folder is not a valid custom game: $normalizedPath")
                return@launch
            }

            val manualFolders = PrefManager.customGameManualFolders.toMutableSet()
            if (!manualFolders.contains(normalizedPath)) {
                manualFolders.add(normalizedPath)
                PrefManager.customGameManualFolders = manualFolders
            }

            customGameScanner.invalidateCache()
            onFilterApps(paginationCurrentPage)
        }
    }

    fun onGogOAuthResult(
        resultCode: Int,
        authCode: String?,
        errorMessage: String?,
    ) {
        val payload = OAuthResultPayload(
            resultCode = resultCode,
            authCode = authCode,
            errorMessage = errorMessage,
        )
        val message = LibraryAuthResultParser.resolveErrorMessage(
            payload = payload,
            cancelFallbackMessage = context.getString(R.string.gog_login_cancel),
        )
        if (message != null) {
            postAuthMessage(message)
            return
        }

        val code = authCode ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching { GOGService.authenticateWithCode(context, code) }.getOrElse {
                Timber.tag("LibraryViewModel").e(it, "GOG authentication failed")
                postAuthMessage(context.getString(R.string.gog_login_cancel))
                return@launch
            }

            if (result.isSuccess) {
                GOGService.start(context)
                GOGService.triggerLibrarySync(context)
                postAuthMessage(context.getString(R.string.gog_login_success_title))
                refreshPlatformAuthState()
                onFilterApps(paginationCurrentPage)
            } else {
                postAuthMessage(result.exceptionOrNull()?.message ?: context.getString(R.string.gog_login_cancel))
            }
        }
    }

    fun onEpicOAuthResult(
        resultCode: Int,
        authCode: String?,
        errorMessage: String?,
    ) {
        val payload = OAuthResultPayload(
            resultCode = resultCode,
            authCode = authCode,
            errorMessage = errorMessage,
        )
        val message = LibraryAuthResultParser.resolveErrorMessage(
            payload = payload,
            cancelFallbackMessage = context.getString(R.string.epic_login_cancel),
        )
        if (message != null) {
            postAuthMessage(message)
            return
        }

        val code = authCode ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching { EpicService.authenticateWithCode(context, code) }.getOrElse {
                Timber.tag("LibraryViewModel").e(it, "Epic authentication failed")
                postAuthMessage(context.getString(R.string.epic_login_cancel))
                return@launch
            }

            if (result.isSuccess) {
                EpicService.start(context)
                EpicService.triggerLibrarySync(context)
                postAuthMessage(context.getString(R.string.epic_login_success_title))
                refreshPlatformAuthState()
                onFilterApps(paginationCurrentPage)
            } else {
                postAuthMessage(result.exceptionOrNull()?.message ?: context.getString(R.string.epic_login_cancel))
            }
        }
    }

    fun onAmazonOAuthResult(
        resultCode: Int,
        authCode: String?,
        errorMessage: String?,
    ) {
        val payload = OAuthResultPayload(
            resultCode = resultCode,
            authCode = authCode,
            errorMessage = errorMessage,
        )
        val message = LibraryAuthResultParser.resolveErrorMessage(
            payload = payload,
            cancelFallbackMessage = context.getString(R.string.amazon_login_cancel),
        )
        if (message != null) {
            postAuthMessage(message)
            return
        }

        val code = authCode ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching { AmazonService.authenticateWithCode(context, code) }.getOrElse {
                Timber.tag("LibraryViewModel").e(it, "Amazon authentication failed")
                postAuthMessage(context.getString(R.string.amazon_login_cancel))
                return@launch
            }

            if (result.isSuccess) {
                AmazonService.start(context)
                AmazonService.triggerLibrarySync(context)
                postAuthMessage(context.getString(R.string.amazon_login_success_title))
                refreshPlatformAuthState()
                onFilterApps(paginationCurrentPage)
            } else {
                postAuthMessage(result.exceptionOrNull()?.message ?: context.getString(R.string.amazon_login_cancel))
            }
        }
    }

    fun refreshPlatformAuthState() {
        viewModelScope.launch(Dispatchers.IO) {
            val auth = snapshotPlatformAuthState()
            _state.update {
                it.copy(
                    isSteamLoggedIn = auth.steamLoggedIn,
                    isGogLoggedIn = auth.gogLoggedIn,
                    isEpicLoggedIn = auth.epicLoggedIn,
                    isAmazonLoggedIn = auth.amazonLoggedIn,
                )
            }
        }
    }

    fun onAuthMessageShown(eventId: Long) {
        _state.update {
            if (it.authMessageEventId == eventId) {
                it.copy(authMessage = null)
            } else {
                it
            }
        }
    }

    private fun postAuthMessage(message: String) {
        _state.update {
            it.copy(
                authMessage = message,
                authMessageEventId = it.authMessageEventId + 1,
            )
        }
    }

    private fun snapshotPlatformAuthState(): PlatformAuthState {
        return PlatformAuthState(
            steamLoggedIn = SteamService.isLoggedIn,
            gogLoggedIn = GOGService.hasStoredCredentials(context),
            epicLoggedIn = EpicService.hasStoredCredentials(context),
            amazonLoggedIn = AmazonService.hasStoredCredentials(context),
        )
    }

    private fun onFilterApps(paginationPage: Int = 0): Job {
        Timber.tag("LibraryViewModel").d("onFilterApps - appList.size: ${appList.size}, isFirstLoad: $isFirstLoad")
        return viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isLoading = true) }

            val currentState = _state.value
            val currentFilter = AppFilter.getAppType(currentState.appInfoSortType)

            // Fetch download directory apps once on IO thread and cache as a HashSet for O(1) lookups
            val downloadDirectoryApps = DownloadService.getDownloadDirectoryApps()
            val downloadDirectorySet = downloadDirectoryApps.toHashSet()

            fun passesCompatibleFilter(gameName: String): Boolean {
                if (!currentState.appInfoSortType.contains(AppFilter.COMPATIBLE)) {
                    return true
                }
                val status = currentState.compatibilityMap[gameName] ?: return true
                return status == GameCompatibilityStatus.COMPATIBLE || status == GameCompatibilityStatus.GPU_COMPATIBLE
            }

            val steamFilteredBeforeCompatibility: List<SteamLibraryApp> = appList
                .asSequence()
                .filter { item ->
                    SteamService.familyMembers.ifEmpty {
                        SteamService.getSteam3AccountId()?.toInt()?.let { accountId ->
                            listOf(accountId)
                        } ?: emptyList()
                    }.let { owners ->
                        if (owners.isEmpty()) {
                            true // no owner info ⇒ don’t filter the item out
                        } else {
                            owners.any { item.ownerAccountId.contains(it) }
                        }
                    }
                }
                .filter { item ->
                    currentFilter.any { item.type == it }
                }
                .filter { item ->
                    if (currentState.appInfoSortType.contains(AppFilter.SHARED)) {
                        true
                    } else {
                        item.ownerAccountId.contains(PrefManager.steamUserAccountId) || PrefManager.steamUserAccountId == 0
                    }
                }
                .filter { item ->
                    if (currentState.searchQuery.isNotEmpty()) {
                        matches(item.name, currentState.searchQuery)
                    } else {
                        true
                    }
                }
                .filter { item ->
                    val installedOnly = currentState.currentTab.installedOnly ||
                        currentState.appInfoSortType.contains(AppFilter.INSTALLED)
                    if (installedOnly) {
                        downloadDirectorySet.contains(item.installDir.ifBlank { item.name })
                    } else {
                        true
                    }
                }
                .toList()

            // Filter Steam apps first (no pagination yet)
            // Note: Don't sort individual lists - we'll sort the combined list for consistent ordering
            val filteredSteamApps: List<SteamLibraryApp> = steamFilteredBeforeCompatibility
                .asSequence()
                .filter { item -> passesCompatibleFilter(item.name) }
                .sortedWith(
                    compareByDescending<SteamLibraryApp> {
                        downloadDirectorySet.contains(it.installDir.ifBlank { it.name })
                    }.thenBy { it.name.lowercase() },
                )
                .toList()

            // Map Steam apps to UI items
            data class LibraryEntry(val item: LibraryItem, val isInstalled: Boolean)
            val shouldResolveSteamSize =
                currentState.currentSortOption == SortOption.SIZE_SMALLEST ||
                    currentState.currentSortOption == SortOption.SIZE_LARGEST
            val steamSizeCache = mutableMapOf<Int, Long>()

            val steamEntries: List<LibraryEntry> = filteredSteamApps.map { item ->
                val isInstalled = downloadDirectorySet.contains(item.installDir.ifBlank { item.name })
                val totalSizeBytes = if (shouldResolveSteamSize) {
                    steamSizeCache.getOrPut(item.id) {
                        steamAppDao.getAppDepots(item.id)
                            ?.values
                            ?.sumOf { depot ->
                                depot.manifests["public"]?.size ?: depot.manifests.values.firstOrNull()?.size ?: 0L
                            } ?: 0L
                    }
                } else {
                    0L
                }
                LibraryEntry(
                    item = LibraryItem(
                        index = 0, // temporary, will be re-indexed after combining and paginating
                        appId = "${GameSource.STEAM.name}_${item.id}",
                        name = item.name,
                        iconHash = item.clientIconHash,
                        capsuleImageUrl = item.getCapsuleUrl(),
                        headerImageUrl = item.headerUrl,
                        heroImageUrl = item.getHeroUrl(),
                        isShared = (PrefManager.steamUserAccountId != 0 && !item.ownerAccountId.contains(PrefManager.steamUserAccountId)),
                        sizeBytes = totalSizeBytes,
                    ),
                    isInstalled = isInstalled,
                )
            }

            // Scan Custom Games roots and create UI items (filtered by search query inside scanner)
            // Only include custom games if GAME filter is selected
            val customGameItems = if (currentState.appInfoSortType.contains(AppFilter.GAME)) {
                customGameScanner.scanAsLibraryItems(
                    query = currentState.searchQuery,
                )
            } else {
                emptyList()
            }
            val customEntries = customGameItems
                .filter { passesCompatibleFilter(it.name) }
                .map { LibraryEntry(it, true) }

            // Filter GOG games
            val filteredGOGGames = gogGameList
                .asSequence()
                .filter { game ->
                    if (currentState.searchQuery.isNotEmpty()) {
                        matches(game.title, currentState.searchQuery)
                    } else {
                        true
                    }
                }
                .filter { game ->
                    val installedOnly = currentState.currentTab.installedOnly ||
                        currentState.appInfoSortType.contains(AppFilter.INSTALLED)
                    if (installedOnly) {
                        game.isInstalled
                    } else {
                        true
                    }
                }
                .toList()

            val gogEntries = filteredGOGGames
                .filter { passesCompatibleFilter(it.title) }
                .map { game ->
                    LibraryEntry(
                        item = LibraryItem(
                            index = 0,
                            appId = "${GameSource.GOG.name}_${game.id}",
                            name = game.title,
                            iconHash = game.iconUrl.ifEmpty { game.imageUrl },
                            capsuleImageUrl = game.iconUrl.ifEmpty { game.imageUrl },
                            headerImageUrl = game.imageUrl.ifEmpty { game.iconUrl },
                            heroImageUrl = game.imageUrl.ifEmpty { game.iconUrl },
                            isShared = false,
                            gameSource = GameSource.GOG,
                        ),
                        isInstalled = game.isInstalled,
                    )
                }

            // Filter Epic games
            val filteredEpicGames = epicGameList
                .asSequence()
                .filter { game ->
                    if (currentState.searchQuery.isNotEmpty()) {
                        matches(game.title, currentState.searchQuery)
                    } else {
                        true
                    }
                }
                .filter { game ->
                    val installedOnly = currentState.currentTab.installedOnly ||
                        currentState.appInfoSortType.contains(AppFilter.INSTALLED)
                    if (installedOnly) {
                        game.isInstalled
                    } else {
                        true
                    }
                }
                .toList()

            val epicEntries = filteredEpicGames
                .filter { passesCompatibleFilter(it.title) }
                .map { game ->
                    LibraryEntry(
                        item = LibraryItem(
                            index = 0,
                            appId = "${GameSource.EPIC.name}_${game.id}",
                            name = game.title,
                            iconHash = game.artSquare.ifEmpty { game.artCover },
                            capsuleImageUrl = game.artCover.ifEmpty { game.artSquare },
                            headerImageUrl = game.artPortrait.ifEmpty { game.artSquare.ifEmpty { game.artCover } },
                            heroImageUrl = game.artPortrait.ifEmpty { game.artSquare.ifEmpty { game.artCover } },
                            isShared = false,
                            gameSource = GameSource.EPIC,
                        ),
                        isInstalled = game.isInstalled,
                    )
                }

            // Amazon games
            val filteredAmazonGames = amazonGameList
                .asSequence()
                .filter { game ->
                    if (currentState.searchQuery.isNotEmpty()) {
                        matches(game.title, currentState.searchQuery)
                    } else {
                        true
                    }
                }
                .filter { game ->
                    val installedOnly = currentState.currentTab.installedOnly ||
                        currentState.appInfoSortType.contains(AppFilter.INSTALLED)
                    if (installedOnly) {
                        game.isInstalled
                    } else {
                        true
                    }
                }
                .toList()

            val amazonEntries = filteredAmazonGames
                .filter { passesCompatibleFilter(it.title) }
                .map { game ->
                    LibraryEntry(
                        item = LibraryItem(
                            index = 0,
                            appId = "AMAZON_${game.appId}",
                            name = game.title,
                            iconHash = game.artUrl,
                            capsuleImageUrl = game.artUrl,
                            headerImageUrl = game.heroUrl.ifEmpty { game.artUrl },
                            heroImageUrl = game.heroUrl.ifEmpty { game.artUrl },
                            isShared = false,
                            gameSource = GameSource.AMAZON,
                        ),
                        isInstalled = game.isInstalled,
                    )
                }

            // Calculate installed counts
            val gogInstalledCount = filteredGOGGames.count { it.isInstalled }
            val epicInstalledCount = filteredEpicGames.count { it.isInstalled }
            val amazonInstalledCount = filteredAmazonGames.count { it.isInstalled }
            // Save game counts for skeleton loaders (only when not searching, to get accurate counts)
            // This needs to happen before filtering by source, so we save the total counts
            if (currentState.searchQuery.isEmpty()) {
                PrefManager.customGamesCount = customGameItems.size
                PrefManager.steamGamesCount = steamFilteredBeforeCompatibility.size
                PrefManager.gogGamesCount = filteredGOGGames.size
                PrefManager.gogInstalledGamesCount = gogInstalledCount
                PrefManager.epicGamesCount = filteredEpicGames.size
                PrefManager.epicInstalledGamesCount = epicInstalledCount
                PrefManager.amazonInstalledGamesCount = amazonInstalledCount
                Timber.tag("LibraryViewModel")
                    .d(
                        "%snull",
                        "Saved counts - Custom: ${customGameItems.size}, Steam: ${steamFilteredBeforeCompatibility.size}, " +
                            "GOG: ${filteredGOGGames.size}, GOG installed: $gogInstalledCount, Epic: ${filteredEpicGames.size}, ",
                    )
            }

            // Compute effective source filters based on current tab
            // ALL tab uses user preferences, other tabs override with their presets
            // Use captured currentState (not _state.value) to avoid TOCTOU race
            val currentTab = currentState.currentTab
            val includeSteam = if (currentTab == LibraryTab.ALL) {
                currentState.showSteamInLibrary
            } else {
                currentTab.showSteam
            }
            val includeOpen = if (currentTab == LibraryTab.ALL) {
                currentState.showCustomGamesInLibrary
            } else {
                currentTab.showCustom
            }

            val authState = snapshotPlatformAuthState()

            val includeGOG = (
                if (currentTab == LibraryTab.ALL) {
                    currentState.showGOGInLibrary
                } else {
                    currentTab.showGoG
                }
                ) &&
                authState.gogLoggedIn

            val includeEpic = (
                if (currentTab == LibraryTab.ALL) {
                    currentState.showEpicInLibrary
                } else {
                    currentTab.showEpic
                }
                ) &&
                authState.epicLoggedIn

            val includeAmazon = (
                if (currentTab == LibraryTab.ALL) {
                    currentState.showAmazonInLibrary
                } else {
                    currentTab.showAmazon
                }
                ) &&
                authState.amazonLoggedIn

            // Combine both lists and apply sort option
            val sortComparator: Comparator<LibraryEntry> = when (currentState.currentSortOption) {
                SortOption.INSTALLED_FIRST -> compareBy<LibraryEntry> { entry ->
                    if (entry.isInstalled) 0 else 1
                }.thenBy { it.item.name.lowercase() }

                SortOption.NAME_ASC -> compareBy { it.item.name.lowercase() }

                SortOption.NAME_DESC -> compareByDescending { it.item.name.lowercase() }

                SortOption.RECENTLY_PLAYED -> compareBy<LibraryEntry> { entry ->
                    if (entry.isInstalled) 0 else 1
                }.thenBy { it.item.name.lowercase() }

                SortOption.SIZE_SMALLEST -> compareBy<LibraryEntry> { it.item.sizeBytes }
                    .thenBy { it.item.name.lowercase() }

                SortOption.SIZE_LARGEST -> compareByDescending<LibraryEntry> { it.item.sizeBytes }
                    .thenBy { it.item.name.lowercase() }
            }

            val combined = buildList {
                if (includeSteam) addAll(steamEntries)
                if (includeOpen) addAll(customEntries)
                if (includeGOG) addAll(gogEntries)
                if (includeEpic) addAll(epicEntries)
                if (includeAmazon) addAll(amazonEntries)
            }.sortedWith(sortComparator).mapIndexed { idx, entry ->
                entry.item.copy(index = idx, isInstalled = entry.isInstalled)
            }

            // Total count for the current filter
            val totalFound = combined.size

            // Determine how many pages and slice the list for incremental loading
            val pageSize = PrefManager.itemsPerPage
            // Update internal pagination state
            paginationCurrentPage = paginationPage
            lastPageInCurrentFilter = if (totalFound == 0) 0 else (totalFound - 1) / pageSize
            // Calculate how many items to show: (pagesLoaded * pageSize)
            val endIndex = min((paginationPage + 1) * pageSize, totalFound)
            val pagedList = combined.take(endIndex)

            Timber.tag("LibraryViewModel").d("Filtered list size (with Custom Games): $totalFound")

            if (isFirstLoad) {
                isFirstLoad = false
            }

            // Fetch compatibility for current page games
            fetchCompatibilityForPage(pagedList.map { it.name })

            _state.update {
                it.copy(
                    appInfoList = pagedList,
                    currentPaginationPage = paginationPage + 1, // visual display is not 0 indexed
                    lastPaginationPage = lastPageInCurrentFilter + 1,
                    totalAppsInFilter = totalFound,
                    isLoading = false, // Loading complete
                    isSteamLoggedIn = authState.steamLoggedIn,
                    isGogLoggedIn = authState.gogLoggedIn,
                    isEpicLoggedIn = authState.epicLoggedIn,
                    isAmazonLoggedIn = authState.amazonLoggedIn,
                    // Per-source counts for tab badges
                    // Use user prefs + auth state only (not current tab) so badges stay stable across tab switches
                    allCount = (if (currentState.showSteamInLibrary) steamEntries.size else 0) +
                        (if (currentState.showCustomGamesInLibrary) customEntries.size else 0) +
                        (if (currentState.showGOGInLibrary && authState.gogLoggedIn) gogEntries.size else 0) +
                        (if (currentState.showEpicInLibrary && authState.epicLoggedIn) epicEntries.size else 0) +
                        (
                            if (currentState.showAmazonInLibrary &&
                                authState.amazonLoggedIn
                            ) {
                                amazonEntries.size
                            } else {
                                0
                            }
                            ),
                    steamCount = if (currentState.showSteamInLibrary) steamEntries.size else 0,
                    gogCount = if (currentState.showGOGInLibrary && authState.gogLoggedIn) gogEntries.size else 0,
                    epicCount = if (currentState.showEpicInLibrary && authState.epicLoggedIn) epicEntries.size else 0,
                    amazonCount = if (currentState.showAmazonInLibrary &&
                        authState.amazonLoggedIn
                    ) {
                        amazonEntries.size
                    } else {
                        0
                    },
                    localCount = if (currentState.showCustomGamesInLibrary) customEntries.size else 0,
                )
            }
        }
    }

    /**
     * Compares the game name against the search query using an exact match
     * and then again using a normalized form with diacritics removed.
     */
    private fun matches(gameName: String, searchQuery: String): Boolean {
        return gameName.contains(searchQuery, ignoreCase = true) || gameName.unaccent().contains(searchQuery, ignoreCase = true)
    }

    /**
     * Fetches compatibility information for games in paginated batches.
     * Checks cache first, then fetches uncached games in batches of 50.
     */
    private fun fetchCompatibilityForPage(gameNames: List<String>) {
        if (gameNames.isEmpty()) {
            Timber.tag("LibraryViewModel").d("fetchCompatibilityForPage: No game names provided")
            return
        }

        if (gpuName == "Unknown GPU") {
            Timber.tag("LibraryViewModel").w("Skipping compatibility fetch - GPU name is unknown")
            return
        }

        viewModelScope.launch {
            try {
                val results = gameCompatibilityService.getCompatibility(gameNames, gpuName)

                if (results.isNotEmpty()) {
                    updateCompatibilityState(results)
                    if (_state.value.appInfoSortType.contains(AppFilter.COMPATIBLE)) {
                        onFilterApps(paginationCurrentPage)
                    }
                }
            } catch (e: Exception) {
                Timber.tag("LibraryViewModel").e(e, "Error fetching compatibility data")
            }
        }
    }

    /**
     * Updates the state with compatibility results.
     */
    private fun updateCompatibilityState(
        results: Map<String, GameCompatibilityService.GameCompatibilityResponse>,
    ) {
        val compatibilityMap = results.mapValues { (_, response) ->
            compatibilityStatusFor(response)
        }

        // Update state with compatibility map (merge with existing)
        _state.update { currentState ->
            val mergedMap = currentState.compatibilityMap.toMutableMap()
            mergedMap.putAll(compatibilityMap)
            Timber.tag("LibraryViewModel").d("Updated state with ${compatibilityMap.size} compatibility entries, total: ${mergedMap.size}")
            currentState.copy(compatibilityMap = mergedMap)
        }
    }

    private fun compatibilityStatusFor(
        response: GameCompatibilityService.GameCompatibilityResponse,
    ): GameCompatibilityStatus {
        return when {
            response.isNotWorking -> GameCompatibilityStatus.NOT_COMPATIBLE
            !response.hasBeenTried -> GameCompatibilityStatus.UNKNOWN
            response.gpuPlayableCount > 0 -> GameCompatibilityStatus.GPU_COMPATIBLE
            response.totalPlayableCount > 0 -> GameCompatibilityStatus.COMPATIBLE
            else -> GameCompatibilityStatus.UNKNOWN
        }
    }
}
