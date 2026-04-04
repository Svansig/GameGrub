package app.gamegrub.ui.feedback

import android.content.Context
import app.gamegrub.R
import app.gamegrub.api.compatibility.GameFeedbackUtils
import app.gamegrub.ui.component.dialog.state.GameFeedbackDialogState
import app.gamegrub.ui.utils.SnackbarManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Coordinates game feedback submission workflow.
 *
 * This class encapsulates the logic for:
 * - Submitting feedback via the worker API
 * - Handling success/failure states
 * - Showing appropriate user messaging
 *
 * The coordinator preserves all existing feedback behavior from GameGrubMain while
 * moving it into a testable boundary that doesn't live in composable callbacks.
 *
 * @param context Application context used for feedback submission.
 * @param viewModelScope Coroutine scope for launching async feedback operations.
 */
class GameFeedbackCoordinator(
    private val context: Context,
    private val viewModelScope: CoroutineScope,
) {
    /**
     * Submits game feedback based on the provided feedback state.
     *
     * This method handles the entire feedback submission workflow including:
     * - Calling the feedback API
     * - Handling success/failure outcomes
     * - Showing appropriate snackbar messages
     *
     * @param feedbackState The feedback state containing rating, tags, and text.
     * @param onComplete Callback invoked when feedback submission completes (success or failure).
     */
    fun submitFeedback(
        feedbackState: GameFeedbackDialogState,
        onComplete: () -> Unit,
    ) {
        val appId = feedbackState.appId

        viewModelScope.launch {
            try {
                val result = GameFeedbackUtils.submitGameFeedback(
                    context = context,
                    appId = appId,
                    rating = feedbackState.rating,
                    tags = feedbackState.selectedTags.toList(),
                    notes = feedbackState.feedbackText.takeIf { it.isNotBlank() },
                )

                if (result) {
                    SnackbarManager.show(context.getString(R.string.game_feedback_submit_success))
                } else {
                    SnackbarManager.show(context.getString(R.string.game_feedback_submit_failed))
                }
            } catch (e: Exception) {
                Timber.e(e, "GameFeedbackCoordinator: Error submitting game feedback")
                SnackbarManager.show(context.getString(R.string.game_feedback_submit_error))
            } finally {
                onComplete()
            }
        }
    }
}
