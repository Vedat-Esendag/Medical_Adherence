# Architecture Overview

## TL;DR
Modern Android app using **MVVM architecture** with Jetpack Compose, Room database, Kotlin Coroutines, and StateFlow for reactive UI. Single Activity with composable screens.

## Architecture Pattern: MVVM

### Model-View-ViewModel
The app follows strict MVVM separation:

**Model**
- Data classes: `Medication`, `DoseEvent`
- Room entities: `MedicationEntity`, `DoseEventEntity`
- Repository: `MedicationRepository`
- Location: `app/src/main/java/com/example/medicaladherence/data/`

**View**
- Composable screens: `HomeScreen`, `StatsScreen`, etc.
- Reusable components: `DoseCard`
- Location: `app/src/main/java/com/example/medicaladherence/ui/`

**ViewModel**
- State management: `HomeViewModel`, `StatsViewModel`, etc.
- UI state classes: `HomeUiState`, `StatsUiState`
- Location: `app/src/main/java/com/example/medicaladherence/viewmodel/`

## Project Structure

```
app/src/main/java/com/example/medicaladherence/
├── MainActivity.kt                  # Single activity + nav graph
│
├── data/
│   ├── model/                       # Data classes
│   │   ├── Medication.kt            # Core medication model
│   │   ├── DoseEvent.kt             # Dose tracking events
│   │   └── MedicationFrequency.kt   # Enum for frequencies
│   │
│   ├── local/                       # Room database
│   │   ├── AppDatabase.kt           # Room database definition
│   │   ├── Converters.kt            # Type converters for Room
│   │   ├── entity/                  # Room entities
│   │   │   ├── MedicationEntity.kt
│   │   │   ├── DoseEventEntity.kt
│   │   │   └── SettingsEntity.kt
│   │   └── dao/                     # Data access objects
│   │       ├── MedicationDao.kt
│   │       ├── DoseEventDao.kt
│   │       └── SettingsDao.kt
│   │
│   └── repository/                  # Repository layer
│       └── MedicationRepository.kt  # Single source of truth
│
├── viewmodel/                       # ViewModels + UI state
│   ├── HomeViewModel.kt
│   ├── StatsViewModel.kt
│   ├── MedicationsLibraryViewModel.kt
│   ├── AddMedicationViewModel.kt
│   └── SettingsViewModel.kt
│
└── ui/
    ├── screens/                     # Full-screen composables
    │   ├── HomeScreen.kt
    │   ├── StatsScreen.kt
    │   ├── MedicationsLibraryScreen.kt
    │   ├── AddEditMedicationScreen.kt
    │   └── SettingsScreen.kt
    │
    ├── components/                  # Reusable composables
    │   └── DoseCard.kt
    │
    ├── theme/                       # Material 3 theming
    │   ├── Color.kt
    │   ├── Type.kt
    │   └── Theme.kt
    │
    └── nav/                         # Navigation
        └── NavGraph.kt              # Route constants
```

## Single Activity Architecture

### MainActivity
- Single `ComponentActivity` (`MainActivity.kt:24`)
- Hosts `NavHost` with all screen destinations
- Bottom navigation bar integrated
- Manages app-level font scale state

### Navigation
- Jetpack Compose Navigation
- Routes defined in `Routes` object (`NavGraph.kt:6-13`)
- Bottom nav shows on: Home, Medications, Stats, Settings
- Hidden on: Add/Edit screens

### Routes
```kotlin
HOME = "home"
MEDICATIONS = "medications"
ADD_MEDICATION = "add_medication"
EDIT_MEDICATION = "add_medication?id={medId}"
STATS = "stats"
SETTINGS = "settings"
```

## Data Flow

### Unidirectional Data Flow
```
User Action
    ↓
ViewModel (receives event)
    ↓
Repository (business logic)
    ↓
Room Database (persistence)
    ↓
Repository Flow<T> (emits changes)
    ↓
ViewModel StateFlow (updates UI state)
    ↓
Composable (recomposes with new state)
```

### Example: Marking Dose as Taken
1. User taps "Taken" button (`DoseCard.kt:172`)
2. `onTaken()` callback invoked
3. `HomeViewModel.markTaken()` called (`HomeViewModel.kt`)
4. `repository.markDose()` saves to Room
5. Repository emits updated data via Flow
6. ViewModel updates `StateFlow<HomeUiState>`
7. HomeScreen recomposes with new data

## Reactive UI with StateFlow

### Pattern
Every ViewModel exposes UI state via `StateFlow`:
```kotlin
private val _uiState = MutableStateFlow(HomeUiState())
val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
```

Screens collect state and recompose:
```kotlin
val uiState by viewModel.uiState.collectAsState()
```

### Benefits
- Automatic UI updates when state changes
- Lifecycle-aware (stops collecting when not visible)
- Thread-safe state management
- Testable ViewModel logic

## Dependency Injection

### Manual DI with Provider
`RepositoryProvider.kt` provides singleton repository:
```kotlin
object RepositoryProvider {
    fun getRepository(): MedicationRepository
}
```

ViewModels receive repository via constructor:
```kotlin
class HomeViewModel(
    private val repository: MedicationRepository = RepositoryProvider.getRepository()
)
```

### Why Not Hilt/Dagger?
- Simple app with single repository
- Reduces complexity for prototype
- Easy to migrate to Hilt later if needed

## Layers & Responsibilities

### Presentation Layer (UI + ViewModel)
- **Screens**: Render UI, collect state
- **ViewModels**: Manage UI state, handle user events
- **No business logic in screens**: ViewModels orchestrate

### Domain Layer (Models)
- Data classes: `Medication`, `DoseEvent`
- Enums: `MedicationFrequency`
- Pure Kotlin, no Android dependencies

### Data Layer (Repository + Room)
- **Repository**: Single source of truth, exposes Flows
- **Room**: Local persistence with SQLite
- **DAOs**: Database queries
- **Entities**: Room-specific data classes

## Key Architectural Decisions

### Why MVVM?
- Recommended by Google for Android
- Clear separation of concerns
- Testable business logic
- Lifecycle-aware components

### Why Single Activity?
- Smoother navigation transitions
- Shared state easier to manage
- Compose works best with single activity
- Material 3 bottom nav integration

### Why Room?
- Type-safe database queries
- Compile-time verification
- Flow support for reactive updates
- Migration support for schema changes

### Why StateFlow over LiveData?
- Better Kotlin Coroutines integration
- More predictable behavior
- Compose's `collectAsState()` works seamlessly
- Simpler testing

## Threading Model

### Coroutines
All async work uses Kotlin Coroutines:
- `viewModelScope` for ViewModel operations
- Automatic cancellation when ViewModel cleared
- Structured concurrency

### Dispatchers
- **Main**: UI updates (StateFlow emissions)
- **IO**: Room database operations (implicit)
- **Default**: Heavy computations (not currently used)

## Testing Strategy

### Unit Tests
- ViewModels: Test state transformations
- Repository: Test data operations
- Use `runTest` for coroutines

### UI Tests
- Compose UI Test: Test screens in isolation
- Verify button clicks, state changes
- Screenshot tests for regression

## Future Architectural Improvements

### Potential Enhancements
- Hilt for dependency injection
- Use cases layer for complex business logic
- Separate domain models from entities
- WorkManager for notifications
- DataStore for preferences (replacing Room SettingsEntity)
