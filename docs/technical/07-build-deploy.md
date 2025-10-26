# Build & Deployment Guide

## TL;DR
Standard Android Gradle build. Debug builds for development, release builds for production. Room uses KSP for annotation processing.

## Prerequisites

### Required Tools
- **Android Studio**: Hedgehog (2023.1.1) or later
- **JDK**: Version 11 (set in Android Studio)
- **Android SDK**: API 29-36 installed
- **Kotlin**: 2.0.21 (managed by Gradle)

### Recommended
- **Device/Emulator**: Android 10+ (API 29+)
- **RAM**: 8GB minimum for Android Studio
- **Disk Space**: 10GB for SDK + project

## Project Setup

### Clone Repository
```bash
git clone <repository-url>
cd Medical_Adherence
```

### Open in Android Studio
1. Launch Android Studio
2. File → Open → Select `Medical_Adherence` folder
3. Wait for Gradle sync to complete
4. Resolve any SDK installation prompts

### Gradle Sync
Automatic on first open. Manual sync:
- File → Sync Project with Gradle Files
- Or click Sync icon in toolbar

## Build Variants

### Debug Build
For development with full logging.

**Features**:
- Debuggable
- No minification
- Faster build times
- Includes debugging symbols

**Build**:
```bash
./gradlew assembleDebug
```

**Output**: `app/build/outputs/apk/debug/app-debug.apk`

### Release Build
For production deployment.

**Features**:
- Not debuggable
- Minification disabled (for now)
- Optimized (when minification enabled)

**Build**:
```bash
./gradlew assembleRelease
```

**Output**: `app/build/outputs/apk/release/app-release-unsigned.apk`

**Note**: Requires signing for distribution

## Building from Command Line

### Install Debug Build
```bash
./gradlew installDebug
```

Builds and installs to connected device/emulator.

### Clean Build
```bash
./gradlew clean
./gradlew build
```

Removes all build artifacts and rebuilds.

### Build All Variants
```bash
./gradlew build
```

Builds both debug and release APKs.

## Running the App

### From Android Studio
1. Select device/emulator from dropdown
2. Click Run (green play button)
3. Or press `Shift + F10`

**First Run**:
- App installs
- Launches automatically
- Database created on first access

### From Command Line
```bash
./gradlew installDebug
adb shell am start -n com.example.medicaladherence/.MainActivity
```

## Testing

### Unit Tests
```bash
./gradlew test
```

Runs all unit tests in `app/src/test/`.

### Instrumented Tests
```bash
./gradlew connectedAndroidTest
```

Runs tests in `app/src/androidTest/` on connected device.

### Specific Test
```bash
./gradlew test --tests "com.example.medicaladherence.HomeViewModelTest"
```

## Code Generation

### Room with KSP
Room entities/DAOs use KSP for code generation.

**Trigger Generation**:
```bash
./gradlew kspDebugKotlin
```

**Generated Files**: `app/build/generated/ksp/debug/kotlin/`

**Auto-Generated**:
- DAO implementations
- Database implementations
- Type converter references

### Compose Compiler
Kotlin Compose plugin generates:
- Composer functions
- State tracking code
- Recomposition logic

**Enabled in**: `app/build.gradle.kts:39-40`

## ProGuard / R8

### Current Configuration
Minification **disabled** in release builds.

**Location**: `app/build.gradle.kts:22-29`
```kotlin
buildTypes {
    release {
        isMinifyEnabled = false
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

### Enable Minification
Set `isMinifyEnabled = true` for smaller APKs.

**ProGuard Rules**: `app/proguard-rules.pro`

Common rules needed:
```proguard
# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Kotlin
-keep class kotlin.** { *; }
```

## Signing APKs

### Debug Signing
Automatic with debug keystore.

**Location**: `~/.android/debug.keystore`

### Release Signing
Create keystore:
```bash
keytool -genkey -v -keystore release.keystore \
  -alias medical_adherence \
  -keyalg RSA -keysize 2048 -validity 10000
```

**Configure in `app/build.gradle.kts`**:
```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("release.keystore")
            storePassword = "password"
            keyAlias = "medical_adherence"
            keyPassword = "password"
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

**Security**: Store passwords in `local.properties` or env vars, not in Git.

## Dependencies Management

### Update Dependencies
1. Edit `gradle/libs.versions.toml`
2. Update Room version in `app/build.gradle.kts`
3. Sync Gradle
4. Test thoroughly

**Check for Updates**:
```bash
./gradlew dependencyUpdates
```
(Requires `com.github.ben-manes.versions` plugin)

### Refresh Dependencies
```bash
./gradlew --refresh-dependencies
```

## Build Performance

### Gradle Daemon
Enabled by default. Check:
```bash
./gradlew --status
```

### Build Cache
Enabled in `gradle.properties`:
```properties
org.gradle.caching=true
```

### Parallel Builds
```properties
org.gradle.parallel=true
org.gradle.workers.max=4
```

### Configure Memory
In `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=512m
```

## Troubleshooting

### Gradle Sync Failed
- Check internet connection (downloads dependencies)
- Invalidate Caches: File → Invalidate Caches / Restart
- Delete `.gradle` folder and resync

### KSP Build Errors
```bash
./gradlew clean
./gradlew kspDebugKotlin
```

### Room Schema Errors
Delete app data or uninstall:
```bash
adb uninstall com.example.medicaladherence
```

### Out of Memory
Increase heap size in `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx4096m
```

### Compose Issues
- Update to latest Compose BOM
- Clean and rebuild
- Check Kotlin version compatibility

## CI/CD Setup

### GitHub Actions Example
```yaml
name: Android CI

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 11
        uses: actions/setup-java@v3
        with:
          java-version: '11'
      - name: Build with Gradle
        run: ./gradlew build
      - name: Run tests
        run: ./gradlew test
```

## APK Analysis

### Analyze APK Size
Android Studio: Build → Analyze APK
Or:
```bash
./gradlew assembleRelease
```
Then open `app/build/outputs/apk/release/app-release.apk` in Android Studio.

### APK Contents
- `classes.dex`: Kotlin/Java code
- `resources.arsc`: Resources
- `res/`: Images, layouts (compiled)
- `lib/`: Native libraries (none currently)

## Version Management

### Update Version
**Location**: `app/build.gradle.kts:16-17`
```kotlin
defaultConfig {
    versionCode = 2         // Increment for each release
    versionName = "1.1.0"   // User-facing version
}
```

**Semantic Versioning**: MAJOR.MINOR.PATCH

## Deployment Checklist

### Pre-Release
- [ ] Update version code and name
- [ ] Run all tests: `./gradlew test connectedAndroidTest`
- [ ] Enable minification (optional)
- [ ] Configure release signing
- [ ] Test on multiple devices/API levels
- [ ] Review ProGuard rules if minifying
- [ ] Update README/changelog

### Build Release
```bash
./gradlew assembleRelease
```

### Verify APK
- Install on test device
- Check all features work
- Verify no crashes
- Check APK size

### Distribute
- Upload to Google Play Console
- Or distribute APK directly (sideloading)

## Database Migrations

### Schema Changes
When changing Room entities:
1. Increment database version in `AppDatabase.kt`
2. Add migration:
```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // ALTER TABLE ...
    }
}
```
3. Add to database builder:
```kotlin
Room.databaseBuilder(...)
    .addMigrations(MIGRATION_1_2)
    .build()
```

**Current**: Using `.fallbackToDestructiveMigration()` (data loss on schema change)

## Useful Gradle Tasks

```bash
./gradlew tasks                  # List all tasks
./gradlew dependencies           # Show dependency tree
./gradlew clean                  # Clean build artifacts
./gradlew build                  # Build all variants
./gradlew installDebug           # Install debug APK
./gradlew uninstallAll           # Uninstall from device
./gradlew connectedCheck         # Run all tests
./gradlew lint                   # Run lint checks
```
