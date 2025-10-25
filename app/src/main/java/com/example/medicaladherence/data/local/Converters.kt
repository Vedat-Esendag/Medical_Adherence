package com.example.medicaladherence.data.local

import androidx.room.TypeConverter
import com.example.medicaladherence.data.model.MedicationFrequency
import java.time.LocalDate

class Converters {

    // String List converter
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return value.joinToString(",")
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return if (value.isEmpty()) emptyList() else value.split(",")
    }

    // Int List converter
    @TypeConverter
    fun fromIntList(value: List<Int>): String {
        return value.joinToString(",")
    }

    @TypeConverter
    fun toIntList(value: String): List<Int> {
        return if (value.isEmpty()) {
            emptyList()
        } else {
            value.split(",").map { it.toInt() }
        }
    }

    // LocalDate converter
    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? {
        return value?.toString()
    }

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? {
        return value?.let { LocalDate.parse(it) }
    }

    // MedicationFrequency converter
    @TypeConverter
    fun fromMedicationFrequency(value: MedicationFrequency): String {
        return value.name
    }

    @TypeConverter
    fun toMedicationFrequency(value: String): MedicationFrequency {
        return MedicationFrequency.valueOf(value)
    }
}
