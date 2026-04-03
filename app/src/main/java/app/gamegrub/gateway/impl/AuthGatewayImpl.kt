package app.gamegrub.gateway.impl

import app.gamegrub.data.GameSource
import app.gamegrub.gateway.AuthGateway
import app.gamegrub.gateway.AuthState
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthGatewayImpl @Inject constructor() : AuthGateway {

    private val loggedInStores = mutableSetOf<GameSource>()

    override suspend fun login(source: GameSource): Result<Unit> {
        loggedInStores.add(source)
        return Result.success(Unit)
    }

    override suspend fun logout(source: GameSource): Result<Unit> {
        loggedInStores.remove(source)
        return Result.success(Unit)
    }

    override fun isLoggedIn(source: GameSource): Boolean {
        return source in loggedInStores
    }

    override fun getAuthState(source: GameSource): AuthState {
        return if (source in loggedInStores) {
            AuthState.LoggedIn(source, "User")
        } else {
            AuthState.NotLoggedIn
        }
    }

    override fun getLoginUrl(source: GameSource): String {
        return when (source) {
            GameSource.STEAM -> "https://steamcommunity.com/login/"
            GameSource.GOG -> "https://auth.gog.com/"
            GameSource.EPIC -> "https://www.epicgames.com/id/login"
            GameSource.AMAZON -> "https://www.amazon.com/ap/signin"
            GameSource.CUSTOM_GAME -> ""
        }
    }

    override suspend fun handleAuthCallback(source: GameSource, callbackUrl: String): Result<Unit> {
        loggedInStores.add(source)
        return Result.success(Unit)
    }

    override fun getLoggedInStores(): Set<GameSource> {
        return loggedInStores.toSet()
    }
}
