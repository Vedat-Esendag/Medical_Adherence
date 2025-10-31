package com.example.medicaladherence.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.medicaladherence.ui.components.DoseCard
import com.example.medicaladherence.ui.components.PinEntryDialog
import com.example.medicaladherence.viewmodel.HomeViewModel
import com.example.medicaladherence.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToAdd: () -> Unit,
    onNavigateToEdit: (String) -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current

    // Initialize viewModel with context
    LaunchedEffect(Unit) {
        viewModel.initialize(context)
    }

    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = androidx.compose.material3.SnackbarHostState()

    val settingsViewModel: SettingsViewModel = viewModel()
    val caretakerPin by settingsViewModel.caretakerPin.collectAsState()

    var showEditPinDialog by remember { mutableStateOf(false) }
    var showDeletePinDialog by remember { mutableStateOf(false) }
    var showAddPinDialog by remember { mutableStateOf(false) }
    var pendingEditMedId by remember { mutableStateOf<String?>(null) }
    var pendingDeleteMedId by remember { mutableStateOf<String?>(null) }

    // Show snackbar when message is present with Undo action
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )

            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoLastMarkedDose()
            }

            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Medical Adherence") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (caretakerPin != null) {
                        showAddPinDialog = true
                    } else {
                        onNavigateToAdd()
                    }
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Medication")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (uiState.todayDoses.isEmpty()) {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "No medications yet",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap the + button below to add your first medication",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        if (caretakerPin != null) {
                            showAddPinDialog = true
                        } else {
                            onNavigateToAdd()
                        }
                    }
                ) {
                    Text("Add Medication")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = 80.dp  // Clears bottom nav + FAB
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header card with countdown and stats
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (uiState.nextDoseName.isNotEmpty()) {
                                // Has upcoming dose
                                Text(
                                    text = "Next dose",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = uiState.nextDoseCountdown,
                                    style = MaterialTheme.typography.displayMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "${uiState.nextDoseName} ${uiState.nextDoseDosage}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "at ${uiState.nextDoseTime}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )

                                // Show "Take Now" button if within dose window
                                if (uiState.isInDoseWindow) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = {
                                            viewModel.markTaken(
                                                uiState.nextDoseMedicationId,
                                                uiState.nextDoseTime
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Take Now")
                                    }
                                }
                            } else {
                                // All done for today
                                Text(
                                    text = "✓",
                                    style = MaterialTheme.typography.displayLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "All doses completed!",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(16.dp))

                            // Weekly stats
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "This week",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = "${uiState.weeklyAdherencePercent}%",
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
                                        text = "${uiState.streakDays} days",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                // Today's doses
                item {
                    Text(
                        text = "Today's Doses",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(uiState.todayDoses) { dose ->
                    DoseCard(
                        dose = dose,
                        onTaken = { viewModel.markTaken(dose.medication.id, dose.time) },
                        onMissed = { viewModel.markMissed(dose.medication.id, dose.time) },
                        onSnooze = { viewModel.snooze15(dose.medication.id, dose.time) },
                        onUndo = { viewModel.undoDose(dose.medication.id, dose.time) },
                        onEdit = {
                            if (caretakerPin != null) {
                                pendingEditMedId = dose.medication.id
                                showEditPinDialog = true
                            } else {
                                onNavigateToEdit(dose.medication.id)
                            }
                        },
                        onDelete = {
                            if (caretakerPin != null) {
                                pendingDeleteMedId = dose.medication.id
                                showDeletePinDialog = true
                            } else {
                                viewModel.deleteMedication(dose.medication.id)
                            }
                        }
                    )
                }
            }
        }
    }

    // PIN Dialogs
    if (showAddPinDialog) {
        PinEntryDialog(
            onDismiss = { showAddPinDialog = false },
            onPinEntered = { enteredPin ->
                if (settingsViewModel.verifyPin(enteredPin)) {
                    showAddPinDialog = false
                    onNavigateToAdd()
                }
            }
        )
    }

    if (showEditPinDialog) {
        PinEntryDialog(
            onDismiss = {
                showEditPinDialog = false
                pendingEditMedId = null
            },
            onPinEntered = { enteredPin ->
                if (settingsViewModel.verifyPin(enteredPin)) {
                    showEditPinDialog = false
                    pendingEditMedId?.let { onNavigateToEdit(it) }
                    pendingEditMedId = null
                }
            }
        )
    }

    if (showDeletePinDialog) {
        PinEntryDialog(
            onDismiss = {
                showDeletePinDialog = false
                pendingDeleteMedId = null
            },
            onPinEntered = { enteredPin ->
                if (settingsViewModel.verifyPin(enteredPin)) {
                    showDeletePinDialog = false
                    pendingDeleteMedId?.let { viewModel.deleteMedication(it) }
                    pendingDeleteMedId = null
                }
            }
        )
    }
}
