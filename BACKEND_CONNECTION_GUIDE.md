# Backend Connection Troubleshooting Guide

## Current Status

Based on your logs, the app is working but **cannot connect to the backend server**.

### What's Working ✅
- App launches successfully
- ARCore initializes properly
- Camera permissions granted
- AR session configured
- Frame processing running

### What's Not Working ❌
- Backend API unreachable at `10.0.2.2:443`
- Network timeouts after 30 seconds
- No posters loaded from backend
- ConfigSyncService failing to fetch data

---

## The Backend Connection Issue

### Error in Logs:
```
java.net.SocketTimeoutException: failed to connect to /10.0.2.2 (port 443) 
from /172.20.38.102 (port 60658) after 30000ms
```

### What This Means:
- `10.0.2.2` is the Android emulator's special alias for `localhost` on your host machine
- Port `443` is HTTPS
- The app expects a backend server running on your computer at `https://localhost:443`
- **The server is not running or not accessible**

---

## Solutions

### Option 1: Start Your Backend Server (Recommended)

If you have a backend server for this app:

1. **Start the backend:**
   ```bash
   # Navigate to your backend directory
   cd /path/to/backend
   
   # Start the server (example commands)
   npm start
   # or
   python manage.py runserver
   # or
   docker-compose up
   ```

2. **Verify it's running:**
   ```bash
   curl https://localhost:443/api/v1/health
   # or
   curl http://localhost:443/api/v1/health
   ```

3. **Check the port:**
   - Your backend might be running on a different port (e.g., 8000, 3000, 4000)
   - Update `ApiConfig.kt` to match your backend port

---

### Option 2: Use Test Posters (Quick Testing)

The app now falls back to test posters when the backend is unavailable.

**Add a test poster to assets:**

1. **Create assets folder:**
   ```bash
   mkdir -p mobile-app/app/src/main/assets
   ```

2. **Add a test image:**
   - Copy a poster image (JPG/PNG) to `mobile-app/app/src/main/assets/test_poster.jpg`
   - Image should have a human face
   - Recommended size: 1024x1024 or smaller

3. **Rebuild:**
   ```bash
   cd mobile-app
   ./gradlew installDebug
   ```

4. **Result:**
   - Camera will show immediately
   - App will use test poster for detection
   - No backend required

---

### Option 3: Update API Configuration

If your backend is running on a different address/port:

**Edit `ApiConfig.kt`:**

```kotlin
// Current (for emulator):
const val BASE_URL = "https://10.0.2.2:443"

// For different port:
const val BASE_URL = "https://10.0.2.2:8000"  // Port 8000

// For physical device (use your computer's IP):
const val BASE_URL = "https://192.168.1.100:443"  // Replace with your IP

// For production:
const val BASE_URL = "https://api.talkar.com"
```

**Find your computer's IP:**
```bash
# macOS/Linux:
ifconfig | grep "inet "

# Windows:
ipconfig
```

---

### Option 4: Mock Backend (Development)

Create a simple mock server for testing:

**Using Python:**
```python
# mock_server.py
from flask import Flask, jsonify
from flask_cors import CORS

app = Flask(__name__)
CORS(app)

@app.route('/api/v1/images', methods=['GET'])
def get_images():
    return jsonify([
        {
            "id": "poster1",
            "name": "Test Poster",
            "imageUrl": "https://example.com/poster.jpg",
            "description": "Test poster for AR",
            "dialogues": []
        }
    ])

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=443, ssl_context='adhoc')
```

**Run it:**
```bash
pip install flask flask-cors pyopenssl
python mock_server.py
```

---

## Current App Behavior (After Latest Fix)

### With Backend Available:
1. App starts
2. Loads posters from backend (10 second timeout)
3. Configures ARCore with poster images
4. Camera shows with detection enabled
5. Point at poster → detection works

### Without Backend (Graceful Degradation):
1. App starts
2. Attempts to load posters (times out after 10s)
3. Falls back to test poster from assets
4. Camera shows immediately
5. Detection works with test poster only

### Logs to Expect:

**Success:**
```
Loading posters from backend...
Fetching images from API...
✅ ARCore config updated with 3 poster images
AR session ready, starting frame processing
```

**Timeout (Graceful):**
```
Loading posters from backend...
Poster loading timed out, trying test poster
Using test poster as fallback
⚠️ No posters loaded - detection will not work
Camera will still be visible for testing
AR session ready, starting frame processing
```

---

## Performance Notes

### Current Performance Issue:
- **984ms freeze** (88 skipped frames) during startup
- Caused by poster loading blocking initialization

### Why It Happens:
1. App starts
2. Tries to connect to backend
3. Waits 30 seconds for timeout
4. Main thread blocked during network wait
5. UI freezes

### Fix Applied:
- Added 10 second timeout (reduced from 30s)
- Camera shows even if posters fail to load
- Better error handling

### Further Optimization:
To eliminate the freeze completely, implement poster caching:
- Download posters once on first launch
- Store in local database
- Load from cache on subsequent launches
- Background sync for updates

---

## Testing Checklist

### 1. Test Without Backend:
- [ ] App launches
- [ ] Camera feed visible (not black)
- [ ] Green camera indicator appears
- [ ] Logs show "Camera will still be visible for testing"
- [ ] No crash or error dialogs

### 2. Test With Backend:
- [ ] Start backend server
- [ ] App launches
- [ ] Posters load successfully
- [ ] Logs show "✅ ARCore config updated with X poster images"
- [ ] Point camera at poster
- [ ] Detection works

### 3. Test Performance:
- [ ] App launches in < 3 seconds
- [ ] No "Skipped X frames" warnings (or < 30 frames)
- [ ] UI remains responsive
- [ ] Camera feed smooth (no stuttering)

---

## Quick Fixes Summary

### To See Camera Immediately:
```bash
# Add test poster to assets
mkdir -p mobile-app/app/src/main/assets
cp /path/to/poster.jpg mobile-app/app/src/main/assets/test_poster.jpg

# Rebuild
cd mobile-app
./gradlew installDebug
```

### To Connect to Backend:
1. Start your backend server
2. Verify it's accessible at `https://localhost:443`
3. Or update `ApiConfig.kt` with correct URL
4. Rebuild and test

### To Reduce Startup Time:
- Implement poster caching (see `RACE_CONDITION_FIX_SUMMARY.md`)
- Or use bundled test posters
- Or show loading indicator during poster fetch

---

## Next Steps

1. **Immediate:** Add test poster to assets for quick testing
2. **Short-term:** Start backend server or update API config
3. **Medium-term:** Implement poster caching
4. **Long-term:** Add loading states and offline support

---

## Related Documentation

- `AR_IMPROVEMENTS_SUMMARY.md` - Overview of all AR improvements
- `RACE_CONDITION_FIX_SUMMARY.md` - Performance optimization details
- `ARCORE_API_KEY_SETUP.md` - ARCore API key configuration

---

## Support

If you continue to see issues:

1. **Check logs for:**
   - "AR session ready, starting frame processing" ✅
   - "Frame processing loop started" ✅
   - Network timeout errors ❌

2. **Verify:**
   - Camera permissions granted
   - ARCore installed on device/emulator
   - Backend server running (if using backend)
   - Test poster in assets (if using fallback)

3. **Common Issues:**
   - Black screen → Check camera permissions
   - No detection → Check posters loaded
   - Slow startup → Check network timeouts
   - Crash → Check logs for exceptions
