package at.sunilson.justlift.features.workout.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.TrendingFlat
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import at.sunilson.justlift.shared.presentation.theme.JustLiftTheme

@Composable
fun TendenciesSheet(
    tendencies: List<ExerciseTrend>,
    selectedTimeframe: TrendTimeframe,
    onTimeframeSelected: (TrendTimeframe) -> Unit,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Trends",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onInfoClick, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = "Info",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Timeframe filter
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TrendTimeframe.entries.forEach { timeframe ->
                TimeframeChip(
                    timeframe = timeframe,
                    isSelected = selectedTimeframe == timeframe,
                    onClick = { onTimeframeSelected(timeframe) }
                )
            }
        }

        if (tendencies.isEmpty()) {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Outlined.TrendingUp,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "No trends yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        } else {
            // Table header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Exercise",
                    modifier = Modifier.weight(1.3f),
                    style = MaterialTheme.typography.labelMedium,  // Fits
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "↑",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,  // Fits
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "↓",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,  // Fits
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Tendencies list
            LazyColumn(
                modifier = Modifier.height(400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(tendencies) { index, trend ->
                    TrendRow(trend = trend, isEven = index % 2 == 0)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun TimeframeChip(
    timeframe: TrendTimeframe,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = timeframe.label,
        style = MaterialTheme.typography.labelMedium,
        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

@Composable
private fun TrendRow(
    trend: ExerciseTrend,
    isEven: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isEven) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Exercise name
        Text(
            text = trend.exerciseName,
            style = MaterialTheme.typography.bodySmall,  // Fits with difficulty label
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1.3f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        // Upward trend
        TrendValueDisplay(
            avg = trend.avgUpwardTrend,
            recent = trend.recentUpwardTrend,
            modifier = Modifier.weight(1f)
        )

        // Downward trend
        TrendValueDisplay(
            avg = trend.avgDownwardTrend,
            recent = trend.recentDownwardTrend,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TrendValueDisplay(
    avg: Double,
    recent: Double,
    modifier: Modifier = Modifier
) {
    val color = when {
        avg > 0.5 -> JustLiftTheme.extendedColors.success
        avg < -0.5 -> JustLiftTheme.extendedColors.error
        else -> MaterialTheme.colorScheme.onSurface
    }

    val icon = when {
        avg > 0.5 -> Icons.Outlined.TrendingUp
        avg < -0.5 -> Icons.Outlined.TrendingDown
        else -> Icons.Outlined.TrendingFlat
    }

    val prefix = if (avg > 0) "+" else ""

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = color
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = "$prefix${"%.0f".format(avg)}",
            color = color,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
    }
}
