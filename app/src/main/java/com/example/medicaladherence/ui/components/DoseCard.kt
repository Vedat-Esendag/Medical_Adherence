package com.example.medicaladherence.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.medicaladherence.viewmodel.DoseItem

/**
 * Card displaying a single dose with action buttons (Taken, Missed, Snooze)
 */
@Composable
fun DoseCard(
    dose: DoseItem,
    onTaken: () -> Unit,
    onMissed: () -> Unit,
    onSnooze: () -> Unit,
    onUndo: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Medication?") },
            text = { Text("Are you sure you want to delete ${dose.medication.name}? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp,
            hoveredElevation = 3.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pill icon (medication symbol)
            Text(
                text = "💊",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dose.medication.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${dose.medication.dosage} at ${dose.time}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                        text = { Text("Delete") },
                        onClick = {
                            showMenu = false
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }

        // Show either collapsed state (chip + undo) or action buttons
        if (dose.taken != null) {
            // Collapsed state - show chip + undo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = { },
                    label = {
                        Text(
                            text = if (dose.taken == true) "✓ Taken" else "Missed",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (dose.taken == true)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                TextButton(onClick = onUndo) {
                    Text("Undo")
                }
            }
        } else {
            // Action buttons with visual hierarchy
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // TAKEN = Primary (most important action)
                Button(
                    onClick = onTaken,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    enabled = dose.taken != true
                ) {
                    Text("Taken")
                }

                // MISSED = Outlined (secondary, softer)
                OutlinedButton(
                    onClick = onMissed,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    enabled = dose.taken != false
                ) {
                    Text("Missed")
                }

                // SNOOZE = Tonal (tertiary, least prominent)
                FilledTonalButton(
                    onClick = onSnooze,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    enabled = dose.taken == null
                ) {
                    Text("Snooze 15m")
                }
            }
        }
    }
}
