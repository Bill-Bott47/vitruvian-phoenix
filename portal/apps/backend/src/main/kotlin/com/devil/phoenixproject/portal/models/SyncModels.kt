package com.devil.phoenixproject.portal.models

import kotlinx.serialization.Serializable

// === Push Request/Response ===

@Serializable
data class SyncPushRequest(
    val deviceId: String,
    val deviceName: String? = null,
    val platform: String, // "android" or "ios"
    val lastSync: Long,
    val sessions: List<WorkoutSessionDto> = emptyList(),
    val records: List<PersonalRecordDto> = emptyList(),
    val phaseStats: List<PhaseStatisticsDto> = emptyList(),
    val routines: List<RoutineDto> = emptyList(),
    val routineExercises: List<RoutineExerciseDto> = emptyList(),
    val supersets: List<SupersetDto> = emptyList(),
    val exercises: List<CustomExerciseDto> = emptyList(),
    val badges: List<EarnedBadgeDto> = emptyList(),
    val gamificationStats: GamificationStatsDto? = null
)

@Serializable
data class SyncPushResponse(
    val syncTime: Long,
    val idMappings: IdMappings
)

@Serializable
data class IdMappings(
    val sessions: Map<String, String> = emptyMap(),
    val records: Map<String, String> = emptyMap(),
    val phaseStats: Map<String, String> = emptyMap(),
    val routines: Map<String, String> = emptyMap(),
    val routineExercises: Map<String, String> = emptyMap(),
    val supersets: Map<String, String> = emptyMap(),
    val exercises: Map<String, String> = emptyMap(),
    val badges: Map<String, String> = emptyMap()
)

// === Pull Request/Response ===

@Serializable
data class SyncPullRequest(
    val deviceId: String,
    val lastSync: Long
)

@Serializable
data class SyncPullResponse(
    val syncTime: Long,
    val sessions: List<WorkoutSessionDto> = emptyList(),
    val records: List<PersonalRecordDto> = emptyList(),
    val phaseStats: List<PhaseStatisticsDto> = emptyList(),
    val routines: List<RoutineDto> = emptyList(),
    val routineExercises: List<RoutineExerciseDto> = emptyList(),
    val supersets: List<SupersetDto> = emptyList(),
    val exercises: List<CustomExerciseDto> = emptyList(),
    val badges: List<EarnedBadgeDto> = emptyList(),
    val gamificationStats: GamificationStatsDto? = null
)

// === Status Response ===

@Serializable
data class SyncStatusResponse(
    val lastSync: Long?,
    val pendingChanges: Int,
    val subscriptionStatus: String,
    val subscriptionExpiresAt: String?
)

// === Entity DTOs ===

@Serializable
data class WorkoutSessionDto(
    val clientId: String,
    val serverId: String? = null,
    val timestamp: Long,
    val mode: String,
    val targetReps: Int,
    val weightPerCableKg: Float,
    val progressionKg: Float = 0f,
    val duration: Int = 0,
    val totalReps: Int = 0,
    val warmupReps: Int = 0,
    val workingReps: Int = 0,
    val isJustLift: Boolean = false,
    val stopAtTop: Boolean = false,
    val eccentricLoad: Int = 100,
    val echoLevel: Int = 1,
    val exerciseId: String? = null,
    val exerciseName: String? = null,
    val routineSessionId: String? = null,
    val routineName: String? = null,
    val safetyFlags: Int = 0,
    val deloadWarningCount: Int = 0,
    val romViolationCount: Int = 0,
    val spotterActivations: Int = 0,
    val peakForceConcentricA: Float? = null,
    val peakForceConcentricB: Float? = null,
    val peakForceEccentricA: Float? = null,
    val peakForceEccentricB: Float? = null,
    val avgForceConcentricA: Float? = null,
    val avgForceConcentricB: Float? = null,
    val avgForceEccentricA: Float? = null,
    val avgForceEccentricB: Float? = null,
    val heaviestLiftKg: Float? = null,
    val totalVolumeKg: Float? = null,
    val estimatedCalories: Float? = null,
    val warmupAvgWeightKg: Float? = null,
    val workingAvgWeightKg: Float? = null,
    val burnoutAvgWeightKg: Float? = null,
    val peakWeightKg: Float? = null,
    val rpe: Int? = null,
    val deletedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class PersonalRecordDto(
    val clientId: String,
    val serverId: String? = null,
    val exerciseId: String,
    val exerciseName: String,
    val weight: Float,
    val reps: Int,
    val oneRepMax: Float,
    val achievedAt: Long,
    val workoutMode: String,
    val prType: String = "MAX_WEIGHT",
    val volume: Float = 0f,
    val sessionId: String? = null,
    val deletedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class PhaseStatisticsDto(
    val clientId: String,
    val serverId: String? = null,
    val sessionId: String,
    val concentricKgAvg: Float,
    val concentricKgMax: Float,
    val concentricVelAvg: Float,
    val concentricVelMax: Float,
    val concentricWattAvg: Float,
    val concentricWattMax: Float,
    val eccentricKgAvg: Float,
    val eccentricKgMax: Float,
    val eccentricVelAvg: Float,
    val eccentricVelMax: Float,
    val eccentricWattAvg: Float,
    val eccentricWattMax: Float,
    val timestamp: Long,
    val deletedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class RoutineDto(
    val clientId: String,
    val serverId: String? = null,
    val name: String,
    val description: String = "",
    val lastUsed: Long? = null,
    val useCount: Int = 0,
    val deletedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class SupersetDto(
    val clientId: String,
    val serverId: String? = null,
    val routineId: String,
    val name: String,
    val colorIndex: Int = 0,
    val restBetweenSeconds: Int = 10,
    val orderIndex: Int,
    val deletedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class RoutineExerciseDto(
    val clientId: String,
    val serverId: String? = null,
    val routineId: String,
    val supersetId: String? = null,
    val exerciseId: String? = null,
    val exerciseName: String,
    val exerciseMuscleGroup: String = "",
    val exerciseEquipment: String = "",
    val exerciseDefaultCableConfig: String = "DOUBLE",
    val cableConfig: String = "DOUBLE",
    val orderIndex: Int,
    val setReps: String = "10,10,10",
    val weightPerCableKg: Float = 0f,
    val setWeights: String = "",
    val mode: String = "OldSchool",
    val eccentricLoad: Int = 100,
    val echoLevel: Int = 1,
    val progressionKg: Float = 0f,
    val restSeconds: Int = 60,
    val duration: Int? = null,
    val setRestSeconds: String = "[]",
    val perSetRestTime: Int = 0,
    val isAMRAP: Boolean = false,
    val orderInSuperset: Int = 0,
    val deletedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class CustomExerciseDto(
    val clientId: String,
    val serverId: String? = null,
    val name: String,
    val description: String? = null,
    val muscleGroup: String,
    val muscleGroups: String,
    val muscles: String? = null,
    val equipment: String,
    val movement: String? = null,
    val sidedness: String? = null,
    val grip: String? = null,
    val gripWidth: String? = null,
    val minRepRange: Float? = null,
    val aliases: String? = null,
    val defaultCableConfig: String,
    val oneRepMaxKg: Float? = null,
    val deletedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class EarnedBadgeDto(
    val clientId: String,
    val serverId: String? = null,
    val badgeId: String,
    val earnedAt: Long,
    val celebratedAt: Long? = null,
    val deletedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class GamificationStatsDto(
    val clientId: String,
    val totalWorkouts: Int = 0,
    val totalReps: Int = 0,
    val totalVolumeKg: Int = 0,
    val longestStreak: Int = 0,
    val currentStreak: Int = 0,
    val uniqueExercisesUsed: Int = 0,
    val prsAchieved: Int = 0,
    val lastWorkoutDate: Long? = null,
    val streakStartDate: Long? = null,
    val updatedAt: Long
)

// === MetricSample DTOs (for lazy load) ===

@Serializable
data class MetricSampleDto(
    val timestamp: Long,
    val position: Float? = null,
    val positionB: Float? = null,
    val velocity: Float? = null,
    val velocityB: Float? = null,
    val load: Float? = null,
    val loadB: Float? = null,
    val power: Float? = null,
    val status: Int = 0
)

@Serializable
data class MetricSamplesUploadRequest(
    val deviceId: String,
    val samples: List<MetricSampleDto>
)

@Serializable
data class MetricSamplesResponse(
    val sessionId: String,
    val samples: List<MetricSampleDto>
)
