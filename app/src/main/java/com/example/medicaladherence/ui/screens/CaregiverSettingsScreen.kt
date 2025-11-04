package com.example.medicaladherence.ui.screens

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
            // Profile & Account Section
            item {
                SettingsSection(title = "Profile & Account") {
                    OutlinedTextField(
                        value = uiState.caregiverName,
                        onValueChange = { viewModel.updateCaregiverName(it) },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(Icons.Default.Person, null)
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = uiState.caregiverPhone,
                        onValueChange = { viewModel.updateCaregiverPhone(it) },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(Icons.Default.Phone, null)
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = uiState.caregiverEmail,
                        onValueChange = { viewModel.updateCaregiverEmail(it) },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(Icons.Default.Email, null)
                        }
                    )
                }
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
                    onRemove = { showRemoveDialog = patient }
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
            
            // Display Preferences Section
            item {
                SettingsSection(title = "Display Preferences") {
                    // Font Size Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Font Size",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "${(uiState.fontScale * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Slider(
                            value = uiState.fontScale,
                            onValueChange = { viewModel.updateFontScale(it) },
                            valueRange = 0.8f..1.4f,
                            steps = 5,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Small",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Large",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // High Contrast Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "High Contrast Mode",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Improves visibility",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.highContrastMode,
                            onCheckedChange = { viewModel.updateHighContrastMode(it) }
                        )
                    }
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

@Composable
private fun PatientManageCard(
    patient: PatientProfile,
    onRemove: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
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
                        text = patient.name,
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
            
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    "Remove",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

