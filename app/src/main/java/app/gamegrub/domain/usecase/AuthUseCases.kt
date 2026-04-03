package app.gamegrub.domain.usecase

import app.gamegrub.data.GameSource
import app.gamegrub.gateway.AuthGateway
import app.gamegrub.gateway.AuthState
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authGateway: AuthGateway,
) {
    suspend operator fun invoke(source: GameSource): Result<Unit> {
        return authGateway.login(source)
    }
}

class LogoutUseCase @Inject constructor(
    private val authGateway: AuthGateway,
) {
    suspend operator fun invoke(source: GameSource): Result<Unit> {
        return authGateway.logout(source)
    }
}

class GetAuthStateUseCase @Inject constructor(
    private val authGateway: AuthGateway,
) {
    operator fun invoke(source: GameSource): AuthState {
        return authGateway.getAuthState(source)
    }
}

class IsLoggedInUseCase @Inject constructor(
    private val authGateway: AuthGateway,
) {
    operator fun invoke(source: GameSource): Boolean {
        return authGateway.isLoggedIn(source)
    }
}

class GetLoginUrlUseCase @Inject constructor(
    private val authGateway: AuthGateway,
) {
    operator fun invoke(source: GameSource): String {
        return authGateway.getLoginUrl(source)
    }
}

class HandleAuthCallbackUseCase @Inject constructor(
    private val authGateway: AuthGateway,
) {
    suspend operator fun invoke(source: GameSource, callbackUrl: String): Result<Unit> {
        return authGateway.handleAuthCallback(source, callbackUrl)
    }
}

class GetLoggedInStoresUseCase @Inject constructor(
    private val authGateway: AuthGateway,
) {
    operator fun invoke(): Set<GameSource> {
        return authGateway.getLoggedInStores()
    }
}
