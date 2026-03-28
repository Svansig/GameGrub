package app.gamegrub.service.steam.di

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
import `in`.dragonbra.javasteam.steam.handlers.steamuser.ChatMode
import `in`.dragonbra.javasteam.steam.handlers.steamuser.LogOnDetails
import `in`.dragonbra.javasteam.steam.handlers.steamuser.SteamUser
import `in`.dragonbra.javasteam.steam.steamclient.SteamClient
import `in`.dragonbra.javasteam.types.SteamID
import app.gamegrub.GameGrubApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Adapter that wraps SteamClient JavaSteam library into our clean interface.
 */
@Singleton
class SteamConnectionAdapter @Inject constructor(
    private val clientProvider: SteamClientProvider,
) : SteamConnection {

    private val _connectionState = MutableStateFlow(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    override val isConnected: Boolean
        get() = clientProvider.client?.isConnected == true

    override val isLoggedIn: Boolean
        get() = clientProvider.client?.steamID?.isValid == true

    override val steamId: SteamID?
        get() = clientProvider.client?.steamID

    override fun connect() {
        clientProvider.client?.connect()
        _connectionState.value = ConnectionState.Connecting
    }

    override fun disconnect() {
        clientProvider.client?.disconnect()
        _connectionState.value = ConnectionState.Disconnected
    }

    fun setState(state: ConnectionState) {
        _connectionState.value = state
    }
}

/**
 * Provides access to the SteamClient instance.
 * This is set by SteamService when it initializes.
 */
@Singleton
class SteamClientProvider @Inject constructor() {
    var client: SteamClient? = null
    var steamUser: SteamUser? = null
    var steamCloud: `in`.dragonbra.javasteam.steam.handlers.steamcloud.SteamCloud? = null
    var steamUserStats: `in`.dragonbra.javasteam.steam.handlers.steamuserstats.SteamUserStats? = null
}

/**
 * Adapter for Steam authentication operations.
 */
@Singleton
class SteamAuthClientAdapter @Inject constructor(
    private val clientProvider: SteamClientProvider,
) : SteamAuthClient {

    override suspend fun loginWithCredentials(
        username: String,
        password: String,
        rememberSession: Boolean,
        authenticator: IAuthenticator,
    ): AuthResult = withContext(Dispatchers.IO) {
        try {
            val client = clientProvider.client ?: return@withContext AuthResult(false, error = "Not connected")
            val service = app.gamegrub.service.steam.SteamService.instance
                ?: return@withContext AuthResult(false, error = "Service not running")

            val authDetails = AuthSessionDetails().apply {
                this.username = username.trim()
                this.password = password
                this.persistentSession = rememberSession
                this.authenticator = authenticator
                this.deviceFriendlyName = SteamUtils.getMachineName(service)
                this.clientOSType = EOSType.WinUnknown
            }

            val authSession = client.authentication
                .beginAuthSessionViaCredentials(authDetails)
                .await()

            val pollResult = authSession.pollingWaitForResult().await()

            if (pollResult.accountName.isEmpty() && pollResult.refreshToken.isEmpty()) {
                return@withContext AuthResult(false, error = "No credentials received")
            }

            AuthResult(
                success = true,
                username = pollResult.accountName,
                clientId = authSession.clientID,
                accessToken = pollResult.accessToken,
                refreshToken = pollResult.refreshToken,
            )
        } catch (e: Exception) {
            Timber.e(e, "Credential login failed")
            AuthResult(false, error = e.message)
        }
    }

    override suspend fun loginWithQr(
        challengeUrlChanged: IChallengeUrlChanged,
    ): AuthResult = withContext(Dispatchers.IO) {
        try {
            val client = clientProvider.client ?: return@withContext AuthResult(false, error = "Not connected")
            val service = app.gamegrub.service.steam.SteamService.instance
                ?: return@withContext AuthResult(false, error = "Service not running")

            val authDetails = AuthSessionDetails().apply {
                this.deviceFriendlyName = SteamUtils.getMachineName(service)
                this.clientOSType = EOSType.WinUnknown
                this.persistentSession = true
            }

            val authSession = client.authentication
                .beginAuthSessionViaQR(authDetails)
                .await()

            authSession.challengeUrlChanged = challengeUrlChanged

            var authPollResult: AuthPollResult? = null
            var waiting = true

            while (waiting && authPollResult == null) {
                try {
                    authPollResult = authSession.pollAuthSessionStatus().await()
                } catch (e: Exception) {
                    if (e is java.util.concurrent.CancellationException) {
                        waiting = false
                    } else {
                        throw e
                    }
                }
                if (waiting) delay(authSession.pollingInterval.toLong())
            }

            if (authPollResult == null) {
                return@withContext AuthResult(false, error = "QR cancelled or timed out")
            }

            AuthResult(
                success = true,
                username = authPollResult.accountName,
                clientId = authSession.clientID,
                accessToken = authPollResult.accessToken,
                refreshToken = authPollResult.refreshToken,
            )
        } catch (e: Exception) {
            Timber.e(e, "QR login failed")
            AuthResult(false, error = e.message)
        }
    }

    override fun cancelQrLogin() {
        Timber.d("QR login cancelled")
    }

    override fun logOut() {
        clientProvider.steamUser?.logOff()
    }
}

/**
 * Adapter for Steam user operations.
 */
@Singleton
class SteamUserClientAdapter @Inject constructor(
    private val clientProvider: SteamClientProvider,
) : SteamUserClient {

    private val _personaState = MutableStateFlow(SteamPersona())
    override val personaState: StateFlow<SteamPersona> = _personaState.asStateFlow()

    override val userSteamId: SteamID?
        get() = clientProvider.client?.steamID

    override suspend fun setPersonaState(state: Int) = withContext(Dispatchers.IO) {
        clientProvider.steamUser?.let { user ->
            user.setPersonaState(`in`.dragonbra.javasteam.enums.EPersonaState.fromValue(state))
        }
    }

    override suspend fun requestPersonaInfo(steamId: SteamID) = withContext(Dispatchers.IO) {
        val service = app.gamegrub.service.steam.SteamService.instance
        service?._steamFriends?.requestFriendInfo(steamId)
    }

    override suspend fun kickPlayingSession(onlyGame: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            clientProvider.steamUser?.kickPlayingSession(onlyStopGame = onlyGame)
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to kick playing session")
            false
        }
    }

    fun updatePersona(persona: SteamPersona) {
        _personaState.value = persona
    }
}

/**
 * Adapter for Steam library operations.
 */
@Singleton
class SteamLibraryClientAdapter @Inject constructor() : SteamLibraryClient {

    override suspend fun getOwnedGames(steamId: SteamID): List<OwnedGame> {
        val service = app.gamegrub.service.steam.SteamService.instance ?: return emptyList()
        val steamFriends = service._steamFriends ?: return emptyList()

        return try {
            val result = steamFriends.getOwnedGames(steamId.convertToLong()).await()
            result?.games?.map { game ->
                OwnedGame(
                    appId = game.appID,
                    name = game.name ?: "Unknown",
                    playtimeMinutes = game.playtimeForever,
                    iconUrl = game.imgIconUrl ?: "",
                    logoUrl = game.imgLogoUrl ?: "",
                )
            } ?: emptyList()
        } catch (e: Exception) {
            Timber.e(e, "Failed to get owned games")
            emptyList()
        }
    }

    override suspend fun checkDlcOwnership(appIds: Set<Int>): Set<Int> {
        val service = app.gamegrub.service.steam.SteamService.instance ?: return emptySet()
        val steamApps = service._steamApps ?: return emptySet()
        if (appIds.isEmpty()) return emptySet()

        return try {
            val requests = appIds.map { `in`.dragonbra.javasteam.steam.handlers.steamapps.PICSRequest(appId = it) }
            val result = steamApps.getPICSProductInfo(requests, emptyList()).await()
            result?.apps?.keys ?: emptySet()
        } catch (e: Exception) {
            Timber.e(e, "Failed to check DLC ownership")
            emptySet()
        }
    }
}

@Singleton
class SteamCloudClientAdapter @Inject constructor(
    private val clientProvider: SteamClientProvider,
) : SteamCloudClient {

    override suspend fun signalAppExitSyncDone(
        appId: Int,
        clientId: String,
        uploadsCompleted: Boolean,
    ) {
        clientProvider.steamCloud?.signalAppExitSyncDone(
            appId = appId,
            clientId = clientId,
            uploadsCompleted = uploadsCompleted,
        )
    }

    override suspend fun syncUserFiles(
        appId: Int,
        clientId: String,
        preferredSave: String,
    ): CloudSyncResult {
        return try {
            val service = app.gamegrub.service.steam.SteamService.instance
                ?: return CloudSyncResult(false, error = "Service not running")
            val appInfo = service.appDao?.findApp(appId)
                ?: return CloudSyncResult(false, error = "App not found")
            val steamCloud = clientProvider.steamCloud
                ?: return CloudSyncResult(false, error = "Cloud not available")

            val result = app.gamegrub.service.steam.SteamAutoCloud.syncUserFiles(
                appInfo = appInfo,
                clientId = clientId,
                steamInstance = service,
                steamCloud = steamCloud,
                preferredSave = app.gamegrub.enums.SaveLocation.valueOf(preferredSave),
                parentScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO),
                prefixToPath = { path -> path },
            ).await()

            CloudSyncResult(
                success = result != null,
                uploadsCompleted = result?.uploadsCompleted == true,
                downloadsCompleted = result?.downloadsCompleted == true,
            )
        } catch (e: Exception) {
            Timber.e(e, "Cloud sync failed")
            CloudSyncResult(false, error = e.message)
        }
    }
}

@Singleton
class SteamStatsClientAdapter @Inject constructor(
    private val clientProvider: SteamClientProvider,
    private val appInfoClient: SteamAppInfoClient,
) : SteamStatsClient {

    override suspend fun getUserStats(appId: Int, steamId: SteamID): UserStatsResult {
        val stats = clientProvider.steamUserStats
            ?: return UserStatsResult(false, ByteArray(0))
        return try {
            val result = stats.getUserStats(appId, steamId).await()
            UserStatsResult(
                success = result.result == `in`.dragonbra.javasteam.enums.EResult.OK,
                schema = result.schema?.toByteArray() ?: ByteArray(0),
                achievementBlocks = result.achievementBlocks?.map { block ->
                    AchievementBlock(
                        achievementId = (block.achievementId as? Number)?.toInt() ?: 0,
                        unlockTimes = block.unlockTime ?: emptyList(),
                    )
                } ?: emptyList(),
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to get user stats")
            UserStatsResult(false, ByteArray(0))
        }
    }

    override suspend fun storeStats(appId: Int, stats: Map<Int, Int>, achievements: Map<Int, Boolean>): Boolean {
        val statsClient = clientProvider.steamUserStats ?: return false
        return try {
            statsClient.storeStats(appId, stats, achievements).await()
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to store stats")
            false
        }
    }

    override fun getSchema(appId: Int): ByteArray? {
        return null
    }
}

@Singleton
class SteamAppInfoClientAdapter @Inject constructor(
    private val appDao: app.gamegrub.db.dao.SteamAppDao,
    private val appInfoDao: app.gamegrub.db.dao.AppInfoDao,
) : SteamAppInfoClient {

    override fun getAppInfo(appId: Int): AppInfoData? {
        val info = appInfoDao.getInstalledApp(appId) ?: return null
        return AppInfoData(appId = info.appId, installPath = info.installPath)
    }

    override fun findApp(appId: Int): AppGameData? {
        val app = appDao.findApp(appId) ?: return null
        return AppGameData(appId = app.appId, name = app.name, packageName = app.packageName)
    }
}
@Singleton
class GameEventEmitterAdapter @Inject constructor() : GameEventEmitter {

    override fun emitSteamEvent(event: Any) {
        if (event is SteamEvent) {
            GameGrubApp.events.emit(event)
        }
    }

    override fun emitAndroidEvent(event: Any) {
        if (event is app.gamegrub.events.AndroidEvent) {
            GameGrubApp.events.emit(event)
        }
    }
}

/**
 * Adapter for preferences.
 */
@Singleton
class SteamPreferencesAdapter @Inject constructor() : SteamPreferences {
    override var username: String
        get() = PrefManager.username
        set(value) { PrefManager.username = value }
    override var accessToken: String
        get() = PrefManager.accessToken
        set(value) { PrefManager.accessToken = value }
    override var refreshToken: String
        get() = PrefManager.refreshToken
        set(value) { PrefManager.refreshToken = value }
    override var clientId: Long?
        get() = PrefManager.clientId
        set(value) { PrefManager.clientId = value ?: 0L }
    override var personaState: Int
        get() = PrefManager.personaState
        set(value) { PrefManager.personaState = value }
    override var steamUserAvatarHash: String
        get() = PrefManager.steamUserAvatarHash
        set(value) { PrefManager.steamUserAvatarHash = value }
    override var steamUserAccountId: Int
        get() = PrefManager.steamUserAccountId
        set(value) { PrefManager.steamUserAccountId = value }
}
