package com.example.medicaladherence.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.example.medicaladherence.viewmodel.SettingsViewModel
import com.example.medicaladherence.ui.components.SetPinDialog
import com.example.medicaladherence.ui.components.PinEntryDialog
import com.example.medicaladherence.ui.components.PatientQRDisplayDialog
import com.example.medicaladherence.util.QRCodeGenerator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onFontScaleChanged: (Float) -> Unit,
    onHighContrastChanged: (Boolean) -> Unit = {},
    viewModel: SettingsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSetPinDialog by remember { mutableStateOf(false) }
    var showRemovePinDialog by remember { mutableStateOf(false) }
    val hasPin = viewModel.caretakerPin.collectAsState().value != null
    
    // Dual-profile system states
    val userProfile by viewModel.userProfile.collectAsState()
    val pairingPin by viewModel.pairingPin.collectAsState()
    val patientName by viewModel.patientName.collectAsState()
    var showQRDialog by remember { mutableStateOf(false) }
    var qrCodeBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var isGeneratingQR by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Profile Card with Sign Out
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = patientName ?: "User",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = when (userProfile) {
                                    "patient" -> "Patient Profile"
                                    "caregiver" -> "Caregiver Profile"
                                    else -> "Profile"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        
                        OutlinedButton(
                            onClick = { showSignOutDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sign Out")
                        }
                    }
                }
            }
            
            item {
                // High Contrast Mode section
                Text(
                    text = "High Contrast Mode",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "High Contrast",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Black background with white text for better visibility",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = viewModel.highContrastMode.collectAsState().value,
                        onCheckedChange = { enabled ->
                            viewModel.setHighContrastMode(enabled)
                            onHighContrastChanged(enabled)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = if (enabled) "High contrast mode enabled" else "High contrast mode disabled",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    )
                }
            }

            HorizontalDivider()

            // Font size section
            Text(
                text = "Font Size",
                style = MaterialTheme.typography.titleLarge
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup()
            ) {
                // Normal option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .selectable(
                            selected = (uiState.fontScale == 1.0f),
                            onClick = {
                                viewModel.setFontScale(1.0f)
                                onFontScaleChanged(1.0f)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "Font size set to Normal",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (uiState.fontScale == 1.0f),
                        onClick = null
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Normal",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Standard text size",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Large option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .selectable(
                            selected = (uiState.fontScale == 1.15f),
                            onClick = {
                                viewModel.setFontScale(1.15f)
                                onFontScaleChanged(1.15f)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "Font size set to Large",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (uiState.fontScale == 1.15f),
                        onClick = null
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Large",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Larger text for easier reading",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider()

            // Theme section (info only)
            Text(
                text = "Theme",
                style = MaterialTheme.typography.titleLarge
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Follow system",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "App theme matches your device settings (Light/Dark mode)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider()

            // Caretaker PIN Section
            Text(
                text = "Caretaker Protection",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Caretaker PIN",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (hasPin) "PIN is set" else "No PIN set",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (hasPin) {
                            TextButton(onClick = { showRemovePinDialog = true }) {
                                Text("Remove")
                            }
                        } else {
                            Button(onClick = { showSetPinDialog = true }) {
                                Text("Set PIN")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Protect medication changes with a PIN. Only users with the PIN can add, edit, or delete medications.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Patient Pairing Section (only for patients)
            pairingPin?.let { pin ->
                if (userProfile == "patient") {
                    HorizontalDivider()
                    
                    Text(
                        text = "Caregiver Pairing",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Your Pairing PIN",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = pin,
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Pairing PIN", pin)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "PIN copied to clipboard", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Assignment,
                                        contentDescription = "Copy PIN",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Share this PIN and QR code with your caregiver to allow them to monitor your medications.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Button(
                                onClick = {
                                    isGeneratingQR = true
                                    coroutineScope.launch {
                                        val qrData = viewModel.generatePatientQRData()
                                        if (qrData != null) {
                                            qrCodeBitmap = QRCodeGenerator.generateQRCode(qrData, 512)
                                            showQRDialog = true
                                        } else {
                                            snackbarHostState.showSnackbar(
                                                message = "Failed to generate QR code",
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                        isGeneratingQR = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isGeneratingQR
                            ) {
                                if (isGeneratingQR) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Generating...")
                                } else {
                                    Icon(Icons.Default.Share, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Show QR Code")
                                }
                            }
                        }
                    }
                }
            }
            }
        }
    }

    // Sign Out Confirmation Dialog
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out? You'll need to select your profile again next time.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearUserProfile()
                        showSignOutDialog = false
                    }
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // PIN Dialogs
    if (showSetPinDialog) {
        SetPinDialog(
            onDismiss = { showSetPinDialog = false },
            onPinSet = { pin ->
                viewModel.setCaretakerPin(pin)
                showSetPinDialog = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Caretaker PIN set successfully",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        )
    }

    if (showRemovePinDialog) {
        PinEntryDialog(
            title = "Remove Caretaker PIN",
            onDismiss = { showRemovePinDialog = false },
            onPinEntered = { enteredPin ->
                if (viewModel.verifyPin(enteredPin)) {
                    viewModel.removeCaretakerPin()
                    showRemovePinDialog = false
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Caretaker PIN removed",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            }
        )
    }
    
    // QR Code Dialog for patients
    if (showQRDialog) {
        pairingPin?.let { pin ->
            PatientQRDisplayDialog(
                patientName = patientName ?: "Unknown",
                patientPin = pin,
                qrCodeBitmap = qrCodeBitmap,
                onDismiss = { 
                    showQRDialog = false
                    qrCodeBitmap = null
                },
                onRefresh = {
                    coroutineScope.launch {
                        val qrData = viewModel.generatePatientQRData()
                        if (qrData != null) {
                            qrCodeBitmap = QRCodeGenerator.generateQRCode(qrData, 512)
                        }
                    }
                }
            )
        }
    }
}
