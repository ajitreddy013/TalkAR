# Debug 3D Video Rendering

## New Build Installed

✅ Build successful with detailed logging
✅ Installed on device

## Testing Steps

### 1. Clear Logs

```bash
adb logcat -c
```

### 2. Start Log Monitoring

```bash
adb logcat | grep -E "(VideoPlaneNode|TalkARView|TalkARViewModel)"
```

### 3. Launch App

```bash
adb shell am start -n com.talkar.app/.MainActivity
```

### 4. Test Workflow

1. Point camera at Sunrich poster
2. Wait for "Image detected"
3. Long-press on screen
4. Watch logs carefully

## Expected Logs

### When Video Loads

```
VideoPlaneNode: ========================================
VideoPlaneNode: 📹 Loading video: android.resource://...
VideoPlaneNode: ========================================
VideoPlaneNode: Preparing video asynchronously...
VideoPlaneNode: MediaPlayer instance stored
```

### When Video is Prepared

```
VideoPlaneNode: ========================================
VideoPlaneNode: ✅ Video prepared successfully!
VideoPlaneNode:    Duration: 5000ms (5s)
VideoPlaneNode:    Resolution: 1920x1080
VideoPlaneNode:    Aspect ratio: 1.777
VideoPlaneNode: ========================================
```

### When 3D Plane is Created

```
VideoPlaneNode: ========================================
VideoPlaneNode: Creating 3D video plane: 0.8m x 1.2m
VideoPlaneNode: Video aspect ratio: 1.777
VideoPlaneNode: ========================================
VideoPlaneNode: Step 1: Loading material from assets...
VideoPlaneNode: ✅ Material loaded successfully
VideoPlaneNode: Step 2: Creating OpenGL texture...
VideoPlaneNode: ✅ OpenGL texture created: ID=123
VideoPlaneNode: Step 3: Creating SurfaceTexture...
VideoPlaneNode: ✅ SurfaceTexture created
VideoPlaneNode: Step 4: Connecting MediaPlayer to SurfaceTexture...
VideoPlaneNode: ✅ MediaPlayer connected to SurfaceTexture
VideoPlaneNode: Step 5: Creating Filament Stream...
VideoPlaneNode: ✅ Filament Stream created
VideoPlaneNode: Step 6: Creating Filament Texture...
VideoPlaneNode: ✅ Filament Texture created
VideoPlaneNode: Step 7: Connecting Texture to Stream...
VideoPlaneNode: ✅ Texture connected to Stream
VideoPlaneNode: Step 8: Setting up frame update callback...
VideoPlaneNode: ✅ Frame callback set
VideoPlaneNode: Step 9: Creating material instance...
VideoPlaneNode: ✅ Material instance created with video texture
VideoPlaneNode: Step 10: Creating plane geometry...
VideoPlaneNode: Plane geometry created: 4 vertices, 6 indices
VideoPlaneNode: ✅ Plane geometry created
VideoPlaneNode: Step 11: Creating renderable entity...
VideoPlaneNode: ✅ Renderable entity created: 12345
VideoPlaneNode: Step 12: Adding entity to scene...
VideoPlaneNode: ✅ Entity added to scene
VideoPlaneNode: Step 13: Positioning entity...
VideoPlaneNode: ✅ Entity positioned (1cm in front of image)
VideoPlaneNode: ========================================
VideoPlaneNode: 🎉 3D video plane created successfully!
VideoPlaneNode:    Material: Loaded
VideoPlaneNode:    Texture: External (ID=123)
VideoPlaneNode:    Geometry: 4 vertices, 6 indices
VideoPlaneNode:    Entity: 12345
VideoPlaneNode:    Size: 0.8m x 1.2m
VideoPlaneNode: ========================================
VideoPlaneNode: ▶️ Video playback started
```

## What to Check

### ✅ Success Indicators

- All steps show ✅ checkmarks
- No ❌ error messages
- Entity ID is non-zero
- Texture ID is non-zero
- "3D video plane created successfully!"

### ❌ Failure Indicators

Look for these errors:

1. **Material Loading Failed**
   ```
   VideoPlaneNode: ❌ Failed to load material
   ```
   **Fix:** Material file missing from assets

2. **OpenGL Texture Failed**
   ```
   VideoPlaneNode: ✅ OpenGL texture created: ID=0
   ```
   **Fix:** GL context issue

3. **Stream Creation Failed**
   ```
   VideoPlaneNode: ❌ Failed to create video plane: ...
   ```
   **Fix:** Filament API issue

4. **Transform Instance is 0**
   ```
   VideoPlaneNode: ⚠️ Transform instance is 0
   ```
   **Fix:** Entity not properly created

## Debugging Checklist

### If No Logs Appear

- [ ] App crashed? Check: `adb logcat | grep "AndroidRuntime"`
- [ ] Video not loading? Check: `adb logcat | grep "TalkARViewModel"`
- [ ] Image not detected? Check: `adb logcat | grep "TalkARView"`

### If Material Fails to Load

```bash
# Check if material is in APK
unzip -l app/build/outputs/apk/debug/app-debug.apk | grep video_material

# Should show:
# assets/materials/video_material.filamat
```

### If Texture ID is 0

OpenGL context issue - the GL texture creation failed. This means:
- GL context not available
- Called from wrong thread
- GL not initialized

### If Entity Not Added to Scene

Check if scene reference is valid:
- Scene passed to VideoPlaneNode
- Scene is from ARSceneView
- Scene is initialized

## Key Fix in This Build

**Changed:** Stream creation now uses the same texture ID as SurfaceTexture

**Before:**
```kotlin
val videoStream = Stream.Builder().build(engine)  // Wrong!
val textureId = createGLTextureId()
val surfaceTexture = SurfaceTexture(textureId)
```

**After:**
```kotlin
val textureId = createGLTextureId()
val surfaceTexture = SurfaceTexture(textureId)
val videoStream = Stream.Builder()
    .stream(textureId.toLong())  // Same texture!
    .build(engine)
```

This ensures MediaPlayer writes to the same texture that Filament reads from.

## Next Steps

1. **Run the test** - Follow steps above
2. **Share logs** - Copy the VideoPlaneNode logs
3. **Check visuals** - Do you see video on the poster?

If you see all ✅ checkmarks but still no video:
- The rendering pipeline is working
- Issue is likely with material shader or texture sampling
- We'll need to check the material definition

If you see ❌ errors:
- Share the specific error
- We'll fix that step

---

**Ready to test!** Clear logs, launch app, and share what you see.
