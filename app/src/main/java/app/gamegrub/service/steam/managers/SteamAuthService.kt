package app.gamegrub.service.steam.managers

import app.gamegrub.enums.LoginResult
import app.gamegrub.service.steam.di.AuthResult
import app.gamegrub.service.steam.di.ConnectionState
import app.gamegrub.service.steam.di.GameEventEmitter
import app.gamegrub.service.steam.di.SteamAuthClient
import app.gamegrub.service.steam.di.SteamConnection
import app.gamegrub.service.steam.di.SteamPreferences
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.steam.authentication.IAuthenticator
import `in`.dragonbra.javasteam.steam.authentication.IChallengeUrlChanged
import `in`.dragonbra.javasteam.steam.authentication.QrAuthSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Steam authentication flows.
 *
 * Dependencies are injected via constructor - no service locator pattern.
 * All dependencies are non-nullable for fail-fast behavior.
 */
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

    /**
     * Login with username and password.
     */
    suspend fun loginWithCredentials(
        username: String,
        password: String,
        rememberSession: Boolean,
        authenticator: IAuthenticator,
    ) {
        _loginResult.value = LoginResult.InProgress

        val result = authClient.loginWithCredentials(
            username = username,
            password = password,
            rememberSession = rememberSession,
            authenticator = authenticator,
        )

        handleAuthResult(result)
    }

    /**
     * Login via QR code.
     */
    suspend fun loginWithQr() {
        _isWaitingForQRAuth.value = true

        val result = authClient.loginWithQr(challengeUrlChanged = this)

        _isWaitingForQRAuth.value = false
        handleAuthResult(result)
    }

    /**
     * Cancel QR code login.
     */
    fun cancelQrLogin() {
        _isWaitingForQRAuth.value = false
        authClient.cancelQrLogin()
    }

    /**
     * Logout and clean up.
     */
    fun logout() {
        authClient.logOut()
        _loginResult.value = LoginResult.Failed
    }

    /**
     * Check if user data should be cleared for specific failure results.
     */
    fun shouldClearUserData(result: EResult): Boolean = when (result) {
        EResult.InvalidPassword,
        EResult.IllegalPassword,
        EResult.PasswordUnset,
        EResult.AccountLogonDenied,
        EResult.AccountLogonDeniedNoMail,
        EResult.AccountLogonDeniedVerifiedEmailRequired,
        EResult.AccountLoginDeniedNeedTwoFactor,
        EResult.InvalidLoginAuthCode,
        EResult.ExpiredLoginAuthCode,
        EResult.RequirePasswordReEntry,
        EResult.ParentalControlRestricted,
        EResult.CachedCredentialInvalid,
        -> true
        else -> false
    }

    override fun onChanged(qrAuthSession: QrAuthSession?) {
        val url = qrAuthSession?.challengeUrl
        Timber.d("QR challenge URL changed: $url")
    }

    private fun handleAuthResult(result: AuthResult) {
        if (result.success) {
            _loginResult.value = LoginResult.Success
            preferences.username = result.username
            preferences.clientId = result.clientId
            preferences.accessToken = result.accessToken
            preferences.refreshToken = result.refreshToken
            Timber.i("Login successful for ${result.username}")
        } else {
            _loginResult.value = LoginResult.Failed
            Timber.e("Login failed: ${result.error}")
        }
    }
}
