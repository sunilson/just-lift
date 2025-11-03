package at.sunilson.justlift.features.workout.presentation.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.sunilson.justlift.features.workout.data.VitruvianDeviceManager

@Composable
fun WorkoutDataWidget(
    modifier: Modifier = Modifier,
    workoutState: VitruvianDeviceManager.WorkoutState,
    machineState: VitruvianDeviceManager.MachineState
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (workoutState.calibratingRepsCompleted < 3) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Calibration Repetitions: ${workoutState.calibratingRepsCompleted}")
        } else {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Repetitions: ${workoutState.upwardRepetitionsCompleted}")
        }
        if (workoutState.autoStopInSeconds != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Auto stop in seconds: ${workoutState.autoStopInSeconds}")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Left cable force: ${machineState.forceLeftCable}kg")
        Spacer(modifier = Modifier.height(16.dp))
        Text("Right cable force: ${machineState.forceRightCable}kg")
        Spacer(modifier = Modifier.height(16.dp))
        Text("Left Cable Position: ${machineState.positionCableLeft}")
        LinearProgressIndicator(progress = { machineState.positionCableLeft.toFloat() })
        Spacer(modifier = Modifier.height(16.dp))
        Text("Right Cable Position: ${machineState.positionCableRight}")
        LinearProgressIndicator(progress = { machineState.positionCableRight.toFloat() })
    }

}
