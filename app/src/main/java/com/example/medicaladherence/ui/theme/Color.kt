package com.example.medicaladherence.ui.theme

import androidx.compose.ui.graphics.Color

// Calm blue color scheme (fallback when dynamic colors not available)
val CalmBlue80 = Color(0xFFB3D9FF)
val CalmBlueGrey80 = Color(0xFFBFC9D9)
val CalmAccent80 = Color(0xFFB8D4EF)

val CalmBlue40 = Color(0xFF4A90E2)
val CalmBlueGrey40 = Color(0xFF5C6B7D)
val CalmAccent40 = Color(0xFF6B9FD4)

// Legacy colors (kept for reference)
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// High Contrast Theme Colors
val HighContrastBackground = Color(0xFF000000) // Pure black
val HighContrastSurface = Color(0xFF1A1A1A) // Very dark gray
val HighContrastPrimary = Color(0xFFFFFFFF) // Pure white
val HighContrastOnPrimary = Color(0xFF000000) // Black text on white
val HighContrastSecondary = Color(0xFFFFEB3B) // Bright yellow
val HighContrastOnSecondary = Color(0xFF000000) // Black text on yellow
val HighContrastError = Color(0xFFFF0000) // Bright red
val HighContrastOnBackground = Color(0xFFFFFFFF) // White text
val HighContrastOnSurface = Color(0xFFFFFFFF) // White text
val HighContrastOutline = Color(0xFFFFFFFF) // White borders

// Adherence Level Colors (semantic naming)
object AdherenceColors {
    // Background tints for adherence cards
    val ExcellentBg = Color(0xFFE8F5E9)       // Light green
    val GoodBg = Color(0xFFFFF9C4)            // Light yellow
    val WarningBg = Color(0xFFFFEBEE)         // Light red
    
    // Text/foreground colors for adherence levels
    val ExcellentText = Color(0xFF2E7D32)     // Dark green
    val GoodText = Color(0xFFEF6C00)          // Orange
    val WarningText = Color(0xFFC62828)       // Dark red
}