package app.gamegrub.gateway

import android.content.Context
import android.content.Intent

/**
 * Gateway for external launch request handling.
 *
 * This gateway abstracts the [app.gamegrub.LaunchRequestManager] to enable
 * dependency injection and testability. It handles:
 * - Parsing and handling launch intents from external sources (deep links)
 * - Tracking whether the app was launched via external intent
 * - Pending launch request management for cold-start scenarios
 *
 * @see app.gamegrub.LaunchRequestManager
 * @see app.gamegrub.utils.general.IntentLaunchManager
 */
interface LaunchRequestGateway {

    /**
     * Handles an incoming launch intent, either processing it immediately
     * or storing it as a pending request for later.
     *
     * @param context The application context
     * @param intent The incoming intent to process
     * @param isNewIntent Whether this is a new intent (vs initial intent)
     * @param consumePending Whether to consume any pending request
     * @return true if the intent was handled, false otherwise
     */
    fun handleLaunchIntent(
        context: Context,
        intent: Intent,
        isNewIntent: Boolean = false,
        consumePending: Boolean = false,
    ): Boolean

    /**
     * Returns whether the app was launched via an external intent (deep link).
     *
     * @return true if launched via external intent, false otherwise
     */
    fun wasLaunchedViaExternalIntent(): Boolean

    /**
     * Clears the external launch flag.
     */
    fun clearExternalLaunchFlag()

    /**
     * Checks if there's a pending launch request.
     *
     * @return true if there's a pending request, false otherwise
     */
    fun hasPendingLaunchRequest(): Boolean

    /**
     * Peeks at the pending launch request without consuming it.
     *
     * @return The pending request, or null if none
     */
    fun peekPendingLaunchRequest(): app.gamegrub.utils.general.IntentLaunchManager.LaunchRequest?

    /**
     * Consumes and returns the pending launch request.
     *
     * @return The pending request, or null if none
     */
    fun consumePendingLaunchRequest(): app.gamegrub.utils.general.IntentLaunchManager.LaunchRequest?

    /**
     * Clears any pending launch request.
     */
    fun clearPendingLaunchRequest()
}
