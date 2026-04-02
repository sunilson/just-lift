package at.sunilson.justlift.features.workout.presentation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.paging.compose.collectAsLazyPagingItems
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDestination(
    navController: NavController,
    viewModel: AndroidWorkoutViewModel = koinViewModel()
) {
    // Flows that can be paused when app is in background
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()
    val twoUserMode by viewModel.twoUserMode.collectAsStateWithLifecycle()
    val pagedHistory = viewModel.pagedHistory.collectAsLazyPagingItems()
    val historySnapshot = (0 until pagedHistory.itemCount).mapNotNull { pagedHistory[it] }

    WorkoutScreen(
        state = state,
        currentUserId = currentUserId,
        twoUserMode = twoUserMode,
        pagedHistory = historySnapshot,
        onDeviceSelected = viewModel::onDeviceSelected,
        onEccentricSliderValueChange = viewModel::onEccentricSliderValueChange,
        onRepetitionsSliderValueChange = viewModel::onRepetitionsSliderValueChange,
        onEchoDifficultyChange = viewModel::onEchoDifficultyChange,
        onUseTtsChange = viewModel::onUseTtsChange,
        onTwoUserModeChange = viewModel::onTwoUserModeChange,
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
