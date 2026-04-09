package app.gamegrub.service

import android.content.Context
import app.gamegrub.data.AppInfo
import app.gamegrub.data.SteamLibraryApp
import app.gamegrub.db.dao.AmazonGameDao
import app.gamegrub.db.dao.AppInfoDao
import app.gamegrub.db.dao.EpicGameDao
import app.gamegrub.db.dao.GOGGameDao
import app.gamegrub.db.dao.SteamAppDao
import app.gamegrub.enums.Marker
import app.gamegrub.service.amazon.AmazonConstants
import app.gamegrub.service.epic.EpicConstants
import app.gamegrub.service.gog.GOGConstants
import app.gamegrub.service.steam.SteamPaths
import app.gamegrub.storage.StorageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

internal data class StartupInstallValidationSummary(
    val steamUpdated: Int,
    val gogUpdated: Int,
    val epicUpdated: Int,
    val amazonUpdated: Int,
) {
    val totalUpdated: Int = steamUpdated + gogUpdated + epicUpdated + amazonUpdated
}

internal fun resolveInstalledPathFromMarkers(candidatePaths: List<String>): String? {
    val normalizedPaths = candidatePaths
        .asSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
        .toList()

    val markerResolvedPath = normalizedPaths.firstOrNull { path ->
        StorageManager.hasMarker(path, Marker.DOWNLOAD_COMPLETE_MARKER) &&
            !StorageManager.hasMarker(path, Marker.DOWNLOAD_IN_PROGRESS_MARKER)
    }
    if (markerResolvedPath != null) {
        return markerResolvedPath
    }

    // Legacy installs may not include marker files; accept real directories unless a download is still in progress.
    return normalizedPaths.firstOrNull { path ->
        java.io.File(path).isDirectory &&
            !StorageManager.hasMarker(path, Marker.DOWNLOAD_IN_PROGRESS_MARKER)
    }
}

internal fun isSteamAppInstalled(app: app.gamegrub.data.SteamApp, installPaths: List<String>): Boolean {
    val candidateNames = listOf(app.config.installDir, app.installDir, app.name)
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .flatMap { name ->
            val sanitized = name.replace(Regex("[^a-zA-Z0-9 ]"), "").trim()
            if (sanitized.isNotEmpty() && sanitized != name) {
                listOf(name, sanitized)
            } else {
                listOf(name)
            }
        }
        .distinct()

    if (candidateNames.isEmpty()) {
        return false
    }

    val candidatePaths = buildList {
        for (basePath in installPaths) {
            for (candidateName in candidateNames) {
                add(java.nio.file.Paths.get(basePath, candidateName).toString())
            }
        }
    }
    return resolveInstalledPathFromMarkers(candidatePaths) != null
}

internal fun isSteamLibraryAppInstalled(app: SteamLibraryApp, installPaths: List<String>): Boolean {
    val candidateNames = listOf(app.installDir, app.name)
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .flatMap { name ->
            val sanitized = name.replace(Regex("[^a-zA-Z0-9 ]"), "").trim()
            if (sanitized.isNotEmpty() && sanitized != name) {
                listOf(name, sanitized)
            } else {
                listOf(name)
            }
        }
        .distinct()

    if (candidateNames.isEmpty()) {
        return false
    }

    val candidatePaths = buildList {
        for (basePath in installPaths) {
            for (candidateName in candidateNames) {
                add(java.nio.file.Paths.get(basePath, candidateName).toString())
            }
        }
    }
    return resolveInstalledPathFromMarkers(candidatePaths) != null
}

@Singleton
class InstalledGamesStartupValidator @Inject constructor(
    private val steamAppDao: SteamAppDao,
    private val appInfoDao: AppInfoDao,
    private val gogGameDao: GOGGameDao,
    private val epicGameDao: EpicGameDao,
    private val amazonGameDao: AmazonGameDao,
    @param:ApplicationContext private val context: Context,
) {
    private suspend fun reconcileInstalledFlags(): StartupInstallValidationSummary = withContext(Dispatchers.IO) {
        val steamUpdated = reconcileSteamInstalledFlags()
        val gogUpdated = reconcileGogInstalledFlags()
        val epicUpdated = reconcileEpicInstalledFlags()
        val amazonUpdated = reconcileAmazonInstalledFlags()

        StartupInstallValidationSummary(
            steamUpdated = steamUpdated,
            gogUpdated = gogUpdated,
            epicUpdated = epicUpdated,
            amazonUpdated = amazonUpdated,
        )
    }

    private suspend fun reconcileSteamInstalledFlags(): Int {
        val steamApps = steamAppDao.getAllOwnedAppsAsList()
        val installPaths = SteamPaths.allInstallPaths
        var changed = 0

        for (app in steamApps) {
            val expectedInstalled = isSteamAppInstalled(app, installPaths)
            val existingAppInfo = appInfoDao.get(app.id)

            if (existingAppInfo == null) {
                if (expectedInstalled) {
                    appInfoDao.insert(
                        AppInfo(
                            id = app.id,
                            isDownloaded = true,
                        ),
                    )
                    changed++
                }
                continue
            }

            if (existingAppInfo.isDownloaded != expectedInstalled) {
                appInfoDao.update(existingAppInfo.copy(isDownloaded = expectedInstalled))
                changed++
            }
        }

        return changed
    }

    private suspend fun reconcileGogInstalledFlags(): Int {
        val games = gogGameDao.getAllAsList()
        var changed = 0

        for (game in games) {
            val installedPath = resolveInstalledPathFromMarkers(
                candidatePaths = listOf(
                    game.installPath,
                    GOGConstants.getGameInstallPath(game.title),
                ),
            )
            val expectedInstalled = installedPath != null

            if (expectedInstalled != game.isInstalled || (installedPath != null && installedPath != game.installPath)) {
                gogGameDao.update(
                    game.copy(
                        isInstalled = expectedInstalled,
                        installPath = installedPath ?: "",
                        installSize = if (expectedInstalled) game.installSize else 0L,
                    ),
                )
                changed++
            }
        }

        return changed
    }

    private suspend fun reconcileEpicInstalledFlags(): Int {
        val games = epicGameDao.getAllAsList()
        var changed = 0

        for (game in games) {
            val installedPath = resolveInstalledPathFromMarkers(
                candidatePaths = listOf(
                    game.installPath,
                    EpicConstants.getGameInstallPath(context, game.appName),
                ),
            )
            val expectedInstalled = installedPath != null

            if (expectedInstalled != game.isInstalled || (installedPath != null && installedPath != game.installPath)) {
                epicGameDao.update(
                    game.copy(
                        isInstalled = expectedInstalled,
                        installPath = installedPath ?: "",
                        installSize = if (expectedInstalled) game.installSize else 0L,
                    ),
                )
                changed++
            }
        }

        return changed
    }

    private suspend fun reconcileAmazonInstalledFlags(): Int {
        val games = amazonGameDao.getAllAsList()
        var changed = 0

        for (game in games) {
            val installedPath = resolveInstalledPathFromMarkers(
                candidatePaths = listOf(
                    game.installPath,
                    AmazonConstants.getGameInstallPath(context, game.title),
                ),
            )
            val expectedInstalled = installedPath != null

            val shouldUpdate = expectedInstalled != game.isInstalled || (installedPath != null && installedPath != game.installPath)
            if (!shouldUpdate) {
                continue
            }

            if (expectedInstalled) {
                amazonGameDao.markAsInstalled(
                    productId = game.productId,
                    path = installedPath,
                    size = game.installSize,
                    versionId = game.versionId,
                )
            } else {
                amazonGameDao.markAsUninstalled(game.productId)
            }
            changed++
        }

        return changed
    }

    suspend fun reconcileInstalledFlagsSafely() {
        runCatching {
            val summary = reconcileInstalledFlags()
            Timber.tag("InstallStartupValidation").i(
                "Startup install reconciliation complete: total=%d (steam=%d, gog=%d, epic=%d, amazon=%d)",
                summary.totalUpdated,
                summary.steamUpdated,
                summary.gogUpdated,
                summary.epicUpdated,
                summary.amazonUpdated,
            )
        }.onFailure { throwable ->
            Timber.tag("InstallStartupValidation").e(throwable, "Startup install reconciliation failed")
        }
    }
}
