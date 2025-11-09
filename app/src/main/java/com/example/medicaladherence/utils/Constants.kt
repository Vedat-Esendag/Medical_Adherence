package com.example.medicaladherence.utils

/**
 * Application-wide constants for configuration and thresholds.
 * Centralizing these values improves maintainability and clarity.
 */
object AppConstants {
    
    // Dose Timing Configuration
    const val DOSE_WINDOW_MINUTES = 30
    const val SNOOZE_DURATION_MINUTES = 15
    
    // Adherence Thresholds (percentages)
    const val ADHERENCE_EXCELLENT = 90
    const val ADHERENCE_GOOD = 80
    const val ADHERENCE_FAIR = 75
    const val ADHERENCE_OKAY = 50
    const val ADHERENCE_PROBLEMATIC = 70
    const val ADHERENCE_WARNING = 30
    
    // Time Period Calculations
    const val DAYS_IN_WEEK = 7
    const val DAYS_IN_MONTH = 30
    const val STREAK_LOOKBACK_DAYS = 90
    
    // PIN Configuration
    const val PIN_LENGTH = 6
    const val PIN_MIN = 100000
    const val PIN_MAX = 999999
    
    // UI Configuration
    const val QR_CODE_SIZE = 512
    const val QR_CODE_MAX_LENGTH = 4000
    
    // StateFlow Configuration
    const val STATEFLOW_TIMEOUT_MS = 5000L
}

