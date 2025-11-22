# Data Models & Firestore Documents

## TL;DR
Two main domain models: **Medication** (what to take) and **DoseEvent** (tracking when taken). Firestore DTOs handle serialization to/from cloud storage. Dual-role system supports both patients and caregivers.

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

**Location**: `Medication.kt`

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

### PatientProfile
User profile with role and authentication information.

**Location**: `app/src/main/java/com/example/medicaladherence/data/model/PatientProfile.kt`

```kotlin
data class PatientProfile(
    val userId: String,          // Firebase UID or offline ID
    val role: String,            // "patient" or "caregiver"
    val name: String,            // Display name
    val pin: String,             // 6-digit pairing PIN (patients only)
    val fcmToken: String? = null // For push notifications
)
```

## Firestore Document Models

### FirestoreUserProfile
User profile stored in Firestore `users/` collection.

**Location**: `app/src/main/java/com/example/medicaladherence/data/firebase/FirestoreModels.kt`

```kotlin
data class FirestoreUserProfile(
    val role: String = "",           // "patient" | "caregiver"
    val name: String = "",           // User's display name
    val pin: String = "",            // 6-digit pairing PIN
    val fcmToken: String? = null,    // FCM token for notifications
    val createdAt: Timestamp? = null // Account creation time
)
```

**Firestore Path**: `users/{userId}`

### FirestoreMedication
Medication document stored in user's medications subcollection.

**Location**: `FirestoreModels.kt`

```kotlin
data class FirestoreMedication(
    val id: String = "",
    val name: String = "",
    val dosage: String = "",
    val times: List<String> = emptyList(),
    val notes: String? = null,
    val frequency: String = "daily",
    val specificDays: List<Int> = emptyList(),
    val createdAt: Timestamp? = null
)
```

**Firestore Path**: `users/{userId}/medications/{medicationId}`

### FirestoreDoseEvent
Dose event document stored in user's doseEvents subcollection.

**Location**: `FirestoreModels.kt`

```kotlin
data class FirestoreDoseEvent(
    val medId: String = "",
    val date: String = "",           // ISO date string (yyyy-MM-dd)
    val time: String = "",           // HH:mm
    val taken: Boolean = false,
    val timestamp: Timestamp? = null
)
```

**Firestore Path**: `users/{userId}/doseEvents/{compositeId}`
- `compositeId` format: `{medId}_{date}_{time}`
- Example: `abc123_2025-01-15_07:00`

### FirestoreSettings
User settings stored in Firestore.

**Location**: `FirestoreModels.kt`

```kotlin
data class FirestoreSettings(
    val fontScale: Float = 1.0f,         // Font size multiplier
    val highContrastMode: Boolean = false, // High contrast theme
    val caretakerPin: String? = null     // Optional security PIN
)
```

**Firestore Path**: `users/{userId}/settings/app_settings`

### CaregiverLink
Links a caregiver to a patient for monitoring.

**Location**: `FirestoreModels.kt`

```kotlin
data class CaregiverLink(
    val caregiverUserId: String = "",
    val patientUserId: String = "",
    val patientPin: String = "",
    val patientName: String = "",
    val addedAt: Timestamp? = null,
    val displayName: String? = null,    // Caregiver's custom name for patient
    val phoneNumber: String? = null,
    val notes: String? = null
)
```

**Firestore Path**: `caregiver_links/{linkId}`
- Queryable by `caregiverUserId` to get all patients for a caregiver
- Queryable by `patientUserId` to get all caregivers for a patient

## Firestore Collections Structure

```
Firestore Database
│
├── users/
│   └── {userId}/                          (Document)
│       ├── Fields:
│       │   - role: "patient" | "caregiver"
│       │   - name: String
│       │   - pin: String (6-digit)
│       │   - fcmToken: String
│       │   - createdAt: Timestamp
│       │
│       ├── medications/                   (Subcollection)
│       │   └── {medicationId}/            (Document)
│       │       - id, name, dosage, times, notes
│       │       - frequency, specificDays, createdAt
│       │
│       ├── doseEvents/                    (Subcollection)
│       │   └── {compositeId}/             (Document)
│       │       - Format: "{medId}_{date}_{time}"
│       │       - medId, date, time, taken, timestamp
│       │
│       └── settings/                      (Subcollection)
│           └── app_settings/              (Document)
│               - fontScale, highContrastMode, caretakerPin
│
├── caregiver_links/                       (Top-level collection)
│   └── {linkId}/                          (Document)
│       - caregiverUserId, patientUserId
│       - patientPin, patientName
│       - addedAt, displayName, phoneNumber, notes
```

## Serialization & Deserialization

### Extension Functions
Conversion between domain models and Firestore DTOs.

**Location**: `app/src/main/java/com/example/medicaladherence/data/firebase/FirestoreExtensions.kt`

```kotlin
// Medication → Firestore
fun Medication.toFirestoreDto() = FirestoreMedication(
    id = id,
    name = name,
    dosage = dosage,
    times = times,
    notes = notes,
    frequency = frequency.name.lowercase(),
    specificDays = specificDays,
    createdAt = Timestamp.now()
)

// Firestore → Medication
fun FirestoreMedication.toMedication() = Medication(
    id = id,
    name = name,
    dosage = dosage,
    times = times,
    notes = notes,
    frequency = MedicationFrequency.valueOf(
        frequency.replaceFirstChar { it.uppercase() }
    ),
    specificDays = specificDays
)
```

**Why Separate?**
- Domain models have no Firebase dependencies
- DTOs are Firestore-specific with defaults for missing fields
- Easier to test domain logic
- Clear separation of concerns
- Backward compatibility when schema evolves

## Data Relationships

### Logical Relationships
```
User (1) → (N) Medications
User (1) → (N) DoseEvents  
User (1) → (N) CaregiverLinks

Medication (1) → (N) DoseEvents (via medId)
CaregiverLink (N) ← (1) Caregiver User
CaregiverLink (N) → (1) Patient User
```

### Firestore Implementation
- **Subcollections** for user-specific data (medications, doseEvents, settings)
- **Top-level collection** for many-to-many relationships (caregiver_links)
- **Composite IDs** for uniqueness (dose events)
- **Indexes** on query fields (caregiverUserId, patientPin)

### Cascade Operations
Handled in repository layer:

```kotlin
// FirebaseMedicationRepository.kt
suspend fun deleteMedication(medId: String) {
    // Delete medication document
    getCurrentUserDoc()
        .collection("medications")
        .document(medId)
        .delete()
        .await()
    
    // Delete all associated dose events
    getCurrentUserDoc()
        .collection("doseEvents")
        .whereEqualTo("medId", medId)
        .get()
        .await()
        .documents
        .forEach { it.reference.delete() }
}
```

## Query Patterns

### Common Queries

**Get user's medications**:
```kotlin
val medications: Flow<List<Medication>> = callbackFlow {
    val listener = firestore
        .collection("users")
        .document(userId)
        .collection("medications")
        .addSnapshotListener { snapshot, error ->
            // Convert and emit
        }
    awaitClose { listener.remove() }
}
```

**Find patient by PIN**:
```kotlin
suspend fun getPatientDataByPin(pin: String): PatientDataExport? {
    val users = firestore
        .collection("users")
        .whereEqualTo("pin", pin)
        .limit(1)
        .get()
        .await()
    
    return if (users.isEmpty) null
           else loadPatientData(users.documents[0].id)
}
```

**Get caregiver's patients**:
```kotlin
suspend fun getCaregiverPatients(): Flow<List<PatientData>> {
    val caregiverId = getCurrentUserId()
    return firestore
        .collection("caregiver_links")
        .whereEqualTo("caregiverUserId", caregiverId)
        .snapshots()
        .map { /* transform to patient data */ }
}
```

## Offline Behavior

### Cached Data
- Firestore automatically caches all read data
- Writes queue locally when offline
- App functions fully offline using cached data
- Real-time listeners continue working with cached data

### Conflict Resolution
- Firestore uses **last-write-wins** strategy
- Timestamp fields help identify most recent update
- Optimistic UI updates provide instant feedback
- Server changes reconcile automatically when online

## Sample Data

### Example Medication Document
```json
{
  "id": "abc-123",
  "name": "Amlodipine",
  "dosage": "5 mg",
  "times": ["07:00", "19:00"],
  "notes": "Take with food",
  "frequency": "daily",
  "specificDays": [],
  "createdAt": { "_seconds": 1729933200, "_nanoseconds": 0 }
}
```

### Example DoseEvent Document
```json
{
  "medId": "abc-123",
  "date": "2025-01-15",
  "time": "07:00",
  "taken": true,
  "timestamp": { "_seconds": 1736928000, "_nanoseconds": 0 }
}
```

### Example CaregiverLink Document
```json
{
  "caregiverUserId": "xyz789",
  "patientUserId": "abc123",
  "patientPin": "487392",
  "patientName": "Maria Garcia",
  "displayName": "Mom",
  "addedAt": { "_seconds": 1736928000, "_nanoseconds": 0 }
}
```

## Schema Evolution

### Adding New Fields
Firestore is schema-less, making evolution easy:

1. **Add field to DTO with default value**:
```kotlin
data class FirestoreMedication(
    // ... existing fields ...
    val colorCode: String = "#0D47A1"  // NEW FIELD
)
```

2. **Add field to domain model**:
```kotlin
data class Medication(
    // ... existing fields ...
    val colorCode: String = "#0D47A1"
)
```

3. **Update mapping functions**:
```kotlin
fun FirestoreMedication.toMedication() = Medication(
    // ... existing mappings ...
    colorCode = colorCode  // Maps new field
)
```

4. **Existing documents work automatically**:
- Old documents return default value for missing field
- No migration scripts needed
- No breaking changes for existing data

### Deprecating Fields
1. Add new field with default
2. Update app logic to use new field
3. Old field remains in old documents (harmless)
4. Optional: Run Cloud Function to clean up old field

### Breaking Changes
For truly breaking changes:
1. Add version field to documents
2. Check version in app before reading
3. Handle multiple versions gracefully
4. Prompt user to update app if needed
