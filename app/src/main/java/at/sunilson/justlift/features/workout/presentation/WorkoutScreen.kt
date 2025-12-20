package at.sunilson.justlift.features.workout.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import at.sunilson.justlift.features.workout.data.VitruvianDeviceManager
import at.sunilson.justlift.features.workout.data.VitruvianDeviceManager.EchoDifficulty
import at.sunilson.justlift.features.workout.presentation.preview.FakePeripheral
import at.sunilson.justlift.features.workout.presentation.widgets.ConnectionWidget
import at.sunilson.justlift.features.workout.presentation.widgets.PauseTimerWidget
import at.sunilson.justlift.features.workout.presentation.widgets.WorkoutConfigurationWidget
import at.sunilson.justlift.features.workout.presentation.widgets.WorkoutDataWidget
import at.sunilson.justlift.shared.presentation.PreviewLightDarkDevices
import at.sunilson.justlift.shared.presentation.ScreenPreview
import com.juul.kable.ExperimentalApi
import com.juul.kable.Peripheral
import com.juul.kable.State
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Tune
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import at.sunilson.justlift.features.workout.presentation.history.HistoryOverlay
import androidx.compose.ui.zIndex
import at.sunilson.justlift.features.workout.presentation.widgets.DifficultySettingsSheet

@OptIn(ExperimentalApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    state: WorkoutViewModel.State,
    onDeviceSelected: (Peripheral) -> Unit = {},
    onUseNoRepLimitChange: (Boolean) -> Unit = {},
    onEccentricSliderValueChange: (Float) -> Unit = {},
    onRepetitionsSliderValueChange: (Float) -> Unit = {},
    onEchoDifficultyChange: (EchoDifficulty) -> Unit = {},
    onOpenDifficultySettings: () -> Unit = {},
    onDismissDifficultySettings: () -> Unit = {},
    onDifficultySheetSelectDifficulty: (EchoDifficulty) -> Unit = {},
    onDifficultySheetUpdateGain: (Float) -> Unit = {},
    onDifficultySheetUpdateCap: (Float) -> Unit = {},
    onDifficultySheetResetSelected: () -> Unit = {},
    onStartWorkoutClicked: () -> Unit = {},
    onStopWorkoutClicked: () -> Unit = {},
    onDisconnectClicked: () -> Unit = {},
    onClearSavedDeviceClicked: () -> Unit = {},
    onHistoryClicked: () -> Unit = {},
    onDismissHistoryClicked: () -> Unit = {}
) {
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = rememberStandardBottomSheetState(skipHiddenState = false))
    val isWorkoutInProgress = state.workoutState != null && state.machineState != null
    val isConnected = state.connectedPeripheralState is State.Connected
    val starting = state.autoStartInSeconds != null
    val scope = rememberCoroutineScope()
    // Show the sheet when disconnected, hide when connected
    LaunchedEffect(isConnected) {
        if (isConnected) {
            scaffoldState.bottomSheetState.hide()
        } else {
            scaffoldState.bottomSheetState.expand()
        }
    }

    // Ensure the bottom sheet does not cover the history overlay
    LaunchedEffect(state.showHistory) {
        if (state.showHistory) {
            scaffoldState.bottomSheetState.hide()
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetDragHandle = null,
        sheetSwipeEnabled = isConnected,
        topBar = {
            TopAppBar(
                title = { Text("Just Lift") },
                actions = {
                    IconButton(onClick = onOpenDifficultySettings) {
                        Icon(imageVector = Icons.Outlined.Tune, contentDescription = "Difficulty settings")
                    }
                    IconButton(onClick = onHistoryClicked) {
                        Icon(imageVector = androidx.compose.material.icons.Icons.Outlined.History, contentDescription = "History")
                    }
                    IconButton(onClick = { scope.launch { scaffoldState.bottomSheetState.expand() } }) {
                        Icon(imageVector = Icons.Outlined.Bluetooth, contentDescription = "Connect")
                    }
                }
            )
        },
        sheetContent = {
            // Show a close button when already connected so the sheet can be dismissed
            if (isConnected) {
                Box(Modifier.fillMaxWidth()) {
                    IconButton(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        onClick = { scope.launch { scaffoldState.bottomSheetState.hide() } }
                    ) {
                        Icon(imageVector = Icons.Outlined.Close, contentDescription = "Close")
                    }
                    Column(Modifier.padding(top = 40.dp)) {
                        ConnectionWidget(
                            availableDevices = state.availablePeripherals.toList(),
                            connectedPeripheral = state.connectedPeripheral,
                            isAutoConnecting = state.isAutoConnecting,
                            savedDeviceName = state.savedDevice?.name ?: state.savedDevice?.id,
                            onDeviceSelected = onDeviceSelected,
                            onClearSavedDevice = onClearSavedDeviceClicked,
                            onDisconnectClicked = onDisconnectClicked
                        )
                    }
                }
            } else {
                ConnectionWidget(
                    availableDevices = state.availablePeripherals.toList(),
                    connectedPeripheral = state.connectedPeripheral,
                    isAutoConnecting = state.isAutoConnecting,
                    savedDeviceName = state.savedDevice?.name ?: state.savedDevice?.id,
                    onDeviceSelected = onDeviceSelected,
                    onClearSavedDevice = onClearSavedDeviceClicked,
                    onDisconnectClicked = onDisconnectClicked
                )
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (starting) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Auto start in seconds: ${state.autoStartInSeconds}")
                }

                if (isWorkoutInProgress) {
                    WorkoutDataWidget(
                        workoutState = state.workoutState,
                        machineState = state.machineState
                    )
                }

                if (!isWorkoutInProgress && isConnected && !starting) {
                    Spacer(modifier = Modifier.height(16.dp))
                    WorkoutConfigurationWidget(
                        state = state,
                        onUseNoRepLimitChange = onUseNoRepLimitChange,
                        onEccentricSliderValueChange = onEccentricSliderValueChange,
                        onRepetitionsSliderValueChange = onRepetitionsSliderValueChange,
                        onEchoDifficultyChange = onEchoDifficultyChange
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        "Lift and hold to start workout",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    // Show pause timer (if currently paused) slightly smaller below the instruction
                    state.pauseStartTimestamp?.let { pauseStart ->
                        Spacer(modifier = Modifier.height(8.dp))
                        PauseTimerWidget(pauseStartTimestamp = pauseStart)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                if (isConnected && !isWorkoutInProgress) {
                    Button(onClick = { onDisconnectClicked() }) {
                        Text("Disconnect Device")
                    }

                    // Show previous set data when available (after finishing a set)
                    state.previousWorkoutState?.let { previous ->
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Previous set",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        WorkoutDataWidget(
                            workoutState = previous,
                            machineState = null
                        )
                    }
                }
                if (isConnected && isWorkoutInProgress) {
                    Button(onClick = { onStopWorkoutClicked() }) { Text("Stop Workout") }
                }
            }

            // History overlay with animated in/out from the bottom
            AnimatedVisibility(
                visible = state.showHistory,
                enter = fadeIn(animationSpec = tween(150)) +
                    slideInVertically(
                        // Start just below the screen
                        initialOffsetY = { fullHeight -> fullHeight },
                        animationSpec = tween(250)
                    ),
                exit = fadeOut(animationSpec = tween(150)) +
                    slideOutVertically(
                        // Slide down off the screen
                        targetOffsetY = { fullHeight -> fullHeight },
                        animationSpec = tween(200)
                    )
            ) {
                Box(modifier = Modifier.fillMaxSize().zIndex(1f)) {
                    HistoryOverlay(
                        history = state.history.toList(),
                        onDismiss = onDismissHistoryClicked
                    )
                }
            }
        }
        AnimatedVisibility(
            enter = fadeIn(),
            exit = fadeOut(),
            visible = scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded && !state.showHistory
        ) {
            Box(
                modifier = Modifier
                    .clickable {}
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            )
        }
    }

    // Difficulty settings modal bottom sheet (independent of connection sheet)
    if (state.showDifficultySheet) {
        ModalBottomSheet(onDismissRequest = onDismissDifficultySettings) {
            DifficultySettingsSheet(
                selected = state.difficultySheetSelection,
                onSelect = onDifficultySheetSelectDifficulty,
                gain = state.difficultySheetGain,
                onGainChange = onDifficultySheetUpdateGain,
                capKg = state.difficultySheetCap,
                onCapChange = onDifficultySheetUpdateCap,
                onResetSelected = onDifficultySheetResetSelected
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
                    positionCableLeft = 0.75,
                    positionCableRight = 0.3
                )
            )
        )
    }
}
