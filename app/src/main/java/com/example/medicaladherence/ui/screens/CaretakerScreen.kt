package com.example.medicaladherence.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.medicaladherence.viewmodel.CaretakerViewModel
import com.example.medicaladherence.viewmodel.MedicationAdherence
import com.example.medicaladherence.viewmodel.MissedDoseInfo
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// Utility function for subtle background tints based on adherence
fun getAdherenceTint(percentage: Int): Color = when {
    percentage >= 70 -> Color(0xFFE8F5E9)  // Light green
    percentage >= 30 -> Color(0xFFFFF9C4)  // Light yellow
    else -> Color(0xFFFFEBEE)              // Light red
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (uiState.patientPin.isNotEmpty()) {
                                Text(
                                    text = "PIN: ${uiState.patientPin}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = getRelativeTimeString(uiState.lastUpdated),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refresh() }
                    ) {
                        Icon(Icons.Default.Refresh, "Refresh data")
                    }
                    IconButton(
                        onClick = { /* TODO: Implement notification to patient */ }
                    ) {
                        Icon(Icons.Default.Notifications, "Notify patient")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            // Loading state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Loading patient data...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Content loaded
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

                // 2. Needs Attention Section (Compact)
                val problematicMeds = uiState.medicationBreakdown.filter { it.percentage < 70 }
            
                if (problematicMeds.isNotEmpty()) {
                    item {
                        CompactNeedsAttentionSection(
                            problematicMeds = problematicMeds,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactNeedsAttentionSection(
    problematicMeds: List<MedicationAdherence>,
    modifier: Modifier = Modifier
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    
    Column(modifier = modifier) {
        Text(
            text = "Needs Attention",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 24.dp, horizontal = 0.dp)
        )
        
        // Show top 3 compact cards
        val topThree = problematicMeds.take(3)
        topThree.forEach { med ->
            CompactAttentionCard(
                medication = med,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        // "View all" button if more than 3
        if (problematicMeds.size > 3) {
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedButton(
                onClick = { showBottomSheet = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("View all (${problematicMeds.size - 3} more)")
            }
        }
    }
    
    // Bottom sheet with full list
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "All Medications Needing Attention",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                problematicMeds.forEach { med ->
                    SeverityMedicationCard(
                        medication = med,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun CompactAttentionCard(
    medication: MedicationAdherence,
    modifier: Modifier = Modifier
) {
    val percentage = medication.percentage
    val missedCount = medication.totalCount - medication.takenCount
    
    // Get color based on severity
    val severityColor = when {
        percentage >= 70 -> Color(0xFF2E7D32)  // Green (shouldn't happen in this section)
        percentage >= 30 -> Color(0xFFEF6C00)  // Orange
        else -> Color(0xFFD32F2F)              // Red
    }
    
    val backgroundColor = when {
        percentage >= 70 -> Color(0xFFE8F5E9)
        percentage >= 30 -> Color(0xFFFFF9C4)
        else -> Color(0xFFFFEBEE)
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Color indicator bar
            Surface(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp),
                color = severityColor,
                shape = RoundedCornerShape(2.dp)
            ) {}
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Middle: Medication info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = medication.medicationName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (missedCount > 0) "$missedCount dose${if (missedCount > 1) "s" else ""} missed" else "No doses scheduled",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Right: Percentage badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = severityColor.copy(alpha = 0.15f),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = severityColor,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
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
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Large percentage display
            Text(
                text = "$weeklyAdherence%",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "This Week",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
            Spacer(Modifier.height(16.dp))

            // Quick stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Month",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "$monthlyAdherence%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Streak",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "$currentStreak days",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun SeverityMedicationCard(
    medication: MedicationAdherence,
    modifier: Modifier = Modifier
) {
    val percentage = medication.percentage
    val tintColor = getAdherenceTint(percentage)
    
    // Get color for percentage text
    val percentageColor = when {
        percentage >= 70 -> Color(0xFF2E7D32)  // Green
        percentage >= 30 -> Color(0xFFEF6C00)  // Orange
        else -> Color(0xFFD32F2F)              // Red
    }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = tintColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${medication.medicationName} ${medication.dosage}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    if (medication.totalCount > 0) {
                        val missedCount = medication.totalCount - medication.takenCount
                        Text(
                            text = when {
                                percentage < 30 -> "Only $percentage% taken this week"
                                missedCount > 0 -> "$missedCount of ${medication.totalCount} doses missed"
                                else -> "All doses taken this week"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "No scheduled doses",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = if (medication.totalCount == 0) "—" else "$percentage%",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = percentageColor
                )
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
            text = "Recent Missed Doses",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp, 24.dp, 16.dp, 8.dp)
        )

        if (missedDoses.isEmpty()) {
            // Empty state celebration
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
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
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "No Missed Doses!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Patient is doing great!",
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
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color(0xFFFFEBEE) // Light red tint
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dose.medicationName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = dose.dosage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = dose.time,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD32F2F)
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
    val percentage = medication.percentage
    
    // Get color for percentage text
    val percentageColor = when {
        percentage >= 70 -> Color(0xFF2E7D32)  // Green
        percentage >= 30 -> Color(0xFFEF6C00)  // Orange
        else -> Color(0xFFD32F2F)              // Red
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${medication.medicationName} ${medication.dosage}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (medication.totalCount == 0) "No scheduled doses" else "${medication.takenCount}/${medication.totalCount} doses taken",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = if (medication.totalCount == 0) "—" else "$percentage%",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = percentageColor
            )
        }
    }
}
