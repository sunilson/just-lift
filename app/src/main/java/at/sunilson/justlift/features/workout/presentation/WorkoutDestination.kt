package at.sunilson.justlift.features.workout.presentation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.paging.compose.collectAsLazyPagingItems
import org.koin.androidx.compose.koinViewModel
import at.sunilson.justlift.features.workout.presentation.history.WorkoutHistoryUiModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDestination(
    navController: NavController,
    viewModel: WorkoutViewModel = koinViewModel()
) {
    // Flows that can be paused when app is in background
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pagedHistory = viewModel.pagedHistory.collectAsLazyPagingItems()


    WorkoutScreen(
        state = state,
        pagedHistory = pagedHistory,
        onDeviceSelected = viewModel::onDeviceSelected,
        onUseNoRepLimitChange = viewModel::onUseNoRepLimitChange,
        onEccentricSliderValueChange = viewModel::onEccentricSliderValueChange,
        onRepetitionsSliderValueChange = viewModel::onRepetitionsSliderValueChange,
        onEchoDifficultyChange = viewModel::onEchoDifficultyChange,
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
        onDismissHistoryClicked = viewModel::onDismissHistoryClicked
    )
}
