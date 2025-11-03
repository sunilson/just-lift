package at.sunilson.justlift.features.workout.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import at.sunilson.justlift.features.workout.data.VitruvianDeviceManager
import at.sunilson.justlift.features.workout.presentation.connection.ConnectionScreen
import at.sunilson.justlift.features.workout.presentation.preview.FakePeripheral
import at.sunilson.justlift.shared.presentation.PreviewLightDarkDevices
import at.sunilson.justlift.shared.presentation.ScreenPreview
import com.juul.kable.ExperimentalApi
import com.juul.kable.Peripheral
import com.juul.kable.State
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@OptIn(ExperimentalApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    state: WorkoutViewModel.State,
    onDeviceSelected: (Peripheral) -> Unit = {},
    onUseNoRepLimitChange: (Boolean) -> Unit = {},
    onEccentricSliderValueChange: (Float) -> Unit = {},
    onRepetitionsSliderValueChange: (Float) -> Unit = {},
    onStartWorkoutClicked: () -> Unit = {},
    onStopWorkoutClicked: () -> Unit = {},
    onDisconnectClicked: () -> Unit = {}
) {
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = rememberStandardBottomSheetState(skipHiddenState = false))

    LaunchedEffect(state.connectedPeripheralState, state.availablePeripherals) {
        if (state.connectedPeripheralState is State.Connected && state.availablePeripherals.isEmpty()) {
            scaffoldState.bottomSheetState.hide()
        } else {
            scaffoldState.bottomSheetState.expand()
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetDragHandle = null,
        sheetSwipeEnabled = false,
        sheetContent = {
            ConnectionScreen(
                availableDevices = state.availablePeripherals.toList(),
                onDeviceSelected = onDeviceSelected,
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
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
        AnimatedVisibility(
            enter = fadeIn(),
            exit = fadeOut(),
            visible = scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded
        ) {
            Box(
                modifier = Modifier
                    .clickable {}
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            )
        }
    }
}

@PreviewLightDarkDevices
@Composable
private fun `Devices available`() {
    ScreenPreview {
        WorkoutScreen(
            state = WorkoutViewModel.State(
                availablePeripherals = persistentListOf(
                    FakePeripheral("Machine 1"),
                )
            )
        )
    }
}

@PreviewLightDarkDevices
@Composable
private fun `Connected but no workout started`() {
    ScreenPreview {
        WorkoutScreen(
            state = WorkoutViewModel.State(
                connectedPeripheral = FakePeripheral("Machine 1"),
                connectedPeripheralState = State.Connected(CoroutineScope(Dispatchers.Main)),
            )
        )
    }
}

@PreviewLightDarkDevices
@Composable
private fun `Workout in progress`() {
    ScreenPreview {
        WorkoutScreen(
            state = WorkoutViewModel.State(
                connectedPeripheral = FakePeripheral("Machine 1"),
                connectedPeripheralState = State.Connected(CoroutineScope(Dispatchers.Main)),
                workoutState = VitruvianDeviceManager.WorkoutState(
                    calibratingRepsCompleted = 3,
                    maxReps = 10,
                    upwardRepetitionsCompleted = 10,
                    downwardRepetitionsCompleted = 10,
                    timeElapsed = 10.toDuration(DurationUnit.SECONDS)
                ),
                machineState = VitruvianDeviceManager.MachineState(
                    forceLeftCable = 20.0,
                    forceRightCable = 22.0,
                    positionCableLeft = 0.5,
                    positionCableRight = 0.5
                )
            )
        )
    }
}
