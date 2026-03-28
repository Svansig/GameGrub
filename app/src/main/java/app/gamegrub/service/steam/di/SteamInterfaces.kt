package app.gamegrub.service.steam.di

import `in`.dragonbra.javasteam.steam.authentication.IAuthenticator
import `in`.dragonbra.javasteam.steam.authentication.IChallengeUrlChanged
import `in`.dragonbra.javasteam.steam.handlers.steamapps.SteamApps
import `in`.dragonbra.javasteam.steam.handlers.steamcloud.SteamCloud
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.SteamFriends
import `in`.dragonbra.javasteam.steam.handlers.steamuser.SteamUser
import `in`.dragonbra.javasteam.steam.handlers.steamuserstats.SteamUserStats
import `in`.dragonbra.javasteam.steam.steamclient.SteamClient
import `in`.dragonbra.javasteam.types.SteamID
import kotlinx.coroutines.flow.StateFlow

/**
 * Abstraction for Steam client connection state.
 * Allows testing without real Steam connection.
 */
interface SteamConnection {
    val isConnected: Boolean
    val isLoggedIn: Boolean
    val steamId: SteamID?
    val connectionState: StateFlow<ConnectionState>

    fun connect()
    fun disconnect()
}

enum class ConnectionState {
    Disconnected,
    Connecting,
    Connected,
    LoggingIn,
    LoggedIn,
    Error
}

/**
 * Abstraction for Steam authentication operations.
 * Allows testing without real Steam authentication.
 */
interface SteamAuthClient {
    suspend fun loginWithCredentials(
        username: String,
        password: String,
        rememberSession: Boolean,
        authenticator: IAuthenticator,
    ): AuthResult

    suspend fun loginWithQr(
        challengeUrlChanged: IChallengeUrlChanged,
    ): AuthResult

    fun cancelQrLogin()
    fun logOut()
}

data class AuthResult(
    val success: Boolean,
    val username: String = "",
    val clientId: Long = 0,
    val accessToken: String = "",
    val refreshToken: String = "",
    val error: String? = null,
)

/**
 * Abstraction for Steam user operations.
 */
interface SteamUserClient {
    val userSteamId: SteamID?
    val personaState: StateFlow<SteamPersona>

    suspend fun setPersonaState(state: Int)
    suspend fun requestPersonaInfo(steamId: SteamID)
    suspend fun kickPlayingSession(onlyGame: Boolean = true): Boolean
}

data class SteamPersona(
    val name: String = "",
    val avatarHash: String = "",
    val isPlayingGame: Boolean = false,
    val gameAppID: Int = 0,
    val gameName: String? = null,
)

/**
 * Abstraction for Steam library operations.
 */
interface SteamLibraryClient {
    suspend fun getOwnedGames(steamId: SteamID): List<OwnedGame>
    suspend fun checkDlcOwnership(appIds: Set<Int>): Set<Int>
}

data class OwnedGame(
    val appId: Int,
    val name: String,
    val playtimeMinutes: Int,
    val iconUrl: String,
    val logoUrl: String,
)

/**
 * Abstraction for Steam cloud operations.
 */
interface SteamCloudClient {
    suspend fun signalAppExitSyncDone(
        appId: Int,
        clientId: String,
        uploadsCompleted: Boolean,
    )
}

/**
 * Abstraction for event emission.
 * Decouples from GameGrubApp singleton.
 */
interface GameEventEmitter {
    fun emitSteamEvent(event: Any)
    fun emitAndroidEvent(event: Any)
}

/**
 * Abstraction for persistent preferences.
 */
interface SteamPreferences {
    var username: String
    var accessToken: String
    var refreshToken: String
    var clientId: Long?
    var personaState: Int
    var steamUserAvatarHash: String
    var steamUserAccountId: Int
}
