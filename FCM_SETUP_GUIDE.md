# Firebase Cloud Messaging (FCM) Setup Guide

This guide walks you through setting up the complete FCM notification system for the Medical Adherence app.

## Architecture Overview

```
Caregiver App → Firestore → Cloud Function → FCM → Patient App
```

**Flow:**
1. Caregiver clicks "Send Reminder" button in the app
2. App writes a notification request to Firestore
3. Cloud Function detects the write and sends an FCM notification
4. Patient receives push notification on their device

---

## Part 1: Android App Setup ✅ (COMPLETED)

The following components have been added to your Android app:

### 1.1 FCM Dependency
- Added `firebase-messaging:23.4.0` to `app/build.gradle.kts`

### 1.2 FCM Service
- Created `MyFirebaseMessagingService.kt` in `app/src/main/java/com/example/medicaladherence/fcm/`
- Handles incoming FCM messages and displays notifications
- Automatically saves FCM tokens to Firestore

### 1.3 AndroidManifest Registration
- Registered FCM service in `AndroidManifest.xml`

### 1.4 Token Management
- Updated `FirebaseAuthManager.kt` to request FCM token on authentication
- Token is automatically saved to Firestore user document

### 1.5 Caregiver UI
- Added "Send Reminder" button to `CaretakerScreen.kt`
- Shows confirmation dialog before sending
- Button appears at the top of the patient dashboard

### 1.6 Notification Function
- Added `sendNotificationToPatient()` to `CaretakerViewModel.kt`
- Creates notification request in Firestore

### 1.7 Constants
- Added FCM constants to `utils/Constants.kt`

---

## Part 2: Cloud Functions Setup (REQUIRED)

### 2.1 Prerequisites

You need to have:
- Firebase CLI installed globally
- Node.js 18+ installed
- Firebase project set up

### 2.2 Install Firebase CLI

```bash
# Install Firebase CLI (if not already installed)
npm install -g firebase-tools

# Verify installation
firebase --version
```

### 2.3 Login to Firebase

```bash
# Login to your Firebase account
firebase login

# This will open a browser for authentication
```

### 2.4 Initialize Firebase in Your Project

```bash
# Navigate to your project root
cd /Users/vedatesendag/Documents/GitHub/Medical_Adherence

# Initialize Firebase (if not already done)
firebase init

# Select:
# - Firestore
# - Functions
# - Use existing project: Medical_Adherence
# - Language: JavaScript
# - ESLint: No (optional)
# - Install dependencies: Yes
```

### 2.5 Install Function Dependencies

```bash
# Navigate to functions directory
cd functions

# Install dependencies
npm install

# This installs:
# - firebase-admin (for Firestore and FCM access)
# - firebase-functions (for Cloud Functions)
```

### 2.6 Deploy Cloud Functions

```bash
# From project root or functions directory
firebase deploy --only functions

# Expected output:
# ✔ functions[sendNotification(us-central1)] Successful create operation.
# Function URL: https://us-central1-YOUR-PROJECT.cloudfunctions.net/sendNotification
```

### 2.7 Verify Deployment

```bash
# List deployed functions
firebase functions:list

# Should show:
# sendNotification (Cloud Firestore)
```

---

## Part 3: Firestore Security Rules

Add these rules to your `firestore.rules` file:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // Notification requests collection
    match /notificationRequests/{requestId} {
      // Caregivers can create notification requests
      allow create: if request.auth != null &&
                       get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'Caregiver';

      // Only Cloud Functions can update (mark as sent)
      allow update: if false;

      // No one can read (only Cloud Functions)
      allow read: if false;

      // No one can delete
      allow delete: if false;
    }

    // Users collection - allow FCM token updates
    match /users/{userId} {
      // Users can update their own FCM token
      allow update: if request.auth != null &&
                       request.auth.uid == userId;

      // ... your existing user rules
    }

    // ... your other existing rules
  }
}
```

Deploy the security rules:

```bash
firebase deploy --only firestore:rules
```

---

## Part 4: Testing the System

### 4.1 Test FCM Token Generation (Patient Side)

1. Run the patient app on an emulator or device
2. Login as a patient
3. Check Logcat for: `"FCM token retrieved: [token]"`
4. Open Firebase Console → Firestore → users collection
5. Find your patient user document
6. Verify it has an `fcmToken` field with a long string value

### 4.2 Test Notification Sending (End-to-End)

1. **Setup:**
   - Run patient app on Device/Emulator A (login as patient)
   - Run caregiver app on Device/Emulator B (login as caregiver)
   - Link caregiver to patient using QR code

2. **Send Notification:**
   - In caregiver app, open patient dashboard
   - Click "Send Reminder to [Patient Name]" button at the top
   - Click "Send" in confirmation dialog

3. **Expected Results:**
   - Patient app should receive notification within 1-3 seconds
   - Notification should show "Medication Reminder" as title
   - Body: "Your caregiver wants you to check your medications"
   - Tapping notification should open the app

### 4.3 Debugging

If notifications don't arrive:

**Check 1: FCM Token**
```bash
# Check if patient has FCM token in Firestore
# Firebase Console → Firestore → users → [patient-id]
# Should have: fcmToken: "ey..."
```

**Check 2: Notification Request Created**
```bash
# Firebase Console → Firestore → notificationRequests
# Should see a document with:
# - patientPin: "123456"
# - sent: true (or false if error)
# - timestamp: [timestamp]
```

**Check 3: Cloud Function Logs**
```bash
# View function logs
firebase functions:log

# Or in Firebase Console → Functions → Logs
# Look for:
# - "Processing notification request for patient PIN: 123456"
# - "Notification sent successfully"
```

**Check 4: Notification Permissions**
- Make sure patient app has notification permissions enabled
- On Android, check Settings → Apps → Medical Adherence → Notifications

**Check 5: Device Online**
- FCM requires internet connection
- Patient device must have active internet

---

## Part 5: Common Issues & Solutions

### Issue 1: "Patient has no FCM token"

**Solution:**
- Patient needs to login at least once
- FCM token is generated on first authentication
- Check patient user document in Firestore for `fcmToken` field

### Issue 2: Cloud Function not triggering

**Solution:**
```bash
# Redeploy the function
firebase deploy --only functions

# Check function exists
firebase functions:list
```

### Issue 3: Notification not appearing on device

**Solutions:**
- Check notification permissions in device settings
- Try sending a test notification from Firebase Console:
  - Firebase Console → Cloud Messaging → Send test message
  - Paste patient's FCM token
  - Send

### Issue 4: "Permission denied" when creating notification request

**Solution:**
- Ensure Firestore security rules allow caregivers to create notifications
- Check that user document has `role: "Caregiver"`

---

## Part 6: Firebase Console Quick Links

After deployment, use these Firebase Console sections:

1. **Firestore Database:**
   - View notification requests
   - Check user FCM tokens
   - URL: `https://console.firebase.google.com/project/YOUR-PROJECT/firestore`

2. **Cloud Functions:**
   - View function logs
   - Monitor function executions
   - URL: `https://console.firebase.google.com/project/YOUR-PROJECT/functions`

3. **Cloud Messaging:**
   - Send test notifications
   - View messaging statistics
   - URL: `https://console.firebase.google.com/project/YOUR-PROJECT/messaging`

---

## Part 7: Cost Considerations

**Free Tier Limits:**
- Cloud Functions: 2M invocations/month (free)
- FCM: Unlimited notifications (free)
- Firestore: 50K reads, 20K writes/day (free)

**For a school project:**
- Even with 100 notifications/day = 3,000/month
- Well within free tier limits
- **Cost: $0**

---

## Quick Start Commands

```bash
# 1. Install dependencies
cd functions
npm install

# 2. Deploy functions
cd ..
firebase deploy --only functions

# 3. Deploy security rules
firebase deploy --only firestore:rules

# 4. View logs
firebase functions:log --only sendNotification

# 5. Test (send a notification from caregiver app)
# Then check logs immediately:
firebase functions:log --only sendNotification --limit 5
```

---

## Verification Checklist

Before considering setup complete, verify:

- [ ] Firebase CLI installed and logged in
- [ ] Cloud Function deployed successfully (`firebase functions:list`)
- [ ] Firestore security rules updated and deployed
- [ ] Patient app generates FCM token on login (check Logcat)
- [ ] Patient's FCM token saved in Firestore (check Firebase Console)
- [ ] Caregiver app shows "Send Reminder" button
- [ ] Clicking button creates document in `notificationRequests` collection
- [ ] Cloud Function processes the request (check logs)
- [ ] Patient receives notification on device
- [ ] Tapping notification opens the app

---

## Next Steps

Once setup is complete:

1. **Test with multiple patients:**
   - Create multiple patient accounts
   - Link them to different caregivers
   - Verify each receives their own notifications

2. **Customize notifications:**
   - Edit the notification message in `CaretakerViewModel.kt` (line 438)
   - Change title/body text as needed

3. **Add analytics:**
   - Track when notifications are sent
   - Monitor delivery success rates
   - Log user interactions

4. **Enhance features:**
   - Add custom notification sounds
   - Include action buttons in notifications
   - Support multiple notification types

---

## Support

If you encounter issues:

1. **Check logs:**
   ```bash
   firebase functions:log --only sendNotification
   ```

2. **Check Firestore:**
   - View `notificationRequests` collection
   - Check for `error` field in documents

3. **Check Android logs:**
   ```bash
   adb logcat | grep -i "FCM\|FirebaseMessaging"
   ```

4. **Firebase Console:**
   - Functions → Logs (for Cloud Function errors)
   - Firestore → Data (for notification requests)
   - Authentication → Users (for user data)

---

## Conclusion

Your FCM notification system is now set up! The architecture is:

- **Secure:** Cloud Functions run server-side with admin privileges
- **Scalable:** FCM handles delivery to thousands of devices
- **Real-time:** Notifications arrive within 1-3 seconds
- **Free:** Completely free for school projects
- **Reliable:** Firebase handles retry and delivery

Caregivers can now send instant reminders to patients with a single button click!
