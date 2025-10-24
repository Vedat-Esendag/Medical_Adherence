# Medical Adherence App

A clean, minimal Android app prototype for tracking medication adherence. Built with Jetpack Compose, Material 3, and modern Android architecture.

## Features

### Core Functionality
- **Today's Doses**: View all scheduled medications for today with easy-to-use action buttons
- **Quick Actions**: Mark doses as taken, missed, or snooze for 15 minutes
- **Live Countdown**: Real-time countdown to your next scheduled dose
- **Add Medications**: Simple form to add new medications with custom schedules
- **Adherence Statistics**: Weekly adherence percentage and daily breakdown with visual bars
- **Streak Tracking**: Monitor your consecutive days of perfect adherence

### Accessibility & UX
- **Large Touch Targets**: All interactive elements are ≥48dp for easy tapping
- **Adjustable Font Size**: Settings screen with Normal and Large font options
- **Calm Color Palette**: Soothing blue theme designed to reduce anxiety
- **Friendly Copy**: Encouraging, non-judgmental language throughout
- **Material 3 Design**: Modern, familiar interface following Google's latest design system
- **Dynamic Colors**: Adapts to your device theme on Android 12+

### Technical Highlights
- **Single Activity Architecture**: Navigation-Compose for smooth transitions
- **MVVM Pattern**: ViewModel + StateFlow for reactive, testable code
- **In-Memory Data**: Pre-seeded with sample data for Maria and Ahmed
- **No External Dependencies**: Pure Jetpack stack (no Room, WorkManager, etc.)

## Architecture

```
app/
├── data/
│   ├── model/           # Medication, DoseEvent data classes
│   └── repo/            # InMemoryMedicationRepository
├── viewmodel/           # HomeViewModel, AddMedicationViewModel, etc.
├── ui/
│   ├── screens/         # HomeScreen, StatsScreen, SettingsScreen, etc.
│   ├── components/      # Reusable DoseCard component
│   ├── nav/             # Navigation graph
│   └── theme/           # Material 3 theme, colors, typography
└── MainActivity.kt
```

## How to Run

1. **Prerequisites**
   - Android Studio Hedgehog (2023.1.1) or later
   - Android SDK 29+ (minSdk: 29, targetSdk: 36)
   - Kotlin 2.0.21

2. **Clone & Open**
   ```bash
   git clone <repository-url>
   cd Medical_Adherence
   ```
   - Open the project in Android Studio
   - Wait for Gradle sync to complete

3. **Run**
   - Connect an Android device or start an emulator
   - Click **Run** (green play button) or press `Shift + F10`
   - The app will install and launch

4. **Explore**
   - Home screen shows pre-seeded medications for Maria and Ahmed
   - Try marking doses as taken/missed
   - Add your own medication via the FAB (+) button
   - View statistics to see weekly adherence and daily breakdown
   - Adjust font size in Settings

## Seed Data

The app comes with sample data:

- **Maria**: Amlodipine 5mg (07:00), Metoprolol 50mg (19:00), Aspirin 81mg (21:00)
- **Ahmed**: Mesalamine 800mg (08:00), Azathioprine 50mg (22:00)
- **Adherence**: ~80% for the past week with randomized dose events

## Technologies Used

- **Kotlin** 2.0.21
- **Jetpack Compose** (BOM 2024.09.00)
- **Material 3** - Modern design system
- **Navigation-Compose** 2.8.5 - Screen navigation
- **ViewModel + StateFlow** - State management
- **Coroutines** 1.9.0 - Asynchronous operations

## Future Enhancements

This is a prototype. Potential improvements:
- Persistent storage (Room database)
- Real notifications with AlarmManager/WorkManager
- Medication refill reminders
- Multi-user support with authentication
- Export adherence reports (PDF/CSV)
- Integration with health data APIs

## License

This project is a prototype for demonstration purposes.
