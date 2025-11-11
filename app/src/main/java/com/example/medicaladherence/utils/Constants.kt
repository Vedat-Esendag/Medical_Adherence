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
    
    // Trend Analysis
    const val TREND_THRESHOLD_PERCENT = 10
    
    // Reporting Limits
    const val RECENT_MISSED_DOSES_LIMIT = 10

    // FCM Configuration
    const val FCM_CHANNEL_ID = "caregiver_messages"
    const val FCM_CHANNEL_NAME = "Caregiver Messages"
    const val FCM_NOTIFICATION_ID = 1001

    // FCM Direct API Configuration (added for Spark plan compatibility)
    // TODO: Get from Firebase Console → Project Settings → Cloud Messaging → Server key
    const val FCM_SERVER_KEY = "AIzaSyBUkMOF_pUBlEO4k3jWZKHm52lWtiVFrTQ"
    const val FCM_SEND_URL = "https://fcm.googleapis.com/fcm/send"

    // Toggle between different FCM approaches
    // USE_LOCAL_SERVER: true = use local Express server (for development/testing)
    // USE_CLOUD_FUNCTIONS: true = use Cloud Functions (requires Blaze plan)
    // Both false: use Direct HTTP API (deprecated, works on Spark plan)
    const val USE_LOCAL_SERVER = true
    const val USE_CLOUD_FUNCTIONS = false

    // User Role Constants (to prevent typos and ensure consistency)
    const val ROLE_PATIENT = "patient"
    const val ROLE_CAREGIVER = "caregiver"
}

