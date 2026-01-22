# Technology Stack

**Analysis Date:** 2026-01-21

## Languages

**Primary:**
- Kotlin 2.3.0 - Core multiplatform language for shared logic, Android, and iOS framework
- Swift - iOS app UI (SwiftUI integration)
- JSON - Configuration and API communication

**Secondary:**
- Shell/Bash - Build scripts and CI/CD workflows

## Runtime

**Environment:**
- Kotlin Multiplatform Mobile (KMP) - Unified codebase targeting Android and iOS
- JVM 17 - Android runtime target (compileSdk 36, minSdk 26)
- iOS deployment target - iosArm64 architecture (physical device distribution)

**Package Manager:**
- Gradle 8.x - Build automation and dependency management
- lockfile: `gradle/wrapper/gradle-wrapper.properties` (auto-managed)

## Frameworks

**Core UI:**
- Compose Multiplatform 1.10.0 - Unified cross-platform UI framework
- Jetpack Compose (Android) - Material3 design system via androidx.compose
- SwiftUI (iOS) - Native iOS UI, integrates with shared Compose framework

**Business Logic:**
- Kotlin Coroutines 1.10.2 - Async/reactive programming
- Kotlin Serialization 1.9.0 - JSON serialization
- Kotlin DateTime 0.7.1 - Cross-platform date/time utilities

**Navigation:**
- Jetpack Navigation Compose 2.9.1 - Cross-platform routing
- AndroidX Navigation Compose 2.9.1 - Android-specific navigation

**Dependency Injection:**
- Koin 4.1.1 (multiplatform) - Service locator and DI container
- Koin Compose 4.1.1 - Compose integration

**Database:**
- SQLDelight 2.2.1 - Type-safe SQL queries with multiplatform driver support
- SQLDelight migrations (11 versions in `shared/src/commonMain/sqldelight/com/devil/phoenixproject/database/migrations/`)

**Preferences/Settings:**
- Multiplatform Settings 1.3.0 - Encrypted preferences (iOS keychain, Android SharedPreferences)
- Multiplatform Settings Coroutines 1.3.0 - Flow-based preferences API

**HTTP/Networking:**
- Ktor Client 3.3.3 - HTTP client with platform-specific engines:
  - OkHttp engine (Android)
  - Darwin engine (iOS)
- ContentNegotiation plugin - JSON serialization support

**BLE (Bluetooth Low Energy):**
- Kable 0.40.2 - Kotlin Multiplatform BLE library for device communication
  - Nordic UART Service protocol implementation
  - Unified across Android (native) and iOS (native)

**Logging:**
- Kermit 2.0.8 - Multiplatform logging with tag support

**Media/Assets:**
- Coil 3.3.0 - Image loading and caching (async, HTTP-backed)
- Compottie 2.0.2 - Lottie animations (Compose Multiplatform)
- Media3 ExoPlayer 1.6.0 (Android only) - HLS video playback

**Charts:**
- Vico 2.3.6 (Android only) - Compose Material3 charts for analytics

**UI Utilities:**
- Reorderable 3.0.0 - Drag-and-drop list reordering
- Splash Screen 1.2.0 - Themed splash screen (AndroidX)

**Premium/Subscriptions:**
- RevenueCat 2.2.15+17.25.0 (KMP) - In-app subscriptions (currently disabled, Issue #215)

## Key Dependencies

**Critical for BLE:**
- `com.juul.kable:kable-core` (0.40.2) - Device scanning, connection, characteristic read/write
- Nordic UART Service UUIDs hardcoded in `shared/src/commonMain/kotlin/com/devil/phoenixproject/data/repository/KableBleRepository.kt`

**Critical for Data Persistence:**
- `app.cash.sqldelight:runtime` (2.2.1) - Database schema generation and query compilation
- `app.cash.sqldelight:android-driver` - SQLite driver (Android)
- `app.cash.sqldelight:native-driver` - SQLite driver (iOS)
- `app.cash.sqldelight:coroutines-extensions` - Flow-based query results

**Critical for API Communication:**
- `io.ktor:ktor-client-core` (3.3.3) - HTTP requests to `https://phoenix-portal-backend.up.railway.app`
- `io.ktor:ktor-client-content-negotiation` (3.3.3) - JSON serialization
- `io.ktor:ktor-serialization-kotlinx-json` (3.3.3) - Kotlinx-serialization integration

**Android-Specific:**
- androidx-core-ktx (1.17.0) - Core Android utilities
- androidx-lifecycle (2.10.0) - ViewModel/SavedState
- androidx-activity-compose (1.12.2) - Activity/Compose integration
- compose-bom (2025.12.01) - Compose dependency management

**Testing:**
- JUnit 4.13.2 - Unit test framework
- Mockk 1.14.7 - Mocking (with Android variant)
- Turbine 1.2.1 - Flow/coroutine testing utilities
- Truth 1.4.5 - Fluent assertion library
- Kotlin Test - Multiplatform test framework
- Coroutines Test - Coroutine testing utilities

## Configuration

**Build Configuration:**
- Root: `C:\Users\dasbl\AndroidStudioProjects\Project-Phoenix-MP\build.gradle.kts`
- Shared module: `C:\Users\dasbl\AndroidStudioProjects\Project-Phoenix-MP\shared\build.gradle.kts`
- Android app: `C:\Users\dasbl\AndroidStudioProjects\Project-Phoenix-MP\androidApp\build.gradle.kts`
- Dependency versions: `C:\Users\dasbl\AndroidStudioProjects\Project-Phoenix-MP\gradle\libs.versions.toml`

**Database Configuration:**
- SQLDelight package: `com.devil.phoenixproject.database`
- Database name (Android): `vitruvian.db`
- Current schema version: 11 (includes 10 migrations)
- Location: `shared/src/commonMain/sqldelight/com/devil/phoenixproject/database/`

**API Configuration:**
- Portal backend URL (hardcoded): `https://phoenix-portal-backend.up.railway.app`
- Implementation: `shared/src/commonMain/kotlin/com/devil/phoenixproject/data/sync/PortalApiClient.kt`
- HTTP timeout: 30 seconds (request), 10 seconds (connect)

**BLE Constants:**
- Nordic UART Service UUID: `6e400001-b5a3-f393-e0a9-e50e24dcca9e`
- MONITOR characteristic (poll): `90e991a6-c548-44ed-969b-eb541014eae3`
- REPS characteristic (notify): `8308f2a6-0875-4a94-a86f-5c5c5e1b068a`
- Multiple custom characteristics for diagnostics, heuristics, version, mode
- Location: `shared/src/commonMain/kotlin/com/devil/phoenixproject/data/repository/KableBleRepository.kt`

**Platform Configuration:**
- Android namespace: `com.devil.phoenixproject`
- iOS namespace: `com.devil.phoenixproject.shared` (framework)
- Min SDK (Android): 26
- Compile SDK (Android): 36
- Target SDK (Android): 36
- JVM target: JVM_17

**Kotlin Compiler Options:**
- Expect/actual classes enabled: `-Xexpect-actual-classes`
- Experimental time API: `kotlin.time.ExperimentalTime`
- Experimental coroutines API: `kotlinx.coroutines.ExperimentalCoroutinesApi`

## Build Artifacts

**APK (Android):**
- Named pattern: `ProjectPhoenix-{versionName}-{buildType}.apk`
- Debug: `ProjectPhoenix-0.3.3-DEBUG-debug.apk` (app ID suffix: `.debug`)
- Release: ProGuard minified, full debug symbols

**Framework (iOS):**
- XCFramework output: `shared.xcframework`
- Architecture: iosArm64 (physical device distribution)
- Static framework: `isStatic = true`

**Version Information:**
- Current version: 0.3.3 (versionCode 3)
- CI override: `-Pversion.code=XXX` (for automated versioning)

## Platform Requirements

**Development:**
- JDK 17+ (explicit compilation target)
- Gradle 8.x
- Kotlin 2.3.0 compatible IDE (Android Studio, IntelliJ IDEA)
- For iOS: macOS with Xcode (framework distribution only)

**Android Runtime:**
- Android 8+ (API 26 minimum)
- Up to Android 15 (API 36 target)
- BLE 4.0+ hardware

**iOS Runtime:**
- iOS deployment target: Determined by Xcode project config
- Physical devices only (iosArm64, no simulator distribution)

**Production Deployment:**
- Android: Google Play Store
- iOS: TestFlight (internal), App Store (public)

---

*Stack analysis: 2026-01-21*
