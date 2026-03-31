package app.gamegrub.service.steam

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.IBinder
import androidx.room.withTransaction
import app.gamegrub.BuildConfig
import app.gamegrub.GameGrubApp
import app.gamegrub.NetworkMonitor
import app.gamegrub.PrefManager
import app.gamegrub.R
import app.gamegrub.data.AppInfo
import app.gamegrub.data.DepotInfo
import app.gamegrub.data.DownloadInfo
import app.gamegrub.data.DownloadingAppInfo
import app.gamegrub.data.GameProcessInfo
import app.gamegrub.data.LaunchInfo
import app.gamegrub.data.OwnedGames
import app.gamegrub.data.PostSyncInfo
import app.gamegrub.data.SteamApp
import app.gamegrub.data.SteamFriend
import app.gamegrub.data.SteamLicense
import app.gamegrub.data.UserFileInfo
import app.gamegrub.db.GameGrubDatabase
import app.gamegrub.db.dao.SteamAppDao
import app.gamegrub.db.dao.SteamLicenseDao
import app.gamegrub.enums.LoginResult
import app.gamegrub.enums.Marker
import app.gamegrub.enums.SaveLocation
import app.gamegrub.enums.SyncResult
import app.gamegrub.events.AndroidEvent
import app.gamegrub.events.SteamEvent
import app.gamegrub.service.NotificationHelper
import app.gamegrub.service.steam.SteamService.Companion.getAppDirPath
import app.gamegrub.service.steam.managers.LaunchIntentResult
import app.gamegrub.statsgen.Achievement
import app.gamegrub.ui.utils.SnackbarManager
import app.gamegrub.utils.container.ContainerUtils
import app.gamegrub.utils.network.Net
import app.gamegrub.utils.steam.SteamUtils
import app.gamegrub.utils.steam.generateSteamApp
import app.gamegrub.utils.storage.MarkerUtils
import com.winlator.container.Container
import com.winlator.container.ContainerManager
import com.winlator.xenvironment.ImageFs
import dagger.hilt.android.AndroidEntryPoint
import `in`.dragonbra.javasteam.depotdownloader.DepotDownloader
import `in`.dragonbra.javasteam.depotdownloader.IDownloadListener
import `in`.dragonbra.javasteam.depotdownloader.data.AppItem
import `in`.dragonbra.javasteam.depotdownloader.data.DownloadItem
import `in`.dragonbra.javasteam.enums.ELicenseFlags
import `in`.dragonbra.javasteam.enums.EOSType
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.networking.steam3.ProtocolTypes
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesClientObjects
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesFamilygroupsSteamclient
import `in`.dragonbra.javasteam.rpc.service.FamilyGroups
import `in`.dragonbra.javasteam.steam.authentication.IAuthenticator
import `in`.dragonbra.javasteam.steam.authentication.IChallengeUrlChanged
import `in`.dragonbra.javasteam.steam.authentication.QrAuthSession
import `in`.dragonbra.javasteam.steam.discovery.FileServerListProvider
import `in`.dragonbra.javasteam.steam.discovery.ServerQuality
import `in`.dragonbra.javasteam.steam.handlers.steamapps.GamePlayedInfo
import `in`.dragonbra.javasteam.steam.handlers.steamapps.License
import `in`.dragonbra.javasteam.steam.handlers.steamapps.PICSRequest
import `in`.dragonbra.javasteam.steam.handlers.steamapps.SteamApps
import `in`.dragonbra.javasteam.steam.handlers.steamapps.callback.LicenseListCallback
import `in`.dragonbra.javasteam.steam.handlers.steamcloud.SteamCloud
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.SteamFriends
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.PersonaStateCallback
import `in`.dragonbra.javasteam.steam.handlers.steamgameserver.SteamGameServer
import `in`.dragonbra.javasteam.steam.handlers.steammasterserver.SteamMasterServer
import `in`.dragonbra.javasteam.steam.handlers.steamscreenshots.SteamScreenshots
import `in`.dragonbra.javasteam.steam.handlers.steamunifiedmessages.SteamUnifiedMessages
import `in`.dragonbra.javasteam.steam.handlers.steamuser.ChatMode
import `in`.dragonbra.javasteam.steam.handlers.steamuser.LogOnDetails
import `in`.dragonbra.javasteam.steam.handlers.steamuser.SteamUser
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOffCallback
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOnCallback
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.PlayingSessionStateCallback
import `in`.dragonbra.javasteam.steam.handlers.steamuserstats.SteamUserStats
import `in`.dragonbra.javasteam.steam.handlers.steamworkshop.SteamWorkshop
import `in`.dragonbra.javasteam.steam.steamclient.AsyncJobFailedException
import `in`.dragonbra.javasteam.steam.steamclient.SteamClient
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackManager
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.ConnectedCallback
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.DisconnectedCallback
import `in`.dragonbra.javasteam.steam.steamclient.configuration.SteamConfiguration
import `in`.dragonbra.javasteam.types.DepotManifest
import `in`.dragonbra.javasteam.types.FileData
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.javasteam.util.log.LogListener
import `in`.dragonbra.javasteam.util.log.LogManager
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Collections
import java.util.EnumSet
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.io.path.pathString
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.future.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

/**
 * Steam foreground service - handles all Steam integration.
 *
 * Core functionality:
 * - Authentication (QR code, username/password)
 * - Game library management
 * - Game downloads and updates
 * - Cloud save synchronization
 *
 * Note: This is a large coordinator (~3800 lines). Consider decomposing.
 */

@AndroidEntryPoint
class SteamService : Service(), IChallengeUrlChanged {

    // To view log messages in android logcat properly
    private val logger = object : LogListener {
        override fun onLog(clazz: Class<*>, message: String?, throwable: Throwable?) {
            val logMessage = message ?: "No message given"
            Timber.i(throwable, "[${clazz.simpleName}] -> $logMessage")
        }

        override fun onError(clazz: Class<*>, message: String?, throwable: Throwable?) {
            val logMessage = message ?: "No message given"
            Timber.e(throwable, "[${clazz.simpleName}] -> $logMessage")
        }
    }

    @Inject
    lateinit var db: GameGrubDatabase

    @Inject
    lateinit var steamClientProvider: app.gamegrub.service.steam.di.SteamClientProvider

    @Inject
    lateinit var libraryDomain: app.gamegrub.service.steam.domain.SteamLibraryDomain

    @Inject
    lateinit var sessionDomain: app.gamegrub.service.steam.domain.SteamSessionDomain

    private lateinit var notificationHelper: NotificationHelper

    internal var callbackManager: CallbackManager? = null
    internal var steamClient: SteamClient? = null
    internal val callbackSubscriptions: ArrayList<Closeable> = ArrayList()

    private var _unifiedFriends: SteamUnifiedFriends? = null
    private var _steamUser: SteamUser? = null
    private var _steamApps: SteamApps? = null
    private var _steamFriends: SteamFriends? = null
    private var _steamCloud: SteamCloud? = null
    private var _steamUserStats: SteamUserStats? = null
    private var _steamFamilyGroups: FamilyGroups? = null

    private var retryAttempt = 0

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var reconnectJob: Job? = null

    private val onEndProcess: (AndroidEvent.EndProcess) -> Unit = {
        scope.launch { stop() }
    }

    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback

    @Inject
    lateinit var accountDomain: app.gamegrub.service.steam.domain.SteamAccountDomain

    // Compatibility bridge for existing UI/session call sites during steam-domain refactor.
    val localPersona
        get() = accountDomain.localPersona

    @Inject
    lateinit var cloudStatsDomain: app.gamegrub.service.steam.domain.SteamCloudStatsDomain

    @Inject
    lateinit var installDomain: app.gamegrub.service.steam.domain.SteamInstallDomain

    @Inject
    lateinit var picsSyncDomain: app.gamegrub.service.steam.domain.SteamPicsSyncDomain

    private val downloadJobs get() = installDomain.downloadJobs

    companion object {
        const val MAX_PICS_BUFFER = 256
        const val MAX_RETRY_ATTEMPTS = 20
        const val INVALID_APP_ID: Int = Int.MAX_VALUE
        const val INVALID_PKG_ID: Int = Int.MAX_VALUE
        private const val STEAM_CONTROLLER_CONFIG_FILENAME = "steam_controller.vdf"
        private val catalogManager: app.gamegrub.service.steam.managers.SteamCatalogManager
            get() = app.gamegrub.service.steam.managers.SteamCatalogManager

        private val installDomain: app.gamegrub.service.steam.domain.SteamInstallDomain
            get() = instance?.installDomain
                ?: throw IllegalStateException("SteamService not running")

        var requestTimeout = 30.seconds
        var responseTimeout = 120.seconds

        private val PROTOCOL_TYPES = EnumSet.of(ProtocolTypes.WEB_SOCKET)

        internal var instance: SteamService? = null

        /**
         * Returns the instance or throws with a clear error message.
         * Use this when the service MUST be running for the operation to proceed.
         */
        fun requireInstance(): SteamService = instance
            ?: throw IllegalStateException(
                "SteamService is not running. Start the service before calling Steam operations.",
            )

        /**
         * Returns the instance or null with a Timber warning logged.
         */
        @Suppress("UNUSED")
        fun getInstanceOrNull(): SteamService? = instance?.also {
            Timber.v("SteamService instance accessed")
        } ?: null.also {
            Timber.w("SteamService instance is null - service may not be started")
        }

        var cachedAchievements: List<Achievement>? = null
            private set
        var cachedAchievementsAppId: Int? = null
            private set

        val hasWifiOrEthernet: Boolean get() = NetworkMonitor.hasWifiOrEthernet.value

        /** @return true if download may proceed; false if blocked (notifies user) */
        private fun checkWifiOrNotify(): Boolean {
            if (PrefManager.downloadOnWifiOnly && !hasWifiOrEthernet) {
                val svc = instance
                if (svc != null) {
                    svc.notificationHelper.notify(svc.getString(R.string.download_no_wifi))
                } else {
                    Timber.w("checkWifiOrNotify: no SteamService instance to notify")
                }
                return false
            }
            return true
        }

        private fun removeDownloadJob(appId: Int) {
            installDomain.removeDownloadJob(appId)
        }

        private fun notifyDownloadStarted(appId: Int) {
            GameGrubApp.events.emit(AndroidEvent.DownloadStatusChanged(appId, true))
        }

        /** Returns true if there is an incomplete download on disk (no complete marker). */
        fun hasPartialDownload(appId: Int): Boolean = runBlocking {
            requireInstance().libraryDomain.hasPartialDownload(appId)
        }

        // Track whether a game is currently running to prevent premature service stop.
        @JvmStatic
        var keepAlive: Boolean
            get() = instance?.sessionDomain?.getKeepAlive() ?: false
            set(value) {
                instance?.sessionDomain?.setKeepAlive(value)
            }

        @Volatile
        var isImporting: Boolean = false

        var isStopping: Boolean = false
            private set
        var isConnected: Boolean = false
            private set
        var isRunning: Boolean = false
            private set
        var isLoggingOut: Boolean
            get() = instance?.accountDomain?.isLoggingOut ?: false
            private set(value) { instance?.accountDomain?.setLoggingOut(value) }
        val isLoggedIn: Boolean
            get() = instance?.steamClient?.steamID?.isValid == true
        var isWaitingForQRAuth: Boolean = false
            private set

        val userSteamId: SteamID?
            get() = instance?.steamClient?.steamID

        fun getSteamId64(): Long? {
            val svc = instance
            if (svc != null) {
                return svc.accountDomain.userManager.getSteamId64()
            }
            return PrefManager.steamUserSteamId64.takeIf { it > 0L }
        }

        fun getSteam3AccountId(): Long? {
            val svc = instance
            if (svc != null) {
                return svc.accountDomain.userManager.getSteam3AccountId()
            }
            return PrefManager.steamUserAccountId.toLong().takeIf { it > 0L }
        }

        val familyMembers: List<Int>
            get() = instance?.accountDomain?.familyGroupMembers?.value ?: emptyList()

        val isLoginInProgress: Boolean
            get() = instance?.accountDomain?.loginResult?.value == LoginResult.InProgress

        suspend fun setPersonaState(state: EPersonaState) {
            val svc = instance
            if (svc == null) {
                Timber.w("Ignoring setPersonaState because SteamService is not running")
                return
            }
            svc.accountDomain.friendsManager.setPersonaState(state)
        }

        suspend fun requestUserPersona() {
            val svc = instance
            if (svc == null) {
                Timber.w("Ignoring requestUserPersona because SteamService is not running")
                return
            }
            svc.accountDomain.friendsManager.requestUserPersona()
        }

        suspend fun getSelfCurrentlyPlayingAppId(): Int? {
            val svc = instance
            if (svc == null) {
                Timber.w("Ignoring getSelfCurrentlyPlayingAppId because SteamService is not running")
                return null
            }
            return svc.accountDomain.friendsManager.getSelfCurrentlyPlayingAppId()
        }

        suspend fun kickPlayingSession(onlyGame: Boolean = true): Boolean =
            withContext(Dispatchers.IO) {
                val svc = instance ?: return@withContext false
                svc.sessionDomain.kickPlayingSession(onlyGame) { onlyStopGame ->
                    svc._steamUser?.kickPlayingSession(onlyStopGame = onlyStopGame)
                        ?: throw IllegalStateException("SteamUser is not initialized")
                }
            }

        /**
         * Get licenses from database for use with DepotDownloader
         */
        suspend fun getLicensesFromDb(): List<License> = withContext(Dispatchers.IO) {
            val svc = instance ?: return@withContext emptyList()
            svc.libraryDomain.getLicensesFromDb()
        }

        fun getPkgInfoOf(appId: Int): SteamLicense? = runBlocking {
            requireInstance().libraryDomain.getPkgInfoOf(appId)
        }

        fun getAppInfoOf(appId: Int): SteamApp? = runBlocking {
            requireInstance().libraryDomain.getAppInfoOf(appId)
        }

        fun getDownloadingAppInfoOf(appId: Int): DownloadingAppInfo? = runBlocking {
            requireInstance().libraryDomain.getDownloadingAppInfoOf(appId)
        }

        fun getDownloadableDlcAppsOf(appId: Int): List<SteamApp>? = runBlocking {
            requireInstance().libraryDomain.getDownloadableDlcAppsOf(appId)
        }

        fun getHiddenDlcAppsOf(appId: Int): List<SteamApp>? = runBlocking {
            requireInstance().libraryDomain.getHiddenDlcAppsOf(appId)
        }

        fun getInstalledApp(appId: Int): AppInfo? = runBlocking {
            requireInstance().libraryDomain.getInstalledApp(appId)
        }

        fun getInstalledDepotsOf(appId: Int): List<Int>? {
            return getInstalledApp(appId)?.downloadedDepots
        }

        fun getInstalledDlcDepotsOf(appId: Int): List<Int>? {
            return getInstalledApp(appId)?.dlcDepots
        }

        fun getAppDownloadInfo(appId: Int): DownloadInfo? {
            return installDomain.getAppDownloadInfo(appId)
        }

        fun isAppInstalled(appId: Int): Boolean {
            return MarkerUtils.hasMarker(getAppDirPath(appId), Marker.DOWNLOAD_COMPLETE_MARKER)
        }

        fun getAppDlc(appId: Int): Map<Int, DepotInfo> {
            return getAppInfoOf(appId)?.let {
                it.depots.filter { it.value.dlcAppId != INVALID_APP_ID }
            }.orEmpty()
        }

        suspend fun getOwnedAppDlc(appId: Int): Map<Int, DepotInfo> {
            val client = instance?.steamClient ?: return emptyMap()
            client.steamID?.accountID?.toInt() ?: return emptyMap()
            val steamId = userSteamId ?: return emptyMap()
            val ownedGameIds = getOwnedGames(steamId.convertToUInt64()).map { it.appId }.toHashSet()

            return catalogManager.filterOwnedAppDlc(
                appDlcDepots = getAppDlc(appId),
                invalidAppId = INVALID_APP_ID,
                ownedGameIds = ownedGameIds,
                hasLicense = { dlcAppId -> runBlocking { instance?.libraryDomain?.findLicense(dlcAppId) } != null },
                hasPicsApp = { dlcAppId -> runBlocking { instance?.libraryDomain?.findApp(dlcAppId) } != null },
            )
        }

        fun getMainAppDlcIdsWithoutProperDepotDlcIds(appId: Int): MutableList<Int> {
            val hiddenDlcAppIds = getHiddenDlcAppsOf(appId).orEmpty().map { it.id }
            return catalogManager.resolveMainAppDlcIdsWithoutProperDepotDlcIds(
                appInfo = getAppInfoOf(appId),
                hiddenDlcAppIds = hiddenDlcAppIds,
            )
        }

        /**
         * Refresh the owned games list by querying Steam, diffing with the local DB, and
         * queueing PICS requests for anything new so metadata gets populated.
         *
         * @return number of newly discovered appIds that were scheduled for PICS.
         */
        suspend fun refreshOwnedGamesFromServer(): Int = requireInstance().libraryDomain.refreshOwnedGamesFromServer()

        /**
         * Common Filter for downloadable depots
         */
        fun filterForDownloadableDepots(
            depot: DepotInfo,
            has64Bit: Boolean,
            preferredLanguage: String,
            ownedDlc: Map<Int, DepotInfo>?,
        ): Boolean = catalogManager.filterForDownloadableDepots(
            depot = depot,
            has64Bit = has64Bit,
            preferredLanguage = preferredLanguage,
            ownedDlc = ownedDlc,
        )

        fun getMainAppDepots(appId: Int, containerLanguage: String): Map<Int, DepotInfo> {
            val appInfo = getAppInfoOf(appId) ?: return emptyMap()
            val ownedDlc = runBlocking { getOwnedAppDlc(appId) }

            return catalogManager.getMainAppDepots(
                appInfo = appInfo,
                containerLanguage = containerLanguage,
                ownedDlc = ownedDlc,
            )
        }

        /**
         * Get downloadable depots for a given app (default language), including all DLCs
         * @return Map of app ID to depot ID to depot info
         */
        fun getDownloadableDepots(appId: Int): Map<Int, DepotInfo> {
            val preferredLanguage = PrefManager.containerLanguage
            return getDownloadableDepots(appId, preferredLanguage)
        }

        /**
         * Get downloadable depots for a given app (container language), including all DLCs
         * @return Map of app ID to depot ID to depot info
         */
        fun getDownloadableDepots(appId: Int, preferredLanguage: String): Map<Int, DepotInfo> {
            val appInfo = getAppInfoOf(appId) ?: return emptyMap()
            val ownedDlc = runBlocking { getOwnedAppDlc(appId) }

            return catalogManager.getDownloadableDepots(
                appInfo = appInfo,
                preferredLanguage = preferredLanguage,
                ownedDlc = ownedDlc,
                indirectDlcApps = getDownloadableDlcAppsOf(appId).orEmpty(),
            )
        }

        fun getAppDirName(app: SteamApp?): String {
            // The folder name, if it got made
            var appName = app?.config?.installDir.orEmpty()
            if (appName.isEmpty()) {
                appName = app?.name.orEmpty()
            }
            return appName
        }

        /**
         * Resolve best matching directory: completed install > partial > null.
         * Extracted for testability — called by [getAppDirPath].
         */
        fun resolveExistingAppDir(installPaths: List<String>, names: List<String>): String? {
            var firstExisting: String? = null
            for (basePath in installPaths) {
                for (name in names) {
                    if (name.isEmpty()) continue
                    val path = Paths.get(basePath, name)
                    if (Files.isDirectory(path)) {
                        if (MarkerUtils.hasMarker(path.pathString, Marker.DOWNLOAD_COMPLETE_MARKER)) {
                            return path.pathString
                        }
                        if (firstExisting == null) firstExisting = path.pathString
                    }
                }
            }
            return firstExisting
        }

        fun getAppDirPath(gameId: Int): String = SteamPaths.getAppDirPath(gameId)

        /** select the primary binary */
        fun choosePrimaryExe(
            files: List<FileData>?,
            gameName: String,
        ): FileData? = installDomain.choosePrimaryExe(files, gameName)

        /**
         * Picks the real shipped EXE for a Steam app.
         *
         * ❶ try the dev-supplied launch entry (skip obvious stubs)
         * ❷ else score all manifest-flagged EXEs and keep the best
         * ❸ else fall back to the largest flagged EXE in the biggest depot
         * If everything fails, return the game's install directory.
         */
        fun getInstalledExe(appId: Int): String {
            val appInfo = getAppInfoOf(appId) ?: return ""
            val steamClient = instance?.steamClient
            val licenses = runBlocking { getLicensesFromDb() }

            return installDomain.resolveInstalledExe(
                appInfo = appInfo,
                canQueryManifests = steamClient != null && licenses.isNotEmpty(),
                fallbackExecutable = {
                    getWindowsLaunchInfos(appId).firstOrNull()?.executable.orEmpty()
                },
                loadManifestCandidates = { depotId, manifestInfo ->
                    val manifest = DepotManifest.loadFromFile(
                        "${getAppDirPath(appId)}/.DepotDownloader/${depotId}_${manifestInfo.gid}.manifest",
                    )
                    Timber.d("Using manifest for depot $depotId  size=${manifestInfo.size}")
                    manifest?.files?.map { file ->
                        app.gamegrub.service.steam.managers.SteamInstalledExeManager.ExecutableCandidate(
                            source = file,
                            path = file.fileName,
                            totalSize = file.totalSize,
                            hasExecutableFlag = installDomain.hasExecutableFlag(file.flags),
                            isStub = installDomain.isStub(file),
                        )
                    }
                },
                choosePrimary = { candidates, gameName ->
                    val files = candidates.map { it.source }
                    val chosen = installDomain.choosePrimaryExe(files, gameName)
                    if (chosen == null) {
                        null
                    } else {
                        candidates.firstOrNull { it.source == chosen }
                    }
                },
            )
        }

        /**
         * Resolves the effective launch executable for a Steam game (container config or auto-detected).
         * Returns a non-empty sentinel when [com.winlator.container.Container.isLaunchRealSteam] is true so the launch is not blocked.
         */
        fun getLaunchExecutable(appId: String, container: Container): String {
            if (container.isLaunchRealSteam) return "steam"
            val gameId = ContainerUtils.extractGameIdFromContainerId(appId)
            return container.executablePath.ifEmpty { getInstalledExe(gameId) }
        }

        fun deleteApp(appId: Int): Boolean {
            val appDirPath = getAppDirPath(appId)
            MarkerUtils.removeMarker(appDirPath, Marker.DOWNLOAD_COMPLETE_MARKER)
            val svc = instance ?: return false
            svc.scope.launch {
                svc.db.withTransaction {
                    svc.libraryDomain.deleteAppData(appId)

                    val indirectDlcAppIds = getDownloadableDlcAppsOf(appId).orEmpty().map { it.id }
                    indirectDlcAppIds.forEach { dlcAppId ->
                        svc.libraryDomain.deleteAppData(dlcAppId)
                    }
                }
            }

            return File(appDirPath).deleteRecursively()
        }

        fun downloadApp(appId: Int): DownloadInfo? {
            val currentDownloadInfo = getAppDownloadInfo(appId)
            if (currentDownloadInfo != null) {
                return currentDownloadInfo
            } else {
                // If downloading app info exists
                val downloadingAppInfo = getDownloadingAppInfoOf(appId)
                if (downloadingAppInfo != null) {
                    return downloadApp(appId, downloadingAppInfo.dlcAppIds, isUpdateOrVerify = false)
                } else {
                    // Otherwise it is verifying files
                    val dlcAppIds = getInstalledDlcDepotsOf(appId).orEmpty().toMutableList()

                    getDownloadableDlcAppsOf(appId)?.forEach { dlcApp ->
                        val installedDlcApp = getInstalledApp(dlcApp.id)
                        if (installedDlcApp != null) {
                            dlcAppIds.add(installedDlcApp.id)
                        }
                    }

                    return downloadApp(appId, dlcAppIds, isUpdateOrVerify = true)
                }
            }
        }

        fun downloadApp(appId: Int, dlcAppIds: List<Int>, isUpdateOrVerify: Boolean): DownloadInfo? {
            if (!checkWifiOrNotify()) return null
            val ctx = instance?.applicationContext ?: return null
            return getAppInfoOf(appId)?.let {
                val container = ContainerManager(ctx).getContainerById("STEAM_$appId")
                val containerLanguage = container?.language ?: PrefManager.containerLanguage

                Timber.tag("SteamService").d("downloadApp: downloading app $appId with language $containerLanguage")

                val depots = getDownloadableDepots(appId = appId, preferredLanguage = containerLanguage)
                downloadApp(
                    appId = appId,
                    downloadableDepots = depots,
                    userSelectedDlcAppIds = dlcAppIds,
                    branch = "public",
                    containerLanguage = containerLanguage,
                    isUpdateOrVerify = isUpdateOrVerify,
                )
            }
        }

        fun isImageFsInstalled(context: Context): Boolean {
            return ImageFs.find(context).rootDir.exists()
        }

        fun isImageFsInstallable(context: Context, variant: String): Boolean {
            val imageFs = ImageFs.find(context)
            if (variant.equals(Container.BIONIC)) {
                return File(imageFs.filesDir, "imagefs_bionic.txz").exists() ||
                    context.assets.list("")
                        ?.contains("imagefs_bionic.txz") == true
            } else {
                return File(imageFs.filesDir, "imagefs_gamenative.txz").exists() ||
                    context.assets.list("")
                        ?.contains("imagefs_gamenative.txz") == true
            }
        }

        @Suppress("UNUSED")
        fun isSteamInstallable(context: Context): Boolean {
            val imageFs = ImageFs.find(context)
            return File(imageFs.filesDir, "steam.tzst").exists()
        }

        fun isFileInstallable(context: Context, filename: String): Boolean {
            val imageFs = ImageFs.find(context)
            return File(imageFs.filesDir, filename).exists()
        }

        suspend fun fetchFile(
            url: String,
            dest: File,
            onProgress: (Float) -> Unit,
        ) = withContext(Dispatchers.IO) {
            val tmp = File(dest.absolutePath + ".part")
            try {
                val http = SteamUtils.http

                val req = Request.Builder().url(url).build()
                http.newCall(req).execute().use { rsp ->
                    check(rsp.isSuccessful) { "HTTP ${rsp.code}" }
                    val body = rsp.body
                    val total = body.contentLength()
                    tmp.outputStream().use { out ->
                        body.byteStream().copyTo(out, 8 * 1024) { read ->
                            onProgress(read.toFloat() / total)
                        }
                    }
                    if (total > 0 && tmp.length() != total) {
                        tmp.delete()
                        error("incomplete download")
                    }
                    if (!tmp.renameTo(dest)) {
                        tmp.copyTo(dest, overwrite = true)
                        tmp.delete()
                    }
                }
            } catch (e: Exception) {
                tmp.delete()
                throw e
            }
        }

        suspend fun fetchFileWithFallback(
            fileName: String,
            dest: File,
            onProgress: (Float) -> Unit,
        ) = withContext(Dispatchers.IO) {
            val primaryUrl = "https://downloads.gamenative.app/$fileName"
            val fallbackUrl = "https://pub-9fcd5294bd0d4b85a9d73615bf98f3b5.r2.dev/$fileName"
            try {
                fetchFile(primaryUrl, dest, onProgress)
            } catch (e: Exception) {
                Timber.w(e, "Primary download failed; retrying with fallback URL")
                try {
                    fetchFile(fallbackUrl, dest, onProgress)
                } catch (e2: Exception) {
                    dest.delete()
                    throw IOException(
                        "Failed to download $fileName. Please check your network connection or try a VPN.",
                        e2,
                    )
                }
            }
        }

        /** copyTo with progress callback */
        private inline fun InputStream.copyTo(
            out: OutputStream,
            bufferSize: Int = DEFAULT_BUFFER_SIZE,
            progress: (Long) -> Unit,
        ) {
            val buf = ByteArray(bufferSize)
            var bytesRead: Int
            var total = 0L
            while (read(buf).also { bytesRead = it } >= 0) {
                if (bytesRead == 0) continue
                out.write(buf, 0, bytesRead)
                total += bytesRead
                progress(total)
            }
        }

        fun downloadImageFs(
            onDownloadProgress: (Float) -> Unit,
            parentScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
            variant: String,
            context: Context,
        ) = parentScope.async {
            Timber.i("imagefs will be downloaded")
            val filesDir = context.filesDir
            if (variant == Container.BIONIC) {
                val dest = File(filesDir, "imagefs_bionic.txz")
                Timber.d("Downloading imagefs_bionic to %s", dest)
                fetchFileWithFallback("imagefs_bionic.txz", dest, onDownloadProgress)
            } else {
                val dest = File(filesDir, "imagefs_gamenative.txz")
                Timber.d("Downloading imagefs_gamenative to %s", dest)
                fetchFileWithFallback("imagefs_gamenative.txz", dest, onDownloadProgress)
            }
        }

        fun downloadImageFsPatches(
            onDownloadProgress: (Float) -> Unit,
            parentScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
            context: Context,
        ) = parentScope.async {
            Timber.i("imagefs will be downloaded")
            val dest = File(context.filesDir, "imagefs_patches_gamenative.tzst")
            Timber.d("Downloading imagefs_patches_gamenative.tzst to %s", dest)
            fetchFileWithFallback("imagefs_patches_gamenative.tzst", dest, onDownloadProgress)
        }

        fun downloadFile(
            onDownloadProgress: (Float) -> Unit,
            parentScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
            context: Context,
            fileName: String,
        ) = parentScope.async {
            Timber.i("$fileName will be downloaded")
            val dest = File(context.filesDir, fileName)
            Timber.d("Downloading %s to %s", fileName, dest)
            fetchFileWithFallback(fileName, dest, onDownloadProgress)
        }

        fun downloadSteam(
            onDownloadProgress: (Float) -> Unit,
            parentScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
            context: Context,
        ) = parentScope.async {
            Timber.i("imagefs will be downloaded")
            val dest = File(context.filesDir, "steam.tzst")
            Timber.d("Downloading steam.tzst to %s", dest)
            fetchFileWithFallback("steam.tzst", dest, onDownloadProgress)
        }

        private fun readBuiltInSteamInputTemplate(fileName: String): String? {
            val assets = instance?.assets ?: return null
            return runCatching {
                assets.open("steaminput/$fileName").use { stream ->
                    stream.readBytes().toString(Charsets.UTF_8)
                }
            }.getOrNull()
        }

        private fun readDownloadedSteamInputTemplate(appId: Int): String? {
            val configFile = File(getAppDirPath(appId), STEAM_CONTROLLER_CONFIG_FILENAME)
            if (!configFile.exists()) return null
            return configFile.readText(Charsets.UTF_8)
        }

        fun resolveSteamControllerVdfText(appId: Int): String? {
            val config = getAppInfoOf(appId)?.config ?: return null
            val route = installDomain.routeFor(config.steamControllerTemplateIndex)
            return when (route.source) {
                app.gamegrub.service.steam.managers.SteamControllerTemplateRoutingManager.TemplateSource.Downloaded -> {
                    readDownloadedSteamInputTemplate(appId)
                }

                app.gamegrub.service.steam.managers.SteamControllerTemplateRoutingManager.TemplateSource.Manifest -> {
                    val manifestPath = config.steamInputManifestPath?.trim().orEmpty()
                    val appPath = getAppDirPath(appId)
                    val manifestFile = installDomain.resolveSteamInputManifestFile(appPath, manifestPath)
                        ?: return null
                    installDomain.loadConfigFromManifest(manifestFile)
                }

                app.gamegrub.service.steam.managers.SteamControllerTemplateRoutingManager.TemplateSource.BuiltIn -> {
                    val template = route.builtInTemplateName ?: return null
                    readBuiltInSteamInputTemplate(template)
                }
            }
        }

        fun downloadApp(
            appId: Int,
            downloadableDepots: Map<Int, DepotInfo>,
            userSelectedDlcAppIds: List<Int>,
            branch: String,
            containerLanguage: String,
            isUpdateOrVerify: Boolean,
        ): DownloadInfo? {
            val appDirPath = getAppDirPath(appId)

            if (!checkWifiOrNotify()) return null
            if (getAppDownloadInfo(appId) != null) return getAppDownloadInfo(appId)
            Timber.d("depots is empty? %s", downloadableDepots.isEmpty())
            if (downloadableDepots.isEmpty()) return null

            val plan = installDomain.buildDownloadPlan(
                appId = appId,
                downloadableDepots = downloadableDepots,
                userSelectedDlcAppIds = userSelectedDlcAppIds,
                mainDepots = getMainAppDepots(appId, containerLanguage),
                downloadableDlcApps = getDownloadableDlcAppsOf(appId).orEmpty(),
                installedDownloadedDepots = getInstalledApp(appId)?.downloadedDepots,
                isUpdateOrVerify = isUpdateOrVerify,
                invalidAppId = INVALID_APP_ID,
                initialMainAppDlcIds = getMainAppDlcIdsWithoutProperDepotDlcIds(appId),
            )

            val mainAppDepots = plan.mainAppDepots
            val dlcAppDepots = plan.dlcAppDepots
            val selectedDepots = plan.selectedDepots
            val downloadingAppIds = CopyOnWriteArrayList(plan.downloadingAppIds)
            val calculatedDlcAppIds = CopyOnWriteArrayList(plan.calculatedDlcAppIds)
            val mainAppDlcIds = plan.mainAppDlcIds

            Timber.i("selectedDepots is empty? %s", selectedDepots.isEmpty())

            if (selectedDepots.isEmpty()) return null

            Timber.i("Starting download for $appId")
            Timber.i("App contains ${mainAppDepots.size} depot(s): ${mainAppDepots.keys}")
            Timber.i("DLC contains ${dlcAppDepots.size} depot(s): ${dlcAppDepots.keys}")
            Timber.i("downloadingAppIds: $downloadingAppIds")

            // Save downloading app info
            runBlocking {
                instance?.libraryDomain?.saveDownloadingAppInfo(appId, userSelectedDlcAppIds)
            }

            val info = DownloadInfo(selectedDepots.size, appId, downloadingAppIds).also { di ->
                di.setPersistencePath(appDirPath)
                // Set weights for each depot based on manifest sizes
                val sizes = selectedDepots.map { (_, depot) ->
                    val mInfo = depot.manifests[branch]
                        ?: depot.encryptedManifests[branch]
                        ?: return@map 1L
                    mInfo.size
                }
                sizes.forEachIndexed { i, bytes -> di.setWeight(i, bytes) }

                // Total expected size (used for ETA based on recent download speed)
                val totalBytes = sizes.sum()
                di.setTotalExpectedBytes(totalBytes)

                // Load persisted bytes downloaded value on resume
                val persistedBytes = di.loadPersistedBytesDownloaded(appDirPath)
                if (persistedBytes > 0L) {
                    di.initializeBytesDownloaded(persistedBytes)
                    Timber.i("Resumed download: initialized with $persistedBytes bytes")
                }

                val svc = instance ?: return null
                val downloadJob = svc.scope.launch {
                    try {
                        // Get licenses from database
                        val licenses = getLicensesFromDb()
                        if (licenses.isEmpty()) {
                            Timber.w("No licenses available for download")
                            return@launch
                        }

                        // Some notes here:
                        // Write should always be 1 in mobile device, as normally it does not use a SSD for storage
                        // And to have maximum throughput, set downloadRatio = decompressRatio = 1.0 x CPU Cores
                        var downloadRatio = 0.0
                        var decompressRatio = 0.0

                        when (PrefManager.downloadSpeed) {
                            8 -> {
                                downloadRatio = 0.6
                                decompressRatio = 0.2
                            }

                            16 -> {
                                downloadRatio = 1.2
                                decompressRatio = 0.4
                            }

                            24 -> {
                                downloadRatio = 1.5
                                decompressRatio = 0.5
                            }

                            32 -> {
                                downloadRatio = 2.4
                                decompressRatio = 0.8
                            }
                        }

                        val cpuCores = Runtime.getRuntime().availableProcessors()
                        val maxDownloads = (cpuCores * downloadRatio).toInt().coerceAtLeast(1)
                        val maxDecompress = (cpuCores * decompressRatio).toInt().coerceAtLeast(1)

                        Timber.i("CPU Cores: $cpuCores")
                        Timber.i("maxDownloads: $maxDownloads")
                        Timber.i("maxDecompress: $maxDecompress")

                        // Create DepotDownloader instance
                        val depotDownloader = DepotDownloader(
                            instance!!.steamClient!!,
                            licenses,
                            debug = false,
                            androidEmulation = true,
                            maxDownloads = maxDownloads,
                            maxDecompress = maxDecompress,
                            parentJob = coroutineContext[Job.Key],
                            autoStartDownload = false,
                        )

                        // Create listeners for DLC apps
                        val depotIdToIndex = selectedDepots.keys.mapIndexed { index, depotId -> depotId to index }.toMap()
                        val listener = AppDownloadListener(di, depotIdToIndex)
                        depotDownloader.addListener(listener)

                        if (mainAppDepots.isNotEmpty()) {
                            // Create mapping from depotId to index for progress tracking
                            val mainAppDepotIds = mainAppDepots.keys.sorted()

                            // Create AppItem with only mandatory appId
                            val mainAppItem = AppItem(
                                appId,
                                installDirectory = getAppDirPath(appId),
                                depot = mainAppDepotIds,
                            )

                            // Add item to downloader
                            depotDownloader.add(mainAppItem)
                        }

                        // Create AppItem for each DLC app
                        calculatedDlcAppIds.forEach { dlcAppId ->
                            val dlcDepots = selectedDepots.filter { it.value.dlcAppId == dlcAppId }
                            val dlcDepotIds = dlcDepots.keys.sorted()

                            val dlcAppItem = AppItem(
                                dlcAppId,
                                installDirectory = getAppDirPath(appId),
                                depot = dlcDepotIds,
                            )

                            depotDownloader.add(dlcAppItem)
                        }

                        val appConfig = getAppInfoOf(appId)?.config
                        if (appConfig != null) {
                            installDomain.tryDownloadWorkshopControllerConfig(
                                templateIndex = appConfig.steamControllerTemplateIndex,
                                configDetails = appConfig.steamControllerConfigDetails,
                                appDirPath = getAppDirPath(appId),
                                configFileName = STEAM_CONTROLLER_CONFIG_FILENAME,
                                fetchPublishedFileDetailsJson = { publishedFileId ->
                                    val requestBody = FormBody.Builder()
                                        .add("itemcount", "1")
                                        .add("publishedfileids[0]", publishedFileId.toString())
                                        .build()

                                    val request = Request.Builder()
                                        .url(
                                            "https://api.steampowered.com/" +
                                                "ISteamRemoteStorage/GetPublishedFileDetails/v1",
                                        )
                                        .post(requestBody)
                                        .build()

                                    Net.http.newCall(request).execute().use { response ->
                                        if (!response.isSuccessful) {
                                            Timber.w(
                                                "Failed to get steam controller config details for $publishedFileId: ${response.code}",
                                            )
                                            return@use null
                                        }
                                        response.body.string()
                                    }
                                },
                                downloadFileBytes = { fileUrl ->
                                    val downloadRequest = Request.Builder()
                                        .url(fileUrl)
                                        .get()
                                        .build()

                                    Net.http.newCall(downloadRequest).execute().use { downloadResponse ->
                                        if (!downloadResponse.isSuccessful) {
                                            Timber.w(
                                                "Failed to download steam controller config from %s: %d",
                                                fileUrl,
                                                downloadResponse.code,
                                            )
                                            return@use null
                                        }
                                        downloadResponse.body.bytes()
                                    }
                                },
                            )
                        }

                        // Signal that no more items will be added
                        depotDownloader.finishAdding()

                        // Start Download
                        depotDownloader.startDownloading()

//                        Timber.Forest.i("Downloading game to " + defaultAppInstallPath)

                        // Wait for completion
                        depotDownloader.getCompletion().await()

                        // Close the downloader
                        depotDownloader.close()

                        // Complete app download
                        if (mainAppDepots.isNotEmpty()) {
                            val mainAppDepotIds = mainAppDepots.keys.sorted()
                            completeAppDownload(
                                di,
                                appId,
                                mainAppDepotIds,
                                mainAppDlcIds,
                                appDirPath,
                            )
                        }

                        // Complete dlc app download
                        calculatedDlcAppIds.forEach { dlcAppId ->
                            val dlcDepots = selectedDepots
                                .filter { it.value.dlcAppId == dlcAppId }
                            val dlcDepotIds = dlcDepots.keys.sorted()
                            completeAppDownload(di, dlcAppId, dlcDepotIds, emptyList(), appDirPath)
                        }

                        // Remove the job here
                        removeDownloadJob(appId)

                        // Remove the downloading app info
                        instance?.libraryDomain?.deleteDownloadingApp(appId)
                    } catch (e: Exception) {
                        Timber.e(e, "Download failed for app $appId")
                        di.persistProgressSnapshot()
                        // Mark all depots as failed
                        selectedDepots.keys.sorted().forEachIndexed { idx, _ ->
                            di.setWeight(idx, 0)
                            di.setProgress(1f, idx)
                        }
                        removeDownloadJob(appId)
                    }
                }
                downloadJob.invokeOnCompletion { throwable ->
                    if (throwable is CancellationException) {
                        Timber.d(throwable, "Download canceled for app $appId")
                        removeDownloadJob(appId)
                    }
                }
                di.setDownloadJob(downloadJob)
            }

            installDomain.downloadJobs[appId] = info
            notifyDownloadStarted(appId)
            return info
        }

        private suspend fun completeAppDownload(
            downloadInfo: DownloadInfo,
            downloadingAppId: Int,
            entitledDepotIds: List<Int>,
            selectedDlcAppIds: List<Int>,
            appDirPath: String,
        ) {
            Timber.i("Item $downloadingAppId download completed, saving database")
            instance?.libraryDomain?.upsertInstalledAppDownloadState(
                appId = downloadingAppId,
                entitledDepotIds = entitledDepotIds,
                selectedDlcAppIds = selectedDlcAppIds,
            )

            // Remove completed appId from downloadInfo.dlcAppIds
            downloadInfo.downloadingAppIds.removeIf { it == downloadingAppId }

            // All downloading appIds are removed
            if (downloadInfo.downloadingAppIds.isEmpty()) {
                // Handle completion: add markers
                withContext(Dispatchers.IO) {
                    MarkerUtils.addMarker(appDirPath, Marker.DOWNLOAD_COMPLETE_MARKER)
                    MarkerUtils.removeMarker(appDirPath, Marker.STEAM_DLL_REPLACED)
                    MarkerUtils.removeMarker(appDirPath, Marker.STEAM_COLDCLIENT_USED)
                }

                // clean up DB record BEFORE notifying UI to avoid stale "Resume" button
                instance?.libraryDomain?.deleteDownloadingApp(downloadInfo.gameId)

                GameGrubApp.events.emit(AndroidEvent.LibraryInstallStatusChanged(downloadInfo.gameId))

                // Clear persisted bytes file on successful completion
                downloadInfo.clearPersistedBytesDownloaded(appDirPath)
            }
        }

        /**
         * Listener for download progress and completion events from DepotDownloader
         */
        private class AppDownloadListener(
            private val downloadInfo: DownloadInfo,
            private val depotIdToIndex: Map<Int, Int>,
        ) : IDownloadListener {
            // Track cumulative uncompressed bytes per depot to calculate deltas
            // (uncompressedBytes from onChunkCompleted is cumulative per depot)
            private val depotCumulativeUncompressedBytes = mutableMapOf<Int, Long>()
            override fun onItemAdded(item: DownloadItem) {
                Timber.d("Item ${item.appId} added to queue")
            }

            override fun onDownloadStarted(item: DownloadItem) {
                Timber.i("Item ${item.appId} download started")
            }

            override fun onDownloadCompleted(item: DownloadItem) {
                Timber.i("Item ${item.appId} download completed")
            }

            override fun onDownloadFailed(item: DownloadItem, error: Throwable) {
                Timber.e(error, "Item ${item.appId} failed to download")
                downloadInfo.failedToDownload()

                // Remove the downloading app info
                runBlocking {
                    instance?.libraryDomain?.deleteDownloadingApp(downloadInfo.gameId)
                }

                removeDownloadJob(downloadInfo.gameId)
                instance?.let { service ->
                    SnackbarManager.show(service.getString(R.string.download_failed_try_again))
                }
            }

            override fun onStatusUpdate(message: String) {
                Timber.d("Download status: $message")
                downloadInfo.updateStatusMessage(message)
            }

            override fun onChunkCompleted(
                depotId: Int,
                depotPercentComplete: Float,
                compressedBytes: Long,
                uncompressedBytes: Long,
            ) {
                !depotCumulativeUncompressedBytes.containsKey(depotId)

                // uncompressedBytes is cumulative per depot, so calculate delta
                val previousBytes = depotCumulativeUncompressedBytes[depotId] ?: 0L
                val deltaBytes = uncompressedBytes - previousBytes
                depotCumulativeUncompressedBytes[depotId] = uncompressedBytes

                if (deltaBytes > 0L) {
                    // Normal case: add the delta
                    downloadInfo.updateBytesDownloaded(deltaBytes, System.currentTimeMillis())
                }

                depotIdToIndex[depotId]?.let { index ->
                    downloadInfo.setProgress(depotPercentComplete, index)
                }

                // Persist progress snapshot
                downloadInfo.persistProgressSnapshot()
            }

            override fun onDepotCompleted(depotId: Int, compressedBytes: Long, uncompressedBytes: Long) {
                Timber.i("Depot $depotId completed (compressed: $compressedBytes, uncompressed: $uncompressedBytes)")

                // Ensure we capture any remaining bytes
                val previousBytes = depotCumulativeUncompressedBytes[depotId] ?: 0L
                val deltaBytes = uncompressedBytes - previousBytes
                depotCumulativeUncompressedBytes[depotId] = uncompressedBytes

                if (deltaBytes > 0L) {
                    downloadInfo.updateBytesDownloaded(deltaBytes, System.currentTimeMillis())
                }

                depotIdToIndex[depotId]?.let { index ->
                    downloadInfo.setProgress(1f, index)
                }

                // Persist progress snapshot
                downloadInfo.persistProgressSnapshot()
            }
        }

        fun getWindowsLaunchInfos(appId: Int): List<LaunchInfo> {
            return getAppInfoOf(appId)?.let { appInfo ->
                appInfo.config.launch.filter { launchInfo ->
                    // since configOS was unreliable and configArch was even more unreliable
                    launchInfo.executable.endsWith(".exe")
                }
            }.orEmpty()
        }

        suspend fun notifyRunningProcesses(vararg gameProcesses: GameProcessInfo) =
            withContext(Dispatchers.IO) {
                val svc = instance ?: return@withContext
                svc.sessionDomain.notifyRunningProcesses(
                    gameProcesses = gameProcesses,
                    isConnected = isConnected,
                    resolvePlayedInfo = { gameProcess ->
                        getAppInfoOf(gameProcess.appId)?.let { appInfo ->
                            getPkgInfoOf(gameProcess.appId)?.let { pkgInfo ->
                                appInfo.branches[gameProcess.branch]?.let { branch ->
                                    val processId = gameProcess.processes
                                        .firstOrNull { it.parentIsSteam }
                                        ?.processId
                                        ?: gameProcess.processes.firstOrNull()?.processId
                                        ?: 0

                                    val userAccountId = userSteamId?.accountID?.toInt() ?: return@let null
                                    GamePlayedInfo(
                                        gameId = gameProcess.appId.toLong(),
                                        processId = processId,
                                        ownerId = if (pkgInfo.ownerAccountId.contains(userAccountId)) {
                                            userAccountId
                                        } else {
                                            pkgInfo.ownerAccountId.first()
                                        },
                                        // TODO: figure out what this is and un-hardcode
                                        launchSource = 100,
                                        gameBuildId = branch.buildId.toInt(),
                                        processIdList = gameProcess.processes,
                                    )
                                }
                            }
                        }
                    },
                    notifyGamesPlayed = { gamesPlayed ->
                        svc._steamApps?.notifyGamesPlayed(
                            gamesPlayed = gamesPlayed,
                            clientOsType = EOSType.AndroidUnknown,
                        )
                    },
                )
            }

        fun beginLaunchApp(
            appId: Int,
            parentScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
            ignorePendingOperations: Boolean = false,
            preferredSave: SaveLocation = SaveLocation.None,
            prefixToPath: (String) -> String,
            isOffline: Boolean = false,
            onProgress: ((message: String, progress: Float) -> Unit)? = null,
        ): Deferred<PostSyncInfo> = instance?.sessionDomain?.beginLaunchApp(
            appId = appId,
            isOffline = isOffline,
            isConnected = isConnected,
            ignorePendingOperations = ignorePendingOperations,
            parentScope = parentScope,
            syncUserFiles = {
                PrefManager.clientId?.let { clientId ->
                    instance?.let { steamInstance ->
                        getAppInfoOf(appId)?.let { appInfo ->
                            steamInstance._steamCloud?.let { steamCloud ->
                                SteamAutoCloud.syncUserFiles(
                                    appInfo = appInfo,
                                    clientId = clientId,
                                    steamInstance = steamInstance,
                                    steamCloud = steamCloud,
                                    preferredSave = preferredSave,
                                    parentScope = parentScope,
                                    prefixToPath = prefixToPath,
                                    onProgress = onProgress,
                                ).await()
                            }
                        }
                    }
                }
            },
            signalLaunchIntent = {
                PrefManager.clientId?.let { clientId ->
                    instance?.let { steamInstance ->
                        val steamCloud = steamInstance._steamCloud
                        if (steamCloud == null) {
                            return@let LaunchIntentResult()
                        }
                        Timber.i(
                            "Signaling app launch:\n\tappId: %d\n\tclientId: %s\n\tosType: %s",
                            appId,
                            PrefManager.clientId,
                            EOSType.AndroidUnknown,
                        )

                        val pendingRemoteOperations = steamCloud.signalAppLaunchIntent(
                            appId = appId,
                            clientId = clientId,
                            machineName = steamInstance.accountDomain.deviceIdentityManager.getMachineName(steamInstance),
                            ignorePendingOperations = ignorePendingOperations,
                            osType = EOSType.AndroidUnknown,
                        ).await()

                        return@let LaunchIntentResult(
                            pendingRemoteOperations = pendingRemoteOperations,
                            hasAppSessionActiveOperation = pendingRemoteOperations.any {
                                it.operation ==
                                    SteammessagesClientObjects.ECloudPendingRemoteOperation
                                        .k_ECloudPendingRemoteOperationAppSessionActive
                            },
                        )
                    }
                }

                LaunchIntentResult()
            },
            kickExistingPlayingSession = {
                instance?._steamUser?.kickPlayingSession()
            },
        ) ?: parentScope.async { PostSyncInfo(SyncResult.UnknownFail) }

        fun forceSyncUserFiles(
            appId: Int,
            prefixToPath: (String) -> String,
            preferredSave: SaveLocation = SaveLocation.None,
            parentScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
            overrideLocalChangeNumber: Long? = null,
        ): Deferred<PostSyncInfo> = instance?.cloudStatsDomain?.forceSyncUserFiles(
            appId = appId,
            prefixToPath = prefixToPath,
            preferredSave = preferredSave,
            parentScope = parentScope,
            overrideLocalChangeNumber = overrideLocalChangeNumber,
        ) ?: parentScope.async { PostSyncInfo(SyncResult.UnknownFail) }

        fun closeApp(context: Context, appId: Int, isOffline: Boolean, prefixToPath: (String) -> String): Deferred<Unit> {
            val svc = instance
            val serviceSessionDomain = svc?.sessionDomain
            if (svc == null || serviceSessionDomain == null) {
                return CoroutineScope(Dispatchers.IO).async { }
            }

            return serviceSessionDomain.closeApp(
                appId = appId,
                isOffline = isOffline,
                isConnected = isConnected,
                parentScope = svc.scope,
                syncAchievements = {
                    syncAchievementsFromGoldberg(context, appId)
                },
                syncUserFiles = {
                    PrefManager.clientId?.let { clientId ->
                        instance?.let { steamInstance ->
                            getAppInfoOf(appId)?.let { appInfo ->
                                steamInstance._steamCloud?.let { steamCloud ->
                                    SteamAutoCloud.syncUserFiles(
                                        appInfo = appInfo,
                                        clientId = clientId,
                                        steamInstance = steamInstance,
                                        steamCloud = steamCloud,
                                        parentScope = svc.scope,
                                        prefixToPath = prefixToPath,
                                    ).await()
                                }
                            }
                        }
                    }
                },
                signalExitSyncDone = { postSyncInfo ->
                    PrefManager.clientId?.let { clientId ->
                        instance?._steamCloud?.signalAppExitSyncDone(
                            appId = appId,
                            clientId = clientId,
                            uploadsCompleted = postSyncInfo?.uploadsCompleted == true,
                            uploadsRequired = postSyncInfo?.uploadsRequired == false,
                        )
                    }
                },
            )
        }

        data class FileChanges(
            val filesDeleted: List<UserFileInfo>,
            val filesModified: List<UserFileInfo>,
            val filesCreated: List<UserFileInfo>,
        )

        private fun login(
            username: String,
            accessToken: String? = null,
            refreshToken: String? = null,
            password: String? = null,
            rememberSession: Boolean = true,
            twoFactorAuth: String? = null,
            emailAuth: String? = null,
            clientId: Long? = null,
        ) {
            val svc = requireInstance()
            val steamUser = svc._steamUser
                ?: throw IllegalStateException("SteamUser not initialized")

            PrefManager.username = username

            if ((password != null && rememberSession) || refreshToken != null) {
                if (accessToken != null) PrefManager.accessToken = accessToken
                if (refreshToken != null) PrefManager.refreshToken = refreshToken
                if (clientId != null) PrefManager.clientId = clientId
            }

            GameGrubApp.events.emit(SteamEvent.LogonStarted(username))

            steamUser.logOn(
                LogOnDetails(
                    username = svc.accountDomain.userManager.removeSpecialChars(username).trim(),
                    password = password?.let { svc.accountDomain.userManager.removeSpecialChars(it).trim() },
                    shouldRememberPassword = rememberSession,
                    twoFactorCode = twoFactorAuth,
                    authCode = emailAuth,
                    accessToken = refreshToken,
                    loginID = svc.accountDomain.deviceIdentityManager.getUniqueDeviceId(svc),
                    machineName = svc.accountDomain.deviceIdentityManager.getMachineName(svc),
                    chatMode = ChatMode.NEW_STEAM_CHAT,
                ),
            )
        }

        suspend fun startLoginWithCredentials(
            username: String,
            password: String,
            rememberSession: Boolean,
            authenticator: IAuthenticator,
        ) = requireInstance().accountDomain.authService.loginWithCredentials(
            username = username,
            password = password,
            rememberSession = rememberSession,
            authenticator = authenticator,
        )

        suspend fun startLoginWithQr() = requireInstance().accountDomain.authService.loginWithQr()

        fun completeLoginWithAuthTokens(
            username: String,
            accessToken: String,
            refreshToken: String,
            clientId: Long,
            rememberSession: Boolean = true,
        ) {
            login(
                username = username,
                accessToken = accessToken,
                refreshToken = refreshToken,
                rememberSession = rememberSession,
                clientId = clientId,
            )
        }

        fun stopLoginWithQr() {
            requireInstance().accountDomain.authService.cancelQrLogin()
        }

        fun stop() {
            instance?.let { steamInstance ->
                steamInstance.scope.launch {
                    steamInstance.stop()
                }
            }
        }

        fun logOut() {
            requireInstance().accountDomain.authService.logOut()
            isLoggingOut = true
        }

        private fun clearUserData(clearCloudSyncState: Boolean = false) {
            PrefManager.clearSteamSessionPreferences()

            clearDatabase(clearCloudSyncState = clearCloudSyncState)
        }

        private fun shouldClearUserDataForLoggedOnFailure(result: EResult): Boolean = when (result) {
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

        fun clearDatabase(clearCloudSyncState: Boolean = false) {
            val svc = instance ?: return
            svc.scope.launch {
                svc.db.withTransaction {
                    svc.libraryDomain.clearAllLibraryData()
                    if (clearCloudSyncState) {
                        svc.libraryDomain.deleteAllPicsChanges()
                    }
                    svc.libraryDomain.clearDownloadState()
                    svc.sessionDomain.clearAllTickets()
                }
            }
        }

        private fun cancelLongLivedSteamJobs() {
            // Cancel previous continuous jobs or else they will continue to run even after logout
            instance?.picsSyncDomain?.cancelPicsJobs()
        }

        private fun performLogOffDuties() {
            val username = PrefManager.username

            clearUserData(clearCloudSyncState = true)

            val event = SteamEvent.LoggedOut(username)
            GameGrubApp.events.emit(event)

            cancelLongLivedSteamJobs()
        }

        suspend fun getOwnedGames(friendID: Long): List<OwnedGames> = instance?.libraryDomain?.getOwnedGames(friendID) ?: emptyList()

        // Add helper to detect if any downloads or cloud sync are in progress
        fun hasActiveOperations(): Boolean {
            val anySyncInProgress = instance?.sessionDomain?.hasActiveSessionOperations() == true
            return anySyncInProgress || installDomain.downloadJobs.values.any { it.getProgress() < 1f }
        }

        // Should service auto-stop when idle (backgrounded)?
        var autoStopWhenIdle: Boolean
            get() = instance?.sessionDomain?.getAutoStopWhenIdle() ?: false
            set(value) {
                instance?.sessionDomain?.setAutoStopWhenIdle(value)
            }

        suspend fun isUpdatePending(
            appId: Int,
            branch: String = "public",
        ): Boolean = withContext(Dispatchers.IO) {
            // Don't try if there's no internet
            if (!isConnected) return@withContext false

            val steamApps = instance?._steamApps ?: return@withContext false

            // ── 1. Fetch the latest app header from Steam (PICS).
            val pics = steamApps.picsGetProductInfo(
                apps = listOf(PICSRequest(id = appId)),
                packages = emptyList(),
            ).await()

            val remoteAppInfo = pics.results
                .firstOrNull()
                ?.apps
                ?.values
                ?.firstOrNull()
                ?: return@withContext false // nothing returned ⇒ treat as up-to-date

            val remoteSteamApp = remoteAppInfo.keyValues.generateSteamApp()
            val localSteamApp = getAppInfoOf(appId) ?: return@withContext true // not cached yet

            // ── 2. Compare manifest IDs of the depots we actually install.
            getDownloadableDepots(appId).keys.any { depotId ->
                val remoteManifest = remoteSteamApp.depots[depotId]?.manifests?.get(branch)
                val localManifest = localSteamApp.depots[depotId]?.manifests?.get(branch)
                // If remote manifest is null, skip this depot (hack for Castle Crashers)
                if (remoteManifest == null) return@any false
                remoteManifest.gid != localManifest?.gid
            }
        }

        @Suppress("UNUSED")
        suspend fun checkDlcOwnershipViaPICSBatch(dlcAppIds: Set<Int>): Set<Int> {
            val steamApps = instance?._steamApps ?: return emptySet()

            return catalogManager.checkDlcOwnershipViaPICSBatch(
                dlcAppIds = dlcAppIds,
                loadAccessTokens = { appIds ->
                    val tokens = steamApps.picsGetAccessTokens(
                        appIds = appIds,
                        packageIds = emptyList(),
                    ).await()

                    Timber.d("Access tokens response:")
                    Timber.d("  - Granted tokens: ${tokens.appTokens.keys}")
                    Timber.d("  - Denied tokens: ${tokens.appTokensDenied}")
                    tokens.appTokens
                },
                loadPicsProductInfo = { requests ->
                    Timber.d("Querying PICS chunk with ${requests.size} apps")
                    val callback = steamApps.picsGetProductInfo(
                        apps = requests,
                        packages = emptyList(),
                    ).await()

                    val returnedAppIds = callback.results.flatMap { picsCallback ->
                        Timber.d("  PICS result: ${picsCallback.apps.keys.size} apps returned")
                        picsCallback.apps.keys
                    }.toSet()
                    returnedAppIds
                },
            ).also { ownedIds ->
                Timber.i("Final owned DLC appIds: $ownedIds")
                Timber.i("Total owned: ${ownedIds.size} out of ${dlcAppIds.size} checked")
            }
        }

        suspend fun generateAchievements(appId: Int, configDirectory: String) {
            val mgr = requireInstance().cloudStatsDomain
            mgr.generateAchievements(appId, configDirectory)
            cachedAchievements = mgr.cachedAchievements.value
            cachedAchievementsAppId = mgr.cachedAchievementsAppId.value
        }

        fun clearCachedAchievements() {
            requireInstance().cloudStatsDomain.clearCachedAchievements()
            cachedAchievements = null
            cachedAchievementsAppId = null
        }

        fun getGseSaveDirs(context: Context, appId: Int): List<File> {
            return instance?.cloudStatsDomain?.getGseSaveDirs(context, appId) ?: emptyList()
        }

        suspend fun syncAchievementsFromGoldberg(context: Context, appId: Int) {
            instance?.cloudStatsDomain?.syncAchievementsFromGoldberg(context, appId)
        }

        @Suppress("UNUSED")
        suspend fun storeAchievementUnlocks(
            appId: Int,
            configDirectory: String,
            unlockedNames: Set<String>,
            gseStatsDir: File,
        ): Result<Unit> {
            return instance?.cloudStatsDomain?.storeAchievementUnlocks(
                appId = appId,
                configDirectory = configDirectory,
                unlockedNames = unlockedNames,
                gseStatsDir = gseStatsDir,
            ) ?: Result.failure(IllegalStateException("Service not available"))
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this


        // JavaSteam logger CME hot-fix
        runCatching {
            val clazz = Class.forName("in.dragonbra.javasteam.util.log.LogManager")
            val field = clazz.getDeclaredField("LOGGERS").apply { isAccessible = true }
            field.set(
                null,
                ConcurrentHashMap<Any, Any>(), // replaces the HashMap
            )
        }

        GameGrubApp.events.on<AndroidEvent.EndProcess, Unit>(onEndProcess)

        // clear stale download records (completed games) but keep interrupted ones (preserves DLC selection)
        scope.launch {
            for (record in libraryDomain.getAllDownloadingApps()) {
                if (isAppInstalled(record.appId)) {
                    libraryDomain.deleteDownloadingApp(record.appId)
                }
            }
        }

        notificationHelper = NotificationHelper(applicationContext)

        // pause downloads when Wi-Fi/Ethernet connectivity changes
        connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onLost(p0: Network) = checkAndPauseDownloads()
            override fun onCapabilitiesChanged(p0: Network, p1: NetworkCapabilities) = checkAndPauseDownloads()

            // query ConnectivityManager directly (not NetworkMonitor) to avoid
            // callback ordering race between our two separate registrations.
            // no VPN exclusion needed here — activeNetwork is always fresh
            // (stale-VPN guard is only needed in NetworkMonitor's multi-network tracking)
            private fun hasActiveWifiOrEthernet(): Boolean {
                val activeNet = connectivityManager.activeNetwork ?: return false
                val caps = connectivityManager.getNetworkCapabilities(activeNet) ?: return false
                return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            }

            // no transition guard needed — if Wi-Fi already down, downloadJobs is empty (no-op)
            private fun checkAndPauseDownloads() {
                if (PrefManager.downloadOnWifiOnly && !hasActiveWifiOrEthernet()) {
                    for ((appId, info) in downloadJobs.entries.toList()) {
                        Timber.d("Pausing download for $appId — WiFi/Ethernet lost")
                        info.cancel()
                        GameGrubApp.events.emit(AndroidEvent.DownloadPausedDueToConnectivity(appId))
                        removeDownloadJob(appId)
                    }
                    notificationHelper.notify(getString(R.string.download_paused_wifi))
                }
            }
        }
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)

        // To view log messages in android logcat properly
        LogManager.addListener(logger)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Notification intents
        when (intent?.action) {
            NotificationHelper.ACTION_EXIT -> {
                Timber.d("Exiting app via notification intent")

                val event = AndroidEvent.EndProcess
                GameGrubApp.events.emit(event)

                return START_NOT_STICKY
            }
        }

        if (!isRunning) {
            val serverListPath = SteamPaths.serverListPath
            Timber.i("Using server list path: $serverListPath")

            val configuration = SteamConfiguration.create {
                it.withProtocolTypes(PROTOCOL_TYPES)
                it.withCellID(PrefManager.cellId)
                it.withServerListProvider(FileServerListProvider(File(serverListPath)))
                it.withConnectionTimeout(60000L)
                it.withHttpClient(
                    OkHttpClient.Builder()
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .readTimeout(60, TimeUnit.SECONDS)
                        .writeTimeout(30, TimeUnit.SECONDS)
                        .pingInterval(15, TimeUnit.SECONDS) // keep WebSocket alive during idle
                        .build(),
                )
            }

            // create our steam client instance
            steamClient = SteamClient(configuration).apply {
                // remove callbacks we're not using.
                removeHandler(SteamGameServer::class.java)
                removeHandler(SteamMasterServer::class.java)
                removeHandler(SteamWorkshop::class.java)
                removeHandler(SteamScreenshots::class.java)
            }

            // create the callback manager which will route callbacks to function calls
            callbackManager = CallbackManager(steamClient!!)

            // get the different handlers to be used throughout the service
            _steamUser = steamClient!!.getHandler(SteamUser::class.java)
            _steamApps = steamClient!!.getHandler(SteamApps::class.java)
            _steamFriends = steamClient!!.getHandler(SteamFriends::class.java)
            _steamCloud = steamClient!!.getHandler(SteamCloud::class.java)
            _steamUserStats = steamClient!!.getHandler(SteamUserStats::class.java)

            steamClientProvider.client = steamClient
            steamClientProvider.steamUser = _steamUser
            steamClientProvider.steamApps = _steamApps
            steamClientProvider.steamCloud = _steamCloud
            steamClientProvider.steamUserStats = _steamUserStats

            _unifiedFriends = SteamUnifiedFriends(this)
            _steamFamilyGroups = steamClient!!.getHandler<SteamUnifiedMessages>()!!.createService<FamilyGroups>()

            // subscribe to the callbacks we are interested in
            with(callbackSubscriptions) {
                with(callbackManager!!) {
                    add(subscribe(ConnectedCallback::class.java, ::onConnected))
                    add(
                        subscribe(
                            DisconnectedCallback::class.java,
                            ::onDisconnected,
                        ),
                    )
                    add(subscribe(LoggedOnCallback::class.java, ::onLoggedOn))
                    add(subscribe(LoggedOffCallback::class.java, ::onLoggedOff))
                    add(
                        subscribe(
                            PersonaStateCallback::class.java,
                            ::onPersonaStateReceived,
                        ),
                    )
                    add(subscribe(LicenseListCallback::class.java, ::onLicenseList))
                    add(
                        subscribe(
                            PlayingSessionStateCallback::class.java,
                            ::onPlayingSessionState,
                        ),
                    )
                }
            }

            isRunning = true

            // we should use Dispatchers.IO here since we are running a sleeping/blocking function
            // "The idea is that the IO dispatcher spends a lot of time waiting (IO blocked),
            // while the Default dispatcher is intended for CPU intensive tasks, where there
            // is little or no sleep."
            // source: https://stackoverflow.com/a/59040920
            scope.launch {
                while (isRunning) {
                    // logD("runWaitCallbacks")

                    try {
                        callbackManager!!.runWaitCallbacks(1000L)
                    } catch (e: Exception) {
                        Timber.e("runWaitCallbacks failed: $e")
                    }
                }
            }

            connectToSteam()
        }

        val notification = notificationHelper.createForegroundNotification("Running...")
        startForeground(1, notification)

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        // Persist download progress for all active downloads
        // This is a safety net for OS kills (unlikely but possible)
        downloadJobs.values.forEach { downloadInfo ->
            downloadInfo.persistProgressSnapshot()
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        notificationHelper.cancel()

        connectivityManager.unregisterNetworkCallback(networkCallback)

        scope.launch { stop() }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun connectToSteam() {
        CoroutineScope(Dispatchers.Default).launch {
            // this call errors out if run on the main thread
            steamClient!!.connect()

            delay(5000)

            if (!isConnected) {
                Timber.w("Failed to connect to Steam, marking endpoint bad and force disconnecting")

                try {
                    steamClient!!.servers.tryMark(steamClient!!.currentEndpoint, PROTOCOL_TYPES, ServerQuality.BAD)
                } catch (_: NullPointerException) {
                    // I don't care
                } catch (e: Exception) {
                    Timber.e(e, "Failed to mark endpoint as bad:")
                }

                try {
                    steamClient!!.disconnect()
                } catch (_: NullPointerException) {
                    // I don't care
                } catch (e: Exception) {
                    Timber.e(e, "There was an issue when disconnecting:")
                }
            }
        }
    }

    private suspend fun stop() {
        Timber.i("Stopping Steam service")
        if (steamClient != null && steamClient!!.isConnected) {
            isStopping = true

            steamClient!!.disconnect()

            while (isStopping) {
                delay(200L)
            }

            // the reason we don't clearValues() here is because the onDisconnect
            // callback does it for us
        } else {
            clearValues()
        }
    }

    private fun clearValues() {
        accountDomain.setLoginResult(LoginResult.Failed)
        accountDomain.setLoggingOut(false)
        isRunning = false
        isConnected = false
        isWaitingForQRAuth = false

        steamClient = null
        _steamUser = null
        _steamApps = null
        _steamFriends = null
        _steamCloud = null

        steamClientProvider.client = null
        steamClientProvider.steamUser = null
        steamClientProvider.steamApps = null
        steamClientProvider.steamCloud = null
        steamClientProvider.steamUserStats = null

        callbackSubscriptions.forEach { it.close() }
        callbackSubscriptions.clear()
        callbackManager = null

        _unifiedFriends?.close()
        _unifiedFriends = null

        reconnectJob?.cancel()
        isStopping = false
        retryAttempt = 0

        GameGrubApp.events.off<AndroidEvent.EndProcess, Unit>(onEndProcess)
        GameGrubApp.events.clearAllListenersOf<SteamEvent<Any>>()

        LogManager.removeListener(logger)
    }

    private fun reconnect() {
        notificationHelper.notify("Retrying...")

        isConnected = false

        val event = SteamEvent.Disconnected(isTerminal = false)
        GameGrubApp.events.emit(event)

        steamClient!!.disconnect()
    }

    // region [REGION] callbacks
    @Suppress("UNUSED_PARAMETER", "unused")
    private fun onConnected(callback: ConnectedCallback) {
        Timber.i("Connected to Steam")

        reconnectJob?.cancel()
        retryAttempt = 0
        isConnected = true

        var isAutoLoggingIn = false

        if (PrefManager.username.isNotEmpty() && PrefManager.refreshToken.isNotEmpty()) {
            isAutoLoggingIn = true

            login(
                username = PrefManager.username,
                refreshToken = PrefManager.refreshToken,
                rememberSession = true,
            )
        }

        val event = SteamEvent.Connected(isAutoLoggingIn)
        GameGrubApp.events.emit(event)
    }

    private fun onDisconnected(callback: DisconnectedCallback) {
        Timber.i("Disconnected from Steam. User initiated: ${callback.isUserInitiated}")

        isConnected = false

        if (!isStopping && retryAttempt < MAX_RETRY_ATTEMPTS) {
            retryAttempt++
            val backoffMs = (1000L * minOf(1 shl (retryAttempt - 1), 60)).coerceAtMost(60_000L)

            Timber.w("Attempting to reconnect (retry $retryAttempt) after ${backoffMs}ms")

            val event = SteamEvent.RemotelyDisconnected
            GameGrubApp.events.emit(event)

            reconnectJob = scope.launch {
                delay(backoffMs)
                if (isRunning && !isStopping) connectToSteam()
            }
        } else {
            // only terminal when retries exhausted, not when user/system stopped the service
            val event = SteamEvent.Disconnected(isTerminal = !isStopping)
            GameGrubApp.events.emit(event)

            clearValues()

            stopSelf()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private fun onLoggedOn(callback: LoggedOnCallback) {
        Timber.i("Logged onto Steam: ${callback.result}")

        if (userSteamId?.isValid == true) {
            if (PrefManager.steamUserAccountId != userSteamId!!.accountID.toInt()) {
                PrefManager.steamUserAccountId = userSteamId!!.accountID.toInt()
                Timber.d("Saving logged in Steam accountID ${userSteamId!!.accountID.toInt()}")
            }
            val steamId64 = userSteamId!!.convertToUInt64()
            if (PrefManager.steamUserSteamId64 != steamId64) {
                PrefManager.steamUserSteamId64 = steamId64
                Timber.d("Saving logged in Steam ID64 $steamId64")
            }
        }

        when (callback.result) {
            EResult.TryAnotherCM -> {
                accountDomain.setLoginResult(LoginResult.Failed)
                reconnect()
            }

            EResult.OK -> {
                // save the current cellid somewhere. if we lose our saved server list, we can use this when retrieving
                // servers from the Steam Directory.
                if (!PrefManager.cellIdManuallySet) {
                    PrefManager.cellId = callback.cellID
                }

                // retrieve persona data of logged in user
                scope.launch { requestUserPersona() }

                // Request family share info if we have a familyGroupId.
                if (callback.familyGroupId != 0L) {
                    scope.launch {
                        val request = SteammessagesFamilygroupsSteamclient.CFamilyGroups_GetFamilyGroup_Request.newBuilder().apply {
                            familyGroupid = callback.familyGroupId
                        }.build()

                        _steamFamilyGroups!!.getFamilyGroup(request).await().let {
                            if (it.result != EResult.OK) {
                                Timber.w("An error occurred loading family group info.")
                                return@launch
                            }

                            val response = it.body

                            Timber.i("Found family share: ${response.name}, with ${response.membersCount} members.")

                            response.membersList.forEach { member ->
                                val accountID = SteamID(member.steamid).accountID.toInt()
                                accountDomain.addFamilyGroupMember(accountID)
                            }
                        }
                    }
                }

                picsSyncDomain.continuousPICSChangesChecker(scope, _steamApps)
                picsSyncDomain.continuousPICSGetProductInfo(scope, _steamApps)

                // Tell steam we're online, this allows friends to update.
                _steamFriends?.setPersonaState(PrefManager.personaState)

                notificationHelper.notify("Connected")

                accountDomain.setLoginResult(LoginResult.Success)
            }

            else -> {
                if (shouldClearUserDataForLoggedOnFailure(callback.result)) {
                    clearUserData()
                }

                accountDomain.setLoginResult(LoginResult.Failed)

                reconnect()
            }
        }

        val event = SteamEvent.LogonEnded(PrefManager.username, accountDomain.loginResult.value)
        GameGrubApp.events.emit(event)
    }

    private fun onLoggedOff(callback: LoggedOffCallback) {
        Timber.i("Logged off of Steam: ${callback.result}")

        notificationHelper.notify("Disconnected...")

        if (isLoggingOut) {
            performLogOffDuties()

            scope.launch { stop() }
        } else if (callback.result == EResult.LogonSessionReplaced) {
            // Unexpected session replacement should not wipe persisted Steam state.
            cancelLongLivedSteamJobs()
            scope.launch { stop() }
        } else if (callback.result == EResult.LoggedInElsewhere) {
            // received when a client runs an app and wants to forcibly close another
            // client running an app
            val event = SteamEvent.ForceCloseApp
            GameGrubApp.events.emit(event)

            reconnect()
        } else {
            reconnect()
        }
    }

    private fun onPlayingSessionState(callback: PlayingSessionStateCallback) {
        Timber.d("onPlayingSessionState called with isPlayingBlocked = %s", callback.isPlayingBlocked)
        sessionDomain.updatePlayingSessionState(callback.isPlayingBlocked)
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun onPersonaStateReceived(callback: PersonaStateCallback) {
        // Ignore accounts that arent individuals
        if (!callback.friendId.isIndividualAccount) {
            return
        }

        // Ignore states where the name is blank.
        if (callback.playerName.isEmpty()) {
            return
        }

        // Timber.d("Persona state received: ${callback.name}")

        scope.launch {
            db.withTransaction {
                // Send off an event if we change states.
                if (callback.friendId == steamClient!!.steamID) {
                    Timber.d("Local persona state received: %s", callback.playerName)

                    val avatarHash = callback.avatarHash.toHexString()
                    val playerName = callback.playerName

                    // When connected, callback may return Offline due to missing Status flag in request.
                    // Trust PrefManager.personaState (user's chosen state) in that case.
                    val state = if (callback.personaState == EPersonaState.Offline && isConnected) {
                        PrefManager.personaState
                    } else {
                        callback.personaState
                    }

                    val resolvedGameName = libraryDomain.findApp(callback.gamePlayedAppId)?.name ?: callback.gameName

                    accountDomain.updateLocalPersona {
                        it.copy(
                            avatarHash = avatarHash,
                            name = playerName,
                            state = state,
                            gameAppID = callback.gamePlayedAppId,
                            gameName = resolvedGameName,
                        )
                    }

                    accountDomain.userManager.cachePersona(name = playerName, avatarHash = avatarHash)

                    val event = SteamEvent.PersonaStateReceived(accountDomain.localPersona.value)
                    GameGrubApp.events.emit(event)
                }
            }
        }
    }

    private fun onLicenseList(callback: LicenseListCallback) {
        if (callback.result != EResult.OK) {
            Timber.w("Failed to get License list")
            return
        }

        Timber.i("Received License List ${callback.result}, size: ${callback.licenseList.size}")

        scope.launch {
            val packageRequests = mutableListOf<PICSRequest>()

            db.withTransaction {
                val syncResult = libraryDomain.syncLicensesForPics(
                    callbackLicenses = callback.licenseList,
                    preferredOwnerAccountId = userSteamId?.accountID?.toInt(),
                )

                if (syncResult.addedCount > 0) {
                    Timber.i("Adding ${syncResult.addedCount} licenses")
                }
                if (syncResult.removedCount > 0) {
                    Timber.i("Removing ${syncResult.removedCount} (stale) licenses")
                }

                packageRequests.addAll(syncResult.packageRequests)
            }

            packageRequests
                .chunked(MAX_PICS_BUFFER)
                .forEach { chunk ->
                    Timber.d("onLicenseList: Queueing ${chunk.size} package(s) for PICS")
                    picsSyncDomain.packagePicsChannel.send(chunk)
                }
        }
    }

    override fun onChanged(qrAuthSession: QrAuthSession?) {
        qrAuthSession?.let { qr ->
            if (!BuildConfig.DEBUG) {
                Timber.d("QR code changed -> ${qr.challengeUrl}")
            }

            val event = SteamEvent.QrChallengeReceived(qr.challengeUrl)
            GameGrubApp.events.emit(event)
        } ?: run { Timber.w("QR challenge url was null") }
    }
    // endregion

    suspend fun getEncryptedAppTicket(appId: Int): ByteArray? =
        requireInstance().sessionDomain.getEncryptedAppTicket(appId)

    suspend fun getEncryptedAppTicketBase64(appId: Int): String? =
        requireInstance().sessionDomain.getEncryptedAppTicketBase64(appId)
}
