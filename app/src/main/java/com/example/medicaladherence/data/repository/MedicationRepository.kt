package com.example.medicaladherence.data.repository

import com.example.medicaladherence.data.local.AppDatabase
import com.example.medicaladherence.data.local.toEntity
import com.example.medicaladherence.data.local.toMedication
import com.example.medicaladherence.data.local.toDoseEvent
import com.example.medicaladherence.data.model.DoseEvent
import com.example.medicaladherence.data.model.Medication
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class MedicationRepository(private val database: AppDatabase) {

    private val medicationDao = database.medicationDao()
    private val doseEventDao = database.doseEventDao()

    // Expose medications as Flow
    val medications: Flow<List<Medication>> = medicationDao.getAllMedications()
        .map { entities -> entities.map { it.toMedication() } }

    suspend fun getMedicationById(id: String): Medication? {
        return medicationDao.getMedicationById(id)?.toMedication()
    }

    suspend fun addOrUpdateMedication(medication: Medication) {
        medicationDao.insertMedication(medication.toEntity())
    }

    suspend fun deleteMedication(medId: String) {
        medicationDao.deleteMedicationById(medId)
        doseEventDao.deleteEventsForMedication(medId)
    }

    suspend fun markDose(medId: String, date: LocalDate, time: String, taken: Boolean) {
        val event = DoseEvent(medId = medId, date = date, time = time, taken = taken)
        doseEventDao.insertEvent(event.toEntity())
    }

    suspend fun undoDose(medId: String, date: LocalDate, time: String) {
        val existingEvent = doseEventDao.getEvent(medId, date, time)

        if (existingEvent != null) {
            // Delete the event to return to "not marked" state
            doseEventDao.deleteEvent(existingEvent)
        }
    }

    suspend fun getTodayDoses(): List<Triple<Medication, String, Boolean?>> {
        val today = LocalDate.now()
        val medicationsList = medications.first()
        val todayEvents = doseEventDao.getEventsForDate(today).first().map { it.toDoseEvent() }

        return medicationsList.flatMap { med ->
            med.times.map { time ->
                val event = todayEvents.find { it.medId == med.id && it.time == time }
                Triple(med, time, event?.taken)
            }
        }.sortedBy { (_, time, _) ->
            LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm"))
        }
    }

    suspend fun calculateWeeklyAdherence(): Int {
        val today = LocalDate.now()
        val weekAgo = today.minusDays(6)

        val weekEvents = doseEventDao.getEventsInRange(weekAgo, today)
            .first()
            .map { it.toDoseEvent() }

        if (weekEvents.isEmpty()) return 0

        val takenCount = weekEvents.count { it.taken }
        return ((takenCount.toFloat() / weekEvents.size) * 100).toInt()
    }

    suspend fun calculateStreak(): Int {
        val today = LocalDate.now()
        var streak = 0
        var currentDate = today.minusDays(1)

        while (true) {
            val dayEvents = doseEventDao.getEventsForDate(currentDate)
                .first()
                .map { it.toDoseEvent() }

            if (dayEvents.isEmpty()) break

            if (dayEvents.all { it.taken }) {
                streak++
                currentDate = currentDate.minusDays(1)
            } else {
                break
            }
        }

        return streak
    }

    suspend fun getDailyAdherenceForWeek(): Map<LocalDate, Int> {
        val today = LocalDate.now()
        val result = mutableMapOf<LocalDate, Int>()

        for (dayOffset in 6 downTo 0) {
            val date = today.minusDays(dayOffset.toLong())
            val dayEvents = doseEventDao.getEventsForDate(date)
                .first()
                .map { it.toDoseEvent() }

            val percentage = if (dayEvents.isEmpty()) {
                0
            } else {
                ((dayEvents.count { it.taken }.toFloat() / dayEvents.size) * 100).toInt()
            }

            result[date] = percentage
        }

        return result
    }

    // Seed initial data
    suspend fun seedDataIfEmpty(medicationsList: List<Medication>) {
        val existingCount = medicationDao.getAllMedications().first().size

        if (existingCount == 0) {
            // Database is empty, seed it
            medicationsList.forEach { medication ->
                medicationDao.insertMedication(medication.toEntity())
            }

            // Also seed some historical events for demo purposes
            val today = LocalDate.now()
            for (dayOffset in 1..6) {
                val date = today.minusDays(dayOffset.toLong())
                medicationsList.forEach { med ->
                    med.times.forEach { time ->
                        // 80% chance of being taken
                        val taken = Math.random() < 0.80
                        val event = DoseEvent(
                            medId = med.id,
                            date = date,
                            time = time,
                            taken = taken
                        )
                        doseEventDao.insertEvent(event.toEntity())
                    }
                }
            }
        }
    }

    // Placeholder for snooze functionality
    fun snooze(medId: String, time: String, minutes: Int) {
        // In a real app, this would schedule a notification
        // For now, this is just for UI feedback
    }
}
