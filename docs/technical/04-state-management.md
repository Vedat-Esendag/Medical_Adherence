# State Management with StateFlow

## TL;DR
MVVM with StateFlow for reactive UI. ViewModels expose `StateFlow<UiState>`, screens collect with `collectAsState()`. Unidirectional data flow.

## StateFlow Pattern

### ViewModel Setup
Every ViewModel follows this pattern:

```kotlin
class HomeViewModel(
    private val repository: MedicationRepository
) : ViewModel() {

    // Private mutable state
    private val _uiState = MutableStateFlow(HomeUiState())

    // Public immutable state
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Update state
    private fun updateState(transform: HomeUiState.() -> HomeUiState) {
        _uiState.value = _uiState.value.transform()
    }
}
```

**Location**: All files in `app/src/main/java/com/example/medicaladherence/viewmodel/`

### Screen Collection
Screens collect state and recompose:

```kotlin
@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    // Use uiState properties
    Text(text = uiState.nextDoseName)
}
```

**Benefits**:
- Automatic recomposition when state changes
- Lifecycle-aware (stops when screen not visible)
- Thread-safe updates
- Survives configuration changes

## UI State Classes

### HomeUiState
**Location**: `HomeViewModel.kt:17-30`

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

**Principles**:
- Immutable data class
- Default values for all properties
- Contains ALL screen state
- No business logic

### StatsUiState
```kotlin
data class StatsUiState(
    val weeklyPercentage: Int = 0,
    val streakDays: Int = 0,
    val dailyBars: List<DayBar> = emptyList()
)
```

### AddMedicationUiState
```kotlin
data class AddMedicationUiState(
    val name: String = "",
    val dosage: String = "",
    val times: List<String> = emptyList(),
    val notes: String = "",
    val frequency: MedicationFrequency = MedicationFrequency.Daily,
    val specificDays: List<Int> = emptyList(),
    val isValid: Boolean = false,
    val nameError: String? = null,
    val dosageError: String? = null,
    val timesError: String? = null,
    val savedSuccessfully: Boolean = false
)
```

## State Updates

### Updating State
Pattern used across ViewModels:

```kotlin
// Copy with changes
_uiState.value = _uiState.value.copy(
    nextDoseName = "Amlodipine",
    nextDoseCountdown = "3h 24m"
)

// Or with helper function
private fun updateState(block: HomeUiState.() -> HomeUiState) {
    _uiState.value = _uiState.value.block()
}

// Usage
updateState {
    copy(nextDoseName = "Amlodipine")
}
```

### Multiple Properties
```kotlin
_uiState.value = _uiState.value.copy(
    todayDoses = doses,
    weeklyAdherencePercent = adherence,
    streakDays = streak
)
```

## Repository Integration

### Flow from Repository
Repository exposes `Flow<T>`:

```kotlin
class MedicationRepository {
    val medications: Flow<List<Medication>> =
        medicationDao.getAllMedications()
            .map { entities → entities.map { it.toMedication() } }
}
```

### ViewModel Collects Flow
```kotlin
init {
    viewModelScope.launch {
        repository.medications.collect { meds →
            _uiState.value = _uiState.value.copy(
                medications = meds
            )
        }
    }
}
```

**Example**: `MedicationsLibraryViewModel.kt`
```kotlin
init {
    viewModelScope.launch {
        repository.medications.collect { medicationsList →
            _medications.value = medicationsList
        }
    }
}
```

## Coroutines & ViewModelScope

### ViewModelScope
All async work runs in `viewModelScope`:

```kotlin
fun markTaken(medId: String, time: String) {
    viewModelScope.launch {
        repository.markDose(medId, LocalDate.now(), time, taken = true)
        loadData()  // Refresh UI
        _uiState.value = _uiState.value.copy(
            snackbarMessage = "Marked as taken"
        )
    }
}
```

**Benefits**:
- Automatically cancelled when ViewModel cleared
- Crash-safe (exceptions don't kill app)
- Structured concurrency

### Timers
Example: Live countdown timer

**Location**: `HomeViewModel.kt:65-72`
```kotlin
private fun startCountdownTimer() {
    viewModelScope.launch {
        while (true) {
            updateCountdown()
            delay(1000)  // Update every second
        }
    }
}
```

**Lifecycle**: Runs while ViewModel alive, cancels when cleared

## User Events

### Event Handling Pattern
Screens call ViewModel methods:

```kotlin
// Screen
DoseCard(
    dose = dose,
    onTaken = { viewModel.markTaken(dose.medication.id, dose.time) },
    onMissed = { viewModel.markMissed(dose.medication.id, dose.time) }
)

// ViewModel
fun markTaken(medId: String, time: String) {
    viewModelScope.launch {
        repository.markDose(medId, LocalDate.now(), time, taken = true)
        loadData()
    }
}
```

### Validation Events
Real-time validation in forms:

```kotlin
// Screen
OutlinedTextField(
    value = uiState.name,
    onValueChange = { viewModel.updateName(it) },
    isError = uiState.nameError != null
)

// ViewModel
fun updateName(value: String) {
    _uiState.value = _uiState.value.copy(name = value)
    validate()
}

private fun validate() {
    _uiState.value = _uiState.value.copy(
        nameError = if (_uiState.value.name.isBlank()) "Required" else null,
        isValid = _uiState.value.name.isNotBlank() && ...
    )
}
```

## Snackbar State

### Transient State
Snackbar messages are one-time events:

```kotlin
data class HomeUiState(
    val snackbarMessage: String? = null
)

// Trigger snackbar
_uiState.value = _uiState.value.copy(
    snackbarMessage = "Medication deleted"
)

// Clear after shown
fun clearSnackbar() {
    _uiState.value = _uiState.value.copy(snackbarMessage = null)
}
```

**Screen**:
```kotlin
LaunchedEffect(uiState.snackbarMessage) {
    uiState.snackbarMessage?.let { message →
        snackbarHostState.showSnackbar(message)
        viewModel.clearSnackbar()
    }
}
```

## Loading States

### Not Currently Implemented
Could add loading states:

```kotlin
data class HomeUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val data: List<DoseItem> = emptyList()
)
```

## State Hoisting

### Font Scale Example
Font scale hoisted to MainActivity:

**MainActivity**: `MainActivity.kt:37-39`
```kotlin
var fontScale by remember { mutableFloatStateOf(1.0f) }

MedicalAdherenceTheme(fontScale = fontScale) {
    // ...
}
```

**Settings Screen**:
```kotlin
SettingsScreen(
    onFontScaleChanged = { newScale → fontScale = newScale }
)
```

**Benefits**:
- State survives navigation
- Shared across all screens
- Single source of truth

## Shared ViewModel Pattern

### Problem: Multiple ViewModel Instances
When creating ViewModels independently in each screen, you get separate instances with isolated state:

```kotlin
// ❌ WRONG: Creates new instance
@Composable
fun SettingsScreen() {
    val viewModel: SettingsViewModel = viewModel()  // New instance!
    // State not shared with other screens
}
```

**Issues**:
- Profile data saved in one screen not visible in another
- Duplicate loading from database
- Inconsistent state across navigation
- User sees empty/null values

### Solution: Single Shared Instance

**Create once at the app level**, pass through the tree:

**Location**: `MainActivity.kt:56-58`
```kotlin
@Composable
fun MedicalAdherenceApp(repository: FirebaseMedicationRepository) {
    // Create ONCE at app level
    val settingsViewModel: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = SettingsViewModelFactory(repository)
    )
    
    // Pass to child composables
    when (userProfile) {
        "patient" -> {
            PatientMainScreen(
                settingsViewModel = settingsViewModel,  // Pass shared instance
                onHighContrastChanged = { enabled ->
                    settingsViewModel.setHighContrastMode(enabled)
                }
            )
        }
    }
}
```

**Pass through navigation**:

**Location**: `MainActivity.kt:92-94, 268-274`
```kotlin
@Composable
fun PatientMainScreen(
    settingsViewModel: SettingsViewModel,  // Required parameter, no default
    onHighContrastChanged: (Boolean) -> Unit
) {
    // ...
    
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onFontScaleChanged = { newScale -> fontScale = newScale },
                onHighContrastChanged = onHighContrastChanged,
                viewModel = settingsViewModel  // Pass shared instance
            )
        }
    }
}
```

**Screen receives shared instance**:

**Location**: `SettingsScreen.kt:37`
```kotlin
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onFontScaleChanged: (Float) -> Unit,
    onHighContrastChanged: (Boolean) -> Unit = {},
    viewModel: SettingsViewModel  // No default = enforces shared instance
)
```

**Benefits**:
- ✅ Single source of truth for settings state
- ✅ Profile data visible across all screens
- ✅ No duplicate database queries
- ✅ State survives navigation
- ✅ Consistent user experience

### When to Share ViewModels

**Share when:**
- Multiple screens need same data (Settings profile across app)
- Data should persist across navigation (User profile, preferences)
- Avoiding duplicate network/database calls

**Don't share when:**
- Screen-specific state (Home screen countdown timer)
- Temporary form data (Add medication form)
- Independent features (Statistics vs. Medications)

## Defensive Profile Loading

### Problem: Auto-Login from Stale Data
After sign out, cached Firebase data might auto-load incomplete profiles:

```kotlin
// ❌ WRONG: Loads ANY profile, even invalid ones
init {
    viewModelScope.launch {
        val profile = repository.getCurrentUserProfile()
        if (profile != null) {
            _userProfile.value = profile.role  // Accepts incomplete profiles!
        }
    }
}
```

**Issues**:
- Loads profiles with only `role` but missing `name`
- Auto-navigates to patient/caregiver screen after sign out
- User can't return to ProfileSelectionScreen
- Inconsistent state

### Solution: Strict Validation

**Location**: `SettingsViewModel.kt:40-68`
```kotlin
init {
    loadProfileIfExists()
}

private fun loadProfileIfExists() {
    viewModelScope.launch {
        try {
            val profile = repository.getCurrentUserProfile()
            
            // ✅ STRICT VALIDATION: Check BOTH role AND name
            val hasValidRole = profile != null && !profile.role.isNullOrEmpty()
            val hasValidName = profile != null && !profile.name.isNullOrEmpty()
            
            if (hasValidRole && hasValidName) {
                // Valid profile: load it
                _userProfile.value = profile.role
                _pairingPin.value = profile.pin
                _patientName.value = profile.name
                android.util.Log.d("SettingsViewModel", "✅ Loaded valid profile: ${profile.role}, name: ${profile.name}")
            } else {
                // Invalid/incomplete profile: reject it
                _userProfile.value = null
                _pairingPin.value = null
                _patientName.value = null
                android.util.Log.d("SettingsViewModel", "❌ Invalid profile (role: ${profile?.role}, name: ${profile?.name}) - showing selection screen")
            }
        } catch (e: Exception) {
            // Error loading: fail safe to null
            _userProfile.value = null
            _pairingPin.value = null
            _patientName.value = null
            android.util.Log.d("SettingsViewModel", "⚠️ Profile load error - showing selection screen: ${e.message}")
        }
    }
}
```

**Key Principles**:
1. **Strict Validation**: Check multiple required fields (role AND name)
2. **Fail Closed**: On error or invalid data, set state to null
3. **Show Selection**: Null profile → shows ProfileSelectionScreen
4. **Comprehensive Logging**: Debug with emoji-coded logs (✅ ❌ ⚠️)

**Result**:
- ✅ ProfileSelectionScreen always shows after sign out
- ✅ No auto-login from incomplete cached data
- ✅ User has full control over profile selection

### Synchronous State Updates

For instant UI response, update local state synchronously before async operations:

**Location**: `SettingsViewModel.kt:79-102`
```kotlin
fun setUserProfile(profile: String, name: String? = null) {
    // 1. Update local state IMMEDIATELY (synchronous)
    val pin = if (profile == "patient") {
        generatePairingPin()
    } else ""
    
    _userProfile.value = profile
    _pairingPin.value = pin
    _patientName.value = name ?: "User"
    
    android.util.Log.d("SettingsViewModel", "Profile state updated: $profile")
    
    // 2. THEN save to Firebase asynchronously (doesn't block UI)
    viewModelScope.launch {
        try {
            android.util.Log.d("SettingsViewModel", "Saving profile to Firebase: $profile, name: $name")
            repository.setUserProfile(profile, name ?: "User", pin)
            android.util.Log.d("SettingsViewModel", "Profile saved to Firebase successfully")
        } catch (e: Exception) {
            android.util.Log.e("SettingsViewModel", "Error saving profile to Firebase (will retry later)", e)
            // Local state already updated, so UI works even if Firebase fails
        }
    }
}
```

**Benefits**:
- ✅ UI updates instantly (< 100ms)
- ✅ No waiting for network operations
- ✅ Works offline
- ✅ Firebase syncs in background

### Sign Out with Cleanup

Clear local state immediately, clean up Firebase asynchronously:

**Location**: `SettingsViewModel.kt:174-200`
```kotlin
fun clearUserProfile() {
    // 1. Clear local state IMMEDIATELY (synchronous) for instant UI update
    _userProfile.value = null
    _pairingPin.value = null
    _patientName.value = null

    android.util.Log.d("SettingsViewModel", "Profile state cleared locally")

    // 2. Then clean up Firebase asynchronously
    viewModelScope.launch {
        try {
            // Delete user profile from Firebase
            repository.deleteUserProfile()
            android.util.Log.d("SettingsViewModel", "Profile deleted from Firebase")
        } catch (e: Exception) {
            android.util.Log.e("SettingsViewModel", "Error deleting profile from Firebase (already cleared locally)", e)
        }

        try {
            // Sign out from Firebase Auth
            RepositoryProvider.getAuthManager().signOut()
            android.util.Log.d("SettingsViewModel", "Signed out from Firebase Auth")
        } catch (e: Exception) {
            android.util.Log.e("SettingsViewModel", "Error signing out from Firebase Auth", e)
        }
    }
}
```

**Key Pattern**:
1. **Local state first**: Instant UI response
2. **Network operations second**: Don't block user
3. **Error handling**: Local state already cleared, errors logged but don't affect UI

## Derived State

### Computed Properties
Example: Checking if in dose window

**Location**: `HomeViewModel.kt:100-110`
```kotlin
private fun updateCountdown() {
    val now = LocalTime.now()
    val nextDose = findNextDose()

    val isInWindow = nextDose?.let {
        val doseTime = LocalTime.parse(it.time, formatter)
        val minutesUntil = java.time.Duration.between(now, doseTime).toMinutes()
        minutesUntil <= 15  // Within 15 minutes
    } ?: false

    _uiState.value = _uiState.value.copy(isInDoseWindow = isInWindow)
}
```

## Testing ViewModels

### Unit Test Example
```kotlin
@Test
fun `markTaken updates state correctly`() = runTest {
    val viewModel = HomeViewModel(fakeRepository)

    viewModel.markTaken("med-1", "07:00")

    val state = viewModel.uiState.value
    assertEquals("Marked as taken", state.snackbarMessage)
}
```

### Testing StateFlow
```kotlin
@Test
fun `uiState emits initial state`() = runTest {
    val viewModel = HomeViewModel(fakeRepository)

    val state = viewModel.uiState.value
    assertTrue(state.todayDoses.isEmpty())
    assertEquals(0, state.weeklyAdherencePercent)
}
```

## Best Practices

### Core State Management
1. **Immutable state**: Use data classes with `copy()`
2. **Private mutable, public immutable**: `MutableStateFlow` + `StateFlow`
3. **Single UI state class**: One per screen
4. **No logic in UI state**: Only data, no functions
5. **ViewModelScope**: All coroutines in viewModelScope
6. **Lifecycle-aware**: StateFlow stops collecting when screen hidden
7. **Default values**: All UI state properties have defaults
8. **Validation**: Real-time validation with error states
9. **Transient events**: Snackbar messages cleared after shown
10. **Repository as source**: ViewModel transforms repository data to UI state

### Shared ViewModels
11. **Create once**: Instantiate at app level, pass through tree
12. **No default parameters**: Force explicit passing of shared instances
13. **Share when needed**: For cross-screen data (settings, profile)
14. **Don't over-share**: Keep screen-specific ViewModels separate

### Defensive Loading
15. **Strict validation**: Check ALL required fields before accepting data
16. **Fail closed**: On error or invalid data, set to null/safe default
17. **Comprehensive logging**: Use emoji-coded logs for debugging (✅ ❌ ⚠️)
18. **Reject incomplete data**: Don't accept partially loaded profiles/data

### Performance Patterns
19. **Synchronous local updates**: Update UI state immediately
20. **Asynchronous network**: Do Firebase/network operations in background
21. **Don't block UI**: Never wait for network before showing UI changes
22. **Error handling**: Local state updated even if network fails

### Sign Out & Cleanup
23. **Immediate local clear**: Clear state synchronously for instant response
24. **Async cleanup**: Delete Firebase data in background
25. **Comprehensive deletion**: Remove all user data (profile + auth)
26. **Error resilience**: Continue even if cleanup operations fail
