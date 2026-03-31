package app.gamegrub.service.steam.domain

import android.content.Context
import app.gamegrub.GameGrubApp
import app.gamegrub.NetworkMonitor
import app.gamegrub.PrefManager
import app.gamegrub.data.DepotInfo
import app.gamegrub.data.DownloadInfo
import app.gamegrub.data.ManifestInfo
import app.gamegrub.data.SteamApp
import app.gamegrub.data.SteamControllerConfigDetail
import app.gamegrub.enums.Marker
import app.gamegrub.events.AndroidEvent
import app.gamegrub.service.steam.SteamPaths
import app.gamegrub.service.steam.SteamService
import app.gamegrub.service.steam.managers.SteamCatalogManager
import app.gamegrub.service.steam.managers.SteamControllerTemplateRoutingManager
import app.gamegrub.service.steam.managers.SteamInputManager
import app.gamegrub.service.steam.managers.SteamInstallManager
import app.gamegrub.service.steam.managers.SteamInstalledExeManager
import app.gamegrub.utils.network.Net
import app.gamegrub.utils.storage.MarkerUtils
import com.winlator.container.ContainerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import `in`.dragonbra.javasteam.depotdownloader.DepotDownloader
import `in`.dragonbra.javasteam.depotdownloader.IDownloadListener
import `in`.dragonbra.javasteam.depotdownloader.data.AppItem
import `in`.dragonbra.javasteam.depotdownloader.data.DownloadItem
import `in`.dragonbra.javasteam.steam.steamclient.SteamClient
import `in`.dragonbra.javasteam.types.FileData
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Request
import timber.log.Timber

@Singleton
class SteamInstallDomain @Inject constructor(
    val inputManager: SteamInputManager,
    val libraryDomain: SteamLibraryDomain,
    @ApplicationContext private val context: Context,
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val downloadJobs = ConcurrentHashMap<Int, DownloadInfo>()

    fun getAppDownloadInfo(appId: Int): DownloadInfo? = downloadJobs[appId]

    fun notifyDownloadStarted(appId: Int) {
        GameGrubApp.events.emit(AndroidEvent.DownloadStatusChanged(appId, true))
    }

    private fun notifyDownloadStopped(appId: Int) {
        GameGrubApp.events.emit(AndroidEvent.DownloadStatusChanged(appId, false))
    }

    fun removeDownloadJob(appId: Int) {
        val removed = downloadJobs.remove(appId)
        if (removed != null) {
            notifyDownloadStopped(appId)
        }
    }

    /**
     * Entry point for download - determines download strategy based on current state
     */
    fun downloadApp(appId: Int): DownloadInfo? {
        val currentDownloadInfo = downloadJobs[appId]
        if (currentDownloadInfo != null) {
            return downloadApp(appId, currentDownloadInfo.downloadingAppIds, isUpdateOrVerify = false)
        }

        val downloadingAppInfo = runBlocking { libraryDomain.getDownloadingAppInfoOf(appId) }
        if (downloadingAppInfo != null) {
            return downloadApp(appId, downloadingAppInfo.dlcAppIds, isUpdateOrVerify = false)
        }

        // Otherwise it is verifying files
        val dlcAppIds = runBlocking {
            libraryDomain.getInstalledApp(appId)?.downloadedDepots?.orEmpty()?.toMutableList()
                ?: mutableListOf()
        }

        runBlocking {
            libraryDomain.getDownloadableDlcAppsOf(appId)?.forEach { dlcApp ->
                val installedDlcApp = runBlocking { libraryDomain.getAppInfoOf(dlcApp.id) }
                if (installedDlcApp != null) {
                    dlcAppIds.add(installedDlcApp.id)
                }
            }
        }

        return downloadApp(appId, dlcAppIds, isUpdateOrVerify = true)
    }

    /**
     * Download app with specific DLC list
     */
    fun downloadApp(appId: Int, dlcAppIds: List<Int>, isUpdateOrVerify: Boolean): DownloadInfo? {
        if (!checkWifiOrNotify()) return null
        val appInfo = runBlocking { libraryDomain.getAppInfoOf(appId) } ?: return null

        val container = ContainerManager(context).getContainerById("STEAM_$appId")
        val containerLanguage = container?.language ?: PrefManager.containerLanguage

        Timber.tag("SteamService").d("downloadApp: downloading app $appId with language $containerLanguage")

        val depots = getDownloadableDepots(appId, containerLanguage)
        return downloadApp(
            appId = appId,
            downloadableDepots = depots,
            userSelectedDlcAppIds = dlcAppIds,
            branch = "public",
            containerLanguage = containerLanguage,
            isUpdateOrVerify = isUpdateOrVerify,
        )
    }

    private fun checkWifiOrNotify(): Boolean {
        if (PrefManager.downloadOnWifiOnly && !hasWifiOrEthernet()) {
            Timber.w("checkWifiOrNotify: no wifi and wifi-only download enabled")
            return false
        }
        return true
    }

    private fun hasWifiOrEthernet(): Boolean {
        return NetworkMonitor.hasWifiOrEthernet.value
    }

    fun getDownloadableDepots(appId: Int, preferredLanguage: String): Map<Int, DepotInfo> {
        val appInfo = runBlocking { libraryDomain.getAppInfoOf(appId) } ?: return emptyMap()
        val ownedDlc = runBlocking { getOwnedAppDlc(appId) }
        val indirectDlcApps = runBlocking { libraryDomain.getDownloadableDlcAppsOf(appId) }.orEmpty()

        return SteamCatalogManager.getDownloadableDepots(
            appInfo = appInfo,
            preferredLanguage = preferredLanguage,
            ownedDlc = ownedDlc,
            indirectDlcApps = indirectDlcApps,
        )
    }

    private suspend fun getOwnedAppDlc(appId: Int): Map<Int, DepotInfo> {
        val appDlc = libraryDomain.getAppDlc(appId)
        if (appDlc.isEmpty()) {
            return emptyMap()
        }

        val dlcAppIds = appDlc.values
            .map { it.dlcAppId }
            .filter { it != SteamService.INVALID_APP_ID }
            .toSet()

        val ownedGameIds = if (dlcAppIds.isEmpty()) {
            emptySet()
        } else {
            libraryDomain.checkDlcOwnership(dlcAppIds)
        }

        return SteamCatalogManager.filterOwnedAppDlc(
            appDlcDepots = appDlc,
            invalidAppId = SteamService.INVALID_APP_ID,
            ownedGameIds = ownedGameIds,
            hasLicense = { dlcAppId -> libraryDomain.findLicense(dlcAppId) != null },
            hasPicsApp = { dlcAppId -> libraryDomain.findApp(dlcAppId) != null },
        )
    }

    /**
     * Main download orchestration method
     */
    fun downloadApp(
        appId: Int,
        downloadableDepots: Map<Int, DepotInfo>,
        userSelectedDlcAppIds: List<Int>,
        branch: String,
        containerLanguage: String,
        isUpdateOrVerify: Boolean,
    ): DownloadInfo? {
        val appDirPath = SteamPaths.getAppDirPath(appId)

        if (!checkWifiOrNotify()) return null
        if (downloadJobs.contains(appId)) return getAppDownloadInfo(appId)
        Timber.d("depots is empty? %s", downloadableDepots.isEmpty())
        if (downloadableDepots.isEmpty()) return null

        val plan = buildDownloadPlan(
            appId = appId,
            downloadableDepots = downloadableDepots,
            userSelectedDlcAppIds = userSelectedDlcAppIds,
            mainDepots = getMainAppDepots(appId, containerLanguage),
            downloadableDlcApps = runBlocking { libraryDomain.getDownloadableDlcAppsOf(appId) }.orEmpty(),
            installedDownloadedDepots = runBlocking { libraryDomain.getInstalledApp(appId) }?.downloadedDepots,
            isUpdateOrVerify = isUpdateOrVerify,
            invalidAppId = Int.MAX_VALUE,
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

        runBlocking {
            libraryDomain.saveDownloadingAppInfo(appId, userSelectedDlcAppIds)
        }

        val info = DownloadInfo(selectedDepots.size, appId, downloadingAppIds).also { di ->
            di.setPersistencePath(appDirPath)
            val sizes = selectedDepots.map { (_, depot) ->
                val mInfo = depot.manifests[branch] ?: depot.encryptedManifests[branch] ?: return@map 1L
                mInfo.size
            }
            sizes.forEachIndexed { i, bytes -> di.setWeight(i, bytes) }

            val totalBytes = sizes.sum()
            di.setTotalExpectedBytes(totalBytes)

            val persistedBytes = di.loadPersistedBytesDownloaded(appDirPath)
            if (persistedBytes > 0L) {
                di.initializeBytesDownloaded(persistedBytes)
                Timber.i("Resumed download: initialized with $persistedBytes bytes")
            }
        }

        // Get steamClient from SteamService - this is a temporary coupling
        val steamClient = SteamService.instance?.steamClient
        if (steamClient == null) {
            Timber.e("SteamClient not available for download")
            return null
        }

        val downloadJob = scope.launch {
            try {
                val licenses = runBlocking { libraryDomain.getLicensesFromDb() }
                if (licenses.isEmpty()) {
                    Timber.w("No licenses available for download")
                    return@launch
                }

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

                val depotDownloader = DepotDownloader(
                    steamClient,
                    licenses,
                    debug = false,
                    androidEmulation = true,
                    maxDownloads = maxDownloads,
                    maxDecompress = maxDecompress,
                    parentJob = coroutineContext[Job.Key],
                    autoStartDownload = false,
                )

                val depotIdToIndex = selectedDepots.keys.mapIndexed { index, depotId -> depotId to index }.toMap()
                val listener = AppDownloadListener(info, depotIdToIndex, this@SteamInstallDomain)
                depotDownloader.addListener(listener)

                if (mainAppDepots.isNotEmpty()) {
                    val mainAppDepotIds = mainAppDepots.keys.sorted()
                    val mainAppItem = AppItem(
                        appId,
                        installDirectory = appDirPath,
                        depot = mainAppDepotIds,
                    )
                    depotDownloader.add(mainAppItem)
                }

                calculatedDlcAppIds.forEach { dlcAppId ->
                    val dlcDepots = selectedDepots.filter { it.value.dlcAppId == dlcAppId }
                    val dlcDepotIds = dlcDepots.keys.sorted()

                    val dlcAppItem = AppItem(
                        dlcAppId,
                        installDirectory = appDirPath,
                        depot = dlcDepotIds,
                    )
                    depotDownloader.add(dlcAppItem)
                }

                val appConfig = runBlocking { libraryDomain.getAppInfoOf(appId) }?.config
                if (appConfig != null) {
                    tryDownloadWorkshopControllerConfig(
                        templateIndex = appConfig.steamControllerTemplateIndex,
                        configDetails = appConfig.steamControllerConfigDetails,
                        appDirPath = appDirPath,
                        configFileName = "steam_controller.vdf",
                        fetchPublishedFileDetailsJson = { publishedFileId ->
                            val requestBody = FormBody.Builder()
                                .add("itemcount", "1")
                                .add("publishedfileids[0]", publishedFileId.toString())
                                .build()

                            val request = Request.Builder()
                                .url("https://api.steampowered.com/ISteamRemoteStorage/GetPublishedFileDetails/v1")
                                .post(requestBody)
                                .build()

                            Net.http.newCall(request).execute().use { response ->
                                if (!response.isSuccessful) {
                                    Timber.w("Failed to get steam controller config details for $publishedFileId: ${response.code}")
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
                                    Timber.w("Failed to download steam controller config from %s: %d", fileUrl, downloadResponse.code)
                                    return@use null
                                }
                                downloadResponse.body.bytes()
                            }
                        },
                    )
                }

                depotDownloader.finishAdding()
                depotDownloader.startDownloading()
                depotDownloader.getCompletion().await()
                depotDownloader.close()

                if (mainAppDepots.isNotEmpty()) {
                    val mainAppDepotIds = mainAppDepots.keys.sorted()
                    completeAppDownload(info, appId, mainAppDepotIds, mainAppDlcIds, appDirPath)
                }

                calculatedDlcAppIds.forEach { dlcAppId ->
                    val dlcDepots = selectedDepots.filter { it.value.dlcAppId == dlcAppId }
                    val dlcDepotIds = dlcDepots.keys.sorted()
                    completeAppDownload(info, dlcAppId, dlcDepotIds, emptyList(), appDirPath)
                }

                removeDownloadJob(appId)
                runBlocking { libraryDomain.deleteDownloadingApp(appId) }
            } catch (e: Exception) {
                Timber.e(e, "Download failed for app $appId")
                info.persistProgressSnapshot()
                selectedDepots.keys.sorted().forEachIndexed { idx, _ ->
                    info.setWeight(idx, 0)
                    info.setProgress(1f, idx)
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

        info.setDownloadJob(downloadJob)
        downloadJobs[appId] = info
        notifyDownloadStarted(appId)
        return info
    }

    fun getMainAppDepots(appId: Int, containerLanguage: String): Map<Int, DepotInfo> {
        val appInfo = runBlocking { libraryDomain.getAppInfoOf(appId) } ?: return emptyMap()
        val ownedDlc = runBlocking { getOwnedAppDlc(appId) }

        return SteamCatalogManager.getMainAppDepots(
            appInfo = appInfo,
            containerLanguage = containerLanguage,
            ownedDlc = ownedDlc,
        )
    }

    fun getMainAppDlcIdsWithoutProperDepotDlcIds(appId: Int): MutableList<Int> {
        val hiddenDlcAppIds = runBlocking {
            libraryDomain.getHiddenDlcAppsOf(appId).orEmpty().map { it.id }
        }
        val appInfo = runBlocking { libraryDomain.getAppInfoOf(appId) }

        return SteamCatalogManager.resolveMainAppDlcIdsWithoutProperDepotDlcIds(
            appInfo = appInfo,
            hiddenDlcAppIds = hiddenDlcAppIds,
        )
    }

    suspend fun completeAppDownload(
        downloadInfo: DownloadInfo,
        downloadingAppId: Int,
        entitledDepotIds: List<Int>,
        selectedDlcAppIds: List<Int>,
        appDirPath: String,
    ) {
        libraryDomain.upsertInstalledAppDownloadState(
            appId = downloadingAppId,
            entitledDepotIds = entitledDepotIds,
            selectedDlcAppIds = selectedDlcAppIds,
        )

        downloadInfo.downloadingAppIds.removeIf { it == downloadingAppId }

        if (downloadInfo.downloadingAppIds.isEmpty()) {
            withContext(Dispatchers.IO) {
                MarkerUtils.addMarker(appDirPath, Marker.DOWNLOAD_COMPLETE_MARKER)
                MarkerUtils.removeMarker(appDirPath, Marker.STEAM_DLL_REPLACED)
                MarkerUtils.removeMarker(appDirPath, Marker.STEAM_COLDCLIENT_USED)
            }

            libraryDomain.deleteDownloadingApp(downloadInfo.gameId)
            GameGrubApp.events.emit(AndroidEvent.LibraryInstallStatusChanged(downloadInfo.gameId))
            downloadInfo.clearPersistedBytesDownloaded(appDirPath)
        }
    }

    /**
     * Listener for download progress and completion events from DepotDownloader
     */
    private class AppDownloadListener(
        private val downloadInfo: DownloadInfo,
        private val depotIdToIndex: Map<Int, Int>,
        private val installDomain: SteamInstallDomain,
    ) : IDownloadListener {
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

            runBlocking {
                installDomain.libraryDomain.deleteDownloadingApp(downloadInfo.gameId)
            }

            installDomain.removeDownloadJob(downloadInfo.gameId)
            Timber.w("Download failed for app ${downloadInfo.gameId}")
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
            val previousBytes = depotCumulativeUncompressedBytes[depotId] ?: 0L
            val deltaBytes = uncompressedBytes - previousBytes
            depotCumulativeUncompressedBytes[depotId] = uncompressedBytes

            if (deltaBytes > 0L) {
                downloadInfo.updateBytesDownloaded(deltaBytes, System.currentTimeMillis())
            }

            depotIdToIndex[depotId]?.let { index ->
                downloadInfo.setProgress(depotPercentComplete, index)
            }

            downloadInfo.persistProgressSnapshot()
        }

        override fun onDepotCompleted(depotId: Int, compressedBytes: Long, uncompressedBytes: Long) {
            Timber.i("Depot $depotId completed (compressed: $compressedBytes, uncompressed: $uncompressedBytes)")

            val previousBytes = depotCumulativeUncompressedBytes[depotId] ?: 0L
            val deltaBytes = uncompressedBytes - previousBytes
            depotCumulativeUncompressedBytes[depotId] = uncompressedBytes

            if (deltaBytes > 0L) {
                downloadInfo.updateBytesDownloaded(deltaBytes, System.currentTimeMillis())
            }

            depotIdToIndex[depotId]?.let { index ->
                downloadInfo.setProgress(1f, index)
            }

            downloadInfo.persistProgressSnapshot()
        }
    }

    fun buildDownloadPlan(
        appId: Int,
        downloadableDepots: Map<Int, DepotInfo>,
        userSelectedDlcAppIds: List<Int>,
        mainDepots: Map<Int, DepotInfo>,
        downloadableDlcApps: List<SteamApp>,
        installedDownloadedDepots: List<Int>?,
        isUpdateOrVerify: Boolean,
        invalidAppId: Int,
        initialMainAppDlcIds: List<Int>,
    ) = SteamInstallManager.buildDownloadPlan(
        appId = appId,
        downloadableDepots = downloadableDepots,
        userSelectedDlcAppIds = userSelectedDlcAppIds,
        mainDepots = mainDepots,
        downloadableDlcApps = downloadableDlcApps,
        installedDownloadedDepots = installedDownloadedDepots,
        isUpdateOrVerify = isUpdateOrVerify,
        invalidAppId = invalidAppId,
        initialMainAppDlcIds = initialMainAppDlcIds,
    )

    fun choosePrimaryExe(files: List<FileData>?, gameName: String): FileData? =
        SteamInstallManager.choosePrimaryExe(files, gameName)

    fun hasExecutableFlag(flags: Any): Boolean = SteamInstallManager.hasExecutableFlag(flags)

    fun isStub(file: FileData): Boolean = SteamInstallManager.isStub(file)

    fun <T> resolveInstalledExe(
        appInfo: SteamApp?,
        canQueryManifests: Boolean,
        fallbackExecutable: () -> String,
        loadManifestCandidates: (depotId: Int, manifest: ManifestInfo) -> List<SteamInstalledExeManager.ExecutableCandidate<T>>?,
        choosePrimary: (List<SteamInstalledExeManager.ExecutableCandidate<T>>, String) -> SteamInstalledExeManager.ExecutableCandidate<T>?,
    ): String = SteamInstallManager.resolveInstalledExe(
        appInfo = appInfo,
        canQueryManifests = canQueryManifests,
        fallbackExecutable = fallbackExecutable,
        loadManifestCandidates = loadManifestCandidates,
        choosePrimary = choosePrimary,
    )

    fun selectSteamControllerConfig(details: List<SteamControllerConfigDetail>): SteamControllerConfigDetail? =
        inputManager.selectSteamControllerConfig(details)

    fun resolveSteamInputManifestFile(appDirPath: String, manifestPath: String): File? =
        inputManager.resolveSteamInputManifestFile(appDirPath, manifestPath)

    fun loadConfigFromManifest(manifestFile: File): String? =
        inputManager.loadConfigFromManifest(manifestFile)

    fun routeFor(templateIndex: Int): SteamControllerTemplateRoutingManager.TemplateRoute =
        inputManager.routeFor(templateIndex)

    fun requiresWorkshopDownload(templateIndex: Int): Boolean =
        inputManager.requiresWorkshopDownload(templateIndex)

    fun tryDownloadWorkshopControllerConfig(
        templateIndex: Int,
        configDetails: List<SteamControllerConfigDetail>,
        appDirPath: String,
        configFileName: String,
        fetchPublishedFileDetailsJson: (Long) -> String?,
        downloadFileBytes: (String) -> ByteArray?,
    ): Boolean = inputManager.tryDownloadWorkshopControllerConfig(
        templateIndex = templateIndex,
        configDetails = configDetails,
        appDirPath = appDirPath,
        configFileName = configFileName,
        fetchPublishedFileDetailsJson = fetchPublishedFileDetailsJson,
        downloadFileBytes = downloadFileBytes,
    )
}
