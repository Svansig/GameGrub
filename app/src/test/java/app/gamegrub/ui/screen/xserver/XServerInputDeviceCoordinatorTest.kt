package app.gamegrub.ui.screen.xserver

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class XServerInputDeviceCoordinatorTest {

    @Test
    fun shouldEvaluateAutoShow_returnsTrue_whenNoExternalDevicesAndNoInternalTouchpad() {
        val presence = XServerInputDeviceCoordinator.DevicePresence(
            hasPhysicalKeyboard = false,
            hasPhysicalMouse = false,
            hasPhysicalController = false,
        )

        val shouldEvaluate = XServerInputDeviceCoordinator.shouldEvaluateAutoShow(
            hasInternalTouchpad = false,
            presence = presence,
            isTouchscreenMode = false,
        )

        assertTrue(shouldEvaluate)
    }

    @Test
    fun shouldEvaluateAutoShow_returnsFalse_whenTouchscreenModeEnabled() {
        val presence = XServerInputDeviceCoordinator.DevicePresence(
            hasPhysicalKeyboard = false,
            hasPhysicalMouse = false,
            hasPhysicalController = false,
        )

        val shouldEvaluate = XServerInputDeviceCoordinator.shouldEvaluateAutoShow(
            hasInternalTouchpad = false,
            presence = presence,
            isTouchscreenMode = true,
        )

        assertFalse(shouldEvaluate)
    }

    @Test
    fun shouldHideForExternalKeyboard_returnsTrue_whenUiIsInteractive() {
        val guard = XServerInputDeviceCoordinator.UiGuardState(
            showElementEditor = false,
            keepPausedForEditor = false,
            showQuickMenu = false,
            isEditMode = false,
            isTouchscreenMode = false,
            hasUpdatedScreenGamepad = false,
        )

        assertTrue(XServerInputDeviceCoordinator.shouldHideForExternalKeyboard(guard))
        assertTrue(XServerInputDeviceCoordinator.shouldHideForExternalMouse(guard))
        assertTrue(XServerInputDeviceCoordinator.shouldHideForExternalGamepad(guard))
    }

    @Test
    fun shouldHideForExternalInput_returnsFalse_whenQuickMenuVisible() {
        val guard = XServerInputDeviceCoordinator.UiGuardState(
            showElementEditor = false,
            keepPausedForEditor = false,
            showQuickMenu = true,
            isEditMode = false,
            isTouchscreenMode = false,
            hasUpdatedScreenGamepad = false,
        )

        assertFalse(XServerInputDeviceCoordinator.shouldHideForExternalKeyboard(guard))
        assertFalse(XServerInputDeviceCoordinator.shouldHideForExternalMouse(guard))
        assertFalse(XServerInputDeviceCoordinator.shouldHideForExternalGamepad(guard))
    }

    @Test
    fun shouldHideForExternalInput_returnsFalse_whenAlreadyUpdated() {
        val guard = XServerInputDeviceCoordinator.UiGuardState(
            showElementEditor = false,
            keepPausedForEditor = false,
            showQuickMenu = false,
            isEditMode = false,
            isTouchscreenMode = false,
            hasUpdatedScreenGamepad = true,
        )

        assertFalse(XServerInputDeviceCoordinator.shouldHideForExternalKeyboard(guard))
        assertFalse(XServerInputDeviceCoordinator.shouldHideForExternalMouse(guard))
        assertFalse(XServerInputDeviceCoordinator.shouldHideForExternalGamepad(guard))
    }

    @Test
    fun shouldHideForInternalTouchpad_ignoresTouchscreenModeButRespectsUiGuards() {
        val touchscreenGuard = XServerInputDeviceCoordinator.UiGuardState(
            showElementEditor = false,
            keepPausedForEditor = false,
            showQuickMenu = false,
            isEditMode = false,
            isTouchscreenMode = true,
            hasUpdatedScreenGamepad = false,
        )
        val blockedGuard = touchscreenGuard.copy(showQuickMenu = true)

        assertTrue(XServerInputDeviceCoordinator.shouldHideForInternalTouchpad(touchscreenGuard))
        assertFalse(XServerInputDeviceCoordinator.shouldHideForInternalTouchpad(blockedGuard))
    }
}

