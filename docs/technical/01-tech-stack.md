# Tech Stack Reference

## TL;DR
Modern Android stack: Kotlin 2.0.21, Jetpack Compose, Material 3, Firebase/Firestore, Navigation Compose 2.8.5, Coroutines 1.9.0, WorkManager 2.9.0, targeting Android 15+ (SDK 36).

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

### Firebase Firestore
- **Firebase BOM**: 32.7.0 (Bill of Materials for version management)
- **Components**:
  - `firebase-firestore-ktx`: Cloud NoSQL database with offline support
  - `firebase-auth-ktx`: Anonymous authentication
  - `firebase-messaging`: FCM push notifications (23.4.0)

Location: `app/build.gradle.kts`

### Firestore Features
- Real-time listeners for reactive data updates
- Automatic offline persistence and caching
- Cloud sync across devices
- Subcollection support for hierarchical data
- Built-in conflict resolution

### Coroutines Support
- **kotlinx-coroutines-play-services**: 1.7.3
- Enables `await()` extension on Firebase Tasks
- Seamless integration with suspend functions

Location: `app/build.gradle.kts`

## Asynchronous Programming

### Kotlin Coroutines
- **Version**: 1.9.0
- **Library**: `kotlinx-coroutines-android`
- **Usage**: All async operations (Firestore queries, HTTP requests, timers, etc.)

Location: `gradle/libs.versions.toml:13`

### Flow
- `StateFlow` for reactive UI state
- Firestore snapshot listeners converted to `Flow<T>` via `callbackFlow`
- `collectAsState()` in Compose for reactive UI updates

## Testing Libraries

### Unit Testing
- **JUnit**: 4.13.2
- Test coroutines with `runTest`

### Android Testing
- **AndroidX JUnit**: 1.3.0
- **Espresso Core**: 3.7.0
- **Compose UI Test**: via BOM

Location: `gradle/libs.versions.toml:5-7`, `app/build.gradle.kts`

## Background Tasks & Notifications

### WorkManager
- **Version**: 2.9.0
- **Library**: `androidx.work:work-runtime-ktx`
- **Usage**: Scheduled local medication reminders
- **Features**:
  - Guaranteed execution even if app is closed
  - Flexible scheduling (one-time, periodic)
  - Battery-efficient background processing
  - Survives app restarts and device reboots

Location: `app/build.gradle.kts`

### Firebase Cloud Messaging (FCM)
- **Version**: 23.4.0 (via Firebase BOM)
- **Usage**: Push notifications from caregivers to patients
- **Features**:
  - Real-time message delivery
  - Works even when app is in background
  - Custom notification payloads
  - Token-based targeting

Location: `app/src/main/java/com/example/medicaladherence/fcm/MyFirebaseMessagingService.kt`

## QR Code Libraries

### ZXing (Zebra Crossing)
- **Core**: com.google.zxing:core:3.5.2
- **Android Embedded**: com.journeyapps:zxing-android-embedded:4.3.0
- **Usage**: 
  - QR code generation for patient pairing codes
  - QR code scanning in caregiver app
- **Features**:
  - Fast QR code encoding/decoding
  - Camera integration
  - Customizable QR code appearance

Location: `app/build.gradle.kts`

## Networking & Serialization

### OkHttp
- **Version**: 4.12.0
- **Library**: `com.squareup.okhttp3:okhttp`
- **Usage**: HTTP requests to local FCM server (development)
- **Features**:
  - Efficient HTTP client
  - Connection pooling
  - Automatic retries

### Gson
- **Version**: 2.10.1
- **Library**: `com.google.code.gson:gson`
- **Usage**: JSON serialization for QR code data and API payloads
- **Features**:
  - Fast JSON parsing
  - Type-safe deserialization
  - Custom type adapters

Location: `app/build.gradle.kts`

## Development Tools

### Android Studio
- Recommended: Hedgehog (2023.1.1) or later
- Compose preview support
- Live literals
- Layout inspector for Compose

### Kotlin Symbol Processing (KSP)
- Used for annotation processing where needed
- Faster than KAPT
- Better IDE support and build performance

Location: `build.gradle.kts`, `app/build.gradle.kts`

## Dependencies Overview

### Version Catalog
Core dependencies managed in `gradle/libs.versions.toml`:

```toml
[versions]
kotlin = "2.0.21"
compose-bom = "2024.09.00"
navigation = "2.8.5"
coroutines = "1.9.0"
lifecycle = "2.9.4"
```

Additional versions specified directly in `app/build.gradle.kts`:
- Firebase BOM: 32.7.0
- WorkManager: 2.9.0
- ZXing: 3.5.2 / 4.3.0
- Gson: 2.10.1
- OkHttp: 4.12.0

### Key Libraries
```kotlin
// UI
implementation(platform(libs.androidx.compose.bom))
implementation(libs.androidx.compose.material3)
implementation(libs.androidx.navigation.compose)
implementation("androidx.compose.material:material-icons-extended")

// Architecture
implementation(libs.androidx.lifecycle.viewmodel.compose)
implementation(libs.androidx.lifecycle.runtime.compose)

// Firebase
implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
implementation("com.google.firebase:firebase-firestore-ktx")
implementation("com.google.firebase:firebase-auth-ktx")
implementation("com.google.firebase:firebase-messaging")

// Coroutines
implementation(libs.kotlinx.coroutines.android)
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services")

// Background Tasks
implementation("androidx.work:work-runtime-ktx")

// QR Codes
implementation("com.google.zxing:core")
implementation("com.journeyapps:zxing-android-embedded")

// Networking
implementation("com.squareup.okhttp3:okhttp")
implementation("com.google.code.gson:gson")
```

Location: `app/build.gradle.kts`

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
    id("com.google.gms.google-services")  // Firebase integration
}
```

Location: `app/build.gradle.kts`

### Firebase Configuration
- **google-services.json**: Firebase project configuration file
- **google-services plugin**: Processes Firebase configuration at build time
- **Location**: `app/google-services.json`

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

### Firebase Suite
- Firebase BOM for version management
- Firestore for cloud database
- Firebase Auth for anonymous authentication
- FCM for push notifications
- Minimal third-party exposure (Google-owned)

### Supporting Libraries
- ZXing for QR code functionality
- OkHttp for HTTP networking
- Gson for JSON serialization
- WorkManager for background tasks (AndroidX)

### What's NOT Included
- No analytics or tracking libraries
- No crash reporting (Crashlytics not enabled)
- No ad networks
- No social media SDKs
- No image loading libraries (Glide, Coil)

This keeps the app:
- Privacy-focused (no user tracking)
- Focused on core functionality
- Maintainable with minimal dependencies
- Secure with trusted libraries only

## Upgrade Path

### Staying Current
To update dependencies:
1. Update version catalog: `gradle/libs.versions.toml`
2. Update Firebase BOM and other versions in `app/build.gradle.kts`
3. Run `./gradlew --refresh-dependencies`
4. Test thoroughly, especially Firestore interactions

### Breaking Changes to Watch
- Kotlin version updates (may require code changes)
- Compose BOM updates (Material 3 API changes)
- Firebase BOM updates (potential API changes across Firebase products)
- Navigation Compose API changes
- Firestore schema changes (handle gracefully with defaults)

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
- Coroutines for efficient async operations
- Flow for backpressure handling
- Firestore offline persistence and caching
- WorkManager's intelligent job scheduling
