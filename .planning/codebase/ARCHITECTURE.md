# Architecture

**Analysis Date:** 2026-01-21

## Pattern Overview

**Overall:** Clean Architecture with Kotlin Multiplatform Model-View-ViewModel (MVVM)

**Key Characteristics:**
- Separation of concerns: Domain (pure Kotlin business logic), Data (repositories, databases), and Presentation (UI/Compose)
- Reactive data flows using Kotlin coroutines and Flow
- Cross-platform code in `commonMain` with platform-specific implementations in `androidMain`/`iosMain`
- Dependency injection via Koin framework
- Type-safe database access with SQLDelight

## Layers

**Domain Layer:**
- Purpose: Pure business logic, models, and use cases independent of any framework
- Location: `shared/src/commonMain/kotlin/com/devil/phoenixproject/domain/`
- Contains: Models (Models.kt, Exercise.kt, Routine.kt, etc.), Use Cases (ProgressionUseCase.kt, RepCounterFromMachine.kt, etc.)
- Depends on: Nothing (no external dependencies)
- Used by: Data layer, Presentation layer (ViewModels)

**Data Layer:**
- Purpose: Data access abstraction, persistence, and external service communication
- Location: `shared/src/commonMain/kotlin/com/devil/phoenixproject/data/`
- Contains:
  - Repositories (BleRepository.kt, ExerciseRepository.kt, WorkoutRepository.kt, etc.)
  - Local storage (DatabaseFactory.kt, DriverFactory.kt using SQLDelight)
  - BLE communication (BleExtensions.kt, BleExceptions.kt)
  - Preferences management (PreferencesManager.kt)
- Depends on: Domain models, Kable BLE library, SQLDelight
- Used by: Presentation layer (ViewModels)

**Presentation Layer:**
- Purpose: UI rendering, state management, and user interaction handling
- Location: `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/`
- Contains:
  - ViewModels (MainViewModel.kt, ThemeViewModel.kt, etc. - 4600+ lines for MainViewModel)
  - Composable screens and components (ActiveWorkoutScreen.kt, HomeScreen.kt, etc.)
  - Navigation routing (NavGraph.kt, NavigationRoutes.kt)
  - Theme configuration (Theme.kt, DataColors.kt, Shapes.kt)
- Depends on: Domain models, Data repositories
- Used by: Android/iOS platform-specific entry points

**Platform-Specific Layers:**
- Location: `shared/src/androidMain/` and `shared/src/iosMain/`
- Contains: Implementations of `expect`/`actual` declarations for BLE, database drivers, platform utilities
- Purpose: Handle platform-specific APIs while maintaining common interface

## Data Flow

**Workout Execution Flow:**

1. User selects exercise and parameters on HomeScreen
2. MainViewModel receives selection, prepares WorkoutParameters
3. ViewModel connects to device via BleRepository
4. BleRepository uses Kable library to establish BLE connection
5. Device sends real-time WorkoutMetric data (load, position, velocity) via BLE UART service
6. ViewModel receives metrics in realtime flow, performs rep counting via RepCounterFromMachine
7. Rep events trigger state updates in WorkoutState (SetSummary, Resting, etc.)
8. UI reflects current state: rep counter, force gauges, metrics graphs
9. On set completion, ViewModel calculates summary metrics and saves to database via WorkoutRepository
10. CompletedSetRepository stores session data with metrics for history/analytics

**State Management:**

- ViewModels expose UI state as StateFlow and SharedFlow
- Composables collect flows with `collectAsState()` for reactive updates
- Exercise and personal record data cached in memory by repositories
- Preferences managed through PreferencesManager (multiplatform-settings)
- Database queries executed asynchronously with Flow for real-time updates

## Key Abstractions

**BleRepository Interface:**
- Purpose: Abstracts BLE connectivity and device communication
- Examples: `shared/src/commonMain/kotlin/com/devil/phoenixproject/data/repository/BleRepository.kt`
- Pattern: Sealed state machines (ConnectionState, WorkoutState, RoutineFlowState)
- Implementation variants: KableBleRepository (Nordic UART), SimulatorBleRepository (for testing)

**WorkoutRepository Interface:**
- Purpose: Abstracts workout session storage and retrieval
- Examples: `shared/src/commonMain/kotlin/com/devil/phoenixproject/data/repository/WorkoutRepository.kt`
- Pattern: Repository pattern with Flow-based queries
- Implementation: Uses SQLDelightCompletedSetRepository

**ExerciseRepository Interface:**
- Purpose: Exercise library data access with caching
- Examples: `shared/src/commonMain/kotlin/com/devil/phoenixproject/data/repository/ExerciseRepository.kt`
- Pattern: In-memory cache with lazy-loaded SQLDelight queries
- Implementation: SqlDelightExerciseRepository

**RepCounterFromMachine Use Case:**
- Purpose: Converts raw BLE rep notifications into rep events
- Examples: `shared/src/commonMain/kotlin/com/devil/phoenixproject/domain/usecase/RepCounterFromMachine.kt`
- Pattern: State machine for rep detection from two packet formats (legacy 6-byte and official 24-byte)

## Entry Points

**Android Entry Point:**
- Location: `androidApp/src/main/kotlin/com/devil/phoenixproject/MainActivity.kt`
- Triggers: App launch
- Responsibilities:
  - Enables edge-to-edge display
  - Requests BLE permissions via RequireBlePermissions composable
  - Renders root App() composable

**iOS Entry Point:**
- Location: iosApp/ (Swift)
- Triggers: App launch (platform-specific)
- Responsibilities: Initialize shared Koin DI, run migrations via KoinKt.runMigrations()

**Application Initialization:**
- Location: `androidApp/src/main/kotlin/com/devil/phoenixproject/VitruvianApp.kt` (Application class)
- Executes on first app creation:
  1. Initialize DeviceInfo with BuildConfig values
  2. Start Koin dependency injection with Android context
  3. Run database migrations via MigrationManager
  4. Load persisted preferences and theme settings

**Root Composable:**
- Location: `shared/src/commonMain/kotlin/com/devil/phoenixproject/App.kt`
- Flow:
  1. Show splash screen (2500ms) if EULA accepted
  2. Show EULA acceptance dialog first-time (EulaScreen)
  3. Render EnhancedMainScreen with navigation graph (NavGraph)
  4. Theme applied via VitruvianTheme wrapper

## Error Handling

**Strategy:** Sealed classes for expected errors, exceptions for unexpected failures

**Patterns:**

- **ConnectionState.Error** - Captures BLE connection failures with message and optional throwable
- **WorkoutState.Error** - Captures runtime workout errors (machine faults, protocol violations)
- **Try-catch in ViewModels** - Catch repository exceptions, convert to state updates
- **Logger calls** - Use Kermit logging framework for errors (see MainViewModel catch blocks)
- **User dialogs** - ConnectionErrorDialog, ConnectionLostDialog for critical errors

Example error flow:
```kotlin
try {
    bleRepository.connect(device)
} catch (e: Exception) {
    Logger.e("MainViewModel") { "Connection failed: ${e.message}" }
    _connectionState.value = ConnectionState.Error("Connection failed", e)
    // Show user-facing dialog
    _showConnectionError.value = true
}
```

## Cross-Cutting Concerns

**Logging:**
- Framework: Kermit (co.touchlab.kermit.Logger)
- Pattern: Used throughout for BLE events, state transitions, errors
- Example: `Logger.d("MainViewModel") { "Starting workout with mode: $mode" }`

**Validation:**
- Location: Domain models validate invariants in init blocks and data class getters
- Example: EchoLevel validation (1-4 range), weight limits (0-220kg in 0.5kg increments)
- BLE protocol validation: Packet format detection (legacy vs official), MTU negotiation

**Authentication:**
- Optional cloud sync authentication via PortalAuthRepository
- Managed separately from device BLE authentication (no auth required for machine connection)
- RevenueCat subscription disabled (TODO markers in VitruvianApp)

**Database Access:**
- Thread-safe via SQLDelight coroutines support (sqldelight.coroutines)
- All database queries wrapped in Flow for reactive updates
- Migrations managed by MigrationManager with version checks

**Dependency Injection:**
- Single Koin instance initialized once in VitruvianApp
- Modules defined in commonModule (shared) and platformModule (android/ios)
- ViewModels created via koinViewModel() in Compose
- Repositories injected into ViewModels via constructor

---

*Architecture analysis: 2026-01-21*
