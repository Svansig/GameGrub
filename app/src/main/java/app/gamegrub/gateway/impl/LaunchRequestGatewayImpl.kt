package app.gamegrub.gateway.impl

import android.content.Context
import android.content.Intent
import app.gamegrub.LaunchRequestManager
import app.gamegrub.gateway.LaunchRequestGateway
import app.gamegrub.launch.IntentLaunchManager
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [LaunchRequestGateway] that delegates to [LaunchRequestManager].
 *
 * This implementation wraps the static LaunchRequestManager to provide
 * an injectable gateway. Future work may migrate the underlying logic
 * directly into this class or a domain object.
 *
 * @see LaunchRequestGateway
 * @see LaunchRequestManager
 */
@Singleton
class LaunchRequestGatewayImpl @Inject constructor() : LaunchRequestGateway {

    override fun handleLaunchIntent(
        context: Context,
        intent: Intent,
        isNewIntent: Boolean,
        consumePending: Boolean,
    ): Boolean {
        return try {
            LaunchRequestManager.handleLaunchIntent(
                context = context,
                intent = intent,
                isNewIntent = isNewIntent,
                consumePending = consumePending,
                applyTemporaryConfig = { appId, launchRequest ->
                    launchRequest.containerConfig?.let {
                        IntentLaunchManager.applyTemporaryConfigOverride(context, appId, it)
                        Timber.i("[RendererSelection][Intent] appId=%s applied temporary container override", appId)
                    }
                },
            )
            true
        } catch (e: Exception) {
            Timber.e(e, "[IntentLaunch] Failed to handle launch intent")
            false
        }
    }

    override fun wasLaunchedViaExternalIntent(): Boolean {
        return LaunchRequestManager.wasLaunchedViaExternalIntent
    }

    override fun clearExternalLaunchFlag() {
        LaunchRequestManager.clearExternalLaunchFlag()
    }

    override fun hasPendingLaunchRequest(): Boolean {
        return LaunchRequestManager.hasPendingLaunchRequest()
    }

    override fun peekPendingLaunchRequest(): IntentLaunchManager.LaunchRequest? {
        return LaunchRequestManager.peekPendingLaunchRequest()
    }

    override fun consumePendingLaunchRequest(): IntentLaunchManager.LaunchRequest? {
        return LaunchRequestManager.consumePendingLaunchRequest()
    }

    override fun clearPendingLaunchRequest() {
        LaunchRequestManager.clearPendingLaunchRequest()
    }
}
