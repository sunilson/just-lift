package at.sunilson.justlift.features.permissions.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun PermissionsScreen(
    bluetoothPermissionGiven: Boolean,
    onRequestPermission: () -> Unit,
) {
    Column {
        if (!bluetoothPermissionGiven) {
            Button(onClick = {
                onRequestPermission()
            }) { Text("Request scan permission") }

        }
    }
}
