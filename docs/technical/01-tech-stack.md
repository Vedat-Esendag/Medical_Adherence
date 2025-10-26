# Tech Stack Reference

## TL;DR
Modern Android stack: Kotlin 2.0.21, Jetpack Compose, Material 3, Room 2.6.1, Navigation Compose 2.8.5, Coroutines 1.9.0, targeting Android 15+ (SDK 36).

## Core Technologies

### Language & SDK
- **Kotlin**: 2.0.21
- **Java**: Target version 11 (JVM)
- **Min SDK**: 29 (Android 10)
- **Target SDK**: 36 (Android 15+)
- **Compile SDK**: 36

Location: `app/build.gradle.kts:10, 34-36`

### Build System
- **Gradle**: 8.13.0 (Android Gradle Plugin)
- **Kotlin DSL**: For build scripts
- **KSP**: 2.0.21-1.0.25 (Kotlin Symbol Processing)

Location: `build.gradle.kts:2-6`

## UI Framework

### Jetpack Compose
- **Compose BOM**: 2024.09.00
- **Compiler**: Kotlin Compose Plugin 2.0.21
- **Components**:
  - `compose.ui`: Core UI primitives
  - `compose.ui.graphics`: Graphics rendering
  - `compose.ui.tooling`: Preview support
  - `compose.material3`: Material Design 3

Location: `gradle/libs.versions.toml:10`, `app/build.gradle.kts:48-52`

### Material Design 3
- Latest Material 3 components
- Dynamic color support (Android 12+)
- Theming system with custom colors
- Accessibility-first components

Location: `app/src/main/java/com/example/medicaladherence/ui/theme/`

## Architecture Components

### Lifecycle & ViewModel
- **Lifecycle Runtime**: 2.9.4
- **ViewModel Compose**: 2.9.4
- **Runtime Compose**: 2.9.4 (for `collectAsState()`)

Location: `gradle/libs.versions.toml:8, 12`

### Navigation
- **Navigation Compose**: 2.8.5
- Single Activity with composable destinations
- Type-safe navigation arguments

Location: `gradle/libs.versions.toml:11`

## Data Persistence

### Room Database
- **Version**: 2.6.1
- **Components**:
  - `room-runtime`: Core library
  - `room-ktx`: Kotlin extensions + Coroutines
  - `room-compiler`: Annotation processor (KSP)

Location: `app/build.gradle.kts:65-67`

### Type Converters
- Custom converters for `List<String>`, `List<Int>`
- Enum support for `MedicationFrequency`

Location: `app/src/main/java/com/example/medicaladherence/data/local/Converters.kt`

## Asynchronous Programming

### Kotlin Coroutines
- **Version**: 1.9.0
- **Library**: `kotlinx-coroutines-android`
- **Usage**: All async operations (Room queries, timers, etc.)

Location: `gradle/libs.versions.toml:13`

### Flow
- `StateFlow` for reactive UI state
- Room DAOs return `Flow<T>` for reactive queries
- `collectAsState()` in Compose

## Testing Libraries

### Unit Testing
- **JUnit**: 4.13.2
- Test coroutines with `runTest`

### Android Testing
- **AndroidX JUnit**: 1.3.0
- **Espresso Core**: 3.7.0
- **Compose UI Test**: via BOM

Location: `gradle/libs.versions.toml:5-7`, `app/build.gradle.kts:69-75`

## Development Tools

### Android Studio
- Recommended: Hedgehog (2023.1.1) or later
- Compose preview support
- Live literals
- Layout inspector for Compose

### Kotlin Symbol Processing (KSP)
- Replaces KAPT for Room
- Faster annotation processing
- Better IDE support

Location: `build.gradle.kts:6`, `app/build.gradle.kts:5`

## Dependencies Overview

### Version Catalog
All dependencies managed in `gradle/libs.versions.toml`:

```toml
[versions]
kotlin = "2.0.21"
compose-bom = "2024.09.00"
room = "2.6.1" (in app/build.gradle.kts)
navigation = "2.8.5"
coroutines = "1.9.0"
```

### Key Libraries
```kotlin
// UI
implementation(platform(libs.androidx.compose.bom))
implementation(libs.androidx.compose.material3)
implementation(libs.androidx.navigation.compose)

// Architecture
implementation(libs.androidx.lifecycle.viewmodel.compose)
implementation(libs.androidx.lifecycle.runtime.compose)

// Data
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
ksp("androidx.room:room-compiler:2.6.1")

// Async
implementation(libs.kotlinx.coroutines.android)
```

Location: `app/build.gradle.kts:43-67`

## Build Features

### Enabled Features
```kotlin
buildFeatures {
    compose = true
}
```

Location: `app/build.gradle.kts:38-40`

### ProGuard
- Minification: Disabled in release build
- ProGuard rules: `proguard-rules.pro`

Location: `app/build.gradle.kts:22-29`

## Gradle Plugins

### Applied Plugins
```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}
```

Location: `app/build.gradle.kts:1-6`

## Version Management

### Semantic Versioning
- **versionCode**: 1 (increment for each release)
- **versionName**: "1.0" (user-facing version)

Location: `app/build.gradle.kts:16-17`

## Kotlin Features Used

### Language Features
- Data classes
- Sealed classes (not currently used, but available)
- Coroutines with structured concurrency
- Extension functions
- Null safety
- Lambdas and higher-order functions

### Kotlin DSL
- Type-safe Gradle configuration
- Cleaner syntax than Groovy

## Android Features

### Modern Android APIs
- **Edge-to-edge**: `enableEdgeToEdge()` in MainActivity
- **Material 3**: Latest design system
- **Jetpack Compose**: Declarative UI
- **Navigation Compose**: Type-safe navigation

### Minimum API Level (29)
Provides access to:
- Material Design components
- Jetpack Compose (requires API 21+)
- Modern lifecycle components
- Dark theme support

## External Dependencies

### None Currently
- No third-party libraries (Retrofit, Glide, etc.)
- Pure Jetpack stack
- No analytics or crash reporting
- No backend integration

This keeps the app:
- Lightweight
- Privacy-focused (local only)
- Easy to maintain
- Minimal attack surface

## Upgrade Path

### Staying Current
To update dependencies:
1. Update version catalog: `gradle/libs.versions.toml`
2. Update Room version in `app/build.gradle.kts`
3. Run `./gradlew --refresh-dependencies`
4. Test thoroughly

### Breaking Changes to Watch
- Kotlin version updates (may require code changes)
- Compose BOM updates (Material 3 changes)
- Room migrations (schema changes)
- Navigation Compose API changes

## Build Variants

### Current Setup
- **Debug**: Development builds
- **Release**: Production builds (no minification)

### Build Types
```kotlin
buildTypes {
    release {
        isMinifyEnabled = false
    }
}
```

Location: `app/build.gradle.kts:22-29`

## Performance Optimizations

### Compiler Optimizations
- Kotlin Compose Compiler plugin
- KSP for faster annotation processing
- R8 available (currently disabled)

### Runtime
- Coroutines for efficient async
- Flow for backpressure handling
- Room caching and query optimization
