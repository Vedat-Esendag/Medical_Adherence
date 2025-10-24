package com.example.medicaladherence.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.medicaladherence.viewmodel.MedicationsLibraryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationsLibraryScreen(
    onNavigateToAdd: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    viewModel: MedicationsLibraryViewModel = viewModel()
) {
    val medications by viewModel.medications.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var medicationToDelete by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Medications") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAdd) {
                Icon(Icons.Default.Add, contentDescription = "Add Medication")
            }
        }
    ) { padding ->
        if (medications.isEmpty()) {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "💊",
                    style = MaterialTheme.typography.displayLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No medications yet",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap the + button to add your first medication",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "All Medications (${medications.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(medications) { medication ->
                    MedicationLibraryCard(
                        medication = medication,
                        onEdit = { onNavigateToEdit(medication.id) },
                        onDelete = {
                            medicationToDelete = medication.id
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog && medicationToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                medicationToDelete = null
            },
            title = { Text("Delete Medication?") },
            text = { Text("This will permanently remove this medication and all its history.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        medicationToDelete?.let { viewModel.deleteMedication(it) }
                        showDeleteDialog = false
                        medicationToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    medicationToDelete = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MedicationLibraryCard(
    medication: com.example.medicaladherence.data.model.Medication,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Pill icon
            Text(
                text = "💊",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 16.dp)
            )

            // Medication info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = medication.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = medication.dosage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Schedule
                Text(
                    text = "Schedule:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                medication.times.forEach { time ->
                    Text(
                        text = "• Daily at $time",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Notes if present
                if (!medication.notes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = medication.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }

            // Overflow menu
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, "More options")
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            showMenu = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}
