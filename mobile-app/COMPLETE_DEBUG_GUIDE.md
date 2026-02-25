# Complete Debug Guide - 3D Video Rendering (Enhanced v2)

## Critical Diagnostic Build

✅ Enhanced logging at EVERY step of the flow
✅ Will pinpoint exactly where the break occurs
✅ Tracks VideoPlaneNode creation, storage, and retrieval

## Testing Commands

### 1. Clear All Logs
```bash
adb logcat -c
```

### 2. Monitor Logs (Use This!)
```bash
adb logcat | grep -E "TalkAR|VideoPlane|IMAGE|VIDEO NODE"
```

### 3. Launch App
```bash
adb shell am start -n com.talkar.app/.MainActivity
```

## Complete Flow with Expected Logs

### Step 1: App Starts
```
TalkARView: ARSceneView created, configuring session...
TalkARView: AR session available, configuring...
TalkARView: ✅✅✅ AR session configured successfully with 2 images!
TalkARView: Ready to detect images - point camera at Sunrich poster
```

### Step 2: Point at Poster (CRITICAL - Image Detection)
```
TalkARView: Frame has 1 updated image(s)
TalkARView: ========================================
TalkARView: 🎯 NEW IMAGE DETECTED!
TalkARView:    Name: sunrich
TalkARView:    Index: 0
TalkARView:    Size: 0.8m x 1.2m
TalkARView: ========================================
TalkARView: 📍 Attempting to create anchor...
TalkARView: ✅ Anchor created successfully!
TalkARView: 🔗 Creating AnchorNode...
TalkARView: ✅ AnchorNode created: [AnchorNode@xxxxx]
TalkARView: ➕ Adding AnchorNode to scene...
TalkARView: ✅ AnchorNode added to scene
TalkARView: ========================================
TalkARView: 🎥 Creating VideoPlaneNode...
TalkARView:    Context: [Context]
TalkARView:    Engine: [Engine]
TalkARView:    Scene: [Scene]
TalkARView:    AnchorNode: [AnchorNode@xxxxx]
TalkARView:    Image size: 0.8m x 1.2m
TalkARView: ========================================
VideoPlaneNode: VideoPlaneNode created (0.8m x 1.2m)
TalkARView: ✅ VideoPlaneNode constructor completed: [VideoPlaneNode@xxxxx]
TalkARView: 💾 Storing nodes in maps...
TalkARView: ✅ Nodes stored successfully
TalkARView: ========================================
TalkARView: ✅✅✅ VIDEO NODE CREATED AND STORED!
TalkARView:    Image: sunrich
TalkARView:    Index: 0
TalkARView:    Size: 0.8m x 1.2m
TalkARView:    VideoNode: [VideoPlaneNode@xxxxx]
TalkARView:    videoNodesRef size: 1
TalkARView:    videoNodesRef keys: [sunrich]
TalkARView: ========================================
TalkARViewModel: Image detected: sunrich
```

### Step 3: Long Press (CRITICAL - Video Loading Trigger)
```
TalkARView: Long press on tracked image: sunrich
TalkARViewModel: ========================================
TalkARViewModel: 👆 IMAGE LONG-PRESSED!
TalkARViewModel:    Image: sunrich
TalkARViewModel: ========================================
TalkARViewModel: State changed to: LOADING_VIDEO
TalkARViewModel: Fetching video for: sunrich
```

### Step 4: Video URI Set (CRITICAL - State Update)
```
TalkARViewModel: ========================================
TalkARViewModel: 📦 Using local video fallback
TalkARViewModel:    Reason: Network unavailable - using offline video
TalkARViewModel:    Image: sunrich
TalkARViewModel:    Local URI: android.resource://com.talkar.app/2131689472
TalkARViewModel: ========================================
TalkARViewModel: ✅ State updated with local video URI
TalkARViewModel:    currentVideoUri: android.resource://com.talkar.app/2131689472
TalkARViewModel:    interactionState: PLAYING_INITIAL_VIDEO
```

### Step 5: LaunchedEffect Triggers (CRITICAL - Video Playback Start)
```
TalkARView: ========================================
TalkARView: 🔄 LaunchedEffect triggered!
TalkARView:    videoUriToPlay: android.resource://com.talkar.app/2131689472
TalkARView:    trackedImageNames: [sunrich]
TalkARView:    videoNodesRef size: 1
TalkARView:    videoNodesRef keys: [sunrich]
TalkARView: ========================================
TalkARView: ✅ Video URI is not null, attempting to play...
TalkARView: ✅ Found tracked image: sunrich
TalkARView: ========================================
TalkARView: 🎬 PLAYING VIDEO ON AR PLANE
TalkARView:    Image: sunrich
TalkARView:    URI: android.resource://com.talkar.app/2131689472
TalkARView:    VideoNode instance: [VideoPlaneNode@xxxxx]
TalkARView: ========================================
TalkARView: 🚀 Calling videoNode.loadVideo()...
TalkARView: ✅ videoNode.loadVideo() called successfully
```

### Step 6: VideoPlaneNode Loads Video (CRITICAL - MediaPlayer Setup)
```
VideoPlaneNode: ========================================
VideoPlaneNode: 📹 Loading video: android.resource://com.talkar.app/2131689472
VideoPlaneNode:    Auto-play: true
VideoPlaneNode: ========================================
VideoPlaneNode: Preparing video asynchronously...
VideoPlaneNode: MediaPlayer instance stored
```

### Step 7: Video Prepared (CRITICAL - 3D Plane Creation Trigger)
```
VideoPlaneNode: ========================================
VideoPlaneNode: ✅ Video prepared successfully!
VideoPlaneNode:    Duration: 5000ms (5s)
VideoPlaneNode:    Resolution: 1920x1080
VideoPlaneNode:    Aspect ratio: 1.777
VideoPlaneNode: ========================================
```

### Step 8: 3D Plane Created (CRITICAL - Filament Rendering)
```
VideoPlaneNode: ========================================
VideoPlaneNode: Creating 3D video plane: 0.8m x 1.2m
VideoPlaneNode: Video aspect ratio: 1.777
VideoPlaneNode: ========================================
VideoPlaneNode: Step 1: Loading material from assets...
VideoPlaneNode: ✅ Material loaded successfully
VideoPlaneNode: Step 2: Creating OpenGL texture...
VideoPlaneNode: ✅ OpenGL texture created: ID=123
VideoPlaneNode: Step 3: Creating SurfaceTexture...
VideoPlaneNode: ✅ SurfaceTexture created
VideoPlaneNode: Step 4: Connecting MediaPlayer to SurfaceTexture...
VideoPlaneNode: ✅ MediaPlayer connected to SurfaceTexture
VideoPlaneNode: Step 5: Creating Filament Stream...
VideoPlaneNode: ✅ Filament Stream created
VideoPlaneNode: Step 6: Creating Filament Texture...
VideoPlaneNode: ✅ Filament Texture created
VideoPlaneNode: Step 7: Connecting Texture to Stream...
VideoPlaneNode: ✅ Texture connected to Stream
VideoPlaneNode: Step 8: Setting up frame update callback...
VideoPlaneNode: ✅ Frame callback set
VideoPlaneNode: Step 9: Creating material instance...
VideoPlaneNode: ✅ Material instance created with video texture
VideoPlaneNode: Step 10: Creating plane geometry...
VideoPlaneNode: Plane geometry created: 4 vertices, 6 indices
VideoPlaneNode: ✅ Plane geometry created
VideoPlaneNode: Step 11: Creating renderable entity...
VideoPlaneNode: ✅ Renderable entity created: 12345
VideoPlaneNode: Step 12: Adding entity to scene...
VideoPlaneNode: ✅ Entity added to scene
VideoPlaneNode: Step 13: Positioning entity...
VideoPlaneNode: ✅ Entity positioned (1cm in front of image)
VideoPlaneNode: ========================================
VideoPlaneNode: 🎉 3D video plane created successfully!
VideoPlaneNode:    Material: Loaded
VideoPlaneNode:    Texture: External (ID=123)
VideoPlaneNode:    Geometry: 4 vertices, 6 indices
VideoPlaneNode:    Entity: 12345
VideoPlaneNode:    Size: 0.8m x 1.2m
VideoPlaneNode: ========================================
VideoPlaneNode: ▶️ Video playback started
```

## Diagnostic Decision Tree

### ❌ BREAK POINT 1: No Image Detection
**Look for:** "🎯 NEW IMAGE DETECTED!"

**If MISSING:**
- AR tracking not working
- Check lighting conditions
- Check poster visibility
- Check ARCore installation

**Commands:**
```bash
adb logcat | grep -i "augmented\|tracking\|arcore"
```

### ❌ BREAK POINT 2: Image Detected But No VideoNode Created
**Look for:** "✅✅✅ VIDEO NODE CREATED AND STORED!"

**If MISSING after seeing "🎯 NEW IMAGE DETECTED!":**
- Anchor creation failed
- VideoPlaneNode constructor threw exception
- Check for "❌❌❌ EXCEPTION creating VideoPlaneNode!"

**What to check:**
- Look for exception stack traces
- Check if anchor is null
- Verify Filament engine is available

### ❌ BREAK POINT 3: VideoNode Created But Long Press Not Working
**Look for:** "👆 IMAGE LONG-PRESSED!"

**If MISSING:**
- Gesture detection not working
- Check touch events
- Try longer press duration

### ❌ BREAK POINT 4: Long Press Works But No Video URI Set
**Look for:** "📦 Using local video fallback" or "✅ Backend video loaded!"

**If MISSING after "👆 IMAGE LONG-PRESSED!":**
- ViewModel not updating state
- API call hanging
- Exception in viewModelScope

### ❌ BREAK POINT 5: Video URI Set But LaunchedEffect Not Triggered
**Look for:** "🔄 LaunchedEffect triggered!"

**If MISSING after "✅ State updated with local video URI":**
- Compose recomposition issue
- videoUriToPlay not being passed correctly
- Check TalkARScreen state collection

**What to check:**
- Verify videoNodesRef size > 0
- Verify trackedImageNames not empty
- Check videoNodesRef keys match image name

### ❌ BREAK POINT 6: LaunchedEffect Triggered But VideoNode Not Found
**Look for:** "✅ Found tracked image: sunrich" and "🎬 PLAYING VIDEO ON AR PLANE"

**If you see:** "❌ VideoNode not found for image: sunrich"

**Problem:** VideoNode was created but not stored correctly or was removed

**What to check:**
- Check "videoNodesRef keys" in LaunchedEffect log
- Verify image name matches exactly
- Check if image tracking was lost

### ❌ BREAK POINT 7: loadVideo() Called But Not Executing
**Look for:** "🚀 Calling videoNode.loadVideo()..." and "📹 Loading video"

**If first log appears but second doesn't:**
- VideoPlaneNode.loadVideo() is not executing
- Coroutine scope issue
- Check for exceptions

### ❌ BREAK POINT 8: Video Loading But Not Preparing
**Look for:** "Preparing video asynchronously..." and "✅ Video prepared successfully!"

**If first log appears but second doesn't:**
- MediaPlayer preparation failed
- Video file not found
- Video format not supported
- Check for MediaPlayer error callbacks

### ❌ BREAK POINT 9: Video Prepared But No 3D Plane
**Look for:** "✅ Video prepared successfully!" and "Creating 3D video plane"

**If first log appears but second doesn't:**
- Exception in createVideoPlane()
- Check for error logs
- Verify Filament resources available

### ❌ BREAK POINT 10: 3D Plane Created But No Visuals
**Look for:** "🎉 3D video plane created successfully!"

**If this appears but no video visible:**
- Material shader issue
- Texture not updating
- Entity not visible in scene
- Transform positioning wrong
- Check OpenGL texture ID (should be > 0)
- Check entity ID (should be > 0)

## Quick Diagnostic Commands

### Check Image Detection
```bash
adb logcat -d | grep "NEW IMAGE DETECTED"
```

### Check VideoNode Creation
```bash
adb logcat -d | grep "VIDEO NODE CREATED AND STORED"
```

### Check Long Press
```bash
adb logcat -d | grep "IMAGE LONG-PRESSED"
```

### Check Video URI
```bash
adb logcat -d | grep "State updated with.*video URI"
```

### Check LaunchedEffect
```bash
adb logcat -d | grep "LaunchedEffect triggered"
```

### Check Video Loading
```bash
adb logcat -d | grep "Loading video:"
```

### Check Video Preparation
```bash
adb logcat -d | grep "Video prepared successfully"
```

### Check 3D Plane Creation
```bash
adb logcat -d | grep "3D video plane created successfully"
```

### Check for Exceptions
```bash
adb logcat -d | grep -i "exception\|error\|failed"
```

## What to Report

Please run the test and report:

1. **Which logs appeared?** (Use the break point numbers 1-10)
2. **Which log was the LAST one you saw?** (This is the break point!)
3. **Any error messages or exceptions?**
4. **Can you hear audio?** (Yes/No)
5. **Can you see video visuals?** (Yes/No)

Example report:
```
✅ Break Point 1: Image detected (saw "NEW IMAGE DETECTED")
✅ Break Point 2: VideoNode created (saw "VIDEO NODE CREATED AND STORED")
✅ Break Point 3: Long press worked (saw "IMAGE LONG-PRESSED")
✅ Break Point 4: Video URI set (saw "State updated with local video URI")
✅ Break Point 5: LaunchedEffect triggered (saw "LaunchedEffect triggered")
✅ Break Point 6: VideoNode found (saw "PLAYING VIDEO ON AR PLANE")
✅ Break Point 7: loadVideo called (saw "Loading video:")
✅ Break Point 8: Video prepared (saw "Video prepared successfully!")
❌ Break Point 9: STOPPED HERE - No "Creating 3D video plane" log
Audio: Yes
Visuals: No
```

---

**Ready to test!** This enhanced logging will pinpoint the exact break point.
