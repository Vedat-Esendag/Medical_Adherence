# Firebase Firestore Cleanup Guide

This guide provides instructions for cleaning up test data in Firebase Firestore for the Medical Adherence app.

---

## Why Clean Up?

After development and testing, your Firestore database may contain:

- **Test user profiles** with incomplete or invalid data
- **Offline user documents** created during network-free testing (e.g., `offline_user_*`)
- **Orphaned data** from sign-out operations that were interrupted
- **Cached profiles** that may cause auto-login issues

Cleaning this data ensures:
- ✅ ProfileSelectionScreen shows correctly on app launch
- ✅ No auto-login from stale cached profiles
- ✅ Clean testing environment
- ✅ Accurate data for production deployment

---

## Manual Cleanup (Firebase Console)

### Step 1: Access Firebase Console

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Sign in with your Google account
3. Select your **Medical Adherence** project
4. Click **Firestore Database** in the left sidebar

### Step 2: Navigate to Users Collection

1. In Firestore Database, you'll see the collections panel
2. Click on the **`users`** collection
3. You'll see a list of user documents (each with a unique ID)

### Step 3: Identify Test Users

Look for documents with IDs matching these patterns:

**Offline Users (Created During Network-Free Testing):**
```
offline_user_local
offline_user_12345678
offline_user_*
```

**Anonymous Firebase Users (Testing):**
```
[Random 28-character Firebase UID]
Example: yBxK9mNpQrXz3vLfJhGtWcPsR2M1
```

**How to identify test users:**
- Click on each document to view its contents
- Check the `name` field - test names like "Test User", "John Doe", "Jane Smith"
- Check the `role` field - should be either "patient" or "caregiver"
- Check the `pin` field - patients have a 6-digit PIN

### Step 4: Delete Test Users

For each test user document:

1. Click the document ID to open it
2. Click the **three dots menu** (⋮) in the top right
3. Select **Delete document**
4. Confirm deletion in the dialog

**⚠️ IMPORTANT:** Do NOT delete production users or users you want to keep!

### Step 5: Clean Up Subcollections (Optional)

Each user document may have subcollections:
- `medications/` - Patient's medication list
- `doseEvents/` - Dose taking history
- `settings/` - App settings

**These are automatically cleaned when you delete the parent user document.**

However, if you want to verify:
1. Expand a user document in the console
2. Check for subcollections
3. They should be empty after deleting the parent document

### Step 6: Clean Up Caregiver Links (If Applicable)

If you've been testing the caregiver feature:

1. Navigate to the **`caregiver_links`** collection
2. Delete any test caregiver-patient links
3. Look for entries matching your test user PINs

**How to identify test links:**
- Check `patientPin` field
- Check `patientName` field
- Match against the users you deleted

### Step 7: Verify Cleanup

After deletion:

1. Refresh the Firestore console
2. Verify `users` collection is empty (or only contains production users)
3. Verify `caregiver_links` collection is empty (if you deleted everything)

---

## Safety Considerations

### Before You Delete

**1. Backup Important Data (Production Only)**

If your app is in production with real users:

1. Go to Firestore Database → **Data** tab
2. Click **Export/Import** at the top
3. Export data before doing bulk deletions
4. Save export to Google Cloud Storage

**2. Identify Production Users**

If mixing test and production data:
- Check `name` field for real names vs test names
- Check timestamps (recent users may be real)
- Check PIN usage (linked caregivers indicate real users)

**3. Test Environment Separation**

**Best Practice:** Use separate Firebase projects for development and production:
- `medical-adherence-dev` - For testing, can delete freely
- `medical-adherence-prod` - For production, be careful

### Data You Should NEVER Delete

❌ **Do NOT delete:**
- Production user profiles (real patients)
- Real medication data
- Real dose events (adherence history)
- Real caregiver links
- System collections (if any added later)

✅ **Safe to delete:**
- `offline_user_*` documents
- Test users with names like "Test", "Demo", "John Doe"
- Users created during development
- Orphaned caregiver links with deleted patient PINs

---

## Alternative: Automated Cleanup Script

For faster cleanup, especially during active development, use the automated script:

**See `scripts/cleanup_firebase.sh`** (created alongside this document)

The script uses Firebase CLI to:
- Query and list all offline users
- Delete users matching test patterns
- Clean up orphaned caregiver links
- Provide safety confirmations before deletion

---

## What Happens After Cleanup?

### Expected App Behavior

1. **First Launch After Cleanup:**
   - App shows **ProfileSelectionScreen**
   - No auto-login occurs
   - Clean slate for testing

2. **Data Loss:**
   - All deleted users lose their:
     - Profile information (name, role, PIN)
     - Medications list
     - Dose events (adherence history)
     - Settings (font size, high contrast mode, caretaker PIN)

3. **Offline Users:**
   - If you delete `offline_user_local`, any offline data is lost
   - Next offline session creates a new `offline_user_local` document

### App Still Remembers (Not Affected by Cleanup)

- **Firebase Anonymous Auth:** Auth records persist separately
  - These don't interfere with profile selection
  - Firebase manages these automatically
- **Local App Cache:** Firestore offline cache on device
  - Clear app data on device to fully reset: Settings → Apps → Medical Adherence → Clear Data

---

## Firestore Structure Reference

For reference, here's the complete Firestore structure:

```
firestore/
├── users/
│   └── {userId}/
│       ├── role: "patient" | "caregiver"
│       ├── name: String
│       ├── pin: String (6 digits for patients)
│       ├── medications/
│       │   └── {medicationId}/
│       │       ├── name: String
│       │       ├── dosage: String
│       │       ├── times: List<String>
│       │       ├── notes: String
│       │       └── frequency: String
│       ├── doseEvents/
│       │   └── {eventId}/
│       │       ├── medId: String
│       │       ├── date: String (ISO format)
│       │       ├── time: String (HH:mm)
│       │       └── taken: Boolean
│       └── settings/
│           └── app_settings/
│               ├── fontScale: Float
│               ├── highContrastMode: Boolean
│               └── caretakerPin: String (optional)
└── caregiver_links/
    └── {linkId}/
        ├── caregiverUserId: String
        ├── patientUserId: String
        ├── patientPin: String
        ├── patientName: String
        └── addedAt: Timestamp
```

---

## Troubleshooting

### Issue: Can't see users collection in Firestore Console

**Cause:** No data has been created yet

**Solution:**
1. Launch the app
2. Select a profile (Patient or Caregiver)
3. Return to Firestore console and refresh
4. `users` collection should now appear

### Issue: Deleted users but app still auto-logs in

**Cause:** Local Firestore cache on device

**Solution:**
1. On Android device/emulator: Settings → Apps → Medical Adherence → Clear Data
2. Restart app
3. Should show ProfileSelectionScreen

### Issue: Can't delete documents (Permission denied)

**Cause:** Insufficient Firebase console permissions

**Solution:**
1. Verify you're logged in as project owner
2. Check Firebase project settings → Users & Permissions
3. Ensure you have "Editor" or "Owner" role

### Issue: Deleted user but subcollections remain

**Cause:** Firestore doesn't auto-delete subcollections

**Solution:**
1. Manually delete subcollections first
2. Then delete parent document
3. Or use the automated script which handles this

---

## Bulk Deletion (Advanced)

For very large datasets, use Firebase CLI in terminal:

### Prerequisites

```bash
# Install Firebase CLI
npm install -g firebase-tools

# Login to Firebase
firebase login

# Set project
firebase use <your-project-id>
```

### Delete All Users (⚠️ USE WITH CAUTION)

```bash
# List all users
firebase firestore:delete users --recursive --yes

# This deletes EVERYTHING in users collection!
```

### Delete Specific Pattern (Safer)

Unfortunately, Firebase CLI doesn't support pattern matching directly. You'll need to use the provided shell script or manually script the deletions.

**See `scripts/cleanup_firebase.sh` for a safer scripted approach.**

---

## Best Practices for Future Development

1. **Use Emulator for Testing:**
   - Set up Firebase Emulator Suite
   - Test locally without touching production Firestore
   - Data is automatically wiped between sessions

2. **Separate Projects:**
   - Development: `medical-adherence-dev`
   - Production: `medical-adherence-prod`

3. **Naming Conventions:**
   - Prefix test users: `test_*`, `dev_*`
   - Easy to identify and bulk delete later

4. **Regular Cleanup:**
   - Clean up weekly during active development
   - Prevents accumulation of stale data

5. **Document Production Users:**
   - Keep a list of production user IDs
   - Never delete these, even accidentally

---

## Related Documentation

- **Recent Fixes:** See `docs/RECENT_FIXES.md` for context on why cleanup is important
- **Automated Script:** See `scripts/cleanup_firebase.sh` for scripted cleanup
- **Database Schema:** See `docs/technical/05-database.md` for Firestore structure
- **State Management:** See `docs/technical/04-state-management.md` for ViewModel behavior

---

**When in doubt, backup before deleting!**

