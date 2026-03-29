package app.gamegrub.ui.component.dialog.state

import androidx.compose.runtime.saveable.mapSaver

data class GameManagerDialogState(
    val visible: Boolean,
) {
    companion object {
        val Saver = mapSaver(
            save = { state ->
                mapOf(
                    "visible" to state.visible,
                )
            },
            restore = { savedMap ->
                GameManagerDialogState(
                    visible = savedMap["visible"] as Boolean,
                )
            },
        )
    }
}
