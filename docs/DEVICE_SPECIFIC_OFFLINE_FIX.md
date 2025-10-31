# Device-Specific Offline ID Fix

## Problem Solved

When testing patient and caregiver profiles on the same machine with two emulators, both emulators were using the same offline user ID (`offline_user_local`), causing:

- **Patient profile overwritten**: When caregiver profile was created, it replaced the patient profile in Firebase
- **PIN query failed**: Query for patient PIN returned 0 results because the patient document was replaced
- **Unable to test connection**: Caregiver couldn't connect to patient using PIN

## Root Cause

Both emulators fell back to the same hardcoded offline ID:
```
Emulator 1: offline_user_local (Patient)
Emulator 2: offline_user_local (Caregiver) ← Overwrites patient!
```

Even though both emulators had internet connectivity, Firebase Anonymous Authentication was failing, causing both to use the same fallback ID.

## Solution Implemented

Modified `FirebaseAuthManager` to generate **device-specific offline IDs** based on each device's unique Android ID.

### Changes Made

#### 1. FirebaseAuthManager.kt

**Added:**
- Android `Context` parameter to constructor
- `generateDeviceSpecificOfflineId()` method that:
  - Reads device's unique Android ID using `Settings.Secure.ANDROID_ID`
  - Creates offline ID as: `offline_user_{first8chars}`
  - Example: `offline_user_abc12345`

**Modified:**
- `ensureAuthenticated()` now generates device-specific ID instead of hardcoded `offline_user_local`
- `getInstance()` companion method accepts optional Context parameter

#### 2. RepositoryProvider.kt

**Modified:**
- `provideRepository()` now passes application context to `FirebaseAuthManager.getInstance()`
- This ensures each device gets its unique Android ID for generating offline user IDs

## Result

Now each emulator gets a **unique, persistent offline ID**:

```
Emulator 1: offline_user_abc12345 (Patient with PIN 151745)
Emulator 2: offline_user_xyz67890 (Caregiver)
```

- Patient profile saved to: `users/offline_user_abc12345/`
- Caregiver profile saved to: `users/offline_user_xyz67890/`
- **No conflicts!** Both profiles exist simultaneously
- PIN query finds the correct patient document

## Testing Instructions

### 1. Clean Up Old Data

First, delete the old `offline_user_local` document from Firebase:

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Firestore Database → `users` collection
3. Delete document with ID: `offline_user_local`
4. Delete document with ID: `offline_user_caregiver` (if exists)

### 2. Test Patient-Caregiver Connection

**Emulator 1 (Patient):**
1. Launch app
2. Select "I'm a Patient"
3. Enter name: "Test Patient"
4. Add at least one medication
5. Go to Settings → Note the 6-digit PIN (e.g., "151745")
6. Keep app running

**Check Logs (Emulator 1):**
```
FirebaseAuth  W  Using offline mode with device-specific ID: offline_user_abc12345
FirebaseRepo  D  👤 Setting profile for user: offline_user_abc12345, role: patient, PIN: 151745
FirebaseRepo  D  ✅ Profile saved successfully
```

**Emulator 2 (Caregiver):**
1. Launch app
2. Select "I'm a Caregiver"
3. Enter name: "Test Caregiver"
4. Tap "Add Patient" → "Enter PIN"
5. Enter patient's PIN: "151745"
6. Tap "Add Patient"
7. **Should succeed!** ✅

**Check Logs (Emulator 2):**
```
FirebaseAuth  W  Using offline mode with device-specific ID: offline_user_xyz67890
CaregiverPatientsVM  D  🔍 Attempting to import patient with PIN: 151745
FirebaseRepo  D  ⚙️ Searching for patient with PIN: 151745
FirebaseRepo  D  📊 Query returned 1 user(s)  ← SUCCESS!
FirebaseRepo  D  ✅ Found patient: Test Patient (ID: offline_user_abc12345)
CaregiverPatientsVM  D  🎉 Successfully imported patient: Test Patient
```

### 3. Verify in Firebase Console

After testing, check Firebase Console → Firestore → `users` collection:

You should see **TWO separate user documents**:
1. `offline_user_abc12345` (Patient)
   - role: "patient"
   - name: "Test Patient"
   - pin: "151745"
   
2. `offline_user_xyz67890` (Caregiver)
   - role: "caregiver"
   - name: "Test Caregiver"
   - pin: "" (empty)

## Technical Details

### Android ID

The Android ID is a unique 64-bit number generated when the device is first set up:
- **Persistent**: Stays the same across app reinstalls
- **Device-specific**: Each emulator has a different Android ID
- **Reliable**: Available on all Android devices
- **Format**: Hexadecimal string (e.g., "abc123def456")

### Offline ID Format

```
offline_user_{first_8_chars_of_android_id}
```

Examples:
- `offline_user_abc12345`
- `offline_user_xyz67890`
- `offline_user_fedcba98`

### Fallbacks

1. **If Android ID unavailable**: Falls back to `offline_user_unknown`
2. **If Context not provided**: Falls back to `offline_user_local`
3. **If any error occurs**: Falls back to `offline_user_local`

## Code Changes Summary

### Files Modified
1. `app/src/main/java/com/example/medicaladherence/data/firebase/FirebaseAuthManager.kt`
   - Added Context parameter
   - Added `generateDeviceSpecificOfflineId()` method
   - Updated `ensureAuthenticated()` to use device-specific IDs
   - Updated `getInstance()` to accept Context

2. `app/src/main/java/com/example/medicaladherence/data/repo/RepositoryProvider.kt`
   - Updated `provideRepository()` to pass context to FirebaseAuthManager
   - Updated `getAuthManager()` with note about context availability

### Lines Changed
- FirebaseAuthManager.kt: ~30 lines added/modified
- RepositoryProvider.kt: ~5 lines modified

## Benefits

✅ **Multiple profiles on same machine**: Test patient and caregiver simultaneously  
✅ **No profile conflicts**: Each device gets unique ID  
✅ **PIN connection works**: Caregiver can successfully connect to patient  
✅ **Persistent IDs**: Same device always gets same offline ID  
✅ **No code changes needed for production**: Works with Firebase Auth when available  
✅ **Backward compatible**: Falls back gracefully if context unavailable  

## Future Improvements

If Firebase Anonymous Authentication is enabled in Firebase Console:
- Both emulators will get real Firebase Auth UIDs automatically
- Offline IDs will only be used as fallback
- Each session gets a unique Firebase UID (more secure)

To enable:
1. Firebase Console → Authentication
2. Sign-in method → Anonymous → Enable
3. Restart emulators → Will use Firebase Auth instead of offline IDs

## Troubleshooting

### Issue: Still seeing `offline_user_local` in logs

**Solution:**
- Wipe emulator data: Settings → Apps → Medical Adherence → Clear Data
- Rebuild and reinstall app
- Check you're running the latest code

### Issue: Both emulators have same device-specific ID

**Solution:**
- Each emulator should have unique Android ID by default
- Verify in emulator: Settings → About Emulated Device → Android ID
- If same, create new emulator with different configuration

### Issue: Patient not found even after fix

**Solution:**
- Delete old `offline_user_local` document from Firebase
- Sign out and recreate profiles on both emulators
- Check logs for actual user IDs being used

---

**This fix enables full testing of patient-caregiver connections on a single development machine with multiple emulators!**

