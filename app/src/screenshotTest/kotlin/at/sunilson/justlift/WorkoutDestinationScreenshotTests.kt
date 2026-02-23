package at.sunilson.justlift

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import at.sunilson.justlift.features.workout.data.VitruvianDeviceManager
import at.sunilson.justlift.features.workout.data.VitruvianDeviceManager.EchoDifficulty
import at.sunilson.justlift.features.workout.presentation.WorkoutViewModel
import at.sunilson.justlift.features.workout.presentation.widgets.WorkoutConfigurationWidget
import at.sunilson.justlift.features.workout.presentation.widgets.WorkoutDataWidget
import at.sunilson.justlift.features.workout.presentation.widgets.DifficultySettingsSheet
import at.sunilson.justlift.features.workout.presentation.history.TendenciesSheet
import at.sunilson.justlift.features.workout.presentation.history.ExerciseTrend
import at.sunilson.justlift.features.workout.presentation.history.TrendTimeframe
import at.sunilson.justlift.features.workout.presentation.widgets.ConnectionWidget
import at.sunilson.justlift.features.workout.presentation.preview.FakePeripheral
import at.sunilson.justlift.shared.presentation.theme.JustLiftTheme
import com.android.tools.screenshot.PreviewTest
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@PreviewTest
@PreviewLightDark
@Composable
private fun `Workout configuration widget`() {
    JustLiftTheme {
        Box(modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
        ) {
            WorkoutConfigurationWidget(
                state = WorkoutViewModel.State(
                    eccentricSliderValue = 70f,
                    repetitionsSliderValue = 10,
                    echoDifficulty = EchoDifficulty.HARDER
                )
            )
        }
    }
}

@PreviewTest
@PreviewLightDark
@Composable
private fun `Workout data widget with live data`() {
    JustLiftTheme {
        Box(modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
        ) {
            WorkoutDataWidget(
                workoutState = VitruvianDeviceManager.WorkoutState(
                    calibratingRepsCompleted = 3,
                    maxReps = 10,
                    upwardRepetitionsCompleted = 8,
                    downwardRepetitionsCompleted = 8,
                    timeElapsed = 60.toDuration(DurationUnit.SECONDS),
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
                ),
                exerciseName = "Bicep Curl"
            )
        }
    }
}

@PreviewTest
@PreviewLightDark
@Composable
private fun `Workout data widget without live data`() {
    JustLiftTheme {
        Box(modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
        ) {
            WorkoutDataWidget(
                workoutState = VitruvianDeviceManager.WorkoutState(
                    calibratingRepsCompleted = 3,
                    maxReps = 10,
                    upwardRepetitionsCompleted = 12,
                    downwardRepetitionsCompleted = 12,
                    timeElapsed = 90.toDuration(DurationUnit.SECONDS),
                    averageUpwardForce = 28.0,
                    averageDownwardForce = 22.5,
                    maxUpwardForce = 35.0,
                    maxDownwardForce = 30.0
                ),
                machineState = null,
                exerciseName = "Chest Press"
            )
        }
    }
}

@PreviewTest
@PreviewLightDark
@Composable
private fun `Difficulty settings sheet`() {
    JustLiftTheme {
        Box(modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
        ) {
            DifficultySettingsSheet(
                selected = EchoDifficulty.HARD,
                onSelect = {},
                gain = 1.5f,
                onGainChange = {},
                capKg = 35f,
                onCapChange = {},
                useTts = true,
                onUseTtsChange = {},
                twoUserMode = false,
                onTwoUserModeChange = {},
                onResetSelected = {},
                onEditExerciseNames = {}
            )
        }
    }
}

@PreviewTest
@PreviewLightDark
@Composable
private fun `Tendencies sheet`() {
    JustLiftTheme {
        Box(modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
        ) {
            TendenciesSheet(
                tendencies = listOf(
                    ExerciseTrend("Bicep Curl (HARD)", 2.5, 1.2, 45.0, 42.0),
                    ExerciseTrend("Squat (NORMAL)", -1.5, -0.8, 80.0, 75.0),
                    ExerciseTrend("Chest Press (HARDER)", 5.2, 2.8, 55.0, 50.0)
                ),
                selectedTimeframe = TrendTimeframe.ONE_WEEK,
                onTimeframeSelected = {},
                onInfoClick = {}
            )
        }
    }
}

@PreviewTest
@PreviewLightDark
@Composable
private fun `Connection widget with devices`() {
    JustLiftTheme {
        Box(modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
        ) {
            ConnectionWidget(
                availableDevices = listOf(
                    FakePeripheral("Vitruvian V-Form"),
                    FakePeripheral("Vitruvian Trainer+")
                ),
                connectedPeripheral = null,
                isAutoConnecting = false,
                savedDeviceName = "Vitruvian V-Form",
                onDeviceSelected = {},
                onClearSavedDevice = {},
                onDisconnectClicked = {}
            )
        }
    }
}

@PreviewTest
@PreviewLightDark
@Composable
private fun `Connection widget connected`() {
    JustLiftTheme {
        Box(modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
        ) {
            ConnectionWidget(
                availableDevices = emptyList(),
                connectedPeripheral = FakePeripheral("Vitruvian V-Form"),
                isAutoConnecting = false,
                savedDeviceName = null,
                onDeviceSelected = {},
                onClearSavedDevice = {},
                onDisconnectClicked = {}
            )
        }
    }
}
