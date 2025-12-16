package at.sunilson.justlift.features.workout.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.interaction.MutableInteractionSource
import at.sunilson.justlift.features.workout.presentation.widgets.WorkoutDataWidget
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.ui.zIndex

@Composable
fun HistoryOverlay(
    history: List<WorkoutHistoryEntry>,
    onDismiss: () -> Unit
) {
    // Single stacking container so scrim and content share z-order and hit testing
    Box(modifier = Modifier.fillMaxSize()) {
        // Scrim - taps here dismiss
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { onDismiss() }
        )

        // Content container with top-close button (like connection bottom sheet)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Close button at the top right
            IconButton(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    // Ensure the close button is above the scrollable content for hit-testing
                    .zIndex(1f),
                onClick = onDismiss
            ) {
                Icon(imageVector = Icons.Outlined.Close, contentDescription = "Close")
            }

            // Content - consume taps so they don't pass to scrim or underlying UI
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { /* consume */ }
                    .padding(horizontal = 16.dp)
                    .padding(top = 40.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Workout History", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))
                if (history.isEmpty()) {
                    Text("No workouts yet")
                } else {
                    history.forEach { entry ->
                        val dateText = remember(entry.timestampMillis) {
                            val fmt = java.text.DateFormat.getDateTimeInstance()
                            fmt.format(java.util.Date(entry.timestampMillis))
                        }
                        Text(
                            dateText,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        WorkoutDataWidget(
                            workoutState = entry.workoutState,
                            machineState = null
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
                Button(onClick = onDismiss) { Text("Close") }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
