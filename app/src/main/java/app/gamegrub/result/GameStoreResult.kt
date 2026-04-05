package app.gamegrub.result

import app.gamegrub.data.GameSource

sealed class GameStoreResult<out T> {
    data class Success<T>(val data: T) : GameStoreResult<T>()
    data class Error(val error: GameStoreError) : GameStoreResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    fun getOrNull(): T? = (this as? Success)?.data

    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw GameStoreException(error)
    }

    inline fun <R> map(transform: (T) -> R): GameStoreResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
    }

    inline fun onSuccess(action: (T) -> Unit): GameStoreResult<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onError(action: (GameStoreError) -> Unit): GameStoreResult<T> {
        if (this is Error) action(error)
        return this
    }
}

data class GameStoreError(
    val code: ErrorCode,
    val message: String,
    val source: GameSource? = null,
    val recoverable: Boolean = true,
    val cause: Throwable? = null,
)

enum class ErrorCode {
    NETWORK_ERROR,
    AUTH_ERROR,
    DOWNLOAD_ERROR,
    LAUNCH_ERROR,
    NOT_FOUND_ERROR,
    PERMISSION_ERROR,
    UNKNOWN_ERROR,
}

class GameStoreException(val error: GameStoreError) : Exception(error.message)

inline fun <T> runGameStoreCatching(block: () -> T): GameStoreResult<T> {
    return try {
        GameStoreResult.Success(block())
    } catch (e: GameStoreException) {
        GameStoreResult.Error(e.error)
    } catch (e: Exception) {
        GameStoreResult.Error(GameStoreError(ErrorCode.UNKNOWN_ERROR, e.message ?: "Unknown error", cause = e))
    }
}

fun <T> GameStoreResult<T>.getOrDefault(default: T): T = when (this) {
    is GameStoreResult.Success -> data
    is GameStoreResult.Error -> default
}

inline fun <T> GameStoreResult<T>.getOrElse(handler: (GameStoreError) -> T): T = when (this) {
    is GameStoreResult.Success -> data
    is GameStoreResult.Error -> handler(error)
}
