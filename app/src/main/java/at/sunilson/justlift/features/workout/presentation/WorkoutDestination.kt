package at.sunilson.justlift.features.workout.presentation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDestination(
    navController: NavController,
    viewModel: WorkoutViewModel = koinViewModel()
) {
    // Flows that can be paused when app is in background
    val state by viewModel.state.collectAsStateWithLifecycle()


    WorkoutScreen(
        state = state,
        onDeviceSelected = viewModel::onDeviceSelected,
        onUseNoRepLimitChange = viewModel::onUseNoRepLimitChange,
        onEccentricSliderValueChange = viewModel::onEccentricSliderValueChange,
        onRepetitionsSliderValueChange = viewModel::onRepetitionsSliderValueChange,
        onEchoDifficultyChange = viewModel::onEchoDifficultyChange,
        onStartWorkoutClicked = viewModel::onStartWorkoutClicked,
        onStopWorkoutClicked = viewModel::onStopWorkoutClicked,
        onDisconnectClicked = viewModel::onDisconnectClicked
    )
}
