package at.sunilson.justlift.features.workout.presentation.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun TendenciesSheet(
    tendencies: List<ExerciseTrend>,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Exercise Tendencies",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onInfoClick) {
                Icon(Icons.Outlined.Info, contentDescription = "Show Info")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (tendencies.isEmpty()) {
            Text("No tendencies yet. Complete more workouts to see trends!")
        } else {
            LazyColumn {
                item {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Text(
                            "Exercise",
                            modifier = Modifier.weight(1.5f),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            "Upward (Rec.)",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            "Downward (Rec.)",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    HorizontalDivider()
                }
                items(tendencies) { trend ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                        Text(
                            trend.exerciseName,
                            modifier = Modifier.weight(1.5f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            TrendValue(value = trend.avgUpwardTrend)
                            TrendValue(
                                value = trend.recentUpwardTrend,
                                isSmall = true
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            TrendValue(value = trend.avgDownwardTrend)
                            TrendValue(
                                value = trend.recentDownwardTrend,
                                isSmall = true
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
                item {
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }
}

@Composable
private fun TrendValue(value: Double, isSmall: Boolean = false, modifier: Modifier = Modifier) {
    val color = when {
        value > 0 -> MaterialTheme.colorScheme.primary
        value < 0 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val prefix = if (value > 0) "+" else ""
    val style = if (isSmall) {
        MaterialTheme.typography.labelSmall
    } else {
        MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
    }
    Text(
        text = "$prefix${"%.2f".format(value)}kg",
        color = color,
        style = style,
        modifier = modifier
    )
}
