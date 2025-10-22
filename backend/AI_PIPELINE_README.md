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

**Request Body:**

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
  "script": "Welcome! I'm so excited to show you around our wonderful exhibition today!",
  "language": "en",
  "emotion": "happy"
}
```

#### POST /api/v1/ai-pipeline/generate_product_script

Generate a product description script.

**Request Body:**

```json
{
  "productName": "iPhone"
}
```

**Response:**

```json
{
  "success": true,
  "script": "Experience the future in your hands with the revolutionary iPhone. Cutting-edge technology meets elegant design."
}
```

#### POST /api/v1/ai-pipeline/generate_audio

Convert text to audio.

**Request:**

```json
{
  "text": "Welcome to our product showcase!",
  "language": "en",
  "emotion": "happy"
}
```

**Response:**

```json
{
  "success": true,
  "audioUrl": "https://example.com/audio.mp3",
  "duration": 15
}
```

#### POST /api/v1/ai-pipeline/generate_lipsync

Generate lip-sync video from image and audio.

**Request:**

```json
{
  "audio_url": "https://example.com/audio.mp3",
  "avatar": "celebrity_face.png"
}
```

**Response:**

```json
{
  "success": true,
  "videoUrl": "https://example.com/video.mp4",
  "duration": 15
}
```

#### POST /api/v1/ai-pipeline/generate_ad_content

Generate complete ad content from a product name (script → audio → video).

**Request:**

```json
{
  "product": "Sunrich Water Bottle"
}
```

**Response:**

```json
{
  "success": true,
  "script": "Refresh your day with Sunrich Water!",
  "audio_url": "https://example.com/audio.mp3",
  "video_url": "https://example.com/video.mp4"
}
```

## Development vs Production

### Development Mode

- Uses mock services for all API calls
- No real API keys required
- Activated when `NODE_ENV=development`

### Production Mode

- Uses real API services (OpenAI, ElevenLabs, Sync.so)
- Requires valid API keys
- Activated when `NODE_ENV=production`
