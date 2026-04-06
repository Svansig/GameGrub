package app.gamegrub.ui.screen.library

import app.gamegrub.data.LibraryItem
import app.gamegrub.ui.data.LibraryState
import app.gamegrub.ui.enums.LibraryTab
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryScreenEmptyStateTest {
    @Test
    fun shouldShowLibraryEmptyStateSplash_steamLoggedOutAndNoGames_showsSplash() {
        val state = LibraryState(
            currentTab = LibraryTab.STEAM,
            isSteamLoggedIn = false,
            appInfoList = emptyList(),
        )

        assertTrue(shouldShowLibraryEmptyStateSplash(state))
    }

    @Test
    fun shouldShowLibraryEmptyStateSplash_steamLoggedOutWithCachedGames_hidesSplash() {
        val state = LibraryState(
            currentTab = LibraryTab.STEAM,
            isSteamLoggedIn = false,
            appInfoList = listOf(
                LibraryItem(
                    index = 0,
                    appId = "STEAM_10",
                    name = "Counter-Strike",
                ),
            ),
        )

        assertFalse(shouldShowLibraryEmptyStateSplash(state))
    }

    @Test
    fun shouldShowLibraryEmptyStateSplash_gogLoggedOut_showsSplash() {
        val state = LibraryState(
            currentTab = LibraryTab.GOG,
            isGogLoggedIn = false,
        )

        assertTrue(shouldShowLibraryEmptyStateSplash(state))
    }
}

