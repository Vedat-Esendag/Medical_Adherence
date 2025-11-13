# File Reference Guide

Quick reference to all major files in the Medical Adherence app, organized by category with line counts, purposes, and key responsibilities.

---

## Table of Contents

1. [Core Architecture Files](#core-architecture-files)
2. [ViewModels](#viewmodels)
3. [UI Screens (Compose)](#ui-screens-compose)
4. [Data Models](#data-models)
5. [Firebase Integration](#firebase-integration)
6. [Utilities & Helpers](#utilities--helpers)
7. [Navigation & App Setup](#navigation--app-setup)
8. [Theme & Design](#theme--design)
9. [Project Configuration](#project-configuration)

---

## Core Architecture Files

### Repository Layer

| File | Lines | Location | Purpose |
|------|-------|----------|---------|
| `FirebaseMedicationRepository.kt` | 814 | `data/repository/` | Central data layer - all Firebase operations, business logic, adherence calculations |
| `RepositoryProvider.kt` | 50 | `data/repository/` | Singleton provider for repository instance (simple DI) |

**FirebaseMedicationRepository.kt Key Methods:**
```kotlin
// User Management
- getCurrentUserId(): String?
- getCurrentUserProfile(): UserProfile?
- setUserProfile(profile: UserProfile)
- updateFcmToken(userId: String, token: String)

// Medication CRUD
- val medications: Flow<List<Medication>>
- addMedication(medication: Medication): Result<String>
- updateMedication(medication: Medication): Result<Unit>
- deleteMedication(medId: String): Result<Unit>

// Dose Tracking
- markDoseTaken(medId: String, date: LocalDate, time: String)
- markDoseMissed(medId: String, date: LocalDate, time: String)
- undoDose(medId: String, date: LocalDate, time: String)
- getDoseEventsForDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<DoseEvent>>

// Statistics & Analytics
- calculateWeeklyAdherence(): Double
- calculateMonthlyAdherence(): Double
- calculateStreak(): Int
- getProblematicMedications(): List<Medication>
- getTodayDoses(): List<DoseInfo>

// Caregiver Functions
- getCaregiverPatients(): Flow<List<PatientInfo>>
- getMedicationsForPatientByPin(pin: String): Flow<List<Medication>>
- getDoseEventsForPatientByPin(pin: String, startDate: LocalDate, endDate: LocalDate): Flow<List<DoseEvent>>
- addPatientToCaregiver(patientPin: String, displayName: String)
- removePatientFromCaregiver(patientPin: String)

// Notifications
- sendViaLocalServer(patientPin: String, title: String, body: String): Result<String>
- sendViaCloudFunction(patientPin: String, title: String, body: String): Result<String>
- sendViaDirectAPI(patientPin: String, title: String, body: String): Result<String>
```

---

## ViewModels

### Patient-Focused ViewModels

| File | Lines | Location | Purpose |
|------|-------|----------|---------|
| `HomeViewModel.kt` | 249 | `viewmodel/` | Patient home screen logic - countdown timer, dose tracking, quick actions |
| `MedicationViewModel.kt` | 180 | `viewmodel/` | Add/edit medication form logic, validation |
| `StatsViewModel.kt` | 220 | `viewmodel/` | Statistics calculations, adherence trends, streak tracking |

**HomeViewModel.kt:**
```kotlin
// UI State
data class HomeUiState(
    val isLoading: Boolean,
    val todayDoses: List<DoseInfo>,
    val nextDoseCountdown: String?,
    val weeklyAdherence: Double,
    val currentStreak: Int,
    val showSuccess: Boolean,
    val error: String?
)

// Key Methods
- markDoseTaken(medId: String, time: String)
- markDoseMissed(medId: String, time: String)
- undoDose(medId: String, time: String)
- snoozeDose(medId: String, time: String, minutes: Int)
- refresh()
- private fun startCountdownTimer()
- private fun loadTodayDoses()
```

**StatsViewModel.kt:**
```kotlin
// UI State
data class StatsUiState(
    val weeklyAdherence: Double,
    val monthlyAdherence: Double,
    val currentStreak: Int,
    val longestStreak: Int,
    val problematicMeds: List<Medication>,
    val dailyBreakdown: Map<DayOfWeek, Double>,
    val trend: AdherenceTrend,
    val isLoading: Boolean
)

// Key Methods
- loadStatistics()
- calculateTrend(): AdherenceTrend
- getDailyBreakdown(): Map<DayOfWeek, Double>
```

### Caregiver-Focused ViewModels

| File | Lines | Location | Purpose |
|------|-------|----------|---------|
| `CaretakerViewModel.kt` | 602 | `viewmodel/` | Caregiver monitoring dashboard - patient data, notifications, adherence tracking |

**CaretakerViewModel.kt:**
```kotlin
// Dual-Mode Support
// Constructor: CaretakerViewModel(repository, patientPin: String? = null)
// If patientPin is null: Monitor current user (self-monitoring)
// If patientPin is set: Monitor specific patient (caregiver mode)

// UI State
data class CaretakerUiState(
    val isLoading: Boolean,
    val patientInfo: PatientInfo?,
    val todayDoses: List<DoseInfo>,
    val weeklyAdherence: Double,
    val monthlyAdherence: Double,
    val currentStreak: Int,
    val problematicMeds: List<Medication>,
    val recentMissedDoses: List<DoseInfo>,
    val notificationSent: Boolean,
    val error: String?
)

// Key Methods
- loadPatientData()
- sendReminder(title: String, body: String)
- refreshData()
- updatePatientDisplayName(newName: String)
- removePatient()
```

### Settings & Profile ViewModels

| File | Lines | Location | Purpose |
|------|-------|----------|---------|
| `SettingsViewModel.kt` | 190 | `viewmodel/` | App settings - accessibility, notifications, caregiver PIN |
| `ProfileViewModel.kt` | 120 | `viewmodel/` | User profile management - role, name, PIN generation |

---

## UI Screens (Compose)

### Main Screens

| File | Lines | Location | Purpose |
|------|-------|----------|---------|
| `HomeScreen.kt` | 334 | `ui/screens/` | Patient home - medication list, countdown timers, quick actions |
| `CaretakerScreen.kt` | 1,089 | `ui/screens/` | Caregiver dashboard - patient monitoring, adherence charts, notifications |
| `StatsScreen.kt` | 488 | `ui/screens/` | Statistics & analytics - adherence graphs, streaks, trends |
| `SettingsScreen.kt` | 553 | `ui/screens/` | App settings - accessibility, notifications, caregiver settings |
| `AddEditMedicationScreen.kt` | 335 | `ui/screens/` | Add/edit medication form - name, dosage, schedule |
| `CaregiverSettingsScreen.kt` | 391 | `ui/screens/` | Caregiver-specific settings - patient list, PIN management |
| `RoleSelectionScreen.kt` | 180 | `ui/screens/` | Initial role selection - patient or caregiver |

**Screen Complexity Ranking:**
1. **CaretakerScreen.kt** (1,089 lines) - Most complex
   - Multiple charts and graphs
   - Patient data export
   - Real-time sync indicators
   - Notification controls

2. **SettingsScreen.kt** (553 lines)
   - Accessibility controls
   - Notification preferences
   - PIN generation/display

3. **StatsScreen.kt** (488 lines)
   - Multiple adherence charts
   - Trend indicators
   - Daily breakdown graph

### UI Components

| File | Lines | Location | Purpose |
|------|-------|----------|---------|
| `DoseCard.kt` | 180 | `ui/components/` | Medication dose display with countdown timer |
| `MedicationCard.kt` | 150 | `ui/components/` | Medication overview card for lists |
| `AdherenceChart.kt` | 220 | `ui/components/` | Weekly/monthly adherence bar chart |
| `PinDialog.kt` | 140 | `ui/components/` | PIN display with QR code |
| `ConfirmationDialog.kt` | 80 | `ui/components/` | Reusable confirmation dialog |
| `LoadingIndicator.kt` | 40 | `ui/components/` | Centered loading spinner |

---

## Data Models

### Domain Models

| File | Lines | Location | Purpose |
|------|-------|----------|---------|
| `Medication.kt` | 60 | `data/model/` | Core medication entity with schedule |
| `DoseEvent.kt` | 40 | `data/model/` | Record of taken/missed dose |
| `UserProfile.kt` | 45 | `data/model/` | User profile (role, name, PIN, FCM token) |
| `DoseInfo.kt` | 55 | `data/model/` | Dose display info with status |
| `PatientDataExport.kt` | 70 | `data/model/` | Caregiver view of patient data |

**Medication.kt:**
```kotlin
data class Medication(
    val id: String,
    val name: String,
    val dosage: String,
    val times: List<String>,                    // ["08:00", "20:00"]
    val frequency: MedicationFrequency,         // DAILY, WEEKLY, SPECIFIC_DAYS
    val specificDays: List<Int> = emptyList(),  // [1,3,5] for Mon/Wed/Fri
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

enum class MedicationFrequency {
    DAILY, WEEKLY, SPECIFIC_DAYS
}
```

**DoseEvent.kt:**
```kotlin
data class DoseEvent(
    val id: String,              // Composite: "${medId}_${date}_${time}"
    val medId: String,
    val date: String,            // yyyy-MM-dd
    val time: String,            // HH:mm
    val taken: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
```

**UserProfile.kt:**
```kotlin
data class UserProfile(
    val userId: String,
    val role: UserRole,          // PATIENT, CAREGIVER
    val name: String,
    val pin: String,             // 6-digit PIN
    val fcmToken: String?,
    val createdAt: Long
)

enum class UserRole {
    PATIENT, CAREGIVER
}
```

### Firestore DTOs

| File | Lines | Location | Purpose |
|------|-------|----------|---------|
| `FirestoreModels.kt` | 180 | `data/model/firestore/` | Firestore-specific data transfer objects |

**FirestoreModels.kt:**
```kotlin
// DTOs for Firestore serialization
data class FirestoreUserProfile(...)
data class FirestoreMedication(...)
data class FirestoreDoseEvent(...)
data class FirestoreCaregiverLink(...)
data class FirestoreSettings(...)

// Extension functions for conversion
fun FirestoreMedication.toDomain(): Medication
fun Medication.toFirestore(): FirestoreMedication
// ... similar for all models
```

---

## Firebase Integration

### Authentication & Messaging

| File | Lines | Location | Purpose |
|------|-------|----------|---------|
| `FirebaseAuthManager.kt` | 170 | `data/firebase/` | Authentication, user ID generation, FCM token management |
| `MyFirebaseMessagingService.kt` | 104 | `services/` | FCM message receiver, notification display |
| `FCMHelper.kt` | 81 | `utils/` | Direct HTTP API notification sender |
| `LocalFCMHelper.kt` | 87 | `utils/` | Local server notification sender |

**FirebaseAuthManager.kt:**
```kotlin
class FirebaseAuthManager(private val context: Context) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // Authentication
    suspend fun signInAnonymouslyOrCreateOfflineUser(): String
    fun getCurrentUserId(): String?
    fun isUserSignedIn(): Boolean

    // FCM Token Management
    suspend fun requestFcmToken(): String?
    suspend fun updateFcmToken(userId: String, token: String)

    // Offline Support
    private fun createOfflineUserId(context: Context): String
    // Returns: "offline_user_<ANDROID_ID>"
}
```

**MyFirebaseMessagingService.kt:**
```kotlin
class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage)
    override fun onNewToken(token: String)

    private fun showNotification(title: String, body: String)
    private fun createNotificationChannel()
}
```

### Firestore Extensions

| File | Lines | Location | Purpose |
|------|-------|----------|---------|
| `FirestoreExtensions.kt` | 95 | `data/firebase/` | Extension functions for reactive Firestore queries |

**FirestoreExtensions.kt:**
```kotlin
// Convert Firestore Query to Kotlin Flow
fun <T : Any> Query.asFlow(mapper: (DocumentSnapshot) -> T?): Flow<List<T>>

// Convert DocumentReference to Flow
fun <T : Any> DocumentReference.asFlow(mapper: (DocumentSnapshot) -> T?): Flow<T?>

// Firestore-specific utilities
suspend fun <T> awaitTaskResult(task: Task<T>): T
```

---

## Utilities & Helpers

### Notifications & Scheduling

| File | Lines | Location | Purpose |
|------|-------|----------|---------|
| `NotificationScheduler.kt` | 150 | `utils/` | WorkManager-based local notification scheduling |
| `DoseReminderWorker.kt` | 80 | `workers/` | WorkManager worker for dose reminders |

**NotificationScheduler.kt:**
```kotlin
object NotificationScheduler {
    fun scheduleDoseNotifications(context: Context, medication: Medication)
    fun cancelDoseNotifications(context: Context, medicationId: String)
    fun scheduleAllMedications(context: Context, medications: List<Medication>)

    private fun createWorkRequest(medId: String, time: LocalTime): OneTimeWorkRequest
}
```

### Constants & Configuration

| File | Lines | Location | Purpose |
|------|-------|----------|---------|
| `Constants.kt` | 65 | `utils/` | App-wide configuration constants |

**Constants.kt:**
```kotlin
object Constants {
    // FCM Configuration
    const val USE_LOCAL_SERVER = true
    const val USE_CLOUD_FUNCTIONS = false
    const val FCM_SERVER_KEY = "..."

    // Adherence Thresholds
    const val ADHERENCE_EXCELLENT = 90
    const val ADHERENCE_GOOD = 80
    const val ADHERENCE_PROBLEMATIC = 70
    const val ADHERENCE_WARNING = 30

    // Dose Settings
    const val DOSE_WINDOW_MINUTES = 30
    const val SNOOZE_DURATION_MINUTES = 15

    // Statistics
    const val STREAK_LOOKBACK_DAYS = 90
    const val RECENT_MISSED_DOSES_LIMIT = 10
    const val TREND_THRESHOLD_PERCENT = 10

    // UI
    const val STATEFLOW_TIMEOUT_MS = 5000L
    const val COUNTDOWN_UPDATE_INTERVAL = 1000L

    // Notifications
    const val NOTIFICATION_CHANNEL_ID = "medication_reminders"
    const val NOTIFICATION_CHANNEL_NAME = "Medication Reminders"
}
```

### Date & Time Utilities

| File | Lines | Location | Purpose |
|------|-------|----------|---------|
| `DateTimeUtils.kt` | 120 | `utils/` | Date/time formatting, parsing, calculations |

**DateTimeUtils.kt:**
```kotlin
object DateTimeUtils {
    fun formatTime(time: LocalTime): String
    fun formatDate(date: LocalDate): String
    fun parseTime(timeString: String): LocalTime
    fun parseDate(dateString: String): LocalDate

    fun calculateCountdown(targetTime: LocalDateTime): String
    fun isOverdue(scheduledTime: LocalDateTime): Boolean
    fun getDayOfWeekName(dayOfWeek: DayOfWeek): String
}
```

---

## Navigation & App Setup

### Navigation

| File | Lines | Location | Purpose |
|------|-------|----------|---------|
| `NavGraph.kt` | 180 | `navigation/` | Compose Navigation graph, routes, arguments |
| `Screen.kt` | 60 | `navigation/` | Sealed class defining all app screens |

**Screen.kt:**
```kotlin
sealed class Screen(val route: String) {
    object RoleSelection : Screen("role_selection")
    object Home : Screen("home")
    object CaretakerDashboard : Screen("caretaker_dashboard/{patientPin}") {
        fun createRoute(patientPin: String) = "caretaker_dashboard/$patientPin"
    }
    object AddMedication : Screen("add_medication")
    object EditMedication : Screen("edit_medication/{medId}") {
        fun createRoute(medId: String) = "edit_medication/$medId"
    }
    object Stats : Screen("stats")
    object Settings : Screen("settings")
    object CaregiverSettings : Screen("caregiver_settings")
}
```

### Main App

| File | Lines | Location | Purpose |
|------|-------|----------|---------|
| `MainActivity.kt` | 120 | `ui/` | Single activity, sets up Compose theme and navigation |
| `MedicalAdherenceApp.kt` | 80 | `ui/` | Root composable, navigation host |

---

## Theme & Design

### Material 3 Theme

| File | Lines | Location | Purpose |
|------|-------|----------|---------|
| `Theme.kt` | 200 | `ui/theme/` | Material 3 theme setup, dynamic colors, accessibility |
| `Color.kt` | 80 | `ui/theme/` | Color palettes (light, dark, high contrast) |
| `Type.kt` | 60 | `ui/theme/` | Typography definitions |

**Theme.kt:**
```kotlin
@Composable
fun MedicalAdherenceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    highContrastMode: Boolean = false,
    dynamicColor: Boolean = true,
    fontScale: Float = 1.0f,
    content: @Composable () -> Unit
)
```

**Color.kt:**
```kotlin
// Light Theme
val CalmBlue40 = Color(0xFF4A90E2)
val CalmBlueGrey40 = Color(0xFF607D8B)
val CalmAccent40 = Color(0xFF00BCD4)

// Dark Theme
val CalmBlue80 = Color(0xFF90CAF9)
val CalmBlueGrey80 = Color(0xFFB0BEC5)
val CalmAccent80 = Color(0xFF4DD0E1)

// High Contrast
val HighContrastPrimary = Color(0xFF0D47A1)
val HighContrastOnPrimary = Color(0xFFFFFFFF)
```

---

## Project Configuration

### Gradle Files

| File | Lines | Location | Purpose |
|------|-------|----------|---------|
| `build.gradle.kts` (app) | 180 | `app/` | App-level dependencies, plugins, build config |
| `build.gradle.kts` (project) | 40 | `/` | Project-level build configuration |
| `gradle.properties` | 20 | `/` | Gradle JVM settings |

**Key Dependencies (build.gradle.kts):**
```kotlin
dependencies {
    // Firebase (BOM v32.7.0)
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-messaging:23.4.0")

    // Compose & Material 3
    implementation(platform("androidx.compose:compose-bom:2024.01.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // QR Code
    implementation("com.google.zxing:core:3.5.2")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    // HTTP
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // JSON
    implementation("com.google.code.gson:gson:2.10.1")
}
```

### Android Manifest

| File | Lines | Location | Purpose |
|------|-------|----------|---------|
| `AndroidManifest.xml` | 85 | `app/src/main/` | App permissions, activities, services |

**Key Manifest Entries:**
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.CAMERA" />

<application>
    <activity android:name=".ui.MainActivity" />

    <service
        android:name=".MyFirebaseMessagingService"
        android:exported="false">
        <intent-filter>
            <action android:name="com.google.firebase.MESSAGING_EVENT" />
        </intent-filter>
    </service>
</application>
```

---

## File Size Summary

### By Category

**ViewModels:**
- Total: ~1,361 lines across 5 files
- Largest: CaretakerViewModel.kt (602 lines)
- Average: 272 lines per file

**UI Screens:**
- Total: ~3,370 lines across 7 main screens
- Largest: CaretakerScreen.kt (1,089 lines)
- Average: 481 lines per file

**Repository & Data:**
- FirebaseMedicationRepository.kt: 814 lines
- Data models: ~450 lines total
- Firebase integration: ~450 lines total

**Total Codebase:**
- Kotlin files: ~8,000 lines
- Compose UI: ~4,500 lines (56%)
- Business logic: ~2,000 lines (25%)
- Data models & DTOs: ~1,500 lines (19%)

---

## Code Organization Patterns

### Package Structure

```
com.example.medicaladherence/
├── data/
│   ├── model/
│   │   ├── domain/           (Medication, DoseEvent, UserProfile)
│   │   └── firestore/        (DTOs for Firebase)
│   ├── repository/           (FirebaseMedicationRepository)
│   └── firebase/             (Auth, Extensions)
│
├── ui/
│   ├── screens/              (All Compose screens)
│   ├── components/           (Reusable UI components)
│   └── theme/                (Material 3 theme)
│
├── viewmodel/                (All ViewModels)
│
├── navigation/               (Nav graph, routes)
│
├── utils/                    (Helpers, Constants)
│
├── services/                 (FCM service)
│
└── workers/                  (WorkManager workers)
```

### Naming Conventions

**Files:**
- ViewModels: `*ViewModel.kt`
- Screens: `*Screen.kt`
- Components: `*Card.kt`, `*Dialog.kt`
- Models: PascalCase nouns (`Medication.kt`)
- Utilities: Descriptive names (`DateTimeUtils.kt`)

**Classes:**
- Data classes: PascalCase (`Medication`, `DoseEvent`)
- ViewModels: `*ViewModel`
- Composables: PascalCase functions (`HomeScreen()`)

**Variables:**
- camelCase for properties
- UPPER_SNAKE_CASE for constants
- Descriptive names (no abbreviations)

---

## Quick Navigation

### Find a File by Feature

**Medication Management:**
- Add/Edit: `AddEditMedicationScreen.kt`, `MedicationViewModel.kt`
- Display: `HomeScreen.kt`, `DoseCard.kt`
- Delete: `FirebaseMedicationRepository.kt:deleteMedication()`

**Dose Tracking:**
- Mark Taken/Missed: `HomeViewModel.kt:markDoseTaken()`, `DoseCard.kt`
- History: `FirebaseMedicationRepository.kt:getDoseEventsForDateRange()`
- Undo: `HomeViewModel.kt:undoDose()`

**Statistics:**
- UI: `StatsScreen.kt`
- Logic: `StatsViewModel.kt`
- Calculations: `FirebaseMedicationRepository.kt:calculate*Adherence()`

**Caregiver Monitoring:**
- Dashboard: `CaretakerScreen.kt`
- Logic: `CaretakerViewModel.kt`
- Pairing: `CaregiverSettingsScreen.kt`, `PinDialog.kt`

**Notifications:**
- FCM: `MyFirebaseMessagingService.kt`, `FCMHelper.kt`
- Local: `NotificationScheduler.kt`, `DoseReminderWorker.kt`
- Sending: `CaretakerViewModel.kt:sendReminder()`

**Authentication:**
- Manager: `FirebaseAuthManager.kt`
- Role Selection: `RoleSelectionScreen.kt`
- Profile: `ProfileViewModel.kt`

**Settings:**
- UI: `SettingsScreen.kt`, `CaregiverSettingsScreen.kt`
- Logic: `SettingsViewModel.kt`
- Theme: `Theme.kt`

---

## Key Architectural Decisions

### Why These File Sizes?

**Large Files (>500 lines):**
1. **CaretakerScreen.kt (1,089)**: Complex dashboard with multiple charts, patient data, notifications
2. **FirebaseMedicationRepository.kt (814)**: Central data layer handling all Firebase operations
3. **CaretakerViewModel.kt (602)**: Dual-mode support, extensive patient monitoring logic
4. **SettingsScreen.kt (553)**: Many settings options with accessibility features

**These are acceptable because:**
- Single Responsibility Principle: Each handles one specific domain
- Compose screens naturally grow with UI complexity
- Repository centralizes data access (better than scattered)
- Clear internal organization with comments

**Alternative Approaches:**
- Could split CaretakerScreen into multiple composables (recommended for production)
- Could extract statistics calculations to separate service
- Could use Hilt for dependency injection (removes RepositoryProvider)

---

## File Reference Cheat Sheet

### "Where do I find...?"

| Feature | Primary File | Supporting Files |
|---------|-------------|------------------|
| Medication CRUD | `FirebaseMedicationRepository.kt` | `MedicationViewModel.kt`, `AddEditMedicationScreen.kt` |
| Dose tracking | `HomeViewModel.kt` | `DoseCard.kt`, `FirebaseMedicationRepository.kt` |
| Adherence stats | `StatsViewModel.kt` | `StatsScreen.kt`, `AdherenceChart.kt` |
| Caregiver dashboard | `CaretakerScreen.kt` | `CaretakerViewModel.kt` |
| Push notifications | `MyFirebaseMessagingService.kt` | `FCMHelper.kt`, `LocalFCMHelper.kt` |
| PIN generation | `ProfileViewModel.kt` | `PinDialog.kt`, `SettingsScreen.kt` |
| Real-time sync | `FirestoreExtensions.kt` | `FirebaseMedicationRepository.kt` |
| Offline support | `FirebaseAuthManager.kt` | `FirebaseMedicationRepository.kt` |
| Theme/styling | `Theme.kt` | `Color.kt`, `Type.kt` |
| Navigation | `NavGraph.kt` | `Screen.kt`, `MedicalAdherenceApp.kt` |

---

## Codebase Health Metrics

**Strengths:**
✅ Clear package organization
✅ Consistent naming conventions
✅ Single activity architecture
✅ Repository pattern for data access
✅ Reactive data flow (Flow/StateFlow)
✅ Compose for UI (modern, declarative)

**Areas for Improvement:**
⚠️ Large screen files (>500 lines) - consider splitting
⚠️ No unit tests yet
⚠️ Manual dependency injection (use Hilt)
⚠️ Some ViewModels tightly coupled to repository
⚠️ Limited error handling in some flows

**Recommended Refactoring:**
1. Extract chart components from CaretakerScreen
2. Create StatisticsCalculator service
3. Implement Hilt for DI
4. Add unit tests for ViewModels
5. Create sealed class for UI events

---

## Summary

This codebase demonstrates:
- **Modern Android architecture** (MVVM + Repository)
- **Clean code organization** (packages by feature/layer)
- **Reactive programming** (Flow, StateFlow, Compose)
- **Firebase integration** (Firestore, Auth, FCM)
- **Accessibility focus** (Material 3, font scaling, high contrast)

**Total Lines of Code:** ~8,000
**Primary Language:** Kotlin 100%
**UI Framework:** Jetpack Compose
**Architecture:** MVVM + Repository Pattern
**Backend:** Firebase (Firestore, Auth, FCM)

For implementation details, see the other documentation files in this series.

---

**End of File Reference**
