package at.sunilson.justlift

import android.Manifest
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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

@OptIn(ExperimentalPermissionsApi::class)
@PreviewScreenSizes
@Composable
fun JustLiftApp() {
    val navController = rememberNavController()
    val bluetoothPermissionState = rememberMultiplePermissionsState(listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT))

    val startDestination = if (bluetoothPermissionState.allPermissionsGranted) {
        Workout
    } else {
        Permissions
    }

    Scaffold {
        Box(Modifier.padding(it)) {
            NavHost(
                navController = navController,
                startDestination = startDestination
            ) {
                composable<Permissions> { PermissionsDestination(navController) }
                composable<Workout> { WorkoutDestination((navController)) }
            }
        }
    }
}
