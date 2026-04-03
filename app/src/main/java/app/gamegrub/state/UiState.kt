package app.gamegrub.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

abstract class StateHolder<S>(initialState: S) {
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    protected val currentState: S
        get() = _state.value

    protected fun updateState(reducer: S.() -> S) {
        _state.value = currentState.reducer()
    }

    protected fun setState(newState: S) {
        _state.value = newState
    }
}

sealed class UiState<out T> {
    data object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : UiState<Nothing>()
    data object Empty : UiState<Nothing>()

    val isLoading: Boolean get() = this is Loading
    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isEmpty: Boolean get() = this is Empty

    fun getOrNull(): T? = (this as? Success)?.data

    inline fun <R> map(transform: (T) -> R): UiState<R> = when (this) {
        is Success -> Success(transform(data))
        is Loading -> Loading
        is Error -> Error(message, throwable)
        is Empty -> Empty
    }
}

sealed class UiEffect {
    data class ShowSnackbar(val message: String) : UiEffect()
    data class Navigate(val route: String) : UiEffect()
    data class LaunchBrowser(val url: String) : UiEffect()
    data class Share(val text: String) : UiEffect()
    data class Error(val message: String, val throwable: Throwable? = null) : UiEffect()
}

interface UiEffectHandler {
    fun handleEffect(effect: UiEffect)
}
