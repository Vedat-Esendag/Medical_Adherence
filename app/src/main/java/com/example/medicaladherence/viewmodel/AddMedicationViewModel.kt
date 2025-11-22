package com.example.medicaladherence.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicaladherence.data.model.Medication
import com.example.medicaladherence.data.model.MedicationFrequency
import com.example.medicaladherence.data.repo.RepositoryProvider
import com.example.medicaladherence.data.repository.FirebaseMedicationRepository
import com.example.medicaladherence.notification.NotificationScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

data class AddMedicationUiState(
    val name: String = "",
    val dosage: String = "",
    val times: List<String> = emptyList(),
    val notes: String = "",
    val frequency: MedicationFrequency = MedicationFrequency.Daily,
    val specificDays: List<Int> = emptyList(),
    val intervalDays: Int = 1,
    val startDate: LocalDate = LocalDate.now(),
    val nameError: String? = null,
    val dosageError: String? = null,
    val timesError: String? = null,
    val frequencyError: String? = null,
    val isValid: Boolean = false,
    val savedSuccessfully: Boolean = false
)

class AddMedicationViewModel(
    private val repository: FirebaseMedicationRepository = RepositoryProvider.getRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddMedicationUiState())
    val uiState: StateFlow<AddMedicationUiState> = _uiState.asStateFlow()

    private lateinit var notificationScheduler: NotificationScheduler

    fun initialize(context: Context) {
        notificationScheduler = NotificationScheduler(context)
    }

    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(name = name)
        validate()
    }

    fun updateDosage(dosage: String) {
        _uiState.value = _uiState.value.copy(dosage = dosage)
        validate()
    }

    fun addTime(time: String) {
        val currentTimes = _uiState.value.times.toMutableList()
        if (!currentTimes.contains(time)) {
            currentTimes.add(time)
            currentTimes.sort()
            _uiState.value = _uiState.value.copy(times = currentTimes)
            validate()
        }
    }

    fun removeTime(time: String) {
        _uiState.value = _uiState.value.copy(
            times = _uiState.value.times.filter { it != time }
        )
        validate()
    }

    fun updateNotes(notes: String) {
        _uiState.value = _uiState.value.copy(notes = notes)
    }

    fun updateFrequency(frequency: MedicationFrequency) {
        _uiState.value = _uiState.value.copy(frequency = frequency)
        validate()
    }

    fun toggleDay(dayOfWeek: Int) {
        val currentDays = _uiState.value.specificDays.toMutableList()
        if (currentDays.contains(dayOfWeek)) {
            currentDays.remove(dayOfWeek)
        } else {
            currentDays.add(dayOfWeek)
            currentDays.sort()
        }
        _uiState.value = _uiState.value.copy(specificDays = currentDays)
        validate()
    }

    fun updateIntervalDays(days: Int) {
        _uiState.value = _uiState.value.copy(intervalDays = days.coerceIn(1, 365))
        validate()
    }

    fun updateStartDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(startDate = date)
        validate()
    }

    private fun validate() {
        val state = _uiState.value
        var nameError: String? = null
        var dosageError: String? = null
        var timesError: String? = null
        var frequencyError: String? = null

        if (state.name.isBlank()) {
            nameError = "Name is required"
        }

        if (state.dosage.isBlank()) {
            dosageError = "Dosage is required"
        }

        // Times are optional only for AsNeeded medications
        if (state.frequency != MedicationFrequency.AsNeeded && state.times.isEmpty()) {
            timesError = "At least one time is required"
        }

        // Frequency-specific validation
        when (state.frequency) {
            MedicationFrequency.SpecificDays, MedicationFrequency.Weekly -> {
                if (state.specificDays.isEmpty()) {
                    frequencyError = "Please select at least one day"
                }
            }
            MedicationFrequency.EveryXDays -> {
                if (state.intervalDays < 1 || state.intervalDays > 365) {
                    frequencyError = "Interval must be between 1 and 365 days"
                }
            }
            else -> { /* No additional validation needed */ }
        }

        val isValid = nameError == null && dosageError == null && timesError == null && frequencyError == null

        _uiState.value = state.copy(
            nameError = nameError,
            dosageError = dosageError,
            timesError = timesError,
            frequencyError = frequencyError,
            isValid = isValid
        )
    }

    fun loadMedication(medId: String) {
        viewModelScope.launch {
            val medication = repository.getMedicationById(medId)
            if (medication != null) {
                _uiState.value = _uiState.value.copy(
                    name = medication.name,
                    dosage = medication.dosage,
                    times = medication.times,
                    notes = medication.notes ?: "",
                    frequency = medication.frequency,
                    specificDays = medication.specificDays,
                    intervalDays = medication.intervalDays ?: 1,
                    startDate = medication.startDate?.let { LocalDate.parse(it) } ?: LocalDate.now()
                )
                validate()
            }
        }
    }

    fun save(): Boolean {
        validate()
        val state = _uiState.value

        if (!state.isValid) {
            return false
        }

        val medication = Medication(
            id = UUID.randomUUID().toString(),
            name = state.name,
            dosage = state.dosage,
            times = state.times,
            notes = state.notes.ifBlank { null },
            frequency = state.frequency,
            specificDays = state.specificDays,
            intervalDays = if (state.frequency == MedicationFrequency.EveryXDays) state.intervalDays else null,
            startDate = if (state.frequency == MedicationFrequency.EveryXDays) state.startDate.toString() else null
        )

        viewModelScope.launch {
            repository.addOrUpdateMedication(medication)

            // Schedule notifications
            if (::notificationScheduler.isInitialized) {
                notificationScheduler.scheduleMedicationNotifications(medication)
            }

            _uiState.value = _uiState.value.copy(savedSuccessfully = true)
        }
        return true
    }

    fun reset() {
        _uiState.value = AddMedicationUiState()
    }

    fun saveWithId(medId: String): Boolean {
        validate()
        if (!_uiState.value.isValid) return false

        val state = _uiState.value
        val medication = Medication(
            id = medId,
            name = state.name,
            dosage = state.dosage,
            times = state.times,
            notes = state.notes.ifBlank { null },
            frequency = state.frequency,
            specificDays = state.specificDays,
            intervalDays = if (state.frequency == MedicationFrequency.EveryXDays) state.intervalDays else null,
            startDate = if (state.frequency == MedicationFrequency.EveryXDays) state.startDate.toString() else null
        )

        viewModelScope.launch {
            repository.addOrUpdateMedication(medication)

            // Schedule notifications
            if (::notificationScheduler.isInitialized) {
                notificationScheduler.scheduleMedicationNotifications(medication)
            }

            _uiState.value = _uiState.value.copy(savedSuccessfully = true)
        }
        return true
    }
}
