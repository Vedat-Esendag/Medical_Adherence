package com.example.medicaladherence.notification

import android.content.Context
import androidx.work.*
import com.example.medicaladherence.data.model.Medication
import com.example.medicaladherence.data.model.MedicationFrequency
import com.example.medicaladherence.data.repository.RepositoryProvider
import kotlinx.coroutines.flow.first
import java.time.LocalTime
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class NotificationScheduler(private val context: Context) {

    fun scheduleMedicationNotifications(medication: Medication) {
        // Don't schedule notifications for AsNeeded medications
        if (medication.frequency == MedicationFrequency.AsNeeded) {
            return
        }

        medication.times.forEach { time ->
            scheduleNotificationForTime(medication, time)
        }
    }

    private fun scheduleNotificationForTime(medication: Medication, time: String) {
        val workTag = "med_${medication.id}_$time"

        // Cancel existing work with this tag
        WorkManager.getInstance(context).cancelAllWorkByTag(workTag)

        val inputData = Data.Builder()
            .putString(MedicationReminderWorker.KEY_MEDICATION_NAME, medication.name)
            .putString(MedicationReminderWorker.KEY_DOSAGE, medication.dosage)
            .putString(MedicationReminderWorker.KEY_TIME, time)
            .putString(MedicationReminderWorker.KEY_MED_ID, medication.id)
            .putString(MedicationReminderWorker.KEY_FREQUENCY, medication.frequency.name)
            .putString(MedicationReminderWorker.KEY_SPECIFIC_DAYS, medication.specificDays.joinToString(","))
            .putInt(MedicationReminderWorker.KEY_INTERVAL_DAYS, medication.intervalDays ?: 0)
            .putString(MedicationReminderWorker.KEY_START_DATE, medication.startDate ?: "")
            .build()

        val delay = calculateDelayUntilTime(time)

        val workRequest = OneTimeWorkRequestBuilder<MedicationReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag(workTag)
            .addTag("medication_${medication.id}")
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)

        // Also schedule for tomorrow (recurring)
        scheduleRecurringNotification(medication, time, workTag)
    }

    private fun scheduleRecurringNotification(medication: Medication, time: String, workTag: String) {
        val inputData = Data.Builder()
            .putString(MedicationReminderWorker.KEY_MEDICATION_NAME, medication.name)
            .putString(MedicationReminderWorker.KEY_DOSAGE, medication.dosage)
            .putString(MedicationReminderWorker.KEY_TIME, time)
            .putString(MedicationReminderWorker.KEY_MED_ID, medication.id)
            .putString(MedicationReminderWorker.KEY_FREQUENCY, medication.frequency.name)
            .putString(MedicationReminderWorker.KEY_SPECIFIC_DAYS, medication.specificDays.joinToString(","))
            .putInt(MedicationReminderWorker.KEY_INTERVAL_DAYS, medication.intervalDays ?: 0)
            .putString(MedicationReminderWorker.KEY_START_DATE, medication.startDate ?: "")
            .build()

        val delay = calculateDelayUntilTime(time)
        val dailyDelay = if (delay > 0) delay else delay + (24 * 60 * 60 * 1000)

        val periodicWorkRequest = PeriodicWorkRequestBuilder<MedicationReminderWorker>(
            24, TimeUnit.HOURS,
            15, TimeUnit.MINUTES // Flex interval
        )
            .setInitialDelay(dailyDelay, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag("${workTag}_recurring")
            .addTag("medication_${medication.id}")
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "${workTag}_recurring",
                ExistingPeriodicWorkPolicy.REPLACE,
                periodicWorkRequest
            )
    }

    private fun calculateDelayUntilTime(timeString: String): Long {
        val targetTime = LocalTime.parse(timeString)
        val now = LocalDateTime.now()
        var targetDateTime = now.with(targetTime)

        // If time has passed today, schedule for tomorrow
        if (targetDateTime.isBefore(now)) {
            targetDateTime = targetDateTime.plusDays(1)
        }

        return Duration.between(now, targetDateTime).toMillis()
    }

    fun cancelNotifications(medicationId: String) {
        WorkManager.getInstance(context).cancelAllWorkByTag("medication_$medicationId")
    }

    fun cancelAllNotifications() {
        WorkManager.getInstance(context).cancelAllWork()
    }

    fun rescheduleAllNotifications(medications: List<Medication>) {
        cancelAllNotifications()
        medications.forEach { medication ->
            scheduleMedicationNotifications(medication)
        }
    }

    // Convenience method for rescheduling from Firebase
    suspend fun rescheduleAllFromFirebase() {
        val repository = RepositoryProvider.getRepository()
        val medications = repository.medications.first()
        rescheduleAllNotifications(medications)
    }
}
