package at.sunilson.justlift.features.workout.presentation.widgets

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Composable
fun PauseTimerWidget(
    pauseStartTimestamp: Long,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
    tickMillis: Long = 1000L
) {
    var now by remember(pauseStartTimestamp) { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(pauseStartTimestamp) {
        // Update once per tick while this composable is in composition
        while (isActive) {
            now = System.currentTimeMillis()
            delay(tickMillis)
        }
    }
    val elapsed = (now - pauseStartTimestamp).coerceAtLeast(0L)
    val formatted = elapsed
        .toDuration(DurationUnit.MILLISECONDS)
        .formatHhMmSs()
    Text(text = "Pause: $formatted", style = textStyle, color = color, modifier = modifier)
}

private fun Duration.formatHhMmSs(): String {
    val totalSeconds = inWholeSeconds
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}
