# Medical Adherence App - Documentation Index

Welcome to the Medical Adherence Android App documentation! This guide will help you navigate all available documentation and understand the codebase.

---

## Quick Navigation

### For Quick Overview (5 minutes)
1. **Start here**: [ARCHITECTURE_SUMMARY.md](ARCHITECTURE_SUMMARY.md)
   - Technology stack
   - Core patterns
   - Quick reference for key components
   - Developer checklists

### For Complete Understanding (30-45 minutes)
1. **Then read**: [CODEBASE_DOCUMENTATION.md](CODEBASE_DOCUMENTATION.md)
   - Comprehensive project structure
   - Detailed feature breakdown
   - Full architecture explanations
   - Data models and ViewModels

### For Implementation Details
1. **Check relevant sections** in CODEBASE_DOCUMENTATION.md:
   - Section 3: Key Files (find specific files)
   - Section 4: Features (understand feature implementations)
   - Section 6: Firebase Integration (learn about backend)
   - Section 8: Data Models (understand data structures)

---

## Documentation Files

### Main Documentation

| File | Size | Purpose | Read Time |
|------|------|---------|-----------|
| [README.md](README.md) | 3.7 KB | Project overview, how to run | 5 min |
| [ARCHITECTURE_SUMMARY.md](ARCHITECTURE_SUMMARY.md) | 15 KB | Quick reference, patterns, architecture diagrams | 15 min |
| [CODEBASE_DOCUMENTATION.md](CODEBASE_DOCUMENTATION.md) | 43 KB | Comprehensive codebase guide | 45 min |
| [FCM_SETUP_GUIDE.md](FCM_SETUP_GUIDE.md) | 10.7 KB | Firebase Cloud Messaging setup instructions | 30 min |

---

## By Role

### New Developer Joining the Team

1. **Day 1**: Read [README.md](README.md) and [ARCHITECTURE_SUMMARY.md](ARCHITECTURE_SUMMARY.md)
2. **Day 2**: Explore [CODEBASE_DOCUMENTATION.md](CODEBASE_DOCUMENTATION.md) sections 1-5
3. **Day 3**: Deep dive into specific features (Section 4 & 8)
4. **Task**: Use "New Screen Checklist" in ARCHITECTURE_SUMMARY.md to add a test screen

### Android Architect

- Focus on sections 5-7 in CODEBASE_DOCUMENTATION.md
- Review patterns and Firebase integration architecture
- Check data models (Section 8)

### Firebase Backend Engineer

- Read Section 6 (Firebase Integration Points)
- Review Firestore document structure in ARCHITECTURE_SUMMARY.md
- Check [FCM_SETUP_GUIDE.md](FCM_SETUP_GUIDE.md) for Cloud Functions setup

### QA/Tester

- Review Section 4 (Main Features Implemented)
- Check ARCHITECTURE_SUMMARY.md for navigation structure
- Use device-specific testing in Section 14 (Testing & Development)

---

## Key Sections in CODEBASE_DOCUMENTATION.md

### 1. PROJECT OVERVIEW
- App description
- Key characteristics
- Target specifications

### 2. PROJECT STRUCTURE
- Complete file tree
- Package organization
- 40+ Kotlin files

### 3. KEY FILES AND THEIR PURPOSES
- Quick reference table
- File locations
- What each file does

### 4. MAIN FEATURES IMPLEMENTED
- **Patient Features**: Medication management, dose tracking, statistics
- **Caregiver Features**: Patient management, monitoring, communication
- **Data & Integration Features**: Firebase, QR codes, notifications

### 5. ARCHITECTURE PATTERNS USED
- MVVM pattern (5.1)
- Single Activity architecture (5.2)
- Repository pattern (5.3)
- Singleton pattern (5.4)
- StateFlow (5.5)
- Factory pattern (5.6)
- Firestore structure (5.7)

### 6. FIREBASE INTEGRATION POINTS
- Authentication (6.1)
- Firestore (6.2)
- Cloud Messaging (6.3)
- Data mapping (6.4)
- Offline persistence (6.5)

### 7. UI COMPONENTS AND SCREENS
- Screen hierarchy
- Detailed screen specifications
- Reusable components
- Material 3 design system

### 8. DATA MODELS
- Domain models (Medication, DoseEvent, PatientProfile)
- Firestore models
- State models

### 9. VIEWMODELS AND REPOSITORIES
- ViewModel responsibilities
- Repository responsibilities
- Key methods
- Implementation patterns

---

## Key Concepts Quick Reference

### MVVM Pattern
```
User Input → UI (Compose) → ViewModel → Repository → Firebase
```
- UI is declarative (Jetpack Compose)
- ViewModel holds state (StateFlow)
- Repository abstracts data operations

### Real-time Sync
```
Firestore Listener → ViewModel State → UI Recomposition
```
- Changes in Firestore trigger listeners
- StateFlow notifies observers
- Compose recomposes automatically

### Authentication
```
Device → Anonymous Auth → Firebase UID or Device-specific ID
          ↓
      FCM Token → Save to Firestore
```

### Patient-Caregiver Link
```
Patient generates QR code → Caregiver scans
                            ↓
                   Create caregiver_link document
                   ↓
         Caregiver can now monitor patient
```

---

## Project Statistics

- **Language**: Kotlin 2.0.21
- **Min SDK**: 29 (Android 10)
- **Target SDK**: 36 (Android 15)
- **Architecture**: MVVM with StateFlow
- **Backend**: Firebase Firestore + Cloud Messaging
- **UI Framework**: Jetpack Compose
- **Files**: 40+ Kotlin source files
- **Lines of Code**: 4,000+
- **ViewModels**: 8
- **Screens**: 10
- **Data Models**: 10+

---

## Common Tasks

### How to...

#### Add a New Medication Field
1. Edit `data/model/Medication.kt`
2. Add field to `FirestoreMedication` in `data/firebase/FirestoreModels.kt`
3. Update mapping functions in same file
4. Add to form in `AddEditMedicationScreen.kt`
5. Update ViewModel validation

#### Add a New Statistic
1. Add calculation method to `FirebaseMedicationRepository`
2. Add state to ViewModel UI state
3. Load data in ViewModel init
4. Display in appropriate screen

#### Create a New Screen
1. Follow "New Screen Checklist" in ARCHITECTURE_SUMMARY.md
2. Use existing screen as template (e.g., HomeScreen.kt)
3. Create ViewModel with StateFlow
4. Register in MainActivity.kt NavHost
5. Add to Routes object

#### Send Notification to Patient
1. Call `CaretakerViewModel.sendNotificationToPatient()`
2. Uses FCM configuration from Constants.kt
3. Respects USE_LOCAL_SERVER vs USE_CLOUD_FUNCTIONS settings

---

## Dependencies and Libraries

### Core Android
- androidx.core:core-ktx
- androidx.lifecycle:lifecycle-runtime-ktx
- androidx.activity:activity-compose

### Jetpack Compose
- androidx.compose.bom:2024.09.00
- androidx.compose.ui
- androidx.compose.material3
- androidx.compose.navigation

### Firebase
- firebase-firestore-ktx
- firebase-auth-ktx
- firebase-messaging:23.4.0

### Utilities
- androidx.work:work-runtime-ktx (WorkManager)
- com.google.zxing:core (QR codes)
- com.journeyapps:zxing-android-embedded
- com.google.code.gson:gson
- com.squareup.okhttp3:okhttp

---

## File Organization Reference

### Data Layer (`data/`)
- **model/**: Domain models (Medication, DoseEvent, etc.)
- **repo/**: In-memory repository (legacy) and provider
- **repository/**: FirebaseMedicationRepository (current implementation)
- **firebase/**: Firebase integration, auth, Firestore models

### State Management (`viewmodel/`)
- One ViewModel per screen
- Each has corresponding Factory class
- StateFlow for reactive state

### UI Layer (`ui/`)
- **screens/**: Full screen composables
- **components/**: Reusable components (DoseCard, etc.)
- **theme/**: Material 3 theming
- **nav/**: Navigation routes

### Backend Integration (`fcm/` + `notification/`)
- **fcm/**: MyFirebaseMessagingService (receive notifications)
- **notification/**: NotificationScheduler + MedicationReminderWorker (local reminders)

### Utilities (`util/` + `utils/`)
- **util/**: QR code generation and parsing
- **utils/**: Constants, FCM helpers, local server helper

---

## Testing Information

### Test Setup
- Unit tests: `app/src/test/`
- Instrumented tests: `app/src/androidTest/`
- Test runner: AndroidJUnitRunner
- Framework: Compose testing library

### Sample Data
Pre-populated patients for testing:
- **Maria**: 3 medications, ~80% adherence
- **Ahmed**: 2 medications, ~80% adherence
- Dose history: Past 7 days with randomized events

### Firebase Emulator
Can be configured for local testing without Firebase project

---

## Getting Help

### If you need to understand...

| Question | Section | File |
|----------|---------|------|
| How the app is structured? | 2 | CODEBASE_DOCUMENTATION.md |
| What a specific file does? | 3 | CODEBASE_DOCUMENTATION.md |
| How a feature works? | 4 | CODEBASE_DOCUMENTATION.md |
| Architecture patterns? | 5 | CODEBASE_DOCUMENTATION.md |
| How Firebase is used? | 6 | CODEBASE_DOCUMENTATION.md |
| How to add a screen? | 5 | ARCHITECTURE_SUMMARY.md (checklist) |
| How to set up FCM? | All | FCM_SETUP_GUIDE.md |
| Quick architecture overview? | All | ARCHITECTURE_SUMMARY.md |
| How to run the app? | All | README.md |

---

## Version Information

- **Documentation Generated**: November 13, 2024
- **App Version**: 1.0
- **Last Major Update**: Addition of FCM and caregiver features
- **Current Branch**: cleanup

---

## Next Steps

1. **Explore the codebase** using the file tree in Section 2
2. **Read the appropriate documentation** based on your role above
3. **Use the architecture diagram** in ARCHITECTURE_SUMMARY.md as reference
4. **Follow the checklists** when adding new features
5. **Refer back to data models** section when implementing data operations

---

## Feedback & Improvements

As you work with the codebase, consider updating this documentation with:
- Clarifications if something is unclear
- New patterns discovered
- Tips for common tasks
- Known limitations

---

**Last Updated**: November 13, 2024
**Status**: Complete and comprehensive
**Maintainer**: Medical Adherence Team

