package app.gamegrub.ui.screen.xserver

import app.gamegrub.PrefManager
import com.winlator.container.Container
import com.winlator.inputcontrols.ControlsProfile
import com.winlator.inputcontrols.InputControlsManager
import com.winlator.widget.InputControlsView
import com.winlator.xserver.XServer
import timber.log.Timber

/**
 * Centralizes physical-controller profile loading and persistence flow used by XServerScreen.
 */
internal object XServerPhysicalControllerCoordinator {
    fun resolveStartupProfile(
        manager: InputControlsManager?,
        container: Container,
    ): ControlsProfile? {
        Timber.d("=== Profile Loading Start ===")
        Timber.d("Container: %s", container.name)

        val targetProfile = XServerControlsProfileResolver.resolveCurrentOrFallbackProfile(
            manager = manager,
            container = container,
        )

        if (targetProfile == null) {
            Timber.d("No controls profile available")
            return null
        }

        val profileId = container.getExtra("profileId", "0").toIntOrNull() ?: 0
        if (profileId != 0 && targetProfile.id == profileId) {
            Timber.d("Using CUSTOM profile: %s (ID: %s)", targetProfile.name, targetProfile.id)
        } else {
            Timber.d("Using DEFAULT profile: %s (ID: %s)", targetProfile.name, targetProfile.id)
        }

        val controllers = targetProfile.loadControllers()
        Timber.d("Controllers loaded: %s controller(s)", controllers.size)
        controllers.forEachIndexed { index, controller ->
            Timber.d(
                "  [%s] ID: %s, Name: %s, Bindings: %s",
                index,
                controller.id,
                controller.name,
                controller.controllerBindingCount,
            )
        }
        Timber.d("=== Profile Loading Complete ===")

        return targetProfile
    }

    fun applyInputControlsViewDefaults(inputControlsView: InputControlsView, container: Container) {
        val opacity = PrefManager.getFloat("controls_opacity", InputControlsView.DEFAULT_OVERLAY_OPACITY)
        inputControlsView.setOverlayOpacity(opacity)
        inputControlsView.setContainerShooterMode(container.isShooterMode)
    }

    fun createOrUpdateHandler(
        existingHandler: PhysicalControllerHandler?,
        profile: ControlsProfile,
        xServer: XServer?,
        onOpenNavigationMenu: (() -> Unit)?,
    ): PhysicalControllerHandler {
        return if (existingHandler != null) {
            existingHandler.setProfile(profile)
            existingHandler
        } else {
            PhysicalControllerHandler(profile, xServer, onOpenNavigationMenu)
        }
    }

    fun shouldShowOnScreenControls(
        container: Container,
        hasPhysicalController: Boolean,
        hasPhysicalKeyboard: Boolean,
        hasPhysicalMouse: Boolean,
    ): Boolean {
        return when {
            container.isTouchscreenMode -> false
            hasPhysicalController -> false
            hasPhysicalKeyboard || hasPhysicalMouse -> false
            else -> true
        }
    }

    fun resolveDialogProfile(
        manager: InputControlsManager,
        container: Container,
        gameName: String,
    ): ControlsProfile? {
        return XServerControlsProfileResolver.getOrCreateContainerProfile(
            manager = manager,
            container = container,
            gameName = gameName,
            suffix = "Physical Controller",
        )
    }

    fun persistDialogProfileChanges(profile: ControlsProfile, container: Container) {
        profile.addController("*")
        container.putExtra("profileId", profile.id.toString())
        container.saveData()
        profile.save()
        profile.loadControllers()
    }
}
