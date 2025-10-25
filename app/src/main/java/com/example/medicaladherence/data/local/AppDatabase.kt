package com.example.medicaladherence.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.medicaladherence.data.local.dao.DoseEventDao
import com.example.medicaladherence.data.local.dao.MedicationDao
import com.example.medicaladherence.data.local.dao.SettingsDao
import com.example.medicaladherence.data.local.entity.DoseEventEntity
import com.example.medicaladherence.data.local.entity.MedicationEntity
import com.example.medicaladherence.data.local.entity.SettingsEntity

@Database(
    entities = [
        MedicationEntity::class,
        DoseEventEntity::class,
        SettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun medicationDao(): MedicationDao
    abstract fun doseEventDao(): DoseEventDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "medical_adherence_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
