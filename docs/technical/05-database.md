# Room Database Architecture

## TL;DR
Room SQLite database with 3 tables: medications, dose_events, settings. DAOs expose `Flow<T>` for reactive queries. Type converters handle complex types.

## Database Setup

### AppDatabase Class
**Location**: `app/src/main/java/com/example/medicaladherence/data/local/AppDatabase.kt`

```kotlin
@Database(
    entities = [
        MedicationEntity::class,
        DoseEventEntity::class,
        SettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao
    abstract fun doseEventDao(): DoseEventDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "medical_adherence_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
```

**Key Features**:
- Singleton pattern
- Thread-safe initialization
- Database version 1
- Type converters applied globally

## Database Tables

### medications Table
```sql
CREATE TABLE medications (
    id TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    dosage TEXT NOT NULL,
    times TEXT NOT NULL,           -- JSON array
    notes TEXT,
    frequency TEXT NOT NULL,        -- Enum name
    specificDays TEXT NOT NULL      -- Comma-separated
);
```

**Indexes**: Primary key on `id`

### dose_events Table
```sql
CREATE TABLE dose_events (
    medId TEXT NOT NULL,
    date INTEGER NOT NULL,          -- LocalDate as epoch day
    time TEXT NOT NULL,
    taken INTEGER NOT NULL,         -- Boolean as 0/1
    timestamp INTEGER NOT NULL,
    PRIMARY KEY (medId, date, time)
);
```

**Composite Primary Key**: (medId, date, time) - one event per dose

**Foreign Key**: `medId` references `medications(id)` (logical, not enforced)

### settings Table
```sql
CREATE TABLE settings (
    id INTEGER PRIMARY KEY NOT NULL,
    fontScale REAL NOT NULL DEFAULT 1.0
);
```

**Single Row**: Only one settings record (id=1)

## DAOs (Data Access Objects)

### MedicationDao
**Location**: `app/src/main/java/com/example/medicaladherence/data/local/dao/MedicationDao.kt`

```kotlin
@Dao
interface MedicationDao {

    @Query("SELECT * FROM medications ORDER BY name ASC")
    fun getAllMedications(): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications WHERE id = :medicationId")
    suspend fun getMedicationById(medicationId: String): MedicationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedication(medication: MedicationEntity)

    @Update
    suspend fun updateMedication(medication: MedicationEntity)

    @Delete
    suspend fun deleteMedication(medication: MedicationEntity)

    @Query("DELETE FROM medications WHERE id = :medicationId")
    suspend fun deleteMedicationById(medicationId: String)
}
```

**Key Methods**:
- `getAllMedications()`: Returns `Flow` for reactive updates
- `getMedicationById()`: Suspending function for one-time fetch
- `insertMedication()`: REPLACE strategy handles both insert and update

### DoseEventDao
**Location**: `app/src/main/java/com/example/medicaladherence/data/local/dao/DoseEventDao.kt`

```kotlin
@Dao
interface DoseEventDao {

    @Query("SELECT * FROM dose_events WHERE date = :date")
    fun getEventsForDate(date: LocalDate): Flow<List<DoseEventEntity>>

    @Query("SELECT * FROM dose_events WHERE date BETWEEN :startDate AND :endDate")
    fun getEventsInRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<DoseEventEntity>>

    @Query("SELECT * FROM dose_events WHERE medId = :medId AND date = :date AND time = :time")
    suspend fun getEvent(
        medId: String,
        date: LocalDate,
        time: String
    ): DoseEventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: DoseEventEntity)

    @Delete
    suspend fun deleteEvent(event: DoseEventEntity)

    @Query("DELETE FROM dose_events WHERE medId = :medId")
    suspend fun deleteEventsForMedication(medId: String)
}
```

**Key Methods**:
- `getEventsForDate()`: Today's events (for Home screen)
- `getEventsInRange()`: Week's events (for Stats)
- `deleteEventsForMedication()`: Cascade delete

### SettingsDao
**Location**: `app/src/main/java/com/example/medicaladherence/data/local/dao/SettingsDao.kt`

```kotlin
@Dao
interface SettingsDao {

    @Query("SELECT * FROM settings WHERE id = 1")
    fun getSettings(): Flow<SettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: SettingsEntity)

    @Query("UPDATE settings SET fontScale = :fontScale WHERE id = 1")
    suspend fun updateFontScale(fontScale: Float)
}
```

## Type Converters

### Converters Class
**Location**: `app/src/main/java/com/example/medicaladherence/data/local/Converters.kt`

Handles complex types that SQLite doesn't support natively:

**List<String> → JSON**:
```kotlin
@TypeConverter
fun fromStringList(value: List<String>): String {
    return Json.encodeToString(value)
}

@TypeConverter
fun toStringList(value: String): List<String> {
    return Json.decodeFromString(value)
}
```

**List<Int> → Comma-Separated**:
```kotlin
@TypeConverter
fun fromIntList(value: List<Int>): String {
    return value.joinToString(",")
}

@TypeConverter
fun toIntList(value: String): List<Int> {
    return if (value.isEmpty()) emptyList()
           else value.split(",").map { it.toInt() }
}
```

**LocalDate → Long (Epoch Day)**:
```kotlin
@TypeConverter
fun fromLocalDate(value: LocalDate): Long {
    return value.toEpochDay()
}

@TypeConverter
fun toLocalDate(value: Long): LocalDate {
    return LocalDate.ofEpochDay(value)
}
```

**MedicationFrequency → String**:
```kotlin
@TypeConverter
fun fromFrequency(value: MedicationFrequency): String {
    return value.name
}

@TypeConverter
fun toFrequency(value: String): MedicationFrequency {
    return MedicationFrequency.valueOf(value)
}
```

## Reactive Queries with Flow

### Pattern
DAOs return `Flow<T>` for live updates:

```kotlin
@Query("SELECT * FROM medications")
fun getAllMedications(): Flow<List<MedicationEntity>>
```

**Benefits**:
- Automatic UI updates when data changes
- Emits new values on insert/update/delete
- Lifecycle-aware collection in ViewModels
- Backpressure handling

### Collection in Repository
```kotlin
val medications: Flow<List<Medication>> =
    medicationDao.getAllMedications()
        .map { entities → entities.map { it.toMedication() } }
```

### Collection in ViewModel
```kotlin
init {
    viewModelScope.launch {
        repository.medications.collect { meds →
            _medications.value = meds
        }
    }
}
```

## Suspend Functions

### One-Time Operations
Use `suspend` for single operations:

```kotlin
@Query("SELECT * FROM medications WHERE id = :id")
suspend fun getMedicationById(id: String): MedicationEntity?

suspend fun addMedication(med: Medication) {
    repository.addOrUpdateMedication(med)
}
```

**Usage**: Call from coroutine:
```kotlin
viewModelScope.launch {
    val med = repository.getMedicationById("abc-123")
}
```

## Database Initialization

### App Startup
Database created lazily on first access:

```kotlin
// In Application class or RepositoryProvider
val database = AppDatabase.getDatabase(context)
val repository = MedicationRepository(database)
```

### Pre-Population
Not currently implemented, but could add:

```kotlin
Room.databaseBuilder(...)
    .addCallback(object : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // Insert seed data
        }
    })
    .build()
```

## Transactions

### Implicit Transactions
Room automatically wraps these in transactions:
- `@Insert`, `@Update`, `@Delete` with multiple items
- `@Query` with multiple statements

### Explicit Transactions
For complex operations:

```kotlin
@Transaction
suspend fun deleteMedicationWithHistory(medId: String) {
    deleteMedicationById(medId)
    deleteEventsForMedication(medId)
}
```

## Query Optimization

### Indexes
- Primary keys automatically indexed
- Composite key on dose_events for fast lookups

### Query Performance
```kotlin
// Fast: Uses primary key
suspend fun getMedicationById(id: String)

// Fast: Index on date (composite key)
fun getEventsForDate(date: LocalDate)

// Acceptable: Small dataset, ordered by name
fun getAllMedications()
```

## Error Handling

### Database Errors
Room throws exceptions on:
- Constraint violations
- SQL syntax errors
- Type conversion failures

**Handling in Repository**:
```kotlin
suspend fun addMedication(med: Medication): Result<Unit> {
    return try {
        medicationDao.insertMedication(med.toEntity())
        Result.success(Unit)
    } catch (e: SQLiteException) {
        Result.failure(e)
    }
}
```

## Testing

### In-Memory Database
For unit tests:

```kotlin
@Before
fun setup() {
    database = Room.inMemoryDatabaseBuilder(
        context,
        AppDatabase::class.java
    ).build()
}

@After
fun teardown() {
    database.close()
}
```

### Testing DAOs
```kotlin
@Test
fun insertAndGetMedication() = runTest {
    val med = MedicationEntity(...)
    dao.insertMedication(med)

    val retrieved = dao.getMedicationById(med.id)
    assertEquals(med, retrieved)
}
```

## Migrations

### Current Version
Database version 1 - no migrations yet.

### Future Migrations
When schema changes, add migration:

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE medications ADD COLUMN color TEXT DEFAULT '#0D47A1'"
        )
    }
}

Room.databaseBuilder(...)
    .addMigrations(MIGRATION_1_2)
    .build()
```

## Best Practices

1. **Flow for live data**: Use `Flow<T>` for UI-bound queries
2. **Suspend for one-time**: Use `suspend` for single operations
3. **Type converters**: Handle complex types cleanly
4. **Composite keys**: For many-to-many relationships
5. **Transactions**: Wrap multi-step operations
6. **Indexes**: On frequently queried columns
7. **Singleton database**: One instance per app
8. **In-memory testing**: Fast, isolated unit tests
9. **REPLACE strategy**: Simplifies upsert logic
10. **Migrations**: Always provide migration path
