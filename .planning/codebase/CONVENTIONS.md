# Coding Conventions

**Analysis Date:** 2026-01-21

## Naming Patterns

**Files:**
- Domain models: PascalCase + `Models.kt` suffix (e.g., `Models.kt`, `AnalyticsModels.kt`, `TemplateModels.kt`, `TrainingCycleModels.kt`)
- Repository files: `[Entity]Repository.kt` (e.g., `BleRepository.kt`, `WorkoutRepository.kt`)
- Extension files: `[Domain].extensions.kt` with platform suffix for platform-specific (e.g., `BleExtensions.android.kt`)
- View model files: `[Feature]ViewModel.kt` (e.g., `MainViewModel.kt`, `ExerciseConfigViewModel.kt`)
- Test files: `[Tested]Test.kt` or `[Tested]E2ETest.kt` (e.g., `SqlDelightWorkoutRepositoryTest.kt`, `AppE2ETest.kt`)
- Exception files: `[Domain]Exceptions.kt` (e.g., `BleExceptions.kt`)
- Utility files: `[Utility]Utils.kt` or object name matching (e.g., `Constants.kt`, `UnitConverter.kt`)
- Test utilities: `Fake[Interface].kt` for test doubles (e.g., `FakeBleRepository.kt`, `FakeWorkoutRepository.kt`)

**Functions:**
- camelCase for all functions
- Descriptive verb-first names: `startScanning()`, `saveSession()`, `updateRoutineFlow()`
- Test function names use backticks and descriptive format: `` `saveSession persists session to database`() ``
- Helper functions in tests: `createTestSession()`, `verifyConnected()`
- Use verb prefixes: `get`, `set`, `is`, `has`, `create`, `update`, `delete`, `save`, `emit`, `simulate` (for test doubles)

**Variables:**
- camelCase for all variables
- Prefix `_` for private MutableStateFlow/MutableSharedFlow: `_connectionState`, `_metricsFlow`
- Prefix `is` or `has` for booleans: `isActive`, `hasError`, `shouldFailConnect`
- Suffix `Flow` for Flow types: `metricsFlow`, `sessionsFlow`
- Suffix `State` for StateFlow types: `connectionState`, `handleState`
- Suffix `Result` for Result types: `scanResult`, `connectResult`

**Types/Classes:**
- PascalCase for all types (data classes, sealed classes, enums)
- Sealed class variants: PascalCase objects or data classes, e.g., `object Disconnected : ConnectionState()`, `data class Connected(...) : ConnectionState()`
- Enum values: UPPER_SNAKE_CASE (e.g., `WaitingForRest`, `Released`, `Grabbed`, `Moving` - follow Kotlin enum convention)
- Exception classes: `[Description]Exception` (e.g., `BluetoothDisabledException`, `ConnectionLostException`, `GattStatusException`)
- Model data classes: Include comprehensive documentation with usage notes and constraints
- Repository interface names: `[Domain]Repository` (e.g., `BleRepository`, `WorkoutRepository`)

**Constants:**
- Object names for groupings: `Constants`, `UnitConverter`, `OneRepMaxCalculator`, `ProtocolConstants`
- Values: UPPER_SNAKE_CASE (e.g., `APP_VERSION`, `MIN_WEIGHT_KG`, `BLE_SCAN_TIMEOUT_MS`)
- Companion object for enum factories: `Companion { fun fromValue() }` pattern

## Code Style

**Formatting:**
- IDE default Kotlin formatting (no explicit ktlint/detekt config found)
- Line length: Standard Kotlin (~120 chars based on file widths)
- Indentation: 4 spaces
- Trailing commas in multi-line structures (Kotlin 1.4+ style)

**Linting:**
- No explicit .eslintrc or ktlint config in repository
- Follows standard Kotlin style conventions via IDE
- Gradle builds enforce successful compilation before release builds

**Comments:**
- Use KDoc (/** */) for public functions and classes:
  ```kotlin
  /**
   * Calculate estimated 1RM using Brzycki formula.
   * @param weight Weight lifted
   * @param reps Number of reps completed
   * @return Estimated one rep max
   */
  fun brzycki(weight: Float, reps: Int): Float
  ```
- Use inline comments for complex logic, especially in rep counting and BLE protocols
- Use /// markers for sections: `// ========== Session CRUD Tests ==========`
- Add warnings for deprecated or tricky behaviors: "⚠️ ..." comments in BLE extensions

## Import Organization

**Order:**
1. `package` statement
2. Empty line
3. Kotlin standard library imports (`kotlin.*`)
4. Androidx/compose imports (`androidx.*`, `compose.*`)
5. Koin imports (`org.koin.*`)
6. Project-internal imports (`com.devil.phoenixproject.*`)
7. Third-party imports (Kermit, Turbine, etc.)
8. Conditional imports based on function needs (coroutines, flows, testing)

**Path Aliases:**
- No explicit path aliases observed in code
- Full package paths used consistently throughout: `com.devil.phoenixproject.*`
- Multiplatform imports handled via `expect`/`actual` mechanism

## Error Handling

**Exception Hierarchy:**
- Custom BLE exceptions extend `Exception` with optional `cause` parameter: `BleExceptions.kt`
- Exceptions include detailed messages with context (e.g., `GattStatusException` includes GATT status code)
- All exceptions are documented with KDoc describing when thrown and user impact

**Exception Types Used:**
- `BluetoothDisabledException` - Bluetooth disabled on device
- `BluetoothException` - General BLE error fallback
- `ConnectionLostException` - Unexpected disconnection
- `ConnectionRejectedException` - Device refused connection
- `GattRequestRejectedException` - GATT operation rejected
- `GattStatusException` - GATT operation failed with status code
- `NotReadyException` - Device not initialized yet
- `ScanFailedException` - BLE scan failed to start

**Error Handling Patterns:**
- Repository methods return `Result<T>` for operations that can fail
- Suspending functions use `Result.success()` / `Result.failure()` builders
- Flow-based errors use `ConnectionState.Error(message, throwable?)` for connection states
- Errors are logged with Kermit logger before propagation
- Try-catch blocks wrap Android-specific operations (MTU negotiation, connection priority)

**Propagation:**
- BLE errors propagate through `connectionState` flow to UI
- Repository errors returned as `Result` for caller decision
- Exceptions include optional `cause: Throwable?` for root cause preservation

## Logging

**Framework:** Kermit (co.touchlab.kermit)

**Setup:**
- Logger created with tag: `Logger.withTag("TagName")`
- Default logger: `Logger.withTag("ClassName")`
- Private logger in classes: `private val log = Logger.withTag("RepCounterFromMachine")`

**Patterns:**
- Info level: Operation completion and state changes
  ```kotlin
  Logger.i("BleExtensions") { "✅ HIGH connection priority set successfully" }
  ```
- Warning level: Fallbacks, degradation, unexpected paths
  ```kotlin
  Logger.w("BleExtensions") { "⚠️ Cannot request connection priority: not an AndroidPeripheral" }
  ```
- Debug level: Detailed configuration and parameter logging
  ```kotlin
  logDebug("🔧 RepCounter.configure() called:")
  ```
- Use emoji prefixes for visual scanning: ✅ success, ❌ error, ⚠️ warning, 🔧 action
- BLE protocol logging: Include state transitions and command details

**What to Log:**
- BLE connection state transitions
- Configuration changes with parameters
- Operation completion/failure
- Unexpected code paths and fallbacks
- Negotiated values (MTU, connection priority)

## Function Design

**Size:** Most functions 20-50 lines, with complex domain logic (rep counting, state machines) allowed up to 100+ lines with clear sections

**Parameters:**
- Named parameters for clarity, especially in constructors:
  ```kotlin
  data class Connected(
      val deviceName: String,
      val deviceAddress: String,
      val hardwareModel: VitruvianModel = VitruvianModel.Unknown
  )
  ```
- Default parameters extensively used: `message: String = "default message"`
- Receiver functions used for test helper patterns: `workoutRobot(viewModel, fakeBleRepository) { ... }`

**Return Values:**
- Early returns for error/guard conditions: `if (value == null) return`
- Sealed class returns for state: `sealed class ConnectionState { ... }`
- `Result<T>` for fallible operations: `suspend fun startScanning(): Result<Unit>`
- `Flow<T>` / `StateFlow<T>` for reactive streams
- `Unit` explicitly when no return needed (not omitted)

**Scope Functions:**
- `?.let { }` for safe property handling
- `.apply { }` for receiver initialization rarely used
- Extension functions for platform-specific code

## Module Design

**Exports:**
- Interface-driven design: Repositories expose interfaces, not implementations
- `KoinModule` pattern for dependency injection (no barrel files observed)
- Public API clearly defined in interfaces
- Test doubles (Fake*) only exported to test source sets

**Expect/Actual Pattern:**
- Common interface in `commonMain` (e.g., `BleExtensions.kt`)
- Platform implementations: `BleExtensions.android.kt`, `BleExtensions.iosMain.kt`
- Database drivers: `DriverFactory.kt` in commonMain, implementations in platform-specific
- UI components: `BlePermissionHandler.android.kt`, `VideoPlayer.android.kt`

**File Organization:**
- Data layer: `data/repository/`, `data/ble/`, `data/local/`, `data/preferences/`
- Domain layer: `domain/model/`, `domain/usecase/`, `domain/subscription/`
- Presentation layer: `presentation/viewmodel/`, `presentation/screen/`, `presentation/components/`
- Utilities: `util/`, `config/`

**Test Organization:**
- Unit tests in `androidUnitTest` source set for shared module
- Instrumented tests in `androidTest` for app module
- Test doubles in `testutil/` folder (Fake* implementations)
- E2E tests use Robot pattern: `e2e/robot/workoutRobot()`

## Kotlin Version & Features

**Kotlin:** 2.0.21

**Language Features Used:**
- Data classes for models (immutable value objects)
- Sealed classes for state hierarchies (ConnectionState, WorkoutState, ProgramMode)
- Enum classes for fixed sets (HandleState, ProgramMode variants, PRType)
- Extension functions for platform-specific behavior
- Scope functions (let, apply) for null-safe operations
- Infix functions: Not commonly observed
- Delegation: Not commonly observed
- Destructuring: Used in coroutine patterns `{ _, event -> ... }`

**Coroutines & Flow:**
- Suspending functions: `suspend fun connect(device: ScannedDevice): Result<Unit>`
- `runTest {}` for unit tests of suspend functions
- `MutableStateFlow` for reactive state: internal `_state`, exposed as `StateFlow<State>`
- `MutableSharedFlow` for events with no replay: `_metricsFlow`, `_repEvents`
- Flow operators: `.map()`, `.filter()`, `.first()`, `.firstOrNull()`, `.take()`, `.collect()`
- `viewModelScope.launch {}` for ViewModel side effects
- `rememberCoroutineScope()` in Compose for event handlers
- `StateFlow.stateIn()` for derived state with sharing

**Compose:**
- `@Composable` functions: Standard naming without suffix
- State management: `remember {}`, `rememberCoroutineScope()`
- Effect management: `LaunchedEffect()`, `DisposableEffect()`
- Modifiers: Extensive use in layouts
- Theming: `VitruvianTheme {}` wrapper for app-wide styling

## Deprecated/Legacy Patterns

- Legacy web app protocol (96-byte frames) superseded by Phoenix Backend (25-byte frames) - see `ProtocolConstants` comments
- Legacy BLE packet format documented in `RepNotification` with backward compatibility for Samsung devices

