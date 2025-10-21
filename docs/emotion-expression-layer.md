# Emotion & Expression Layer Documentation

## Overview

The Emotion & Expression Layer adds lifelike facial expressions to avatars in the TalkAR application, making them appear more alive and engaging. This feature maps emotions to facial animations including mouth movements, eyebrow positions, and head tilts.

## Supported Emotions

The system currently supports 4 preset emotions:

1. **Neutral** - Relaxed, default expression
2. **Happy** - Smiling with raised eyebrows
3. **Surprised** - Wide eyes and open mouth
4. **Serious** - Lowered eyebrows and straight mouth

## API Endpoints

### 1. Generate Sync Video with Emotion

**Endpoint**: `POST /api/v1/sync/generate`
**Description**: Generates a lip-sync video with emotional expressions
**Request Body**:

```json
{
  "text": "Hello, world!",
  "language": "en",
  "voiceId": "en-female-1",
  "emotion": "happy", // Optional, defaults to "neutral"
  "imageUrl": "https://example.com/image.jpg"
}
```

**Response**:

```json
{
  "jobId": "uuid",
  "status": "completed",
  "videoUrl": "https://example.com/video.mp4",
  "duration": 15
}
```

### 2. Get Talking Head Video with Emotion

**Endpoint**: `GET /api/v1/sync/talking-head/{imageId}?language=en&emotion=happy`
**Description**: Retrieves a pre-generated talking head video with emotional expressions
**Query Parameters**:

- `language` (required): Language code (e.g., "en", "es", "fr")
- `emotion` (optional): Emotion type (defaults to "neutral")

**Response**:

```json
{
  "imageId": "uuid",
  "videoUrl": "https://example.com/video.mp4",
  "duration": 15,
  "title": "Welcome to TalkAR (en)",
  "description": "This is a pre-saved talking head video",
  "language": "en",
  "emotion": "happy",
  "voiceId": "en-female-1",
  "createdAt": "2023-01-01T00:00:00Z"
}
```

## Database Schema

### Dialogue Table

The Dialogue model has been extended to include an emotion field:

| Column    | Type       | Description                                       |
| --------- | ---------- | ------------------------------------------------- |
| id        | UUID       | Unique identifier                                 |
| imageId   | UUID       | Reference to the image                            |
| text      | TEXT       | Dialogue text                                     |
| language  | STRING(5)  | Language code                                     |
| voiceId   | STRING     | Voice identifier                                  |
| emotion   | STRING(20) | Emotion type (neutral, happy, surprised, serious) |
| isDefault | BOOLEAN    | Whether this is the default dialogue              |
| isActive  | BOOLEAN    | Whether the dialogue is active                    |
| createdAt | DATETIME   | Creation timestamp                                |
| updatedAt | DATETIME   | Last update timestamp                             |

## Mobile App Integration

### 1. Data Models

The mobile app data models have been updated to include emotion support:

```kotlin
data class Dialogue(
    val id: String,
    val text: String,
    val language: String,
    val voiceId: String?,
    val emotion: String?,  // New field
    val isDefault: Boolean = false
)

data class TalkingHeadVideo(
    val imageId: String,
    val videoUrl: String,
    val duration: Int,
    val title: String,
    val description: String,
    val language: String,
    val emotion: String?,  // New field
    val voiceId: String,
    val createdAt: String
)
```

### 2. Emotional Avatar Component

A new `EmotionalAvatarView` component has been created to render avatars with emotional expressions:

```kotlin
@Composable
fun EmotionalAvatarView(
    isVisible: Boolean,
    avatar: Avatar?,
    image: BackendImage?,
    emotion: String = "neutral",
    isTalking: Boolean = false,
    modifier: Modifier = Modifier
)
```

## Admin Dashboard Integration

### 1. Emotion Selector Component

A new `EmotionSelector` component allows content creators to select emotions:

```tsx
<EmotionSelector
  value={emotion}
  onChange={setEmotion}
  label="Emotion"
  fullWidth
/>
```

### 2. Dialogue Editor

The dialogue editor has been enhanced to include emotion selection:

```tsx
<DialogueEditor
  open={open}
  dialogue={dialogue}
  onSave={handleSave}
  onClose={handleClose}
  imageId={imageId}
/>
```

## Implementation Details

### 1. Facial Animation Mapping

Emotions are mapped to specific facial animations:

| Emotion   | Mouth         | Eyebrows      | Eyes          |
| --------- | ------------- | ------------- | ------------- |
| Neutral   | Relaxed curve | Slight arch   | Normal        |
| Happy     | Upward curve  | Raised        | Slight upward |
| Surprised | Open circle   | Highly raised | Wide open     |
| Serious   | Straight line | Lowered       | Intense       |

### 2. Animation Blending

The system uses animation blending to smoothly transition between emotions and lip-sync movements:

- **Mouth Animation**: Controlled by `mouthProgress` parameter (0.0 to 1.0)
- **Blinking**: Random blinking animation with 2-5 second intervals
- **Emotion Transitions**: Immediate emotion changes with smooth facial feature adjustments

## Performance Optimizations

1. **Recomposition Optimization**: Uses `derivedStateOf` to minimize unnecessary UI updates
2. **Coroutine Management**: Properly cancels animation jobs when components are disposed
3. **Efficient Drawing**: Uses Canvas API for direct drawing operations without bitmap creation

## Testing

### Backend Testing

```bash
# Test talking head with emotion
curl -X GET "http://localhost:3000/api/v1/sync/talking-head/test-image-id?language=en&emotion=happy"

# Test sync video generation with emotion
curl -X POST "http://localhost:3000/api/v1/sync/generate" \
  -H "Content-Type: application/json" \
  -d '{"text":"Hello!","language":"en","emotion":"happy","voiceId":"en-female-1"}'
```

### Mobile App Testing

The EmotionalAvatarView component can be tested in isolation with different emotion parameters to verify facial expressions.

## Future Enhancements

1. **Additional Emotions**: Support for more complex emotions like sad, angry, confused
2. **Custom Emotion Mapping**: Allow content creators to define custom emotion-to-animation mappings
3. **Real-time Emotion Detection**: Integrate with AI services to detect user emotions and mirror them
4. **Advanced Animation**: Add head tilt, eye movement, and other subtle expressions
