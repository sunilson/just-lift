package at.sunilson.justlift.features.workout.presentation.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import at.sunilson.justlift.shared.presentation.theme.JustLiftTheme

@Composable
fun ExerciseNameEditorSheet(
    exerciseNames: List<String>,
    onRenameExercise: (oldName: String, newName: String) -> Unit,
    onDeleteExercise: (name: String, alternativeName: String?) -> Unit
) {
    var editingName by remember { mutableStateOf<String?>(null) }
    var deletingName by remember { mutableStateOf<String?>(null) }
    var selectedAlternative by remember { mutableStateOf<String?>(null) }
    var newNameText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .heightIn(max = 600.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header - simplified
        Text(
            text = "Edit Exercises",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (exerciseNames.isEmpty()) {
            Text(
                text = "No exercises found",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(exerciseNames) { name ->
                    ExerciseListItem(
                        name = name,
                        onEdit = {
                            editingName = name
                            newNameText = name
                        },
                        onDelete = {
                            deletingName = name
                            selectedAlternative = null
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }

    // Delete confirmation dialog
    if (deletingName != null) {
        AlertDialog(
            onDismissRequest = { deletingName = null },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    "Delete Exercise",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        "Transfer workouts for \"$deletingName\" to another exercise?",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(8.dp)
                    ) {
                        items(exerciseNames.filter { it != deletingName }) { name ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedAlternative = name }
                                    .padding(vertical = 8.dp, horizontal = 8.dp)
                            ) {
                                RadioButton(
                                    selected = selectedAlternative == name,
                                    onClick = { selectedAlternative = name },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (selectedAlternative == name) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Text(
                    text = if (selectedAlternative != null) "Transfer & Delete" else "Delete",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(JustLiftTheme.extendedColors.error)
                        .clickable {
                            val name = deletingName ?: return@clickable
                            onDeleteExercise(name, selectedAlternative)
                            deletingName = null
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )
            },
            dismissButton = {
                TextButton(onClick = { deletingName = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit dialog
    if (editingName != null) {
        AlertDialog(
            onDismissRequest = { editingName = null },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    "Rename Exercise",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                OutlinedTextField(
                    value = newNameText,
                    onValueChange = { newNameText = it },
                    label = { Text("New Name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            },
            confirmButton = {
                Text(
                    text = "Rename",
                    color = if (newNameText.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (newNameText.isNotBlank()) {
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        JustLiftTheme.extendedColors.gradientStart,
                                        JustLiftTheme.extendedColors.gradientEnd
                                    )
                                )
                            } else {
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            }
                        )
                        .clickable(enabled = newNameText.isNotBlank()) {
                            val old = editingName
                            if (old != null && newNameText.isNotBlank() && old != newNameText) {
                                onRenameExercise(old, newNameText)
                            }
                            editingName = null
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )
            },
            dismissButton = {
                TextButton(onClick = { editingName = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ExerciseListItem(
    name: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.FitnessCenter,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) {
            Icon(
                Icons.Outlined.Edit,
                contentDescription = "Edit",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = "Delete",
                tint = JustLiftTheme.extendedColors.error.copy(alpha = 0.85f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
