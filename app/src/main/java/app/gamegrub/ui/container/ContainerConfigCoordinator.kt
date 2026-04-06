package app.gamegrub.ui.container

import android.content.Context
import app.gamegrub.launch.IntentLaunchManager
import app.gamegrub.utils.container.ContainerUtils
import timber.log.Timber

/**
 * Coordinates container configuration save, discard, and restore operations.
 *
 * This class encapsulates the logic for:
 * - Saving container configuration permanently
 * - Discarding temporary configuration overrides
 * - Restoring original configuration
 *
 * The coordinator preserves all existing container config behavior from GameGrubMain while
 * moving it into a testable boundary that doesn't live in composable callbacks.
 *
 * @param context Application context used for container operations.
 */
class ContainerConfigCoordinator(
    private val context: Context,
) {
    /**
     * Saves the container configuration permanently and clears any temporary override.
     *
     * @param appId The app ID whose container config should be saved.
     */
    fun saveConfig(appId: String) {
        IntentLaunchManager.getEffectiveContainerConfig(context, appId)?.let { config ->
            ContainerUtils.applyToContainer(context, appId, config)
            Timber.i("[ContainerConfigCoordinator]: Saved container configuration for app $appId")
        }
        IntentLaunchManager.clearTemporaryOverride(appId)
    }

    /**
     * Discards the temporary configuration and restores the original configuration.
     *
     * @param appId The app ID whose container config should be discarded/restored.
     */
    fun discardConfig(appId: String) {
        IntentLaunchManager.restoreOriginalConfiguration(context, appId)
        IntentLaunchManager.clearTemporaryOverride(appId)
        Timber.i("[ContainerConfigCoordinator]: Discarded temporary config and restored original for app $appId")
    }

    /**
     * Restores original configuration without clearing the temporary override.
     *
     * This is used when the dialog is dismissed without explicit save/discard.
     *
     * @param appId The app ID whose container config should be restored.
     */
    fun restoreOriginalConfig(appId: String) {
        IntentLaunchManager.restoreOriginalConfiguration(context, appId)
        IntentLaunchManager.clearTemporaryOverride(appId)
    }
}
