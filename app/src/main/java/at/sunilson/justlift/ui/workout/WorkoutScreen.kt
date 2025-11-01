package at.sunilson.justlift.ui.workout

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.sunilson.justlift.bluetooth.VitruvianDeviceManager
import com.juul.kable.ExperimentalApi
import com.juul.kable.Peripheral

@OptIn(ExperimentalApi::class)
@Composable
fun WorkoutScreen(
    uiState: WorkoutViewModel.WorkoutUiState = WorkoutViewModel.WorkoutUiState(),
    connectedDevice: Peripheral? = null,
    workoutState: VitruvianDeviceManager.WorkoutState? = null,
    onEccentricSliderValueChange: (Float) -> Unit = {},
    onRepetitionsSliderValueChange: (Float) -> Unit = {},
    onStartWorkoutClicked: () -> Unit = {},
    onStopWorkoutClicked: () -> Unit = {},
    onDisconnectClicked: () -> Unit = {}
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(style = MaterialTheme.typography.headlineLarge, text = "Just Lift")

        Spacer(modifier = Modifier.height(48.dp))
        Text("Connected device: ${connectedDevice?.name ?: "None"}")

        Spacer(modifier = Modifier.height(16.dp))
        Text("Workout state: $workoutState")

        Spacer(modifier = Modifier.height(16.dp))
        Text("Eccentric Percentage: ${(uiState.eccentricSliderValue * 100).toInt()}%")
        Slider(value = uiState.eccentricSliderValue, onValueChange = onEccentricSliderValueChange, valueRange = 0f..1.3f, steps = 13)

        Spacer(modifier = Modifier.height(16.dp))
        Text("Repetitions: ${uiState.repetitionsSliderValue}")
        Slider(value = uiState.repetitionsSliderValue.toFloat(), onValueChange = onRepetitionsSliderValueChange, valueRange = 1f..20f, steps = 19)

        Spacer(modifier = Modifier.height(32.dp))
        if (connectedDevice != null) {
            Button(onClick = { onDisconnectClicked() }) {
                Text("Disconnect Device")
            }
            if (workoutState == null) {
                Button(onClick = { onStartWorkoutClicked() }) {
                    Text("Start Workout")
                }
            }

            if (workoutState != null) {
                Button(onClick = { onStopWorkoutClicked() }) { Text("Stop Workout") }
            }
        }
    }
}
