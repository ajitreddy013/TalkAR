# Backend API Configuration Guide

## Overview

The TalkAR mobile app supports multiple backend environments for the lip-sync video generation service:

1. **Development**: Google Colab + ngrok (dynamic URLs)
2. **Demo**: Hugging Face Spaces (free tier)
3. **Production**: Render.com (default)

All environments use the same API interface for consistency.

---

## Configuration Methods

### Method 1: Gradle Properties (Recommended)

Create or edit `gradle.properties` in your project root:

```properties
# Development (Google Colab + ngrok)
API_PROTOCOL=https
API_HOST=your-ngrok-id.ngrok-free.app
API_PORT=443
API_VERSION=v1

# OR Demo (Hugging Face Spaces)
API_PROTOCOL=https
API_HOST=your-username-talkar-backend.hf.space
API_PORT=443
API_VERSION=v1

# OR Production (Render.com - default)
API_PROTOCOL=https
API_HOST=talkar-backend.onrender.com
API_PORT=443
API_VERSION=v1
```

### Method 2: Command Line Override

```bash
# Development with ngrok
./gradlew assembleDebug \
  -PAPI_PROTOCOL=https \
  -PAPI_HOST=abc123.ngrok-free.app \
  -PAPI_PORT=443

# Demo with Hugging Face
./gradlew assembleDebug \
  -PAPI_PROTOCOL=https \
  -PAPI_HOST=username-talkar.hf.space \
  -PAPI_PORT=443
```

### Method 3: Environment Variables (CI/CD)

```bash
export API_PROTOCOL=https
export API_HOST=abc123.ngrok-free.app
export API_PORT=443
export API_VERSION=v1
```

---

## Environment-Specific Setup

### 1. Development: Google Colab + ngrok

**Use Case**: Local development with Wav2Lip running on Google Colab

**Setup Steps**:

1. Start your backend on Google Colab
2. Expose it via ngrok:
   ```python
   from pyngrok import ngrok
   public_url = ngrok.connect(4000)
   print(f"Backend URL: {public_url}")
   ```
3. Extract the ngrok URL (e.g., `https://abc123.ngrok-free.app`)
4. Configure the mobile app:
   ```properties
   API_PROTOCOL=https
   API_HOST=abc123.ngrok-free.app
   API_PORT=443
   ```

**Limitations**:
- ngrok URLs change on each restart
- Free tier has connection limits
- Requires manual URL update

**Best Practices**:
- Use ngrok's static domains (paid) for stable URLs
- Keep ngrok session alive during development
- Update `gradle.properties` when URL changes

---

### 2. Demo: Hugging Face Spaces

**Use Case**: Public demo deployment with free hosting

**Setup Steps**:

1. Deploy backend to Hugging Face Spaces
2. Get your Space URL (e.g., `https://username-talkar-backend.hf.space`)
3. Configure the mobile app:
   ```properties
   API_PROTOCOL=https
   API_HOST=username-talkar-backend.hf.space
   API_PORT=443
   ```

**Limitations**:
- Free tier has CPU-only (slower generation)
- Cold start delays (first request takes longer)
- Rate limiting on free tier
- May sleep after inactivity

**Best Practices**:
- Implement longer timeouts for cold starts
- Show "warming up" message to users
- Consider upgrading to GPU tier for production
- Cache videos aggressively (24-hour retention)

---

### 3. Production: Render.com (Default)

**Use Case**: Production deployment with reliable hosting

**Setup Steps**:

1. Backend is already deployed to Render.com
2. Default configuration works out of the box:
   ```properties
   API_PROTOCOL=https
   API_HOST=talkar-backend.onrender.com
   API_PORT=443
   ```

**Features**:
- Stable URL (no changes)
- Better performance than free tiers
- Automatic SSL certificates
- Health checks and monitoring

---

## API Endpoints

All environments use the same API interface:

### 1. Generate Lip-Sync Video

```http
POST /api/v1/lipsync/generate
Content-Type: application/json

{
  "posterId": "poster-123",
  "text": "Hello, welcome to TalkAR!",
  "voiceId": "en-US-Standard-A"
}

Response:
{
  "success": true,
  "videoId": "video-abc123",
  "status": "processing"
}
```

### 2. Check Generation Status

```http
GET /api/v1/lipsync/status/{videoId}

Response:
{
  "success": true,
  "videoId": "video-abc123",
  "status": "complete",
  "progress": 1.0,
  "videoUrl": "https://backend.com/videos/video-abc123.mp4",
  "lipCoordinates": {
    "lipX": 0.45,
    "lipY": 0.55,
    "lipWidth": 0.15,
    "lipHeight": 0.10
  },
  "checksum": "sha256:abc123...",
  "durationMs": 5000,
  "sizeBytes": 1048576
}
```

### 3. Download Video

```http
GET {videoUrl}

Response: Binary video file (MP4, H.264)
```

---

## Configuration in Code

### ApiConfig.kt

The `ApiConfig` object reads configuration from `BuildConfig`:

```kotlin
object ApiConfig {
    private val PROTOCOL: String = BuildConfig.API_PROTOCOL  // "https"
    private val HOST: String = BuildConfig.API_HOST          // "abc123.ngrok-free.app"
    private val PORT: Int = BuildConfig.API_PORT             // 443
    private val API_VERSION: String = BuildConfig.API_VERSION // "v1"
    
    val BASE_URL = "$PROTOCOL://$HOST:$PORT"
    val API_V1_URL = "$BASE_URL/api/$API_VERSION"
    val LIPSYNC_ENDPOINT = "$API_V1_URL/lipsync"
}
```

### Build Variants

The app has three build variants:

1. **debug**: Uses `gradle.properties` or defaults to `10.0.2.2:443`
2. **beta**: Uses production backend (`talkar-backend.onrender.com`)
3. **release**: Uses production backend (`talkar-backend.onrender.com`)

---

## Testing Different Environments

### Test with ngrok (Development)

```bash
# 1. Start backend on Colab with ngrok
# 2. Get ngrok URL: https://abc123.ngrok-free.app
# 3. Build and run app
./gradlew assembleDebug \
  -PAPI_HOST=abc123.ngrok-free.app \
  -PAPI_PORT=443
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Test with Hugging Face (Demo)

```bash
# 1. Deploy to HF Spaces
# 2. Get Space URL: https://username-talkar.hf.space
# 3. Build and run app
./gradlew assembleDebug \
  -PAPI_HOST=username-talkar.hf.space \
  -PAPI_PORT=443
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Test with Production

```bash
# Uses default configuration
./gradlew assembleRelease
adb install -r app/build/outputs/apk/release/app-release.apk
```

---

## Troubleshooting

### Issue: "Backend Unavailable" Error

**Causes**:
- Wrong URL in configuration
- Backend is down or sleeping (HF Spaces)
- Network connectivity issues
- ngrok session expired

**Solutions**:
1. Check `ApiConfig` logs in Logcat:
   ```
   I/ApiConfig: Initializing ApiConfig with BASE_URL: https://abc123.ngrok-free.app
   ```
2. Verify backend is running:
   ```bash
   curl https://abc123.ngrok-free.app/api/v1/health
   ```
3. Update ngrok URL if changed
4. Wait for HF Spaces to wake up (cold start)

### Issue: "Generation Timeout"

**Causes**:
- Slow backend (CPU-only on HF Spaces)
- Large video generation
- Network latency

**Solutions**:
1. Increase timeout in `BackendVideoFetcherImpl`:
   ```kotlin
   private const val STATUS_POLL_TIMEOUT_MS = 120000L // 2 minutes
   ```
2. Upgrade to GPU tier on HF Spaces
3. Use production backend (Render.com)

### Issue: ngrok URL Changes

**Causes**:
- ngrok free tier generates new URLs on restart
- Session expired

**Solutions**:
1. Use ngrok static domains (paid):
   ```bash
   ngrok http 4000 --domain=your-static-domain.ngrok-free.app
   ```
2. Update `gradle.properties` with new URL
3. Rebuild app

---

## API Interface Consistency

All environments MUST implement the same API interface:

### Required Endpoints

✅ `POST /api/v1/lipsync/generate`
✅ `GET /api/v1/lipsync/status/{videoId}`
✅ `GET {videoUrl}` (video download)

### Required Response Fields

✅ `success` (boolean)
✅ `videoId` (string)
✅ `status` (string: "processing", "complete", "failed")
✅ `videoUrl` (string, when complete)
✅ `lipCoordinates` (object with lipX, lipY, lipWidth, lipHeight)
✅ `checksum` (string, SHA-256)

### Error Handling

All environments MUST return consistent error responses:

```json
{
  "success": false,
  "error": "Error message",
  "code": 2002
}
```

---

## Environment Comparison

| Feature | Development (ngrok) | Demo (HF Spaces) | Production (Render) |
|---------|-------------------|------------------|---------------------|
| **URL Stability** | ❌ Changes on restart | ✅ Stable | ✅ Stable |
| **Performance** | ⚠️ Depends on Colab | ❌ CPU-only (slow) | ✅ Fast |
| **Cost** | 🆓 Free | 🆓 Free | 💰 Paid |
| **Cold Start** | ✅ None | ❌ 30-60s | ✅ None |
| **Rate Limiting** | ⚠️ ngrok limits | ⚠️ HF limits | ✅ High limits |
| **Uptime** | ⚠️ Manual | ⚠️ Sleeps | ✅ 99.9% |
| **Best For** | Local dev | Public demo | Production |

---

## Recommendations

### For Development
- Use Google Colab + ngrok for quick iteration
- Keep ngrok session alive
- Update URL in `gradle.properties` when it changes

### For Demo
- Deploy to Hugging Face Spaces for public demos
- Warn users about cold start delays
- Consider GPU tier for better performance

### For Production
- Use Render.com (default) for stable, fast service
- Monitor backend health and performance
- Implement proper error handling and retries

---

## Requirements Satisfied

✅ **Requirement 12.1**: Development environment (Google Colab + ngrok) configured
✅ **Requirement 12.2**: Demo environment (Hugging Face Spaces) configured
✅ **Requirement 12.4**: API interface consistency ensured across all environments

---

## Next Steps

1. ✅ Configure backend URL for your environment
2. ✅ Build and install app with correct configuration
3. ✅ Test lip-sync video generation
4. ✅ Verify API consistency across environments
5. ⏭️ Proceed to Task 19: System Integration Checkpoint
