# Week 6 Testing Guide - AI Pipeline Setup & Integration

## Overview

This guide provides comprehensive testing procedures for the AI Pipeline endpoints implemented in Week 6.

## Prerequisites

1. **Backend Server Running**

   ```bash
   cd backend
   npm install
   npm run dev  # Server should run on http://localhost:3000
   ```

2. **Environment Variables**
   - Create a `.env` file in the `backend/` directory
   - Add necessary API keys (see `env.example` for template)
   - For development/testing, mock implementations will be used if API keys are not provided

## Test Suite

### Running Tests

```bash
# Run comprehensive Week 6 endpoint tests
node test-week6-endpoints.js

# Run individual component tests
node test-ad-content.js
node test-ai-pipeline.js
node test-script-generation.js
node test-tts.js
```

## Test Scenarios

### 1. Test `/generate_script` Endpoint

**Purpose**: Verify script generation from product names

**Request**:

```bash
curl -X POST http://localhost:3000/api/v1/ai-pipeline/generate_script \
  -H "Content-Type: application/json" \
  -d '{
    "imageId": "test-product-123",
    "language": "en",
    "emotion": "happy",
    "productName": "Pepsi"
  }'
```

**Expected Response**:

```json
{
  "success": true,
  "script": "Refresh your day with Pepsi — bold taste, cool vibes!",
  "language": "en",
  "emotion": "happy"
}
```

**Validations**:

- ✓ Returns a valid script string
- ✓ Script is 2-4 lines long
- ✓ Script matches emotion and language parameters
- ✓ Falls back to mock data if API keys not configured

### 2. Test `/generate_audio` Endpoint

**Purpose**: Verify TTS (text-to-speech) conversion

**Request**:

```bash
curl -X POST http://localhost:3000/api/v1/ai-pipeline/generate_audio \
  -H "Content-Type: application/json" \
  -d '{
    "text": "Welcome to our exhibition!",
    "language": "en",
    "emotion": "happy",
    "voiceId": "21m00Tcm4TlvDq8ikWAM"
  }'
```

**Expected Response**:

```json
{
  "success": true,
  "audioUrl": "http://localhost:3000/audio/audio-123.mp3",
  "duration": 10
}
```

**Validations**:

- ✓ Returns valid audio URL
- ✓ Audio file is accessible
- ✓ Duration is reasonable
- ✓ Audio matches emotion parameter

### 3. Test `/generate_lipsync` Endpoint

**Purpose**: Verify lip-sync video generation

**Request**:

```bash
curl -X POST http://localhost:3000/api/v1/ai-pipeline/generate_lipsync \
  -H "Content-Type: application/json" \
  -d '{
    "imageId": "test-image-123",
    "audio_url": "http://localhost:3000/audio/audio-123.mp3",
    "emotion": "happy",
    "avatar": "avatar-image.png"
  }'
```

**Expected Response**:

```json
{
  "success": true,
  "videoUrl": "https://example.com/video/lipsync-123.mp4",
  "duration": 15,
  "jobId": "job-12345"
}
```

**Validations**:

- ✓ Returns valid video URL
- ✓ Video URL is accessible
- ✓ Job ID present for async operations
- ✓ Duration matches audio length

### 4. Test `/generate_ad_content` Endpoint

**Purpose**: Verify complete pipeline (script → audio → lipsync)

**Request**:

```bash
curl -X POST http://localhost:3000/api/v1/ai-pipeline/generate_ad_content \
  -H "Content-Type: application/json" \
  -d '{
    "product": "Pepsi"
  }'
```

**Expected Response**:

```json
{
  "success": true,
  "script": "Refresh your day with Pepsi — bold taste, cool vibes!",
  "audio_url": "http://localhost:3000/audio/audio-123.mp3",
  "video_url": "https://example.com/video/lipsync-123.mp4"
}
```

**Validations**:

- ✓ All three components (script, audio, video) generated
- ✓ Complete pipeline executes in < 60 seconds
- ✓ URLs are accessible
- ✓ Content matches product name

### 5. Test `/generate_ad_content_streaming` Endpoint

**Purpose**: Verify optimized streaming pipeline

**Request**:

```bash
curl -X POST http://localhost:3000/api/v1/ai-pipeline/generate_ad_content_streaming \
  -H "Content-Type: application/json" \
  -d '{
    "product": "iPhone 15 Pro"
  }'
```

**Expected Response**:

```json
{
  "success": true,
  "script": "Experience the future with iPhone 15 Pro...",
  "audio_url": "http://localhost:3000/audio/audio-456.mp3",
  "video_url": "https://example.com/video/lipsync-456.mp4"
}
```

**Validations**:

- ✓ Faster initial response time
- ✓ Optimized for streaming playback
- ✓ Same output quality as regular endpoint

## Error Handling Tests

### Test 1: Missing Product Parameter

**Request**:

```bash
curl -X POST http://localhost:3000/api/v1/ai-pipeline/generate_ad_content \
  -H "Content-Type: application/json" \
  -d '{}'
```

**Expected**: `400 Bad Request` with error message

### Test 2: Empty Product Name

**Request**:

```bash
curl -X POST http://localhost:3000/api/v1/ai-pipeline/generate_ad_content \
  -H "Content-Type: application/json" \
  -d '{"product": ""}'
```

**Expected**: `400 Bad Request` with validation error

### Test 3: Product Name Too Long

**Request**:

```bash
curl -X POST http://localhost:3000/api/v1/ai-pipeline/generate_ad_content \
  -H "Content-Type: application/json" \
  -d '{"product": "' + "A".repeat(150) + '"}'
```

**Expected**: `400 Bad Request` with length validation error

### Test 4: Invalid Language Code

**Request**:

```bash
curl -X POST http://localhost:3000/api/v1/ai-pipeline/generate_script \
  -H "Content-Type: application/json" \
  -d '{"imageId": "test", "language": "invalid"}'
```

**Expected**: `400 Bad Request` with validation error

## Integration Testing

### Test 1: Sequential Pipeline

Test the complete flow step-by-step:

1. Generate script for product
2. Convert script to audio
3. Generate lip-sync video from audio
4. Verify all URLs are accessible

### Test 2: Multiple Products

Test with various products:

- Electronics: "iPhone", "MacBook", "Tesla"
- Fashion: "Nike Shoes", "Ray-Ban Sunglasses"
- Food: "Pepsi", "Coca-Cola"
- Appliances: "Coffee Maker", "Bluetooth Speaker"

### Test 3: Different Emotions

Test with different emotions:

- "happy" - upbeat, energetic
- "neutral" - calm, informative
- "serious" - professional, authoritative
- "surprised" - excited, amazed

### Test 4: Different Languages

Test with different languages:

- English (en)
- Spanish (es)
- French (fr)

## Performance Benchmarks

Expected performance metrics:

| Endpoint                         | Expected Time | Max Time |
| -------------------------------- | ------------- | -------- |
| `/generate_script`               | < 3s          | < 10s    |
| `/generate_audio`                | < 5s          | < 15s    |
| `/generate_lipsync`              | < 10s         | < 30s    |
| `/generate_ad_content`           | < 20s         | < 60s    |
| `/generate_ad_content_streaming` | < 15s         | < 45s    |

## Mobile App Integration Testing

### Test 1: Android App Connection

1. Start Android app
2. Point camera at product poster
3. Verify API calls are made to backend
4. Verify ad content is generated
5. Verify video plays in AR overlay

### Test 2: Network Error Handling

1. Disconnect from network
2. Try to generate ad content
3. Verify error message shown
4. Verify fallback behavior

### Test 3: API Timeout Handling

1. Set very short timeout
2. Try to generate ad content
3. Verify timeout handling
4. Verify retry mechanism

## Postman Collection

A Postman collection is available for testing:

- Import `TalkAR_Week6.postman_collection.json`
- Update environment variables
- Run collection

### Environment Variables

- `baseUrl`: `http://localhost:3000/api/v1`
- `productName`: Test product name

## Mock vs Real API Testing

### Mock Mode (Development)

- No API keys required
- Faster responses
- Deterministic outputs
- Good for development and testing

### Real API Mode (Production)

- Requires valid API keys
- Real AI-generated content
- Variable response times
- Better quality output

**To enable mock mode**:

- Don't set API keys in `.env`
- Set `NODE_ENV=development`

## Troubleshooting

### Common Issues

1. **Server not responding**

   - Check if server is running on port 3000
   - Check firewall settings
   - Verify CORS configuration

2. **API key errors**

   - Verify API keys in `.env` file
   - Check key permissions
   - Verify key hasn't expired

3. **Slow responses**

   - Check network connectivity
   - Verify API rate limits
   - Check server logs for errors

4. **Invalid responses**
   - Check request format
   - Verify required parameters
   - Check API documentation

## Success Criteria

All tests should pass:

- ✓ All endpoints return valid responses
- ✓ Error handling works correctly
- ✓ Performance benchmarks met
- ✓ Mobile app integration works
- ✓ Content quality acceptable

## Next Steps

After successful testing:

1. Deploy to staging environment
2. Test with real API keys
3. Monitor performance metrics
4. Gather user feedback
5. Deploy to production

## Test Automation

For CI/CD integration:

```bash
# Run all tests with coverage
npm test

# Run specific test suite
npm run test:unit
npm run test:integration
npm run test:performance
```

## Documentation

Additional documentation:

- API Reference: `docs/API.md`
- Architecture: `docs/ARCHITECTURE.md`
- Deployment: `docs/DEPLOYMENT.md`
