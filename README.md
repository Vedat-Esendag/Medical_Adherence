# Medical Adherence App

A modern Android medication tracking application for patients and caregivers, built with Jetpack Compose, Firebase, and MVVM architecture.

## 📱 Overview

This app helps patients track medication adherence and enables caregivers to monitor patient compliance remotely.

**Key Features**:
- Patient medication tracking with real-time adherence statistics
- Caregiver patient monitoring dashboard
- Push notifications via Firebase Cloud Messaging
- QR code-based patient data exchange
- Offline-first with Firebase Firestore sync

## 🏗️ Tech Stack

- **Language**: Kotlin 2.0.21
- **UI**: Jetpack Compose + Material 3
- **Architecture**: MVVM with StateFlow
- **Backend**: Firebase (Firestore + FCM + Anonymous Auth)
- **Background**: WorkManager for scheduled reminders
- **Navigation**: Compose Navigation
- **Async**: Kotlin Coroutines + Flow

## 📚 Documentation

**For complete documentation, see [`/docs/`](docs/)**

### Quick Links:
- **[00-EXAM-START-HERE.md](docs/00-EXAM-START-HERE.md)** ⭐ Start here for exam reference
- **[EXAM_QUICK_INDEX.md](docs/EXAM_QUICK_INDEX.md)** - Master concept lookup (use Ctrl+F)
- **[CONCEPT_TO_CODE_MAP.md](docs/CONCEPT_TO_CODE_MAP.md)** - Quick reference table
- **[CODEBASE_REFERENCE.md](docs/CODEBASE_REFERENCE.md)** - Comprehensive code documentation
- **[ARCHITECTURE.md](docs/ARCHITECTURE.md)** - System architecture overview
- **[FCM_SETUP.md](docs/FCM_SETUP.md)** - Firebase Cloud Messaging setup guide

**Technical Deep Dives**: See [`/docs/technical/`](docs/technical/) for detailed documentation on architecture, state management, navigation, database, and more.

## 🚀 Quick Start

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- Android SDK 29+ (minSdk: 29, targetSdk: 36)
- Kotlin 2.0.21
- Firebase project with Firestore and FCM enabled

### Run the App

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd Medical_Adherence
   ```

2. **Open in Android Studio**
   - File → Open → Select project folder
   - Wait for Gradle sync to complete

3. **Add Firebase Configuration**
   - Download `google-services.json` from Firebase Console
   - Place in `app/` directory

4. **Run**
   - Connect Android device or start emulator
   - Click **Run** (green play button) or press `Shift + F10`

## 🏛️ Architecture

```
MVVM Pattern:
├── UI Layer (Jetpack Compose)
│   ├── Screens (HomeScreen, StatsScreen, etc.)
│   └── Components (DoseCard, PinDialog, etc.)
├── ViewModel Layer (StateFlow)
│   ├── HomeViewModel
│   ├── AddMedicationViewModel
│   ├── StatsViewModel
│   └── [5 more ViewModels]
└── Data Layer
    ├── Repository (FirebaseMedicationRepository)
    ├── Models (Medication, DoseEvent, PatientProfile)
    └── Firebase (Firestore, FCM, Auth)
```

**Key Patterns**:
- Single Activity Architecture
- Repository Pattern for data abstraction
- StateFlow for reactive UI state
- Singleton pattern for repository instance
- Flow for real-time Firestore listeners

## 📦 Project Structure

```
app/src/main/java/com/example/medicaladherence/
├── MainActivity.kt                 # Single activity entry point
├── data/
│   ├── model/                     # Domain models
│   ├── repository/                # Data layer (Firebase)
│   └── firebase/                  # Firebase integration
├── viewmodel/                     # ViewModels (MVVM)
├── ui/
│   ├── screens/                   # Composable screens
│   ├── components/                # Reusable components
│   ├── theme/                     # Material 3 theme
│   └── nav/                       # Navigation
├── fcm/                           # Firebase Cloud Messaging
├── notification/                  # WorkManager scheduling
└── utils/                         # Utilities & constants
```

## 🔥 Firebase Setup

This app requires Firebase for:
- **Firestore**: Cloud database for medications and dose events
- **Firebase Auth**: Anonymous authentication
- **FCM**: Push notifications from caregiver to patient

**Setup Guide**: See [docs/FCM_SETUP.md](docs/FCM_SETUP.md) for complete Firebase configuration.

## 👥 User Roles

### Patient Mode
- Track own medications
- Mark doses as taken/missed
- View adherence statistics and streaks
- Generate QR code to share with caregiver

### Caregiver Mode
- Monitor multiple patients
- View patient adherence in real-time
- Send push notification reminders
- Add patients via QR scan or PIN

## 🧪 Testing

- Unit tests: `app/src/test/`
- Instrumented tests: `app/src/androidTest/`

**Run tests**:
```bash
./gradlew test                    # Unit tests
./gradlew connectedAndroidTest    # Instrumented tests
```

## 📱 Build & Deploy

**Debug APK**:
```bash
./gradlew assembleDebug
```

**Release APK**:
```bash
./gradlew assembleRelease
```

**See**: [docs/technical/07-build-deploy.md](docs/technical/07-build-deploy.md) for detailed build instructions.

## 🎓 For Exam Preparation

This codebase is documented for exam reference. Start with:
1. [docs/00-EXAM-START-HERE.md](docs/00-EXAM-START-HERE.md) - Exam homepage
2. [docs/EXAM_QUICK_INDEX.md](docs/EXAM_QUICK_INDEX.md) - Fast concept lookup
3. [docs/CONCEPT_TO_CODE_MAP.md](docs/CONCEPT_TO_CODE_MAP.md) - Quick reference table

All code implementations are referenced with file:line numbers for easy exam lookup.

## 📄 License

This project is for educational purposes.

---

**Documentation**: `/docs/` folder  
**Source Code**: `/app/` folder  
**Firebase Functions**: `/functions/` folder  

For detailed documentation, architecture explanations, and exam reference materials, see the `/docs/` directory.
