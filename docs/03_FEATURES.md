# Feature Deep-Dive

This document explains each major feature of the Medical Adherence app, how it works technically, and provides demo talking points.

---

## Feature 1: Dual-Role System (Patient + Caregiver)

### What it does
Users can select their role when first opening the app: "I'm a Patient" or "I'm a Caregiver". The same app provides completely different interfaces and functionality based on this choice. Users can even switch roles or use both roles on different devices.

### Why it matters
Most medication apps target only patients OR only caregivers. This dual-role approach eliminates the need for two separate apps, simplifies development, and ensures consistent data models between roles.

### How it works technically

1. **Profile Selection Screen** (ProfileSelectionScreen.kt)
   - First screen shown to new users
   - Two large, accessible buttons for role selection
   - Stores selection in Firebase Firestore under `users/{userId}/profile`

2. **Firebase User Profile**
   ```kotlin
   data class FirestoreUserProfile(
       val userId: String,
       val role: String,      // "patient" or "caregiver"
       val name: String,
       val pin: String        // 6-digit PIN for pairing
   )
   ```

3. **Conditional Navigation**
   - Patient role ’ Navigate to HomeScreen (medication tracking)
   - Caregiver role ’ Navigate to CaregiverPatientsScreen (patient list)

4. **Role Persistence**
   - Profile stored in Firestore
   - Survives app reinstall (if using same device)
   - Can be changed by deleting profile in settings

### Key files
- **ProfileSelectionScreen.kt** - UI for role selection
- **ProfileSelectionViewModel.kt** - Saves role to Firebase
- **FirebaseMedicationRepository.kt:setUserProfile()** - Saves profile to Firestore
- **MainActivity.kt** - Determines initial navigation based on role

### Code snippet (simplified)
```kotlin
// ProfileSelectionViewModel.kt
fun selectRole(role: String, name: String) {
    viewModelScope.launch {
        val pin = generateRandomPin() // 6-digit number
        repository.setUserProfile(role, name, pin)

        // Navigate based on role
        if (role == "patient") {
            navigateTo("home")
        } else {
            navigateTo("caregiver_patients")
        }
    }
}

// ProfileSelectionScreen.kt
Button(onClick = { viewModel.selectRole("patient", userName) }) {
    Text("I'm a Patient")
}

Button(onClick = { viewModel.selectRole("caregiver", userName) }) {
    Text("I'm a Caregiver")
}
```

### Challenges overcome
- **Single codebase complexity**: Managing two different UI flows in one app required careful state management
- **Navigation logic**: Ensuring the right screen shows based on role without navigation loops
- **Data model consistency**: Same medication/dose models work for both roles

### Demo talking points
- "Notice how the same app serves both patients and caregivers"
- "This eliminates the need for two separate apps"
- "The role is stored in Firebase, so it persists across app launches"
- "Caregivers can monitor multiple patients from one device"

---

## Feature 2: PIN-Based Pairing System

### What it does
Patients generate a 6-digit PIN that caregivers can use to connect to their account. The PIN can be displayed as a QR code for easy scanning, or caregivers can manually enter the PIN.

### Why it matters
Traditional account linking requires complex OAuth flows, email verification, or friend requests. A simple PIN system is perfect for elderly users who struggle with technology.

### How it works technically

1. **PIN Generation**
   - Generated when user creates profile
   - 6-digit random number (100000-999999)
   - Stored in Firestore user profile

2. **QR Code Display**
   ```kotlin
   // HomeScreen.kt
   val qrBitmap = remember(pin) {
       QRCodeGenerator.generateQRCode("MED_ADHERENCE:$pin")
   }
   Image(bitmap = qrBitmap.asImageBitmap())
   ```

3. **Caregiver Pairing**
   - Caregiver enters PIN manually OR scans QR code
   - App queries Firestore: `users.where("pin", "==", enteredPin)`
   - If found, caregiver can access that patient's data

4. **Firestore Query**
   ```kotlin
   // FirebaseMedicationRepository.kt
   suspend fun getPatientDataByPin(pin: String): PatientDataExport? {
       val users = firestore.collection("users")
           .whereEqualTo("pin", pin)
           .get()
           .await()

       if (users.isEmpty) return null

       val patientId = users.documents[0].id
       return loadPatientData(patientId)
   }
   ```

5. **Real-Time Updates**
   - Once paired, caregiver sees real-time updates via Firestore listeners
   - No need to "refresh" - data syncs automatically

### Key files
- **HomeScreen.kt** - Displays PIN and QR code
- **CaretakerScreen.kt** - PIN entry dialog for caregivers
- **QRScannerScreen.kt** - QR code scanning functionality
- **FirebaseMedicationRepository.kt:getPatientDataByPin()** - Queries by PIN

### Code snippet (simplified)
```kotlin
// HomeScreen.kt - Patient shows PIN
Card {
    Column {
        Text("Your PIN: $pin", fontSize = 32.sp)
        Image(bitmap = generateQRCode(pin).asImageBitmap())
        Text("Share with caregiver")
    }
}

// CaretakerScreen.kt - Caregiver enters PIN
TextField(
    value = pinInput,
    onValueChange = { if (it.length <= 6) pinInput = it },
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
)

Button(onClick = { viewModel.loadPatientByPin(pinInput) }) {
    Text("Connect to Patient")
}

// CaretakerViewModel.kt
fun loadPatientByPin(pin: String) {
    viewModelScope.launch {
        val data = repository.getPatientDataByPin(pin)
        if (data != null) {
            _uiState.value = _uiState.value.copy(
                patientName = data.patientName,
                medicationCount = data.medications.size,
                weeklyAdherence = data.weeklyAdherence
            )
        } else {
            _errorMessage.value = "Patient not found"
        }
    }
}
```

### Challenges overcome
- **PIN collisions**: 6-digit PINs have 1 million combinations, collision chance is very low
- **Security**: PINs are not passwords - they're for linking, not authentication
- **QR code generation**: Integrated ZXing library for QR creation and scanning
- **Offline pairing**: Currently requires internet to query Firestore

### Demo talking points
- "Notice how simple the pairing is - just 6 digits"
- "Elderly patients can show the QR code to family members"
- "No email verification or complex setup needed"
- "Once paired, caregiver sees real-time updates"
- "For production, I'd add rate limiting to prevent PIN brute-force"

---

## Feature 3: FCM Push Notifications

### What it does
Caregivers can send push notifications to patient devices with custom reminder messages. Notifications appear as Android system notifications even when the app is closed.

### Why it matters
Caregivers can remind patients to take medication without phone calls or text messages. This provides peace of mind and increases adherence rates.

### How it works technically

The app implements **THREE different FCM approaches**:

#### Approach 1: Direct FCM API (Currently Used)
```kotlin
// FCMHelper.kt
suspend fun sendNotification(token: String, title: String, body: String) {
    val url = "https://fcm.googleapis.com/fcm/send"
    val json = JSONObject().apply {
        put("to", token)
        put("notification", JSONObject().apply {
            put("title", title)
            put("body", body)
        })
    }

    val request = Request.Builder()
        .url(url)
        .addHeader("Authorization", "key=$SERVER_KEY")
        .post(json.toString().toRequestBody())
        .build()

    okHttpClient.newCall(request).execute()
}
```

**Pros**: Works on Firebase Spark (free) plan
**Cons**: Server key in client code (security risk)

#### Approach 2: Cloud Functions (Production-Ready)
```javascript
// functions/index.js
exports.sendNotification = functions.https.onCall(async (data, context) => {
    const { patientId, message } = data;

    // Get patient's FCM token from Firestore
    const userDoc = await admin.firestore()
        .collection('users')
        .doc(patientId)
        .get();

    const token = userDoc.data().fcmToken;

    // Send notification
    await admin.messaging().send({
        token: token,
        notification: {
            title: "Medication Reminder",
            body: message
        }
    });
});
```

**Pros**: Secure (server key not exposed), serverless
**Cons**: Requires Firebase Blaze plan ($$$)

#### Approach 3: Local Development Server
```kotlin
// LocalNotificationServer.kt
class LocalNotificationServer {
    fun startServer() {
        val server = Ktor server on localhost:8080
        server.post("/send") {
            val token = call.receive<String>()
            FCMHelper.sendNotification(token)
        }
    }
}
```

**Pros**: Development/testing without deploying
**Cons**: Only works locally

### FCM Token Management
```kotlin
// MyFirebaseMessagingService.kt
override fun onNewToken(token: String) {
    // Save to Firestore when token refreshes
    viewModelScope.launch {
        repository.updateFCMToken(token)
    }
}

// FirebaseMedicationRepository.kt
suspend fun updateFCMToken(token: String) {
    getCurrentUserDoc()
        .update("fcmToken", token)
        .await()
}
```

### Notification Handling
```kotlin
// MyFirebaseMessagingService.kt
override fun onMessageReceived(message: RemoteMessage) {
    val notification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle(message.notification?.title)
        .setContentText(message.notification?.body)
        .setSmallIcon(R.drawable.ic_medication)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .build()

    notificationManager.notify(NOTIFICATION_ID, notification)
}
```

### Key files
- **MyFirebaseMessagingService.kt** - Receives FCM messages
- **FCMHelper.kt** - Direct API sender
- **LocalNotificationServer.kt** - Local development server
- **CaretakerViewModel.kt:sendReminder()** - Triggers notification send
- **functions/index.js** - Cloud Functions implementation

### Code snippet (simplified)
```kotlin
// CaretakerScreen.kt - Caregiver UI
Button(onClick = {
    viewModel.sendReminder("Don't forget your evening medication!")
}) {
    Icon(Icons.Default.Notifications)
    Text("Send Reminder")
}

// CaretakerViewModel.kt
fun sendReminder(message: String) {
    viewModelScope.launch {
        val patientToken = repository.getPatientFCMToken(patientPin)

        if (USE_CLOUD_FUNCTIONS) {
            // Approach 2: Cloud Functions
            firebaseFunctions.getHttpsCallable("sendNotification")
                .call(mapOf("token" to patientToken, "message" to message))
        } else {
            // Approach 1: Direct API
            FCMHelper.sendNotification(patientToken, "Reminder", message)
        }

        _message.value = "Reminder sent!"
    }
}

// Patient receives notification
// MyFirebaseMessagingService.kt
override fun onMessageReceived(message: RemoteMessage) {
    showNotification(
        title = message.notification?.title ?: "Medication Reminder",
        body = message.notification?.body ?: "Time to take your medication"
    )
}
```

### Challenges overcome
- **Free tier limitation**: Firebase Spark plan doesn't support Cloud Functions with external HTTP
- **Solution**: Implemented Direct API approach (less secure but works)
- **Token refresh**: FCM tokens can expire, so app updates token on every app start
- **Notification channels**: Android 8+ requires notification channels for proper display

### Demo talking points
- "I implemented three different FCM approaches to learn all options"
- "Currently using Direct API because Firebase free tier doesn't support Cloud Functions"
- "For production, I'd use Cloud Functions for better security"
- "Notice how notifications appear even when app is closed"
- "This is the industry-standard push notification system used by major apps"

---

## Feature 4: Offline-First Architecture

### What it does
The app works fully offline using device-specific IDs. All medication management, dose tracking, and statistics work without internet. When connectivity returns, data automatically syncs to Firebase.

### Why it matters
Many elderly users have unreliable internet or don't always have WiFi enabled. Medical adherence shouldn't depend on network availability.

### How it works technically

1. **Device-Specific User IDs**
   ```kotlin
   // FirebaseAuthManager.kt
   suspend fun ensureAuthenticated(): String {
       val currentUser = Firebase.auth.currentUser

       if (currentUser != null) {
           return currentUser.uid // Online: Use Firebase UID
       }

       // Offline: Generate consistent ID from Android device ID
       val androidId = Settings.Secure.getString(
           context.contentResolver,
           Settings.Secure.ANDROID_ID
       )
       return "offline_$androidId"
   }
   ```

2. **Firestore Offline Persistence**
   ```kotlin
   // MainActivity.kt
   Firebase.firestore.firestoreSettings = firestoreSettings {
       isPersistenceEnabled = true
       cacheSizeBytes = FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED
   }
   ```

3. **Automatic Sync**
   - Firestore automatically caches writes when offline
   - When connection restored, pending writes sync automatically
   - Real-time listeners resume and emit updated data

4. **What Works Offline**
   -  Add/edit/delete medications
   -  Mark doses as taken/missed
   -  View adherence statistics
   -  All patient features
   - L Caregiver pairing (requires Firestore query)
   - L Push notifications (requires FCM)

### Key files
- **FirebaseAuthManager.kt** - Handles offline/online user IDs
- **MainActivity.kt** - Enables Firestore offline persistence
- **FirebaseMedicationRepository.kt** - All methods work offline via Firestore cache

### Code snippet (simplified)
```kotlin
// FirebaseAuthManager.kt
suspend fun ensureAuthenticated(): String {
    // Try Firebase Auth first
    val firebaseUser = auth.currentUser
    if (firebaseUser != null) {
        return firebaseUser.uid
    }

    // Try anonymous sign-in
    return try {
        val result = auth.signInAnonymously().await()
        result.user!!.uid
    } catch (e: Exception) {
        // Offline fallback: Use Android device ID
        val androidId = getAndroidDeviceId()
        "offline_$androidId"
    }
}

// FirebaseMedicationRepository.kt
suspend fun addMedication(medication: Medication) {
    // This works offline - Firestore queues the write
    getCurrentUserDoc()
        .collection("medications")
        .add(medication.toFirestoreDto())
        .await() // Completes immediately when offline
}

// MainActivity.kt - Enable offline support
Firebase.firestore.apply {
    firestoreSettings = firestoreSettings {
        isPersistenceEnabled = true
        cacheSizeBytes = CACHE_SIZE_UNLIMITED
    }
}
```

### Challenges overcome
- **Unique offline IDs**: Used Android device ID to ensure consistent user identification
- **Firestore limitations**: Some queries don't work offline (e.g., `.whereEqualTo()` for PIN lookup)
- **Data conflicts**: Firestore automatically resolves conflicts with last-write-wins
- **Testing**: Had to test with airplane mode to verify offline functionality

### Demo talking points
- "Watch what happens when I turn off WiFi - the app still works perfectly"
- "I can add medications, mark doses, view statistics - all offline"
- "When I reconnect, data automatically syncs to Firebase"
- "This is crucial for elderly users with unreliable internet"
- "Firestore provides offline caching out of the box"

---

## Feature 5: Real-Time Synchronization

### What it does
When a patient marks a dose as taken, caregivers see the update immediately (within 2 seconds) without refreshing. All data syncs in real-time across all devices.

### Why it matters
Caregivers want immediate peace of mind knowing their loved one took medication. Traditional apps require manual refresh or polling, which is slow and battery-draining.

### How it works technically

1. **Firestore Real-Time Listeners**
   ```kotlin
   val medications: Flow<List<Medication>> = callbackFlow {
       val listener = firestore
           .collection("users/$userId/medications")
           .addSnapshotListener { snapshot, error ->
               if (error != null) {
                   close(error)
                   return@addSnapshotListener
               }

               val meds = snapshot?.documents?.mapNotNull {
                   it.toObject(Medication::class.java)
               } ?: emptyList()

               trySend(meds) // Emit new data to Flow
           }

       awaitClose { listener.remove() } // Cleanup
   }
   ```

2. **StateFlow Propagation**
   ```kotlin
   // CaretakerViewModel.kt
   init {
       viewModelScope.launch {
           repository.getPatientDataByPin(patientPin).collect { data ->
               _uiState.value = _uiState.value.copy(
                   weeklyAdherence = data.weeklyAdherence,
                   todayDoses = data.todayDoses,
                   lastUpdated = System.currentTimeMillis()
               )
           }
       }
   }
   ```

3. **Compose Recomposition**
   ```kotlin
   // CaretakerScreen.kt
   val uiState by viewModel.uiState.collectAsState()

   // UI automatically updates when uiState changes
   Text("Weekly Adherence: ${uiState.weeklyAdherence}%")
   ```

4. **Flow Chain**
   ```
   Patient marks dose ’ Firestore write
                      ’ Firestore triggers listener
                      ’ Repository emits new Flow value
                      ’ ViewModel updates StateFlow
                      ’ Composable recomposes
                      ’ Caregiver sees update
   ```

### Key files
- **FirebaseMedicationRepository.kt** - Real-time listeners via `callbackFlow`
- **CaretakerViewModel.kt** - Collects repository Flows
- **CaretakerScreen.kt** - Observes ViewModel StateFlow

### Code snippet (simplified)
```kotlin
// FirebaseMedicationRepository.kt
fun getPatientMedications(patientId: String): Flow<List<Medication>> =
    callbackFlow {
        val listener = firestore
            .collection("users/$patientId/medications")
            .addSnapshotListener { snapshot, error ->
                val meds = snapshot?.toObjects(Medication::class.java) ?: emptyList()
                trySend(meds) // Sends to Flow whenever Firestore changes
            }

        awaitClose { listener.remove() }
    }

// CaretakerViewModel.kt
init {
    viewModelScope.launch {
        repository.getPatientMedications(patientId).collect { medications ->
            _uiState.value = _uiState.value.copy(
                medicationCount = medications.size,
                lastUpdated = System.currentTimeMillis()
            )
        }
    }
}

// CaretakerScreen.kt
val uiState by viewModel.uiState.collectAsState()

Card {
    Text("Medications: ${uiState.medicationCount}")
    Text("Last updated: ${formatTime(uiState.lastUpdated)}")
}
```

### Challenges overcome
- **Memory leaks**: Used `awaitClose` to properly remove Firestore listeners
- **Lifecycle awareness**: StateFlow automatically stops when screen not visible
- **Performance**: Firestore sends only changed documents, not entire collection

### Demo talking points
- "Let me show you real-time sync - I'll open the patient app on device 1"
- "And the caregiver dashboard on device 2"
- "Watch when I mark this dose as taken on the patient device..."
- "The caregiver screen updates immediately - no refresh needed!"
- "This uses Firebase real-time listeners, not polling"
- "Updates typically arrive in under 2 seconds"

---

## Feature 6: Medication Scheduling

### What it does
Patients can add medications with flexible scheduling: daily, specific days of the week, every X days, or as-needed. Each medication can have multiple doses per day at specific times.

### Why it matters
Real medication schedules are complex - not every med is daily. Some are weekly, some are every other day, some are only on weekends. The app must handle all these patterns.

### How it works technically

1. **Medication Frequency Enum**
   ```kotlin
   enum class MedicationFrequency {
       DAILY,           // Every day
       SPECIFIC_DAYS,   // Mon, Wed, Fri, etc.
       INTERVAL_DAYS,   // Every X days
       AS_NEEDED        // PRN - no schedule
   }
   ```

2. **Medication Data Model**
   ```kotlin
   data class Medication(
       val id: String,
       val name: String,
       val dosage: String,
       val times: List<String>,        // ["08:00", "20:00"]
       val frequency: MedicationFrequency,
       val specificDays: List<Int>?,   // [1,3,5] = Mon/Wed/Fri
       val intervalDays: Int?,         // 3 = every 3 days
       val startDate: LocalDate
   )
   ```

3. **Dose Calculation**
   ```kotlin
   // FirebaseMedicationRepository.kt
   suspend fun getTodayDoses(): List<Triple<Medication, String, Boolean?>> {
       val today = LocalDate.now()
       val medications = medications.first()

       return medications.flatMap { med ->
           if (isMedicationScheduledForToday(med, today)) {
               med.times.map { time ->
                   val taken = getDoseEvent(med.id, today, time)?.taken
                   Triple(med, time, taken)
               }
           } else {
               emptyList()
           }
       }
   }

   private fun isMedicationScheduledForToday(med: Medication, date: LocalDate): Boolean {
       return when (med.frequency) {
           DAILY -> true

           SPECIFIC_DAYS -> {
               val dayOfWeek = date.dayOfWeek.value // 1=Mon, 7=Sun
               med.specificDays?.contains(dayOfWeek) ?: false
           }

           INTERVAL_DAYS -> {
               val daysSinceStart = ChronoUnit.DAYS.between(med.startDate, date)
               daysSinceStart % med.intervalDays == 0L
           }

           AS_NEEDED -> false // Don't show in daily list
       }
   }
   ```

4. **UI Form**
   ```kotlin
   // AddEditMedicationScreen.kt
   when (frequency) {
       DAILY -> { /* No additional fields */ }

       SPECIFIC_DAYS -> {
           DaysOfWeekSelector(
               selected = specificDays,
               onSelect = { specificDays = it }
           )
       }

       INTERVAL_DAYS -> {
           TextField(
               value = intervalDays.toString(),
               label = { Text("Every X days") },
               keyboardType = KeyboardType.Number
           )
       }
   }
   ```

### Key files
- **AddEditMedicationScreen.kt** - Medication form UI
- **AddMedicationViewModel.kt** - Form validation and saving
- **Medication.kt** - Data model
- **FirebaseMedicationRepository.kt:getTodayDoses()** - Determines today's schedule

### Code snippet (simplified)
```kotlin
// AddMedicationViewModel.kt
fun saveMedication() {
    val medication = Medication(
        id = UUID.randomUUID().toString(),
        name = name,
        dosage = dosage,
        times = times, // ["08:00", "14:00", "20:00"]
        frequency = frequency,
        specificDays = if (frequency == SPECIFIC_DAYS) selectedDays else null,
        intervalDays = if (frequency == INTERVAL_DAYS) interval else null,
        startDate = LocalDate.now()
    )

    viewModelScope.launch {
        repository.addMedication(medication)
        navigateBack()
    }
}

// HomeViewModel.kt - Load today's doses
fun loadTodayDoses() {
    viewModelScope.launch {
        val doses = repository.getTodayDoses() // Only meds scheduled for today
        _uiState.value = _uiState.value.copy(todayDoses = doses)
    }
}
```

### Challenges overcome
- **Complex date math**: Used Java 8 Time API for reliable date calculations
- **Data model flexibility**: Supporting multiple frequency types in one model
- **UI/UX**: Making the form simple despite complex logic underneath

### Demo talking points
- "Let me add a medication with a complex schedule"
- "This one is only Monday, Wednesday, Friday"
- "Notice how the UI only shows it on those days"
- "I can also do 'every 3 days' for medications like that"
- "The app automatically calculates which doses are due today"

---

## Feature 7: Adherence Tracking & Statistics

### What it does
Tracks which doses were taken/missed and calculates adherence percentages, streak days, daily breakdowns, and identifies problematic medications.

### Why it matters
Adherence metrics help patients stay motivated and help caregivers identify issues early. Visual feedback encourages consistent behavior.

### How it works technically

1. **Dose Event Recording**
   ```kotlin
   data class DoseEvent(
       val id: String,
       val medId: String,
       val date: LocalDate,
       val time: String,
       val taken: Boolean,      // true = taken, false = missed
       val timestamp: Long      // When recorded
   )
   ```

2. **Adherence Calculation**
   ```kotlin
   // FirebaseMedicationRepository.kt
   suspend fun calculateWeeklyAdherence(): Int {
       val today = LocalDate.now()
       val weekStart = today.minusDays(6) // Last 7 days

       val allScheduledDoses = (0..6).sumOf { daysAgo ->
           val date = today.minusDays(daysAgo.toLong())
           getTodayDoses(date).size // Scheduled doses for that day
       }

       val takenDoses = doseEvents
           .filter { it.date >= weekStart && it.date <= today }
           .count { it.taken }

       return if (allScheduledDoses > 0) {
           ((takenDoses.toFloat() / allScheduledDoses) * 100).toInt()
       } else {
           100 // No meds = 100% adherence
       }
   }
   ```

3. **Streak Calculation**
   ```kotlin
   suspend fun calculateStreak(): Int {
       var streak = 0
       var date = LocalDate.now()

       while (true) {
           val dayAdherence = calculateDayAdherence(date)

           if (dayAdherence < 100) break // Missed dose, streak broken

           streak++
           date = date.minusDays(1)

           // Stop at 365 days max
           if (streak >= 365) break
       }

       return streak
   }

   private suspend fun calculateDayAdherence(date: LocalDate): Int {
       val scheduled = getTodayDoses(date).size
       if (scheduled == 0) return 100 // No meds = perfect day

       val taken = doseEvents
           .filter { it.date == date && it.taken }
           .size

       return ((taken.toFloat() / scheduled) * 100).toInt()
   }
   ```

4. **Problematic Medication Detection**
   ```kotlin
   data class MedicationAdherence(
       val medication: Medication,
       val adherencePercent: Int,
       val totalDoses: Int,
       val takenDoses: Int,
       val missedDoses: Int
   )

   suspend fun getProblematicMedications(): List<MedicationAdherence> {
       val meds = medications.first()

       return meds.map { med ->
           val events = doseEvents.filter { it.medId == med.id }
           val taken = events.count { it.taken }
           val total = events.size

           MedicationAdherence(
               medication = med,
               adherencePercent = if (total > 0) (taken * 100 / total) else 100,
               totalDoses = total,
               takenDoses = taken,
               missedDoses = total - taken
           )
       }.filter { it.adherencePercent < 80 } // Less than 80% is problematic
         .sortedBy { it.adherencePercent }
   }
   ```

### Key files
- **StatsScreen.kt** - Displays adherence UI
- **StatsViewModel.kt** - Calculates statistics
- **CaretakerScreen.kt** - Shows caregiver analytics
- **FirebaseMedicationRepository.kt** - Core calculation logic

### Code snippet (simplified)
```kotlin
// StatsViewModel.kt
init {
    viewModelScope.launch {
        val weeklyAdherence = repository.calculateWeeklyAdherence()
        val monthlyAdherence = repository.calculateMonthlyAdherence()
        val streak = repository.calculateStreak()
        val dailyBreakdown = repository.getDailyAdherenceBreakdown()

        _uiState.value = StatsUiState(
            weeklyAdherence = weeklyAdherence,
            monthlyAdherence = monthlyAdherence,
            streakDays = streak,
            dailyBreakdown = dailyBreakdown
        )
    }
}

// StatsScreen.kt
Card {
    Text("Weekly Adherence", style = MaterialTheme.typography.headlineSmall)
    LinearProgressIndicator(
        progress = uiState.weeklyAdherence / 100f,
        color = if (uiState.weeklyAdherence >= 80) Green else Red
    )
    Text("${uiState.weeklyAdherence}%", fontSize = 48.sp)
}

Card {
    Icon(Icons.Default.LocalFireDepartment)
    Text("${uiState.streakDays} Day Streak!")
}
```

### Challenges overcome
- **Performance**: Calculating adherence requires querying many dose events - used Firestore indexing
- **Edge cases**: Handling days with no medications scheduled
- **Streak logic**: Defining what counts as "maintaining streak" (100% adherence required)

### Demo talking points
- "This shows weekly adherence percentage - currently 92%"
- "The green progress bar provides positive visual feedback"
- "Notice the streak counter - gamification encourages consistency"
- "Daily breakdown shows which days need improvement"
- "Caregivers can see problematic medications that are frequently missed"

---

## Feature 8: Caregiver Dashboard

### What it does
Provides caregivers with a comprehensive view of patient medication adherence, including real-time dose status, adherence trends, problematic medications, and the ability to send push notifications.

### Why it matters
Family caregivers need visibility into their loved one's medication habits without being physically present. The dashboard provides peace of mind and enables early intervention.

### How it works technically

1. **Caregiver UI State** (603 lines of logic)
   ```kotlin
   data class CaretakerUiState(
       val patientName: String,
       val patientPin: String,
       val medicationCount: Int,
       val weeklyAdherence: Int,
       val monthlyAdherence: Int,
       val currentStreak: Int,
       val longestStreak: Int,
       val todayDoses: List<TodayDoseInfo>,
       val recentMissedDoses: List<MissedDoseInfo>,
       val problematicMedications: List<MedicationAdherence>,
       val adherenceTrend: String, // "Improving", "Declining", "Stable"
       val lastUpdated: Long
   )
   ```

2. **Patient Data Export**
   ```kotlin
   // FirebaseMedicationRepository.kt
   suspend fun getPatientDataByPin(pin: String): PatientDataExport? {
       // 1. Find patient by PIN
       val userQuery = firestore.collection("users")
           .whereEqualTo("pin", pin)
           .get()
           .await()

       if (userQuery.isEmpty) return null
       val patientId = userQuery.documents[0].id

       // 2. Load all patient data
       val profile = getProfile(patientId)
       val medications = getMedications(patientId)
       val doseEvents = getDoseEvents(patientId)

       // 3. Calculate statistics
       return PatientDataExport(
           patientId = patientId,
           patientName = profile.name,
           medications = medications,
           weeklyAdherence = calculateAdherence(doseEvents, 7),
           monthlyAdherence = calculateAdherence(doseEvents, 30),
           streakDays = calculateStreak(doseEvents),
           problematicMedications = identifyProblematic(medications, doseEvents)
       )
   }
   ```

3. **Trend Analysis**
   ```kotlin
   private fun calculateAdherenceTrend(doseEvents: List<DoseEvent>): String {
       val thisWeek = calculateWeekAdherence(0)  // Current week
       val lastWeek = calculateWeekAdherence(1)  // Previous week

       return when {
           thisWeek > lastWeek + 5 -> "Improving"
           thisWeek < lastWeek - 5 -> "Declining"
           else -> "Stable"
       }
   }
   ```

4. **Real-Time Dashboard Updates**
   ```kotlin
   // CaretakerViewModel.kt
   init {
       loadPatientData()
       startAutoRefresh() // Refresh every 30 seconds
   }

   private fun startAutoRefresh() {
       viewModelScope.launch {
           while (true) {
               delay(30_000) // 30 seconds
               loadPatientData()
           }
       }
   }
   ```

### Key files
- **CaretakerScreen.kt** - Full dashboard UI (1,090 lines)
- **CaretakerViewModel.kt** - Dashboard logic (603 lines)
- **PatientDataExport.kt** - DTO for patient data
- **FirebaseMedicationRepository.kt:getPatientDataByPin()** - Data aggregation

### Code snippet (simplified)
```kotlin
// CaretakerScreen.kt
LazyColumn {
    // Summary cards
    item {
        Row {
            StatCard(
                title = "Weekly Adherence",
                value = "${uiState.weeklyAdherence}%",
                color = if (uiState.weeklyAdherence >= 80) Green else Red
            )
            StatCard(
                title = "Current Streak",
                value = "${uiState.currentStreak} days",
                icon = Icons.Default.LocalFireDepartment
            )
        }
    }

    // Today's doses
    item {
        Card {
            Text("Today's Medications", style = MaterialTheme.typography.headlineSmall)
            uiState.todayDoses.forEach { dose ->
                DoseRow(
                    name = dose.medicationName,
                    time = dose.time,
                    status = when (dose.taken) {
                        true -> "Taken"
                        false -> "Missed"
                        null -> "Pending"
                    }
                )
            }
        }
    }

    // Problematic medications
    item {
        Card {
            Text("Needs Attention")
            uiState.problematicMedications.forEach { med ->
                Row {
                    Text(med.medication.name)
                    Text("${med.adherencePercent}%", color = Red)
                }
            }
        }
    }

    // Send notification button
    item {
        Button(onClick = { viewModel.sendReminder() }) {
            Icon(Icons.Default.Notifications)
            Text("Send Reminder")
        }
    }
}
```

### Challenges overcome
- **Complex UI**: 1,090 lines in CaretakerScreen.kt, needed careful organization
- **Data aggregation**: Combining multiple Firestore collections efficiently
- **Performance**: Caching patient data to avoid repeated queries
- **Real-time vs polling**: Balancing real-time listeners with battery life

### Demo talking points
- "This is the caregiver dashboard - notice all the insights"
- "Weekly and monthly adherence percentages at a glance"
- "Today's doses show what's taken, missed, or pending"
- "Problematic medications section helps identify issues early"
- "Trend analysis shows if adherence is improving or declining"
- "One-button notification sending for quick reminders"
- "All data updates in real-time without refresh"

---

## Summary

These 8 features demonstrate:
- **Full-stack mobile development** (UI + backend)
- **Firebase mastery** (Firestore, Auth, FCM)
- **Modern Android** (Jetpack Compose, MVVM, StateFlow)
- **User-centered design** (accessibility, offline support)
- **Real-world problem solving** (medication adherence)

Each feature has depth and complexity, showing professional-level implementation while remaining accessible for academic presentations.
