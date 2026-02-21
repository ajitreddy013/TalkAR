# Backend Integration Guide

## Overview

The TalkAR mobile app now integrates with your backend API to fetch dynamic video content and process conversational queries. This document explains how the integration works and how to configure it.

---

## Architecture

```
Mobile App (Android)
    ↓
TalkARApiService
    ↓
Backend API (Node.js/Express)
    ↓
AI Pipeline / Sync Services
```

---

## API Endpoints Used

### 1. Get Initial Video
**Endpoint:** `GET /api/v1/sync/talking-head/:imageName`

**Query Parameters:**
- `language` (optional): Language code (default: "en")
- `emotion` (optional): Emotion for video (default: "excited")

**Response:**
```json
{
  "videoUrl": "https://backend.com/videos/sunrich_intro.mp4",
  "duration": 15000,
  "emotion": "excited",
  "script": "Welcome to Sunrich!"
}
```

**Used when:** User long-presses on detected image

---

### 2. Send Voice Query
**Endpoint:** `POST /api/v1/ai-pipeline/voice_query`

**Request Body:**
```json
{
  "query": "Tell me more about this product"
}
```

**Response:**
```json
{
  "success": true,
  "response": "Sunrich is a premium water bottle...",
  "audioUrl": "https://backend.com/audio/response_123.mp3",
  "emotion": "happy"
}
```

**Used when:** Speech recognition completes after initial video

---

### 3. Get Image Metadata (Optional)
**Endpoint:** `GET /api/v1/images`

**Response:**
```json
[
  {
    "id": "img_123",
    "name": "sunrich",
    "description": "Sunrich Water Bottle",
    "imageUrl": "https://backend.com/images/sunrich.jpg",
    "thumbnailUrl": "https://backend.com/images/sunrich_thumb.jpg",
    "script": "Stay hydrated with Sunrich!"
  }
]
```

**Used when:** Fetching additional image information

---

## Configuration

### Backend URL Configuration

The app uses `BuildConfig` values set in `build.gradle`:

**Debug Build (Development):**
```gradle
buildConfigField "String", "API_PROTOCOL", "\"http\""
buildConfigField "String", "API_HOST", "\"10.0.2.2\""  // Android emulator localhost
buildConfigField "int", "API_PORT", "4000"
buildConfigField "String", "API_VERSION", "\"v1\""
```

**Release Build (Production):**
```gradle
buildConfigField "String", "API_PROTOCOL", "\"https\""
buildConfigField "String", "API_HOST", "\"talkar-backend.onrender.com\""
buildConfigField "int", "API_PORT", "443"
buildConfigField "String", "API_VERSION", "\"v1\""
```

### Override via gradle.properties

You can override these values without modifying code:

```properties
# gradle.properties
API_PROTOCOL=https
API_HOST=your-backend.com
API_PORT=443
API_VERSION=v1
```

Then build with:
```bash
./gradlew assembleDebug -PAPI_HOST=your-backend.com
```

---

## Flow Diagram

### Initial Video Flow

```
1. User points camera at image
   ↓
2. ARCore detects "sunrich"
   ↓
3. User long-presses screen
   ↓
4. App shows "⏳ Loading video..."
   ↓
5. TalkARApiService.getInitialVideo("sunrich")
   ↓
6. Backend returns video URL
   ↓
7. App plays video (audio + visual when 3D rendering complete)
   ↓
8. Video completes
```

### Conversational Response Flow

```
1. Video completes
   ↓
2. App shows "🎤 Listening..."
   ↓
3. User speaks: "Tell me more"
   ↓
4. Speech recognition converts to text
   ↓
5. App shows "💭 Processing..."
   ↓
6. TalkARApiService.sendVoiceQuery("Tell me more")
   ↓
7. Backend processes with AI
   ↓
8. Backend returns response audio URL
   ↓
9. App plays response video/audio
   ↓
10. Flow completes, ready for next interaction
```

---

## Offline Fallback

The app gracefully handles network errors:

### When Backend is Unavailable

1. **Initial Video:** Falls back to local `sunrich_video.mp4`
2. **Voice Response:** Falls back to local video with generic message
3. **Error Message:** Shows "Using offline video (network error)"

### Fallback Logic

```kotlin
try {
    // Try backend first
    val response = apiService.getInitialVideo(imageName)
    playVideo(response.videoUrl)
} catch (e: ApiException) {
    // Fallback to local
    playVideo(localVideoUri)
    showMessage("Using offline video")
}
```

---

## Error Handling

### API Exceptions

The app handles different error scenarios:

**Network Errors (statusCode = 0):**
- No internet connection
- Backend server down
- DNS resolution failure

**HTTP Errors:**
- `400` Bad Request: Invalid parameters
- `401` Unauthorized: API key issues
- `404` Not Found: Image/video not found
- `429` Rate Limit: Too many requests
- `500` Server Error: Backend issue
- `408` Timeout: Request took too long

### User Experience

- **Loading State:** Shows "⏳ Loading video..." during API call
- **Error State:** Shows error message with dismiss button
- **Fallback:** Automatically uses local content on error
- **Retry:** User can retry by long-pressing again

---

## Testing

### Test with Local Backend

1. **Start backend:**
   ```bash
   cd backend
   npm run dev
   ```

2. **Configure app for local backend:**
   ```bash
   cd mobile-app
   ./gradlew installDebug -PAPI_HOST=10.0.2.2 -PAPI_PORT=4000
   ```

3. **Test flow:**
   - Point camera at sunrich image
   - Long-press → should fetch from `http://10.0.2.2:4000`
   - Check backend logs for API calls

### Test with Production Backend

1. **Build release:**
   ```bash
   ./gradlew assembleRelease
   ```

2. **Install and test:**
   - App will use production URL from BuildConfig
   - Should fetch from `https://talkar-backend.onrender.com`

### Test Offline Mode

1. **Enable airplane mode** on device
2. **Long-press on image**
3. **Verify:** App uses local video and shows offline message

---

## Debugging

### View API Logs

```bash
adb logcat | grep "TalkARApiService"
```

### Successful API Call

```
TalkARApiService: Fetching video for: sunrich
TalkARApiService: Response: 200 OK
TalkARViewModel: Loaded video from backend: https://...
```

### API Error

```
TalkARApiService: API error: 404 Not Found
TalkARViewModel: API error loading video, using local fallback
```

### Network Error

```
TalkARApiService: Network error: Unable to resolve host
TalkARViewModel: Using offline video (network error)
```

---

## Backend Requirements

### Required Endpoints

Your backend MUST implement:

1. ✅ `GET /api/v1/sync/talking-head/:imageName`
2. ✅ `POST /api/v1/ai-pipeline/voice_query`

### Optional Endpoints

3. ⚪ `GET /api/v1/images` (for metadata)

### Response Format

All responses must be valid JSON with appropriate status codes:
- `200` OK: Success
- `400` Bad Request: Invalid input
- `404` Not Found: Resource not found
- `500` Server Error: Backend error

### CORS Configuration

If testing from emulator/device, ensure CORS is enabled:

```javascript
// backend/src/app.ts
app.use(cors({
  origin: '*', // Or specific origins
  methods: ['GET', 'POST'],
  credentials: true
}));
```

---

## Performance Considerations

### Timeouts

- **Connect Timeout:** 30 seconds
- **Read Timeout:** 30 seconds

### Caching

Currently no caching implemented. Future improvements:
- Cache video URLs for detected images
- Cache API responses for common queries
- Preload videos in background

### Network Usage

- Initial video: ~5-10 MB (depends on video length)
- Voice response: ~1-3 MB (audio only)
- API requests: <1 KB

---

## Security

### HTTPS

Production builds use HTTPS by default:
- ✅ Encrypted communication
- ✅ Certificate validation
- ✅ Secure data transfer

### API Keys

If your backend requires API keys:

1. Add to `gradle.properties` (gitignored):
   ```properties
   API_KEY=your_secret_key
   ```

2. Add to BuildConfig:
   ```gradle
   buildConfigField "String", "API_KEY", "\"${project.API_KEY}\""
   ```

3. Use in API service:
   ```kotlin
   connection.setRequestProperty("X-API-Key", BuildConfig.API_KEY)
   ```

---

## Troubleshooting

### Issue: "Network error: Unable to resolve host"

**Cause:** Backend URL is incorrect or unreachable

**Solution:**
1. Check backend is running: `curl http://localhost:4000/api/v1/images`
2. Verify API_HOST in build.gradle
3. For emulator, use `10.0.2.2` not `localhost`
4. For device, use actual IP or domain

---

### Issue: "API error: 404 Not Found"

**Cause:** Image name doesn't exist in backend

**Solution:**
1. Check image name matches backend data
2. Verify endpoint: `GET /api/v1/sync/talking-head/sunrich`
3. Check backend logs for the request

---

### Issue: "Using offline video" always shows

**Cause:** Backend returns empty videoUrl

**Solution:**
1. Check backend response format
2. Ensure `videoUrl` field is populated
3. Verify video file exists and is accessible

---

### Issue: Video URL returns 403 Forbidden

**Cause:** Video file not publicly accessible

**Solution:**
1. Check S3/storage permissions
2. Ensure signed URLs if using private storage
3. Verify CORS headers on video files

---

## Next Steps

### Immediate

1. ✅ Backend integration complete
2. ⏳ Test with real backend
3. ⏳ Implement 3D video rendering
4. ⏳ Add video caching

### Future Enhancements

- Preload videos in background
- Cache API responses
- Retry logic with exponential backoff
- Analytics tracking
- A/B testing support
- Multi-language support
- Offline queue for failed requests

---

## API Service Code

The integration is implemented in:
- `TalkARApiService.kt` - HTTP client for backend API
- `TalkARViewModel.kt` - Business logic and state management
- `TalkARScreen.kt` - UI with loading states

### Key Classes

```kotlin
// API Service
class TalkARApiService {
    suspend fun getInitialVideo(imageName: String): VideoResponse
    suspend fun sendVoiceQuery(query: String): VoiceQueryResponse
    suspend fun getImageMetadata(imageName: String): ImageMetadata
}

// Response Models
data class VideoResponse(videoUrl, duration, emotion, script)
data class VoiceQueryResponse(success, response, audioUrl, emotion)
data class ImageMetadata(id, name, description, imageUrl, script)
```

---

## Support

For issues or questions:
1. Check logs: `adb logcat | grep TalkAR`
2. Review this documentation
3. Check backend API documentation
4. Test with curl/Postman first
