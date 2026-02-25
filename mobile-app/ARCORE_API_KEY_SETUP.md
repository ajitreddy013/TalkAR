# ARCore API Key Setup

## Overview

The TalkAR app requires an ARCore API key for cloud-based AR features like Cloud Anchors. Without this key, you'll see a warning in the logs:

```
The API key for use with the Google AR service could not be obtained!
```

## Getting Your ARCore API Key

1. **Go to Google Cloud Console**
   - Visit: https://console.cloud.google.com/

2. **Create or Select a Project**
   - Create a new project or select an existing one
   - Project name example: "TalkAR"

3. **Enable ARCore API**
   - Go to: https://console.cloud.google.com/apis/library
   - Search for "ARCore API"
   - Click "Enable"

4. **Create API Credentials**
   - Go to: https://console.cloud.google.com/apis/credentials
   - Click "Create Credentials" → "API Key"
   - Copy the generated API key

5. **Restrict the API Key (Recommended)**
   - Click on the created API key to edit it
   - Under "Application restrictions":
     - Select "Android apps"
     - Click "Add an item"
     - Package name: `com.talkar.app`
     - SHA-1 certificate fingerprint: Get from your keystore
   - Under "API restrictions":
     - Select "Restrict key"
     - Select "ARCore API"
   - Click "Save"

## Getting SHA-1 Fingerprint

### For Debug Builds:
```bash
cd mobile-app
./gradlew signingReport
```

Look for the SHA-1 under "Variant: debug"

### For Release Builds:
```bash
keytool -list -v -keystore /path/to/your/keystore.jks -alias your_alias
```

## Adding the API Key to Your App

1. **Open AndroidManifest.xml**
   ```
   mobile-app/app/src/main/AndroidManifest.xml
   ```

2. **Replace the dummy API key**
   Find this line:
   ```xml
   <meta-data
       android:name="com.google.ar.core.API_KEY"
       android:value="AIzaSyDummy_Replace_With_Your_Actual_ARCore_API_Key" />
   ```

   Replace with your actual key:
   ```xml
   <meta-data
       android:name="com.google.ar.core.API_KEY"
       android:value="AIzaSyYourActualAPIKeyHere" />
   ```

3. **Rebuild the app**
   ```bash
   cd mobile-app
   ./gradlew clean assembleDebug
   ```

## Security Best Practices

### For Development:
- Use a separate API key for development
- Restrict to debug SHA-1 fingerprint only

### For Production:
- Use a different API key for production
- Restrict to release SHA-1 fingerprint only
- Store in environment variables or secure vault
- Never commit API keys to version control

### Using Environment Variables (Recommended):

1. **Add to local.properties** (not committed to git):
   ```properties
   ARCORE_API_KEY=AIzaSyYourActualAPIKeyHere
   ```

2. **Update build.gradle**:
   ```gradle
   android {
       defaultConfig {
           // Load from local.properties
           def localProperties = new Properties()
           localProperties.load(new FileInputStream(rootProject.file("local.properties")))
           
           manifestPlaceholders = [
               ARCORE_API_KEY: localProperties.getProperty("ARCORE_API_KEY", "")
           ]
       }
   }
   ```

3. **Update AndroidManifest.xml**:
   ```xml
   <meta-data
       android:name="com.google.ar.core.API_KEY"
       android:value="${ARCORE_API_KEY}" />
   ```

## Verifying the Setup

After adding the API key, check the logs when launching the app:

**Before (with dummy key):**
```
The API key for use with the Google AR service could not be obtained!
```

**After (with valid key):**
```
ARCore API key validated successfully
```

## Troubleshooting

### "API key not valid" error:
- Verify the key is correctly copied (no extra spaces)
- Check that ARCore API is enabled in Google Cloud Console
- Verify SHA-1 fingerprint matches your app's signing certificate

### "Permission denied" error:
- Check API restrictions in Google Cloud Console
- Ensure package name matches: `com.talkar.app`
- Verify SHA-1 fingerprint is correct

### Still seeing warning after adding key:
- Clean and rebuild: `./gradlew clean assembleDebug`
- Uninstall and reinstall the app
- Check that the key is in the correct meta-data tag

## Cost Considerations

- ARCore API has a free tier
- Cloud Anchors may incur costs after free quota
- Monitor usage in Google Cloud Console
- Set up billing alerts

## References

- [ARCore API Documentation](https://developers.google.com/ar/develop/java/enable-arcore)
- [Google Cloud Console](https://console.cloud.google.com/)
- [ARCore Pricing](https://developers.google.com/ar/pricing)
