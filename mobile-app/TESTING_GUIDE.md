# TalkAR Testing Guide

## Prerequisites

1. **Device**: Samsung SM-A356E (or any ARCore-supported Android device)
2. **Connection**: USB cable or WiFi debugging enabled
3. **Printed Images**: 
   - Sunrich bottle image (80cm size recommended)
   - Tony image (80cm size recommended)
4. **App**: Latest build from `feature/arcore-augmented-images` branch

## Installation

### Option 1: USB Connection
```bash
cd mobile-app
./gradlew installDebug
```

### Option 2: WiFi Debugging
```bash
# Connect device to same WiFi network
adb connect <device-ip>:5555
cd mobile-app
./gradlew installDebug
```

## Testing Scenarios

### 1. Image Detection Test ✅ WORKING

**Steps:**
1. Open TalkAR app
2. Grant camera and audio permissions
3. Point camera at Sunrich or Tony image
4. Keep image flat and well-lit

**Expected Result:**
- Green card appears: "✅ Detected: sunrich" or "✅ Detected: tony"
- Detection happens at ~60fps
- Tracking is stable even with slight movement

**Status:** ✅ Fully functional

---

### 2. Multi-Image Detection Test ✅ WORKING

**Steps:**
1. Point camera at Sunrich image → should detect "sunrich"
2. Move camera to Tony image → should detect "tony"
3. Move back to Sunrich → should detect "sunrich" again

**Expected Result:**
- App switches between detected images smoothly
- Each image shows its own name
- No lag or crashes

**Status:** ✅ Fully functional

---

### 3. Video Playback Test 🚧 READY TO TEST

**Steps:**
1. Point camera at Sunrich or Tony image
2. Wait for detection (green card appears)
3. **Long-press anywhere on screen**
4. Hold for ~1 second

**Expected Result:**
- Status changes to "▶️ Playing video..."
- MediaPlayer loads `sunrich_video.mp4`
- **Audio should play** (you'll hear the video sound)
- Video texture NOT visible yet (3D rendering pending)

**What to Check:**
- Does audio play?
- Any error messages?
- Check logcat for "VideoAnchorNode" logs

**Status:** 🚧 Ready for testing (audio only, no visual)

---

### 4. Speech Recognition Test 🚧 READY TO TEST

**Steps:**
1. Complete video playback test above
2. Wait for video to finish
3. Status should change to "🎤 Listening..."
4. Speak into the device

**Expected Result:**
- Microphone activates
- Speech is recognized
- Text appears on screen
- Response video plays (same video for now)

**Status:** 🚧 Ready for testing (not tested yet)

---

## Troubleshooting

### Image Not Detecting

**Symptoms:** No green card appears, camera shows but nothing happens

**Solutions:**
1. Ensure image is printed at ~80cm (poster size)
2. Check lighting - avoid glare or shadows
3. Keep image flat (not curved or wrinkled)
4. Move camera closer/farther (try 30-100cm distance)
5. Check logcat: `adb logcat | grep TalkARView`

### App Crashes on Launch

**Symptoms:** App closes immediately after opening

**Solutions:**
1. Check ARCore is installed: `adb shell pm list packages | grep arcore`
2. Update ARCore from Play Store
3. Grant camera permission manually in Settings
4. Check logcat: `adb logcat | grep AndroidRuntime`

### Video Not Playing

**Symptoms:** Long-press does nothing, no audio

**Solutions:**
1. Check logcat: `adb logcat | grep VideoAnchorNode`
2. Verify video file exists: `mobile-app/app/src/main/res/raw/sunrich_video.mp4`
3. Check MediaPlayer errors in logs
4. Ensure device volume is up

### No Audio During Video

**Symptoms:** Video seems to play but no sound

**Solutions:**
1. Check device volume (media volume, not ringer)
2. Verify audio permission granted
3. Test video file plays in other apps
4. Check logcat for MediaPlayer errors

---

## Viewing Logs

### All TalkAR Logs
```bash
adb logcat | grep -E "TalkAR|VideoAnchor|ARSession"
```

### Image Detection Only
```bash
adb logcat | grep "Image detected"
```

### Video Playback Only
```bash
adb logcat | grep "VideoAnchorNode"
```

### Errors Only
```bash
adb logcat | grep -E "ERROR|Exception"
```

---

## Expected Log Output

### Successful Image Detection
```
TalkARView: ✅ Image detected: sunrich
TalkARView: ✅ Anchor created for image: sunrich
TalkARView: ✅ Video node created for: sunrich (0.8m x 0.6m)
```

### Successful Video Load
```
VideoAnchorNode: Loading video: android.resource://com.talkar.app/2131689472
VideoAnchorNode: Video prepared, duration: 15000ms
VideoAnchorNode: Video playback started
```

### Video Completion
```
VideoAnchorNode: Video playback completed
TalkARViewModel: Initial video completed, starting speech recognition
```

---

## Known Issues

1. **3D Video Rendering Not Implemented**
   - Video audio plays but no visual overlay
   - This is expected - 3D plane rendering is next step
   
2. **ARCore Internal Warnings**
   - IMU timing warnings are normal
   - Hit test failures are expected (we don't use hit testing)
   - These don't affect image detection

3. **Backend Not Connected**
   - Using local video file instead of backend URLs
   - Speech recognition works but response is placeholder

---

## Success Criteria

### Minimum Viable (Current State)
- ✅ Detect both sunrich and tony images
- ✅ Stable tracking at 60fps
- 🚧 Audio plays on long-press (needs testing)

### Next Milestone
- 🎯 Video visible in AR space (3D rendering)
- 🎯 Speech recognition triggers response
- 🎯 Backend integration for dynamic videos

### Full Feature Complete
- 🎯 Complete interaction flow working
- 🎯 Multiple dialogues per image
- 🎯 Production-ready performance

---

## Reporting Issues

When reporting issues, please include:

1. **Device Info**: Model, Android version
2. **Steps to Reproduce**: Exact sequence that caused issue
3. **Expected vs Actual**: What should happen vs what happened
4. **Logs**: Relevant logcat output
5. **Screenshots/Video**: If applicable

Example:
```
Device: Samsung SM-A356E, Android 14
Steps: 
1. Opened app
2. Pointed at sunrich image
3. Long-pressed screen
4. No audio played

Expected: Video audio should play
Actual: Silence, no error message

Logs:
VideoAnchorNode: Loading video: android.resource://...
MediaPlayer error: what=-38, extra=0
```

---

## Next Steps After Testing

1. **If audio plays**: Implement 3D video plane rendering
2. **If audio doesn't play**: Debug MediaPlayer integration
3. **If detection fails**: Adjust image database or thresholds
4. **If app crashes**: Fix critical bugs before proceeding

---

## Contact

For questions or issues during testing, check:
- Implementation status: `mobile-app/ARCORE_IMPLEMENTATION_STATUS.md`
- Pull request: #65 on GitHub
- Logs: Always check logcat first
