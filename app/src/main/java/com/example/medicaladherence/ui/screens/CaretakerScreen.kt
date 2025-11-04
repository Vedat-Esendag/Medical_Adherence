package com.example.medicaladherence.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.medicaladherence.viewmodel.CaretakerViewModel
import com.example.medicaladherence.viewmodel.MedicationAdherence
import com.example.medicaladherence.viewmodel.MissedDoseInfo
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// Utility functions for severity-based color coding
fun getSeverityColor(percentage: Int): Color = when {
    percentage >= 90 -> Color(0xFF2E7D32)  // Dark Green
    percentage >= 80 -> Color(0xFF0288D1)  // Blue
    percentage >= 60 -> Color(0xFFEF6C00)  // Dark Orange
    percentage >= 40 -> Color(0xFFD32F2F)  // Red
    else -> Color(0xFF9C27B0)              // Purple
}

fun getSeverityLabel(percentage: Int): String = when {
    percentage >= 90 -> "EXCELLENT"
    percentage >= 80 -> "GOOD"
    percentage >= 60 -> "CONCERNING"
    percentage >= 40 -> "CRITICAL"
    else -> "SEVERE"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaretakerScreen(
    viewModel: CaretakerViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.patientName.ifEmpty { "Loading..." },
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (uiState.patientPin.isNotEmpty()) {
                            Text(
                                text = "PIN: ${uiState.patientPin}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Implement in Option C */ }) {
                        Icon(Icons.Default.Phone, "Call")
                    }
                    IconButton(onClick = { /* TODO: Implement in Option C */ }) {
                        Icon(Icons.Default.Message, "Message")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 1. Hero Status Card
            item {
                HeroStatusCard(
                    weeklyAdherence = uiState.weeklyAdherence,
                    monthlyAdherence = uiState.monthlyAdherence,
                    currentStreak = uiState.currentStreak,
                    longestStreak = uiState.longestStreak,
                    trend = uiState.adherenceTrend
                )
            }

            // 2. Needs Attention Section Header (Only if <80% meds exist)
            val problematicMeds = uiState.medicationBreakdown.filter { it.percentage < 80 }
            
            if (problematicMeds.isNotEmpty()) {
                item {
                    Text(
                        text = "🚨 NEEDS ATTENTION (${problematicMeds.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD32F2F),
                        modifier = Modifier.padding(16.dp, 24.dp, 16.dp, 8.dp)
                    )
                }
            }

            // 3. Problematic Medication Cards
            items(problematicMeds) { med ->
                SeverityMedicationCard(
                    medication = med,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            // 4. Missed Doses Timeline
            item {
                GroupedMissedDosesSection(
                    missedDoses = uiState.recentMissedDoses
                )
            }

            // 5. All Medications (Collapsed)
            item {
                AllMedicationsSection(
                    allMedications = uiState.medicationBreakdown
                )
            }

            // Bottom padding
            item {
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun HeroStatusCard(
    weeklyAdherence: Int,
    monthlyAdherence: Int,
    currentStreak: Int,
    longestStreak: Int,
    trend: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                weeklyAdherence >= 90 -> Color(0xFF2E7D32).copy(alpha = 0.1f)
                weeklyAdherence >= 80 -> Color(0xFF0288D1).copy(alpha = 0.1f)
                weeklyAdherence >= 60 -> Color(0xFFEF6C00).copy(alpha = 0.1f)
                else -> Color(0xFFD32F2F).copy(alpha = 0.1f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Large circular progress indicator
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(140.dp)
            ) {
                CircularProgressIndicator(
                    progress = { weeklyAdherence / 100f },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 12.dp,
                    color = getSeverityColor(weeklyAdherence),
                    trackColor = getSeverityColor(weeklyAdherence).copy(alpha = 0.2f)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$weeklyAdherence%",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = getSeverityColor(weeklyAdherence)
                    )
                    Text(
                        text = "THIS WEEK",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Trend indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = when {
                        trend.contains("Improving", ignoreCase = true) -> Icons.Default.TrendingUp
                        trend.contains("Declining", ignoreCase = true) -> Icons.Default.TrendingDown
                        else -> Icons.Default.TrendingFlat
                    },
                    contentDescription = null,
                    tint = when {
                        trend.contains("Improving", ignoreCase = true) -> Color(0xFF2E7D32)
                        trend.contains("Declining", ignoreCase = true) -> Color(0xFFD32F2F)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = trend,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(12.dp))

            // Quick stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickStat("Month", "$monthlyAdherence%")
                QuickStat("Streak", "$currentStreak🔥")
                QuickStat("Best", "$longestStreak days")
            }
        }
    }
}

@Composable
fun QuickStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SeverityMedicationCard(
    medication: MedicationAdherence,
    modifier: Modifier = Modifier
) {
    val percentage = medication.percentage
    val severityColor = getSeverityColor(percentage)
    val severityLabel = getSeverityLabel(percentage)

    // Border thickness based on severity
    val borderWidth = when {
        percentage < 40 -> 3.dp
        percentage < 60 -> 2.dp
        else -> 1.dp
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = severityColor.copy(alpha = 0.08f)
        ),
        border = BorderStroke(borderWidth, severityColor.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(
            defaultElevation = when {
                percentage < 40 -> 6.dp
                percentage < 60 -> 3.dp
                else -> 1.dp
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header row: Severity badge + Name + Percentage
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Severity badge
                Surface(
                    color = severityColor,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = severityLabel,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = medication.medicationName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = medication.dosage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = if (medication.totalCount == 0) "No data" else "$percentage%",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = severityColor
                )
            }

            Spacer(Modifier.height(12.dp))

            // Progress bar
            if (medication.totalCount > 0) {
                Column {
                    LinearProgressIndicator(
                        progress = { percentage / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = severityColor,
                        trackColor = severityColor.copy(alpha = 0.2f)
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "${medication.takenCount} of ${medication.totalCount} doses taken (Last 7 days)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Action buttons (placeholders for now - will be functional in Option C)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { /* TODO: Will be implemented in Option C */ },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Details")
                    }

                    Button(
                        onClick = { /* TODO: Will be implemented in Option C */ },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = severityColor
                        )
                    ) {
                        Icon(
                            Icons.Default.NotificationImportant,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Remind")
                    }
                }
            }
        }
    }
}

@Composable
fun GroupedMissedDosesSection(
    missedDoses: List<MissedDoseInfo>,
    modifier: Modifier = Modifier
) {
    var expandedDates by remember { mutableStateOf(setOf<LocalDate>()) }

    Column(modifier = modifier) {
        Text(
            text = "📅 RECENT MISSED DOSES (${missedDoses.size})",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp, 24.dp, 16.dp, 8.dp)
        )

        if (missedDoses.isEmpty()) {
            // Empty state celebration
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF2E7D32).copy(alpha = 0.1f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "No Missed Doses!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                    Text(
                        text = "Patient is doing great! 🎉",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Group by date
            val groupedByDate = missedDoses.groupBy { it.date }

            groupedByDate.forEach { (date, doses) ->
                val isExpanded = expandedDates.contains(date)
                val dateLabel = when (date) {
                    LocalDate.now() -> "Today"
                    LocalDate.now().minusDays(1) -> "Yesterday"
                    else -> date.format(DateTimeFormatter.ofPattern("MMM dd"))
                }

                // Date header (clickable to expand/collapse)
                Surface(
                    onClick = {
                        expandedDates = if (isExpanded) {
                            expandedDates - date
                        } else {
                            expandedDates + date
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$dateLabel (${doses.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand"
                        )
                    }
                }

                // Expanded dose items
                AnimatedVisibility(visible = isExpanded) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        doses.forEach { dose ->
                            MissedDoseItem(dose)
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MissedDoseItem(dose: MissedDoseInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFEBEE) // Light red
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Cancel,
                contentDescription = null,
                tint = Color(0xFFD32F2F),
                modifier = Modifier.size(24.dp)
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dose.medicationName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = dose.dosage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = dose.time,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun AllMedicationsSection(
    allMedications: List<MedicationAdherence>,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        // Toggle button
        Surface(
            onClick = { isExpanded = !isExpanded },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Medication,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "All Medications (${allMedications.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand"
                )
            }
        }

        // Expanded medication list
        AnimatedVisibility(visible = isExpanded) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                allMedications.forEach { med ->
                    CompactMedicationCard(med)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun CompactMedicationCard(medication: MedicationAdherence) {
    val severityColor = getSeverityColor(medication.percentage)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = severityColor.copy(alpha = 0.05f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = medication.medicationName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (medication.totalCount == 0) "No data" else "${medication.takenCount}/${medication.totalCount} doses",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = if (medication.totalCount == 0) "N/A" else "${medication.percentage}%",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = severityColor
            )
        }
    }
}
