---
status: verifying
trigger: "iOS app crashes immediately on fresh install with SQLiteException during database query"
created: 2026-01-24T12:00:00Z
updated: 2026-01-24T12:15:00Z
---

## Current Focus

hypothesis: CONFIRMED and FIXED - iOS DriverFactory.ios.kt was creating tables with wrong schema
test: Build iOS target and run unit tests
expecting: Build succeeds, tests pass
next_action: Verify fix with production-like testing

## Symptoms

expected: App should start successfully for a brand new user installing for the first time
actual: App crashes immediately (within 0.2-11 seconds) with SQLiteException. Exception shows prepareStatement failure in SQLDelight/SQLiter.
errors: SQLiteException at co.touchlab.sqliter.interop.SQLiteException during prepareStatement -> createStatement -> awaitAsList flow
reproduction: Fresh install from TestFlight on iPhone16,1 running iOS 26.2, version 0.3.3 (2026012378)
started: User's first ever install - never had the app before

## Eliminated

## Evidence

- timestamp: 2026-01-24T12:00:00Z
  checked: User report and stack trace
  found: Crash occurs on Thread 0 (main) during coroutine exception, prepareStatement fails in SQLiter
  implication: Database connection or schema not ready when query executes

- timestamp: 2026-01-24T12:03:00Z
  checked: App.kt initialization flow
  found: MainViewModel is created immediately via koinViewModel<MainViewModel>() in App composable
  implication: ViewModel init block queries database immediately on app launch

- timestamp: 2026-01-24T12:04:00Z
  checked: MainViewModel.kt init block (lines 465-492)
  found: Three database flows start immediately: workoutRepository.getAllSessions(), workoutRepository.getAllRoutines(), exerciseRepository.importExercises()
  implication: Database queries run during app startup

- timestamp: 2026-01-24T12:05:00Z
  checked: DriverFactory.ios.kt table definitions vs VitruvianDatabase.sq
  found: CRITICAL SCHEMA MISMATCH - iOS creates tables with WRONG columns:
    - Exercise: iOS had 10 columns, SQLDelight expects 25+ (missing: description, created, muscleGroups, muscles, equipment, movement, sidedness, grip, gripWidth, minRepRange, popularity, archived, isCustom, timesPerformed, lastPerformed, aliases, defaultCableConfig)
    - WorkoutSession: iOS used exercise_id/started_at/target_weight_kg, SQLDelight uses timestamp/targetReps/weightPerCableKg
    - Routine: iOS used created_at/last_used_at/colorIndex/isFavorite, SQLDelight uses createdAt/lastUsed/description/useCount
    - RoutineExercise: iOS was missing many columns (exerciseName, exerciseMuscleGroup, exerciseEquipment, etc.)
    - PersonalRecord: iOS used exercise_id/record_type/value/achieved_at, SQLDelight uses exerciseId/exerciseName/weight/reps/oneRepMax/achievedAt/workoutMode/prType/volume
    - MetricSample: iOS used session_id/position_mm_a/velocity_mm_s_a/load_kg_a/power_w_a, SQLDelight uses sessionId/position/positionB/velocity/velocityB/load/loadB/power/status
    - Missing tables: ExerciseVideo, ConnectionLog, DiagnosticsHistory, PhaseStatistics
  implication: When SQLDelight generates queries for "SELECT * FROM Exercise", it expects 25+ columns, but iOS tables only have 10. The prepareStatement fails because the schema doesn't match.

- timestamp: 2026-01-24T12:10:00Z
  checked: iOS build after fix
  found: Build successful with no errors
  implication: Schema changes are syntactically correct

- timestamp: 2026-01-24T12:12:00Z
  checked: Unit tests after fix
  found: All tests pass
  implication: Fix doesn't break existing functionality

## Resolution

root_cause: iOS DriverFactory.ios.kt manually creates database tables with a completely different schema than what SQLDelight's VitruvianDatabase.sq defines. The tables have wrong column names, missing columns, and incorrect types. Additionally, 4 tables were completely missing (ExerciseVideo, ConnectionLog, DiagnosticsHistory, PhaseStatistics). When SQLDelight-generated queries run (e.g., selectAllExercises which does "SELECT * FROM Exercise"), they fail because the expected columns don't exist.

fix: Rewrote DriverFactory.ios.kt createAllTables() to match VitruvianDatabase.sq exactly:
  1. Exercise table: Added all 25 columns to match SQLDelight schema
  2. WorkoutSession table: Fixed column names (timestamp not started_at, targetReps not target_reps, etc.)
  3. Routine table: Fixed column names (createdAt not created_at, lastUsed not last_used_at, etc.)
  4. RoutineExercise table: Added all exercise metadata columns
  5. PersonalRecord table: Fixed to match schema (exerciseId, exerciseName, weight, reps, oneRepMax, etc.)
  6. MetricSample table: Fixed column names (sessionId, position, positionB, velocity, etc.)
  7. Added missing tables: ExerciseVideo, ConnectionLog, DiagnosticsHistory, PhaseStatistics
  8. Updated createAllIndexes() to use correct column names

verification: iOS target compiles successfully, unit tests pass

files_changed:
  - shared/src/iosMain/kotlin/com/devil/phoenixproject/data/local/DriverFactory.ios.kt
