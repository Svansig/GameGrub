package app.gamegrub.ui.screen.xserver

import android.view.Display
import android.view.View
import android.view.WindowInsets
import android.view.inputmethod.InputMethodManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import app.gamegrub.GameGrubApp
import app.gamegrub.PrefManager
import app.gamegrub.externaldisplay.IMEInputReceiver
import app.gamegrub.ui.component.QuickMenuAction
import app.gamegrub.ui.runtime.XServerRuntime
import app.gamegrub.utils.container.ContainerUtils
import com.posthog.PostHog
import com.winlator.container.Container
import com.winlator.widget.XServerView
import timber.log.Timber

/**
 * Centralizes quick-menu and overlay action behavior while keeping Compose state
 * ownership in XServerScreen through the supplied callbacks.
 */
internal object XServerOverlayActionCoordinator {
    fun pauseForOverlayIfAllowed(neverSuspend: Boolean) {
        if (neverSuspend) {
            Timber.d("Skipping overlay suspend due to suspend policy=never")
            return
        }
        XServerRuntime.get().xEnvironment?.onPause()
        GameGrubApp.isOverlayPaused = true
    }

    fun resumeIfAllowedAfterOverlay(neverSuspend: Boolean, manualResumeMode: Boolean) {
        if (!GameGrubApp.isOverlayPaused) {
            return
        }
        if (neverSuspend) {
            clearOverlayPauseState()
            return
        }
        if (manualResumeMode) {
            Timber.d("Keeping game suspended until Resume is pressed")
            return
        }
        XServerRuntime.get().xEnvironment?.onResume()
        clearOverlayPauseState()
    }

    fun resumeFromManualButton(neverSuspend: Boolean, onKeepPausedForEditorChanged: (Boolean) -> Unit) {
        if (!GameGrubApp.isOverlayPaused) {
            return
        }
        if (!neverSuspend) {
            XServerRuntime.get().xEnvironment?.onResume()
        }
        onKeepPausedForEditorChanged(false)
        clearOverlayPauseState()
    }

    fun dismissOverlayMenu(
        imeInputReceiver: IMEInputReceiver?,
        keyboardRequestedFromOverlay: Boolean,
        keepPausedForEditor: Boolean,
        manualResumeMode: Boolean,
        neverSuspend: Boolean,
        onKeyboardRequestedFromOverlayChanged: (Boolean) -> Unit,
        onShowQuickMenuChanged: (Boolean) -> Unit,
    ) {
        if (!keyboardRequestedFromOverlay) {
            imeInputReceiver?.hideKeyboard()
        }
        val resumeImmediatelyForKeyboard = keyboardRequestedFromOverlay && manualResumeMode
        onKeyboardRequestedFromOverlayChanged(false)
        if (!keepPausedForEditor) {
            if (resumeImmediatelyForKeyboard) {
                forceResumeIfSuspended(neverSuspend)
            } else {
                resumeIfAllowedAfterOverlay(neverSuspend, manualResumeMode)
            }
        }
        onShowQuickMenuChanged(false)
    }

    fun handleQuickMenuAction(
        itemId: Int,
        anchorView: View,
        imm: InputMethodManager,
        imeInputReceiver: IMEInputReceiver?,
        container: Container,
        appId: String,
        xServerView: XServerView?,
        areControlsVisible: Boolean,
        onAreControlsVisibleChanged: (Boolean) -> Unit,
        isPerformanceHudEnabled: Boolean,
        onPerformanceHudEnabledChanged: (Boolean) -> Unit,
        neverSuspend: Boolean,
        onKeepPausedForEditorChanged: (Boolean) -> Unit,
        onKeyboardRequestedFromOverlayChanged: (Boolean) -> Unit,
        onTogglePerformanceHud: (Boolean) -> Unit,
        onEditControlsRequested: () -> Unit,
        onEditPhysicalControllerRequested: () -> Unit,
        onExitRequested: () -> Unit,
    ): Boolean {
        return when (itemId) {
            QuickMenuAction.KEYBOARD -> {
                showKeyboard(anchorView, imm, imeInputReceiver, onKeyboardRequestedFromOverlayChanged)
                true
            }

            QuickMenuAction.INPUT_CONTROLS -> {
                toggleInputControls(
                    container = container,
                    xServerView = xServerView,
                    areControlsVisible = areControlsVisible,
                    onAreControlsVisibleChanged = onAreControlsVisibleChanged,
                )
                true
            }

            QuickMenuAction.EDIT_CONTROLS -> {
                PostHog.capture(event = "edit_controls_in_game")
                onKeepPausedForEditorChanged(true)
                onEditControlsRequested()
                true
            }

            QuickMenuAction.EDIT_PHYSICAL_CONTROLLER -> {
                PostHog.capture(event = "edit_physical_controller_from_menu")
                onKeepPausedForEditorChanged(true)
                onEditPhysicalControllerRequested()
                true
            }

            QuickMenuAction.PERFORMANCE_HUD -> {
                val enabled = !isPerformanceHudEnabled
                onPerformanceHudEnabledChanged(enabled)
                PrefManager.showFps = enabled
                onTogglePerformanceHud(enabled)
                PostHog.capture(
                    event = "performance_hud_toggled",
                    properties = mapOf("enabled" to enabled),
                )
                false
            }

            QuickMenuAction.EXIT_GAME -> {
                PostHog.capture(
                    event = "game_closed",
                    properties = mapOf(
                        "game_name" to ContainerUtils.resolveGameName(appId),
                        "game_store" to ContainerUtils.extractGameSourceFromContainerId(appId).name,
                    ),
                )
                imeInputReceiver?.hideKeyboard()
                forceResumeIfSuspended(neverSuspend)
                onExitRequested()
                true
            }

            else -> false
        }
    }

    fun handleBack(
        anchorView: View,
        imeInputReceiver: IMEInputReceiver?,
        showQuickMenu: Boolean,
        neverSuspend: Boolean,
        onDismissOverlayMenu: () -> Unit,
        onKeyboardRequestedFromOverlayChanged: (Boolean) -> Unit,
        onShowQuickMenuChanged: (Boolean) -> Unit,
        onHasPhysicalControllerChanged: (Boolean) -> Unit,
    ) {
        val imeVisible = ViewCompat.getRootWindowInsets(anchorView)
            ?.isVisible(WindowInsetsCompat.Type.ime()) == true

        if (imeVisible) {
            PostHog.capture(event = "onscreen_keyboard_disabled")
            imeInputReceiver?.hideKeyboard()
            anchorView.post {
                anchorView.windowInsetsController?.hide(WindowInsets.Type.ime())
            }
            return
        }

        if (showQuickMenu) {
            onDismissOverlayMenu()
            return
        }

        Timber.i("BackHandler")
        pauseForOverlayIfAllowed(neverSuspend)
        onKeyboardRequestedFromOverlayChanged(false)

        val controllerManager = com.winlator.inputcontrols.ControllerManager.getInstance()
        controllerManager.scanForDevices()
        onHasPhysicalControllerChanged(controllerManager.detectedDevices.isNotEmpty())
        XServerRuntime.get().touchpadView?.postDelayed(
            {
                val touchpadView = XServerRuntime.get().touchpadView
                touchpadView?.releasePointerCapture()
            },
            100,
        )

        onShowQuickMenuChanged(true)
    }

    private fun clearOverlayPauseState() {
        GameGrubApp.isOverlayPaused = false
    }

    private fun forceResumeIfSuspended(neverSuspend: Boolean) {
        if (GameGrubApp.isOverlayPaused && !neverSuspend) {
            XServerRuntime.get().xEnvironment?.onResume()
        }
        clearOverlayPauseState()
    }

    @Suppress("DEPRECATION")
    private fun showKeyboard(
        anchorView: View,
        imm: InputMethodManager,
        imeInputReceiver: IMEInputReceiver?,
        onKeyboardRequestedFromOverlayChanged: (Boolean) -> Unit,
    ) {
        onKeyboardRequestedFromOverlayChanged(true)
        anchorView.post {
            if (anchorView.windowToken == null) {
                return@post
            }
            val showKeyboard = {
                PostHog.capture(event = "onscreen_keyboard_enabled")
                val isExternalDisplaySession =
                    (anchorView.display?.displayId ?: Display.DEFAULT_DISPLAY) != Display.DEFAULT_DISPLAY

                if (isExternalDisplaySession) {
                    imeInputReceiver?.showKeyboard() ?: imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
                } else {
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
                }
            }
            anchorView.postDelayed({ showKeyboard() }, 500)
        }
    }

    private fun toggleInputControls(
        container: Container,
        xServerView: XServerView?,
        areControlsVisible: Boolean,
        onAreControlsVisibleChanged: (Boolean) -> Unit,
    ) {
        if (areControlsVisible) {
            PostHog.capture(event = "onscreen_controller_disabled")
            XServerInputControlsOverlayHelper.hideInputControls()
        } else {
            PostHog.capture(event = "onscreen_controller_enabled")
            val targetProfile = XServerControlsProfileResolver.resolveCurrentOrFallbackProfile(
                manager = XServerRuntime.get().inputControlsManager,
                container = container,
            )
            if (targetProfile != null) {
                XServerInputControlsOverlayHelper.showInputControls(
                    targetProfile,
                    requireNotNull(xServerView).getxServer().winHandler,
                    container,
                )
            }
        }
        onAreControlsVisibleChanged(!areControlsVisible)
    }
}
