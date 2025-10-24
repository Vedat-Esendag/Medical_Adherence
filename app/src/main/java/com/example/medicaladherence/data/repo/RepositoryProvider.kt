package com.example.medicaladherence.data.repo

/**
 * Singleton provider for the medication repository.
 * This ensures all ViewModels share the same repository instance.
 */
object RepositoryProvider {
    val repository: InMemoryMedicationRepository by lazy {
        InMemoryMedicationRepository()
    }
}
