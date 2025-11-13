# Technical Implementation Details

This document provides a deep dive into the technical implementation of the Medical Adherence app, covering Firebase integration, FCM notifications, offline-first design, state management, and Compose UI patterns.

---

## Table of Contents

1. [Firebase/Firestore Structure](#firebasefirestore-structure)
2. [FCM Push Notifications](#fcm-push-notifications)
3. [Offline-First Design](#offline-first-design)
4. [State Management](#state-management)
5. [Compose UI & Material 3](#compose-ui--material-3)
6. [Key Dependencies](#key-dependencies)
7. [Constants & Configuration](#constants--configuration)
8. [Unique Technical Patterns](#unique-technical-patterns)

---

## Firebase/Firestore Structure

### Collections & Documents Hierarchy

```
Firestore Database
│
├── users/
│   └── {userId}/                          (Document)
│       ├── Fields:
│       │   - role: "patient" | "caregiver"
│       │   - name: String
│       │   - pin: String (6-digit)
│       │   - fcmToken: String
│       │   - createdAt: Timestamp
│       │
│       ├── medications/                   (Subcollection)
│       │   └── {medicationId}/            (Document)
│       │       - id: String
│       │       - name: String
│       │       - dosage: String
│       │       - times: Array<String>     (e.g., ["08:00", "20:00"])
│       │       - frequency: "daily" | "weekly" | "specific_days"
│       │       - specificDays: Array<Int>  (1=Mon, 7=Sun)
│       │       - notes: String
│       │       - createdAt: Timestamp
│       │
│       ├── doseEvents/                    (Subcollection)
│       │   └── {compositeId}/             (Document)
│       │       - Format: "{medId}_{date}_{time}"
│       │       - medId: String
│       │       - date: String (yyyy-MM-dd)
│       │       - time: String (HH:mm)
│       │       - taken: Boolean
│       │       - timestamp: Timestamp
│       │
│       └── settings/                      (Subcollection)
│           └── app_settings               (Document)
│               - fontScale: Float (1.0 default)
│               - caretakerPin: String?
│               - highContrastMode: Boolean
│               - alertThreshold: Int (default 70%)
│               - dailySummaryEnabled: Boolean
│               - dailySummaryTime: String
│
├── caregiver_links/
│   └── {linkId}/                          (Document)
│       - caregiverUserId: String
│       - patientUserId: String
│       - patientPin: String
│       - patientName: String
│       - displayName: String              (Custom patient name)
│       - phoneNumber: String?
│       - notes: String?
│       - addedAt: Timestamp
│
└── notificationRequests/                  (For Cloud Functions approach)
    └── {requestId}/                       (Document)
        - patientPin: String
        - title: String
        - body: String
        - timestamp: Timestamp
        - sent: Boolean
```

### Why This Structure?

**Subcollections vs. Root Collections:**
- Medications and doseEvents are **subcollections** under users
- Allows per-user data isolation
- Easier security rules (can scope to `/users/{userId}/`)
- Natural data organization

**Composite Keys for Dose Events:**
```kotlin
// Prevents duplicate entries for same medication/date/time
val doseEventId = "${medId}_${date}_${time}"
// Example: "aspirin123_2025-01-15_08:00"
```

**Benefits:**
- Direct document lookup without queries
- Automatic deduplication
- Efficient updates (no need to search first)

**Caregiver Links as Root Collection:**
- Allows many-to-many relationships
- One caregiver can monitor multiple patients
- One patient can have multiple caregivers
- Query by either `caregiverUserId` or `patientPin`

### Firestore Configuration

**Offline Persistence:**
```kotlin
// Automatically enabled in Firebase SDK 9.0+
// No explicit configuration needed
Firebase.firestore.apply {
    firestoreSettings = firestoreSettings {
        isPersistenceEnabled = true  // Default: true
    }
}
```

**Why Offline Persistence Matters:**
- App works without internet
- Writes are queued locally
- Automatic sync when online
- Local cache for faster reads

---

## FCM Push Notifications

### Three Implementation Approaches

The app implements **three different FCM notification approaches**, configurable via constants:

#### Approach 1: Local Express Server (Development)

**Configuration:**
```kotlin
// Constants.kt
const val USE_LOCAL_SERVER = true
const val USE_CLOUD_FUNCTIONS = false
```

**How It Works:**
```
CaretakerViewModel
    ↓
LocalFCMHelper.sendNotification()
    ↓
HTTP POST to http://10.0.2.2:3000/sendNotification
    ↓
Express Server (Node.js)
    ↓
Firebase Admin SDK
    ↓
FCM API
    ↓
Patient Device
```

**Implementation (`LocalFCMHelper.kt`):**
```kotlin
object LocalFCMHelper {
    private const val SERVER_URL = "http://10.0.2.2:3000/sendNotification"

    suspend fun sendNotification(
        patientPin: String,
        title: String,
        body: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val json = JSONObject().apply {
            put("patientPin", patientPin)
            put("title", title)
            put("body", body)
        }

        val request = Request.Builder()
            .url(SERVER_URL)
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        // Execute and handle response...
    }
}
```

**Pros:**
- Secure (no API keys in app)
- Works on Firebase Spark plan (free)
- Easy to debug

**Cons:**
- Requires local server running
- Only works with emulators (10.0.2.2 is emulator localhost)

---

#### Approach 2: Cloud Functions (Production)

**Configuration:**
```kotlin
const val USE_LOCAL_SERVER = false
const val USE_CLOUD_FUNCTIONS = true
```

**How It Works:**
```
CaretakerViewModel
    ↓
Write to Firestore: notificationRequests/{id}
    ↓
Cloud Function Trigger (onWrite)
    ↓
Firebase Admin SDK
    ↓
FCM API
    ↓
Patient Device
```

**Implementation:**
```kotlin
// In Repository
suspend fun sendViaCloudFunction(
    patientPin: String,
    title: String,
    body: String
): Result<String> = withContext(Dispatchers.IO) {
    try {
        val request = hashMapOf(
            "patientPin" to patientPin,
            "title" to title,
            "body" to body,
            "timestamp" to FieldValue.serverTimestamp(),
            "sent" to false
        )

        firestore.collection("notificationRequests")
            .add(request)
            .await()

        Result.success("Request sent")
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

**Cloud Function (Node.js):**
```javascript
exports.sendNotificationOnRequest = functions.firestore
    .document('notificationRequests/{requestId}')
    .onCreate(async (snap, context) => {
        const data = snap.data();
        const { patientPin, title, body } = data;

        // Query user by PIN
        const userSnapshot = await admin.firestore()
            .collection('users')
            .where('pin', '==', patientPin)
            .limit(1)
            .get();

        const fcmToken = userSnapshot.docs[0].data().fcmToken;

        // Send via Admin SDK
        await admin.messaging().send({
            token: fcmToken,
            notification: { title, body }
        });

        // Mark as sent
        await snap.ref.update({ sent: true });
    });
```

**Pros:**
- Most secure (server-side only)
- Scalable
- No API keys in app

**Cons:**
- Requires Firebase Blaze plan (paid)
- More complex deployment

---

#### Approach 3: Direct HTTP API (Legacy)

**Configuration:**
```kotlin
const val USE_LOCAL_SERVER = false
const val USE_CLOUD_FUNCTIONS = false
```

**How It Works:**
```
CaretakerViewModel
    ↓
FCMHelper.sendNotification()
    ↓
HTTP POST to https://fcm.googleapis.com/fcm/send
    ↓
FCM API (Legacy v1)
    ↓
Patient Device
```

**Implementation (`FCMHelper.kt`):**
```kotlin
object FCMHelper {
    private const val FCM_URL = "https://fcm.googleapis.com/fcm/send"

    suspend fun sendNotification(
        fcmToken: String,
        title: String,
        body: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val json = JSONObject().apply {
            put("to", fcmToken)
            put("notification", JSONObject().apply {
                put("title", title)
                put("body", body)
                put("sound", "default")
                put("priority", "high")
            })
        }

        val request = Request.Builder()
            .url(FCM_URL)
            .addHeader("Authorization", "key=${Constants.FCM_SERVER_KEY}")
            .addHeader("Content-Type", "application/json")
            .post(json.toString().toRequestBody())
            .build()

        // Execute...
    }
}
```

**Pros:**
- Simple implementation
- No server required
- Works on Spark plan

**Cons:**
- **Security risk**: API key in app code
- **Deprecated**: Google recommends HTTP v1 API
- Client can see server key (extractable from APK)

---

### Token Management

**Getting FCM Token (`FirebaseAuthManager.kt`):**
```kotlin
suspend fun requestFcmToken(): String? {
    return try {
        FirebaseMessaging.getInstance().token.await()
    } catch (e: Exception) {
        Log.e("Auth", "Failed to get FCM token", e)
        null
    }
}
```

**Saving Token to Firestore:**
```kotlin
suspend fun updateFcmToken(userId: String, token: String) {
    firestore.collection("users")
        .document(userId)
        .update("fcmToken", token)
        .await()
}
```

**When Token is Refreshed:**
```kotlin
// MyFirebaseMessagingService.kt
override fun onNewToken(token: String) {
    super.onNewToken(token)
    Log.d("FCM", "New token: $token")

    // Update in Firestore
    viewModelScope.launch {
        val userId = authManager.getCurrentUserId()
        if (userId != null) {
            repository.updateFcmToken(userId, token)
        }
    }
}
```

---

### Receiving Notifications

**Service Registration (`AndroidManifest.xml`):**
```xml
<service
    android:name=".MyFirebaseMessagingService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

**Handling Incoming Messages:**
```kotlin
class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FCM", "Message from: ${remoteMessage.from}")

        remoteMessage.notification?.let { notification ->
            showNotification(
                title = notification.title ?: "Medication Reminder",
                body = notification.body ?: ""
            )
        }
    }

    private fun showNotification(title: String, body: String) {
        // Create notification channel (Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Medication Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders from caregivers"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Build notification
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_medication)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)  // Open app on tap
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
```

**Notification Behavior:**
- Shows even when app is closed
- Tapping opens HomeScreen
- High priority (heads-up notification)
- Vibration + LED indicator
- Auto-dismiss on tap

---

## Offline-First Design

### The Challenge

**Problem:**
- Firebase Authentication requires internet connection
- Anonymous sign-in fails when offline
- Users need to track medications offline

**Solution: Device-Specific Fallback IDs**

### Implementation

**FirebaseAuthManager.kt:**
```kotlin
suspend fun signInAnonymouslyOrCreateOfflineUser(context: Context): String {
    return try {
        // Try Firebase Auth
        val result = auth.signInAnonymously().await()
        result.user?.uid ?: throw Exception("No UID")
    } catch (e: Exception) {
        // Fallback: Create device-specific offline ID
        createOfflineUserId(context)
    }
}

private fun createOfflineUserId(context: Context): String {
    val androidId = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ANDROID_ID
    )
    val shortId = androidId?.take(8) ?: "unknown"
    return "offline_user_$shortId"
}
```

**Example IDs:**
- Online: `"kX9mPqR7sNTg2hWvL4Yz"` (Firebase UID)
- Offline: `"offline_user_9774d56d"` (Device-specific)

### Why This Works

**1. Device-Specific:**
- Each device/emulator has unique `ANDROID_ID`
- Prevents profile conflicts when using multiple emulators
- Persistent across app reinstalls (usually)

**2. Firestore Offline Persistence:**
- Firestore caches data locally
- Writes are queued when offline
- Automatic sync when connection restored

**3. Migration Path:**
```
User starts offline → Uses "offline_user_abc123"
    ↓
Internet available
    ↓
signInAnonymously() succeeds → Returns "kX9mPqR7sNTg2hWvL4Yz"
    ↓
App now uses Firebase UID
    ↓
(Old offline data remains in Firestore cache)
```

### Firestore Sync Behavior

**When Offline:**
```kotlin
// Write operations are queued
firestore.collection("users")
    .document(userId)
    .collection("medications")
    .add(medication)
    .await()  // Returns immediately with pending write
```

**When Online:**
- Firestore uploads queued writes
- Downloads server changes
- Resolves conflicts (last-write-wins)
- Fires snapshot listeners with updated data

**Offline Capabilities:**
- ✅ Add medications
- ✅ Mark doses taken/missed
- ✅ View local data
- ✅ Calculate statistics (from cache)
- ❌ Send FCM notifications (requires internet)
- ❌ Real-time sync with caregivers

---

## State Management

### Architecture: MVVM + StateFlow + Repository

```
┌─────────────────────────────────────┐
│         Compose UI Layer            │
│  @Composable HomeScreen()           │
│  val state by viewModel             │
│      .uiState.collectAsState()      │
└──────────────┬──────────────────────┘
               │ Observes StateFlow
               │
┌──────────────▼──────────────────────┐
│          ViewModel Layer            │
│  class HomeViewModel(repo) {        │
│    private val _uiState =           │
│      MutableStateFlow(HomeUiState())│
│    val uiState: StateFlow =         │
│      _uiState.asStateFlow()         │
│  }                                  │
└──────────────┬──────────────────────┘
               │ Calls methods
               │
┌──────────────▼──────────────────────┐
│        Repository Layer             │
│  class FirebaseMedicationRepo {     │
│    val medications: Flow<List>      │
│    suspend fun addMedication()      │
│  }                                  │
└──────────────┬──────────────────────┘
               │ Uses
               │
┌──────────────▼──────────────────────┐
│         Data Sources                │
│  - Firebase Firestore               │
│  - Firebase Auth                    │
│  - Firebase Messaging               │
└─────────────────────────────────────┘
```

### StateFlow Pattern

**Why StateFlow over LiveData?**
- Coroutine-native (no lifecycle dependencies)
- Type-safe with Kotlin Flow operators
- Better integration with Compose
- Explicit initial value required

**HomeViewModel Example:**
```kotlin
class HomeViewModel(
    private val repository: FirebaseMedicationRepository
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Data from repository
    val medications: StateFlow<List<Medication>> = repository.medications
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        loadTodayDoses()
        startCountdownTimer()
    }

    // Actions
    fun markDoseTaken(medId: String, time: String) {
        viewModelScope.launch {
            repository.markDoseTaken(medId, LocalDate.now(), time)
            _uiState.update { it.copy(
                showSuccess = true,
                successMessage = "Dose marked as taken"
            )}
        }
    }
}
```

**UI State Data Class:**
```kotlin
data class HomeUiState(
    val isLoading: Boolean = false,
    val todayDoses: List<DoseInfo> = emptyList(),
    val nextDoseCountdown: String? = null,
    val weeklyAdherence: Double = 0.0,
    val currentStreak: Int = 0,
    val showSuccess: Boolean = false,
    val successMessage: String = "",
    val error: String? = null
)
```

### Repository Pattern

**FirebaseMedicationRepository.kt (814 lines):**

**Responsibilities:**
1. Abstracts Firestore operations
2. Converts Firestore DTOs to domain models
3. Handles offline/online state
4. Provides Flow-based reactive data
5. Centralizes business logic

**Key Methods:**

```kotlin
interface MedicationRepository {
    // Observables
    val medications: Flow<List<Medication>>
    val doseEvents: Flow<List<DoseEvent>>

    // User Profile
    suspend fun getCurrentUserProfile(): UserProfile?
    suspend fun setUserProfile(profile: UserProfile)

    // Medications
    suspend fun addMedication(medication: Medication): Result<String>
    suspend fun updateMedication(medication: Medication): Result<Unit>
    suspend fun deleteMedication(medId: String): Result<Unit>

    // Dose Events
    suspend fun markDoseTaken(medId: String, date: LocalDate, time: String)
    suspend fun markDoseMissed(medId: String, date: LocalDate, time: String)
    suspend fun undoDose(medId: String, date: LocalDate, time: String)

    // Statistics
    suspend fun calculateWeeklyAdherence(): Double
    suspend fun calculateMonthlyAdherence(): Double
    suspend fun calculateStreak(): Int
    suspend fun getProblematicMedications(): List<Medication>

    // Caregiver
    suspend fun getCaregiverPatients(): Flow<List<PatientInfo>>
    suspend fun getMedicationsForPatientByPin(pin: String): Flow<List<Medication>>
    suspend fun sendNotification(pin: String, title: String, body: String)
}
```

**Flow-Based Data:**
```kotlin
override val medications: Flow<List<Medication>> = callbackFlow {
    val userId = authManager.getCurrentUserId() ?: return@callbackFlow

    val listener = firestore.collection("users")
        .document(userId)
        .collection("medications")
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val meds = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject<FirestoreMedication>()?.toDomain()
            } ?: emptyList()

            trySend(meds)
        }

    awaitClose { listener.remove() }
}
```

### Real-Time Updates with Flow Extensions

**FirestoreExtensions.kt:**
```kotlin
fun <T : Any> Query.asFlow(mapper: (DocumentSnapshot) -> T?): Flow<List<T>> =
    callbackFlow {
        val listener = addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val items = snapshot?.documents?.mapNotNull(mapper) ?: emptyList()
            trySend(items)
        }

        awaitClose { listener.remove() }
    }
```

**Usage:**
```kotlin
firestore.collection("users")
    .document(userId)
    .collection("doseEvents")
    .asFlow { it.toObject<FirestoreDoseEvent>()?.toDomain() }
    .collect { events ->
        // UI updates automatically
    }
```

---

## Compose UI & Material 3

### Theme System

**MedicalAdherenceTheme.kt:**
```kotlin
@Composable
fun MedicalAdherenceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    highContrastMode: Boolean = false,
    dynamicColor: Boolean = true,        // Material You (Android 12+)
    fontScale: Float = 1.0f,             // Accessibility
    content: @Composable () -> Unit
) {
    // Choose color scheme
    val colorScheme = when {
        highContrastMode && darkTheme -> HighContrastDarkColorScheme
        highContrastMode && !darkTheme -> HighContrastLightColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Scale typography
    val scaledTypography = Typography.copy(
        displayLarge = Typography.displayLarge.copy(
            fontSize = Typography.displayLarge.fontSize * fontScale
        ),
        // ... scale all text styles
    )

    CompositionLocalProvider(LocalFontScale provides fontScale) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = scaledTypography,
            content = content
        )
    }
}
```

### Color Palettes

**Color.kt:**
```kotlin
// Light Theme (Calm Blue)
val CalmBlue40 = Color(0xFF4A90E2)       // Primary
val CalmBlueGrey40 = Color(0xFF607D8B)   // Secondary
val CalmAccent40 = Color(0xFF00BCD4)     // Tertiary

// Dark Theme
val CalmBlue80 = Color(0xFF90CAF9)
val CalmBlueGrey80 = Color(0xFFB0BEC5)
val CalmAccent80 = Color(0xFF4DD0E1)

// High Contrast (Accessibility)
val HighContrastPrimary = Color(0xFF0D47A1)
val HighContrastOnPrimary = Color(0xFFFFFFFF)

val LightColorScheme = lightColorScheme(
    primary = CalmBlue40,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBBDEFB),
    secondary = CalmBlueGrey40,
    tertiary = CalmAccent40,
    background = Color(0xFFFAFAFA),
    surface = Color.White,
    error = Color(0xFFD32F2F)
)
```

### Accessibility Features

**1. Font Scaling:**
```kotlin
@Composable
fun DoseCard(medication: Medication) {
    val fontScale = LocalFontScale.current

    Text(
        text = medication.name,
        fontSize = 20.sp * fontScale,  // Scales with user preference
        fontWeight = FontWeight.Bold
    )
}
```

**2. High Contrast Mode:**
- Increased color contrast ratios (WCAG AAA compliance)
- Bolder borders and dividers
- Higher emphasis on interactive elements

**3. Touch Targets:**
```kotlin
Button(
    onClick = { /* ... */ },
    modifier = Modifier
        .heightIn(min = 48.dp)  // Minimum touch target size
        .widthIn(min = 48.dp)
) {
    Text("Mark Taken")
}
```

### Recomposition Optimization

**1. State Hoisting:**
```kotlin
@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    HomeContent(
        uiState = uiState,
        onDoseTaken = viewModel::markDoseTaken  // Pass callbacks up
    )
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    onDoseTaken: (String, String) -> Unit
) {
    // Stateless composable - easier to test and preview
}
```

**2. Remember & DerivedStateOf:**
```kotlin
@Composable
fun DoseList(doses: List<DoseInfo>) {
    // Only recomputes when doses change
    val sortedDoses = remember(doses) {
        doses.sortedBy { it.scheduledTime }
    }

    // Recomputes only when result would change
    val hasOverdueDoses = remember {
        derivedStateOf {
            doses.any { it.isOverdue }
        }
    }
}
```

**3. Keys for LazyColumn:**
```kotlin
LazyColumn {
    items(
        items = medications,
        key = { it.id }  // Prevents unnecessary recomposition
    ) { medication ->
        MedicationCard(medication)
    }
}
```

### Material 3 Components

**Used Throughout App:**
- `Card` with `ElevatedCard` variant
- `FloatingActionButton` (FAB)
- `TopAppBar` with `CenterAlignedTopAppBar`
- `NavigationBar` with `NavigationBarItem`
- `AlertDialog` for confirmations
- `TextField` with `OutlinedTextField` variant
- `Button` with `FilledTonalButton`, `OutlinedButton` variants
- `IconButton` and `IconToggleButton`
- `LinearProgressIndicator` for adherence
- `CircularProgressIndicator` for loading

**Material You (Dynamic Colors):**
```kotlin
// Automatically adapts to user's wallpaper (Android 12+)
dynamicLightColorScheme(LocalContext.current)
```

---

## Key Dependencies

**Firebase (BOM v32.7.0):**
```gradle
implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
implementation("com.google.firebase:firebase-firestore-ktx")
implementation("com.google.firebase:firebase-auth-ktx")
implementation("com.google.firebase:firebase-messaging:23.4.0")
```

**Coroutines:**
```gradle
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
```

**Compose & Material 3:**
```gradle
implementation(platform("androidx.compose:compose-bom:2024.01.00"))
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.material:material-icons-extended")
implementation("androidx.compose.ui:ui-tooling-preview")
implementation("androidx.navigation:navigation-compose:2.7.6")
```

**Lifecycle & ViewModel:**
```gradle
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
```

**WorkManager (Local Notifications):**
```gradle
implementation("androidx.work:work-runtime-ktx:2.9.0")
```

**QR Code Scanning:**
```gradle
implementation("com.google.zxing:core:3.5.2")
implementation("com.journeyapps:zxing-android-embedded:4.3.0")
```

**HTTP Client (Local Server):**
```gradle
implementation("com.squareup.okhttp3:okhttp:4.12.0")
```

**JSON Parsing:**
```gradle
implementation("com.google.code.gson:gson:2.10.1")
```

---

## Constants & Configuration

**Constants.kt:**
```kotlin
object Constants {
    // FCM Configuration
    const val USE_LOCAL_SERVER = true
    const val USE_CLOUD_FUNCTIONS = false
    const val FCM_SERVER_KEY = "YOUR_KEY_HERE"  // Only for direct API

    // Adherence Thresholds
    const val ADHERENCE_EXCELLENT = 90       // Green zone
    const val ADHERENCE_GOOD = 80            // Good zone
    const val ADHERENCE_FAIR = 75
    const val ADHERENCE_OKAY = 50
    const val ADHERENCE_PROBLEMATIC = 70     // Warning threshold
    const val ADHERENCE_WARNING = 30         // Red zone

    // Dose Settings
    const val DOSE_WINDOW_MINUTES = 30       // ±30 min flexibility
    const val SNOOZE_DURATION_MINUTES = 15

    // Statistics
    const val STREAK_LOOKBACK_DAYS = 90      // Max streak calculation
    const val RECENT_MISSED_DOSES_LIMIT = 10 // Show recent 10 missed
    const val TREND_THRESHOLD_PERCENT = 10   // >10% change = trend

    // UI
    const val STATEFLOW_TIMEOUT_MS = 5000L   // 5s timeout for StateFlow
    const val COUNTDOWN_UPDATE_INTERVAL = 1000L // 1s countdown updates

    // Notifications
    const val NOTIFICATION_CHANNEL_ID = "medication_reminders"
    const val NOTIFICATION_CHANNEL_NAME = "Medication Reminders"
}
```

---

## Unique Technical Patterns

### 1. Dual-Mode ViewModel

**CaretakerViewModel supports two modes:**
```kotlin
class CaretakerViewModel(
    private val repository: FirebaseMedicationRepository,
    private val patientPin: String? = null  // null = current user mode
) : ViewModel() {

    val medications: StateFlow<List<Medication>> =
        if (patientPin != null) {
            // Caregiver mode: Monitor specific patient
            repository.getMedicationsForPatientByPin(patientPin)
        } else {
            // Current user mode: Monitor self
            repository.medications
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
```

**Use Cases:**
- Patient monitoring their own medications
- Caregiver monitoring a patient via PIN

### 2. Composite Key Strategy

**Prevents duplicate dose events:**
```kotlin
suspend fun markDoseTaken(medId: String, date: LocalDate, time: String) {
    val compositeId = "${medId}_${date}_$time"

    firestore.collection("users")
        .document(userId)
        .collection("doseEvents")
        .document(compositeId)  // Idempotent writes
        .set(DoseEvent(medId, date.toString(), time, taken = true))
}
```

### 3. Real-Time Countdown Timer

**Updates every second:**
```kotlin
private fun startCountdownTimer() {
    viewModelScope.launch {
        while (isActive) {
            val nextDose = getNextScheduledDose()
            _uiState.update { it.copy(
                nextDoseCountdown = calculateCountdown(nextDose)
            )}
            delay(1000)  // Update every second
        }
    }
}

private fun calculateCountdown(dose: DoseInfo?): String {
    if (dose == null) return "No upcoming doses"

    val now = LocalDateTime.now()
    val target = LocalDateTime.of(dose.date, dose.time)
    val duration = Duration.between(now, target)

    return when {
        duration.isNegative -> "Overdue by ${formatDuration(duration.abs())}"
        duration.toHours() > 24 -> "In ${duration.toDays()} days"
        else -> "In ${formatDuration(duration)}"
    }
}
```

### 4. Batch Deletion with Cascade

**Delete medication + all dose events:**
```kotlin
suspend fun deleteMedication(medId: String): Result<Unit> = withContext(Dispatchers.IO) {
    try {
        val batch = firestore.batch()

        // Delete medication document
        val medRef = firestore.collection("users")
            .document(userId)
            .collection("medications")
            .document(medId)
        batch.delete(medRef)

        // Delete all associated dose events
        val events = firestore.collection("users")
            .document(userId)
            .collection("doseEvents")
            .whereEqualTo("medId", medId)
            .get()
            .await()

        events.documents.forEach { batch.delete(it.reference) }

        // Atomic commit
        batch.commit().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

### 5. Pull-to-Refresh Pattern

**All ViewModels implement:**
```kotlin
fun refresh() {
    viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }

        // Reload data
        repository.refreshData()

        _uiState.update { it.copy(isLoading = false) }
    }
}
```

**UI Integration:**
```kotlin
val pullRefreshState = rememberPullRefreshState(
    refreshing = uiState.isLoading,
    onRefresh = { viewModel.refresh() }
)
```

---

## Performance Considerations

### 1. StateFlow Timeout

**Prevents memory leaks:**
```kotlin
repository.medications
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),  // Stop 5s after last subscriber
        initialValue = emptyList()
    )
```

### 2. Firestore Query Optimization

**Use indexes for complex queries:**
```kotlin
// Requires composite index in Firestore console
firestore.collection("users")
    .document(userId)
    .collection("doseEvents")
    .whereEqualTo("date", today)
    .whereEqualTo("taken", false)
    .orderBy("time")
```

### 3. Lazy Loading

**Load only visible data:**
```kotlin
LazyColumn {
    items(medications) { medication ->
        MedicationCard(medication)  // Only composes visible items
    }
}
```

---

## Security Considerations

**Current State:**
- No Firestore security rules (all data readable)
- FCM server key in app code (if using direct API)
- Anonymous authentication (no user verification)

**Production Recommendations:**
1. Implement Firestore security rules:
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read, write: if request.auth.uid == userId;

      match /medications/{medId} {
        allow read, write: if request.auth.uid == userId;
      }
    }

    match /caregiver_links/{linkId} {
      allow read: if request.auth.uid == resource.data.caregiverUserId
                  || request.auth.uid == resource.data.patientUserId;
    }
  }
}
```

2. Migrate to Cloud Functions or Local Server (remove API key from app)
3. Add patient consent system for caregiver access
4. Implement rate limiting for PIN queries

---

## Testing Strategy

**Unit Tests (Recommended):**
- ViewModel logic (StateFlow updates)
- Repository methods (mock Firestore)
- Adherence calculations
- Countdown timer logic

**Integration Tests:**
- Firestore read/write operations
- FCM token management
- Offline/online transitions

**UI Tests (Compose):**
```kotlin
@Test
fun markDoseAsTaken_updatesUI() {
    composeTestRule.setContent {
        HomeScreen(viewModel = fakeViewModel)
    }

    composeTestRule
        .onNodeWithText("Mark Taken")
        .performClick()

    composeTestRule
        .onNodeWithText("Dose marked as taken")
        .assertIsDisplayed()
}
```

---

## Summary

This technical implementation showcases:

✅ **Modern Android Architecture** (MVVM + StateFlow + Repository)
✅ **Real-Time Sync** (Firestore snapshot listeners)
✅ **Offline-First** (Device-specific IDs + Firestore caching)
✅ **Flexible FCM** (3 approaches for different environments)
✅ **Material 3 Design** (Dynamic colors + accessibility)
✅ **Reactive UI** (Compose + Flow integration)
✅ **Professional Patterns** (Composite keys, batch operations, dual-mode ViewModels)

**Key Files:**
- `FirebaseMedicationRepository.kt:1-814` - Core data layer
- `HomeViewModel.kt:1-249` - Patient UI logic
- `CaretakerViewModel.kt:1-602` - Caregiver monitoring
- `MyFirebaseMessagingService.kt:1-104` - FCM handling
- `Theme.kt:1-200` - Material 3 theming
- `Constants.kt:1-65` - App configuration

For more details, see the other documentation files in this series.
