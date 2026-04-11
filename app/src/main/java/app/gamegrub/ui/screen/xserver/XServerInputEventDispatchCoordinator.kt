package app.gamegrub.ui.screen.xserver

import android.view.KeyEvent
import android.view.MotionEvent

/**
 * Encapsulates key and motion event dispatch ordering while keeping state and
 * side effects in XServerScreen.
 */
internal object XServerInputEventDispatchCoordinator {
    data class ManualResumeState(
        val manualResumeMode: Boolean,
        val isOverlayPaused: Boolean,
        val showQuickMenu: Boolean,
        val keepPausedForEditor: Boolean,
    )

    fun handleKeyEvent(
        event: KeyEvent,
        isKeyboard: Boolean,
        isGamepad: Boolean,
        uiGuardState: XServerInputDeviceCoordinator.UiGuardState,
        manualResumeState: ManualResumeState,
        onResumeFromManualButton: () -> Unit,
        dispatchGamepadKeyRaw: (KeyEvent) -> Boolean,
        dispatchGamepadKeyPhysicalController: (KeyEvent) -> Boolean,
        dispatchGamepadKeyInputControls: (KeyEvent) -> Boolean,
        dispatchKeyboardKey: (KeyEvent) -> Boolean,
    ): Boolean {
        val waitingForManualResume =
            manualResumeState.manualResumeMode &&
                    manualResumeState.isOverlayPaused &&
                    !manualResumeState.showQuickMenu &&
                    !manualResumeState.keepPausedForEditor

        if (waitingForManualResume && isGamepad) {
            return when (event.keyCode) {
                KeyEvent.KEYCODE_BUTTON_A,
                KeyEvent.KEYCODE_BUTTON_START,
                    -> {
                    if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                        onResumeFromManualButton()
                    }
                    true
                }

                else -> false
            }
        }

        if ((
                    uiGuardState.showElementEditor ||
                            uiGuardState.keepPausedForEditor ||
                            uiGuardState.showQuickMenu ||
                            uiGuardState.isEditMode
                    ) && (isGamepad || isKeyboard)
        ) {
            return false
        }

        var handled = false
        if (isGamepad) {
            val rawFirstKey = isRawFirstGamepadKey(event.keyCode)

            if (rawFirstKey) {
                handled = dispatchGamepadKeyRaw(event)
            }
            if (!handled) {
                handled = dispatchGamepadKeyPhysicalController(event)
            }
            if (!handled) {
                handled = dispatchGamepadKeyInputControls(event)
            }
            if (!handled && !rawFirstKey) {
                handled = dispatchGamepadKeyRaw(event)
            }
        }
        if (!handled && isKeyboard) {
            handled = dispatchKeyboardKey(event)
        }

        return handled
    }

    fun handleMotionEvent(
        event: MotionEvent?,
        isGamepad: Boolean,
        uiGuardState: XServerInputDeviceCoordinator.UiGuardState,
        isOverlayPaused: Boolean,
        hasPointerCapture: Boolean,
        dispatchGamepadMotionRaw: (MotionEvent) -> Boolean,
        dispatchGamepadMotionPhysicalController: (MotionEvent) -> Boolean,
        dispatchGamepadMotionInputControls: (MotionEvent) -> Boolean,
        onInternalTouchpadDetected: () -> Unit,
        onHideInputControlsRequested: () -> Unit,
        onTryCapturePointer: () -> Unit,
    ): Boolean {
        if ((
                    uiGuardState.showElementEditor ||
                            uiGuardState.keepPausedForEditor ||
                            uiGuardState.showQuickMenu ||
                            uiGuardState.isEditMode
                    ) && isGamepad
        ) {
            return false
        }

        var handled = false
        if (isGamepad && event != null) {
            handled = dispatchGamepadMotionRaw(event)
            if (!handled) {
                handled = dispatchGamepadMotionPhysicalController(event)
            }
            if (!handled) {
                handled = dispatchGamepadMotionInputControls(event)
            }
        }

        if (!hasPointerCapture && !isOverlayPaused && event != null) {
            val device = event.device
            val isExternal = device.isExternal
            if (device.supportsSource(android.view.InputDevice.SOURCE_TOUCHPAD) && !isExternal) {
                onInternalTouchpadDetected()
                if (XServerInputDeviceCoordinator.shouldHideForInternalTouchpad(uiGuardState)) {
                    onHideInputControlsRequested()
                }
            }
            onTryCapturePointer()
        }

        return handled
    }

    private fun isRawFirstGamepadKey(keyCode: Int): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_BUTTON_A,
            KeyEvent.KEYCODE_BUTTON_B,
            KeyEvent.KEYCODE_BUTTON_X,
            KeyEvent.KEYCODE_BUTTON_Y,
            KeyEvent.KEYCODE_BUTTON_L1,
            KeyEvent.KEYCODE_BUTTON_R1,
            KeyEvent.KEYCODE_BUTTON_L2,
            KeyEvent.KEYCODE_BUTTON_R2,
            KeyEvent.KEYCODE_BUTTON_SELECT,
            KeyEvent.KEYCODE_BUTTON_START,
                -> true

            else -> false
        }
    }
}
