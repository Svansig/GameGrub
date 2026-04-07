package app.gamegrub.ui.runtime

import androidx.compose.runtime.mutableStateOf
import app.gamegrub.events.EventDispatcher
import app.gamegrub.service.steam.AchievementWatcher
import com.winlator.inputcontrols.InputControlsManager
import com.winlator.widget.InputControlsView
import com.winlator.widget.TouchpadView
import com.winlator.widget.XServerView
import com.winlator.xenvironment.XEnvironment
import com.winlator.container.Container

class XServerRuntimeState {

    val events = EventDispatcher()

    var onDestinationChangedListener: app.gamegrub.NavChangedListener? = null

    private val _xEnvironment = mutableStateOf<XEnvironment?>(null)
    val xEnvironment: XEnvironment?
        get() = _xEnvironment.value

    private val _xServerView = mutableStateOf<XServerView?>(null)
    val xServerView: XServerView?
        get() = _xServerView.value

    private val _inputControlsView = mutableStateOf<InputControlsView?>(null)
    val inputControlsView: InputControlsView?
        get() = _inputControlsView.value

    private val _inputControlsManager = mutableStateOf<InputControlsManager?>(null)
    val inputControlsManager: InputControlsManager?
        get() = _inputControlsManager.value

    private val _touchpadView = mutableStateOf<TouchpadView?>(null)
    val touchpadView: TouchpadView?
        get() = _touchpadView.value

    private val _achievementWatcher = mutableStateOf<AchievementWatcher?>(null)
    val achievementWatcher: AchievementWatcher?
        get() = _achievementWatcher.value

    private val _isOverlayPaused = mutableStateOf(false)
    val isOverlayPaused: Boolean
        get() = _isOverlayPaused.value

    @Volatile
    private var _isActivityInForeground = true
    val isActivityInForeground: Boolean
        get() = _isActivityInForeground

    private var _activeSuspendPolicy = Container.SUSPEND_POLICY_MANUAL
    val activeSuspendPolicy: String
        get() = _activeSuspendPolicy

    private var hasInitializedSuspendPolicyState = false

    fun setXEnvironment(value: XEnvironment?) {
        _xEnvironment.value = value
    }

    fun setXServerView(value: XServerView?) {
        _xServerView.value = value
    }

    fun setInputControlsView(value: InputControlsView?) {
        _inputControlsView.value = value
    }

    fun setInputControlsManager(value: InputControlsManager?) {
        _inputControlsManager.value = value
    }

    fun setTouchpadView(value: TouchpadView?) {
        _touchpadView.value = value
    }

    fun setAchievementWatcher(value: AchievementWatcher?) {
        _achievementWatcher.value = value
    }

    fun setOverlayPaused(paused: Boolean) {
        _isOverlayPaused.value = paused
    }

    fun setActivityInForeground(inForeground: Boolean) {
        _isActivityInForeground = inForeground
    }

    fun setActiveSuspendPolicy(policy: String) {
        _activeSuspendPolicy = Container.normalizeSuspendPolicy(policy)
        hasInitializedSuspendPolicyState = true
    }

    fun clearActiveSuspendState() {
        _activeSuspendPolicy = Container.SUSPEND_POLICY_MANUAL
        _isOverlayPaused.value = false
        hasInitializedSuspendPolicyState = false
    }

    fun hasValidSuspendPolicyState(): Boolean = hasInitializedSuspendPolicyState

    fun isNeverSuspendMode(): Boolean = _activeSuspendPolicy.equals(
        Container.SUSPEND_POLICY_NEVER, ignoreCase = true
    )

    fun isManualSuspendMode(): Boolean = _activeSuspendPolicy.equals(
        Container.SUSPEND_POLICY_MANUAL, ignoreCase = true
    )

    fun clear() {
        _xEnvironment.value = null
        _xServerView.value = null
        _inputControlsView.value = null
        _inputControlsManager.value = null
        _touchpadView.value = null
        _achievementWatcher.value = null
        clearActiveSuspendState()
    }
}

object XServerRuntime {
    private var instance: XServerRuntimeState? = null

    fun get(): XServerRuntimeState {
        return instance ?: synchronized(this) {
            instance ?: XServerRuntimeState().also { instance = it }
        }
    }

    fun reset() {
        instance?.clear()
        instance = null
    }
}