# TTS (Text-to-Speech) Integration

## Overview

This document provides detailed information about the TTS (Text-to-Speech) integration implemented for the TalkAR application. The integration supports two TTS providers:

1. **ElevenLabs** (Best quality)
2. **Google Cloud TTS** (Free-tier fallback)

## Features Implemented

### 1. Dual TTS Provider Support

- **ElevenLabs**: Premium quality voices with advanced features
- **Google Cloud TTS**: Reliable Google-powered TTS with free tier
- **Provider Selection**: Configurable via environment variables

### 2. Multi-Language Support

- English (en-US)
- Spanish (es-ES)
- French (fr-FR)
- Extensible to additional languages

### 3. Emotion-Based Voice Modulation

- **Neutral**: Standard speaking style
- **Happy**: Higher pitch and faster speaking rate
- **Serious**: Lower pitch and slower speaking rate
- **Surprised**: Higher pitch and faster speaking rate with emphasis

### 4. Audio File Persistence

- Automatic saving of generated audio to `/audio/` directory
- Timestamped filenames for version control
- Organized file structure based on parameters

### 5. Comprehensive Error Handling

- Input validation for all parameters
- API-specific error messages
- Timeout and connectivity error handling
- Graceful fallback to mock implementations

## API Endpoints

### POST /api/v1/ai-pipeline/generate_audio

Generate an audio file from text.

**Request:**

```json
{
  "text": "Welcome to our amazing product showcase.",
  "language": "en",
  "emotion": "happy"
}
```

**Response:**

```json
{
  "success": true,
  "audioUrl": "http://localhost:3000/audio/audio-abc123-en-happy-2023-01-01T12-00-00-000Z.mp3",
  "duration": 5.5
}
```

## Environment Configuration

### Required Environment Variables

```env
# Choose TTS provider (elevenlabs or google)
TTS_PROVIDER=elevenlabs

# API Keys (at least one required)
ELEVENLABS_API_KEY=your-elevenlabs-api-key
GOOGLE_CLOUD_TTS_API_KEY=your-google-cloud-tts-api-key
```

### Provider Selection

- **ElevenLabs**: Set `TTS_PROVIDER=elevenlabs` (default)
- **Google Cloud TTS**: Set `TTS_PROVIDER=google`

## Implementation Details

### 1. Audio Generation Process

The TTS integration follows these steps:

1. Validate input parameters
2. Check cache for existing audio
3. Select TTS provider based on configuration
4. Generate audio using selected provider
5. Save audio file to `/audio/` directory
6. Return audio URL and duration

### 2. File Saving

Generated audio files are automatically saved to `/audio/` with the following naming convention:

```
{provider}-audio-{textHash}-{language}-{emotion}-{timestamp}.mp3
```

### 3. Caching

- In-memory caching with 5-minute TTL
- Cache keys based on text, language, and emotion
- Automatic cache invalidation

### 4. Error Handling

- Input validation for text length (max 5000 characters)
- Language code validation (2-character codes)
- Emotion validation (neutral, happy, surprised, serious)
- Specific error messages for different failure modes
- Timeout handling (30 seconds default)
- Fallback to mock implementations

## Testing

### Automated Tests

The implementation includes comprehensive tests:

- Basic audio generation
- Multi-language support
- Emotion-based voice modulation
- Error handling validation
- Caching mechanism verification

### Manual Testing

Use the provided test scripts:

- `test-tts.js`: Basic TTS testing
- `test-tts-comprehensive.js`: Comprehensive TTS testing

## Performance Considerations

### Response Times

- **ElevenLabs**: ~2-5 seconds
- **Google Cloud TTS**: ~1-3 seconds
- **Mock Services**: < 100ms

### Rate Limiting

- ElevenLabs: 10,000 characters/month (free tier)
- Google Cloud TTS: 4 million characters/month (free tier)

## Future Enhancements

### Short-term Improvements

1. **Advanced Voice Customization**: More granular voice settings
2. **SSML Support**: Enhanced audio with Speech Synthesis Markup Language
3. **Batch Processing**: Generate multiple audio files in parallel

### Long-term Features

1. **Real-time Streaming**: WebSocket-based audio streaming
2. **Voice Cloning**: Custom voice models for brand consistency
3. **Audio Analytics**: Monitor usage and performance metrics

## Troubleshooting

### Common Issues

1. **API Key Errors**

   - Verify API keys in `.env` file
   - Check for extra spaces or characters
   - Ensure keys have proper permissions

2. **Provider Selection Issues**

   - Confirm `TTS_PROVIDER` is set correctly
   - Verify the selected provider's API key is configured

3. **File Saving Problems**

   - Check directory permissions for `/audio/`
   - Ensure sufficient disk space
   - Verify path length limitations

4. **Audio Quality Issues**
   - Check voice settings in provider configurations
   - Verify text preprocessing
   - Test with different emotions

### Getting Help

For issues not covered in this guide:

1. Review server logs for detailed error messages
2. Check API provider documentation
3. Refer to project architecture documentation

## Example Usage

### Basic Audio Generation

```bash
curl -X POST http://localhost:3000/api/v1/ai-pipeline/generate_audio \
  -H "Content-Type: application/json" \
  -d '{
    "text": "Welcome to our product showcase!",
    "language": "en",
    "emotion": "happy"
  }'
```

### Testing Playback

You can test playback of generated audio files in your browser by navigating to:

```
http://localhost:3000/audio/mock-audio-5x8x7-en-neutral-2023-01-01T12-00-00-000Z.mp3
```

Or using Postman:

1. Send a POST request to generate audio
2. Copy the `audioUrl` from the response
3. Open the URL in a new tab or use Postman's built-in audio player

## File Structure

```
/audio/
  ├── mock-audio-5x8x7-en-neutral-2023-01-01T12-00-00-000Z.mp3
  ├── mock-audio-h5nrhu-en-happy-2023-01-01T12-00-00-000Z.mp3
  └── mock-audio-a1ylxv-en-serious-2023-01-01T12-00-00-000Z.mp3
```

The audio files are organized by:

- Provider (mock, elevenlabs, google)
- Text hash for uniqueness
- Language code
- Emotion
- Timestamp
