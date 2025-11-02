package at.sunilson.justlift.ui.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.juul.kable.State
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDestination(
    navController: NavController,
    viewModel: WorkoutViewModel = koinViewModel()
) {
    // Flows that can be paused when app is in background
    val state by viewModel.state.collectAsStateWithLifecycle()
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
                onDeviceSelected = viewModel::onDeviceSelected
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
        ) {
            WorkoutScreen(
                state = state,
                onEccentricSliderValueChange = viewModel::onEccentricSliderValueChange,
                onRepetitionsSliderValueChange = viewModel::onRepetitionsSliderValueChange,
                onStartWorkoutClicked = viewModel::onStartWorkoutClicked,
                onStopWorkoutClicked = viewModel::onStopWorkoutClicked,
                onDisconnectClicked = viewModel::onDisconnectClicked
            )
        }
        AnimatedVisibility(
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
