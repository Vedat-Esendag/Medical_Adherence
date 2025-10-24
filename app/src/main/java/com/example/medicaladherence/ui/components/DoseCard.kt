package com.example.medicaladherence.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Medication icon (heart represents health/wellness)
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dose.medication.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${dose.medication.dosage} at ${dose.time}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Action buttons (large for accessibility)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = onTaken,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp),
                enabled = dose.taken != true
            ) {
                Text(if (dose.taken == true) "✓ Taken" else "Taken")
            }

            FilledTonalButton(
                onClick = onMissed,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp),
                enabled = dose.taken != false
            ) {
                Text(if (dose.taken == false) "✗ Missed" else "Missed")
            }

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
