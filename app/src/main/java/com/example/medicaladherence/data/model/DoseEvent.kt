package com.example.medicaladherence.data.model

import java.time.LocalDate

/**
 * Records whether a specific dose was taken or missed.
 *
 * @property medId ID of the medication this event belongs to
 * @property date Date when the dose was scheduled
 * @property time Scheduled time in HH:mm format
 * @property taken Whether the dose was taken (true) or missed (false)
 */
data class DoseEvent(
    val medId: String,
    val date: LocalDate,
    val time: String, // HH:mm format
    val taken: Boolean
)
