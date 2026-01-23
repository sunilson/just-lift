package at.sunilson.justlift.features.workout.presentation.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
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
            .padding(16.dp)
            .heightIn(max = 500.dp)
    ) {
        Text("Edit Exercise Names", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        if (exerciseNames.isEmpty()) {
            Text("No exercises found", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                items(exerciseNames) { name ->
                    ListItem(
                        headlineContent = { Text(name) },
                        trailingContent = {
                            Row {
                                IconButton(onClick = {
                                    editingName = name
                                    newNameText = name
                                }) {
                                    Icon(Icons.Outlined.Edit, contentDescription = "Edit")
                                }
                                IconButton(onClick = {
                                    deletingName = name
                                    selectedAlternative = null
                                }) {
                                    Icon(Icons.Outlined.Delete, contentDescription = "Delete")
                                }
                            }
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }

    if (deletingName != null) {
        AlertDialog(
            onDismissRequest = { deletingName = null },
            title = { Text("Delete Exercise") },
            text = {
                Column {
                    Text("Do you want to transfer associated workouts for \"$deletingName\" to another exercise?")
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        items(exerciseNames.filter { it != deletingName }) { name ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = selectedAlternative == name,
                                    onClick = { selectedAlternative = name }
                                )
                                Text(
                                    text = name,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = deletingName ?: return@Button
                        onDeleteExercise(name, selectedAlternative)
                        deletingName = null
                    }
                ) {
                    Text(if (selectedAlternative != null) "Transfer & Delete" else "Just Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingName = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (editingName != null) {
        AlertDialog(
            onDismissRequest = { editingName = null },
            title = { Text("Rename Exercise") },
            text = {
                OutlinedTextField(
                    value = newNameText,
                    onValueChange = { newNameText = it },
                    label = { Text("New Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val old = editingName
                        if (old != null && newNameText.isNotBlank() && old != newNameText) {
                            onRenameExercise(old, newNameText)
                        }
                        editingName = null
                    },
                    enabled = newNameText.isNotBlank()
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingName = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
