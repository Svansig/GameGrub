package app.gamegrub.ui

import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.core.view.WindowCompat

class ImmersiveModeManager(private val window: Window) {

    private var desiredSystemUiVisible: Boolean = false

    fun applyImmersiveMode() {
        if (desiredSystemUiVisible) {
            WindowCompat.setDecorFitsSystemWindows(window, true)
            window.insetsController?.show(
                WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars(),
            )
            return
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.insetsController?.let { controller ->
            controller.hide(
                WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars(),
            )
            controller.systemBarsBehavior =
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    fun onWindowFocusChanged(hasFocus: Boolean) {
        if (hasFocus && !desiredSystemUiVisible) {
            applyImmersiveMode()
        }
    }

    fun setSystemUIVisibility(visible: Boolean) {
        desiredSystemUiVisible = visible
        applyImmersiveMode()
    }

    fun isSystemUIVisible(): Boolean = desiredSystemUiVisible
}
