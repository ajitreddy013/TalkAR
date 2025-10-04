# TalkAR System Flow Documentation

## Overview

TalkAR is an Augmented Reality application that allows users to point their camera at images and see talking head overlays with lip-sync and head movement.

## Complete System Flow

### 1. Admin Uploads Image + Script (Backend)

- **Endpoint**: `POST /images`
- **Process**:
  - Admin uploads image file via admin dashboard
  - Image stored in backend with metadata
  - Script/dialogue associated with image via `POST /images/:id/dialogues`
- **Storage**: SQLite database with Image and Dialogue models
- **Files**: `backend/src/routes/images.ts`, `backend/src/models/Image.ts`

### 2. Mobile App Image Recognition (ARCore)

- **Process**:
  - User opens mobile app and points camera at image
  - ARCore recognizes image using `ARImageRecognitionService`
  - Image recognition triggers `onImageRecognized` callback
  - Augmented image data passed to `ARViewModel`
- **Files**: `mobile-app/app/src/main/java/com/talkar/app/data/services/ARImageRecognitionService.kt`

### 3. Sync API Integration (Backend)

- **Endpoint**: `POST /sync/generate`
- **Process**:
  - Mobile app calls sync API with recognized image URL
  - Backend integrates with Sync.so API
  - Generates lip-sync video with English language
  - Returns video URL for AR overlay
- **Files**: `backend/src/routes/sync.ts`, `backend/src/services/syncService.ts`

### 4. AR Overlay with Lip-Sync (Mobile)

- **Process**:
  - Sync video positioned over recognized image using ARCore tracking
  - `AROverlay` component handles video positioning
  - Video plays with proper lip-sync and head movement
  - Fallback UI for non-AR display
- **Files**: `mobile-app/app/src/main/java/com/talkar/app/ui/components/AROverlay.kt`

## Key Components

### Backend Components

- **Image Storage**: `backend/src/models/Image.ts`
- **Sync Service**: `backend/src/services/syncService.ts`
- **API Routes**: `backend/src/routes/sync.ts`, `backend/src/routes/images.ts`
- **Validation**: `backend/src/middleware/validation.ts`

### Mobile Components

- **AR Recognition**: `mobile-app/app/src/main/java/com/talkar/app/data/services/ARImageRecognitionService.kt`
- **AR Overlay**: `mobile-app/app/src/main/java/com/talkar/app/ui/components/AROverlay.kt`
- **AR Screen**: `mobile-app/app/src/main/java/com/talkar/app/ui/screens/ARScreen.kt`
- **AR ViewModel**: `mobile-app/app/src/main/java/com/talkar/app/ui/viewmodels/ARViewModel.kt`

## Data Flow

1. **Admin Dashboard** → Uploads image + script → **Backend Database**
2. **Mobile Camera** → ARCore recognition → **Mobile App**
3. **Mobile App** → Calls sync API → **Backend Sync Service**
4. **Backend** → Sync.so API → **Lip-sync video generation**
5. **Mobile App** → AR overlay → **User sees talking head**

## Technical Features

### AR Recognition

- Uses ARCore for image tracking
- AugmentedImage database for reference images
- Real-time image recognition and tracking

### Lip-Sync Integration

- Sync.so API integration for video generation
- English language enforcement
- Head movement and lip-sync parameters
- High-quality video output

### AR Overlay

- Positioned over recognized image using ARCore tracking
- Video overlay with proper scaling and positioning
- Fallback UI for non-AR scenarios
- Real-time video playback

## API Endpoints

### Backend APIs

- `POST /images` - Upload image
- `POST /images/:id/dialogues` - Add script to image
- `POST /sync/generate` - Generate lip-sync video
- `GET /sync/status/:jobId` - Check sync status
- `GET /sync/voices` - Get available voices

### Mobile APIs

- Image recognition via ARCore
- Sync video generation
- AR overlay positioning

## Configuration

### Backend Environment Variables

- `SYNC_API_URL` - Sync.so API endpoint
- `SYNC_API_KEY` - Sync.so API key
- Database configuration for image storage

### Mobile Permissions

- Camera permission for AR
- Internet permission for API calls
- ARCore features enabled

## Error Handling

### Backend

- API validation with Joi schemas
- Sync API error handling
- Image upload validation

### Mobile

- AR initialization error handling
- Network error handling
- Fallback UI for failed recognition

## Performance Considerations

### Backend

- Async video processing
- Job status tracking
- Efficient image storage

### Mobile

- ARCore session management
- Video overlay optimization
- Memory management for AR

## Security

### Backend

- Input validation
- API key management
- File upload security

### Mobile

- Permission management
- Secure API communication
- AR session security

## Future Enhancements

1. **Real-time Translation**: Multi-language support
2. **Voice Cloning**: Custom voice generation
3. **Advanced AR**: 3D model overlays
4. **Analytics**: Usage tracking and insights
5. **Offline Mode**: Local video processing

## Testing

### Backend Tests

- API endpoint testing
- Sync service testing
- Image upload testing

### Mobile Tests

- AR recognition testing
- Video overlay testing
- Performance testing

## Deployment

### Backend

- Docker containerization
- Kubernetes deployment
- Database migration

### Mobile

- Android APK generation
- ARCore compatibility
- Play Store deployment

## Monitoring

### Backend

- API performance monitoring
- Sync service monitoring
- Database performance

### Mobile

- AR performance monitoring
- Video playback monitoring
- User experience tracking
