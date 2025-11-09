package com.example.medicaladherence

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import android.util.Log
import com.example.medicaladherence.data.repo.RepositoryProvider
import com.example.medicaladherence.data.repository.FirebaseMedicationRepository
import com.example.medicaladherence.notification.NotificationScheduler
import com.example.medicaladherence.ui.nav.Routes
import com.example.medicaladherence.ui.screens.*
import com.example.medicaladherence.ui.theme.MedicalAdherenceTheme
import com.example.medicaladherence.viewmodel.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Firebase repository
        val repository = RepositoryProvider.provideRepository(applicationContext)

        setContent {
            MedicalAdherenceApp(repository)
        }
    }
}

@Composable
fun MedicalAdherenceApp(repository: FirebaseMedicationRepository) {
    val settingsViewModel: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = SettingsViewModelFactory(repository)
    )

    val highContrastMode by settingsViewModel.highContrastMode.collectAsState()
    val userProfile by settingsViewModel.userProfile.collectAsState()

    MedicalAdherenceTheme(highContrastMode = highContrastMode) {
        when (userProfile) {
            null -> {
                ProfileSelectionScreen(
                    onProfileSelected = { profile, name ->
                        settingsViewModel.setUserProfile(profile, name)
                    }
                )
            }
            "patient" -> {
                key("patient_screen") {
                    PatientMainScreen(
                        settingsViewModel = settingsViewModel,
                        onHighContrastChanged = { enabled ->
                            settingsViewModel.setHighContrastMode(enabled)
                        }
                    )
                }
            }
            "caregiver" -> {
                key("caregiver_screen") {
                    CaregiverMainScreen(repository)
                }
            }
        }
    }
}

@Composable
fun PatientMainScreen(
    settingsViewModel: SettingsViewModel,
    onHighContrastChanged: (Boolean) -> Unit
) {
    val navController = rememberNavController()
    var fontScale by remember { mutableFloatStateOf(1.0f) }
    val context = LocalContext.current

    // Request notification permission (Android 13+) and schedule notifications
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _: Boolean ->
        // Notification permission handled
    }

    LaunchedEffect(Unit) {
        // Request notification permission if needed (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Schedule notifications for patient medications
        try {
            val scheduler = NotificationScheduler(context)
            scheduler.rescheduleAllFromFirebase()
        } catch (e: Exception) {
            Log.e("PatientMainScreen", "Error scheduling notifications", e)
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Define which routes should show bottom bar (NO CARETAKER TAB)
    val routesWithBottomBar = listOf(
        Routes.HOME,
        Routes.MEDICATIONS,
        Routes.STATS,
        Routes.SETTINGS
    )
    val showBottomBar = currentRoute in routesWithBottomBar

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        icon = {
                            Icon(
                                if (currentRoute == Routes.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                                contentDescription = "Home"
                            )
                        },
                        label = { Text("Home") },
                        selected = currentRoute == Routes.HOME,
                        onClick = {
                            if (currentRoute != Routes.HOME) {
                                navController.navigate(Routes.HOME) {
                                    popUpTo(Routes.HOME) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        }
                    )

                    NavigationBarItem(
                        icon = {
                            Text(
                                text = "💊",
                                style = MaterialTheme.typography.titleLarge
                            )
                        },
                        label = { Text("Medications") },
                        selected = currentRoute == Routes.MEDICATIONS,
                        onClick = {
                            if (currentRoute != Routes.MEDICATIONS) {
                                navController.navigate(Routes.MEDICATIONS) {
                                    launchSingleTop = true
                                }
                            }
                        }
                    )

                    NavigationBarItem(
                        icon = {
                            Text(
                                text = "📊",
                                style = MaterialTheme.typography.titleLarge
                            )
                        },
                        label = { Text("Stats") },
                        selected = currentRoute == Routes.STATS,
                        onClick = {
                            if (currentRoute != Routes.STATS) {
                                navController.navigate(Routes.STATS) {
                                    launchSingleTop = true
                                }
                            }
                        }
                    )

                    NavigationBarItem(
                        icon = {
                            Icon(
                                if (currentRoute == Routes.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                                contentDescription = "Settings"
                            )
                        },
                        label = { Text("Settings") },
                        selected = currentRoute == Routes.SETTINGS,
                        onClick = {
                            if (currentRoute != Routes.SETTINGS) {
                                navController.navigate(Routes.SETTINGS) {
                                    launchSingleTop = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Routes.HOME) {
                    HomeScreen(
                        onNavigateToAdd = { navController.navigate(Routes.ADD_MEDICATION) },
                        onNavigateToEdit = { medId ->
                            navController.navigate("add_medication?id=$medId")
                        }
                    )
                }

                composable(Routes.MEDICATIONS) {
                    MedicationsLibraryScreen(
                        onNavigateToAdd = { navController.navigate(Routes.ADD_MEDICATION) },
                        onNavigateToEdit = { medId ->
                            navController.navigate("add_medication?id=$medId")
                        }
                    )
                }

                composable(
                    route = "add_medication?id={medId}",
                    arguments = listOf(
                        navArgument("medId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) { backStackEntry ->
                    val medId = backStackEntry.arguments?.getString("medId")
                    AddEditMedicationScreen(
                        medicationId = medId,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Routes.STATS) {
                    StatsScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onFontScaleChanged = { newScale -> fontScale = newScale },
                        onHighContrastChanged = onHighContrastChanged,
                        viewModel = settingsViewModel
                    )
                }
            }
        }
    }

@Composable
fun CaregiverMainScreen(repository: FirebaseMedicationRepository) {
    val navController = rememberNavController()
    val context = LocalContext.current
    var showCameraPermissionDialog by remember { mutableStateOf(false) }
    var showCameraPermissionDeniedDialog by remember { mutableStateOf(false) }
    
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            navController.navigate("qr_scanner")
        } else {
            showCameraPermissionDeniedDialog = true
        }
    }
    
    NavHost(
        navController = navController,
        startDestination = "caregiver_patients"
    ) {
        composable("caregiver_patients") {
            val viewModel: CaregiverPatientsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = CaregiverPatientsViewModelFactory(repository)
            )
            val patients by viewModel.patients.collectAsState()
            val importStatus by viewModel.importStatus.collectAsState()
            val snackbarHostState = remember { SnackbarHostState() }
            val coroutineScope = rememberCoroutineScope()
            var hasNavigated by rememberSaveable { mutableStateOf(false) }
            
            // Handle import status
            LaunchedEffect(importStatus) {
                when (val status = importStatus) {
                    is ImportStatus.Success -> {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Patient added successfully!")
                        }
                        viewModel.resetImportStatus()
                    }
                    is ImportStatus.Error -> {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(status.message)
                        }
                        viewModel.resetImportStatus()
                    }
                    else -> {}
                }
            }
            
            // Adaptive single-patient auto-navigation
            LaunchedEffect(patients) {
                if (patients.size == 1 && !hasNavigated) {
                    delay(400) // Brief transition for smooth UX
                    hasNavigated = true
                    navController.navigate("patient_monitor/${patients[0].pin}")
                }
            }
            
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { padding ->
                Box(modifier = Modifier.padding(padding)) {
                    CaregiverPatientsScreen(
                        patients = patients,
                        onScanQR = {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                            
                            if (hasPermission) {
                                navController.navigate("qr_scanner")
                            } else {
                                showCameraPermissionDialog = true
                            }
                        },
                        onSelectPatient = { pin ->
                            navController.navigate("patient_monitor/$pin")
                        },
                        onRemovePatient = { patient ->
                            viewModel.removePatient(patient)
                        },
                        onManualPinEntry = { pin ->
                            viewModel.importPatientFromPin(pin)
                        },
                        onNavigateToSettings = {
                            navController.navigate("caregiver_settings")
                        }
                    )
                }
            }
        }
        
        composable("qr_scanner") {
            val viewModel: CaregiverPatientsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = CaregiverPatientsViewModelFactory(repository)
            )
            
            QRScannerScreen(
                onQRScanned = { qrData ->
                    viewModel.importPatientFromQR(qrData)
                    navController.popBackStack()
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable("patient_monitor/{pin}") { backStackEntry ->
            val pin = backStackEntry.arguments?.getString("pin") ?: ""
            // Reuse CaretakerScreen for monitoring specific patient
            CaretakerScreen(
                viewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = CaretakerViewModelFactory(repository, pin)
                ),
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable("caregiver_settings") {
            val viewModel: CaregiverSettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = CaregiverSettingsViewModelFactory(repository)
            )
            
            CaregiverSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onAddPatient = {
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                    
                    if (hasPermission) {
                        navController.navigate("qr_scanner")
                    } else {
                        showCameraPermissionDialog = true
                    }
                }
            )
        }
    }
    
    // Camera Permission Rationale Dialog
    if (showCameraPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showCameraPermissionDialog = false },
            title = { Text("Camera Permission Required") },
            text = { Text("We need camera access to scan QR codes and add patients. This helps you quickly connect with your patients.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCameraPermissionDialog = false
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                ) {
                    Text("Allow")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCameraPermissionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Camera Permission Denied Dialog
    if (showCameraPermissionDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showCameraPermissionDeniedDialog = false },
            title = { Text("Camera Access Denied") },
            text = { Text("Without camera access, you cannot scan QR codes. You can enable it later in Settings or manually enter the patient's PIN.") },
            confirmButton = {
                TextButton(onClick = { showCameraPermissionDeniedDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

