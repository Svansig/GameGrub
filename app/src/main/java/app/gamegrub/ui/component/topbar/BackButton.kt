package app.gamegrub.ui.component.topbar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.gamegrub.ui.theme.GameGrubTheme

@Composable
fun BackButton(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        content = {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Navigate Back")
        },
    )
}

@Preview
@Composable
private fun Preview_BackButton() {
    GameGrubTheme {
        Surface {
            BackButton(onClick = {})
        }
    }
}
