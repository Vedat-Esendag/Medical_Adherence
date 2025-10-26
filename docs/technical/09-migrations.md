# Database Migrations

## TL;DR
Room database migrations handle schema changes. Currently using `.fallbackToDestructiveMigration()` (data loss). For production, implement proper migrations.

## Current Migration Strategy

### Fallback to Destructive Migration
**Location**: `AppDatabase.kt:42`

```kotlin
Room.databaseBuilder(
    context.applicationContext,
    AppDatabase::class.java,
    "medical_adherence_database"
)
    .fallbackToDestructiveMigration()  // ⚠️ Destroys data
    .build()
```

**Behavior**:
- Schema mismatch detected
- Database dropped completely
- New database created with current schema
- **All user data lost**

**Use Case**: Development/prototype phase only

## Production Migration Strategy

### Step-by-Step Migration

**1. Update Entity**
Modify entity class:
```kotlin
@Entity(tableName = "medications")
data class MedicationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val dosage: String,
    val times: List<String>,
    val notes: String?,
    val frequency: MedicationFrequency,
    val specificDays: List<Int>,
    val color: String = "#0D47A1"  // NEW FIELD
)
```

**2. Increment Database Version**
```kotlin
@Database(
    entities = [...],
    version = 2,  // Was 1
    exportSchema = true  // Enable schema export
)
```

**3. Create Migration**
```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE medications ADD COLUMN color TEXT NOT NULL DEFAULT '#0D47A1'"
        )
    }
}
```

**4. Add Migration to Database Builder**
```kotlin
Room.databaseBuilder(...)
    .addMigrations(MIGRATION_1_2)
    .build()
```

**5. Test Migration**
```kotlin
@Test
fun migrate1To2() {
    // Helper creates version 1 database
    val db = helper.createDatabase(TEST_DB, 1)

    // Insert old data
    db.execSQL(
        "INSERT INTO medications (id, name, dosage, ...) VALUES ('med-1', 'Aspirin', ...)"
    )
    db.close()

    // Run migration
    val migratedDb = helper.runMigrationsAndValidate(
        TEST_DB,
        2,
        true,
        MIGRATION_1_2
    )

    // Verify new column exists
    val cursor = migratedDb.query("SELECT color FROM medications WHERE id = 'med-1'")
    cursor.moveToFirst()
    assertEquals("#0D47A1", cursor.getString(0))
}
```

## Common Migration Scenarios

### Add Column
```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE medications ADD COLUMN priority INTEGER NOT NULL DEFAULT 0"
        )
    }
}
```

### Rename Column
SQLite doesn't support RENAME COLUMN (old versions):
```kotlin
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create new table with new column name
        database.execSQL("""
            CREATE TABLE medications_new (
                id TEXT PRIMARY KEY NOT NULL,
                medication_name TEXT NOT NULL,  -- renamed from 'name'
                dosage TEXT NOT NULL,
                ...
            )
        """)

        // Copy data
        database.execSQL("""
            INSERT INTO medications_new (id, medication_name, dosage, ...)
            SELECT id, name, dosage, ... FROM medications
        """)

        // Drop old table
        database.execSQL("DROP TABLE medications")

        // Rename new table
        database.execSQL("ALTER TABLE medications_new RENAME TO medications")
    }
}
```

### Add New Table
```kotlin
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE refills (
                id TEXT PRIMARY KEY NOT NULL,
                medId TEXT NOT NULL,
                refillDate INTEGER NOT NULL,
                pillsRemaining INTEGER NOT NULL,
                FOREIGN KEY (medId) REFERENCES medications(id) ON DELETE CASCADE
            )
        """)

        // Create index
        database.execSQL(
            "CREATE INDEX index_refills_medId ON refills(medId)"
        )
    }
}
```

### Drop Column
Requires table recreation:
```kotlin
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create table without unwanted column
        database.execSQL("""
            CREATE TABLE medications_new (
                id TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                dosage TEXT NOT NULL
                -- 'notes' column removed
            )
        """)

        // Copy data (excluding dropped column)
        database.execSQL("""
            INSERT INTO medications_new (id, name, dosage)
            SELECT id, name, dosage FROM medications
        """)

        database.execSQL("DROP TABLE medications")
        database.execSQL("ALTER TABLE medications_new RENAME TO medications")
    }
}
```

### Change Column Type
```kotlin
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Times: String → List<String>
        // Requires data transformation

        database.execSQL("""
            CREATE TABLE medications_new (
                id TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                dosage TEXT NOT NULL,
                times TEXT NOT NULL  -- Now stores JSON array
            )
        """)

        // Transform data
        database.execSQL("""
            INSERT INTO medications_new (id, name, dosage, times)
            SELECT id, name, dosage, '["' || time || '"]' FROM medications
        """)

        database.execSQL("DROP TABLE medications")
        database.execSQL("ALTER TABLE medications_new RENAME TO medications")
    }
}
```

## Migration Chaining

### Multiple Migrations
```kotlin
Room.databaseBuilder(...)
    .addMigrations(
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_3_4,
        MIGRATION_4_5
    )
    .build()
```

Room automatically chains:
- User on version 1 → Runs 1→2, 2→3, 3→4, 4→5
- User on version 3 → Runs 3→4, 4→5
- User on version 5 → No migration needed

## Schema Export

### Enable Export
```kotlin
@Database(
    entities = [...],
    version = 2,
    exportSchema = true
)
```

### Configure Output
In `app/build.gradle.kts`:
```kotlin
android {
    defaultConfig {
        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
            }
        }
    }
}
```

For KSP:
```kotlin
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
```

### Generated Schema Files
```
app/schemas/
├── 1.json    # Version 1 schema
├── 2.json    # Version 2 schema
└── 3.json    # Version 3 schema
```

**Commit schemas**: Track schema evolution in Git

## Testing Migrations

### Migration Test Helper
```kotlin
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun migrate1To2_containsCorrectData() {
        // Create version 1
        val db = helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                "INSERT INTO medications (id, name, dosage, times, notes, frequency, specificDays) " +
                "VALUES ('med-1', 'Aspirin', '81 mg', '[\"07:00\"]', null, 'Daily', '')"
            )
            close()
        }

        // Migrate to version 2
        val migratedDb = helper.runMigrationsAndValidate(
            TEST_DB,
            2,
            true,
            MIGRATION_1_2
        )

        // Verify
        val cursor = migratedDb.query("SELECT * FROM medications WHERE id = 'med-1'")
        cursor.moveToFirst()
        assertEquals("Aspirin", cursor.getString(cursor.getColumnIndex("name")))
        assertEquals("#0D47A1", cursor.getString(cursor.getColumnIndex("color")))
        cursor.close()
    }
}
```

## Migration Failures

### Missing Migration
If no migration path exists:
- Throws `IllegalStateException`
- App crashes on database access
- Requires reinstall or destructive migration

### Failed Migration
If SQL error in migration:
- Exception thrown
- Database in unknown state
- May require reinstall

### Rollback
Room doesn't support automatic rollback:
- Migrations must be idempotent if re-run
- Manual rollback requires new migration

## Best Practices

1. **Always test migrations**: Use MigrationTestHelper
2. **Export schemas**: Track in version control
3. **Incremental versions**: Don't skip version numbers
4. **Backup data**: Warn users before destructive changes
5. **Idempotent migrations**: Safe to re-run if failed
6. **Default values**: Provide for new NOT NULL columns
7. **Data validation**: Check data integrity after migration
8. **Fallback only in dev**: Never ship with fallbackToDestructiveMigration()

## Migration Checklist

Before releasing schema change:

- [ ] Update entity class
- [ ] Increment database version
- [ ] Create Migration object
- [ ] Add migration to builder
- [ ] Export schema
- [ ] Write migration test
- [ ] Test on old version device
- [ ] Remove fallbackToDestructiveMigration()
- [ ] Update documentation

## Production Deployment

### Pre-Release Testing
1. Install old version on test device
2. Add sample data
3. Install new version
4. Verify migration succeeded
5. Check all data present
6. Test app functionality

### Monitoring
After release:
- Monitor crash reports for migration errors
- Track database version distribution
- Be prepared to release hotfix if migration fails

## Complex Migration Example

### Scenario: Split name into firstName/lastName

```kotlin
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add new columns
        database.execSQL("ALTER TABLE medications ADD COLUMN displayName TEXT")
        database.execSQL("ALTER TABLE medications ADD COLUMN genericName TEXT")

        // Migrate data
        val cursor = database.query("SELECT id, name FROM medications")
        while (cursor.moveToNext()) {
            val id = cursor.getString(0)
            val fullName = cursor.getString(1)

            // Simple split logic (real app would be more complex)
            val parts = fullName.split(" ", limit = 2)
            val display = parts.getOrNull(0) ?: fullName
            val generic = parts.getOrNull(1) ?: ""

            database.execSQL(
                "UPDATE medications SET displayName = ?, genericName = ? WHERE id = ?",
                arrayOf(display, generic, id)
            )
        }
        cursor.close()

        // Remove old column (requires table recreation)
        database.execSQL("""
            CREATE TABLE medications_new (
                id TEXT PRIMARY KEY NOT NULL,
                displayName TEXT NOT NULL,
                genericName TEXT,
                dosage TEXT NOT NULL,
                times TEXT NOT NULL,
                notes TEXT,
                frequency TEXT NOT NULL,
                specificDays TEXT NOT NULL
            )
        """)

        database.execSQL("""
            INSERT INTO medications_new
            SELECT id, displayName, genericName, dosage, times, notes, frequency, specificDays
            FROM medications
        """)

        database.execSQL("DROP TABLE medications")
        database.execSQL("ALTER TABLE medications_new RENAME TO medications")
    }
}
```

## Future Enhancements

### Planned Schema Changes
Document potential future migrations:
- Add medication categories/tags
- Add dosage windows (time ranges)
- Add refill tracking
- Add medication interactions
- Add photo attachments

Planning ahead helps design migrations incrementally.
