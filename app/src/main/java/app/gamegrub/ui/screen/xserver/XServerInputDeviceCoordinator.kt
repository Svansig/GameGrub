package app.gamegrub.ui.screen.xserver

/**
 * Encapsulates input-device transition decisions so XServerScreen keeps only
 * the Android-facing event wiring.
 */
internal object XServerInputDeviceCoordinator {
    data class DevicePresence(
        val hasPhysicalKeyboard: Boolean,
        val hasPhysicalMouse: Boolean,
        val hasPhysicalController: Boolean,
    )

    data class UiGuardState(
        val showElementEditor: Boolean,
        val keepPausedForEditor: Boolean,
        val showQuickMenu: Boolean,
        val isEditMode: Boolean,
        val isTouchscreenMode: Boolean,
        val hasUpdatedScreenGamepad: Boolean,
    )

    fun shouldEvaluateAutoShow(
        hasInternalTouchpad: Boolean,
        presence: DevicePresence,
        isTouchscreenMode: Boolean,
    ): Boolean {
        return !hasInternalTouchpad &&
                !presence.hasPhysicalMouse &&
                !presence.hasPhysicalKeyboard &&
                !presence.hasPhysicalController &&
                !isTouchscreenMode
    }

    fun shouldHideForExternalKeyboard(uiGuardState: UiGuardState): Boolean {
        return canProcessExternalTransition(uiGuardState)
    }

    fun shouldHideForExternalMouse(uiGuardState: UiGuardState): Boolean {
        return canProcessExternalTransition(uiGuardState)
    }

    fun shouldHideForExternalGamepad(uiGuardState: UiGuardState): Boolean {
        return canProcessExternalTransition(uiGuardState)
    }

    fun shouldHideForInternalTouchpad(uiGuardState: UiGuardState): Boolean {
        return !uiGuardState.showElementEditor &&
                !uiGuardState.keepPausedForEditor &&
                !uiGuardState.showQuickMenu &&
                !uiGuardState.isEditMode &&
                !uiGuardState.hasUpdatedScreenGamepad
    }

    private fun canProcessExternalTransition(uiGuardState: UiGuardState): Boolean {
        return !uiGuardState.showElementEditor &&
                !uiGuardState.keepPausedForEditor &&
                !uiGuardState.showQuickMenu &&
                !uiGuardState.isEditMode &&
                !uiGuardState.isTouchscreenMode &&
                !uiGuardState.hasUpdatedScreenGamepad
    }
}
