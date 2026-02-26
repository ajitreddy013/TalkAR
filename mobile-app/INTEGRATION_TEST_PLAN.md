# Task 19: System Integration Test Plan

## Overview

This document outlines the comprehensive integration testing strategy for the Talking Photo feature. Task 19 validates that all components work together correctly in real-world scenarios.

---

## Test Environment Setup

### Prerequisites
- Android device with ARCore support (API 24+)
- Backend service running (ngrok, HF Spaces, or Render)
- Test posters with human faces
- Network connectivity
- Sufficient storage space (>500MB)

### Test Data
- **Test Poster 1**: Celebrity poster with clear human face
- **Test Poster 2**: Different poster for refresh scan testing
- **Test Text**: "Hello, welcome to TalkAR!"
- **Test Voice**: "en-US-Standard-A"

---

## 19.1 Test Complete Flow: Detection → Generation → Download → Cache → Playback

### Objective
Verify the entire end-to-end flow works correctly from poster detection through video playback.

### Test Steps

#### Step 1: Poster Detection
1. Launch app
2. Point camera at test poster with human face
3. **Expected**: Poster detected within 2 seconds
4. **Expected**: UI shows "Generating video..." message
5. **Verify**: ARTrackingManager creates anchor
6. **Verify**: TalkingPhotoController state = FETCHING_VIDEO

#### Step 2: Video Generation
1. **Expected**: Backend API called with correct parameters
2. **Expected**: UI shows progress indicator
3. **Expected**: Status polling begins (every 2 seconds)
4. **Verify**: TalkingPhotoController state = GENERATING
5. **Verify**: Progress updates displayed to user

#### Step 3: Video Download
1. **Expected**: When status = "complete", download begins
2. **Expected**: UI shows download progress (0-100%)
3. **Expected**: Video downloaded to cache directory
4. **Verify**: TalkingPhotoController state = DOWNLOADING
5. **Verify**: Progress callback updates UI

#### Step 4: Video Caching
1. **Expected**: Downloaded video stored in cache
2. **Expected**: Checksum calculated and validated
3. **Expected**: Metadata saved to SQLite database
4. **Expected**: Cache entry includes lip coordinates
5. **Verify**: VideoCache.isCached() returns true
6. **Verify**: Cache size updated correctly

#### Step 5: Video Playback
1. **Expected**: Video decoder initialized with cached file
2. **Expected**: Lip region renderer configured with coordinates
3. **Expected**: Video plays at 60fps
4. **Expected**: Lip region overlays on poster with alpha blending
5. **Expected**: Audio synchronized with video
6. **Verify**: TalkingPhotoController state = PLAYING
7. **Verify**: Poster remains static, only lips move

### Success Criteria
- ✅ Complete flow takes <30 seconds (excluding generation time)
- ✅ No errors or crashes
- ✅ Video cached successfully
- ✅ Playback smooth at 60fps
- ✅ Lip sync accurate
- ✅ All state transitions correct

### Test Script
```kotlin
@Test
fun testCompleteFlow_DetectionToPlayback() = runTest {
    // 1. Setup
    val testPoster = TestData.POSTER_WITH_HUMAN_FACE
    val testText = "Hello, welcome to TalkAR!"
    
    // 2. Detect poster
    val anchor = arTrackingManager.processFrame(testPoster)
    assertNotNull(anchor)
    
    // 3. Initialize controller
    controller.initialize(testPoster.id, anchor)
    assertEquals(TalkingPhotoState.FETCHING_VIDEO, controller.getState())
    
    // 4. Wait for generation and download
    advanceTimeBy(30_000) // 30 seconds
    
    // 5. Verify cache
    assertTrue(videoCache.isCached(testPoster.id))
    
    // 6. Verify playback
    assertEquals(TalkingPhotoState.PLAYING, controller.getState())
    
    // 7. Verify rendering
    assertTrue(lipRegionRenderer.isVisible())
    assertEquals(60, renderCoordinator.getCurrentFps())
}
```

---

## 19.2 Test Cache Hit Scenario: Detection → Cache Retrieval → Playback

### Objective
Verify that cached videos are retrieved instantly without backend calls.

### Test Steps

#### Step 1: First Detection (Cache Miss)
1. Launch app
2. Detect test poster
3. Wait for video generation and download
4. **Verify**: Video cached successfully
5. Stop playback

#### Step 2: Second Detection (Cache Hit)
1. Point camera away from poster
2. Point camera back at same poster
3. **Expected**: Video retrieved from cache instantly (<100ms)
4. **Expected**: No backend API calls made
5. **Expected**: Playback starts immediately
6. **Verify**: TalkingPhotoController state = READY → PLAYING
7. **Verify**: No GENERATING or DOWNLOADING states

#### Step 3: Offline Test
1. Disable network connectivity
2. Detect same poster again
3. **Expected**: Video plays from cache
4. **Expected**: No network errors
5. **Verify**: Offline playback works correctly

### Success Criteria
- ✅ Cache retrieval <100ms
- ✅ No backend calls on cache hit
- ✅ Offline playback works
- ✅ State transitions skip GENERATING/DOWNLOADING
- ✅ Playback quality identical to first time

### Test Script
```kotlin
@Test
fun testCacheHit_InstantPlayback() = runTest {
    val testPoster = TestData.POSTER_WITH_HUMAN_FACE
    
    // 1. First detection - cache miss
    controller.initialize(testPoster.id, anchor1)
    advanceTimeBy(30_000)
    assertTrue(videoCache.isCached(testPoster.id))
    controller.stop()
    
    // 2. Second detection - cache hit
    val startTime = System.currentTimeMillis()
    controller.initialize(testPoster.id, anchor2)
    val retrievalTime = System.currentTimeMillis() - startTime
    
    // 3. Verify instant retrieval
    assertTrue(retrievalTime < 100) // <100ms
    assertEquals(TalkingPhotoState.PLAYING, controller.getState())
    
    // 4. Verify no backend calls
    verify(backendVideoFetcher, never()).generateLipSync(any())
}
```

---

## 19.3 Test Tracking Loss and Recovery

### Objective
Verify that playback pauses when poster goes out of frame and resumes when it returns.

### Test Steps

#### Step 1: Normal Playback
1. Detect poster and start playback
2. **Verify**: Video playing at 60fps
3. **Verify**: TalkingPhotoController state = PLAYING

#### Step 2: Tracking Loss
1. Move camera away from poster (out of frame)
2. **Expected**: Tracking lost detected immediately
3. **Expected**: Video playback pauses
4. **Expected**: UI shows "Align poster properly" message
5. **Verify**: TalkingPhotoController state = PAUSED
6. **Verify**: Current playback position saved
7. **Verify**: onTrackingLost() callback triggered

#### Step 3: Tracking Recovery
1. Move camera back to poster (in frame)
2. **Expected**: Tracking resumed immediately
3. **Expected**: Video playback resumes from saved position
4. **Expected**: "Align poster properly" message hidden
5. **Verify**: TalkingPhotoController state = PLAYING
6. **Verify**: Playback continues from pause point (not restart)
7. **Verify**: onTrackingResumed() callback triggered

#### Step 4: Multiple Loss/Recovery Cycles
1. Repeat tracking loss and recovery 5 times
2. **Expected**: Each cycle works correctly
3. **Expected**: No memory leaks or performance degradation
4. **Verify**: Playback position maintained across cycles

### Success Criteria
- ✅ Tracking loss detected within 1 frame (16ms)
- ✅ Playback pauses immediately
- ✅ Resume from saved position (not restart)
- ✅ UI messages display correctly
- ✅ Multiple cycles work without issues

### Test Script
```kotlin
@Test
fun testTrackingLossAndRecovery() = runTest {
    // 1. Start playback
    controller.initialize(testPoster.id, anchor)
    advanceTimeBy(5_000) // Play for 5 seconds
    val pausePosition = controller.getCurrentPosition()
    
    // 2. Simulate tracking loss
    controller.updateTracking(null) // No anchor
    assertEquals(TalkingPhotoState.PAUSED, controller.getState())
    
    // 3. Simulate tracking recovery
    controller.updateTracking(anchor)
    assertEquals(TalkingPhotoState.PLAYING, controller.getState())
    
    // 4. Verify resume from position
    val resumePosition = controller.getCurrentPosition()
    assertEquals(pausePosition, resumePosition, delta = 100) // Within 100ms
}
```

---

## 19.4 Test "Refresh Scan" Functionality

### Objective
Verify that the "Refresh Scan" button clears current poster and allows scanning a new one.

### Test Steps

#### Step 1: Initial Poster Detection
1. Detect test poster 1
2. Start video playback
3. **Verify**: "Refresh Scan" button visible
4. **Verify**: TalkingPhotoController state = PLAYING

#### Step 2: Refresh Scan
1. Click "Refresh Scan" button
2. **Expected**: Current playback stops
3. **Expected**: Current anchor cleared
4. **Expected**: UI shows "Point camera at a poster..." message
5. **Verify**: TalkingPhotoController state = IDLE
6. **Verify**: ARTrackingManager.refreshScan() called
7. **Verify**: Resources released

#### Step 3: New Poster Detection
1. Point camera at test poster 2 (different poster)
2. **Expected**: New poster detected
3. **Expected**: New video generation starts
4. **Expected**: Previous poster ignored
5. **Verify**: New anchor created
6. **Verify**: New video cached separately

#### Step 4: Single Poster Mode
1. While poster 2 is tracked, show poster 1 in frame
2. **Expected**: Poster 1 ignored (single poster mode)
3. **Expected**: Poster 2 continues playing
4. **Verify**: Only one poster tracked at a time

### Success Criteria
- ✅ Refresh scan clears current poster
- ✅ New poster can be detected after refresh
- ✅ Resources properly released
- ✅ Single poster mode enforced
- ✅ UI updates correctly

### Test Script
```kotlin
@Test
fun testRefreshScan_NewPosterDetection() = runTest {
    // 1. Detect first poster
    controller.initialize(poster1.id, anchor1)
    advanceTimeBy(30_000)
    assertEquals(TalkingPhotoState.PLAYING, controller.getState())
    
    // 2. Refresh scan
    arTrackingManager.refreshScan()
    assertEquals(TalkingPhotoState.IDLE, controller.getState())
    assertNull(arTrackingManager.getCurrentAnchor())
    
    // 3. Detect second poster
    val anchor2 = arTrackingManager.processFrame(poster2)
    assertNotNull(anchor2)
    controller.initialize(poster2.id, anchor2)
    
    // 4. Verify new video
    advanceTimeBy(30_000)
    assertEquals(TalkingPhotoState.PLAYING, controller.getState())
    assertTrue(videoCache.isCached(poster2.id))
}
```

---

## 19.5 Test All Error Scenarios and User Messages

### Objective
Verify that all error scenarios are handled gracefully with appropriate user messages.

### Test Scenarios

#### Error 1: Poster Not Detected (Timeout)
- **Trigger**: Point camera at blank wall for 10 seconds
- **Expected Error**: TalkingPhotoError.PosterNotDetected
- **Expected Message**: "No poster detected. Try better lighting."
- **Expected Action**: "Ensure the poster is well-lit and flat"
- **Verify**: Error logged with code 2001

#### Error 2: No Human Face
- **Trigger**: Detect poster without human face (product/mascot)
- **Expected Error**: TalkingPhotoError.NoHumanFace
- **Expected Message**: "This poster doesn't contain a human face."
- **Expected Action**: "Please scan a poster with a human face"
- **Verify**: Error logged with code 2007

#### Error 3: Backend Unavailable
- **Trigger**: Disconnect network before generation
- **Expected Error**: TalkingPhotoError.BackendUnavailable
- **Expected Message**: "Service unavailable. Please try again later."
- **Expected Action**: "Check your internet connection"
- **Expected Retry**: 3 attempts with exponential backoff
- **Verify**: Error logged with code 2002

#### Error 4: Generation Failed
- **Trigger**: Backend returns error status
- **Expected Error**: TalkingPhotoError.GenerationFailed
- **Expected Message**: "Failed to generate the lip-sync video."
- **Expected Action**: "Please try again"
- **Verify**: Error logged with code 2003

#### Error 5: Download Failed
- **Trigger**: Interrupt network during download
- **Expected Error**: TalkingPhotoError.DownloadFailed
- **Expected Message**: "Failed to download the video."
- **Expected Action**: "Check your internet connection"
- **Expected Retry**: 3 attempts
- **Verify**: Error logged with code 2004

#### Error 6: Invalid Coordinates
- **Trigger**: Backend returns coordinates outside 0-1 range
- **Expected Error**: TalkingPhotoError.InvalidCoordinates
- **Expected Message**: "The video data received is invalid."
- **Expected Action**: "Try scanning the poster again"
- **Verify**: Error logged with code 2005

#### Error 7: Cache Corrupted
- **Trigger**: Corrupt cached video file
- **Expected Error**: TalkingPhotoError.CacheCorrupted
- **Expected Message**: "The cached video file is corrupted."
- **Expected Action**: "The video will be re-downloaded automatically"
- **Expected Recovery**: Auto-delete and re-download
- **Verify**: Error logged with code 2006

### Success Criteria
- ✅ All 7 error types handled correctly
- ✅ User-friendly messages displayed
- ✅ Actionable advice provided
- ✅ Retry logic works (3 attempts, exponential backoff)
- ✅ Automatic recovery for corrupted cache
- ✅ All errors logged with correct codes

### Test Script
```kotlin
@Test
fun testAllErrorScenarios() = runTest {
    // Test each error scenario
    val errorScenarios = listOf(
        ErrorScenario(
            trigger = { /* no poster */ },
            expectedError = TalkingPhotoError.PosterNotDetected::class,
            expectedCode = 2001
        ),
        // ... other scenarios
    )
    
    errorScenarios.forEach { scenario ->
        scenario.trigger()
        val error = controller.getLastError()
        assertIs<scenario.expectedError>(error)
        assertEquals(scenario.expectedCode, error.code)
        
        val message = ErrorMessageMapper.mapError(error)
        assertNotNull(message.title)
        assertNotNull(message.action)
    }
}
```

---

## 19.6 Verify State Transitions Work Correctly

### Objective
Verify that the TalkingPhotoController state machine transitions correctly.

### State Diagram
```
IDLE → FETCHING_VIDEO → GENERATING → DOWNLOADING → READY → PLAYING
                                                              ↓
                                                           PAUSED
                                                              ↓
                                                            ERROR
```

### Test Scenarios

#### Scenario 1: Happy Path
```
IDLE → FETCHING_VIDEO → GENERATING → DOWNLOADING → READY → PLAYING
```
- **Trigger**: Normal detection and playback
- **Verify**: Each state transition occurs in order
- **Verify**: No invalid transitions

#### Scenario 2: Cache Hit Path
```
IDLE → FETCHING_VIDEO → READY → PLAYING
```
- **Trigger**: Detect cached poster
- **Verify**: GENERATING and DOWNLOADING skipped
- **Verify**: Direct transition to READY

#### Scenario 3: Tracking Loss Path
```
PLAYING → PAUSED → PLAYING
```
- **Trigger**: Poster goes out of frame and returns
- **Verify**: Pause and resume work correctly

#### Scenario 4: Error Path
```
GENERATING → ERROR
DOWNLOADING → ERROR
```
- **Trigger**: Backend or download failure
- **Verify**: Error state reached
- **Verify**: Error details available

#### Scenario 5: Refresh Scan Path
```
PLAYING → IDLE
```
- **Trigger**: Click "Refresh Scan" button
- **Verify**: Return to IDLE state
- **Verify**: Resources released

### Invalid Transitions (Should Not Occur)
- ❌ IDLE → PLAYING (must go through FETCHING_VIDEO)
- ❌ GENERATING → PLAYING (must go through DOWNLOADING → READY)
- ❌ ERROR → PLAYING (must return to IDLE first)

### Success Criteria
- ✅ All valid transitions work
- ✅ Invalid transitions prevented
- ✅ State changes logged
- ✅ UI updates on state changes
- ✅ Callbacks triggered correctly

### Test Script
```kotlin
@Test
fun testStateTransitions_HappyPath() = runTest {
    val stateHistory = mutableListOf<TalkingPhotoState>()
    controller.setCallbacks(
        onStateChanged = { state -> stateHistory.add(state) }
    )
    
    // Execute happy path
    controller.initialize(testPoster.id, anchor)
    advanceTimeBy(30_000)
    
    // Verify state sequence
    assertEquals(
        listOf(
            TalkingPhotoState.IDLE,
            TalkingPhotoState.FETCHING_VIDEO,
            TalkingPhotoState.GENERATING,
            TalkingPhotoState.DOWNLOADING,
            TalkingPhotoState.READY,
            TalkingPhotoState.PLAYING
        ),
        stateHistory
    )
}
```

---

## 19.7 Test Resource Cleanup on App Background

### Objective
Verify that resources are properly released when app goes to background.

### Test Steps

#### Step 1: Active Playback
1. Start video playback
2. **Verify**: Video decoder active
3. **Verify**: Camera active
4. **Verify**: AR session active
5. **Verify**: Memory usage normal

#### Step 2: App Goes to Background
1. Press home button (app backgrounded)
2. **Expected**: Video decoder released
3. **Expected**: Camera released
4. **Expected**: AR session paused
5. **Expected**: Playback position saved
6. **Verify**: TalkingPhotoController.release() called
7. **Verify**: Memory released

#### Step 3: App Returns to Foreground
1. Return to app
2. **Expected**: Resources re-initialized
3. **Expected**: Playback can resume
4. **Expected**: No memory leaks
5. **Verify**: AR session resumed
6. **Verify**: Camera re-initialized

#### Step 4: Memory Leak Check
1. Repeat background/foreground cycle 10 times
2. **Expected**: Memory usage stable
3. **Expected**: No resource leaks
4. **Verify**: Heap size doesn't grow
5. **Verify**: No leaked activities or contexts

### Success Criteria
- ✅ Resources released on background
- ✅ Resources restored on foreground
- ✅ No memory leaks
- ✅ Playback position preserved
- ✅ Multiple cycles work correctly

### Test Script
```kotlin
@Test
fun testResourceCleanup_AppBackground() = runTest {
    // 1. Start playback
    controller.initialize(testPoster.id, anchor)
    advanceTimeBy(5_000)
    val position = controller.getCurrentPosition()
    
    // 2. Simulate background
    activityScenario.moveToState(Lifecycle.State.STOPPED)
    
    // 3. Verify resources released
    verify(videoDecoder).release()
    verify(arSession).pause()
    
    // 4. Simulate foreground
    activityScenario.moveToState(Lifecycle.State.RESUMED)
    
    // 5. Verify resources restored
    verify(arSession).resume()
    
    // 6. Verify position preserved
    assertEquals(position, controller.getCurrentPosition(), delta = 100)
}
```

---

## Test Execution Checklist

### Pre-Test Setup
- [ ] Backend service running and accessible
- [ ] Test device with ARCore support
- [ ] Test posters prepared
- [ ] Network connectivity verified
- [ ] Sufficient storage space (>500MB)
- [ ] Logcat monitoring enabled

### Test Execution
- [ ] 19.1 - Complete flow test
- [ ] 19.2 - Cache hit test
- [ ] 19.3 - Tracking loss/recovery test
- [ ] 19.4 - Refresh scan test
- [ ] 19.5 - All error scenarios test
- [ ] 19.6 - State transitions test
- [ ] 19.7 - Resource cleanup test

### Post-Test Verification
- [ ] All tests passed
- [ ] No crashes or ANRs
- [ ] No memory leaks
- [ ] Performance metrics met
- [ ] Logs reviewed for errors
- [ ] User experience smooth

---

## Success Criteria Summary

### Performance Metrics
- ✅ Poster detection: <2 seconds
- ✅ Cache retrieval: <100ms
- ✅ Rendering: 60fps sustained
- ✅ Tracking latency: <16ms per frame

### Functional Requirements
- ✅ Complete flow works end-to-end
- ✅ Cache hit scenario works offline
- ✅ Tracking loss/recovery works correctly
- ✅ Refresh scan allows new poster detection
- ✅ All 7 error types handled gracefully
- ✅ State machine transitions correctly
- ✅ Resources cleaned up properly

### Quality Requirements
- ✅ No crashes or ANRs
- ✅ No memory leaks
- ✅ User-friendly error messages
- ✅ Smooth 60fps playback
- ✅ Accurate lip synchronization

---

## Next Steps After Task 19

Once all integration tests pass:
1. ✅ Mark Task 19 complete in tasks.md
2. ✅ Update PHASE_4_PROGRESS.md (100% complete)
3. ✅ Create Task 19 completion summary
4. ✅ Commit and push changes
5. ⏭️ Proceed to Phase 5: Testing & Optimization

---

## Notes

- Integration tests require physical device with ARCore
- Some tests may take several minutes due to video generation
- Network connectivity required for most tests
- Backend service must be running and accessible
- Test data (posters) must be prepared in advance

---

**Task:** 19 - System Integration Checkpoint
**Status:** Ready for execution
**Prerequisites:** All Phase 4 tasks complete
**Next:** Phase 5 - Comprehensive testing and optimization
