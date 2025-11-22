package com.example.medicaladherence.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.medicaladherence.MainActivity
import com.example.medicaladherence.R
import com.example.medicaladherence.data.model.MedicationFrequency
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class MedicationReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val medicationName = inputData.getString(KEY_MEDICATION_NAME) ?: return Result.failure()
        val dosage = inputData.getString(KEY_DOSAGE) ?: ""
        val time = inputData.getString(KEY_TIME) ?: ""
        val medId = inputData.getString(KEY_MED_ID) ?: ""
        val frequencyStr = inputData.getString(KEY_FREQUENCY) ?: "Daily"
        val specificDaysStr = inputData.getString(KEY_SPECIFIC_DAYS) ?: ""
        val intervalDays = inputData.getInt(KEY_INTERVAL_DAYS, 0)
        val startDateStr = inputData.getString(KEY_START_DATE) ?: ""

        // Check if medication should be taken today based on frequency
        val frequency = try {
            MedicationFrequency.valueOf(frequencyStr)
        } catch (e: Exception) {
            MedicationFrequency.Daily
        }

        val shouldNotifyToday = shouldTakeToday(
            frequency,
            specificDaysStr,
            intervalDays,
            startDateStr
        )

        if (!shouldNotifyToday) {
            // Skip notification for today
            return Result.success()
        }

        createNotificationChannel()
        showNotification(medicationName, dosage, time, medId)

        return Result.success()
    }

    private fun shouldTakeToday(
        frequency: MedicationFrequency,
        specificDaysStr: String,
        intervalDays: Int,
        startDateStr: String
    ): Boolean {
        val today = LocalDate.now()

        return when (frequency) {
            MedicationFrequency.Daily -> true

            MedicationFrequency.SpecificDays, MedicationFrequency.Weekly -> {
                val specificDays = specificDaysStr.split(",")
                    .filter { it.isNotBlank() }
                    .mapNotNull { it.toIntOrNull() }
                val dayOfWeek = today.dayOfWeek.value // 1=Mon, 7=Sun
                specificDays.contains(dayOfWeek)
            }

            MedicationFrequency.EveryXDays -> {
                if (intervalDays <= 0 || startDateStr.isBlank()) return false
                val startDate = try {
                    LocalDate.parse(startDateStr)
                } catch (e: Exception) {
                    return false
                }
                val daysSinceStart = ChronoUnit.DAYS.between(startDate, today)
                daysSinceStart % intervalDays == 0L
            }

            MedicationFrequency.AsNeeded -> false
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Medication Reminders"
            val descriptionText = "Notifications for medication times"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(medicationName: String, dosage: String, time: String, medId: String) {
        // Check for notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Permission not granted, cannot show notification
                return
            }
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            medId.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.
            drawable.ic_launcher_foreground)
            .setContentTitle("Time for your medication")
            .setContentText("$medicationName $dosage at $time")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$medicationName $dosage at $time"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        NotificationManagerCompat.from(applicationContext).notify(
            medId.hashCode(),
            notification
        )
    }

    companion object {
        const val CHANNEL_ID = "medication_reminders"
        const val KEY_MEDICATION_NAME = "medication_name"
        const val KEY_DOSAGE = "dosage"
        const val KEY_TIME = "time"
        const val KEY_MED_ID = "med_id"
        const val KEY_FREQUENCY = "frequency"
        const val KEY_SPECIFIC_DAYS = "specific_days"
        const val KEY_INTERVAL_DAYS = "interval_days"
        const val KEY_START_DATE = "start_date"
    }
}
