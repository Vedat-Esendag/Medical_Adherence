package com.example.medicaladherence.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicaladherence.data.model.Medication
import com.example.medicaladherence.data.repo.InMemoryMedicationRepository
import com.example.medicaladherence.data.repo.RepositoryProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class MedicationsLibraryViewModel(
    private val repository: InMemoryMedicationRepository = RepositoryProvider.repository
) : ViewModel() {

    val medications: StateFlow<List<Medication>> = repository.medications
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteMedication(medId: String) {
        repository.deleteMedication(medId)
    }
}
