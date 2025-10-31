package com.example.medicaladherence.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalContext

// CompositionLocal for font scale
val LocalFontScale = compositionLocalOf { 1.0f }

private val DarkColorScheme = darkColorScheme(
    primary = CalmBlue80,
    secondary = CalmBlueGrey80,
    tertiary = CalmAccent80
)

private val LightColorScheme = lightColorScheme(
    primary = CalmBlue40,
    secondary = CalmBlueGrey40,
    tertiary = CalmAccent40
)

private val HighContrastColorScheme = darkColorScheme(
    primary = HighContrastPrimary,
    onPrimary = HighContrastOnPrimary,
    primaryContainer = HighContrastSurface,
    onPrimaryContainer = HighContrastOnBackground,
    secondary = HighContrastSecondary,
    onSecondary = HighContrastOnSecondary,
    secondaryContainer = HighContrastSurface,
    onSecondaryContainer = HighContrastOnBackground,
    tertiary = HighContrastSecondary,
    onTertiary = HighContrastOnSecondary,
    tertiaryContainer = HighContrastSurface,
    onTertiaryContainer = HighContrastOnBackground,
    error = HighContrastError,
    onError = HighContrastOnPrimary,
    errorContainer = HighContrastSurface,
    onErrorContainer = HighContrastError,
    background = HighContrastBackground,
    onBackground = HighContrastOnBackground,
    surface = HighContrastSurface,
    onSurface = HighContrastOnSurface,
    surfaceVariant = HighContrastSurface,
    onSurfaceVariant = HighContrastOnSurface,
    outline = HighContrastOutline,
    outlineVariant = HighContrastOutline,
)

@Composable
fun MedicalAdherenceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    highContrastMode: Boolean = false,
    dynamicColor: Boolean = true,
    fontScale: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        highContrastMode -> HighContrastColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val typography = if (highContrastMode) {
        getScaledTypography(fontScale, highContrast = true)
    } else {
        getScaledTypography(fontScale)
    }

    CompositionLocalProvider(LocalFontScale provides fontScale) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}