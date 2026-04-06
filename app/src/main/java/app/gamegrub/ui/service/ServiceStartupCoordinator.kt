package app.gamegrub.ui.service

import android.content.Context
import android.content.Intent
import androidx.navigation.NavHostController
import app.gamegrub.gateway.AuthStateGateway
import app.gamegrub.service.InstalledGamesStartupValidator
import app.gamegrub.service.amazon.AmazonService
import app.gamegrub.service.epic.EpicService
import app.gamegrub.service.gog.GOGService
import app.gamegrub.service.steam.SteamService
import app.gamegrub.ui.data.MainState
import app.gamegrub.ui.model.MainViewModel
import app.gamegrub.ui.screen.GameGrubScreen
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

@Singleton
class ServiceStartupCoordinator @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val authStateGateway: AuthStateGateway,
    private val installedGamesStartupValidator: InstalledGamesStartupValidator,
) {
    private val startupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile
    private var startupInstallValidationStarted: Boolean = false

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

        // Start GOGService if user has GOG
        if (GOGService.hasStoredCredentials(context) &&
            !GOGService.isRunning
        ) {
            Timber.tag("GOG").d("[ServiceStartupCoordinator]: Starting GOGService for logged-in user")
            GOGService.start(context)
        } else {
            Timber.tag("GOG").d("GOG SERVICE Not going to start: ${GOGService.isRunning}")
        }

        // Start EpicService if user has Epic credentials
        if (EpicService.hasStoredCredentials(context) &&
            !EpicService.isRunning
        ) {
            Timber.d("[ServiceStartupCoordinator]: Starting EpicService for logged-in user")
            EpicService.start(context)
        }

        // Start AmazonService if user has Amazon credentials
        if (AmazonService.hasStoredCredentials(context) &&
            !AmazonService.isRunning
        ) {
            Timber.d("[ServiceStartupCoordinator]: Starting AmazonService for logged-in user")
            AmazonService.start(context)
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
