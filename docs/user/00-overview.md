# Medical Adherence App - Overview

## TL;DR
A clean, minimal Android app for tracking medication adherence with large touch targets, calm colors, and encouraging feedback designed for elderly users.

## What It Does
The Medical Adherence app helps you:
- **Track daily medications** with scheduled reminders
- **Monitor adherence** with weekly statistics and streak tracking
- **Manage schedules** with flexible frequency options (daily, specific days, etc.)
- **Stay motivated** with non-judgmental, friendly feedback

## Key Features

### Today's Tracking
- Live countdown to your next dose
- Quick "Taken", "Missed", or "Snooze 15m" buttons
- Visual confirmation with undo option

### Medication Library
- Store all your medications with dosages and schedules
- Add multiple doses per day (e.g., morning and evening)
- Optional notes for special instructions

### Statistics Dashboard
- Weekly adherence percentage (0-100%)
- Streak counter for consecutive perfect days
- Daily breakdown with visual bars
- Encouraging feedback based on your performance

### Accessibility First
- Large touch targets (≥48dp) for easy tapping
- Adjustable font sizes (Normal and Large)
- Soothing blue color scheme to reduce anxiety
- Friendly, non-judgmental language throughout

## Who It's For
Designed for patients who need help staying consistent with medication schedules, especially elderly users or those managing multiple medications. Also includes optional caregiver features for family members who want to help monitor adherence remotely.

## Getting Started
1. Choose your role (Patient or Caregiver)
2. **As a Patient:**
   - Tap the **+ button** to add your first medication
   - Enter medication name, dosage, and schedule
   - Mark doses as taken throughout the day
   - Check Stats tab to see your weekly progress
   - Optionally share your PIN with a caregiver for remote monitoring
3. **As a Caregiver:**
   - Scan a patient's QR code or enter their 6-digit PIN
   - Monitor their medication adherence in real-time
   - View their statistics and medication schedules

## Navigation
Main sections accessible via the bottom bar (varies by role):
- **Home**: Today's doses and quick actions (Patient)
- **Patients**: List of connected patients (Caregiver)
- **Medications**: Full library of all medications (Patient)
- **Stats**: Weekly adherence and trends
- **Settings**: Font size, high contrast mode, and pairing options

## Privacy & Data Storage
Your medication data is securely stored in Firebase Cloud Firestore:
- **Offline support**: Works without internet, automatically syncs when back online
- **Private by default**: Data only shared with explicitly connected caregivers via PIN/QR code
- **No public sharing**: No social features, ads, or third-party data sharing
- **Secure cloud backup**: Data persists across device changes and app reinstalls
