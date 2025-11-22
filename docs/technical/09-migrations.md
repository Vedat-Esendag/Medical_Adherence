# Firestore Schema Evolution

## TL;DR
Firestore's flexible NoSQL structure makes schema changes easier than SQL migrations. Add fields with defaults, deprecate old fields gracefully, and handle multiple schema versions in the app code.

## Schema Evolution Philosophy

### No Migrations Required
Unlike SQL databases (Room), Firestore:
- **Doesn't enforce schemas**: Documents can have different fields
- **Handles missing fields**: Returns `null` or default values
- **Allows gradual rollout**: Old and new schemas coexist
- **No downtime**: Schema changes happen without database migrations

### When Schema Changes ARE Needed
While Firestore is flexible, you still need to handle:
- Adding new required fields
- Changing field types
- Renaming fields
- Removing deprecated fields (optional)
- Breaking changes in data structure

## Adding New Fields

### Simple Field Addition

**Scenario**: Add a `colorCode` field to medications

**Step 1: Update Firestore DTO with default**
```kotlin
data class FirestoreMedication(
    val id: String = "",
    val name: String = "",
    val dosage: String = "",
    // ... existing fields ...
    val colorCode: String = "#0D47A1"  // NEW FIELD with default
)
```

**Step 2: Update domain model**
```kotlin
data class Medication(
    val id: String,
    val name: String,
    val dosage: String,
    // ... existing fields ...
    val colorCode: String = "#0D47A1"  // NEW FIELD with default
)
```

**Step 3: Update mapping functions**
```kotlin
fun FirestoreMedication.toMedication() = Medication(
    id = id,
    name = name,
    dosage = dosage,
    // ... existing mappings ...
    colorCode = colorCode  // NEW MAPPING
)

fun Medication.toFirestoreDto() = FirestoreMedication(
    id = id,
    name = name,
    dosage = dosage,
    // ... existing mappings ...
    colorCode = colorCode  // NEW MAPPING
)
```

**Step 4: Deploy and use**
- Old documents: Read with default value `#0D47A1`
- New documents: Written with actual value
- No migration script needed!

### Handling Nullable vs Non-Nullable

**Option A: Nullable with null default (preferred)**
```kotlin
data class FirestoreMedication(
    // ... existing fields ...
    val reminderSound: String? = null  // Nullable, defaults to null
)
```
- Old documents return `null`
- New documents can set value
- App handles `null` case gracefully

**Option B: Non-nullable with default**
```kotlin
data class FirestoreMedication(
    // ... existing fields ...
    val priority: String = "normal"  // Non-nullable, defaults to "normal"
)
```
- Old documents return `"normal"`
- Simpler logic in app
- Choose default carefully

## Changing Field Types

### Safe Type Changes

**Scenario**: Change `times` from `List<String>` to `List<DoseTime>`

**Step 1: Add new field alongside old one**
```kotlin
data class FirestoreMedication(
    val times: List<String> = emptyList(),     // OLD FIELD
    val doseSchedule: List<Map<String, Any>> = emptyList()  // NEW FIELD
)
```

**Step 2: Write both fields for new documents**
```kotlin
fun Medication.toFirestoreDto() = FirestoreMedication(
    times = times.map { it.time },           // Keep for backward compatibility
    doseSchedule = times.map { doseTime ->   // New structured format
        mapOf(
            "time" to doseTime.time,
            "withFood" to doseTime.withFood
        )
    }
)
```

**Step 3: Read preferring new field**
```kotlin
fun FirestoreMedication.toMedication(): Medication {
    val doseTimes = if (doseSchedule.isNotEmpty()) {
        // Use new field if present
        doseSchedule.map { map ->
            DoseTime(
                time = map["time"] as String,
                withFood = map["withFood"] as? Boolean ?: false
            )
        }
    } else {
        // Fall back to old field
        times.map { DoseTime(time = it, withFood = false) }
    }
    
    return Medication(/* ... */, doseTimes = doseTimes)
}
```

**Step 4: Deprecate old field after grace period**
- After all users updated, stop writing old field
- Continue reading old field for legacy documents
- Optionally clean up old field with Cloud Function

### Unsafe Type Changes
Some changes require careful handling:

**String to Number**:
```kotlin
// Old: val dosage: String = "5 mg"
// New: val dosageAmount: Double = 5.0, dosageUnit: String = "mg"
```
- Add new fields
- Parse old field when present
- Handle parse errors gracefully

**Single to Array**:
```kotlin
// Old: val note: String = "Take with food"
// New: val notes: List<String> = listOf("Take with food", "Avoid alcohol")
```
- Read old field as single-item list
- Write new field as list

## Renaming Fields

### Approach 1: Dual-Write Strategy

**Step 1: Write to both old and new names**
```kotlin
val data = mapOf(
    "dosage" to "5 mg",      // OLD NAME
    "doseAmount" to "5 mg"   // NEW NAME
)
firestore.collection(...).document(id).set(data).await()
```

**Step 2: Read from new name, fall back to old**
```kotlin
fun FirestoreMedication.toMedication(): Medication {
    val dose = doseAmount.ifEmpty { dosage }  // Prefer new, fall back to old
    return Medication(/* ... */, dosage = dose)
}
```

**Step 3: Stop writing old name after grace period**

**Step 4: Remove old field reading logic later**

### Approach 2: Cloud Function Migration

For large-scale renames, use a Cloud Function:

```javascript
// Cloud Function to rename field across all documents
const admin = require('firebase-admin');
const db = admin.firestore();

exports.renameMedicationField = functions.https.onRequest(async (req, res) => {
    const usersSnapshot = await db.collection('users').get();
    
    const batch = db.batch();
    let count = 0;
    
    for (const userDoc of usersSnapshot.docs) {
        const medsSnapshot = await userDoc.ref.collection('medications').get();
        
        for (const medDoc of medsSnapshot.docs) {
            const data = medDoc.data();
            if (data.dosage && !data.doseAmount) {
                batch.update(medDoc.ref, {
                    doseAmount: data.dosage,
                    dosage: admin.firestore.FieldValue.delete()
                });
                count++;
            }
            
            if (count >= 500) {  // Firestore batch limit
                await batch.commit();
                count = 0;
            }
        }
    }
    
    if (count > 0) {
        await batch.commit();
    }
    
    res.send('Migration complete');
});
```

## Deprecating Fields

### Gradual Deprecation

**Phase 1: Stop writing deprecated field**
```kotlin
fun Medication.toFirestoreDto() = FirestoreMedication(
    // ... other fields ...
    // DON'T write oldFieldName anymore
)
```

**Phase 2: Continue reading for compatibility**
```kotlin
fun FirestoreMedication.toMedication(): Medication {
    // Still read oldFieldName if present
    val value = newFieldName ?: oldFieldName ?: defaultValue
}
```

**Phase 3: Remove reading logic (after grace period)**
```kotlin
fun FirestoreMedication.toMedication(): Medication {
    // Only read newFieldName
    val value = newFieldName ?: defaultValue
}
```

**Phase 4: Clean up old documents (optional)**
Use Cloud Function to remove old fields from existing documents.

## Version Gating

### Document Version Field

For complex schema changes, add a version field:

```kotlin
data class FirestoreMedication(
    val schemaVersion: Int = 2,  // Current schema version
    // ... other fields ...
)
```

**Reading with version check**:
```kotlin
fun FirestoreMedication.toMedication(): Medication {
    return when (schemaVersion) {
        1 -> parseMedicationV1(this)
        2 -> parseMedicationV2(this)
        else -> throw IllegalStateException("Unknown schema version: $schemaVersion")
    }
}

private fun parseMedicationV1(dto: FirestoreMedication): Medication {
    // Handle old schema
}

private fun parseMedicationV2(dto: FirestoreMedication): Medication {
    // Handle new schema
}
```

**When to use version field**:
- Multiple breaking changes at once
- Complex data structure transformations
- Need to track which schema version a document uses
- Want to force users to update app for old versions

## Handling Breaking Changes

### Strategy: Feature Flags + Min Version

**Step 1: Add feature flag to user profile**
```kotlin
data class FirestoreUserProfile(
    val appVersion: String = "1.0.0",
    val schemaVersion: Int = 1
)
```

**Step 2: Check version before using new features**
```kotlin
if (userProfile.schemaVersion >= 2) {
    // Use new feature
    displayMedicationReminders(medication.doseSchedule)
} else {
    // Fall back to old feature
    displayMedicationReminders(medication.times)
}
```

**Step 3: Require app update for critical changes**
```kotlin
val MIN_SUPPORTED_SCHEMA = 2

if (userProfile.schemaVersion < MIN_SUPPORTED_SCHEMA) {
    showUpdateRequiredDialog()
    return
}
```

## Migration from Room to Firestore

This app previously used Room database. Here's how the migration worked:

### One-Time Data Export

**Step 1: Export Room data before migration**
```kotlin
suspend fun exportRoomDataToFirestore() {
    val roomDatabase = AppDatabase.getDatabase(context)
    val medications = roomDatabase.medicationDao().getAllMedications()
    
    medications.forEach { med ->
        firestore
            .collection("users")
            .document(userId)
            .collection("medications")
            .document(med.id)
            .set(med.toFirestoreDto())
            .await()
    }
}
```

**Step 2: Remove Room dependencies**
- Delete Room entities, DAOs, AppDatabase
- Remove Room from `build.gradle.kts`
- Remove Room initialization code

**Step 3: Update all data access to use Firestore**
- Replace DAO calls with Firestore queries
- Convert Room `Flow` to Firestore listeners
- Update repository implementation

### Key Differences to Handle

| Aspect | Room | Firestore | Solution |
|--------|------|-----------|----------|
| IDs | Auto-increment | UUID/Custom | Generate UUIDs client-side |
| Relationships | Foreign keys | Subcollections | Use subcollections for hierarchical data |
| Queries | SQL | NoSQL | Rewrite queries using Firestore API |
| Transactions | SQL transactions | Firestore batch/transaction | Use batch writes or transactions |
| Migrations | Required | Optional | Add defaults to DTOs |

## Testing Schema Changes

### Unit Tests
```kotlin
@Test
fun `old schema documents parse correctly`() {
    val oldDto = FirestoreMedication(
        id = "123",
        name = "Aspirin",
        dosage = "81 mg"
        // colorCode field missing (old schema)
    )
    
    val medication = oldDto.toMedication()
    
    assertEquals("#0D47A1", medication.colorCode)  // Uses default
}

@Test
fun `new schema documents parse correctly`() {
    val newDto = FirestoreMedication(
        id = "123",
        name = "Aspirin",
        dosage = "81 mg",
        colorCode = "#FF0000"  // New field present
    )
    
    val medication = newDto.toMedication()
    
    assertEquals("#FF0000", medication.colorCode)  // Uses actual value
}
```

### Integration Tests with Firestore Emulator
```kotlin
@Before
fun setup() {
    // Use Firestore emulator for testing
    FirebaseFirestore.getInstance().useEmulator("10.0.2.2", 8080)
}

@Test
fun `mixed schema versions coexist`() = runTest {
    // Create old schema document
    firestore.collection("users").document("user1")
        .collection("medications").document("med1")
        .set(mapOf("name" to "Aspirin", "dosage" to "81 mg"))
        .await()
    
    // Create new schema document
    firestore.collection("users").document("user1")
        .collection("medications").document("med2")
        .set(mapOf(
            "name" to "Ibuprofen",
            "dosage" to "200 mg",
            "colorCode" to "#FF0000"
        ))
        .await()
    
    // Both should parse correctly
    val meds = repository.getMedications().first()
    assertEquals(2, meds.size)
    assertEquals("#0D47A1", meds[0].colorCode)  // Default
    assertEquals("#FF0000", meds[1].colorCode)  // Actual
}
```

## Best Practices

### 1. Always Use Defaults
```kotlin
data class FirestoreMedication(
    val name: String = "",           // ✅ Has default
    val notes: String? = null,       // ✅ Nullable with default
    val priority: String = "normal"  // ✅ Non-null with sensible default
)
```

### 2. Additive Changes First
- Add new fields before removing old ones
- Maintain backward compatibility for grace period
- Give users time to update

### 3. Communicate Breaking Changes
```kotlin
// In app code
if (userAppVersion < REQUIRED_MIN_VERSION) {
    showDialog(
        title = "Update Required",
        message = "Please update to the latest version to continue using the app."
    )
}
```

### 4. Test with Old Data
- Keep test documents with old schemas
- Verify app handles missing fields
- Test all parsing paths

### 5. Document Schema Changes
```kotlin
/**
 * Medication schema version 2 (added 2025-01-15)
 * Changes:
 * - Added colorCode field (default: "#0D47A1")
 * - Added priority field (default: "normal")
 * - Deprecated: oldDosageFormat (use dosage + unit instead)
 */
data class FirestoreMedication(/* ... */)
```

## Comparison: Room vs Firestore Migrations

| Aspect | Room Migrations | Firestore Evolution |
|--------|----------------|-------------------|
| Required | Yes, app crashes without | No, gracefully handles |
| Downtime | Can cause issues | Zero downtime |
| Testing | Complex | Simpler |
| Rollback | Difficult | Easy (deploy old app) |
| Multiple versions | One at a time | Many coexist |
| User impact | Must update immediately | Update when convenient |
| Data loss risk | High if migration fails | Low (defaults handle missing fields) |

## Resources

- [Firestore Data Model](https://firebase.google.com/docs/firestore/data-model)
- [Firestore Best Practices](https://firebase.google.com/docs/firestore/best-practices)
- [Cloud Functions for Firebase](https://firebase.google.com/docs/functions)
- [Firestore Emulator](https://firebase.google.com/docs/emulator-suite/connect_firestore)
