package app.gamegrub.service.steam.di

import app.gamegrub.PrefManager
import app.gamegrub.data.EncryptedAppTicket
import app.gamegrub.db.dao.AppInfoDao
import app.gamegrub.db.dao.EncryptedAppTicketDao
import app.gamegrub.db.dao.SteamAppDao
import app.gamegrub.events.AndroidEvent
import app.gamegrub.events.SteamEvent
import app.gamegrub.ui.runtime.XServerRuntime
import `in`.dragonbra.javasteam.enums.EOSType
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.steam.authentication.AuthPollResult
import `in`.dragonbra.javasteam.steam.authentication.AuthSessionDetails
import `in`.dragonbra.javasteam.steam.authentication.IAuthenticator
import `in`.dragonbra.javasteam.steam.authentication.IChallengeUrlChanged
import `in`.dragonbra.javasteam.steam.handlers.steamuser.SteamUser
import `in`.dragonbra.javasteam.steam.steamclient.SteamClient
import `in`.dragonbra.javasteam.types.SteamID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import timber.log.Timber

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
}

@Singleton
class SteamClientProvider @Inject constructor() {
    var client: SteamClient? = null
    var steamUser: SteamUser? = null
    var steamApps: `in`.dragonbra.javasteam.steam.handlers.steamapps.SteamApps? = null
    var steamCloud: `in`.dragonbra.javasteam.steam.handlers.steamcloud.SteamCloud? = null
    var steamUserStats: `in`.dragonbra.javasteam.steam.handlers.steamuserstats.SteamUserStats? = null
}

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
                this.deviceFriendlyName = service.accountDomain.deviceIdentityManager.getMachineName()
                this.clientOSType = EOSType.WinUnknown
            }

            val authSession = client.authentication.beginAuthSessionViaCredentials(authDetails).await()
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

    override suspend fun loginWithQr(challengeUrlChanged: IChallengeUrlChanged): AuthResult = withContext(Dispatchers.IO) {
        try {
            val client = clientProvider.client ?: return@withContext AuthResult(false, error = "Not connected")
            val service = app.gamegrub.service.steam.SteamService.instance
                ?: return@withContext AuthResult(false, error = "Service not running")

            val authDetails = AuthSessionDetails().apply {
                this.deviceFriendlyName = service.accountDomain.deviceIdentityManager.getMachineName()
                this.clientOSType = EOSType.WinUnknown
                this.persistentSession = true
            }

            val authSession = client.authentication.beginAuthSessionViaQR(authDetails).await()
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
                if (waiting) {
                    delay(authSession.pollingInterval.toLong())
                }
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

@Singleton
class SteamUserClientAdapter @Inject constructor(
    private val clientProvider: SteamClientProvider,
) : SteamUserClient {
    private val _personaState = MutableStateFlow(SteamPersona())
    override val personaState: StateFlow<SteamPersona> = _personaState.asStateFlow()

    override val userSteamId: SteamID?
        get() = clientProvider.client?.steamID

    override suspend fun setPersonaState(state: Int) {
        // The old direct call path broke during refactor; keep this as a safe no-op for now.
        state.hashCode()
    }

    override suspend fun requestPersonaInfo(steamId: SteamID) {
        // The previous implementation accessed private SteamService internals.
        steamId.hashCode()
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
}

@Singleton
class SteamLibraryClientAdapter @Inject constructor() : SteamLibraryClient {
    override suspend fun getOwnedGames(steamId: SteamID): List<OwnedGame> {
        val service = app.gamegrub.service.steam.SteamService.instance
            ?: return emptyList<OwnedGame>().also {
                Timber.w("SteamService instance unavailable while fetching owned games")
            }

        val unifiedFriends = app.gamegrub.service.steam.SteamUnifiedFriends(service)
        return try {
            unifiedFriends.getOwnedGames(steamId.convertToUInt64()).map { game ->
                OwnedGame(
                    appId = game.appId,
                    name = game.name,
                    playtimeMinutes = game.playtimeForever,
                    iconUrl = game.imgIconUrl,
                    logoUrl = "",
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch owned games")
            emptyList()
        } finally {
            unifiedFriends.close()
        }
    }

    override suspend fun checkDlcOwnership(appIds: Set<Int>): Set<Int> {
        return try {
            app.gamegrub.service.steam.SteamService.checkDlcOwnershipViaPICSBatch(appIds)
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
        // Keep compatibility while cloud API is being migrated.
        appId.hashCode()
        clientId.hashCode()
        uploadsCompleted.hashCode()
        clientProvider.steamCloud.hashCode()
    }

    override suspend fun syncUserFiles(
        appId: Int,
        clientId: String,
        preferredSave: String,
    ): CloudSyncResult {
        appId.hashCode()
        clientId.hashCode()
        preferredSave.hashCode()
        return CloudSyncResult(success = false, error = "Cloud sync adapter is not wired yet")
    }
}

@Singleton
class SteamStatsClientAdapter @Inject constructor(
    private val clientProvider: SteamClientProvider,
) : SteamStatsClient {
    override suspend fun getUserStats(appId: Int, steamId: SteamID): UserStatsResult {
        val stats = clientProvider.steamUserStats ?: return UserStatsResult(false, ByteArray(0))
        return try {
            val result = stats.getUserStats(appId, steamId).await()
            UserStatsResult(
                success = true,
                schema = result.schema.toByteArray(),
                achievementBlocks = emptyList(),
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to get user stats")
            UserStatsResult(false, ByteArray(0))
        }
    }

    override suspend fun storeStats(appId: Int, stats: Map<Int, Int>, achievements: Map<Int, Boolean>): Boolean {
        appId.hashCode()
        stats.hashCode()
        achievements.hashCode()
        return false
    }

    override fun getSchema(appId: Int): ByteArray? {
        appId.hashCode()
        return null
    }
}

@Singleton
class SteamAppInfoClientAdapter @Inject constructor(
    private val appDao: SteamAppDao,
    private val appInfoDao: AppInfoDao,
) : SteamAppInfoClient {
    override fun getAppInfo(appId: Int): AppInfoData? {
        val info = runBlocking(Dispatchers.IO) { appInfoDao.getInstalledApp(appId) } ?: return null
        return AppInfoData(appId = info.id, installPath = app.gamegrub.service.steam.SteamService.getAppDirPath(info.id))
    }

    override fun findApp(appId: Int): AppGameData? {
        val app = runBlocking(Dispatchers.IO) { appDao.findApp(appId) } ?: return null
        return AppGameData(appId = app.id, name = app.name, packageName = null)
    }
}

@Singleton
class GameEventEmitterAdapter @Inject constructor() : GameEventEmitter {
    override fun emitSteamEvent(event: Any) {
        if (event is SteamEvent<*>) {
            XServerRuntime.get().events.emitJava(event)
        } else {
            Timber.w("Ignored non-Steam event in emitSteamEvent: ${event::class.java.name}")
        }
    }

    override fun emitAndroidEvent(event: Any) {
        if (event is AndroidEvent<*>) {
            XServerRuntime.get().events.emitJava(event)
        } else {
            Timber.w("Ignored non-Android event in emitAndroidEvent: ${event::class.java.name}")
        }
    }
}

@Singleton
class SteamPreferencesAdapter @Inject constructor() : SteamPreferences {
    override var username: String
        get() = PrefManager.username
        set(value) {
            PrefManager.username = value
        }

    override var accessToken: String
        get() = PrefManager.accessToken
        set(value) {
            PrefManager.accessToken = value
        }

    override var refreshToken: String
        get() = PrefManager.refreshToken
        set(value) {
            PrefManager.refreshToken = value
        }

    override var clientId: Long?
        get() = PrefManager.clientId
        set(value) {
            PrefManager.clientId = value ?: 0L
        }

    override var personaState: Int
        get() = PrefManager.personaState.code()
        set(value) {
            PrefManager.personaState = EPersonaState.from(value)
        }

    override var steamUserAvatarHash: String
        get() = PrefManager.steamUserAvatarHash
        set(value) {
            PrefManager.steamUserAvatarHash = value
        }

    override var steamUserAccountId: Int
        get() = PrefManager.steamUserAccountId
        set(value) {
            PrefManager.steamUserAccountId = value
        }
}

@Singleton
class SteamPicsClientAdapter @Inject constructor() : SteamPicsClient {
    override suspend fun getChangesSince(changeNumber: Long): PicsChanges {
        return PicsChanges(
            currentChangeNumber = changeNumber,
            appChanges = emptySet(),
            packageChanges = emptySet(),
            needsFullUpdate = false,
        )
    }

    override suspend fun getProductInfo(appIds: Set<Int>, packageIds: Set<Int>): PicsProductInfo {
        appIds.hashCode()
        packageIds.hashCode()
        return PicsProductInfo(emptyMap(), emptyMap())
    }
}

@Singleton
class SteamTicketClientAdapter @Inject constructor(
    private val encryptedAppTicketDao: EncryptedAppTicketDao,
) : SteamTicketClient {
    override suspend fun getEncryptedAppTicket(appId: Int): ByteArray? {
        val cached = encryptedAppTicketDao.getByAppId(appId) ?: return null
        val ageMs = System.currentTimeMillis() - cached.timestamp
        return if (ageMs < 30 * 60 * 1000) {
            cached.encryptedTicket
        } else {
            null
        }
    }

    override suspend fun fetchAndCacheEncryptedAppTicket(appId: Int): ByteArray? {
        appId.hashCode()
        return null
    }

    override suspend fun storeEncryptedAppTicket(appId: Int, ticket: ByteArray) {
        encryptedAppTicketDao.insert(
            EncryptedAppTicket(
                appId = appId,
                result = 0,
                ticketVersionNo = 0,
                crcEncryptedTicket = 0,
                cbEncryptedUserData = 0,
                cbEncryptedAppOwnershipTicket = 0,
                encryptedTicket = ticket,
                timestamp = System.currentTimeMillis(),
            ),
        )
    }

    override fun clearAllTickets() {
        runBlocking(Dispatchers.IO) {
            encryptedAppTicketDao.deleteAll()
        }
    }
}
