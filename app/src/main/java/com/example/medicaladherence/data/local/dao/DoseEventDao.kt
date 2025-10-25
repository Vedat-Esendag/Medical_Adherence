package com.example.medicaladherence.data.local.dao

import androidx.room.*
import com.example.medicaladherence.data.local.entity.DoseEventEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface DoseEventDao {

    @Query("SELECT * FROM dose_events WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC, time ASC")
    fun getEventsInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<DoseEventEntity>>

    @Query("SELECT * FROM dose_events WHERE date = :date")
    fun getEventsForDate(date: LocalDate): Flow<List<DoseEventEntity>>

    @Query("SELECT * FROM dose_events WHERE medId = :medicationId AND date = :date AND time = :time")
    suspend fun getEvent(medicationId: String, date: LocalDate, time: String): DoseEventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: DoseEventEntity)

    @Delete
    suspend fun deleteEvent(event: DoseEventEntity)

    @Query("DELETE FROM dose_events WHERE medId = :medicationId")
    suspend fun deleteEventsForMedication(medicationId: String)
}
