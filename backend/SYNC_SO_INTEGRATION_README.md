# Sync.so API Integration Documentation

This document describes the implementation of the Sync.so API integration for converting speech + image/avatar → talking head video.

## Overview

The Sync.so API integration enables the generation of lip-sync videos by combining:

- Audio files (speech)
- Images or avatars
- Optional emotion parameters

## Implementation Details

### Endpoint

```
POST /api/v1/ai-pipeline/generate_lipsync
```

### Request Format

The endpoint accepts requests in the exact format specified:

```json
{
  "audio_url": "https://example.com/audio.mp3",
  "avatar": "celebrity_face.png"
}
```

It also supports additional parameters for flexibility:

```json
{
  "audio_url": "https://example.com/audio.mp3",
  "avatar": "celebrity_face.png",
  "imageId": "optional-image-id",
  "emotion": "neutral|happy|surprised|serious"
}
```

### Response Format

The endpoint returns a response in the following format:

```json
{
  "success": true,
  "videoUrl": "https://generated-video-url.com/video.mp4",
  "duration": 15
}
```

For asynchronous operations, it may also include a job ID:

```json
{
  "success": true,
  "videoUrl": "https://generated-video-url.com/video.mp4",
  "duration": 15,
  "jobId": "abc-123-def-456"
}
```

### Job Tracking

For asynchronous operations, the job status can be checked using:

```
GET /api/v1/ai-pipeline/lipsync/status/:jobId
```

### Database Storage

Generated video URLs are automatically saved to the database in the Avatar model:

- `avatarVideoUrl` field stores the generated video URL
- Creates new Avatar records or updates existing ones
- Maintains image-avatar mappings for future reference

## Implementation Components

### 1. Service Layer (`aiPipelineService.ts`)

Key methods implemented:

- `generateLipSync()` - Main entry point for lip-sync generation
- `callSyncAPI()` - Calls the real Sync.so API in production
- `generateMockLipSync()` - Provides mock implementation for development
- `saveVideoUrlToDatabase()` - Persists generated video URLs
- `pollSyncJob()` - Polls for job completion status
- `getSyncJobStatus()` - Retrieves job status information

### 2. Route Layer (`aiPipeline.ts`)

- Implements the `/generate_lipsync` endpoint
- Handles parameter validation
- Supports both `audio_url` (Sync.so format) and [audioUrl](file:///Users/ajitreddy/Engineering/Projets/TalkAR/TalkAR%20-/backend/src/services/aiPipelineService.ts#L51-L51) (internal format)
- Returns appropriate error messages for missing parameters

### 3. Database Integration

- Uses the existing Avatar model to store video URLs
- Automatically creates or updates avatar records
- Maintains image-avatar mappings for efficient retrieval

## Features Implemented

1. ✅ **Endpoint Creation**: `/api/v1/ai-pipeline/generate_lipsync`
2. ✅ **Parameter Support**: Accepts `audio_url` and `avatar` as specified
3. ✅ **Job Tracking**: Supports asynchronous operations with job IDs
4. ✅ **Polling Mechanism**: Provides status checking for long-running operations
5. ✅ **Database Storage**: Automatically saves generated video URLs
6. ✅ **Error Handling**: Comprehensive error handling with meaningful messages
7. ✅ **Fallback Support**: Mock implementation for development environments
8. ✅ **Testing Suite**: Comprehensive tests verifying all functionality

## Testing

A comprehensive test suite is available in `test-sync-integration.js` that verifies:

- Correct parameter handling
- Response format compliance
- Error handling for missing parameters
- Job status checking
- Database storage verification

## Environment Configuration

To use the real Sync.so API instead of mock implementation:

1. Set `SYNC_API_KEY` in your environment variables
2. Optionally set `SYNC_API_URL` (defaults to https://api.sync.so/v2)

For development/testing, the system automatically uses mock implementations when API keys are not configured.

## Usage Examples

### Basic Request

```bash
curl -X POST http://localhost:3000/api/v1/ai-pipeline/generate_lipsync \
  -H "Content-Type: application/json" \
  -d '{
    "audio_url": "https://example.com/audio.mp3",
    "avatar": "celebrity_face.png"
  }'
```

### Request with Emotion

```bash
curl -X POST http://localhost:3000/api/v1/ai-pipeline/generate_lipsync \
  -H "Content-Type: application/json" \
  -d '{
    "audio_url": "https://example.com/audio.mp3",
    "avatar": "celebrity_face.png",
    "emotion": "happy"
  }'
```

### Check Job Status

```bash
curl -X GET http://localhost:3000/api/v1/ai-pipeline/lipsync/status/abc-123-def-456
```
