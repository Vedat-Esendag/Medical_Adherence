package com.example.medicaladherence.data

import com.example.medicaladherence.data.model.Medication
import com.example.medicaladherence.data.model.MedicationFrequency
import java.util.UUID

object SeedData {
    fun getMedicationsSeed(): List<Medication> {
        return listOf(
            Medication(
                id = UUID.randomUUID().toString(),
                name = "Amlodipine",
                dosage = "5 mg",
                times = listOf("07:00"),
                notes = "Take with water in the morning",
                frequency = MedicationFrequency.Daily,
                specificDays = emptyList()
            ),
            Medication(
                id = UUID.randomUUID().toString(),
                name = "Metoprolol",
                dosage = "50 mg",
                times = listOf("19:00"),
                notes = "Take with dinner",
                frequency = MedicationFrequency.Daily,
                specificDays = emptyList()
            ),
            Medication(
                id = UUID.randomUUID().toString(),
                name = "Aspirin",
                dosage = "81 mg",
                times = listOf("21:00"),
                notes = "Low-dose for heart health",
                frequency = MedicationFrequency.Daily,
                specificDays = emptyList()
            ),
            Medication(
                id = UUID.randomUUID().toString(),
                name = "Mesalamine",
                dosage = "800 mg",
                times = listOf("08:00"),
                notes = "For IBD management",
                frequency = MedicationFrequency.Daily,
                specificDays = emptyList()
            ),
            Medication(
                id = UUID.randomUUID().toString(),
                name = "Azathioprine",
                dosage = "50 mg",
                times = listOf("22:00"),
                notes = "Immunosuppressant - take at bedtime",
                frequency = MedicationFrequency.Daily,
                specificDays = emptyList()
            )
        )
    }
}
