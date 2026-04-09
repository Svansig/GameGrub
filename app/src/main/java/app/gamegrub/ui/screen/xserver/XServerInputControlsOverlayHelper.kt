package app.gamegrub.ui.screen.xserver

import android.view.View
import app.gamegrub.GameGrubApp
import app.gamegrub.ui.runtime.XServerRuntime
import com.winlator.container.Container
import com.winlator.inputcontrols.ControllerManager
import com.winlator.inputcontrols.ControlsProfile
import com.winlator.winhandler.WinHandler
import timber.log.Timber

/**
 * Centralizes on-screen controls overlay behavior for the XServer screen.
 *
 * The helper intentionally preserves the existing GameGrubApp-based state model
 * so this extraction stays behavior-neutral while shrinking XServerScreen.
 */
internal object XServerInputControlsOverlayHelper {
    fun showInputControls(profile: ControlsProfile, winHandler: WinHandler, container: Container) {
        profile.isVirtualGamepad = true

        GameGrubApp.inputControlsView?.let { icView ->
            if (!profile.isElementsLoaded || icView.width == 0 || icView.height == 0) {
                if (icView.width == 0 || icView.height == 0) {
                    Timber.d("Deferring element loading until view has dimensions")
                    icView.post {
                        Timber.d("Loading elements after layout: ${icView.width}x${icView.height}")
                        profile.loadElements(icView)
                        icView.profile = profile
                        icView.isShowTouchscreenControls = true
                        icView.visibility = View.VISIBLE
                        icView.requestFocus()
                        icView.invalidate()
                    }
                } else {
                    Timber.d("Loading elements with dimensions: ${icView.width}x${icView.height}")
                    profile.loadElements(icView)
                    icView.profile = profile
                    icView.isShowTouchscreenControls = true
                    icView.visibility = View.VISIBLE
                    icView.requestFocus()
                    icView.invalidate()
                }
            } else {
                Timber.d("Elements already loaded, showing controls")
                icView.profile = profile
                icView.isShowTouchscreenControls = true
                icView.visibility = View.VISIBLE
                icView.requestFocus()
                icView.invalidate()
            }
        }

        GameGrubApp.touchpadView?.setSensitivity(profile.cursorSpeed * 1.0f)
        GameGrubApp.touchpadView?.setPointerButtonRightEnabled(false)

        if (container.containerVariant.equals(Container.BIONIC) && profile.isVirtualGamepad) {
            val controllerManager = ControllerManager.getInstance()
            controllerManager.setSlotEnabled(0, true)
            controllerManager.unassignSlot(0)
            winHandler.refreshControllerMappings()
        }
    }

    fun hideInputControls() {
        GameGrubApp.inputControlsView?.isShowTouchscreenControls = false
        GameGrubApp.inputControlsView?.visibility = View.GONE
        GameGrubApp.inputControlsView?.profile = null

        GameGrubApp.touchpadView?.setSensitivity(1.0f)
        GameGrubApp.touchpadView?.setPointerButtonLeftEnabled(true)
        GameGrubApp.touchpadView?.setPointerButtonRightEnabled(true)
        GameGrubApp.touchpadView?.isEnabled?.let {
            if (!it) {
                GameGrubApp.touchpadView?.isEnabled = true
                XServerRuntime.get().xServerView?.renderer?.setCursorVisible(true)
            }
        }
        GameGrubApp.inputControlsView?.invalidate()
    }
}
