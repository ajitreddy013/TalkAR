# Camera Preview Fix Summary

## ✅ Fixed: IllegalStateException (Double Renderer Setup)

**Problem**: App was trying to set GLSurfaceView renderer twice, causing `IllegalStateException: setRenderer has already been called for this instance`

**Solution Applied**:
1. Removed initial placeholder renderer from AndroidView factory block
2. Set renderer only once in `setupCameraRenderer()` when ARCore session is ready
3. Added `glSurfaceView.onResume()` call after setting renderer to start GL rendering

**Files Modified**:
- `mobile-app/app/src/main/java/com/talkar/app/ui/components/ArSceneViewComposable.kt`

**Status**: ✅ FIXED - App builds and installs successfully, no more IllegalStateException

---

## 🔄 Current Issues Identified from Logs

### 1. Backend Connection Timeout (CRITICAL)

**Problem**: 
```
E PosterRepository: ❌ Failed to load posters
E PosterRepository: kotlinx.coroutines.TimeoutCancellationException: Timed out waiting for 10000 ms
W ArSceneView: Poster loading timed out, trying test poster
W PosterRepository: test_poster.jpg not found in assets, creating programmatic test image
```

**Root Cause**: 
- Debug build configured to use `https://10.0.2.2:443` (Android emulator localhost)
- Physical device cannot reach this address
- Backend needs to be on same network or publicly accessible

**Solution Options**:

#### Option A: Use Local Network IP (Recommended for Development)
1. Find your computer's local IP address:
   ```bash
   # macOS/Linux
   ifconfig | grep "inet " | grep -v 127.0.0.1
   
   # Or on macOS
   ipconfig getifaddr en0
   ```

2. Update `mobile-app/gradle.properties` (create if doesn't exist):
   ```properties
   # Replace with your computer's actual IP address
   API_HOST=192.168.1.XXX
   API_PORT=4000
   API_PROTOCOL=http
   ```

3. Ensure backend is running and accessible:
   ```bash
   # Test from your device's browser
   http://192.168.1.XXX:4000/api/v1/images
   ```

#### Option B: Use Production Backend
Update `mobile-app/gradle.properties`:
```properties
API_HOST=talkar-backend.onrender.com
API_PORT=443
API_PROTOCOL=https
```

#### Option C: Use ngrok Tunnel (Quick Testing)
```bash
# On your computer where backend runs
ngrok http 4000

# Copy the https URL (e.g., https://abc123.ngrok.io)
# Update gradle.properties:
API_HOST=abc123.ngrok.io
API_PORT=443
API_PROTOCOL=https
```

---

### 2. AR Tracking Instability

**Problems Identified**:
- Low inliers: `Insufficient inliers or feature ratio ( 3 / 55)`
- RANSAC failures: `RANSAC failed... reporting degeneracy`
- Fast movement: `VIO is moving fast with speed (m/s): 1.10141`
- IMU buffer issues: `IMU buffer is empty or has only one sample`
- Map optimization failures: `MAP SOLVE... Reason: NO_CONVERGENCE`

**Root Causes**:
1. **Using programmatic test image** - Plain colored bitmap with no texture/features
2. **Device movement too fast** - ARCore needs slower, steadier movement
3. **IMU sensor lag** - Gyro/accelerometer data not reaching ARCore fast enough
4. **Insufficient keyframes** - Not enough visual landmarks for 3D map building

**Solutions**:

#### Immediate: Fix Backend Connection (Priority 1)
Once backend is connected, app will load real poster images with actual features/texture that ARCore can track.

#### ARCore Configuration Improvements
Add to `ArSceneViewComposable.kt`:

```kotlin
// In createARSession(), update config:
val config = Config(session).apply {
    // Enable plane finding for better tracking
    planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
    
    // Enable light estimation for better rendering
    lightEstimationMode = Config.LightEstimationMode.AMBIENT_INTENSITY
    
    // Enable depth for better occlusion (if device supports)
    if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
        depthMode = Config.DepthMode.AUTOMATIC
    }
    
    // Optimize for poster tracking
    updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
    focusMode = Config.FocusMode.AUTO
    
    // Augmented images config
    augmentedImageDatabase = imageDatabase
}
```

#### User Guidance
Add instructions to UI:
- "Hold device steady"
- "Move slowly when scanning"
- "Ensure good lighting"
- "Point at poster with clear features"

---

### 3. Test Poster Quality

**Problem**: Programmatic test bitmap (512x512 solid color) has no trackable features

**Solution**: Add a real test image to assets

1. Create `mobile-app/app/src/main/assets/test_poster.jpg`
2. Use an image with:
   - High contrast
   - Clear features/edges
   - Human face (for face detection)
   - At least 512x512 resolution
   - Good JPEG quality

---

## 📋 Action Items (Priority Order)

### Priority 1: Fix Backend Connection (BLOCKING)
- [ ] Choose connection method (local IP, production, or ngrok)
- [ ] Update `gradle.properties` with correct API_HOST
- [ ] Verify backend is accessible from device
- [ ] Rebuild and test app

### Priority 2: Add Real Test Poster
- [ ] Add `test_poster.jpg` to assets folder
- [ ] Use image with human face and clear features
- [ ] Test poster detection with real image

### Priority 3: Improve AR Tracking
- [ ] Enable plane finding and light estimation
- [ ] Add user guidance UI for steady movement
- [ ] Test with real poster from backend

### Priority 4: Optimize Performance
- [ ] Monitor CPU usage during AR tracking
- [ ] Reduce video bitrate if needed
- [ ] Add frame rate limiting if necessary

---

## 🧪 Testing Checklist

### Backend Connection Test
```bash
# 1. Check backend is running
curl http://YOUR_IP:4000/api/v1/images

# 2. Rebuild app
cd mobile-app
./gradlew clean assembleDebug

# 3. Install on device
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 4. Monitor logs
adb logcat | grep -E "PosterRepository|ApiConfig|Backend"
```

### Expected Success Logs
```
D ApiConfig: Initializing ApiConfig with BASE_URL: http://YOUR_IP:4000
D PosterRepository: 🔍 Loading posters from backend...
D PosterRepository: 📥 Found X images from backend
D PosterRepository: ✅ Downloaded XXXX bytes
D PosterRepository: ✅ Successfully loaded X posters
D ArSceneView: ✅ ARCore initialized successfully with X posters
```

### AR Tracking Test
1. Point camera at poster with human face
2. Move device slowly and steadily
3. Check for tracking logs:
   ```
   D ARTrackingManager: Poster detected: [poster_name]
   D ARTrackingManager: Tracking state: TRACKING
   ```

---

## 📊 Current Status

| Component | Status | Notes |
|-----------|--------|-------|
| ARCore Crash | ✅ FIXED | No more segmentation faults |
| Renderer Setup | ✅ FIXED | No more IllegalStateException |
| Backend Connection | ❌ BLOCKED | Timeout - needs network config |
| Poster Loading | ⚠️ FALLBACK | Using programmatic test image |
| AR Tracking | ⚠️ UNSTABLE | Low inliers, needs real poster |
| Camera Preview | ❓ UNKNOWN | Need user confirmation |
| GL Rendering | ❓ UNKNOWN | No GL logs yet (may need frames) |

---

## 🎯 Next Steps

1. **User Action Required**: 
   - Confirm if camera preview is visible on device screen
   - If black screen persists, we'll add more GL debugging

2. **Fix Backend Connection**:
   - Choose connection method
   - Update gradle.properties
   - Rebuild and test

3. **Verify Poster Loading**:
   - Check logs for successful poster download
   - Verify ARCore image database creation

4. **Test AR Tracking**:
   - Point at real poster
   - Verify detection and tracking
   - Check video overlay rendering

---

## 📝 Files Modified

1. `mobile-app/app/src/main/java/com/talkar/app/ui/components/ArSceneViewComposable.kt`
   - Removed double renderer setup
   - Added onResume() call
   - Fixed IllegalStateException

---

## 🔗 Related Documents

- `FIX_PROGRESS_SUMMARY.md` - Overall progress tracking
- `ARCORE_CRASH_DIAGNOSIS.md` - ARCore crash analysis
- `BACKEND_CONNECTION_GUIDE.md` - Backend setup instructions

---

**Last Updated**: February 27, 2026
**Status**: Camera renderer fixed, backend connection needed
