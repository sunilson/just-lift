package at.sunilson.justlift.features.workout.presentation.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.sunilson.justlift.features.workout.presentation.WorkoutViewModel
import com.juul.kable.ExperimentalApi
import com.juul.kable.State

@OptIn(ExperimentalApi::class)
@Composable
fun WorkoutScreen(
    state: WorkoutViewModel.State,
    onUseNoRepLimitChange: (Boolean) -> Unit = {},
    onEccentricSliderValueChange: (Float) -> Unit = {},
    onRepetitionsSliderValueChange: (Float) -> Unit = {},
    onStartWorkoutClicked: () -> Unit = {},
    onStopWorkoutClicked: () -> Unit = {},
    onDisconnectClicked: () -> Unit = {}
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(style = MaterialTheme.typography.headlineLarge, text = "Just Lift")

        Spacer(modifier = Modifier.height(48.dp))
        Text("Connected device: ${state.connectedPeripheral?.name ?: "None"}")

        Spacer(modifier = Modifier.height(16.dp))
        Text("Workout state: ${state.workoutState}")

        Spacer(modifier = Modifier.height(16.dp))
        Text("Machine state: ${state.machineState}")

        Spacer(modifier = Modifier.height(16.dp))
        Text("Auto start in seconds: ${state.autoStartInSeconds ?: "N/A"}")

        Spacer(modifier = Modifier.height(16.dp))
        Text("Auto stop in seconds: ${state.workoutState?.autoStopInSeconds ?: "N/A"}")

        Spacer(modifier = Modifier.height(16.dp))
        Text("Eccentric Percentage: ${(state.eccentricSliderValue * 100).toInt()}%")
        Slider(
            value = state.eccentricSliderValue,
            onValueChange = onEccentricSliderValueChange,
            valueRange = 0f..1.3f,
            steps = 14,
            enabled = state.workoutState == null
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text("No rep limit")
        Checkbox(checked = state.useNoRepLimit, onCheckedChange = { onUseNoRepLimitChange(it) }, enabled = state.workoutState == null)

        AnimatedVisibility(!state.useNoRepLimit) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Repetitions: ${state.repetitionsSliderValue}")
                Slider(
                    value = state.repetitionsSliderValue.toFloat(),
                    onValueChange = onRepetitionsSliderValueChange,
                    valueRange = 1f..20f,
                    steps = 19,
                    enabled = state.workoutState == null
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        if (state.connectedPeripheralState != State.Disconnected()) {
            Button(onClick = { onDisconnectClicked() }) {
                Text("Disconnect Device")
            }
            if (state.workoutState == null) {
                Button(onClick = { onStartWorkoutClicked() }) {
                    Text("Start Workout")
                }
            }

            if (state.workoutState != null) {
                Button(onClick = { onStopWorkoutClicked() }) { Text("Stop Workout") }
            }
        }
    }
}
