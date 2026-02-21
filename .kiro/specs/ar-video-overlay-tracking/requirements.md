# Requirements Document

## Introduction

This document specifies requirements for implementing AR video overlay functionality in the TalkAR Android application. The feature enables video playback overlaid on ARCore-detected reference images, with real-time tracking as the camera moves. The current implementation plays audio but fails to render video frames due to ExoPlayer reporting 0x0 video dimensions and improper frame rendering in the PlayerView overlay.

## Glossary

- **AR_System**: The ARCore-based augmented reality system that detects and tracks reference images
- **Video_Overlay**: The video playback surface that renders on top of detected AR images
- **Reference_Image**: A physical image (e.g., poster) that ARCore detects and tracks in the camera view
- **Image_Anchor**: ARCore's tracking data for a detected reference image, including position and orientation
- **Video_Decoder**: The component responsible for decoding video frames from source files
- **Render_Surface**: The graphics surface where decoded video frames are displayed
- **Tracking_Update**: Position and orientation data from ARCore for a detected image, provided per frame
- **Frame_Rate**: The rate at which video frames are rendered, target is 60 frames per second
- **Video_Transform**: The position, scale, and rotation applied to align video with the detected image

## Requirements

### Requirement 1: Image Detection and Tracking

**User Story:** As a user, I want the app to detect reference images through my camera, so that I can trigger AR video overlays.

#### Acceptance Criteria

1. WHEN a Reference_Image appears in the camera view, THE AR_System SHALL detect it within 2 seconds
2. WHEN a Reference_Image is detected, THE AR_System SHALL create an Image_Anchor with position and orientation data
3. WHILE a Reference_Image is visible, THE AR_System SHALL provide Tracking_Updates at the camera Frame_Rate
4. IF a Reference_Image leaves the camera view for more than 5 seconds, THEN THE AR_System SHALL mark the Image_Anchor as inactive
5. WHEN an inactive Image_Anchor's Reference_Image reappears, THE AR_System SHALL resume tracking without requiring re-detection

### Requirement 2: Video Playback Initialization

**User Story:** As a user, I want video to start playing when a reference image is detected, so that I see the AR content immediately.

#### Acceptance Criteria

1. WHEN an Image_Anchor is created, THE Video_Overlay SHALL load the associated video file from res/raw/
2. WHEN the video file is loaded, THE Video_Decoder SHALL extract video track dimensions and codec information
3. THE Video_Decoder SHALL report non-zero video dimensions for all valid video files
4. WHEN video loading completes, THE Video_Overlay SHALL begin playback within 500 milliseconds
5. IF video loading fails, THEN THE Video_Overlay SHALL log the error with codec and file information

### Requirement 3: Video Frame Rendering

**User Story:** As a user, I want to see video frames rendered on the detected image, so that the AR experience is complete.

#### Acceptance Criteria

1. WHEN the Video_Decoder produces a frame, THE Render_Surface SHALL display it within 16 milliseconds (60fps target)
2. THE Render_Surface SHALL render both video and audio tracks from the video file
3. FOR ALL video frames, THE Render_Surface SHALL maintain the original video aspect ratio
4. WHEN video playback is active, THE Render_Surface SHALL decode and display frames continuously without dropping frames under normal conditions
5. THE Video_Overlay SHALL use a hardware-accelerated Render_Surface for video decoding and display

### Requirement 4: Video Overlay Positioning

**User Story:** As a user, I want the video to appear exactly on top of the detected image, so that it looks naturally integrated with the physical world.

#### Acceptance Criteria

1. WHEN a Tracking_Update is received, THE Video_Overlay SHALL calculate the Video_Transform to match the Image_Anchor position
2. THE Video_Overlay SHALL scale the Render_Surface to match the Reference_Image physical dimensions
3. THE Video_Overlay SHALL apply rotation to align with the Reference_Image orientation in 3D space
4. FOR ALL Tracking_Updates, THE Video_Overlay SHALL update the Video_Transform within the same frame (16ms at 60fps)
5. THE Video_Overlay SHALL position the video center to align with the Image_Anchor center point

### Requirement 5: Real-Time Tracking Synchronization

**User Story:** As a user, I want the video to follow the image smoothly as I move my camera, so that the AR experience feels natural and responsive.

#### Acceptance Criteria

1. WHEN the camera moves, THE Video_Overlay SHALL update position at the camera Frame_Rate (60fps)
2. THE Video_Overlay SHALL apply Tracking_Updates without visible lag or jitter
3. WHILE tracking is active, THE Video_Overlay SHALL maintain synchronization between video position and Image_Anchor position with less than 33ms latency
4. WHEN rapid camera movement occurs, THE Video_Overlay SHALL continue smooth tracking without stuttering
5. THE Video_Overlay SHALL use the AR rendering pipeline's transformation matrices for position calculations

### Requirement 6: Video Playback Control

**User Story:** As a user, I want video playback to respond appropriately to tracking state changes, so that the experience is predictable.

#### Acceptance Criteria

1. WHEN an Image_Anchor becomes inactive, THE Video_Overlay SHALL pause video playback
2. WHEN an Image_Anchor resumes tracking, THE Video_Overlay SHALL resume video playback from the paused position
3. WHEN video playback reaches the end, THE Video_Overlay SHALL loop the video from the beginning
4. THE Video_Overlay SHALL maintain audio-video synchronization throughout playback with less than 100ms drift
5. WHEN the app loses focus, THE Video_Overlay SHALL pause playback and release decoder resources

### Requirement 7: Multi-Device Compatibility

**User Story:** As a user on any supported Android device, I want the AR video feature to work reliably, so that I have a consistent experience.

#### Acceptance Criteria

1. THE Video_Overlay SHALL support hardware video decoding on devices with hardware decoders
2. IF hardware decoding is unavailable, THEN THE Video_Overlay SHALL fall back to software decoding
3. THE Video_Overlay SHALL support video files in H.264 and H.265 codecs
4. THE Video_Overlay SHALL function on Android devices running API level 24 (Android 7.0) and above
5. THE Video_Overlay SHALL adapt rendering quality based on device GPU capabilities to maintain 60fps

### Requirement 8: Video Resource Management

**User Story:** As a user, I want the app to manage resources efficiently, so that it doesn't drain my battery or cause performance issues.

#### Acceptance Criteria

1. WHEN a Video_Overlay is no longer needed, THE Video_Overlay SHALL release the Video_Decoder and Render_Surface resources
2. THE Video_Overlay SHALL reuse decoder instances when switching between videos of the same codec
3. WHEN memory pressure occurs, THE Video_Overlay SHALL release cached video frames
4. THE Video_Overlay SHALL limit maximum concurrent video overlays to 3 instances
5. WHEN the app enters background, THE Video_Overlay SHALL release all video decoder resources within 1 second

### Requirement 9: Error Handling and Recovery

**User Story:** As a user, I want the app to handle errors gracefully, so that one failure doesn't break the entire AR experience.

#### Acceptance Criteria

1. IF video decoding fails, THEN THE Video_Overlay SHALL log the error and display an error placeholder
2. WHEN decoder initialization fails, THE Video_Overlay SHALL retry initialization once after 500ms
3. IF the Render_Surface becomes invalid, THEN THE Video_Overlay SHALL recreate it and resume playback
4. WHEN frame rendering fails, THE Video_Overlay SHALL skip the failed frame and continue with the next frame
5. THE Video_Overlay SHALL provide error callbacks with specific error codes for debugging

### Requirement 10: Video Format Support

**User Story:** As a content creator, I want to use standard video formats, so that I can easily create AR content without special encoding.

#### Acceptance Criteria

1. THE Video_Overlay SHALL support MP4 container format for video files
2. THE Video_Overlay SHALL decode video files with resolutions from 480p to 1080p
3. THE Video_Overlay SHALL support video frame rates of 24fps, 30fps, and 60fps
4. THE Video_Overlay SHALL handle both portrait and landscape video orientations
5. WHEN a video file has unsupported parameters, THE Video_Overlay SHALL log specific unsupported parameter details

### Requirement 11: Performance Monitoring

**User Story:** As a developer, I want to monitor video overlay performance, so that I can identify and fix performance issues.

#### Acceptance Criteria

1. THE Video_Overlay SHALL log the actual Frame_Rate achieved during playback
2. THE Video_Overlay SHALL track and log dropped frame count per playback session
3. THE Video_Overlay SHALL measure and log the latency between Tracking_Update receipt and Video_Transform application
4. WHEN Frame_Rate drops below 45fps for more than 2 seconds, THE Video_Overlay SHALL log a performance warning
5. THE Video_Overlay SHALL provide metrics for decoder initialization time and first frame render time

### Requirement 12: Round-Trip Video Processing

**User Story:** As a developer, I want to verify video processing integrity, so that I can ensure no corruption occurs during decode-render cycles.

#### Acceptance Criteria

1. FOR ALL decoded video frames, THE Video_Decoder SHALL preserve the original frame dimensions
2. FOR ALL rendered frames, THE Render_Surface SHALL maintain the color space of the source video
3. WHEN a video frame is decoded and rendered, THE Video_Overlay SHALL verify frame timestamp consistency
4. THE Video_Overlay SHALL validate that audio and video track timestamps remain synchronized within 100ms throughout playback
5. FOR ALL video files, loading then unloading then reloading SHALL produce identical decoder configuration
