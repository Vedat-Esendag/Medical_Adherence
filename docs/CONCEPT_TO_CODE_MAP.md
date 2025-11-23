# Concept → Code Quick Reference Table

**Purpose**: Ultra-fast table lookup for exam. Use Ctrl+F to find concepts instantly.

**Priority 1 topics are at the top** - most likely exam questions.

---

## Priority 1: Core Android & Compose Concepts

| Concept | My Implementation | File:Line | Notes |
|---------|-------------------|-----------|-------|
| **MVVM** | ViewModels + Repository pattern | HomeViewModel.kt:55 | Used throughout app |
| **ViewModel** | State holders for screens | 8 ViewModels in viewmodel/ | Lifecycle-aware |
| **StateFlow** | Reactive UI state | HomeViewModel.kt:59-60 | Modern alternative to LiveData |
| **Repository Pattern** | Data abstraction layer | FirebaseMedicationRepository.kt:27 | Single source of truth |
| **Jetpack Compose** | Declarative UI framework | All files in ui/screens/ | 100% Compose, no XML |
| **@Composable** | UI functions | HomeScreen.kt, StatsScreen.kt, etc. | 10 screens + components |
| **LazyColumn** | Scrollable lists | HomeScreen.kt, MedicationsLibraryScreen.kt | With key parameter |
| **Compose Navigation** | Screen navigation | MainActivity.kt:80-150 | String-based routes |
| **Coroutines** | Async operations | viewModelScope in all ViewModels | Structured concurrency |
| **Flow** | Reactive data streams | FirebaseMedicationRepository.kt:45 | Replaces LiveData |
| **remember** | Composition-scoped state | Used in screens for dialogs | Survives recomposition |
| **rememberSaveable** | Process death-safe state | AddEditMedicationScreen.kt | Survives app kill |
| **Scaffold** | Screen structure | All screens | Material 3 layout |
| **Material 3** | Design system | ui/theme/ + all screens | Modern Material Design |
| **viewModelScope** | Coroutine scope | All ViewModels:launch{} | Auto-cancels on clear |

## Priority 2: Firebase & Data

| Concept | My Implementation | File:Line | Notes |
|---------|-------------------|-----------|-------|
| **Firebase Firestore** | Cloud NoSQL database | FirebaseMedicationRepository.kt:27 | Real-time sync |
| **Real-time Listeners** | Live data updates | FirebaseMedicationRepository.kt:45 | callbackFlow pattern |
| **Firebase Auth** | Anonymous authentication | FirebaseAuthManager.kt:30 | Device-specific IDs |
| **Firebase FCM** | Push notifications | MyFirebaseMessagingService.kt:20 | Cloud Messaging |
| **Firestore Queries** | Data filtering | FirebaseMedicationRepository.kt:100+ | whereEqualTo, orderBy |
| **Suspend Functions** | Coroutine functions | All repository methods | With .await() |
| **Data Classes** | Immutable models | Medication.kt, DoseEvent.kt | Kotlin data classes |
| **Flow Operators** | Transform streams | Repository → ViewModel | map, filter, catch |
| **callbackFlow** | Listener → Flow | FirebaseMedicationRepository.kt:45 | Firebase to Flow bridge |

## Priority 3: Background & System

| Concept | My Implementation | File:Line | Notes |
|---------|-------------------|-----------|-------|
| **WorkManager** | Background task scheduling | NotificationScheduler.kt:45 | Medication reminders |
| **Worker** | Background work executor | MedicationReminderWorker.kt:20 | Executes on schedule |
| **Notifications** | System notifications | NotificationScheduler.kt | Local reminders |
| **Notification Channels** | Android 8+ channels | MainActivity.kt:onCreate | Required for notifications |
| **Permissions** | Runtime permissions | AndroidManifest.xml | POST_NOTIFICATIONS, CAMERA |
| **Single Activity** | One activity architecture | MainActivity.kt:20-100 | Compose-based |
| **AndroidManifest** | App configuration | AndroidManifest.xml | Permissions, services |

## Priority 4: Dependency Injection & Patterns

| Concept | My Implementation | File:Line | Notes |
|---------|-------------------|-----------|-------|
| **Singleton Pattern** | Single repository instance | RepositoryProvider.kt:10 | Thread-safe |
| **Factory Pattern** | ViewModel creation | *ViewModelFactory.kt files | Manual DI |
| **Dependency Injection** | Manual DI via singleton | RepositoryProvider.kt | No Hilt/Dagger |
| **Observer Pattern** | StateFlow observation | All screens | collectAsState() |
| **State Hoisting** | State management | DoseCard.kt:45 | Callbacks pattern |

## Priority 5: Kotlin Features

| Concept | My Implementation | File:Line | Notes |
|---------|-------------------|-----------|-------|
| **Coroutines** | Async programming | viewModelScope.launch{} | All ViewModels |
| **suspend functions** | Coroutine functions | Repository methods | Can await |
| **Data Classes** | Model objects | data/model/ | Medication, DoseEvent |
| **Sealed Classes** | *(If used)* | *(Check your code)* | Type-safe states |
| **Extension Functions** | Kotlin extensions | FirestoreExtensions.kt | Helper functions |
| **Scope Functions** | let, apply, also | Throughout code | Kotlin idioms |
| **Null Safety** | ? and !! operators | Throughout | Kotlin null safety |
| **Lambda Expressions** | Callback functions | All Composables | onClick = { } |

## Priority 6: UI Components

| Concept | My Implementation | File:Line | Notes |
|---------|-------------------|-----------|-------|
| **Button** | Material 3 buttons | All screens | onClick callbacks |
| **Text** | Material 3 text | All screens | Typography styles |
| **TextField** | Input fields | AddEditMedicationScreen.kt | Material 3 |
| **Card** | Material 3 cards | DoseCard.kt, PatientCard.kt | Elevated cards |
| **Icon** | Material icons | All screens | Material Icons |
| **Dialog** | Modal dialogs | PinDialog.kt, etc. | AlertDialog |
| **FAB** | Floating Action Button | HomeScreen.kt | Add medication |
| **TopAppBar** | App bar | All screens in Scaffold | Material 3 |
| **BottomNavigationBar** | Navigation bar | Patient screens | Material 3 |
| **Spacer** | Layout spacing | All screens | Compose layout |
| **Column/Row** | Layout containers | All screens | Compose layout |

## Priority 7: Advanced Features

| Concept | My Implementation | File:Line | Notes |
|---------|-------------------|-----------|-------|
| **QR Code Generation** | Patient data export | QRCodeGenerator.kt:20 | ZXing library |
| **QR Code Scanning** | Add patient by QR | QRScannerScreen.kt | Camera permission |
| **Image Bitmap** | QR code display | QRCodeGenerator.kt | Android Bitmap |
| **JSON Serialization** | QR data format | PatientDataExport.kt | Gson library |
| **HTTP Client** | FCM API calls | FCMHelper.kt:30 | OkHttp library |
| **Date/Time** | Scheduling logic | LocalDate, LocalTime | Java Time API |

---

## Concepts NOT Used (Be Ready to Explain)

| Concept | Why Not Used | Alternative | Exam Answer |
|---------|--------------|-------------|-------------|
| **Fragments** | Full Jetpack Compose | Composable screens | "Used Single Activity + Compose instead of Fragments" |
| **XML Layouts** | 100% Compose | @Composable functions | "Declarative Compose UI instead of imperative XML" |
| **Room Database** | *(If not used)* | Firebase Firestore only | "Cloud-first approach with Firebase" |
| **Hilt/Dagger** | Manual DI | RepositoryProvider singleton | "Kept DI simple for learning" |
| **LiveData** | Modern alternative | StateFlow | "StateFlow is coroutine-native and more modern" |
| **RxJava** | Native coroutines | Kotlin Flow | "Flow integrates better with coroutines" |
| **Retrofit** | Firebase SDK | Firebase REST APIs | "Firebase SDK handles networking" |
| **ConstraintLayout** | Compose layout | Column/Row/Box | "Compose has built-in layout" |
| **ViewBinding** | No views | Compose state | "Compose doesn't need view binding" |
| **findViewById** | Compose | Direct state access | "Compose eliminates findViewById" |

---

## Android Course Topics Checklist

### Week 36-37: Android Basics
- [x] Activities - MainActivity.kt:20
- [ ] Fragments - Not used (Compose instead)
- [x] Intents - Share QR code in SettingsScreen.kt
- [x] Lifecycle - ViewModel lifecycle-aware
- [x] Manifest - AndroidManifest.xml

### Week 38-39: Kotlin
- [x] Coroutines - viewModelScope everywhere
- [x] Data Classes - Medication, DoseEvent, etc.
- [ ] Sealed Classes - *(Check if you used)*
- [x] Flow - Repository and ViewModel
- [x] Suspend Functions - All repository methods

### Week 40-41: Jetpack Compose
- [x] @Composable - All screens
- [x] State - StateFlow pattern
- [x] remember - Dialog states
- [x] rememberSaveable - Form inputs
- [x] LazyColumn - Lists with key
- [x] Navigation - Compose Navigation

### Week 42-43: Architecture
- [x] MVVM - Throughout app
- [x] ViewModel - 8 ViewModels
- [x] Repository - FirebaseMedicationRepository
- [x] DI - Manual via RepositoryProvider
- [ ] Hilt - Not used

### Week 44-45: Data
- [x] Firestore - Cloud database
- [ ] Room - *(Check if you used)*
- [x] Real-time Listeners - callbackFlow
- [x] Queries - whereEqualTo, orderBy

### Week 46-47: Background
- [x] WorkManager - NotificationScheduler
- [x] Notifications - Local reminders
- [x] FCM - Push notifications
- [x] Channels - Notification channels

---

## Quick Search Tips

**Finding implementations during exam:**

1. **Ctrl+F in this file** → Find concept in table
2. **Note file:line** → Example: "HomeViewModel.kt:59"
3. **Reference in answer** → "In HomeViewModel.kt line 59, I implemented..."

**Common searches:**
- "StateFlow" → HomeViewModel.kt:59-60
- "MVVM" → Throughout, example: HomeViewModel.kt:55
- "Compose" → All ui/screens/ files
- "Firebase" → FirebaseMedicationRepository.kt:27
- "Coroutines" → viewModelScope in all ViewModels
- "Navigation" → MainActivity.kt:80-150

---

## For Printing (Exam Backup)

**Print this table** as a backup reference in case of computer issues during exam.

**Most important rows** (memorize file:line):
1. MVVM → HomeViewModel.kt:55
2. StateFlow → HomeViewModel.kt:59-60
3. Repository → FirebaseMedicationRepository.kt:27
4. Compose screens → ui/screens/
5. Navigation → MainActivity.kt:80
6. Firebase → FirebaseMedicationRepository.kt
7. WorkManager → NotificationScheduler.kt:45
8. FCM → MyFirebaseMessagingService.kt:20

---

**Last Updated**: Auto-generated for exam  
**Purpose**: Fast table lookup during 3-hour exam  
**Usage**: Ctrl+F to find concept → Get file:line → Reference in exam answer

