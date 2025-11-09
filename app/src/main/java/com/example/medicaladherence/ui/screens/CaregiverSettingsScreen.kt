package com.example.medicaladherence.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.medicaladherence.data.model.PatientProfile
import com.example.medicaladherence.viewmodel.CaregiverSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaregiverSettingsScreen(
    viewModel: CaregiverSettingsViewModel,
    onNavigateBack: () -> Unit,
    onAddPatient: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showRemoveDialog by remember { mutableStateOf<PatientProfile?>(null) }
    var editingPatient by remember { mutableStateOf<PatientProfile?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Notification Settings Section
            item {
                NotificationSettingsSection(
                    alertThreshold = uiState.alertThreshold,
                    dailySummaryEnabled = uiState.dailySummaryEnabled,
                    onAlertThresholdChange = { viewModel.updateAlertThreshold(it) },
                    onDailySummaryChange = { viewModel.updateDailySummary(it) }
                )
            }
            
            // Manage Patients Section
            item {
                SettingsSection(title = "Manage Patients") {
                    if (uiState.patients.isEmpty()) {
                        Text(
                            text = "No patients added yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Patient list
            items(uiState.patients) { patient ->
                PatientManageCard(
                    patient = patient,
                    onRemove = { showRemoveDialog = patient },
                    onEdit = { editingPatient = patient }
                )
            }
            
            // Add Patient Button
            item {
                OutlinedButton(
                    onClick = onAddPatient,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add New Patient")
                }
            }
        }
    }
    
    // Remove Patient Confirmation Dialog
    showRemoveDialog?.let { patient ->
        AlertDialog(
            onDismissRequest = { showRemoveDialog = null },
            title = { Text("Remove Patient") },
            text = { 
                Text("Are you sure you want to stop monitoring ${patient.name}? This will remove all their data from your device.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removePatient(patient)
                        showRemoveDialog = null
                    }
                ) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Edit Patient BottomSheet
    editingPatient?.let { patient ->
        EditPatientBottomSheet(
            patient = patient,
            onDismiss = { editingPatient = null },
            onSave = { displayName, phoneNumber, notes ->
                viewModel.updatePatient(patient, displayName, phoneNumber, notes)
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationSettingsSection(
    alertThreshold: Int,
    dailySummaryEnabled: Boolean,
    onAlertThresholdChange: (Int) -> Unit,
    onDailySummaryChange: (Boolean) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val thresholdOptions = listOf(50, 60, 70, 80, 90)
    
    SettingsSection(title = "Notifications") {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Alert Threshold Dropdown
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Alert me when adherence drops below:",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = "$alertThreshold%",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        thresholdOptions.forEach { threshold ->
                            DropdownMenuItem(
                                text = { Text("$threshold%") },
                                onClick = {
                                    onAlertThresholdChange(threshold)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
            
            // Daily Summary Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Send daily summary",
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = dailySummaryEnabled,
                    onCheckedChange = onDailySummaryChange
                )
            }
            
            // Future enhancements:
            // - Time picker for daily summary scheduling
            // - FCM integration for actual push notifications
            
            // Info text
            Text(
                text = "Note: Notification delivery requires FCM setup (future enhancement)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun PatientManageCard(
    patient: PatientProfile,
    onRemove: () -> Unit,
    onEdit: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Avatar
                PatientAvatar(
                    name = patient.name,
                    modifier = Modifier.size(40.dp)
                )
                
                Column {
                    Text(
                        text = patient.displayName ?: patient.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "PIN: ${patient.pin}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Edit,
                    "Edit",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                
                IconButton(onClick = { onRemove() }) {
                    Icon(
                        Icons.Default.Delete,
                        "Remove",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditPatientBottomSheet(
    patient: PatientProfile,
    onDismiss: () -> Unit,
    onSave: (displayName: String?, phoneNumber: String?, notes: String?) -> Unit
) {
    var displayName by remember { mutableStateOf(patient.displayName ?: patient.name) }
    var phoneNumber by remember { mutableStateOf(patient.phoneNumber ?: "") }
    var notes by remember { mutableStateOf(patient.notes ?: "") }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Edit Patient Info",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Display Name") },
                supportingText = { Text("How you'd like to refer to this patient") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Phone Number") },
                supportingText = { Text("For quick calling (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                supportingText = { Text("Medication reminders, doctor info, etc. (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        onSave(
                            displayName.takeIf { it.isNotBlank() },
                            phoneNumber.takeIf { it.isNotBlank() },
                            notes.takeIf { it.isNotBlank() }
                        )
                        onDismiss()
                    }
                ) {
                    Text("Save")
                }
            }
        }
    }
}

