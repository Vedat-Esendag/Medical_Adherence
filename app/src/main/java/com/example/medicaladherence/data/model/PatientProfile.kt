package com.example.medicaladherence.data.model

/**
 * Represents a patient profile in the caregiver's list.
 * This is a simple data model (not a Room entity).
 */
data class PatientProfile(
    val pin: String,
    val name: String,
    val addedAt: Long = System.currentTimeMillis(),
    val lastSyncedAt: Long = System.currentTimeMillis(), // With Firebase, always "synced" in real-time
    val medicationCount: Int = 0, // Can be calculated from Firestore if needed
    // Editable patient metadata
    val displayName: String? = null,
    val phoneNumber: String? = null,
    val notes: String? = null
)
