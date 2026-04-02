package at.sunilson.justlift

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import at.sunilson.justlift.di.iosModule
import at.sunilson.justlift.features.workout.presentation.WorkoutDestination
import at.sunilson.justlift.shared.presentation.theme.JustLiftTheme
import org.koin.core.context.startKoin

fun initKoin() {
    startKoin {
        modules(iosModule)
    }
}

fun MainViewController() = ComposeUIViewController { IosApp() }

@Composable
private fun IosApp() {
    JustLiftTheme {
        Scaffold(
            contentWindowInsets = WindowInsets(0),
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                WorkoutDestination()
            }
        }
    }
}
