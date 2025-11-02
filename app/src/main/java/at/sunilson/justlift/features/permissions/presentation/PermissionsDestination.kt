package at.sunilson.justlift.features.permissions.presentation

import android.Manifest
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import at.sunilson.justlift.navigation.Workout
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionsDestination(navController: NavController) {
    val bluetoothPermissionState = rememberMultiplePermissionsState(listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT))
    val context = LocalContext.current

    if (bluetoothPermissionState.allPermissionsGranted) {
        navController.navigate(Workout) {
            popUpTo(navController.graph.id) {
                inclusive = true
            }
        }
        return
    }

    PermissionsScreen(
        bluetoothPermissionGiven = bluetoothPermissionState.allPermissionsGranted,
        onRequestPermission = {
            if (!bluetoothPermissionState.allPermissionsGranted) {
                if (bluetoothPermissionState.shouldShowRationale) {
                    Toast.makeText(context, "You need to accept the permission to use Bluetooth devices", Toast.LENGTH_LONG).show() // TODO
                    bluetoothPermissionState.launchMultiplePermissionRequest()
                } else {
                    bluetoothPermissionState.launchMultiplePermissionRequest()
                }
            }
        },
    )
}
