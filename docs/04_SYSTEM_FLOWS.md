# System Flows

This document provides visual flowcharts (ASCII art) for all major system operations.

## 1. User Authentication Flow

```
App Launch → Firebase.auth.currentUser?
    ├─ YES → Return UID → Load Profile → Navigate by Role
    └─ NO  → Try signInAnonymously()
              ├─ SUCCESS → Return Firebase UID → Request FCM Token
              └─ FAIL (offline) → Generate "offline_user_<ANDROID_ID>"
                                → App works offline with local cache
```

## 2. Medication Dose Flow

```
Patient Opens HomeScreen
    ↓
Load Today's Doses (based on medication frequency)
    ↓
Display with Countdown Timer
    ↓
Patient Clicks "Mark Taken"
    ↓
ViewModel.markDoseTaken(medId, time)
    ↓
Repository.recordDoseTaken() → Firestore.collection("doseEvents").add()
    ↓
├─ Optimistic UI Update (immediate)
└─ Firestore Real-Time Listener fires
       ↓
   Repository emits new Flow
       ↓
   ViewModel updates StateFlow
       ↓
   Compose recomposes UI (< 2 seconds)
```

## 3. Caregiver Notification Flow

```
Caregiver Dashboard → Clicks "Send Reminder"
    ↓
Get Patient's FCM Token from Firestore
    ↓
FCMHelper.sendNotification(token, message)
    ↓
POST https://fcm.googleapis.com/fcm/send
  Headers: { Authorization: "key=<SERVER_KEY>" }
  Body: { to: token, notification: { title, body } }
    ↓
FCM Server routes to patient device
    ↓
MyFirebaseMessagingService.onMessageReceived()
    ↓
Show Android Notification (even if app closed)
```

## 4. QR Code Pairing Flow

```
PATIENT: Generate PIN (6-digit random) → Display as QR Code
CAREGIVER: Scan QR Code OR Enter PIN manually
    ↓
Extract PIN from QR: "MED_ADHERENCE:487392"
    ↓
Firestore.collection("users").whereEqualTo("pin", "487392").get()
    ↓
├─ FOUND → Load patient data → Display Dashboard → Setup real-time listeners
└─ NOT FOUND → Show error "Patient not found"
```

## 5. Real-Time Sync Flow

```
Device 1: Patient marks dose taken
    ↓
Firestore Write (users/{id}/doseEvents)
    ↓
Firestore triggers snapshot listeners on ALL connected devices
    ↓
├─ Device 1 (patient) receives update → UI refreshes
└─ Device 2 (caregiver) receives update → Dashboard refreshes

Total latency: ~400ms (< half second)
```

## 6. Offline to Online Transition

```
App Starts (No Internet)
    ↓
signInAnonymously() fails → Use offline ID: "offline_user_<ANDROID_ID>"
    ↓
App works fully: Add meds, mark doses (Firestore queues writes)
    ↓
⏳ WiFi enabled
    ↓
Firestore detects connection → Uploads queued writes → Downloads server changes
    ↓
signInAnonymously() succeeds → Returns Firebase UID → Request FCM Token
    ↓
App fully online: Real-time sync + push notifications enabled
```

## 7. Statistics Calculation

```
Load Statistics
    ↓
├─ Weekly Adherence: Get last 7 days → Count (taken / scheduled) * 100
├─ Monthly Adherence: Get last 30 days → Same calculation
├─ Current Streak: Check each day backwards → Break when < 100%
├─ Problematic Meds: Calculate per-medication adherence → Filter < 80%
└─ Daily Breakdown: Mon-Sun adherence percentages
    ↓
Update UI with charts and progress bars
```

## 8. WorkManager Notifications

```
Patient Adds Medication with times ["08:00", "20:00"]
    ↓
NotificationScheduler.scheduleDoseNotifications(med)
    ↓
For each time: Create OneTimeWorkRequest with delay until time
    ↓
WorkManager.enqueue(request) → Persists to database
    ↓
⏳ 08:00 arrives
    ↓
WorkManager triggers DoseReminderWorker
    ↓
Show Android Notification: "Time for Aspirin - Take 100mg"
    ↓
Patient taps → Opens HomeScreen with dose card
```

## Key Takeaways

- **Authentication**: Anonymous Firebase Auth with offline fallback
- **Dose Tracking**: Optimistic updates + real-time sync
- **Notifications**: FCM for caregiver reminders, WorkManager for local
- **Pairing**: PIN-based Firestore query (simple for elderly users)
- **Sync**: Firestore snapshot listeners (< 2 second latency)
- **Offline**: Device-specific IDs + automatic Firestore caching
- **Statistics**: Real-time recalculation after each dose event
