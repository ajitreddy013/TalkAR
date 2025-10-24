# Environmental Realism API Documentation

## Overview

The Environmental Realism feature enhances the TalkAR application by adding immersive audio and visual effects that respond to real-world conditions. This includes ambient background audio, dynamic lighting based on environmental conditions, and realistic shadow rendering.

## Features

### 1. Ambient Background Audio

Plays subtle background audio that fades in and out based on avatar speech activity.

#### Key Components

- **AmbientAudioService**: Manages ambient audio playback with fade-in/fade-out effects
- **Audio Configuration**: Admin-configurable settings for audio type, volume, and fade duration

#### API Methods

##### AmbientAudioService

```kotlin
class AmbientAudioService(context: Context) {
    fun initialize(audioResourceId: Int): Boolean
    fun startAmbientAudio()
    fun fadeOut()
    fun fadeIn()
    fun pause()
    fun resume()
    fun stop()
    fun isPlaying(): Boolean
    fun getCurrentVolume(): Float
    fun setVolume(volume: Float)
}
```

##### EnhancedARService Integration

```kotlin
class EnhancedARService(context: Context) {
    fun setAvatarSpeaking(isSpeaking: Boolean)
    fun startAmbientAudio()
    fun stopAmbientAudio()
    fun pauseAmbientAudio()
    fun resumeAmbientAudio()
}
```

#### Configuration Options

- **Audio Type**: Soft music, nature sounds, urban ambience
- **Volume**: 0-100% ambient volume level
- **Fade Duration**: 500ms-5000ms fade transition time

### 2. Dynamic Lighting

Uses ARCore's light estimation to dynamically adjust avatar rendering based on environmental lighting.

#### Key Components

- **Light Estimation**: ARCore-based environmental lighting analysis
- **Lighting Quality**: Categorizes lighting conditions (Excellent, Good, Fair, Poor)

#### API Methods

##### EnhancedARService

```kotlin
class EnhancedARService(context: Context) {
    val lightEstimate: StateFlow<LightEstimate?>
    val lightingQuality: StateFlow<LightingQuality>

    enum class LightingQuality {
        EXCELLENT, GOOD, FAIR, POOR, UNKNOWN
    }
}
```

##### LightEstimate (ARCore)

```kotlin
class LightEstimate {
    fun getState(): State
    fun getPixelIntensity(): Float
    fun getTimestamp(): Long
    fun getEnvironmentalHdrAmbientSphericalHarmonics(): FloatArray
    fun getEnvironmentalHdrMainLightDirection(): FloatArray
    fun getEnvironmentalHdrMainLightIntensity(): FloatArray

    enum class State {
        NOT_VALID, VALID
    }
}
```

#### Lighting Quality Mapping

| Pixel Intensity | Lighting Quality |
| --------------- | ---------------- |
| > 0.7           | EXCELLENT        |
| > 0.4           | GOOD             |
| > 0.2           | FAIR             |
| ≤ 0.2           | POOR             |

### 3. Shadow Rendering

Renders realistic shadows below avatars to enhance visual grounding.

#### Key Components

- **ShadowPlane**: Custom view for rendering dynamic shadows
- **Shadow Intensity**: Adjustable based on lighting conditions

#### API Methods

##### ShadowPlane

```kotlin
class ShadowPlane(context: Context) : View(context) {
    fun updateShadowProperties(image: AugmentedImage, lightEstimate: LightEstimate?)
    fun setShadowAlpha(alpha: Float)
    fun setShadowColor(color: Int)
    fun setShadowRadius(radius: Float)
    fun reset()
}
```

#### Shadow Intensity Mapping

| Lighting Quality | Shadow Intensity |
| ---------------- | ---------------- |
| EXCELLENT        | 40%              |
| GOOD             | 30%              |
| FAIR             | 20%              |
| POOR             | 10%              |

## Integration Guide

### 1. Mobile App Integration

#### Initializing Ambient Audio

```kotlin
// In your AR service or view model
val ambientAudioService = AmbientAudioService(context)
ambientAudioService.initialize(R.raw.ambient_music)

// Start ambient audio when AR session begins
ambientAudioService.startAmbientAudio()

// Control audio based on avatar speech
fun onAvatarStartsSpeaking() {
    ambientAudioService.fadeOut()
}

fun onAvatarStopsSpeaking() {
    ambientAudioService.fadeIn()
}
```

#### Using Light Estimation

```kotlin
// In your frame processing loop
private fun processFrame(frame: Frame) {
    val lightEstimate = frame.lightEstimate

    // Update avatar materials based on lighting
    updateAvatarLighting(lightEstimate)

    // Update shadow rendering
    shadowPlane.updateShadowProperties(augmentedImage, lightEstimate)
}
```

#### Rendering Shadows

```kotlin
// In your AR overlay view
val shadowPlane = ShadowPlane(context)
shadowPlane.updateShadowProperties(augmentedImage, lightEstimate)

// Add to your view hierarchy
addView(shadowPlane)
```

### 2. Admin Dashboard Integration

#### Audio Configuration

```typescript
// Settings page with audio controls
const [ambientAudioEnabled, setAmbientAudioEnabled] = useState(true);
const [ambientAudioVolume, setAmbientAudioVolume] = useState(30);
const [ambientAudioType, setAmbientAudioType] = useState("soft_music");
const [audioFadeDuration, setAudioFadeDuration] = useState(2000);

// Save settings to backend
const saveSettings = () => {
  api.put("/api/v1/settings/environmental-realism", {
    ambientAudioEnabled,
    ambientAudioVolume,
    ambientAudioType,
    audioFadeDuration,
  });
};
```

## Performance Optimizations

### 1. Audio System

- **Fade Job Management**: Cancels previous fade jobs to prevent conflicts
- **Volume Update Throttling**: Updates volume at 50ms intervals
- **Direct Volume Setting**: Bypasses fade animation when setting volume directly

### 2. Lighting System

- **Update Throttling**: Limits light estimation updates to every 500ms
- **Efficient State Management**: Uses StateFlow for reactive updates

### 3. Shadow Rendering

- **Gradient Caching**: Caches radial gradients to avoid recreation
- **Update Throttling**: Limits shadow updates to every 100ms
- **Change Detection**: Only updates when significant changes occur

## Testing

### Light Estimation Testing

```kotlin
// Test different lighting scenarios
fun testLightEstimation() {
    // Bright daylight
    testScenario("Bright Daylight", 0.8f, State.VALID)

    // Indoor lighting
    testScenario("Indoor Lighting", 0.5f, State.VALID)

    // Dim lighting
    testScenario("Dim Lighting", 0.2f, State.VALID)

    // Poor lighting
    testScenario("Poor Lighting", 0.1f, State.VALID)
}
```

### Audio Fade Testing

```kotlin
// Test fade in/out functionality
fun testAudioFade() {
    ambientAudioService.startAmbientAudio() // Should fade in
    delay(2000)
    ambientAudioService.fadeOut() // Should fade out
    delay(2000)
    ambientAudioService.fadeIn() // Should fade back in
}
```

## Configuration Endpoints

### Backend API

```
GET  /api/v1/settings/environmental-realism
POST /api/v1/settings/environmental-realism
```

#### Request/Response Format

```json
{
  "ambientAudioEnabled": true,
  "ambientAudioVolume": 30,
  "ambientAudioType": "soft_music",
  "audioFadeDuration": 2000,
  "dynamicLightingEnabled": true,
  "shadowIntensity": 30
}
```

## Error Handling

### Common Issues

1. **Audio Resource Not Found**: Ensure audio files are properly placed in res/raw/
2. **Light Estimation Not Available**: Handle State.NOT_VALID gracefully
3. **Performance Issues**: Monitor frame rate and adjust update intervals

### Best Practices

1. **Resource Management**: Always stop and release audio resources when no longer needed
2. **Battery Optimization**: Use throttling to minimize CPU usage
3. **User Experience**: Provide visual feedback for lighting quality issues

## Future Enhancements

1. **Spatial Audio**: Implement 3D audio positioning based on avatar location
2. **Advanced Lighting**: Use HDR lighting estimation for more realistic avatar rendering
3. **Weather Effects**: Add dynamic weather-based audio and visual effects
4. **Personalization**: Allow users to customize environmental settings
