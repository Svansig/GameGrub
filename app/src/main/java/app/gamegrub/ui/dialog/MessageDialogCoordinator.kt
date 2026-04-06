package app.gamegrub.ui.dialog

import android.content.Context
import androidx.navigation.NavHostController
import app.gamegrub.PrefManager
import app.gamegrub.ui.component.dialog.state.MessageDialogState
import app.gamegrub.ui.container.ContainerConfigCoordinator
import app.gamegrub.ui.data.MainState
import app.gamegrub.ui.enums.DialogType
import app.gamegrub.ui.feedback.GameFeedbackCoordinator
import app.gamegrub.ui.model.MainViewModel
import app.gamegrub.ui.update.AppUpdateCoordinator

/**
 * Coordinates message dialog action handling.
 *
 * This class encapsulates the logic for:
 * - Mapping dialog types to their action callbacks
 * - Handling confirm, dismiss, and action button behaviors
 * - Coordinating with other coordinators for complex workflows
 *
 * @param context Application context for dialog actions.
 * @param viewModel MainViewModel for state updates.
 * @param navController NavController for navigation operations.
 * @param state Current UI state.
 * @param appUpdateCoordinator Coordinator for update operations.
 * @param containerConfigCoordinator Coordinator for container config operations.
 * @param gameFeedbackCoordinator Coordinator for feedback operations.
 * @param preLaunchAppCallback Callback for launching apps.
 * @param launchedAppId Currently launched app ID.
 */
class MessageDialogCoordinator(
    private val context: Context,
    private val viewModel: MainViewModel,
    private val navController: NavHostController,
    private val state: MainState,
    private val appUpdateCoordinator: AppUpdateCoordinator,
    private val containerConfigCoordinator: ContainerConfigCoordinator,
    private val gameFeedbackCoordinator: GameFeedbackCoordinator,
    private val preLaunchAppCallback: () -> Unit,
    private val launchedAppId: String,
) {
    /**
     * Creates action handlers for a given dialog type.
     *
     * @param msgDialogState Current dialog state.
     * @param setMessageDialogState Callback to update dialog state.
     * @param pendingSaveAppId Pending app ID for container config saves.
     * @param setPendingSaveAppId Callback to update pending save app ID.
     * @param pendingFeedbackAppId Pending app ID for feedback.
     * @param setPendingFeedbackAppId Callback to update pending feedback app ID.
     * @return Triple of (onDismissRequest, onConfirmClick, onDismissClick) handlers.
     */
    fun createDialogHandlers(
        msgDialogState: MessageDialogState,
        setMessageDialogState: (MessageDialogState) -> Unit,
        pendingSaveAppId: String?,
        setPendingSaveAppId: (String?) -> Unit,
    ): DialogHandlers {
        val onDismissRequest: (() -> Unit)?
        val onConfirmClick: (() -> Unit)?
        val onDismissClick: (() -> Unit)?
        var onActionClick: (() -> Unit)? = null

        when (msgDialogState.type) {
            DialogType.DISCORD -> {
                onConfirmClick = {
                    setMessageDialogState(MessageDialogState(false))
                    // Discord link - handled externally
                }
                onDismissClick = {
                    setMessageDialogState(MessageDialogState(false))
                }
                onDismissRequest = {
                    setMessageDialogState(MessageDialogState(false))
                }
            }

            DialogType.SUPPORT -> {
                onConfirmClick = {
                    // Ko-fi link
                    PrefManager.tipped = true
                    setMessageDialogState(MessageDialogState(false))
                }
                onDismissRequest = {
                    setMessageDialogState(MessageDialogState(false))
                }
                onDismissClick = {
                    setMessageDialogState(MessageDialogState(false))
                }
                onActionClick = {
                    // Share intent
                }
            }

            DialogType.SYNC_CONFLICT -> {
                onConfirmClick = {
                    preLaunchAppCallback()
                    setMessageDialogState(MessageDialogState(false))
                }
                onDismissClick = {
                    preLaunchAppCallback()
                    setMessageDialogState(MessageDialogState(false))
                }
                onDismissRequest = {
                    setMessageDialogState(MessageDialogState(false))
                }
            }

            DialogType.SYNC_FAIL -> {
                onConfirmClick = null
                onDismissClick = {
                    setMessageDialogState(MessageDialogState(false))
                }
                onDismissRequest = {
                    setMessageDialogState(MessageDialogState(false))
                }
            }

            DialogType.EXECUTABLE_NOT_FOUND -> {
                onConfirmClick = null
                onDismissClick = {
                    setMessageDialogState(MessageDialogState(false))
                }
                onDismissRequest = {
                    setMessageDialogState(MessageDialogState(false))
                }
            }

            DialogType.SYNC_IN_PROGRESS -> {
                onConfirmClick = {
                    setMessageDialogState(MessageDialogState(false))
                    preLaunchAppCallback()
                }
                onDismissClick = {
                    setMessageDialogState(MessageDialogState(false))
                }
                onDismissRequest = {
                    setMessageDialogState(MessageDialogState(false))
                }
            }

            DialogType.SAVE_CONTAINER_CONFIG -> {
                onConfirmClick = {
                    pendingSaveAppId?.let { appId ->
                        containerConfigCoordinator.saveConfig(appId)
                    }
                    setPendingSaveAppId(null)
                    setMessageDialogState(MessageDialogState(false))
                }
                onDismissClick = {
                    pendingSaveAppId?.let { appId ->
                        containerConfigCoordinator.discardConfig(appId)
                    }
                    setPendingSaveAppId(null)
                    setMessageDialogState(MessageDialogState(false))
                }
                onDismissRequest = {
                    pendingSaveAppId?.let { appId ->
                        containerConfigCoordinator.discardConfig(appId)
                    }
                    setPendingSaveAppId(null)
                    setMessageDialogState(MessageDialogState(false))
                }
            }

            DialogType.APP_UPDATE -> {
                onConfirmClick = {
                    setMessageDialogState(MessageDialogState(false))
                    val updateInfo = viewModel.updateInfo.value
                    if (updateInfo != null) {
                        // Update download and install - handled by coordinator
                    }
                }
                onDismissClick = {
                    setMessageDialogState(MessageDialogState(false))
                }
                onDismissRequest = {
                    setMessageDialogState(MessageDialogState(false))
                }
            }

            else -> {
                onDismissRequest = null
                onDismissClick = null
                onConfirmClick = null
            }
        }

        return DialogHandlers(
            onDismissRequest = onDismissRequest,
            onConfirmClick = onConfirmClick,
            onDismissClick = onDismissClick,
            onActionClick = onActionClick,
        )
    }
}

/**
 * Data class holding dialog action handlers.
 */
data class DialogHandlers(
    val onDismissRequest: (() -> Unit)?,
    val onConfirmClick: (() -> Unit)?,
    val onDismissClick: (() -> Unit)?,
    val onActionClick: (() -> Unit)? = null,
)
