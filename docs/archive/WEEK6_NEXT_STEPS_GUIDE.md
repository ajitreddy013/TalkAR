# Week 6 - Next Steps Implementation Guide

## 🎯 Step-by-Step Execution

This guide walks you through completing the remaining steps to go from development to production.

---

## ✅ Step 1: Add Real API Keys (Optional but Recommended)

### Current Status

- Server running on: Port 4000
- Using mock implementations (no API keys configured)
- Development mode active

### Adding Real API Keys

#### 1.1 Edit Backend Environment File

```bash
cd backend
nano .env  # or use your preferred editor
```

#### 1.2 Add Your API Keys

```bash
# OpenAI (for script generation)
OPENAI_API_KEY=sk-your-actual-key-here

# ElevenLabs (for text-to-speech)
ELEVENLABS_API_KEY=your-actual-key-here

# Sync.so (for lip-sync video)
SYNC_API_KEY=your-actual-key-here

# GroqCloud (alternative AI provider)
GROQCLOUD_API_KEY=your-actual-key-here

# Google Cloud TTS (alternative TTS provider)
GOOGLE_CLOUD_TTS_API_KEY=your-actual-key-here
```

#### 1.3 Configure Provider Selection

```bash
# AI Provider (openai or groq)
AI_PROVIDER=openai

# TTS Provider (elevenlabs or google)
TTS_PROVIDER=elevenlabs
```

#### 1.4 Restart Server to Apply Changes

```bash
# Stop current server
pkill -f "npm run dev"

# Restart server
cd backend
npm run dev
```

#### 1.5 Verify Real API Integration

```bash
# Test with real APIs
curl -X POST http://localhost:4000/api/v1/ai-pipeline/generate_ad_content \
  -H "Content-Type: application/json" \
  -d '{"product": "Pepsi"}'
```

**Expected**: Higher quality AI-generated content (not mock scripts)

---

## ✅ Step 2: Update Android App Configuration

### 2.1 Current Configuration Issues

**Problem**: Server is running on port 4000, but app expects port 3000

**Current App Config**:

- Default Host: `10.17.5.127`
- Default Port: `3000`
- Protocol: `http`

### 2.2 Update Options

#### Option A: Update Server Port (Easiest)

Edit `backend/.env`:

```bash
PORT=3000  # Change from 4000 to 3000
```

Restart server:

```bash
pkill -f "npm run dev"
cd backend && npm run dev
```

#### Option B: Update Android App Config

Edit `mobile-app/app/src/main/java/com/talkar/app/data/config/ApiConfig.kt`:

```kotlin
private val PORT: Int = 4000  // Change from 3000 to 4000
```

Then rebuild the app.

### 2.3 Find Your Local IP Address

```bash
# On macOS/Linux
ifconfig | grep "inet " | grep -v 127.0.0.1

# Or
ipconfig getifaddr en0
```

**Example Output**: `192.168.1.100`

### 2.4 Update Android Build Config

Edit `mobile-app/app/build.gradle`:

```gradle
android {
    buildTypes {
        debug {
            buildConfigField "String", "API_PROTOCOL", "\"http\""
            buildConfigField "String", "API_HOST", "\"YOUR_IP_ADDRESS\""
            buildConfigField "Int", "API_PORT", "3000"
            buildConfigField "String", "API_VERSION", "\"v1\""
        }
    }
}
```

Replace `YOUR_IP_ADDRESS` with your actual IP (e.g., `192.168.1.100`)

### 2.5 Rebuild Android App

```bash
cd mobile-app
./gradlew assembleDebug
# or
./gradlew installDebug
```

---

## ✅ Step 3: Test Android App Connection

### 3.1 Ensure Server is Running

```bash
# Check server health
curl http://localhost:3000/health
# or
curl http://YOUR_IP:3000/health
```

### 3.2 Test from Android Device

1. **Open App** on device/emulator
2. **Point Camera** at product poster
3. **Check Logs** for API calls:

```bash
# Android Studio Logcat
adb logcat | grep "ApiConfig\|AdContentGenerationService"
```

### 3.3 Expected Behavior

- ✅ Camera opens
- ✅ ARCore detects poster
- ✅ API call to `/generate_ad_content` endpoint
- ✅ Video plays in AR overlay
- ✅ Talking head animation visible

### 3.4 Troubleshooting

**Issue**: App can't connect to backend

**Solutions**:

```bash
# Check firewall
sudo ufw allow 3000/tcp

# Check server is listening on network interface
netstat -an | grep 3000

# Test connectivity from device
# Install curl on Android or use browser
# Navigate to: http://YOUR_IP:3000/health
```

---

## ✅ Step 4: Deploy When Ready

### 4.1 Pre-Deployment Checklist

- [ ] All tests passing
- [ ] Real API keys configured
- [ ] Android app connects successfully
- [ ] Performance metrics acceptable
- [ ] Error handling verified
- [ ] Documentation complete

### 4.2 Deployment Options

#### Option A: Local Development (Current)

```bash
# Server already running
# No deployment needed
# Good for testing
```

#### Option B: Staging Environment

```bash
# Update .env for staging
NODE_ENV=staging
PORT=4000

# Deploy to staging server
# Update Android app config for staging URL
```

#### Option C: Production Deployment

1. **Build for production**:

```bash
cd backend
npm run build
npm start  # Uses dist/index.js
```

2. **Set environment variables**:

```bash
NODE_ENV=production
PORT=3000
# Add all API keys
# Configure CORS for production domain
```

3. **Use process manager** (PM2):

```bash
npm install -g pm2
pm2 start dist/index.js
pm2 save
pm2 startup
```

4. **Update Android app**:

- Set production API URL
- Build release APK
- Deploy to Play Store

---

## 🚀 Step 5: Continue with Week 7 Development

### Week 7 Suggested Focus Areas

1. **Performance Optimization**

   - Add Redis caching
   - Implement request queuing
   - Optimize video quality settings

2. **Enhanced Features**

   - Support more languages
   - Add voice cloning
   - Improve lip-sync quality
   - Add 3D avatar support

3. **Testing & Quality**

   - Integration tests
   - Performance benchmarks
   - User acceptance testing

4. **Documentation**
   - API documentation
   - Deployment guides
   - User manuals

---

## 📋 Quick Reference Commands

### Server Management

```bash
# Start server
cd backend && npm run dev

# Restart server
pkill -f "npm run dev" && cd backend && npm run dev

# Check server status
curl http://localhost:4000/health
```

### Testing

```bash
# Run comprehensive tests
cd backend && node test-week6-endpoints.js

# Test specific endpoint
curl -X POST http://localhost:4000/api/v1/ai-pipeline/generate_ad_content \
  -H "Content-Type: application/json" \
  -d '{"product": "Pepsi"}'
```

### Android App

```bash
# Build debug APK
cd mobile-app && ./gradlew assembleDebug

# Install on connected device
adb install app/build/outputs/apk/debug/app-debug.apk

# View logs
adb logcat | grep TalkAR
```

---

## 🎯 Current Status Summary

✅ **Week 6 Complete**

- Backend pipeline working
- All endpoints tested and passing
- Mock implementations functional
- Documentation complete
- Android app integrated

⏳ **Next Actions**

1. Add real API keys (optional)
2. Update Android config (change port 3000 → 4000)
3. Test app-device connection
4. Deploy when ready
5. Begin Week 7 development

---

## 📞 Need Help?

**Backend Issues**:

- Check `backend/WEEK6_TESTING_GUIDE.md`
- Review server logs: `tail -f /tmp/talkar-server.log`

**Android Issues**:

- Check `mobile-app/app/src/main/java/com/talkar/app/data/config/ApiConfig.kt`
- Review Android Studio logs

**API Issues**:

- Verify API keys are valid
- Check rate limits
- Review error logs

---

**Ready to proceed with the next step!** 🚀
