# Testing Strategy

## TL;DR
JUnit for unit tests, Espresso + Compose UI Test for instrumented tests. Test ViewModels, repositories, and key UI flows. Room in-memory database for data layer tests.

## Testing Stack

### Unit Testing
- **JUnit**: 4.13.2
- **Kotlin Test**: Built-in assertions
- **Coroutines Test**: `kotlinx-coroutines-test`

### Instrumented Testing
- **AndroidX JUnit**: 1.3.0
- **Espresso Core**: 3.7.0
- **Compose UI Test**: From Compose BOM

**Location**: Dependencies in `app/build.gradle.kts:69-75`

## Test Structure

### Directory Layout
```
app/src/
├── test/                           # Unit tests (JVM)
│   └── java/com/example/medicaladherence/
│       ├── viewmodel/
│       │   ├── HomeViewModelTest.kt
│       │   └── StatsViewModelTest.kt
│       ├── repository/
│       │   └── MedicationRepositoryTest.kt
│       └── model/
│           └── MedicationTest.kt
│
└── androidTest/                    # Instrumented tests (Device)
    └── java/com/example/medicaladherence/
        ├── ui/
        │   ├── HomeScreenTest.kt
        │   └── AddMedicationScreenTest.kt
        └── database/
            └── MedicationDaoTest.kt
```

## Unit Testing

### ViewModel Tests
Test state management and business logic.

**Example**: `HomeViewModelTest.kt`
```kotlin
class HomeViewModelTest {

    private lateinit var fakeRepository: FakeMedicationRepository
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        fakeRepository = FakeMedicationRepository()
        viewModel = HomeViewModel(fakeRepository)
    }

    @Test
    fun `initial state has empty doses list`() {
        val state = viewModel.uiState.value
        assertTrue(state.todayDoses.isEmpty())
    }

    @Test
    fun `markTaken updates state and repository`() = runTest {
        // Given
        val medId = "med-1"
        val time = "07:00"

        // When
        viewModel.markTaken(medId, time)

        // Then
        assertEquals("Marked as taken", viewModel.uiState.value.snackbarMessage)
        assertTrue(fakeRepository.markedDoses.contains(medId to time))
    }

    @Test
    fun `calculateWeeklyAdherence returns correct percentage`() = runTest {
        // Given
        fakeRepository.setAdherence(85)

        // When
        viewModel.loadData()

        // Then
        assertEquals(85, viewModel.uiState.value.weeklyAdherencePercent)
    }
}
```

### Repository Tests
Test data operations and transformations.

**Example**: `MedicationRepositoryTest.kt`
```kotlin
class MedicationRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: MedicationRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).build()
        repository = MedicationRepository(database)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `addMedication saves to database`() = runTest {
        // Given
        val medication = Medication(
            id = "test-1",
            name = "Aspirin",
            dosage = "81 mg",
            times = listOf("07:00")
        )

        // When
        repository.addOrUpdateMedication(medication)

        // Then
        val saved = repository.getMedicationById("test-1")
        assertEquals("Aspirin", saved?.name)
    }

    @Test
    fun `deleteMedication removes medication and events`() = runTest {
        // Given
        val medication = createTestMedication("med-1")
        repository.addOrUpdateMedication(medication)
        repository.markDose("med-1", LocalDate.now(), "07:00", true)

        // When
        repository.deleteMedication("med-1")

        // Then
        assertNull(repository.getMedicationById("med-1"))
        val events = repository.getTodayDoses()
        assertTrue(events.none { it.first.id == "med-1" })
    }
}
```

### Model Tests
Test data class behavior and validation.

```kotlin
class MedicationTest {

    @Test
    fun `medication with empty name is invalid`() {
        // Validation logic in AddMedicationViewModel
        // Could extract to separate validator class for testing
    }

    @Test
    fun `medication frequency enum has all expected values`() {
        val frequencies = MedicationFrequency.values()
        assertTrue(frequencies.contains(MedicationFrequency.Daily))
        assertTrue(frequencies.contains(MedicationFrequency.SpecificDays))
    }
}
```

## Instrumented Testing

### DAO Tests
Test database operations on real SQLite.

**Example**: `MedicationDaoTest.kt`
```kotlin
@RunWith(AndroidJUnit4::class)
class MedicationDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: MedicationDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).build()
        dao = database.medicationDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndGetMedication() = runTest {
        // Given
        val entity = MedicationEntity(
            id = "test-1",
            name = "Amlodipine",
            dosage = "5 mg",
            times = listOf("07:00"),
            notes = null,
            frequency = MedicationFrequency.Daily,
            specificDays = emptyList()
        )

        // When
        dao.insertMedication(entity)
        val retrieved = dao.getMedicationById("test-1")

        // Then
        assertNotNull(retrieved)
        assertEquals("Amlodipine", retrieved?.name)
    }

    @Test
    fun getAllMedicationsReturnsFlow() = runTest {
        // When
        val flow = dao.getAllMedications()
        val medications = flow.first()

        // Then
        assertTrue(medications.isEmpty())
    }
}
```

### Compose UI Tests
Test screen behavior and interactions.

**Example**: `HomeScreenTest.kt`
```kotlin
@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var fakeViewModel: FakeHomeViewModel

    @Before
    fun setup() {
        fakeViewModel = FakeHomeViewModel()
    }

    @Test
    fun emptyStateShowsMessage() {
        composeTestRule.setContent {
            MedicalAdherenceTheme {
                HomeScreen(
                    onNavigateToAdd = {},
                    onNavigateToEdit = {},
                    viewModel = fakeViewModel
                )
            }
        }

        composeTestRule
            .onNodeWithText("No medications yet")
            .assertIsDisplayed()
    }

    @Test
    fun doseCardDisplaysMedicationInfo() {
        // Given
        fakeViewModel.setDoses(listOf(
            DoseItem(
                medication = Medication(
                    id = "med-1",
                    name = "Amlodipine",
                    dosage = "5 mg",
                    times = listOf("07:00")
                ),
                time = "07:00",
                taken = null
            )
        ))

        composeTestRule.setContent {
            HomeScreen(viewModel = fakeViewModel)
        }

        // Then
        composeTestRule.onNodeWithText("Amlodipine").assertIsDisplayed()
        composeTestRule.onNodeWithText("5 mg at 07:00").assertIsDisplayed()
        composeTestRule.onNodeWithText("Taken").assertIsDisplayed()
    }

    @Test
    fun clickingTakenButtonCallsViewModel() {
        // Setup dose
        fakeViewModel.setDoses(listOf(testDose))

        composeTestRule.setContent {
            HomeScreen(viewModel = fakeViewModel)
        }

        // When
        composeTestRule.onNodeWithText("Taken").performClick()

        // Then
        assertTrue(fakeViewModel.markTakenCalled)
    }
}
```

### Navigation Tests
Test screen navigation flows.

```kotlin
@Test
fun fabNavigatesToAddScreen() {
    composeTestRule.setContent {
        val navController = rememberNavController()
        MedicalAdherenceApp(navController)
    }

    // Click FAB
    composeTestRule
        .onNodeWithContentDescription("Add Medication")
        .performClick()

    // Verify on Add screen
    composeTestRule
        .onNodeWithText("Add Medication")
        .assertIsDisplayed()
}
```

## Test Doubles

### Fake Repository
For ViewModel tests.

```kotlin
class FakeMedicationRepository : MedicationRepository {

    private val medicationsFlow = MutableStateFlow<List<Medication>>(emptyList())
    val markedDoses = mutableListOf<Pair<String, String>>()

    override val medications: Flow<List<Medication>> = medicationsFlow

    override suspend fun markDose(
        medId: String,
        date: LocalDate,
        time: String,
        taken: Boolean
    ) {
        markedDoses.add(medId to time)
    }

    fun setMedications(meds: List<Medication>) {
        medicationsFlow.value = meds
    }

    fun setAdherence(percentage: Int) {
        // Mock adherence calculation
    }
}
```

### Fake ViewModel
For UI tests.

```kotlin
class FakeHomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    var markTakenCalled = false

    fun setDoses(doses: List<DoseItem>) {
        _uiState.value = _uiState.value.copy(todayDoses = doses)
    }

    fun markTaken(medId: String, time: String) {
        markTakenCalled = true
    }
}
```

## Coroutines Testing

### runTest
For suspending functions.

```kotlin
@Test
fun `suspending function test`() = runTest {
    // Given
    val repository = FakeMedicationRepository()

    // When
    repository.markDose("med-1", LocalDate.now(), "07:00", true)

    // Then
    assertEquals(1, repository.markedDoses.size)
}
```

### TestDispatcher
For controlling coroutine execution.

```kotlin
@Test
fun `test with controlled time`() = runTest {
    val testDispatcher = StandardTestDispatcher()

    viewModelScope.launch(testDispatcher) {
        delay(1000)
        // assertions
    }

    advanceTimeBy(1000)
    // verify delayed actions
}
```

## Testing Best Practices

### Unit Tests
1. **Fast**: No device/emulator needed
2. **Isolated**: Test one unit at a time
3. **Deterministic**: Same input = same output
4. **AAA Pattern**: Arrange, Act, Assert

### Instrumented Tests
1. **Use in-memory database**: Faster than real DB
2. **Test real Android behavior**: Lifecycle, View rendering
3. **Compose test rules**: `createComposeRule()`
4. **Content descriptions**: Add for testability

### General
1. **Test behavior, not implementation**: Don't test private methods
2. **One assertion per test**: (guideline, not rule)
3. **Descriptive test names**: `markTaken_updatesStateAndRepository()`
4. **Setup/teardown**: Use `@Before` and `@After`
5. **Test edge cases**: Empty lists, null values, errors

## Running Tests

### All Tests
```bash
./gradlew test                    # Unit tests
./gradlew connectedAndroidTest    # Instrumented tests
./gradlew connectedCheck           # All tests
```

### Specific Test
```bash
./gradlew test --tests "HomeViewModelTest"
./gradlew test --tests "HomeViewModelTest.markTaken_updatesState"
```

### With Coverage
```bash
./gradlew testDebugUnitTestCoverage
```

Coverage report: `app/build/reports/coverage/`

## Continuous Integration

### GitHub Actions Example
```yaml
- name: Run unit tests
  run: ./gradlew test

- name: Upload test results
  uses: actions/upload-artifact@v3
  if: always()
  with:
    name: test-results
    path: app/build/test-results/
```

## Test Coverage Goals

### Target Coverage
- **ViewModels**: 80%+ (core business logic)
- **Repository**: 80%+ (data operations)
- **DAOs**: 70%+ (database queries)
- **UI**: 60%+ (key user flows)

### Not Everything Needs Tests
Skip testing:
- Data classes (unless complex logic)
- Simple getters/setters
- Framework code
- UI layouts (use manual/visual testing)

## Screenshot Testing

### Not Currently Implemented
Could add with Paparazzi or Shot:

```kotlin
@Test
fun homeScreenSnapshot() {
    paparazzi.snapshot {
        HomeScreen(viewModel = fakeViewModel)
    }
}
```

## Future Testing Improvements

1. **Increase coverage**: Add more ViewModel and Repository tests
2. **UI test suite**: Cover all key user journeys
3. **Screenshot tests**: Regression testing for UI
4. **Integration tests**: End-to-end flows
5. **Performance tests**: Database query speed
6. **Accessibility tests**: TalkBack compatibility
