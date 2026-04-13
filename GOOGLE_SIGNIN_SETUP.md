# Google Sign-In Setup Guide

Google Sign-In has been successfully integrated into your EasyDrive login page.

## What Was Added

1. **Dependencies** (app/build.gradle.kts)
   - Added Google Play Services Auth library for Google Sign-In

2. **UI Updates** (activity_login.xml)
   - Added "OR" divider between email/password login and Google Sign-In
   - Added "Continue with Google" button with Google icon

3. **Code Implementation** (LoginActivity.kt)
   - Configured GoogleSignInClient with Firebase integration
   - Added Google Sign-In button click handler
   - Implemented firebaseAuthWithGoogle() method
   - Auto-creates user profile in Firestore for new Google users
   - Handles both existing and new users seamlessly

## IMPORTANT: Firebase Console Setup Required

Before Google Sign-In will work, you MUST complete these steps in Firebase Console:

### Step 1: Enable Google Sign-In in Firebase
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project: **easydrive-96485**
3. Navigate to **Authentication** > **Sign-in method**
4. Click on **Google** provider
5. Click **Enable** toggle
6. Save the changes

### Step 2: Get Your Web Client ID
1. In the same Google Sign-In settings, you'll see **Web SDK configuration**
2. Copy the **Web client ID** (it looks like: `123456789-abcdefg.apps.googleusercontent.com`)

### Step 3: Add Web Client ID to Your Code
1. Open `app/src/main/java/com/example/easydrive/LoginActivity.kt`
2. Find line 63: `val webClientId = "YOUR_WEB_CLIENT_ID_HERE"`
3. Replace `YOUR_WEB_CLIENT_ID_HERE` with your actual Web Client ID
4. Example: `val webClientId = "335037242068-abc123xyz.apps.googleusercontent.com"`

### Step 4: Add SHA-1 Fingerprint
1. Get your SHA-1 fingerprint by running in terminal:
   ```bash
   ./gradlew signingReport
   ```
2. Copy the SHA-1 from the debug variant
3. In Firebase Console, go to **Project Settings** > **Your apps**
4. Click on your Android app
5. Scroll down and click **Add fingerprint**
6. Paste your SHA-1 fingerprint
7. Click **Save**

### Step 5: Download Updated google-services.json
1. After adding the SHA-1, download the updated `google-services.json`
2. Replace the existing file in `app/google-services.json`
3. The new file will have OAuth client information

## How It Works

1. User clicks "Continue with Google" button
2. Google Sign-In dialog appears
3. User selects their Google account
4. Firebase authenticates the user
5. If new user: Creates profile in Firestore with "Renter" type
6. If existing user: Retrieves user type from Firestore
7. Navigates to appropriate screen (MainActivity or AgencyDashboardActivity)

## Testing

After completing the setup steps above:
1. Sync Gradle files in Android Studio
2. Build and run the app
3. Click "Continue with Google" on the login screen
4. Select a Google account
5. You should be logged in and redirected to the home screen

## Troubleshooting

**Build Error: "Unresolved reference: default_web_client_id"**
- This is fixed by using a hardcoded Web Client ID instead
- Make sure you've replaced `YOUR_WEB_CLIENT_ID_HERE` with your actual Web Client ID

**Google Sign-In doesn't work:**
1. Verify SHA-1 fingerprint is added in Firebase Console
2. Ensure google-services.json is up to date
3. Check that Google Sign-In is enabled in Firebase Authentication
4. Make sure the Web Client ID is correct
5. Verify the app package name matches Firebase configuration

**"Developer Error" or "Sign-in failed":**
- This usually means SHA-1 fingerprint is missing or incorrect
- Re-run `./gradlew signingReport` and add the SHA-1 to Firebase Console
