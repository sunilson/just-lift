package at.sunilson.justlift.features.workout.presentation.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.juul.kable.ExperimentalApi
import com.juul.kable.Peripheral

@OptIn(ExperimentalApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ConnectionWidget(
    availableDevices: List<Peripheral>,
    connectedPeripheral: Peripheral?,
    isAutoConnecting: Boolean,
    savedDeviceName: String?,
    onDeviceSelected: (Peripheral) -> Unit,
    onClearSavedDevice: () -> Unit,
    onDisconnectClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(16.dp)
    ) {
        // Saved device / auto-connect status (only when not connected)
        if (savedDeviceName != null && connectedPeripheral == null) {
            Column(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = "Saved device: $savedDeviceName",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    TextButton(onClick = onClearSavedDevice) { Text("Clear") }
                }
                if (isAutoConnecting) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.padding(horizontal = 4.dp))
                        Text(text = "Auto-connecting…", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Current connection
        if (connectedPeripheral != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Connected to: ${connectedPeripheral.name ?: "Unknown"}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                TextButton(onClick = onDisconnectClicked) { Text("Disconnect") }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Text(text = "Available Devices:", style = MaterialTheme.typography.headlineMedium)
        availableDevices.forEach { peripheral ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onDeviceSelected(peripheral) },
                enabled = !isAutoConnecting
            ) {
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = peripheral.name ?: "Unknown Device", style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}
