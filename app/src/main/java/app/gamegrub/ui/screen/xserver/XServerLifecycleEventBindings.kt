package app.gamegrub.ui.screen.xserver

import app.gamegrub.events.AndroidEvent
import app.gamegrub.events.SteamEvent
import app.gamegrub.ui.runtime.XServerRuntime
import com.winlator.core.Callback
import com.winlator.core.ProcessHelper

/**
 * Centralizes event bus and debug callback lifecycle wiring for XServerScreen.
 */
internal object XServerLifecycleEventBindings {
    fun register(
        onActivityDestroyed: (AndroidEvent.ActivityDestroyed) -> Unit,
        onKeyEvent: (AndroidEvent.KeyEvent) -> Boolean,
        onMotionEvent: (AndroidEvent.MotionEvent) -> Boolean,
        onGuestProgramTerminated: (AndroidEvent.GuestProgramTerminated) -> Unit,
        onForceCloseApp: (SteamEvent.ForceCloseApp) -> Unit,
        debugCallback: Callback<String>,
        addDebugCallback: (Callback<String>) -> Unit = ProcessHelper::addDebugCallback,
        removeDebugCallback: (Callback<String>) -> Unit = ProcessHelper::removeDebugCallback,
    ): () -> Unit {
        XServerRuntime.get().events.on<AndroidEvent.ActivityDestroyed, Unit>(onActivityDestroyed)
        XServerRuntime.get().events.on<AndroidEvent.KeyEvent, Boolean>(onKeyEvent)
        XServerRuntime.get().events.on<AndroidEvent.MotionEvent, Boolean>(onMotionEvent)
        XServerRuntime.get().events.on<AndroidEvent.GuestProgramTerminated, Unit>(onGuestProgramTerminated)
        XServerRuntime.get().events.on<SteamEvent.ForceCloseApp, Unit>(onForceCloseApp)
        addDebugCallback(debugCallback)

        return {
            XServerRuntime.get().events.off<AndroidEvent.ActivityDestroyed, Unit>(onActivityDestroyed)
            XServerRuntime.get().events.off<AndroidEvent.KeyEvent, Boolean>(onKeyEvent)
            XServerRuntime.get().events.off<AndroidEvent.MotionEvent, Boolean>(onMotionEvent)
            XServerRuntime.get().events.off<AndroidEvent.GuestProgramTerminated, Unit>(onGuestProgramTerminated)
            XServerRuntime.get().events.off<SteamEvent.ForceCloseApp, Unit>(onForceCloseApp)
            removeDebugCallback(debugCallback)
        }
    }
}
