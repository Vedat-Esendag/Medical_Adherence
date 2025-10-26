# Data Models & Entities

## TL;DR
Two main models: **Medication** (what to take) and **DoseEvent** (tracking when taken). Room entities mirror these with type converters for complex types.

## Domain Models

### Medication
Core model representing a medication and its schedule.

**Location**: `app/src/main/java/com/example/medicaladherence/data/model/Medication.kt`

```kotlin
data class Medication(
    val id: String,              // UUID
    val name: String,            // "Amlodipine"
    val dosage: String,          // "5 mg"
    val times: List<String>,     // ["07:00", "19:00"]
    val notes: String? = null,   // "Take with food"
    val frequency: MedicationFrequency = MedicationFrequency.Daily,
    val specificDays: List<Int> = emptyList()  // [1,3,5] = Mon/Wed/Fri
)
```

**Field Details**:
- `id`: Generated with `UUID.randomUUID().toString()`
- `times`: HH:mm format (24-hour)
- `specificDays`: 1=Monday, 7=Sunday (only used with SpecificDays frequency)

### MedicationFrequency
Enum for scheduling options.

**Location**: `Medication.kt:6-12`

```kotlin
enum class MedicationFrequency {
    Daily,          // Every day
    SpecificDays,   // Selected days (Mon, Wed, Fri)
    EveryXDays,     // Every N days (not fully implemented)
    Weekly,         // Once per week
    AsNeeded        // PRN (not scheduled)
}
```

### DoseEvent
Tracks whether a specific dose was taken or missed.

**Location**: `app/src/main/java/com/example/medicaladherence/data/model/DoseEvent.kt`

```kotlin
data class DoseEvent(
    val medId: String,           // References Medication.id
    val date: LocalDate,         // Which day
    val time: String,            // "07:00"
    val taken: Boolean,          // true=taken, false=missed
    val timestamp: Long = System.currentTimeMillis()
)
```

**Usage**:
- One event per medication per time per day
- `taken=true`: Marked as taken
- `taken=false`: Marked as missed
- No event = not yet marked

## Room Entities

### MedicationEntity
Database representation of Medication.

**Location**: `app/src/main/java/com/example/medicaladherence/data/local/entity/MedicationEntity.kt`

```kotlin
@Entity(tableName = "medications")
@TypeConverters(Converters::class)
data class MedicationEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val dosage: String,
    val times: List<String>,            // Converted by Converters
    val notes: String?,
    val frequency: MedicationFrequency, // Converted by Converters
    val specificDays: List<Int>         // Converted by Converters
)
```

**Converters**:
- `List<String>` → JSON array
- `List<Int>` → Comma-separated string
- `MedicationFrequency` → String name

### DoseEventEntity
Database representation of DoseEvent.

**Location**: `app/src/main/java/com/example/medicaladherence/data/local/entity/DoseEventEntity.kt`

```kotlin
@Entity(
    tableName = "dose_events",
    primaryKeys = ["medId", "date", "time"]  // Composite key
)
@TypeConverters(Converters::class)
data class DoseEventEntity(
    val medId: String,           // Foreign key to medications
    val date: LocalDate,         // Converted by Converters
    val time: String,
    val taken: Boolean,
    val timestamp: Long
)
```

**Composite Primary Key**: One event per (medication, date, time)

### SettingsEntity
Stores user preferences.

**Location**: `app/src/main/java/com/example/medicaladherence/data/local/entity/SettingsEntity.kt`

```kotlin
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey
    val id: Int = 1,              // Single row
    val fontScale: Float = 1.0f   // 1.0 or 1.15
)
```

## Type Converters

### Converters Class
Handles complex type storage in SQLite.

**Location**: `app/src/main/java/com/example/medicaladherence/data/local/Converters.kt`

```kotlin
class Converters {
    // List<String> ↔ JSON
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return json.decodeFromString(value)
    }

    // List<Int> ↔ comma-separated
    @TypeConverter
    fun fromIntList(value: List<Int>): String {
        return value.joinToString(",")
    }

    @TypeConverter
    fun toIntList(value: String): List<Int> {
        return if (value.isEmpty()) emptyList()
               else value.split(",").map { it.toInt() }
    }

    // MedicationFrequency ↔ String
    @TypeConverter
    fun fromFrequency(value: MedicationFrequency): String {
        return value.name
    }

    @TypeConverter
    fun toFrequency(value: String): MedicationFrequency {
        return MedicationFrequency.valueOf(value)
    }

    // LocalDate ↔ Long (epoch day)
    @TypeConverter
    fun fromLocalDate(value: LocalDate): Long {
        return value.toEpochDay()
    }

    @TypeConverter
    fun toLocalDate(value: Long): LocalDate {
        return LocalDate.ofEpochDay(value)
    }
}
```

## Extension Functions

### Entity ↔ Model Mapping

**Location**: `app/src/main/java/com/example/medicaladherence/data/local/Mappers.kt`

```kotlin
// Entity → Model
fun MedicationEntity.toMedication() = Medication(
    id = id,
    name = name,
    dosage = dosage,
    times = times,
    notes = notes,
    frequency = frequency,
    specificDays = specificDays
)

// Model → Entity
fun Medication.toEntity() = MedicationEntity(
    id = id,
    name = name,
    dosage = dosage,
    times = times,
    notes = notes,
    frequency = frequency,
    specificDays = specificDays
)

// Similar for DoseEvent
fun DoseEventEntity.toDoseEvent() = DoseEvent(...)
fun DoseEvent.toEntity() = DoseEventEntity(...)
```

**Why Separate?**
- Domain models have no Room annotations
- Entities are Room-specific
- Easier to test domain logic
- Clear separation of concerns

## UI State Models

### DoseItem
Combines medication with dose status for UI display.

**Location**: `app/src/main/java/com/example/medicaladherence/viewmodel/HomeViewModel.kt:32-36`

```kotlin
data class DoseItem(
    val medication: Medication,
    val time: String,            // "07:00"
    val taken: Boolean?          // null=not marked, true=taken, false=missed
)
```

**Usage**: Home screen dose cards

### HomeUiState
Complete state for Home screen.

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

### DayBar
Daily stats for bar chart.

**Location**: `app/src/main/java/com/example/medicaladherence/viewmodel/StatsViewModel.kt`

```kotlin
data class DayBar(
    val dayLabel: String,        // "Mon", "Tue", etc.
    val percentage: Int          // 0-100
)
```

## Data Validation

### Medication Validation
Performed in `AddMedicationViewModel`:

```kotlin
private fun validate() {
    nameError = if (name.isBlank()) "Name required" else null
    dosageError = if (dosage.isBlank()) "Dosage required" else null
    timesError = if (times.isEmpty()) "Add at least one time" else null

    isValid = nameError == null && dosageError == null && timesError == null
}
```

### Time Format
- Always stored as "HH:mm" (24-hour)
- Parsed/displayed using `DateTimeFormatter.ofPattern("HH:mm")`
- Example: "07:00", "19:30"

## Database Relationships

### Logical Relationships
```
Medication (1) ←→ (N) DoseEvent
  id                  medId
```

- One medication can have many dose events
- DoseEvents reference medication by `medId`
- Cascade delete: Deleting medication deletes all its events

**Implementation**: `MedicationRepository.kt:33-36`
```kotlin
suspend fun deleteMedication(medId: String) {
    medicationDao.deleteMedicationById(medId)
    doseEventDao.deleteEventsForMedication(medId)
}
```

## Sample Data

### Example Medication
```kotlin
Medication(
    id = "abc-123",
    name = "Amlodipine",
    dosage = "5 mg",
    times = listOf("07:00", "19:00"),
    notes = "Take with food",
    frequency = MedicationFrequency.Daily,
    specificDays = emptyList()
)
```

### Example DoseEvent
```kotlin
DoseEvent(
    medId = "abc-123",
    date = LocalDate.of(2025, 10, 26),
    time = "07:00",
    taken = true,
    timestamp = 1729933200000
)
```

## Model Evolution

### Adding Fields
1. Add to data class
2. Add to entity
3. Update converters if needed
4. Create Room migration
5. Update mappers

### Migration Example
```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE medications ADD COLUMN color TEXT DEFAULT '#0D47A1'"
        )
    }
}
```
