# Testing Instructions - Enhanced Diagnostic Build

## Overview

I've added comprehensive logging to identify exactly where the 3D video rendering breaks. The logs will show which of 10 critical break points is failing.

## Quick Test (Automated)

```bash
cd mobile-app
./test_video_rendering.sh
```

This script will:
1. Clear logs
2. Launch the app
3. Monitor logs in real-time
4. Analyze which break points passed/failed
5. Show you exactly where it broke

## Manual Test

### Step 1: Clear Logs
```bash
adb logcat -c
```

### Step 2: Start Monitoring
```bash
adb logcat | grep -E "TalkAR|VideoPlane|IMAGE|VIDEO NODE"
```

### Step 3: Launch App
```bash
adb shell am start -n com.talkar.app/.MainActivity
```

### Step 4: Test Flow
1. Point camera at Sunrich poster
2. Wait for "✅ Detected: sunrich" message
3. Long-press on the poster
4. Observe logs and video playback

## What to Look For

### ✅ Success Path (All 10 Break Points)

```
BP1: 🎯 NEW IMAGE DETECTED!
BP2: ✅✅✅ VIDEO NODE CREATED AND STORED!
BP3: 👆 IMAGE LONG-PRESSED!
BP4: ✅ State updated with local video URI
BP5: 🔄 LaunchedEffect triggered!
BP6: 🎬 PLAYING VIDEO ON AR PLANE
BP7: 📹 Loading video:
BP8: ✅ Video prepared successfully!
BP9: Creating 3D video plane:
BP10: 🎉 3D video plane created successfully!
```

### ❌ Failure Scenarios

**If you see BP1-BP7 but NOT BP8:**
- MediaPlayer preparation failed
- Video file issue

**If you see BP1-BP8 but NOT BP9:**
- createVideoPlane() not called
- Exception in onPrepared callback

**If you see BP1-BP9 but NOT BP10:**
- Filament rendering issue
- Material/texture/entity creation failed

**If you see BP10 but no visuals:**
- Shader issue
- Texture not updating
- Entity not visible

## Key Information to Share

After testing, please provide:

1. **Which break points passed?** (BP1-BP10)
2. **Last successful break point?**
3. **Any error messages?**
4. **Audio working?** (Yes/No)
5. **Visuals showing?** (Yes/No)

Example:
```
✅ BP1-BP8 all passed
❌ BP9 failed - no "Creating 3D video plane" log
Audio: Yes
Visuals: No
Error: [paste any error messages]
```

## Detailed Logs

For complete analysis, you can save all logs:

```bash
adb logcat -c
adb logcat > full_test.log &
# Run test
# Press Ctrl+C when done
```

Then search for specific issues:
```bash
grep -i "exception" full_test.log
grep -i "error" full_test.log | grep -v "GetRecentDevicePose\|hit_test"
grep "VideoPlane" full_test.log
```

## Common Issues

### No Image Detection (BP1 fails)
- Check lighting (need bright, even lighting)
- Check poster is flat and visible
- Check ARCore is installed
- Try moving closer/farther from poster

### VideoNode Not Created (BP2 fails)
- Check for exceptions in logs
- Verify Filament engine is available
- Check anchor creation

### Long Press Not Working (BP3 fails)
- Try pressing longer (2+ seconds)
- Make sure you're pressing on the poster area
- Check touch events in logs

### LaunchedEffect Not Triggering (BP5 fails)
- Check if videoNodesRef is empty
- Check if trackedImageNames is empty
- Verify state is being collected

### Video Not Preparing (BP8 fails)
- Check video file exists in res/raw/
- Check video format is supported
- Look for MediaPlayer error callbacks

### 3D Plane Not Creating (BP9-BP10 fails)
- Check for Filament exceptions
- Verify material file exists in assets
- Check OpenGL texture ID > 0
- Check entity ID > 0

## Next Steps

Based on which break point fails, we'll implement the appropriate fix. The enhanced logging will tell us exactly what's wrong!

---

**Ready to test!** Run the automated script or follow manual steps above.
