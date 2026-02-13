# Week 6 - AI Pipeline Quick Start Guide

## 🚀 Getting Started in 5 Minutes

### 1. Start the Backend (1 minute)

```bash
cd backend
npm install
npm run dev
```

Server runs on: `http://localhost:3000`

### 2. Test the Endpoints (2 minutes)

Open a new terminal and run:

```bash
cd backend
node test-week6-endpoints.js
```

This runs comprehensive tests for all endpoints.

### 3. Generate Ad Content (1 minute)

```bash
curl -X POST http://localhost:3000/api/v1/ai-pipeline/generate_ad_content \
  -H "Content-Type: application/json" \
  -d '{"product": "Pepsi"}'
```

### 4. Expected Response

```json
{
  "success": true,
  "script": "Refresh your day with Pepsi — bold taste, cool vibes!",
  "audio_url": "http://localhost:3000/audio/audio-123.mp3",
  "video_url": "https://example.com/video/lipsync-123.mp4"
}
```

## 📚 Available Endpoints

### 1. Generate Script

```bash
POST /api/v1/ai-pipeline/generate_script
{
  "imageId": "product-123",
  "language": "en",
  "emotion": "happy",
  "productName": "Pepsi"
}
```

### 2. Generate Audio

```bash
POST /api/v1/ai-pipeline/generate_audio
{
  "text": "Refresh your day with Pepsi!",
  "language": "en",
  "emotion": "happy"
}
```

### 3. Generate Lip-Sync

```bash
POST /api/v1/ai-pipeline/generate_lipsync
{
  "imageId": "product-123",
  "audio_url": "http://localhost:3000/audio/audio.mp3",
  "emotion": "happy"
}
```

### 4. Generate Complete Ad Content (All-in-One)

```bash
POST /api/v1/ai-pipeline/generate_ad_content
{
  "product": "Pepsi"
}
```

### 5. Generate Streaming Ad Content (Optimized)

```bash
POST /api/v1/ai-pipeline/generate_ad_content_streaming
{
  "product": "iPhone 15 Pro"
}
```

## 🔧 Configuration

### Environment Variables

Create `backend/.env`:

```bash
# Server
PORT=3000
NODE_ENV=development

# API Keys (Optional for mock mode)
OPENAI_API_KEY=your-key
ELEVENLABS_API_KEY=your-key
SYNC_API_KEY=your-key

# Provider Selection
AI_PROVIDER=openai  # or groq
TTS_PROVIDER=elevenlabs  # or google
```

### Mock Mode (No API Keys Required)

For development, mock implementations run automatically if API keys aren't set.

## 📱 Android App Integration

### Connect Mobile App

1. Update `ApiConfig.kt`:

```kotlin
const val BASE_URL = "http://YOUR_IP:3000"
```

2. Run on device/emulator

3. Point camera at product poster

4. Video plays automatically in AR overlay

## 🧪 Running Tests

### Comprehensive Tests

```bash
node backend/test-week6-endpoints.js
```

### Individual Tests

```bash
node backend/test-ad-content.js
node backend/test-ai-pipeline.js
```

### Jest Unit Tests

```bash
npm test
```

## 📊 Test Results

Run tests to verify:

```bash
cd backend
node test-week6-endpoints.js
```

Expected output:

```
✓ Generate Script
✓ Generate Audio
✓ Generate Lip-Sync
✓ Complete Ad Content
✓ Error Handling
✓ Streaming Endpoint
✓ Sequential Pipeline

Total Tests: 7
Passed: 7
Failed: 0
```

## 📁 Project Structure

```
backend/
├── src/
│   ├── routes/
│   │   └── aiPipeline.ts          # All endpoints
│   ├── services/
│   │   └── aiPipelineService.ts    # Core logic
│   └── index.ts                    # Server entry
├── test-week6-endpoints.js         # Comprehensive tests
├── test-ad-content.js               # Ad tests
├── test-ai-pipeline.js              # Pipeline tests
└── WEEK6_TESTING_GUIDE.md          # Full testing guide
```

## ⚡ Performance

| Endpoint           | Time |
| ------------------ | ---- |
| Script Generation  | ~2s  |
| Audio Generation   | ~3s  |
| Lip-Sync           | ~5s  |
| Complete Pipeline  | ~20s |
| Streaming Pipeline | ~18s |

## 🐛 Troubleshooting

### Server Won't Start

```bash
# Check if port 3000 is available
lsof -i :3000
```

### Tests Failing

```bash
# Make sure server is running
curl http://localhost:3000/health
```

### Mock vs Real APIs

- **Mock Mode**: No API keys needed (development)
- **Real APIs**: Requires API keys (production)

## 📖 Documentation

- **Testing**: `backend/WEEK6_TESTING_GUIDE.md`
- **Deliverables**: `WEEK6_DELIVERABLES.md`
- **Summary**: `WEEK6_IMPLEMENTATION_SUMMARY.md`
- **API Reference**: `docs/API.md`

## ✅ Week 6 Complete!

All endpoints working:

- ✅ Generate script
- ✅ Generate audio (TTS)
- ✅ Generate lip-sync
- ✅ Complete ad content pipeline
- ✅ Android app integration
- ✅ Comprehensive testing

**Ready for Week 7! 🎉**
