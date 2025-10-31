package com.example.medicaladherence.data.model

import com.google.gson.Gson
import java.time.LocalDate

/**
 * Data class for exporting/importing patient data via QR code
 */
data class PatientDataExport(
    val pin: String,
    val name: String,
    val medications: List<MedicationExport>,
    val doseEvents: List<DoseEventExport>,
    val exportedAt: Long = System.currentTimeMillis()
) {
    fun toJson(): String {
        return Gson().toJson(this)
    }
    
    companion object {
        fun fromJson(json: String): PatientDataExport? {
            return try {
                Gson().fromJson(json, PatientDataExport::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * Simplified medication data for QR code export
 */
data class MedicationExport(
    val id: String,
    val name: String,
    val dosage: String,
    val times: List<String>,
    val notes: String?,
    val frequency: String,
    val specificDays: List<Int>
) {
    fun toMedication(): Medication {
        return Medication(
            id = id,
            name = name,
            dosage = dosage,
            times = times,
            notes = notes,
            frequency = MedicationFrequency.valueOf(frequency),
            specificDays = specificDays
        )
    }

    companion object {
        fun fromMedication(med: Medication): MedicationExport {
            return MedicationExport(
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
}

/**
 * Simplified dose event data for QR code export
 */
data class DoseEventExport(
    val id: Long,
    val medId: String,
    val date: String, // ISO format
    val time: String,
    val taken: Boolean
) {
    fun toDoseEvent(): DoseEvent {
        return DoseEvent(
            medId = medId,
            date = LocalDate.parse(date),
            time = time,
            taken = taken
        )
    }

    companion object {
        fun fromDoseEvent(event: DoseEvent): DoseEventExport {
            return DoseEventExport(
                id = 0L, // Not used in Firebase
                medId = event.medId,
                date = event.date.toString(),
                time = event.time,
                taken = event.taken
            )
        }
    }
}

