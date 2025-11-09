package com.example.medicaladherence.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.medicaladherence.data.repository.FirebaseMedicationRepository
import com.example.medicaladherence.utils.AppConstants
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

data class CaretakerUiState(
    val patientName: String = "Patient",
    val patientPin: String = "",
    val medicationCount: Int = 0,
    val weeklyAdherence: Int = 0,
    val monthlyAdherence: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val todayDoses: List<TodayDoseInfo> = emptyList(),
    val recentMissedDoses: List<MissedDoseInfo> = emptyList(),
    val problematicMedications: List<MedicationAdherence> = emptyList(),
    val medicationBreakdown: List<MedicationAdherence> = emptyList(),
    val adherenceTrend: String = "Stable", // "Improving", "Declining", "Stable"
    val lastUpdated: Long = 0L, // Timestamp in milliseconds
    val isLoading: Boolean = true,
    val error: String? = null
)

data class MissedDoseInfo(
    val medicationName: String,
    val dosage: String,
    val date: LocalDate,
    val time: String
)

data class TodayDoseInfo(
    val medicationName: String,
    val dosage: String,
    val time: String,
    val taken: Boolean?  // null = pending, true = taken, false = missed
)

/**
 * ViewModel for monitoring medication adherence and patient health metrics.
 * 
 * Supports two modes:
 * - **Current User Mode**: When `patientPin` is null, displays data for the logged-in user
 * - **Caregiver Mode**: When `patientPin` is provided, displays data for a specific patient
 * 
 * Key Features:
 * - Real-time medication tracking
 * - Weekly and monthly adherence calculations
 * - Streak tracking (current and longest)
 * - Problematic medication identification
 * - Adherence trend analysis (Improving/Declining/Stable)
 * 
 * @param repository The medication data repository
 * @param patientPin Optional PIN for monitoring a specific patient (caregiver mode)
 */
class CaretakerViewModel(
    private val repository: FirebaseMedicationRepository,
    private val patientPin: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(CaretakerUiState())
    val uiState: StateFlow<CaretakerUiState> = _uiState.asStateFlow()

    // Real-time medications flow
    val medications: StateFlow<List<com.example.medicaladherence.data.model.Medication>> = if (patientPin != null) {
        repository.getMedicationsForPatientByPin(patientPin)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(AppConstants.STATEFLOW_TIMEOUT_MS),
                initialValue = emptyList()
            )
    } else {
        repository.medications
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(AppConstants.STATEFLOW_TIMEOUT_MS),
                initialValue = emptyList()
            )
    }

    init {
        // Set up real-time medication count observer
        viewModelScope.launch {
            medications.collect { meds ->
                _uiState.value = _uiState.value.copy(medicationCount = meds.size)
            }
        }
        
        loadCaretakerData()
    }

    /**
     * Loads all caretaker/monitoring data and updates the UI state.
     * Automatically selects between patient-specific or current user data loading.
     */
    private fun loadCaretakerData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                val today = LocalDate.now()
                val weekAgo = today.minusDays((AppConstants.DAYS_IN_WEEK - 1).toLong())
                val monthAgo = today.minusDays((AppConstants.DAYS_IN_MONTH - 1).toLong())

                val uiState = if (patientPin != null) {
                    loadPatientData(patientPin, today, weekAgo, monthAgo)
                } else {
                    loadCurrentUserData(today, weekAgo, monthAgo)
                }

                _uiState.value = uiState
            } catch (e: Exception) {
                android.util.Log.e("CaretakerVM", "❌ Error loading caretaker data", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load patient data: ${e.message}"
                )
            }
        }
    }

    /**
     * Loads comprehensive adherence data for a specific patient (caregiver mode).
     * 
     * @param pin The patient's unique PIN
     * @param today Current date for calculations
     * @param weekAgo Start of the weekly tracking period
     * @param monthAgo Start of the monthly tracking period
     * @return Complete UI state with all patient metrics
     */
    private suspend fun loadPatientData(
        pin: String,
        today: LocalDate,
        weekAgo: LocalDate,
        monthAgo: LocalDate
    ): CaretakerUiState {
        val patientData = repository.getPatientDataByPin(pin)
        val patientName = patientData?.name ?: "Patient"

        val weeklyAdherence = repository.calculatePatientAdherence(pin, weekAgo, today)
        val monthlyAdherence = repository.calculatePatientAdherence(pin, monthAgo, today)

        val medications = repository.getMedicationsForPatientByPin(pin).first()
        val doseEvents = repository.getDoseEventsForPatientByPin(pin, weekAgo, today)

        val todayDoses = repository.getTodayDosesForPatientByPin(pin).map { (med, time, taken) ->
            TodayDoseInfo(
                medicationName = med.name,
                dosage = med.dosage,
                time = time,
                taken = taken
            )
        }

        val currentStreak = calculateStreakForPatient(medications, doseEvents, today)
        val missedDoses = calculateMissedDosesForPatient(medications, doseEvents, weekAgo, today)
        val medicationBreakdown = calculateMedicationStats(medications, doseEvents, weekAgo, today)
        val problematicMeds = medicationBreakdown.filter { it.percentage < AppConstants.ADHERENCE_PROBLEMATIC }
        val trend = calculateTrendForPatient(pin, weekAgo, today)

        return CaretakerUiState(
            patientName = patientName,
            patientPin = pin,
            medicationCount = medications.size,
            weeklyAdherence = weeklyAdherence,
            monthlyAdherence = monthlyAdherence,
            currentStreak = currentStreak,
            longestStreak = currentStreak,
            todayDoses = todayDoses,
            recentMissedDoses = missedDoses,
            problematicMedications = problematicMeds,
            medicationBreakdown = medicationBreakdown,
            adherenceTrend = trend,
            lastUpdated = System.currentTimeMillis(),
            isLoading = false
        )
    }

    /**
     * Loads comprehensive adherence data for the currently logged-in user.
     * 
     * @param today Current date for calculations
     * @param weekAgo Start of the weekly tracking period
     * @param monthAgo Start of the monthly tracking period
     * @return Complete UI state with all user metrics
     */
    private suspend fun loadCurrentUserData(
        today: LocalDate,
        weekAgo: LocalDate,
        monthAgo: LocalDate
    ): CaretakerUiState {
        val weeklyAdherence = repository.calculateWeeklyAdherence()
        val monthlyAdherence = repository.calculateMonthlyAdherence()
        val currentStreak = repository.calculateStreak()
        val longestStreak = repository.calculateLongestStreak()
        val missedDoses = repository.getRecentMissedDoses(AppConstants.DAYS_IN_WEEK)
        val medications = repository.medications.first()

        val todayDoses = repository.getTodayDoses().map { (med, time, taken) ->
            TodayDoseInfo(
                medicationName = med.name,
                dosage = med.dosage,
                time = time,
                taken = taken
            )
        }

        val medicationBreakdown = medications.map { med ->
            repository.calculateMedicationAdherence(
                medId = med.id,
                startDate = weekAgo,
                endDate = today
            )
        }.sortedBy { it.percentage }

        val problematicMeds = medicationBreakdown.filter { it.percentage < AppConstants.ADHERENCE_PROBLEMATIC }
        val trend = calculateTrend(repository)

        return CaretakerUiState(
            medicationCount = medications.size,
            weeklyAdherence = weeklyAdherence,
            monthlyAdherence = monthlyAdherence,
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            todayDoses = todayDoses,
            recentMissedDoses = missedDoses,
            problematicMedications = problematicMeds,
            medicationBreakdown = medicationBreakdown,
            adherenceTrend = trend,
            lastUpdated = System.currentTimeMillis(),
            isLoading = false
        )
    }

    /**
     * Calculates adherence statistics for all medications over a date range.
     * 
     * For each medication, computes:
     * - Expected doses (based on scheduled times and date range)
     * - Actual taken doses
     * - Adherence percentage
     * 
     * @param medications List of medications to analyze
     * @param doseEvents Historical dose events (taken/missed records)
     * @param startDate Start of analysis period (inclusive)
     * @param endDate End of analysis period (inclusive)
     * @return List of medication adherence stats, sorted by percentage (lowest first)
     */
    private fun calculateMedicationStats(
        medications: List<com.example.medicaladherence.data.model.Medication>,
        doseEvents: List<com.example.medicaladherence.data.model.DoseEvent>,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<MedicationAdherence> {
        return medications.map { med ->
            var expected = 0
            var taken = 0

            var date = startDate
            while (!date.isAfter(endDate)) {
                expected += med.times.size
                taken += med.times.count { time ->
                    doseEvents.any {
                        it.medId == med.id && it.date == date && it.time == time && it.taken
                    }
                }
                date = date.plusDays(1)
            }

            val percentage = if (expected > 0) (taken * 100) / expected else 0

            MedicationAdherence(
                medicationName = med.name,
                dosage = med.dosage,
                percentage = percentage,
                takenCount = taken,
                totalCount = expected
            )
        }.sortedBy { it.percentage }
    }

    private fun calculateStreakForPatient(
        medications: List<com.example.medicaladherence.data.model.Medication>,
        doseEvents: List<com.example.medicaladherence.data.model.DoseEvent>,
        today: LocalDate
    ): Int {
        var streak = 0
        var date = today
        
        while (true) {
            val expectedDoses = medications.sumOf { med ->
                med.times.size
            }
            
            if (expectedDoses == 0) {
                break
            }
            
            val takenDoses = doseEvents.count { 
                it.date == date && it.taken 
            }
            
            if (takenDoses >= expectedDoses) {
                streak++
                date = date.minusDays(1)
            } else {
                break
            }
        }
        
        return streak
    }

    private fun calculateMissedDosesForPatient(
        medications: List<com.example.medicaladherence.data.model.Medication>,
        doseEvents: List<com.example.medicaladherence.data.model.DoseEvent>,
        start: LocalDate,
        end: LocalDate
    ): List<MissedDoseInfo> {
        val missed = mutableListOf<MissedDoseInfo>()
        
        var date = start
        while (!date.isAfter(end)) {
            medications.forEach { med ->
                med.times.forEach { time ->
                    val event = doseEvents.find { 
                        it.medId == med.id && it.date == date && it.time == time 
                    }
                    if (event == null || !event.taken) {
                        missed.add(MissedDoseInfo(
                            medicationName = med.name,
                            dosage = med.dosage,
                            date = date,
                            time = time
                        ))
                    }
                }
            }
            date = date.plusDays(1)
        }
        
        return missed.take(AppConstants.RECENT_MISSED_DOSES_LIMIT)
    }

    private fun calculateProblematicMedsForPatient(
        medications: List<com.example.medicaladherence.data.model.Medication>,
        doseEvents: List<com.example.medicaladherence.data.model.DoseEvent>,
        start: LocalDate,
        end: LocalDate
    ): List<MedicationAdherence> {
        return medications.mapNotNull { med ->
            var expected = 0
            var taken = 0
            
            var date = start
            while (!date.isAfter(end)) {
                expected += med.times.size
                taken += med.times.count { time ->
                    doseEvents.any { 
                        it.medId == med.id && it.date == date && it.time == time && it.taken 
                    }
                }
                date = date.plusDays(1)
            }
            
            val percentage = if (expected > 0) (taken * 100) / expected else 0
            
            if (percentage < AppConstants.ADHERENCE_PROBLEMATIC) {
                MedicationAdherence(
                    medicationName = med.name,
                    dosage = med.dosage,
                    percentage = percentage,
                    takenCount = taken,
                    totalCount = expected
                )
            } else {
                null
            }
        }.sortedBy { it.percentage }
    }

    private suspend fun calculateTrendForPatient(pin: String, currentWeekStart: LocalDate, currentWeekEnd: LocalDate): String {
        val thisWeekAdherence = repository.calculatePatientAdherence(pin, currentWeekStart, currentWeekEnd)
        
        val lastWeekEnd = currentWeekStart.minusDays(1)
        val lastWeekStart = lastWeekEnd.minusDays((AppConstants.DAYS_IN_WEEK - 1).toLong())
        val lastWeekAdherence = repository.calculatePatientAdherence(pin, lastWeekStart, lastWeekEnd)

        return when {
            thisWeekAdherence > lastWeekAdherence + AppConstants.TREND_THRESHOLD_PERCENT -> "Improving"
            thisWeekAdherence < lastWeekAdherence - AppConstants.TREND_THRESHOLD_PERCENT -> "Declining"
            else -> "Stable"
        }
    }

    private suspend fun calculateTrend(repository: FirebaseMedicationRepository): String {
        val today = LocalDate.now()
        val thisWeekAdherence = repository.calculateWeeklyAdherence()

        val twoWeeksAgo = today.minusDays((AppConstants.DAYS_IN_WEEK * 2 - 1).toLong())
        val oneWeekAgo = today.minusDays(AppConstants.DAYS_IN_WEEK.toLong())

        val lastWeekAdherence = repository.calculateAdherenceForPeriod(twoWeeksAgo, oneWeekAgo)

        return when {
            thisWeekAdherence > lastWeekAdherence + AppConstants.TREND_THRESHOLD_PERCENT -> "Improving"
            thisWeekAdherence < lastWeekAdherence - AppConstants.TREND_THRESHOLD_PERCENT -> "Declining"
            else -> "Stable"
        }
    }

    /**
     * Refreshes all caretaker data from the repository.
     * Useful for pull-to-refresh or manual data updates.
     */
    fun refresh() {
        loadCaretakerData()
    }
}

class CaretakerViewModelFactory(
    private val repository: FirebaseMedicationRepository,
    private val patientPin: String? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CaretakerViewModel::class.java)) {
            return CaretakerViewModel(repository, patientPin) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
