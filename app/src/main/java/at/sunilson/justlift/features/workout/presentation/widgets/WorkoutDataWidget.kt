package at.sunilson.justlift.features.workout.presentation.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import at.sunilson.justlift.features.workout.data.VitruvianDeviceManager
import at.sunilson.justlift.shared.presentation.theme.JustLiftTheme
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Composable
fun WorkoutDataWidget(
    modifier: Modifier = Modifier,
    workoutState: VitruvianDeviceManager.WorkoutState?,
    machineState: VitruvianDeviceManager.MachineState?,
    exerciseName: String? = null
) {
    if (workoutState == null) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Exercise name header
        exerciseName?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        // Auto-stop countdown
        AnimatedVisibility(workoutState.autoStopInSeconds != null && machineState != null) {
            Text(
                text = "Stop: ${workoutState.autoStopInSeconds}s",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(16.dp)
            )
        }

        // Main stats card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Primary stat - Reps (prominent display)
            val repsLabel = if (workoutState.calibratingRepsCompleted < 3) "Calibrating" else "Reps"
            val repsValue = if (workoutState.calibratingRepsCompleted < 3)
                workoutState.calibratingRepsCompleted
            else
                workoutState.upwardRepetitionsCompleted

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = repsLabel.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$repsValue",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 1.dp
            )

            // Force stats header
            Text(
                text = "FORCE (kg)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // Force stats - 2x2 grid with better structure
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Upward column
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "PUSH ↑",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    StatRow(label = "Avg", value = "%.0f".format(workoutState.averageUpwardForce))
                    Spacer(modifier = Modifier.height(4.dp))
                    StatRow(label = "Max", value = "%.0f".format(workoutState.maxUpwardForce))
                }

                // Downward column
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "PULL ↓",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    StatRow(label = "Avg", value = "%.0f".format(workoutState.averageDownwardForce))
                    Spacer(modifier = Modifier.height(4.dp))
                    StatRow(label = "Max", value = "%.0f".format(workoutState.maxDownwardForce))
                }
            }

            // Position stats
            if (workoutState.avgMinPositionLeft > 0 || workoutState.avgMinPositionRight > 0 ||
                workoutState.avgMaxPositionLeft > 0 || workoutState.avgMaxPositionRight > 0) {

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 1.dp
                )

                Text(
                    text = "RANGE OF MOTION",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (workoutState.avgMinPositionLeft > 0 || workoutState.avgMinPositionRight > 0) {
                        StatCard(
                            label = "Bottom",
                            value = "${"%.0f".format((workoutState.avgMinPositionLeft + workoutState.avgMinPositionRight) / 2.0 * 100)}%",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (workoutState.avgMaxPositionLeft > 0 || workoutState.avgMaxPositionRight > 0) {
                        StatCard(
                            label = "Top",
                            value = "${"%.0f".format((workoutState.avgMaxPositionLeft + workoutState.avgMaxPositionRight) / 2.0 * 100)}%",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Live machine state
        if (machineState != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Live",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )

                // Cable forces side by side
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CableForceDisplay(
                        label = "L",
                        value = "${"%.0f".format(machineState.forceLeftCable)}",
                        modifier = Modifier.weight(1f)
                    )
                    CableForceDisplay(
                        label = "R",
                        value = "${"%.0f".format(machineState.forceRightCable)}",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Position bars
                CablePositionBar(
                    label = "L",
                    progress = machineState.positionCableLeft.toFloat()
                )
                CablePositionBar(
                    label = "R",
                    progress = machineState.positionCableRight.toFloat()
                )
            }
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun CableForceDisplay(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

@Composable
private fun CablePositionBar(
    label: String,
    progress: Float
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "cable_progress"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.width(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "${(animatedProgress * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

@PreviewLightDark
@Composable
private fun WorkoutDataWidgetWithExercisePreview() {
    JustLiftTheme {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            WorkoutDataWidget(
                exerciseName = "Bicep Curl",
                workoutState = VitruvianDeviceManager.WorkoutState(
                    calibratingRepsCompleted = 3,
                    maxReps = null,
                    upwardRepetitionsCompleted = 8,
                    downwardRepetitionsCompleted = 8,
                    timeElapsed = 45.toDuration(DurationUnit.SECONDS),
                    averageUpwardForce = 25.5,
                    averageDownwardForce = 20.3,
                    maxUpwardForce = 32.0,
                    maxDownwardForce = 28.5
                ),
                machineState = VitruvianDeviceManager.MachineState(
                    forceLeftCable = 24.5,
                    forceRightCable = 23.0,
                    positionCableLeft = 0.72,
                    positionCableRight = 0.68
                )
            )
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
                exerciseName = "Chest Press",
                workoutState = VitruvianDeviceManager.WorkoutState(
                    calibratingRepsCompleted = 3,
                    maxReps = null,
                    upwardRepetitionsCompleted = 12,
                    downwardRepetitionsCompleted = 12,
                    timeElapsed = 120.toDuration(DurationUnit.SECONDS),
                    averageUpwardForce = 28.0,
                    averageDownwardForce = 22.5,
                    maxUpwardForce = 35.0,
                    maxDownwardForce = 30.0
                ),
                machineState = null
            )
        }
    }
}
