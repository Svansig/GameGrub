package app.gamegrub.service.steam.managers

import android.content.Context
import app.gamegrub.GameGrubApp
import app.gamegrub.PrefManager
import app.gamegrub.data.SteamApp
import app.gamegrub.enums.LoginResult
import app.gamegrub.events.SteamEvent
import app.gamegrub.utils.container.ContainerUtils
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
import `in`.dragonbra.javasteam.steam.steamclient.SteamClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Manages Steam authentication flows including credentials and QR code login.
 *
 * Responsibilities:
 * - Username/password authentication
 * - QR code authentication
 * - Session management
 * - Login state tracking
 */
class SteamAuthService(
    private val getService: () -> SteamServiceAccess?,
    private val onLoginSuccess: (username: String, clientId: Long, accessToken: String, refreshToken: String) -> Unit,
    private val onLoginFailure: (username: String, LoginResult, String?) -> Unit,
) : IChallengeUrlChanged {

    private var _isWaitingForQRAuth = false
    val isWaitingForQRAuth: Boolean get() = _isWaitingForQRAuth

    private var _loginResult: LoginResult = LoginResult.Failed
    val loginResult: LoginResult get() = _loginResult

    val isLoggedIn: Boolean
        get() = getService()?.steamClient?.steamID?.isValid == true

    val isLoginInProgress: Boolean
        get() = _loginResult == LoginResult.InProgress

    /**
     * Login with username/password using Steam's authentication flow.
     */
    suspend fun startLoginWithCredentials(
        username: String,
        password: String,
        rememberSession: Boolean,
        authenticator: IAuthenticator,
    ) = withContext(Dispatchers.IO) {
        try {
            Timber.i("Logging in via credentials.")
            _loginResult = LoginResult.InProgress

            val service = getService()
            if (service == null) {
                handleLoginFailure(username, "Service not initialized")
                return@withContext
            }

            service.steamClient?.let { steamClient ->
                val machineName = SteamUtils.getMachineName(service.context)
                val authDetails = AuthSessionDetails().apply {
                    this.username = username.trim()
                    this.password = password
                    this.persistentSession = rememberSession
                    this.authenticator = authenticator
                    this.deviceFriendlyName = machineName
                    this.clientOSType = EOSType.WinUnknown
                }

                emitEvent(SteamEvent.LogonStarted(username))

                val authSession = steamClient.authentication
                    .beginAuthSessionViaCredentials(authDetails)
                    .await()

                val pollResult = authSession.pollingWaitForResult().await()

                if (pollResult.accountName.isEmpty() && pollResult.refreshToken.isEmpty()) {
                    throw Exception("No account name or refresh token received.")
                }

                onLoginSuccess(
                    pollResult.accountName,
                    authSession.clientID,
                    pollResult.accessToken,
                    pollResult.refreshToken,
                )
            } ?: run {
                handleLoginFailure(username, "No connection to Steam")
            }
        } catch (e: Exception) {
            Timber.e(e, "Login failed")
            val message = when (e) {
                is java.util.concurrent.CancellationException -> "Unknown cancellation"
                is AuthenticationException -> e.result?.name ?: e.message
                else -> e.message ?: e.javaClass.name
            }
            handleLoginFailure(username, message)
        }
    }

    /**
     * Login via QR code authentication.
     */
    suspend fun startLoginWithQr() = withContext(Dispatchers.IO) {
        try {
            Timber.i("Logging in via QR.")

            val service = getService()
            if (service == null) {
                emitEvent(SteamEvent.QrAuthEnded(success = false, message = "Service not initialized"))
                return@withContext
            }

            service.steamClient?.let { steamClient ->
                _isWaitingForQRAuth = true

                val machineName = SteamUtils.getMachineName(service.context)
                val authDetails = AuthSessionDetails().apply {
                    this.deviceFriendlyName = machineName
                    this.clientOSType = EOSType.WinUnknown
                    this.persistentSession = true
                }

                val authSession = steamClient.authentication
                    .beginAuthSessionViaQR(authDetails)
                    .await()

                authSession.challengeUrlChanged = this@SteamAuthService

                emitEvent(SteamEvent.QrChallengeReceived(authSession.challengeUrl))
                Timber.d("PollingInterval: ${authSession.pollingInterval.toLong()}")

                var authPollResult: AuthPollResult? = null

                while (_isWaitingForQRAuth && authPollResult == null) {
                    try {
                        authPollResult = authSession.pollAuthSessionStatus().await()
                    } catch (e: Exception) {
                        Timber.e(e, "Poll auth session status error")
                        throw e
                    }
                    delay(authSession.pollingInterval.toLong())
                }

                _isWaitingForQRAuth = false
                emitEvent(SteamEvent.QrAuthEnded(authPollResult != null))

                if (authPollResult == null) {
                    throw Exception("Got no auth poll result")
                }

                onLoginSuccess(
                    authPollResult.accountName,
                    authSession.clientID,
                    authPollResult.accessToken,
                    authPollResult.refreshToken,
                )
            } ?: run {
                emitEvent(SteamEvent.QrAuthEnded(success = false, message = "No connection to Steam"))
            }
        } catch (e: Exception) {
            Timber.e(e, "QR failed")
            val message = when (e) {
                is java.util.concurrent.CancellationException -> "QR Session timed out"
                is AuthenticationException -> e.result?.name ?: e.message
                else -> e.message ?: e.javaClass.name
            }
            emitEvent(SteamEvent.QrAuthEnded(success = false, message = message))
        }
    }

    /**
     * Stop QR code authentication polling.
     */
    fun stopLoginWithQr() {
        Timber.i("Stopping QR polling")
        _isWaitingForQRAuth = false
    }

    /**
     * Perform logout and clear session data.
     */
    fun logOut(clearCloudSyncState: Boolean = true) {
        CoroutineScope(Dispatchers.Default).launch {
            _loginResult = LoginResult.Failed
            performLogOffDuties(clearCloudSyncState)

            val service = getService()
            val steamUser = service?.steamUser
            steamUser?.logOff()
        }
    }

    /**
     * Clear user session preferences and database.
     */
    fun clearUserData(clearCloudSyncState: Boolean = false) {
        PrefManager.clearSteamSessionPreferences()
        clearDatabase(clearCloudSyncState)
    }

    /**
     * Check if user data should be cleared for specific login failure results.
     */
    fun shouldClearUserDataForLoggedOnFailure(result: EResult): Boolean = when (result) {
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

    /**
     * Get OAuth token from loginusers.vdf if available.
     */
    fun getLoginUsersVdfOauth(context: Context, appId: String): String? {
        return try {
            val imageFs = com.winlator.xenvironment.ImageFs.find(context)
            val loginUsersPath = "${imageFs.wineprefix}/drive_c/Program Files (x86)/Steam/config/loginusers.vdf"
            val loginUsersFile = java.io.File(loginUsersPath)
                if (loginUsersFile.exists()) {
                    val content = loginUsersFile.readText()
                    val vdf = `in`.dragonbra.javasteam.types.KeyValue.loadFromString(content) ?: return null
                    vdf["users"].children?.forEach { user ->
                    val steamId = user.name
                    if (steamId != null && steamId.isNotEmpty()) {
                        val token = user["RefreshToken"].value
                        if (token != null && token.isNotEmpty()) {
                            return token
                        }
                    }
                }
            }
            null
        } catch (e: Exception) {
            Timber.e(e, "Failed to read loginusers.vdf")
            null
        }
    }

    override fun onChanged(qrAuthSession: QrAuthSession?) {
        val url = qrAuthSession?.challengeUrl
        if (url != null) {
            emitEvent(SteamEvent.QrChallengeReceived(url))
        }
    }

    private fun handleLoginFailure(username: String, message: String?) {
        val event = SteamEvent.LogonEnded(username, LoginResult.Failed, message)
        emitEvent(event)
        onLoginFailure(username, LoginResult.Failed, message)
    }

    private fun emitEvent(event: SteamEvent) {
        GameGrubApp.events.emit(event)
    }

    private fun performLogOffDuties(clearCloudSyncState: Boolean = false) {
        // Will be implemented to handle cleanup
        Timber.d("Performing log off duties, clearCloudSync=$clearCloudSyncState")
    }

    private fun clearDatabase(clearCloudSyncState: Boolean = false) {
        // Will be implemented to clear database
        Timber.d("Clearing database, clearCloudSync=$clearCloudSyncState")
    }

    companion object {
        /**
         * Internal login method that performs the actual logOn call to Steam.
         */
        internal fun performLogin(
            steamUser: SteamUser,
            username: String,
            context: Context,
            accessToken: String? = null,
            refreshToken: String? = null,
            password: String? = null,
            rememberSession: Boolean = true,
            twoFactorAuth: String? = null,
            emailAuth: String? = null,
            clientId: Long? = null,
        ) {
            PrefManager.username = username

            if ((password != null && rememberSession) || refreshToken != null) {
                if (accessToken != null) PrefManager.accessToken = accessToken
                if (refreshToken != null) PrefManager.refreshToken = refreshToken
                if (clientId != null) PrefManager.clientId = clientId
            }

            steamUser.logOn(
                LogOnDetails(
                    username = SteamUtils.removeSpecialChars(username).trim(),
                    password = password?.let { SteamUtils.removeSpecialChars(it).trim() },
                    shouldRememberPassword = rememberSession,
                    twoFactorCode = twoFactorAuth,
                    authCode = emailAuth,
                    accessToken = refreshToken,
                    loginID = SteamUtils.getUniqueDeviceId(context),
                    machineName = SteamUtils.getMachineName(context),
                    chatMode = ChatMode.NEW_STEAM_CHAT,
                ),
            )
        }
    }
}

/**
 * Access interface for SteamService properties needed by managers.
 */
interface SteamServiceAccess {
    val context: Context
    val steamClient: SteamClient?
    val steamUser: SteamUser?
}
