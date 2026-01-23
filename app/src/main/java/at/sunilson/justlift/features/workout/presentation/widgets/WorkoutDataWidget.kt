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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import at.sunilson.justlift.features.workout.data.VitruvianDeviceManager
import at.sunilson.justlift.shared.presentation.components.GlassCard
import at.sunilson.justlift.shared.presentation.components.GlassSurface
import at.sunilson.justlift.shared.presentation.theme.JustLiftTheme
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Composable
private fun DataRow(
    label: String,
    value: String,
    highlight: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun WorkoutDataWidget(
    modifier: Modifier = Modifier,
    workoutState: VitruvianDeviceManager.WorkoutState?,
    machineState: VitruvianDeviceManager.MachineState?,
    exerciseName: String? = null
) {
    if (workoutState == null) return

    val glassColors = JustLiftTheme.glassColors

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ProvideTextStyle(MaterialTheme.typography.titleLarge) {
            // Exercise name header
            exerciseName?.let {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    tint = glassColors.tintPrimary,
                    elevation = 4.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            it,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Auto-stop countdown
            AnimatedVisibility(workoutState.autoStopInSeconds != null && machineState != null) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    tint = glassColors.tintPrimary.copy(alpha = 0.3f),
                    elevation = 8.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        JustLiftTheme.extendedColors.gradientStart.copy(alpha = 0.2f),
                                        JustLiftTheme.extendedColors.gradientEnd.copy(alpha = 0.2f)
                                    )
                                )
                            )
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Auto stop in ${workoutState.autoStopInSeconds}s",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Main workout stats card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Reps section with highlight
                    if (workoutState.calibratingRepsCompleted < 3) {
                        DataRow(
                            label = "Calibration Reps",
                            value = "${workoutState.calibratingRepsCompleted}",
                            highlight = true
                        )
                    } else {
                        DataRow(
                            label = "Repetitions",
                            value = "${workoutState.upwardRepetitionsCompleted}",
                            highlight = true
                        )
                    }

                    // Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0f),
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0f)
                                    )
                                )
                            )
                    )

                    // Force stats
                    DataRow(
                        label = "Avg. Upward Force",
                        value = "${"%.1f".format(workoutState.averageUpwardForce)}kg"
                    )
                    DataRow(
                        label = "Avg. Downward Force",
                        value = "${"%.1f".format(workoutState.averageDownwardForce)}kg"
                    )
                    DataRow(
                        label = "Max Upward Force",
                        value = "${"%.1f".format(workoutState.maxUpwardForce)}kg"
                    )
                    DataRow(
                        label = "Max Downward Force",
                        value = "${"%.1f".format(workoutState.maxDownwardForce)}kg"
                    )

                    // Position stats if available
                    if (workoutState.avgMinPositionLeft > 0 || workoutState.avgMinPositionRight > 0) {
                        DataRow(
                            label = "Avg. Peak Bottom",
                            value = "${"%.0f".format((workoutState.avgMinPositionLeft + workoutState.avgMinPositionRight) / 2.0 * 100)}%"
                        )
                    }
                    if (workoutState.avgMaxPositionLeft > 0 || workoutState.avgMaxPositionRight > 0) {
                        DataRow(
                            label = "Avg. Peak Top",
                            value = "${"%.0f".format((workoutState.avgMaxPositionLeft + workoutState.avgMaxPositionRight) / 2.0 * 100)}%"
                        )
                    }
                }
            }

            // Live machine state card
            if (machineState != null) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    tint = glassColors.tintSecondary,
                    elevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "Live Cable Data",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Left cable
                            GlassSurface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "Left",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "${"%.1f".format(machineState.forceLeftCable)}kg",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Right cable
                            GlassSurface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "Right",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "${"%.1f".format(machineState.forceRightCable)}kg",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Position progress bars
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Left position
                            CablePositionBar(
                                label = "L",
                                progress = machineState.positionCableLeft.toFloat()
                            )
                            // Right position
                            CablePositionBar(
                                label = "R",
                                progress = machineState.positionCableRight.toFloat()
                            )
                        }
                    }
                }
            }
        }
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
            label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                        )
                    )
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            "${(animatedProgress * 100).toInt()}%",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(36.dp)
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
                exerciseName = "Bicep Curls",
                workoutState = VitruvianDeviceManager.WorkoutState(
                    calibratingRepsCompleted = 3,
                    maxReps = null,
                    upwardRepetitionsCompleted = 12,
                    downwardRepetitionsCompleted = 12,
                    timeElapsed = 45.toDuration(DurationUnit.SECONDS),
                    averageUpwardForce = 25.0,
                    averageDownwardForce = 20.0,
                    maxUpwardForce = 35.0,
                    maxDownwardForce = 30.0
                ),
                machineState = null
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
