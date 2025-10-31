package com.example.medicaladherence.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.medicaladherence.data.model.PatientProfile
import com.example.medicaladherence.data.repo.RepositoryProvider
import com.example.medicaladherence.data.repository.FirebaseMedicationRepository
import com.example.medicaladherence.util.QRCodeScanner
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CaregiverPatientsViewModel(
    private val repository: FirebaseMedicationRepository = RepositoryProvider.getRepository()
) : ViewModel() {

    val patients: StateFlow<List<PatientProfile>> = repository.getCaregiverPatients()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
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
                android.util.Log.d("CaregiverPatientsVM", "🔍 Attempting to import patient with PIN: $pin")
                
                // Validate PIN format
                if (pin.length != 6 || !pin.all { it.isDigit() }) {
                    android.util.Log.w("CaregiverPatientsVM", "❌ Invalid PIN format: $pin")
                    _importStatus.value = ImportStatus.Error("PIN must be 6 digits")
                    return@launch
                }
                
                // Check for duplicate patient
                val existingPatients = patients.value
                if (existingPatients.any { it.pin == pin }) {
                    android.util.Log.w("CaregiverPatientsVM", "⚠️ Patient with PIN $pin already in list")
                    _importStatus.value = ImportStatus.Error("This patient is already in your list")
                    return@launch
                }
                
                // Try to find patient data in local database (for testing/same device)
                android.util.Log.d("CaregiverPatientsVM", "📡 Querying Firebase for patient with PIN: $pin")
                val patientData = repository.getPatientDataByPin(pin)
                
                if (patientData != null) {
                    android.util.Log.d("CaregiverPatientsVM", "✅ Found patient: ${patientData.name}, importing data...")
                    // Import the data
                    repository.importPatientData(patientData)
                    _importStatus.value = ImportStatus.Success(patientData.name)
                    android.util.Log.d("CaregiverPatientsVM", "🎉 Successfully imported patient: ${patientData.name}")
                } else {
                    // PIN not found on this device
                    android.util.Log.w("CaregiverPatientsVM", "❌ Patient with PIN $pin not found in Firebase")
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
                e.printStackTrace()
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

