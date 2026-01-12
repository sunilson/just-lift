package at.sunilson.justlift.features.workout.presentation.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import at.sunilson.justlift.features.workout.data.VitruvianDeviceManager

@Composable
fun DifficultySettingsSheet(
    selected: VitruvianDeviceManager.EchoDifficulty,
    onSelect: (VitruvianDeviceManager.EchoDifficulty) -> Unit,
    gain: Float,
    onGainChange: (Float) -> Unit,
    capKg: Float,
    onCapChange: (Float) -> Unit,
    useTts: Boolean,
    onUseTtsChange: (Boolean) -> Unit,
    onResetSelected: () -> Unit,
    onEditExerciseNames: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Difficulty presets", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))

        SingleChoiceSegmentedButtonRow {
            VitruvianDeviceManager.EchoDifficulty.entries.forEachIndexed { index, difficulty ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index, VitruvianDeviceManager.EchoDifficulty.entries.size),
                    onClick = { onSelect(difficulty) },
                    selected = difficulty == selected,
                    label = {
                        Text(
                            difficulty.toString(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(24.dp))

        Text("Machine parameters", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))

        // Gain slider (0.5 .. 3.333) — quantize to 0.01 steps
        val gainMin = 0.5f
        val gainMax = 3.333f
        Text("Gain: ${((gain * 100f).toInt() / 100f)}", style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = gain.coerceIn(gainMin, gainMax),
            onValueChange = { raw ->
                val clamped = raw.coerceIn(gainMin, gainMax)
                // Quantize to 0.01
                val quantized = kotlin.math.round(clamped * 100f) / 100f
                onGainChange(quantized)
            },
            valueRange = gainMin..gainMax
        )

        Spacer(Modifier.height(8.dp))

        // Cap slider (15 .. 50 kg) — integer steps
        val capMin = 15f
        val capMax = 50f
        Text("Cap: ${capKg.toInt()} kg", style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = capKg.coerceIn(capMin, capMax),
            onValueChange = { raw ->
                val clamped = raw.coerceIn(capMin, capMax)
                // Quantize to 1 kg
                val quantized = kotlin.math.round(clamped)
                onCapChange(quantized)
            },
            valueRange = capMin..capMax,
            steps = (capMax - capMin).toInt() - 1
        )

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(24.dp))

        Text("Exercise settings", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))

        Button(onClick = onResetSelected) {
            Text("Reset selected difficulty")
        }

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onEditExerciseNames) {
            Text("Edit exercise names")
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(24.dp))

        Text("Rep announcement", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        SingleChoiceSegmentedButtonRow {
             SegmentedButton(
                selected = !useTts,
                onClick = { onUseTtsChange(false) },
                shape = SegmentedButtonDefaults.itemShape(0, 2),
                label = { Text("Beeps") }
            )
            SegmentedButton(
                selected = useTts,
                onClick = { onUseTtsChange(true) },
                shape = SegmentedButtonDefaults.itemShape(1, 2),
                label = { Text("TTS") }
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}
