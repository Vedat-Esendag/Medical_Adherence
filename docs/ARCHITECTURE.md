1# Medical Adherence App - Architecture Summary

## Quick Reference Guide

### Technology Stack
- **Language**: Kotlin 2.0.21
- **UI Framework**: Jetpack Compose with Material 3
- **Architecture**: MVVM (Model-View-ViewModel)
- **State Management**: StateFlow + Coroutines
- **Backend**: Firebase Firestore + Cloud Messaging
- **Navigation**: Jetpack Navigation Compose
- **Background Tasks**: WorkManager
- **QR Codes**: ZXing library

### Package Organization

```
com.example.medicaladherence/
├── data/                          Data Layer
│   ├── model/                     Domain models (Medication, DoseEvent, etc.)
│   ├── repo/                      Repository interface & singleton provider
│   ├── repository/                Firestore implementation
│   └── firebase/                  Firebase integration & auth
├── viewmodel/                     State management (MVVM)
├── ui/                            UI Layer (Jetpack Compose)
│   ├── screens/                   Screen composables
│   ├── components/                Reusable UI components
│   ├── theme/                     Material 3 theming
│   └── nav/                       Navigation routes
├── fcm/                           Firebase Cloud Messaging
├── notification/                  Notification scheduling (WorkManager)
├── util/                          Utility functions (QR codes)
└── utils/                         Constants & helpers
```

---

## Core Patterns

### 1. MVVM Architecture

```
Jetpack Compose UI
        ↓ collectAsState()
    ViewModel (StateFlow)
        ↓ businessLogic()
    Repository
        ↓
    Firestore
```

**Data Flow:**
1. UI collects StateFlow from ViewModel
2. User interacts with UI
3. ViewModel launches coroutine to call repository
4. Repository updates Firestore
5. Firestore listener updates ViewModel state
6. UI recomposes with new state

### 2. Single Activity Pattern

- **MainActivity**: Only activity in app
- Hosts **Jetpack Compose** UI tree
- Uses **NavHost** for screen navigation
- State preserved through configuration changes

### 3. Repository Pattern

**FirebaseMedicationRepository** provides:
- User authentication & profile management
- Medication CRUD operations
- Dose event tracking
- Adherence calculations
- Patient management (for caregivers)
- QR-based data exchange

### 4. Singleton Pattern

**RepositoryProvider** ensures:
- Single repository instance app-wide
- Thread-safe initialization
- All ViewModels share same data source
- Consistent state across app

### 5. State Management with StateFlow

```kotlin
// ViewModel
private val _uiState = MutableStateFlow<UiState>()
val uiState: StateFlow<UiState> = _uiState.asStateFlow()

// Screen
val uiState by viewModel.uiState.collectAsState()

// Automatic recomposition on state change
```

---

## Key Components Overview

### Data Layer

#### Models
- **Medication**: Name, dosage, times, frequency, notes
- **DoseEvent**: Which medication, when, taken/missed status
- **PatientProfile**: PIN, name, contact info, medication count
- **FirestoreUserProfile**: User role (patient/caregiver), name, PIN, FCM token

#### Repository
```kotlin
FirebaseMedicationRepository {
    // User Profile
    suspend fun setUserProfile(role, name, pin)
    suspend fun getCurrentUserProfile(): FirestoreUserProfile?
    
    // Medications
    suspend fun addOrUpdateMedication(Medication)
    suspend fun deleteMedication(id)
    val medications: Flow<List<Medication>>
    
    // Dose Events
    suspend fun markDose(medId, date, time, taken)
    suspend fun getTodayDoses(): List<Triple<Medication, time, taken?>>
    
    // Statistics
    suspend fun calculateWeeklyAdherence(): Int
    suspend fun calculateStreak(): Int
    suspend fun getDailyAdherenceForWeek(): Map<LocalDate, Int>
    
    // Caregiver
    fun getCaregiverPatients(): Flow<List<PatientProfile>>
    fun getMedicationsForPatientByPin(pin): Flow<List<Medication>>
    
    // QR Exchange
    suspend fun exportPatientData(pin, name): PatientDataExport
    suspend fun importPatientData(PatientDataExport)
}
```

### ViewModel Layer

#### Key ViewModels

| ViewModel | Responsibility |
|-----------|-----------------|
| **HomeViewModel** | Today's doses, countdown timer, quick actions |
| **StatsViewModel** | Weekly/monthly adherence, insights, trends |
| **AddMedicationViewModel** | Form state, validation, save medication |
| **SettingsViewModel** | Font scale, theme, user profile, caregiver PIN |
| **CaretakerViewModel** | Monitor patient, send notifications, trend analysis |
| **CaregiverPatientsViewModel** | Patient list, QR import, PIN-based import |

#### State Management Pattern

```kotlin
data class [Screen]UiState(
    val data: Type = defaultValue,
    val isLoading: Boolean = false,
    val error: String? = null,
    val snackbarMessage: String? = null
)

class [Screen]ViewModel {
    private val _uiState = MutableStateFlow([Screen]UiState())
    val uiState = _uiState.asStateFlow()
    
    init {
        loadData()
    }
    
    private fun loadData() {
        viewModelScope.launch {
            val result = repository.getData()
            _uiState.value = _uiState.value.copy(data = result)
        }
    }
}
```

### UI Layer

#### Screen Composition

```
Screen()
├── Scaffold(
│   ├── topBar = TopAppBar()
│   ├── bottomBar = NavigationBar() // patients only
│   ├── floatingActionButton = FAB() // where applicable
│   └── content = {
│       LazyColumn {
│           items(data) { item ->
│               Card { Item() }
│           }
│       }
│   }
│)
```

#### Components
- **DoseCard**: Medication with action buttons
- **PatientCard**: Patient info with quick actions
- **StatBar**: Adherence percentage visualization
- **PinDialog**: PIN entry/display

---

## Firebase Integration Architecture

### Authentication Flow

```
App Start
    ↓
FirebaseAuthManager.ensureAuthenticated()
    ↓
Try: signInAnonymously()
    ├─ Success → Get Firebase UID
    │
    └─ Failure (offline) → Use device-specific ID
                          (Android ID based)
    ↓
Request FCM Token
    ↓
Save token to Firestore users/{userId}
```

### Firestore Document Structure

```
users/ {collection}
├── {userId} {document}
│   ├── role: "patient" | "caregiver"
│   ├── name: String
│   ├── pin: String (6-digit)
│   ├── fcmToken: String
│   ├── createdAt: Timestamp
│   │
│   ├── medications/ {subcollection}
│   │   ├── {medId} → FirestoreMedication
│   │   │   ├── name, dosage, times[], notes
│   │   │   ├── frequency, specificDays[]
│   │   │   └── ...
│   │   └── ...
│   │
│   ├── doseEvents/ {subcollection}
│   │   ├── {medId}_{date}_{time} → FirestoreDoseEvent
│   │   │   ├── medId, date, time, taken
│   │   │   └── ...
│   │   └── ...
│   │
│   └── settings/ {subcollection}
│       └── app_settings → FirestoreSettings
│           ├── fontScale, highContrastMode
│           ├── alertThreshold, dailySummaryEnabled
│           └── ...
│
└── ...

caregiver_links/ {collection}
├── {linkId} {document}
│   ├── caregiverUserId, patientUserId
│   ├── patientPin, patientName
│   ├── addedAt: Timestamp
│   ├── displayName, phoneNumber, notes
│   └── ...
└── ...
```

### Real-time Listeners

```kotlin
// Flow-based listeners
val medications: Flow<List<Medication>> = callbackFlow {
    val listener = firestore.collection("users/$userId/medications")
        .addSnapshotListener { snapshot, error ->
            val meds = snapshot?.documents
                ?.mapNotNull { it.toObject(FirestoreMedication::class.java) }
                ?.map { it.toMedication() }
            trySend(meds ?: emptyList())
        }
    
    awaitClose { listener.remove() }
}.catch { emit(emptyList()) }
```

### FCM Integration

```
Caregiver App
    ↓ sendNotification()
FirebaseMessaging API / Cloud Functions / Local Server
    ↓ HTTP POST to FCM
Firebase Cloud Messaging
    ↓ Push notification
Patient Device
    ↓
MyFirebaseMessagingService.onMessageReceived()
    ↓
showNotification() → System Notification
```

---

## Key Features & Their Implementation

### 1. Dose Tracking

**Components:**
- `HomeScreen` - UI
- `HomeViewModel` - State & logic
- `FirebaseMedicationRepository.markDose()` - Persistence

**Flow:**
1. User taps "Mark Taken" on dose card
2. HomeViewModel calls `repository.markDose()`
3. Repository writes to Firestore `users/{id}/doseEvents/{id}_{date}_{time}`
4. Firestore listener updates medications flow
5. ViewModel state updates, UI recomposes

### 2. Adherence Calculation

**Methods in Repository:**
```kotlin
suspend fun calculateWeeklyAdherence(): Int
suspend fun calculateMonthlyAdherence(): Int
suspend fun getDailyAdherenceForWeek(): Map<LocalDate, Int>
suspend fun calculateMedicationAdherence(medId, startDate, endDate): MedicationAdherence
```

**Logic:**
1. Query doseEvents for date range
2. Count taken vs. total events
3. Calculate percentage
4. Return to ViewModel for display

### 3. Countdown Timer

**Implementation in HomeViewModel:**
```kotlin
private fun startCountdownTimer() {
    viewModelScope.launch {
        while (true) {
            updateCountdown() // Calculate next dose time
            delay(1000)       // Update every second
        }
    }
}
```

### 4. Patient Monitoring (Caregiver)

**Components:**
- `CaretakerScreen` - UI
- `CaretakerViewModel` - Logic
- `FirebaseMedicationRepository.getMedicationsForPatientByPin()` - Real-time flow
- `FirebaseMedicationRepository.getDoseEventsForPatientByPin()` - Patient's dose history

**Features:**
- Real-time patient medications via Firestore listener
- Weekly/monthly adherence calculations
- Problematic medication identification
- Adherence trend analysis
- Recent missed doses list

### 5. QR Code Exchange

**Patient Side (Export):**
1. User opens "Share Profile" option
2. App creates `PatientDataExport` with:
   - PIN, name
   - All medications
   - Last 30 days of dose events
3. Serialize to JSON
4. Encode to QR code (512x512)

**Caregiver Side (Import):**
1. Scan QR code with `QRScannerScreen`
2. Parse QR data using `QRCodeScanner.parseQRData()`
3. Extract `PatientDataExport`
4. Check for duplicates
5. Call `repository.importPatientData()`
6. Create `caregiver_link` document

---

## State Flow Example

### Adding a Medication

```
AddEditMedicationScreen
    ↓ user enters data
AddMedicationViewModel
    ├─ updateName(name) → validate()
    ├─ updateDosage(dosage) → validate()
    ├─ addTime(time) → validate()
    └─ saveMedication()
        ↓
        repository.addOrUpdateMedication(Medication)
        ↓
        Firestore: users/{id}/medications/{medId} ← writes
        ↓
        Firestore Listener (on medications flow)
        ↓
        HomeViewModel receives updated medications
        ↓
        HomeScreen recomposes with new medication
```

---

## Notification Architecture

### WorkManager (Local Reminders)

**Setup in NotificationScheduler:**
```kotlin
fun scheduleMedicationNotifications(medication: Medication) {
    medication.times.forEach { time ->
        // Calculate delay until time
        val delay = calculateDelayUntilTime(time)
        
        // One-time work for today
        val workRequest = OneTimeWorkRequestBuilder<MedicationReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()
        
        WorkManager.getInstance(context).enqueue(workRequest)
        
        // Recurring daily work (for future days)
        val periodicWorkRequest = PeriodicWorkRequestBuilder<MedicationReminderWorker>(
            24, TimeUnit.HOURS,
            15, TimeUnit.MINUTES
        )
            .setInitialDelay(dailyDelay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()
        
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(tag, REPLACE, periodicWorkRequest)
    }
}
```

### Firebase Cloud Messaging (Push from Caregiver)

**Multiple Implementation Options:**

1. **Local Server** (Development)
   - Express.js server on local machine
   - Caregiver sends to local server
   - Server sends to FCM HTTP API

2. **Cloud Functions** (Production)
   - Trigger on Firestore write
   - Send to FCM with admin SDK
   - Requires Blaze plan

3. **Direct HTTP API** (Deprecated)
   - Direct HTTP POST to FCM
   - Works on Spark plan
   - Less reliable

---

## Best Practices Implemented

### 1. Lifecycle-Aware Coroutines
```kotlin
viewModelScope.launch {
    // Cancelled when ViewModel destroyed
    val data = repository.getData()
    _uiState.value = _uiState.value.copy(data = data)
}
```

### 2. Type-Safe State
```kotlin
data class UiState(val value: T = default)
val stateFlow: StateFlow<UiState>
// Type-checked at compile time
```

### 3. Null Safety
```kotlin
val medication: Medication?
medication?.name  // Safe navigation
medication?.let { m -> /* use m */ }  // Scope function
```

### 4. Resource Lifecycle
```kotlin
override fun onCleared() {
    super.onCleared()
    // Clean up resources
}
```

### 5. Error Handling
```kotlin
try {
    val result = repository.getData()
} catch (e: Exception) {
    _uiState.value = _uiState.value.copy(error = e.message)
}
```

### 6. Offline-First Design
- Firestore caches data locally
- App works offline
- Changes sync when reconnected
- WorkManager schedules independently of network

---

## Navigation Structure

```
MainActivity
    ├─ ProfileSelectionScreen
    │
    ├─ PatientMainScreen
    │   └─ NavHost(startDestination = HOME)
    │       ├─ HOME → HomeScreen
    │       ├─ MEDICATIONS → MedicationsLibraryScreen
    │       ├─ ADD_MEDICATION → AddEditMedicationScreen
    │       ├─ STATS → StatsScreen
    │       └─ SETTINGS → SettingsScreen
    │
    └─ CaregiverMainScreen
        └─ NavHost(startDestination = CAREGIVER_PATIENTS)
            ├─ CAREGIVER_PATIENTS → CaregiverPatientsScreen
            ├─ QR_SCANNER → QRScannerScreen
            ├─ PATIENT_MONITOR/{pin} → CaretakerScreen
            └─ CAREGIVER_SETTINGS → CaregiverSettingsScreen
```

---

## Summary Statistics

- **Total Kotlin Files**: 40+
- **Total Lines of Code**: 4000+
- **ViewModels**: 8
- **Screens**: 10
- **Data Models**: 10+
- **Firebase Collections**: 3 (users, caregiver_links, settings)
- **Permissions**: 8
- **Dependencies**: 20+ major libraries

---

## Quick Start for Developers

### New Screen Checklist
- [ ] Create `{Screen}Screen.kt` in `ui/screens/`
- [ ] Create `{Screen}ViewModel.kt` in `viewmodel/`
- [ ] Create `{Screen}ViewModelFactory.kt` in `viewmodel/`
- [ ] Define `{Screen}UiState` data class
- [ ] Add screen to `MainActivity.kt` NavHost
- [ ] Add route to `Routes.kt`
- [ ] Create repository methods if needed
- [ ] Add Material 3 styling
- [ ] Test on emulator (API 29+)

### New Feature Checklist
- [ ] Add domain model in `data/model/`
- [ ] Add Firestore model in `data/firebase/`
- [ ] Add repository methods in `FirebaseMedicationRepository`
- [ ] Add ViewModel state and methods
- [ ] Create UI screens
- [ ] Add unit tests
- [ ] Add Firestore security rules (if needed)
- [ ] Test offline scenarios

