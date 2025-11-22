package com.example.medicaladherence.data.model

import com.example.medicaladherence.utils.AppConstants
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
    init {
        require(pin.length == AppConstants.PIN_LENGTH && pin.all { it.isDigit() }) { 
            "PIN must be exactly ${AppConstants.PIN_LENGTH} digits" 
        }
        require(name.isNotBlank()) { "Patient name cannot be blank" }
    }
    
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
    val specificDays: List<Int>,
    val intervalDays: Int? = null,
    val startDate: String? = null
) {
    init {
        require(name.isNotBlank()) { "Medication name cannot be blank" }
        require(dosage.isNotBlank()) { "Medication dosage cannot be blank" }
        // Times are optional only for AsNeeded medications
        val freq = try { MedicationFrequency.valueOf(frequency) } catch (e: Exception) { MedicationFrequency.Daily }
        if (freq != MedicationFrequency.AsNeeded) {
            require(times.isNotEmpty()) { "Medication must have at least one scheduled time" }
        }
    }

    fun toMedication(): Medication {
        return Medication(
            id = id,
            name = name,
            dosage = dosage,
            times = times,
            notes = notes,
            frequency = MedicationFrequency.valueOf(frequency),
            specificDays = specificDays,
            intervalDays = intervalDays,
            startDate = startDate
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
                specificDays = med.specificDays,
                intervalDays = med.intervalDays,
                startDate = med.startDate
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

