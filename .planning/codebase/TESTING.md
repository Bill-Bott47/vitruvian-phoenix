# Testing Patterns

**Analysis Date:** 2026-01-21

## Test Framework

**Runner:**
- JUnit 4 (via `org.junit.Test`, `org.junit.Before`, `org.junit.After`, `org.junit.Rule`)
- Kotlinx Coroutines Test (kotlinx.coroutines.test) for suspend functions
- Gradle test tasks: `./gradlew :androidApp:testDebugUnitTest`, `./gradlew :shared:testDebugUnitTest`

**Assertion Library:**
- Google Truth (com.google.truth:truth) - fluent assertions via `assertThat()`
- Kotlin Test (kotlin.test) - `assertEquals()`, `assertNull()`, `assertTrue()`, `assertFalse()`
- CustomTestMatcher syntax not observed - uses standard assertion methods

**Run Commands:**
```bash
./gradlew :shared:testDebugUnitTest           # Run shared module unit tests
./gradlew :androidApp:testDebugUnitTest       # Run app-specific unit tests
./gradlew :androidApp:connectedAndroidTest    # Run instrumented (androidTest) tests
./gradlew test                                 # Run all tests (includes unit + instrumented)
```

**Test Coverage:**
- Turbine (app.cash.turbine) for Flow testing: `flow.test { ... cancelAndIgnoreRemainingEvents() }`
- No explicit coverage reports configured
- Target: Repository layer and critical domain logic covered
- E2E tests use Compose test API for UI testing

## Test File Organization

**Location:**
- Unit tests: `shared/src/androidUnitTest/kotlin/` for shared module domain/data logic
- App unit tests: `androidApp/src/test/kotlin/` for app-specific rules and utilities
- Instrumented tests: `androidApp/src/androidTest/kotlin/` for UI and integration testing
- Test doubles: `androidApp/src/androidTest/kotlin/com/devil/phoenixproject/testutil/` (shared with androidUnitTest)

**Naming:**
- Test classes: `[Tested]Test.kt` (e.g., `SqlDelightWorkoutRepositoryTest.kt`)
- E2E tests: `[Feature]E2ETest.kt` (e.g., `AppE2ETest.kt`, `WorkoutFlowE2ETest.kt`)
- Test doubles: `Fake[Interface].kt` (e.g., `FakeBleRepository.kt`, `FakeWorkoutRepository.kt`)
- Test rules: `[Feature]Rule.kt` (e.g., `TestCoroutineRule.kt`)

**Structure:**
```
shared/src/androidUnitTest/kotlin/com/devil/phoenixproject/
├── data/repository/                  # Repository tests
│   ├── SqlDelightWorkoutRepositoryTest.kt
│   ├── SqlDelightExerciseRepositoryTest.kt
│   └── ...
├── e2e/                              # End-to-end workflow tests
│   └── WorkoutFlowE2ETest.kt
└── testutil/                         # Test doubles and factories
    ├── Fake*.kt                      # Fake implementations
    └── TestDatabaseFactory.android.kt
```

## Test Structure

**Test Suite Organization:**

```kotlin
class SqlDelightWorkoutRepositoryTest {

    private lateinit var database: com.devil.phoenixproject.database.VitruvianDatabase
    private lateinit var exerciseRepository: FakeExerciseRepository
    private lateinit var repository: SqlDelightWorkoutRepository

    @Before
    fun setup() {
        database = createTestDatabase()
        exerciseRepository = FakeExerciseRepository()
        repository = SqlDelightWorkoutRepository(database, exerciseRepository)
    }

    // ========== Session CRUD Tests ==========

    @Test
    fun `saveSession persists session to database`() = runTest {
        val session = createTestSession(id = "test-session-001")

        repository.saveSession(session)

        val retrieved = repository.getSession("test-session-001")
        assertEquals("test-session-001", retrieved?.id)
    }

    // ========== Helper Methods ==========

    private fun createTestSession(
        id: String = "test-session",
        timestamp: Long = System.currentTimeMillis()
    ) = WorkoutSession(...)
}
```

**Test Method Pattern:**
1. Describe behavior in backtick-quoted function name: `` `saveSession persists session to database`() ``
2. Setup: Create test data using factories (createTestSession)
3. Execute: Call the method being tested
4. Assert: Verify behavior with fluent assertions
5. Cleanup: Automatic via @Before/@After lifecycle

**Setup/Teardown:**
- `@Before fun setup()` - Initialize test doubles and system under test
- `@After fun tearDown()` - Optional cleanup (usually automatic with fixtures)
- Koin test setup: `stopKoin()` then `startKoin { modules(testModule) }`

## Async Testing Patterns

**Coroutine Testing:**

```kotlin
@Test
fun `async operation completes correctly`() = runTest {
    // runTest provides test dispatcher and automatically manages time
    val result = repository.saveSession(testSession)
    advanceUntilIdle()  // Run pending coroutines

    assertEquals("saved-id", result?.id)
}
```

**Flow Testing with Turbine:**

```kotlin
@Test
fun `getAllSessions returns all saved sessions`() = runTest {
    repository.saveSession(createTestSession(id = "session-1"))

    repository.getAllSessions().test {
        val sessions = awaitItem()
        assertEquals(1, sessions.size)
        cancelAndIgnoreRemainingEvents()
    }
}
```

**StateFlow Testing:**

```kotlin
@Test
fun `connection state updates when connected`() = runTest {
    fake.simulateConnect("Vee_MyTrainer")
    advanceUntilIdle()

    assertTrue(fake.connectionState.value is ConnectionState.Connected)
}
```

**MainClock for Animation Timing (Compose E2E):**

```kotlin
@Test
fun `splash screen fades in correctly`() {
    composeRule.mainClock.autoAdvance = false
    composeRule.setContent { App() }

    composeRule.mainClock.advanceTimeBy(SPLASH_DURATION_MS)
    composeRule.waitForIdle()

    composeRule.onNodeWithText("PROJECT PHOENIX").assertDoesNotExist()
}
```

## Mocking & Test Doubles

**Framework:** No explicit mocking library required for this codebase

**Strategy:** Fake implementations (in-memory repositories) instead of mocks
- Cleaner assertions without `.verify()` chains
- Better for testing state interactions
- Easier to debug than mock behavior verification

**Fake Repository Pattern:**

```kotlin
@Suppress("unused") // Test utility methods available for future tests
class FakeBleRepository : BleRepository {

    // Internal state for control
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // Track calls for verification
    val commandsReceived = mutableListOf<ByteArray>()

    // Control behavior
    var scanResult: Result<Unit> = Result.success(Unit)
    var shouldFailConnect = false

    // Test control methods
    fun simulateConnect(deviceName: String) {
        _connectionState.value = ConnectionState.Connected(deviceName = deviceName)
    }

    fun reset() {
        _connectionState.value = ConnectionState.Disconnected
        commandsReceived.clear()
    }

    // ========== Interface Implementation ==========

    override suspend fun startScanning(): Result<Unit> {
        if (scanResult.isSuccess) {
            _connectionState.value = ConnectionState.Scanning
        }
        return scanResult
    }
}
```

**Robot Pattern for E2E (Readable Test DSL):**

```kotlin
@Test
fun `complete connection flow`() = runTest {
    workoutRobot(viewModel, fakeBleRepository) {
        // Fluent test helper methods
        verifyDisconnected()

        connectToDevice("Vee_MyTrainer")
        advanceUntilIdle()

        verifyConnected()
        verifyConnectedTo("Vee_MyTrainer")
    }
}
```

**What to Fake:**
- Repository interfaces (always use Fake* in tests)
- Remote services (BLE, network APIs)
- Time-dependent operations (clock, delays)
- External dependencies (file system, preferences)

**What NOT to Fake:**
- Domain logic / use cases (test real implementation)
- Data models and builders
- Database (use in-memory driver `JdbcSqliteDriver.IN_MEMORY`)
- Serialization logic

## Fixtures and Factories

**Test Data Builders:**

```kotlin
private fun createTestSession(
    id: String = "test-session",
    timestamp: Long = System.currentTimeMillis()
) = WorkoutSession(
    id = id,
    timestamp = timestamp,
    mode = "OldSchool",
    reps = 10,
    weightPerCableKg = 25f,
    totalReps = 10,
    workingReps = 10,
    exerciseId = "test-exercise",
    exerciseName = "Test Exercise"
)
```

**Location:**
- Helper functions at end of test class (private scope)
- Factories available at package level when used by multiple tests
- Named parameters heavily used for flexibility: `createTestSession(id = "custom-id")`

**Database Fixture:**

```kotlin
// TestDatabaseFactory.android.kt
actual fun createTestDatabase(): VitruvianDatabase {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    VitruvianDatabase.Schema.create(driver)
    return VitruvianDatabase(driver)
}
```

## Testing Rules

**TestCoroutineRule (Custom JUnit Rule):**

```kotlin
@get:Rule
val coroutineRule = TestCoroutineRule()

@Test
fun `test with main dispatcher`() = runTest {
    // Coroutine tests automatically use TestCoroutineRule dispatcher
    // Access via coroutineRule.dispatcher if needed
    val result = viewModel.doSomething()
    advanceUntilIdle()
    assertTrue(result)
}
```

**Location:** `/androidApp/src/test/kotlin/com/devil/phoenixproject/TestCoroutineRule.kt`

**Purpose:**
- Replaces Main dispatcher with StandardTestDispatcher
- Eliminates need for @OptIn on test methods
- Provides time advancement control
- Automatic setup/teardown via TestWatcher

**Compose Testing Rule:**

```kotlin
@get:Rule
val composeRule = createAndroidComposeRule<ComponentActivity>()

@Test
fun `ui element appears`() {
    composeRule.setContent { App() }

    composeRule.onNodeWithText("Expected Text").assertIsDisplayed()
}
```

## Test Types

**Unit Tests:**
- **Scope:** Single repository or use case function
- **Dependencies:** All fakes/in-memory
- **Location:** `shared/src/androidUnitTest/` for domain/data tests
- **Duration:** < 100ms each
- **Examples:** `SqlDelightWorkoutRepositoryTest`, `SqlDelightExerciseRepositoryTest`

**Integration Tests:**
- **Scope:** Multiple components working together (ViewModel + Repositories + Database)
- **Dependencies:** Real database (in-memory), fake BLE
- **Location:** `androidApp/src/androidTest/` for app-specific tests
- **Duration:** < 1s each
- **Examples:** `AppE2ETest` tests complete app flows

**End-to-End Tests:**
- **Scope:** Full user workflow from UI to data persistence
- **Dependencies:** Fake BLE, in-memory database
- **Location:** `shared/src/androidUnitTest/e2e/` or `androidApp/src/androidTest/e2e/`
- **Duration:** 1-5s each
- **Method:** Compose test API for UI assertions
- **Examples:**
  - `AppE2ETest.splashThenHomeContentAppears()` - Splash display, navigation timing
  - `WorkoutFlowE2ETest.complete connection flow` - Full device connection

**Instrumented Tests:**
- **Runner:** AndroidJUnit4 (on-device or emulator)
- **Dependencies:** Koin test module with fakes
- **Assertion:** Compose UI testing API (`onNodeWithText()`, `performClick()`)
- **Setup:** Custom Koin module injected at runtime

## Common Patterns

**Testing Suspend Functions:**

```kotlin
@Test
fun `getSession returns null for non-existent session`() = runTest {
    val result = repository.getSession("non-existent")
    assertNull(result)
}
```

**Testing Flows with Turbine:**

```kotlin
@Test
fun `getAllSessions emits all sessions`() = runTest {
    repository.saveSession(session1)
    repository.saveSession(session2)

    repository.getAllSessions().test {
        val sessions = awaitItem()
        assertEquals(2, sessions.size)
        cancelAndIgnoreRemainingEvents()
    }
}
```

**Testing State Changes:**

```kotlin
@Test
fun `connection state updates when connected`() = runTest {
    fake.simulateConnect("VIT-200")

    assertTrue(fake.connectionState.value is ConnectionState.Connected)
    val connected = fake.connectionState.value as ConnectionState.Connected
    assertEquals("VIT-200", connected.deviceName)
}
```

**Testing Repository Field Persistence:**

```kotlin
@Test
fun `saveSession persists all fields correctly`() = runTest {
    val session = WorkoutSession(
        id = "full-session",
        timestamp = 1234567890L,
        mode = "Echo",
        reps = 12,
        weightPerCableKg = 35.5f,
        // ... 30+ fields ...
    )

    repository.saveSession(session)

    val retrieved = repository.getSession("full-session")!!
    assertEquals(session.id, retrieved.id)
    assertEquals(session.timestamp, retrieved.timestamp)
    assertEquals(session.mode, retrieved.mode)
    // Verify each field for complete coverage
}
```

**Testing Result<T> Success/Failure:**

```kotlin
@Test
fun `startScanning returns success when enabled`() = runTest {
    fake.scanResult = Result.success(Unit)

    val result = fake.startScanning()

    assertTrue(result.isSuccess)
}

@Test
fun `startScanning returns failure when disabled`() = runTest {
    fake.scanResult = Result.failure(Exception("Scan failed"))

    val result = fake.startScanning()

    assertTrue(result.isFailure)
}
```

## Coverage Goals

**Current Coverage:**
- Repository tests: Comprehensive CRUD and Flow emissions
- Domain logic: Use case and rep counting algorithms
- Integration: Full workout flows from UI to database

**Untested Areas:**
- UI layout details (Compose preview tests exist separately)
- Android framework integration (permissions, lifecycle)
- Real BLE hardware interaction (simulator only)
- RevenueCat subscription handling (commented out in dependencies)

**Test Count:**
- Repository tests: ~8-10 per repository class
- E2E tests: ~5-10 per feature flow
- Total: 50+ tests across shared and app modules

## Debugging Failed Tests

**Common Issues:**

1. **Flow test hangs:** Missing `cancelAndIgnoreRemainingEvents()`
   ```kotlin
   repository.getAllSessions().test {
       val items = awaitItem()
       cancelAndIgnoreRemainingEvents()  // Always cleanup
   }
   ```

2. **Coroutine timeout:** Use `advanceUntilIdle()` before assertions
   ```kotlin
   fake.simulateConnect("device")
   advanceUntilIdle()
   assertTrue(fake.connectionState.value is ConnectionState.Connected)
   ```

3. **Compose test element not found:** Check mainClock timing
   ```kotlin
   composeRule.mainClock.advanceTimeBy(animationDuration)
   composeRule.waitForIdle()
   composeRule.onNodeWithText("Text").assertIsDisplayed()
   ```

4. **Database uniqueness violations:** Ensure test data has unique IDs
   ```kotlin
   createTestSession(id = "unique-${System.nanoTime()}")
   ```

