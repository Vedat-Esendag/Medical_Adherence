package com.example.medicaladherence.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.medicaladherence.data.repository.FirebaseMedicationRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

data class CaretakerUiState(
    val patientName: String = "Patient",
    val weeklyAdherence: Int = 0,
    val monthlyAdherence: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val recentMissedDoses: List<MissedDoseInfo> = emptyList(),
    val problematicMedications: List<MedicationAdherence> = emptyList(),
    val adherenceTrend: String = "Stable", // "Improving", "Declining", "Stable"
    val lastUpdated: String = ""
)

data class MissedDoseInfo(
    val medicationName: String,
    val dosage: String,
    val date: LocalDate,
    val time: String
)

class CaretakerViewModel(
    private val repository: FirebaseMedicationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CaretakerUiState())
    val uiState: StateFlow<CaretakerUiState> = _uiState.asStateFlow()

    init {
        loadCaretakerData()
    }

    private fun loadCaretakerData() {
        viewModelScope.launch {
            try {
                // Weekly adherence
                val weeklyAdherence = repository.calculateWeeklyAdherence()

                // Monthly adherence
                val monthlyAdherence = repository.calculateMonthlyAdherence()

                // Streak
                val currentStreak = repository.calculateStreak()
                val longestStreak = repository.calculateLongestStreak()

                // Recent missed doses
                val missedDoses = repository.getRecentMissedDoses(7) // Last 7 days

                // Problematic medications (< 70% adherence)
                val medications = repository.medications.first()
                val today = LocalDate.now()
                val weekAgo = today.minusDays(6)

                val problematicMeds = medications.mapNotNull { med ->
                    val adherence = repository.calculateMedicationAdherence(
                        medId = med.id,
                        startDate = weekAgo,
                        endDate = today
                    )
                    if (adherence.percentage < 70) adherence else null
                }.sortedBy { it.percentage }

                // Adherence trend
                val trend = calculateTrend(repository)

                // Last updated time
                val lastUpdated = java.time.LocalTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("HH:mm")
                )

                _uiState.value = CaretakerUiState(
                    weeklyAdherence = weeklyAdherence,
                    monthlyAdherence = monthlyAdherence,
                    currentStreak = currentStreak,
                    longestStreak = longestStreak,
                    recentMissedDoses = missedDoses,
                    problematicMedications = problematicMeds,
                    adherenceTrend = trend,
                    lastUpdated = lastUpdated
                )
            } catch (e: Exception) {
                // Handle error
                _uiState.value = CaretakerUiState()
            }
        }
    }

    private suspend fun calculateTrend(repository: FirebaseMedicationRepository): String {
        val today = LocalDate.now()
        val thisWeekAdherence = repository.calculateWeeklyAdherence()

        // Calculate last week's adherence
        val twoWeeksAgo = today.minusDays(13)
        val oneWeekAgo = today.minusDays(7)

        val lastWeekAdherence = repository.calculateAdherenceForPeriod(twoWeeksAgo, oneWeekAgo)

        return when {
            thisWeekAdherence > lastWeekAdherence + 10 -> "Improving"
            thisWeekAdherence < lastWeekAdherence - 10 -> "Declining"
            else -> "Stable"
        }
    }

    fun refresh() {
        loadCaretakerData()
    }
}

class CaretakerViewModelFactory(
    private val repository: FirebaseMedicationRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CaretakerViewModel::class.java)) {
            return CaretakerViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
