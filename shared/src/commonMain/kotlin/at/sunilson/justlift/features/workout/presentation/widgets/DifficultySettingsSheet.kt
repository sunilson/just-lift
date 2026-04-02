package at.sunilson.justlift.features.workout.presentation.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.outlined.FitnessCenter
import at.sunilson.justlift.features.workout.data.VitruvianDeviceManager
import at.sunilson.justlift.platform.formatTwoDecimals

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
    twoUserMode: Boolean,
    onTwoUserModeChange: (Boolean) -> Unit,
    fixedWeightMode: Boolean,
    onFixedWeightModeChange: (Boolean) -> Unit,
    fixedWeightKg: Float,
    onFixedWeightKgChange: (Float) -> Unit,
    onResetSelected: () -> Unit,
    onEditExerciseNames: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Settings",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Training Mode Section
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader(title = "Training Mode", icon = Icons.Outlined.FitnessCenter)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WorkoutModeOption(
                    label = "Echo",
                    isSelected = !fixedWeightMode,
                    onClick = { onFixedWeightModeChange(false) },
                    modifier = Modifier.weight(1f)
                )
                WorkoutModeOption(
                    label = "Fixed Weight",
                    isSelected = fixedWeightMode,
                    onClick = { onFixedWeightModeChange(true) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (fixedWeightMode) {
            // Fixed Weight Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader(title = "Weight per Cable", icon = Icons.Outlined.Speed)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SliderParameter(
                        label = "Weight",
                        value = fixedWeightKg,
                        valueDisplay = "${fixedWeightKg.toInt()} kg",
                        min = 5f,
                        max = 100f,
                        steps = 94,
                        onValueChange = { raw ->
                            val clamped = raw.coerceIn(5f, 100f)
                            val quantized = kotlin.math.round(clamped)
                            onFixedWeightKgChange(quantized)
                        }
                    )
                }
            }
        } else {
            // Difficulty Presets Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader(title = "Difficulty", icon = Icons.Outlined.Speed)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VitruvianDeviceManager.EchoDifficulty.entries.forEach { difficulty ->
                        DifficultyChip(
                            difficulty = difficulty,
                            isSelected = difficulty == selected,
                            onClick = { onSelect(difficulty) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Machine Parameters Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader(title = "Parameters", icon = null)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SliderParameter(
                        label = "Gain",
                        value = gain,
                        valueDisplay = gain.formatTwoDecimals(),
                        min = 0.5f,
                        max = 3.333f,
                        onValueChange = { raw ->
                            val clamped = raw.coerceIn(0.5f, 3.333f)
                            val quantized = kotlin.math.round(clamped * 100f) / 100f
                            onGainChange(quantized)
                        }
                    )

                    SliderParameter(
                        label = "Weight Cap",
                        value = capKg,
                        valueDisplay = "${capKg.toInt()} kg",
                        min = 15f,
                        max = 50f,
                        steps = 34,
                        onValueChange = { raw ->
                            val clamped = raw.coerceIn(15f, 50f)
                            val quantized = kotlin.math.round(clamped)
                            onCapChange(quantized)
                        }
                    )
                }
            }
        }

        // Rep Announcement Section
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader(title = "Announcements", icon = Icons.Outlined.VolumeUp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnnouncementOption(
                    icon = Icons.Outlined.VolumeUp,
                    label = "Beeps",
                    isSelected = !useTts,
                    onClick = { onUseTtsChange(false) },
                    modifier = Modifier.weight(1f)
                )
                AnnouncementOption(
                    icon = Icons.Outlined.RecordVoiceOver,
                    label = "Voice",
                    isSelected = useTts,
                    onClick = { onUseTtsChange(true) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Workout Mode Section
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader(title = "Workout Mode", icon = Icons.Outlined.Group)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WorkoutModeOption(
                    label = "1 User",
                    isSelected = !twoUserMode,
                    onClick = { onTwoUserModeChange(false) },
                    modifier = Modifier.weight(1f)
                )
                WorkoutModeOption(
                    label = "2 Users",
                    isSelected = twoUserMode,
                    onClick = { onTwoUserModeChange(true) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Actions Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionButton(
                icon = Icons.Outlined.Refresh,
                label = "Reset",
                onClick = onResetSelected,
                modifier = Modifier.weight(1f)
            )

            ActionButton(
                icon = Icons.Outlined.Edit,
                label = "Exercises",
                onClick = onEditExerciseNames,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector?) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DifficultyChip(
    difficulty: VitruvianDeviceManager.EchoDifficulty,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Use numbers for guaranteed fit
    val displayName = when (difficulty) {
        VitruvianDeviceManager.EchoDifficulty.WARMUP -> "Warm"
        VitruvianDeviceManager.EchoDifficulty.HARD -> "Hard"
        VitruvianDeviceManager.EchoDifficulty.HARDER -> "Harder"
        VitruvianDeviceManager.EchoDifficulty.HARDEST -> "Max"
    }
    Text(
        text = displayName,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurface,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    )
}

@Composable
private fun SliderParameter(
    label: String,
    value: Float,
    valueDisplay: String,
    min: Float,
    max: Float,
    steps: Int = 0,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = valueDisplay,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Slider(
            value = value.coerceIn(min, max),
            onValueChange = onValueChange,
            valueRange = min..max,
            steps = steps,
            modifier = Modifier.padding(top = 8.dp),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
private fun AnnouncementOption(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

@Composable
private fun WorkoutModeOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurface,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    )
}
