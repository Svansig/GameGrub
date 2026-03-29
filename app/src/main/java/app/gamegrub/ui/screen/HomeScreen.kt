package app.gamegrub.ui.screen

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import app.gamegrub.ui.model.HomeViewModel
import app.gamegrub.ui.screen.library.HomeLibraryScreen
import app.gamegrub.ui.theme.GameGrubTheme

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onChat: (Long) -> Unit,
    onClickExit: () -> Unit,
    onClickPlay: (String, Boolean) -> Unit,
    onTestGraphics: (String) -> Unit,
    onLogout: () -> Unit,
    onNavigateRoute: (String) -> Unit,
    onGoOnline: () -> Unit,
    isOffline: Boolean = false,
) {
    // Pressing back while logged in, confirm we want to close the app.
    BackHandler {
        onClickExit()
    }

    // Always show the Library screen
    HomeLibraryScreen(
        onClickPlay = onClickPlay,
        onTestGraphics = onTestGraphics,
        onNavigateRoute = onNavigateRoute,
        onLogout = onLogout,
        onGoOnline = onGoOnline,
        isOffline = isOffline,
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
    device = "spec:width=1080px,height=1920px,dpi=440,orientation=landscape",
)
@Composable
private fun Preview_HomeScreenContent() {
    GameGrubTheme {
        HomeScreen(
            onChat = {},
            onClickPlay = { _, _ -> },
            onTestGraphics = { },
            onLogout = {},
            onNavigateRoute = {},
            onClickExit = {},
            onGoOnline = {},
        )
    }
}
