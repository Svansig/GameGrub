package app.gamegrub.ui

import android.view.KeyEvent
import android.view.MotionEvent
import app.gamegrub.GameGrubApp
import app.gamegrub.events.AndroidEvent
import app.gamegrub.ui.runtime.XServerRuntime

/**
 * Dispatches input events (key and motion) to registered listeners.
 * Allows UI components to handle hardware input before falling through
 * to default Android handling.
 */
class InputEventDispatcher {

    /**
     * Dispatch a key event to listeners.
     * @return true if event was consumed by a listener
     */
    fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val eventDispatched = XServerRuntime.get().events.emit(AndroidEvent.KeyEvent(event)) { keyEvent ->
            keyEvent.any { it }
        } == true

        return eventDispatched
    }

    /**
     * Dispatch a generic motion event (e.g., gamepad, joystick) to listeners.
     * @return true if event was consumed by a listener
     */
    fun dispatchGenericMotionEvent(event: MotionEvent?): Boolean {
        val eventDispatched = XServerRuntime.get().events.emit(AndroidEvent.MotionEvent(event)) { ev ->
            ev.any { it }
        } == true

        return eventDispatched
    }
}
