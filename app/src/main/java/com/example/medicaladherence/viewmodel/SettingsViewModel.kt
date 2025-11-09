package com.example.medicaladherence.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.medicaladherence.data.firebase.FirestoreSettings
import com.example.medicaladherence.data.repo.RepositoryProvider
import com.example.medicaladherence.data.repository.FirebaseMedicationRepository
import com.example.medicaladherence.utils.AppConstants
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val fontScale: Float = 1.0f
)

class SettingsViewModel(
    private val repository: FirebaseMedicationRepository = RepositoryProvider.getRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    // User profile from Firestore - using MutableStateFlow to avoid immediate execution
    private val _userProfile = MutableStateFlow<String?>(null)
    val userProfile: StateFlow<String?> = _userProfile.asStateFlow()

    private val _pairingPin = MutableStateFlow<String?>(null)
    val pairingPin: StateFlow<String?> = _pairingPin.asStateFlow()

    private val _patientName = MutableStateFlow<String?>(null)
    val patientName: StateFlow<String?> = _patientName.asStateFlow()

    // App settings from Firestore
    private val settingsFlow = repository.getSettings()

    init {
        loadProfileIfExists()
    }

    private fun loadProfileIfExists() {
        viewModelScope.launch {
            try {
                val profile = repository.getCurrentUserProfile()
                
                // Strict validation: profile must have BOTH role AND name to be valid
                // This prevents loading incomplete/stale profiles after sign out
                val hasValidRole = profile != null && !profile.role.isNullOrEmpty()
                val hasValidName = profile != null && !profile.name.isNullOrEmpty()
                
                if (hasValidRole && hasValidName) {
                    _userProfile.value = profile.role
                    _pairingPin.value = profile.pin
                    _patientName.value = profile.name
                } else {
                    _userProfile.value = null
                    _pairingPin.value = null
                    _patientName.value = null
                }
            } catch (e: Exception) {
                _userProfile.value = null
                _pairingPin.value = null
                _patientName.value = null
            }
        }
    }

    val caretakerPin: StateFlow<String?> = settingsFlow
        .map { it?.caretakerPin }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.STATEFLOW_TIMEOUT_MS), null)

    val highContrastMode: StateFlow<Boolean> = settingsFlow
        .map { it?.highContrastMode ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.STATEFLOW_TIMEOUT_MS), false)

    // Save user profile (patient or caregiver)
    fun setUserProfile(profile: String, name: String? = null) {
        // Update local state IMMEDIATELY (synchronously)
        val pin = if (profile == "patient") {
            generatePairingPin()
        } else ""
        
        _userProfile.value = profile
        _pairingPin.value = pin
        _patientName.value = name ?: "User"
        
        // Then save to Firebase asynchronously
        viewModelScope.launch {
            try {
                repository.setUserProfile(profile, name ?: "User", pin)
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Error saving profile to Firebase (will retry later)", e)
                // Local state already updated, so UI will work
            }
        }
    }

    private fun generatePairingPin(): String {
        return (AppConstants.PIN_MIN..AppConstants.PIN_MAX).random().toString()
    }

    // Save caretaker PIN
    fun setCaretakerPin(pin: String) {
        viewModelScope.launch {
            val current = settingsFlow.first() ?: FirestoreSettings()
            repository.saveSettings(current.copy(caretakerPin = pin))
        }
    }

    fun removeCaretakerPin() {
        viewModelScope.launch {
            val current = settingsFlow.first() ?: FirestoreSettings()
            repository.saveSettings(current.copy(caretakerPin = null))
        }
    }

    fun verifyPin(enteredPin: String): Boolean {
        val storedPin = caretakerPin.value
        return storedPin != null && storedPin == enteredPin
    }

    suspend fun isPinRequired(): Boolean {
        return caretakerPin.value != null
    }

    // High contrast mode
    fun setHighContrastMode(enabled: Boolean) {
        viewModelScope.launch {
            val current = settingsFlow.first() ?: FirestoreSettings()
            repository.saveSettings(current.copy(highContrastMode = enabled))
        }
    }

    // Font scale
    fun setFontScale(scale: Float) {
        viewModelScope.launch {
            val current = settingsFlow.first() ?: FirestoreSettings()
            repository.saveSettings(current.copy(fontScale = scale))
        }
    }

    // Update patient name
    fun updatePatientName(name: String) {
        viewModelScope.launch {
            val profile = repository.getCurrentUserProfile()
            if (profile != null) {
                repository.setUserProfile(profile.role, name, profile.pin)
                _patientName.value = name
            }
        }
    }

    // Generate QR data for patient
    suspend fun generatePatientQRData(): String? {
        val pin = pairingPin.value ?: return null
        val name = patientName.value ?: "Unknown Patient"

        return try {
            val patientData = repository.exportPatientData(pin, name)
            patientData.toJson()
        } catch (e: Exception) {
            android.util.Log.e("SettingsViewModel", "Failed to generate QR data: ${e.message}", e)
            null
        }
    }

    // Sign out
    fun clearUserProfile() {
        // Clear local state IMMEDIATELY (synchronously) for instant UI update
        _userProfile.value = null
        _pairingPin.value = null
        _patientName.value = null

        // Then clean up Firebase asynchronously
        viewModelScope.launch {
            try {
                // Delete user profile from Firebase
                repository.deleteUserProfile()
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Error deleting profile from Firebase (already cleared locally)", e)
            }

            try {
                // Sign out from Firebase Auth
                RepositoryProvider.getAuthManager().signOut()
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Error signing out from Firebase Auth", e)
            }
        }
    }
}

class SettingsViewModelFactory(
    private val repository: FirebaseMedicationRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            return SettingsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
