# TalkAR - UML Sequence Diagrams

## Table of Contents
1. [Main System Sequence Diagram](#1-main-system-sequence-diagram)
2. [Admin Upload Content Flow](#2-admin-upload-content-flow)
3. [Mobile AR Experience Flow](#3-mobile-ar-experience-flow)
4. [User Authentication Flow](#4-user-authentication-flow)
5. [Video Generation Flow](#5-video-generation-flow)
6. [Image Recognition Flow](#6-image-recognition-flow)
7. [Content Sync Flow](#7-content-sync-flow)

---

## 1. Main System Sequence Diagram

**Purpose**: Overall system interaction overview

```mermaid
sequenceDiagram
    actor Admin as 👤 Admin User
    actor MobileUser as 📱 Mobile User
    participant Dashboard as Admin Dashboard
    participant Backend as Backend API
    participant DB as Database
    participant S3 as AWS S3
    participant Mobile as Mobile App
    participant AR as ARCore
    participant Sync as Sync API
    
    %% Admin Flow
    rect rgb(200, 220, 250)
        Note over Admin,S3: Admin Content Upload Flow
        Admin->>Dashboard: Login
        Dashboard->>Backend: POST /auth/login
        Backend->>DB: Validate credentials
        DB-->>Backend: User data
        Backend-->>Dashboard: JWT Token
        
        Admin->>Dashboard: Upload Image + Script
        Dashboard->>Backend: POST /images (multipart)
        Backend->>S3: Upload image file
        S3-->>Backend: Image URL
        Backend->>DB: Save image metadata
        DB-->>Backend: Success
        Backend-->>Dashboard: Image created
    end
    
    %% Mobile User Flow
    rect rgb(220, 250, 220)
        Note over MobileUser,Sync: Mobile AR Experience Flow
        MobileUser->>Mobile: Open App & Scan
        Mobile->>AR: Initialize ARCore
        AR-->>Mobile: Session ready
        
        Mobile->>AR: Detect image
        AR-->>Mobile: Image recognized
        
        Mobile->>Backend: GET /images/:id
        Backend->>DB: Query image data
        DB-->>Backend: Image + Dialogues
        Backend-->>Mobile: Content data
        
        Mobile->>Backend: POST /lipsync/generate
        Backend->>Sync: Generate video
        Sync-->>Backend: Video URL
        Backend-->>Mobile: Video ready
        
        Mobile->>Mobile: Display AR overlay
        Mobile->>MobileUser: Play talking head video
    end
```

**Description**: This diagram shows the complete flow from admin uploading content to mobile users experiencing AR.

---

## 2. Admin Upload Content Flow

**Purpose**: Detailed admin content creation process

```mermaid
sequenceDiagram
    actor Admin as 👤 Admin
    participant UI as Admin Dashboard
    participant API as Backend API
    participant Auth as AuthService
    participant Upload as UploadService
    participant S3 as AWS S3
    participant DB as PostgreSQL
    
    %% Login
    Admin->>UI: Enter credentials
    UI->>API: POST /auth/login
    API->>Auth: validateCredentials(email, password)
    Auth->>DB: SELECT user WHERE email
    DB-->>Auth: User record
    Auth->>Auth: verifyPassword(hash)
    
    alt Valid credentials
        Auth->>Auth: generateToken()
        Auth-->>API: JWT Token
        API-->>UI: 200 OK + Token
        UI-->>Admin: Login success
    else Invalid credentials
        Auth-->>API: 401 Unauthorized
        API-->>UI: Error message
        UI-->>Admin: Show error
    end
    
    %% Upload Image
    Admin->>UI: Select image file
    UI->>UI: Validate file (size, type)
    UI->>API: POST /images (multipart form)
    API->>API: Verify JWT token
    
    API->>Upload: uploadImage(file)
    Upload->>S3: PUT object
    S3-->>Upload: Image URL
    Upload->>Upload: generateThumbnail()
    Upload->>S3: PUT thumbnail
    S3-->>Upload: Thumbnail URL
    Upload-->>API: URLs returned
    
    API->>DB: INSERT INTO images
    DB-->>API: Image ID
    API-->>UI: 201 Created + Image data
    UI-->>Admin: Image uploaded successfully
    
    %% Create Script
    Admin->>UI: Enter dialogue text
    Admin->>UI: Select language & voice
    UI->>API: POST /images/:id/dialogues
    API->>DB: INSERT INTO dialogues
    DB-->>API: Dialogue ID
    API-->>UI: 201 Created
    UI-->>Admin: Script saved
```

**Key Interactions**:
- Admin authentication with JWT
- Image upload to AWS S3
- Thumbnail generation
- Database persistence
- Script/dialogue creation

---

## 3. Mobile AR Experience Flow

**Purpose**: Complete mobile user AR journey

```mermaid
sequenceDiagram
    actor User as 📱 User
    participant App as Mobile App
    participant VM as ARViewModel
    participant Repo as ImageRepository
    participant AR as ARService
    participant ARCore as ARCore SDK
    participant API as Backend API
    participant Cache as Local DB
    
    %% App Start
    User->>App: Open TalkAR App
    App->>App: Check camera permission
    
    alt Permission not granted
        App->>User: Request permission
        User->>App: Grant permission
    end
    
    App->>VM: Initialize
    VM->>Repo: loadImages()
    
    par Fetch from API
        Repo->>API: GET /images
        API-->>Repo: Images list
    and Check cache
        Repo->>Cache: SELECT * FROM images
        Cache-->>Repo: Cached images
    end
    
    Repo-->>VM: Images ready
    
    %% AR Initialization
    VM->>AR: initialize()
    AR->>ARCore: Create session
    ARCore-->>AR: Session created
    AR->>ARCore: Load image database
    ARCore-->>AR: Database loaded
    AR-->>VM: AR ready
    VM-->>App: Update UI state
    
    %% Image Scanning
    User->>App: Point camera at image
    App->>AR: Process camera frame
    
    loop Scanning
        AR->>ARCore: Update session
        ARCore->>ARCore: Detect images
        
        alt Image detected
            ARCore-->>AR: AugmentedImage found
            AR-->>VM: onImageRecognized(imageId)
            VM->>VM: Update state
        end
    end
    
    %% Fetch Content
    VM->>Repo: getImageById(id)
    
    alt Content cached
        Repo->>Cache: Query local DB
        Cache-->>Repo: Cached data
    else Not cached
        Repo->>API: GET /images/:id
        API-->>Repo: Image data
        Repo->>Cache: Store in local DB
    end
    
    Repo-->>VM: Image with dialogues
    
    %% Generate Video
    VM->>Repo: generateVideo(imageId, scriptId)
    Repo->>API: POST /lipsync/generate
    API-->>Repo: Video URL
    Repo-->>VM: Video ready
    
    %% Display AR
    VM->>AR: createAnchor(image)
    AR->>ARCore: Create anchor at pose
    ARCore-->>AR: Anchor created
    AR-->>VM: Anchor ready
    
    VM->>App: Display video overlay
    App->>User: Show talking head in AR
    
    %% Continue tracking
    loop Video playing
        AR->>ARCore: Track image
        ARCore-->>AR: Tracking state
        
        alt Tracking lost
            AR-->>VM: Tracking lost
            VM->>App: Pause video
        else Still tracking
            App->>App: Continue playback
        end
    end
```

**Key Interactions**:
- Permission handling
- ARCore initialization
- Image detection and tracking
- Content caching strategy
- Video overlay in AR space

---

## 4. User Authentication Flow

**Purpose**: Login and token management

```mermaid
sequenceDiagram
    actor User as 👤 User
    participant Client as Client App
    participant API as Backend API
    participant Auth as AuthService
    participant DB as Database
    participant JWT as JWT Library
    
    %% Login Flow
    User->>Client: Enter email & password
    Client->>Client: Validate input format
    Client->>API: POST /auth/login
    
    API->>Auth: login(email, password)
    Auth->>DB: SELECT * FROM users WHERE email
    
    alt User not found
        DB-->>Auth: NULL
        Auth-->>API: Error: User not found
        API-->>Client: 404 Not Found
        Client-->>User: Invalid credentials
    else User found
        DB-->>Auth: User record
        Auth->>Auth: verifyPassword(plaintext, hash)
        
        alt Invalid password
            Auth-->>API: Error: Invalid password
            API->>DB: UPDATE failed_attempts
            API-->>Client: 401 Unauthorized
            Client-->>User: Invalid credentials
        else Valid password
            Auth->>DB: RESET failed_attempts
            Auth->>JWT: sign(payload, secret)
            JWT-->>Auth: Access token
            Auth->>JWT: sign(payload, secret, longExpiry)
            JWT-->>Auth: Refresh token
            
            Auth-->>API: Tokens + User data
            API-->>Client: 200 OK + Tokens
            Client->>Client: Store tokens securely
            Client-->>User: Login successful
        end
    end
    
    %% Token Refresh Flow
    Note over User,JWT: Token Refresh (when expired)
    Client->>API: POST /auth/refresh
    API->>Auth: verifyRefreshToken(token)
    Auth->>JWT: verify(token, secret)
    
    alt Valid refresh token
        JWT-->>Auth: Decoded payload
        Auth->>JWT: sign(newPayload, secret)
        JWT-->>Auth: New access token
        Auth-->>API: New tokens
        API-->>Client: 200 OK + New tokens
        Client->>Client: Update stored tokens
    else Invalid token
        JWT-->>Auth: Error: Invalid
        Auth-->>API: 401 Unauthorized
        API-->>Client: Token expired
        Client-->>User: Please login again
    end
```

**Key Interactions**:
- Email/password validation
- Password hash verification (bcrypt)
- JWT token generation
- Refresh token mechanism
- Failed attempt tracking

---

## 5. Video Generation Flow

**Purpose**: Lip-sync video generation process

```mermaid
sequenceDiagram
    actor User as 📱 User
    participant Mobile as Mobile App
    participant API as Backend API
    participant Sync as SyncService
    participant Cache as Cache/Redis
    participant ExtAPI as Sync API
    participant S3 as AWS S3
    participant DB as Database
    
    %% Request Video
    User->>Mobile: Trigger video generation
    Mobile->>API: POST /lipsync/generate
    Note right of API: Request contains:<br/>text, voiceId,<br/>language, imageId
    
    API->>Sync: generateLipSyncVideo(request)
    
    %% Check Cache
    Sync->>Cache: Check if video exists
    Cache-->>Sync: Cache result
    
    alt Video cached
        Sync->>DB: GET video URL
        DB-->>Sync: Video URL
        Sync-->>API: Return cached URL
        API-->>Mobile: 200 OK + Video URL
        Mobile->>Mobile: Load and play video
    else Video not cached
        %% Generate new video
        Sync->>Sync: Validate request
        Sync->>Sync: Generate unique videoId
        
        Sync->>ExtAPI: POST /generate (with params)
        ExtAPI->>ExtAPI: Process request
        ExtAPI-->>Sync: Job ID + Status: processing
        
        Sync->>DB: INSERT job (status: processing)
        Sync-->>API: 202 Accepted + Job ID
        API-->>Mobile: Processing started
        
        %% Poll for status
        Mobile->>Mobile: Start polling
        
        loop Poll every 2 seconds
            Mobile->>API: GET /lipsync/status/:videoId
            API->>Sync: getVideoStatus(videoId)
            Sync->>ExtAPI: GET /jobs/:jobId
            ExtAPI-->>Sync: Job status
            
            alt Still processing
                Sync-->>API: Status: processing
                API-->>Mobile: Still processing
                Mobile->>Mobile: Wait 2 seconds
            else Processing failed
                Sync-->>API: Status: failed
                API-->>Mobile: Generation failed
                Mobile->>User: Show error
            else Processing complete
                ExtAPI-->>Sync: Video URL
                Sync->>S3: Download and store
                S3-->>Sync: S3 URL
                Sync->>DB: UPDATE job (url, status: completed)
                Sync->>Cache: Store URL (TTL: 24h)
                Sync-->>API: Status: completed + URL
                API-->>Mobile: Video ready + URL
                Mobile->>Mobile: Download and play
                Mobile->>User: Display video
            end
        end
    end
```

**Key Interactions**:
- Cache checking for performance
- Async video generation
- Job status polling
- S3 storage for videos
- Error handling with retries

---

## 6. Image Recognition Flow

**Purpose**: AR image detection and tracking

```mermaid
sequenceDiagram
    actor User as 📱 User
    participant App as Mobile App
    participant AR as ARService
    participant ARCore as ARCore SDK
    participant MLKit as ML Kit
    participant VM as ARViewModel
    participant Repo as Repository
    
    %% Initialize AR
    User->>App: Open AR scanner
    App->>AR: initialize()
    AR->>ARCore: Check availability
    
    alt ARCore available
        ARCore-->>AR: Available
        AR->>ARCore: Create session
        ARCore-->>AR: Session created
        
        AR->>ARCore: Configure session
        Note right of AR: Set focus mode,<br/>update mode,<br/>plane detection
        ARCore-->>AR: Configured
        
        AR->>Repo: Get image list
        Repo-->>AR: Image URLs
        AR->>AR: Build image database
        AR->>ARCore: setImageDatabase(db)
        ARCore-->>AR: Database loaded
        
    else ARCore not available
        ARCore-->>AR: Not available
        AR->>MLKit: Initialize ML Kit (fallback)
        MLKit-->>AR: Ready
    end
    
    AR-->>App: AR initialized
    App->>AR: Start camera
    
    %% Scanning Loop
    loop Camera frames
        User->>App: Point camera at image
        App->>AR: Process frame
        AR->>ARCore: Update session
        ARCore->>ARCore: Analyze frame
        
        alt Using ARCore
            ARCore->>ARCore: Match against database
            
            alt Image detected
                ARCore->>ARCore: Create AugmentedImage
                ARCore-->>AR: Image found + tracking state
                AR->>AR: Check tracking quality
                
                alt Good tracking
                    AR->>ARCore: Get image pose
                    ARCore-->>AR: Position + orientation
                    AR->>ARCore: Create anchor
                    ARCore-->>AR: Anchor created
                    AR->>VM: onImageRecognized(imageData)
                    VM->>VM: Update state
                    VM-->>App: Show AR overlay
                    App-->>User: Display talking head
                else Poor tracking
                    AR-->>App: Show tracking hint
                    App-->>User: "Keep image in view"
                end
            else No image detected
                ARCore-->>AR: No matches
                AR-->>App: Continue scanning
            end
            
        else Using ML Kit (fallback)
            AR->>MLKit: labelImage(bitmap)
            MLKit->>MLKit: Analyze image
            MLKit-->>AR: Labels + confidence
            
            AR->>AR: Match labels with database
            
            alt Match found (confidence > 70%)
                AR->>VM: onImageRecognized(imageData)
                VM-->>App: Show content
                App-->>User: Display info
            else No confident match
                AR-->>App: Continue scanning
            end
        end
    end
    
    %% Tracking Lost
    Note over User,Repo: When tracking is lost
    ARCore-->>AR: Tracking state: stopped
    AR->>VM: onTrackingLost()
    VM->>App: Pause video
    App-->>User: Resume scanning
```

**Key Interactions**:
- ARCore initialization with fallback
- Image database loading
- Frame-by-frame processing
- Tracking quality assessment
- Anchor creation for AR overlay
- ML Kit fallback mechanism

---

## 7. Content Sync Flow

**Purpose**: Mobile app data synchronization

```mermaid
sequenceDiagram
    actor User as 📱 User
    participant App as Mobile App
    participant Repo as ImageRepository
    participant API as Backend API
    participant Cache as Local DB (Room)
    participant Network as Network Layer
    
    %% Sync Trigger
    alt App Launch
        User->>App: Open app
        App->>Repo: syncImages()
    else Manual Refresh
        User->>App: Pull to refresh
        App->>Repo: syncImages(force: true)
    else Background Sync
        App->>App: Background timer triggered
        App->>Repo: syncImages()
    end
    
    %% Check Network
    Repo->>Network: Check connectivity
    
    alt No network
        Network-->>Repo: Offline
        Repo->>Cache: Load cached data
        Cache-->>Repo: Cached images
        Repo-->>App: Return cached data
        App-->>User: Show offline indicator
    else Network available
        Network-->>Repo: Online
        
        %% Get local version
        Repo->>Cache: Get last sync timestamp
        Cache-->>Repo: Last sync time
        
        %% Call API
        Repo->>API: GET /images?since=timestamp
        
        alt API Success
            API->>API: Get updated images
            API-->>Repo: Updated images list
            
            Repo->>Repo: Compare with local data
            
            par Update local database
                Repo->>Cache: BEGIN TRANSACTION
                
                loop For each updated image
                    Repo->>Cache: INSERT OR REPLACE
                end
                
                loop For each deleted image
                    Repo->>Cache: DELETE
                end
                
                Repo->>Cache: UPDATE sync_metadata
                Repo->>Cache: COMMIT TRANSACTION
                Cache-->>Repo: Success
                
            and Download media
                loop For each new image
                    Repo->>API: Download image file
                    API-->>Repo: Image bytes
                    Repo->>Cache: Store locally
                end
            end
            
            Repo-->>App: Sync complete + new data
            App-->>User: Show updated content
            
        else API Error
            alt Network error
                API-->>Repo: Network timeout
                Repo->>Cache: Load cached data
                Repo-->>App: Using cached data
                App-->>User: Show offline mode
            else Server error
                API-->>Repo: 500 Server error
                Repo-->>App: Sync failed
                App-->>User: Show error + retry
            else Auth error
                API-->>Repo: 401 Unauthorized
                Repo->>Repo: Clear auth token
                Repo-->>App: Session expired
                App-->>User: Redirect to login
            end
        end
    end
    
    %% Background Cleanup
    Note over Repo,Cache: Clean up old cached data
    Repo->>Cache: DELETE expired entries
    Cache-->>Repo: Cleanup complete
```

**Key Interactions**:
- Multiple sync triggers (launch, manual, background)
- Network connectivity check
- Incremental sync with timestamps
- Database transactions for consistency
- Media file downloads
- Error handling for offline scenarios

---

## Sequence Diagram Summary

| # | Diagram | Participants | Key Flow |
|---|---------|--------------|----------|
| 1 | **Main System** | 8 | Admin uploads → User AR experience |
| 2 | **Admin Upload** | 6 | Login → Upload → Store → Save |
| 3 | **Mobile AR** | 7 | Initialize → Scan → Detect → Display |
| 4 | **Authentication** | 5 | Login → Validate → Generate tokens |
| 5 | **Video Generation** | 7 | Request → Generate → Poll → Complete |
| 6 | **Image Recognition** | 6 | Initialize → Scan → Detect → Track |
| 7 | **Content Sync** | 5 | Trigger → Fetch → Update → Complete |

---

## Key Patterns Used

### 1. **Request-Response Pattern**
```
Client → Server: Request
Server → Client: Response
```

### 2. **Async Processing with Polling**
```
Client → Server: Start job
Server → Client: Job ID
loop
    Client → Server: Check status
    Server → Client: Status update
end
```

### 3. **Cache-Aside Pattern**
```
Client → Cache: Check
alt Hit
    Cache → Client: Data
else Miss
    Client → API: Fetch
    Client → Cache: Store
end
```

### 4. **Fallback Pattern**
```
Client → Primary: Try
alt Success
    Primary → Client: Result
else Failure
    Client → Fallback: Try alternative
end
```

---

## How to Use These Diagrams

### 🚀 View in Mermaid Live:
1. **Visit**: https://mermaid.live
2. **Copy**: Any diagram code above
3. **Paste**: Into editor
4. **View**: Sequence diagram renders!
5. **Export**: PNG/SVG for documentation

### 📂 GitHub:
```bash
git add SEQUENCE_DIAGRAMS.md
git commit -m "Add UML sequence diagrams"
git push
# Auto-renders on GitHub!
```

### 💻 VS Code:
1. Install "Markdown Preview Mermaid Support"
2. Open `SEQUENCE_DIAGRAMS.md`
3. Press `Ctrl+Shift+V`
4. View all diagrams

---

## Diagram Notation

### Participants
- **actor**: User or external entity
- **participant**: System component

### Messages
- `→`: Synchronous call
- `-->>`: Return/response
- `->>`: Asynchronous message

### Fragments
- **alt**: Alternative paths (if/else)
- **opt**: Optional execution
- **loop**: Repeated execution
- **par**: Parallel execution
- **rect**: Grouping for context

---

**Perfect for**: Understanding system interactions, API documentation, developer onboarding!

**Created**: October 8, 2025  
**Standard**: UML 2.0 Sequence Diagrams  
**Format**: Mermaid sequenceDiagram syntax
