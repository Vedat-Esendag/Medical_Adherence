package com.example.medicaladherence.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.medicaladherence.data.firebase.FirestoreSettings
import com.example.medicaladherence.data.model.PatientProfile
import com.example.medicaladherence.data.repository.FirebaseMedicationRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CaregiverSettingsUiState(
    val caregiverName: String = "",
    val caregiverPhone: String = "",
    val caregiverEmail: String = "",
    val patients: List<PatientProfile> = emptyList(),
    val fontScale: Float = 1.0f,
    val highContrastMode: Boolean = false
)

class CaregiverSettingsViewModel(
    private val repository: FirebaseMedicationRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CaregiverSettingsUiState())
    val uiState: StateFlow<CaregiverSettingsUiState> = _uiState.asStateFlow()
    
    // Get settings flow from repository
    private val settingsFlow = repository.getSettings()
    
    init {
        loadSettings()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            // Load patients
            repository.getCaregiverPatients().collect { patients ->
                _uiState.update { it.copy(patients = patients) }
            }
        }
        
        viewModelScope.launch {
            // Load display preferences from settings flow
            settingsFlow.collect { settings ->
                _uiState.update {
                    it.copy(
                        fontScale = settings?.fontScale ?: 1.0f,
                        highContrastMode = settings?.highContrastMode ?: false
                    )
                }
            }
        }
        
        // Load caregiver profile (TODO: implement when repository methods are available)
        // For now, profile fields will be empty and saved locally when changed
    }
    
    fun updateCaregiverName(name: String) {
        _uiState.update { it.copy(caregiverName = name) }
        // TODO: Save to repository when method is available
    }
    
    fun updateCaregiverPhone(phone: String) {
        _uiState.update { it.copy(caregiverPhone = phone) }
        // TODO: Save to repository when method is available
    }
    
    fun updateCaregiverEmail(email: String) {
        _uiState.update { it.copy(caregiverEmail = email) }
        // TODO: Save to repository when method is available
    }
    
    fun updateFontScale(scale: Float) {
        _uiState.update { it.copy(fontScale = scale) }
        viewModelScope.launch {
            val current = settingsFlow.first() ?: FirestoreSettings()
            repository.saveSettings(current.copy(fontScale = scale))
        }
    }
    
    fun updateHighContrastMode(enabled: Boolean) {
        _uiState.update { it.copy(highContrastMode = enabled) }
        viewModelScope.launch {
            val current = settingsFlow.first() ?: FirestoreSettings()
            repository.saveSettings(current.copy(highContrastMode = enabled))
        }
    }
    
    fun removePatient(patient: PatientProfile) {
        viewModelScope.launch {
            repository.removePatientFromCaregiver(patient.pin)
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

