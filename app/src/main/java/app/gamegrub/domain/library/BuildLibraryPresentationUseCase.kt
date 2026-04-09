package app.gamegrub.domain.library

import app.gamegrub.data.LibraryItem
import app.gamegrub.ui.data.LibraryState
import app.gamegrub.ui.enums.LibraryTab
import app.gamegrub.ui.enums.SortOption
import javax.inject.Inject

class BuildLibraryPresentationUseCase @Inject constructor() {
    data class Entry(
        val item: LibraryItem,
        val isInstalled: Boolean,
    )

    data class Input(
        val currentState: LibraryState,
        val authSteamLoggedIn: Boolean,
        val authGogLoggedIn: Boolean,
        val authEpicLoggedIn: Boolean,
        val authAmazonLoggedIn: Boolean,
        val steamEntries: List<Entry>,
        val customEntries: List<Entry>,
        val gogEntries: List<Entry>,
        val epicEntries: List<Entry>,
        val amazonEntries: List<Entry>,
        val steamCountForBadges: Int,
        val gogCountForBadges: Int,
        val epicCountForBadges: Int,
        val amazonCountForBadges: Int,
        val steamInstalledCount: Int,
        val gogInstalledCount: Int,
        val epicInstalledCount: Int,
        val amazonInstalledCount: Int,
        val paginationPage: Int,
        val pageSize: Int,
    )

    data class Output(
        val pagedList: List<LibraryItem>,
        val totalFound: Int,
        val lastPageInFilter: Int,
        val currentPaginationPage: Int,
        val allCount: Int,
        val installedCount: Int,
        val steamCount: Int,
        val gogCount: Int,
        val epicCount: Int,
        val amazonCount: Int,
        val localCount: Int,
    )

    operator fun invoke(input: Input): Output {
        val currentState = input.currentState
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
        val includeGog = (
            if (currentTab == LibraryTab.ALL) currentState.showGOGInLibrary else currentTab.showGoG
            ) && (input.authGogLoggedIn || currentTab.installedOnly)
        val includeEpic = (
            if (currentTab == LibraryTab.ALL) currentState.showEpicInLibrary else currentTab.showEpic
            ) && (input.authEpicLoggedIn || currentTab.installedOnly)
        val includeAmazon = (
            if (currentTab == LibraryTab.ALL) currentState.showAmazonInLibrary else currentTab.showAmazon
            ) && (input.authAmazonLoggedIn || currentTab.installedOnly)

        val sortComparator: Comparator<Entry> = when (currentState.currentSortOption) {
            SortOption.INSTALLED_FIRST -> compareBy<Entry> { if (it.isInstalled) 0 else 1 }
                .thenBy { it.item.name.lowercase() }

            SortOption.NAME_ASC -> compareBy { it.item.name.lowercase() }

            SortOption.NAME_DESC -> compareByDescending { it.item.name.lowercase() }

            SortOption.RECENTLY_PLAYED -> compareBy<Entry> { if (it.isInstalled) 0 else 1 }
                .thenBy { it.item.name.lowercase() }

            SortOption.SIZE_SMALLEST -> compareBy<Entry> { it.item.sizeBytes }
                .thenBy { it.item.name.lowercase() }

            SortOption.SIZE_LARGEST -> compareByDescending<Entry> { it.item.sizeBytes }
                .thenBy { it.item.name.lowercase() }
        }

        val combined = buildList {
            if (includeSteam) addAll(input.steamEntries)
            if (includeOpen) addAll(input.customEntries)
            if (includeGog) addAll(input.gogEntries)
            if (includeEpic) addAll(input.epicEntries)
            if (includeAmazon) addAll(input.amazonEntries)
        }.sortedWith(sortComparator).mapIndexed { idx, entry ->
            entry.item.copy(index = idx, isInstalled = entry.isInstalled)
        }

        val totalFound = combined.size
        val lastPageInFilter = if (totalFound == 0) 0 else (totalFound - 1) / input.pageSize
        val endIndex = minOf((input.paginationPage + 1) * input.pageSize, totalFound)
        val pagedList = combined.take(endIndex)

        val allCount = (if (currentState.showSteamInLibrary) input.steamCountForBadges else 0) +
            (if (currentState.showCustomGamesInLibrary) input.customEntries.size else 0) +
            (if (currentState.showGOGInLibrary && input.authGogLoggedIn) input.gogCountForBadges else 0) +
            (if (currentState.showEpicInLibrary && input.authEpicLoggedIn) input.epicCountForBadges else 0) +
            (if (currentState.showAmazonInLibrary && input.authAmazonLoggedIn) input.amazonCountForBadges else 0)

        return Output(
            pagedList = pagedList,
            totalFound = totalFound,
            lastPageInFilter = lastPageInFilter,
            currentPaginationPage = input.paginationPage + 1,
            allCount = allCount,
            installedCount = input.steamInstalledCount + input.gogInstalledCount +
                input.epicInstalledCount + input.amazonInstalledCount + input.customEntries.size,
            steamCount = if (currentState.showSteamInLibrary) input.steamCountForBadges else 0,
            gogCount = if (currentState.showGOGInLibrary && input.authGogLoggedIn) input.gogCountForBadges else 0,
            epicCount = if (currentState.showEpicInLibrary && input.authEpicLoggedIn) input.epicCountForBadges else 0,
            amazonCount = if (currentState.showAmazonInLibrary && input.authAmazonLoggedIn) input.amazonCountForBadges else 0,
            localCount = if (currentState.showCustomGamesInLibrary) input.customEntries.size else 0,
        )
    }
}

