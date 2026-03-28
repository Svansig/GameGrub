package app.gamegrub

import android.content.Context
import android.content.Intent
import app.gamegrub.events.AndroidEvent
import app.gamegrub.ui.utils.SnackbarManager
import app.gamegrub.utils.general.IntentLaunchManager
import timber.log.Timber

/**
 * Manages external launch intents - handles pending launch requests from cold starts
 * and new intents, coordinating with the UI to launch games via deep links.
 */
object LaunchRequestManager {

    @Volatile
    private var pendingLaunchRequest: IntentLaunchManager.LaunchRequest? = null

    private var _wasLaunchedViaExternalIntent: Boolean = false

    val wasLaunchedViaExternalIntent: Boolean
        get() = _wasLaunchedViaExternalIntent

    fun markAsExternalLaunch() {
        _wasLaunchedViaExternalIntent = true
    }

    fun clearExternalLaunchFlag() {
        _wasLaunchedViaExternalIntent = false
    }

    @Synchronized
    fun setPendingLaunchRequest(request: IntentLaunchManager.LaunchRequest) {
        Timber.d("[IntentLaunch]: Setting pending launch request for app ${request.appId}")
        pendingLaunchRequest = request
    }

    @Synchronized
    fun consumePendingLaunchRequest(): IntentLaunchManager.LaunchRequest? {
        val request = pendingLaunchRequest
        Timber.d("[IntentLaunch]: Consuming pending launch request for app ${request?.appId}")
        pendingLaunchRequest = null
        return request
    }

    fun hasPendingLaunchRequest(): Boolean = pendingLaunchRequest != null

    @Synchronized
    fun peekPendingLaunchRequest(): IntentLaunchManager.LaunchRequest? = pendingLaunchRequest

    fun clearPendingLaunchRequest() {
        synchronized(this) {
            pendingLaunchRequest = null
        }
        Timber.d("[LaunchRequestManager]: Cleared pending launch request")
    }

    fun handleLaunchIntent(
        context: Context,
        intent: Intent,
        isNewIntent: Boolean = false,
        consumePending: Boolean = false,
        applyTemporaryConfig: ((String, IntentLaunchManager.LaunchRequest) -> Unit)? = null,
    ) {
        if (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0) {
            Timber.d("[IntentLaunch]: Ignoring intent re-delivered from recents")
            return
        }

        Timber.d("[IntentLaunch]: handleLaunchIntent called with action=${intent.action}, isNewIntent=$isNewIntent")

        try {
            val launchRequest = IntentLaunchManager.parseLaunchIntent(intent)

            if (launchRequest != null) {
                Timber.d("[IntentLaunch]: Received external launch intent for app ${launchRequest.appId}")

                if (isNewIntent || consumePending) {
                    if (consumePending) {
                        consumePendingLaunchRequest()
                    }

                    Timber.d("[IntentLaunch]: Emitting ExternalGameLaunch event for app ${launchRequest.appId}")

                    launchRequest.containerConfig?.let { config ->
                        applyTemporaryConfig?.invoke(launchRequest.appId, launchRequest)
                    }

                    GameGrubApp.events.emit(AndroidEvent.ExternalGameLaunch(launchRequest.appId))
                } else {
                    setPendingLaunchRequest(launchRequest)
                    Timber.d("[IntentLaunch]: Stored pending launch request for app ${launchRequest.appId}")
                }
            } else if (intent.action == "${BuildConfig.APPLICATION_ID}.LAUNCH_GAME") {
                clearExternalLaunchFlag()
                Timber.w("[IntentLaunch]: parseLaunchIntent returned null for LAUNCH_GAME intent")
                SnackbarManager.show(context.getString(R.string.intent_launch_failed))
            }
        } catch (e: Exception) {
            Timber.e(e, "[IntentLaunch]: Failed to handle launch intent")
        }
    }
}
