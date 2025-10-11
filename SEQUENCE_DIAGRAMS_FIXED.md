# TalkAR - UML Sequence Diagrams (Fixed)

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
    participant Admin
    participant Dashboard
    participant Backend
    participant Database
    participant S3
    participant MobileUser
    participant MobileApp
    participant ARCore
    participant SyncAPI
    
    Note over Admin,S3: Admin Content Upload Flow
    Admin->>Dashboard: Login
    Dashboard->>Backend: POST /auth/login
    Backend->>Database: Validate credentials
    Database-->>Backend: User data
    Backend-->>Dashboard: JWT Token
    
    Admin->>Dashboard: Upload Image
    Dashboard->>Backend: POST /images
    Backend->>S3: Upload file
    S3-->>Backend: Image URL
    Backend->>Database: Save metadata
    Database-->>Backend: Success
    Backend-->>Dashboard: Image created
    
    Note over MobileUser,SyncAPI: Mobile AR Experience Flow
    MobileUser->>MobileApp: Open app and scan
    MobileApp->>ARCore: Initialize AR
    ARCore-->>MobileApp: AR ready
    
    MobileApp->>ARCore: Detect image
    ARCore-->>MobileApp: Image recognized
    
    MobileApp->>Backend: GET /images/:id
    Backend->>Database: Query data
    Database-->>Backend: Image data
    Backend-->>MobileApp: Content
    
    MobileApp->>Backend: POST /lipsync/generate
    Backend->>SyncAPI: Generate video
    SyncAPI-->>Backend: Video URL
    Backend-->>MobileApp: Video ready
    
    MobileApp->>MobileUser: Display AR video
```

---

## 2. Admin Upload Content Flow

**Purpose**: Detailed admin content creation process

```mermaid
sequenceDiagram
    participant Admin
    participant Dashboard
    participant API
    participant Auth
    participant Upload
    participant S3
    participant DB
    
    Admin->>Dashboard: Enter credentials
    Dashboard->>API: POST /auth/login
    API->>Auth: validateCredentials()
    Auth->>DB: SELECT user
    DB-->>Auth: User record
    Auth->>Auth: verifyPassword()
    
    alt Valid credentials
        Auth->>Auth: generateToken()
        Auth-->>API: JWT Token
        API-->>Dashboard: 200 OK
        Dashboard-->>Admin: Login success
    else Invalid credentials
        Auth-->>API: 401 Error
        API-->>Dashboard: Error
        Dashboard-->>Admin: Login failed
    end
    
    Admin->>Dashboard: Select image file
    Dashboard->>Dashboard: Validate file
    Dashboard->>API: POST /images
    API->>API: Verify token
    
    API->>Upload: uploadImage()
    Upload->>S3: PUT object
    S3-->>Upload: Image URL
    Upload->>Upload: generateThumbnail()
    Upload->>S3: PUT thumbnail
    S3-->>Upload: Thumbnail URL
    Upload-->>API: URLs
    
    API->>DB: INSERT image
    DB-->>API: Image ID
    API-->>Dashboard: 201 Created
    Dashboard-->>Admin: Upload success
    
    Admin->>Dashboard: Enter script text
    Dashboard->>API: POST /dialogues
    API->>DB: INSERT dialogue
    DB-->>API: Dialogue ID
    API-->>Dashboard: 201 Created
    Dashboard-->>Admin: Script saved
```

---

## 3. Mobile AR Experience Flow

**Purpose**: Complete mobile user AR journey

```mermaid
sequenceDiagram
    participant User
    participant App
    participant ViewModel
    participant Repository
    participant ARService
    participant ARCore
    participant API
    participant Cache
    
    User->>App: Open TalkAR
    App->>App: Check permission
    
    alt Permission not granted
        App->>User: Request permission
        User->>App: Grant
    end
    
    App->>ViewModel: initialize()
    ViewModel->>Repository: loadImages()
    Repository->>API: GET /images
    API-->>Repository: Images
    Repository->>Cache: Store
    Repository-->>ViewModel: Data ready
    
    ViewModel->>ARService: initialize()
    ARService->>ARCore: Create session
    ARCore-->>ARService: Session ready
    ARService->>ARCore: Load image database
    ARCore-->>ARService: Database loaded
    ARService-->>ViewModel: AR ready
    
    User->>App: Point camera
    App->>ARService: Process frame
    
    loop Scanning
        ARService->>ARCore: Update session
        ARCore->>ARCore: Detect images
        
        alt Image detected
            ARCore-->>ARService: Image found
            ARService-->>ViewModel: onImageRecognized()
        end
    end
    
    ViewModel->>Repository: getImageById()
    
    alt Cached
        Repository->>Cache: Query
        Cache-->>Repository: Data
    else Not cached
        Repository->>API: GET /images/:id
        API-->>Repository: Data
        Repository->>Cache: Store
    end
    
    Repository-->>ViewModel: Image data
    
    ViewModel->>Repository: generateVideo()
    Repository->>API: POST /lipsync/generate
    API-->>Repository: Video URL
    Repository-->>ViewModel: Ready
    
    ViewModel->>ARService: createAnchor()
    ARService->>ARCore: Create anchor
    ARCore-->>ARService: Anchor created
    
    ViewModel->>App: Display overlay
    App->>User: Show talking head
```

---

## 4. User Authentication Flow

**Purpose**: Login and token management

```mermaid
sequenceDiagram
    participant User
    participant Client
    participant API
    participant AuthService
    participant Database
    participant JWT
    
    User->>Client: Enter credentials
    Client->>Client: Validate format
    Client->>API: POST /auth/login
    
    API->>AuthService: login()
    AuthService->>Database: SELECT user
    
    alt User not found
        Database-->>AuthService: NULL
        AuthService-->>API: Error
        API-->>Client: 404 Not Found
        Client-->>User: Invalid credentials
    else User found
        Database-->>AuthService: User record
        AuthService->>AuthService: verifyPassword()
        
        alt Invalid password
            AuthService-->>API: Error
            API->>Database: UPDATE failed_attempts
            API-->>Client: 401 Unauthorized
            Client-->>User: Invalid credentials
        else Valid password
            AuthService->>Database: RESET attempts
            AuthService->>JWT: sign()
            JWT-->>AuthService: Access token
            AuthService->>JWT: sign()
            JWT-->>AuthService: Refresh token
            
            AuthService-->>API: Tokens
            API-->>Client: 200 OK
            Client->>Client: Store tokens
            Client-->>User: Login success
        end
    end
    
    Note over User,JWT: Token Refresh Flow
    Client->>API: POST /auth/refresh
    API->>AuthService: verifyRefreshToken()
    AuthService->>JWT: verify()
    
    alt Valid token
        JWT-->>AuthService: Payload
        AuthService->>JWT: sign()
        JWT-->>AuthService: New token
        AuthService-->>API: New tokens
        API-->>Client: 200 OK
        Client->>Client: Update tokens
    else Invalid token
        JWT-->>AuthService: Error
        AuthService-->>API: 401
        API-->>Client: Expired
        Client-->>User: Login again
    end
```

---

## 5. Video Generation Flow

**Purpose**: Lip-sync video generation process

```mermaid
sequenceDiagram
    participant User
    participant Mobile
    participant API
    participant SyncService
    participant Cache
    participant SyncAPI
    participant S3
    participant DB
    
    User->>Mobile: Trigger video
    Mobile->>API: POST /lipsync/generate
    
    API->>SyncService: generateVideo()
    
    SyncService->>Cache: Check cache
    
    alt Video cached
        Cache-->>SyncService: URL
        SyncService->>DB: Get URL
        DB-->>SyncService: Video URL
        SyncService-->>API: Cached URL
        API-->>Mobile: 200 OK
        Mobile->>Mobile: Play video
    else Not cached
        SyncService->>SyncService: Validate request
        SyncService->>SyncService: Generate ID
        
        SyncService->>SyncAPI: POST /generate
        SyncAPI->>SyncAPI: Process
        SyncAPI-->>SyncService: Job ID
        
        SyncService->>DB: INSERT job
        SyncService-->>API: 202 Accepted
        API-->>Mobile: Processing
        
        Mobile->>Mobile: Start polling
        
        loop Poll every 2 seconds
            Mobile->>API: GET /status/:id
            API->>SyncService: getStatus()
            SyncService->>SyncAPI: GET /jobs/:id
            SyncAPI-->>SyncService: Status
            
            alt Processing
                SyncService-->>API: Processing
                API-->>Mobile: Still processing
            else Failed
                SyncService-->>API: Failed
                API-->>Mobile: Error
                Mobile->>User: Show error
            else Completed
                SyncAPI-->>SyncService: Video URL
                SyncService->>S3: Store video
                S3-->>SyncService: S3 URL
                SyncService->>DB: UPDATE job
                SyncService->>Cache: Store URL
                SyncService-->>API: Complete
                API-->>Mobile: Video ready
                Mobile->>Mobile: Play
                Mobile->>User: Display
            end
        end
    end
```

---

## 6. Image Recognition Flow

**Purpose**: AR image detection and tracking

```mermaid
sequenceDiagram
    participant User
    participant App
    participant ARService
    participant ARCore
    participant MLKit
    participant ViewModel
    participant Repo
    
    User->>App: Open scanner
    App->>ARService: initialize()
    ARService->>ARCore: Check availability
    
    alt ARCore available
        ARCore-->>ARService: Available
        ARService->>ARCore: Create session
        ARCore-->>ARService: Session created
        
        ARService->>ARCore: Configure
        ARCore-->>ARService: Configured
        
        ARService->>Repo: Get images
        Repo-->>ARService: Image list
        ARService->>ARService: Build database
        ARService->>ARCore: Set database
        ARCore-->>ARService: Ready
        
    else Not available
        ARCore-->>ARService: Unavailable
        ARService->>MLKit: Initialize
        MLKit-->>ARService: Ready
    end
    
    ARService-->>App: Initialized
    App->>ARService: Start camera
    
    loop Scan frames
        User->>App: Point camera
        App->>ARService: Process frame
        ARService->>ARCore: Update session
        ARCore->>ARCore: Analyze
        
        alt Using ARCore
            ARCore->>ARCore: Match database
            
            alt Image detected
                ARCore->>ARCore: Create augmented image
                ARCore-->>ARService: Image found
                ARService->>ARService: Check tracking
                
                alt Good tracking
                    ARService->>ARCore: Get pose
                    ARCore-->>ARService: Position
                    ARService->>ARCore: Create anchor
                    ARCore-->>ARService: Anchor
                    ARService->>ViewModel: onImageRecognized()
                    ViewModel->>App: Update UI
                    App->>User: Show overlay
                else Poor tracking
                    ARService-->>App: Show hint
                    App-->>User: Keep in view
                end
            else No match
                ARCore-->>ARService: No match
                ARService-->>App: Continue scan
            end
            
        else Using MLKit
            ARService->>MLKit: Label image
            MLKit->>MLKit: Analyze
            MLKit-->>ARService: Labels
            
            ARService->>ARService: Match database
            
            alt Match found
                ARService->>ViewModel: onImageRecognized()
                ViewModel->>App: Update
                App->>User: Show content
            else No match
                ARService-->>App: Continue
            end
        end
    end
```

---

## 7. Content Sync Flow

**Purpose**: Mobile app data synchronization

```mermaid
sequenceDiagram
    participant User
    participant App
    participant Repo
    participant API
    participant Cache
    participant Network
    
    alt App Launch
        User->>App: Open app
        App->>Repo: syncImages()
    else Manual Refresh
        User->>App: Pull refresh
        App->>Repo: syncImages()
    else Background
        App->>App: Timer
        App->>Repo: syncImages()
    end
    
    Repo->>Network: Check connection
    
    alt Offline
        Network-->>Repo: No network
        Repo->>Cache: Load cached
        Cache-->>Repo: Data
        Repo-->>App: Cached data
        App-->>User: Show offline
    else Online
        Network-->>Repo: Connected
        
        Repo->>Cache: Get last sync
        Cache-->>Repo: Timestamp
        
        Repo->>API: GET /images
        
        alt Success
            API-->>Repo: Image list
            
            Repo->>Repo: Compare data
            
            Repo->>Cache: BEGIN TRANSACTION
            
            loop Each image
                Repo->>Cache: INSERT OR REPLACE
            end
            
            loop Each deleted
                Repo->>Cache: DELETE
            end
            
            Repo->>Cache: UPDATE metadata
            Repo->>Cache: COMMIT
            Cache-->>Repo: Success
            
            loop Each new image
                Repo->>API: Download file
                API-->>Repo: File
                Repo->>Cache: Store
            end
            
            Repo-->>App: Sync complete
            App-->>User: Updated
            
        else Error
            alt Network error
                API-->>Repo: Timeout
                Repo->>Cache: Load cached
                Repo-->>App: Cached
                App-->>User: Offline mode
            else Server error
                API-->>Repo: 500 Error
                Repo-->>App: Failed
                App-->>User: Show error
            else Auth error
                API-->>Repo: 401
                Repo->>Repo: Clear token
                Repo-->>App: Expired
                App-->>User: Login
            end
        end
    end
    
    Repo->>Cache: DELETE expired
    Cache-->>Repo: Cleaned
```

---

## Sequence Diagram Summary

| # | Diagram | Participants | Purpose |
|---|---------|--------------|---------|
| 1 | Main System | 9 | Overall system flow |
| 2 | Admin Upload | 7 | Content creation |
| 3 | Mobile AR | 8 | AR experience |
| 4 | Authentication | 6 | Login & tokens |
| 5 | Video Generation | 8 | Async video processing |
| 6 | Image Recognition | 7 | AR detection |
| 7 | Content Sync | 6 | Data synchronization |

---

## Key Patterns

### Request-Response
```
Client->>Server: Request
Server-->>Client: Response
```

### Async with Polling
```
Client->>Server: Start job
Server-->>Client: Job ID
loop
    Client->>Server: Check status
    Server-->>Client: Status
end
```

### Cache Pattern
```
Client->>Cache: Check
alt Hit
    Cache-->>Client: Data
else Miss
    Client->>API: Fetch
    Client->>Cache: Store
end
```

---

## How to Use

### 🚀 Mermaid Live (30 seconds):
1. Visit: **https://mermaid.live**
2. Copy any diagram above
3. Paste in editor
4. Export as PNG/SVG

### 📂 GitHub:
```bash
git add SEQUENCE_DIAGRAMS_FIXED.md
git commit -m "Add fixed sequence diagrams"
git push
```

### 💻 VS Code:
1. Install "Markdown Preview Mermaid Support"
2. Open file
3. Press `Ctrl+Shift+V`

---

## Message Types

| Symbol | Meaning |
|--------|---------|
| `->>` | Synchronous call |
| `-->>` | Return/response |
| `->>+` | Call with activation |
| `-->>-` | Return with deactivation |

---

## Fragment Types

| Fragment | Purpose |
|----------|---------|
| `alt/else` | Conditional |
| `opt` | Optional |
| `loop` | Repetition |
| `par` | Parallel |

---

**Created**: October 8, 2025  
**Format**: UML Sequence Diagrams (Mermaid)  
**Status**: Syntax Fixed ✅
