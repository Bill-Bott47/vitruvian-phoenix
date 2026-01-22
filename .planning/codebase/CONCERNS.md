# Codebase Concerns

**Analysis Date:** 2026-01-21

## Tech Debt

**Complex BLE State Management:**
- **Issue:** The BLE connection and workout state machines are fragile and tightly coupled. Multiple guards exist to prevent race conditions (`stopWorkoutInProgress`, `setCompletionInProgress`).
- **Files:** `shared/src/commonMain/kotlin/com/devil/phoenixproject/data/repository/KableBleRepository.kt` (2687 lines), `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/viewmodel/MainViewModel.kt` (4625 lines)
- **Impact:** Difficult to maintain, prone to subtle bugs during edge cases (rapid state transitions, connection interruptions, device disconnects during workouts)
- **Fix approach:** Refactor state machines to use explicit state transition logic with validated transitions, separate BLE protocol handling from workout state management, add state diagram documentation

**Monolithic MainViewModel:**
- **Issue:** MainViewModel contains 4600+ lines managing workouts, rep counting, BLE connection, routine navigation, gamification, and more. This violates single responsibility principle.
- **Files:** `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/viewmodel/MainViewModel.kt`
- **Impact:** High cognitive load, difficult to test, difficult to modify without unintended side effects. Navigation logic is spread across multiple screen composables and ViewModel mixed together.
- **Fix approach:** Split into domain-specific ViewModels (WorkoutVM, RoutineVM, ConnectionVM, GamificationVM) with clear boundaries and testable interfaces

**Navigation Logic Scattered in Screens:**
- **Issue:** Navigation decisions are split between ActiveWorkoutScreen, SetReady, RoutineComplete, and MainViewModel. The flow depends on multiple state fields (`routineFlowState`, `workoutState`, `loadedRoutine`) and `Issue #XXX` comments indicate this was problematic.
- **Files:** `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/screen/ActiveWorkoutScreen.kt` (395 lines, lines 116-237 show complex navigation guards), `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/screen/SetReady.kt`
- **Impact:** Navigation is fragile - Issue #204 mentions double navigation bug fixed via `hasNavigatedAway` guard, Issue #142 needed SetSummary added to prevent flickering. Adding new screens requires updating multiple files.
- **Fix approach:** Centralize routing logic using a state machine or navigation graph compiler (Jetpack Navigation on Android), make RoutineFlowState authoritative

**Magic Numbers and Thresholds:**
- **Issue:** Scattered constants for handle detection, velocity thresholds, position ranges, timeouts across multiple files without clear rationale.
- **Files:** `KableBleRepository.kt` (HANDLE_GRABBED_THRESHOLD=8.0mm, VELOCITY_THRESHOLD=50.0mm/s, CONNECTION_TIMEOUT_MS=15000, DESIRED_MTU=247), `MainViewModel.kt` (STALL_VELOCITY_LOW=2.5, STALL_VELOCITY_HIGH=10.0, AMRAP_STARTUP_GRACE_MS=8000, AUTO_STOP_DURATION_SECONDS=2.5)
- **Impact:** Hard to tune for different hardware or user preferences. Comments say "matches official app" but no systematic way to update all related thresholds together.
- **Fix approach:** Create a ConfigurationProfile class with named, documented parameters. Store values in database or build-time config, not hardcoded constants.

**Special Case Handling for Handle Detection:**
- **Issue:** Multiple special cases for handle state detection: `HANDLE_GRABBED_THRESHOLD=8.0mm`, `HANDLE_REST_THRESHOLD=5.0mm`, `WAITING_FOR_REST_TIMEOUT_MS=3000ms` for iOS pre-tensioned handles, dynamic baseline delta for overhead pulleys (`GRAB_DELTA_THRESHOLD=10.0mm`).
- **Files:** `KableBleRepository.kt` lines 96-99, 137-145, lines 34-35
- **Impact:** Each new machine variant requires new special cases. Current approach doesn't scale beyond the two Vitruvian models.
- **Fix approach:** Implement machine profile/variant system with pluggable handle detection strategies, add e2e tests for each variant

---

## Known Bugs

**Double Navigation/Flickering During Routine Flows:**
- **Symptoms:** Screen briefly shows SetReady then pops back, or shows blank RoutineOverview before navigating away
- **Files:** `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/screen/ActiveWorkoutScreen.kt` (Issue #142 mentioned in comments lines 210-211, Issue #204 mentioned line 157)
- **Trigger:** Completing a set in a routine, rapid state changes during navigation, autoplay countdown transitions
- **Workaround:** Multiple guards added: `hasNavigatedAway` flag (line 160), `!isWorkoutActive` check (lines 212-216), `setCompletionInProgress` guard in ViewModel
- **Note:** Recent commits show repeated fixes for navigation issues ("fix: prevent warmup reps and UI layout issues", "fix: enhance next exercise calculation for superset-aware navigation")

**Race Condition in Workout Completion:**
- **Symptoms:** Duplicate workout sessions recorded, metrics lost, multiple stopWorkout calls
- **Files:** `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/viewmodel/MainViewModel.kt` (lines 434-438)
- **Trigger:** `handleMonitorMetric()` can call `stopWorkout()` multiple times during state transitions. Issue #97 referenced in code.
- **Workaround:** `stopWorkoutInProgress` guard flag prevents duplicate calls, but adds complexity
- **Current state:** Requires testing under various connection interruption scenarios to confirm fix stability

**Stale State in Routine Flow:**
- **Symptoms:** Issue #209 - `isJustLift` flag incorrectly preserved from previous Just Lift session into routine session
- **Files:** `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/viewmodel/MainViewModel.kt` (lines 1606-1615)
- **Trigger:** Loading a routine after completing a Just Lift session without properly resetting state
- **Workaround:** Added explicit `isJustLift = false` check in `proceedFromSummary()`, but this is defensive coding masking root cause
- **Root cause:** `.copy()` on WorkoutParameters doesn't properly reset all flags between different workout modes

**Auto-Stop Triggering Prematurely:**
- **Symptoms:** Workout stops when user hasn't actually released handles, especially in AMRAP mode
- **Files:** `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/viewmodel/MainViewModel.kt` (Issue #204, #214 mentioned)
- **Cause:** Two-tier velocity hysteresis (STALL_VELOCITY_LOW=2.5, STALL_VELOCITY_HIGH=10.0 mm/s) with grace periods not properly synchronized across different exercise types
- **Trigger:** AMRAP exercises after normal rep exercises, velocity filtering not stable during transition
- **Recent fix:** "fix: require ROM before stall/auto-stop detection (#209)" suggests ROM calibration was missing for some exercise types

**Position Calibration Issues:**
- **Symptoms:** Wrong rep counts recorded, position indicator doesn't track actual handles
- **Files:** `shared/src/commonMain/kotlin/com/devil/phoenixproject/domain/usecase/RepCounterFromMachine.kt` (824 lines) tracks multiple position ranges
- **Cause:** RepCounterFromMachine has complex position tracking with ranges, peaks, and ghosts. ROM (range of motion) calibration timing critical.
- **Impact:** Single-side exercises, compound movements, and cable setup variations cause calibration to fail
- **Note:** Comment says "matches official app behavior" but is implementing complex heuristics instead of trusting machine counters

---

## Security Considerations

**Tokens Stored Without Encryption:**
- **Risk:** Auth tokens and user data stored in unencrypted SharedPreferences (Android) or UserDefaults (iOS)
- **Files:** `shared/src/commonMain/kotlin/com/devil/phoenixproject/data/sync/PortalTokenStorage.kt` (lines 29-37)
- **Current state:** `settings[KEY_TOKEN] = response.token` stores plain JWT token
- **Recommendations:** Use platform-specific secure storage (Android KeyStore, iOS Keychain), implement token refresh/rotation, add certificate pinning for HTTPS

**Hardcoded API Endpoint:**
- **Risk:** Portal API URL hardcoded in source: `https://phoenix-portal-backend.up.railway.app`
- **Files:** `shared/src/commonMain/kotlin/com/devil/phoenixproject/data/sync/PortalApiClient.kt` (line 18)
- **Impact:** Cannot swap endpoints without rebuilding. URL appears in git history, reverse engineering apps expose it.
- **Recommendations:** Move to BuildConfig/environment variables, implement endpoint override for testing

**BLE Communication Unencrypted:**
- **Risk:** Device sends/receives all data over standard BLE without encryption. Nordic UART Service is not encrypted by default.
- **Files:** `KableBleRepository.kt` (entire file handles BLE protocol)
- **Severity:** Moderate - BLE range is limited (~10m), but local sniffing is trivial
- **Note:** Parent company app likely unencrypted too (community rescue project), but document for awareness

**No Certificate Pinning:**
- **Risk:** HTTPS requests to portal backend vulnerable to MITM via compromised CA or network-level interception
- **Files:** `PortalApiClient.kt` - HttpClient configured without SSL pinning
- **Recommendations:** Add certificate pinning using Ktor's HttpClientConfig, validate against Portal's SSL certificate

---

## Performance Bottlenecks

**Large Metrics Export on iOS:**
- **Problem:** Exporting all workout metrics to JSON can cause OOM crashes on iOS
- **Files:** `shared/src/commonMain/kotlin/com/devil/phoenixproject/util/DataBackupManager.kt` (lines 73-80)
- **Cause:** SQLite driver on iOS doesn't handle large result sets efficiently. Loading all metrics at once exhausts memory.
- **Current mitigation:** Per-session metric loading in loop instead of single query (lines 77-80)
- **Improvement path:** Implement pagination/streaming for exports, compress JSON, add progress callback for UI feedback

**Rep Counter Position Tracking Growing Memory:**
- **Problem:** `positionHistoryForDirection` list grows unbounded during long workouts
- **Files:** `RepCounterFromMachine.kt` (line 48, DIRECTION_WINDOW_SIZE=3)
- **Impact:** 60-minute workout = memory leak if position samples are added every 250ms (Heuristic poll rate)
- **Fix:** Implement circular buffer or automatic pruning of old positions

**BLE Monitor Data Collection Polling:**
- **Problem:** Polling MONITOR characteristic every 250ms (line 122 in KableBleRepository) creates constant BLE traffic and CPU wake-ups
- **Files:** `KableBleRepository.kt` (HEURISTIC_POLL_INTERVAL_MS = 250L)
- **Impact:** Battery drain on mobile devices, especially during long workouts
- **Scaling issue:** If monitoring multiple machines (trainer class scenario), per-device polling multiplies battery cost
- **Improvement path:** Implement interrupt-driven sampling (notifications) instead of polling where possible, adaptive polling based on exercise type

**Velocity Calculation EMA Smoothing:**
- **Problem:** VELOCITY_SMOOTHING_ALPHA=0.3 is aggressive and causes lag detection of quick movements
- **Files:** `KableBleRepository.kt` (lines 104-108)
- **Comment:** "prevents false stall detection during controlled tempo movements" but no benchmarks
- **Impact:** Possible slowdown in real-time rep detection, ghost reps from stale velocity
- **Test gap:** No unit tests for velocity smoothing under different movement speeds

---

## Fragile Areas

**RepCounterFromMachine State Machine:**
- **Files:** `shared/src/commonMain/kotlin/com/devil/phoenixproject/domain/usecase/RepCounterFromMachine.kt` (824 lines)
- **Why fragile:**
  - Tracks 15+ state variables (warmupReps, workingReps, hasPendingRep, activePhase, lastTopCounter, maxRepPosA, etc.)
  - Position threshold crossing logic (lines 156-183) uses stateful booleans with implicit state (wasAboveTopThreshold, wasAboveBottomThreshold)
  - Phase progress calculation (phaseProgress, phaseStartPosition, phasePeakPosition) updated in multiple methods without clear ownership
  - ROM calibration timing depends on when user starts moving - can fail silently
- **Safe modification:** Add comprehensive unit tests for all position sequences (top-only, stalled at top, rapid reps, extreme ranges). Document state diagram. Consider state machine library instead of booleans.
- **Test coverage:** `RepCounterFromMachineTest.kt` exists but line coverage needs verification for all position scenarios

**Handle Detection State Machine:**
- **Files:** `KableBleRepository.kt` (handleDetectionEnabledTimestamp, HANDLE_DETECTION_DEBOUNCE_MS debounce for iOS race condition)
- **Why fragile:**
  - Multiple entry points: enableHandleDetection(), handleMonitorMetric(), handleRepEvent()
  - Depends on external state (position, velocity) but doesn't validate preconditions
  - iOS-specific workaround (500ms debounce) masks timing issue instead of fixing root cause
  - No explicit state validation before transitions
- **Safe modification:** Add precondition checks, log state transitions with timestamps, add integration tests for rapid enable/disable cycles
- **Current issues:** iOS autostart race condition (lines 457-460) - duplicate enableHandleDetection() calls can reset state machine mid-grab

**Routine Navigation with Supersets:**
- **Files:** `MainViewModel.kt` (getNextStep function, superset-aware navigation)
- **Why fragile:**
  - Supersets require skipping logic that differs from normal exercise progression
  - `_skippedExercises` and `_completedExercises` sets must be synchronized with currentExerciseIndex
  - SupersetGroups table optionally exists in database (new feature, older DBs missing it)
  - Navigation logic in ActiveWorkoutScreen and SetReady combined with ViewModel logic
- **Safe modification:** Add invariant checks: always verify currentExerciseIndex < exercises.size, verify skipped/completed are consistent with actual counts
- **Test coverage:** Needs integration tests for supersets with different skip/complete combinations

**Workout State Transitions:**
- **Files:** `MainViewModel.kt` (_workoutState StateFlow and its transitions)
- **Why fragile:**
  - State transitions happen from multiple places: startWorkout(), stopWorkout(), handleMonitorMetric(), handleRepEvent(), proceedFromSummary()
  - Invalid transitions not blocked (e.g., Idle → Resting should not be possible)
  - State can be set by launchIn flows and launch blocks simultaneously without locks
  - Issue #97 (stopWorkout race condition) and Issue #204 (navigation timing) indicate prior state transition bugs
- **Safe modification:** Create explicit StateTransitionValidator, use Mutex for state updates, add assertion tests for invalid transitions
- **Documentation needed:** State diagram showing all valid transitions, comment each setState call with precondition

---

## Scaling Limits

**BLE Connection Pool:**
- **Current capacity:** Single active connection (one Vitruvian machine at a time)
- **Limit:** Trainer classes, group facilities would need to manage multiple machines
- **Scaling path:** Implement BLE manager with connection pool, add device prioritization (closest/strongest signal), implement queue for batch operations

**Database Query Performance:**
- **Current capacity:** Works well for single-user history (recent commit shows `_workoutHistory.value = sessions.take(20)`)
- **Limit:** 1000+ workout sessions cause full table scans to be slow
- **Scaling path:** Add database indexes on timestamp and exerciseId, implement pagination in history screens, archive old sessions

**Metrics Memory:**
- **Current capacity:** Single workout session with metrics works fine
- **Limit:** Exporting 100+ sessions or importing large backups causes OOM
- **Scaling path:** Implement streaming JSON parser/writer, compress backup format, add progress callbacks for long operations

**Rep Counter State Variables:**
- **Current capacity:** Single workout per session OK
- **Limit:** Parallel rep tracking (for trainer classes managing multiple simultaneous workouts) not supported
- **Architecture issue:** RepCounterFromMachine is stateful singleton, not reusable for multiple concurrent workouts

---

## Dependencies at Risk

**Kable BLE Library (Kotlin Multiplatform):**
- **Risk:** Relatively new library, not as battle-tested as established alternatives
- **Impact:** BLE-specific bugs not yet discovered, documentation sparse for edge cases
- **Mitigation:** Heavy testing on target devices, fallback to simulator repo for manual testing
- **Migration plan:** Android could switch to Nordic Android-BLE, iOS could use native CoreBluetooth if needed

**SQLDelight 2.0:**
- **Risk:** Still evolving, occasional breaking changes between versions
- **Impact:** Migrations could break if SQLDelight changes API
- **Current:** Using 2.0.2 with custom migration system (mostly empty in MigrationManager.kt)
- **Concern:** No actual migrations implemented yet - next breaking change will be painful
- **Plan:** Keep SQLDelight versions pinned, test schema changes early

**Ktor Client (HTTP):**
- **Risk:** Large dependency, needs configuration for each platform (SSL, cookies, proxies)
- **Impact:** Portal sync could fail on corporate networks with proxies
- **Current:** HttpClient configured with timeouts but no advanced networking options
- **Improvement:** Add user-configurable proxy/network settings

---

## Missing Critical Features

**Data Integrity After Crashes:**
- **Problem:** No transaction handling if app crashes mid-workout. Metrics partially recorded, session state inconsistent.
- **Impact:** Can't trust data from crashed sessions, manual recovery impossible
- **Example:** If BLE disconnects and app crashes during stopWorkout(), session might be half-recorded
- **Solution:** Use database transactions around session creation/completion, implement crash recovery on startup

**Offline Sync Resilience:**
- **Problem:** Portal sync fails silently if offline. No queue of pending changes.
- **Files:** `SyncManager.kt` - syncs happen but no retry logic for network failures
- **Impact:** User workout data lost if sync fails and app is closed
- **Solution:** Implement persistent sync queue, add retry with exponential backoff

**Firmware Update Management:**
- **Problem:** OTA update UUIDs defined but feature commented out (`@Suppress("unused") // Reserved for OTA update feature`)
- **Impact:** No way to update machine firmware, security patches can't be deployed
- **Files:** `KableBleRepository.kt` (lines 79-82)
- **Solution:** Implement update protocol, test on real hardware

**Graceful Degradation for Missing Machine Features:**
- **Problem:** Assumes all machines have all BLE characteristics. Missing characteristics cause silent failures.
- **Example:** Older machines might not have HEURISTIC_UUID characteristic
- **Impact:** Features silently disabled instead of showing error
- **Solution:** Detect available characteristics on connect, disable features gracefully, show warnings to user

---

## Test Coverage Gaps

**BLE State Transitions Under Adversarial Conditions:**
- **What's not tested:** Rapid connect/disconnect cycles, connection loss during handshake, MTU negotiation failures, missing characteristics
- **Files:** `KableBleRepository.kt` - no unit tests, only integration tests via SimulatorBleRepository
- **Risk:** Edge cases with real hardware not caught until user hits them
- **Priority:** High - BLE is core to the app
- **Approach:** Add unit tests for connection state machine with mock Kable peripherals, add e2e tests on real devices for each supported model

**Navigation State Consistency:**
- **What's not tested:** Completing routine with autoplay OFF, skipping all exercises in routine, exiting during different workout states, rapid back button presses
- **Files:** `ActiveWorkoutScreen.kt`, `SetReady.kt`, `RoutineComplete.kt` - navigation logic is tested indirectly via E2E tests
- **Risk:** Navigation bugs still appear (Issue #204, #142 recently fixed), regression tests needed
- **Priority:** High - user-facing issue
- **Approach:** Add screenshot tests for navigation state combinations, property-based tests for routine progression

**Rep Counter Edge Cases:**
- **What's not tested:** Rep counting with position noise, handles not returning to baseline, asymmetric movement (single-side exercises), very fast reps, stalled at top/bottom
- **Files:** `RepCounterFromMachineTest.kt` - file exists but coverage unknown
- **Risk:** Users report wrong rep counts for certain exercises, no way to debug
- **Priority:** High - data quality
- **Approach:** Add parameterized tests for all position sequences observed on real machines

**Handle Detection with Different Cable Setups:**
- **What's not tested:** Overhead pulleys (requires delta-based detection), pre-tensioned cables on startup (iOS issue #96), mixed cable/bodyweight exercises
- **Files:** `KableBleRepository.kt` - handle detection tested with simulator defaults only
- **Risk:** Feature works in lab, fails in user's home gym with unique setup
- **Priority:** Medium - affects some users
- **Approach:** Add configurable handle detection profiles, simulator presets for each known setup variant

**Database Migration and Upgrade Paths:**
- **What's not tested:** App update with schema changes, data export/import roundtrip, backup file format compatibility
- **Files:** `MigrationManager.kt` - empty, no migrations yet. DataBackupManager has import/export but no validation.
- **Risk:** Next schema change will break or silently corrupt user data
- **Priority:** Medium-High
- **Approach:** Add database schema versioning tests, backup file integrity validation, test upgrade paths between versions

**Sync Conflict Resolution:**
- **What's not tested:** Conflicting edits to routines on multiple devices, sync during offline period, partial sync failure recovery
- **Files:** `SyncManager.kt`, `PortalApiClient.kt` - sync logic exists but conflict handling untested
- **Risk:** Data loss or inconsistency when syncing from multiple devices
- **Priority:** High - users with multiple devices affected
- **Approach:** Add conflict resolution tests, implement merge strategies, add user notifications for conflicts

---

## Code Quality Issues

**Unused Imports and Dead Code:**
- Multiple `@Suppress("unused")` annotations for reserved features (OTA, auth, update state) - good for clarity but adds maintenance burden
- Simulator is fully featured but requires manual switching - could use feature flags or build variants instead

**Inconsistent Error Handling:**
- Some places use Result<T>.isSuccess/isFailure, others use try/catch
- Error messages user-visible vs. developer-visible mixed together
- No standard error recovery flow

**Comments Referencing Issue Numbers:**
- Comments like "Issue #XXX" reference something outside codebase
- Should either include Issue description in comment or document in CONCERNS.md
- Currently hard to tell which issues are resolved vs. still pending

**Logging with Direct String Interpolation:**
- Some log calls build strings eagerly instead of using lazy lambdas
- Performance impact during high-frequency metric collection
- Inconsistent log levels (some debug as info)

---

*Concerns audit: 2026-01-21*
