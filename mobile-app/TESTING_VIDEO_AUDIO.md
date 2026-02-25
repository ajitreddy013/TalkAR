# Testing Video Audio Playback

## Current Status

✅ **Video audio playback is working**
⏳ **3D visual rendering is pending**

The app now successfully plays video audio when you interact with detected images. The visual component (seeing the video on the AR plane) will be added in the next phase.

## What to Expect

### When You Point Camera at Poster

1. **Image Detection** (60fps)
   - App detects Sunrich or Tony poster
   - Green overlay shows "Image detected: sunrich" or "Image detected: tony"
   - Tracking is stable and smooth

2. **Long Press to Play Video**
   - Long-press anywhere on screen
   - App fetches video from backend (or uses local fallback)
   - Loading state shows "Loading video..."

3. **Video Audio Plays** ✅
   - You will HEAR the video audio
   - You will NOT see the video yet (3D rendering pending)
   - Audio plays for full duration
   - When complete, speech recognition starts

4. **Speech Recognition**
   - "Listening..." appears
   - Speak your question
   - App sends to backend for AI response
   - New video plays with response

## Testing Steps

### 1. Build and Install

```bash
cd mobile-app
./gradlew app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 2. Launch App

```bash
adb shell am start -n com.talkar.app/.MainActivity
```

### 3. Test Image Detection

- Point camera at Sunrich poster (sunrich.jpg)
- Should see: "Image detected: sunrich"
- Point camera at Tony poster (tony.png)
- Should see: "Image detected: tony"

### 4. Test Video Audio

- Long-press on screen while image is detected
- Should see: "Loading video..."
- Should hear: Video audio playing
- Should NOT see: Video visuals (pending)

### 5. Test Speech Recognition

- Wait for video to complete
- Should see: "Listening..."
- Speak a question
- Should see: "Processing speech..."
- Should hear: AI response audio

### 6. Check Logs

```bash
adb logcat | grep -E "(TalkARView|VideoPlaneNode|TalkARViewModel)"
```

**Expected logs:**
```
TalkARView: ✅ Image detected: sunrich
VideoPlaneNode: Loading video: content://...
VideoPlaneNode: Video prepared, duration: 5000ms
VideoPlaneNode: ✅ Video audio ready (3D visual rendering pending)
VideoPlaneNode: Video playback started
VideoPlaneNode: Video playback completed
```

## Known Behavior

### What Works ✅

- Image detection at 60fps
- Video audio playback
- Backend video fetching
- Offline fallback to local video
- Speech recognition
- AI response audio

### What's Pending ⏳

- 3D visual rendering of video
- Seeing video on AR plane
- Video texture updates

### What You'll Experience

**Current:**
```
1. Point at poster → Image detected ✅
2. Long press → Loading video ✅
3. Video plays → HEAR audio ✅
4. Video plays → SEE video ❌ (pending)
5. Video ends → Speech recognition ✅
```

**After 3D rendering:**
```
1. Point at poster → Image detected ✅
2. Long press → Loading video ✅
3. Video plays → HEAR audio ✅
4. Video plays → SEE video ✅ (new!)
5. Video ends → Speech recognition ✅
```

## Troubleshooting

### No Audio Playing

**Check:**
1. Volume is up
2. Video file exists (check logs)
3. MediaPlayer prepared successfully

**Logs to check:**
```bash
adb logcat | grep "VideoPlaneNode"
```

**Expected:**
```
VideoPlaneNode: Video prepared, duration: 5000ms
VideoPlaneNode: Video playback started
```

### Video Not Loading

**Check:**
1. Backend is running (port 4000)
2. Network connectivity
3. Offline fallback video exists

**Logs to check:**
```bash
adb logcat | grep "TalkARViewModel"
```

**Expected:**
```
TalkARViewModel: Fetching video for: sunrich
TalkARViewModel: Video fetched successfully
```

### Image Not Detected

**Check:**
1. Good lighting
2. Poster is flat and visible
3. Camera permissions granted
4. ARCore installed

**Logs to check:**
```bash
adb logcat | grep "TalkARView"
```

**Expected:**
```
TalkARView: ✅ Image detected: sunrich
```

## Backend Testing

### Start Backend

```bash
cd backend
npm start
```

**Expected:**
```
Server running on port 4000
```

### Test Endpoints

```bash
# Get initial video
curl http://localhost:4000/api/v1/sync/talking-head/sunrich

# Test AI pipeline
curl -X POST http://localhost:4000/api/v1/ai-pipeline/voice_query \
  -H "Content-Type: application/json" \
  -d '{"text": "Hello", "imageName": "sunrich"}'
```

## Next Steps

Once audio playback is confirmed working:

1. **Implement 3D visual rendering**
   - Create Filament material with video texture
   - Build plane geometry
   - Update texture every frame

2. **Test visual rendering**
   - See video on AR plane
   - Verify aspect ratio
   - Check tracking stability

3. **Optimize performance**
   - Reduce frame drops
   - Improve texture updates
   - Lower latency

## Questions?

If you encounter issues:

1. Check logs with `adb logcat`
2. Verify backend is running
3. Test with local video first
4. Check camera permissions
5. Ensure ARCore is installed

## Success Criteria

✅ **Phase 1 (Current):**
- Image detection works
- Video audio plays
- Speech recognition works
- Backend integration works

⏳ **Phase 2 (Next):**
- Video visuals render on AR plane
- Texture updates smoothly
- Aspect ratio is correct
- Performance is good (30+ fps)

---

**Last Updated:** Feb 21, 2026
**Status:** Audio playback working, visual rendering pending
