package com.example.medicaladherence.viewmodel

import androidx.lifecycle.ViewModel
import com.example.medicaladherence.data.model.Medication
import com.example.medicaladherence.data.repo.InMemoryMedicationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class AddMedicationUiState(
    val name: String = "",
    val dosage: String = "",
    val timesCsv: String = "",
    val notes: String = "",
    val nameError: String? = null,
    val dosageError: String? = null,
    val timesError: String? = null,
    val isValid: Boolean = false
)

class AddMedicationViewModel(
    private val repository: InMemoryMedicationRepository = InMemoryMedicationRepository()
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

    fun updateTimes(times: String) {
        _uiState.value = _uiState.value.copy(timesCsv = times)
        validate()
    }

    fun updateNotes(notes: String) {
        _uiState.value = _uiState.value.copy(notes = notes)
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

        if (state.timesCsv.isBlank()) {
            timesError = "At least one time is required"
        } else {
            // Validate time format (simple check for HH:mm pattern)
            val times = state.timesCsv.split(",").map { it.trim() }
            val timePattern = Regex("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")
            if (times.any { !timePattern.matches(it) }) {
                timesError = "Times must be in HH:mm format (e.g., 08:00, 14:30)"
            }
        }

        val isValid = nameError == null && dosageError == null && timesError == null

        _uiState.value = state.copy(
            nameError = nameError,
            dosageError = dosageError,
            timesError = timesError,
            isValid = isValid
        )
    }

    fun save(): Boolean {
        validate()
        if (!_uiState.value.isValid) return false

        val state = _uiState.value
        val times = state.timesCsv.split(",").map { it.trim() }

        val medication = Medication(
            id = UUID.randomUUID().toString(),
            name = state.name,
            dosage = state.dosage,
            times = times,
            notes = state.notes.ifBlank { null }
        )

        repository.addOrUpdateMedication(medication)
        return true
    }
}
