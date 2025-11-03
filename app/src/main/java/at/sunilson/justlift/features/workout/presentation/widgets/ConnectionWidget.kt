package at.sunilson.justlift.features.workout.presentation.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.juul.kable.ExperimentalApi
import com.juul.kable.Peripheral

@OptIn(ExperimentalApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ConnectionWidget(
    availableDevices: List<Peripheral>,
    onDeviceSelected: (Peripheral) -> Unit
) {
    Column(
        modifier = Modifier
            .height(400.dp)
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Available Devices:", style = MaterialTheme.typography.headlineMedium)
        availableDevices.forEach { peripheral ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onDeviceSelected(peripheral) },
            ) {
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = peripheral.name ?: "Unknown Device", style = MaterialTheme.typography.bodyLarge,
                )
            }

        }
    }
}
