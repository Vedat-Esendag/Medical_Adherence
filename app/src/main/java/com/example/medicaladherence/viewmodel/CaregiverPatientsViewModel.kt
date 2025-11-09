package com.example.medicaladherence.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.medicaladherence.data.model.PatientProfile
import com.example.medicaladherence.data.repo.RepositoryProvider
import com.example.medicaladherence.data.repository.FirebaseMedicationRepository
import com.example.medicaladherence.util.QRCodeScanner
import com.example.medicaladherence.utils.AppConstants
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CaregiverPatientsViewModel(
    private val repository: FirebaseMedicationRepository = RepositoryProvider.getRepository()
) : ViewModel() {

    val patients: StateFlow<List<PatientProfile>> = repository.getCaregiverPatients()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(AppConstants.STATEFLOW_TIMEOUT_MS),
            initialValue = emptyList()
        )
    
    private val _importStatus = MutableStateFlow<ImportStatus>(ImportStatus.Idle)
    val importStatus: StateFlow<ImportStatus> = _importStatus.asStateFlow()
    
    /**
     * Import patient data from scanned QR code
     */
    fun importPatientFromQR(qrData: String) {
        viewModelScope.launch {
            _importStatus.value = ImportStatus.Loading
            
            val parseResult = QRCodeScanner.parseQRData(qrData)
            
            parseResult.fold(
                onSuccess = { patientData ->
                    try {
                        // Check for duplicate patient
                        val existingPatients = patients.value
                        if (existingPatients.any { it.pin == patientData.pin }) {
                            _importStatus.value = ImportStatus.Error("This patient is already in your list")
                            return@launch
                        }
                        
                        repository.importPatientData(patientData)
                        _importStatus.value = ImportStatus.Success(patientData.name)
                    } catch (e: Exception) {
                        _importStatus.value = ImportStatus.Error("Failed to import patient data: ${e.message}")
                    }
                },
                onFailure = { error ->
                    _importStatus.value = ImportStatus.Error(error.message ?: "Invalid QR code format")
                }
            )
        }
    }
    
    /**
     * Import patient from manually entered PIN
     * Works for testing when patient and caregiver profiles are on same device
     */
    fun importPatientFromPin(pin: String) {
        viewModelScope.launch {
            _importStatus.value = ImportStatus.Loading
            
            try {
                // Validate PIN format
                if (pin.length != AppConstants.PIN_LENGTH || !pin.all { it.isDigit() }) {
                    _importStatus.value = ImportStatus.Error("PIN must be ${AppConstants.PIN_LENGTH} digits")
                    return@launch
                }
                
                // Check for duplicate patient
                val existingPatients = patients.value
                if (existingPatients.any { it.pin == pin }) {
                    _importStatus.value = ImportStatus.Error("This patient is already in your list")
                    return@launch
                }
                
                // Try to find patient data in local database (for testing/same device)
                val patientData = repository.getPatientDataByPin(pin)
                
                if (patientData != null) {
                    // Import the data
                    repository.importPatientData(patientData)
                    _importStatus.value = ImportStatus.Success(patientData.name)
                } else {
                    // PIN not found on this device
                    _importStatus.value = ImportStatus.Error(
                        "Patient not found. Make sure:\n" +
                        "• The PIN is correct ($pin)\n" +
                        "• Patient has signed out and back in (recreates profile)\n" +
                        "• Patient and caregiver are using different profiles on this device\n" +
                        "• For separate devices, use QR code instead"
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("CaregiverPatientsVM", "❌ Error importing patient: ${e.message}", e)
                _importStatus.value = ImportStatus.Error("Failed to add patient: ${e.message}")
            }
        }
    }
    
    /**
     * Re-sync patient data (update from new QR scan)
     */
    fun syncPatientData(qrData: String) {
        importPatientFromQR(qrData) // Same operation as import
    }
    
    /**
     * Remove patient from monitoring list
     */
    fun removePatient(patient: PatientProfile) {
        viewModelScope.launch {
            try {
                repository.removePatientFromCaregiver(patient.pin)
            } catch (e: Exception) {
                android.util.Log.e("CaregiverPatientsVM", "Failed to remove patient: ${e.message}", e)
                _importStatus.value = ImportStatus.Error("Failed to remove patient. Please try again.")
            }
        }
    }
    
    fun resetImportStatus() {
        _importStatus.value = ImportStatus.Idle
    }
}

sealed class ImportStatus {
    object Idle : ImportStatus()
    object Loading : ImportStatus()
    data class Success(val patientName: String) : ImportStatus()
    data class Error(val message: String) : ImportStatus()
}

class CaregiverPatientsViewModelFactory(
    private val repository: FirebaseMedicationRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CaregiverPatientsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CaregiverPatientsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

