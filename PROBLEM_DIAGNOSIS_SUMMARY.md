# Problem Diagnosis Summary

## User's Expected Flow
1. User scans girl photo with camera
2. Backend detects photo and creates deep-sync video
3. Lip coordinates extracted from video
4. Lips overlaid on live camera feed
5. User sees talking photo with moving lips

## Current Problems (5 Critical Issues)

### 🔴 Problem 1: Black Screen - No Camera Preview
**Status**: Camera texture configured but not rendered  
**Impact**: User sees black/dark gray screen  
**Location**: `ArSceneViewComposable.kt` - `onDrawFrame()` method  
**Fix**: Implement OpenGL shaders to draw camera texture  
**Time**: 2-3 hours  

### 🔴 Problem 2: Poster Detection Disabled
**Status**: Mock data mode enabled  
**Impact**: Real girl photo never loaded, ARCore can't detect it  
**Location**: `PosterRepository.kt` line 23  
**Fix**: Change `USE_MOCK_DATA = false`  
**Time**: 5 minutes  

### 🔴 Problem 3: Backend Not Connected
**Status**: Mock mode bypasses backend entirely  
**Impact**: No real poster data, no video generation  
**Location**: `PosterRepository.kt` + `ApiConfig.kt`  
**Fix**: Disable mock mode, verify backend URL  
**Time**: 1 hour  

### 🔴 Problem 4: Video Generation Failing
**Status**: Backend unreachable or misconfigured  
**Impact**: "Failed to generate lip sync video" error  
**Location**: `BackendVideoFetcherImpl.kt`  
**Fix**: Add logging, verify backend endpoints, improve error handling  
**Time**: 2-3 hours  

### 🟡 Problem 5: Lip Overlay Not Rendering
**Status**: Component exists but may not be fully implemented  
**Impact**: Even if video works, lips won't show on camera  
**Location**: `LipRegionOverlay.kt` + `TalkingPhotoViewModel.kt`  
**Fix**: Implement overlay rendering and transform calculation  
**Time**: 3-4 hours  

## Root Cause Analysis

### Why Camera is Black
```kotlin
// Current code in onDrawFrame():
if (camera.trackingState == TrackingState.TRACKING) {
    // ❌ Missing: Draw camera background texture
    trackingManager.processFrame(frame)
}

// What's needed:
if (camera.trackingState == TrackingState.TRACKING) {
    // ✅ Add this: Render camera texture to screen
    drawCameraBackground(frame, cameraTextureId)
    trackingManager.processFrame(frame)
}
```

### Why Poster Detection Fails
```kotlin
// Current setting:
private const val USE_MOCK_DATA = true  // ← Bypasses backend

// Mock data creates:
- Red square labeled "Mock Poster 1"
- Blue square labeled "Mock Poster 2"  
- Green square labeled "Mock Poster 3"

// But user has:
- Girl photo from backend

// Result: ARCore looks for colored squares, not girl photo
```

### Why Backend Fails
```kotlin
// Current flow:
1. USE_MOCK_DATA = true
2. loadPosters() returns mock data immediately
3. Backend never called
4. Girl photo never downloaded
5. ARCore image database has wrong images

// What should happen:
1. USE_MOCK_DATA = false
2. loadPosters() calls backend API
3. Downloads girl photo
4. Adds to ARCore image database
5. Detection works
```

## Quick Fix Priority

### Immediate (Do First)
1. ✅ **Enable backend** - Set `USE_MOCK_DATA = false`
2. ✅ **Verify backend running** - Check server is accessible
3. ✅ **Test poster loading** - Confirm girl photo downloads

### High Priority (Do Next)
4. ✅ **Fix camera rendering** - Implement OpenGL background shader
5. ✅ **Add detailed logging** - Track where failures occur
6. ✅ **Test poster detection** - Verify ARCore detects girl photo

### Medium Priority (Do After)
7. ✅ **Fix video generation** - Ensure backend creates video
8. ✅ **Implement lip overlay** - Render lips on camera feed
9. ✅ **Test complete flow** - End-to-end user experience

## Testing Commands

### Check Backend Health
```bash
# From computer (not emulator)
curl http://localhost:YOUR_PORT/api/health

# From emulator
curl http://10.0.2.2:YOUR_PORT/api/health
```

### Check Girl Photo Exists
```bash
curl http://YOUR_BACKEND/api/images

# Should return:
{
  "images": [
    {
      "id": "girl_photo_1",
      "name": "Girl Photo",
      "imageUrl": "http://...",
      "hasHumanFace": true
    }
  ]
}
```

### Check Logs for Poster Loading
```bash
adb logcat | grep "PosterRepository"

# Should see:
# D/PosterRepository: Loading posters from backend...
# D/PosterRepository: Found 1 images from backend
# D/PosterRepository: ✅ Loaded poster: Girl Photo
```

### Check Logs for ARCore Detection
```bash
adb logcat | grep "ArSceneView"

# Should see:
# D/ArSceneView: Image database created with 1 images
# D/ArSceneView: Poster detected: girl_photo_1
```

## Expected Log Flow (When Working)

```
1. App Start:
   D/ArSceneView: Initializing ARCore session...
   D/ArSceneView: ✅ ARCore initialized successfully

2. Poster Loading:
   D/PosterRepository: Loading posters from backend...
   D/PosterRepository: Found 1 images from backend
   D/PosterRepository: Downloading image: http://...
   D/PosterRepository: ✅ Loaded poster: Girl Photo
   D/ArSceneView: Image database created with 1 images

3. Camera Preview:
   D/ArSceneView: GL Surface created
   D/ArSceneView: ✅ ARCore camera texture configured: 1
   D/ArSceneView: ✅ GL renderer initialized
   D/ArSceneView: Drawing camera background  ← Should see this every frame

4. Poster Detection:
   D/ArSceneView: Poster detected: girl_photo_1
   D/TalkingPhotoViewModel: onPosterDetected: girl_photo_1

5. Video Generation:
   D/TalkingPhotoController: Initializing talking photo for poster: girl_photo_1
   D/BackendVideoFetcher: Generating lip-sync video for poster: girl_photo_1
   D/BackendVideoFetcher: ✅ Generation started: videoId=abc123
   D/BackendVideoFetcher: Status check: videoId=abc123, status=processing
   D/BackendVideoFetcher: Status check: videoId=abc123, status=complete
   D/BackendVideoFetcher: Downloading video from: http://...
   D/TalkingPhotoController: ✅ Video setup complete, ready for playback

6. Playback:
   D/TalkingPhotoController: State transition: READY -> PLAYING
   D/LipRegionOverlay: Rendering lip region with 68 points
```

## Files That Need Changes

### Must Change
1. `PosterRepository.kt` - Line 23: `USE_MOCK_DATA = false`
2. `ArSceneViewComposable.kt` - Add camera background rendering
3. `BackendVideoFetcherImpl.kt` - Add detailed logging

### Should Change
4. `LipRegionOverlay.kt` - Implement overlay rendering
5. `TalkingPhotoViewModel.kt` - Add transform calculation
6. `ApiConfig.kt` - Verify backend URL

### Nice to Have
7. `BackendHealthCheck.kt` - Create health check utility
8. `ErrorMessages.kt` - User-friendly error messages

## Success Metrics

✅ **Camera Working**: Live camera feed visible (not black)  
✅ **Backend Connected**: Girl photo downloads successfully  
✅ **Detection Working**: ARCore detects girl photo  
✅ **Video Generated**: Backend creates lip-sync video  
✅ **Overlay Working**: Lips render on camera feed  
✅ **Complete Flow**: User sees talking photo  

## Next Steps

See `COMPLETE_USER_FLOW_IMPLEMENTATION_PLAN.md` for detailed implementation guide.

**Start with**: Phase 1 (Camera Rendering) and Phase 2 (Backend Integration)  
**Estimated time**: 3-4 hours to get basic flow working  
**Full implementation**: 11-16 hours (2-3 days)
