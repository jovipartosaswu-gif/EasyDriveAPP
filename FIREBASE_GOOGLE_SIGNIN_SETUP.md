# Firebase Google Sign-In Configuration Guide

Follow these steps to connect Google Sign-In to your Firebase project.

## Step 1: Enable Google Sign-In in Firebase Console

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project: **easydrive-96485**
3. In the left sidebar, click **Authentication**
4. Click on the **Sign-in method** tab
5. Find **Google** in the list of providers
6. Click on **Google** to open settings
7. Toggle the **Enable** switch to ON
8. You'll see two fields:
   - **Project support email**: Select your email from dropdown
   - **Project public-facing name**: Should show "EasyDrive" (or your app name)
9. Click **Save**

## Step 2: Get Your Web Client ID

After enabling Google Sign-In, you'll see the configuration details:

1. In the same Google Sign-In settings page, look for **Web SDK configuration**
2. You'll see **Web client ID** - it looks like:
   ```
   335037242068-xxxxxxxxxxxxxxxxx.apps.googleusercontent.com
   ```
3. **Copy this Web client ID** - you'll need it in Step 4

## Step 3: Add SHA-1 Fingerprint

### For Debug Build (Development):

1. Open Terminal/Command Prompt in your project directory
2. Run this command:
   ```bash
   ./gradlew signingReport
   ```
3. Look for the **debug** variant section in the output
4. Copy the **SHA1** fingerprint (looks like: `AA:BB:CC:DD:...`)

### For Release Build (Production):

If you have a release keystore, get its SHA-1:
```bash
keytool -list -v -keystore your-release-key.keystore -alias your-key-alias
```

### Add SHA-1 to Firebase:

1. In Firebase Console, click the **gear icon** (⚙️) next to "Project Overview"
2. Click **Project settings**
3. Scroll down to **Your apps** section
4. Find your Android app (com.example.easydrive)
5. Click **Add fingerprint**
6. Paste your SHA-1 fingerprint
7. Click **Save**

**Important**: Add both debug and release SHA-1 fingerprints if you have both!

## Step 4: Download Updated google-services.json

1. Still in **Project settings** > **Your apps**
2. Click the **Download google-services.json** button
3. Replace the existing file at: `app/google-services.json`
4. The new file will contain OAuth client information

## Step 5: Update LoginActivity with Web Client ID

1. Open `app/src/main/java/com/example/easydrive/LoginActivity.kt`
2. Find line 63 (in the `onCreate` method):
   ```kotlin
   val webClientId = "YOUR_WEB_CLIENT_ID_HERE"
   ```
3. Replace with your actual Web Client ID from Step 2:
   ```kotlin
   val webClientId = "335037242068-xxxxxxxxxxxxxxxxx.apps.googleusercontent.com"
   ```

## Step 6: Sync and Build

1. In Android Studio, click **File** > **Sync Project with Gradle Files**
2. Wait for sync to complete
3. Build and run your app
4. Test the Google Sign-In button

## Verification Checklist

Before testing, make sure:
- ✅ Google Sign-In is enabled in Firebase Console
- ✅ SHA-1 fingerprint is added to Firebase
- ✅ Web Client ID is copied to LoginActivity.kt
- ✅ google-services.json is updated (optional but recommended)
- ✅ Project is synced and built successfully

## Testing Google Sign-In

1. Run the app on your device/emulator
2. Click "Continue with Google" button
3. Select a Google account
4. Grant permissions if asked
5. You should be logged in and redirected to the home screen

## Troubleshooting

### Error: "Developer Error" or "Sign-in failed"
**Cause**: SHA-1 fingerprint is missing or incorrect
**Solution**: 
- Run `./gradlew signingReport` again
- Verify the SHA-1 in Firebase Console matches exactly
- Wait 5-10 minutes after adding SHA-1 for changes to propagate

### Error: "API not enabled"
**Cause**: Google Sign-In API is not enabled
**Solution**: 
- Go to Firebase Console > Authentication > Sign-in method
- Make sure Google provider is enabled (toggle should be ON)

### Error: "Invalid client ID"
**Cause**: Wrong Web Client ID in code
**Solution**:
- Double-check the Web Client ID in Firebase Console
- Make sure you copied the **Web client ID**, not Android client ID
- Verify it's pasted correctly in LoginActivity.kt (no extra spaces)

### Sign-in works but user data not saved
**Cause**: Firestore permissions or network issue
**Solution**:
- Check Firestore rules allow writes
- Check device internet connection
- Look at Logcat for error messages

## What Happens After Sign-In

When a user signs in with Google:

1. **New User**:
   - Profile is created in Firestore `users` collection
   - User type is set to "Renter" by default
   - User is redirected to MainActivity

2. **Existing User**:
   - User type is retrieved from Firestore
   - User is redirected to appropriate screen:
     - "Renter" → MainActivity
     - "Agency" → AgencyDashboardActivity

## Security Notes

- Never commit your Web Client ID to public repositories
- Consider using BuildConfig or local.properties for sensitive IDs
- Always use release SHA-1 for production builds
- Keep your google-services.json file secure

## Need Help?

If you encounter issues:
1. Check Android Studio Logcat for error messages
2. Verify all steps above are completed
3. Try cleaning and rebuilding: **Build** > **Clean Project** > **Rebuild Project**
4. Make sure you're using a real device or emulator with Google Play Services

---

**Your Firebase Project Details:**
- Project ID: easydrive-96485
- Project Number: 335037242068
- Package Name: com.example.easydrive
