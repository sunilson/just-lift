package at.sunilson.justlift.features.workout.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import at.sunilson.justlift.features.workout.data.VitruvianDeviceManager.EchoDifficulty
import at.sunilson.justlift.features.workout.presentation.history.HistoryOverlay
import at.sunilson.justlift.features.workout.presentation.history.TendenciesSheet
import at.sunilson.justlift.features.workout.presentation.history.WorkoutHistoryEntry
import at.sunilson.justlift.features.workout.presentation.history.WorkoutHistoryUiModel
import at.sunilson.justlift.features.workout.presentation.widgets.ConnectionWidget
import at.sunilson.justlift.features.workout.presentation.widgets.DifficultySettingsSheet
import at.sunilson.justlift.features.workout.presentation.widgets.ExerciseNameEditorSheet
import at.sunilson.justlift.features.workout.presentation.widgets.ExerciseSelectionSheet
import at.sunilson.justlift.features.workout.presentation.widgets.PauseTimerWidget
import at.sunilson.justlift.features.workout.presentation.widgets.WorkoutConfigurationWidget
import at.sunilson.justlift.features.workout.presentation.widgets.WorkoutDataWidget
import at.sunilson.justlift.shared.presentation.animations.JustLiftAnimations
import at.sunilson.justlift.shared.presentation.theme.JustLiftTheme
import com.juul.kable.ExperimentalApi
import com.juul.kable.Peripheral
import com.juul.kable.State
import kotlinx.coroutines.launch

@OptIn(ExperimentalApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    state: WorkoutScreenState,
    currentUserId: Int = 1,
    twoUserMode: Boolean = false,
    pagedHistory: List<WorkoutHistoryUiModel> = emptyList(),
    onDeviceSelected: (Peripheral) -> Unit = {},
    onEccentricSliderValueChange: (Float) -> Unit = {},
    onRepetitionsSliderValueChange: (Float) -> Unit = {},
    onEchoDifficultyChange: (EchoDifficulty) -> Unit = {},
    onUseTtsChange: (Boolean) -> Unit = {},
    onTwoUserModeChange: (Boolean) -> Unit = {},
    onFixedWeightModeChange: (Boolean) -> Unit = {},
    onFixedWeightKgChange: (Float) -> Unit = {},
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
    onDismissHistoryClicked: () -> Unit = {},
    onShowTendenciesClicked: () -> Unit = {},
    onDismissTendenciesClicked: () -> Unit = {},
    onShowTendenciesInfoClicked: () -> Unit = {},
    onDismissTendenciesInfoClicked: () -> Unit = {},
    onTrendTimeframeChanged: (at.sunilson.justlift.features.workout.presentation.history.TrendTimeframe) -> Unit = {},
    onEditExerciseName: (WorkoutHistoryEntry) -> Unit = {},
    onExerciseSelected: (String) -> Unit = {},
    onDismissExerciseSelection: () -> Unit = {},
    onUserSwitchClicked: () -> Unit = {},
    onOpenExerciseNameEditor: () -> Unit = {},
    onDismissExerciseNameEditor: () -> Unit = {},
    onRenameExercise: (String, String) -> Unit = { _, _ -> },
    onDeleteExercise: (String, String?) -> Unit = { _, _ -> }
) {
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(skipHiddenState = false)
    )
    val isWorkoutInProgress = state.workoutState != null && state.machineState != null
    val isConnected = state.connectedPeripheralState is State.Connected
    val starting = state.autoStartInSeconds != null
    val scope = rememberCoroutineScope()

    LaunchedEffect(isConnected) {
        if (isConnected) {
            scaffoldState.bottomSheetState.hide()
        } else {
            scaffoldState.bottomSheetState.expand()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetSwipeEnabled = true,
            containerColor = Color.Transparent,
            sheetContainerColor = MaterialTheme.colorScheme.surface,  // Full opacity
            topBar = {
                GlassTopAppBar(
                    currentUserId = currentUserId,
                    onUserSwitchClicked = onUserSwitchClicked,
                    onOpenDifficultySettings = onOpenDifficultySettings,
                    onHistoryClicked = onHistoryClicked,
                    onBluetoothClicked = { scope.launch { scaffoldState.bottomSheetState.expand() } }
                )
            },
            sheetContent = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
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
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                    // Auto-start countdown
                    AnimatedVisibility(
                        visible = starting,
                        enter = JustLiftAnimations.glassEnter()
                    ) {
                        Text(
                            text = "Auto start in ${state.autoStartInSeconds}s",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(16.dp)
                        )
                    }

                    // Active workout display
                    AnimatedVisibility(
                        visible = isWorkoutInProgress,
                        enter = JustLiftAnimations.glassEnter()
                    ) {
                        WorkoutDataWidget(
                            workoutState = state.workoutState,
                            machineState = state.machineState
                        )
                    }

                    // Connected but no workout - show configuration
                    AnimatedVisibility(
                        visible = !isWorkoutInProgress && isConnected && !starting,
                        enter = JustLiftAnimations.slideUpEnter()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            WorkoutConfigurationWidget(
                                state = state,
                                onEccentricSliderValueChange = onEccentricSliderValueChange,
                                onRepetitionsSliderValueChange = onRepetitionsSliderValueChange,
                                onEchoDifficultyChange = onEchoDifficultyChange,
                                onFixedWeightKgChange = onFixedWeightKgChange
                            )

                            // Instruction card - simple
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Ready to lift",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Lift and hold to start workout",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                state.pauseStartTimestamp?.let { pauseStart ->
                                    Spacer(modifier = Modifier.height(8.dp))
                                    PauseTimerWidget(pauseStartTimestamp = pauseStart)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Disconnect button when connected and not in workout
                    AnimatedVisibility(
                        visible = isConnected && !isWorkoutInProgress,
                        enter = JustLiftAnimations.slideUpEnter(delayMillis = 200)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Disconnect",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable(onClick = onDisconnectClicked)
                                    .padding(vertical = 12.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            // Previous set data
                            state.previousWorkoutState?.let { previous ->
                                PreviousSetSection(
                                    previousWorkoutState = previous,
                                    previousWorkoutEntry = state.previousWorkoutEntry,
                                    previousWorkoutExerciseName = state.previousWorkoutExerciseName,
                                    onEditExerciseName = onEditExerciseName
                                )
                            }
                        }
                    }

                    // Stop workout button - simple gradient
                    AnimatedVisibility(
                        visible = isConnected && isWorkoutInProgress,
                        enter = JustLiftAnimations.slideUpEnter()
                    ) {
                        Text(
                            text = "Stop Workout",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            JustLiftTheme.extendedColors.gradientStart,
                                            JustLiftTheme.extendedColors.gradientEnd
                                        )
                                    )
                                )
                                .clickable(onClick = onStopWorkoutClicked)
                                .padding(vertical = 14.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
            }

            // Scrim for bottom sheet
            AnimatedVisibility(
                enter = fadeIn(),
                exit = fadeOut(),
                visible = scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded
            ) {
                Spacer(
                    modifier = Modifier
                        .clickable {}
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                )
            }
        }
    }

    // Modal bottom sheets
    if (state.showDifficultySheet) {
        ModalBottomSheet(onDismissRequest = onDismissDifficultySettings) {
            DifficultySettingsSheet(
                selected = state.difficultySheetSelection,
                onSelect = onDifficultySheetSelectDifficulty,
                gain = state.difficultySheetGain,
                onGainChange = onDifficultySheetUpdateGain,
                capKg = state.difficultySheetCap,
                onCapChange = onDifficultySheetUpdateCap,
                useTts = state.useTts,
                onUseTtsChange = onUseTtsChange,
                twoUserMode = twoUserMode,
                onTwoUserModeChange = onTwoUserModeChange,
                fixedWeightMode = state.fixedWeightMode,
                onFixedWeightModeChange = onFixedWeightModeChange,
                fixedWeightKg = state.fixedWeightKg,
                onFixedWeightKgChange = onFixedWeightKgChange,
                onResetSelected = onDifficultySheetResetSelected,
                onEditExerciseNames = onOpenExerciseNameEditor
            )
        }
    }

    if (state.showExerciseNameEditor) {
        ModalBottomSheet(onDismissRequest = onDismissExerciseNameEditor) {
            ExerciseNameEditorSheet(
                exerciseNames = state.allExerciseNames,
                onRenameExercise = onRenameExercise,
                onDeleteExercise = onDeleteExercise
            )
        }
    }

    if (state.showHistory) {
        ModalBottomSheet(onDismissRequest = onDismissHistoryClicked) {
            HistoryOverlay(
                history = pagedHistory,
                onDismiss = onDismissHistoryClicked,
                onEditExerciseName = onEditExerciseName,
                onShowTendencies = onShowTendenciesClicked
            )
        }
    }

    if (state.showTendencies) {
        ModalBottomSheet(onDismissRequest = onDismissTendenciesClicked) {
            TendenciesSheet(
                tendencies = state.tendencies,
                selectedTimeframe = state.selectedTrendTimeframe,
                onTimeframeSelected = onTrendTimeframeChanged,
                onInfoClick = onShowTendenciesInfoClicked
            )
        }
    }

    if (state.showTendenciesInfo) {
        AlertDialog(
            onDismissRequest = onDismissTendenciesInfoClicked,
            confirmButton = {
                TextButton(onClick = onDismissTendenciesInfoClicked) {
                    Text("Got it")
                }
            },
            title = { Text("How Tendencies Work") },
            text = {
                Column {
                    Text(
                        "Tendencies show your progress over time for each exercise.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Trend:",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "The main value shows the difference between your estimated strength (accounting for weight and reps) in the selected timeframe and all your workouts before that timeframe. The smaller value below it shows your current average estimated strength in that timeframe.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Score (%):",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Visible in the history list. It compares your current workout's volume (reps × weight) to your historical average for that exercise. +10% means you performed 10% better than your usual average.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        )
    }

    state.showExerciseSelection?.let { entry ->
        ExerciseSelectionSheet(
            exerciseNameSuggestions = state.exerciseNameSuggestions,
            initialName = entry.exerciseName,
            onExerciseSelected = onExerciseSelected,
            onDismiss = onDismissExerciseSelection
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GlassTopAppBar(
    currentUserId: Int,
    onUserSwitchClicked: () -> Unit,
    onOpenDifficultySettings: () -> Unit,
    onHistoryClicked: () -> Unit,
    onBluetoothClicked: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                "Just Lift",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        ),
        actions = {
            // User ID badge
            Text(
                text = currentUserId.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .clickable(onClick = onUserSwitchClicked)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
                onClick = onOpenDifficultySettings,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }
            IconButton(
                onClick = onHistoryClicked,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = "History",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }
            IconButton(
                onClick = onBluetoothClicked,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Bluetooth,
                    contentDescription = "Connect",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    )
}

@OptIn(ExperimentalApi::class)
@Composable
private fun PreviousSetSection(
    previousWorkoutState: at.sunilson.justlift.features.workout.data.VitruvianDeviceManager.WorkoutState,
    previousWorkoutEntry: WorkoutHistoryEntry?,
    previousWorkoutExerciseName: String?,
    onEditExerciseName: (WorkoutHistoryEntry) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Previous Set",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.weight(1f))
            previousWorkoutEntry?.let { entry ->
                IconButton(onClick = { onEditExerciseName(entry) }, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit exercise name",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        WorkoutDataWidget(
            workoutState = previousWorkoutState,
            machineState = null,
            exerciseName = previousWorkoutExerciseName
        )
    }
}

