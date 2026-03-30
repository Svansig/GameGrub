package app.gamegrub.service.steam.managers

import app.gamegrub.enums.LoginResult
import app.gamegrub.service.steam.SteamService
import app.gamegrub.service.steam.di.AuthResult
import app.gamegrub.service.steam.di.GameEventEmitter
import app.gamegrub.service.steam.di.SteamAuthClient
import app.gamegrub.service.steam.di.SteamConnection
import app.gamegrub.service.steam.di.SteamPreferences
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.steam.authentication.IAuthenticator
import `in`.dragonbra.javasteam.steam.authentication.IChallengeUrlChanged
import `in`.dragonbra.javasteam.steam.authentication.QrAuthSession
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

@Singleton
class SteamAuthService @Inject constructor(
    private val authClient: SteamAuthClient,
    private val connection: SteamConnection,
    private val preferences: SteamPreferences,
    private val eventEmitter: GameEventEmitter,
) : IChallengeUrlChanged {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _loginResult = MutableStateFlow(LoginResult.Failed)
    val loginResult: StateFlow<LoginResult> = _loginResult.asStateFlow()

    private val _isWaitingForQRAuth = MutableStateFlow(false)
    val isWaitingForQRAuth: StateFlow<Boolean> = _isWaitingForQRAuth.asStateFlow()

    val isLoggedIn: Boolean get() = connection.isLoggedIn
    val isLoginInProgress: Boolean get() = _loginResult.value == LoginResult.InProgress

    suspend fun loginWithCredentials(
        username: String,
        password: String,
        rememberSession: Boolean,
        authenticator: IAuthenticator,
    ) {
        Timber.i("Logging in via credentials: $username")
        _loginResult.value = LoginResult.InProgress

        val result = authClient.loginWithCredentials(
            username = username,
            password = password,
            rememberSession = rememberSession,
            authenticator = authenticator,
        )

        handleAuthResult(result)
    }

    suspend fun loginWithQr() {
        Timber.i("Logging in via QR")
        _isWaitingForQRAuth.value = true

        val result = authClient.loginWithQr(challengeUrlChanged = this)

        _isWaitingForQRAuth.value = false
        handleAuthResult(result)
    }

    fun cancelQrLogin() {
        Timber.i("Cancelling QR login")
        _isWaitingForQRAuth.value = false
        authClient.cancelQrLogin()
    }

    fun logOut() {
        Timber.i("Logging out")
        _loginResult.value = LoginResult.Failed
        authClient.logOut()
    }

    fun shouldClearUserData(result: EResult): Boolean = when (result) {
        EResult.InvalidPassword, EResult.IllegalPassword, EResult.PasswordUnset,
        EResult.AccountLogonDenied, EResult.AccountLogonDeniedNoMail,
        EResult.AccountLogonDeniedVerifiedEmailRequired, EResult.AccountLoginDeniedNeedTwoFactor,
        EResult.InvalidLoginAuthCode, EResult.ExpiredLoginAuthCode,
        EResult.RequirePasswordReEntry, EResult.ParentalControlRestricted,
        EResult.CachedCredentialInvalid,
        -> true

        else -> false
    }

    override fun onChanged(qrAuthSession: QrAuthSession?) {
        val url = qrAuthSession?.challengeUrl
        if (url != null) {
            eventEmitter.emitSteamEvent(app.gamegrub.events.SteamEvent.QrChallengeReceived(url))
        }
    }

    private fun handleAuthResult(result: AuthResult) {
        if (result.success) {
            _loginResult.value = LoginResult.InProgress
            preferences.username = result.username
            preferences.clientId = result.clientId
            preferences.accessToken = result.accessToken
            preferences.refreshToken = result.refreshToken
            // Complete the flow with a real Steam logon so callbacks (licenses/PICS/library) can run.
            SteamService.completeLoginWithAuthTokens(
                username = result.username,
                accessToken = result.accessToken,
                refreshToken = result.refreshToken,
                clientId = result.clientId,
            )
            Timber.i("Auth session succeeded, waiting for LoggedOn callback: ${result.username}")
        } else {
            _loginResult.value = LoginResult.Failed
            eventEmitter.emitSteamEvent(app.gamegrub.events.SteamEvent.LogonEnded("", LoginResult.Failed, result.error))
            Timber.e("Login failed: ${result.error}")
        }
    }
}
