package at.sunilson.justlift.features.workout.presentation.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import at.sunilson.justlift.features.workout.data.VitruvianDeviceManager.EchoDifficulty
import at.sunilson.justlift.features.workout.presentation.WorkoutViewModel
import at.sunilson.justlift.shared.presentation.theme.JustLiftTheme

@Composable
fun WorkoutConfigurationWidget(
    state: WorkoutViewModel.State,
    onUseNoRepLimitChange: (Boolean) -> Unit = {},
    onEccentricSliderValueChange: (Float) -> Unit = {},
    onRepetitionsSliderValueChange: (Float) -> Unit = {},
    onEchoDifficultyChange: (EchoDifficulty) -> Unit = {},
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Eccentric Percentage: ${(state.eccentricSliderValue.toDouble()).toInt()}%")
        Slider(
            value = state.eccentricSliderValue,
            onValueChange = onEccentricSliderValueChange,
            valueRange = 0f..130f,
            steps = 12,
            enabled = state.workoutState == null
        )
        Spacer(modifier = Modifier.height(16.dp))
        SingleChoiceSegmentedButtonRow {
            EchoDifficulty.entries.forEachIndexed { index, difficulty ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = EchoDifficulty.entries.size),
                    onClick = { onEchoDifficultyChange(difficulty) },
                    selected = difficulty == state.echoDifficulty,
                    label = { Text(difficulty.toString()) }
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("No rep limit")
            Checkbox(checked = state.useNoRepLimit, onCheckedChange = { onUseNoRepLimitChange(it) }, enabled = state.workoutState == null)
        }

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
    }
}

@PreviewLightDark
@Composable
private fun WorkoutConfigurationWidgetPreview() {
    JustLiftTheme {
        Box(modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
        ) {
            WorkoutConfigurationWidget(
                state = WorkoutViewModel.State(
                    eccentricSliderValue = 70f,
                    useNoRepLimit = false,
                    repetitionsSliderValue = 10,
                    echoDifficulty = EchoDifficulty.HARDER
                )
            )
        }
    }
}
