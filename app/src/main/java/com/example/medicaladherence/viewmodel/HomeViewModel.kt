package com.example.medicaladherence.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicaladherence.data.model.Medication
import com.example.medicaladherence.data.repo.InMemoryMedicationRepository
import com.example.medicaladherence.data.repo.RepositoryProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class HomeUiState(
    val todayDate: LocalDate = LocalDate.now(),
    val nextDoseCountdown: String = "--:--",
    val nextDoseName: String = "",
    val nextDoseDosage: String = "",
    val nextDoseTime: String = "",
    val todayDoses: List<DoseItem> = emptyList(),
    val weeklyAdherencePercent: Int = 0,
    val streakDays: Int = 0,
    val snackbarMessage: String? = null
)

data class DoseItem(
    val medication: Medication,
    val time: String,
    val taken: Boolean?
)

class HomeViewModel(
    private val repository: InMemoryMedicationRepository = RepositoryProvider.repository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
        startCountdownTimer()
    }

    private fun loadData() {
        viewModelScope.launch {
            // Combine data from repository
            val doses = repository.todayDoses().map { (med, time, taken) ->
                DoseItem(medication = med, time = time, taken = taken)
            }

            _uiState.value = _uiState.value.copy(
                todayDoses = doses,
                weeklyAdherencePercent = repository.calculateWeeklyAdherence(),
                streakDays = repository.calculateStreak()
            )
        }
    }

    private fun startCountdownTimer() {
        viewModelScope.launch {
            while (true) {
                updateCountdown()
                delay(1000) // Update every second
            }
        }
    }

    private fun updateCountdown() {
        val now = LocalTime.now()
        val formatter = DateTimeFormatter.ofPattern("HH:mm")

        // Find the next upcoming dose
        val nextDose = _uiState.value.todayDoses
            .filter { it.taken != true } // Not yet taken
            .minByOrNull {
                LocalTime.parse(it.time, formatter)
                    .let { time -> if (time.isBefore(now)) time.plusHours(24) else time }
            }

        if (nextDose != null) {
            val doseTime = LocalTime.parse(nextDose.time, formatter)
            val minutesUntil = java.time.Duration.between(now, doseTime).toMinutes()
            val secondsUntil = java.time.Duration.between(now, doseTime).seconds % 60
            val countdown = String.format("%02d:%02d", minutesUntil, secondsUntil)

            _uiState.value = _uiState.value.copy(
                nextDoseCountdown = countdown,
                nextDoseName = nextDose.medication.name,
                nextDoseDosage = nextDose.medication.dosage,
                nextDoseTime = nextDose.time
            )
        } else {
            _uiState.value = _uiState.value.copy(
                nextDoseCountdown = "All done!",
                nextDoseName = "",
                nextDoseDosage = "",
                nextDoseTime = ""
            )
        }
    }

    fun markTaken(medId: String, time: String) {
        repository.markDose(medId, LocalDate.now(), time, taken = true)
        loadData()
        showSnackbar("Dose marked as taken")
    }

    fun markMissed(medId: String, time: String) {
        repository.markDose(medId, LocalDate.now(), time, taken = false)
        loadData()
        showSnackbar("Dose marked as missed")
    }

    fun snooze15(medId: String, time: String) {
        repository.snooze(medId, time, 15)
        showSnackbar("Snoozed for 15 minutes")
    }

    fun undoLastAction() {
        repository.undoLastAction()
        loadData()
        showSnackbar("Action undone")
    }

    fun deleteMedication(medId: String) {
        repository.deleteMedication(medId)
        loadData()
        showSnackbar("Medication deleted")
    }

    private fun showSnackbar(message: String) {
        _uiState.value = _uiState.value.copy(snackbarMessage = message)
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    fun refresh() {
        loadData()
    }
}
