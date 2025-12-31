package at.sunilson.justlift.features.workout.presentation.widgets

import android.graphics.Paint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import at.sunilson.justlift.features.workout.data.VitruvianDeviceManager
import at.sunilson.justlift.shared.presentation.theme.JustLiftTheme
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Composable
private fun DataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WorkoutDataWidget(
    modifier: Modifier = Modifier,
    workoutState: VitruvianDeviceManager.WorkoutState,
    machineState: VitruvianDeviceManager.MachineState?
) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        // Provide a larger default text style within this widget for better readability
        ProvideTextStyle(MaterialTheme.typography.titleLarge) {
            Spacer(modifier = Modifier.height(16.dp))
            // Show auto-stop countdown only for the live workout (not for previous set rendering)
            AnimatedVisibility(workoutState.autoStopInSeconds != null && machineState != null) {
                Column {
                    Text(
                        "Auto stop in ${workoutState.autoStopInSeconds} seconds",
                        style = MaterialTheme.typography.displaySmall
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (workoutState.calibratingRepsCompleted < 3) {
                        DataRow(
                            label = "Calibration Reps",
                            value = "${workoutState.calibratingRepsCompleted}"
                        )
                    } else {
                        DataRow(
                            label = "Repetitions",
                            value = "${workoutState.upwardRepetitionsCompleted}"
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    DataRow(
                        label = "Avg. Upward Force",
                        value = "${"%.1f".format(workoutState.averageUpwardForce)}kg"
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    DataRow(
                        label = "Avg. Downward Force",
                        value = "${"%.1f".format(workoutState.averageDownwardForce)}kg"
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    DataRow(
                        label = "Max Upward Force",
                        value = "${"%.1f".format(workoutState.maxUpwardForce)}kg"
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    DataRow(
                        label = "Max Downward Force",
                        value = "${"%.1f".format(workoutState.maxDownwardForce)}kg"
                    )
                    if (workoutState.avgMinPositionLeft > 0 || workoutState.avgMinPositionRight > 0) {
                        Spacer(modifier = Modifier.height(24.dp))
                        DataRow(
                            label = "Avg. Peak Bottom",
                            value = "${"%.0f".format((workoutState.avgMinPositionLeft + workoutState.avgMinPositionRight) / 2.0 * 100)}%"
                        )
                    }
                    if (workoutState.avgMaxPositionLeft > 0 || workoutState.avgMaxPositionRight > 0) {
                        Spacer(modifier = Modifier.height(24.dp))
                        DataRow(
                            label = "Avg. Peak Top",
                            value = "${"%.0f".format((workoutState.avgMaxPositionLeft + workoutState.avgMaxPositionRight) / 2.0 * 100)}%"
                        )
                    }
                }
            }

            // Live machine/cable information is only shown when available
            if (machineState != null) {
                Spacer(modifier = Modifier.height(32.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        DataRow(
                            label = "Left cable force",
                            value = "${machineState.forceLeftCable}kg"
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        DataRow(
                            label = "Right cable force",
                            value = "${machineState.forceRightCable}kg"
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("L", style = MaterialTheme.typography.displaySmall)
                    Spacer(modifier = Modifier.width(16.dp))
                    LinearProgressIndicator(
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp),
                        progress = { machineState.positionCableLeft.toFloat() })
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("R", style = MaterialTheme.typography.displaySmall)
                    Spacer(modifier = Modifier.width(16.dp))
                    LinearProgressIndicator(
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp),
                        progress = { machineState.positionCableRight.toFloat() })
                }
            }
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
