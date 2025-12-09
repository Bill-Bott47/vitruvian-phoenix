package com.devil.phoenixproject.data.local

import com.devil.phoenixproject.domain.model.WorkoutSession
import kotlinx.serialization.Serializable

/**
 * Serializable backup data classes for export/import functionality.
 * These provide JSON serialization for database backup/restore.
 *
 * Design rationale:
 * - Uses kotlinx.serialization for cross-platform JSON conversion
 * - Uses primitive types (String, Int, Long, Float, Boolean) for JSON compatibility
 * - Includes extension functions for bidirectional Domain Model <-> Backup conversion
 */

/**
 * Backup representation of WorkoutSession
 */
@Serializable
data class WorkoutSessionBackup(
    val id: String,
    val timestamp: Long,
    val mode: String,
    val reps: Int,
    val weightPerCableKg: Float,
    val progressionKg: Float,
    val duration: Long,
    val totalReps: Int,
    val warmupReps: Int,
    val workingReps: Int,
    val isJustLift: Boolean,
    val stopAtTop: Boolean,
    val eccentricLoad: Int = 100,
    val echoLevel: Int = 1,
    val exerciseId: String? = null,
    val exerciseName: String? = null,
    val routineSessionId: String? = null,
    val routineName: String? = null,
    val safetyFlags: Int = 0,
    val deloadWarningCount: Int = 0,
    val romViolationCount: Int = 0,
    val spotterActivations: Int = 0
)

/**
 * Backup representation of WorkoutMetric
 * Note: Matches parent repo format (loadA/B, positionA/B) for backup compatibility.
 * KMP internal schema (MetricSample) uses simplified single values.
 */
@Serializable
data class WorkoutMetricBackup(
    val id: Long = 0,
    val sessionId: String,
    val timestamp: Long,
    val loadA: Float,
    val loadB: Float,
    val positionA: Float,  // Cable position in mm
    val positionB: Float,  // Cable position in mm
    val ticks: Int,
    val status: Int = 0
)

/**
 * Backup representation of Routine
 */
@Serializable
data class RoutineBackup(
    val id: String,
    val name: String,
    val description: String = "",
    val createdAt: Long,
    val lastUsed: Long? = null,
    val useCount: Int = 0
)

/**
 * Backup representation of RoutineExercise
 */
@Serializable
data class RoutineExerciseBackup(
    val id: String,
    val routineId: String,
    val exerciseName: String,
    val exerciseMuscleGroup: String,
    val exerciseEquipment: String = "",
    val exerciseDefaultCableConfig: String,
    val exerciseId: String? = null,
    val cableConfig: String,
    val orderIndex: Int,
    val setReps: String,
    val weightPerCableKg: Float,
    val setWeights: String = "",
    val mode: String = "OldSchool",
    val eccentricLoad: Int = 100,
    val echoLevel: Int = 1,
    val progressionKg: Float = 0f,
    val restSeconds: Int = 60,
    val duration: Int? = null,
    val setRestSeconds: String = "[]",
    val perSetRestTime: Boolean = false,
    val isAMRAP: Boolean = false,
    // Superset support (KMP extension)
    val supersetGroupId: String? = null,
    val supersetOrder: Int = 0,
    val supersetRestSeconds: Int = 10
)

/**
 * Backup representation of WeeklyProgram
 */
@Serializable
data class WeeklyProgramBackup(
    val id: String,
    val title: String,
    val notes: String? = null,
    val isActive: Boolean = false,
    val lastUsed: Long? = null,
    val createdAt: Long
)

/**
 * Backup representation of ProgramDay
 */
@Serializable
data class ProgramDayBackup(
    val id: Int = 0,
    val programId: String,
    val dayOfWeek: Int,
    val routineId: String
)

/**
 * Backup representation of PersonalRecord
 */
@Serializable
data class PersonalRecordBackup(
    val id: Long = 0,
    val exerciseId: String,
    val weightPerCableKg: Float,
    val reps: Int,
    val timestamp: Long,
    val workoutMode: String,
    val prType: String = "MAX_WEIGHT",
    val volume: Float = 0f
)

/**
 * Root backup data structure containing all exportable data
 */
@Serializable
data class BackupData(
    val version: Int = 1,
    val exportedAt: String,
    val appVersion: String,
    val data: BackupContent
)

/**
 * Container for all backup data entities
 */
@Serializable
data class BackupContent(
    val workoutSessions: List<WorkoutSessionBackup> = emptyList(),
    val workoutMetrics: List<WorkoutMetricBackup> = emptyList(),
    val routines: List<RoutineBackup> = emptyList(),
    val routineExercises: List<RoutineExerciseBackup> = emptyList(),
    val weeklyPrograms: List<WeeklyProgramBackup> = emptyList(),
    val programDays: List<ProgramDayBackup> = emptyList(),
    val personalRecords: List<PersonalRecordBackup> = emptyList()
)

// ============================================================================
// Extension functions: Domain Model -> Backup conversion
// ============================================================================

/**
 * Convert WorkoutSession domain model to WorkoutSessionBackup
 */
fun WorkoutSession.toBackup() = WorkoutSessionBackup(
    id = id,
    timestamp = timestamp,
    mode = mode,
    reps = reps,
    weightPerCableKg = weightPerCableKg,
    progressionKg = progressionKg,
    duration = duration,
    totalReps = totalReps,
    warmupReps = warmupReps,
    workingReps = workingReps,
    isJustLift = isJustLift,
    stopAtTop = stopAtTop,
    eccentricLoad = eccentricLoad,
    echoLevel = echoLevel,
    exerciseId = exerciseId,
    exerciseName = exerciseName,
    routineSessionId = routineSessionId,
    routineName = routineName,
    safetyFlags = safetyFlags,
    deloadWarningCount = deloadWarningCount,
    romViolationCount = romViolationCount,
    spotterActivations = spotterActivations
)

// ============================================================================
// Extension functions: Backup -> Domain Model conversion
// ============================================================================

/**
 * Convert WorkoutSessionBackup to WorkoutSession domain model
 */
fun WorkoutSessionBackup.toDomainModel() = WorkoutSession(
    id = id,
    timestamp = timestamp,
    mode = mode,
    reps = reps,
    weightPerCableKg = weightPerCableKg,
    progressionKg = progressionKg,
    duration = duration,
    totalReps = totalReps,
    warmupReps = warmupReps,
    workingReps = workingReps,
    isJustLift = isJustLift,
    stopAtTop = stopAtTop,
    eccentricLoad = eccentricLoad,
    echoLevel = echoLevel,
    exerciseId = exerciseId,
    exerciseName = exerciseName,
    routineSessionId = routineSessionId,
    routineName = routineName,
    safetyFlags = safetyFlags,
    deloadWarningCount = deloadWarningCount,
    romViolationCount = romViolationCount,
    spotterActivations = spotterActivations
)
