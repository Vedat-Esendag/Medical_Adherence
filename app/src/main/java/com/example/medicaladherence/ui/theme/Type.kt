package com.example.medicaladherence.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Base typography (scale = 1.0)
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp,
        lineHeight = 52.sp
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

/**
 * Returns a Typography instance scaled by the given factor and optionally with bold text for high contrast
 */
fun getScaledTypography(scale: Float, highContrast: Boolean = false): Typography {
    val fontWeight = if (highContrast) FontWeight.Bold else FontWeight.Normal
    val headingWeight = if (highContrast) FontWeight.ExtraBold else FontWeight.Normal
    val mediumWeight = if (highContrast) FontWeight.ExtraBold else FontWeight.Medium

    return Typography(
        displayLarge = Typography.displayLarge.copy(
            fontSize = Typography.displayLarge.fontSize * scale,
            fontWeight = headingWeight
        ),
        displayMedium = Typography.displayMedium.copy(
            fontSize = Typography.displayMedium.fontSize * scale,
            fontWeight = headingWeight
        ),
        displaySmall = Typography.displaySmall.copy(
            fontSize = Typography.displaySmall.fontSize * scale,
            fontWeight = headingWeight
        ),
        headlineLarge = Typography.headlineLarge.copy(
            fontSize = Typography.headlineLarge.fontSize * scale,
            fontWeight = headingWeight
        ),
        headlineMedium = Typography.headlineMedium.copy(
            fontSize = Typography.headlineMedium.fontSize * scale,
            fontWeight = headingWeight
        ),
        headlineSmall = Typography.headlineSmall.copy(
            fontSize = Typography.headlineSmall.fontSize * scale,
            fontWeight = headingWeight
        ),
        titleLarge = Typography.titleLarge.copy(
            fontSize = Typography.titleLarge.fontSize * scale,
            fontWeight = mediumWeight
        ),
        titleMedium = Typography.titleMedium.copy(
            fontSize = Typography.titleMedium.fontSize * scale,
            fontWeight = mediumWeight
        ),
        titleSmall = Typography.titleSmall.copy(
            fontSize = Typography.titleSmall.fontSize * scale,
            fontWeight = mediumWeight
        ),
        bodyLarge = Typography.bodyLarge.copy(
            fontSize = Typography.bodyLarge.fontSize * scale,
            fontWeight = fontWeight
        ),
        bodyMedium = Typography.bodyMedium.copy(
            fontSize = Typography.bodyMedium.fontSize * scale,
            fontWeight = fontWeight
        ),
        bodySmall = Typography.bodySmall.copy(
            fontSize = Typography.bodySmall.fontSize * scale,
            fontWeight = fontWeight
        ),
        labelLarge = Typography.labelLarge.copy(
            fontSize = Typography.labelLarge.fontSize * scale,
            fontWeight = mediumWeight
        ),
        labelMedium = Typography.labelMedium.copy(
            fontSize = Typography.labelMedium.fontSize * scale,
            fontWeight = mediumWeight
        ),
        labelSmall = Typography.labelSmall.copy(
            fontSize = Typography.labelSmall.fontSize * scale,
            fontWeight = mediumWeight
        )
    )
}