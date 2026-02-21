# Quick Test Reference

## One-Command Test

```bash
cd mobile-app && ./test_video_rendering.sh
```

## Manual Test (3 Commands)

```bash
# 1. Clear logs
adb logcat -c

# 2. Monitor (keep this running)
adb logcat | grep -E "TalkAR|VideoPlane|IMAGE|VIDEO NODE"

# 3. Launch app (in another terminal)
adb shell am start -n com.talkar.app/.MainActivity
```

## Test Steps

1. Point camera at Sunrich poster
2. Wait for "✅ Detected: sunrich"
3. Long-press on poster (2+ seconds)
4. Watch logs

## What to Look For

### ✅ All Working
```
🎯 NEW IMAGE DETECTED!
✅✅✅ VIDEO NODE CREATED AND STORED!
👆 IMAGE LONG-PRESSED!
✅ State updated with local video URI
🔄 LaunchedEffect triggered!
🎬 PLAYING VIDEO ON AR PLANE
📹 Loading video:
✅ Video prepared successfully!
Creating 3D video plane:
🎉 3D video plane created successfully!
```

### ❌ Something Broken
Look for the **last log** you see, then report:
- "Stopped at: [last log message]"
- Audio: Yes/No
- Visuals: Yes/No
- Any errors

## Quick Checks

### Check Image Detection
```bash
adb logcat -d | grep "NEW IMAGE DETECTED"
```

### Check VideoNode Creation
```bash
adb logcat -d | grep "VIDEO NODE CREATED"
```

### Check Video Loading
```bash
adb logcat -d | grep "Loading video:"
```

### Check 3D Plane
```bash
adb logcat -d | grep "3D video plane created"
```

### Check Errors
```bash
adb logcat -d | grep -i "exception\|error" | grep -v "GetRecentDevicePose\|hit_test"
```

## Report Template

Copy and fill this:

```
Break Points:
- BP1 (Image Detection): ✅ or ❌
- BP2 (VideoNode Created): ✅ or ❌
- BP3 (Long Press): ✅ or ❌
- BP4 (Video URI Set): ✅ or ❌
- BP5 (LaunchedEffect): ✅ or ❌
- BP6 (VideoNode Found): ✅ or ❌
- BP7 (loadVideo Called): ✅ or ❌
- BP8 (Video Prepared): ✅ or ❌
- BP9 (3D Plane Started): ✅ or ❌
- BP10 (3D Plane Complete): ✅ or ❌

Audio: Yes/No
Visuals: Yes/No

Errors:
[paste any error messages]

Last successful log:
[paste the last successful log line]
```

---

**That's it!** Run the test and share the results.
