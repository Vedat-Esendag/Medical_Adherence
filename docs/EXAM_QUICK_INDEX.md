# Exam Quick Reference Index

## 🎯 How to Use During Exam

1. **Question asks about a concept** (e.g., "Explain StateFlow") → **Ctrl+F** to search this file
2. **Find the section** with file:line references
3. **Reference that code** in your written exam answer
4. **Repeat** for every question

**Speed goal: Find any implementation in <10 seconds**

---

## 📚 Course Topics by Week

### Week 36-37: Android Fundamentals
**Topics**: Activities, Lifecycle, Intents, Manifest

**My Implementation**:
- **Single Activity**: `MainActivity.kt:20-100`
  - Only one activity in entire app
  - Hosts all Jetpack Compose UI
  - Navigation handled by Compose Navigation
- **Lifecycle Management**: `MainActivity.kt:onCreate()` - App initialization
- **Intents**: `SettingsScreen.kt` - Share QR code via Intent
- **Manifest**: `AndroidManifest.xml` - Permissions, FCM service registration

**Key Point**: Used Single Activity Architecture with Jetpack Compose - no multiple activities or fragments needed.

---

### Week 38-39: Kotlin Essentials
**Topics**: Coroutines, Data Classes, Sealed Classes, Flow

**My Implementation**:
- **Coroutines**: 
  - `HomeViewModel.kt:80` - `viewModelScope.launch { }`
  - Used in all 8 ViewModels for async operations
  - Pattern: `viewModelScope.launch { val result = repository.getData() }`
  
- **Data Classes**:
  - `Medication.kt:10` - `data class Medication(...)`
  - `DoseEvent.kt:8` - `data class DoseEvent(...)`
  - `PatientProfile.kt:10` - `data class PatientProfile(...)`
  - All models are immutable data classes
  
- **Flow** (replaces LiveData):
  - `FirebaseMedicationRepository.kt:45` - `val medications: Flow<List<Medication>>`
  - `HomeViewModel.kt:60` - `val uiState: StateFlow<HomeUiState>`
  - Pattern: Real-time Firestore listeners → Flow → StateFlow → UI
  
- **Sealed Classes**: *(Check if you used any for state/result types)*

---

### Week 40-41: Jetpack Compose
**Topics**: @Composable, State, LazyColumn, remember, Navigation

**My Implementation**:
- **@Composable Functions**:
  - All screens: `HomeScreen.kt`, `StatsScreen.kt`, `AddEditMedicationScreen.kt`, etc.
  - Components: `DoseCard.kt`, `PinDialog.kt`
  - Total: 10 screens + multiple reusable components
  
- **State Management**:
  - `HomeViewModel.kt:59` - `private val _uiState = MutableStateFlow(HomeUiState())`
  - `HomeViewModel.kt:60` - `val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()`
  - Pattern: ViewModel holds MutableStateFlow, exposes as StateFlow
  
- **State Collection in UI**:
  - `HomeScreen.kt` - `val uiState by viewModel.uiState.collectAsState()`
  - Automatic recomposition when state changes
  
- **remember**:
  - Used for composition-scoped state (survives recomposition only)
  - Example: Dialog states, local UI toggles
  
- **rememberSaveable**:
  - Used for persistent state (survives process death)
  - Example: Form inputs in AddEditMedicationScreen
  
- **LazyColumn**:
  - `HomeScreen.kt` - Today's doses list
  - `MedicationsLibraryScreen.kt` - All medications
  - `StatsScreen.kt` - Medication breakdown
  - **Key Pattern**: `items(list, key = { it.id }) { item -> ... }`
  - Why key param: Prevents incorrect recomposition
  
- **Compose Navigation**:
  - `MainActivity.kt:80-150` - NavHost setup
  - Routes: "home", "medications", "addEdit/{id}", "stats", "settings"
  - Pattern: `navController.navigate("route")`, `navController.popBackStack()`

---

### Week 42-43: Architecture Patterns
**Topics**: MVVM, Repository Pattern, Dependency Injection

**My Implementation**:
- **MVVM Pattern** (Used throughout):
  - **Model**: `data/model/` - Medication, DoseEvent, PatientProfile
  - **View**: `ui/screens/` - All Composable screens
  - **ViewModel**: `viewmodel/` - 8 ViewModels (HomeViewModel, AddMedicationViewModel, etc.)
  - **Flow**: User action → ViewModel → Repository → Firestore → Flow → StateFlow → UI recompose
  
- **ViewModels** (8 total):
  - `HomeViewModel.kt:55` - Home screen state & logic
  - `AddMedicationViewModel.kt` - Form validation & save
  - `StatsViewModel.kt` - Adherence calculations
  - `SettingsViewModel.kt` - User preferences
  - `CaretakerViewModel.kt` - Patient monitoring (caregiver view)
  - `CaregiverPatientsViewModel.kt` - Patient list management
  - `CaregiverSettingsViewModel.kt` - Caregiver preferences
  - `MedicationsLibraryViewModel.kt` - Medication library
  
- **Repository Pattern**:
  - `FirebaseMedicationRepository.kt:27` - Single repository class
  - **Purpose**: Abstract all data operations (Firebase, calculations)
  - **Key methods**:
    - `addOrUpdateMedication()` - CRUD operation
    - `markDose()` - Track dose events
    - `calculateWeeklyAdherence()` - Business logic
    - `getTodayDoses()` - Query + transform data
  - **Why**: Separation of concerns - ViewModels don't know about Firebase
  
- **Singleton Pattern**:
  - `RepositoryProvider.kt:10` - `object RepositoryProvider`
  - Ensures single repository instance app-wide
  - Thread-safe with `@Volatile` + synchronized block
  - Pattern:
    ```kotlin
    object RepositoryProvider {
        @Volatile private var repository: FirebaseMedicationRepository? = null
        fun getRepository(): FirebaseMedicationRepository {
            return repository ?: synchronized(this) { /* init */ }
        }
    }
    ```
  
- **Dependency Injection**:
  - **Manual DI** via RepositoryProvider (no Hilt/Dagger)
  - ViewModels receive repository in constructor
  - **Why no Hilt**: Kept project simple for learning purposes

---

### Week 44-45: Data Persistence & Firebase
**Topics**: Room, Firestore, Real-time Database

**My Implementation**:
- **Firebase Firestore** (Cloud Database):
  - `FirebaseMedicationRepository.kt:27-500` - All Firebase operations
  - **Collections**:
    - `users/{userId}/medications` - User's medications
    - `users/{userId}/doseEvents` - Dose tracking history
    - `users/{userId}/settings` - User preferences
    - `caregiver_links/{linkId}` - Caregiver-patient connections
  
- **Real-time Listeners**:
  - `FirebaseMedicationRepository.kt:45` - Medications Flow
  - Pattern: `callbackFlow { addSnapshotListener { } }`
  - Updates UI instantly when Firestore data changes
  
- **Firestore Writes**:
  - `FirebaseMedicationRepository.kt:150` - `.set()` with `.await()`
  - `FirebaseMedicationRepository.kt:200` - `.update()` with `.await()`
  - All wrapped in `suspend fun` for coroutines
  
- **Firestore Queries**:
  - `.whereEqualTo()`, `.whereGreaterThan()` for filtering
  - `.orderBy()` for sorting
  - Example: Get last 7 days of dose events
  
- **Room Database**: *(If you used it for offline storage)*
  - Document your Room entities, DAOs, Database class
  - Or note: "Not used - Firebase only for cloud-first approach"
  
- **Offline Support**:
  - Firestore SDK caches data locally automatically
  - App works offline, syncs when online
  - WorkManager schedules notifications independently

---

### Week 46-47: Background Tasks & Notifications
**Topics**: WorkManager, AlarmManager, Notifications, FCM

**My Implementation**:
- **WorkManager**:
  - `NotificationScheduler.kt:45` - Schedule medication reminders
  - `MedicationReminderWorker.kt:20` - Worker class executes notification
  - Pattern: `PeriodicWorkRequest` with 24-hour interval
  - Why WorkManager: Survives app kill, battery optimization friendly
  
- **Notifications**:
  - `NotificationScheduler.kt` - Create & show notifications
  - **Channel Setup**: Notification channels for Android 8+
  - **Permission**: POST_NOTIFICATIONS for Android 13+
  - High priority for timely delivery
  
- **Firebase Cloud Messaging (FCM)**:
  - `MyFirebaseMessagingService.kt:20` - Receive push notifications
  - `FirebaseAuthManager.kt:50` - FCM token management
  - `FCMHelper.kt:30` - Send notifications via HTTP API
  - **Use Case**: Caregiver sends reminder to patient
  - **Flow**: Caregiver clicks button → HTTP POST to FCM → Patient receives notification
  - See `FCM_SETUP.md` for complete guide

---

## 🏗️ Architecture Patterns Deep Dive

### MVVM Pattern
**Where**: Every screen implements this

**Components**:
- **Model**: `Medication.kt`, `DoseEvent.kt`, `PatientProfile.kt`
- **View**: `HomeScreen.kt`, `StatsScreen.kt`, etc. (pure Composables)
- **ViewModel**: `HomeViewModel.kt:55`, `AddMedicationViewModel.kt`, etc.
- **Repository**: `FirebaseMedicationRepository.kt:27`

**Data Flow**:
```
User Input (View)
    ↓
ViewModel receives action
    ↓
ViewModel calls Repository
    ↓
Repository updates Firestore
    ↓
Firestore listener triggers
    ↓
Flow emits new data
    ↓
StateFlow updates
    ↓
View recomposes automatically
```

**Key Implementation**:
```kotlin
// ViewModel
class HomeViewModel(private val repository: FirebaseMedicationRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    fun markDoseTaken(medId: String, time: String) {
        viewModelScope.launch {
            repository.markDose(medId, LocalDate.now(), time, taken = true)
            // State updates automatically via Flow listener
        }
    }
}

// View (Composable)
@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    // UI automatically recomposes when uiState changes
}
```

---

### State Management with StateFlow
**Declarations**: `HomeViewModel.kt:59-60`

**Pattern**:
```kotlin
// In ViewModel:
private val _uiState = MutableStateFlow<T>(initialValue)
val uiState: StateFlow<T> = _uiState.asStateFlow()

// Update state:
_uiState.value = _uiState.value.copy(newProperty = newValue)

// In Composable:
val state by viewModel.uiState.collectAsState()
// Automatic recomposition when state changes
```

**Why StateFlow over LiveData**:
- Coroutine-native (better with suspend functions)
- Always has a value (no null)
- Easier to combine/transform with Flow operators
- More predictable behavior

**See Also**: [technical/04-state-management.md](technical/04-state-management.md)

---

### Repository Pattern
**Implementation**: `FirebaseMedicationRepository.kt:27`

**Purpose**: 
- Abstract data sources (Firebase, Room, network)
- Single source of truth for data
- ViewModels don't know about Firebase details
- Business logic (calculations) in one place

**Key Methods**:
```kotlin
class FirebaseMedicationRepository {
    // Real-time data
    val medications: Flow<List<Medication>> = callbackFlow { /* Firestore listener */ }
    
    // Write operations
    suspend fun addOrUpdateMedication(medication: Medication)
    suspend fun markDose(medId: String, date: LocalDate, time: String, taken: Boolean)
    
    // Queries
    suspend fun getTodayDoses(): List<Triple<Medication, String, Boolean?>>
    
    // Business logic
    suspend fun calculateWeeklyAdherence(): Int
    suspend fun calculateStreak(): Int
}
```

**Benefits**:
- Testable (can mock repository)
- Swappable data sources (Firebase → Room)
- Centralized data logic

---

## 🎨 Jetpack Compose UI

### Composable Functions
**All screens**:
- `HomeScreen.kt` - Main dashboard
- `AddEditMedicationScreen.kt` - Form for add/edit medication
- `StatsScreen.kt` - Adherence statistics
- `SettingsScreen.kt` - User preferences
- `MedicationsLibraryScreen.kt` - All medications list
- `ProfileSelectionScreen.kt` - Patient/Caregiver role selection
- `CaretakerScreen.kt` - Caregiver patient monitoring
- `CaregiverPatientsScreen.kt` - Caregiver's patient list
- `CaregiverSettingsScreen.kt` - Caregiver settings
- `QRScannerScreen.kt` - QR code scanner

**Reusable Components**:
- `DoseCard.kt` - Medication dose card with action buttons
- `PinDialog.kt` - PIN entry/display dialog
- `PatientQRDisplayDialog.kt` - QR code display
- `ManualPinEntryDialog.kt` - Manual PIN input
- `AddPatientMethodDialog.kt` - Choose QR or PIN

---

### LazyColumn Pattern
**Implementation**: `HomeScreen.kt`, `MedicationsLibraryScreen.kt`, `StatsScreen.kt`

**Key Pattern**:
```kotlin
LazyColumn {
    items(
        items = medications,
        key = { it.id }  // ← IMPORTANT: Prevents incorrect recomposition
    ) { medication ->
        MedicationCard(medication)
    }
}
```

**Why key parameter**:
- Compose uses key to identify items
- Without key: items can get mixed up during recomposition
- With key: Compose tracks each item correctly
- Improves performance and prevents UI bugs

---

### Scaffold & Material 3
**Used in all screens**:
```kotlin
Scaffold(
    topBar = { TopAppBar(...) },
    bottomBar = { NavigationBar(...) },  // Patient screens only
    floatingActionButton = { FloatingActionButton(...) },  // Where applicable
    content = { paddingValues ->
        // Screen content
    }
)
```

**Material 3 Theme**: `ui/theme/` - Color.kt, Theme.kt, Type.kt

---

### State Hoisting
**Example**: `DoseCard.kt:45`

**Pattern**:
```kotlin
// Stateless component - state hoisted to ViewModel
@Composable
fun DoseCard(
    medication: Medication,
    taken: Boolean?,
    onMarkTaken: () -> Unit,  // ← Callbacks passed down
    onMarkMissed: () -> Unit
) {
    // UI only - no state management
    Button(onClick = onMarkTaken) { Text("Mark Taken") }
}
```

**Why**:
- Composables are stateless and reusable
- ViewModel holds the truth (state)
- UI just displays and reports events

---

## 🌐 Firebase Integration

### Firebase Authentication
**Implementation**: `FirebaseAuthManager.kt:30`

**Pattern**: Anonymous Authentication
- Each device gets unique anonymous user ID
- No sign-up/login required
- Fallback to device-specific ID if offline
- Used for user identification in Firestore

**Code**:
```kotlin
suspend fun ensureAuthenticated(): String {
    val auth = FirebaseAuth.getInstance()
    if (auth.currentUser == null) {
        auth.signInAnonymously().await()
    }
    return auth.currentUser?.uid ?: getDeviceId()
}
```

---

### Firestore Real-time Listeners
**Implementation**: `FirebaseMedicationRepository.kt:45`

**Pattern**: Flow-based listeners
```kotlin
val medications: Flow<List<Medication>> = callbackFlow {
    val listener = firestore.collection("users/$userId/medications")
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val meds = snapshot?.documents
                ?.mapNotNull { it.toObject(FirestoreMedication::class.java) }
                ?.map { it.toMedication() }
                ?: emptyList()
            trySend(meds)
        }
    
    awaitClose { listener.remove() }
}.catch { emit(emptyList()) }
```

**Benefits**:
- Real-time updates (no polling)
- Automatic UI refresh
- Works with coroutines/StateFlow
- Lifecycle-aware (stops listening when cancelled)

---

### Firestore Write Operations
**Implementation**: `FirebaseMedicationRepository.kt:150, 200`

**Pattern**: Suspend functions with await()
```kotlin
suspend fun addOrUpdateMedication(medication: Medication) {
    val firestoreMed = FirestoreMedication.fromMedication(medication)
    firestore.collection("users/$userId/medications")
        .document(medication.id)
        .set(firestoreMed)
        .await()  // ← Suspend until complete
}
```

**All writes use coroutines** - no callbacks, clean async code

---

### Firebase Cloud Messaging (FCM)
**Complete Guide**: See [FCM_SETUP.md](FCM_SETUP.md)

**Components**:
- `MyFirebaseMessagingService.kt:20` - Receive notifications
- `FirebaseAuthManager.kt:50` - Token management
- `FCMHelper.kt:30` - Send notifications (HTTP API)

**Flow**:
1. Patient app generates FCM token on first launch
2. Token saved to Firestore user document
3. Caregiver clicks "Send Reminder" button
4. App sends HTTP POST to FCM API with patient's token
5. Patient receives push notification

**Why Direct HTTP API**: Free tier friendly, no Cloud Functions needed

---

## ⚙️ Navigation

### Compose Navigation
**Setup**: `MainActivity.kt:80-150`

**Routes**:
- Patient: "home", "medications", "addEdit/{id}", "stats", "settings"
- Caregiver: "caregiverPatients", "patientMonitor/{pin}", "qrScanner", "caregiverSettings"

**Pattern**:
```kotlin
NavHost(navController, startDestination = "home") {
    composable("home") { HomeScreen(navController, ...) }
    composable("addEdit/{id}") { backStackEntry ->
        val medId = backStackEntry.arguments?.getString("id")
        AddEditMedicationScreen(navController, medId)
    }
}

// Navigate:
navController.navigate("addEdit/${medicationId}")

// Go back:
navController.popBackStack()
```

**See Also**: [technical/03-navigation.md](technical/03-navigation.md)

---

## 🔄 Coroutines & Threading

### viewModelScope
**Used in all ViewModels**: `HomeViewModel.kt:80`, etc.

**Pattern**:
```kotlin
fun loadData() {
    viewModelScope.launch {
        val result = repository.getData()
        _uiState.value = _uiState.value.copy(data = result)
    }
}
```

**Why viewModelScope**:
- Auto-cancels when ViewModel cleared (lifecycle-aware)
- Prevents memory leaks
- Structured concurrency (child coroutines cancelled with parent)

---

### Dispatchers
**Default**: `Dispatchers.Main` (viewModelScope default)
**I/O Operations**: `Dispatchers.IO` (Firebase calls automatically use IO dispatcher)

**Pattern**:
```kotlin
viewModelScope.launch {
    // Already on Main dispatcher
    val result = withContext(Dispatchers.IO) {
        // Heavy I/O work
    }
    // Back on Main - safe to update UI
}
```

**Note**: Firebase SDK handles threading automatically - `.await()` switches to IO internally

---

## 🎯 Common Exam Question Patterns

### "Explain [concept] and show your implementation"
**Strategy**:
1. Use Ctrl+F in this file to find concept
2. Note file:line references
3. Write answer: "In my implementation (FileName.kt:line), I used [concept] to [purpose]..."

**Example**: "Explain StateFlow"
→ Answer: "StateFlow is a state holder Flow that emits current state and updates. In my implementation (HomeViewModel.kt:59-60), I use MutableStateFlow privately and expose it as StateFlow publicly. The UI collects this with collectAsState() for automatic recomposition."

---

### "What's the difference between X and Y?"
**Strategy**:
1. Find both concepts in this index
2. Compare your usage
3. Explain when you used each and why

**Example**: "Difference between remember and rememberSaveable"
→ Answer: "remember survives recomposition only, while rememberSaveable survives process death. I use remember for temporary UI state like dialog visibility, and rememberSaveable for form inputs that users expect to persist."

---

### "Why did you choose [technology]?"
**Strategy**: Reference [ARCHITECTURE.md](ARCHITECTURE.md) for rationale

**Example**: "Why Compose over XML?"
→ Answer: "Jetpack Compose is modern, declarative UI with less boilerplate. Compose automatically recomposes when state changes, eliminating findViewById() and manual view updates. All my screens (HomeScreen.kt, StatsScreen.kt) are pure Composables."

---

## ❌ Implementation Gaps (Prepare Explanations)

### Concepts NOT Used in My App

**Fragments**:
- **Why not**: Used full Jetpack Compose with Single Activity Architecture
- **Alternative**: Composable screens handle what Fragments did in XML-based apps
- **Reference**: All screens in `ui/screens/` are Composables, not Fragments

**Room Database**: *(If you didn't use it)*
- **Why not**: Cloud-first approach with Firebase Firestore only
- **Alternative**: Firestore provides offline caching automatically
- **Reference**: `FirebaseMedicationRepository.kt` - all data operations use Firestore

**Hilt/Dagger**:
- **Why not**: Manual dependency injection kept project simple
- **Alternative**: `RepositoryProvider.kt` singleton pattern
- **Reference**: `RepositoryProvider.kt:10` - thread-safe singleton

**XML Layouts**:
- **Why not**: 100% Jetpack Compose
- **Alternative**: All UI is declarative Compose functions
- **Reference**: `ui/screens/` - all Composables, zero XML

---

## 💡 Unique Implementations (Strengths to Highlight)

### Dual Role System
- **Patient mode**: Track own medications, mark doses, view stats
- **Caregiver mode**: Monitor multiple patients, send reminders
- **Implementation**: `ProfileSelectionScreen.kt` - role selection at start
- **Reference**: `FirebaseMedicationRepository.kt` - separate methods for caregiver queries

### QR Code Patient Exchange
- **Feature**: Patient generates QR with data → Caregiver scans to add patient
- **Implementation**: 
  - `QRCodeGenerator.kt:20` - Generate QR from PatientDataExport
  - `QRScannerScreen.kt` - Scan and parse QR
  - `CaregiverPatientsViewModel.kt` - Import patient data
- **Why unique**: Elegant solution for same-device demo without server

### Real-time Firebase Sync
- **Feature**: Changes sync instantly across devices
- **Implementation**: Firestore real-time listeners with Flow
- **Reference**: `FirebaseMedicationRepository.kt:45` - callbackFlow pattern
- **Benefit**: Caregiver sees patient actions immediately

### Push Notifications
- **Feature**: Caregiver can send reminders to patient devices
- **Implementation**: FCM with direct HTTP API
- **Reference**: `MyFirebaseMessagingService.kt`, `FCMHelper.kt`
- **See**: [FCM_SETUP.md](FCM_SETUP.md) for complete guide

---

## 📖 Quick File Reference

**Need to find:**
- **How I handle errors?** → ViewModels - try/catch in launch blocks
- **How I do navigation?** → `MainActivity.kt:80` - NavHost setup
- **How notifications work?** → `NotificationScheduler.kt` + [FCM_SETUP.md](FCM_SETUP.md)
- **Firebase queries?** → `FirebaseMedicationRepository.kt:100-500`
- **StateFlow pattern?** → Any ViewModel (e.g., `HomeViewModel.kt:59-60`)
- **Composable examples?** → Any screen in `ui/screens/`
- **Data models?** → `data/model/` - Medication, DoseEvent, PatientProfile

---

## 📚 See Also

For more detailed explanations:
- **[CODEBASE_REFERENCE.md](CODEBASE_REFERENCE.md)** - Comprehensive code documentation
- **[ARCHITECTURE.md](ARCHITECTURE.md)** - System architecture overview
- **[FCM_SETUP.md](FCM_SETUP.md)** - Firebase Cloud Messaging complete guide
- **[technical/04-state-management.md](technical/04-state-management.md)** - StateFlow deep dive
- **[technical/00-architecture.md](technical/00-architecture.md)** - MVVM pattern details
- **[technical/03-navigation.md](technical/03-navigation.md)** - Navigation-Compose details

---

**Last Updated**: Auto-generated for exam preparation  
**Source Code**: `/Users/vedatesendag/Documents/GitHub/Medical_Adherence/`  
**Purpose**: Fast lookup during 3-hour written exam

**Remember**: Use Ctrl+F to find concepts instantly. Reference file:line in your answers. Show don't just tell!

