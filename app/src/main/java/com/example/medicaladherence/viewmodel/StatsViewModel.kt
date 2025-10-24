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
    val feedbackMessage: String = ""
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

            _uiState.value = StatsUiState(
                weeklyPercentage = weeklyPercentage,
                dailyBars = dailyBars,
                feedbackMessage = feedback
            )
        }
    }

    fun refresh() {
        loadStats()
    }
}
