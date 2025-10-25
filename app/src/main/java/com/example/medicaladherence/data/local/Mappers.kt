package com.example.medicaladherence.data.local

import com.example.medicaladherence.data.local.entity.DoseEventEntity
import com.example.medicaladherence.data.local.entity.MedicationEntity
import com.example.medicaladherence.data.model.DoseEvent
import com.example.medicaladherence.data.model.Medication

// Medication mappers
fun MedicationEntity.toMedication(): Medication {
    return Medication(
        id = id,
        name = name,
        dosage = dosage,
        times = times,
        notes = notes,
        frequency = frequency,
        specificDays = specificDays
    )
}

fun Medication.toEntity(): MedicationEntity {
    return MedicationEntity(
        id = id,
        name = name,
        dosage = dosage,
        times = times,
        notes = notes,
        frequency = frequency,
        specificDays = specificDays
    )
}

// DoseEvent mappers
fun DoseEventEntity.toDoseEvent(): DoseEvent {
    return DoseEvent(
        medId = medId,
        date = date,
        time = time,
        taken = taken
    )
}

fun DoseEvent.toEntity(): DoseEventEntity {
    return DoseEventEntity(
        medId = medId,
        date = date,
        time = time,
        taken = taken
    )
}
