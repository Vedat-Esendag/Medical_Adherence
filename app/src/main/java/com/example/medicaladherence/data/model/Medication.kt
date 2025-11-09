package com.example.medicaladherence.data.model

/**
 * Frequency options for medication scheduling
 */
enum class MedicationFrequency {
    Daily,
    SpecificDays,  // e.g., Mon, Wed, Fri
    EveryXDays,    // e.g., every 3 days
    Weekly,        // e.g., every Sunday
    AsNeeded
}

/**
 * Represents a medication that a user takes on a regular schedule.
 *
 * @property id Unique identifier for the medication
 * @property name Name of the medication (e.g., "Amlodipine")
 * @property dosage Dosage strength (e.g., "5 mg", "800 mg")
 * @property times List of scheduled times in HH:mm format (e.g., ["07:00", "19:00"])
 * @property notes Optional notes about the medication
 * @property frequency How often the medication should be taken
 * @property specificDays For weekly scheduling: 1=Monday, 7=Sunday
 */
data class Medication(
    val id: String,
    val name: String,
    val dosage: String,
    val times: List<String>, // HH:mm format
    val notes: String? = null,
    val frequency: MedicationFrequency = MedicationFrequency.Daily,
    val specificDays: List<Int> = emptyList()
) {
    init {
        require(name.isNotBlank()) { "Medication name cannot be blank" }
        require(dosage.isNotBlank()) { "Medication dosage cannot be blank" }
        require(times.isNotEmpty()) { "Medication must have at least one scheduled time" }
        times.forEach { time ->
            require(time.matches(Regex("\\d{2}:\\d{2}"))) { 
                "Time must be in HH:mm format, got: $time" 
            }
        }
        specificDays.forEach { day ->
            require(day in 1..7) { 
                "Specific days must be between 1 (Monday) and 7 (Sunday), got: $day" 
            }
        }
    }
}
