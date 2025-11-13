package com.example.medicaladherence.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
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
    onScanQR: () -> Unit, // Now opens method selection dialog
    onSelectPatient: (String) -> Unit,
    onRemovePatient: (PatientProfile) -> Unit,
    onManualPinEntry: (String) -> Unit = {},
    onResyncPatient: (PatientProfile) -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    // Sort patients by addedAt descending (newest first)
    val sortedPatients = remember(patients) {
        patients.sortedByDescending { it.addedAt }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Patients") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                    IconButton(onClick = onScanQR) {
                        Icon(Icons.Default.Add, "Add Patient")
                    }
                }
            )
        },
        floatingActionButton = {
            if (sortedPatients.isEmpty()) {
                FloatingActionButton(
                    onClick = onScanQR,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, "Add Patient")
                }
            }
        }
    ) { padding ->
        if (sortedPatients.isEmpty()) {
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
                        text = "Add a patient to start monitoring their medications",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
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
                items(sortedPatients) { patient ->
                    PatientCard(
                        patient = patient,
                        onSelect = { onSelectPatient(patient.pin) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PatientCard(
    patient: PatientProfile,
    onSelect: () -> Unit,
    onRemove: () -> Unit = {},
    onSync: () -> Unit = {}
) {
    val lastSyncDisplay = remember(patient.lastSyncedAt) {
        getRelativeTimeString(patient.lastSyncedAt)
    }
    
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onSelect,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Avatar with initials
            PatientAvatar(
                name = patient.name,
                modifier = Modifier.size(48.dp)
            )
            
            // Patient info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = patient.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "PIN: ${patient.pin}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "${patient.medicationCount} medication(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Synced $lastSyncDisplay",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // Chevron arrow
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun PatientAvatar(
    name: String,
    modifier: Modifier = Modifier
) {
    // Get initials from name
    val initials = remember(name) {
        name.split(" ")
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .ifEmpty { "?" }
    }
    
    // Generate consistent color based on name
    val backgroundColor = remember(name) {
        val colors = listOf(
            Color(0xFF4CAF50), // Green
            Color(0xFF2196F3), // Blue
            Color(0xFF9C27B0), // Purple
            Color(0xFFFF9800), // Orange
            Color(0xFF00BCD4), // Cyan
            Color(0xFFE91E63)  // Pink
        )
        colors[name.hashCode().mod(colors.size)]
    }
    
    Surface(
        modifier = modifier,
        shape = androidx.compose.foundation.shape.CircleShape,
        color = backgroundColor
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = initials,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
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
