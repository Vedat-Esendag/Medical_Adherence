package com.example.medicaladherence.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.medicaladherence.data.local.Converters
import com.example.medicaladherence.data.model.MedicationFrequency

@Entity(tableName = "medications")
@TypeConverters(Converters::class)
data class MedicationEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val dosage: String,
    val times: List<String>, // Will be converted by Converters
    val notes: String?,
    val frequency: MedicationFrequency,
    val specificDays: List<Int> // Will be converted by Converters
)
