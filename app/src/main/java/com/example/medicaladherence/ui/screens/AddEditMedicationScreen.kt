package com.example.medicaladherence.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.medicaladherence.data.model.MedicationFrequency
import com.example.medicaladherence.viewmodel.AddMedicationViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditMedicationScreen(
    medicationId: String? = null,
    onNavigateBack: () -> Unit,
    viewModel: AddMedicationViewModel = viewModel()
) {
    // Load medication if editing
    LaunchedEffect(medicationId) {
        if (medicationId != null) {
            viewModel.loadMedication(medicationId)
        }
    }
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showTimePicker by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (medicationId != null) "Edit Medication" else "Add Medication") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Enter your medication details",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = uiState.name,
                onValueChange = { viewModel.updateName(it) },
                label = { Text("Medication Name") },
                placeholder = { Text("e.g., Amlodipine") },
                isError = uiState.nameError != null,
                supportingText = uiState.nameError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.dosage,
                onValueChange = { viewModel.updateDosage(it) },
                label = { Text("Dosage") },
                placeholder = { Text("e.g., 5 mg") },
                isError = uiState.dosageError != null,
                supportingText = uiState.dosageError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Frequency selector
            Text(
                text = "Frequency",
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                MedicationFrequency.values().forEach { freq ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (uiState.frequency == freq),
                                onClick = { viewModel.updateFrequency(freq) },
                                role = Role.RadioButton
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (uiState.frequency == freq),
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (freq) {
                                MedicationFrequency.Daily -> "Daily"
                                MedicationFrequency.SpecificDays -> "Specific days"
                                MedicationFrequency.EveryXDays -> "Every X days"
                                MedicationFrequency.Weekly -> "Weekly"
                                MedicationFrequency.AsNeeded -> "As needed"
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            // Show day selector if SpecificDays is selected
            if (uiState.frequency == MedicationFrequency.SpecificDays) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Select days:",
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    days.forEachIndexed { index, day ->
                        val dayNum = index + 1
                        FilterChip(
                            selected = uiState.specificDays.contains(dayNum),
                            onClick = { viewModel.toggleDay(dayNum) },
                            label = { Text(day, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Times section with chips and add button
            Column {
                Text(
                    text = "Times",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Show existing times as chips
                if (uiState.times.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.times.forEach { time ->
                            InputChip(
                                selected = false,
                                onClick = { },
                                label = { Text(time) },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { viewModel.removeTime(time) },
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            "Remove",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Add time button
                OutlinedButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add time")
                }

                // Error text
                if (uiState.timesError != null) {
                    Text(
                        text = uiState.timesError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }

            OutlinedTextField(
                value = uiState.notes,
                onValueChange = { viewModel.updateNotes(it) },
                label = { Text("Notes (optional)") },
                placeholder = { Text("e.g., Take with food") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        val saved = if (medicationId != null) {
                            viewModel.saveWithId(medicationId)
                        } else {
                            viewModel.save()
                        }

                        if (saved) {
                            onNavigateBack()
                        }
                    },
                    enabled = uiState.isValid,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Text("Save")
                }
            }
        }

        // Time picker dialog
        if (showTimePicker) {
            TimePickerDialog(
                onDismiss = { showTimePicker = false },
                onConfirm = {
                    val hour = timePickerState.hour
                    val minute = timePickerState.minute
                    val time = String.format("%02d:%02d", hour, minute)
                    viewModel.addTime(time)
                    showTimePicker = false
                }
            ) {
                TimePicker(state = timePickerState)
            }
        }
    }
}

@Composable
fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("OK")
            }
        },
        text = { content() }
    )
}
