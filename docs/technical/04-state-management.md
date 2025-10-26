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
