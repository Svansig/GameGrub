package app.gamegrub.ui

import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.core.view.WindowCompat

/**
 * Sole owner for immersive mode and system bar visibility writes on a [Window].
 *
 * This manager centralizes calls that modify decor fitting and status/navigation bar visibility,
 * so call sites can request intent (`visible` vs immersive) without duplicating platform details.
 *
 * @property window Target window that receives all system UI updates.
 */
class ImmersiveModeManager(private val window: Window) {

    /**
     * Last requested visibility state for status and navigation bars.
     *
     * `true` means bars should be visible; `false` means immersive mode should hide them.
     */
    private var desiredSystemUiVisible: Boolean = false

    /**
     * Apply the current desired system UI state to [window].
     *
     * This method is safe to call repeatedly and is used by lifecycle and focus callbacks.
     */
    fun applyImmersiveMode() {
        applySystemUiVisibility(desiredSystemUiVisible)
    }

    /**
     * Re-assert immersive mode on focus regain when bars are expected to stay hidden.
     *
     * @param hasFocus `true` when the host window gained focus.
     */
    fun onWindowFocusChanged(hasFocus: Boolean) {
        if (hasFocus && !desiredSystemUiVisible) {
            applyImmersiveMode()
        }
    }

    /**
     * Request system bar visibility and apply it immediately.
     *
     * @param visible `true` to show status/navigation bars, `false` for immersive hidden bars.
     */
    fun setSystemUIVisibility(visible: Boolean) {
        desiredSystemUiVisible = visible
        applyImmersiveMode()
    }

    /**
     * Report the currently requested system UI visibility state.
     *
     * @return `true` when bars are requested visible, otherwise `false`.
     */
    fun isSystemUIVisible(): Boolean = desiredSystemUiVisible

    /**
     * Write the concrete platform window-insets behavior for a requested visibility state.
     *
     * @param visible Desired status/navigation bar visibility.
     */
    private fun applySystemUiVisibility(visible: Boolean) {
        if (visible) {
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
}
