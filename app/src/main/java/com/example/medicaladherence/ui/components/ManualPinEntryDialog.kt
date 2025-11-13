package com.example.medicaladherence.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.medicaladherence.utils.AppConstants

/**
 * Dialog for manually entering a patient's 6-digit PIN to add them.
 * Validates PIN format and provides user feedback.
 */
@Composable
fun ManualPinEntryDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    isLoading: Boolean = false
) {
    var pin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Enter Patient PIN")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Enter the ${AppConstants.PIN_LENGTH}-digit PIN that the patient can see in their app.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= AppConstants.PIN_LENGTH && it.all { char -> char.isDigit() }) {
                            pin = it
                            errorMessage = null
                        }
                    },
                    label = { Text("Patient PIN") },
                    placeholder = { Text("000000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = if (errorMessage != null) {
                        { Text(errorMessage!!, color = MaterialTheme.colorScheme.error) }
                    } else {
                        { Text("${pin.length}/${AppConstants.PIN_LENGTH} digits") }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        pin.isEmpty() -> {
                            errorMessage = "Please enter a PIN"
                        }
                        pin.length != AppConstants.PIN_LENGTH -> {
                            errorMessage = "PIN must be ${AppConstants.PIN_LENGTH} digits"
                        }
                        else -> {
                            onConfirm(pin)
                        }
                    }
                },
                enabled = !isLoading
            ) {
                Text("Add Patient")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancel")
            }
        }
    )
}
