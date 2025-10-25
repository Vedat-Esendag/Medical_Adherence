package com.example.medicaladherence.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "dose_events")
data class DoseEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val medId: String,
    val date: LocalDate,
    val time: String, // HH:mm format
    val taken: Boolean
)
