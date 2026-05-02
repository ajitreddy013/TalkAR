# Complete User Flow Implementation Plan

## Current User Flow (Expected)

1. **User opens app** → TalkingPhotoScreen displays
2. **User points camera at girl photo** → ARCore detects poster
3. **Poster detected** → Backend generates lip-sync video
4. **Video ready** → Lip coordinates extracted
5. **Video plays** → Lip region overlaid on live camera feed
6. **User sees talking photo** → Girl's lips move with speech

## Current Implementation Status

### ✅ What's Working

1. **ARCore Session Management**
   - Session state checks prevent crashes
   - Lifecycle management (resume/pause)
   - GLSurfaceView configured with OpenGL ES 2.0

2. **Mock Data Mode**
   - `USE_MOCK_DATA = true` in PosterRepository
   - Generates colored test posters instantly
   - No backend dependency for testing

3. **Architecture Components**
   - TalkingPhotoController orchestrates flow
   - TalkingPhotoViewModel manages state
   - ArSceneViewComposable handles AR camera
   - All components properly connected

### ❌ What's Broken

#### Problem 1: Camera Feed Not Visible (BLACK SCREEN)
**Location**: `ArSceneViewComposable.kt`

**Root Cause**: 
- Camera texture ID is configured on ARCore session
- But no OpenGL shaders to actually render the texture to screen
- The `onDrawFrame()` method doesn't draw the camera background

**Impact**: User sees black/dark gray screen instead of live camera

**Fix Required**:
```kotlin
// Need to implement in onDrawFrame():
1. Bind camera texture from ARCore
2. Draw full-screen quad with texture
3. Use external OES texture sampler in fragment shader
```

---

#### Problem 2: Poster Detection Not Working
**Location**: `ArSceneViewComposable.kt` + `PosterRepository.kt`

**Root Cause**:
- `USE_MOCK_DATA = true` creates colored bitmaps (red, blue, green squares)
- These don't match the actual girl photo from backend
- ARCore can't detect the girl photo because it's not in the image database

**Impact**: When user points camera at girl photo, nothing happens

**Fix Required**:
```kotlin
// Option A: Use real backend data
USE_MOCK_DATA = false

// Option B: Add girl photo to assets
1. Download girl photo from backend
2. Add to assets/test_poster.jpg
3. Mock data will load it
```

---

#### Problem 3: Backend Integration Disabled
**Location**: `PosterRepository.kt` line 23

**Root Cause**:
```kotlin
private const val USE_MOCK_DATA = true  // ← This bypasses backend
```

**Impact**: 
- Backend API never called
- Girl photo never downloaded
- No real poster detection possible

**Fix Required**:
```kotlin
private const val USE_MOCK_DATA = false  // Use real backend
```

---

#### Problem 4: Backend Video Generation Failing
**Location**: `BackendVideoFetcherImpl.kt`

**Root Cause**: 
- Backend server not running at `10.0.2.2:443` (emulator localhost)
- Or backend URL misconfigured
- Or backend API endpoints changed

**Impact**: "Failed to generate lip sync video" error

**Fix Required**:
1. Verify backend server is running
2. Check backend URL in ApiConfig
3. Test backend endpoints with Postman
4. Add better error messages

---

#### Problem 5: Lip Region Overlay Not Rendering
**Location**: `TalkingPhotoScreen.kt` + `LipRegionOverlay.kt`

**Root Cause**:
- LipRegionOverlay component exists but may not be fully implemented
- Lip coordinates from backend may be in wrong format
- Transform matrix calculation may be incorrect

**Impact**: Even if video generates, lips don't overlay on camera

**Fix Required**:
1. Verify LipRegionOverlay implementation
2. Check lip coordinate format from backend
3. Implement proper transform calculation

---

## Detailed Implementation Plan

### Phase 1: Fix Camera Preview (CRITICAL)
**Priority**: HIGHEST  
**Time**: 2-3 hours

#### Step 1.1: Implement Camera Background Renderer
**File**: `ArSceneViewComposable.kt`

```kotlin
// Add to onDrawFrame() method:
override fun onDrawFrame(gl: GL10?) {
    // Clear screen
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT)
    
    try {
        // Check session state
        if (!lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            return
        }
        
        // Update ARCore frame
        val frame = session.update()
        val camera = frame.camera
        
        if (camera.trackingState == TrackingState.TRACKING) {
            // ✅ ADD THIS: Draw camera background
            drawCameraBackground(frame)
            
            // Process frame for poster detection
            trackingManager.processFrame(frame)
        }
    } catch (e: Exception) {
        // Error handling...
    }
}

// ✅ ADD THIS: New method to draw camera texture
private fun drawCameraBackground(frame: Frame) {
    // 1. Bind camera texture
    GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
    GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTextureId)
    
    // 2. Use shader program
    GLES20.glUseProgram(cameraShaderProgram)
    
    // 3. Set vertex positions (full-screen quad)
    val quadCoords = floatArrayOf(
        -1.0f, -1.0f,  // Bottom left
         1.0f, -1.0f,  // Bottom right
        -1.0f,  1.0f,  // Top left
         1.0f,  1.0f   // Top right
    )
    // ... set up vertex buffer and draw
    
    // 4. Transform texture coordinates
    val texCoords = FloatArray(8)
    frame.transformDisplayUvCoords(
        quadTexCoordsBuffer,
        texCoordsBuffer
    )
    
    // 5. Draw quad
    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
}
```

#### Step 1.2: Create OpenGL Shaders
**File**: `ArSceneViewComposable.kt`

```kotlin
// Vertex shader for camera background
private val cameraVertexShader = """
    attribute vec4 a_Position;
    attribute vec2 a_TexCoord;
    varying vec2 v_TexCoord;
    void main() {
        gl_Position = a_Position;
        v_TexCoord = a_TexCoord;
    }
""".trimIndent()

// Fragment shader for external OES texture (ARCore camera)
private val cameraFragmentShader = """
    #extension GL_OES_EGL_image_external : require
    precision mediump float;
    varying vec2 v_TexCoord;
    uniform samplerExternalOES u_Texture;
    void main() {
        gl_FragColor = texture2D(u_Texture, v_TexCoord);
    }
""".trimIndent()

// Compile and link shaders in onSurfaceCreated()
```

**Expected Result**: User sees live camera feed

---

### Phase 2: Enable Backend Integration
**Priority**: HIGH  
**Time**: 1 hour

#### Step 2.1: Disable Mock Data
**File**: `PosterRepository.kt` line 23

```kotlin
// Change from:
private const val USE_MOCK_DATA = true

// To:
private const val USE_MOCK_DATA = false
```

#### Step 2.2: Verify Backend Configuration
**File**: `ApiConfig.kt` or similar

```kotlin
// Verify backend URL is correct
// For emulator: http://10.0.2.2:PORT
// For physical device: http://YOUR_IP:PORT

// Check endpoints:
// POST /api/generate-talking-head
// GET /api/lip-sync-status/{videoId}
// GET /api/images (to get girl photo)
```

#### Step 2.3: Test Backend Connectivity
**File**: Create `BackendHealthCheck.kt`

```kotlin
suspend fun checkBackendHealth(): Result<Boolean> {
    return try {
        val response = apiService.healthCheck()
        Result.success(response.isSuccessful)
    } catch (e: Exception) {
        Log.e(TAG, "Backend unreachable", e)
        Result.failure(e)
    }
}
```

**Expected Result**: App connects to backend and downloads girl photo

---

### Phase 3: Fix Poster Detection
**Priority**: HIGH  
**Time**: 1-2 hours

#### Step 3.1: Verify Girl Photo in Backend
**Action**: Check backend has girl photo uploaded

```bash
# Test backend API
curl http://YOUR_BACKEND/api/images

# Should return:
{
  "images": [
    {
      "id": "girl_photo_1",
      "name": "Girl Photo",
      "imageUrl": "http://...",
      "hasHumanFace": true
    }
  ]
}
```

#### Step 3.2: Add Logging to Poster Loading
**File**: `PosterRepository.kt`

```kotlin
suspend fun loadPosters(): Result<List<ReferencePoster>> = withContext(Dispatchers.IO) {
    try {
        Log.d(TAG, "🔍 Loading posters from backend...")
        
        val posters = mutableListOf<ReferencePoster>()
        
        imageRepository.getAllImages().collect { images ->
            Log.d(TAG, "📥 Found ${images.size} images from backend")
            
            for (image in images) {
                Log.d(TAG, "  - Image: ${image.name} (${image.id})")
                Log.d(TAG, "    URL: ${image.imageUrl}")
                Log.d(TAG, "    Has face: ${image.hasHumanFace}")
                
                // Download and process...
            }
        }
        
        Log.d(TAG, "✅ Loaded ${posters.size} posters into ARCore")
        Result.success(posters)
    } catch (e: Exception) {
        Log.e(TAG, "❌ Failed to load posters", e)
        Result.failure(e)
    }
}
```

#### Step 3.3: Verify ARCore Image Database
**File**: `ArSceneViewComposable.kt`

```kotlin
private fun createImageDatabase(
    session: Session,
    posters: List<ReferencePoster>
): AugmentedImageDatabase {
    val database = AugmentedImageDatabase(session)
    
    Log.d(TAG, "📸 Creating ARCore image database with ${posters.size} posters")
    
    posters.forEach { poster ->
        try {
            val bitmap = BitmapFactory.decodeByteArray(
                poster.imageData,
                0,
                poster.imageData.size
            )
            
            if (bitmap != null) {
                database.addImage(
                    poster.name,
                    bitmap,
                    poster.physicalWidthMeters
                )
                Log.d(TAG, "  ✅ Added: ${poster.name} (${bitmap.width}x${bitmap.height})")
            } else {
                Log.w(TAG, "  ❌ Failed to decode: ${poster.name}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "  ❌ Error adding ${poster.name}", e)
        }
    }
    
    Log.d(TAG, "✅ Image database created with ${database.numImages} images")
    return database
}
```

**Expected Result**: ARCore detects girl photo when camera points at it

---

### Phase 4: Fix Video Generation
**Priority**: HIGH  
**Time**: 2-3 hours

#### Step 4.1: Add Detailed Backend Logging
**File**: `BackendVideoFetcherImpl.kt`

```kotlin
override suspend fun generateLipSync(request: TalkingPhotoRequest): Result<String> {
    Log.d(TAG, "🎬 Generating lip-sync video")
    Log.d(TAG, "  Poster ID: ${request.posterId}")
    Log.d(TAG, "  Text: ${request.text}")
    Log.d(TAG, "  Voice: ${request.voiceId}")
    
    return retryWithExponentialBackoff {
        Log.d(TAG, "  📡 Sending request to backend...")
        
        val response = apiService.generateTalkingHeadVideo(
            TalkingHeadRequest(
                imageId = request.posterId,
                text = request.text,
                voiceId = request.voiceId
            )
        )
        
        Log.d(TAG, "  📥 Response code: ${response.code()}")
        Log.d(TAG, "  📥 Response body: ${response.body()}")
        
        if (response.isSuccessful && response.body()?.success == true) {
            val videoId = response.body()?.videoUrl ?: throw Exception("No videoId")
            Log.d(TAG, "  ✅ Generation started: $videoId")
            videoId
        } else {
            val errorMsg = response.body()?.message ?: "Unknown error"
            Log.e(TAG, "  ❌ Generation failed: $errorMsg")
            throw Exception(errorMsg)
        }
    }
}
```

#### Step 4.2: Add Status Polling Logging
**File**: `BackendVideoFetcherImpl.kt`

```kotlin
override suspend fun checkStatus(videoId: String): Result<StatusResponse> {
    Log.d(TAG, "🔍 Checking status for: $videoId")
    
    return try {
        val response = apiService.getLipSyncStatus(videoId)
        
        Log.d(TAG, "  📥 Status response: ${response.body()}")
        
        if (response.isSuccessful && response.body()?.success == true) {
            val body = response.body()!!
            Log.d(TAG, "  Status: ${body.status}")
            Log.d(TAG, "  Progress: ${body.progress}")
            
            // Map response...
            Result.success(statusResponse)
        } else {
            Log.e(TAG, "  ❌ Status check failed")
            Result.failure(Exception("Status check failed"))
        }
    } catch (e: Exception) {
        Log.e(TAG, "  ❌ Status check error", e)
        Result.failure(e)
    }
}
```

#### Step 4.3: Handle Backend Errors Gracefully
**File**: `TalkingPhotoControllerImpl.kt`

```kotlin
private suspend fun fetchAndSetupVideo(posterId: String): Result<Unit> {
    return withContext(Dispatchers.IO) {
        try {
            setState(TalkingPhotoState.GENERATING)
            callbacks?.onGenerationStarted()
            
            Log.d(TAG, "🎬 Starting video generation for: $posterId")
            
            // Generate video
            val videoIdResult = backendFetcher.generateLipSync(request)
            
            if (videoIdResult.isFailure) {
                val error = videoIdResult.exceptionOrNull()
                Log.e(TAG, "❌ Generation failed", error)
                
                // Show user-friendly error
                val userError = when {
                    error?.message?.contains("timeout") == true ->
                        "Backend is taking too long. Please try again."
                    error?.message?.contains("connection") == true ->
                        "Cannot reach backend server. Check your connection."
                    else ->
                        "Video generation failed: ${error?.message}"
                }
                
                throw Exception(userError)
            }
            
            // Continue with polling...
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Video setup failed", e)
            withContext(Dispatchers.Main) {
                callbacks?.onError(TalkingPhotoError.GenerationFailed(e.message ?: "Unknown"))
                setState(TalkingPhotoState.ERROR)
            }
            Result.failure(e)
        }
    }
}
```

**Expected Result**: Video generates successfully or shows clear error

---

### Phase 5: Implement Lip Region Overlay
**Priority**: MEDIUM  
**Time**: 3-4 hours

#### Step 5.1: Verify LipRegionOverlay Component
**File**: Check if `LipRegionOverlay.kt` exists

```kotlin
@Composable
fun LipRegionOverlay(
    modifier: Modifier = Modifier,
    lipCoordinates: LipCoordinates?,
    transform: Matrix?
) {
    if (lipCoordinates == null || transform == null) {
        return
    }
    
    Canvas(modifier = modifier) {
        // Draw lip region with alpha blending
        val path = Path().apply {
            // Convert lip coordinates to screen space using transform
            val screenCoords = lipCoordinates.points.map { point ->
                val transformed = floatArrayOf(point.x, point.y)
                transform.mapPoints(transformed)
                Offset(transformed[0], transformed[1])
            }
            
            // Create path from points
            moveTo(screenCoords[0].x, screenCoords[0].y)
            screenCoords.drop(1).forEach { point ->
                lineTo(point.x, point.y)
            }
            close()
        }
        
        // Draw with alpha blending
        drawPath(
            path = path,
            color = Color.White,
            alpha = 0.8f
        )
    }
}
```

#### Step 5.2: Implement Transform Calculation
**File**: `TalkingPhotoViewModel.kt`

```kotlin
fun onTrackingUpdate(trackingData: TrackingData) {
    controller.updateTracking(trackingData)
    _isTracking.value = trackingData.isTracking
    
    // Calculate transform matrix from tracking data
    if (trackingData.isTracking) {
        val transform = calculateTransformMatrix(
            position = trackingData.position,
            rotation = trackingData.rotation,
            scale = trackingData.scale
        )
        _transform.value = transform
    }
    
    _state.value = controller.getState()
}

private fun calculateTransformMatrix(
    position: Vector3,
    rotation: Quaternion,
    scale: Vector2
): Matrix {
    val matrix = Matrix()
    
    // Apply translation
    matrix.postTranslate(position.x, position.y)
    
    // Apply rotation (convert quaternion to angle)
    val angle = 2 * Math.acos(rotation.w.toDouble()).toFloat()
    matrix.postRotate(Math.toDegrees(angle.toDouble()).toFloat())
    
    // Apply scale
    matrix.postScale(scale.x, scale.y)
    
    return matrix
}
```

**Expected Result**: Lip region overlays correctly on detected poster

---

## Testing Checklist

### Test 1: Camera Preview
- [ ] Open app
- [ ] See live camera feed (not black screen)
- [ ] Camera feed updates in real-time
- [ ] No crashes or freezes

### Test 2: Poster Detection
- [ ] Point camera at girl photo
- [ ] ARCore detects poster (check logs)
- [ ] "Generating video" message appears
- [ ] No "poster not found" errors

### Test 3: Video Generation
- [ ] Backend receives generation request
- [ ] Status polling works
- [ ] Video downloads successfully
- [ ] No timeout errors

### Test 4: Lip Sync Playback
- [ ] Video plays automatically when ready
- [ ] Lip region overlays on poster
- [ ] Lips move in sync with audio
- [ ] Alpha blending looks natural

### Test 5: Tracking
- [ ] Move camera away from poster
- [ ] Video pauses
- [ ] Point camera back at poster
- [ ] Video resumes

### Test 6: Error Handling
- [ ] Disconnect backend
- [ ] See clear error message
- [ ] Retry button works
- [ ] Refresh scan button works

---

## Quick Start Implementation Order

### Day 1: Get Camera Working
1. Implement camera background renderer (Phase 1)
2. Test camera preview shows live feed
3. Commit: "Implement camera background rendering"

### Day 2: Enable Backend
1. Set `USE_MOCK_DATA = false` (Phase 2)
2. Add backend health check
3. Test poster loading from backend
4. Commit: "Enable backend integration"

### Day 3: Fix Detection & Generation
1. Add detailed logging (Phase 3 & 4)
2. Test poster detection
3. Test video generation
4. Commit: "Fix poster detection and video generation"

### Day 4: Implement Overlay
1. Create LipRegionOverlay component (Phase 5)
2. Implement transform calculation
3. Test lip sync playback
4. Commit: "Implement lip region overlay"

---

## Files to Modify

### Critical Files
1. `ArSceneViewComposable.kt` - Camera rendering
2. `PosterRepository.kt` - Backend integration
3. `BackendVideoFetcherImpl.kt` - Error handling
4. `LipRegionOverlay.kt` - Lip overlay rendering
5. `TalkingPhotoViewModel.kt` - Transform calculation

### Configuration Files
1. `ApiConfig.kt` - Backend URL
2. `build.gradle` - Dependencies (if needed)

### New Files to Create
1. `BackendHealthCheck.kt` - Health check utility
2. `CameraBackgroundRenderer.kt` - Separate renderer class (optional)

---

## Success Criteria

✅ **User Flow Complete When**:
1. User opens app → sees live camera feed
2. User points at girl photo → ARCore detects it
3. Backend generates video → progress shown
4. Video ready → plays automatically
5. Lips overlay on photo → move in sync with speech
6. User moves camera → video pauses/resumes correctly

---

## Estimated Total Time
- Phase 1 (Camera): 2-3 hours
- Phase 2 (Backend): 1 hour
- Phase 3 (Detection): 1-2 hours
- Phase 4 (Generation): 2-3 hours
- Phase 5 (Overlay): 3-4 hours
- Testing & Debugging: 2-3 hours

**Total: 11-16 hours (2-3 days)**
