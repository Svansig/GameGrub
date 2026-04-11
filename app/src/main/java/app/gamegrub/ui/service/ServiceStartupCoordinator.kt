package app.gamegrub.ui.service

import android.content.Context
import android.content.Intent
import androidx.navigation.NavHostController
import app.gamegrub.data.GameSource
import app.gamegrub.gateway.AuthStateGateway
import app.gamegrub.service.InstalledGamesStartupValidator
import app.gamegrub.service.base.GameStoreCoordinator
import app.gamegrub.service.steam.SteamService
import app.gamegrub.ui.data.MainState
import app.gamegrub.ui.model.MainViewModel
import app.gamegrub.ui.screen.GameGrubScreen
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServiceStartupCoordinator @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val authStateGateway: AuthStateGateway,
    private val installedGamesStartupValidator: InstalledGamesStartupValidator,
    private val storeCoordinators: Set<@JvmSuppressWildcards GameStoreCoordinator>,
) {
    private val startupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var startupInstallValidationStarted: Boolean = false

    private val nonSteamCoordinators: List<GameStoreCoordinator>
        get() = storeCoordinators.filter { it.gameSource != GameSource.STEAM }

    private fun getCoordinator(gameSource: GameSource): GameStoreCoordinator? {
        return storeCoordinators.find { it.gameSource == gameSource }
    }

    fun evaluateAndStartServices(
        viewModel: MainViewModel,
        navController: NavHostController,
        state: MainState,
        isConnecting: Boolean,
        onConnectingChanged: (Boolean) -> Unit,
        onNavigation: (() -> Unit)? = null,
    ) {
        if (!startupInstallValidationStarted) {
            synchronized(this) {
                if (!startupInstallValidationStarted) {
                    startupInstallValidationStarted = true
                    startupScope.launch {
                        installedGamesStartupValidator.reconcileInstalledFlagsSafely()
                    }
                }
            }
        }

        // Only attempt reconnection if not already connected/connecting and not in offline mode
        val shouldAttemptReconnect = !state.isSteamConnected &&
                !isConnecting &&
                !SteamService.keepAlive

        if (shouldAttemptReconnect) {
            Timber.d("[ServiceStartupCoordinator]: Steam not connected - attempting reconnection")
            onConnectingChanged(true)
            viewModel.startConnecting()
            context.startForegroundService(Intent(context, SteamService::class.java))
        }

        // Start non-Steam services if they have stored credentials but aren't running
        nonSteamCoordinators.forEach { coordinator ->
            val hasCredentials = coordinator.hasStoredCredentials()
            val isRunning = coordinator.isRunning
            if (hasCredentials && !isRunning) {
                Timber.d("[ServiceStartupCoordinator]: Starting ${coordinator.gameSource}Service for logged-in user")
                coordinator.start()
            } else {
                Timber.tag(coordinator.gameSource.name).d("${coordinator.gameSource}Service not going to start: isRunning=$isRunning")
            }
        }

        // Handle navigation when already logged in (e.g., app resumed with active session)
        // Only navigate if currently on LoginUser screen to avoid disrupting user's current view
        if (authStateGateway.getLoggedInStores().isNotEmpty() && !SteamService.keepAlive) {
            val baseRoute = viewModel.getPersistedRoute() ?: GameGrubScreen.Home.route
            val targetRoute = if (SteamService.isLoggedIn) {
                baseRoute
            } else {
                // Non-Steam platforms: ensure offline param for Home
                if (baseRoute.startsWith(GameGrubScreen.Home.route)) {
                    GameGrubScreen.Home.route + "?offline=true"
                } else {
                    baseRoute
                }
            }
            navigateFromLoginIfNeeded(navController, targetRoute, "ResumeSession")
        }

        // Execute optional navigation callback if provided
        onNavigation?.invoke()
    }

    private fun navigateFromLoginIfNeeded(
        navController: NavHostController,
        targetRoute: String,
        logTag: String,
    ) {
        val currentRoute = navController.currentDestination?.route
        if (currentRoute == GameGrubScreen.LoginUser.route) {
            Timber.tag(logTag).i("Navigating from LoginUser to $targetRoute")
            navController.navigate(targetRoute) {
                popUpTo(GameGrubScreen.LoginUser.route) {
                    inclusive = true
                }
            }
        }
    }
}

/**
 * Entry point for accessing ServiceStartupCoordinator from Android components that don't support Hilt injection directly.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ServiceStartupCoordinatorEntryPoint {
    fun serviceStartupCoordinator(): ServiceStartupCoordinator
}

/**
 * Helper to obtain ServiceStartupCoordinator instance from Context.
 */
fun Context.getServiceStartupCoordinator(): ServiceStartupCoordinator {
    return EntryPointAccessors
        .fromApplication(this, ServiceStartupCoordinatorEntryPoint::class.java)
        .serviceStartupCoordinator()
}
