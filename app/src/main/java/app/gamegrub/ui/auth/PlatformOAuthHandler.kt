package app.gamegrub.ui.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import app.gamegrub.R
import app.gamegrub.service.amazon.AmazonService
import app.gamegrub.service.epic.EpicService
import app.gamegrub.service.gog.GOGService
import app.gamegrub.ui.screen.auth.AmazonOAuthActivity
import app.gamegrub.ui.screen.auth.EpicOAuthActivity
import app.gamegrub.ui.screen.auth.GOGOAuthActivity
import app.gamegrub.ui.utils.SnackbarManager
import app.gamegrub.service.auth.PlatformOAuthHandlers
import kotlinx.coroutines.launch

/**
 * Result holder for OAuth authentication attempts.
 * @property success Whether authentication succeeded.
 * @property errorMessage Error message if authentication failed, null otherwise.
 */
data class OAuthResult(
    val success: Boolean,
    val errorMessage: String? = null,
)

/**
 * State holder for platform OAuth authentication.
 * @property isAuthenticating Whether an OAuth flow is currently in progress.
 * @property lastResult The result of the last OAuth attempt.
 */
data class PlatformOAuthState(
    val isAuthenticating: Boolean = false,
    val lastResult: OAuthResult? = null,
)

/**
 * Callbacks for platform OAuth authentication.
 * @property onGogLoginSuccess Called when GOG login succeeds.
 * @property onEpicLoginSuccess Called when Epic login succeeds.
 * @property onAmazonLoginSuccess Called when Amazon login succeeds.
 * @property onError Called when any OAuth flow fails.
 */
data class PlatformOAuthCallbacks(
    val onGogLoginSuccess: () -> Unit = {},
    val onEpicLoginSuccess: () -> Unit = {},
    val onAmazonLoginSuccess: () -> Unit = {},
    val onError: (String) -> Unit = {},
)

/**
 * Custom hook that provides platform OAuth (GOG, Epic, Amazon) launchers and handles results.
 * This encapsulates all OAuth activity launcher creation and callback handling to keep composables clean.
 * 
 * @param context Application context for launching OAuth activities.
 * @param callbacks Callbacks for handling OAuth success and error states.
 * @return Pair of launchers map and current OAuth state.
 */
@Composable
fun rememberPlatformOAuth(
    context: Context,
    callbacks: PlatformOAuthCallbacks,
): Pair<Map<String, (Intent) -> Unit>, PlatformOAuthState> {
    val lifecycleScope = rememberCoroutineScope()
    var oauthState by rememberSaveable { mutableStateOf(PlatformOAuthState()) }
    
    val gogOAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            val message = result.data?.getStringExtra(GOGOAuthActivity.EXTRA_ERROR)
                ?: context.getString(R.string.gog_login_cancel)
            SnackbarManager.show(message)
            oauthState = PlatformOAuthState(lastResult = OAuthResult(false, message))
            callbacks.onError(message)
            return@rememberLauncherForActivityResult
        }
        val code = result.data?.getStringExtra(GOGOAuthActivity.EXTRA_AUTH_CODE)
        if (code == null) {
            val message = result.data?.getStringExtra(GOGOAuthActivity.EXTRA_ERROR)
                ?: context.getString(R.string.gog_login_cancel)
            SnackbarManager.show(message)
            oauthState = PlatformOAuthState(lastResult = OAuthResult(false, message))
            callbacks.onError(message)
            return@rememberLauncherForActivityResult
        }
        
        oauthState = PlatformOAuthState(isAuthenticating = true)
        lifecycleScope.launch {
            PlatformOAuthHandlers.handleGogAuthentication(
                context = context,
                authCode = code,
                coroutineScope = lifecycleScope,
                onLoadingChange = { isLoading ->
                    oauthState = oauthState.copy(isAuthenticating = isLoading)
                },
                onError = { msg ->
                    oauthState = PlatformOAuthState(lastResult = OAuthResult(false, msg))
                    callbacks.onError(msg ?: "GOG login failed")
                },
                onSuccess = {
                    SnackbarManager.show(context.getString(R.string.gog_login_success_title))
                    oauthState = PlatformOAuthState(lastResult = OAuthResult(true))
                    callbacks.onGogLoginSuccess()
                },
                onDialogClose = { },
            )
        }
    }

    val epicOAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            val message = result.data?.getStringExtra(EpicOAuthActivity.EXTRA_ERROR)
                ?: context.getString(R.string.epic_login_cancel)
            SnackbarManager.show(message)
            oauthState = PlatformOAuthState(lastResult = OAuthResult(false, message))
            callbacks.onError(message)
            return@rememberLauncherForActivityResult
        }
        val code = result.data?.getStringExtra(EpicOAuthActivity.EXTRA_AUTH_CODE)
        if (code == null) {
            val message = result.data?.getStringExtra(EpicOAuthActivity.EXTRA_ERROR)
                ?: context.getString(R.string.epic_login_cancel)
            SnackbarManager.show(message)
            oauthState = PlatformOAuthState(lastResult = OAuthResult(false, message))
            callbacks.onError(message)
            return@rememberLauncherForActivityResult
        }
        
        oauthState = PlatformOAuthState(isAuthenticating = true)
        lifecycleScope.launch {
            PlatformOAuthHandlers.handleEpicAuthentication(
                context = context,
                authCode = code,
                coroutineScope = lifecycleScope,
                onLoadingChange = { isLoading ->
                    oauthState = oauthState.copy(isAuthenticating = isLoading)
                },
                onError = { msg ->
                    oauthState = PlatformOAuthState(lastResult = OAuthResult(false, msg))
                    callbacks.onError(msg ?: "Epic login failed")
                },
                onSuccess = {
                    SnackbarManager.show(context.getString(R.string.epic_login_success_title))
                    oauthState = PlatformOAuthState(lastResult = OAuthResult(true))
                    callbacks.onEpicLoginSuccess()
                },
                onDialogClose = { },
            )
        }
    }

    val amazonOAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            val message = result.data?.getStringExtra(AmazonOAuthActivity.EXTRA_ERROR)
                ?: context.getString(R.string.amazon_login_cancel)
            SnackbarManager.show(message)
            oauthState = PlatformOAuthState(lastResult = OAuthResult(false, message))
            callbacks.onError(message)
            return@rememberLauncherForActivityResult
        }
        val code = result.data?.getStringExtra(AmazonOAuthActivity.EXTRA_AUTH_CODE)
        if (code == null) {
            val message = result.data?.getStringExtra(AmazonOAuthActivity.EXTRA_ERROR)
                ?: context.getString(R.string.amazon_login_cancel)
            SnackbarManager.show(message)
            oauthState = PlatformOAuthState(lastResult = OAuthResult(false, message))
            callbacks.onError(message)
            return@rememberLauncherForActivityResult
        }
        
        oauthState = PlatformOAuthState(isAuthenticating = true)
        lifecycleScope.launch {
            PlatformOAuthHandlers.handleAmazonAuthentication(
                context = context,
                authCode = code,
                coroutineScope = lifecycleScope,
                onLoadingChange = { isLoading ->
                    oauthState = oauthState.copy(isAuthenticating = isLoading)
                },
                onError = { msg ->
                    oauthState = PlatformOAuthState(lastResult = OAuthResult(false, msg))
                    callbacks.onError(msg ?: "Amazon login failed")
                },
                onSuccess = {
                    SnackbarManager.show(context.getString(R.string.amazon_login_success_title))
                    oauthState = PlatformOAuthState(lastResult = OAuthResult(true))
                    callbacks.onAmazonLoginSuccess()
                },
                onDialogClose = { },
            )
        }
    }

    val launchers = remember {
        mapOf(
            "gog" to { intent: Intent -> gogOAuthLauncher.launch(intent) },
            "epic" to { intent: Intent -> epicOAuthLauncher.launch(intent) },
            "amazon" to { intent: Intent -> amazonOAuthLauncher.launch(intent) },
        )
    }

    return launchers to oauthState
}
