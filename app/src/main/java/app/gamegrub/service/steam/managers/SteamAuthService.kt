package app.gamegrub.service.steam.managers

import app.gamegrub.GameGrubApp
import app.gamegrub.PrefManager
import app.gamegrub.enums.LoginResult
import app.gamegrub.events.SteamEvent
import app.gamegrub.utils.steam.SteamUtils
import `in`.dragonbra.javasteam.enums.EOSType
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.steam.authentication.AuthPollResult
import `in`.dragonbra.javasteam.steam.authentication.AuthSessionDetails
import `in`.dragonbra.javasteam.steam.authentication.AuthenticationException
import `in`.dragonbra.javasteam.steam.authentication.IAuthenticator
import `in`.dragonbra.javasteam.steam.authentication.IChallengeUrlChanged
import `in`.dragonbra.javasteam.steam.authentication.QrAuthSession
import `in`.dragonbra.javasteam.steam.handlers.steamuser.ChatMode
import `in`.dragonbra.javasteam.steam.handlers.steamuser.LogOnDetails
import `in`.dragonbra.javasteam.steam.handlers.steamuser.SteamUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SteamAuthService @Inject constructor() : IChallengeUrlChanged {

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _loginResult = MutableStateFlow(LoginResult.Failed)
    val loginResult: StateFlow<LoginResult> = _loginResult.asStateFlow()

    private val _isWaitingForQRAuth = MutableStateFlow(false)
    val isWaitingForQRAuth: StateFlow<Boolean> = _isWaitingForQRAuth.asStateFlow()

    val isLoggedIn: Boolean
        get() = SteamService.instance?.steamClient?.steamID?.isValid == true

    val isLoginInProgress: Boolean
        get() = _loginResult.value == LoginResult.InProgress

    suspend fun loginWithCredentials(
        username: String,
        password: String,
        rememberSession: Boolean,
        authenticator: IAuthenticator,
    ) {
        val service = SteamService.instance ?: run {
            Timber.e("Cannot login - service not running")
            return
        }

        try {
            Timber.i("Logging in via credentials.")
            _loginResult.value = LoginResult.InProgress

            val steamClient = service.steamClient ?: run {
                emitLoginFailure(username, "No connection to Steam")
                return
            }

            val authDetails = AuthSessionDetails().apply {
                this.username = username.trim()
                this.password = password
                this.persistentSession = rememberSession
                this.authenticator = authenticator
                this.deviceFriendlyName = SteamUtils.getMachineName(service)
                this.clientOSType = EOSType.WinUnknown
            }

            GameGrubApp.events.emit(SteamEvent.LogonStarted(username))

            val authSession = steamClient.authentication
                .beginAuthSessionViaCredentials(authDetails)
                .await()

            val pollResult = authSession.pollingWaitForResult().await()

            if (pollResult.accountName.isEmpty() && pollResult.refreshToken.isEmpty()) {
                throw Exception("No account name or refresh token received.")
            }

            performLogin(
                steamUser = service._steamUser!!,
                context = service,
                username = pollResult.accountName,
                accessToken = pollResult.accessToken,
                refreshToken = pollResult.refreshToken,
                clientId = authSession.clientID,
                rememberSession = rememberSession,
            )

            _loginResult.value = LoginResult.Success
        } catch (e: Exception) {
            Timber.e(e, "Login failed")
            val message = when (e) {
                is java.util.concurrent.CancellationException -> "Unknown cancellation"
                is AuthenticationException -> e.result?.name ?: e.message
                else -> e.message ?: e.javaClass.name
            }
            emitLoginFailure(username, message)
        }
    }

    suspend fun loginWithQr() {
        val service = SteamService.instance ?: run {
            Timber.e("Cannot start QR login - service not running")
            return
        }

        try {
            Timber.i("Logging in via QR.")
            val steamClient = service.steamClient ?: run {
                emitQrFailure("No connection to Steam")
                return
            }

            _isWaitingForQRAuth.value = true

            val authDetails = AuthSessionDetails().apply {
                this.deviceFriendlyName = SteamUtils.getMachineName(service)
                this.clientOSType = EOSType.WinUnknown
                this.persistentSession = true
            }

            val authSession = steamClient.authentication
                .beginAuthSessionViaQR(authDetails)
                .await()

            authSession.challengeUrlChanged = this

            GameGrubApp.events.emit(SteamEvent.QrChallengeReceived(authSession.challengeUrl))

            var authPollResult: AuthPollResult? = null
            while (_isWaitingForQRAuth.value && authPollResult == null) {
                try {
                    authPollResult = authSession.pollAuthSessionStatus().await()
                } catch (e: Exception) {
                    Timber.e(e, "Poll auth session status error")
                    throw e
                }
                delay(authSession.pollingInterval.toLong())
            }

            _isWaitingForQRAuth.value = false
            GameGrubApp.events.emit(SteamEvent.QrAuthEnded(authPollResult != null))

            if (authPollResult == null) {
                throw Exception("Got no auth poll result")
            }

            performLogin(
                steamUser = service._steamUser!!,
                context = service,
                username = authPollResult.accountName,
                accessToken = authPollResult.accessToken,
                refreshToken = authPollResult.refreshToken,
                clientId = authSession.clientID,
            )

            _loginResult.value = LoginResult.Success
        } catch (e: Exception) {
            Timber.e(e, "QR login failed")
            _isWaitingForQRAuth.value = false
            val message = when (e) {
                is java.util.concurrent.CancellationException -> "QR Session timed out"
                is AuthenticationException -> e.result?.name ?: e.message
                else -> e.message ?: e.javaClass.name
            }
            emitQrFailure(message)
        }
    }

    fun cancelQrLogin() {
        Timber.i("Cancelling QR login")
        _isWaitingForQRAuth.value = false
    }

    fun logOut() {
        scope.launch {
            _loginResult.value = LoginResult.Failed
            val service = SteamService.instance ?: return@launch
            service.performLogOffDuties(clearCloudSyncState = true)
            service._steamUser?.logOff()
        }
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
            GameGrubApp.events.emit(SteamEvent.QrChallengeReceived(url))
        }
    }

    private fun performLogin(
        steamUser: SteamUser,
        context: android.content.Context,
        username: String,
        accessToken: String? = null,
        refreshToken: String? = null,
        password: String? = null,
        rememberSession: Boolean = true,
        clientId: Long? = null,
    ) {
        PrefManager.username = username
        if (refreshToken != null || (password != null && rememberSession)) {
            if (accessToken != null) PrefManager.accessToken = accessToken
            if (refreshToken != null) PrefManager.refreshToken = refreshToken
            if (clientId != null) PrefManager.clientId = clientId
        }

        steamUser.logOn(
            LogOnDetails(
                username = SteamUtils.removeSpecialChars(username).trim(),
                password = password?.let { SteamUtils.removeSpecialChars(it).trim() },
                shouldRememberPassword = rememberSession,
                accessToken = refreshToken,
                loginID = SteamUtils.getUniqueDeviceId(context),
                machineName = SteamUtils.getMachineName(context),
                chatMode = ChatMode.NEW_STEAM_CHAT,
            ),
        )
    }

    private fun emitLoginFailure(username: String, message: String?) {
        GameGrubApp.events.emit(SteamEvent.LogonEnded(username, LoginResult.Failed, message))
    }

    private fun emitQrFailure(message: String?) {
        GameGrubApp.events.emit(SteamEvent.QrAuthEnded(success = false, message = message))
    }
}
