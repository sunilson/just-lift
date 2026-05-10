package at.sunilson.justlift.features.workout.presentation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import org.koin.mp.KoinPlatform.getKoin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDestination() {
    val koin = getKoin()
    val viewModel: WorkoutViewModel = viewModel { koin.get<WorkoutViewModel>() }

    val state by viewModel.state.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val twoUserMode by viewModel.twoUserMode.collectAsState()

    WorkoutScreen(
        state = state,
        currentUserId = currentUserId,
        twoUserMode = twoUserMode,
        pagedHistory = emptyList(),
        onDeviceSelected = viewModel::onDeviceSelected,
        onEccentricSliderValueChange = viewModel::onEccentricSliderValueChange,
        onRepetitionsSliderValueChange = viewModel::onRepetitionsSliderValueChange,
        onEchoDifficultyChange = viewModel::onEchoDifficultyChange,
        onUseTtsChange = viewModel::onUseTtsChange,
        onTwoUserModeChange = viewModel::onTwoUserModeChange,
        onSetsPerUserChange = viewModel::onSetsPerUserChange,
        onFixedWeightModeChange = viewModel::onFixedWeightModeChange,
        onFixedWeightKgChange = viewModel::onFixedWeightKgChange,
        onOpenDifficultySettings = viewModel::onOpenDifficultySettings,
        onDismissDifficultySettings = viewModel::onDismissDifficultySettings,
        onDifficultySheetSelectDifficulty = viewModel::onDifficultySheetSelectDifficulty,
        onDifficultySheetUpdateGain = viewModel::onDifficultySheetUpdateGain,
        onDifficultySheetUpdateCap = viewModel::onDifficultySheetUpdateCap,
        onDifficultySheetResetSelected = viewModel::onDifficultySheetResetSelected,
        onStartWorkoutClicked = viewModel::onStartWorkoutClicked,
        onStopWorkoutClicked = viewModel::onStopWorkoutClicked,
        onDisconnectClicked = viewModel::onDisconnectClicked,
        onClearSavedDeviceClicked = viewModel::onClearSavedDeviceClicked,
        onHistoryClicked = viewModel::onHistoryClicked,
        onDismissHistoryClicked = viewModel::onDismissHistoryClicked,
        onShowTendenciesClicked = viewModel::onShowTendenciesClicked,
        onDismissTendenciesClicked = viewModel::onDismissTendenciesClicked,
        onShowTendenciesInfoClicked = viewModel::onShowTendenciesInfoClicked,
        onDismissTendenciesInfoClicked = viewModel::onDismissTendenciesInfoClicked,
        onTrendTimeframeChanged = viewModel::onTrendTimeframeChanged,
        onEditExerciseName = viewModel::onEditExerciseName,
        onExerciseSelected = viewModel::onExerciseSelected,
        onDismissExerciseSelection = viewModel::onDismissExerciseSelection,
        onUserSwitchClicked = viewModel::onUserSwitchClicked,
        onOpenExerciseNameEditor = viewModel::onOpenExerciseNameEditor,
        onDismissExerciseNameEditor = viewModel::onDismissExerciseNameEditor,
        onRenameExercise = viewModel::onRenameExercise,
        onDeleteExercise = viewModel::onDeleteExercise
    )
}
