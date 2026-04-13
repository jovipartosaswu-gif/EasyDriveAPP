# How to Get SHA-1 Fingerprint

## Error Code 10 Explanation
Error code 10 means: "Developer Error" - Your SHA-1 fingerprint is not registered in Firebase Console.

## Quick Fix - Get SHA-1 Using Command

### Option 1: Using keytool directly (Easiest)

Open Command Prompt or PowerShell and run:

```bash
keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

This will show output like:
```
Alias name: androiddebugkey
Creation date: ...
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Android Debug, O=Android, C=US
Issuer: CN=Android Debug, O=Android, C=US
Serial number: 1
Valid from: ... until: ...
Certificate fingerprints:
         SHA1: AA:BB:CC:DD:EE:FF:11:22:33:44:55:66:77:88:99:00:AA:BB:CC:DD
         SHA256: ...
```

**Copy the SHA1 line** (the part after "SHA1: ")

### Option 2: Using Android Studio

1. Open Android Studio
2. Click **View** menu > **Tool Windows** > **Terminal**
3. In the terminal at the bottom, type:
   ```
   keytool -list -v -keystore %USERPROFILE%\.android\debug.keystore -alias androiddebugkey -storepass android -keypass android
   ```
4. Press Enter
5. Copy the SHA1 value

### Option 3: Check if Java is installed

If the above commands don't work, you might need to use the full path to keytool:

1. Find your Java installation (usually in Android Studio):
   - Windows: `C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe`
   
2. Run:
   ```
   "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
   ```

## After Getting SHA-1

1. **Copy the SHA-1** (looks like: `AA:BB:CC:DD:EE:FF:11:22:33:44:55:66:77:88:99:00:AA:BB:CC:DD`)

2. **Go to Firebase Console**:
   - Open: https://console.firebase.google.com/
   - Select project: **easydrive-96485**
   - Click the **gear icon** ⚙️ next to "Project Overview"
   - Click **Project settings**
   - Scroll down to **Your apps** section
   - Find your Android app (com.example.easydrive)
   - Click **Add fingerprint** button
   - Paste your SHA-1
   - Click **Save**

3. **Wait 5 minutes** for Firebase to update

4. **Rebuild and test** your app

## Still Not Working?

If you can't run the keytool command, try this:

1. In Android Studio, go to **Build** > **Generate Signed Bundle / APK**
2. Select **APK**
3. Click **Next**
4. If you don't have a keystore, click **Create new...**
5. This process will show you the SHA-1 fingerprint

## Alternative: Get SHA-1 from Google Play Console

If you've uploaded your app to Google Play Console:
1. Go to Google Play Console
2. Select your app
3. Go to **Setup** > **App signing**
4. Copy the SHA-1 certificate fingerprint
5. Add it to Firebase Console

---

**Your Firebase Project:**
- Project ID: easydrive-96485
- Package Name: com.example.easydrive
- Web Client ID: 335037242068-liqttd6uj8tetki8su5icsdfjiu39i5p.apps.googleusercontent.com ✅ (Already configured)

**What's Missing:**
- SHA-1 Fingerprint ❌ (Need to add this to Firebase)
