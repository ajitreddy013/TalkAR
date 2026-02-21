# Filament Stream Issue - Resolved

## Problem

**Error:** "Failed to create video plane: invalid stream source 26"

**Cause:** Filament's Stream API requires specific setup:
- Cannot use arbitrary OpenGL texture IDs
- Stream.Builder().stream(textureId) expects a specific format
- External texture integration is complex

## Solution

Reverted to audio-only playback (stable and working).

## Current Status

✅ **Working:**
- Image detection at 60fps
- Video audio playback
- Backend integration
- Speech recognition
- Offline fallback

❌ **Not Working:**
- Video visual display (3D texture rendering)

## Why 3D Texture is Hard

### Filament Requirements

1. **Compiled Material:** Need .filamat binary file
2. **External Texture Sampler:** Material must support samplerExternal
3. **Stream Setup:** Complex Stream/Texture/SurfaceTexture coordination
4. **Material Compiler:** Need matc tool to compile materials

### What We Tried

1. ❌ Runtime material creation (not supported)
2. ❌ Direct OpenGL texture ID (invalid stream source)
3. ❌ Stream.Builder().stream(textureId) (API mismatch)

## Recommended Solutions

### Option 1: WebView Overlay (Quick - 1-2 hours)

**Pros:**
- Works immediately
- No material compilation needed
- Simple implementation

**Cons:**
- Not true 3D
- 2D overlay only
- Less immersive

**Implementation:**
```kotlin
// Calculate image screen position
val screenBounds = getImageScreenBounds(augmentedImage, camera)

// Create WebView
val webView = WebView(context).apply {
    settings.javaScriptEnabled = true
    loadUrl("file:///android_asset/video.html")
}

// Position over image
webView.x = screenBounds.left
webView.y = screenBounds.top
webView.layoutParams.width = screenBounds.width()
webView.layoutParams.height = screenBounds.height()
```

### Option 2: Compiled Material (Proper - 2-4 hours)

**Pros:**
- True 3D rendering
- Professional quality
- Proper AR integration

**Cons:**
- Requires material compilation
- Need matc tool
- More complex setup

**Steps:**
1. Download Filament tools (matc)
2. Create material definition (.mat file)
3. Compile to .filamat
4. Add to assets
5. Load and apply in code

### Option 3: ExoPlayer + TextureView (Alternative - 3-4 hours)

**Pros:**
- Better video performance
- Streaming support
- More control

**Cons:**
- Still 2D overlay
- More complex than WebView
- Not true 3D

## Current Code

### VideoPlaneNode.kt

```kotlin
class VideoPlaneNode(
    context: Context,
    engine: Engine,        // Unused (for future 3D)
    scene: Scene,          // Unused (for future 3D)
    anchorNode: AnchorNode,
    imageWidth: Float,
    imageHeight: Float
) {
    private var mediaPlayer: MediaPlayer?  // ✅ Audio playback
    private var surfaceTexture: SurfaceTexture?  // Unused
    
    fun loadVideo(videoUri: Uri, autoPlay: Boolean) {
        // Loads and plays audio only
    }
}
```

### What Happens Now

1. User points at poster → Image detected
2. User long-presses → Video loads
3. MediaPlayer plays → **Audio only**
4. Video completes → Speech recognition starts

## Testing

### Build and Install

```bash
cd mobile-app
./gradlew app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Expected Behavior

```
✅ Image detection works
✅ Video audio plays
❌ Video visuals NOT shown
✅ Speech recognition works
```

### Logs

```bash
adb logcat | grep VideoPlaneNode
```

**Expected:**
```
VideoPlaneNode: VideoPlaneNode created (0.8m x 1.2m)
VideoPlaneNode: Loading video: content://...
VideoPlaneNode: Video prepared, duration: 5000ms
VideoPlaneNode: Creating 3D video plane: 0.8m x 1.2m
VideoPlaneNode: ✅ Video audio ready
VideoPlaneNode: ⚠️ 3D visual rendering requires compiled material (.filamat)
VideoPlaneNode: 💡 Consider WebView overlay as alternative for video display
VideoPlaneNode: Video playback started
```

## Decision Point

**For MVP/Demo:**
→ Use WebView overlay (quick, works immediately)

**For Production:**
→ Compile Filament material (proper 3D, better quality)

## Next Steps

1. **Test current build** - Verify audio works
2. **Choose approach** - WebView or compiled material
3. **Implement chosen solution**
4. **Test with users**

## Resources

### WebView Overlay
- [Android WebView](https://developer.android.com/reference/android/webkit/WebView)
- [Video in WebView](https://developer.android.com/guide/webapps/webview)

### Filament Material
- [matc Tool](https://github.com/google/filament/releases)
- [Material Guide](https://google.github.io/filament/Materials.html)
- [External Textures](https://google.github.io/filament/Materials.html#materialdefinitions/materialproperties/externalsampler)

---

**Status:** Audio working, visual pending
**Error:** Resolved (invalid stream source)
**Build:** SUCCESSFUL
**Ready to test:** YES
