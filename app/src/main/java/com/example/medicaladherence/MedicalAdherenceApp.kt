package com.example.medicaladherence

import android.app.Application
import androidx.lifecycle.lifecycleScope
import com.example.medicaladherence.data.SeedData
import com.example.medicaladherence.data.repo.RepositoryProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MedicalAdherenceApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()

        // Initialize repository and seed data
        applicationScope.launch {
            val repository = RepositoryProvider.provideRepository(applicationContext)
            repository.seedDataIfEmpty(SeedData.getMedicationsSeed())
        }
    }
}
