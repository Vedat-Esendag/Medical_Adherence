# Debug Caregiver-Patient Connection (Two Emulators)

Since you're already using two separate emulators but the connection still isn't working, let's debug step-by-step.

## Step 1: Verify Patient Profile is Saved

### On Emulator 1 (Patient):

1. **Create patient profile**
   - Select "I'm a Patient"
   - Enter name: "Test Patient"

2. **Check Logcat immediately:**
   - Filter: `FirebaseRepo`
   - Look for:
   ```
   👤 Setting profile for user: [USER_ID], role: patient, PIN: [6-DIGIT-PIN]
   ✅ Profile saved successfully - User: [USER_ID], Role: patient, Name: Test Patient, PIN: [PIN]
   ```
   - **Write down the USER_ID and PIN**

3. **Add at least one medication**
   - Go to Medications tab
   - Add any medication with at least one time
   - This is required for the connection to work

4. **Go to Settings tab**
   - Verify the 6-digit PIN is displayed
   - **Write down this PIN**

5. **IMPORTANT: Do NOT sign out!**
   - Keep the patient profile active
   - Keep the app running or in background

## Step 2: Connect from Caregiver

### On Emulator 2 (Caregiver):

1. **Create caregiver profile**
   - Select "I'm a Caregiver"
   - Enter name: "Test Caregiver"

2. **Check Logcat:**
   - Filter: `FirebaseRepo`
   - Look for:
   ```
   👤 Setting profile for user: [USER_ID_2], role: caregiver, PIN: 
   ✅ Profile saved successfully - User: [USER_ID_2], Role: caregiver
   ```
   - **Verify USER_ID_2 is DIFFERENT from patient's USER_ID**
   - If they're the same (both `offline_user_local`), that's the problem!

3. **Try to connect to patient**
   - Tap "Add Patient" button
   - Tap "Enter PIN"
   - Enter the patient's 6-digit PIN
   - Tap "Add Patient"

4. **Watch Logcat carefully:**
   - Filter: `CaregiverPatientsVM` and `FirebaseRepo`
   - You should see:
   ```
   🔍 Attempting to import patient with PIN: [PIN]
   📡 Querying Firebase for patient with PIN: [PIN]
   ⚙️ Searching for patient with PIN: [PIN]
   📊 Query returned X user(s)
   ```

## Scenarios and Solutions

### Scenario A: Query returns 0 users
```
📊 Query returned 0 user(s)
❌ No patient found with PIN: [PIN]
```

**Possible causes:**
1. **Patient profile was deleted** (patient signed out)
2. **Patient profile not saved to Firebase** (offline mode issue)
3. **Firebase not connected** (emulator network issue)

**Solutions:**
- Make sure patient didn't sign out
- Check Firebase Console → Firestore → `users` collection
- Verify patient document exists with correct PIN
- Check emulator has internet connection

### Scenario B: Both emulators use same User ID
```
Patient User ID: offline_user_local
Caregiver User ID: offline_user_local  ← PROBLEM!
```

**Cause:** Both emulators are in offline mode and using the same fallback ID

**Solution:** 
- Connect emulators to internet
- Restart emulators with internet enabled
- Firebase should assign different anonymous auth IDs

### Scenario C: Patient has no medications
```
✅ Found patient: Test Patient (ID: [USER_ID])
💊 Found 0 medication(s)  ← PROBLEM!
```

**Cause:** Patient must have at least one medication for export to work

**Solution:**
- Go back to patient emulator
- Add at least one medication
- Try caregiver connection again

### Scenario D: Firebase permission error
```
❌ Error getting patient data by PIN: PERMISSION_DENIED
```

**Cause:** Firestore security rules blocking the query

**Solution:** Check Firebase Console → Firestore → Rules

## Detailed Debugging Steps

### Check 1: Verify Firebase Connectivity

On BOTH emulators, run this check:

1. Open Android Studio → Logcat
2. Filter by `FirebaseApp`
3. Look for initialization messages
4. Should see: `Firebase initialized successfully`

If you see errors like "unable to resolve host" or "connection timeout", the emulator doesn't have internet.

### Check 2: Verify Patient Document Exists

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Go to Firestore Database → `users` collection
4. Look for a document with:
   - `role: "patient"`
   - `name: "Test Patient"`
   - `pin: "[your-6-digit-PIN]"`
5. If it doesn't exist, the profile wasn't saved

### Check 3: Verify Different User IDs

From the logs:
- Patient User ID: `____________` (write it here)
- Caregiver User ID: `____________` (write it here)

These MUST be different. If they're the same, both emulators are sharing the same Firebase instance (unlikely but possible).

### Check 4: Try Manual Query

Use Firebase Console to test the query:

1. Firestore Database → `users` collection
2. Click "Start collection"
3. Use filter:
   - Field: `pin`
   - Operator: `==`
   - Value: `123456` (your actual PIN)
4. Click "Run"
5. Should return 1 document

If this works in Firebase Console but not in app, it's a code issue.

## Common Issues

### Issue: "Patient not found" but patient exists in Firebase
**Possible causes:**
1. PIN doesn't match exactly (typo?)
2. Firestore offline persistence cache not syncing
3. Query timing issue (profile not fully saved yet)

**Solution:**
- Double-check PIN (copy-paste to avoid typos)
- Wait 5 seconds after creating patient profile before connecting
- Check both emulators have stable internet

### Issue: Both emulators show offline_user_local
**Cause:** Emulators not connected to Firebase properly

**Solution:**
1. Close both emulators
2. In Android Studio: Tools → AVD Manager
3. Wipe data on both emulators
4. Restart both emulators
5. Make sure they have internet (test by opening Chrome in emulator)
6. Try again

### Issue: Error "This patient is already in your list"
**Cause:** You already added this patient before

**Solution:**
- In caregiver view, swipe to delete the existing patient
- Or use a different patient PIN

## What to Share for Further Help

If it still doesn't work, please share:

1. **Logcat output** when caregiver tries to connect (the full sequence with emoji logs)
2. **Patient User ID** from logs
3. **Caregiver User ID** from logs  
4. **Screenshot** of Firebase Console showing the patient document
5. **Error message** shown in the app

## Quick Test Checklist

- [ ] Patient profile created on Emulator 1
- [ ] Patient added at least one medication
- [ ] Patient PIN visible in Settings (6 digits)
- [ ] Patient did NOT sign out
- [ ] Caregiver profile created on Emulator 2
- [ ] Both emulators have internet connection
- [ ] Patient and Caregiver have DIFFERENT User IDs in logs
- [ ] Patient document exists in Firebase Console
- [ ] PIN entered correctly in caregiver (no typos)

---

Run through this checklist and check the logs. The enhanced logging I added will tell us exactly where it's failing!

