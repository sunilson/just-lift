package at.sunilson.justlift.features.workout.presentation.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import at.sunilson.justlift.features.workout.presentation.widgets.WorkoutDataWidget

@Composable
fun HistoryOverlay(
    history: LazyPagingItems<WorkoutHistoryUiModel>?,
    onDismiss: () -> Unit
) {
    // Content - consume taps so they don't pass to scrim or underlying UI
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { /* consume */ }
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text("Workout History", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (history == null || history.itemCount == 0) {
            item {
                Text("No workouts yet")
            }
        } else {
            items(
                count = history.itemCount,
                key = history.itemKey { model ->
                    when (model) {
                        is WorkoutHistoryUiModel.Entry -> model.entry.timestampMillis
                        is WorkoutHistoryUiModel.Header -> "header_${model.date}"
                    }
                }
            ) { index ->
                when (val model = history[index]) {
                    is WorkoutHistoryUiModel.Header -> {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            model.date,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    is WorkoutHistoryUiModel.Entry -> {
                        val entry = model.entry
                        val timeText = remember(entry.timestampMillis) {
                            val fmt = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT)
                            fmt.format(java.util.Date(entry.timestampMillis))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                timeText,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            WorkoutDataWidget(
                                workoutState = entry.workoutState,
                                machineState = null
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                    null -> { /* Paging placeholder */ }
                }
            }
        }

        item {
            Button(onClick = onDismiss) { Text("Close") }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
