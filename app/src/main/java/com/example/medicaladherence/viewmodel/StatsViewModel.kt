package com.example.medicaladherence.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicaladherence.data.repo.InMemoryMedicationRepository
import com.example.medicaladherence.data.repo.RepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

data class DayBar(
    val dayLabel: String,
    val percentage: Int
)

data class StatsUiState(
    val weeklyPercentage: Int = 0,
    val dailyBars: List<DayBar> = emptyList(),
    val feedbackMessage: String = "",
    val streakDays: Int = 0
)

class StatsViewModel(
    private val repository: InMemoryMedicationRepository = RepositoryProvider.repository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            val weeklyPercentage = repository.calculateWeeklyAdherence()
            val dailyAdherence = repository.getDailyAdherenceForWeek()

            val dailyBars = dailyAdherence.entries
                .sortedBy { it.key }
                .map { (date, percentage) ->
                    DayBar(
                        dayLabel = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        percentage = percentage
                    )
                }

            val feedback = when {
                weeklyPercentage >= 90 -> "Excellent work! You're staying on track with your medications."
                weeklyPercentage >= 75 -> "Good job! Keep up the consistency."
                weeklyPercentage >= 50 -> "You're doing okay. Try to improve your consistency."
                else -> "Let's work on building a better routine together."
            }

            // Calculate streak: consecutive days with >= 80% adherence from today backwards
            val sortedDays = dailyAdherence.entries.sortedByDescending { it.key }
            var streak = 0
            for ((_, percentage) in sortedDays) {
                if (percentage >= 80) {
                    streak++
                } else {
                    break
                }
            }

            _uiState.value = StatsUiState(
                weeklyPercentage = weeklyPercentage,
                dailyBars = dailyBars,
                feedbackMessage = feedback,
                streakDays = streak
            )
        }
    }

    fun refresh() {
        loadStats()
    }
}
