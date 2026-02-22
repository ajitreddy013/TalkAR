# 3D Video Rendering Implementation Plan

## Current Status (Updated: Feb 21, 2026)

✅ **Working:**
- Image detection at 60fps
- Video audio playback via MediaPlayer
- Backend integration
- Offline fallback
- VideoPlaneNode with audio playback (no visual yet)

⏳ **Next:**
- 3D visual rendering of video on AR plane

❌ **Blocked:**
- Sceneform approach - dependency conflict with Sceneview (both use different Filament versions)

---

## Challenge

Rendering video textures in 3D AR space requires:
1. Creating a 3D plane mesh
2. Setting up SurfaceTexture for MediaPlayer output
3. Creating Filament material with external texture
4. Applying video texture to plane geometry
5. Updating texture every frame

This is complex with Sceneview 2.2.1 + Filament.

---

## Recommended Approaches

### Option A: Use ARCore Sceneform (Recommended)

**Why:** Sceneform has built-in video support

**Steps:**
1. Add Sceneform dependency (Google's AR SDK)
2. Use `VideoNode` class (built-in)
3. Attach to anchor
4. Much simpler than raw Filament

**Pros:**
- ✅ Built-in video support
- ✅ Handles texture updates automatically
- ✅ Well-documented
- ✅ Maintained by Google

**Cons:**
- ⚠️ Adds another dependency (~15MB)
- ⚠️ May conflict with Sceneview

**Code Example:**
```kotlin
// With Sceneform
val videoNode = VideoNode(context, videoUri)
videoNode.setParent(anchorNode)
videoNode.play()
```

---

### Option B: ExoPlayer + Custom Renderer

**Why:** ExoPlayer has better video handling than MediaPlayer

**Steps:**
1. Replace MediaPlayer with ExoPlayer
2. Create custom Filament material
3. Set up external texture
4. Update texture from ExoPlayer surface

**Pros:**
- ✅ Better video performance
- ✅ More control over playback
- ✅ Supports streaming

**Cons:**
- ⚠️ Complex Filament integration
- ⚠️ Need to handle texture updates manually
- ⚠️ More code to maintain

---

### Option C: WebView Overlay (Quick Solution)

**Why:** Simple and works immediately

**Steps:**
1. Create transparent WebView overlay
2. Position over detected image
3. Play video in WebView
4. Use CSS to match AR perspective

**Pros:**
- ✅ Very simple to implement
- ✅ Works immediately
- ✅ No 3D rendering complexity

**Cons:**
- ⚠️ Not true 3D (2D overlay)
- ⚠️ Doesn't track perfectly with image
- ⚠️ Less immersive

**Code Example:**
```kotlin
// Position WebView over image
val webView = WebView(context)
webView.loadUrl("file:///android_asset/video.html")
// Position based on image screen coordinates
```

---

### Option D: Unity AR Foundation (Alternative Platform)

**Why:** Unity has excellent AR video support

**Steps:**
1. Port app to Unity
2. Use AR Foundation
3. Use Video Player component
4. Much easier 3D video

**Pros:**
- ✅ Excellent AR support
- ✅ Built-in video rendering
- ✅ Cross-platform (iOS + Android)

**Cons:**
- ⚠️ Complete rewrite
- ⚠️ Different tech stack
- ⚠️ Larger app size

---

## Recommended Path Forward (Updated)

### Current Implementation: Audio Only ✅

**Status:** COMPLETE

VideoPlaneNode now plays video audio successfully:
- MediaPlayer integration working
- Background thread loading
- Proper lifecycle management
- Callbacks for completion/errors

### Next Phase: Add 3D Visual Rendering

**Recommended Approach:** Custom Filament Renderer (Option D)

Since Sceneform conflicts with Sceneview, we need to use Sceneview's Filament directly:

1. Create SurfaceTexture for MediaPlayer output
2. Create Filament Stream and Texture
3. Create plane geometry using Sceneview's geometry builders
4. Apply video texture to material
5. Update texture every frame

**Alternative:** WebView Overlay (Quick Win)

If 3D rendering proves too complex, fall back to WebView overlay:
- 2D video positioned over detected image
- Works immediately
- Not true 3D but functional for MVP

---

## Implementation: WebView Overlay (Quick Solution)

### 1. Create Video Overlay Component

```kotlin
// VideoOverlay.kt
@Composable
fun VideoOverlay(
    videoUri: Uri,
    imageScreenBounds: Rect,
    onCompleted: () -> Unit
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        // Auto-play video
                        view?.evaluateJavascript("document.querySelector('video').play()", null)
                    }
                }
                
                loadDataWithBaseURL(
                    null,
                    createVideoHtml(videoUri),
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        },
        modifier = Modifier
            .offset(
                x = imageScreenBounds.left.dp,
                y = imageScreenBounds.top.dp
            )
            .size(
                width = imageScreenBounds.width().dp,
                height = imageScreenBounds.height().dp
            )
    )
}

private fun createVideoHtml(videoUri: Uri): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <style>
                body { margin: 0; padding: 0; background: transparent; }
                video {
                    width: 100%;
                    height: 100%;
                    object-fit: cover;
                }
            </style>
        </head>
        <body>
            <video src="$videoUri" autoplay playsinline onended="window.Android.onCompleted()"></video>
        </body>
        </html>
    """.trimIndent()
}
```

### 2. Calculate Image Screen Position

```kotlin
// In TalkARView.kt
fun getImageScreenBounds(augmentedImage: AugmentedImage, camera: Camera): Rect {
    val centerPose = augmentedImage.centerPose
    val extentX = augmentedImage.extentX
    val extentZ = augmentedImage.extentZ
    
    // Project 3D corners to 2D screen
    val corners = arrayOf(
        floatArrayOf(-extentX/2, 0f, -extentZ/2),
        floatArrayOf(extentX/2, 0f, -extentZ/2),
        floatArrayOf(extentX/2, 0f, extentZ/2),
        floatArrayOf(-extentX/2, 0f, extentZ/2)
    )
    
    val screenPoints = corners.map { corner ->
        val worldPos = centerPose.transformPoint(corner)
        projectToScreen(worldPos, camera)
    }
    
    // Calculate bounding rect
    val minX = screenPoints.minOf { it.x }
    val maxX = screenPoints.maxOf { it.x }
    val minY = screenPoints.minOf { it.y }
    val maxY = screenPoints.maxOf { it.y }
    
    return Rect(minX.toInt(), minY.toInt(), maxX.toInt(), maxY.toInt())
}
```

---

## Implementation: Sceneform VideoNode (Proper 3D)

### 1. Add Sceneform Dependency

```gradle
// app/build.gradle
dependencies {
    implementation 'com.google.ar.sceneform:core:1.17.1'
    implementation 'com.google.ar.sceneform.ux:sceneform-ux:1.17.1'
}
```

### 2. Create Video Node

```kotlin
// VideoAnchorNode.kt
import com.google.ar.sceneform.rendering.ExternalTexture
import com.google.ar.sceneform.rendering.ModelRenderable

class VideoAnchorNode(
    private val context: Context,
    private val anchorNode: AnchorNode,
    private val imageWidth: Float,
    private val imageHeight: Float
) {
    private var mediaPlayer: MediaPlayer? = null
    private var externalTexture: ExternalTexture? = null
    private var videoRenderable: ModelRenderable? = null
    
    fun loadVideo(videoUri: Uri, autoPlay: Boolean = true) {
        // Create external texture for video
        externalTexture = ExternalTexture().also { texture ->
            // Create media player
            mediaPlayer = MediaPlayer().apply {
                setSurface(texture.surface)
                setDataSource(context, videoUri)
                isLooping = false
                
                setOnPreparedListener {
                    if (autoPlay) start()
                }
                
                prepareAsync()
            }
        }
        
        // Create plane renderable with video texture
        MaterialFactory.makeOpaqueWithTexture(context, externalTexture)
            .thenAccept { material ->
                videoRenderable = ShapeFactory.makeCube(
                    Vector3(imageWidth, 0.001f, imageHeight),
                    Vector3.zero(),
                    material
                )
                
                // Create node and attach
                val videoNode = Node()
                videoNode.renderable = videoRenderable
                videoNode.setParent(anchorNode)
            }
    }
    
    fun play() {
        mediaPlayer?.start()
    }
    
    fun pause() {
        mediaPlayer?.pause()
    }
    
    fun cleanup() {
        mediaPlayer?.release()
        mediaPlayer = null
        externalTexture = null
    }
}
```

---

## Implementation: ExoPlayer + Filament (Advanced)

### 1. Add ExoPlayer

```gradle
dependencies {
    implementation 'com.google.android.exoplayer:exoplayer:2.19.1'
}
```

### 2. Create Custom Material

```kotlin
// Create Filament material with external texture
val materialBuilder = Material.Builder()
    .platform(Material.Platform.MOBILE)
    .require(VertexAttribute.POSITION)
    .require(VertexAttribute.UV0)
    
val materialPackage = materialBuilder.build(engine)
val material = materialPackage.createInstance()

// Set up external texture sampler
val textureId = createExternalTexture()
material.setParameter("videoTexture", textureId, TextureSampler.SAMPLER_EXTERNAL)
```

### 3. Update Texture Every Frame

```kotlin
// In ARSceneView.onFrame
override fun onFrame(frameTime: FrameTime) {
    super.onFrame(frameTime)
    
    // Update video texture
    surfaceTexture?.updateTexImage()
    
    // Update material
    videoMaterial?.setExternalTexture("videoTexture", textureId)
}
```

---

## Comparison Matrix

| Feature | WebView | Sceneform | ExoPlayer+Filament |
|---------|---------|-----------|-------------------|
| Implementation Time | 2-4 hours | 1-2 days | 2-3 days |
| True 3D | ❌ | ✅ | ✅ |
| Perfect Tracking | ⚠️ | ✅ | ✅ |
| Performance | Good | Excellent | Excellent |
| Complexity | Low | Medium | High |
| Maintenance | Low | Medium | High |
| Streaming Support | ✅ | ⚠️ | ✅ |
| App Size Impact | +0MB | +15MB | +5MB |

---

## Recommendation

**For MVP/Demo:** Start with **WebView Overlay**
- Quick to implement
- Shows video visually
- Can demo immediately
- Iterate based on feedback

**For Production:** Upgrade to **Sceneform**
- True 3D rendering
- Better user experience
- Industry standard

**For Advanced:** Consider **ExoPlayer + Filament**
- Only if you need streaming
- Only if you need custom effects
- More maintenance overhead

---

## Next Steps

1. **Decide on approach** based on timeline and requirements
2. **Implement chosen solution**
3. **Test with real videos**
4. **Gather user feedback**
5. **Iterate and improve**

---

## Resources

### Sceneform
- [Sceneform Documentation](https://developers.google.com/ar/develop/java/sceneform)
- [Video Rendering Guide](https://developers.google.com/ar/develop/java/sceneform/video-rendering)

### ExoPlayer
- [ExoPlayer Documentation](https://exoplayer.dev/)
- [Surface Rendering](https://exoplayer.dev/surface-rendering.html)

### Filament
- [Filament Documentation](https://google.github.io/filament/)
- [External Textures](https://google.github.io/filament/Materials.html#materialdefinitions/materialproperties/externalsampler)

### ARCore
- [Augmented Images](https://developers.google.com/ar/develop/java/augmented-images)
- [Anchors and Trackables](https://developers.google.com/ar/develop/java/anchors)

---

## Questions to Answer

Before implementing, consider:

1. **Timeline:** How quickly do you need this?
2. **Quality:** 2D overlay acceptable or need true 3D?
3. **Streaming:** Will videos be streamed or local?
4. **App Size:** Is +15MB acceptable for Sceneform?
5. **Maintenance:** Who will maintain the 3D rendering code?

Based on answers, choose the right approach!
