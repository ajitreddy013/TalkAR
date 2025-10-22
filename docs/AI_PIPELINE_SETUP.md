# AI Pipeline Setup Guide

## Overview

This document explains how to set up and use the AI pipeline for generating talking head videos from images. The pipeline consists of three main steps:

1. Script Generation (using OpenAI)
2. Audio Generation (using ElevenLabs)
3. Lip-Sync Video Generation (using Sync.so)

## Prerequisites

Before setting up the AI pipeline, ensure you have:

1. OpenAI API key
2. ElevenLabs API key
3. Sync.so API key

## Environment Configuration

### 1. Update Environment Variables

Add the following to your `.env` file in the backend directory:

```env
# AI Pipeline API Configuration
OPENAI_API_KEY=your-openai-api-key
ELEVENLABS_API_KEY=your-elevenlabs-api-key
SYNC_API_KEY=your-sync-api-key
SYNC_API_URL=https://api.sync.so/v2
```

### 2. Install Dependencies

The required dependencies are already included in the project:

- `axios` for API calls
- `uuid` for generating unique IDs

## API Endpoints

### POST /api/v1/ai-pipeline/generate

Generate a complete talking head video through the full pipeline.

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
  "jobId": "job_123",
  "message": "AI pipeline started successfully"
}
```

### GET /api/v1/ai-pipeline/status/:jobId

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

### POST /api/v1/ai-pipeline/generate_script

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
  "script": "Welcome to our exhibition...",
  "language": "en",
  "emotion": "happy"
}
```

### POST /api/v1/ai-pipeline/generate_product_script

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

### POST /api/v1/ai-pipeline/generate_audio

Convert text to audio.

**Request Body:**

```json
{
  "text": "Welcome to our exhibition...",
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

### POST /api/v1/ai-pipeline/generate_lipsync

Generate lip-sync video from image and audio.

**Request Body:**

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

### POST /api/v1/ai-pipeline/generate_ad_content

Generate complete ad content from a product name (script → audio → video).

**Request Body:**

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

The AI pipeline automatically uses mock services in development mode and real APIs in production.

### Development Mode

- Set `NODE_ENV=development` in your `.env` file
- Mock services will be used for all API calls
- No real API keys required

### Production Mode

- Set `NODE_ENV=production` in your `.env` file
- Real API services will be used
- Valid API keys required for all services

## Error Handling

The pipeline includes comprehensive error handling:

- API call failures are caught and logged
- Jobs that fail will have their status set to "failed"
- Error messages are stored with the job for debugging
- Fallback to mock services if real APIs fail (in development)

## Testing

### Automated Tests

Run the test suite with:

```bash
cd backend
npm test
```

### Manual Testing

Use the provided test scripts:

```bash
cd backend
node test-ai-pipeline.js
node test-ad-content.js
```

## Monitoring

The pipeline logs all major operations:

- API calls made to external services
- Job status changes
- Errors and exceptions
- Processing times for each step

Check the server logs for monitoring information.

## Troubleshooting

### Common Issues

1. **API Keys Not Working**

   - Verify API keys are correctly set in `.env` file
   - Check that there are no extra spaces or characters
   - Ensure the keys have the necessary permissions

2. **Jobs Stuck in "processing" Status**

   - Check server logs for errors
   - Verify external API services are accessible
   - Restart the server if necessary

3. **Mock Services Always Used**
   - Ensure `NODE_ENV` is set to "production"
   - Verify API keys are present and valid

### Getting Help

For issues not covered in this guide, check:

1. Server logs for detailed error messages
2. API provider documentation for service-specific issues
3. Project documentation for architecture details
