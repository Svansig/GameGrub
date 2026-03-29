package app.gamegrub.ui.screen.xserver

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class XServerInputEventDispatchCoordinatorTest {

    @Test
    fun handleKeyEvent_manualResume_consumesAButtonAndInvokesResumeCallback() {
        var resumeCalls = 0

        val handled = XServerInputEventDispatchCoordinator.handleKeyEvent(
            event = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_A),
            isKeyboard = false,
            isGamepad = true,
            uiGuardState = defaultUiGuardState(),
            manualResumeState = XServerInputEventDispatchCoordinator.ManualResumeState(
                manualResumeMode = true,
                isOverlayPaused = true,
                showQuickMenu = false,
                keepPausedForEditor = false,
            ),
            onResumeFromManualButton = { resumeCalls++ },
            dispatchGamepadKeyRaw = { false },
            dispatchGamepadKeyPhysicalController = { false },
            dispatchGamepadKeyInputControls = { false },
            dispatchKeyboardKey = { false },
        )

        assertTrue(handled)
        assertEquals(1, resumeCalls)
    }

    @Test
    fun handleKeyEvent_returnsFalse_whenUiGuardBlocksInput() {
        var rawCalls = 0

        val handled = XServerInputEventDispatchCoordinator.handleKeyEvent(
            event = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_A),
            isKeyboard = false,
            isGamepad = true,
            uiGuardState = defaultUiGuardState().copy(showQuickMenu = true),
            manualResumeState = defaultManualResumeState(),
            onResumeFromManualButton = { },
            dispatchGamepadKeyRaw = {
                rawCalls++
                false
            },
            dispatchGamepadKeyPhysicalController = { false },
            dispatchGamepadKeyInputControls = { false },
            dispatchKeyboardKey = { false },
        )

        assertFalse(handled)
        assertEquals(0, rawCalls)
    }

    @Test
    fun handleKeyEvent_dispatchesRawFirstForDpadKeys() {
        val order = mutableListOf<String>()

        val handled = XServerInputEventDispatchCoordinator.handleKeyEvent(
            event = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP),
            isKeyboard = false,
            isGamepad = true,
            uiGuardState = defaultUiGuardState(),
            manualResumeState = defaultManualResumeState(),
            onResumeFromManualButton = { },
            dispatchGamepadKeyRaw = {
                order += "raw"
                true
            },
            dispatchGamepadKeyPhysicalController = {
                order += "physical"
                false
            },
            dispatchGamepadKeyInputControls = {
                order += "controls"
                false
            },
            dispatchKeyboardKey = {
                order += "keyboard"
                false
            },
        )

        assertTrue(handled)
        assertEquals(listOf("raw"), order)
    }

    @Test
    fun handleKeyEvent_dispatchesRawLastForNonRawFirstGamepadKeys() {
        val order = mutableListOf<String>()

        val handled = XServerInputEventDispatchCoordinator.handleKeyEvent(
            event = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_MODE),
            isKeyboard = false,
            isGamepad = true,
            uiGuardState = defaultUiGuardState(),
            manualResumeState = defaultManualResumeState(),
            onResumeFromManualButton = { },
            dispatchGamepadKeyRaw = {
                order += "raw"
                true
            },
            dispatchGamepadKeyPhysicalController = {
                order += "physical"
                false
            },
            dispatchGamepadKeyInputControls = {
                order += "controls"
                false
            },
            dispatchKeyboardKey = {
                order += "keyboard"
                false
            },
        )

        assertTrue(handled)
        assertEquals(listOf("physical", "controls", "raw"), order)
    }

    @Test
    fun handleMotionEvent_returnsFalse_whenUiGuardBlocksGamepad() {
        val handled = XServerInputEventDispatchCoordinator.handleMotionEvent(
            event = null,
            isGamepad = true,
            uiGuardState = defaultUiGuardState().copy(showQuickMenu = true),
            isOverlayPaused = false,
            hasPointerCapture = false,
            dispatchGamepadMotionRaw = { false },
            dispatchGamepadMotionPhysicalController = { false },
            dispatchGamepadMotionInputControls = { false },
            onInternalTouchpadDetected = { },
            onHideInputControlsRequested = { },
            onTryCapturePointer = { },
        )

        assertFalse(handled)
    }

    private fun defaultUiGuardState(): XServerInputDeviceCoordinator.UiGuardState {
        return XServerInputDeviceCoordinator.UiGuardState(
            showElementEditor = false,
            keepPausedForEditor = false,
            showQuickMenu = false,
            isEditMode = false,
            isTouchscreenMode = false,
            hasUpdatedScreenGamepad = false,
        )
    }

    private fun defaultManualResumeState(): XServerInputEventDispatchCoordinator.ManualResumeState {
        return XServerInputEventDispatchCoordinator.ManualResumeState(
            manualResumeMode = false,
            isOverlayPaused = false,
            showQuickMenu = false,
            keepPausedForEditor = false,
        )
    }
}

