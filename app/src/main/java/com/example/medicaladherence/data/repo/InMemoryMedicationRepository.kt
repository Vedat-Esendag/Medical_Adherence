package com.example.medicaladherence.data.repo

import com.example.medicaladherence.data.model.DoseEvent
import com.example.medicaladherence.data.model.Medication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Represents the last action performed, allowing for undo functionality
 */
data class LastAction(
    val medId: String,
    val date: LocalDate,
    val time: String,
    val previousTaken: Boolean?
)

/**
 * In-memory repository for medications and dose events.
 * Contains seed data for Maria and Ahmed with ~80% adherence for the past week.
 */
class InMemoryMedicationRepository {

    private val _medications = MutableStateFlow<List<Medication>>(emptyList())
    val medications: StateFlow<List<Medication>> = _medications.asStateFlow()

    private val _events = MutableStateFlow<List<DoseEvent>>(emptyList())
    val events: StateFlow<List<DoseEvent>> = _events.asStateFlow()

    private val _lastAction = MutableStateFlow<LastAction?>(null)
    val lastAction: StateFlow<LastAction?> = _lastAction.asStateFlow()

    init {
        seedData()
    }

    /**
     * Seed medications for Maria and Ahmed with dose events for the past 7 days
     */
    private fun seedData() {
        // Maria's medications
        val mariaMeds = listOf(
            Medication(
                id = "maria-1",
                name = "Amlodipine",
                dosage = "5 mg",
                times = listOf("07:00"),
                notes = "Take with water in the morning",
                frequency = com.example.medicaladherence.data.model.MedicationFrequency.Daily
            ),
            Medication(
                id = "maria-2",
                name = "Metoprolol",
                dosage = "50 mg",
                times = listOf("19:00"),
                notes = "Take with dinner",
                frequency = com.example.medicaladherence.data.model.MedicationFrequency.Daily
            ),
            Medication(
                id = "maria-3",
                name = "Aspirin",
                dosage = "81 mg",
                times = listOf("21:00"),
                notes = "Low-dose for heart health",
                frequency = com.example.medicaladherence.data.model.MedicationFrequency.Daily
            )
        )

        // Ahmed's medications
        val ahmedMeds = listOf(
            Medication(
                id = "ahmed-1",
                name = "Mesalamine",
                dosage = "800 mg",
                times = listOf("08:00"),
                notes = "For IBD management",
                frequency = com.example.medicaladherence.data.model.MedicationFrequency.Daily
            ),
            Medication(
                id = "ahmed-2",
                name = "Azathioprine",
                dosage = "50 mg",
                times = listOf("22:00"),
                notes = "Immunosuppressant - take at bedtime",
                frequency = com.example.medicaladherence.data.model.MedicationFrequency.Daily
            )
        )

        _medications.value = mariaMeds + ahmedMeds

        // Generate events for the past 7 days with ~80% adherence
        val today = LocalDate.now()
        val events = mutableListOf<DoseEvent>()

        for (dayOffset in 0..6) {
            val date = today.minusDays(dayOffset.toLong())

            // For each medication, create events for each scheduled time
            _medications.value.forEach { med ->
                med.times.forEach { time ->
                    // 80% chance of being taken
                    val taken = Math.random() < 0.80
                    events.add(
                        DoseEvent(
                            medId = med.id,
                            date = date,
                            time = time,
                            taken = taken
                        )
                    )
                }
            }
        }

        _events.value = events
    }

    /**
     * Add a new medication or update an existing one
     */
    fun addOrUpdateMedication(medication: Medication) {
        val currentMeds = _medications.value.toMutableList()
        val existingIndex = currentMeds.indexOfFirst { it.id == medication.id }

        if (existingIndex >= 0) {
            currentMeds[existingIndex] = medication
        } else {
            currentMeds.add(medication)
        }

        _medications.value = currentMeds
    }

    /**
     * Delete a medication and all its associated events
     */
    fun deleteMedication(medId: String) {
        _medications.value = _medications.value.filter { it.id != medId }
        _events.value = _events.value.filter { it.medId != medId }
    }

    /**
     * Mark a dose as taken or missed
     */
    fun markDose(medId: String, date: LocalDate, time: String, taken: Boolean) {
        val currentEvents = _events.value.toMutableList()
        val existingIndex = currentEvents.indexOfFirst {
            it.medId == medId && it.date == date && it.time == time
        }

        // Store previous state for undo
        val previousTaken = if (existingIndex >= 0) {
            currentEvents[existingIndex].taken
        } else {
            null
        }
        _lastAction.value = LastAction(medId, date, time, previousTaken)

        val event = DoseEvent(medId = medId, date = date, time = time, taken = taken)

        if (existingIndex >= 0) {
            currentEvents[existingIndex] = event
        } else {
            currentEvents.add(event)
        }

        _events.value = currentEvents
    }

    /**
     * Undo the last action
     */
    fun undoLastAction() {
        val action = _lastAction.value ?: return

        val currentEvents = _events.value.toMutableList()
        val existingIndex = currentEvents.indexOfFirst {
            it.medId == action.medId && it.date == action.date && it.time == action.time
        }

        if (action.previousTaken == null) {
            // Remove the event if it didn't exist before
            if (existingIndex >= 0) {
                currentEvents.removeAt(existingIndex)
            }
        } else {
            // Restore previous state
            val event = DoseEvent(
                medId = action.medId,
                date = action.date,
                time = action.time,
                taken = action.previousTaken
            )
            if (existingIndex >= 0) {
                currentEvents[existingIndex] = event
            } else {
                currentEvents.add(event)
            }
        }

        _events.value = currentEvents
        _lastAction.value = null
    }

    /**
     * Get a medication by ID
     */
    fun getMedicationById(id: String): Medication? {
        return _medications.value.find { it.id == id }
    }

    /**
     * Get today's scheduled doses with their taken status
     * Returns list of (Medication, time, taken?)
     */
    fun todayDoses(): List<Triple<Medication, String, Boolean?>> {
        val today = LocalDate.now()
        val todayEvents = _events.value.filter { it.date == today }

        return _medications.value.flatMap { med ->
            med.times.map { time ->
                val event = todayEvents.find { it.medId == med.id && it.time == time }
                Triple(med, time, event?.taken)
            }
        }.sortedBy { (_, time, _) ->
            // Sort by time
            LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm"))
        }
    }

    /**
     * Calculate weekly adherence percentage
     */
    fun calculateWeeklyAdherence(): Int {
        val today = LocalDate.now()
        val weekAgo = today.minusDays(6)

        val weekEvents = _events.value.filter {
            it.date >= weekAgo && it.date <= today
        }

        if (weekEvents.isEmpty()) return 0

        val takenCount = weekEvents.count { it.taken }
        return ((takenCount.toFloat() / weekEvents.size) * 100).toInt()
    }

    /**
     * Calculate current streak in days
     */
    fun calculateStreak(): Int {
        val today = LocalDate.now()
        var streak = 0
        var currentDate = today

        while (true) {
            val dayEvents = _events.value.filter { it.date == currentDate }
            if (dayEvents.isEmpty()) break

            // Day counts if all doses were taken
            if (dayEvents.all { it.taken }) {
                streak++
                currentDate = currentDate.minusDays(1)
            } else {
                break
            }
        }

        return streak
    }

    /**
     * Get daily adherence for the past 7 days (for stats chart)
     * Returns map of date to percentage
     */
    fun getDailyAdherenceForWeek(): Map<LocalDate, Int> {
        val today = LocalDate.now()
        val result = mutableMapOf<LocalDate, Int>()

        for (dayOffset in 6 downTo 0) {
            val date = today.minusDays(dayOffset.toLong())
            val dayEvents = _events.value.filter { it.date == date }

            val percentage = if (dayEvents.isEmpty()) {
                0
            } else {
                ((dayEvents.count { it.taken }.toFloat() / dayEvents.size) * 100).toInt()
            }

            result[date] = percentage
        }

        return result
    }

    /**
     * Snooze a dose (in this simple version, just a placeholder - no actual timer)
     */
    fun snooze(medId: String, time: String, minutes: Int) {
        // In a real app, this would schedule a notification
        // For now, this is just for UI feedback
    }
}
