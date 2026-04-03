package app.gamegrub.ui.service

import android.content.Context
import android.content.Intent
import app.gamegrub.PrefManager
import app.gamegrub.service.amazon.AmazonService
import app.gamegrub.service.epic.EpicService
import app.gamegrub.service.gog.GOGService
import app.gamegrub.service.steam.SteamService
import app.gamegrub.ui.launch.consumePendingLaunchWithError
import app.gamegrub.ui.launch.handleExternalLaunchSuccess
import app.gamegrub.ui.launch.needsSteamLogin
import app.gamegrub.ui.launch.resolveGameAppId
import app.gamegrub.ui.launch.trackGameLaunched
import app.gamegrub.utils.auth.PlatformAuthUtils
import dagger.BindsInstance
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * Coordinator responsible for evaluating and starting platform services based on stored credentials.
 * Handles service lifecycle decisions and post-service-state navigation.
 *
 * This class extracts the service startup policy from GameGrubMain to improve testability and separation of concerns.
 */
@Singleton
class ServiceStartupCoordinator @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Evaluates service startup conditions and starts services that need to be started.
     *
     * @param viewModelScope   Coroutine scope for ViewModel operations
     * @param viewModel        MainViewModel for state updates
     * @param navController    NavController for navigation operations
     * @param state            Current UI state
     * @param isConnecting     Mutable flag tracking Steam connection attempts
     * @param onNavigation     Optional callback to execute navigation after service evaluation
     */
    fun evaluateAndStartServices(
        viewModelScope: androidx.lifecycle.viewModelScope,
        viewModel: app.gamegrub.ui.model.MainViewModel,
        navController: androidx.navigation.NavHostController,
        state: app.gamegrub.ui.model.MainViewModel.MainUiState,
        isConnecting: androidx.compose.runtime.MutableState<Boolean>,
        onNavigation: (() -> Unit)? = null
    ) {
        // Only attempt reconnection if not already connected/connecting and not in offline mode
        val shouldAttemptReconnect = !state.isSteamConnected &&
                !isConnecting.value &&
                !SteamService.keepAlive

        if (shouldAttemptReconnect) {
            Timber.d("[ServiceStartupCoordinator]: Steam not connected - attempting reconnection")
            isConnecting.value = true
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
        if (PlatformAuthUtils.isSignedInToAnyPlatform(context) && !SteamService.keepAlive) {
            val baseRoute = viewModel.getPersistedRoute() ?: app.gamegrub.ui.screen.GameGrubScreen.Home.route
            val targetRoute = if (SteamService.isLoggedIn) {
                baseRoute
            } else {
                // Non-Steam platforms: ensure offline param for Home
                if (baseRoute.startsWith(app.gamegrub.ui.screen.GameGrubScreen.Home.route)) {
                    app.gamegrub.ui.screen.GameGrubScreen.Home.route + "?offline=true"
                } else {
                    baseRoute
                }
            }
            navController.navigateFromLoginIfNeeded(targetRoute, "ResumeSession")
        }

        // Execute optional navigation callback if provided
        onNavigation?.invoke()
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