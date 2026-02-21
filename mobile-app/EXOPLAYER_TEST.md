# ExoPlayer Test Guide

## Quick Test

```bash
cd mobile-app
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Monitor Logs

```bash
adb logcat -c && adb logcat | grep -E "ExoPlayer|VideoNode|Step|✅|❌|🎉"
```

## Test Steps

1. **Point at poster** - Wait for "✅ Detected: sunrich"
2. **Long-press** - Hold for 2+ seconds
3. **Watch for logs** - Should see 9 steps complete
4. **Check video** - Should see video playing on poster!

## Expected Success Logs

```
✅ ExoPlayer created
✅ Player listener set
✅ Media item set
✅ Player prepared
✅ VideoNode created
✅ VideoNode scaled
✅ VideoNode positioned
✅ VideoNode added to scene
▶️ Video playback started
🎉 ExoPlayer video setup complete!
✅ Video ready to play
```

## What to Report

Just tell me:
- "All 9 steps passed: Yes/No"
- "Video visuals showing: Yes/No"
- "Audio playing: Yes/No"

If it fails, share which step was the last ✅

---

**This should work!** ExoPlayer + Sceneview VideoNode is the proven approach for AR video.
