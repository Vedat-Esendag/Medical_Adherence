package com.example.medicaladherence.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicaladherence.data.model.Medication
import com.example.medicaladherence.data.repo.RepositoryProvider
import com.example.medicaladherence.data.repository.MedicationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MedicationsLibraryViewModel(
    private val repository: MedicationRepository = RepositoryProvider.getRepository()
) : ViewModel() {

    val medications: StateFlow<List<Medication>> = repository.medications
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteMedication(medId: String) {
        viewModelScope.launch {
            repository.deleteMedication(medId)
        }
    }
}
