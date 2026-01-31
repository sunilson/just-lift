package at.sunilson.justlift

import android.Manifest
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import at.sunilson.justlift.navigation.Permissions
import at.sunilson.justlift.navigation.Workout
import at.sunilson.justlift.features.permissions.presentation.PermissionsDestination
import at.sunilson.justlift.features.workout.presentation.WorkoutDestination
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

private const val TRANSITION_DURATION = 500

@OptIn(ExperimentalPermissionsApi::class)
@PreviewScreenSizes
@Composable
fun JustLiftApp() {
    val navController = rememberNavController()
    val bluetoothPermissionState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    )

    // Force dark status bar icons
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    val startDestination = if (bluetoothPermissionState.allPermissionsGranted) {
        Workout
    } else {
        Permissions
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(Modifier.padding(paddingValues)) {
            NavHost(
                navController = navController,
                startDestination = startDestination,
                enterTransition = {
                    fadeIn(
                        animationSpec = tween(
                            durationMillis = TRANSITION_DURATION,
                            easing = FastOutSlowInEasing
                        )
                    ) + scaleIn(
                        initialScale = 0.92f,
                        animationSpec = tween(
                            durationMillis = TRANSITION_DURATION,
                            easing = FastOutSlowInEasing
                        )
                    )
                },
                exitTransition = {
                    fadeOut(
                        animationSpec = tween(
                            durationMillis = TRANSITION_DURATION,
                            easing = FastOutSlowInEasing
                        )
                    ) + scaleOut(
                        targetScale = 1.08f,
                        animationSpec = tween(
                            durationMillis = TRANSITION_DURATION,
                            easing = FastOutSlowInEasing
                        )
                    )
                },
                popEnterTransition = {
                    fadeIn(
                        animationSpec = tween(
                            durationMillis = TRANSITION_DURATION,
                            easing = FastOutSlowInEasing
                        )
                    ) + scaleIn(
                        initialScale = 1.08f,
                        animationSpec = tween(
                            durationMillis = TRANSITION_DURATION,
                            easing = FastOutSlowInEasing
                        )
                    )
                },
                popExitTransition = {
                    fadeOut(
                        animationSpec = tween(
                            durationMillis = TRANSITION_DURATION,
                            easing = FastOutSlowInEasing
                        )
                    ) + scaleOut(
                        targetScale = 0.92f,
                        animationSpec = tween(
                            durationMillis = TRANSITION_DURATION,
                            easing = FastOutSlowInEasing
                        )
                    )
                }
            ) {
                composable<Permissions> {
                    PermissionsDestination(navController)
                }
                composable<Workout> {
                    WorkoutDestination(navController)
                }
            }
        }
    }
}
