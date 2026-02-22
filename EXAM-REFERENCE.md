# MSD Exam Reference — Medical Adherence Portfolio
> Full cheatsheet + exercises + MCQ answers. Ctrl+F to find anything.

---

## THIS APP — Architecture Quick Facts

- **Stack:** Kotlin + Jetpack Compose + Room + Manual DI (NO Hilt, NO Hilt, NO Hilt)
- **DI:** `RepositoryProvider` object singleton + `AppDatabase` object singleton (both `@Volatile` + `synchronized`)
- **5 ViewModels:** HomeViewModel, AddMedicationViewModel, MedicationsLibraryViewModel, StatsViewModel, SettingsViewModel
- **Data flow:** `MedicationDao.getAllMedications(): Flow<List<MedicationEntity>>` → Repository → ViewModel → `collectAsState()` → UI
- **NO FCM, NO WorkManager, NO BroadcastReceiver in the actual codebase**
- **Navigation:** Single Activity (`MainActivity`) + NavHost → Compose screens

### Real code locations
- `RepositoryProvider.kt` — DI singleton, `.getRepository()` in ViewModel default params
- `AppDatabase.kt` — Room DB singleton, 3 entities, 3 DAOs
- `MedicationDao.kt` — `getAllMedications(): Flow<List<MedicationEntity>>` (NOT suspend); writes are `suspend`
- `MedicalAdherenceApplication.kt` — calls `RepositoryProvider.provideRepository(applicationContext)` on startup

---

## WRITTEN EXAM Q1 — Pick 3+ of these

### Stateful vs Stateless (Compose)
- **Stateless:** Receives all data as params, emits events via callbacks. No internal state. Reusable + testable.
- **Stateful:** Owns its state (via `remember` or ViewModel). Manages complex state.
- **State hoisting:** Move state up to parent, pass `value` + `onValueChange` down.

```kotlin
// Stateless — pure function of inputs
@Composable
fun MedicationCard(medication: Medication, onDelete: () -> Unit) {
    Card { Text(medication.name); Button(onClick = onDelete) { Text("Delete") } }
}

// Stateful — owns the list from ViewModel
@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    state.medications.forEach { MedicationCard(it, onDelete = { viewModel.delete(it) }) }
}
```

### UDF — Unidirectional Data Flow
- State flows DOWN, events flow UP
- ViewModel emits UiState → Composable renders → User acts → Event sent to ViewModel → ViewModel updates state
- Predictable — state only changes in one place (ViewModel). Easy to test.

### ViewModel
- Survives configuration changes (rotation) — data not lost
- Exposes state via `StateFlow`. UI collects with `collectAsState()`
- Launches coroutines in `viewModelScope` — auto-cancelled when ViewModel is cleared
- Never pass Context, View, or Activity into ViewModel

```kotlin
class HomeViewModel(
    private val repo: MedicationRepository = RepositoryProvider.getRepository()
) : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo.getAllMedications().collect { meds ->
                _state.update { it.copy(medications = meds) }
            }
        }
    }
}
```

### Dependency Injection (Manual — this app)
- `RepositoryProvider` is a Kotlin `object` (singleton). Provides repository to ViewModels via default param.
- No Hilt in this codebase. Hilt uses `@HiltAndroidApp`, `@AndroidEntryPoint`, `@Inject`, `@Module`, `@InstallIn`, `@Provides`/`@Binds`.
- **@Provides vs @Binds:** `@Provides` = write code to create object. `@Binds` = abstract method connecting interface to impl. NOT interchangeable.

```kotlin
object RepositoryProvider {
    @Volatile private var instance: MedicationRepository? = null

    fun provideRepository(context: Context) =
        instance ?: synchronized(this) {
            instance ?: MedicationRepository(
                AppDatabase.getDatabase(context).medicationDao()
            ).also { instance = it }
        }

    fun getRepository() = instance!!
}
```

### Room Database with Flow
- 3 parts: `@Entity` (table) + `@Dao` (queries) + `@Database` (room builder)
- Reads return `Flow<List<T>>` (NOT suspend) — Room emits new values on any DB change
- Writes are `suspend fun` — run in coroutines
- Room maps DB rows to Kotlin objects via `@Entity` (MCQ Q17)

```kotlin
@Entity(tableName = "medications")
data class MedicationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val doseMg: Int
)

@Dao
interface MedicationDao {
    @Query("SELECT * FROM medications")
    fun getAllMedications(): Flow<List<MedicationEntity>> // NOT suspend

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(medication: MedicationEntity)

    @Delete
    suspend fun delete(medication: MedicationEntity)
}
```

### Permission Handling
- **Normal permissions:** Auto-granted at install. Example: `INTERNET`
- **Dangerous permissions:** User approves at runtime (Android 6+). Example: `CAMERA`, `POST_NOTIFICATIONS` (Android 13+)
- Check → request → handle result. Show rationale if denied once. Send to Settings if permanently denied.

```kotlin
if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA)
    != PackageManager.PERMISSION_GRANTED) {
    ActivityCompat.requestPermissions(activity,
        arrayOf(Manifest.permission.CAMERA), REQUEST_CODE)
}
```

### Sensors (class exercise — not in portfolio)
*"Medical_Adherence doesn't use sensors. In the W45 class exercise I implemented gyroscope + step counter access."*

- 4 components: SensorManager, Sensor, SensorEvent, SensorEventListener
- Register in `onResume()`, unregister in `onPause()` — critical for battery
- MCQ Q13: Register onResume, unregister onPause = only collects when Activity visible

```kotlin
class SensorActivity : ComponentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    override fun onResume() {
        super.onResume()
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }
    override fun onPause() { super.onPause(); sensorManager.unregisterListener(this) }

    override fun onSensorChanged(event: SensorEvent?) {
        when (event?.sensor?.type) {
            Sensor.TYPE_GYROSCOPE -> { val x=event.values[0]; val y=event.values[1]; val z=event.values[2] }
            Sensor.TYPE_STEP_COUNTER -> { val steps = event.values[0].toInt() }
        }
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
```

---

## WRITTEN EXAM Q2 — UCD Reflection Structure

Answer in this order:
1. **Understanding users:** Interviews, surveys, personas. What you found about medication adherence pain points.
2. **Specifying requirements:** How insights → requirements. Example: "Forgot afternoon dose → configurable reminder times."
3. **Creating design solutions:** Wireframes, Figma prototype, user needs shaped choices (large buttons for elderly).
4. **Evaluating with users:** Usability testing, feedback, what changed.
5. **Iterative importance:** 1 concrete feature that evolved through iteration.
6. **Critique of UCD:** Strengths = user focus, structured. Weaknesses = time-consuming, hard to balance needs vs technical constraints.
7. **If starting over:** Earlier usability testing, more diverse user group.

> Min 3 examples per section. Fewer = proportional mark deduction.

**UCD 4-step loop:** Understand context → Specify requirements → Design solutions → Evaluate (repeat)
**ISO 9241-210:** The Human-Centred Design standard. 6 principles including: involve users throughout, iterate, multidisciplinary team.
**Garrett's 5 Planes (bottom→top):** Strategy → Scope → Structure → Skeleton → Surface

---

## WRITTEN EXAM Q3 — FCM

*"Medical_Adherence does not implement FCM. In the W46 class exercise I implemented FirebaseMessagingService."*

### FCM Architecture
App → FCM SDK → Registration Token → App sends token to Your Server → Your Server → FCM API → Device

### Two payload types
- **Notification payload:** FCM shows automatically in system tray when app is background/killed
- **Data payload:** Always delivered to `onMessageReceived()` — your app controls display

### Delivery matrix (the whole answer)
| App State | Notification Payload | Data Payload | onMessageReceived called? |
|-----------|---------------------|--------------|--------------------------|
| Foreground | NOT auto-shown — goes to onMessageReceived | Delivered | YES — always |
| Background | AUTO system tray | Delivered | YES — for data |
| Killed | AUTO system tray | HIGH priority only | YES if HIGH priority |
| Force-Stopped | BLOCKED | BLOCKED | NO — never |

### Class exercise code
```kotlin
class MedicationMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        sendTokenToServer(token) // Server uses this to target this device
    }
    override fun onMessageReceived(message: RemoteMessage) {
        // Notification payload — in foreground, must show manually
        message.notification?.let { showNotification(it.title ?: "", it.body ?: "") }
        // Data payload — always comes here regardless of app state
        if (message.data.isNotEmpty()) {
            showNotification(message.data["title"] ?: "", message.data["body"] ?: "")
        }
    }
}
// Manifest: <service android:name=".MedicationMessagingService" android:exported="false">
//   <intent-filter><action android:name="com.google.firebase.MESSAGING_EVENT"/></intent-filter>
// </service>
```

---

## CLASS EXERCISES (all 5 — for Q1 gaps)

### W44 — Foreground Service
```kotlin
class FileDownloadService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, buildNotification("Download starting...")) // MUST call immediately
        serviceScope.launch {
            for (progress in 0..100 step 10) { updateNotification("$progress%"); delay(500) }
            stopSelf()
        }
        return START_NOT_STICKY
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
// Manifest: FOREGROUND_SERVICE permission + POST_NOTIFICATIONS (Android 13+)
// Start: ContextCompat.startForegroundService(context, Intent(context, FileDownloadService::class.java))
```
**Key:** Must call `startForeground()` immediately. Needs `FOREGROUND_SERVICE` permission.
**Justification:** "App uses local notifications. Foreground service would handle cloud sync."

### W45 — BroadcastReceiver
```kotlin
class WifiReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == ConnectivityManager.CONNECTIVITY_ACTION && isWifiConnected(context!!))
            context.startForegroundService(Intent(context, FileDownloadService::class.java))
    }
}
// Dynamic: registerReceiver(receiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)) in onStart()
// MUST: unregisterReceiver(receiver) in onStop() — memory leak otherwise
// MCQ Q12: Receiver object only valid during onReceive(). NOT on background thread automatically.
```
**Justification:** "No server sync in app. A BootReceiver would reschedule alarms after device restart."

### W45 — Sensors
```kotlin
// Register onResume, unregister onPause (MCQ Q13 answer)
sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL)
// onSensorChanged: event.values[0,1,2] = x/y/z for gyroscope (rad/s)
// TYPE_STEP_COUNTER = cumulative steps since boot
```
**Justification:** "Could use accelerometer to detect sleep and suppress late-night reminders."

### W46 — WorkManager + CoroutineWorker
```kotlin
class MedicationCheckWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result { // suspend = can call Room/Ktor directly
        val taken = RepositoryProvider.getRepository().isAfternoonDoseTaken()
        if (!taken) sendReminderNotification("Take afternoon medication!")
        return Result.success()
    }
}
// Schedule: PeriodicWorkRequestBuilder<MedicationCheckWorker>(15, TimeUnit.MINUTES)
// MINIMUM INTERVAL = 15 MINUTES (MCQ Q10 answer)
// enqueueUniquePeriodicWork("name", ExistingPeriodicWorkPolicy.KEEP, request)
// MCQ Q11: CoroutineWorker provides suspending doWork() — can call suspend functions
```

### W46 — FCM (see Q3 section above)

---

## ANDROID FUNDAMENTALS

### Activity Lifecycle (in order)
`onCreate` → `onStart` → `onResume` → `onPause` → `onStop` → `onDestroy`
- MCQ Q18: Foreground + interactive = `onResume()`
- MCQ Q20: Heads-up notification (partial cover) = `onPause()`
- Config change (rotation): destroyed + recreated, ViewModel survives

### 4 Android Components
- **Activity:** Screen with UI. Backstack.
- **Service:** Background, no UI. Foreground/Background/Bound.
- **BroadcastReceiver:** System/app events. Short-lived `onReceive()`.
- **Content Provider:** Structured data sharing between apps.

### Intents
- **Explicit:** Specify exact component. `Intent(ctx, DetailActivity::class.java)`
- **Implicit:** System chooses. `Intent(Intent.ACTION_SEND)` → user picks app
- MCQ Q19: Explicit = specify component; Implicit = system chooses

### Coroutines + Dispatchers
- `Main` = UI thread, `IO` = network/disk, `Default` = CPU-heavy
- MCQ Q22: Parsing JSON (CPU) → `Default`. Uploading (network) → `IO`
- `suspend fun` = only callable from coroutine or another suspend fun

### Single Activity Architecture (MCQ Q23)
- One Activity + NavHost for all screens. Answer = "2 only" = simplifies navigation and state management.
- Does NOT eliminate ViewModels. Does NOT run screens in own process.

### Compose Recomposition (MCQ Q21)
- Rerenders when observed state values change (`State<T>`, `StateFlow`, `LiveData`)
- NOT every second, NOT on rotation only, NOT via `invalidate()`

---

## MCQ ANSWERS — ALL 23

| Q | A | Key reason |
|---|---|-----------|
| 1 | C | Cross-platform = shared code + near-native; hybrid trades performance for speed |
| 2 | A | Content-driven, minimal device interaction → no reason for native |
| 3 | A | Contextual Inquiry = observe in natural environment = best for discovery phase |
| 4 | D | StatelessWidget immutable; StatefulWidget = 2 classes; build() on State; stateless more efficient |
| 5 | A | MethodChannel = request/response (one call, one reply) |
| 6 | C | ValueNotifier/BLoC = granular rebuilds, better efficiency than setState() |
| 7 | B | expect/actual better for platform-specific top-level functions + constructor types |
| 8 | B | Lifecycle management differs Android vs iOS — main challenge for shared ViewModel |
| 9 | D | sourceSets.commonMain.dependencies — shared code goes in commonMain |
| 10 | B | 15 minutes minimum for PeriodicWorkRequest |
| 11 | A | CoroutineWorker provides suspending doWork() → can call other suspend functions |
| 12 | B | Receiver object only valid during onReceive() call |
| 13 | B | Register onResume, unregister onPause — data only when Activity visible |
| 14 | B | Data-only + Killed = delivered ONLY if HIGH priority |
| 15 | D | I (stateless=reusable) + II (caller controls) + IV (supports UDF) |
| 16 | C | @Provides = code to create; @Binds = connect interface to impl. Both 1 and 2 correct. |
| 17 | A | Room maps DB rows to Kotlin objects via @Entity |
| 18 | D | onResume() = foreground + interactive |
| 19 | C | Explicit = specify component; Implicit = system chooses |
| 20 | B | Heads-up notification (partially covers screen) → onPause() |
| 21 | D | Rerenders when observed state values change |
| 22 | B | Parsing (CPU) → Dispatchers.Default; Uploading (I/O) → Dispatchers.IO |
| 23 | C | 2 only — simplifies navigation and state management across the app |

---

## UCD QUICK REFERENCE

- **ISO 9241-210:** HCD standard. Involve users throughout, iterate, multidisciplinary team.
- **4-step loop:** Understand context → Specify requirements → Design solutions → Evaluate
- **Garrett's 5 Planes (bottom→top):** Strategy → Scope → Structure → Skeleton → Surface
- **Contextual Inquiry:** Observe users in natural environment. Best for discovery. (MCQ Q3 = A)
- **Persona:** Data-grounded fictional user. Prevents designing for yourself.
- **User story:** As a [user] I want [action] so that [goal]
- **Critique of UCD:** Time-consuming; hard to get representative users; balancing user needs vs technical constraints
