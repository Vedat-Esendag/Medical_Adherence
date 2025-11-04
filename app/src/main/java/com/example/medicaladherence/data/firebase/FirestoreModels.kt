package com.example.medicaladherence.data.firebase

import com.example.medicaladherence.data.model.Medication
import com.example.medicaladherence.data.model.MedicationFrequency
import com.example.medicaladherence.data.model.DoseEvent
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import java.time.LocalDate
import java.time.ZoneId

/**
 * User profile in Firestore
 */
data class FirestoreUserProfile(
    @DocumentId val userId: String = "",
    val role: String = "",  // "patient" or "caregiver"
    val name: String = "",
    val pin: String = "",   // 6-digit PIN
    val createdAt: Timestamp = Timestamp.now()
)

/**
 * Settings in Firestore
 */
data class FirestoreSettings(
    val fontScale: Float = 1.0f,
    val caretakerPin: String? = null,
    val highContrastMode: Boolean = false,
    // Caregiver notification preferences
    val alertThreshold: Int = 70,  // Alert when adherence drops below this %
    val dailySummaryEnabled: Boolean = false,
    val dailySummaryTime: String? = null  // "20:00" format, null if disabled
)

/**
 * Caregiver link (allows caregiver to access patient data)
 */
data class FirestoreCaregiverLink(
    @DocumentId val linkId: String = "",
    val caregiverUserId: String = "",
    val patientUserId: String = "",
    val patientPin: String = "",
    val patientName: String = "",
    val addedAt: Timestamp = Timestamp.now(),
    // Editable patient metadata for caregiver
    val displayName: String? = null,     // Caregiver's custom name ("Mom")
    val phoneNumber: String? = null,     // For calling patient
    val notes: String? = null            // Medication/doctor notes
)

/**
 * Medication in Firestore (matches existing Medication model)
 */
data class FirestoreMedication(
    @DocumentId val id: String = "",
    val name: String = "",
    val dosage: String = "",
    val times: List<String> = emptyList(),
    val notes: String? = null,
    val frequency: String = "Daily",
    val specificDays: List<Int> = emptyList()
) {
    fun toMedication(): Medication = Medication(
        id = id,
        name = name,
        dosage = dosage,
        times = times,
        notes = notes,
        frequency = MedicationFrequency.valueOf(frequency),
        specificDays = specificDays
    )

    companion object {
        fun fromMedication(med: Medication): FirestoreMedication = FirestoreMedication(
            id = med.id,
            name = med.name,
            dosage = med.dosage,
            times = med.times,
            notes = med.notes,
            frequency = med.frequency.name,
            specificDays = med.specificDays
        )
    }
}

/**
 * Dose event in Firestore
 */
data class FirestoreDoseEvent(
    @DocumentId val id: String = "",
    val medId: String = "",
    val date: String = "",  // ISO format (yyyy-MM-dd)
    val time: String = "",  // HH:mm format
    val taken: Boolean = false
) {
    fun toDoseEvent(): DoseEvent = DoseEvent(
        medId = medId,
        date = LocalDate.parse(date),
        time = time,
        taken = taken
    )

    companion object {
        fun fromDoseEvent(event: DoseEvent): FirestoreDoseEvent = FirestoreDoseEvent(
            id = "${event.medId}_${event.date}_${event.time}",  // Composite key
            medId = event.medId,
            date = event.date.toString(),
            time = event.time,
            taken = event.taken
        )
    }
}
