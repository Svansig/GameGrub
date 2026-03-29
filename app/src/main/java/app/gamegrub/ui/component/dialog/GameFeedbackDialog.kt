package app.gamegrub.ui.component.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.gamegrub.R
import app.gamegrub.ui.component.dialog.state.GameFeedbackDialogState
import timber.log.Timber

@OptIn(
    ExperimentalLayoutApi::class,
    ExperimentalMaterial3Api::class,
)
@Composable
fun GameFeedbackDialog(
    state: GameFeedbackDialogState,
    onStateChange: (GameFeedbackDialogState) -> Unit,
    onSubmit: (GameFeedbackDialogState) -> Unit,
    onDismiss: () -> Unit,
    onDiscordSupport: () -> Unit,
) {
    if (state.visible) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Title
                    Text(
                        text = stringResource(R.string.game_feedback_game_run),
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )

                    // Rating Stars
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        for (i in 1..5) {
                            Icon(
                                imageVector = if (i <= state.rating) Icons.Filled.Star else Icons.Filled.StarOutline,
                                contentDescription = "Star $i",
                                tint = if (i <=
                                    state.rating
                                ) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clickable {
                                        onStateChange(state.copy(rating = i))
                                    }
                                    .padding(4.dp),
                            )
                        }
                    }

                    // Tags Selection
                    Text(
                        text = stringResource(R.string.game_feedback_issues_question),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(bottom = 8.dp),
                    )

                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                    ) {
                        for (tag in GameFeedbackDialogState.AVAILABLE_TAGS) {
                            val isSelected = tag in state.selectedTags
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    val newTags = if (isSelected) {
                                        state.selectedTags - tag
                                    } else {
                                        state.selectedTags + tag
                                    }
                                    onStateChange(state.copy(selectedTags = newTags))
                                },
                                label = {
                                    Text(
                                        text = tag.replace("_", " ").capitalize(),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                },
                                modifier = Modifier.padding(end = 8.dp, bottom = 8.dp),
                            )
                        }
                    }

                    // Feedback text box
                    OutlinedTextField(
                        value = state.feedbackText,
                        onValueChange = { onStateChange(state.copy(feedbackText = it)) },
                        label = { Text(stringResource(R.string.describe_what_happened)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp)
                            .padding(bottom = 16.dp),
                        maxLines = 5,
                    )

                    // Discord support link
                    TextButton(
                        onClick = onDiscordSupport,
                        modifier = Modifier.padding(bottom = 16.dp),
                    ) {
                        Text(stringResource(R.string.get_support_on_discord))
                    }

                    // Dialog action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(state.dismissBtnText)
                        }

                        Button(
                            onClick = {
                                Timber.d("GameFeedback: Submit button clicked with rating=${state.rating}")
                                onSubmit(state)
                            },
                            modifier = Modifier.padding(start = 8.dp),
                            enabled = state.rating > 0, // Require at least a rating
                        ) {
                            Text(state.confirmBtnText)
                        }
                    }
                }
            }
        }
    }
}

private fun String.capitalize(): String {
    return this.split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { it.uppercase() }
    }
}
