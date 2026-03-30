package app.gamegrub.ui.screen.xserver

import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.winlator.widget.XServerView
import timber.log.Timber

/**
 * Wires XServerView renderer pause/resume with host lifecycle + attach state.
 */
internal object XServerRendererLifecycleCoordinator {
    internal enum class RendererSyncAction {
        NONE,
        RESUME,
        PAUSE,
    }

    fun register(
        lifecycleOwner: LifecycleOwner,
        xServerView: XServerView,
    ): () -> Unit {
        fun syncRendererToCurrentLifecycleState() {
            when (resolveRendererSyncAction(xServerView.isAttachedToWindow, lifecycleOwner.lifecycle.currentState)) {
                RendererSyncAction.NONE -> Unit

                RendererSyncAction.RESUME -> {
                    Timber.d("Synchronizing XServerView renderer to current resumed lifecycle state")
                    xServerView.onResume()
                }

                RendererSyncAction.PAUSE -> {
                    Timber.d("Synchronizing XServerView renderer to current paused lifecycle state")
                    xServerView.onPause()
                }
            }
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_RESUME,
                -> {
                    Timber.d("Synchronizing XServerView renderer for lifecycle event: %s", event)
                    syncRendererToCurrentLifecycleState()
                }

                else -> Unit
            }
        }

        val attachStateListener = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                syncRendererToCurrentLifecycleState()
            }

            override fun onViewDetachedFromWindow(v: View) = Unit
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        xServerView.addOnAttachStateChangeListener(attachStateListener)
        syncRendererToCurrentLifecycleState()

        return {
            xServerView.removeOnAttachStateChangeListener(attachStateListener)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    internal fun resolveRendererSyncAction(
        isAttachedToWindow: Boolean,
        lifecycleState: Lifecycle.State,
    ): RendererSyncAction {
        if (!isAttachedToWindow) {
            return RendererSyncAction.NONE
        }
        return when {
            lifecycleState == Lifecycle.State.DESTROYED -> RendererSyncAction.NONE
            lifecycleState.isAtLeast(Lifecycle.State.RESUMED) -> RendererSyncAction.RESUME
            else -> RendererSyncAction.PAUSE
        }
    }
}
