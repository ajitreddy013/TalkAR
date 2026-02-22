# 3D Video Rendering Status

## Current Implementation

### Phase 1: Geometry + Audio ✅ (COMPLETE)

**What Works:**
- ✅ Video audio playback
- ✅ 3D plane geometry created
- ✅ Plane positioned at AR anchor
- ✅ Proper dimensions (matches image size)
- ✅ Lifecycle management
- ✅ MediaPlayer integration
- ✅ SurfaceTexture created
- ✅ Filament Stream and Texture created

**What's Visible:**
- You will HEAR the video audio
- You will SEE a 3D plane geometry (if material is applied)
- You will NOT see the video texture yet

### Phase 2: Video Texture (IN PROGRESS)

**Challenge:**
Filament materials require pre-compiled binary format (.filamat files), not runtime string compilation.

**Current Status:**
- ✅ External texture created
- ✅ Stream configured
- ✅ SurfaceTexture updates
- ⏳ Material with external texture sampler (pending)

**Options to Complete:**

#### Option A: Use Sceneview's Material System (Recommended)
```kotlin
// Use Sceneview's built-in material loader
val material = MaterialLoader.loadMaterial(
    context = context,
    lifecycle = lifecycle,
    materialPath = "materials/video_material.filamat"
)
```

**Steps:**
1. Create material definition file (.mat)
2. Compile with matc tool to .filamat
3. Add to assets/materials/
4. Load with MaterialLoader
5. Apply to renderable

#### Option B: Use Default Material + Texture Override
```kotlin
// Use Sceneview's default unlit material
// Override base color texture with video texture
```

**Steps:**
1. Get default unlit material from Sceneview
2. Create material instance
3. Set video texture as baseColorMap
4. Apply to renderable

#### Option C: WebView Overlay (Quick Alternative)
- Position WebView over detected image
- Play video in WebView
- 2D overlay, not true 3D
- Works immediately

## Code Structure

### VideoPlaneNode.kt

```kotlin
class VideoPlaneNode(
    context: Context,
    engine: Engine,
    scene: Scene,
    anchorNode: AnchorNode,
    imageWidth: Float,
    imageHeight: Float
) {
    // ✅ MediaPlayer - audio playback
    private var mediaPlayer: MediaPlayer?
    
    // ✅ SurfaceTexture - video frames
    private var surfaceTexture: SurfaceTexture?
    
    // ✅ Filament Stream - external texture stream
    private var stream: Stream?
    
    // ✅ Filament Texture - video texture
    private var texture: Texture?
    
    // ⏳ Material - needs compiled .filamat
    private var material: Material?
    private var materialInstance: MaterialInstance?
    
    // ✅ Geometry - plane mesh
    private var vertexBuffer: VertexBuffer?
    private var indexBuffer: IndexBuffer?
    
    // ✅ Entity - renderable object
    private var entity: Int
}
```

### Flow

1. **loadVideo()** - Load video URI
2. **MediaPlayer.prepareAsync()** - Prepare video
3. **onPrepared()** - Video ready
4. **createVideoPlane()** - Create 3D plane
   - ✅ Create SurfaceTexture
   - ✅ Create Filament Stream
   - ✅ Create Filament Texture
   - ✅ Connect MediaPlayer to SurfaceTexture
   - ✅ Set up frame update callback
   - ⏳ Create material with external texture
   - ✅ Create plane geometry
   - ✅ Create renderable entity
   - ✅ Add to scene
   - ✅ Position at anchor

## Testing

### Current Behavior

```
1. Point at poster → Image detected ✅
2. Long press → Loading video ✅
3. Video loads → Audio plays ✅
4. Video loads → Geometry created ✅
5. Video loads → Texture NOT visible ⏳
6. Video ends → Speech recognition ✅
```

### Expected Logs

```
VideoPlaneNode: Loading video: content://...
VideoPlaneNode: Video prepared, duration: 5000ms
VideoPlaneNode: Creating 3D video plane: 0.8m x 1.2m
VideoPlaneNode: Video aspect ratio: 1.777
VideoPlaneNode: Plane geometry created: 4 vertices, 6 indices
VideoPlaneNode: ✅ Renderable created (material pending)
VideoPlaneNode: ⚠️ Video texture not yet applied - need compiled material binary
VideoPlaneNode: Video playback started
```

## Next Steps

### Immediate (Complete Phase 2)

1. **Create Material Definition**
   ```
   material {
       name: VideoMaterial,
       shadingModel: unlit,
       parameters: [{
           type: samplerExternal,
           name: videoTexture
       }]
   }
   ```

2. **Compile Material**
   ```bash
   matc -o video_material.filamat video_material.mat
   ```

3. **Add to Assets**
   ```
   app/src/main/assets/materials/video_material.filamat
   ```

4. **Load and Apply**
   ```kotlin
   val material = MaterialLoader.loadMaterial(...)
   val instance = material.createInstance()
   instance.setParameter("videoTexture", texture, sampler)
   ```

### Alternative (Quick Win)

Implement WebView overlay:
- Calculate image screen coordinates
- Position WebView over image
- Play video in WebView
- Update position every frame

**Pros:** Works immediately, no material compilation
**Cons:** Not true 3D, less immersive

## Resources

### Filament Material Compiler

- [matc Documentation](https://google.github.io/filament/matc.html)
- [Material Guide](https://google.github.io/filament/Materials.html)
- [External Textures](https://google.github.io/filament/Materials.html#materialdefinitions/materialproperties/externalsampler)

### Sceneview

- [MaterialLoader](https://github.com/SceneView/sceneview-android)
- [Node System](https://github.com/SceneView/sceneview-android/wiki/Nodes)

### Download matc

```bash
# macOS
curl -O https://github.com/google/filament/releases/download/v1.52.0/filament-v1.52.0-mac.tgz
tar -xzf filament-v1.52.0-mac.tgz
./filament/bin/matc --help
```

## Timeline

- **Feb 21, 2026:** Phase 1 complete (geometry + audio)
- **Feb 22, 2026:** Phase 2 in progress (video texture)
- **Next:** Complete material compilation or use WebView overlay

## Decision Point

**Option A:** Spend 2-4 hours compiling material for true 3D
**Option B:** Spend 1-2 hours implementing WebView overlay

**Recommendation:** Try Option A first (proper 3D), fall back to Option B if blocked.

---

**Status:** Audio + geometry working, texture pending material compilation
**Blocker:** Need compiled .filamat material file for external texture
**Workaround:** WebView overlay as alternative
