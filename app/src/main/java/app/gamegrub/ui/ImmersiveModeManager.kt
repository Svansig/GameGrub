package app.gamegrub.ui

import android.os.Build
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import timber.log.Timber

/**
 * Manages immersive fullscreen mode for the app window.
 * Handles showing/hiding system bars for console-like experience.
 */
class ImmersiveModeManager(private val window: Window) {

    private var desiredSystemUiVisible: Boolean = false

    /**
     * Apply immersive mode based on current visibility state.
     * Must be called in multiple lifecycle methods to ensure bars stay hidden.
     */
    fun applyImmersiveMode() {
        if (desiredSystemUiVisible) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(true)
                window.insetsController?.show(
                    WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars(),
                )
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_VISIBLE
            }
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let { controller ->
                controller.hide(
                    WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars(),
                )
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                    or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }
    }

    /**
     * Called when window gains focus - ensures immersive mode persists.
     */
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
