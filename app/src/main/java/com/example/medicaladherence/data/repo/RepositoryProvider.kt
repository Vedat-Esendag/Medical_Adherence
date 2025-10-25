package com.example.medicaladherence.data.repo

import android.content.Context
import com.example.medicaladherence.data.local.AppDatabase
import com.example.medicaladherence.data.repository.MedicationRepository

/**
 * Singleton provider for the medication repository.
 * This ensures all ViewModels share the same repository instance.
 */
object RepositoryProvider {
    @Volatile
    private var repository: MedicationRepository? = null

    fun provideRepository(context: Context): MedicationRepository {
        return repository ?: synchronized(this) {
            val database = AppDatabase.getDatabase(context)
            val newRepo = MedicationRepository(database)
            repository = newRepo
            newRepo
        }
    }

    /**
     * Get the repository instance. Must be initialized first via provideRepository().
     * Throws exception if repository hasn't been initialized.
     */
    fun getRepository(): MedicationRepository {
        return repository ?: throw IllegalStateException(
            "Repository not initialized. Call provideRepository() first."
        )
    }
}
