# AI Pipeline Implementation

## Overview

This document provides a comprehensive overview of the AI Pipeline implementation for the TalkAR application. The pipeline enables the generation of talking head videos from images through a three-step process:

1. Script Generation
2. Audio Generation
3. Lip-Sync Video Generation

## Architecture

The AI Pipeline follows a modular architecture with the following components:

```
AI Pipeline Service
├── Script Generation Module
├── Audio Generation Module
├── Lip-Sync Generation Module
├── Job Queue & Status Tracking
├── Caching Layer
└── Error Handling & Fallbacks
```

## Features Implemented

### 1. Complete AI Pipeline

- **Sequential Processing**: Image → Script → Audio → Lip-Sync Video
- **Asynchronous Processing**: Non-blocking operations with background job processing
- **Job Tracking**: Real-time status updates for each processing step

### 2. API Integration

- **OpenAI GPT**: Script generation with contextual awareness
- **ElevenLabs**: High-quality text-to-speech conversion
- **Sync.so**: Professional lip-sync video generation
- **Mock Services**: Development-friendly fallback implementations

### 3. Emotion Support

- **Emotion-Aware Content**: Scripts, voices, and animations adapt to emotional context
- **Supported Emotions**: neutral, happy, surprised, serious
- **Multi-Language**: Support for English, Spanish, and French

### 4. Caching System

- **In-Memory Cache**: Fast retrieval of frequently requested content
- **TTL Management**: Automatic cache expiration (5 minutes)
- **Cache Keys**: Unique identifiers based on request parameters

### 5. Error Handling

- **Retry Logic**: Exponential backoff for failed API calls
- **Fallback Mechanisms**: Automatic switching to mock services on failure
- **Comprehensive Logging**: Detailed error tracking and debugging information

### 6. API Endpoints

#### POST /api/v1/ai-pipeline/generate

Generate a complete talking head video through the full pipeline.

**Request:**

```json
{
  "imageId": "image_123",
  "language": "en",
  "emotion": "happy"
}
```

**Response:**

```json
{
  "success": true,
  "jobId": "job_123",
  "message": "AI pipeline started successfully"
}
```

#### GET /api/v1/ai-pipeline/status/:jobId

Get the status of a video generation job.

**Response:**

```json
{
  "success": true,
  "job": {
    "jobId": "job_123",
    "imageId": "image_123",
    "status": "completed",
    "script": "Welcome to our exhibition...",
    "audioUrl": "https://example.com/audio.mp3",
    "videoUrl": "https://example.com/video.mp4",
    "createdAt": "2023-01-01T00:00:00.000Z",
    "updatedAt": "2023-01-01T00:00:30.000Z"
  }
}
```

#### POST /api/v1/ai-pipeline/generate_script

Generate only the script/text content.

#### POST /api/v1/ai-pipeline/generate_audio

Convert text to audio.

#### POST /api/v1/ai-pipeline/generate_lipsync

Generate lip-sync video from image and audio.

## Development vs Production

### Development Mode

- Uses mock services for all API calls
- No real API keys required
- Activated when `NODE_ENV=development`

### Production Mode

- Uses real API services (OpenAI, ElevenLabs, Sync.so)
- Requires valid API keys
- Activated when `NODE_ENV=production`

## Environment Configuration

Add the following to your `.env` file:

```env
# AI Pipeline API Configuration
OPENAI_API_KEY=your-openai-api-key
ELEVENLABS_API_KEY=your-elevenlabs-api-key
SYNC_API_KEY=your-sync-api-key
SYNC_API_URL=https://api.sync.so/v2
```

## Testing

### Automated Tests

The implementation includes comprehensive tests for all components:

- Unit tests for service functions
- Integration tests for API endpoints
- Error handling validation

### Manual Testing

Use the provided test scripts:

- `test-ai-pipeline.js`: Complete pipeline testing
- `test-emotion-support.js`: Emotion functionality testing
- `test-error-handling.js`: Error handling validation
- `test-caching.js`: Caching mechanism verification

## Monitoring & Logging

The pipeline includes extensive logging for:

- API calls and responses
- Job status transitions
- Error conditions and exceptions
- Cache hits and misses
- Processing times for each step

## Future Enhancements

### Short-term Improvements

1. **Database Integration**: Replace in-memory storage with persistent database
2. **Redis Implementation**: Production-grade caching with Redis
3. **Advanced Emotion Mapping**: More sophisticated emotion-to-content mapping
4. **Voice Personalization**: User-specific voice preferences

### Long-term Features

1. **Real-time Processing**: WebSocket-based real-time status updates
2. **Batch Processing**: Multiple image processing in parallel
3. **Content Customization**: AI-powered personalization based on user history
4. **Multi-modal Input**: Support for image description and context analysis

## Troubleshooting

### Common Issues

1. **API Keys Not Working**

   - Verify keys in `.env` file
   - Check for extra spaces or characters
   - Ensure keys have proper permissions

2. **Jobs Stuck in "processing"**

   - Check server logs for errors
   - Verify external API connectivity
   - Restart server if necessary

3. **Caching Issues**
   - Clear cache by restarting server
   - Check cache TTL configuration
   - Verify cache key generation logic

### Getting Help

For issues not covered in this guide:

1. Review server logs for detailed error messages
2. Check API provider documentation
3. Refer to project architecture documentation
