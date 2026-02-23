package at.sunilson.justlift.features.workout.presentation.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import at.sunilson.justlift.features.workout.data.VitruvianDeviceManager.EchoDifficulty
import at.sunilson.justlift.features.workout.presentation.WorkoutViewModel
import at.sunilson.justlift.shared.presentation.theme.JustLiftTheme

@Composable
fun WorkoutConfigurationWidget(
    state: WorkoutViewModel.State,
    onEccentricSliderValueChange: (Float) -> Unit = {},
    onRepetitionsSliderValueChange: (Float) -> Unit = {},
    onEchoDifficultyChange: (EchoDifficulty) -> Unit = {},
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Difficulty selection
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Difficulty",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EchoDifficulty.entries.forEach { difficulty ->
                    val isSelected = difficulty == state.echoDifficulty
                    // Single character labels
                    val displayName = when (difficulty) {
                        EchoDifficulty.WARMUP -> "Warm"
                        EchoDifficulty.HARD -> "Hard"
                        EchoDifficulty.HARDER -> "Harder"
                        EchoDifficulty.HARDEST -> "Max"
                    }
                    DifficultyChip(
                        text = displayName,
                        isSelected = isSelected,
                        onClick = { onEchoDifficultyChange(difficulty) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Sliders side by side
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Reps slider
            SliderCard(
                modifier = Modifier.weight(1f),
                label = "Reps",
                value = state.repetitionsSliderValue.toString(),
                sliderValue = state.repetitionsSliderValue.toFloat(),
                onValueChange = onRepetitionsSliderValueChange,
                valueRange = 1f..20f,
                steps = 18,
                enabled = state.workoutState == null,
                accentColor = MaterialTheme.colorScheme.primary
            )

            // Eccentric slider
            SliderCard(
                modifier = Modifier.weight(1f),
                label = "Ecc",
                value = "${state.eccentricSliderValue.toInt()}%",
                sliderValue = state.eccentricSliderValue,
                onValueChange = onEccentricSliderValueChange,
                valueRange = 0f..130f,
                steps = 12,
                enabled = state.workoutState == null,
                accentColor = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun SliderCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    sliderValue: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    enabled: Boolean,
    accentColor: androidx.compose.ui.graphics.Color
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = accentColor,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(12.dp))
        Slider(
            value = sliderValue,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}

@Composable
private fun DifficultyChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp)
    )
}

@PreviewLightDark
@Composable
private fun WorkoutConfigurationWidgetPreview() {
    JustLiftTheme {
        Box(
            modifier = Modifier
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
