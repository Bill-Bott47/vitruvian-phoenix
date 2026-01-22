# External Integrations

**Analysis Date:** 2026-01-21

## APIs & External Services

**Railway Backend (Phoenix Portal):**
- **Service:** Phoenix Portal Backend REST API
- **Base URL:** `https://phoenix-portal-backend.up.railway.app`
- **What it's used for:**
  - User authentication (email/password signup and login)
  - User profile management (getMe endpoint)
  - Sync status queries and change management
  - Workout data push/pull for cloud backup
- **SDK/Client:** Ktor HttpClient 3.3.3
  - Implementation: `shared/src/commonMain/kotlin/com/devil/phoenixproject/data/sync/PortalApiClient.kt`
  - Endpoints covered:
    - POST `/api/auth/login` - Email/password authentication
    - POST `/api/auth/signup` - Account creation
    - GET `/api/auth/me` - Fetch current user profile
    - GET `/api/sync/status` - Check sync state
    - POST `/api/sync/push` - Send local changes to backend
    - POST `/api/sync/pull` - Receive server changes
- **Auth:** Bearer token (JWT) stored in `PortalTokenStorage`
  - Token provider: `tokenStorage.currentUser` and `tokenStorage.isAuthenticated` flows
  - Implementation: `shared/src/commonMain/kotlin/com/devil/phoenixproject/data/sync/PortalTokenStorage.kt`
- **Timeout Configuration:**
  - Request timeout: 30 seconds
  - Connect timeout: 10 seconds
  - Default content type: application/json
- **Error Handling:** Wrapped in Result<T> with PortalApiException

**Vitruvian BLE Devices:**
- **Service:** Vitruvian Trainer+ and V-Form Trainer machines
- **Device names:** Start with `Vee_` (V-Form) or `VIT` (Trainer+)
- **What it's used for:**
  - Real-time workout data collection (position, velocity, load, power)
  - Rep counting and set management
  - Mode changes and diagnostic queries
  - Firmware version queries
  - Phase statistics and heuristic data
- **SDK/Client:** Kable 0.40.2 (Kotlin Multiplatform BLE)
  - Implementation: `shared/src/commonMain/kotlin/com/devil/phoenixproject/data/repository/KableBleRepository.kt`
  - Unified across Android (native BLE) and iOS (native CoreBluetooth)
- **Protocol:** Nordic UART Service with custom characteristics
  - Service UUID: `6e400001-b5a3-f393-e0a9-e50e24dcca9e`
  - Write characteristic (TX): `6e400002-b5a3-f393-e0a9-e50e24dcca9e` (device RX)
  - Custom MONITOR characteristic (poll): `90e991a6-c548-44ed-969b-eb541014eae3`
  - Custom REPS characteristic (notify): `8308f2a6-0875-4a94-a86f-5c5c5e1b068a`
  - Custom DIAGNOSTIC characteristic (poll): `5fa538ec-d041-42f6-bbd6-c30d475387b7`
  - Custom HEURISTIC characteristic (poll): `c7b73007-b245-4503-a1ed-9e4e97eb9802`
  - Custom VERSION characteristic (notify): `74e994ac-0e80-4c02-9cd0-76cb31d3959b`
  - Custom MODE characteristic (notify): `67d0dae0-5bfc-4ea2-acc9-ac784dee7f29`
- **Data Exchange:**
  - Packet factory: `shared/src/commonMain/util/BlePacketFactory.kt` (constructs binary protocol)
  - Metrics: `shared/src/commonMain/kotlin/com/devil/phoenixproject/domain/model/WorkoutMetric.kt`
- **Connection Management:**
  - Scan timeout: 10 seconds (default)
  - Connection timeout: 15 seconds (default)
  - MTU request (platform-optimized)
  - High priority request (Android)

## Data Storage

**Databases:**
- **SQLite (Multiplatform)**
  - Type: Local relational database
  - Client: SQLDelight 2.2.1
  - Database name (Android): `vitruvian.db`
  - Schema location: `shared/src/commonMain/sqldelight/com/devil/phoenixproject/database/`
  - Current version: 11 (with 10 migrations)
  - Tables (key):
    - WorkoutSession - Exercise sessions with metrics
    - MetricSample - Real-time sensor data (position, velocity, load, power)
    - CompletedSet - Logged reps and weight per set
    - Exercise - Exercise definitions with one-rep max tracking
    - RoutineExercise - Exercise assignments in routines
    - PersonalRecord - PR tracking with 1RM calculations
    - Routine - Custom workout routines
    - Superset - Super-set groupings
    - TrainingCycle - Multi-week training programs
    - CycleDay - Day-by-day cycle structure
    - CycleProgress - Progression tracking within a cycle
    - UserProfile - User data (name, color, subscription status)
    - EarnedBadge, StreakHistory, GamificationStats - Gamification tables
  - Drivers:
    - Android: AndroidSqliteDriver (native SQLite)
    - iOS: NativeSqliteDriver
  - Connection: In-process, no remote connection

**File Storage:**
- Local filesystem only - No cloud storage integration for assets
- Lottie animations bundled: `shared/src/commonMain/compose/resources/`
- Images/exercise assets: Embedded in app resources

**Caching:**
- In-memory caching: StateFlow/MutableStateFlow throughout repositories
- HTTP caching: Coil 3.3.0 (image network cache)
- Preferences cache: Multiplatform Settings (encrypted, persisted)
- BLE scan cache: In-memory peripheral advertisement store

## Authentication & Identity

**Auth Provider:**
- Custom - Email/password authentication via Railway backend Portal API
  - Implementation: `shared/src/commonMain/kotlin/com/devil/phoenixproject/data/repository/PortalAuthRepository.kt`
  - Repository interface: `shared/src/commonMain/kotlin/com/devil/phoenixproject/data/repository/AuthRepository.kt`

**Auth Flow:**
- Email/password signup: POST `/api/auth/signup`
- Email/password login: POST `/api/auth/login`
- Token storage: Encrypted via platform keychain
  - Android: SharedPreferences (EncryptedSharedPreferences not explicitly mentioned)
  - iOS: Keychain
- Token validation: GET `/api/auth/me` on session refresh
- Token expiry: Not explicitly configured (assumption: server-managed)
- Logout: Clear token storage (POST signOut())

**OAuth Integrations (Not Implemented):**
- Google Sign-in: Planned (currently returns NotImplementedError in code)
- Apple Sign-in: Planned (currently returns NotImplementedError in code)
- Reason: Requires platform-specific implementations

## Monitoring & Observability

**Error Tracking:**
- Not detected - No Sentry, Firebase Crashlytics, or similar integration

**Logs:**
- **Approach:** Kermit 2.0.8 (multiplatform structured logging)
- **Tag usage:** Logger.withTag("ComponentName")
- **Example loggers:**
  - KableBleRepository: `Logger.withTag("KableBleRepository")`
  - PortalApiClient: Connection logs for API requests
  - DriverFactory (Android): Database migration debug logs
- **Output:** Console/logcat (development), platform-specific handlers (production)

**Metrics Collection:**
- Workout telemetry (WorkoutMetric) - Local storage only
- Heuristic statistics from device - Local database storage
- No remote analytics service detected

## CI/CD & Deployment

**Hosting/Distribution:**
- **Android:** Google Play Store (release builds)
- **iOS:** TestFlight (internal), App Store (public)

**CI Pipeline:**
- **Platform:** GitHub Actions
- **Workflows:**
  - `android-playstore.yml` - Android release to Play Store
  - `android-release-apk.yml` - Android APK build
  - `ios-release-ipa.yml` - iOS IPA build
  - `ios-testflight.yml` - TestFlight public build
  - `ios-testflight-internal.yml` - TestFlight internal build
  - `ci-tests.yml` - Unit tests on push/PR
- **Build agent:** Ubuntu-latest (primary), macOS for iOS framework
- **Gradle caching:** Enabled for faster builds
- **Setup steps:**
  - Java 17 (setup-java action)
  - Gradle wrapper (gradle/actions/setup-gradle)
  - Python (for JWT generation in recent changes)

**Version Management:**
- Current version: 0.3.3 (versionCode 3)
- CI-controlled versioning: `-Pversion.code=XXX` parameter
- Version naming: `ProjectPhoenix-{versionName}-{buildType}.apk`

## Environment Configuration

**Required Environment Variables:**
None explicitly detected in main codebase, but likely configured in:
- GitHub Secrets for CI/CD workflows
- Google Play Console signing keys
- TestFlight provisioning profiles
- Railway backend credentials (base URL hardcoded)

**Configuration Files:**
- `gradle/libs.versions.toml` - All dependency versions
- `shared/build.gradle.kts` - Kotlin/Compose/SQLDelight configuration
- `androidApp/build.gradle.kts` - Android-specific configuration
- `build.gradle.kts` - Root-level plugin configuration

**Secrets Location:**
- Android: Debug keystore (standard location: `~/.android/debug.keystore`)
- iOS: Xcode project signing configuration (Apple Developer certificates)
- API tokens: PortalTokenStorage (encrypted per platform)

## Webhooks & Callbacks

**Incoming:**
- Not detected - No webhook endpoints in application code

**Outgoing:**
- POST requests to Railway Portal API (`/api/sync/push`, `/api/sync/pull`)
- BLE notifications from device (read-only, not HTTP webhooks)

## Network Security

**HTTPS:**
- Portal API: Uses HTTPS with Railway backend
- Certificate pinning: Not detected
- Timeout enforcement: 30s request, 10s connection

**BLE Security:**
- No encryption layer (BLE protocol handles physical security)
- Characteristic permissions: Read/Write/Notify based on characteristic type
- Device pairing: Standard BLE pairing (platform-dependent)

---

*Integration audit: 2026-01-21*
