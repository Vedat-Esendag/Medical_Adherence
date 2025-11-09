package com.example.medicaladherence.data.repo

import android.content.Context
import com.example.medicaladherence.data.firebase.FirebaseAuthManager
import com.example.medicaladherence.data.repository.FirebaseMedicationRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

/**
 * Singleton provider for the medication repository.
 * This ensures all ViewModels share the same repository instance.
 */
object RepositoryProvider {
    @Volatile
    private var repository: FirebaseMedicationRepository? = null

    @Volatile
    private var authManager: FirebaseAuthManager? = null

    fun provideRepository(context: Context): FirebaseMedicationRepository {
        return repository ?: synchronized(this) {
            // Pass context to FirebaseAuthManager for device-specific offline IDs
            val auth = authManager ?: FirebaseAuthManager.getInstance(context).also { authManager = it }

            // Initialize Firestore (offline persistence is enabled by default in newer versions)
            val firestore = FirebaseFirestore.getInstance()

            val newRepo = FirebaseMedicationRepository(firestore, auth)
            repository = newRepo
            newRepo
        }
    }

    /**
     * Get the repository instance. Must be initialized first via provideRepository().
     * Throws exception if repository hasn't been initialized.
     */
    fun getRepository(): FirebaseMedicationRepository {
        return repository ?: throw IllegalStateException(
            "Repository not initialized. Call provideRepository() first."
        )
    }

    /**
     * Get the Firebase auth manager instance.
     * Note: If called before provideRepository(), context won't be available for device-specific IDs
     */
    fun getAuthManager(): FirebaseAuthManager {
        return authManager ?: FirebaseAuthManager.getInstance(null).also { authManager = it }
    }
}
