# Architecture Overview

## TL;DR
Modern Android app using **MVVM architecture** with Jetpack Compose, Firebase Firestore, Kotlin Coroutines, and StateFlow for reactive UI. Single Activity with composable screens. Supports dual roles (patient and caregiver) with real-time cloud sync.

## Architecture Pattern: MVVM

### Model-View-ViewModel
The app follows strict MVVM separation:

**Model**
- Data classes: `Medication`, `DoseEvent`, `PatientProfile`
- Firestore DTOs: `FirestoreMedication`, `FirestoreDoseEvent`, `FirestoreUserProfile`
- Repository: `FirebaseMedicationRepository`
- Location: `app/src/main/java/com/example/medicaladherence/data/`

**View**
- Composable screens: `HomeScreen`, `StatsScreen`, etc.
- Reusable components: `DoseCard`
- Location: `app/src/main/java/com/example/medicaladherence/ui/`

**ViewModel**
- State management: `HomeViewModel`, `StatsViewModel`, etc.
- UI state classes: `HomeUiState`, `StatsUiState`
- Location: `app/src/main/java/com/example/medicaladherence/viewmodel/`

## Project Structure

```
app/src/main/java/com/example/medicaladherence/
├── MainActivity.kt                         # Single activity + nav graph
│
├── data/
│   ├── model/                              # Data classes
│   │   ├── Medication.kt                   # Core medication model
│   │   ├── DoseEvent.kt                    # Dose tracking events
│   │   ├── PatientProfile.kt               # User profile model
│   │   └── PatientDataExport.kt            # QR code data structure
│   │
│   ├── firebase/                           # Firebase integration
│   │   ├── FirebaseAuthManager.kt          # Authentication & offline IDs
│   │   ├── FirestoreModels.kt              # Firestore DTOs
│   │   └── FirestoreExtensions.kt          # Helper extensions
│   │
│   ├── repository/                         # Repository layer
│   │   ├── FirebaseMedicationRepository.kt # Single source of truth
│   │   └── RepositoryProvider.kt           # Singleton provider
│   │
│   └── SeedData.kt                         # Sample data for testing
│
├── fcm/                                    # Firebase Cloud Messaging
│   └── MyFirebaseMessagingService.kt       # Push notifications
│
├── notification/                           # Local notifications
│   └── MedicationReminderWorker.kt         # WorkManager for reminders
│
├── viewmodel/                              # ViewModels + UI state
│   ├── HomeViewModel.kt
│   ├── StatsViewModel.kt
│   ├── MedicationsLibraryViewModel.kt
│   ├── AddMedicationViewModel.kt
│   ├── SettingsViewModel.kt
│   ├── ProfileSelectionViewModel.kt        # Choose patient/caregiver role
│   ├── CaregiverPatientsViewModel.kt       # Caregiver patient list
│   └── CaretakerViewModel.kt               # Patient monitoring dashboard
│
├── ui/
│   ├── screens/                            # Full-screen composables
│   │   ├── ProfileSelectionScreen.kt       # Role selection
│   │   ├── HomeScreen.kt                   # Patient home
│   │   ├── StatsScreen.kt
│   │   ├── MedicationsLibraryScreen.kt
│   │   ├── AddEditMedicationScreen.kt
│   │   ├── SettingsScreen.kt
│   │   ├── CaregiverPatientsScreen.kt      # Caregiver patient list
│   │   ├── CaretakerScreen.kt              # Patient monitoring view
│   │   └── QRScannerScreen.kt              # QR code scanning
│   │
│   ├── components/                         # Reusable composables
│   │   ├── DoseCard.kt
│   │   ├── PatientQRDisplayDialog.kt       # Show QR to caregiver
│   │   └── SetPinDialog.kt                 # PIN entry dialogs
│   │
│   └── theme/                              # Material 3 theming
│       ├── Color.kt
│       ├── Type.kt
│       └── Theme.kt                        # Includes high contrast mode
│
└── utils/
    ├── AppConstants.kt                     # App-wide constants
    ├── QRCodeGenerator.kt                  # QR code generation
    ├── QRCodeScanner.kt                    # QR code scanning
    ├── FCMHelper.kt                        # FCM notification helpers
    └── LocalFCMHelper.kt                   # Local server FCM (dev)
```

## Single Activity Architecture

### MainActivity
- Single `ComponentActivity` (`MainActivity.kt:24`)
- Hosts `NavHost` with all screen destinations
- Bottom navigation bar integrated
- Manages app-level font scale state

### Navigation
- Jetpack Compose Navigation
- Dual navigation flows: Patient vs Caregiver
- Routes defined in `Routes` object
- Bottom nav shows on: Home, Medications, Stats, Settings (patient) or patient list (caregiver)
- Hidden on: Add/Edit screens, profile selection

### Routes
```kotlin
// Initial setup
PROFILE_SELECTION = "profile_selection"

// Patient routes
HOME = "home"
MEDICATIONS = "medications"
ADD_MEDICATION = "add_medication"
EDIT_MEDICATION = "add_medication?id={medId}"
STATS = "stats"
SETTINGS = "settings"

// Caregiver routes
CAREGIVER_PATIENTS = "caregiver_patients"
CAREGIVER_PATIENT_DETAIL = "caregiver_patient_detail/{patientId}"
QR_SCANNER = "qr_scanner"
```

### Dual Role Architecture
The app adapts its navigation based on user role:

**Patient Flow:**
```
ProfileSelection → Home → (Medications/Stats/Settings)
```

**Caregiver Flow:**
```
ProfileSelection → CaregiverPatients → (Scan QR / Add PIN) → PatientDetail
```

## Data Flow

### Unidirectional Data Flow
```
User Action
    ↓
ViewModel (receives event)
    ↓
Repository (business logic)
    ↓
Firebase Firestore (cloud persistence + offline cache)
    ↓
Firestore Snapshot Listener (real-time updates)
    ↓
Repository Flow<T> (emits changes)
    ↓
ViewModel StateFlow (updates UI state)
    ↓
Composable (recomposes with new state)
```

### Example: Marking Dose as Taken
1. User taps "Taken" button in `DoseCard`
2. `onTaken()` callback invoked
3. `HomeViewModel.markTaken()` called
4. `repository.markDoseTaken()` writes to Firestore
5. Firestore snapshot listener detects change
6. Repository emits updated data via Flow
7. ViewModel updates `StateFlow<HomeUiState>`
8. HomeScreen recomposes with new data (< 2 seconds)

### Offline-First Design
- Firestore caches data locally for offline access
- Writes queue locally when offline
- Automatic sync when connection restored
- Real-time listeners work with cached data
- Optimistic UI updates provide immediate feedback

## Reactive UI with StateFlow

### Pattern
Every ViewModel exposes UI state via `StateFlow`:
```kotlin
private val _uiState = MutableStateFlow(HomeUiState())
val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
```

Screens collect state and recompose:
```kotlin
val uiState by viewModel.uiState.collectAsState()
```

### Benefits
- Automatic UI updates when state changes
- Lifecycle-aware (stops collecting when not visible)
- Thread-safe state management
- Testable ViewModel logic

## Dependency Injection

### Manual DI with Provider
`RepositoryProvider.kt` provides singleton repository:
```kotlin
object RepositoryProvider {
    fun provideRepository(context: Context): FirebaseMedicationRepository
    fun getRepository(): FirebaseMedicationRepository
    fun getAuthManager(): FirebaseAuthManager
}
```

ViewModels receive repository via constructor:
```kotlin
class HomeViewModel(
    private val repository: FirebaseMedicationRepository = RepositoryProvider.getRepository()
)
```

### Why Not Hilt/Dagger?
- Simple app with single repository
- Reduces complexity for prototype
- Easy to migrate to Hilt later if needed

## Layers & Responsibilities

### Presentation Layer (UI + ViewModel)
- **Screens**: Render UI, collect state
- **ViewModels**: Manage UI state, handle user events
- **No business logic in screens**: ViewModels orchestrate

### Domain Layer (Models)
- Data classes: `Medication`, `DoseEvent`
- Enums: `MedicationFrequency`
- Pure Kotlin, no Android dependencies

### Data Layer (Repository + Firebase)
- **Repository**: Single source of truth, exposes Flows from Firestore listeners
- **Firebase Firestore**: Cloud persistence with offline caching
- **FirebaseAuthManager**: Handles authentication and offline user IDs
- **Firestore DTOs**: Data transfer objects for serialization

## Key Architectural Decisions

### Why MVVM?
- Recommended by Google for Android
- Clear separation of concerns
- Testable business logic
- Lifecycle-aware components

### Why Single Activity?
- Smoother navigation transitions
- Shared state easier to manage
- Compose works best with single activity
- Material 3 bottom nav integration

### Why Firebase/Firestore?
- Real-time cloud sync across devices
- Built-in offline persistence and caching
- No backend infrastructure to maintain
- Automatic conflict resolution
- Scalable for caregiver-patient data sharing
- Real-time listeners for reactive updates

### Why StateFlow over LiveData?
- Better Kotlin Coroutines integration
- More predictable behavior
- Compose's `collectAsState()` works seamlessly
- Simpler testing

## Threading Model

### Coroutines
All async work uses Kotlin Coroutines:
- `viewModelScope` for ViewModel operations
- Automatic cancellation when ViewModel cleared
- Structured concurrency

### Dispatchers
- **Main**: UI updates (StateFlow emissions), Firestore listeners
- **IO**: Firestore write operations, HTTP requests (FCM)
- **Default**: Heavy computations (QR code generation)

## Testing Strategy

### Unit Tests
- ViewModels: Test state transformations
- Repository: Test data operations
- Use `runTest` for coroutines

### UI Tests
- Compose UI Test: Test screens in isolation
- Verify button clicks, state changes
- Screenshot tests for regression

## Future Architectural Improvements

### Potential Enhancements
- Hilt for dependency injection
- Use cases layer for complex business logic
- Separate domain models from Firestore DTOs
- Enhanced error handling and retry logic for offline scenarios
- More comprehensive security rules for Firestore
- Migration from anonymous auth to social login (optional)
