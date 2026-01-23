package at.sunilson.justlift

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import at.sunilson.justlift.features.workout.data.VitruvianDeviceManager.EchoDifficulty
import at.sunilson.justlift.features.workout.presentation.WorkoutViewModel
import at.sunilson.justlift.features.workout.presentation.widgets.WorkoutConfigurationWidget
import at.sunilson.justlift.features.workout.presentation.history.TendenciesSheet
import at.sunilson.justlift.features.workout.presentation.history.ExerciseTrend
import at.sunilson.justlift.features.workout.presentation.history.TrendTimeframe
import at.sunilson.justlift.shared.presentation.theme.JustLiftTheme
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@PreviewLightDark
@Composable
private fun `Workout configuration widget`() {
    JustLiftTheme {
        Box(modifier = Modifier
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

@PreviewTest
@PreviewLightDark
@Composable
private fun `Tendencies sheet`() {
    JustLiftTheme {
        Box(modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
        ) {
            TendenciesSheet(
                tendencies = listOf(
                    ExerciseTrend("Bicep Curl (HARD)", 2.5, 1.2, 45.0, 42.0),
                    ExerciseTrend("Squat (NORMAL)", -1.5, -0.8, 80.0, 75.0)
                ),
                selectedTimeframe = TrendTimeframe.ONE_WEEK,
                onTimeframeSelected = {},
                onInfoClick = {}
            )
        }
    }
}
