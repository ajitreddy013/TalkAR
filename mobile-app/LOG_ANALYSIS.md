# TalkAR Log Analysis & Troubleshooting

## Understanding the Logs

When testing TalkAR, you'll see many warnings and errors in logcat. Most of these are **expected and harmless**. This document explains what's actually a problem vs what's normal.

---

## ✅ EXPECTED & HARMLESS Warnings

### 1. ARCore Tracking Warnings (IGNORE THESE)

**What you'll see:**
```
INVALID_ARGUMENT: Passed timestamp ... is too new
IMU buffer is empty or has only one sample
VioFaultDetector: Not enough ransac inliers
Map update failed
Too many map update failures, probably lost map tracking
INTERNAL: No point hit
No raw depth data found
```

**Why this happens:**
- We're **only using image tracking**, not plane detection or hit testing
- ARCore tries to do full SLAM tracking but we don't need it
- These warnings don't affect image detection at all
- Lost tracking happens when camera moves away from image (expected)

**What matters:**
- ✅ Image detection works: "✅ Detected: sunrich"
- ✅ Tracking is stable when pointing at image
- ✅ App doesn't crash

**Action:** Ignore these warnings completely

---

### 2. JNI Thread Warnings (IGNORE THESE)

**What you'll see:**
```
Attempt to get JNIEnv* on thread not attached to JVM
```

**Why this happens:**
- ARCore's native C++ code tries to callback to Java
- This is an internal ARCore issue, not our code
- Doesn't affect functionality

**Action:** Ignore this warning

---

### 3. Surface Lifecycle Messages (NORMAL)

**What you'll see:**
```
surfaceDestroyed
Session::Pause
handleAppVisibility mAppVisible = true visible = false
```

**Why this happens:**
- Normal Android lifecycle when app goes to background
- Or when switching between screens
- Proper cleanup is happening

**Action:** This is correct behavior

---

## ⚠️ WARNINGS TO INVESTIGATE

### 1. Frame Skipping (FIXED)

**What you'll see:**
```
Skipped 70 frames! The application may be doing too much work on its main thread
```

**Why this happens:**
- Heavy work blocking the UI thread
- Usually during video loading or AR initialization

**Fix applied:**
- Video loading moved to background thread (Dispatchers.IO)
- MediaPlayer initialization off main thread

**How to verify fix:**
- Check if frame skip count is lower (<30 frames)
- App should feel smooth during video load

---

### 2. Speech Recognition "No Match" (FIXED)

**What you'll see:**
```
Recognition error: No speech match
```

**Why this happens:**
- Speech recognizer couldn't understand the audio
- Low volume, background noise, or unclear speech
- Network timeout (if using online recognition)

**Fix applied:**
- App now uses default response ("Hello") instead of showing error
- Reduced silence timeout from 3s to 2s
- Enabled online recognition for better accuracy

**How to verify fix:**
- App should continue to response video even if speech fails
- No error message shown to user
- Check logs for: "No speech match - using default response"

---

### 3. Monitor Contention (INVESTIGATE IF SEVERE)

**What you'll see:**
```
Long monitor contention with owner ... (492ms)
```

**Why this happens:**
- Multiple threads waiting for same resource
- Can contribute to frame skipping

**When to worry:**
- If contention is >500ms consistently
- If app feels laggy or unresponsive

**Action:**
- Check if multiple video nodes are being created
- Ensure proper cleanup when images are lost

---

## 🚨 REAL ERRORS (FIX THESE)

### 1. MediaPlayer Errors

**What you'll see:**
```
MediaPlayer error: what=-38, extra=0
```

**What it means:**
- Video file not found or corrupted
- Unsupported video format
- Permissions issue

**How to fix:**
- Verify video file exists: `mobile-app/app/src/main/res/raw/sunrich_video.mp4`
- Check video format (should be H.264/AAC MP4)
- Ensure file isn't corrupted

---

### 2. Permission Errors

**What you'll see:**
```
Recognition error: Insufficient permissions
```

**What it means:**
- Camera or microphone permission not granted

**How to fix:**
- Grant permissions in Settings → Apps → TalkAR → Permissions
- Or uninstall and reinstall to trigger permission prompt

---

### 3. ARCore Not Available

**What you'll see:**
```
AR configuration failed: ARCore not installed
```

**What it means:**
- ARCore not installed or outdated

**How to fix:**
- Install ARCore from Play Store
- Update to latest version

---

## 📊 Success Indicators

### What "Working Correctly" Looks Like

**Image Detection:**
```
TalkARView: ✅ Image detected: sunrich
TalkARView: ✅ Anchor created for image: sunrich
TalkARView: ✅ Video node created for: sunrich (0.8m x 0.6m)
```

**Video Playback:**
```
VideoAnchorNode: Loading video: android.resource://...
VideoAnchorNode: Video prepared, duration: 15000ms
VideoAnchorNode: Video playback started
VideoAnchorNode: Video playback completed
```

**Speech Recognition:**
```
SpeechRecognition: Started listening (language: en_IN)
SpeechRecognition: User started speaking
SpeechRecognition: User stopped speaking
SpeechRecognition: ✅ Recognized: "Hello"
```

**OR (if no match):**
```
SpeechRecognition: No speech match - using default response
TalkARViewModel: Speech recognized: "Hello"
```

---

## 🔍 Debugging Commands

### View All TalkAR Logs
```bash
adb logcat | grep -E "TalkAR|VideoAnchor|SpeechRecognition"
```

### View Only Errors
```bash
adb logcat *:E | grep -E "TalkAR|VideoAnchor|SpeechRecognition"
```

### View Image Detection
```bash
adb logcat | grep "Image detected"
```

### View Video Playback
```bash
adb logcat | grep "VideoAnchorNode"
```

### View Speech Recognition
```bash
adb logcat | grep "SpeechRecognition"
```

### Check Frame Skipping
```bash
adb logcat | grep "Skipped.*frames"
```

---

## 🎯 Performance Benchmarks

### Good Performance
- Frame skips: <30 frames during initialization
- Image detection: 50-60 fps
- Video load time: <500ms
- Speech recognition start: <1s

### Acceptable Performance
- Frame skips: 30-70 frames during initialization
- Image detection: 30-50 fps
- Video load time: 500-1000ms
- Speech recognition start: 1-2s

### Poor Performance (Investigate)
- Frame skips: >100 frames
- Image detection: <30 fps
- Video load time: >2s
- Speech recognition start: >3s

---

## 🐛 Common Issues & Solutions

### Issue: Image Not Detecting

**Symptoms:** No "✅ Detected" message

**Check:**
1. Image is printed at ~80cm size
2. Good lighting (no glare)
3. Image is flat (not curved)
4. Camera distance 30-100cm
5. ARCore is installed

**Logs to check:**
```bash
adb logcat | grep "AugmentedImage"
```

---

### Issue: Video Audio Not Playing

**Symptoms:** Long-press does nothing

**Check:**
1. Device volume is up (media volume)
2. Video file exists in res/raw/
3. Audio permission granted

**Logs to check:**
```bash
adb logcat | grep "MediaPlayer"
```

---

### Issue: Speech Recognition Fails

**Symptoms:** Always shows "No speech match"

**Check:**
1. Microphone permission granted
2. Device has internet (for online recognition)
3. Speak clearly and loudly
4. Reduce background noise

**Logs to check:**
```bash
adb logcat | grep "SpeechRecognition"
```

---

### Issue: App Crashes on Launch

**Symptoms:** App closes immediately

**Check:**
1. ARCore is installed
2. Permissions granted
3. Device is ARCore-compatible

**Logs to check:**
```bash
adb logcat | grep "AndroidRuntime"
```

---

## 📝 Reporting Issues

When reporting a bug, include:

1. **Device info:** Model, Android version
2. **Steps to reproduce:** Exact sequence
3. **Expected vs actual:** What should happen vs what happened
4. **Relevant logs:** Use the debugging commands above
5. **Screenshots/video:** If applicable

**Example:**
```
Device: Samsung SM-A356E, Android 14
Steps:
1. Opened app
2. Pointed at sunrich image
3. Long-pressed screen
4. No audio played

Expected: Video audio should play
Actual: Silence

Logs:
VideoAnchorNode: Loading video: android.resource://...
MediaPlayer error: what=-38, extra=0

Screenshot: [attached]
```

---

## 🎓 Understanding ARCore Behavior

### Why So Many Warnings?

ARCore is designed for full SLAM (Simultaneous Localization and Mapping):
- Plane detection
- Hit testing
- Depth mapping
- 6DOF tracking

**We only use:**
- Image tracking (2D markers)

**Result:**
- ARCore tries to do full SLAM
- Fails because we disabled plane detection
- Logs warnings about failed features
- **But image tracking still works perfectly**

This is like buying a Swiss Army knife and only using the blade - the other tools complain they're not being used, but the blade works fine!

---

## 🔄 Next Steps

After reviewing logs:

1. **If image detection works:** ✅ Core functionality is good
2. **If video audio plays:** ✅ MediaPlayer integration works
3. **If speech triggers response:** ✅ Full flow works
4. **If frame skips are low:** ✅ Performance is good

Then proceed to:
- Implement 3D video rendering
- Connect backend API
- Add more dialogues
- Polish UI/UX
