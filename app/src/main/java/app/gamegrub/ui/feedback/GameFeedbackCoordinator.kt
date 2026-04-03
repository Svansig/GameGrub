package app.gamegrub.ui.feedback

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.gamegrub.api.compatibility.GameFeedbackUtils
import app.gamegrub.ui.component.dialog.state.GameFeedbackDialogState
import app.gamegrub.ui.utils.SnackbarManager
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
    private val viewModelScope: androidx.lifecycle.viewModelScope,
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
                    SnackbarManager.show("Thank you for your feedback!")
                } else {
                    SnackbarManager.show("Failed to submit feedback")
                }
            } catch (e: Exception) {
                Timber.e(e, "GameFeedbackCoordinator: Error submitting game feedback")
                SnackbarManager.show("Error submitting feedback")
            } finally {
                onComplete()
            }
        }
    }
}
