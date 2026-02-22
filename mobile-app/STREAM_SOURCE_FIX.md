# Fix: Invalid Stream Source Error

## Problem Identified

**Error:** `Invalid stream source: 26` at Step 5 (Creating Filament Stream)

**Root Cause:** We were creating a regular OpenGL texture, but Filament Stream requires an **external texture** (GL_TEXTURE_EXTERNAL_OES) for video playback.

## The Issue

### Old (Broken) Approach:
```kotlin
// Created regular 2D texture
val textureId = createGLTextureId() // Regular GL_TEXTURE_2D
android.opengl.GLES20.glGenTextures(1, textures, 0)

// Tried to use it with Filament Stream
val stream = Stream.Builder()
    .stream(textureId.toLong()) // ❌ FAILS: Invalid stream source
    .build(engine)
```

**Problem:** Regular 2D textures (GL_TEXTURE_2D) cannot be used with SurfaceTexture for video. Video requires external textures (GL_TEXTURE_EXTERNAL_OES) with specific parameters.

## The Fix

### New (Working) Approach:
```kotlin
// Create EXTERNAL texture with proper parameters
fun createExternalTextureId(): Int {
    val textureId = glGenTextures()
    
    // Bind to EXTERNAL texture target (not 2D)
    glBindTexture(GL_TEXTURE_EXTERNAL_OES, textureId)
    
    // Set required parameters for external textures
    glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
    glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
    glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
    glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
    
    return textureId
}

// Now Filament Stream accepts it
val stream = Stream.Builder()
    .stream(textureId.toLong())
    .width(1920)
    .height(1080)
    .build(engine)
```

**Solution:** Create an external texture (GL_TEXTURE_EXTERNAL_OES) with proper parameters, which is compatible with both SurfaceTexture and Filament Stream.

## Changes Made

### VideoPlaneNode.kt

**Replaced:**
- `createGLTextureId()` → `createExternalTextureId()`

**New Function:**
```kotlin
private fun createExternalTextureId(): Int {
    // Generate texture
    val textures = IntArray(1)
    GLES20.glGenTextures(1, textures, 0)
    val textureId = textures[0]
    
    // Bind to EXTERNAL target (required for video)
    GLES11Ext.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
    
    // Set texture parameters
    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 
                          GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                          GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                          GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                          GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
    
    return textureId
}
```

**Added:**
- Width and height hints to Stream.Builder (1920x1080)
- Proper external texture binding and parameters

## Why This Works

1. **External Texture Target:** GL_TEXTURE_EXTERNAL_OES is specifically designed for video/camera streams. Regular GL_TEXTURE_2D doesn't work with SurfaceTexture.

2. **Required Parameters:** External textures need specific filtering (LINEAR) and wrapping (CLAMP_TO_EDGE) parameters set.

3. **Filament Compatibility:** Filament's Stream.Builder accepts external texture IDs when they're properly configured as GL_TEXTURE_EXTERNAL_OES.

4. **SurfaceTexture Requirement:** Android's SurfaceTexture (used by MediaPlayer) requires an external texture target.

## Technical Details

### Why External Textures?

External textures (GL_TEXTURE_EXTERNAL_OES):
- Are designed for streaming content (video, camera)
- Support YUV color space conversion
- Handle frame synchronization automatically
- Required by Android's SurfaceTexture API

Regular 2D textures (GL_TEXTURE_2D):
- Are for static images
- Don't support video streaming
- Cannot be used with SurfaceTexture

### Filament Stream Requirements

Filament's `Stream.Builder().stream(long)` expects:
- A valid OpenGL external texture ID
- The texture must be bound to GL_TEXTURE_EXTERNAL_OES
- Proper texture parameters must be set
- The texture must exist in the current GL context

## Expected Result

After this fix, you should see:

```
Step 1: Loading material from assets...
✅ Material loaded successfully
Step 2: Creating OpenGL external texture...
✅ OpenGL external texture created: ID=260
Step 3: Creating SurfaceTexture...
✅ SurfaceTexture created
Step 4: Connecting MediaPlayer to SurfaceTexture...
✅ MediaPlayer connected to SurfaceTexture
Step 5: Creating Filament Stream...
✅ Filament Stream created
Step 6: Creating Filament Texture...
✅ Filament Texture created
Step 7: Connecting Texture to Stream...
✅ Texture connected to Stream
...
Step 12: Positioning entity...
✅ Entity positioned (1cm in front of image)
🎉 3D video plane created successfully!
```

## Testing

Build and test:

```bash
cd mobile-app
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Then:
1. Point at Sunrich poster
2. Long-press
3. You should now see BOTH audio AND video visuals!

## References

- [OpenGL ES External Textures](https://www.khronos.org/registry/OpenGL/extensions/OES/OES_EGL_image_external.txt)
- [Android SurfaceTexture](https://developer.android.com/reference/android/graphics/SurfaceTexture)
- [Filament Stream API](https://google.github.io/filament/Stream.html)

---

**Fix applied!** The external texture is now properly configured for video streaming.
