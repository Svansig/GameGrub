package app.gamegrub.service.steam.di

import `in`.dragonbra.javasteam.steam.authentication.IAuthenticator
import `in`.dragonbra.javasteam.steam.authentication.IChallengeUrlChanged
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
    Error,
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

    suspend fun syncUserFiles(
        appId: Int,
        clientId: String,
        preferredSave: String = "None",
    ): CloudSyncResult
}

data class CloudSyncResult(
    val success: Boolean,
    val uploadsCompleted: Boolean = false,
    val downloadsCompleted: Boolean = false,
    val error: String? = null,
)

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

/**
 * Abstraction for Steam achievement/stats operations.
 */
interface SteamStatsClient {
    suspend fun getUserStats(appId: Int, steamId: SteamID): UserStatsResult
    suspend fun storeStats(appId: Int, stats: Map<Int, Int>, achievements: Map<Int, Boolean>): Boolean
    fun getSchema(appId: Int): ByteArray?
}

data class UserStatsResult(
    val success: Boolean,
    val schema: ByteArray,
    val achievementBlocks: List<AchievementBlock> = emptyList(),
)

data class AchievementBlock(
    val achievementId: Int,
    val unlockTimes: List<Int>,
)

/**
 * Abstraction for PICS (Product Info) operations.
 */
interface SteamPicsClient {
    suspend fun getChangesSince(changeNumber: Long): PicsChanges
    suspend fun getProductInfo(appIds: Set<Int>, packageIds: Set<Int>): PicsProductInfo
}

data class PicsChanges(
    val currentChangeNumber: Long,
    val appChanges: Set<Int>,
    val packageChanges: Set<Int>,
    val needsFullUpdate: Boolean,
)

data class PicsProductInfo(
    val apps: Map<Int, PicsAppInfo>,
    val packages: Map<Int, PicsPackageInfo>,
)

data class PicsAppInfo(
    val changeNumber: Long,
    val common: Map<String, String>,
)

data class PicsPackageInfo(
    val changeNumber: Long,
    val common: Map<String, String>,
    val appIds: Set<Int>,
    val depotIds: Set<Int>,
)

/**
 * Abstraction for app info access.
 */
interface SteamAppInfoClient {
    fun getAppInfo(appId: Int): AppInfoData?
    fun findApp(appId: Int): AppGameData?
}

data class AppInfoData(
    val appId: Int,
    val installPath: String,
)

data class AppGameData(
    val appId: Int,
    val name: String,
    val packageName: String?,
)

/**
 * Abstraction for encrypted app ticket operations.
 */
interface SteamTicketClient {
    /** Get cached ticket if fresh (within 30 min), null if stale or missing. */
    suspend fun getEncryptedAppTicket(appId: Int): ByteArray?

    /** Fetch ticket from Steam API, cache it, and return it. Returns null on failure. */
    suspend fun fetchAndCacheEncryptedAppTicket(appId: Int): ByteArray?

    suspend fun storeEncryptedAppTicket(appId: Int, ticket: ByteArray)
    fun clearAllTickets()
}
