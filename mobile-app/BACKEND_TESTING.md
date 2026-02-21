# Backend Integration Testing Guide

## ✅ Current Status

- **Offline Mode:** ✅ Working (confirmed by user)
- **Backend:** ✅ Running on http://localhost:4000
- **API Endpoints:** ✅ Responding correctly
- **App Build:** ✅ Compiled successfully

---

## 🧪 Testing Steps

### 1. Verify Backend is Running

```bash
# Check if backend is running
curl http://localhost:4000/api/v1/images

# Should return list of images including "Tony" and "Sunrich"
```

**Expected Output:**
```json
[
  {
    "id": "db187eb9-...",
    "name": "Tony",
    "imageUrl": "/uploads/...",
    ...
  },
  {
    "id": "96a82b2d-...",
    "name": "Sunrich",
    "imageUrl": "/uploads/...",
    ...
  }
]
```

---

### 2. Test Talking-Head Endpoint

```bash
# Test getting video for Sunrich
curl "http://localhost:4000/api/v1/sync/talking-head/sunrich?language=en&emotion=excited"
```

**Expected Output:**
```json
{
  "imageId": "sunrich",
  "videoUrl": "https://assets.sync.so/docs/example-talking-head.mp4",
  "duration": 15,
  "language": "en",
  "emotion": "excited"
}
```

---

### 3. Install App with Backend Connection

The app is configured to connect to `http://10.0.2.2:4000` in debug mode (Android emulator localhost).

**For Physical Device:**

You need to use your computer's IP address instead:

```bash
# Find your IP address
ifconfig | grep "inet " | grep -v 127.0.0.1

# Example output: inet 192.168.1.100

# Build and install with your IP
cd mobile-app
./gradlew installDebug -PAPI_HOST=192.168.1.100 -PAPI_PORT=4000
```

**For Emulator:**

```bash
cd mobile-app
./gradlew installDebug
# Uses 10.0.2.2:4000 by default
```

---

### 4. Test Online Mode

**Steps:**
1. **Ensure backend is running** (check terminal)
2. **Ensure device/emulator has internet**
3. **Open TalkAR app**
4. **Point camera at Sunrich or Tony image**
5. **Wait for detection** (green card: "✅ Detected: sunrich")
6. **Long-press on screen**

**What Should Happen:**

```
1. Shows "⏳ Loading video..." (fetching from backend)
2. Shows "▶️ Playing video..." (playing backend video)
3. Video audio plays
4. After video: "🎤 Listening..." (speech recognition)
5. Speak something
6. Shows "💭 Processing..." (sending to backend)
7. Shows "▶️ Playing response..." (playing response)
```

**Check Logs:**
```bash
adb logcat | grep -E "TalkARApiService|TalkARViewModel"
```

**Expected Logs (Online):**
```
TalkARViewModel: Image long-pressed: sunrich
TalkARApiService: Fetching video for: sunrich
TalkARApiService: Response: 200 OK
TalkARViewModel: Loaded video from backend: https://assets.sync.so/...
VideoAnchorNode: Loading video: https://assets.sync.so/...
VideoAnchorNode: Video prepared, duration: 15000ms
VideoAnchorNode: Video playback started
```

---

### 5. Test Offline Fallback

**Steps:**
1. **Enable airplane mode** on device
2. **Open TalkAR app**
3. **Point camera at image**
4. **Long-press on screen**

**What Should Happen:**

```
1. Shows "⏳ Loading video..." (trying backend)
2. Shows "▶️ Playing video..." (fallback to local)
3. Shows "Using offline video (network error)" (error message)
4. Local video plays
```

**Expected Logs (Offline):**
```
TalkARViewModel: Image long-pressed: sunrich
TalkARApiService: Network error: Unable to resolve host
TalkARViewModel: API error loading video, using local fallback
VideoAnchorNode: Loading video: android.resource://...
```

---

### 6. Test Voice Query

**Prerequisites:**
- Backend must be running
- Initial video must complete
- Microphone permission granted

**Steps:**
1. **Complete initial video playback**
2. **Wait for "🎤 Listening..."**
3. **Speak clearly:** "Tell me more about this product"
4. **Wait for processing**

**What Should Happen:**

```
1. Speech recognized: "Tell me more about this product"
2. Shows "💭 Processing..." (sending to backend)
3. Backend processes query with AI
4. Shows "▶️ Playing response..." (playing response audio)
5. Response audio plays
```

**Expected Logs:**
```
SpeechRecognition: ✅ Recognized: "Tell me more about this product"
TalkARViewModel: Speech recognized: "Tell me more about this product"
TalkARApiService: Sending voice query: Tell me more about this product
TalkARApiService: Response: 200 OK
TalkARViewModel: Playing response from backend: https://...
```

---

## 🐛 Troubleshooting

### Issue: "Network error: Unable to resolve host"

**Cause:** App can't reach backend

**Solutions:**

1. **Check backend is running:**
   ```bash
   curl http://localhost:4000/api/v1/images
   ```

2. **For physical device, use correct IP:**
   ```bash
   # Get your IP
   ifconfig | grep "inet " | grep -v 127.0.0.1
   
   # Rebuild with your IP
   ./gradlew installDebug -PAPI_HOST=192.168.1.100
   ```

3. **Check device is on same network:**
   - Device and computer must be on same WiFi
   - Check firewall isn't blocking port 4000

4. **Test connectivity from device:**
   ```bash
   # From device browser, visit:
   http://192.168.1.100:4000/api/v1/images
   ```

---

### Issue: "Using offline video" always shows

**Cause:** Backend returns empty videoUrl

**Check:**
```bash
curl "http://localhost:4000/api/v1/sync/talking-head/sunrich"
```

**Should return:**
```json
{
  "videoUrl": "https://...",  // Must not be empty
  ...
}
```

**If empty:**
- Check backend logs
- Verify sync service is configured
- Check database has video URLs

---

### Issue: Video URL returns 404

**Cause:** Video file doesn't exist at URL

**Solutions:**

1. **Check URL is accessible:**
   ```bash
   curl -I "https://assets.sync.so/docs/example-talking-head.mp4"
   ```

2. **Use local backend videos:**
   - Store videos in `backend/uploads/`
   - Return local URLs: `http://10.0.2.2:4000/uploads/video.mp4`

3. **Check CORS headers:**
   - Video server must allow cross-origin requests
   - Add CORS headers to video responses

---

### Issue: Speech recognition fails

**Cause:** Various reasons

**Solutions:**

1. **Check microphone permission:**
   - Settings → Apps → TalkAR → Permissions → Microphone

2. **Speak clearly and loudly:**
   - Reduce background noise
   - Speak directly into microphone

3. **Check internet connection:**
   - Speech recognition uses online service
   - Requires internet for best accuracy

4. **Check logs:**
   ```bash
   adb logcat | grep SpeechRecognition
   ```

---

## 📊 Success Indicators

### Backend Connection Working

**Logs show:**
```
✅ TalkARApiService: Response: 200 OK
✅ TalkARViewModel: Loaded video from backend
✅ VideoAnchorNode: Loading video: https://...
```

**UI shows:**
```
✅ "⏳ Loading video..." (brief)
✅ "▶️ Playing video..." (no error message)
✅ Video audio plays
```

---

### Offline Fallback Working

**Logs show:**
```
⚠️ TalkARApiService: Network error
✅ TalkARViewModel: Using local fallback
✅ VideoAnchorNode: Loading video: android.resource://...
```

**UI shows:**
```
✅ "Using offline video (network error)"
✅ Local video plays
✅ App continues to work
```

---

### Voice Query Working

**Logs show:**
```
✅ SpeechRecognition: ✅ Recognized: "..."
✅ TalkARApiService: Sending voice query
✅ TalkARApiService: Response: 200 OK
✅ TalkARViewModel: Playing response from backend
```

**UI shows:**
```
✅ "🎤 Listening..."
✅ "💭 Processing..."
✅ "▶️ Playing response..."
✅ Response audio plays
```

---

## 🎯 Next Steps

Once backend integration is confirmed working:

1. **Test with multiple images** (Tony, Sunrich)
2. **Test different speech queries**
3. **Test network interruption** (switch airplane mode during playback)
4. **Implement 3D video rendering** (Option 1)
5. **Add multiple dialogues** (Option 3)
6. **Production polish** (Option 4)

---

## 📝 Backend Endpoints Summary

### Currently Used

1. ✅ `GET /api/v1/sync/talking-head/:imageName`
   - Returns initial video URL
   - Used on long-press

2. ✅ `POST /api/v1/ai-pipeline/voice_query`
   - Processes speech query
   - Returns response audio URL

### Optional

3. ⚪ `GET /api/v1/images`
   - Returns image metadata
   - Can be used for additional info

---

## 🔍 Monitoring

### Watch Backend Logs

```bash
# In backend directory
npm run dev

# Watch for API calls
```

**Expected logs when app connects:**
```
GET /api/v1/sync/talking-head/sunrich 200
POST /api/v1/ai-pipeline/voice_query 200
```

### Watch App Logs

```bash
# Watch all TalkAR logs
adb logcat | grep -E "TalkAR|VideoAnchor|SpeechRecognition"

# Watch only API calls
adb logcat | grep TalkARApiService

# Watch only errors
adb logcat *:E | grep TalkAR
```

---

## ✅ Checklist

Before testing:
- [ ] Backend running on port 4000
- [ ] Backend has Tony and Sunrich images
- [ ] App built with correct API_HOST
- [ ] Device connected (USB or WiFi)
- [ ] Device on same network as computer (for physical device)
- [ ] Camera and microphone permissions granted

During testing:
- [ ] Image detection works
- [ ] Long-press triggers video load
- [ ] Backend API is called (check logs)
- [ ] Video plays (audio at minimum)
- [ ] Speech recognition activates
- [ ] Voice query sent to backend
- [ ] Response plays

Offline testing:
- [ ] Airplane mode enabled
- [ ] App falls back to local video
- [ ] Error message shown
- [ ] App continues to work

---

## 🎉 Success!

If you see:
- ✅ Backend logs showing API calls
- ✅ App logs showing "Loaded video from backend"
- ✅ Video audio playing
- ✅ Speech recognition working
- ✅ Response playing

**Then backend integration is working perfectly!** 🚀

You can now move to the next phase (3D video rendering, multiple dialogues, or production polish).
