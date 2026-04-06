package app.gamegrub.launch

import android.content.Context
import app.gamegrub.data.GameSource
import app.gamegrub.domain.customgame.CustomGameScanner
import app.gamegrub.service.amazon.AmazonService
import app.gamegrub.service.epic.EpicService
import app.gamegrub.service.gog.GOGService
import app.gamegrub.service.steam.SteamService
import app.gamegrub.utils.container.ContainerUtils

/**
 * Result model for launch-request app-id resolution and install-state validation.
 */
sealed class GameResolutionResult {
    /**
     * The requested app resolves to an installed title and can continue through launch orchestration.
     */
    data class Success(
        val finalAppId: String,
        val gameId: Int,
        val isSteamInstalled: Boolean,
        val isCustomGame: Boolean,
    ) : GameResolutionResult()

    /**
     * The request resolved to a known title, but it is not currently installed for launch.
     */
    data class NotFound(
        val gameId: Int,
        val originalAppId: String,
    ) : GameResolutionResult()
}

/**
 * Resolves a launch request app id and verifies installation state for its backing store.
 * @param context App context used for container and platform checks.
 * @param appId Encoded GameNative app id (includes source and game id).
 * @return [GameResolutionResult.Success] when installed, otherwise [GameResolutionResult.NotFound].
 */
fun resolveGameAppId(context: Context, appId: String): GameResolutionResult {
    val gameSource = ContainerUtils.extractGameSourceFromContainerId(appId)
    val gameId = ContainerUtils.extractGameIdFromContainerId(appId)
    val isInstalled = when (gameSource) {
        GameSource.STEAM -> {
            if (SteamService.getAppInfoOf(gameId) != null) {
                SteamService.isAppInstalled(gameId)
            } else {
                ContainerUtils.hasContainer(context, appId)
            }
        }

        GameSource.GOG -> {
            GOGService.isGameInstalled(gameId.toString())
        }

        GameSource.EPIC -> {
            EpicService.isGameInstalled(context, gameId)
        }

        GameSource.AMAZON -> {
            AmazonService.isGameInstalledByAppIdSync(context, gameId)
        }

        GameSource.CUSTOM_GAME -> {
            CustomGameScanner.get().isGameInstalled(gameId)
        }
    }

    if (!isInstalled) {
        return GameResolutionResult.NotFound(
            gameId = gameId,
            originalAppId = appId,
        )
    }

    val isSteamInstalled = gameSource == GameSource.STEAM
    val isCustomGame = gameSource == GameSource.CUSTOM_GAME

    return GameResolutionResult.Success(
        finalAppId = appId,
        gameId = gameId,
        isSteamInstalled = isSteamInstalled,
        isCustomGame = isCustomGame,
    )
}

/**
 * Checks whether a pending launch should require a Steam login gate.
 * @param context App context used to read container metadata.
 * @param appId Encoded app id being launched.
 * @return `true` when Steam authentication is required before launch.
 */
fun needsSteamLogin(context: Context, appId: String): Boolean {
    val gameSource = ContainerUtils.extractGameSourceFromContainerId(appId)
    if (gameSource != GameSource.STEAM || SteamService.isLoggedIn) return false
    return try {
        !ContainerUtils.getContainer(context, appId).isSteamOfflineMode
    } catch (_: Exception) {
        true
    }
}
