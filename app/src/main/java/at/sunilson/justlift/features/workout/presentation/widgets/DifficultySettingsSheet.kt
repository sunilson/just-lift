package at.sunilson.justlift.features.workout.presentation.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
    onResetSelected: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Difficulty presets", style = MaterialTheme.typography.headlineSmall)
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

        Spacer(Modifier.height(16.dp))

        // Gain slider (0.5 .. 3.333) — quantize to 0.01 steps
        val gainMin = 0.5f
        val gainMax = 3.333f
        Text("Gain: ${((gain * 100f).toInt() / 100f)}")
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

        Spacer(Modifier.height(16.dp))

        // Cap slider (15 .. 50 kg) — integer steps
        val capMin = 15f
        val capMax = 50f
        Text("Cap: ${capKg.toInt()} kg")
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

        Spacer(Modifier.height(16.dp))
        Button(onClick = onResetSelected) {
            Text("Reset selected difficulty")
        }
    }
}
