# User Flow Analysis: Current Plan vs Actual Requirement

## ❌ MISMATCH IDENTIFIED

### Your Actual User Flow (Talking Photo with Lip-Sync)

**User scans poster → Backend matches image → Backend generates lip-sync video → Only lip movement overlays on poster**

```
1. User points camera at poster/photo
2. ARCore detects the poster image
3. App sends image to backend for matching
4. Backend identifies the person/character
5. Backend generates lip-sync video (talking head)
   - Uses AI to generate lip movements
   - Synced with audio/speech
6. App receives video URL from backend
7. App overlays ONLY the lip/mouth region on the poster
8. Result: Static poster appears to be talking (lips moving)
```

**Key Features:**
- ✅ Backend generates lip-sync videos dynamically
- ✅ Lip coordinates detection
- ✅ Only mouth/lip region is animated
- ✅ Rest of the poster remains static
- ✅ Creates "talking photo" effect

---

### Current Spec Plan (Full Video Overlay)

**User scans poster → Play pre-recorded video → Full video overlays on poster**

```
1. User points camera at poster
2. ARCore detects the poster image
3. App loads pre-recorded video from res/raw/
4. App overlays ENTIRE video on the poster
5. Video plays with full frame rendering
6. Result: Full video playing on top of poster
```

**Key Features:**
- ❌ Uses pre-recorded local videos (sunrich_1.mp4)
- ❌ No backend integration for video generation
- ❌ No lip-sync or talking head functionality
- ❌ Full video overlay (not just lips)
- ❌ No dynamic content generation

---

## Critical Differences

| Aspect | Your Requirement | Current Spec |
|--------|------------------|--------------|
| **Video Source** | Backend-generated lip-sync | Local pre-recorded files |
| **Backend Integration** | ✅ Required | ❌ Not included |
| **Lip Detection** | ✅ Required | ❌ Not included |
| **Overlay Type** | Only lip/mouth region | Full video frame |
| **Dynamic Content** | ✅ Generated per interaction | ❌ Static videos |
| **API Calls** | generateLipSyncVideo() | None |
| **Effect** | Talking photo | Video overlay |

---

## What's Missing from Current Spec

### 1. Backend Integration
Current spec doesn't include:
- API calls to backend for image matching
- Lip-sync video generation requests
- Video URL retrieval from backend
- Polling for video generation status

### 2. Lip Coordinate Detection
Current spec doesn't include:
- Face/lip region detection on poster
- Coordinate mapping for lip overlay
- Masking to show only lip region
- Blending lip video with static poster

### 3. Dynamic Video Loading
Current spec uses:
- Local files from `res/raw/`
- Should use: Backend-generated video URLs

### 4. Talking Head Effect
Current spec renders:
- Full video overlay
- Should render: Only lip region with proper masking

---

## Existing Backend API (Already Implemented)

From `ApiClient.kt` and backend services:

```kotlin
// Lip-sync endpoints
@POST("lipsync/generate")
suspend fun generateLipSyncVideo(@Body request: LipSyncRequest): Response<LipSyncResponse>

@GET("lipsync/status/{videoId}")
suspend fun getLipSyncStatus(@Path("videoId") videoId: String): Response<LipSyncResponse>

@POST("lipsync/talking-head")
suspend fun generateTalkingHeadVideo(@Body request: TalkingHeadRequest): Response<LipSyncResponse>

data class LipSyncRequest(
    val imageId: String,
    val text: String,
    val voiceId: String
)

data class LipSyncResponse(
    val success: Boolean,
    val videoUrl: String? = null,
    val videoId: String? = null,
    val status: String? = null
)
```

**Backend Services:**
- `EnhancedLipSyncService.ts` - Generates lip-sync videos
- `MockLipSyncService.ts` - Mock implementation
- Already integrated in `ConversationalARService.kt`

---

## What Needs to Change

### Phase 1 (Foundation) - ✅ Still Valid
The foundation we built is still useful:
- VideoDecoder - Can decode backend-generated videos
- TextureSurface - Can render video frames
- Data models - Can be extended for lip-sync

### Phase 2-4 Need Major Changes

#### NEW: Lip-Sync Integration Phase
**Before Phase 2, we need:**

1. **Backend Video Fetcher**
   - Call `generateLipSyncVideo()` API
   - Poll for video generation status
   - Download video URL when ready
   - Cache videos locally

2. **Lip Region Detector**
   - Detect face/lip coordinates on poster
   - Calculate lip bounding box
   - Map coordinates to screen space
   - Handle different poster sizes/orientations

3. **Masked Video Renderer**
   - Render only lip region from video
   - Blend with static poster background
   - Apply proper masking/clipping
   - Maintain lip-sync timing

4. **Talking Head Controller**
   - Orchestrate backend API calls
   - Manage video generation lifecycle
   - Handle loading states
   - Error handling for API failures

#### Modified Phase 2: AR Integration
- ARTrackingManager - Still needed for poster detection
- RenderCoordinator - Modified for lip region only
- VideoOverlayController - Modified for masked rendering

---

## Recommended Approach

### Option 1: Update Current Spec (Recommended)
1. Keep Phase 1 foundation (already done)
2. Add new "Lip-Sync Integration" phase before Phase 2
3. Modify Phase 2-4 for lip-only rendering
4. Update requirements.md and design.md

### Option 2: Create Separate Lip-Sync Spec
1. Keep current spec for full video overlay (different feature)
2. Create new spec: "ar-talking-photo-lipsync"
3. Implement lip-sync as separate feature
4. Both features can coexist

### Option 3: Hybrid Approach
1. Complete current spec as-is (full video overlay)
2. Then extend it with lip-sync capabilities
3. Support both modes: full video OR lip-only

---

## Questions for You

1. **Should we pivot the current spec to lip-sync?**
   - Update requirements/design for talking photo
   - Or keep current spec and create new one?

2. **What's the priority?**
   - Talking photo with lip-sync (your described flow)
   - Or full video overlay (current spec)

3. **Backend readiness?**
   - Is the lip-sync API fully functional?
   - Or still using mock data?

4. **Lip detection approach?**
   - Should we use ML Kit for face detection?
   - Or rely on fixed coordinates per poster?
   - Or backend provides lip coordinates?

---

## My Recommendation

**Pause current implementation and update the spec** because:

1. ✅ Phase 1 foundation is still useful
2. ❌ Phase 2-4 don't match your actual requirement
3. ✅ Backend API already exists for lip-sync
4. ❌ Current spec will build wrong feature

**Next Steps:**
1. Update requirements.md for talking photo use case
2. Update design.md with lip-sync architecture
3. Add backend integration components
4. Add lip region detection/masking
5. Then continue implementation

**What do you want to do?**
