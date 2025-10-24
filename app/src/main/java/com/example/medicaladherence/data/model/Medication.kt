package com.example.medicaladherence.data.model

/**
 * Represents a medication that a user takes on a regular schedule.
 *
 * @property id Unique identifier for the medication
 * @property name Name of the medication (e.g., "Amlodipine")
 * @property dosage Dosage strength (e.g., "5 mg", "800 mg")
 * @property times List of scheduled times in HH:mm format (e.g., ["07:00", "19:00"])
 * @property notes Optional notes about the medication
 */
data class Medication(
    val id: String,
    val name: String,
    val dosage: String,
    val times: List<String>, // HH:mm format
    val notes: String? = null
)
