package com.example.medicaladherence.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Dialog that asks the user how they want to add a patient:
 * - Scan QR Code (requires camera permission)
 * - Enter PIN Manually (no camera needed)
 */
@Composable
fun AddPatientMethodDialog(
    onDismiss: () -> Unit,
    onScanQR: () -> Unit,
    onManualEntry: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add Patient")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "How would you like to add a patient?",
                    style = MaterialTheme.typography.bodyLarge
                )

                // QR Scan Button
                Button(
                    onClick = {
                        onDismiss()
                        onScanQR()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Scan QR Code")
                }

                // Manual PIN Button
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onManualEntry()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Enter PIN Manually")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
