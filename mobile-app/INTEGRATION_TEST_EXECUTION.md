# Integration Test Execution Guide

## Quick Start

This guide provides step-by-step instructions for manually executing Task 19 integration tests.

---

## Prerequisites

### 1. Hardware
- ✅ Android device with ARCore support
- ✅ API level 24+ (Android 7.0+)
- ✅ Camera with autofocus
- ✅ Stable internet connection
- ✅ >1GB free storage

### 2. Software
- ✅ Latest app build installed
- ✅ Backend service running (check configuration)
- ✅ ADB installed for log monitoring

### 3. Test Materials
- ✅ Test Poster 1: Celebrity poster with clear human face
- ✅ Test Poster 2: Different poster for refresh scan test
- ✅ Good lighting conditions
- ✅ Flat surface to place posters

---

## Test Execution

### Test 19.1: Complete Flow (Detection → Playback)

**Duration:** ~2-3 minutes

**Steps:**
1. Launch app
2. Point camera at Test Poster 1
3. Wait for detection (should be <2 seconds)
4. Observe "Generating video..." message
5. Wait for video generation (~20-30 seconds)
6. Observe download progress bar
7. Wait for playback to start automatically
8. Verify lip region moves while poster stays static
9. Listen for audio synchronization

**Expected Results:**
- ✅ Poster detected within 2 seconds
- ✅ Progress indicators show during generation/download
- ✅ Video plays smoothly at 60fps
- ✅ Lips move, poster stays static
- ✅ Audio synchronized with lip movement
- ✅ No errors or crashes

**Logcat Commands:**
```bash
# Monitor the complete flow
adb logcat -s TalkingPhotoController:* BackendVideoFetcher:* VideoCache:* ARTrackingManager:*

# Check for errors
adb logcat -s TalkingPhotoErrorHandler:E
```

**Pass Criteria:**
- [ ] Detection time <2 seconds
- [ ] No errors in logs
- [ ] Smooth 60fps playback
- [ ] Lip sync accurate

---

### Test 19.2: Cache Hit (Instant Playback)

**Duration:** ~30 seconds

**Steps:**
1. Ensure Test Poster 1 is already cached (from Test 19.1)
2. Point camera away from poster
3. Point camera back at Test Poster 1
4. Observe instant playback (no generation/download)
5. Verify video plays immediately

**Expected Results:**
- ✅ Video starts playing within 100ms
- ✅ No "Generating..." or "Downloading..." messages
- ✅ Playback identical to first time
- ✅ No network activity

**Logcat Commands:**
```bash
# Verify cache hit
adb logcat -s VideoCache:* | grep "Cache hit"

# Verify no backend calls
adb logcat -s BackendVideoFetcher:* | grep "generateLipSync"
# Should see NO new generation requests
```

**Pass Criteria:**
- [ ] Playback starts <100ms
- [ ] No backend API calls
- [ ] Cache hit logged
- [ ] Smooth playback

**Offline Test:**
1. Enable airplane mode
2. Detect Test Poster 1 again
3. Verify playback works offline

---

### Test 19.3: Tracking Loss and Recovery

**Duration:** ~1 minute

**Steps:**
1. Start playback of Test Poster 1
2. Note current playback position (e.g., 3 seconds)
3. Move camera away from poster (out of frame)
4. Observe "Align poster properly" message
5. Verify video pauses
6. Move camera back to poster (in frame)
7. Verify video resumes from paused position
8. Repeat 3-4 times

**Expected Results:**
- ✅ Tracking loss detected immediately
- ✅ Video pauses within 1 frame (16ms)
- ✅ "Align poster properly" message appears
- ✅ Video resumes from saved position (not restart)
- ✅ Message disappears when tracking resumes
- ✅ Multiple cycles work correctly

**Logcat Commands:**
```bash
# Monitor tracking events
adb logcat -s ARTrackingManager:* TalkingPhotoController:* | grep -E "tracking|pause|resume"
```

**Pass Criteria:**
- [ ] Pause immediate on tracking loss
- [ ] Resume from saved position
- [ ] UI messages display correctly
- [ ] No performance degradation

---

### Test 19.4: Refresh Scan

**Duration:** ~3-4 minutes

**Steps:**
1. Start playback of Test Poster 1
2. Verify "Refresh Scan" button is visible
3. Tap "Refresh Scan" button
4. Observe playback stops
5. Observe "Point camera at a poster..." message
6. Point camera at Test Poster 2 (different poster)
7. Wait for new video generation
8. Verify new video plays
9. Point camera at Test Poster 1 while Poster 2 is playing
10. Verify Poster 1 is ignored (single poster mode)

**Expected Results:**
- ✅ "Refresh Scan" button visible during playback
- ✅ Playback stops when button tapped
- ✅ New poster can be detected
- ✅ New video generated and cached separately
- ✅ Only one poster tracked at a time
- ✅ Previous poster ignored when new one is active

**Logcat Commands:**
```bash
# Monitor refresh scan
adb logcat -s ARTrackingManager:* | grep -E "refreshScan|clearTracking"

# Verify single poster mode
adb logcat -s ARTrackingManager:* | grep "Ignoring poster"
```

**Pass Criteria:**
- [ ] Refresh scan clears current poster
- [ ] New poster detected successfully
- [ ] Single poster mode enforced
- [ ] Resources released properly

---

### Test 19.5: Error Scenarios

**Duration:** ~10-15 minutes

#### Error 1: No Poster Detected
**Steps:**
1. Launch app
2. Point camera at blank wall
3. Wait 10 seconds
4. Observe timeout error message

**Expected:**
- ✅ "No poster detected. Try better lighting."
- ✅ Actionable advice displayed

#### Error 2: No Human Face
**Steps:**
1. Point camera at product poster (no human face)
2. Observe error message

**Expected:**
- ✅ "This poster doesn't contain a human face."
- ✅ "Please scan a poster with a human face"

#### Error 3: Backend Unavailable
**Steps:**
1. Enable airplane mode
2. Detect Test Poster 2 (not cached)
3. Observe network error

**Expected:**
- ✅ "Service unavailable. Please try again later."
- ✅ 3 retry attempts with exponential backoff
- ✅ Delays: 1s, 2s, 4s

#### Error 4: Download Failed
**Steps:**
1. Start video generation
2. Enable airplane mode during download
3. Observe download error

**Expected:**
- ✅ "Failed to download the video."
- ✅ 3 retry attempts
- ✅ Actionable advice

#### Error 5: Cache Corrupted
**Steps:**
1. Use ADB to corrupt cached video file
   ```bash
   adb shell
   cd /data/data/com.talkar.app/cache/videos
   echo "corrupted" > video-file.mp4
   exit
   ```
2. Detect poster with corrupted cache
3. Observe automatic recovery

**Expected:**
- ✅ "The cached video file is corrupted."
- ✅ Automatic deletion and re-download
- ✅ Playback works after recovery

**Logcat Commands:**
```bash
# Monitor all errors
adb logcat -s TalkingPhotoErrorHandler:* ErrorMessageMapper:*

# Check error codes
adb logcat | grep "Error 200[1-7]"
```

**Pass Criteria:**
- [ ] All error messages user-friendly
- [ ] Actionable advice provided
- [ ] Retry logic works (3 attempts)
- [ ] Automatic recovery for corrupted cache
- [ ] All errors logged with correct codes

---

### Test 19.6: State Transitions

**Duration:** ~5 minutes

**Steps:**
1. Monitor state changes during complete flow
2. Verify state sequence matches expected

**Expected State Sequence (Happy Path):**
```
IDLE → FETCHING_VIDEO → GENERATING → DOWNLOADING → READY → PLAYING
```

**Expected State Sequence (Cache Hit):**
```
IDLE → FETCHING_VIDEO → READY → PLAYING
```

**Expected State Sequence (Tracking Loss):**
```
PLAYING → PAUSED → PLAYING
```

**Expected State Sequence (Error):**
```
GENERATING → ERROR
```

**Logcat Commands:**
```bash
# Monitor state transitions
adb logcat -s TalkingPhotoController:* | grep "State transition"
```

**Pass Criteria:**
- [ ] All state transitions occur in correct order
- [ ] No invalid transitions
- [ ] State changes logged
- [ ] UI updates on state changes

---

### Test 19.7: Resource Cleanup

**Duration:** ~2 minutes

**Steps:**
1. Start video playback
2. Press home button (app goes to background)
3. Wait 5 seconds
4. Return to app
5. Verify playback can resume
6. Repeat 5 times
7. Check memory usage

**Expected Results:**
- ✅ Resources released on background
- ✅ Resources restored on foreground
- ✅ Playback position preserved
- ✅ No memory leaks
- ✅ Multiple cycles work correctly

**Logcat Commands:**
```bash
# Monitor lifecycle events
adb logcat -s TalkingPhotoController:* | grep -E "release|initialize"

# Check memory usage
adb shell dumpsys meminfo com.talkar.app | grep -E "TOTAL|Native Heap"
```

**Memory Leak Check:**
```bash
# Before test
adb shell dumpsys meminfo com.talkar.app | grep "TOTAL PSS"

# After 10 background/foreground cycles
adb shell dumpsys meminfo com.talkar.app | grep "TOTAL PSS"

# Memory should be stable (not growing significantly)
```

**Pass Criteria:**
- [ ] Resources released on background
- [ ] Resources restored on foreground
- [ ] No memory leaks (stable heap size)
- [ ] Playback position preserved

---

## Test Results Template

### Test Execution Summary

**Date:** _______________
**Tester:** _______________
**Device:** _______________
**Android Version:** _______________
**App Version:** _______________
**Backend:** _______________

### Results

| Test | Status | Duration | Notes |
|------|--------|----------|-------|
| 19.1 Complete Flow | ⬜ Pass ⬜ Fail | _____ | _____ |
| 19.2 Cache Hit | ⬜ Pass ⬜ Fail | _____ | _____ |
| 19.3 Tracking Loss | ⬜ Pass ⬜ Fail | _____ | _____ |
| 19.4 Refresh Scan | ⬜ Pass ⬜ Fail | _____ | _____ |
| 19.5 Error Scenarios | ⬜ Pass ⬜ Fail | _____ | _____ |
| 19.6 State Transitions | ⬜ Pass ⬜ Fail | _____ | _____ |
| 19.7 Resource Cleanup | ⬜ Pass ⬜ Fail | _____ | _____ |

### Performance Metrics

- Poster detection time: _____ seconds (target: <2s)
- Cache retrieval time: _____ ms (target: <100ms)
- Rendering FPS: _____ fps (target: 60fps)
- Tracking latency: _____ ms (target: <16ms)

### Issues Found

1. _____________________________________
2. _____________________________________
3. _____________________________________

### Overall Assessment

⬜ All tests passed - Ready for Phase 5
⬜ Some tests failed - Fixes required
⬜ Major issues found - Significant work needed

---

## Troubleshooting

### Issue: Poster Not Detected
**Solutions:**
- Ensure good lighting
- Use poster with clear human face
- Check ARCore is installed and updated
- Verify camera permissions granted

### Issue: Backend Unavailable
**Solutions:**
- Check backend URL in ApiConfig logs
- Verify backend service is running
- Test backend with curl: `curl https://backend-url/api/v1/health`
- Check network connectivity

### Issue: Slow Performance
**Solutions:**
- Close other apps
- Restart device
- Check available storage
- Verify device meets minimum specs

### Issue: Crashes
**Solutions:**
- Check logcat for stack traces
- Verify all dependencies installed
- Clear app data and cache
- Reinstall app

---

## Next Steps

After all tests pass:
1. ✅ Fill out test results template
2. ✅ Document any issues found
3. ✅ Mark Task 19 complete in tasks.md
4. ✅ Update PHASE_4_PROGRESS.md
5. ✅ Commit and push changes
6. ⏭️ Proceed to Phase 5

---

**Document Version:** 1.0
**Last Updated:** February 25, 2026
**Status:** Ready for execution
