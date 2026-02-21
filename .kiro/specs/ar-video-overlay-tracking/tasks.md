# Implementation Plan: AR Video Overlay Tracking

## Overview

This implementation plan addresses the critical issues preventing video frame rendering in the TalkAR AR video overlay feature. The current implementation uses ExoPlayer with PlayerView in a Compose overlay, which fails to render video frames despite successful audio playback. This plan replaces the PlayerView-based approach with a TextureView + ExoPlayer architecture that provides direct access to video frames, hardware-accelerated rendering, and proper integration with ARCore's 60fps tracking updates.

### Current Issues Being Fixed
1. **Video Dimension Detection Failure**: ExoPlayer reports 0x0 dimensions even when video tracks are detected
2. **Frame Rendering Failure**: PlayerView in Compose overlay doesn't render frames to screen
3. **AR Integration Gap**: No proper synchronization between ARCore tracking updates and video position

### Key Components to Implement
- **VideoDecoder**: ExoPlayer wrapper with proper Surface management and dimension extraction
- **TextureSurface**: Hardware-accelerated rendering surface with transformation support
- **RenderCoordinator**: 60fps synchronized tracking updates with ARCore integration
- **VideoOverlayController**: Orchestrates lifecycle and coordinates all components
- **ARTrackingManager**: Manages ARCore image detection and anchor lifecycle
- **VideoOverlayManager**: Manages multiple overlays and enforces resource limits

### Implementation Strategy
The tasks are organized in dependency order, with checkpoints at strategic points. Property-based tests (marked with *) are optional for MVP but recommended for production. Each task references specific requirements from requirements.md for traceability.

## Tasks

### Phase 1: Foundation (Tasks 1-4)

- [ ] 1. Set up core data models and error types
  - [ ] 1.1 Create data models package structure
    - Create `com.talkar.app.ar.video.models` package
    - Create `com.talkar.app.ar.video.errors` package
  - [ ] 1.2 Implement core data classes
    - Create VideoConfiguration data class (uri, autoPlay, looping, volume, startPositionMs)
    - Create TrackingData data class (position, rotation, scale, isTracking, timestamp)
    - Create VideoInfo data class (width, height, durationMs, frameRate, codec, hasAudioTrack, hasVideoTrack)
    - Create TransformResult data class (matrix, screenPosition, screenSize, isVisible, distanceFromCamera)
    - Create FrameTime data class (timestampNs, deltaTimeMs)
    - Create TrackedImage data class (id, name, anchor, trackingState, extentX, extentZ)
    - Create OverlayTrackingState data class with tracking timeout logic
  - [ ] 1.3 Implement PlaybackState enum
    - Define states: IDLE, INITIALIZING, READY, PLAYING, PAUSED, STOPPED, ERROR
  - [ ] 1.4 Create VideoError sealed class hierarchy
    - DecoderInitializationFailed, SurfaceCreationFailed, VideoLoadFailed
    - UnsupportedCodec, RenderingFailed, TrackingLost
    - Include error codes and contextual information
  - [ ] 1.5 Create VideoMetrics data class
    - Add fields for initialization time, first frame time, frames rendered/dropped
    - Implement getDroppedFramePercentage() and getAverageLatency() methods
  - _Requirements: 2.2, 2.3, 9.5, 11.1, 11.2, 11.3, 11.5_

- [ ] 2. Implement TextureSurface component
  - [ ] 2.1 Create TextureSurface interface and implementation
    - Implement getSurface(), setTransform(), setSize(), setVisible(), isAvailable(), release() methods
    - Wrap Android TextureView with SurfaceTexture lifecycle management
    - Implement SurfaceTextureListener for surface availability callbacks
    - Handle surface recreation on configuration changes
    - _Requirements: 3.1, 3.5_
  
  - [ ] 2.2 Create Composable wrapper for TextureSurface
    - Implement VideoTextureSurface Composable using AndroidView
    - Use remember to prevent recreation on recomposition
    - Add DisposableEffect for proper cleanup
    - Provide callbacks for surface available/destroyed events
    - _Requirements: 3.1, 3.5_
  
  - [ ]* 2.3 Write property test for TextureSurface
    - **Property 8: Aspect Ratio Preservation**
    - **Validates: Requirements 3.3, 12.1**
    - Test that rendered aspect ratio matches original video aspect ratio across various video dimensions

- [ ] 3. Implement VideoDecoder component
  - [ ] 3.1 Create VideoDecoder interface and ExoPlayer-based implementation
    - Implement initialize(), start(), pause(), stop(), seekTo(), release() methods
    - Configure ExoPlayer with hardware acceleration and decoder fallback
    - Set up proper buffer management (1-3 second buffers)
    - Implement volume control and looping functionality
    - _Requirements: 2.1, 2.4, 6.3, 7.1, 7.2_
  
  - [ ] 3.2 Implement video dimension extraction with fallback
    - Extract dimensions from ExoPlayer's onTracksChanged callback
    - Implement MediaMetadataRetriever fallback for dimension extraction
    - Wait for STATE_READY before querying video dimensions
    - Handle onVideoSizeChanged for dynamic size changes
    - _Requirements: 2.2, 2.3_
  
  - [ ]* 3.3 Write property test for video metadata extraction
    - **Property 5: Video Metadata Extraction**
    - **Validates: Requirements 2.2, 2.3**
    - Test that decoder extracts non-zero dimensions, codec info, and track details for all valid video files
  
  - [ ] 3.4 Implement decoder initialization with retry logic
    - Add retry mechanism (one retry after 500ms on failure)
    - Implement proper error handling and logging with codec details
    - Handle unsupported codec detection and reporting
    - _Requirements: 2.5, 9.2_
  
  - [ ]* 3.5 Write property test for decoder error handling
    - **Property 6: Video Loading Error Reporting**
    - **Validates: Requirements 2.5**
    - Test that all video loading failures produce error logs with codec and file information
  
  - [ ] 3.6 Implement codec support and format validation
    - Add support for H.264 and H.265 codecs
    - Validate MP4 container format
    - Support resolutions from 480p to 1080p
    - Handle 24fps, 30fps, and 60fps frame rates
    - Support portrait and landscape orientations
    - _Requirements: 7.3, 10.1, 10.2, 10.3, 10.4_
  
  - [ ]* 3.7 Write property tests for codec and format support
    - **Property 15: Codec Support**
    - **Validates: Requirements 7.3**
    - **Property 22: MP4 Format Support**
    - **Validates: Requirements 10.1**
    - **Property 23: Resolution Range Support**
    - **Validates: Requirements 10.2**
    - **Property 24: Frame Rate Support**
    - **Validates: Requirements 10.3**
    - **Property 25: Orientation Support**
    - **Validates: Requirements 10.4**
    - Test decoder successfully handles all supported codecs, formats, resolutions, frame rates, and orientations

- [ ] 4. Checkpoint - Verify decoder and surface components
  - [ ] 4.1 Run all TextureSurface and VideoDecoder unit tests
  - [ ] 4.2 Verify dimension extraction works with test video file (sunrich_1.mp4)
  - [ ] 4.3 Confirm surface lifecycle management handles recreation correctly
  - [ ] 4.4 Test ExoPlayer initialization and track detection
  - [ ] 4.5 Verify hardware acceleration is enabled
  - Ask the user if questions arise or if ready to proceed to Phase 2

### Phase 2: AR Integration (Tasks 5-8)

- [ ] 5. Implement RenderCoordinator component
  - [ ] 5.1 Create RenderCoordinator interface and implementation
    - Implement calculateTransform() with ARCore matrix math
    - Use Camera.getProjectionMatrix() and Camera.getViewMatrix()
    - Convert 3D anchor pose to 2D screen coordinates
    - Calculate scale based on distance from camera
    - Implement frustum culling for off-screen overlays
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 5.5_
  
  - [ ]* 5.2 Write property test for transform calculation
    - **Property 9: Transform Calculation Correctness**
    - **Validates: Requirements 4.1, 4.2, 4.3, 4.5**
    - Test that calculated transforms correctly position video center at anchor center, scale to match physical dimensions, and apply proper rotation
  
  - [ ] 5.3 Implement frame callback synchronization with Choreographer
    - Register frame callbacks synchronized with AR rendering
    - Use Choreographer for 60fps updates
    - Calculate frame delta times for smooth animation
    - Implement callback registration and unregistration
    - _Requirements: 5.1, 5.2_
  
  - [ ] 5.4 Add transform caching and optimization
    - Cache projection and view matrices per frame
    - Reuse calculations for multiple overlays
    - Implement dirty flags to avoid unnecessary recalculations
    - Add update throttling (16ms minimum interval)
    - _Requirements: 5.1, 5.3, 5.4_

- [ ] 6. Implement ARTrackingManager component
  - [ ] 6.1 Create ARTrackingManager interface and implementation
    - Implement initialize() with AugmentedImageDatabase setup
    - Implement processFrame() to detect and track images
    - Manage anchor creation and lifecycle
    - Implement getAnchor() for anchor retrieval by image ID
    - _Requirements: 1.1, 1.2, 1.3_
  
  - [ ]* 6.2 Write property test for anchor creation
    - **Property 1: Anchor Creation Completeness**
    - **Validates: Requirements 1.2**
    - Test that all detected reference images result in anchors with valid position and orientation data
  
  - [ ] 6.3 Implement tracking state management
    - Track consecutive tracking/lost frames
    - Implement 5-second timeout for lost tracking (300 frames at 60fps)
    - Filter duplicate tracking events
    - Maintain map of active anchors
    - _Requirements: 1.4, 1.5_
  
  - [ ]* 6.4 Write property tests for tracking state transitions
    - **Property 2: Tracking Timeout Deactivation**
    - **Validates: Requirements 1.4**
    - **Property 3: Tracking Resume Preservation**
    - **Validates: Requirements 1.5**
    - Test that anchors are deactivated after 5 seconds of lost tracking and can resume without re-detection

- [ ] 7. Implement VideoOverlayController component
  - [ ] 7.1 Create VideoOverlayController interface and implementation
    - Implement initialize() to set up decoder and surface
    - Implement play(), pause(), stop() playback controls
    - Implement getState() for state machine management
    - Implement release() for resource cleanup
    - Manage lifecycle of VideoDecoder and TextureSurface
    - _Requirements: 2.1, 2.4, 6.5, 8.1_
  
  - [ ]* 7.2 Write property test for video loading trigger
    - **Property 4: Video Loading Trigger**
    - **Validates: Requirements 2.1**
    - Test that all created image anchors trigger video file loading
  
  - [ ] 7.3 Implement tracking update handling
    - Implement updateTracking() to process ARCore tracking data
    - Coordinate with RenderCoordinator for transform calculation
    - Apply transforms to TextureSurface
    - Record update timestamps for latency tracking
    - _Requirements: 4.4, 5.1, 5.3_
  
  - [ ] 7.4 Implement playback state management
    - Create state machine for IDLE, INITIALIZING, READY, PLAYING, PAUSED, STOPPED, ERROR states
    - Pause playback when tracking is lost
    - Resume playback when tracking returns
    - Handle tracking timeout (5 seconds)
    - _Requirements: 6.1, 6.2_
  
  - [ ]* 7.5 Write property tests for playback control
    - **Property 10: Playback Pause on Tracking Loss**
    - **Validates: Requirements 6.1**
    - **Property 11: Playback Resume from Position**
    - **Validates: Requirements 6.2**
    - **Property 12: Video Looping**
    - **Validates: Requirements 6.3**
    - Test pause on tracking loss, resume from position, and automatic looping behavior
  
  - [ ] 7.6 Implement callback system for lifecycle events
    - Create VideoOverlayCallbacks interface
    - Implement onReady, onFirstFrameRendered, onPlaybackComplete callbacks
    - Implement onError, onTrackingLost, onTrackingResumed callbacks
    - _Requirements: 2.4, 9.5_

- [ ] 8. Checkpoint - Verify core component integration
  - [ ] 8.1 Test VideoOverlayController with mock ARCore data
  - [ ] 8.2 Verify tracking updates flow correctly through RenderCoordinator
  - [ ] 8.3 Confirm playback state transitions (IDLE → READY → PLAYING → PAUSED)
  - [ ] 8.4 Test tracking loss and recovery scenarios
  - [ ] 8.5 Verify transform calculations produce correct screen positions
  - Ask the user if questions arise or if ready to proceed to Phase 3

### Phase 3: Resource Management & Error Handling (Tasks 9-13)

- [ ] 9. Implement resource management and optimization
  - [ ] 9.1 Implement decoder pooling and reuse
    - Create decoder pool with maximum 3 instances
    - Reuse decoders for same codec type
    - Release least-recently-used decoder when pool is full
    - _Requirements: 8.2, 8.4_
  
  - [ ]* 9.2 Write property test for decoder reuse
    - **Property 17: Decoder Reuse Optimization**
    - **Validates: Requirements 8.2**
    - Test that consecutive videos with same codec reuse decoder instances
  
  - [ ] 9.3 Implement memory pressure handling
    - Implement ComponentCallbacks2 for memory pressure events
    - Release cached frames on TRIM_MEMORY_RUNNING_LOW
    - Release inactive overlays on TRIM_MEMORY_RUNNING_CRITICAL
    - Release all overlays on TRIM_MEMORY_UI_HIDDEN
    - _Requirements: 8.3, 8.5_
  
  - [ ] 9.4 Implement concurrent overlay limits
    - Enforce maximum 3 concurrent video overlays
    - Implement least-recently-used eviction policy
    - Provide error when limit is exceeded
    - _Requirements: 8.4_
  
  - [ ]* 9.5 Write property test for resource cleanup
    - **Property 13: Lifecycle Resource Cleanup**
    - **Validates: Requirements 6.5**
    - **Property 16: Resource Release on Disposal**
    - **Validates: Requirements 8.1**
    - Test that resources are properly released on app focus loss and overlay disposal

- [ ] 10. Implement error handling and recovery
  - [ ] 10.1 Implement comprehensive error handling
    - Handle decoder initialization failures with retry logic
    - Handle surface creation failures
    - Handle video loading failures
    - Provide detailed error logging with context
    - _Requirements: 9.1, 9.2, 9.5_
  
  - [ ]* 10.2 Write property test for error handling
    - **Property 18: Decoding Error Handling**
    - **Validates: Requirements 9.1**
    - **Property 21: Error Callback Provision**
    - **Validates: Requirements 9.5**
    - Test that decoding failures are logged and handled without crashes, and error callbacks are invoked
  
  - [ ] 10.3 Implement surface recreation recovery
    - Detect surface destruction events
    - Automatically recreate surface
    - Resume playback after recreation
    - Handle multiple recreation failures gracefully
    - _Requirements: 9.3_
  
  - [ ]* 10.4 Write property test for surface recovery
    - **Property 19: Surface Recreation Recovery**
    - **Validates: Requirements 9.3**
    - Test that invalid surfaces are automatically recreated and playback resumes
  
  - [ ] 10.5 Implement frame rendering error recovery
    - Skip failed frames without stopping playback
    - Log frame rendering failures
    - Continue with next frame
    - _Requirements: 9.4_
  
  - [ ]* 10.6 Write property test for frame error recovery
    - **Property 20: Frame Rendering Error Recovery**
    - **Validates: Requirements 9.4**
    - Test that frame rendering failures don't stop playback
  
  - [ ] 10.7 Implement hardware/software decoder fallback
    - Detect hardware decoder availability
    - Automatically fall back to software decoding
    - Log decoder type being used
    - _Requirements: 7.2_
  
  - [ ]* 10.8 Write property test for decoder fallback
    - **Property 14: Software Decoding Fallback**
    - **Validates: Requirements 7.2**
    - Test that software decoding is used when hardware is unavailable

- [ ] 11. Implement performance monitoring and metrics
  - [ ] 11.1 Create VideoMetrics tracking system
    - Track initialization time and first frame render time
    - Track total frames rendered and dropped frames
    - Calculate average FPS and dropped frame percentage
    - Track tracking update latency with rolling window
    - _Requirements: 11.1, 11.2, 11.3, 11.5_
  
  - [ ]* 11.2 Write property test for metrics collection
    - **Property 27: Performance Metrics Collection**
    - **Validates: Requirements 11.1, 11.2, 11.3, 11.5**
    - Test that all required metrics are collected during playback sessions
  
  - [ ] 11.3 Implement performance warning system
    - Log warning when FPS drops below 45fps for more than 2 seconds
    - Log frame drop percentage
    - Log excessive tracking latency
    - _Requirements: 11.4_
  
  - [ ] 11.4 Add performance optimization features
    - Implement adaptive quality based on FPS
    - Reduce frame rate to 30fps when battery is low
    - Skip rendering when overlay is off-screen
    - _Requirements: 5.4, 7.5_

- [ ] 12. Implement video processing integrity validation
  - [ ] 12.1 Implement frame dimension preservation validation
    - Verify decoded frame dimensions match original
    - Track dimension changes through pipeline
    - _Requirements: 12.1_
  
  - [ ]* 12.2 Write property test for dimension preservation
    - **Property 28: Frame Dimension Preservation**
    - **Validates: Requirements 12.1**
    - Test that frame dimensions are preserved through decode-render pipeline
  
  - [ ] 12.3 Implement color space preservation validation
    - Verify rendered frames maintain source color space
    - Detect color shifts or distortions
    - _Requirements: 12.2_
  
  - [ ]* 12.4 Write property test for color space preservation
    - **Property 29: Color Space Preservation**
    - **Validates: Requirements 12.2**
    - Test that color space is maintained during rendering
  
  - [ ] 12.5 Implement timestamp consistency validation
    - Verify frame timestamps are monotonically increasing
    - Check audio-video sync within 100ms
    - _Requirements: 6.4, 12.3, 12.4_
  
  - [ ]* 12.6 Write property test for timestamp consistency
    - **Property 30: Frame Timestamp Consistency**
    - **Validates: Requirements 12.3**
    - Test that frame timestamps are consistent and monotonically increasing
  
  - [ ] 12.7 Implement decoder configuration round-trip validation
    - Verify load-unload-reload produces identical configuration
    - Track configuration parameters
    - _Requirements: 12.5_
  
  - [ ]* 12.8 Write property test for configuration round-trip
    - **Property 31: Decoder Configuration Round-Trip**
    - **Validates: Requirements 12.5**
    - Test that reloading videos produces identical decoder configuration

- [ ] 13. Checkpoint - Verify error handling and monitoring
  - [ ] 13.1 Test all error scenarios (decoder failure, surface loss, tracking loss)
  - [ ] 13.2 Verify performance metrics are collected accurately
  - [ ] 13.3 Confirm video processing integrity validations work
  - [ ] 13.4 Test memory pressure handling and resource cleanup
  - [ ] 13.5 Verify error callbacks are invoked with correct error codes
  - Ask the user if questions arise or if ready to proceed to Phase 4

### Phase 4: Final Integration & Testing (Tasks 14-17)

- [ ] 14. Implement multi-track rendering support
  - [ ] 14.1 Ensure both audio and video tracks are rendered
    - Configure ExoPlayer to enable both audio and video renderers
    - Verify track selection includes both tracks
    - Monitor audio-video synchronization
    - _Requirements: 3.2, 6.4_
  
  - [ ]* 14.2 Write property test for multi-track rendering
    - **Property 7: Multi-Track Rendering**
    - **Validates: Requirements 3.2**
    - Test that videos with both audio and video tracks render both correctly

- [ ] 15. Implement unsupported parameter reporting
  - [ ] 15.1 Add detailed logging for unsupported parameters
    - Log specific unsupported codec details
    - Log unsupported resolution or frame rate
    - Log unsupported container format
    - _Requirements: 10.5_
  
  - [ ]* 15.2 Write property test for unsupported parameter reporting
    - **Property 26: Unsupported Parameter Reporting**
    - **Validates: Requirements 10.5**
    - Test that unsupported parameters are logged with specific details

- [ ] 16. Create integration layer and wiring
  - [ ] 16.1 Create VideoOverlayManager to orchestrate multiple overlays
    - Manage multiple VideoOverlayController instances
    - Enforce concurrent overlay limits
    - Coordinate with ARTrackingManager
    - Handle overlay lifecycle
    - _Requirements: 1.1, 2.1, 8.4_
  
  - [ ] 16.2 Wire ARTrackingManager with VideoOverlayManager
    - Connect image detection events to overlay creation
    - Connect tracking updates to overlay position updates
    - Handle tracking loss and recovery events
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_
  
  - [ ] 16.3 Integrate with ARCore session and scene
    - Add frame update listener to ARCore scene
    - Process AR frames through ARTrackingManager
    - Synchronize with ARCore's rendering pipeline
    - _Requirements: 1.3, 5.1, 5.5_
  
  - [ ] 16.4 Create Composable UI integration
    - Create main AR video overlay Composable
    - Integrate TextureSurface Composables for each overlay
    - Handle Compose lifecycle and recomposition
    - Add proper DisposableEffect for cleanup
    - _Requirements: 3.1, 3.5_
  
  - [ ]* 16.5 Write integration tests for complete flow
    - Test end-to-end: image detection → video load → playback → tracking
    - Test multi-overlay scenarios
    - Test rapid tracking loss and recovery
    - Test app lifecycle transitions
    - _Requirements: All requirements_

- [ ] 17. Final checkpoint - Complete system verification
  - [ ] 17.1 Run complete test suite (unit + property tests)
  - [ ] 17.2 Verify all 31 correctness properties pass
  - [ ] 17.3 Test on physical Android device (API 24+)
  - [ ] 17.4 Measure and verify 60fps performance target
  - [ ] 17.5 Test with actual reference image (poster) and camera
  - [ ] 17.6 Verify video tracks image movement smoothly
  - [ ] 17.7 Confirm all 12 requirements are satisfied
  - [ ] 17.8 Document any known limitations or issues
  - Ask the user for final review and approval

## Success Criteria

The implementation is complete when:
1. ✅ Video frames render visibly on detected AR images (not just audio)
2. ✅ ExoPlayer reports correct non-zero video dimensions
3. ✅ Video overlay tracks image position smoothly at 60fps as camera moves
4. ✅ All core components (VideoDecoder, TextureSurface, RenderCoordinator, VideoOverlayController, ARTrackingManager) are implemented
5. ✅ Error handling gracefully manages all failure scenarios
6. ✅ Performance metrics show <1% dropped frames and <33ms tracking latency
7. ✅ All 12 requirements have passing acceptance criteria
8. ✅ Integration tests pass for complete AR video overlay flow

## Notes

### Testing Strategy
- **Tasks marked with `*`**: Optional property-based tests that can be skipped for faster MVP
- **Property tests**: Validate universal correctness properties from design.md (31 total properties)
- **Unit tests**: Validate specific examples, edge cases, and integration points
- **Integration tests**: Test complete end-to-end flows with ARCore

### Implementation Guidelines
- **Language**: All code in Kotlin for Android
- **Video Library**: ExoPlayer (Media3) for video decoding
- **AR Library**: ARCore for image detection and tracking
- **Target Platform**: Android API level 24 (Android 7.0) and above
- **Performance Target**: 60fps rendering with <1% dropped frames
- **Hardware Acceleration**: Enabled by default with automatic software fallback

### Task Organization
- **Phase 1 (Tasks 1-4)**: Foundation - Data models, TextureSurface, VideoDecoder
- **Phase 2 (Tasks 5-8)**: AR Integration - RenderCoordinator, ARTrackingManager, VideoOverlayController
- **Phase 3 (Tasks 9-13)**: Resource Management - Pooling, error handling, monitoring
- **Phase 4 (Tasks 14-17)**: Final Integration - Multi-track support, wiring, testing

### Traceability
- Each task references specific requirements from requirements.md
- Property tests reference specific correctness properties from design.md
- Checkpoints ensure incremental validation at phase boundaries

### File Locations
- **Source**: `mobile-app/app/src/main/java/com/talkar/app/ar/`
- **Models**: `mobile-app/app/src/main/java/com/talkar/app/ar/video/models/`
- **Tests**: `mobile-app/app/src/test/java/com/talkar/app/ar/video/`
- **Test Video**: `mobile-app/app/src/main/res/raw/sunrich_1.mp4`

### Critical Implementation Notes
1. **Surface Lifecycle**: Create TextureView and wait for onSurfaceTextureAvailable before initializing ExoPlayer
2. **Dimension Extraction**: Wait for onTracksChanged or onVideoSizeChanged callbacks, don't query dimensions earlier
3. **Compose Integration**: Use remember and DisposableEffect to prevent recreation on recomposition
4. **ARCore Sync**: Use Choreographer for frame-synchronized updates at 60fps
5. **Resource Limits**: Enforce maximum 3 concurrent video overlays
6. **Tracking Timeout**: Mark anchors inactive after 5 seconds (300 frames) of lost tracking
