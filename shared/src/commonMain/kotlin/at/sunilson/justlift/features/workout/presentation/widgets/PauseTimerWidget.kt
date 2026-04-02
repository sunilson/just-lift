package at.sunilson.justlift.features.workout.presentation.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import at.sunilson.justlift.platform.currentTimeMillis
import at.sunilson.justlift.shared.presentation.theme.JustLiftTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Composable
fun PauseTimerWidget(
    pauseStartTimestamp: Long,
    modifier: Modifier = Modifier
) {
    var now by remember(pauseStartTimestamp) { mutableStateOf(currentTimeMillis()) }

    LaunchedEffect(pauseStartTimestamp) {
        while (isActive) {
            now = currentTimeMillis()
            delay(1000L)
        }
    }

    val elapsed = (now - pauseStartTimestamp).coerceAtLeast(0L)
    val timeString = elapsed.toDuration(DurationUnit.MILLISECONDS).formatHhMmSs()

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(JustLiftTheme.extendedColors.warning.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Timer,
            contentDescription = "Rest timer",
            tint = JustLiftTheme.extendedColors.warning,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Rest: $timeString",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = JustLiftTheme.extendedColors.warning
        )
    }
}

private fun Duration.formatHhMmSs(): String {
    val totalSeconds = inWholeSeconds
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "$hours:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    } else {
        "$minutes:${seconds.toString().padStart(2, '0')}"
    }
}
