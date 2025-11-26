# Beta Distribution Guide

## Prerequisites
- Google Play Console access
- Beta APK built and ready (`mobile-app/app/build/outputs/apk/beta/app-beta.apk`)
- List of tester emails

## Option 1: Google Play Internal Testing (Recommended)

### Step 1: Upload Beta Build
1. Log in to [Google Play Console](https://play.google.com/console)
2. Select your app (TalkAR)
3. Navigate to **Testing** → **Internal testing**
4. Click **Create new release**
5. Upload the beta APK: `app-beta.apk`
6. Add release notes:
   ```
   TalkAR Beta v1.0-beta
   - In-app feedback system
   - Performance improvements
   - Bug fixes
   ```
7. Click **Save** and then **Review release**
8. Click **Start rollout to Internal testing**

### Step 2: Add Testers
1. In the Internal testing page, scroll to **Testers**
2. Click **Create email list**
3. Name it "TalkAR Beta Testers"
4. Add tester emails (one per line)
5. Save the list

### Step 3: Share Opt-In Link
1. Copy the opt-in URL from the Internal testing page
2. Send it to your testers via email:

```
Subject: Join TalkAR Beta Testing

Hi [Tester Name],

You've been invited to join the TalkAR Beta testing program!

To get started:
1. Click this link: [OPT-IN URL]
2. Accept the invitation
3. Download TalkAR Beta from the Play Store
4. Follow the testing guide: [LINK TO BETA_TESTING_GUIDE.md]

Thank you for helping us improve TalkAR!

Best,
[Your Name]
```

## Option 2: Firebase App Distribution

### Step 1: Set Up Firebase
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Navigate to **App Distribution**
4. Click **Get started**

### Step 2: Upload APK
```bash
cd mobile-app
firebase appdistribution:distribute app/build/outputs/apk/beta/app-beta.apk \
  --app YOUR_FIREBASE_APP_ID \
  --groups "beta-testers" \
  --release-notes "TalkAR Beta v1.0-beta - Feedback system and improvements"
```

### Step 3: Add Testers
1. In Firebase Console, go to **App Distribution** → **Testers & Groups**
2. Create a group called "beta-testers"
3. Add tester emails
4. Testers will receive an email with download instructions

## Option 3: Direct APK Distribution

### Step 1: Locate Beta APK
```bash
cd mobile-app
ls -lh app/build/outputs/apk/beta/app-beta.apk
```

### Step 2: Share APK
1. Upload the APK to a secure location (Google Drive, Dropbox, etc.)
2. Share the link with testers
3. Include installation instructions:

```
To install TalkAR Beta:
1. Download the APK from: [LINK]
2. On your Android device, go to Settings → Security
3. Enable "Install from Unknown Sources"
4. Open the downloaded APK file
5. Follow the installation prompts
```

## Post-Distribution

### Verify Installation
1. Ask 2-3 testers to confirm successful installation
2. Check for any installation errors
3. Verify the app shows "TalkAR Beta" in the app name

### Monitor Feedback
1. Check the backend database for incoming feedback:
   ```sql
   SELECT * FROM beta_feedbacks ORDER BY created_at DESC LIMIT 10;
   ```
2. Monitor crash reports (if Firebase Crashlytics is set up)
3. Track analytics for usage patterns

## Troubleshooting

### Testers Can't Find the App
- Verify they accepted the opt-in invitation
- Check if the release is rolled out
- Ensure their email is in the tester list

### Installation Fails
- Verify the APK is signed correctly
- Check Android version compatibility (minSdk 24)
- Ensure sufficient storage space

### App Crashes on Launch
- Check crash logs in Firebase Crashlytics
- Verify backend API is accessible
- Test on a similar device

## Next Steps
After distribution, refer to:
- [BETA_TESTING_GUIDE.md](./BETA_TESTING_GUIDE.md) for tester instructions
- [BETA_FEEDBACK_ANALYSIS_TEMPLATE.md](./BETA_FEEDBACK_ANALYSIS_TEMPLATE.md) for analyzing feedback
