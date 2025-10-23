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

### 3. Metadata-Driven Script Generation

- Enhanced script generation based on detailed product metadata
- Support for tone, category, features, and other product attributes
- JSON-based metadata storage with caching

### 4. User Personalization Layer

- User preferences stored in `/mobile-app/app/src/main/assets/user_preferences.json`
- Preferences automatically loaded on app start
- AI tailors output based on user language and tone preferences
- Seamless integration with existing metadata and tone systems

### 5. Tone & Emotion Support

- Dynamic tone selection: friendly, excited, professional, casual, enthusiastic, persuasive
- Admin dashboard dropdown for tone selection
- Prompt engineering dynamically adapts based on selected tone
- Backward compatibility with existing emotion-based system

### 6. Museum Guide Script Generation

- Context-aware scripts for interactive museum guides
- Multi-language support (English, Spanish, French, etc.)
- Emotion-based content generation (neutral, happy, surprised, serious)

### 7. File Persistence

- Automatic saving of generated scripts to `/scripts/` directory
- Timestamped filenames for version control
- Organized file structure based on parameters

### 8. Comprehensive Error Handling

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

Generate a museum guide script or metadata-driven product script.

**Request:**

```json
{
  "imageId": "image_123",
  "language": "en",
  "emotion": "happy",
  "userPreferences": {
    "language": "English",
    "preferred_tone": "casual"
  }
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

## Metadata Structure

Product metadata is stored in `/data/product-metadata.json` with the following structure:

```json
{
  "image_id": "poster_01",
  "product_name": "Sunrich Water Bottle",
  "category": "Beverage",
  "tone": "excited",
  "language": "English"
}
```

### Supported Metadata Fields

- `image_id`: Unique identifier for the poster/image
- `product_name`: Name of the product
- `category`: Product category (e.g., "Beverage", "Fashion")
- `brand`: Brand name
- `price`: Product price
- `currency`: Price currency (e.g., "USD")
- `features`: Array of product features
- `description`: Detailed product description
- `tone`: Desired tone for the advertisement (e.g., "excited", "enthusiastic")
- `language`: Language for the script
- `target_audience`: Array of target audience segments
- `keywords`: Array of relevant keywords

## User Preferences Structure

User preferences are stored in `/mobile-app/app/src/main/assets/user_preferences.json`:

```json
{
  "language": "English",
  "preferred_tone": "casual"
}
```

### Supported Preference Fields

- `language`: Preferred language for content generation
- `preferred_tone`: Preferred tone for content generation (friendly, excited, professional, casual, enthusiastic, persuasive)

## Tone Support

The system supports the following tones for script generation:

- **Friendly**: Warm, approachable, and welcoming
- **Excited**: Energetic, enthusiastic, and vibrant
- **Professional**: Formal, authoritative, and business-oriented
- **Casual**: Relaxed, informal, and conversational
- **Enthusiastic**: Passionate, eager, and optimistic
- **Persuasive**: Convincing, compelling, and influential

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

### 2. Metadata-Based Prompt Engineering

When product metadata is available, the system generates enhanced prompts:

```
Generate a 2-line voiceover for an advertisement about {product_name}.
Category: {category}
Brand: {brand}
Tone: {tone}
Language: {language}

Product Description: {description}
Key Features:
1. {feature_1}
2. {feature_2}
...

Price: {currency} {price}

Create an engaging, concise advertisement script that highlights the product's value proposition and appeals to the target audience.
The tone should be {tone} - {tone_description}.
```

### 3. User Preferences Integration

When user preferences are provided, they are used to enhance the prompt:

```
Generate a 2-line voiceover for an advertisement about {product_name}.
Category: {category}
Brand: {brand}
Tone: {user_preferred_tone}
Language: {user_preferred_language}

Product Description: {description}
Key Features:
1. {feature_1}
2. {feature_2}
...

Price: {currency} {price}

Create an engaging, concise advertisement script that highlights the product's value proposition and appeals to the target audience.
The tone should be {user_preferred_tone} - {tone_description}.
```

### 4. File Saving

Generated scripts are automatically saved to `/scripts/` with the following naming convention:

```
script-{imageId}-{language}-{emotion}-{timestamp}.txt
```

### 5. Caching

- In-memory caching with 5-minute TTL
- Cache keys based on request parameters
- Automatic cache invalidation
- Separate caching for product metadata and user preferences

## Testing

### Automated Tests

The implementation includes comprehensive tests:

- Product script generation
- Metadata-driven script generation
- Tone-based script generation
- User preferences-based script generation
- Museum guide script generation
- Multi-language support
- Error handling validation

### Manual Testing

Use the provided test scripts:

- `test-product-script.js`: Product description testing
- `test-metadata-script.js`: Metadata-driven script generation testing
- `test-tone-script.js`: Tone-based script generation testing
- `test-user-preferences-script.js`: User preferences-based script generation testing
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
4. **Database Integration**: Store metadata in database instead of JSON files
5. **Dynamic Preferences**: Allow users to change preferences in-app

### Long-term Features

1. **Real-time Content Generation**: WebSocket-based streaming responses
2. **Content Versioning**: Track and compare different script versions
3. **Analytics Dashboard**: Monitor usage and performance metrics
4. **A/B Testing**: Compare different script variations for effectiveness
5. **Contextual Personalization**: Adapt content based on user behavior and context

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

### Metadata-Driven Script Generation

```javascript
const script = await AIPipelineService.generateScript({
  imageId: "poster_01",
  language: "en",
  emotion: "excited",
});
console.log(script.text);
// "Wow! Get ready for the incredible Sunrich Water Bottle - you won't believe how awesome it is!"
```

### User Preferences-Based Script Generation

```javascript
const script = await AIPipelineService.generateScript({
  imageId: "poster_02",
  language: "en",
  emotion: "neutral",
  userPreferences: {
    language: "English",
    preferred_tone: "professional",
  },
});
console.log(script.text);
// "Introducing the premium Eco-Friendly Backpack, engineered for discerning professionals who demand excellence."
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
