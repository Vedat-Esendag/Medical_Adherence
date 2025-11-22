package com.example.medicaladherence.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicaladherence.data.model.Medication
import com.example.medicaladherence.data.repository.RepositoryProvider
import com.example.medicaladherence.data.repository.FirebaseMedicationRepository
import com.example.medicaladherence.notification.NotificationScheduler
import com.example.medicaladherence.utils.AppConstants
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class HomeUiState(
    val todayDate: LocalDate = LocalDate.now(),
    val nextDoseCountdown: String = "--:--",
    val nextDoseName: String = "",
    val nextDoseDosage: String = "",
    val nextDoseTime: String = "",
    val todayDoses: List<DoseItem> = emptyList(),
    val weeklyAdherencePercent: Int = 0,
    val streakDays: Int = 0,
    val snackbarMessage: String? = null,
    val isInDoseWindow: Boolean = false,
    val nextDoseMedicationId: String = "",
    val lastMarkedDose: Pair<String, String>? = null // medId to time
)

data class DoseItem(
    val medication: Medication,
    val time: String,
    val taken: Boolean?
)

/**
 * ViewModel for the home screen, managing dose tracking and countdown display.
 * 
 * Key Features:
 * - Real-time countdown to next scheduled dose
 * - Today's dose list with taken/missed status
 * - Quick actions: Mark taken, Mark missed, Snooze
 * - Undo functionality for recent actions
 * - Weekly adherence summary
 * - Current streak tracking
 * - Dose window detection (±30 minutes from scheduled time)
 * 
 * @param repository The medication data repository
 */
class HomeViewModel(
    private val repository: FirebaseMedicationRepository = RepositoryProvider.getRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private lateinit var notificationScheduler: NotificationScheduler

    /**
     * Initializes the ViewModel with notification scheduler and starts data loading.
     * Must be called once when the screen is first created.
     * 
     * @param context Application context for notification scheduling
     */
    fun initialize(context: Context) {
        notificationScheduler = NotificationScheduler(context)
    }

    init {
        loadData()
        startCountdownTimer()
    }

    private fun loadData() {
        viewModelScope.launch {
            // Combine data from repository
            val doses = repository.getTodayDoses().map { (med, time, taken) ->
                DoseItem(medication = med, time = time, taken = taken)
            }

            _uiState.value = _uiState.value.copy(
                todayDoses = doses,
                weeklyAdherencePercent = repository.calculateWeeklyAdherence(),
                streakDays = repository.calculateStreak()
            )
        }
    }

    private fun startCountdownTimer() {
        viewModelScope.launch {
            while (true) {
                updateCountdown()
                delay(1000) // Update every second
            }
        }
    }

    private fun updateCountdown() {
        val now = LocalTime.now()
        val formatter = DateTimeFormatter.ofPattern("HH:mm")

        // Find next FUTURE dose (not taken and time is after now)
        val nextDose = _uiState.value.todayDoses
            .filter { it.taken != true } // Not marked as taken
            .filter {
                val doseTime = LocalTime.parse(it.time, formatter)
                doseTime.isAfter(now) // ONLY future doses
            }
            .minByOrNull {
                LocalTime.parse(it.time, formatter)
            }

        if (nextDose != null) {
            val doseTime = LocalTime.parse(nextDose.time, formatter)
            val duration = java.time.Duration.between(now, doseTime)
            val hours = duration.toHours()
            val minutes = duration.toMinutes() % 60

            val countdown = if (hours > 0) {
                String.format("%d:%02d", hours, minutes)
            } else {
                String.format("%02d:%02d", 0, minutes)
            }

            // Check if within dose window
            val totalMinutes = duration.toMinutes()
            val isInWindow = totalMinutes <= AppConstants.DOSE_WINDOW_MINUTES && totalMinutes >= -AppConstants.DOSE_WINDOW_MINUTES

            _uiState.value = _uiState.value.copy(
                nextDoseCountdown = countdown,
                nextDoseName = nextDose.medication.name,
                nextDoseDosage = nextDose.medication.dosage,
                nextDoseTime = nextDose.time,
                isInDoseWindow = isInWindow,
                nextDoseMedicationId = nextDose.medication.id
            )
        } else {
            // No future doses today
            _uiState.value = _uiState.value.copy(
                nextDoseCountdown = "All done!",
                nextDoseName = "",
                nextDoseDosage = "",
                nextDoseTime = "",
                isInDoseWindow = false,
                nextDoseMedicationId = ""
            )
        }
    }

    /**
     * Marks a scheduled dose as taken.
     * 
     * @param medId The medication's unique identifier
     * @param time The scheduled time in HH:mm format
     */
    fun markTaken(medId: String, time: String) {
        viewModelScope.launch {
            repository.markDose(medId, LocalDate.now(), time, taken = true)
            _uiState.value = _uiState.value.copy(lastMarkedDose = medId to time)
            loadData()
            showSnackbar("Dose marked as taken")
        }
    }

    /**
     * Marks a scheduled dose as missed.
     * 
     * @param medId The medication's unique identifier
     * @param time The scheduled time in HH:mm format
     */
    fun markMissed(medId: String, time: String) {
        viewModelScope.launch {
            repository.markDose(medId, LocalDate.now(), time, taken = false)
            _uiState.value = _uiState.value.copy(lastMarkedDose = medId to time)
            loadData()
            showSnackbar("Dose marked as missed")
        }
    }

    /**
     * Snoozes a scheduled dose reminder for 15 minutes.
     * The dose remains pending but the notification is rescheduled.
     * 
     * @param medId The medication's unique identifier
     * @param time The scheduled time in HH:mm format
     */
    fun snooze15(medId: String, time: String) {
        repository.snooze(medId, time, AppConstants.SNOOZE_DURATION_MINUTES)
        showSnackbar("Snoozed for ${AppConstants.SNOOZE_DURATION_MINUTES} minutes")
    }

    /**
     * Undoes a dose action, resetting it back to pending status.
     * 
     * @param medId The medication's unique identifier
     * @param time The scheduled time in HH:mm format
     */
    fun undoDose(medId: String, time: String) {
        viewModelScope.launch {
            repository.undoDose(medId, LocalDate.now(), time)
            _uiState.value = _uiState.value.copy(lastMarkedDose = null)
            loadData()
            showSnackbar("Action undone")
        }
    }

    /**
     * Undoes the most recent dose action (taken or missed).
     * Uses the tracked last marked dose from the UI state.
     */
    fun undoLastMarkedDose() {
        val lastMarked = _uiState.value.lastMarkedDose ?: return
        undoDose(lastMarked.first, lastMarked.second)
    }

    fun deleteMedication(medId: String) {
        viewModelScope.launch {
            // Cancel notifications first
            if (::notificationScheduler.isInitialized) {
                notificationScheduler.cancelNotifications(medId)
            }

            // Then delete medication
            repository.deleteMedication(medId)
            loadData()
            showSnackbar("Medication deleted")
        }
    }

    private fun showSnackbar(message: String) {
        _uiState.value = _uiState.value.copy(snackbarMessage = message)
    }

    /**
     * Clears the current snackbar message from the UI state.
     */
    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    fun refresh() {
        loadData()
    }
}
