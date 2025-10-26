# Medical Adherence App - Project Overview

**2-Page High SNR Reference** | Version 1.0 | Android App

---

## What It Is

Clean, minimal Android medication tracking app designed for **elderly users**. Built with Jetpack Compose, Material 3, and Room database. Features large touch targets (≥48dp), adjustable fonts, and non-judgmental language.

**Core Function**: Track daily medications → Monitor adherence with stats → Build consistency with streaks

---

## Tech Stack (Essential)

| Category | Technology | Version |
|----------|-----------|---------|
| **Language** | Kotlin | 2.0.21 |
| **UI** | Jetpack Compose + Material 3 | BOM 2024.09.00 |
| **Architecture** | MVVM (ViewModel + StateFlow) | Lifecycle 2.9.4 |
| **Database** | Room (SQLite) | 2.6.1 |
| **Navigation** | Navigation Compose | 2.8.5 |
| **Async** | Kotlin Coroutines + Flow | 1.9.0 |
| **Min SDK** | Android 10 (API 29) | - |
| **Target SDK** | Android 15+ (API 36) | - |

**Build**: Gradle 8.13.0 with KSP for Room annotation processing

---

## Architecture (MVVM Pattern)

```
┌─────────────┐
│   Screen    │  ← Composable, collects StateFlow
└──────┬──────┘
       ↓
┌─────────────┐
│  ViewModel  │  ← State management, business logic
└──────┬──────┘
       ↓
┌─────────────┐
│ Repository  │  ← Single source of truth, exposes Flow
└──────┬──────┘
       ↓
┌─────────────┐
│ Room (DAO)  │  ← Database queries, persistence
└─────────────┘
```

**Data Flow**: User Action → ViewModel → Repository → Room → Flow → StateFlow → Screen Recompose

**Key Files**:
- `MainActivity.kt` - Single activity + NavHost
- `HomeScreen.kt` - Daily dose tracking UI
- `HomeViewModel.kt` - State management with StateFlow
- `MedicationRepository.kt` - Data operations
- `AppDatabase.kt` - Room database definition

---

## Features (User Perspective)

### 1. Home Screen (Daily Tracking)
- **Live countdown** to next dose
- **Dose cards** with 3 action buttons: Taken / Missed / Snooze 15m
- **Weekly stats** at top: Adherence % + Streak days
- **Undo button** for corrections
- **FAB (+)** to add medications

### 2. Medications Library
- View all medications with full schedules
- Edit/delete via overflow menu (⋮)
- Support for multiple doses per day
- Frequency options: Daily, Specific Days, Weekly, Every X Days, As Needed

### 3. Statistics
- **Weekly percentage**: 0-100% adherence with color-coded progress bar
- **Streak counter**: Consecutive days of 100% adherence
- **Daily bar chart**: Visual breakdown for Mon-Sun
- **Encouraging feedback**: Based on performance (non-judgmental)

### 4. Settings
- **Font size**: Normal (1.0x) or Large (1.15x)
- **Theme**: Follows system light/dark mode

---

## Data Models

### Medication
```kotlin
data class Medication(
    val id: String,
    val name: String,           // "Amlodipine"
    val dosage: String,         // "5 mg"
    val times: List<String>,    // ["07:00", "19:00"]
    val notes: String?,         // "Take with food"
    val frequency: MedicationFrequency,
    val specificDays: List<Int> // [1,3,5] for Mon/Wed/Fri
)
```

### DoseEvent
```kotlin
data class DoseEvent(
    val medId: String,
    val date: LocalDate,
    val time: String,           // "07:00"
    val taken: Boolean          // true=taken, false=missed
)
```

**Room Entities**: `MedicationEntity`, `DoseEventEntity` (with type converters for lists)

---

## Project Structure

```
app/src/main/java/com/example/medicaladherence/
├── MainActivity.kt                   # Single activity + navigation
├── data/
│   ├── model/                        # Medication, DoseEvent, enums
│   ├── local/
│   │   ├── AppDatabase.kt            # Room database
│   │   ├── Converters.kt             # Type converters
│   │   ├── entity/                   # Room entities
│   │   └── dao/                      # MedicationDao, DoseEventDao
│   └── repository/
│       └── MedicationRepository.kt   # Single source of truth
├── viewmodel/                        # HomeViewModel, StatsViewModel, etc.
└── ui/
    ├── screens/                      # HomeScreen, StatsScreen, etc.
    ├── components/                   # DoseCard (reusable)
    └── theme/                        # Material 3 theme
```

---

## Key Technical Patterns

### StateFlow for Reactive UI
```kotlin
// ViewModel
private val _uiState = MutableStateFlow(HomeUiState())
val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

// Screen
val uiState by viewModel.uiState.collectAsState()
```

### Room with Flow
```kotlin
@Dao
interface MedicationDao {
    @Query("SELECT * FROM medications")
    fun getAllMedications(): Flow<List<MedicationEntity>>
}
```

### Repository Pattern
```kotlin
class MedicationRepository(database: AppDatabase) {
    val medications: Flow<List<Medication>> =
        medicationDao.getAllMedications().map { it.toMedication() }
}
```

---

## Accessibility Features

| Feature | Implementation |
|---------|---------------|
| **Large Touch Targets** | All buttons ≥48dp |
| **Font Scaling** | Settings: Normal (1.0x) or Large (1.15x) |
| **Color Contrast** | WCAG AA compliant, Material 3 |
| **Calm Palette** | Soothing blue, no alarming colors |
| **Friendly Language** | "Missed" not "Failed", encouraging feedback |
| **TalkBack** | Screen reader compatible |

---

## Quick Start

### For Users
1. Tap **+ button** → Add medication (name, dosage, times)
2. Home screen shows today's doses
3. Tap **Taken** when you take medication
4. Check **Stats** tab for weekly progress

### For Developers
```bash
# Clone and open in Android Studio
cd Medical_Adherence
./gradlew build

# Run on device/emulator
./gradlew installDebug

# Run tests
./gradlew test
```

**Requirements**: Android Studio Hedgehog+, SDK 29+, Kotlin 2.0.21

---

## Routes & Navigation

```kotlin
HOME = "home"                           # Daily tracking
MEDICATIONS = "medications"             # Library view
ADD_MEDICATION = "add_medication"       # Add/edit form
STATS = "stats"                         # Weekly statistics
SETTINGS = "settings"                   # Font & theme
```

**Bottom Nav**: Home | 💊 Medications | 📊 Stats | ⚙️ Settings

---

## Database Schema

### Tables
- `medications`: id, name, dosage, times, notes, frequency, specificDays
- `dose_events`: id, medId, date, time, taken, timestamp
- `settings`: id, fontScale

### Type Converters
- `List<String>` → JSON string
- `List<Int>` → comma-separated
- `MedicationFrequency` → String
- `LocalDate` → Long (epoch day)

---

## State Management

### UI State Classes
```kotlin
data class HomeUiState(
    val todayDoses: List<DoseItem>,
    val nextDoseCountdown: String,      // "3h 24m"
    val weeklyAdherencePercent: Int,    // 0-100
    val streakDays: Int,
    val snackbarMessage: String?
)
```

### Lifecycle
- ViewModel survives configuration changes
- Room queries automatically stop when screen not visible
- Coroutines cancelled when ViewModel cleared

---

## Deployment Info

**Package**: `com.example.medicaladherence`
**Version Code**: 1
**Version Name**: "1.0"

**Build Variants**:
- Debug: Development with full logging
- Release: Production (minification currently disabled)

**No external dependencies**: Pure Jetpack stack, local storage only

---

## Future Enhancements

- Push notifications for dose reminders (WorkManager)
- Data export to CSV/PDF
- Multi-user support with profiles
- Medication refill tracking
- Health data API integration

---

## Key Metrics

- **5 screens**: Home, Medications, Stats, Settings, Add/Edit
- **3 tables**: medications, dose_events, settings
- **5 ViewModels**: One per main screen
- **2 DAOs**: MedicationDao, DoseEventDao
- **1 Repository**: MedicationRepository (single source of truth)

---

**Documentation**: See `docs/user/` for user guides, `docs/technical/` for deep dives

**License**: Prototype for demonstration purposes
