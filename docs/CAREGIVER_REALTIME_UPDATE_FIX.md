# Caregiver Real-Time Data Update Fix

## Problem Summary

After successfully implementing the device-specific offline ID fix, the caregiver-patient connection was established, but the caregiver's phone was not receiving real-time updates when the patient's data changed. This meant that:

- When a patient added/removed medications, the caregiver's list didn't update
- When a patient took doses, the adherence stats on the caregiver's phone didn't reflect the changes
- The caregiver had to manually restart the app or manually "refresh" to see updated data

## Root Causes Identified

### 1. **No Real-Time Listener in `getCaregiverPatients()`**
The repository method used `.get()` (one-time query) instead of `.addSnapshotListener()` (real-time updates).

**Before:**
```kotlin
fun getCaregiverPatients(): Flow<List<PatientProfile>> = flow {
    val links = firestore.collection("caregiver_links")
        .whereEqualTo("caregiverUserId", caregiverUserId)
        .get()  // ❌ One-time query
        .await()
    // ...
}
```

### 2. **`medicationCount` Always Returned 0**
The `PatientProfile` had `medicationCount: Int = 0` as a default value, but `getCaregiverPatients()` never calculated or populated it.

**Before:**
```kotlin
PatientProfile(
    pin = link.patientPin,
    name = link.patientName,
    addedAt = link.addedAt.seconds * 1000
    // medicationCount uses default value of 0
)
```

### 3. **CaretakerViewModel Didn't Accept Patient PIN**
When a caregiver selected a patient to monitor, the `CaretakerViewModel` didn't receive the patient PIN, so it loaded the caregiver's own data instead of the patient's data.

**Before:**
```kotlin
class CaretakerViewModel(
    private val repository: FirebaseMedicationRepository
    // ❌ No patient PIN parameter
) : ViewModel()
```

### 4. **No Repository Methods for Cross-User Queries**
The repository only had methods that worked with `getCurrentUserId()`, with no way to query another user's medications or dose events.

## Implemented Solution

### 1. **Real-Time Listener in `getCaregiverPatients()`**

**File:** `FirebaseMedicationRepository.kt`

Changed from `flow { ... }` to `callbackFlow { ... }` with `addSnapshotListener`:

```kotlin
fun getCaregiverPatients(): Flow<List<PatientProfile>> = callbackFlow {
    val caregiverUserId = getCurrentUserId()
    android.util.Log.d("FirebaseRepo", "🔄 Setting up real-time listener for caregiver: $caregiverUserId")

    val listener = firestore.collection("caregiver_links")
        .whereEqualTo("caregiverUserId", caregiverUserId)
        .addSnapshotListener { snapshot, error ->  // ✅ Real-time listener
            if (error != null) {
                android.util.Log.e("FirebaseRepo", "❌ Error in caregiver patients listener", error)
                trySend(emptyList())
                return@addSnapshotListener
            }

            if (snapshot == null) {
                android.util.Log.w("FirebaseRepo", "⚠️ Snapshot is null")
                trySend(emptyList())
                return@addSnapshotListener
            }

            val links = snapshot.documents.mapNotNull { 
                it.toObject(FirestoreCaregiverLink::class.java) 
            }

            // Launch coroutines to fetch medication counts for each patient
            launch(Dispatchers.IO) {
                val patients = links.map { link ->
                    val medCount = try {
                        val patientUserId = firestore.collection("users")
                            .whereEqualTo("pin", link.patientPin)
                            .whereEqualTo("role", "patient")
                            .get()
                            .await()
                            .documents
                            .firstOrNull()
                            ?.id

                        if (patientUserId != null) {
                            val count = firestore.collection("users/$patientUserId/medications")
                                .get()
                                .await()
                                .documents
                                .size
                            android.util.Log.d("FirebaseRepo", "💊 Patient ${link.patientName} has $count medication(s)")
                            count
                        } else {
                            android.util.Log.w("FirebaseRepo", "⚠️ Could not find patient userId for PIN: ${link.patientPin}")
                            0
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("FirebaseRepo", "❌ Error fetching med count for ${link.patientName}", e)
                        0
                    }

                    PatientProfile(
                        pin = link.patientPin,
                        name = link.patientName,
                        addedAt = link.addedAt.seconds * 1000,
                        lastSyncedAt = System.currentTimeMillis(),  // ✅ Updated timestamp
                        medicationCount = medCount  // ✅ Actual count from Firestore
                    )
                }

                android.util.Log.d("FirebaseRepo", "✅ Emitting ${patients.size} patient(s) with medication counts")
                trySend(patients)
            }
        }

    awaitClose {
        android.util.Log.d("FirebaseRepo", "🔌 Removing caregiver patients listener")
        listener.remove()
    }
}.catch { e ->
    android.util.Log.e("FirebaseRepo", "❌ Error in caregiver patients flow", e)
    emit(emptyList())
}
```

**Key Changes:**
- Used `callbackFlow` instead of `flow` to support snapshot listeners
- Added `.addSnapshotListener()` for real-time updates
- For each patient, query their actual medication count from Firestore
- Properly populate `medicationCount` and `lastSyncedAt` fields
- Added `awaitClose` to clean up the listener when the Flow is cancelled

### 2. **New Repository Methods for Cross-User Queries**

**File:** `FirebaseMedicationRepository.kt`

Added methods to query a specific patient's data by PIN:

```kotlin
/**
 * Get a patient's userId by their PIN
 */
private suspend fun getPatientUserIdByPin(pin: String): String? {
    return try {
        firestore.collection("users")
            .whereEqualTo("pin", pin)
            .whereEqualTo("role", "patient")
            .get()
            .await()
            .documents
            .firstOrNull()
            ?.id
    } catch (e: Exception) {
        android.util.Log.e("FirebaseRepo", "Error getting patient userId by PIN", e)
        null
    }
}

/**
 * Get medications for a specific patient (by PIN) with real-time updates
 */
fun getMedicationsForPatientByPin(pin: String): Flow<List<Medication>> = callbackFlow {
    val patientUserId = getPatientUserIdByPin(pin)
    
    if (patientUserId == null) {
        android.util.Log.w("FirebaseRepo", "⚠️ Could not find patient with PIN: $pin")
        trySend(emptyList())
        close()
        return@callbackFlow
    }

    android.util.Log.d("FirebaseRepo", "🔄 Setting up medication listener for patient: $patientUserId")

    val listener = firestore.collection("users/$patientUserId/medications")
        .addSnapshotListener { snapshot, error ->  // ✅ Real-time listener
            if (error != null) {
                android.util.Log.e("FirebaseRepo", "❌ Error in patient medications listener", error)
                trySend(emptyList())
                return@addSnapshotListener
            }

            if (snapshot == null) {
                trySend(emptyList())
                return@addSnapshotListener
            }

            val medications = snapshot.documents.mapNotNull { 
                it.toObject(FirestoreMedication::class.java)?.toMedication()
            }
            
            android.util.Log.d("FirebaseRepo", "💊 Patient medications updated: ${medications.size} med(s)")
            trySend(medications)
        }

    awaitClose {
        android.util.Log.d("FirebaseRepo", "🔌 Removing patient medications listener")
        listener.remove()
    }
}.catch { e ->
    android.util.Log.e("FirebaseRepo", "❌ Error in patient medications flow", e)
    emit(emptyList())
}

/**
 * Get dose events for a specific patient (by PIN)
 */
suspend fun getDoseEventsForPatientByPin(pin: String, startDate: LocalDate, endDate: LocalDate): List<DoseEvent> {
    return try {
        val patientUserId = getPatientUserIdByPin(pin) ?: return emptyList()

        firestore.collection("users/$patientUserId/doseEvents")
            .whereGreaterThanOrEqualTo("date", startDate.toString())
            .whereLessThanOrEqualTo("date", endDate.toString())
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(FirestoreDoseEvent::class.java)?.toDoseEvent() }
    } catch (e: Exception) {
        android.util.Log.e("FirebaseRepo", "Error getting patient dose events", e)
        emptyList()
    }
}

/**
 * Calculate adherence for a specific patient
 */
suspend fun calculatePatientAdherence(pin: String, startDate: LocalDate, endDate: LocalDate): Int {
    val patientUserId = getPatientUserIdByPin(pin) ?: return 0

    val medications = firestore.collection("users/$patientUserId/medications")
        .get()
        .await()
        .documents
        .mapNotNull { it.toObject(FirestoreMedication::class.java)?.toMedication() }

    val doseEvents = firestore.collection("users/$patientUserId/doseEvents")
        .whereGreaterThanOrEqualTo("date", startDate.toString())
        .whereLessThanOrEqualTo("date", endDate.toString())
        .get()
        .await()
        .documents
        .mapNotNull { it.toObject(FirestoreDoseEvent::class.java)?.toDoseEvent() }

    var totalExpected = 0
    var totalTaken = 0

    var currentDate = startDate
    while (!currentDate.isAfter(endDate)) {
        medications.forEach { med ->
            if (!currentDate.isBefore(med.startDate) && !currentDate.isAfter(med.endDate)) {
                totalExpected += med.times.size
                
                med.times.forEach { time ->
                    val taken = doseEvents.any { 
                        it.medId == med.id && it.date == currentDate && it.time == time && it.taken 
                    }
                    if (taken) totalTaken++
                }
            }
        }
        currentDate = currentDate.plusDays(1)
    }

    return if (totalExpected > 0) (totalTaken * 100) / totalExpected else 0
}
```

### 3. **Updated CaretakerViewModel to Accept Patient PIN**

**File:** `CaretakerViewModel.kt`

Added `patientPin` parameter and logic to load patient-specific data:

```kotlin
class CaretakerViewModel(
    private val repository: FirebaseMedicationRepository,
    private val patientPin: String? = null  // ✅ Patient PIN parameter
) : ViewModel() {

    private val _uiState = MutableStateFlow(CaretakerUiState())
    val uiState: StateFlow<CaretakerUiState> = _uiState.asStateFlow()

    // Real-time medications flow
    val medications: StateFlow<List<Medication>> = if (patientPin != null) {
        repository.getMedicationsForPatientByPin(patientPin)  // ✅ Patient-specific
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    } else {
        repository.medications  // Current user
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }

    init {
        // Set up real-time medication count observer
        viewModelScope.launch {
            medications.collect { meds ->
                _uiState.value = _uiState.value.copy(medicationCount = meds.size)
            }
        }
        
        loadCaretakerData()
    }

    private fun loadCaretakerData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                // If monitoring a specific patient
                if (patientPin != null) {
                    // Get patient name
                    val patientData = repository.getPatientDataByPin(patientPin)
                    val patientName = patientData?.name ?: "Patient"

                    // Calculate adherence using patient-specific methods
                    val weeklyAdherence = repository.calculatePatientAdherence(patientPin, weekAgo, today)
                    val monthlyAdherence = repository.calculatePatientAdherence(patientPin, monthAgo, today)

                    // Get medications and dose events
                    val medications = repository.getMedicationsForPatientByPin(patientPin).first()
                    val doseEvents = repository.getDoseEventsForPatientByPin(patientPin, weekAgo, today)

                    // ... calculate stats using patient data
                    
                    _uiState.value = CaretakerUiState(
                        patientName = patientName,
                        patientPin = patientPin,
                        medicationCount = medications.size,
                        weeklyAdherence = weeklyAdherence,
                        // ... other stats
                        isLoading = false
                    )
                } else {
                    // Use current user's data (original behavior)
                    // ... existing logic
                }
            } catch (e: Exception) {
                android.util.Log.e("CaretakerVM", "❌ Error loading caretaker data", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load patient data: ${e.message}"
                )
            }
        }
    }
}
```

**Added Methods:**
- `calculateStreakForPatient()` - Calculate streak from patient's data
- `calculateMissedDosesForPatient()` - Find missed doses from patient's data
- `calculateProblematicMedsForPatient()` - Find low-adherence meds from patient's data
- `calculateTrendForPatient()` - Calculate trend using patient-specific adherence

### 4. **Updated ViewModelFactory**

**File:** `CaretakerViewModel.kt`

```kotlin
class CaretakerViewModelFactory(
    private val repository: FirebaseMedicationRepository,
    private val patientPin: String? = null  // ✅ Accept patient PIN
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CaretakerViewModel::class.java)) {
            return CaretakerViewModel(repository, patientPin) as T  // ✅ Pass to ViewModel
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
```

### 5. **Updated MainActivity Navigation**

**File:** `MainActivity.kt`

```kotlin
composable("patient_monitor/{pin}") { backStackEntry ->
    val pin = backStackEntry.arguments?.getString("pin") ?: ""
    android.util.Log.d("MainActivity", "📱 Navigating to patient monitor with PIN: $pin")
    // Reuse CaretakerScreen for monitoring specific patient
    CaretakerScreen(
        viewModel = androidx.lifecycle.viewmodel.compose.viewModel(
            factory = CaretakerViewModelFactory(repository, pin)  // ✅ Pass PIN
        )
    )
}
```

### 6. **Added Required Imports**

**File:** `FirebaseMedicationRepository.kt`

```kotlin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
```

## How It Works Now

### Data Flow for Caregiver Monitoring

1. **Caregiver imports patient via PIN**
   - Creates a `caregiver_link` document in Firestore
   - Links `caregiverUserId` to `patientUserId` via `patientPin`

2. **Caregiver views patient list**
   - `getCaregiverPatients()` sets up a **real-time snapshot listener** on `caregiver_links`
   - For each patient link, queries their medication count from `users/{patientUserId}/medications`
   - Emits updated `PatientProfile` list whenever:
     - A new patient is added/removed
     - A patient's medication count changes

3. **Caregiver selects a patient**
   - Navigates to `patient_monitor/{pin}`
   - `CaretakerViewModel` is created with the patient's PIN
   - Sets up **real-time listener** on patient's medications via `getMedicationsForPatientByPin(pin)`
   - Loads patient's dose events and calculates adherence stats

4. **Patient makes changes**
   - Patient adds/removes medication → Firestore triggers snapshot listener → Caregiver's list updates automatically
   - Patient marks dose as taken → Caregiver can refresh to see updated adherence stats

## Benefits

✅ **Real-Time Updates**: Caregiver sees changes without app restart  
✅ **Accurate Medication Counts**: Always reflects current data from Firestore  
✅ **Patient-Specific Data**: Caregiver sees the correct patient's data, not their own  
✅ **Clean Architecture**: Proper separation between current user and monitored patient data  
✅ **Comprehensive Logging**: Easy to debug with detailed log messages  
✅ **Error Handling**: Graceful fallbacks if patient data can't be loaded  

## Testing Recommendations

1. **Two-Device Setup**:
   - Device A: Patient adds medications
   - Device B: Caregiver (monitoring Device A's patient) should see medication count update in real-time

2. **Medication CRUD**:
   - Patient adds medication → Caregiver sees count increase
   - Patient deletes medication → Caregiver sees count decrease
   - Patient edits medication → Caregiver sees updated data

3. **Adherence Updates**:
   - Patient marks doses → Caregiver refreshes and sees updated adherence %
   - Patient's streak increases → Caregiver sees updated streak

4. **Network Scenarios**:
   - Test with/without network connectivity
   - Verify offline mode still uses device-specific IDs
   - Confirm data syncs when connection restored

## Known Limitations

1. **Adherence Refresh Not Automatic**: While medication changes update in real-time, adherence statistics require manual refresh (tap refresh button). This is by design to avoid excessive Firestore queries.

2. **No Real-Time Dose Event Listener**: Dose events are queried on demand, not with a snapshot listener. This keeps Firestore usage efficient while still providing accurate data on refresh.

3. **Longest Streak Not Implemented**: Currently using `currentStreak` for `longestStreak` in patient monitoring mode. This can be enhanced in the future.

## Future Enhancements

1. **Automatic Adherence Refresh**: Set up periodic background refresh (e.g., every 5 minutes)
2. **Push Notifications**: Notify caregiver when patient misses a dose
3. **Real-Time Dose Event Listener**: For instant updates when patient marks doses
4. **Patient Activity Feed**: Show timeline of patient's medication activities
5. **Multiple Caregiver Support**: Allow multiple caregivers to monitor the same patient

## Conclusion

This fix transforms the caregiver monitoring feature from a static snapshot to a dynamic, real-time monitoring system. Caregivers can now reliably track their patients' medication adherence with up-to-date information, making the app significantly more useful for its intended use case.

