# Dependency Conflict Resolution

## Problem

When attempting to add Sceneform for 3D video rendering, we encountered a critical dependency conflict:

```
Duplicate class errors:
- Sceneform 1.17.1 uses Filament 1.17.1
- Sceneview 2.2.1 uses Filament 1.52.0
- Both include same Filament native libraries (.so files)
```

### Build Errors

```
2 files found with path 'lib/arm64-v8a/libfilament-jni.so' from inputs:
- filament-android-1.52.0/jni/arm64-v8a/libfilament-jni.so
- filament-android-1.17.1/jni/arm64-v8a/libfilament-jni.so
```

## Attempted Solutions

### 1. Exclude Filament from Sceneform ❌

```gradle
implementation('com.google.ar.sceneform:core:1.17.1') {
    exclude group: 'com.google.android.filament'
}
```

**Result:** Failed - Sceneform has its own internal Filament package (`com.google.ar.sceneform:filament-android:1.17.1`) that cannot be excluded.

### 2. Use Only Sceneview ✅

**Decision:** Remove Sceneform entirely and use Sceneview's Filament directly.

**Rationale:**
- Sceneview 2.2.1 is newer and actively maintained
- Already integrated for AR image tracking
- Has Filament 1.52.0 (newer version)
- Can implement custom video rendering without Sceneform

## Solution Implemented

### VideoPlaneNode - Phase 1 (Audio Only)

Created `VideoPlaneNode.kt` that:
- ✅ Plays video audio via MediaPlayer
- ✅ Tracks AR images perfectly
- ✅ Handles lifecycle (play/pause/stop)
- ✅ Provides callbacks for completion/errors
- ✅ Loads videos on background thread
- ⏳ 3D visual rendering pending (Phase 2)

### Code Structure

```kotlin
class VideoPlaneNode(
    context: Context,
    engine: Engine,           // Sceneview's Filament engine
    anchorNode: AnchorNode,   // AR anchor from detected image
    imageWidth: Float,        // Physical dimensions
    imageHeight: Float
) {
    private var mediaPlayer: MediaPlayer?
    
    fun loadVideo(videoUri: Uri, autoPlay: Boolean)
    fun play()
    fun pause()
    fun stop()
    fun cleanup()
}
```

### Integration

```kotlin
// In TalkARView.kt
val videoNode = VideoPlaneNode(
    context = context,
    engine = engine,              // From ARSceneView
    anchorNode = anchorNode,      // From detected image
    imageWidth = augmentedImage.extentX,
    imageHeight = augmentedImage.extentZ
)

videoNode.loadVideo(videoUri, autoPlay = true)
```

## Next Steps: 3D Visual Rendering

### Option A: Custom Filament Renderer (Recommended)

Use Sceneview's Filament directly:

1. Create SurfaceTexture for MediaPlayer
2. Create Filament Stream and Texture
3. Build plane geometry with Sceneview
4. Apply video texture to material
5. Update texture every frame

**Pros:**
- No dependency conflicts
- Uses existing Sceneview infrastructure
- Full control over rendering

**Cons:**
- More complex implementation
- Need to understand Filament API
- Manual texture updates

### Option B: WebView Overlay (Quick Alternative)

Position WebView over detected image:

1. Calculate image screen coordinates
2. Create transparent WebView
3. Play video in WebView
4. Update position every frame

**Pros:**
- Very simple to implement
- Works immediately
- No 3D rendering complexity

**Cons:**
- Not true 3D (2D overlay)
- Doesn't track perfectly
- Less immersive

## Build Status

✅ **Current build:** SUCCESSFUL

```bash
./gradlew app:assembleDebug
BUILD SUCCESSFUL in 25s
```

## Dependencies (Final)

```gradle
// ARCore with Sceneview for 3D rendering
implementation 'com.google.ar:core:1.43.0'
implementation 'io.github.sceneview:arsceneview:2.2.1'

// Sceneform REMOVED due to conflict
// implementation 'com.google.ar.sceneform:core:1.17.1' ❌

// ExoPlayer for video playback
implementation 'androidx.media3:media3-exoplayer:1.2.1'
implementation 'androidx.media3:media3-ui:1.2.1'
```

## Testing

### Current Functionality

1. Point camera at Sunrich or Tony poster
2. Image detected at 60fps
3. Long-press to trigger video
4. Video audio plays from backend/local
5. Speech recognition after video completes

### What Works

- ✅ Image detection
- ✅ Video audio playback
- ✅ Backend integration
- ✅ Offline fallback
- ✅ Speech recognition

### What's Pending

- ⏳ 3D visual rendering of video
- ⏳ Video texture on AR plane

## Lessons Learned

1. **Check dependency compatibility early** - Sceneform and Sceneview are incompatible
2. **Native library conflicts are hard to resolve** - .so files cannot be easily excluded
3. **Incremental implementation works** - Audio first, visuals later
4. **Use what you have** - Sceneview's Filament is sufficient for custom rendering

## References

- [Sceneview Documentation](https://github.com/SceneView/sceneview-android)
- [Filament Documentation](https://google.github.io/filament/)
- [ARCore Augmented Images](https://developers.google.com/ar/develop/java/augmented-images)
- [3D_VIDEO_RENDERING_PLAN.md](./3D_VIDEO_RENDERING_PLAN.md) - Detailed implementation options

## Timeline

- **Feb 20, 2026:** Attempted Sceneform integration
- **Feb 21, 2026:** Discovered dependency conflict
- **Feb 21, 2026:** Resolved by removing Sceneform
- **Feb 21, 2026:** Implemented VideoPlaneNode with audio playback
- **Next:** Implement 3D visual rendering with custom Filament renderer
