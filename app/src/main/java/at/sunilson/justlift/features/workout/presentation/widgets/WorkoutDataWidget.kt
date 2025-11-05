package at.sunilson.justlift.features.workout.presentation.widgets

import android.graphics.Paint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import at.sunilson.justlift.features.workout.data.VitruvianDeviceManager
import at.sunilson.justlift.shared.presentation.theme.JustLiftTheme
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WorkoutDataWidget(
    modifier: Modifier = Modifier,
    workoutState: VitruvianDeviceManager.WorkoutState,
    machineState: VitruvianDeviceManager.MachineState
) {
    val animatedCablePositionLeft by animateFloatAsState(
        targetValue = machineState.positionCableLeft.toFloat(),
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
    )

    val animatedCablePositionRight by animateFloatAsState(
        targetValue = machineState.positionCableRight.toFloat(),
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
    )

    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(16.dp))
        AnimatedVisibility(workoutState.autoStopInSeconds != null) {
            Column {
                Text("Auto stop in ${workoutState.autoStopInSeconds} seconds", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (workoutState.calibratingRepsCompleted < 3) {
                    Text("Calibration Repetitions: ${workoutState.calibratingRepsCompleted}")
                } else {

                    Text("Repetitions: ${workoutState.upwardRepetitionsCompleted}")
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Average Upward Force: ${"%.1f".format(workoutState.averageUpwardForce)}kg")
                Spacer(modifier = Modifier.height(16.dp))
                Text("Average Downward Force: ${"%.1f".format(workoutState.averageDownwardForce)}kg")
                Spacer(modifier = Modifier.height(16.dp))
                Text("Max Upward Force: ${"%.1f".format(workoutState.maxUpwardForce)}kg")
                Spacer(modifier = Modifier.height(16.dp))
                Text("Max Downward Force: ${"%.1f".format(workoutState.maxDownwardForce)}kg")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Left cable force: ${machineState.forceLeftCable}kg")
                Spacer(modifier = Modifier.height(16.dp))
                Text("Right cable force: ${machineState.forceRightCable}kg")
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("L", style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.width(16.dp))
            LinearProgressIndicator(
                modifier = Modifier
                    .weight(1f)
                    .height(24.dp),
                progress = { animatedCablePositionLeft })
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("R", style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.width(16.dp))
            LinearProgressIndicator(
                modifier = Modifier
                    .weight(1f)
                    .height(24.dp),
                progress = { animatedCablePositionRight })
        }

    }

}

@PreviewLightDark
@Composable
private fun WorkoutDataWidgetPreview() {
    JustLiftTheme {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            WorkoutDataWidget(
                workoutState = VitruvianDeviceManager.WorkoutState(
                    calibratingRepsCompleted = 3,
                    maxReps = null,
                    upwardRepetitionsCompleted = 5,
                    downwardRepetitionsCompleted = 5,
                    timeElapsed = 120.toDuration(DurationUnit.SECONDS),
                    autoStopInSeconds = 30,
                    averageUpwardForce = 20.0,
                    averageDownwardForce = 15.0,
                    maxUpwardForce = 30.0,
                    maxDownwardForce = 25.0
                ),
                machineState = VitruvianDeviceManager.MachineState(
                    forceLeftCable = 22.5,
                    forceRightCable = 20.0,
                    positionCableLeft = 0.65,
                    positionCableRight = 0.4
                )
            )
        }
    }

}
