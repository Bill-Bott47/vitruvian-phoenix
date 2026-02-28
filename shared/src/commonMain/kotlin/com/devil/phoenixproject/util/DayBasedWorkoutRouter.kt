package com.devil.phoenixproject.util

import java.time.LocalDate
import java.time.DayOfWeek
import java.time.ZoneId

/**
 * Day-based workout routing logic.
 * 
 * Routes users to the appropriate workout based on the current day of the week.
 * Follows a 4-day upper/lower split with optional Day 5 hybrid:
 * - Monday: Upper A (Push emphasis)
 * - Tuesday: Lower A (Squat emphasis)
 * - Wednesday: Upper B (Pull emphasis)
 * - Thursday: Lower B (Hinge emphasis)
 * - Friday: Upper C (Hybrid/Accessory)
 * - Saturday: Lower C (Full lower body)
 * - Sunday: Active Recovery / Rest
 */
object DayBasedWorkoutRouter {

    /**
     * Get the recommended workout type for today.
     */
    fun getTodaysWorkoutType(): WorkoutDayType {
        val dayOfWeek = getCurrentDayOfWeek()
        return getWorkoutTypeForDay(dayOfWeek)
    }

    /**
     * Get the recommended workout type for a specific day.
     */
    fun getWorkoutTypeForDay(dayOfWeek: Int): WorkoutDayType {
        return when (dayOfWeek) {
            1 -> WorkoutDayType.UPPER_A  // Monday
            2 -> WorkoutDayType.LOWER_A  // Tuesday
            3 -> WorkoutDayType.UPPER_B  // Wednesday
            4 -> WorkoutDayType.LOWER_B  // Thursday
            5 -> WorkoutDayType.UPPER_C  // Friday
            6 -> WorkoutDayType.LOWER_C  // Saturday
            7 -> WorkoutDayType.RECOVERY  // Sunday
            else -> WorkoutDayType.UPPER_A
        }
    }

    /**
     * Get current day of week (1 = Monday, 7 = Sunday).
     */
    fun getCurrentDayOfWeek(): Int {
        val today = LocalDate.now(ZoneId.systemDefault())
        return today.dayOfWeek.value
    }

    /**
     * Get the workout name for display.
     */
    fun getWorkoutName(type: WorkoutDayType): String {
        return when (type) {
            WorkoutDayType.UPPER_A -> "Upper A"
            WorkoutDayType.LOWER_A -> "Lower A"
            WorkoutDayType.UPPER_B -> "Upper B"
            WorkoutDayType.LOWER_B -> "Lower B"
            WorkoutDayType.UPPER_C -> "Upper C"
            WorkoutDayType.LOWER_C -> "Lower C"
            WorkoutDayType.RECOVERY -> "Active Recovery"
        }
    }

    /**
     * Get the workout description.
     */
    fun getWorkoutDescription(type: WorkoutDayType): String {
        return when (type) {
            WorkoutDayType.UPPER_A -> "Push emphasis - Chest, Shoulders, Triceps"
            WorkoutDayType.LOWER_A -> "Squat emphasis - Quads, Glutes, Hamstrings"
            WorkoutDayType.UPPER_B -> "Pull emphasis - Back, Biceps, Rear Delts"
            WorkoutDayType.LOWER_B -> "Hinge emphasis - Hamstrings, Glutes, Calves"
            WorkoutDayType.UPPER_C -> "Hybrid - Mix of push/pull for balance"
            WorkoutDayType.LOWER_C -> "Full lower - Complete leg development"
            WorkoutDayType.RECOVERY -> "Light movement, stretching, mobility"
        }
    }

    /**
     * Check if today is a rest day.
     */
    fun isRestDay(): Boolean {
        return getTodaysWorkoutType() == WorkoutDayType.RECOVERY
    }

    /**
     * Get the day name for display.
     */
    fun getDayName(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            1 -> "Monday"
            2 -> "Tuesday"
            3 -> "Wednesday"
            4 -> "Thursday"
            5 -> "Friday"
            6 -> "Saturday"
            7 -> "Sunday"
            else -> "Unknown"
        }
    }

    /**
     * Check if we should prompt the user about a missed workout.
     * Returns true if yesterday was a workout day and they haven't done it.
     */
    fun shouldPromptMissedWorkout(lastWorkoutDayOfWeek: Int?): Boolean {
        if (lastWorkoutDayOfWeek == null) return false
        val today = getCurrentDayOfWeek()
        val daysSince = today - lastWorkoutDayOfWeek
        // Prompt if it's been 1-2 days and yesterday was a workout day (not Sunday)
        return daysSince in 1..2 && lastWorkoutDayOfWeek != 7
    }

    /**
     * Get number of days since last workout.
     */
    fun getDaysSinceLastWorkout(lastWorkoutDayOfWeek: Int?): Int {
        if (lastWorkoutDayOfWeek == null) return 0
        val today = getCurrentDayOfWeek()
        return if (today >= lastWorkoutDayOfWeek) {
            today - lastWorkoutDayOfWeek
        } else {
            // Wrapped around the week
            (7 - lastWorkoutDayOfWeek) + today
        }
    }
}

/**
 * Types of workout days in the ACE split.
 */
enum class WorkoutDayType {
    UPPER_A,   // Push emphasis
    LOWER_A,   // Squat emphasis
    UPPER_B,   // Pull emphasis
    LOWER_B,   // Hinge emphasis
    UPPER_C,   // Hybrid/Accessory
    LOWER_C,   // Full lower
    RECOVERY   // Rest day
}
