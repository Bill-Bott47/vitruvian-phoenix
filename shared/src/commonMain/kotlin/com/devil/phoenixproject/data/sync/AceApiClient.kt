package com.devil.phoenixproject.data.sync

import co.touchlab.kermit.Logger
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * ACE Workout API Client
 * Connects to Phoenix Workout API (pai:8505) for AI-generated workouts
 */
class AceApiClient {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
    }

    companion object {
        // TODO: Move to config - pai local IP or production URL
        private const val BASE_URL = "http://192.168.1.189:8505"
        
        private val log = Logger.withTag("AceApiClient")
    }

    /**
     * Get today's AI-generated workout plan
     */
    suspend fun getTodaysWorkout(userId: String): Result<WorkoutPlanResponse> {
        return try {
            val response: WorkoutPlanResponse = client.get("$BASE_URL/workout/today") {
                header("X-User-Id", userId)
            }.body()
            Result.success(response)
        } catch (e: Exception) {
            log.e { "Failed to get workout: ${e.message}" }
            Result.failure(e)
        }
    }

    /**
     * Log completed workout
     */
    suspend fun logWorkoutCompletion(request: WorkoutCompletionRequest): Result<WorkoutCompletionResponse> {
        return try {
            val response: WorkoutCompletionResponse = client.post("$BASE_URL/workout/complete") {
                header("X-User-Id", request.user_id)
                setBody(request)
            }.body()
            Result.success(response)
        } catch (e: Exception) {
            log.e { "Failed to log workout: ${e.message}" }
            Result.failure(e)
        }
    }

    /**
     * Get exercise library
     */
    suspend fun getExerciseLibrary(): Result<List<ExerciseDefinition>> {
        return try {
            val response: List<ExerciseDefinition> = client.get("$BASE_URL/exercises").body()
            Result.success(response)
        } catch (e: Exception) {
            log.e { "Failed to get exercises: ${e.message}" }
            Result.failure(e)
        }
    }

    /**
     * Health check
     */
    suspend fun healthCheck(): Result<HealthResponse> {
        return try {
            val response: HealthResponse = client.get("$BASE_URL/health").body()
            Result.success(response)
        } catch (e: Exception) {
            log.e { "Health check failed: ${e.message}" }
            Result.failure(e)
        }
    }
}

// ─── Response Models ───

@Serializable
data class WorkoutPlanResponse(
    val plan_id: String,
    val user_id: String,
    val generated_at: String,
    val workout_name: String,
    val estimated_duration_minutes: Int,
    val exercises: List<PlannedExerciseResponse>,
    val ai_notes: String? = null
)

@Serializable
data class PlannedExerciseResponse(
    val order_index: Int,
    val exercise_id: String,
    val exercise_name: String,
    val exercise_type: String,
    val muscle_group: String,
    val sets: List<PlannedSetResponse>,
    val rest_seconds: Int = 60,
    val coaching_note: String? = null,
    val cable_config: String? = "DOUBLE",
    val is_travel_substitute: Boolean = false
)

@Serializable
data class PlannedSetResponse(
    val set_number: Int,
    val set_type: String = "STANDARD",
    val target_reps: Int? = null,
    val target_weight_kg: Double? = null,
    val target_rpe: Int? = null,
    val rest_seconds: Int = 60
)

@Serializable
data class WorkoutCompletionRequest(
    val plan_id: String? = null,
    val user_id: String,
    val started_at: String,
    val completed_at: String,
    val sets: List<CompletedSetRequest>,
    val notes: String? = null
)

@Serializable
data class CompletedSetRequest(
    val exercise_id: String,
    val set_number: Int,
    val actual_reps: Int,
    val actual_weight_kg: Double,
    val logged_rpe: Int? = null,
    val is_pr: Boolean = false
)

@Serializable
data class WorkoutCompletionResponse(
    val status: String,
    val user_id: String,
    val plan_id: String?,
    val sets_logged: Int,
    val duration_minutes: Int,
    val new_prs: Int,
    val message: String
)

@Serializable
data class ExerciseDefinition(
    val id: String,
    val name: String,
    val exercise_type: String,
    val muscle_group: String,
    val equipment: String,
    val description: String? = null,
    val coaching_note: String? = null,
    val default_cable_config: String? = null
)

@Serializable
data class HealthResponse(
    val status: String,
    val service: String,
    val version: String
)
