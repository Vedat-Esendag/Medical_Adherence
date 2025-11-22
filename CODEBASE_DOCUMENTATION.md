# Medical Adherence Android App - Comprehensive Codebase Documentation

## 1. PROJECT OVERVIEW

The Medical Adherence app is a comprehensive Android application designed to help patients track medication adherence and enable caregivers to monitor patient medication compliance. Built with modern Android architecture using Jetpack Compose, Material 3, and Firebase backend.

**Key Characteristics:**
- Single Activity architecture with Jetpack Compose UI
- MVVM pattern with StateFlow for reactive state management
- Firebase Firestore for cloud data persistence
- Firebase Cloud Messaging (FCM) for push notifications
- WorkManager for scheduled medication reminders
- Anonymous Firebase authentication with device-specific offline support
- Multi-user support (Patient and Caregiver roles)

**Target SDK:** Android 36 (Android 15)
**Min SDK:** Android 29
**Language:** Kotlin 2.0.21

---

## 2. PROJECT STRUCTURE

```
Medical_Adherence/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/medicaladherence/
│   │   │   │   ├── MainActivity.kt                      # Single activity, navigation hub
│   │   │   │   ├── MedicalAdherenceApp.kt              # Custom Application class
│   │   │   │   ├── MedicalAdherenceApplication.kt      # Application initialization
│   │   │   │   │
│   │   │   │   ├── data/                               # Data layer
│   │   │   │   │   ├── model/
│   │   │   │   │   │   ├── Medication.kt               # Medication data class
│   │   │   │   │   │   ├── DoseEvent.kt                # Dose tracking data class
│   │   │   │   │   │   ├── PatientProfile.kt           # Patient profile model
│   │   │   │   │   │   └── PatientDataExport.kt        # Export/import model for QR codes
│   │   │   │   │   ├── repository/
│   │   │   │   │   │   ├── FirebaseMedicationRepository.kt  # Firebase data operations
│   │   │   │   │   │   └── RepositoryProvider.kt           # Singleton pattern provider
│   │   │   │   │   └── firebase/
│   │   │   │   │       ├── FirebaseAuthManager.kt       # Authentication & token management
│   │   │   │   │       ├── FirestoreModels.kt           # Firebase data models
│   │   │   │   │       └── FirestoreExtensions.kt       # Extension functions
│   │   │   │   │
│   │   │   │   ├── viewmodel/                           # ViewModels (MVVM state holders)
│   │   │   │   │   ├── HomeViewModel.kt                 # Patient home screen
│   │   │   │   │   ├── StatsViewModel.kt                # Patient statistics
│   │   │   │   │   ├── AddMedicationViewModel.kt        # Add/edit medication form
│   │   │   │   │   ├── SettingsViewModel.kt             # Patient settings
│   │   │   │   │   ├── CaretakerViewModel.kt            # Patient monitoring (caregiver POV)
│   │   │   │   │   ├── CaregiverPatientsViewModel.kt    # Patient list management
│   │   │   │   │   ├── CaregiverSettingsViewModel.kt    # Caregiver settings
│   │   │   │   │   ├── MedicationsLibraryViewModel.kt   # Medication library
│   │   │   │   │   └── [ViewModel factories]            # Factory classes for each VM
│   │   │   │   │
│   │   │   │   ├── ui/
│   │   │   │   │   ├── screens/                         # Screen composables
│   │   │   │   │   │   ├── HomeScreen.kt                # Patient dashboard
│   │   │   │   │   │   ├── StatsScreen.kt               # Adherence statistics
│   │   │   │   │   │   ├── AddEditMedicationScreen.kt   # Medication form
│   │   │   │   │   │   ├── SettingsScreen.kt            # Patient settings UI
│   │   │   │   │   │   ├── MedicationsLibraryScreen.kt  # All medications list
│   │   │   │   │   │   ├── ProfileSelectionScreen.kt    # Role selection (Patient/Caregiver)
│   │   │   │   │   │   ├── CaretakerScreen.kt           # Patient monitoring UI
│   │   │   │   │   │   ├── CaregiverPatientsScreen.kt   # Caregiver's patient list
│   │   │   │   │   │   ├── CaregiverSettingsScreen.kt   # Caregiver settings
│   │   │   │   │   │   └── QRScannerScreen.kt           # QR code scanner UI
│   │   │   │   │   │
│   │   │   │   │   ├── components/                      # Reusable UI components
│   │   │   │   │   │   ├── DoseCard.kt                  # Medication dose card
│   │   │   │   │   │   ├── PinDialog.kt                 # PIN entry dialog
│   │   │   │   │   │   └── PatientQRDisplayDialog.kt    # QR code display
│   │   │   │   │   │
│   │   │   │   │   ├── theme/
│   │   │   │   │   │   ├── Color.kt                     # Color palette
│   │   │   │   │   │   ├── Theme.kt                     # Material 3 theme
│   │   │   │   │   │   └── Type.kt                      # Typography
│   │   │   │   │   │
│   │   │   │   │   └── nav/
│   │   │   │   │       └── NavGraph.kt                  # Navigation routes
│   │   │   │   │
│   │   │   │   ├── fcm/
│   │   │   │   │   └── MyFirebaseMessagingService.kt    # FCM message handler
│   │   │   │   │
│   │   │   │   ├── notification/
│   │   │   │   │   ├── NotificationScheduler.kt         # WorkManager setup
│   │   │   │   │   └── MedicationReminderWorker.kt      # WorkManager task
│   │   │   │   │
│   │   │   │   └── utils/
│   │   │   │       ├── Constants.kt                     # App-wide constants
│   │   │   │       ├── FCMHelper.kt                     # FCM API helper
│   │   │   │       ├── LocalFCMHelper.kt                # Local FCM server helper
│   │   │   │       ├── QRCodeGenerator.kt               # Generate QR codes
│   │   │   │       └── QRCodeScanner.kt                 # Parse scanned QR codes
│   │   │   │
│   │   │   └── res/                                     # Resources
│   │   │       ├── drawable/
│   │   │       ├── values/
│   │   │       ├── mipmap-*/
│   │   │       └── xml/
│   │   │
│   │   ├── test/                                        # Unit tests
│   │   └── androidTest/                                 # Instrumented tests
│   │
│   ├── build.gradle.kts                                 # App-level Gradle
│   └── AndroidManifest.xml                             # Android manifest
│
├── functions/                                           # Firebase Cloud Functions (Node.js)
│   ├── src/
│   │   └── index.ts                                    # FCM notification trigger
│   ├── package.json
│   └── tsconfig.json
│
├── docs/                                                # Documentation
├── build.gradle.kts                                    # Root Gradle config
├── settings.gradle.kts                                 # Gradle settings
├── firebase.json                                       # Firebase config
├── .firebaserc                                         # Firebase project reference
├── README.md                                           # Main readme
└── FCM_SETUP_GUIDE.md                                 # FCM integration guide

```

---

## 3. KEY FILES AND THEIR PURPOSES

### 3.1 Core Application Files

| File | Purpose |
|------|---------|
| `MainActivity.kt` | Single activity container, handles navigation between Patient and Caregiver modes |
| `MedicalAdherenceApplication.kt` | Custom Application class, initializes repository and seeds data |
| `build.gradle.kts` | Dependencies: Jetpack Compose, Firebase, WorkManager, OkHttp, ZXing (QR codes) |

### 3.2 Data Layer

| Package | Files | Purpose |
|---------|-------|---------|
| `data.model` | `Medication`, `DoseEvent`, `PatientProfile`, `PatientDataExport` | Core domain models |
| `data.repository` | `FirebaseMedicationRepository`, `RepositoryProvider` | Firestore operations (CRUD, adherence calculations, patient management), singleton provider |
| `data.firebase` | `FirebaseAuthManager`, `FirestoreModels`, `FirestoreExtensions` | Firebase integration, authentication, Firestore mappings |

### 3.3 ViewModel Layer (State Management)

| ViewModel | Purpose |
|-----------|---------|
| `HomeViewModel` | Patient dashboard: today's doses, countdown timer, quick actions |
| `StatsViewModel` | Weekly/monthly adherence, daily breakdown, insights |
| `AddMedicationViewModel` | Add/edit medication form validation and save |
| `SettingsViewModel` | Font scale, high contrast mode, user profile, caregiver PIN |
| `CaretakerViewModel` | Caregiver view: monitor patient adherence, trends, missed doses |
| `CaregiverPatientsViewModel` | Manage caregiver's patient list, QR import, PIN entry |
| `CaregiverSettingsViewModel` | Caregiver notification preferences |
| `MedicationsLibraryViewModel` | Browse and manage all medications |

### 3.4 UI Layer (Jetpack Compose)

| Screen | Purpose |
|--------|---------|
| `HomeScreen` | Patient's main dashboard with today's doses and countdown |
| `StatsScreen` | Adherence statistics with charts and insights |
| `AddEditMedicationScreen` | Form to create/edit medications |
| `SettingsScreen` | Patient preferences (font, contrast, account) |
| `MedicationsLibraryScreen` | Browse all medications |
| `ProfileSelectionScreen` | Initial role selection (Patient/Caregiver) |
| `CaretakerScreen` | Caregiver's patient monitoring dashboard |
| `CaregiverPatientsScreen` | Caregiver's list of patients |
| `CaregiverSettingsScreen` | Caregiver preferences |
| `QRScannerScreen` | Scan QR codes to add patients |

### 3.5 Utilities & Firebase Integration

| File | Purpose |
|------|---------|
| `Constants.kt` | Adherence thresholds, PIN config, FCM settings, role constants |
| `FCMHelper.kt` | Send notifications via FCM HTTP API |
| `LocalFCMHelper.kt` | Send notifications via local Express server |
| `QRCodeGenerator.kt` | Generate QR codes for patient data export |
| `QRCodeScanner.kt` | Parse scanned QR codes |

### 3.6 Notifications & Reminders

| File | Purpose |
|------|---------|
| `MyFirebaseMessagingService.kt` | FCM service: receives push notifications, displays system notifications |
| `NotificationScheduler.kt` | WorkManager setup for medication reminders |
| `MedicationReminderWorker.kt` | WorkManager task: scheduled dose reminders |

---

## 4. MAIN FEATURES IMPLEMENTED

### 4.1 Patient Features

#### Medication Management
- **Add Medications**: Form with name, dosage, scheduled times, frequency (Daily/Weekly/As Needed), notes
- **Edit Medications**: Update existing medication details
- **Delete Medications**: Remove medications with cascade deletion of dose events
- **View All Medications**: Browse library of all medications

#### Dose Tracking
- **Today's Doses Dashboard**: Real-time view of scheduled doses with status (taken/missed/pending)
- **Quick Actions**: Mark dose as taken, mark as missed, snooze for 15 minutes
- **Undo Functionality**: Revert last action with snackbar notification
- **Countdown Timer**: Real-time countdown to next scheduled dose (updates every second)
- **Dose Window Detection**: ±30 minutes from scheduled time highlights active dose

#### Statistics & Analytics
- **Weekly Adherence**: Percentage of doses taken in past 7 days
- **Monthly Adherence**: Percentage of doses taken in past 30 days
- **Daily Breakdown**: Bar chart showing adherence for each day of the week
- **Streak Tracking**: Current consecutive days of perfect adherence
- **Longest Streak**: Maximum consecutive days of 80%+ adherence
- **Medication Breakdown**: Per-medication adherence percentages
- **Time of Day Insights**: Morning vs. evening adherence comparison
- **Recent Missed Doses**: List of last 10 missed doses with dates
- **Adherence Trends**: Improving/Declining/Stable classification

#### Accessibility & UX
- **Adjustable Font Size**: Settings with Normal and Large options
- **High Contrast Mode**: Enhanced visibility toggle
- **Material 3 Design**: Modern, familiar UI following Google standards
- **Large Touch Targets**: All interactive elements ≥48dp
- **Calm Color Palette**: Soothing blue theme
- **Friendly Language**: Non-judgmental, encouraging copy

#### Settings & Profile
- **User Profile**: Role, name, 6-digit PIN
- **Font Scaling**: Adjust UI text size
- **Caregiver PIN Display**: Share PIN with caregivers
- **Notifications**: Toggle medication reminders

### 4.2 Caregiver Features

#### Patient Management
- **Add Patients**: Via QR code scan or manual 6-digit PIN entry
- **Patient List**: View all monitored patients with medication count
- **Patient Details**: View individual patient's full adherence data
- **Remove Patients**: Unlink patients from monitoring
- **Edit Patient Info**: Custom display name, phone number, notes

#### Patient Monitoring
- **Real-time Data**: Live medication and adherence updates via Firestore
- **Adherence Dashboard**: Weekly/monthly adherence percentages
- **Today's Doses**: Patient's scheduled doses and current status
- **Medication Breakdown**: Per-medication adherence analysis
- **Trend Analysis**: Improving/declining/stable classification
- **Problematic Medications**: Identify medications with low adherence
- **Recent Missed Doses**: Last 10 missed doses with timestamps
- **Streak Tracking**: Current and longest adherence streaks

#### Communication
- **Send Reminders**: Push notifications to patient via FCM
- **Message Notifications**: FCM messages trigger system notifications
- **Custom Messages**: Caregiver can send reminder messages

### 4.3 Data & Integration Features

#### Firebase Integration
- **Cloud Storage**: All data persists to Firestore
- **Real-time Sync**: Changes sync instantly across devices
- **Offline Support**: App works offline with local caching
- **Anonymous Auth**: Each device gets unique anonymous user ID
- **Device-specific IDs**: Consistent offline IDs based on Android ID

#### QR Code Exchange
- **Generate QR Code**: Patient exports profile + last month of dose data as QR
- **Scan QR Code**: Caregiver scans to instantly add patient
- **Data Exchange**: Includes medications and recent dose events
- **PIN-based Fallback**: Manual PIN entry for same-device testing

#### Notifications
- **FCM Integration**: Cloud Messaging for push notifications
- **WorkManager**: Scheduled local medication reminders
- **Notification Channels**: Separate channel for caregiver messages
- **High Priority**: Notifications set to high priority for delivery
- **Persistent Tasks**: Reminders reschedule after app restart

---

## 5. ARCHITECTURE PATTERNS USED

### 5.1 MVVM (Model-View-ViewModel)

```
Data Layer (Repository)
         ↑
         │
ViewModel (State Holder)
         ↑
         │
UI Layer (Composables)
```

**Implementation:**
- **Models**: `Medication`, `DoseEvent`, `PatientProfile` in `data.model` package
- **Repository**: `FirebaseMedicationRepository` manages all data operations
- **ViewModels**: Each screen has a dedicated ViewModel (HomeViewModel, StatsViewModel, etc.)
  - Inherits from `androidx.lifecycle.ViewModel`
  - Uses `StateFlow` for reactive state
  - Launches coroutines in `viewModelScope` for lifecycle-aware operations
- **UI**: Jetpack Compose screens collect StateFlow and recompose on state changes

### 5.2 Single Activity Architecture

- **MainActivity**: Only activity; hosts Jetpack Compose UI
- **Navigation**: `NavHost` with Compose routes handles screen transitions
- **State Preservation**: Composable navigation maintains state via rememberSaveable

### 5.3 Repository Pattern

**FirebaseMedicationRepository** abstracts Firestore operations:
- **User Profile Operations**: Get/set/delete user profile
- **Medication CRUD**: Add, update, delete medications
- **Dose Event Management**: Record taken/missed doses, undo
- **Statistics Calculations**: Weekly adherence, streaks, trends
- **Patient Management**: Get caregiver patients, update patient info
- **Export/Import**: QR-based patient data exchange

**Key Methods:**
```kotlin
// User Profile
suspend fun getCurrentUserProfile(): FirestoreUserProfile?
suspend fun setUserProfile(role: String, name: String, pin: String)

// Medications
val medications: Flow<List<Medication>>
suspend fun addOrUpdateMedication(medication: Medication)
suspend fun deleteMedication(medId: String)

// Dose Events
suspend fun markDose(medId: String, date: LocalDate, time: String, taken: Boolean)
suspend fun undoDose(medId: String, date: LocalDate, time: String)
suspend fun getTodayDoses(): List<Triple<Medication, String, Boolean?>>

// Statistics
suspend fun calculateWeeklyAdherence(): Int
suspend fun calculateStreak(): Int
suspend fun getDailyAdherenceForWeek(): Map<LocalDate, Int>
suspend fun getRecentMissedDoses(days: Int): List<MissedDoseInfo>

// Caregiver
fun getCaregiverPatients(): Flow<List<PatientProfile>>
fun getMedicationsForPatientByPin(pin: String): Flow<List<Medication>>
suspend fun calculatePatientAdherence(pin: String, start: LocalDate, end: LocalDate): Int

// Data Exchange
suspend fun exportPatientData(pin: String, name: String): PatientDataExport
suspend fun getPatientDataByPin(pin: String): PatientDataExport?
suspend fun importPatientData(data: PatientDataExport)
```

### 5.4 Singleton Pattern

**RepositoryProvider**:
- Single instance of `FirebaseMedicationRepository`
- Thread-safe initialization with `@Volatile` and synchronized block
- All ViewModels access same repository instance
- Ensures consistent state across app

```kotlin
object RepositoryProvider {
    @Volatile private var repository: FirebaseMedicationRepository? = null
    
    fun provideRepository(context: Context): FirebaseMedicationRepository {
        return repository ?: synchronized(this) {
            // Initialize and cache
        }
    }
}
```

### 5.5 State Management with StateFlow

**Reactive State Pattern**:
```kotlin
// ViewModel
private val _uiState = MutableStateFlow(HomeUiState())
val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

// Screen
val uiState by viewModel.uiState.collectAsState()
```

**Benefits:**
- Type-safe state holder
- Observable changes trigger recomposition
- Lifecycle-aware (survives configuration changes)
- Testable state transitions

### 5.6 Factory Pattern for ViewModels

Each ViewModel has corresponding factory:
```kotlin
class HomeViewModelFactory(
    private val repository: FirebaseMedicationRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HomeViewModel(repository) as T
    }
}
```

**Used in Compose:**
```kotlin
val viewModel: HomeViewModel = viewModel(
    factory = HomeViewModelFactory(repository)
)
```

### 5.7 Firestore Document Structure

```
users/
├── {userId}/
│   ├── [user profile fields]
│   ├── medications/
│   │   ├── {medId}/
│   │   │   ├── name
│   │   │   ├── dosage
│   │   │   ├── times[]
│   │   │   ├── notes
│   │   │   ├── frequency
│   │   │   └── specificDays[]
│   ├── doseEvents/
│   │   ├── {medId}_{date}_{time}/
│   │   │   ├── medId
│   │   │   ├── date (ISO string)
│   │   │   ├── time (HH:mm)
│   │   │   └── taken (boolean)
│   └── settings/
│       └── app_settings/
│           ├── fontScale
│           ├── highContrastMode
│           ├── alertThreshold
│           └── dailySummaryEnabled

caregiver_links/
├── {linkId}/
│   ├── caregiverUserId
│   ├── patientUserId
│   ├── patientPin
│   ├── patientName
│   ├── addedAt (Timestamp)
│   ├── displayName (caregiver's custom name)
│   ├── phoneNumber
│   └── notes
```

---

## 6. FIREBASE INTEGRATION POINTS

### 6.1 Authentication (FirebaseAuthManager)

**Anonymous Authentication:**
- Each device signs in anonymously (unique Firebase user ID)
- Falls back to device-specific offline ID if network unavailable
- Uses Android ID for consistent offline identification

```kotlin
suspend fun ensureAuthenticated(): String {
    // Returns Firebase UID or device-specific offline ID
}
```

**FCM Token Management:**
```kotlin
private suspend fun requestFCMToken() {
    // Requests and saves FCM token to Firestore for push notifications
}
```

### 6.2 Firestore Realtime Database

**Read Operations:**
```kotlin
// Real-time listener for user's medications
val medications: Flow<List<Medication>> = callbackFlow {
    val listener = firestore.collection("users/$userId/medications")
        .addSnapshotListener { snapshot, error -> ... }
}

// Get caregiver's patients
fun getCaregiverPatients(): Flow<List<PatientProfile>> = callbackFlow {
    val listener = firestore.collection("caregiver_links")
        .whereEqualTo("caregiverUserId", caregiverUserId)
        .addSnapshotListener { snapshot, error -> ... }
}
```

**Write Operations:**
```kotlin
// Save medication
suspend fun addOrUpdateMedication(medication: Medication) {
    firestore.collection("users/$userId/medications")
        .document(medication.id)
        .set(firestoreMed)
        .await()
}

// Record dose event
suspend fun markDose(medId: String, date: LocalDate, time: String, taken: Boolean) {
    firestore.collection("users/$userId/doseEvents")
        .document("${medId}_${date}_${time}")
        .set(firestoreEvent)
        .await()
}
```

**Query Operations:**
```kotlin
// Get dose events for adherence calculation
private suspend fun getDoseEvents(startDate: LocalDate, endDate: LocalDate): List<DoseEvent> {
    return firestore.collection("users/$userId/doseEvents")
        .whereGreaterThanOrEqualTo("date", startDate.toString())
        .whereLessThanOrEqualTo("date", endDate.toString())
        .get()
        .await()
        .documents
        .mapNotNull { it.toObject(FirestoreDoseEvent::class.java)?.toDoseEvent() }
}
```

### 6.3 Firebase Cloud Messaging (FCM)

**FCM Service (MyFirebaseMessagingService):**
```kotlin
override fun onMessageReceived(message: RemoteMessage) {
    // Handle incoming FCM messages
    message.notification?.let {
        showNotification(title = it.title, body = it.body)
    }
}

override fun onNewToken(token: String) {
    // Save new FCM token to Firestore for this user
    firestore.collection("users").document(userId)
        .update("fcmToken", token)
}
```

**Sending Notifications (Caregiver):**
- Direct HTTP API: `FCMHelper.kt` (deprecated, works on Spark plan)
- Cloud Functions: `functions/index.ts` (Blaze plan required)
- Local Server: `LocalFCMHelper.kt` (development/testing)

Configuration in `Constants.kt`:
```kotlin
const val USE_LOCAL_SERVER = true        // Use local Express server
const val USE_CLOUD_FUNCTIONS = false    // Use Cloud Functions
```

### 6.4 Data Mapping (FirestoreModels.kt)

**Bidirectional Conversion:**
```kotlin
// Domain model ↔ Firestore model
data class Medication(...)  // Domain
data class FirestoreMedication(...) {
    fun toMedication(): Medication = ...
    companion object {
        fun fromMedication(med: Medication): FirestoreMedication = ...
    }
}
```

### 6.5 Offline Persistence

- Firestore SDK enables offline persistence by default
- Reads/writes work with cached data when offline
- Changes sync when connection restored
- WorkManager schedules reminders independently of cloud sync

---

## 7. UI COMPONENTS AND SCREENS

### 7.1 Screen Hierarchy

```
MainActivity (Single Activity)
├── ProfileSelectionScreen (Role: Patient or Caregiver)
│
├── PatientMainScreen (Patient Navigation)
│   ├── HomeScreen (Dashboard with today's doses)
│   ├── MedicationsLibraryScreen (All medications)
│   ├── AddEditMedicationScreen (Medication form)
│   ├── StatsScreen (Adherence statistics)
│   └── SettingsScreen (Preferences)
│
└── CaregiverMainScreen (Caregiver Navigation)
    ├── CaregiverPatientsScreen (Patient list)
    ├── CaretakerScreen (Monitor individual patient)
    ├── CaregiverSettingsScreen (Caregiver preferences)
    └── QRScannerScreen (Add patients)
```

### 7.2 Key Screens Details

#### HomeScreen (Patient)
- **Top Bar**: App title "Medical Adherence"
- **Content**: List of today's doses with status
- **Bottom Navigation**: Home | Medications | Stats | Settings
- **FAB**: Add new medication button
- **Dose Cards**: Each shows medication name, dosage, time, and action buttons
  - Mark Taken ✓
  - Mark Missed ✗
  - Snooze 15 min
- **Snackbar**: Undo functionality for last action
- **Countdown**: Next dose timer updates every second
- **Summary Stats**: Weekly adherence %, current streak

#### StatsScreen (Patient)
- **Weekly Adherence**: Large percentage display
- **Feedback Message**: Personalized encouragement based on adherence
- **Daily Bar Chart**: 7 bars showing adherence for each day
- **Best/Worst Day**: Highlighted statistics
- **Medication Breakdown**: List with adherence % per medication
- **Time of Day Insight**: Morning vs. evening comparison
- **Streak Information**: Current and longest streaks

#### CaretakerScreen (Caregiver)
- **Top Bar**: Patient name, PIN, last sync time
- **Send Reminder Button**: Send FCM notification to patient
- **Weekly/Monthly Adherence**: Large percentage displays
- **Current & Longest Streaks**: Tracking information
- **Today's Doses**: Patient's scheduled doses and status
- **Recent Missed Doses**: Last 10 missed doses
- **Medication Breakdown**: Per-medication adherence
- **Problematic Medications**: Low-adherence medications highlighted
- **Adherence Trend**: Improving/Declining/Stable indicator

#### CaregiverPatientsScreen
- **Floating Action Button**: Add patient
- **Patient Cards**: List of monitored patients with:
  - Name
  - Medication count
  - Last synced time
  - Quick actions: Edit, Remove, Monitor
- **Empty State**: "No patients yet" with add button
- **Snackbar**: Feedback on patient add/remove

#### AddEditMedicationScreen
- **Form Fields**:
  - Name (text input)
  - Dosage (text input)
  - Times (time picker, multiple selections)
  - Frequency (dropdown: Daily, Weekly, As Needed, etc.)
  - Specific Days (checkboxes if Weekly selected)
  - Notes (text area)
- **Validation**: Real-time error display
- **Action Buttons**: Save, Cancel
- **Success**: Navigate back after save

### 7.3 Reusable Components

#### DoseCard
```kotlin
@Composable
fun DoseCard(
    medication: Medication,
    time: String,
    taken: Boolean?,
    onMarkTaken: () -> Unit,
    onMarkMissed: () -> Unit,
    onSnooze: () -> Unit,
    onEdit: () -> Unit
)
```
- Displays medication info
- Status indicator (pending/taken/missed)
- Action buttons

#### PinDialog
- Pin entry/display dialog
- Copy to clipboard functionality
- Share PIN option

#### PatientQRDisplayDialog
- Display QR code for patient data
- Shows patient name and PIN
- Download QR code option

### 7.4 Material 3 Design System

**Color Palette** (`Color.kt`):
- Primary: Blue (#0066CC)
- Secondary: Teal
- Tertiary: Purple
- Background: Light/Dark theme support
- AdherenceColors for adherence visualization:
  - ExcellentBg (green)
  - GoodBg (light green)
  - WarningBg (yellow)
  - FailBg (red)

**Typography** (`Type.kt`):
- Display Large/Medium/Small
- Headline Large/Medium/Small
- Title Large/Medium/Small
- Body Large/Medium/Small
- Label Large/Medium/Small

**Theme Support** (`Theme.kt`):
- Dynamic colors (Android 12+)
- High contrast mode option
- Light/dark theme support
- Font scaling support

---

## 8. DATA MODELS

### 8.1 Domain Models

#### Medication
```kotlin
data class Medication(
    val id: String,
    val name: String,
    val dosage: String,
    val times: List<String>,           // HH:mm format
    val notes: String? = null,
    val frequency: MedicationFrequency = Daily,
    val specificDays: List<Int> = emptyList()
)

enum class MedicationFrequency {
    Daily, SpecificDays, EveryXDays, Weekly, AsNeeded
}
```

#### DoseEvent
```kotlin
data class DoseEvent(
    val medId: String,
    val date: LocalDate,
    val time: String,                  // HH:mm format
    val taken: Boolean
)
```

#### PatientProfile
```kotlin
data class PatientProfile(
    val pin: String,                   // 6-digit identifier
    val name: String,
    val addedAt: Long,
    val lastSyncedAt: Long,
    val medicationCount: Int = 0,
    val displayName: String? = null,   // Caregiver's custom name
    val phoneNumber: String? = null,
    val notes: String? = null
)
```

### 8.2 Firestore Models (FirestoreModels.kt)

#### FirestoreUserProfile
```kotlin
data class FirestoreUserProfile(
    @DocumentId val userId: String = "",
    val role: String = "",             // "patient" or "caregiver"
    val name: String = "",
    val pin: String = "",              // 6-digit PIN
    val fcmToken: String? = null,      // For push notifications
    val createdAt: Timestamp = Timestamp.now()
)
```

#### FirestoreMedication
- Maps to Medication domain model
- Stored under `users/{userId}/medications/{medId}`

#### FirestoreDoseEvent
- Maps to DoseEvent domain model
- Stored under `users/{userId}/doseEvents/{medId}_{date}_{time}`

#### FirestoreCaregiverLink
```kotlin
data class FirestoreCaregiverLink(
    @DocumentId val linkId: String = "",
    val caregiverUserId: String = "",
    val patientUserId: String = "",
    val patientPin: String = "",
    val patientName: String = "",
    val addedAt: Timestamp = Timestamp.now(),
    val displayName: String? = null,   // Custom name by caregiver
    val phoneNumber: String? = null,
    val notes: String? = null
)
```

### 8.3 State Models (UI State)

#### HomeUiState
```kotlin
data class HomeUiState(
    val todayDate: LocalDate = LocalDate.now(),
    val nextDoseCountdown: String = "--:--",
    val nextDoseName: String = "",
    val nextDoseDosage: String = "",
    val nextDoseTime: String = "",
    val todayDoses: List<DoseItem> = emptyList(),
    val weeklyAdherencePercent: Int = 0,
    val streakDays: Int = 0,
    val snackbarMessage: String? = null,
    val isInDoseWindow: Boolean = false,
    val nextDoseMedicationId: String = "",
    val lastMarkedDose: Pair<String, String>? = null
)
```

#### StatsUiState
```kotlin
data class StatsUiState(
    val weeklyPercentage: Int = 0,
    val dailyBars: List<DayBar> = emptyList(),
    val feedbackMessage: String = "",
    val streakDays: Int = 0,
    val bestDay: String? = null,
    val worstDay: String? = null,
    val medicationBreakdown: List<MedicationAdherence> = emptyList(),
    val timeOfDayInsight: String? = null
)
```

#### CaretakerUiState
```kotlin
data class CaretakerUiState(
    val patientName: String = "Patient",
    val patientPin: String = "",
    val medicationCount: Int = 0,
    val weeklyAdherence: Int = 0,
    val monthlyAdherence: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val todayDoses: List<TodayDoseInfo> = emptyList(),
    val recentMissedDoses: List<MissedDoseInfo> = emptyList(),
    val problematicMedications: List<MedicationAdherence> = emptyList(),
    val medicationBreakdown: List<MedicationAdherence> = emptyList(),
    val adherenceTrend: String = "Stable",
    val lastUpdated: Long = 0L,
    val isLoading: Boolean = true,
    val error: String? = null
)
```

### 8.4 Export/Import Models

#### PatientDataExport
```kotlin
data class PatientDataExport(
    val pin: String,
    val name: String,
    val medications: List<MedicationExport> = emptyList(),
    val doseEvents: List<DoseEventExport> = emptyList()
)
```

Used for QR code exchange between patient and caregiver.

---

## 9. VIEWMODELS AND REPOSITORIES

### 9.1 ViewModel Responsibilities

Each ViewModel:
1. **Holds State**: MutableStateFlow for mutable state, exposed as StateFlow
2. **Manages Coroutines**: viewModelScope for lifecycle-aware coroutine launching
3. **Handles User Input**: Methods to respond to UI events
4. **Performs Operations**: Delegates to repository for data operations
5. **Provides Feedback**: Snackbar messages, validation errors

### 9.2 Repository Responsibilities

FirebaseMedicationRepository:
1. **Data Access**: CRUD operations on Firestore
2. **Business Logic**: Adherence calculations, trend analysis
3. **Real-time Sync**: Firestore listeners for reactive updates
4. **Cross-user Queries**: Access patient data by PIN (caregiver use case)
5. **Offline Fallback**: Works with cached Firestore data

### 9.3 Key ViewModel Methods

#### HomeViewModel
```kotlin
fun initialize(context: Context)           // Initialize notification scheduler
fun markDoseTaken(medId: String, time: String, date: LocalDate = LocalDate.now())
fun markDoseMissed(medId: String, time: String, date: LocalDate = LocalDate.now())
fun snooze(medId: String, time: String, minutes: Int = 15)
fun undoLastMarkedDose()
fun clearSnackbar()
```

#### StatsViewModel
```kotlin
// Loaded automatically in init, no public methods
// UI observes uiState: StateFlow<StatsUiState>
```

#### AddMedicationViewModel
```kotlin
fun updateName(name: String)
fun updateDosage(dosage: String)
fun addTime(time: String)
fun removeTime(time: String)
fun updateNotes(notes: String)
fun updateFrequency(frequency: MedicationFrequency)
fun toggleDay(dayOfWeek: Int)
fun saveMedication()
fun loadMedicationIfEdit(medicationId: String?)
```

#### CaretakerViewModel
```kotlin
suspend fun sendNotificationToPatient(
    title: String,
    message: String
)
```

#### CaregiverPatientsViewModel
```kotlin
fun importPatientFromQR(qrData: String)
fun importPatientFromPin(pin: String)
fun removePatient(patient: PatientProfile)
fun resetImportStatus()
```

### 9.4 Firestore Repository Key Methods

```kotlin
// User Management
suspend fun setUserProfile(role: String, name: String, pin: String)
suspend fun getCurrentUserProfile(): FirestoreUserProfile?
suspend fun deleteUserProfile()

// Medication Operations
suspend fun addOrUpdateMedication(medication: Medication)
suspend fun deleteMedication(medId: String)
suspend fun getMedicationById(id: String): Medication?
val medications: Flow<List<Medication>>

// Dose Tracking
suspend fun markDose(medId: String, date: LocalDate, time: String, taken: Boolean)
suspend fun undoDose(medId: String, date: LocalDate, time: String)
suspend fun getTodayDoses(): List<Triple<Medication, String, Boolean?>>

// Statistics
suspend fun calculateWeeklyAdherence(): Int
suspend fun calculateMonthlyAdherence(): Int
suspend fun calculateStreak(): Int
suspend fun calculateLongestStreak(): Int
suspend fun getDailyAdherenceForWeek(): Map<LocalDate, Int>
suspend fun calculateMedicationAdherence(medId: String, startDate: LocalDate, endDate: LocalDate): MedicationAdherence
suspend fun calculateTimeOfDayInsight(startDate: LocalDate, endDate: LocalDate): String?
suspend fun getRecentMissedDoses(days: Int): List<MissedDoseInfo>

// Caregiver Operations
fun getCaregiverPatients(): Flow<List<PatientProfile>>
fun getMedicationsForPatientByPin(pin: String): Flow<List<Medication>>
suspend fun getDoseEventsForPatientByPin(pin: String, startDate: LocalDate, endDate: LocalDate): List<DoseEvent>
suspend fun calculatePatientAdherence(pin: String, start: LocalDate, end: LocalDate): Int
suspend fun removePatientFromCaregiver(patientPin: String)
suspend fun updatePatientInfo(patientPin: String, displayName: String?, phoneNumber: String?, notes: String?)

// Data Exchange (QR Codes)
suspend fun exportPatientData(pin: String, name: String): PatientDataExport
suspend fun getPatientDataByPin(pin: String): PatientDataExport?
suspend fun importPatientData(data: PatientDataExport)

// Settings
fun getSettings(): Flow<FirestoreSettings?>
suspend fun saveSettings(settings: FirestoreSettings)
```

---

## 10. CONSTANTS AND CONFIGURATION

### AppConstants.kt
```kotlin
// Dose Timing
const val DOSE_WINDOW_MINUTES = 30          // ±30 min around scheduled time
const val SNOOZE_DURATION_MINUTES = 15      // Snooze duration

// Adherence Thresholds
const val ADHERENCE_EXCELLENT = 90          // 90%+ adherence
const val ADHERENCE_GOOD = 80               // 80%+ adherence
const val ADHERENCE_FAIR = 75               // 75%+ adherence
const val ADHERENCE_OKAY = 50               // 50%+ adherence
const val ADHERENCE_PROBLEMATIC = 70        // Below this = problematic
const val ADHERENCE_WARNING = 30            // Critical threshold

// Time Periods
const val DAYS_IN_WEEK = 7
const val DAYS_IN_MONTH = 30
const val STREAK_LOOKBACK_DAYS = 90

// PIN Configuration
const val PIN_LENGTH = 6                    // 6-digit PIN (100000-999999)
const val PIN_MIN = 100000
const val PIN_MAX = 999999

// FCM Configuration
const val FCM_CHANNEL_ID = "caregiver_messages"
const val FCM_CHANNEL_NAME = "Caregiver Messages"
const val FCM_NOTIFICATION_ID = 1001

// FCM Sending Approach
const val USE_LOCAL_SERVER = true           // Use local Express server
const val USE_CLOUD_FUNCTIONS = false       // Use Cloud Functions

// User Roles
const val ROLE_PATIENT = "patient"
const val ROLE_CAREGIVER = "caregiver"
```

---

## 11. PERMISSIONS AND MANIFEST CONFIGURATION

### Required Permissions
```xml
<!-- Network -->
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>

<!-- Notifications -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
<uses-permission android:name="android.permission.VIBRATE"/>

<!-- Alarms -->
<uses-permission android:name="android.permission.USE_EXACT_ALARM"/>
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM"/>

<!-- Camera (for QR scanning) -->
<uses-permission android:name="android.permission.CAMERA"/>
```

### Application Services
```xml
<!-- FCM Service -->
<service
    android:name=".fcm.MyFirebaseMessagingService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

---

## 12. KEY DEPENDENCIES

### Jetpack Libraries
- **androidx.core:core-ktx**: 1.15.0
- **androidx.lifecycle:lifecycle-runtime-ktx**: Latest
- **androidx.compose.bom**: 2024.09.00
- **androidx.activity:activity-compose**: Latest
- **androidx.navigation:navigation-compose**: 2.8.5

### Firebase
- **firebase-firestore-ktx**: 24.x (BOM 32.7.0)
- **firebase-auth-ktx**: 22.x
- **firebase-messaging**: 23.4.0

### Utilities
- **kotlin:kotlinx-coroutines-play-services**: 1.7.3
- **androidx.work:work-runtime-ktx**: 2.9.0
- **com.google.zxing:core**: 3.5.2 (QR codes)
- **com.journeyapps:zxing-android-embedded**: 4.3.0
- **com.google.code.gson:gson**: 2.10.1
- **com.squareup.okhttp3:okhttp**: 4.12.0

---

## 13. BUILD AND GRADLE CONFIGURATION

### App-level build.gradle.kts
```gradle
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.medicaladherence"
    compileSdk = 36
    
    defaultConfig {
        applicationId = "com.example.medicaladherence"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    
    kotlinOptions {
        jvmTarget = "11"
    }
    
    buildFeatures {
        compose = true
    }
}
```

---

## 14. TESTING & DEVELOPMENT

### Current Test Setup
- **Unit Tests**: `app/src/test/`
- **Instrumented Tests**: `app/src/androidTest/`
- **Test Runner**: AndroidJUnitRunner
- **Compose Testing**: androidx.compose.ui.test.junit4

### Local Development
- **Firebase Emulator**: Can be configured for local testing
- **Seed Data**: Pre-populated with Maria and Ahmed test patients

### Sample Data
**Maria's Medications:**
- Amlodipine 5mg @ 07:00
- Metoprolol 50mg @ 19:00
- Aspirin 81mg @ 21:00

**Ahmed's Medications:**
- Mesalamine 800mg @ 08:00
- Azathioprine 50mg @ 22:00

**Adherence:** ~80% for past 7 days with randomized events

---

## 15. RECENT CHANGES AND FEATURES

### Latest Commits
1. **FCM Integration**: Added Firebase Cloud Messaging for push notifications
2. **Validation & Refactoring**: Improved adherence logic and validation
3. **Constants Centralization**: Moved configuration to AppConstants.kt
4. **Caregiver Features**: Added patient management and QR code exchange
5. **Notification System**: Implemented notification scheduling and reminders

### In Development
- Cloud Functions for FCM triggering (requires Blaze plan)
- Enhanced trend analysis
- Export adherence reports
- Multi-language support

---

## 16. SUMMARY

The Medical Adherence app is a modern, feature-complete medication tracking platform built on:

**Architecture:**
- Clean MVVM pattern with Jetpack Compose
- Repository pattern with Firestore backend
- Reactive state management with StateFlow
- Single activity with navigation-compose

**Core Capabilities:**
- Patient medication tracking with real-time statistics
- Caregiver patient monitoring and communication
- Cloud synchronization with offline support
- Push notifications via Firebase Cloud Messaging
- QR code-based patient data exchange
- Accessibility features (font scaling, high contrast)

**Firebase Integration:**
- Anonymous authentication with device-specific offline IDs
- Firestore for all data persistence
- Cloud Messaging for push notifications
- Cloud Functions for serverless notification triggering

**Development Quality:**
- Type-safe Kotlin with null safety
- Lifecycle-aware coroutines
- Comprehensive data validation
- Material 3 design system
- Accessible UI with large touch targets

This codebase demonstrates professional Android development practices suitable for a healthcare application with proper authentication, security considerations, and user experience design.

