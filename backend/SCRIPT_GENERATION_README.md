# AI Text Generation Integration

## Overview

This document provides detailed information about the AI text generation integration implemented for the TalkAR application. The integration supports two AI providers:

1. OpenAI GPT-4o-mini
2. GroqCloud (LLaMA 3.2 Vision)

## Features Implemented

### 1. Dual AI Provider Support

- **OpenAI GPT-4o-mini**: Fast, reliable, with free tier support
- **GroqCloud (LLaMA 3.2 Vision)**: High-performance open-source alternative
- **Provider Selection**: Configurable via environment variables

### 2. Product Description Generation

- Generate engaging 2-line product descriptions for advertisements
- Support for various product categories
- Caching for frequently requested products

### 3. Museum Guide Script Generation

- Context-aware scripts for interactive museum guides
- Multi-language support (English, Spanish, French, etc.)
- Emotion-based content generation (neutral, happy, surprised, serious)

### 4. File Persistence

- Automatic saving of generated scripts to `/scripts/` directory
- Timestamped filenames for version control
- Organized file structure based on parameters

### 5. Comprehensive Error Handling

- Input validation for all parameters
- API-specific error messages
- Timeout and connectivity error handling
- Graceful fallback to mock implementations

## API Endpoints

### POST /api/v1/ai-pipeline/generate_product_script

Generate a product description script.

**Request:**

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

### POST /api/v1/ai-pipeline/generate_script

Generate a museum guide script.

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
  "script": "Welcome! I'm so excited to show you around our wonderful exhibition today!",
  "language": "en",
  "emotion": "happy"
}
```

## Environment Configuration

### Required Environment Variables

```env
# Choose AI provider (openai or groq)
AI_PROVIDER=openai

# API Keys (at least one required)
OPENAI_API_KEY=your-openai-api-key
GROQCLOUD_API_KEY=your-groqcloud-api-key
```

### Provider Selection

- **OpenAI**: Set `AI_PROVIDER=openai` (default)
- **GroqCloud**: Set `AI_PROVIDER=groq`

## Implementation Details

### 1. Script Generation Function

```javascript
async function generateScript(productName) {
  const prompt = `Describe the product "${productName}" in 2 engaging lines for an advertisement.`;
  // call OpenAI or GroqCloud API here
  return generatedScript;
}
```

### 2. File Saving

Generated scripts are automatically saved to `/scripts/` with the following naming convention:

```
script-{imageId}-{language}-{emotion}-{timestamp}.txt
```

### 3. Caching

- In-memory caching with 5-minute TTL
- Cache keys based on request parameters
- Automatic cache invalidation

### 4. Error Handling

- Input validation for all parameters
- Specific error messages for different failure modes
- Timeout handling (10 seconds default)
- Fallback to mock implementations

## Testing

### Automated Tests

The implementation includes comprehensive tests:

- Product script generation
- Museum guide script generation
- Multi-language support
- Error handling validation

### Manual Testing

Use the provided test scripts:

- `test-product-script.js`: Product description testing
- `test-script-generation.js`: Comprehensive script generation testing

## Performance Considerations

### Response Times

- **OpenAI GPT-4o-mini**: ~1-2 seconds
- **GroqCloud LLaMA 3.2**: ~0.5-1 second
- **Mock Services**: < 100ms

### Rate Limiting

- OpenAI: 60 RPM (free tier), 3000 RPM (paid)
- GroqCloud: 30 RPM (free tier), higher for paid

## Future Enhancements

### Short-term Improvements

1. **Advanced Prompt Engineering**: More sophisticated prompts for better results
2. **Content Customization**: Personalization based on user preferences
3. **Batch Processing**: Generate multiple scripts in parallel

### Long-term Features

1. **Real-time Content Generation**: WebSocket-based streaming responses
2. **Content Versioning**: Track and compare different script versions
3. **Analytics Dashboard**: Monitor usage and performance metrics

## Troubleshooting

### Common Issues

1. **API Key Errors**

   - Verify API keys in `.env` file
   - Check for extra spaces or characters
   - Ensure keys have proper permissions

2. **Provider Selection Issues**

   - Confirm `AI_PROVIDER` is set correctly
   - Verify the selected provider's API key is configured

3. **File Saving Problems**
   - Check directory permissions for `/scripts/`
   - Ensure sufficient disk space
   - Verify path length limitations

### Getting Help

For issues not covered in this guide:

1. Review server logs for detailed error messages
2. Check API provider documentation
3. Refer to project architecture documentation

## Example Usage

### Product Description Generation

```javascript
const script = await AIPipelineService.generateProductScript("iPhone");
console.log(script);
// "Experience the future in your hands with the revolutionary iPhone. Cutting-edge technology meets elegant design."
```

### Museum Guide Script Generation

```javascript
const script = await AIPipelineService.generateScript({
  imageId: "exhibit-123",
  language: "en",
  emotion: "happy",
});
console.log(script.text);
// "Welcome! I'm so excited to show you around our wonderful exhibition today!"
```
