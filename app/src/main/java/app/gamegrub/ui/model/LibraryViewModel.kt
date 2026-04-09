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
import app.gamegrub.db.dao.SteamAppDao
import app.gamegrub.device.DeviceQueryGateway
import app.gamegrub.domain.customgame.CustomGameScanner
import app.gamegrub.domain.library.BuildLibraryPresentationUseCase
import app.gamegrub.domain.library.compatibility.CompatibilityStatusMapper
import app.gamegrub.domain.library.policy.resolveSteamOwnerIds
import app.gamegrub.domain.library.policy.shouldBypassSteamFiltersForInstalledTab
import app.gamegrub.domain.library.policy.shouldIncludeForOwnerScope
import app.gamegrub.domain.library.policy.shouldIncludeForSharedFilter
import app.gamegrub.domain.library.policy.shouldIncludeForTypeFilter
import app.gamegrub.domain.library.search.LibraryQueryMatcher
import app.gamegrub.domain.usecase.CompleteLibraryOAuthUseCase
import app.gamegrub.domain.usecase.RefreshLibraryOrchestrationUseCase
import app.gamegrub.events.AndroidEvent
import app.gamegrub.gateway.AuthStateGateway
import app.gamegrub.gateway.LibraryGateway
import app.gamegrub.service.steam.SteamService
import app.gamegrub.ui.data.LibraryState
import app.gamegrub.ui.enums.AppFilter
import app.gamegrub.ui.enums.LibraryTab
import app.gamegrub.ui.enums.LibraryTab.Companion.next
import app.gamegrub.ui.enums.LibraryTab.Companion.previous
import app.gamegrub.ui.enums.SortOption
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


/**
 * ViewModel that orchestrates library UI state for Steam, custom, GOG, Epic, and Amazon entries.
 *
 * Responsibilities today:
 * - Observe per-source data streams and merge into one presentation list.
 * - Apply search, source toggles, sorting, pagination, ownership filters, and compatibility.
 * - Bridge UI intents (tab/filter/search/auth flows) into persisted preferences and UI state.
 *
 * Why here: this is currently the single composition point for cross-source library behavior.
 *
 * Ownership: This class now carries both presentation orchestration and domain filtering.
 * Refactor target: keep UI intent/state mutations here, but extract filtering/aggregation into
 * a unified `BuildLibraryPresentationUseCase` with policy helpers under
 * `app.gamegrub.domain.library`.
 */
@HiltViewModel
class LibraryViewModel @Inject constructor(
    /**
     * DAO for Steam app metadata and depot size lookups used during sorting.
     * Ownership: direct DAO dependency in ViewModel is convenient but leaks data concerns.
     * Refactor target: surface size/metadata through unified `LibraryGateway` capabilities.
     */
    private val steamAppDao: SteamAppDao,
    /**
     * Unified gateway that publishes source snapshots for library rendering.
     * Ownership: source aggregation belongs behind this boundary.
     */
    private val libraryGateway: LibraryGateway,
    /**
     * Unified read-only auth-state boundary used for platform availability decisions.
     */
    private val authStateGateway: AuthStateGateway,
    /**
     * Service that resolves compatibility status for displayed game names.
     * Ownership: dependency is appropriate for orchestration; batching policy can be extracted.
     * Refactor target: `FetchLibraryCompatibilityUseCase` for retry/caching policy.
     */
    private val gameCompatibilityService: GameCompatibilityService,
    /**
     * Gateway used to resolve device GPU renderer string for compatibility checks.
     * Ownership: platform/device querying should stay out of pure domain policies.
     * Refactor target: resolve compatibility context in data/platform layer and inject result into
     * `FetchLibraryCompatibilityUseCase`.
     */
    private val deviceQueryGateway: DeviceQueryGateway,
    /**
     * Scanner that converts custom game folders into library entries.
     * Ownership: domain scanner is appropriate; scan trigger policy can move to use case.
     * Refactor target: `ScanCustomGamesUseCase`.
     */
    private val customGameScanner: CustomGameScanner,
    /**
     * Builds final library presentation output (source inclusion, sorting, pagination, badges).
     */
    private val buildLibraryPresentationUseCase: BuildLibraryPresentationUseCase,
    /**
     * Orchestrates refresh behavior across library sources.
     */
    private val refreshLibraryOrchestrationUseCase: RefreshLibraryOrchestrationUseCase,
    /**
     * Handles OAuth callback completion and post-login sync for supported stores.
     */
    private val completeLibraryOAuthUseCase: CompleteLibraryOAuthUseCase,
    /**
     * Application context used by OAuth handlers and service triggers.
     * Ownership: VM currently invokes services directly; moving auth/sync orchestration into a
     * coordinator would reduce context-heavy dependencies in this class.
     */
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    /**
     * Immutable snapshot of login state per platform at one point in time.
     *
     * Why: captures auth booleans once so list composition and badge counts use consistent values.
     * Ownership: simple presentation helper; can stay local to this ViewModel.
     */
    private data class PlatformAuthState(
        /** True when Steam has an active signed-in session in app memory. */
        val steamLoggedIn: Boolean,

        /** True when persisted GOG OAuth credentials are present. */
        val gogLoggedIn: Boolean,

        /** True when persisted Epic OAuth credentials are present. */
        val epicLoggedIn: Boolean,

        /** True when persisted Amazon OAuth credentials are present. */
        val amazonLoggedIn: Boolean,
    )

    /**
     * Backing mutable state for the library screen.
     * Ownership: ViewModel state container, belongs here.
     */
    private val _state = MutableStateFlow(LibraryState(isLoading = true))

    /**
     * Public immutable stream consumed by Compose UI.
     * Ownership: ViewModel API surface, belongs here.
     */
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    /**
     * Preserves grid scroll position across recompositions and configuration changes.
     * Ownership: UI state persistence belongs in the ViewModel.
     */
    var listState: LazyGridState by mutableStateOf(LazyGridState(0, 0))

    /**
     * Event handler that re-applies filters when install status changes.
     * Ownership: event-to-state orchestration belongs here.
     */
    private val onInstallStatusChanged: (AndroidEvent.LibraryInstallStatusChanged) -> Unit = {
        onFilterApps(paginationCurrentPage)
    }

    /**
     * Event handler that increments image refresh state and rebuilds the library list.
     * Ownership: UI refresh orchestration belongs here.
     */
    private val onCustomGameImagesFetched: (AndroidEvent.CustomGameImagesFetched) -> Unit = {
        // Increment refresh counter and refresh the library list to pick up newly fetched images
        _state.update { it.copy(imageRefreshCounter = it.imageRefreshCounter + 1) }
        onFilterApps(paginationCurrentPage)
    }

    /**
     * Zero-based page currently requested for incremental loading.
     * Ownership: pagination presentation state belongs here.
     */
    @Volatile
    private var paginationCurrentPage: Int = 0

    /**
     * Zero-based last page index available for the current filter result.
     * Ownership: pagination presentation state belongs here.
     */
    @Volatile
    private var lastPageInCurrentFilter: Int = 0

    /**
     * Latest Steam catalog snapshot from Room before UI filtering.
     * Ownership: caching raw source lists in VM is workable but heavy.
     * Refactor target: subscribe to unified aggregated streams exposed by `LibraryGateway`.
     */
    private var appList: List<SteamLibraryApp> = emptyList()

    /**
     * Latest GOG catalog snapshot from Room before UI filtering.
     * Ownership: see `appList` ownership note.
     */
    private var gogGameList: List<GOGGame> = emptyList()

    /**
     * Latest Epic catalog snapshot from Room before UI filtering.
     * Ownership: see `appList` ownership note.
     */
    private var epicGameList: List<EpicGame> = emptyList()

    /**
     * Latest Amazon catalog snapshot from Room before UI filtering.
     * Ownership: see `appList` ownership note.
     */
    private var amazonGameList: List<AmazonGame> = emptyList()

    /**
     * Cached set of locally downloaded Steam app ids for installed filtering.
     * Ownership: can remain here until install-state aggregation is extracted.
     */
    @Volatile
    private var downloadedSteamAppIds: Set<Int> = emptySet()

    /**
     * Tracks first-load behavior for initial UX timing decisions.
     * Ownership: presentation concern, belongs here.
     */
    private var isFirstLoad = true

    /**
     * Active debounce job for search query updates.
     * Ownership: view intent throttling belongs in ViewModel.
     */
    private var searchDebounceJob: Job? = null

    /**
     * Debounce window for search updates.
     * Ownership: UI tuning parameter, belongs here.
     */
    private val searchDebounceMs = 500L // 500ms debounce

    /**
     * Cached GPU renderer used for compatibility lookups.
     *
     * Why: this avoids repeated gateway calls while paging/filtering repeatedly recomputes lists.
     * Ownership: acceptable temporary cache in VM.
     * Refactor target: move to a device-profile provider consumed by compatibility use cases.
     */
    private val gpuName: String by lazy {
        try {
            val gpu = deviceQueryGateway.getGpuRenderer()
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

    /**
     * Subscribes to Room streams and global app events, then triggers initial auth-state sync.
     *
     * Why: this screen is source-aggregated and must react to incremental local/remote updates.
     * Ownership: lifecycle-bound data observation belongs in ViewModel.
     * Refactor target: split source collectors into a repository/domain stream aggregator.
     */
    init {
        refreshPlatformAuthState()

        viewModelScope.launch(Dispatchers.IO) {
            libraryGateway.observeSourceSnapshot().collect { snapshot ->
                var hasChanges = false

                if (appList != snapshot.steamApps) {
                    Timber.tag("LibraryViewModel").d("Collecting ${snapshot.steamApps.size} apps")
                    appList = snapshot.steamApps
                    hasChanges = true
                }

                if (downloadedSteamAppIds != snapshot.downloadedSteamAppIds) {
                    downloadedSteamAppIds = snapshot.downloadedSteamAppIds
                    hasChanges = true
                }

                if (gogGameList != snapshot.gogGames) {
                    Timber.tag("LibraryViewModel").d("Collecting ${snapshot.gogGames.size} GOG games")
                    gogGameList = snapshot.gogGames
                    hasChanges = true
                }

                if (epicGameList != snapshot.epicGames) {
                    Timber.tag("LibraryViewModel").d("Collecting ${snapshot.epicGames.size} Epic games")
                    epicGameList = snapshot.epicGames
                    hasChanges = true
                }

                if (amazonGameList != snapshot.amazonGames) {
                    Timber.tag("LibraryViewModel").d("Collecting ${snapshot.amazonGames.size} Amazon games")
                    amazonGameList = snapshot.amazonGames
                    hasChanges = true
                }

                if (hasChanges) {
                    onFilterApps(paginationCurrentPage)
                }
            }
        }

        GameGrubApp.events.on<AndroidEvent.LibraryInstallStatusChanged, Unit>(onInstallStatusChanged)
        GameGrubApp.events.on<AndroidEvent.CustomGameImagesFetched, Unit>(onCustomGameImagesFetched)
    }

    /**
     * Clears debounced work and unregisters global event listeners.
     *
     * Why: this ViewModel subscribes to app-wide events and must detach to prevent leaks.
     * Ownership: lifecycle cleanup belongs here.
     */
    override fun onCleared() {
        searchDebounceJob?.cancel()
        GameGrubApp.events.off<AndroidEvent.LibraryInstallStatusChanged, Unit>(onInstallStatusChanged)
        GameGrubApp.events.off<AndroidEvent.CustomGameImagesFetched, Unit>(onCustomGameImagesFetched)
        super.onCleared()
    }

    /**
     * Sets bottom-sheet visibility state for library options UI.
     * Ownership: pure UI state mutation belongs here.
     */
    fun onModalBottomSheet(value: Boolean) {
        _state.update { it.copy(modalBottomSheet = value) }
    }

    /**
     * Enables or disables search mode and clears query when search exits.
     *
     * Why: clearing query on exit restores the full catalog and avoids stale filtered state.
     * Ownership: UI intent handling belongs here.
     */
    fun onIsSearching(value: Boolean) {
        _state.update { it.copy(isSearching = value) }
        if (!value) {
            onSearchQuery("")
        }
    }

    /**
     * Toggles visibility of a specific game source and persists that preference.
     *
     * Why: source visibility survives process restarts through `PrefManager`.
     * Ownership: intent handling belongs here, but persistence details should move out.
     * Refactor target: `UpdateLibrarySourceVisibilityUseCase` backed by `PreferencesGateway`.
     */
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

    /**
     * Updates sort order preference and triggers list recomputation.
     * Ownership: intent handling belongs here; persistence can be extracted.
     */
    fun onSortOptionChanged(sortOption: SortOption) {
        PrefManager.librarySortOption = sortOption
        _state.update { it.copy(currentSortOption = sortOption) }
        onFilterApps()
    }

    /**
     * Updates the options panel open/closed state.
     * Ownership: presentation state update belongs here.
     */
    fun onOptionsPanelToggle(isOpen: Boolean) {
        _state.update { it.copy(isOptionsPanelOpen = isOpen) }
    }

    /**
     * Switches active tab and resets paging to the first page.
     *
     * Why: tab presets change source/install constraints so paging must restart.
     * Ownership: tab intent handling belongs here.
     */
    fun onTabChanged(tab: LibraryTab) {
        _state.update { it.copy(currentTab = tab) }
        onFilterApps(0) // Reset to first page and refresh
    }

    /**
     * Advances to the next tab (controller bumper action).
     * Ownership: view interaction handling belongs here.
     */
    fun onNextTab() {
        _state.update { currentState ->
            val nextTab = currentState.currentTab.next()
            Timber.tag("LibraryViewModel").d("Tab next via bumper: ${currentState.currentTab} -> $nextTab")
            currentState.copy(currentTab = nextTab)
        }
        onFilterApps(0)
    }

    /**
     * Moves to the previous tab (controller bumper action).
     * Ownership: view interaction handling belongs here.
     */
    fun onPreviousTab() {
        _state.update { currentState ->
            val previousTab = currentState.currentTab.previous()
            Timber.tag("LibraryViewModel").d("Tab previous via bumper: ${currentState.currentTab} -> $previousTab")
            currentState.copy(currentTab = previousTab)
        }
        onFilterApps(0)
    }

    /**
     * Updates query text immediately and debounces expensive list recomputation.
     *
     * Why: immediate echo keeps typing responsive while debounce prevents filter thrash.
     * Ownership: UI throttling policy belongs here.
     */
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

    /**
     * Toggles an app filter flag and persists the updated filter set.
     *
     * Ownership: UI intent handling belongs here.
     * Refactor target: `UpdateLibraryFilterUseCase` backed by `PreferencesGateway`.
     */
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

    /**
     * Changes page index by a relative increment and clamps to valid bounds.
     * Ownership: pagination presentation logic belongs here.
     */
    fun onPageChange(pageIncrement: Int) {
        // Amount to change by
        var toPage = max(0, paginationCurrentPage + pageIncrement)
        toPage = min(toPage, lastPageInCurrentFilter)
        onFilterApps(toPage)
    }

    /**
     * Performs manual refresh of remote ownership/auth-backed libraries and rebuilds UI list.
     *
     * Why: pull-to-refresh should force server sync, clear compatibility cache, then re-render.
     * Ownership: orchestration belongs here, but platform sync branching is a domain concern.
     * Refactor target: `RefreshLibraryUseCase` orchestrating `LibraryGateway` +
     * `AuthStateGateway`.
     */
    fun onRefresh() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }

            refreshPlatformAuthState()

            // Clear compatibility cache on manual refresh to get fresh data
            gameCompatibilityService.clearCache()

            try {
                refreshLibraryOrchestrationUseCase()
            } catch (e: Exception) {
                Timber.tag("LibraryViewModel").e(e, "Failed to refresh library sources")
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

    /**
     * Adds a user-selected folder to manual custom-game roots if it resolves to a valid entry.
     *
     * Why: users can augment auto-detected custom games with explicit folders.
     * Ownership: command handling belongs here; folder persistence belongs in a gateway.
     * Refactor target: `RegisterCustomGameFolderUseCase`.
     */
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

    /**
     * Handles GOG OAuth callback payload, updates auth state, and starts post-login sync.
     *
     * Ownership: currently UI-driven auth orchestration.
     * Refactor target: unified OAuth callback coordinator/use case keyed by `GameSource`, sharing
     * `AuthStateGateway` and refresh orchestration.
     */
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
        handleOAuthResult(
            source = GameSource.GOG,
            authCode = code,
            successMessage = context.getString(R.string.gog_login_success_title),
            cancelMessage = context.getString(R.string.gog_login_cancel),
        )
    }

    /**
     * Handles Epic OAuth callback payload, updates auth state, and starts post-login sync.
     * Ownership: same concerns as `onGogOAuthResult`.
     * Refactor target: unified OAuth callback coordinator/use case keyed by `GameSource`.
     */
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
        handleOAuthResult(
            source = GameSource.EPIC,
            authCode = code,
            successMessage = context.getString(R.string.epic_login_success_title),
            cancelMessage = context.getString(R.string.epic_login_cancel),
        )
    }

    /**
     * Handles Amazon OAuth callback payload, updates auth state, and starts post-login sync.
     * Ownership: same concerns as other OAuth handlers.
     * Refactor target: unified OAuth callback coordinator/use case keyed by `GameSource`.
     */
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
        handleOAuthResult(
            source = GameSource.AMAZON,
            authCode = code,
            successMessage = context.getString(R.string.amazon_login_success_title),
            cancelMessage = context.getString(R.string.amazon_login_cancel),
        )
    }

    /**
     * Refreshes login-state flags exposed to the UI.
     *
     * Why: source visibility and badges depend on current auth state.
     * Ownership: state synchronization belongs here.
     */
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

    /**
     * Clears one-time auth message if the UI confirms handling of the current event id.
     *
     * Why: avoids message replay across recompositions.
     * Ownership: one-shot UI event management belongs here.
     */
    fun onAuthMessageShown(eventId: Long) {
        _state.update {
            if (it.authMessageEventId == eventId) {
                it.copy(authMessage = null)
            } else {
                it
            }
        }
    }

    /**
     * Emits an auth toast/snackbar message with monotonically increasing event id.
     * Ownership: one-shot UI event emission belongs here.
     */
    private fun postAuthMessage(message: String) {
        _state.update {
            it.copy(
                authMessage = message,
                authMessageEventId = it.authMessageEventId + 1,
            )
        }
    }

    private fun handleOAuthResult(
        source: GameSource,
        authCode: String,
        successMessage: String,
        cancelMessage: String,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = completeLibraryOAuthUseCase(source, authCode)
            if (result.isSuccess) {
                postAuthMessage(successMessage)
                refreshPlatformAuthState()
                onFilterApps(paginationCurrentPage)
            } else {
                Timber.tag("LibraryViewModel").e(result.exceptionOrNull(), "OAuth completion failed for $source")
                postAuthMessage(result.exceptionOrNull()?.message ?: cancelMessage)
            }
        }
    }

    /**
     * Reads platform auth booleans into a stable snapshot.
     *
     * Why: filter composition should use one coherent auth view for a single recomputation pass.
     * Ownership: helper can stay private to ViewModel.
     */
    private fun snapshotPlatformAuthState(): PlatformAuthState {
        return PlatformAuthState(
            steamLoggedIn = authStateGateway.isLoggedIn(GameSource.STEAM),
            gogLoggedIn = authStateGateway.hasStoredCredentials(GameSource.GOG),
            epicLoggedIn = authStateGateway.hasStoredCredentials(GameSource.EPIC),
            amazonLoggedIn = authStateGateway.hasStoredCredentials(GameSource.AMAZON),
        )
    }

    /**
     * Rebuilds the visible library by applying filters, merging sources, sorting, and pagination.
     *
     * What this method does:
     * - Applies Steam owner/type/shared constraints with installed-tab bypass.
     * - Applies search/install/compatibility filters across all platforms.
     * - Maps raw models into `LibraryItem`, computes badge counts, and updates pagination state.
     * - Triggers compatibility fetch for currently visible items.
     *
     * Why: the screen needs one deterministic pipeline from raw source lists to rendered items.
     *
     * Ownership: this is the largest refactor candidate in the file.
     * Refactor target: split into a `BuildLibraryPresentationUseCase` plus dedicated policies:
     * `SteamFilterDomain`, `LibrarySourceInclusionPolicy`, `LibraryBadgeCountDomain`,
     * and `LibraryPaginationDomain`.
     */
    private fun onFilterApps(paginationPage: Int = 0): Job {
        Timber.tag("LibraryViewModel").d("onFilterApps - appList.size: ${appList.size}, isFirstLoad: $isFirstLoad")
        return viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isLoading = true) }

            val currentState = _state.value
            val currentFilter = AppFilter.getAppType(currentState.appInfoSortType)

            val downloadedSteamAppIdsSnapshot = downloadedSteamAppIds
            val installedTabActive = currentState.currentTab.installedOnly
            val installedFilterEnabled = currentState.appInfoSortType.contains(AppFilter.INSTALLED)

            // Candidate extraction: move to Steam install-state policy when filter pipeline is split.
            fun isSteamInstalled(item: SteamLibraryApp): Boolean {
                return item.id in downloadedSteamAppIdsSnapshot
            }

            val installedSteamAppIds = appList
                .asSequence()
                .filter { item -> isSteamInstalled(item) }
                .map { it.id }
                .toSet()

            // Candidate extraction: merge into compatibility filter policy/use case.
            fun passesCompatibleFilter(gameName: String): Boolean {
                if (!currentState.appInfoSortType.contains(AppFilter.COMPATIBLE)) {
                    return true
                }
                val status = currentState.compatibilityMap[gameName] ?: return true
                return status == GameCompatibilityStatus.COMPATIBLE || status == GameCompatibilityStatus.GPU_COMPATIBLE
            }

            val resolvedOwnerIds = resolveSteamOwnerIds(
                familyMembers = SteamService.familyMembers,
                steamUserAccountId = PrefManager.steamUserAccountId,
                steam3AccountId = SteamService.getSteam3AccountId(),
            )
            val isSharedFilterEnabled = currentState.appInfoSortType.contains(AppFilter.SHARED)

            val steamOwnerScopedApps = appList
                .asSequence()
                .filter { item ->
                    if (shouldBypassSteamFiltersForInstalledTab(item.id, installedTabActive, installedSteamAppIds)) {
                        return@filter true
                    }
                    shouldIncludeForOwnerScope(
                        itemOwnerAccountIds = item.ownerAccountId,
                        resolvedOwnerIds = resolvedOwnerIds,
                    )
                }
                .toList()

            val steamTypeFilteredApps = steamOwnerScopedApps
                .asSequence()
                .filter { item ->
                    if (shouldBypassSteamFiltersForInstalledTab(item.id, installedTabActive, installedSteamAppIds)) {
                        return@filter true
                    }
                    shouldIncludeForTypeFilter(
                        itemType = item.type,
                        allowedTypes = currentFilter,
                    )
                }
                .toList()

            val steamSharedFilteredApps = steamTypeFilteredApps
                .asSequence()
                .filter { item ->
                    if (shouldBypassSteamFiltersForInstalledTab(item.id, installedTabActive, installedSteamAppIds)) {
                        return@filter true
                    }
                    shouldIncludeForSharedFilter(
                        itemOwnerAccountIds = item.ownerAccountId,
                        currentUserAccountId = PrefManager.steamUserAccountId,
                        sharedFilterEnabled = isSharedFilterEnabled,
                    )
                }
                .toList()

            val steamFilteredBeforeCompatibility: List<SteamLibraryApp> = steamSharedFilteredApps
                .asSequence()
                .filter { item ->
                    if (currentState.searchQuery.isNotEmpty()) {
                        LibraryQueryMatcher.matches(item.name, currentState.searchQuery)
                    } else {
                        true
                    }
                }
                .filter { item ->
                    val installedOnly = currentState.currentTab.installedOnly ||
                        installedFilterEnabled
                    if (installedOnly) {
                        isSteamInstalled(item)
                    } else {
                        true
                    }
                }
                .toList()

            Timber.tag("LibraryViewModel").d(
                "Steam filter stages: raw=%d owner=%d type=%d shared=%d final=%d owners=%s self=%d sharedFilter=%s allowedTypes=%s appTypes=%s",
                appList.size,
                steamOwnerScopedApps.size,
                steamTypeFilteredApps.size,
                steamSharedFilteredApps.size,
                steamFilteredBeforeCompatibility.size,
                resolvedOwnerIds.sorted(),
                PrefManager.steamUserAccountId,
                isSharedFilterEnabled,
                currentFilter,
                appList.asSequence().map { it.type }.distinct().toList(),
            )

            // Filter Steam apps first (no pagination yet)
            // Note: Don't sort individual lists - we'll sort the combined list for consistent ordering
            val filteredSteamApps: List<SteamLibraryApp> = steamFilteredBeforeCompatibility
                .asSequence()
                .filter { item -> passesCompatibleFilter(item.name) }
                .sortedWith(
                    compareByDescending<SteamLibraryApp> {
                        isSteamInstalled(it)
                    }.thenBy { it.name.lowercase() },
                )
                .toList()

            // Map Steam apps to UI items

            /**
             * Temporary pairing of rendered item and install flag until final mapping/reindex pass.
             * Ownership: local transformation detail, belongs inside this pipeline.
             */
            data class LibraryEntry(val item: LibraryItem, val isInstalled: Boolean)

            val shouldResolveSteamSize =
                currentState.currentSortOption == SortOption.SIZE_SMALLEST ||
                    currentState.currentSortOption == SortOption.SIZE_LARGEST
            val steamSizeCache = mutableMapOf<Int, Long>()

            val steamEntries: List<LibraryEntry> = filteredSteamApps.map { item ->
                val isInstalled = isSteamInstalled(item)
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
                        LibraryQueryMatcher.matches(game.title, currentState.searchQuery)
                    } else {
                        true
                    }
                }
                .filter { game ->
                    val installedOnly = currentState.currentTab.installedOnly ||
                        installedFilterEnabled
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
                        LibraryQueryMatcher.matches(game.title, currentState.searchQuery)
                    } else {
                        true
                    }
                }
                .filter { game ->
                    val installedOnly = currentState.currentTab.installedOnly ||
                        installedFilterEnabled
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
                        LibraryQueryMatcher.matches(game.title, currentState.searchQuery)
                    } else {
                        true
                    }
                }
                .filter { game ->
                    val installedOnly = currentState.currentTab.installedOnly ||
                        installedFilterEnabled
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

            // Count baselines should not depend on the active tab preset.
            val steamCountForBadges = steamSharedFilteredApps
                .asSequence()
                .filter { item ->
                    if (currentState.searchQuery.isNotEmpty()) LibraryQueryMatcher.matches(item.name, currentState.searchQuery) else true
                }
                .filter { item ->
                    if (installedFilterEnabled) isSteamInstalled(item) else true
                }
                .count { item -> passesCompatibleFilter(item.name) }

            val gogCountForBadges = gogGameList
                .asSequence()
                .filter { game ->
                    if (currentState.searchQuery.isNotEmpty()) LibraryQueryMatcher.matches(game.title, currentState.searchQuery) else true
                }
                .filter { game ->
                    if (installedFilterEnabled) game.isInstalled else true
                }
                .count { game -> passesCompatibleFilter(game.title) }

            val epicCountForBadges = epicGameList
                .asSequence()
                .filter { game ->
                    if (currentState.searchQuery.isNotEmpty()) LibraryQueryMatcher.matches(game.title, currentState.searchQuery) else true
                }
                .filter { game ->
                    if (installedFilterEnabled) game.isInstalled else true
                }
                .count { game -> passesCompatibleFilter(game.title) }

            val amazonCountForBadges = amazonGameList
                .asSequence()
                .filter { game ->
                    if (currentState.searchQuery.isNotEmpty()) LibraryQueryMatcher.matches(game.title, currentState.searchQuery) else true
                }
                .filter { game ->
                    if (installedFilterEnabled) game.isInstalled else true
                }
                .count { game -> passesCompatibleFilter(game.title) }

            // Calculate installed counts
            val steamInstalledCount = steamEntries.count { it.isInstalled }
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

            val authState = snapshotPlatformAuthState()

            val presentation = buildLibraryPresentationUseCase(
                BuildLibraryPresentationUseCase.Input(
                    currentState = currentState,
                    authSteamLoggedIn = authState.steamLoggedIn,
                    authGogLoggedIn = authState.gogLoggedIn,
                    authEpicLoggedIn = authState.epicLoggedIn,
                    authAmazonLoggedIn = authState.amazonLoggedIn,
                    steamEntries = steamEntries.map { BuildLibraryPresentationUseCase.Entry(it.item, it.isInstalled) },
                    customEntries = customEntries.map { BuildLibraryPresentationUseCase.Entry(it.item, it.isInstalled) },
                    gogEntries = gogEntries.map { BuildLibraryPresentationUseCase.Entry(it.item, it.isInstalled) },
                    epicEntries = epicEntries.map { BuildLibraryPresentationUseCase.Entry(it.item, it.isInstalled) },
                    amazonEntries = amazonEntries.map { BuildLibraryPresentationUseCase.Entry(it.item, it.isInstalled) },
                    steamCountForBadges = steamCountForBadges,
                    gogCountForBadges = gogCountForBadges,
                    epicCountForBadges = epicCountForBadges,
                    amazonCountForBadges = amazonCountForBadges,
                    steamInstalledCount = steamInstalledCount,
                    gogInstalledCount = gogInstalledCount,
                    epicInstalledCount = epicInstalledCount,
                    amazonInstalledCount = amazonInstalledCount,
                    paginationPage = paginationPage,
                    pageSize = PrefManager.itemsPerPage,
                ),
            )

            paginationCurrentPage = paginationPage
            lastPageInCurrentFilter = presentation.lastPageInFilter

            Timber.tag("LibraryViewModel").d("Filtered list size (with Custom Games): ${presentation.totalFound}")

            if (isFirstLoad) {
                isFirstLoad = false
            }

            // Fetch compatibility for current page games
            fetchCompatibilityForPage(presentation.pagedList.map { it.name })

            _state.update {
                it.copy(
                    appInfoList = presentation.pagedList,
                    currentPaginationPage = presentation.currentPaginationPage,
                    lastPaginationPage = presentation.lastPageInFilter + 1,
                    totalAppsInFilter = presentation.totalFound,
                    isLoading = false, // Loading complete
                    isSteamLoggedIn = authState.steamLoggedIn,
                    isGogLoggedIn = authState.gogLoggedIn,
                    isEpicLoggedIn = authState.epicLoggedIn,
                    isAmazonLoggedIn = authState.amazonLoggedIn,
                    allCount = presentation.allCount,
                    installedCount = presentation.installedCount,
                    steamCount = presentation.steamCount,
                    gogCount = presentation.gogCount,
                    epicCount = presentation.epicCount,
                    amazonCount = presentation.amazonCount,
                    localCount = presentation.localCount,
                )
            }
        }
    }

    /**
     * Fetches compatibility information for games in paginated batches.
     * Checks cache first, then fetches uncached games in batches of 50.
     *
     * Why: compatibility data is supplemental and should load asynchronously for visible items.
     * Ownership: async orchestration is valid in VM, but batching/retry belongs in domain layer.
     * Refactor target: `FetchLibraryCompatibilityUseCase`.
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
     *
     * Why: state merge avoids discarding previously fetched compatibility entries.
     * Ownership: state mutation belongs here.
     */
    private fun updateCompatibilityState(
        results: Map<String, GameCompatibilityService.GameCompatibilityResponse>,
    ) {
        val compatibilityMap = results.mapValues { (_, response) ->
            CompatibilityStatusMapper.toStatus(response)
        }

        // Update state with compatibility map (merge with existing)
        _state.update { currentState ->
            val mergedMap = currentState.compatibilityMap.toMutableMap()
            mergedMap.putAll(compatibilityMap)
            Timber.tag("LibraryViewModel").d("Updated state with ${compatibilityMap.size} compatibility entries, total: ${mergedMap.size}")
            currentState.copy(compatibilityMap = mergedMap)
        }
    }
}
