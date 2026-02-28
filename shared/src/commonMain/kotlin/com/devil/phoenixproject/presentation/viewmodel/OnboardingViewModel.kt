package com.devil.phoenixproject.presentation.viewmodel

import androidx.lifecycle.ViewModel
import co.touchlab.kermit.Logger
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Clock

/**
 * ViewModel for managing onboarding flow.
 * Uses multiplatform-settings for persistence across Android/iOS.
 *
 * Features:
 * - Tracks onboarding completion status
 * - Stores user preferences (goals, equipment)
 * - Re-prompts user if onboarding not completed
 */
class OnboardingViewModel(
    private val settings: Settings
) : ViewModel() {

    private val log = Logger.withTag("OnboardingViewModel")
    private val json = Json { ignoreUnknownKeys = true }

    private val _onboardingCompleted = MutableStateFlow(checkOnboardingCompleted())
    val onboardingCompleted: StateFlow<Boolean> = _onboardingCompleted.asStateFlow()

    // User preferences from onboarding
    private val _appName = MutableStateFlow(settings.getStringOrNull(APP_NAME_KEY) ?: "AUREA")
    val appName: StateFlow<String> = _appName.asStateFlow()

    private val _selectedGoals = MutableStateFlow(loadGoals())
    val selectedGoals: StateFlow<Set<String>> = _selectedGoals.asStateFlow()

    private val _hasVitruvian = MutableStateFlow(settings.getBooleanOrNull(HAS_VITRUVIAN_KEY) ?: true)
    val hasVitruvian: StateFlow<Boolean> = _hasVitruvian.asStateFlow()

    private val _hasDumbbells = MutableStateFlow(settings.getBooleanOrNull(HAS_DUMBBELLS_KEY) ?: false)
    val hasDumbbells: StateFlow<Boolean> = _hasDumbbells.asStateFlow()

    private val _hasTRX = MutableStateFlow(settings.getBooleanOrNull(HAS_TRX_KEY) ?: false)
    val hasTRX: StateFlow<Boolean> = _hasTRX.asStateFlow()

    private val _hasPullUpBar = MutableStateFlow(settings.getBooleanOrNull(HAS_PULL_UP_BAR_KEY) ?: false)
    val hasPullUpBar: StateFlow<Boolean> = _hasPullUpBar.asStateFlow()

    /**
     * Check if onboarding has been completed.
     */
    private fun checkOnboardingCompleted(): Boolean {
        return settings.getBooleanOrNull(ONBOARDING_COMPLETED_KEY) ?: false
    }

    /**
     * Load goals from stored JSON string.
     */
    private fun loadGoals(): Set<String> {
        val goalsJson = settings.getStringOrNull(SELECTED_GOALS_KEY)
        return if (goalsJson != null) {
            try {
                json.decodeFromString<GoalsData>(goalsJson).goals.toSet()
            } catch (e: Exception) {
                emptySet()
            }
        } else {
            emptySet()
        }
    }

    /**
     * Complete onboarding and save preferences.
     */
    fun completeOnboarding(
        appName: String,
        goals: Set<String>,
        hasVitruvian: Boolean,
        hasDumbbells: Boolean,
        hasTRX: Boolean,
        hasPullUpBar: Boolean
    ) {
        val timestamp = Clock.System.now().toEpochMilliseconds()

        settings[ONBOARDING_COMPLETED_KEY] = true
        settings[ONBOARDING_COMPLETED_TIMESTAMP_KEY] = timestamp
        settings[APP_NAME_KEY] = appName
        settings[HAS_VITRUVIAN_KEY] = hasVitruvian
        settings[HAS_DUMBBELLS_KEY] = hasDumbbells
        settings[HAS_TRX_KEY] = hasTRX
        settings[HAS_PULL_UP_BAR_KEY] = hasPullUpBar
        settings[SELECTED_GOALS_KEY] = json.encodeToString(GoalsData(goals.toList()))

        _onboardingCompleted.value = true
        _appName.value = appName
        _selectedGoals.value = goals
        _hasVitruvian.value = hasVitruvian
        _hasDumbbells.value = hasDumbbells
        _hasTRX.value = hasTRX
        _hasPullUpBar.value = hasPullUpBar

        log.i { "Onboarding completed at timestamp $timestamp" }
    }

    /**
     * Reset onboarding (for testing or re-onboarding).
     */
    fun resetOnboarding() {
        settings.remove(ONBOARDING_COMPLETED_KEY)
        settings.remove(ONBOARDING_COMPLETED_TIMESTAMP_KEY)
        settings.remove(SELECTED_GOALS_KEY)

        _onboardingCompleted.value = false
        _selectedGoals.value = emptySet()

        log.i { "Onboarding reset" }
    }

    companion object {
        private const val ONBOARDING_COMPLETED_KEY = "onboarding_completed"
        private const val ONBOARDING_COMPLETED_TIMESTAMP_KEY = "onboarding_completed_timestamp"
        private const val APP_NAME_KEY = "app_name"
        private const val SELECTED_GOALS_KEY = "selected_goals"
        private const val HAS_VITRUVIAN_KEY = "has_vitruvian"
        private const val HAS_DUMBBELLS_KEY = "has_dumbbells"
        private const val HAS_TRX_KEY = "has_trx"
        private const val HAS_PULL_UP_BAR_KEY = "has_pull_up_bar"
    }

    @Serializable
    private data class GoalsData(val goals: List<String>)
}
