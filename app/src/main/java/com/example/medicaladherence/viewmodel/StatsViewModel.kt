package com.example.medicaladherence.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicaladherence.data.repository.RepositoryProvider
import com.example.medicaladherence.data.repository.FirebaseMedicationRepository
import com.example.medicaladherence.utils.AppConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

data class DayBar(
    val dayLabel: String,
    val percentage: Int
)

data class MedicationAdherence(
    val medicationName: String,
    val dosage: String,
    val percentage: Int,
    val takenCount: Int,
    val totalCount: Int
)

data class StatsUiState(
    val weeklyPercentage: Int = 0,
    val dailyBars: List<DayBar> = emptyList(),
    val feedbackMessage: String = "",
    val streakDays: Int = 0,
    val bestDay: String? = null,
    val worstDay: String? = null,
    val medicationBreakdown: List<MedicationAdherence> = emptyList(),
    val timeOfDayInsight: String? = null
)

class StatsViewModel(
    private val repository: FirebaseMedicationRepository = RepositoryProvider.getRepository()
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
                weeklyPercentage >= AppConstants.ADHERENCE_EXCELLENT -> "Excellent work! You're staying on track with your medications."
                weeklyPercentage >= AppConstants.ADHERENCE_FAIR -> "Good job! Keep up the consistency."
                weeklyPercentage >= AppConstants.ADHERENCE_OKAY -> "You're doing okay. Try to improve your consistency."
                else -> "Let's work on building a better routine together."
            }

            // Calculate streak: consecutive days with >= good adherence from today backwards
            val sortedDays = dailyAdherence.entries.sortedByDescending { it.key }
            var streak = 0
            for ((_, percentage) in sortedDays) {
                if (percentage >= AppConstants.ADHERENCE_GOOD) {
                    streak++
                } else {
                    break
                }
            }

            // Calculate best and worst days
            val bestDayEntry = dailyAdherence.maxByOrNull { it.value }
            val worstDayEntry = dailyAdherence.minByOrNull { it.value }

            val bestDay = bestDayEntry?.let { entry ->
                val dayName = entry.key.dayOfWeek.getDisplayName(
                    TextStyle.FULL,
                    Locale.getDefault()
                )
                "$dayName (${entry.value}%)"
            }

            val worstDay = worstDayEntry?.let { entry ->
                if (entry.value < 100) {
                    val dayName = entry.key.dayOfWeek.getDisplayName(
                        TextStyle.FULL,
                        Locale.getDefault()
                    )
                    "$dayName (${entry.value}%)"
                } else null
            }

            // Calculate per-medication breakdown
            val medications = repository.medications.first()
            val medicationBreakdown = mutableListOf<MedicationAdherence>()

            val today = LocalDate.now()
            val weekAgo = today.minusDays((AppConstants.DAYS_IN_WEEK - 1).toLong())

            medications.forEach { med ->
                val adherence = repository.calculateMedicationAdherence(
                    medId = med.id,
                    startDate = weekAgo,
                    endDate = today
                )
                medicationBreakdown.add(adherence)
            }

            // Calculate time of day insight
            val timeInsight = repository.calculateTimeOfDayInsight(weekAgo, today)

            _uiState.value = StatsUiState(
                weeklyPercentage = weeklyPercentage,
                dailyBars = dailyBars,
                feedbackMessage = feedback,
                streakDays = streak,
                bestDay = bestDay,
                worstDay = worstDay,
                medicationBreakdown = medicationBreakdown.sortedByDescending { it.percentage },
                timeOfDayInsight = timeInsight
            )
        }
    }

    fun refresh() {
        loadStats()
    }
}
