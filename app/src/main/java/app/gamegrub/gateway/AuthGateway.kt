package app.gamegrub.gateway

import app.gamegrub.data.GameSource

sealed class AuthState {
    data object NotLoggedIn : AuthState()
    data object Loading : AuthState()
    data class LoggedIn(val source: GameSource, val username: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

interface AuthGateway {
    suspend fun login(source: GameSource): Result<Unit>

    suspend fun logout(source: GameSource): Result<Unit>

    fun isLoggedIn(source: GameSource): Boolean

    fun getAuthState(source: GameSource): AuthState

    fun getLoginUrl(source: GameSource): String

    suspend fun handleAuthCallback(source: GameSource, callbackUrl: String): Result<Unit>

    fun getLoggedInStores(): Set<GameSource>
}
