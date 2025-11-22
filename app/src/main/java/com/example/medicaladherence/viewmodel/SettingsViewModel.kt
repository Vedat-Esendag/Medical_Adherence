package com.example.medicaladherence.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.medicaladherence.data.firebase.FirestoreSettings
import com.example.medicaladherence.data.repository.RepositoryProvider
import com.example.medicaladherence.data.repository.FirebaseMedicationRepository
import com.example.medicaladherence.utils.AppConstants
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val fontScale: Float = 1.0f
)

/**
 * ViewModel for managing user profile settings and app preferences.
 * 
 * Key Features:
 * - User profile management (role: Patient/Caregiver, name, PIN)
 * - QR code generation for caregiver pairing
 * - App settings (accessibility, notifications)
 * - Profile validation and state management
 * 
 * User Roles:
 * - **Patient**: Has a 6-digit PIN for caregiver pairing
 * - **Caregiver**: Can monitor multiple patients via PIN
 * 
 * @param repository The medication data repository
 */
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

    /**
     * Sets the user's profile type and name.
     * 
     * For patients: Automatically generates a unique 6-digit PIN for caregiver pairing.
     * For caregivers: No PIN is generated.
     * 
     * Updates local state immediately for instant UI feedback, then persists to Firebase.
     * 
     * @param profile The profile type: "patient" or "caregiver"
     * @param name The user's name (defaults to "User" if not provided)
     */
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

    /**
     * Sets a security PIN to protect sensitive caregiver features.
     * 
     * @param pin The 6-digit PIN to set
     */
    fun setCaretakerPin(pin: String) {
        viewModelScope.launch {
            val current = settingsFlow.first() ?: FirestoreSettings()
            repository.saveSettings(current.copy(caretakerPin = pin))
        }
    }

    /**
     * Removes the security PIN, allowing unrestricted access to caregiver features.
     */
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

    /**
     * Updates the patient's display name in their profile.
     * 
     * @param name The new name to set
     */
    fun updatePatientName(name: String) {
        viewModelScope.launch {
            val profile = repository.getCurrentUserProfile()
            if (profile != null) {
                repository.setUserProfile(profile.role, name, profile.pin)
                _patientName.value = name
            }
        }
    }

    /**
     * Generates JSON data for QR code that caregivers can scan to add this patient.
     * 
     * Includes:
     * - Patient's PIN
     * - Patient's name
     * - All medications
     * - Recent dose history (for continuity)
     * 
     * @return JSON string for QR code, or null if generation fails
     */
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

    /**
     * Clears the user's profile and signs them out.
     * Updates local state immediately for instant UI response, then cleans up Firebase.
     */
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
