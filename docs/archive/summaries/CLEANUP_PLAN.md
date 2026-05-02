# Branch Cleanup Plan

## Analysis of 198 Changes

### ✅ KEEP - Phase 1 New Implementation (20 files)
These are our new, properly designed components:

**Data Models & Errors:**
- `mobile-app/app/src/main/java/com/talkar/app/ar/video/models/*.kt` (10 files)
- `mobile-app/app/src/main/java/com/talkar/app/ar/video/errors/VideoError.kt`

**Surface Components:**
- `mobile-app/app/src/main/java/com/talkar/app/ar/video/surface/*.kt` (3 files)

**Decoder Components:**
- `mobile-app/app/src/main/java/com/talkar/app/ar/video/VideoDecoder.kt`
- `mobile-app/app/src/main/java/com/talkar/app/ar/video/ExoPlayerVideoDecoder.kt`

**Tests:**
- `mobile-app/app/src/test/java/com/talkar/app/ar/video/VideoDecoderTest.kt`

**Spec Files:**
- `.kiro/specs/ar-video-overlay-tracking/*` (4 files)

---

### ❌ REMOVE - Old/Conflicting AR Implementations (8 files)

These conflict with our new design and should be removed:

1. **`mobile-app/app/src/main/java/com/talkar/app/ar/ARVideoOverlay.kt`**
   - Old implementation using simplified positioning
   - Conflicts with our new VideoOverlayController design
   - Uses mutableStateOf instead of proper state management

2. **`mobile-app/app/src/main/java/com/talkar/app/ar/ExoPlayerVideoNode.kt`**
   - Old 3D node approach
   - Conflicts with our TextureView + ExoPlayer design
   - Incomplete implementation

3. **`mobile-app/app/src/main/java/com/talkar/app/ar/VideoPlaneNode.kt`**
   - Old Filament-based 3D rendering approach
   - We're using TextureView instead
   - Adds unnecessary complexity

4. **`mobile-app/app/src/main/java/com/talkar/app/ar/VideoAnchorNode.kt`**
   - Old anchor-based approach
   - Conflicts with our ARTrackingManager design

---

### 🔍 REVIEW - Keep if Useful (5 files)

These might be useful but need review:

1. **`mobile-app/app/src/main/java/com/talkar/app/ar/ARSessionConfig.kt`**
   - ARCore session configuration
   - Might be useful for Phase 2 (ARTrackingManager)
   - **Action**: Review and keep if it helps with ARCore setup

2. **`mobile-app/app/src/main/java/com/talkar/app/ar/AugmentedImageDatabase.kt`**
   - Image database management
   - Might be useful for Phase 2 (ARTrackingManager)
   - **Action**: Review and keep if it helps with image detection

3. **`mobile-app/app/src/main/java/com/talkar/app/ar/ARGestureDetector.kt`**
   - Gesture handling for AR
   - Not part of current spec but might be useful later
   - **Action**: Keep for now, not conflicting

4. **`mobile-app/app/src/main/java/com/talkar/app/ar/SpeechRecognitionService.kt`**
   - Speech recognition for conversational AR
   - Not part of video overlay spec
   - **Action**: Keep, different feature

5. **`mobile-app/app/src/main/java/com/talkar/app/ui/components/TalkARView.kt`**
   - Main AR view component
   - Will need updates for Phase 2 integration
   - **Action**: Keep and update in Phase 4

---

### 📄 KEEP - Documentation (27 files)

All documentation files should be kept for reference:
- `mobile-app/3D_RENDERING_STATUS.md`
- `mobile-app/EXOPLAYER_IMPLEMENTATION.md`
- `mobile-app/TESTING_GUIDE.md`
- etc. (24 more)

These document previous attempts and learnings.

---

### ✅ KEEP - Backend Changes (~50 files)

Backend changes are separate from mobile AR work:
- API route modifications
- Service updates
- Test file cleanup
- Configuration changes

**Action**: Keep all backend changes

---

### ✅ KEEP - Other Mobile Changes

- Modified `MainActivity.kt`, `ApiClient.kt`, etc.
- Service updates (ARImageRecognitionService, ConversationalARService)
- Build configuration changes
- Manifest updates

**Action**: Keep, these are infrastructure improvements

---

## Cleanup Actions

### Step 1: Remove Conflicting Files
```bash
git rm mobile-app/app/src/main/java/com/talkar/app/ar/ARVideoOverlay.kt
git rm mobile-app/app/src/main/java/com/talkar/app/ar/ExoPlayerVideoNode.kt
git rm mobile-app/app/src/main/java/com/talkar/app/ar/VideoPlaneNode.kt
git rm mobile-app/app/src/main/java/com/talkar/app/ar/VideoAnchorNode.kt
```

### Step 2: Review and Keep Useful Files
- Review ARSessionConfig.kt
- Review AugmentedImageDatabase.kt
- Keep ARGestureDetector.kt
- Keep SpeechRecognitionService.kt
- Keep TalkARView.kt (will update in Phase 4)

### Step 3: Commit Cleanup
```bash
git commit -m "Clean up old AR video implementations

Removed conflicting implementations:
- ARVideoOverlay (replaced by VideoOverlayController)
- ExoPlayerVideoNode (replaced by VideoDecoder)
- VideoPlaneNode (replaced by TextureSurface)
- VideoAnchorNode (replaced by ARTrackingManager)

Kept useful components for Phase 2 integration."
```

---

## Summary

**Total Changes**: 198 files
- **Keep**: ~185 files (Phase 1 work + backend + docs + infrastructure)
- **Remove**: 4 files (conflicting old implementations)
- **Review**: 5 files (potentially useful for Phase 2)
- **Update Later**: TalkARView.kt (Phase 4 integration)

After cleanup, the branch will have clean separation between:
1. New Phase 1 foundation (data models, TextureSurface, VideoDecoder)
2. Backend improvements
3. Documentation
4. Infrastructure updates
5. Components ready for Phase 2 integration
