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
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    } else {
        repository.medications
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
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

    private fun loadCaretakerData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                android.util.Log.d("CaretakerVM", "Loading data for patient PIN: ${patientPin ?: "current user"}")

                val today = LocalDate.now()
                val weekAgo = today.minusDays(6)
                val monthAgo = today.minusDays(29)

                // If monitoring a specific patient
                if (patientPin != null) {
                    // Get patient name
                    val patientData = repository.getPatientDataByPin(patientPin)
                    val patientName = patientData?.name ?: "Patient"

                    // Calculate adherence using patient-specific methods
                    val weeklyAdherence = repository.calculatePatientAdherence(patientPin, weekAgo, today)
                    val monthlyAdherence = repository.calculatePatientAdherence(patientPin, monthAgo, today)

                    // Get medications and dose events
                    val medications = repository.getMedicationsForPatientByPin(patientPin).first()
                    val doseEvents = repository.getDoseEventsForPatientByPin(patientPin, weekAgo, today)

                    // Get today's doses
                    val todayDoses = repository.getTodayDosesForPatientByPin(patientPin).map { (med, time, taken) ->
                        TodayDoseInfo(
                            medicationName = med.name,
                            dosage = med.dosage,
                            time = time,
                            taken = taken
                        )
                    }

                    // Calculate streak for patient
                    val currentStreak = calculateStreakForPatient(medications, doseEvents, today)

                    // Get recent missed doses
                    val missedDoses = calculateMissedDosesForPatient(medications, doseEvents, weekAgo, today)

                    // All medication adherence breakdown
                    val medicationBreakdown = medications.map { med ->
                        var expected = 0
                        var taken = 0
                        
                        var date = weekAgo
                        while (!date.isAfter(today)) {
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

                    // Problematic medications (< 70% adherence)
                    val problematicMeds = medicationBreakdown.filter { it.percentage < 70 }

                    // Adherence trend
                    val trend = calculateTrendForPatient(patientPin, weekAgo, today)

                    android.util.Log.d("CaretakerVM", "✅ Loaded patient data: $patientName, ${medications.size} meds, $weeklyAdherence% weekly adherence")

                    _uiState.value = CaretakerUiState(
                        patientName = patientName,
                        patientPin = patientPin,
                        medicationCount = medications.size,
                        weeklyAdherence = weeklyAdherence,
                        monthlyAdherence = monthlyAdherence,
                        currentStreak = currentStreak,
                        longestStreak = currentStreak, // TODO: Calculate longest streak
                        todayDoses = todayDoses,
                        recentMissedDoses = missedDoses,
                        problematicMedications = problematicMeds,
                        medicationBreakdown = medicationBreakdown,
                        adherenceTrend = trend,
                        lastUpdated = System.currentTimeMillis(),
                        isLoading = false
                    )
                } else {
                    // Use current user's data (original behavior)
                    val weeklyAdherence = repository.calculateWeeklyAdherence()
                    val monthlyAdherence = repository.calculateMonthlyAdherence()
                    val currentStreak = repository.calculateStreak()
                    val longestStreak = repository.calculateLongestStreak()
                    val missedDoses = repository.getRecentMissedDoses(7)
                    val medications = repository.medications.first()

                    // Get today's doses
                    val todayDoses = repository.getTodayDoses().map { (med, time, taken) ->
                        TodayDoseInfo(
                            medicationName = med.name,
                            dosage = med.dosage,
                            time = time,
                            taken = taken
                        )
                    }

                    // All medication adherence breakdown
                    val medicationBreakdown = medications.map { med ->
                        repository.calculateMedicationAdherence(
                            medId = med.id,
                            startDate = weekAgo,
                            endDate = today
                        )
                    }.sortedBy { it.percentage }

                    // Problematic medications (< 70% adherence)
                    val problematicMeds = medicationBreakdown.filter { it.percentage < 70 }

                    val trend = calculateTrend(repository)

                    _uiState.value = CaretakerUiState(
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
            } catch (e: Exception) {
                android.util.Log.e("CaretakerVM", "❌ Error loading caretaker data", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load patient data: ${e.message}"
                )
            }
        }
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
        
        return missed.take(10) // Return most recent 10
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
            
            if (percentage < 70) {
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
        val lastWeekStart = lastWeekEnd.minusDays(6)
        val lastWeekAdherence = repository.calculatePatientAdherence(pin, lastWeekStart, lastWeekEnd)

        return when {
            thisWeekAdherence > lastWeekAdherence + 10 -> "Improving"
            thisWeekAdherence < lastWeekAdherence - 10 -> "Declining"
            else -> "Stable"
        }
    }

    private suspend fun calculateTrend(repository: FirebaseMedicationRepository): String {
        val today = LocalDate.now()
        val thisWeekAdherence = repository.calculateWeeklyAdherence()

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
