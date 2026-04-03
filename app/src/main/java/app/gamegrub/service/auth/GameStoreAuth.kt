package app.gamegrub.service.auth

import app.gamegrub.data.GameSource

interface GameStoreAuth {
    val gameSource: GameSource

    suspend fun login(context: android.content.Context): Result<Unit>

    suspend fun logout(context: android.content.Context): Result<Unit>

    suspend fun refreshToken(): Result<Unit>

    fun isLoggedIn(): Boolean

    fun getCredentials(): Any?

    fun getLoginUrl(): String

    suspend fun handleCallback(callbackUrl: String): Result<Unit>
}
