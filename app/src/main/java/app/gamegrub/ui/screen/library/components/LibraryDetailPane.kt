package app.gamegrub.ui.screen.library.components

import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import app.gamegrub.PrefManager
import app.gamegrub.data.GameSource
import app.gamegrub.data.LibraryItem
import app.gamegrub.ui.data.LibraryState
import app.gamegrub.ui.enums.AppFilter
import app.gamegrub.ui.screen.library.AppScreen
import app.gamegrub.ui.theme.GameGrubTheme
import java.util.EnumSet

@Composable
internal fun LibraryDetailPane(
    libraryItem: LibraryItem?,
    onClickPlay: (Boolean) -> Unit,
    onTestGraphics: () -> Unit,
    onBack: () -> Unit,
) {
    Surface {
        if (libraryItem == null) {
            val listState = rememberLazyGridState()
            val emptyState = remember {
                LibraryState(
                    appInfoList = emptyList(),
                    appInfoSortType = EnumSet.of(AppFilter.GAME),
                )
            }

            LibraryListPane(
                state = emptyState,
                listState = listState,
                currentLayout = PrefManager.libraryLayout,
                onPageChange = {},
                onNavigate = {},
                onRefresh = {},
            )
        } else {
            AppScreen(
                libraryItem = libraryItem,
                onClickPlay = onClickPlay,
                onTestGraphics = onTestGraphics,
                onBack = onBack,
            )
        }
    }
}

/***********
 * PREVIEW *
 ***********/

@Preview(uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES or android.content.res.Configuration.UI_MODE_TYPE_NORMAL)
@Preview(device = "spec:width=1920px,height=1080px,dpi=440") // Odin2 Mini
@Composable
private fun Preview_LibraryDetailPane() {
    PrefManager.init(LocalContext.current)
    GameGrubTheme {
        LibraryDetailPane(
            libraryItem = LibraryItem(
                appId = "${GameSource.STEAM.name}_${Int.MAX_VALUE}",
                name = "Preview Game",
                iconHash = "",
                gameSource = GameSource.STEAM,
            ),
            onClickPlay = { },
            onTestGraphics = { },
            onBack = { },
        )
    }
}
