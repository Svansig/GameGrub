package app.gamegrub.service.steam.domain

import app.gamegrub.PrefManager
import app.gamegrub.data.SteamApp
import app.gamegrub.db.dao.SteamAppDao
import app.gamegrub.db.dao.SteamLicenseDao
import app.gamegrub.service.steam.SteamService
import app.gamegrub.utils.steam.generateSteamApp
import `in`.dragonbra.javasteam.enums.ELicenseFlags
import `in`.dragonbra.javasteam.steam.handlers.steamapps.PICSRequest
import `in`.dragonbra.javasteam.steam.handlers.steamapps.SteamApps
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.future.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.EnumSet
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SteamPicsSyncDomain @Inject constructor(
    private val appDao: SteamAppDao,
    private val licenseDao: SteamLicenseDao,
    private val libraryDomain: SteamLibraryDomain,
) {
    val appPicsChannel = Channel<List<PICSRequest>>(
        capacity = 1_000,
        onBufferOverflow = BufferOverflow.SUSPEND,
        onUndeliveredElement = { droppedApps ->
            Timber.w("App PICS Channel dropped: ${droppedApps.size} apps")
        },
    )

    val packagePicsChannel = Channel<List<PICSRequest>>(
        capacity = 1_000,
        onBufferOverflow = BufferOverflow.SUSPEND,
        onUndeliveredElement = { droppedPackages ->
            Timber.w("Package PICS Channel dropped: ${droppedPackages.size} packages")
        },
    )

    var picsGetProductInfoJob: Job? = null
    var picsChangesCheckerJob: Job? = null

    fun continuousPICSChangesChecker(parentScope: CoroutineScope, steamApps: SteamApps?): Job = parentScope.launch {
        while (isActive && SteamService.isLoggedIn) {
            kotlinx.coroutines.delay(60)
            picsChangesCheck(steamApps)
        }
    }

    private fun picsChangesCheck(steamApps: SteamApps?) {
        if (steamApps == null) return

        CoroutineScope(Dispatchers.IO).launch {
            ensureActive()

            try {
                val changesSince = steamApps.picsGetChangesSince(
                    lastChangeNumber = PrefManager.lastPICSChangeNumber,
                    sendAppChangeList = true,
                    sendPackageChangelist = true,
                ).await()

                if (PrefManager.lastPICSChangeNumber == changesSince.currentChangeNumber) {
                    Timber.w("Change number was the same as last change number, skipping")
                    return@launch
                }

                PrefManager.lastPICSChangeNumber = changesSince.currentChangeNumber

                Timber.d(
                    "picsGetChangesSince: lastChangeNumber: %s, currentChangeNumber: %s, appChangesCount: %s, pkgChangesCount: %s",
                    changesSince.lastChangeNumber,
                    changesSince.currentChangeNumber,
                    changesSince.appChanges.size,
                    changesSince.packageChanges.size,
                )

                launch {
                    changesSince.appChanges.values
                        .filter { changeData ->
                            val app = libraryDomain.findApp(changeData.id) ?: return@filter false
                            changeData.changeNumber != app.lastChangeNumber
                        }
                        .map { PICSRequest(id = it.id) }
                        .chunked(SteamService.MAX_PICS_BUFFER)
                        .forEach { chunk ->
                            ensureActive()
                            Timber.d("onPicsChanges: Queueing ${chunk.size} app(s) for PICS")
                            appPicsChannel.send(chunk)
                        }
                }

                launch {
                    val pkgsWithChanges = changesSince.packageChanges.values
                        .filter { changeData ->
                            val pkg = libraryDomain.findLicense(changeData.id) ?: return@filter false
                            changeData.changeNumber != pkg.lastChangeNumber
                        }

                    if (pkgsWithChanges.isNotEmpty()) {
                        val pkgsForAccessTokens = pkgsWithChanges.filter { it.isNeedsToken }.map { it.id }

                        val accessTokens = steamApps.picsGetAccessTokens(emptyList(), pkgsForAccessTokens)
                            ?.await()?.packageTokens ?: emptyMap()

                        ensureActive()

                        pkgsWithChanges
                            .map { PICSRequest(it.id, accessTokens[it.id] ?: 0) }
                            .chunked(SteamService.MAX_PICS_BUFFER)
                            .forEach { chunk ->
                                Timber.d("onPicsChanges: Queueing ${chunk.size} package(s) for PICS")
                                packagePicsChannel.send(chunk)
                            }
                    }
                }
            } catch (e: NullPointerException) {
                Timber.w("No lastPICSChangeNumber, skipping")
            } catch (e: Exception) {
                Timber.w(e, "picsChangesCheck error")
            }
        }
    }

    fun continuousPICSGetProductInfo(parentScope: CoroutineScope, steamApps: SteamApps?): Job = parentScope.launch {
        launch {
            appPicsChannel.receiveAsFlow()
                .filter { it.isNotEmpty() }
                .buffer(capacity = SteamService.MAX_PICS_BUFFER, onBufferOverflow = BufferOverflow.SUSPEND)
                .collect { appRequests ->
                    Timber.d("Processing ${appRequests.size} app PICS requests")

                    ensureActive()
                    if (!SteamService.isLoggedIn) return@collect
                    if (steamApps == null) return@collect

                    val callback = steamApps.picsGetProductInfo(
                        apps = appRequests,
                        packages = emptyList(),
                    ).await()

                    callback.results.forEachIndexed { index, picsCallback ->
                        Timber.d("onPicsProduct: ${index + 1} of ${callback.results.size}")

                        ensureActive()

                        val steamAppsMap = picsCallback.apps.values.mapNotNull { app ->
                            val appFromDb = libraryDomain.findApp(app.id)
                            val packageId = appFromDb?.packageId ?: SteamService.INVALID_PKG_ID
                            val packageFromDb = if (packageId != SteamService.INVALID_PKG_ID) libraryDomain.findLicense(packageId) else null
                            val ownerAccountId = packageFromDb?.ownerAccountId ?: emptyList()

                            if (app.changeNumber != appFromDb?.lastChangeNumber) {
                                app.keyValues.generateSteamApp().copy(
                                    packageId = packageId,
                                    ownerAccountId = ownerAccountId,
                                    receivedPICS = true,
                                    lastChangeNumber = app.changeNumber,
                                    licenseFlags = packageFromDb?.licenseFlags ?: EnumSet.noneOf(ELicenseFlags::class.java),
                                )
                            } else {
                                null
                            }
                        }

                        if (steamAppsMap.isNotEmpty()) {
                            Timber.i("Inserting ${steamAppsMap.size} PICS apps to database")
                            appDao.insertAll(steamAppsMap)
                        }
                    }
                }
        }

        launch {
            packagePicsChannel.receiveAsFlow()
                .filter { it.isNotEmpty() }
                .buffer(capacity = SteamService.MAX_PICS_BUFFER, onBufferOverflow = BufferOverflow.SUSPEND)
                .collect { packageRequests ->
                    Timber.d("Processing ${packageRequests.size} package PICS requests")

                    ensureActive()
                    if (!SteamService.isLoggedIn) return@collect
                    if (steamApps == null) return@collect

                    val callback = steamApps.picsGetProductInfo(
                        apps = emptyList(),
                        packages = packageRequests,
                    ).await()

                    callback.results.forEach { picsCallback ->
                        if (!SteamService.isLoggedIn) return@forEach

                        picsCallback.packages.values.forEach { pkg ->
                            val appIds = pkg.keyValues["appids"].children.map { it.asInteger() }
                            licenseDao.updateApps(pkg.id, appIds)

                            val depotIds = pkg.keyValues["depotids"].children.map { it.asInteger() }
                            licenseDao.updateDepots(pkg.id, depotIds)

                            appIds.forEach { appid ->
                                val steamApp = appDao.findApp(appid)?.copy(packageId = pkg.id)
                                if (steamApp != null) {
                                    appDao.update(steamApp)
                                } else {
                                    appDao.insert(SteamApp(id = appid, packageId = pkg.id))
                                }
                            }
                        }
                    }
                }
        }
    }

    fun cancelPicsJobs() {
        picsGetProductInfoJob?.cancel()
        picsChangesCheckerJob?.cancel()
        picsGetProductInfoJob = null
        picsChangesCheckerJob = null
    }
}
