package com.example.medicaladherence.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicaladherence.data.model.Medication
import com.example.medicaladherence.data.repo.RepositoryProvider
import com.example.medicaladherence.data.repository.MedicationRepository
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
    val snackbarMessage: String? = null,
    val isInDoseWindow: Boolean = false,
    val nextDoseMedicationId: String = "",
    val lastMarkedDose: Pair<String, String>? = null // medId to time
)

data class DoseItem(
    val medication: Medication,
    val time: String,
    val taken: Boolean?
)

class HomeViewModel(
    private val repository: MedicationRepository = RepositoryProvider.getRepository()
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
            val doses = repository.getTodayDoses().map { (med, time, taken) ->
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

        // Find next FUTURE dose (not taken and time is after now)
        val nextDose = _uiState.value.todayDoses
            .filter { it.taken != true } // Not marked as taken
            .filter {
                val doseTime = LocalTime.parse(it.time, formatter)
                doseTime.isAfter(now) // ONLY future doses
            }
            .minByOrNull {
                LocalTime.parse(it.time, formatter)
            }

        if (nextDose != null) {
            val doseTime = LocalTime.parse(nextDose.time, formatter)
            val duration = java.time.Duration.between(now, doseTime)
            val hours = duration.toHours()
            val minutes = duration.toMinutes() % 60

            val countdown = if (hours > 0) {
                String.format("%d:%02d", hours, minutes)
            } else {
                String.format("%02d:%02d", 0, minutes)
            }

            // Check if within 30-minute window
            val totalMinutes = duration.toMinutes()
            val isInWindow = totalMinutes <= 30 && totalMinutes >= -30

            _uiState.value = _uiState.value.copy(
                nextDoseCountdown = countdown,
                nextDoseName = nextDose.medication.name,
                nextDoseDosage = nextDose.medication.dosage,
                nextDoseTime = nextDose.time,
                isInDoseWindow = isInWindow,
                nextDoseMedicationId = nextDose.medication.id
            )
        } else {
            // No future doses today
            _uiState.value = _uiState.value.copy(
                nextDoseCountdown = "All done!",
                nextDoseName = "",
                nextDoseDosage = "",
                nextDoseTime = "",
                isInDoseWindow = false,
                nextDoseMedicationId = ""
            )
        }
    }

    fun markTaken(medId: String, time: String) {
        viewModelScope.launch {
            repository.markDose(medId, LocalDate.now(), time, taken = true)
            _uiState.value = _uiState.value.copy(lastMarkedDose = medId to time)
            loadData()
            showSnackbar("Dose marked as taken")
        }
    }

    fun markMissed(medId: String, time: String) {
        viewModelScope.launch {
            repository.markDose(medId, LocalDate.now(), time, taken = false)
            _uiState.value = _uiState.value.copy(lastMarkedDose = medId to time)
            loadData()
            showSnackbar("Dose marked as missed")
        }
    }

    fun snooze15(medId: String, time: String) {
        repository.snooze(medId, time, 15)
        showSnackbar("Snoozed for 15 minutes")
    }

    fun undoDose(medId: String, time: String) {
        viewModelScope.launch {
            repository.undoDose(medId, LocalDate.now(), time)
            _uiState.value = _uiState.value.copy(lastMarkedDose = null)
            loadData()
            showSnackbar("Action undone")
        }
    }

    fun undoLastMarkedDose() {
        val lastMarked = _uiState.value.lastMarkedDose ?: return
        undoDose(lastMarked.first, lastMarked.second)
    }

    fun deleteMedication(medId: String) {
        viewModelScope.launch {
            repository.deleteMedication(medId)
            loadData()
            showSnackbar("Medication deleted")
        }
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
