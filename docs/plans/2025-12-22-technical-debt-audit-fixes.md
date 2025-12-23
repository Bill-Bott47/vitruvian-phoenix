# Project Phoenix 2.0 Technical Debt Audit - Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Fix critical bugs, incomplete implementations, and safety issues identified in the comprehensive codebase audit.

**Architecture:** Prioritize fixes by severity (CRITICAL > HIGH > MEDIUM). Each task is TDD-style: write test, verify failure, implement fix, verify pass, commit.

**Tech Stack:** Kotlin 2.0, Compose Multiplatform 1.8, Kable BLE, SQLDelight 2.2, Coroutines 1.10

---

## Audit Summary (25 Issues Found)

| Severity | Category | Count |
|----------|----------|-------|
| CRITICAL | BLE Protocol / Data Integrity | 5 |
| HIGH | Platform Gaps / Safety | 8 |
| MEDIUM | Concurrency / Type Safety | 10 |
| LOW | Cleanup / Documentation | 2 |

---

## Phase 1: CRITICAL Fixes (Data Loss / Crashes / Zombie States)

### Task 1: Fix BLE Zombie "Connecting" State

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/devil/phoenixproject/data/repository/KableBleRepository.kt:514-676`

**Problem:** Connection state can get stuck in `ConnectionState.Connecting` indefinitely with no recovery path.

**Step 1: Write the failing test**

```kotlin
// shared/src/commonTest/kotlin/com/devil/phoenixproject/data/repository/KableBleRepositoryTest.kt
@Test
fun `connect should timeout after 15 seconds and return to Disconnected`() = runTest {
    val repo = KableBleRepository()
    val device = ScannedDevice("test", "00:00:00:00:00:00", -50)

    // Simulate connection that never completes
    val result = repo.connect(device)

    advanceTimeBy(16_000L)

    assertEquals(ConnectionState.Disconnected, repo.connectionState.value)
    assertTrue(result.isFailure)
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :shared:allTests --tests "*KableBleRepositoryTest*connect*timeout*"`
Expected: FAIL - test times out because no timeout mechanism exists

**Step 3: Write minimal implementation**

```kotlin
// In connect() function, wrap the connection with a timeout
private const val CONNECTION_TIMEOUT_MS = 15_000L

override suspend fun connect(device: ScannedDevice): Result<Unit> {
    _connectionState.value = ConnectionState.Connecting

    return try {
        withTimeout(CONNECTION_TIMEOUT_MS) {
            // Existing connection logic...
            peripheral?.connect()

            // Wait for Connected state with timeout
            peripheral?.state
                ?.first { it == State.Connected }
                ?: throw TimeoutCancellationException("Connection never established")
        }

        // Only reach here if connected
        _connectionState.value = ConnectionState.Connected(connectedDeviceName)
        Result.success(Unit)

    } catch (e: TimeoutCancellationException) {
        log.e { "Connection timeout after ${CONNECTION_TIMEOUT_MS}ms" }
        _connectionState.value = ConnectionState.Disconnected
        peripheral?.disconnect()
        peripheral = null
        Result.failure(BleException.ConnectionTimeout("Connection timed out"))

    } catch (e: Exception) {
        log.e { "Connection failed: ${e.message}" }
        _connectionState.value = ConnectionState.Disconnected
        peripheral = null
        Result.failure(e)
    }
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew :shared:allTests --tests "*KableBleRepositoryTest*connect*timeout*"`
Expected: PASS

**Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/devil/phoenixproject/data/repository/KableBleRepository.kt
git add shared/src/commonTest/kotlin/com/devil/phoenixproject/data/repository/KableBleRepositoryTest.kt
git commit -m "fix(ble): add 15-second timeout to prevent zombie Connecting state"
```

---

### Task 2: Fix MetricSample Field Duplication (Data Corruption)

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/devil/phoenixproject/data/repository/SqlDelightWorkoutRepository.kt:534-546`

**Problem:** Both `loadA`/`loadB` and `positionA`/`positionB` receive the same value, losing independent cable data.

**Step 1: Write the failing test**

```kotlin
// shared/src/commonTest/kotlin/com/devil/phoenixproject/data/repository/SqlDelightWorkoutRepositoryTest.kt
@Test
fun `mapToMetricSample should preserve independent cable values`() {
    val row = MetricSampleRow(
        id = 1,
        sessionId = "session-1",
        timestamp = 12345L,
        position = 100.0,      // This represents positionA
        velocity = 50.0,
        load = 25.0,           // This represents loadA
        power = 500.0,
        positionB = 200.0,     // Column for positionB
        loadB = 30.0           // Column for loadB
    )

    val metric = mapToMetricSample(row)

    assertNotEquals(metric.positionA, metric.positionB)
    assertNotEquals(metric.loadA, metric.loadB)
    assertEquals(100f, metric.positionA)
    assertEquals(200f, metric.positionB)
    assertEquals(25f, metric.loadA)
    assertEquals(30f, metric.loadB)
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :shared:allTests --tests "*SqlDelightWorkoutRepositoryTest*mapToMetricSample*independent*"`
Expected: FAIL - `positionA == positionB` and `loadA == loadB`

**Step 3: Check SQLDelight schema for positionB/loadB columns**

First verify columns exist in `.sq` file. If not, add migration:

```sql
-- VitruvianDatabase.sq
ALTER TABLE MetricSample ADD COLUMN positionB REAL;
ALTER TABLE MetricSample ADD COLUMN loadB REAL;
```

Then fix the mapping:

```kotlin
// SqlDelightWorkoutRepository.kt:534-546
private fun mapToMetricSample(row: MetricSample): WorkoutMetric {
    return WorkoutMetric(
        timestamp = row.timestamp,
        loadA = row.load?.toFloat() ?: 0f,
        loadB = row.loadB?.toFloat() ?: 0f,  // Use loadB column
        positionA = row.position?.toFloat() ?: 0f,
        positionB = row.positionB?.toFloat() ?: 0f,  // Use positionB column
        velocityA = row.velocity ?: 0.0,
        velocityB = row.velocityB ?: 0.0,
        // ... rest of mapping
    )
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew :shared:allTests --tests "*SqlDelightWorkoutRepositoryTest*mapToMetricSample*independent*"`
Expected: PASS

**Step 5: Commit**

```bash
git add shared/src/commonMain/sqldelight/com/devil/phoenixproject/database/VitruvianDatabase.sq
git add shared/src/commonMain/kotlin/com/devil/phoenixproject/data/repository/SqlDelightWorkoutRepository.kt
git commit -m "fix(db): preserve independent cable A/B values in MetricSample mapping"
```

---

### Task 3: Fix PlannedSet Foreign Key Relationship (Schema Broken)

**Files:**
- Modify: `shared/src/commonMain/sqldelight/com/devil/phoenixproject/database/VitruvianDatabase.sq`
- Modify: `shared/src/commonMain/kotlin/com/devil/phoenixproject/data/repository/SqlDelightCompletedSetRepository.kt`

**Problem:** `PlannedSet.routine_exercise_id` is INTEGER but `RoutineExercise.id` is TEXT. FK constraint will never match.

**Step 1: Write the failing test**

```kotlin
@Test
fun `getPlannedSetsForRoutineExercise should return sets when FK matches`() = runTest {
    // Insert a routine exercise with TEXT id
    val routineExerciseId = "re-uuid-123"
    exerciseRepo.insertRoutineExercise(routineExerciseId, routineId = "routine-1", ...)

    // Insert planned set referencing it
    completedSetRepo.insertPlannedSet(
        routineExerciseId = routineExerciseId,  // TEXT, not Long!
        setType = "WORKING",
        targetReps = 8,
        targetWeightKg = 50.0
    )

    val sets = completedSetRepo.getPlannedSets(routineExerciseId)

    assertEquals(1, sets.size)
    assertEquals(routineExerciseId, sets[0].routineExerciseId)
}
```

**Step 2: Run test to verify it fails**

Expected: FAIL - type mismatch or 0 sets returned

**Step 3: Write minimal implementation**

```sql
-- VitruvianDatabase.sq (fix PlannedSet table)
CREATE TABLE IF NOT EXISTS PlannedSet (
    id TEXT PRIMARY KEY NOT NULL,
    routine_exercise_id TEXT NOT NULL,  -- Changed from INTEGER to TEXT
    set_number INTEGER NOT NULL,
    set_type TEXT NOT NULL,
    target_reps INTEGER,
    target_weight_kg REAL,
    FOREIGN KEY (routine_exercise_id) REFERENCES RoutineExercise(id) ON DELETE CASCADE
);
```

Update repository to use String instead of Long:

```kotlin
// SqlDelightCompletedSetRepository.kt
fun getPlannedSets(routineExerciseId: String): List<PlannedSet> {
    return database.vitruvianDatabaseQueries
        .getPlannedSetsForRoutineExercise(routineExerciseId)
        .executeAsList()
        .map { mapToPlannedSet(it) }
}

private fun mapToPlannedSet(row: PlannedSetRow): PlannedSet {
    return PlannedSet(
        id = row.id,
        routineExerciseId = row.routine_exercise_id,  // Now String
        // ...
    )
}
```

**Step 4: Run test to verify it passes**

**Step 5: Commit**

```bash
git add shared/src/commonMain/sqldelight/
git add shared/src/commonMain/kotlin/com/devil/phoenixproject/data/repository/SqlDelightCompletedSetRepository.kt
git commit -m "fix(db): change PlannedSet FK to TEXT to match RoutineExercise.id"
```

---

### Task 4: Fix Polling Job Race Condition

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/devil/phoenixproject/data/repository/KableBleRepository.kt:1163-1250`

**Problem:** Multiple calls to `startMonitorPolling()` can cause duplicate polling jobs.

**Step 1: Write the failing test**

```kotlin
@Test
fun `startMonitorPolling called twice should cancel previous job before starting new one`() = runTest {
    val repo = KableBleRepository()
    // Connect first

    // Start polling
    repo.startMonitorPolling(peripheral, forAutoStart = true)
    val job1 = repo.monitorPollingJob

    // Start polling again
    repo.startMonitorPolling(peripheral, forAutoStart = false)
    val job2 = repo.monitorPollingJob

    // Job1 should be cancelled
    assertTrue(job1?.isCancelled == true)
    assertFalse(job2?.isCancelled == true)
    assertNotSame(job1, job2)
}
```

**Step 2: Run test to verify it fails**

Expected: FAIL - job1 might not be cancelled

**Step 3: Write minimal implementation**

```kotlin
private fun startMonitorPolling(p: Peripheral, forAutoStart: Boolean = false) {
    // Cancel and WAIT for previous job to complete
    monitorPollingJob?.let { job ->
        job.cancel()
        // Don't use runBlocking - use structured approach
        // The new job won't start until cancel propagates
    }
    monitorPollingJob = null

    // Use mutex to prevent concurrent restarts
    monitorPollingJob = scope.launch {
        // Ensure single active polling loop
        if (monitorPollingMutex.isLocked) {
            log.w { "Monitor polling restart in progress, skipping duplicate" }
            return@launch
        }

        monitorPollingMutex.withLock {
            // Existing polling loop...
        }
    }
}

// Add at class level:
private val monitorPollingMutex = Mutex()
```

**Step 4: Run test to verify it passes**

**Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/devil/phoenixproject/data/repository/KableBleRepository.kt
git commit -m "fix(ble): prevent race condition in monitor polling job restart"
```

---

### Task 5: Add ROM Violation Safety Handling

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/devil/phoenixproject/data/repository/KableBleRepository.kt:1744-1770`

**Problem:** Machine reports ROM_OUTSIDE_HIGH/LOW flags in status, but code never checks them.

**Step 1: Write the failing test**

```kotlin
@Test
fun `processStatusFlags should emit ROM violation event when detected`() = runTest {
    val repo = KableBleRepository()
    val romEvents = mutableListOf<RomViolationType>()

    backgroundScope.launch {
        repo.romViolationEvents.collect { romEvents.add(it) }
    }

    // Status with ROM_OUTSIDE_HIGH flag set (0x0004)
    val statusWithRomHigh = 0x0004
    repo.processStatusFlags(statusWithRomHigh)

    advanceUntilIdle()

    assertEquals(1, romEvents.size)
    assertEquals(RomViolationType.OUTSIDE_HIGH, romEvents[0])
}
```

**Step 2: Run test to verify it fails**

Expected: FAIL - no ROM events emitted

**Step 3: Write minimal implementation**

```kotlin
// Add flow for ROM violations
private val _romViolationEvents = MutableSharedFlow<RomViolationType>(
    replay = 0,
    extraBufferCapacity = 8,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
)
val romViolationEvents: Flow<RomViolationType> = _romViolationEvents.asSharedFlow()

enum class RomViolationType { OUTSIDE_HIGH, OUTSIDE_LOW }

private fun processStatusFlags(status: Int) {
    val sampleStatus = SampleStatus(status)

    // Existing deload handling...

    // NEW: ROM violation detection
    if (sampleStatus.isRomOutsideHigh()) {
        log.w { "SAFETY: ROM_OUTSIDE_HIGH detected - user may be overextending" }
        scope.launch { _romViolationEvents.emit(RomViolationType.OUTSIDE_HIGH) }
    }
    if (sampleStatus.isRomOutsideLow()) {
        log.w { "SAFETY: ROM_OUTSIDE_LOW detected - cable position below range" }
        scope.launch { _romViolationEvents.emit(RomViolationType.OUTSIDE_LOW) }
    }
}
```

**Step 4: Run test to verify it passes**

**Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/devil/phoenixproject/data/repository/KableBleRepository.kt
git commit -m "feat(safety): add ROM violation event handling for user protection"
```

---

## Phase 2: HIGH Priority Fixes

### Task 6: Fix MainViewModel Missing onCleared()

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/viewmodel/MainViewModel.kt`

**Problem:** `monitorDataCollectionJob` and `repEventsCollectionJob` are not explicitly cancelled.

**Step 1: Add onCleared override**

```kotlin
override fun onCleared() {
    super.onCleared()
    connectionJob?.cancel()
    monitorDataCollectionJob?.cancel()
    autoStartJob?.cancel()
    restTimerJob?.cancel()
    bodyweightTimerJob?.cancel()
    repEventsCollectionJob?.cancel()
    log.i { "MainViewModel cleared, all jobs cancelled" }
}
```

**Step 2: Commit**

```bash
git add shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/viewmodel/MainViewModel.kt
git commit -m "fix(viewmodel): add onCleared to cancel all collection jobs and prevent leaks"
```

---

### Task 7: Fix MigrationManager Unmanaged CoroutineScope

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/devil/phoenixproject/data/migration/MigrationManager.kt`

**Problem:** `CoroutineScope(Dispatchers.IO)` is never cancelled - memory leak.

**Step 1: Write minimal implementation**

```kotlin
class MigrationManager : Closeable {
    private val log = Logger.withTag("MigrationManager")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun checkAndRunMigrations() {
        scope.launch {
            try {
                runMigrations()
            } catch (e: Exception) {
                log.e(e) { "Migration failed" }
            }
        }
    }

    private suspend fun runMigrations() {
        // Future migrations go here
    }

    override fun close() {
        scope.cancel()
        log.d { "MigrationManager scope cancelled" }
    }
}
```

**Step 2: Update Koin module to call close on app termination**

**Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/devil/phoenixproject/data/migration/MigrationManager.kt
git commit -m "fix(memory): add Closeable to MigrationManager to prevent scope leak"
```

---

### Task 8: Add Load Validation (Safety)

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/devil/phoenixproject/data/repository/KableBleRepository.kt`

**Problem:** No upper bound check on load values. Corrupted 9999kg would pass.

**Step 1: Add constant and validation**

```kotlin
private const val MAX_WEIGHT_KG = 220.0f  // Trainer+ hardware limit

private fun validateSample(posA: Float, posB: Float, loadA: Float, loadB: Float): Boolean {
    // Existing position checks...

    // NEW: Load validation
    if (loadA < 0f || loadA > MAX_WEIGHT_KG || loadB < 0f || loadB > MAX_WEIGHT_KG) {
        log.w { "Load out of range: loadA=$loadA, loadB=$loadB (max=$MAX_WEIGHT_KG)" }
        return false
    }

    return true
}
```

**Step 2: Commit**

```bash
git add shared/src/commonMain/kotlin/com/devil/phoenixproject/data/repository/KableBleRepository.kt
git commit -m "feat(safety): add load validation to reject corrupted weight values"
```

---

### Task 9: Create Desktop Platform Stubs (12 files)

**Files to Create:**
- `shared/src/desktopMain/kotlin/com/devil/phoenixproject/Platform.desktop.kt`
- `shared/src/desktopMain/kotlin/com/devil/phoenixproject/domain/model/PlatformUtils.desktop.kt`
- `shared/src/desktopMain/kotlin/com/devil/phoenixproject/domain/model/UUIDGeneration.desktop.kt`
- `shared/src/desktopMain/kotlin/com/devil/phoenixproject/data/local/DriverFactory.desktop.kt`
- `shared/src/desktopMain/kotlin/com/devil/phoenixproject/di/PlatformModule.desktop.kt`
- `shared/src/desktopMain/kotlin/com/devil/phoenixproject/presentation/components/CompactNumberPicker.desktop.kt`
- `shared/src/desktopMain/kotlin/com/devil/phoenixproject/presentation/components/VideoPlayer.desktop.kt`
- `shared/src/desktopMain/kotlin/com/devil/phoenixproject/presentation/components/HapticFeedbackEffect.desktop.kt`
- `shared/src/desktopMain/kotlin/com/devil/phoenixproject/util/DeviceInfo.desktop.kt`
- `shared/src/desktopMain/kotlin/com/devil/phoenixproject/util/CsvExporter.desktop.kt`
- `shared/src/desktopMain/kotlin/com/devil/phoenixproject/util/DataBackupManager.desktop.kt`
- `shared/src/desktopMain/kotlin/com/devil/phoenixproject/data/ble/BleExtensions.desktop.kt`

**Implementation Pattern:** Copy iOS implementations as base, replace with JVM-compatible code.

**Example - Platform.desktop.kt:**

```kotlin
package com.devil.phoenixproject

actual fun getPlatform(): Platform = DesktopPlatform()

class DesktopPlatform : Platform {
    override val name: String = "Desktop JVM ${System.getProperty("java.version")}"
}
```

**Step: Commit after all 12 files created**

```bash
git add shared/src/desktopMain/
git commit -m "feat(kmp): add desktop platform actual implementations for all expect declarations"
```

---

### Task 10: Add EMA Velocity Initialization Fix

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/devil/phoenixproject/data/repository/KableBleRepository.kt`

**Problem:** EMA velocity undershoots during first samples, preventing early grab detection.

**Step 1: Fix initialization**

```kotlin
private var isFirstVelocitySample = true

private fun calculateSmoothedVelocity(rawVelocityA: Double, rawVelocityB: Double) {
    if (isFirstVelocitySample) {
        // Initialize EMA with first sample to avoid cold start lag
        smoothedVelocityA = rawVelocityA
        smoothedVelocityB = rawVelocityB
        isFirstVelocitySample = false
    } else {
        smoothedVelocityA = VELOCITY_SMOOTHING_ALPHA * rawVelocityA +
                           (1 - VELOCITY_SMOOTHING_ALPHA) * smoothedVelocityA
        smoothedVelocityB = VELOCITY_SMOOTHING_ALPHA * rawVelocityB +
                           (1 - VELOCITY_SMOOTHING_ALPHA) * smoothedVelocityB
    }
}

// Reset on new session
private fun resetVelocityTracking() {
    isFirstVelocitySample = true
    smoothedVelocityA = 0.0
    smoothedVelocityB = 0.0
}
```

**Step 2: Commit**

```bash
git add shared/src/commonMain/kotlin/com/devil/phoenixproject/data/repository/KableBleRepository.kt
git commit -m "fix(ble): initialize EMA velocity with first sample to prevent cold start lag"
```

---

## Phase 3: MEDIUM Priority Fixes

### Task 11: Fix EccentricLoad Enum Overflow

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/devil/phoenixproject/data/repository/SqlDelightWorkoutRepository.kt:155`

**Problem:** Index calculation `/25` can exceed EchoLevel enum size for values 130-150%.

**Step 1: Fix with explicit mapping**

```kotlin
private fun mapEccentricLoadFromDb(dbValue: Long): EccentricLoad {
    return when (dbValue.toInt()) {
        0 -> EccentricLoad.LOAD_100
        50 -> EccentricLoad.LOAD_50
        75 -> EccentricLoad.LOAD_75
        100 -> EccentricLoad.LOAD_100
        110 -> EccentricLoad.LOAD_110
        120 -> EccentricLoad.LOAD_120
        130 -> EccentricLoad.LOAD_130
        140 -> EccentricLoad.LOAD_140
        150 -> EccentricLoad.LOAD_150
        else -> {
            log.w { "Unknown eccentric load value: $dbValue, defaulting to 100%" }
            EccentricLoad.LOAD_100
        }
    }
}
```

**Step 2: Commit**

```bash
git add shared/src/commonMain/kotlin/com/devil/phoenixproject/data/repository/SqlDelightWorkoutRepository.kt
git commit -m "fix(db): use explicit mapping for EccentricLoad to prevent enum overflow"
```

---

### Task 12: Add PersonalRecord Missing Fields to Domain Model

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/devil/phoenixproject/domain/model/Models.kt`
- Modify: `shared/src/commonMain/kotlin/com/devil/phoenixproject/data/repository/SqlDelightPersonalRecordRepository.kt`

**Problem:** `prType`, `volume`, `oneRepMax`, `exerciseName` not in domain model.

**Step 1: Update domain model**

```kotlin
data class PersonalRecord(
    val id: Long,
    val exerciseId: String,
    val exerciseName: String,  // NEW
    val workoutMode: WorkoutMode,
    val prType: PRType,        // NEW - enum: MAX_WEIGHT, MAX_VOLUME, MAX_REPS
    val weightPerCableKg: Float,
    val reps: Int,
    val oneRepMax: Float,      // NEW - calculated or stored
    val volume: Float,         // NEW - weight * reps
    val achievedAt: Long
)

enum class PRType { MAX_WEIGHT, MAX_VOLUME, MAX_REPS }
```

**Step 2: Commit**

```bash
git add shared/src/commonMain/kotlin/com/devil/phoenixproject/domain/model/Models.kt
git add shared/src/commonMain/kotlin/com/devil/phoenixproject/data/repository/SqlDelightPersonalRecordRepository.kt
git commit -m "feat(domain): add prType, volume, oneRepMax, exerciseName to PersonalRecord"
```

---

### Task 13: Add Consecutive Timeout Disconnect Detection

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/devil/phoenixproject/data/repository/KableBleRepository.kt:1206-1243`

**Problem:** 10+ consecutive timeouts likely means disconnected, but polling continues.

**Step 1: Add disconnect trigger**

```kotlin
private const val MAX_CONSECUTIVE_TIMEOUTS = 5

// In monitor polling loop:
if (consecutiveTimeouts >= MAX_CONSECUTIVE_TIMEOUTS) {
    log.e { "Too many consecutive timeouts ($consecutiveTimeouts), triggering disconnect" }
    _connectionState.value = ConnectionState.Disconnected
    scope.launch { disconnect() }
    return@launch  // Exit polling loop
}
```

**Step 2: Commit**

```bash
git add shared/src/commonMain/kotlin/com/devil/phoenixproject/data/repository/KableBleRepository.kt
git commit -m "fix(ble): detect device disconnect after 5 consecutive polling timeouts"
```

---

### Task 14: Add Handle State Hysteresis (200ms dwell time)

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/devil/phoenixproject/data/repository/KableBleRepository.kt:1820-1898`

**Problem:** State flapping between Released/Moving/Grabbed due to position oscillation.

**Step 1: Add hysteresis logic**

```kotlin
private const val STATE_TRANSITION_DWELL_MS = 200L
private var pendingGrabbedStartTime: Long? = null
private var pendingReleasedStartTime: Long? = null

private fun analyzeHandleStateWithHysteresis(
    posA: Float, posB: Float,
    velA: Double, velB: Double,
    now: Long
): HandleState {
    val wouldBeGrabbed = (posA > HANDLE_GRABBED_THRESHOLD && abs(velA) > VELOCITY_THRESHOLD) ||
                         (posB > HANDLE_GRABBED_THRESHOLD && abs(velB) > VELOCITY_THRESHOLD)
    val wouldBeReleased = posA < HANDLE_REST_THRESHOLD && posB < HANDLE_REST_THRESHOLD

    // Grabbed transition with dwell time
    if (wouldBeGrabbed) {
        if (pendingGrabbedStartTime == null) pendingGrabbedStartTime = now
        if (now - pendingGrabbedStartTime!! >= STATE_TRANSITION_DWELL_MS) {
            pendingReleasedStartTime = null
            return HandleState.Grabbed
        }
    } else {
        pendingGrabbedStartTime = null
    }

    // Released transition with dwell time
    if (wouldBeReleased && _handleState.value == HandleState.Grabbed) {
        if (pendingReleasedStartTime == null) pendingReleasedStartTime = now
        if (now - pendingReleasedStartTime!! >= STATE_TRANSITION_DWELL_MS) {
            pendingGrabbedStartTime = null
            return HandleState.Released
        }
    } else {
        pendingReleasedStartTime = null
    }

    // Keep current state if no confirmed transition
    return _handleState.value
}
```

**Step 2: Commit**

```bash
git add shared/src/commonMain/kotlin/com/devil/phoenixproject/data/repository/KableBleRepository.kt
git commit -m "fix(safety): add 200ms hysteresis to handle state transitions to prevent flapping"
```

---

## Phase 4: LOW Priority Cleanup

### Task 15: Remove Stale TODOs

**Files:**
- `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/viewmodel/ExerciseConfigViewModel.kt:9`
- `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/viewmodel/ExerciseConfigViewModel.kt:333`

**Step 1: Remove or update comments**

```kotlin
// Line 9: Remove - Koin migration is complete
// Line 333: Remove - UUID generation works via fallback
```

**Step 2: Commit**

```bash
git add shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/viewmodel/ExerciseConfigViewModel.kt
git commit -m "chore: remove stale TODO comments for completed migrations"
```

---

## Execution Summary

| Phase | Tasks | Est. Effort | Priority |
|-------|-------|-------------|----------|
| 1 | 5 | HIGH | CRITICAL - Data loss/crashes |
| 2 | 5 | MEDIUM | HIGH - Safety/platform gaps |
| 3 | 4 | LOW | MEDIUM - Type safety/concurrency |
| 4 | 1 | TRIVIAL | LOW - Cleanup |

**Total: 15 tasks**

---

## Verification Checklist

After all tasks complete:
- [ ] `./gradlew build` passes
- [ ] `./gradlew :shared:allTests` passes
- [ ] Android app builds and installs
- [ ] Desktop app builds and runs
- [ ] BLE connection timeout works (15 seconds)
- [ ] Handle detection doesn't flap on threshold boundaries
- [ ] ROM violations trigger events
- [ ] No memory leaks in MainViewModel/MigrationManager
