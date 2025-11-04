package com.example.medicaladherence.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.medicaladherence.data.model.PatientProfile
import com.example.medicaladherence.data.repository.FirebaseMedicationRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CaregiverSettingsUiState(
    val patients: List<PatientProfile> = emptyList(),
    // Notification settings
    val alertThreshold: Int = 70,
    val dailySummaryEnabled: Boolean = false,
    val dailySummaryTime: String? = null
)

class CaregiverSettingsViewModel(
    private val repository: FirebaseMedicationRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CaregiverSettingsUiState())
    val uiState: StateFlow<CaregiverSettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadPatients()
        loadSettings()
    }
    
    private fun loadPatients() {
        viewModelScope.launch {
            repository.getCaregiverPatients().collect { patients ->
                _uiState.update { it.copy(patients = patients) }
            }
        }
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            repository.getSettings().collect { settings ->
                if (settings != null) {
                    _uiState.update { 
                        it.copy(
                            alertThreshold = settings.alertThreshold,
                            dailySummaryEnabled = settings.dailySummaryEnabled,
                            dailySummaryTime = settings.dailySummaryTime
                        )
                    }
                }
            }
        }
    }
    
    fun removePatient(patient: PatientProfile) {
        viewModelScope.launch {
            repository.removePatientFromCaregiver(patient.pin)
        }
    }
    
    fun updatePatient(
        patient: PatientProfile,
        displayName: String?,
        phoneNumber: String?,
        notes: String?
    ) {
        viewModelScope.launch {
            repository.updatePatientInfo(patient.pin, displayName, phoneNumber, notes)
        }
    }
    
    fun updateAlertThreshold(threshold: Int) {
        viewModelScope.launch {
            val current = repository.getSettings().first() ?: com.example.medicaladherence.data.firebase.FirestoreSettings()
            repository.saveSettings(current.copy(alertThreshold = threshold))
        }
    }
    
    fun updateDailySummary(enabled: Boolean) {
        viewModelScope.launch {
            val current = repository.getSettings().first() ?: com.example.medicaladherence.data.firebase.FirestoreSettings()
            repository.saveSettings(current.copy(dailySummaryEnabled = enabled))
        }
    }
}

class CaregiverSettingsViewModelFactory(
    private val repository: FirebaseMedicationRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CaregiverSettingsViewModel::class.java)) {
            return CaregiverSettingsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

