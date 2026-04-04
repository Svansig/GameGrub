package app.gamegrub.ui.service

import android.content.Context
import android.content.Intent
import app.gamegrub.gateway.AuthStateGateway
import app.gamegrub.service.amazon.AmazonService
import app.gamegrub.service.epic.EpicService
import app.gamegrub.service.gog.GOGService
import app.gamegrub.service.steam.SteamService
import app.gamegrub.utils.auth.PlatformAuthUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class ServiceStartupCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authStateGateway: AuthStateGateway,
) {
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