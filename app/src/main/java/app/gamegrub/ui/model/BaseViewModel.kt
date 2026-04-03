package app.gamegrub.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.gamegrub.state.UiEffect
import app.gamegrub.state.UiEffectHandler
import app.gamegrub.state.UiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

abstract class BaseViewModel<S, E>(initialState: S) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    protected val currentState: S
        get() = _state.value

    private val _effects = Channel<E>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    protected fun setState(newState: S) {
        _state.value = newState
    }

    protected fun updateState(reducer: S.() -> S) {
        _state.value = currentState.reducer()
    }

    protected fun sendEffect(effect: E) {
        viewModelScope.launch {
            _effects.send(effect)
        }
    }
}

abstract class BaseScreenViewModel : BaseViewModel<ScreenState, ScreenEffect>(ScreenState.Loading) {

    protected fun setLoading() {
        setState(ScreenState.Loading)
    }

    protected fun <T> setSuccess(data: T) {
        setState(ScreenState.Success(data))
    }

    protected fun setError(message: String, throwable: Throwable? = null) {
        setState(ScreenState.Error(message, throwable))
    }

    protected fun setEmpty() {
        setState(ScreenState.Empty)
    }
}

sealed class ScreenState {
    data object Loading : ScreenState()
    data object Empty : ScreenState()
    data class Success<T>(val data: T) : ScreenState()
    data class Error(val message: String, val throwable: Throwable? = null) : ScreenState()
}

sealed class ScreenEffect {
    data class ShowSnackbar(val message: String) : ScreenEffect()
    data class Navigate(val route: String) : ScreenEffect()
    data class LaunchBrowser(val url: String) : ScreenEffect()
    data class Share(val text: String) : ScreenEffect()
    data class Error(val message: String) : ScreenEffect()
}
