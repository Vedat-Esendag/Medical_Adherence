package com.example.medicaladherence.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.medicaladherence.data.model.PatientProfile
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaregiverPatientsScreen(
    patients: List<PatientProfile>,
    onScanQR: () -> Unit,
    onSelectPatient: (String) -> Unit,
    onRemovePatient: (PatientProfile) -> Unit,
    onManualPinEntry: (String) -> Unit = {},
    onResyncPatient: (PatientProfile) -> Unit = {}
) {
    var showRemoveDialog by remember { mutableStateOf<PatientProfile?>(null) }
    var showManualPinDialog by remember { mutableStateOf(false) }
    var showResyncSheet by remember { mutableStateOf<PatientProfile?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Patients") },
                actions = {
                    IconButton(onClick = onScanQR) {
                        Icon(Icons.Default.Add, "Scan QR Code")
                    }
                }
            )
        },
        floatingActionButton = {
            if (patients.isEmpty()) {
                FloatingActionButton(
                    onClick = onScanQR,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, "Scan QR Code")
                }
            }
        }
    ) { padding ->
        if (patients.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(
                        text = "👥",
                        style = MaterialTheme.typography.displayLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Patients Yet",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Scan a patient's QR code to start monitoring their medications",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = { showManualPinDialog = true }) {
                        Text("Or enter PIN manually")
                    }
                }
            }
        } else {
            // Patient list
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(patients) { patient ->
                    PatientCard(
                        patient = patient,
                        onSelect = { onSelectPatient(patient.pin) },
                        onRemove = { showRemoveDialog = patient },
                        onSync = { showResyncSheet = patient }
                    )
                }
            }
        }
    }
    
    // Remove confirmation dialog
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
                        onRemovePatient(patient)
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
    
    // Manual PIN entry dialog
    if (showManualPinDialog) {
        ManualPinEntryDialog(
            onDismiss = { showManualPinDialog = false },
            onPinEntered = { pin ->
                onManualPinEntry(pin)
                showManualPinDialog = false
            }
        )
    }
    
    // Re-sync bottom sheet
    showResyncSheet?.let { patient ->
        ResyncBottomSheet(
            patient = patient,
            onDismiss = { showResyncSheet = null },
            onScanQR = {
                showResyncSheet = null
                onScanQR()
            },
            onManualPin = {
                showResyncSheet = null
                showManualPinDialog = true
            }
        )
    }
}

@Composable
private fun PatientCard(
    patient: PatientProfile,
    onSelect: () -> Unit,
    onRemove: () -> Unit,
    onSync: () -> Unit = {}
) {
    val syncStatus = remember(patient.lastSyncedAt) {
        calculateSyncStatus(patient.lastSyncedAt)
    }
    
    val lastSyncDisplay = remember(patient.lastSyncedAt) {
        getRelativeTimeString(patient.lastSyncedAt)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onSelect,
        colors = CardDefaults.cardColors(
            containerColor = when (syncStatus) {
                SyncStatus.FRESH -> MaterialTheme.colorScheme.surfaceVariant
                SyncStatus.MODERATE -> MaterialTheme.colorScheme.surfaceVariant
                SyncStatus.STALE -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Warning banner for stale data
            if (syncStatus == SyncStatus.STALE) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Data may be outdated",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = patient.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Sync status indicator
                        SyncStatusBadge(syncStatus)
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "PIN: ${patient.pin}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "${patient.medicationCount} medication(s)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Last sync with icon and relative time
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Sync,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = when (syncStatus) {
                                SyncStatus.FRESH -> Color(0xFF4CAF50) // Green
                                SyncStatus.MODERATE -> Color(0xFFFFA726) // Orange
                                SyncStatus.STALE -> MaterialTheme.colorScheme.error
                            }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = lastSyncDisplay,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = when (syncStatus) {
                                SyncStatus.FRESH -> Color(0xFF4CAF50)
                                SyncStatus.MODERATE -> Color(0xFFFFA726)
                                SyncStatus.STALE -> MaterialTheme.colorScheme.error
                            }
                        )
                    }
                }
                
                // Action buttons column
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    IconButton(onClick = onSync) {
                        Icon(
                            Icons.Default.Sync,
                            "Sync Patient Data",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onRemove) {
                        Icon(
                            Icons.Default.Delete,
                            "Remove Patient",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncStatusBadge(status: SyncStatus) {
    val (color, text) = when (status) {
        SyncStatus.FRESH -> Color(0xFF4CAF50) to "✓"
        SyncStatus.MODERATE -> Color(0xFFFFA726) to "!"
        SyncStatus.STALE -> MaterialTheme.colorScheme.error to "⚠"
    }
    
    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.2f),
        modifier = Modifier.size(24.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

enum class SyncStatus {
    FRESH,      // < 3 days
    MODERATE,   // 3-7 days
    STALE       // > 7 days
}

fun calculateSyncStatus(lastSyncedAt: Long): SyncStatus {
    val now = System.currentTimeMillis()
    val daysSinceSync = (now - lastSyncedAt) / (1000 * 60 * 60 * 24)
    
    return when {
        daysSinceSync < 3 -> SyncStatus.FRESH
        daysSinceSync < 7 -> SyncStatus.MODERATE
        else -> SyncStatus.STALE
    }
}

fun getRelativeTimeString(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    val weeks = days / 7
    
    return when {
        seconds < 60 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        weeks < 4 -> "${weeks}w ago"
        else -> {
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            dateFormat.format(Date(timestamp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResyncBottomSheet(
    patient: PatientProfile,
    onDismiss: () -> Unit,
    onScanQR: () -> Unit,
    onManualPin: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Sync ${patient.name}'s Data",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Choose how to update this patient's medication data",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Scan QR Code option
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = onScanQR
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Scan QR Code",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Recommended - gets all data",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Manual PIN option
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = onManualPin
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔢",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enter PIN",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "For testing/same device only",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ManualPinEntryDialog(
    onDismiss: () -> Unit,
    onPinEntered: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter Patient PIN") },
        text = {
            Column {
                Text(
                    text = "Enter the 6-digit PIN from your patient's app.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = pin,
                    onValueChange = { 
                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                            pin = it
                            error = null
                        }
                    },
                    label = { Text("Patient PIN") },
                    placeholder = { Text("000000") },
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        pin.isEmpty() -> error = "Please enter a PIN"
                        pin.length != 6 -> error = "PIN must be 6 digits"
                        else -> onPinEntered(pin)
                    }
                },
                enabled = pin.length == 6
            ) {
                Text("Add Patient")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

