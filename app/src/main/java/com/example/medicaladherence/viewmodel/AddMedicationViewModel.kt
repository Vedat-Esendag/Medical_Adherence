package com.example.medicaladherence.viewmodel

import androidx.lifecycle.ViewModel
import com.example.medicaladherence.data.model.Medication
import com.example.medicaladherence.data.model.MedicationFrequency
import com.example.medicaladherence.data.repo.InMemoryMedicationRepository
import com.example.medicaladherence.data.repo.RepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class AddMedicationUiState(
    val name: String = "",
    val dosage: String = "",
    val times: List<String> = emptyList(),
    val notes: String = "",
    val frequency: MedicationFrequency = MedicationFrequency.Daily,
    val specificDays: List<Int> = emptyList(),
    val nameError: String? = null,
    val dosageError: String? = null,
    val timesError: String? = null,
    val isValid: Boolean = false
)

class AddMedicationViewModel(
    private val repository: InMemoryMedicationRepository = RepositoryProvider.repository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddMedicationUiState())
    val uiState: StateFlow<AddMedicationUiState> = _uiState.asStateFlow()

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

    private fun validate() {
        val state = _uiState.value
        var nameError: String? = null
        var dosageError: String? = null
        var timesError: String? = null

        if (state.name.isBlank()) {
            nameError = "Name is required"
        }

        if (state.dosage.isBlank()) {
            dosageError = "Dosage is required"
        }

        if (state.times.isEmpty()) {
            timesError = "At least one time is required"
        }

        val isValid = nameError == null && dosageError == null && timesError == null

        _uiState.value = state.copy(
            nameError = nameError,
            dosageError = dosageError,
            timesError = timesError,
            isValid = isValid
        )
    }

    fun loadMedication(medId: String) {
        val medication = repository.getMedicationById(medId)
        if (medication != null) {
            _uiState.value = _uiState.value.copy(
                name = medication.name,
                dosage = medication.dosage,
                times = medication.times,
                notes = medication.notes ?: "",
                frequency = medication.frequency,
                specificDays = medication.specificDays
            )
            validate()
        }
    }

    fun save(): Boolean {
        validate()
        val state = _uiState.value

        // Debug logging
        println("DEBUG Save - Name: '${state.name}', Dosage: '${state.dosage}', Times: ${state.times.size}, Valid: ${state.isValid}")

        if (!state.isValid) {
            println("DEBUG Save - Validation failed: nameError=${state.nameError}, dosageError=${state.dosageError}, timesError=${state.timesError}")
            return false
        }

        val medication = Medication(
            id = UUID.randomUUID().toString(),
            name = state.name,
            dosage = state.dosage,
            times = state.times,
            notes = state.notes.ifBlank { null },
            frequency = state.frequency,
            specificDays = state.specificDays
        )

        repository.addOrUpdateMedication(medication)
        println("DEBUG Save - Medication saved: ${medication.id}")
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
            specificDays = state.specificDays
        )

        repository.addOrUpdateMedication(medication)
        return true
    }
}
