package at.sunilson.justlift.ui.workout

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import at.sunilson.justlift.bluetooth.VitruvianDeviceManager
import com.juul.kable.ExperimentalApi
import com.juul.kable.Peripheral

@OptIn(ExperimentalApi::class)
@Composable
fun WorkoutScreen(
    connectedDevice: Peripheral?,
    workoutState: VitruvianDeviceManager.WorkoutState?,
    onStartWorkoutClicked: () -> Unit,
    onStopWorkoutClicked: () -> Unit,
    onDisconnectClicked: () -> Unit
) {
    Column {
        Text("Workout Screen")
        Text("Connected device: ${connectedDevice?.name ?: "None"}")
        Text("Workout state: $workoutState")
        Button(onClick = { onStartWorkoutClicked() }) {
            Text("Start Workout")
        }
        Button(onClick = { onStopWorkoutClicked() }) {
            Text("Stop Workout")
        }
        Button(onClick = { onDisconnectClicked() }) {
            Text("Disconnect Device")
        }
    }
}
