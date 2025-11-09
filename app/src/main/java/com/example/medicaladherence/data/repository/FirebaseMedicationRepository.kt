package com.example.medicaladherence.data.repository

import com.example.medicaladherence.data.firebase.*
import com.example.medicaladherence.data.model.*
import com.example.medicaladherence.utils.AppConstants
import com.example.medicaladherence.viewmodel.MedicationAdherence
import com.example.medicaladherence.viewmodel.MissedDoseInfo
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class FirebaseMedicationRepository(
    private val firestore: FirebaseFirestore,
    private val authManager: FirebaseAuthManager
) {

    // Current user ID (Firebase anonymous auth)
    private suspend fun getCurrentUserId(): String {
        return authManager.ensureAuthenticated()
    }

    // Current user's Firestore document reference
    private suspend fun getCurrentUserDoc() = firestore
        .collection("users")
        .document(getCurrentUserId())

    // ========== USER PROFILE ==========

    suspend fun getCurrentUserProfile(): FirestoreUserProfile? {
        return try {
            getCurrentUserDoc()
                .get()
                .await()
                .toObject(FirestoreUserProfile::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun setUserProfile(role: String, name: String, pin: String) {
        try {
            val userId = getCurrentUserId()
            val profile = FirestoreUserProfile(
                userId = userId,
                role = role,
                name = name,
                pin = pin
            )
            getCurrentUserDoc().set(profile).await()
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepo", "❌ Error saving profile", e)
            throw e
        }
    }

    suspend fun getCurrentPatientPin(): String {
        val profile = getCurrentUserProfile()
        return profile?.pin ?: "default"
    }

    suspend fun deleteUserProfile() {
        try {
            getCurrentUserDoc().delete().await()
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepo", "Error deleting profile", e)
            throw e
        }
    }

    // ========== MEDICATIONS ==========

    val medications: Flow<List<Medication>> = flow {
        val userId = getCurrentUserId()
        firestore.collection("users/$userId/medications")
            .asFlow { doc -> doc.toObject(FirestoreMedication::class.java)?.toMedication() }
            .collect { medications ->
                emit(medications)
            }
    }.catch { e ->
        android.util.Log.e("FirebaseRepo", "Error loading medications", e)
        emit(emptyList())
    }

    fun getMedicationsForPatient(pin: String): Flow<List<Medication>> {
        // Query users by PIN, then get their medications
        // For MVP, we'll use current user's medications
        // (Pin-based cross-user queries need more setup)
        return medications
    }

    suspend fun getMedicationById(id: String): Medication? {
        return try {
            getCurrentUserDoc()
                .collection("medications")
                .document(id)
                .get()
                .await()
                .toObject(FirestoreMedication::class.java)
                ?.toMedication()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun addOrUpdateMedication(medication: Medication) {
        try {
            val firestoreMed = FirestoreMedication.fromMedication(medication)
            getCurrentUserDoc()
                .collection("medications")
                .document(medication.id)
                .set(firestoreMed)
                .await()
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepo", "Error saving medication: ${medication.name}", e)
            throw e
        }
    }

    suspend fun addMedicationForCurrentPatient(medication: Medication) {
        addOrUpdateMedication(medication)
    }

    suspend fun deleteMedication(medId: String) {
        val userDoc = getCurrentUserDoc()

        // Delete medication
        userDoc.collection("medications")
            .document(medId)
            .delete()
            .await()

        // Delete associated dose events
        val events = userDoc.collection("doseEvents")
            .whereEqualTo("medId", medId)
            .get()
            .await()

        val batch = firestore.batch()
        events.documents.forEach { doc ->
            batch.delete(doc.reference)
        }
        batch.commit().await()
    }

    // ========== DOSE EVENTS ==========

    suspend fun markDose(medId: String, date: LocalDate, time: String, taken: Boolean) {
        val event = DoseEvent(medId, date, time, taken)
        val firestoreEvent = FirestoreDoseEvent.fromDoseEvent(event)

        getCurrentUserDoc()
            .collection("doseEvents")
            .document(firestoreEvent.id)
            .set(firestoreEvent)
            .await()
    }

    suspend fun markDoseForCurrentPatient(medId: String, date: LocalDate, time: String, taken: Boolean) {
        markDose(medId, date, time, taken)
    }

    suspend fun undoDose(medId: String, date: LocalDate, time: String) {
        val eventId = "${medId}_${date}_${time}"
        getCurrentUserDoc()
            .collection("doseEvents")
            .document(eventId)
            .delete()
            .await()
    }

    private suspend fun getDoseEvents(startDate: LocalDate, endDate: LocalDate): List<DoseEvent> {
        return try {
            getCurrentUserDoc()
                .collection("doseEvents")
                .whereGreaterThanOrEqualTo("date", startDate.toString())
                .whereLessThanOrEqualTo("date", endDate.toString())
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(FirestoreDoseEvent::class.java)?.toDoseEvent() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getDoseEventsForPatient(pin: String, startDate: LocalDate, endDate: LocalDate): Flow<List<DoseEvent>> {
        // For MVP, return current user's events
        // TODO: Implement cross-user queries for caregiver mode
        return flow {
            emit(getDoseEvents(startDate, endDate))
        }
    }

    suspend fun getTodayDoses(): List<Triple<Medication, String, Boolean?>> {
        val today = LocalDate.now()
        val medicationsList = medications.first()
        val todayEvents = getDoseEvents(today, today)

        return medicationsList.flatMap { med ->
            med.times.map { time ->
                val event = todayEvents.find { it.medId == med.id && it.time == time }
                Triple(med, time, event?.taken)
            }
        }.sortedBy { (_, time, _) ->
            LocalTime.parse(time)
        }
    }

    suspend fun getTodayDosesForPatientByPin(pin: String): List<Triple<Medication, String, Boolean?>> {
        val today = LocalDate.now()
        val medicationsList = getMedicationsForPatientByPin(pin).first()
        val todayEvents = getDoseEventsForPatientByPin(pin, today, today)

        return medicationsList.flatMap { med ->
            med.times.map { time ->
                val event = todayEvents.find { it.medId == med.id && it.time == time }
                Triple(med, time, event?.taken)
            }
        }.sortedBy { (_, time, _) ->
            LocalTime.parse(time)
        }
    }

    // ========== STATISTICS ==========

    suspend fun calculateWeeklyAdherence(): Int {
        val today = LocalDate.now()
        val weekAgo = today.minusDays((AppConstants.DAYS_IN_WEEK - 1).toLong())
        val events = getDoseEvents(weekAgo, today)

        if (events.isEmpty()) return 0

        val takenCount = events.count { it.taken }
        return ((takenCount.toFloat() / events.size) * 100).toInt()
    }

    suspend fun calculateMonthlyAdherence(): Int {
        val today = LocalDate.now()
        val monthAgo = today.minusDays((AppConstants.DAYS_IN_MONTH - 1).toLong())
        return calculateAdherenceForPeriod(monthAgo, today)
    }

    suspend fun calculateAdherenceForPeriod(startDate: LocalDate, endDate: LocalDate): Int {
        val events = getDoseEvents(startDate, endDate)
        if (events.isEmpty()) return 0

        val takenCount = events.count { it.taken }
        return ((takenCount.toFloat() / events.size) * 100).toInt()
    }

    suspend fun calculateStreak(): Int {
        val today = LocalDate.now()
        var streak = 0
        var currentDate = today.minusDays(1)

        while (true) {
            val dayEvents = getDoseEvents(currentDate, currentDate)

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

    suspend fun calculateLongestStreak(): Int {
        val today = LocalDate.now()
        var longestStreak = 0
        var currentStreak = 0
        var checkDate = today.minusDays(AppConstants.STREAK_LOOKBACK_DAYS.toLong())

        while (checkDate.isBefore(today) || checkDate.isEqual(today)) {
            val dayEvents = getDoseEvents(checkDate, checkDate)

            if (dayEvents.isEmpty()) {
                checkDate = checkDate.plusDays(1)
                continue
            }

            if (dayEvents.all { it.taken }) {
                currentStreak++
                if (currentStreak > longestStreak) {
                    longestStreak = currentStreak
                }
            } else {
                currentStreak = 0
            }

            checkDate = checkDate.plusDays(1)
        }

        return longestStreak
    }

    suspend fun getDailyAdherenceForWeek(): Map<LocalDate, Int> {
        val today = LocalDate.now()
        val result = mutableMapOf<LocalDate, Int>()

        for (dayOffset in (AppConstants.DAYS_IN_WEEK - 1) downTo 0) {
            val date = today.minusDays(dayOffset.toLong())
            val dayEvents = getDoseEvents(date, date)

            val percentage = if (dayEvents.isEmpty()) {
                0
            } else {
                ((dayEvents.count { it.taken }.toFloat() / dayEvents.size) * 100).toInt()
            }

            result[date] = percentage
        }

        return result
    }

    suspend fun calculateMedicationAdherence(
        medId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): MedicationAdherence {
        val medication = getMedicationById(medId)
            ?: return MedicationAdherence("Unknown", "", 0, 0, 0)

        val events = getDoseEvents(startDate, endDate).filter { it.medId == medId }

        val totalDoses = events.size
        val takenDoses = events.count { it.taken }
        val percentage = if (totalDoses > 0) {
            ((takenDoses.toFloat() / totalDoses) * 100).toInt()
        } else 0

        return MedicationAdherence(
            medicationName = medication.name,
            dosage = medication.dosage,
            percentage = percentage,
            takenCount = takenDoses,
            totalCount = totalDoses
        )
    }

    suspend fun calculateTimeOfDayInsight(startDate: LocalDate, endDate: LocalDate): String? {
        val events = getDoseEvents(startDate, endDate)

        if (events.isEmpty()) return null

        val morningEvents = events.filter {
            val time = LocalTime.parse(it.time)
            time.isBefore(LocalTime.NOON)
        }
        val eveningEvents = events.filter {
            val time = LocalTime.parse(it.time)
            !time.isBefore(LocalTime.NOON)
        }

        val morningAdherence = if (morningEvents.isNotEmpty()) {
            (morningEvents.count { it.taken }.toFloat() / morningEvents.size * 100).toInt()
        } else 0

        val eveningAdherence = if (eveningEvents.isNotEmpty()) {
            (eveningEvents.count { it.taken }.toFloat() / eveningEvents.size * 100).toInt()
        } else 0

        return when {
            morningAdherence > eveningAdherence + 20 ->
                "You're better at taking morning doses (${morningAdherence}%) than evening doses (${eveningAdherence}%). Set an evening reminder!"
            eveningAdherence > morningAdherence + 20 ->
                "You're better at taking evening doses (${eveningAdherence}%) than morning doses (${morningAdherence}%). Set a morning reminder!"
            morningAdherence >= AppConstants.ADHERENCE_GOOD && eveningAdherence >= AppConstants.ADHERENCE_GOOD ->
                "Excellent! You're consistent with both morning (${morningAdherence}%) and evening (${eveningAdherence}%) doses."
            else -> null
        }
    }

    suspend fun getRecentMissedDoses(days: Int): List<MissedDoseInfo> {
        val today = LocalDate.now()
        val startDate = today.minusDays(days.toLong() - 1)

        val medicationsList = medications.first()
        val missedDoses = mutableListOf<MissedDoseInfo>()

        val events = getDoseEvents(startDate, today).filter { !it.taken }

        events.forEach { event ->
            val medication = medicationsList.find { it.id == event.medId }
            if (medication != null) {
                missedDoses.add(
                    MissedDoseInfo(
                        medicationName = medication.name,
                        dosage = medication.dosage,
                        date = event.date,
                        time = event.time
                    )
                )
            }
        }

        return missedDoses.sortedByDescending { it.date }
    }

    // ========== SETTINGS ==========

    fun getSettings(): Flow<FirestoreSettings?> {
        return flow {
            val user = authManager.currentUser
            if (user == null) {
                emit(null)
                return@flow
            }

            try {
                val settings = firestore.collection("users/${user.uid}/settings")
                    .document("app_settings")
                    .get()
                    .await()
                    .toObject(FirestoreSettings::class.java)
                emit(settings)
            } catch (e: Exception) {
                emit(null)
            }
        }
    }

    suspend fun saveSettings(settings: FirestoreSettings) {
        getCurrentUserDoc()
            .collection("settings")
            .document("app_settings")
            .set(settings, SetOptions.merge())
            .await()
    }

    // ========== EXPORT/IMPORT (QR CODE) ==========

    suspend fun exportPatientData(pin: String, name: String): PatientDataExport {
        val medications = medications.first()

        val endDate = LocalDate.now()
        val startDate = endDate.minusDays(AppConstants.DAYS_IN_MONTH.toLong())
        val doseEvents = getDoseEvents(startDate, endDate)

        // Convert to export format
        val medExports = medications.map { med ->
            MedicationExport(
                id = med.id,
                name = med.name,
                dosage = med.dosage,
                times = med.times,
                notes = med.notes,
                frequency = med.frequency.name,
                specificDays = med.specificDays
            )
        }

        val eventExports = doseEvents.map { event ->
            DoseEventExport(
                id = 0L,  // Not used in Firebase
                medId = event.medId,
                date = event.date.toString(),
                time = event.time,
                taken = event.taken
            )
        }

        return PatientDataExport(
            pin = pin,
            name = name,
            medications = medExports,
            doseEvents = eventExports
        )
    }

    suspend fun getPatientDataByPin(pin: String): PatientDataExport? {
        try {
            // Query users by PIN
            val users = firestore.collection("users")
                .whereEqualTo("pin", pin)
                .whereEqualTo("role", "patient")
                .get()
                .await()

            if (users.isEmpty) {
                return null
            }

            val patientUserId = users.documents.first().id
            val patientName = users.documents.first().getString("name") ?: "Unknown"

            // Get patient's medications
            val medications = firestore.collection("users/$patientUserId/medications")
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(FirestoreMedication::class.java) }

            // Get patient's dose events (last month)
            val endDate = LocalDate.now()
            val startDate = endDate.minusDays(AppConstants.DAYS_IN_MONTH.toLong())
            val doseEvents = firestore.collection("users/$patientUserId/doseEvents")
                .whereGreaterThanOrEqualTo("date", startDate.toString())
                .whereLessThanOrEqualTo("date", endDate.toString())
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(FirestoreDoseEvent::class.java) }

            val medExports = medications.map { med ->
                MedicationExport(
                    id = med.id,
                    name = med.name,
                    dosage = med.dosage,
                    times = med.times,
                    notes = med.notes,
                    frequency = med.frequency,
                    specificDays = med.specificDays
                )
            }

            val eventExports = doseEvents.map { event ->
                DoseEventExport(
                    id = 0L,
                    medId = event.medId,
                    date = event.date,
                    time = event.time,
                    taken = event.taken
                )
            }

            return PatientDataExport(
                pin = pin,
                name = patientName,
                medications = medExports,
                doseEvents = eventExports
            )
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepo", "❌ Error getting patient data by PIN: ${e.message}", e)
            return null
        }
    }

    suspend fun importPatientData(data: PatientDataExport) {
        // Create caregiver link
        val caregiverUserId = getCurrentUserId()
        val patientUserId = firestore.collection("users")
            .whereEqualTo("pin", data.pin)
            .whereEqualTo("role", "patient")
            .get()
            .await()
            .documents
            .firstOrNull()
            ?.id ?: return

        val link = FirestoreCaregiverLink(
            linkId = UUID.randomUUID().toString(),
            caregiverUserId = caregiverUserId,
            patientUserId = patientUserId,
            patientPin = data.pin,
            patientName = data.name
        )

        firestore.collection("caregiver_links")
            .document(link.linkId)
            .set(link)
            .await()
    }

    // ========== CAREGIVER PATIENT MANAGEMENT ==========

    /**
     * Get all patients linked to the current caregiver with real-time updates
     */
    fun getCaregiverPatients(): Flow<List<PatientProfile>> = callbackFlow {
        val caregiverUserId = getCurrentUserId()
        val listener = firestore.collection("caregiver_links")
            .whereEqualTo("caregiverUserId", caregiverUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirebaseRepo", "❌ Error in caregiver patients listener", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val links = snapshot.documents.mapNotNull { 
                    it.toObject(FirestoreCaregiverLink::class.java) 
                }

                // Launch coroutines to fetch medication counts for each patient
                launch(Dispatchers.IO) {
                    val patients = links.map { link ->
                        val medCount = try {
                            val patientUserId = firestore.collection("users")
                                .whereEqualTo("pin", link.patientPin)
                                .whereEqualTo("role", "patient")
                                .get()
                                .await()
                                .documents
                                .firstOrNull()
                                ?.id

                            if (patientUserId != null) {
                                firestore.collection("users/$patientUserId/medications")
                                    .get()
                                    .await()
                                    .documents
                                    .size
                            } else {
                                0
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("FirebaseRepo", "❌ Error fetching med count for ${link.patientName}", e)
                            0
                        }

                        PatientProfile(
                            pin = link.patientPin,
                            name = link.patientName,
                            addedAt = link.addedAt.seconds * 1000,
                            lastSyncedAt = System.currentTimeMillis(),
                            medicationCount = medCount,
                            displayName = link.displayName,
                            phoneNumber = link.phoneNumber,
                            notes = link.notes
                        )
                    }
                    trySend(patients)
                }
            }

        awaitClose {
            listener.remove()
        }
    }.catch { e ->
        android.util.Log.e("FirebaseRepo", "❌ Error in caregiver patients flow", e)
        emit(emptyList())
    }

    /**
     * Get a patient's userId by their PIN
     */
    private suspend fun getPatientUserIdByPin(pin: String): String? {
        return try {
            firestore.collection("users")
                .whereEqualTo("pin", pin)
                .whereEqualTo("role", "patient")
                .get()
                .await()
                .documents
                .firstOrNull()
                ?.id
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepo", "Error getting patient userId by PIN", e)
            null
        }
    }

    /**
     * Get medications for a specific patient (by PIN) with real-time updates
     */
    fun getMedicationsForPatientByPin(pin: String): Flow<List<Medication>> = callbackFlow {
        val patientUserId = getPatientUserIdByPin(pin)
        
        if (patientUserId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("users/$patientUserId/medications")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirebaseRepo", "❌ Error in patient medications listener", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val medications = snapshot.documents.mapNotNull { 
                    it.toObject(FirestoreMedication::class.java)?.toMedication()
                }
                trySend(medications)
            }

        awaitClose {
            listener.remove()
        }
    }.catch { e ->
        android.util.Log.e("FirebaseRepo", "❌ Error in patient medications flow", e)
        emit(emptyList())
    }

    /**
     * Get dose events for a specific patient (by PIN)
     */
    suspend fun getDoseEventsForPatientByPin(pin: String, startDate: LocalDate, endDate: LocalDate): List<DoseEvent> {
        return try {
            val patientUserId = getPatientUserIdByPin(pin) ?: return emptyList()

            firestore.collection("users/$patientUserId/doseEvents")
                .whereGreaterThanOrEqualTo("date", startDate.toString())
                .whereLessThanOrEqualTo("date", endDate.toString())
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(FirestoreDoseEvent::class.java)?.toDoseEvent() }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepo", "Error getting patient dose events", e)
            emptyList()
        }
    }

    /**
     * Calculate adherence for a specific patient
     */
    suspend fun calculatePatientAdherence(pin: String, start: LocalDate, end: LocalDate): Int {
        val patientUserId = getPatientUserIdByPin(pin) ?: return 0

        val doseEvents = firestore.collection("users/$patientUserId/doseEvents")
            .whereGreaterThanOrEqualTo("date", start.toString())
            .whereLessThanOrEqualTo("date", end.toString())
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(FirestoreDoseEvent::class.java)?.toDoseEvent() }

        if (doseEvents.isEmpty()) return 0

        val takenCount = doseEvents.count { it.taken }
        return ((takenCount.toFloat() / doseEvents.size) * 100).toInt()
    }

    /**
     * Remove a patient from the caregiver's list
     */
    suspend fun removePatientFromCaregiver(patientPin: String) {
        val caregiverUserId = getCurrentUserId()

        val links = firestore.collection("caregiver_links")
            .whereEqualTo("caregiverUserId", caregiverUserId)
            .whereEqualTo("patientPin", patientPin)
            .get()
            .await()

        val batch = firestore.batch()
        links.documents.forEach { doc ->
            batch.delete(doc.reference)
        }
        batch.commit().await()
    }

    /**
     * Update patient information (displayName, phoneNumber, notes) in the caregiver link
     */
    suspend fun updatePatientInfo(
        patientPin: String,
        displayName: String?,
        phoneNumber: String?,
        notes: String?
    ) {
        val currentUserId = getCurrentUserId()
        
        // Find the caregiver link document
        val linkQuery = firestore.collection("caregiver_links")
            .whereEqualTo("caregiverUserId", currentUserId)
            .whereEqualTo("patientPin", patientPin)
            .get()
            .await()
        
        linkQuery.documents.firstOrNull()?.reference?.update(
            mapOf(
                "displayName" to displayName,
                "phoneNumber" to phoneNumber,
                "notes" to notes
            )
        )?.await()
    }

    // ========== UTILITY ==========

    suspend fun seedDataIfEmpty(medicationsList: List<Medication>) {
        val existing = medications.first()
        if (existing.isEmpty()) {
            medicationsList.forEach { addOrUpdateMedication(it) }
        }
    }

    fun snooze(medId: String, time: String, minutes: Int) {
        // Placeholder - WorkManager handles this
    }
}
