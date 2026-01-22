# Codebase Structure

**Analysis Date:** 2026-01-21

## Directory Layout

```
Project-Phoenix-MP/
├── shared/                                      # Kotlin Multiplatform shared library
│   ├── src/
│   │   ├── commonMain/                          # Cross-platform code
│   │   │   ├── kotlin/com/devil/phoenixproject/
│   │   │   │   ├── App.kt                       # Root composable with theme/EULA/lifecycle
│   │   │   │   ├── Platform.kt                  # expect/actual declarations
│   │   │   │   ├── config/                      # Configuration management
│   │   │   │   ├── data/                        # Data layer (repositories, database, BLE)
│   │   │   │   ├── di/                          # Dependency injection (KoinInit.kt)
│   │   │   │   ├── domain/                      # Domain models and use cases
│   │   │   │   ├── presentation/                # UI layer (screens, components, viewmodels)
│   │   │   │   ├── ui/                          # Theme and styling
│   │   │   │   └── util/                        # Utilities and constants
│   │   │   ├── composeResources/                # Compose multiplatform resources (images, lottie)
│   │   │   └── sqldelight/                      # SQLDelight schema definitions
│   │   ├── androidMain/                         # Android-specific implementations
│   │   │   └── kotlin/com/devil/phoenixproject/
│   │   │       ├── Platform.android.kt          # Android expect/actual implementations
│   │   │       ├── config/                      # Android config
│   │   │       ├── data/                        # Android data layer specifics (BLE, drivers)
│   │   │       ├── di/                          # Android DI module (PlatformModule.android.kt)
│   │   │       ├── domain/                      # Android domain specifics
│   │   │       ├── presentation/                # Android UI specifics
│   │   │       └── util/                        # Android utilities
│   │   └── iosMain/                             # iOS-specific implementations (mirrors androidMain)
│   │       └── kotlin/com/devil/phoenixproject/
│   └── build.gradle.kts                         # KMP configuration
├── androidApp/                                  # Android application entry point
│   ├── src/
│   │   ├── main/
│   │   │   ├── kotlin/com/devil/phoenixproject/
│   │   │   │   ├── MainActivity.kt              # Android Activity (entry point)
│   │   │   │   ├── VitruvianApp.kt              # Application class (Koin init, migrations)
│   │   │   │   ├── AppModule.kt                 # Android-specific Koin module
│   │   │   │   ├── service/                     # Android services (background tasks)
│   │   │   │   └── ui/                          # Android-specific UI code
│   │   │   └── res/                             # Android resources (drawables, etc)
│   │   ├── androidTest/                         # Instrumented tests (device/emulator)
│   │   └── release/                             # Release build artifacts
│   └── build.gradle.kts                         # Android app build configuration
├── iosApp/                                      # iOS application (SwiftUI)
│   └── [Swift source files]                     # iOS-specific code
├── .planning/
│   └── codebase/                                # Architecture documentation (this directory)
├── docs/                                        # User documentation
├── gradle/                                      # Gradle wrapper and shared configurations
└── build.gradle.kts                             # Root project configuration
```

## Directory Purposes

**shared/src/commonMain/:**
- Purpose: All cross-platform Kotlin code compiled to both Android and iOS
- Contains: Domain models, repositories, composables, viewmodels, database schema
- Key files: `App.kt`, `Platform.kt`, `di/KoinInit.kt`

**shared/src/commonMain/kotlin/com/devil/phoenixproject/domain/:**
- Purpose: Business logic and domain models
- Contains:
  - Models.kt: Core sealed classes (ConnectionState, WorkoutState, ProgramMode, EchoLevel, etc.)
  - Exercise.kt: Exercise domain model
  - Routine.kt: Routine/superset domain models
  - TrainingCycleModels.kt: Training cycle domain models
  - Gamification.kt: Badge and achievement models
  - usecase/: RepCounterFromMachine, ProgressionUseCase, TrendAnalysisUseCase, etc.

**shared/src/commonMain/kotlin/com/devil/phoenixproject/data/:**
- Purpose: Data access layer
- Key subdirectories:
  - `ble/`: BLE protocol definitions and extensions (BleExtensions.kt, BleExceptions.kt)
  - `local/`: Database and storage (DatabaseFactory.kt, DriverFactory.kt, ExerciseImporter.kt)
  - `migration/`: Database migrations (MigrationManager.kt, TrainingCycleMigration.kt)
  - `preferences/`: Settings persistence (PreferencesManager.kt)
  - `repository/`: Interface and implementation repositories
    - Interfaces: BleRepository, ExerciseRepository, WorkoutRepository, etc.
    - Implementations: KableBleRepository, SqlDelightExerciseRepository, etc.
    - Simulator: SimulatorBleRepository for testing without hardware
  - `sync/`: Cloud sync management (SyncTriggerManager.kt)

**shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/:**
- Purpose: UI layer
- Key subdirectories:
  - `viewmodel/`: MainViewModel.kt (4600+ lines - state management), ThemeViewModel, EulaViewModel, etc.
  - `screen/`: Full-screen composables (ActiveWorkoutScreen.kt, HomeScreen.kt, AnalyticsScreen.kt, etc.)
  - `components/`: Reusable UI components (DashboardComponents.kt, ModeSelector.kt, RpeSlider.kt, etc.)
  - `components/exercisepicker/`: Exercise selection UI (ExercisePicker.kt, GroupedExerciseList.kt, etc.)
  - `components/charts/`: Data visualization (WorkoutMetricsDetailChart.kt, VolumeHistoryChart.kt, etc.)
  - `components/cycle/`: Training cycle UI components
  - `navigation/`: NavGraph.kt, NavigationRoutes.kt - routing configuration
  - `util/`: Presentation utilities

**shared/src/commonMain/kotlin/com/devil/phoenixproject/ui/:**
- Purpose: Theme and styling
- Contains:
  - Theme.kt: Main theme composable (VitruvianTheme)
  - Color.kt: Color palette definitions
  - DataColors.kt: Colors for data visualization
  - HomeButtonColors.kt: Button styling
  - Type.kt: Typography definitions
  - Shapes.kt: Shape configurations
  - Spacing.kt: Spacing/padding constants
  - Material3Expressive.kt: Material 3 theme customizations

**shared/src/commonMain/kotlin/com/devil/phoenixproject/util/:**
- Purpose: Utilities and constants
- Key files:
  - BleConstants.kt: Nordic UART UUIDs, timeout constants (10s scan, 15s connection)
  - Constants.kt: Weight limits (0-220kg), rep formulas (Brzycki, Epley)
  - HardwareDetection.kt: Device model detection (Vee_, VIT prefixes)
  - EchoParams.kt: Echo mode parameter definitions
  - KmpUtils.kt: Multiplatform utility functions
  - CsvExporter.kt: Export functionality
  - DeviceInfo.kt: Device metadata (version, debug flag)

**shared/src/commonMain/sqldelight/:**
- Purpose: Type-safe database schema
- Contains: VitruvianDatabase.sq with tables:
  - Exercise: Exercise library (800+ exercises pre-loaded)
  - WorkoutSession: Individual sets and routine sessions
  - MetricSample: Real-time metrics during workout (load, position, velocity)
  - PersonalRecord: PR tracking with 1RM calculations
  - Routine: Custom workout routines
  - Superset: Grouped exercises within routines
  - TrainingCycle: Periodized training programs
  - User badges/achievements (gamification)

**shared/src/androidMain/:**
- Purpose: Android-specific implementations for expect/actual declarations
- Key files:
  - Platform.android.kt: Android platform identification
  - PlatformConfig.android.kt: Android-specific configuration
  - PlatformModule.android.kt: Android DI module with Android Context
  - data/ble/BleExtensions.android.kt: Android BLE-specific code (MTU negotiation, priority)
  - data/local/DriverFactory.android.kt: Android SQLite driver
  - util/DeviceInfo.android.kt: Android device info (from BuildConfig)
  - util/CsvExporter.android.kt: Android file export implementation
  - util/ConnectivityChecker.android.kt: Network connectivity detection
  - presentation/components/*.android.kt: Android-specific UI (haptic feedback, permissions, etc.)

**androidApp/src/main/kotlin/com/devil/phoenixproject/:**
- Purpose: Android-specific application code
- Contains:
  - MainActivity.kt: Composable entry point (requires BLE permissions, renders App())
  - VitruvianApp.kt: Application class - Koin initialization, migrations, DeviceInfo setup
  - AppModule.kt: Android app-level Koin module bindings

**androidApp/src/androidTest/:**
- Purpose: Instrumented tests (run on device/emulator)
- Contains: E2E tests, robot pattern test utilities

## Key File Locations

**Entry Points:**
- `androidApp/src/main/kotlin/com/devil/phoenixproject/MainActivity.kt`: Android Activity (displays Composable)
- `androidApp/src/main/kotlin/com/devil/phoenixproject/VitruvianApp.kt`: Android Application class (Koin init)
- `shared/src/commonMain/kotlin/com/devil/phoenixproject/App.kt`: Root composable

**Configuration:**
- `shared/build.gradle.kts`: KMP build configuration, dependencies
- `androidApp/build.gradle.kts`: Android app build configuration
- `shared/src/commonMain/sqldelight/com/devil/phoenixproject/database/VitruvianDatabase.sq`: Database schema

**Core Logic:**
- `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/viewmodel/MainViewModel.kt`: Central state management (4600+ lines)
- `shared/src/commonMain/kotlin/com/devil/phoenixproject/data/repository/BleRepository.kt`: BLE interface
- `shared/src/commonMain/kotlin/com/devil/phoenixproject/data/repository/KableBleRepository.kt`: BLE implementation (Kable library)
- `shared/src/commonMain/kotlin/com/devil/phoenixproject/data/repository/SimulatorBleRepository.kt`: Test simulator

**Testing:**
- `shared/src/androidUnitTest/kotlin/com/devil/phoenixproject/`: Android unit tests
- `androidApp/src/androidTest/kotlin/com/devil/phoenixproject/`: Instrumented tests

## Naming Conventions

**Files:**
- **Composable screens:** `[Feature]Screen.kt` (e.g., `ActiveWorkoutScreen.kt`, `HomeScreen.kt`, `AnalyticsScreen.kt`)
- **Composable components:** `[Component]Component.kt` or simple `[Component].kt` (e.g., `ModeSelector.kt`, `RpeSlider.kt`)
- **ViewModels:** `[Feature]ViewModel.kt` (e.g., `MainViewModel.kt`, `ThemeViewModel.kt`)
- **Repositories (interface):** `[Domain]Repository.kt` (e.g., `BleRepository.kt`, `ExerciseRepository.kt`)
- **Repository implementations:** `[Implementation][Domain]Repository.kt` (e.g., `SqlDelightExerciseRepository.kt`, `KableBleRepository.kt`)
- **Models:** `[Entity].kt` (e.g., `Models.kt`, `Exercise.kt`, `Routine.kt`)
- **Use cases:** `[UseCase]UseCase.kt` (e.g., `ProgressionUseCase.kt`, `RepCounterFromMachine.kt`)
- **Utilities:** `[Utility].kt` (e.g., `BleConstants.kt`, `DeviceInfo.kt`)

**Directories:**
- All lowercase with no underscores (e.g., `presentation`, `repository`, `viewmodel`)
- Organized by feature/domain (e.g., `data/ble/`, `presentation/components/exercisepicker/`)

**Packages:**
- `com.devil.phoenixproject.[layer].[feature]`
- Example: `com.devil.phoenixproject.presentation.viewmodel`, `com.devil.phoenixproject.data.repository`

## Where to Add New Code

**New Feature (e.g., new workout mode):**
- Domain model: `shared/src/commonMain/kotlin/com/devil/phoenixproject/domain/model/Models.kt` (extend sealed classes)
- Use case: `shared/src/commonMain/kotlin/com/devil/phoenixproject/domain/usecase/[Feature]UseCase.kt`
- Repository: `shared/src/commonMain/kotlin/com/devil/phoenixproject/data/repository/[Feature]Repository.kt`
- ViewModel logic: `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/viewmodel/MainViewModel.kt` (add methods/state flows)
- Screen: `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/screen/[Feature]Screen.kt`
- Components: `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/components/[Feature].kt`
- Tests: `shared/src/androidUnitTest/kotlin/com/devil/phoenixproject/presentation/viewmodel/[Feature]ViewModelTest.kt`

**New Composable Component:**
- Location: `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/components/[ComponentName].kt`
- Pattern: Create `@Composable fun [ComponentName](...)`
- If platform-specific: Add `.android.kt` variant in `shared/src/androidMain/kotlin/`

**New Database Table:**
- Add to: `shared/src/commonMain/sqldelight/com/devil/phoenixproject/database/VitruvianDatabase.sq`
- Create migration: `shared/src/commonMain/kotlin/com/devil/phoenixproject/data/migration/Migration[N].kt`
- Run migration in: `MigrationManager.checkAndRunMigrations()`

**New Repository Interface:**
- Location: `shared/src/commonMain/kotlin/com/devil/phoenixproject/data/repository/[Domain]Repository.kt`
- Implementation: `shared/src/commonMain/kotlin/com/devil/phoenixproject/data/repository/SqlDelight[Domain]Repository.kt`
- Register in Koin: `shared/src/commonMain/kotlin/com/devil/phoenixproject/di/KoinInit.kt` (commonModule)

**Utilities:**
- Shared utilities: `shared/src/commonMain/kotlin/com/devil/phoenixproject/util/[Utility].kt`
- Android-specific: `shared/src/androidMain/kotlin/com/devil/phoenixproject/util/[Utility].android.kt`

## Special Directories

**.planning/codebase/:**
- Purpose: Architecture documentation (ARCHITECTURE.md, STRUCTURE.md, etc.)
- Generated: By GSD mapping commands
- Committed: Yes

**.gradle/:**
- Purpose: Gradle wrapper files and shared gradle scripts
- Generated: By Gradle build system
- Committed: Yes

**build/:**
- Purpose: Compiled build outputs (generated)
- Generated: Yes (by Gradle build)
- Committed: No

**shared/src/commonMain/composeResources/:**
- Purpose: Compose Multiplatform resource files (images, lottie animations)
- Generated: No (manually added)
- Committed: Yes

**docs/:**
- Purpose: User-facing documentation
- Generated: No
- Committed: Yes

---

*Structure analysis: 2026-01-21*
