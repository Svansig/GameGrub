package app.gamegrub.ui.launch

import android.content.Context
import app.gamegrub.LaunchRequestManager
import app.gamegrub.R
import app.gamegrub.launch.needsSteamLogin
import app.gamegrub.ui.component.dialog.state.MessageDialogState
import app.gamegrub.ui.enums.DialogType
import app.gamegrub.ui.utils.SnackbarManager
import app.gamegrub.utils.container.ContainerUtils
import timber.log.Timber

/**
 * Consumes and rejects a pending external launch if Steam login is currently required.
 * @param context App context used for login checks and user messaging.
 */
fun consumePendingLaunchWithError(context: Context) {
    val request = LaunchRequestManager.peekPendingLaunchRequest() ?: return
    if (!needsSteamLogin(context, request.appId)) return
    LaunchRequestManager.consumePendingLaunchRequest()
    SnackbarManager.show(context.getString(R.string.intent_launch_steam_login_failed))
}

/**
 * Shows a standard "game not installed" dialog for failed launch resolution.
 * @param context App context used for localized dialog content.
 * @param originalAppId Original app id used to resolve display name.
 * @param requestAppId App id from the current launch request.
 * @param setMessageDialogState UI callback for posting dialog state.
 * @param logTag Timber tag used for warning output.
 */
fun showGameNotInstalledDialog(
    context: Context,
    originalAppId: String,
    requestAppId: String,
    setMessageDialogState: (MessageDialogState) -> Unit,
    logTag: String,
) {
    val appName = ContainerUtils.resolveGameName(originalAppId)
    Timber.tag(logTag).w("Game not installed: $appName ($requestAppId)")
    setMessageDialogState(
        MessageDialogState(
            visible = true,
            type = DialogType.SYNC_FAIL,
            title = context.getString(R.string.game_not_installed_title),
            message = context.getString(R.string.game_not_installed_message, appName),
            dismissBtnText = context.getString(R.string.ok),
        ),
    )
}
