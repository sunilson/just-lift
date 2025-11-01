package at.sunilson.justlift.ui

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import at.sunilson.justlift.ui.navigation.Permissions
import at.sunilson.justlift.ui.navigation.Workout
import at.sunilson.justlift.ui.permissions.PermissionsDestination
import at.sunilson.justlift.ui.theme.JustLiftTheme
import at.sunilson.justlift.ui.workout.WorkoutDestination
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JustLiftTheme {
                JustLiftApp()
            }
        }
    }
}

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
