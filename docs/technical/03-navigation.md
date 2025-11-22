# Navigation Architecture

## TL;DR
Single Activity with Jetpack Compose Navigation supporting dual-role flows (Patient + Caregiver). Separate NavHost for each role with role-specific bottom navigation. Type-safe route constants.

## Navigation Setup

### Role-Based Navigation
**Location**: `MainActivity.kt`

```kotlin
// In MainActivity - switches based on user role
if (userProfile?.role == "patient") {
    PatientMainScreen(repository, highContrastMode, onHighContrastChanged)
} else if (userProfile?.role == "caregiver") {
    CaregiverMainScreen(repository)
} else {
    // Show profile selection screen
    ProfileSelectionScreen(...)
}
```

### Patient NavHost Configuration
**Location**: `MainActivity.kt` - `PatientMainScreen` composable

```kotlin
NavHost(
    navController = navController,
    startDestination = Routes.HOME,
    modifier = Modifier.padding(innerPadding)
) {
    composable(Routes.HOME) { HomeScreen(...) }
    composable(Routes.MEDICATIONS) { MedicationsLibraryScreen(...) }
    composable(Routes.STATS) { StatsScreen(...) }
    composable(Routes.SETTINGS) { SettingsScreen(...) }
    composable(
        route = "add_medication?id={medId}",
        arguments = listOf(navArgument("medId") { 
            type = NavType.StringType
            nullable = true
        })
    ) { 
        AddEditMedicationScreen(...) 
    }
}
```

### Caregiver NavHost Configuration
**Location**: `MainActivity.kt` - `CaregiverMainScreen` composable

```kotlin
NavHost(
    navController = navController,
    startDestination = "caregiver_patients"
) {
    composable("caregiver_patients") {
        CaregiverPatientsScreen(
            onPatientClick = { patientId ->
                navController.navigate("caregiver_patient_detail/$patientId")
            },
            onAddPatientClick = { /* Show add method dialog */ }
        )
    }
    
    composable("qr_scanner") {
        QRScannerScreen(
            onQRCodeScanned = { qrData -> /* Add patient */ },
            onNavigateBack = { navController.popBackStack() }
        )
    }
    
    composable(
        route = "caregiver_patient_detail/{patientId}",
        arguments = listOf(navArgument("patientId") { 
            type = NavType.StringType 
        })
    ) { backStackEntry ->
        val patientId = backStackEntry.arguments?.getString("patientId")
        CaretakerScreen(
            patientId = patientId,
            onNavigateBack = { navController.popBackStack() }
        )
    }
}
```

## Dual Role Navigation

### Two Separate Navigation Flows

The app has **two completely separate NavHost instances**:
1. **PatientMainScreen** - For patients managing their own medications
2. **CaregiverMainScreen** - For caregivers monitoring patients

**Role determined by**: Firebase user profile `role` field ("patient" or "caregiver")

**Location**: `MainActivity.kt` - switches between PatientMainScreen and CaregiverMainScreen based on role

## Routes

### Route Constants
**Location**: `app/src/main/java/com/example/medicaladherence/ui/nav/NavGraph.kt`

```kotlin
object Routes {
    // Patient Routes
    const val HOME = "home"
    const val MEDICATIONS = "medications"
    const val ADD_MEDICATION = "add_medication"
    const val EDIT_MEDICATION = "add_medication?id={medId}"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    
    // Caregiver Routes
    const val CARETAKER = "caretaker"  // Patient detail view
}

// Additional Caregiver Routes (defined inline in CaregiverMainScreen)
const val CAREGIVER_PATIENTS = "caregiver_patients"      // Patient list
const val QR_SCANNER = "qr_scanner"                      // QR code scanner
const val CAREGIVER_PATIENT_DETAIL = "caregiver_patient_detail/{patientId}"
```

### Patient Routes

**Simple Routes** (no arguments):
- `home` - Today's medication doses with countdown
- `medications` - Full medication library
- `stats` - Weekly adherence statistics
- `settings` - App settings (font size, high contrast, PIN)

**Parameterized Routes**:
- `add_medication?id={medId}` - Add new medication (id=null) or Edit existing (id=medicationId)

### Caregiver Routes

**Simple Routes**:
- `caregiver_patients` - List of all paired patients
- `qr_scanner` - QR code scanner to pair with patient

**Parameterized Routes**:
- `caregiver_patient_detail/{patientId}` - Patient monitoring dashboard with real-time adherence data

## Role-Specific Navigation Flows

### Patient Flow
```
App Launch
    ↓
Check User Profile (Firebase)
    ↓
Role = "patient"
    ↓
PatientMainScreen (NavHost)
    ↓
Bottom Nav: Home | Medications | Stats | Settings
    ↓
User can navigate freely between screens
```

**Key Patient Journeys**:
1. **Track dose**: Home → Mark Taken/Missed → See updated stats
2. **Add medication**: Medications → FAB (+) → Add screen → Save → Back to list
3. **Check progress**: Any screen → Stats tab → View weekly adherence
4. **Share with caregiver**: Settings → Generate PIN/QR → Share with family member

### Caregiver Flow
```
App Launch
    ↓
Check User Profile (Firebase)
    ↓
Role = "caregiver"
    ↓
CaregiverMainScreen (NavHost)
    ↓
Start: Caregiver Patients List
    ↓
FAB (+) → Add Patient Method Dialog
    ├─ Scan QR Code → QR Scanner Screen
    └─ Enter PIN → Manual PIN Dialog
    ↓
Patient Added → Patient List
    ↓
Tap Patient → Patient Detail Dashboard
    ↓
View real-time adherence, send reminders
```

**Key Caregiver Journeys**:
1. **Add patient**: Patient List → FAB → Scan QR or Enter PIN → Patient added
2. **Monitor patient**: Patient List → Tap patient → View dashboard with real-time data
3. **Send reminder**: Patient Detail → Send Reminder button → FCM notification to patient

## Navigation Patterns

### Bottom Navigation

#### Patient Bottom Navigation
**Location**: `MainActivity.kt` - `PatientMainScreen`

Shows on main patient screens:
```kotlin
val routesWithBottomBar = listOf(
    Routes.HOME,
    Routes.MEDICATIONS,
    Routes.STATS,
    Routes.SETTINGS
)
```

Four tabs:
1. **Home** (🏠 house icon) - Today's doses
2. **Medications** (💊 pill icon) - Medication library
3. **Stats** (📊 chart icon) - Adherence statistics
4. **Settings** (⚙️ gear icon) - App settings

**Hidden on**: Add/Edit medication screens

#### Caregiver Bottom Navigation
**Location**: `MainActivity.kt` - `CaregiverMainScreen`

Caregiver flow uses **FAB (Floating Action Button)** instead of bottom nav:
- Main screen: Patient list
- FAB: "+" button to add new patient (QR scan or manual PIN entry)
- No bottom navigation bar in caregiver mode

**Back navigation**: Used to return from patient detail view to patient list

### Back Navigation
Used for:
- Add/Edit medication screen
- Returning from Stats (though also in bottom nav)

**Implementation**: `TopAppBar` with back icon
```kotlin
navigationIcon = {
    IconButton(onClick = onNavigateBack) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
    }
}
```

## Navigation Flow

### User Journeys

**Add Medication**:
```
Home → FAB (+) → AddEditScreen → Save → Home
Medications → FAB (+) → AddEditScreen → Save → Medications
```

**Edit Medication**:
```
Home → DoseCard → ⋮ Menu → Edit → AddEditScreen → Save → Home
Medications → Card → ⋮ Menu → Edit → AddEditScreen → Save → Medications
```

**View Stats**:
```
Any Screen → Bottom Nav (Stats) → StatsScreen
```

## Navigation Arguments

### Optional medicationId
Add/Edit screen accepts optional ID parameter.

**Configuration**: `MainActivity.kt:155-170`
```kotlin
composable(
    route = "add_medication?id={medId}",
    arguments = listOf(
        navArgument("medId") {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        }
    )
) { backStackEntry →
    val medId = backStackEntry.arguments?.getString("medId")
    AddEditMedicationScreen(
        medicationId = medId,
        onNavigateBack = { navController.popBackStack() }
    )
}
```

**Usage**:
- Navigate to `add_medication` → Add mode (medId = null)
- Navigate to `add_medication?id=abc-123` → Edit mode

## NavController Usage

### Passing to Screens
Screens receive navigation callbacks, not NavController directly:

```kotlin
HomeScreen(
    onNavigateToAdd = { navController.navigate(Routes.ADD_MEDICATION) },
    onNavigateToEdit = { medId →
        navController.navigate("add_medication?id=$medId")
    }
)
```

**Benefits**:
- Testable screens (mock callbacks)
- Single responsibility (screens don't know about nav)
- Easier to refactor navigation

### Navigation Options

**Single Top Launch**:
```kotlin
onClick = {
    navController.navigate(Routes.HOME) {
        launchSingleTop = true
    }
}
```
Prevents duplicate instances.

**Pop Up To**:
```kotlin
navController.navigate(Routes.HOME) {
    popUpTo(Routes.HOME) { inclusive = true }
    launchSingleTop = true
}
```
Clears back stack up to destination.

**Simple Pop Back**:
```kotlin
onNavigateBack = { navController.popBackStack() }
```
Returns to previous screen.

## Bottom Bar Visibility

### Conditional Display
Bottom bar hidden on Add/Edit screen.

**Logic**: `MainActivity.kt:44-50`
```kotlin
val currentRoute = navBackStackEntry?.destination?.route
val showBottomBar = currentRoute in routesWithBottomBar
```

**Effect**: Full-screen form when adding/editing medications.

## State Management

### NavController State
```kotlin
val navController = rememberNavController()
val navBackStackEntry by navController.currentBackStackEntryAsState()
val currentRoute = navBackStackEntry?.destination?.route
```

**Use Cases**:
- Highlight current tab in bottom nav
- Show/hide bottom bar
- Conditional UI based on current screen

## Deep Linking

### Not Currently Implemented
Future support could add:
```kotlin
composable(
    route = "home",
    deepLinks = listOf(
        navDeepLink { uriPattern = "medadherence://home" }
    )
) { HomeScreen(...) }
```

## Navigation Testing

### Test Navigation
```kotlin
@Test
fun testNavigationToAddScreen() {
    composeTestRule.setContent {
        val navController = rememberNavController()
        MedicalAdherenceApp(navController)
    }

    composeTestRule.onNodeWithContentDescription("Add Medication").performClick()
    // Assert on AddEditScreen elements
}
```

## Back Stack Management

### Current Behavior
- Bottom nav: Replaces current screen (no back stack build-up)
- FAB → Add screen: Adds to back stack
- Edit → Back: Pops to previous screen

### Preventing Back Stack Issues
Single top launch prevents duplicates:
```kotlin
navController.navigate(Routes.MEDICATIONS) {
    launchSingleTop = true  // Don't duplicate if already on Medications
}
```

## Screen Transitions

### Default Animations
Compose Navigation provides default animations:
- **Enter**: Slide from right, fade in
- **Exit**: Slide to left, fade out
- **Pop Enter**: Slide from left
- **Pop Exit**: Slide to right

### Custom Animations
Not currently implemented, but possible:
```kotlin
composable(
    route = Routes.HOME,
    enterTransition = { slideInHorizontally() },
    exitTransition = { slideOutHorizontally() }
) { HomeScreen(...) }
```

## Route Extensions

### Helper Functions (Not Implemented)
Could add for cleaner code:
```kotlin
object Routes {
    fun editMedication(id: String) = "add_medication?id=$id"
}

// Usage:
navController.navigate(Routes.editMedication(medId))
```

## Navigation Error Handling

### Invalid Routes
Navigation library handles invalid routes gracefully:
- Logs error
- Doesn't crash
- Stays on current screen

### Missing Arguments
Nullable with default:
```kotlin
navArgument("medId") {
    nullable = true
    defaultValue = null
}
```

## Best Practices Used

1. **Type-safe routes**: Constants in `Routes` object
2. **Callback pattern**: Screens receive callbacks, not NavController
3. **Single source**: Navigation graph in MainActivity
4. **Conditional UI**: Bottom bar visibility based on route
5. **Back stack management**: Single top launch where appropriate
6. **Argument validation**: Nullable types with defaults
