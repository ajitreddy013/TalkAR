# TalkAR - Simple Activity Diagrams

## Table of Contents
1. [Main Activity Diagram (Overall System)](#main-activity-diagram-overall-system)
2. [Module 1: Admin Dashboard](#module-1-admin-dashboard)
3. [Module 2: Mobile AR App](#module-2-mobile-ar-app)
4. [Module 3: Backend API](#module-3-backend-api)

---

## Main Activity Diagram (Overall System)

**Purpose**: High-level overview of the entire TalkAR system

```mermaid
flowchart TD
    Start([Start TalkAR]) --> UserType{User Type?}
    
    %% Admin Path
    UserType -->|Admin| AdminLogin[Admin Login]
    AdminLogin --> AdminDashboard[Access Dashboard]
    AdminDashboard --> AdminTask[Upload Images & Scripts]
    AdminTask --> SaveContent[(Save to Database)]
    SaveContent --> AdminEnd([Admin Done])
    
    %% Mobile User Path
    UserType -->|Mobile User| OpenApp[Open Mobile App]
    OpenApp --> GrantPermission[Grant Camera Permission]
    GrantPermission --> ScanImage[Scan Image with Camera]
    ScanImage --> ImageFound{Image Recognized?}
    
    ImageFound -->|No| ScanImage
    ImageFound -->|Yes| FetchData[Fetch Content from API]
    
    FetchData --> GenerateVideo[Generate Lip-Sync Video]
    GenerateVideo --> DisplayAR[Display AR Overlay]
    DisplayAR --> PlayVideo[Play Talking Head]
    PlayVideo --> MobileEnd([Mobile Done])
    
    style Start fill:#4CAF50,stroke:#2E7D32,stroke-width:3px,color:#fff
    style AdminEnd fill:#f44336,stroke:#c62828,stroke-width:3px,color:#fff
    style MobileEnd fill:#f44336,stroke:#c62828,stroke-width:3px,color:#fff
    style AdminDashboard fill:#2196F3,stroke:#1565C0,stroke-width:2px,color:#fff
    style DisplayAR fill:#FF9800,stroke:#E65100,stroke-width:2px,color:#fff
    style PlayVideo fill:#9C27B0,stroke:#6A1B9A,stroke-width:2px,color:#fff
```

**Description**: This is the main flow showing how admins create content and mobile users experience AR.

---

## Module 1: Admin Dashboard

**Purpose**: Admin content management workflow

```mermaid
flowchart TD
    Start([Admin Opens Dashboard]) --> Login[Enter Credentials]
    Login --> ValidAuth{Valid Login?}
    
    ValidAuth -->|No| Login
    ValidAuth -->|Yes| Dashboard[View Dashboard]
    
    Dashboard --> SelectAction{Choose Action}
    
    %% Upload Image
    SelectAction -->|Upload Image| ChooseFile[Select Image File]
    ChooseFile --> UploadImage[Upload to Cloud Storage]
    UploadImage --> SaveImageDB[(Save Image Metadata)]
    SaveImageDB --> Dashboard
    
    %% Create Script
    SelectAction -->|Create Script| EnterText[Enter Dialogue Text]
    EnterText --> SelectLanguage[Choose Language & Voice]
    SelectLanguage --> SaveScriptDB[(Save Script to DB)]
    SaveScriptDB --> Dashboard
    
    %% Manage Avatars
    SelectAction -->|Manage Avatar| ConfigureAvatar[Configure Avatar Settings]
    ConfigureAvatar --> SaveAvatarDB[(Save Avatar Config)]
    SaveAvatarDB --> Dashboard
    
    %% View Analytics
    SelectAction -->|View Analytics| ShowStats[Display Usage Statistics]
    ShowStats --> Dashboard
    
    %% Logout
    SelectAction -->|Logout| End([Logout & Exit])
    
    style Start fill:#4CAF50,stroke:#2E7D32,stroke-width:3px,color:#fff
    style End fill:#f44336,stroke:#c62828,stroke-width:3px,color:#fff
    style Dashboard fill:#2196F3,stroke:#1565C0,stroke-width:2px,color:#fff
    style SaveImageDB fill:#9C27B0,stroke:#6A1B9A,stroke-width:2px
    style SaveScriptDB fill:#9C27B0,stroke:#6A1B9A,stroke-width:2px
    style SaveAvatarDB fill:#9C27B0,stroke:#6A1B9A,stroke-width:2px
```

**Key Activities**:
- Login authentication
- Upload images to cloud
- Create scripts/dialogues
- Configure avatars
- View analytics

---

## Module 2: Mobile AR App

**Purpose**: Mobile user AR experience workflow

```mermaid
flowchart TD
    Start([Open TalkAR App]) --> CheckPermission{Camera Permission?}
    
    CheckPermission -->|No| RequestPermission[Request Permission]
    RequestPermission --> CheckPermission
    
    CheckPermission -->|Yes| InitAR[Initialize ARCore]
    InitAR --> StartCamera[Start Camera]
    StartCamera --> Scanning[Scan Environment]
    
    Scanning --> Detected{Image Detected?}
    Detected -->|No| Scanning
    
    Detected -->|Yes| TrackImage[Track Image Position]
    TrackImage --> FetchAPI[Fetch Data from Backend API]
    
    FetchAPI --> CheckCache{Content Cached?}
    CheckCache -->|Yes| LoadCache[Load from Cache]
    CheckCache -->|No| Download[Download Content]
    Download --> SaveCache[(Save to Cache)]
    SaveCache --> LoadCache
    
    LoadCache --> RequestVideo[Request Lip-Sync Video]
    RequestVideo --> VideoReady{Video Ready?}
    
    VideoReady -->|No| WaitGenerate[Wait for Generation]
    WaitGenerate --> VideoReady
    
    VideoReady -->|Yes| CreateOverlay[Create AR Overlay]
    CreateOverlay --> PlayVideo[Play Talking Head Video]
    
    PlayVideo --> UserChoice{User Action?}
    UserChoice -->|Scan Again| Scanning
    UserChoice -->|Change Language| RequestVideo
    UserChoice -->|Exit| End([Close App])
    
    style Start fill:#4CAF50,stroke:#2E7D32,stroke-width:3px,color:#fff
    style End fill:#f44336,stroke:#c62828,stroke-width:3px,color:#fff
    style InitAR fill:#FF9800,stroke:#E65100,stroke-width:2px,color:#fff
    style CreateOverlay fill:#2196F3,stroke:#1565C0,stroke-width:2px,color:#fff
    style PlayVideo fill:#9C27B0,stroke:#6A1B9A,stroke-width:2px,color:#fff
```

**Key Activities**:
- Permission handling
- ARCore initialization
- Image scanning & detection
- Content caching
- Video generation
- AR overlay display

---

## Module 3: Backend API

**Purpose**: Backend processing workflow

```mermaid
flowchart TD
    Start([API Request Received]) --> ValidateRequest{Valid Request?}
    
    ValidateRequest -->|No| ReturnError[Return 400 Error]
    ReturnError --> End([End])
    
    ValidateRequest -->|Yes| CheckAuth{Authenticated?}
    CheckAuth -->|No| Return401[Return 401 Unauthorized]
    Return401 --> End
    
    CheckAuth -->|Yes| RouteRequest{Request Type?}
    
    %% Get Images
    RouteRequest -->|GET Images| QueryDB[(Query Database)]
    QueryDB --> ReturnImages[Return Images List]
    ReturnImages --> End
    
    %% Upload Image
    RouteRequest -->|POST Image| ValidateFile{Valid File?}
    ValidateFile -->|No| ReturnError
    ValidateFile -->|Yes| UploadS3[Upload to AWS S3]
    UploadS3 --> GetURL[Get S3 URL]
    GetURL --> SaveDB[(Save to Database)]
    SaveDB --> ReturnSuccess[Return Success Response]
    ReturnSuccess --> End
    
    %% Generate Video
    RouteRequest -->|Generate Video| CheckVideoCache{Video Cached?}
    CheckVideoCache -->|Yes| ReturnCached[Return Cached URL]
    ReturnCached --> End
    
    CheckVideoCache -->|No| CallSyncAPI[Call Sync API]
    CallSyncAPI --> VideoGenerated{Success?}
    
    VideoGenerated -->|No| ReturnVideoError[Return Generation Error]
    ReturnVideoError --> End
    
    VideoGenerated -->|Yes| StoreVideo[(Store Video URL)]
    StoreVideo --> ReturnVideoURL[Return Video URL]
    ReturnVideoURL --> End
    
    %% Analytics
    RouteRequest -->|Analytics| CollectMetrics[Collect Metrics]
    CollectMetrics --> AggregateData[Aggregate Data]
    AggregateData --> ReturnStats[Return Statistics]
    ReturnStats --> End
    
    style Start fill:#4CAF50,stroke:#2E7D32,stroke-width:3px,color:#fff
    style End fill:#f44336,stroke:#c62828,stroke-width:3px,color:#fff
    style QueryDB fill:#9C27B0,stroke:#6A1B9A,stroke-width:2px
    style SaveDB fill:#9C27B0,stroke:#6A1B9A,stroke-width:2px
    style StoreVideo fill:#9C27B0,stroke:#6A1B9A,stroke-width:2px
    style CallSyncAPI fill:#FF9800,stroke:#E65100,stroke-width:2px,color:#fff
```

**Key Activities**:
- Request validation
- Authentication check
- Database operations
- File upload to S3
- Video generation via Sync API
- Analytics processing

---

## Diagram Summary

### Overview

| Diagram | Purpose | Key Steps |
|---------|---------|-----------|
| **Main Activity** | Overall system flow | Admin uploads → User scans → AR displays |
| **Admin Dashboard** | Content management | Login → Upload → Create → Configure |
| **Mobile AR App** | User experience | Scan → Detect → Fetch → Display |
| **Backend API** | Server processing | Validate → Process → Store → Return |

---

## Activity Flow Summary

### 1. Main System Flow
```
Admin: Login → Upload Content → Save
Mobile: Open App → Scan → Recognize → Play Video
```

### 2. Admin Module Flow
```
Login → Dashboard → Action (Upload/Script/Avatar/Analytics) → Save → Dashboard
```

### 3. Mobile Module Flow
```
Permissions → Initialize AR → Scan → Detect → Fetch → Generate Video → Display AR
```

### 4. Backend Module Flow
```
Receive Request → Validate → Authenticate → Route → Process → Return Response
```

---

## Simple Workflow Descriptions

### Admin Workflow
1. **Login** to dashboard
2. **Choose** action (upload/create/manage)
3. **Perform** action
4. **Save** to database
5. **Continue** or logout

### Mobile User Workflow
1. **Open** app
2. **Grant** camera permission
3. **Scan** environment
4. **Recognize** image
5. **Fetch** content
6. **Display** AR video
7. **Watch** talking head

### Backend Workflow
1. **Receive** API request
2. **Validate** request data
3. **Authenticate** user
4. **Process** request
5. **Return** response

---

## How to Use These Diagrams

### 🚀 Quick View (30 seconds):
1. **Visit**: https://mermaid.live
2. **Copy** any diagram code above
3. **Paste** into editor
4. **View** rendered diagram
5. **Export** as PNG/SVG

### 📂 Add to GitHub:
```bash
git add SIMPLE_ACTIVITY_DIAGRAMS.md
git commit -m "Add simple activity diagrams"
git push
# Auto-renders on GitHub!
```

### 💻 View in VS Code:
1. Install "Markdown Preview Mermaid Support"
2. Open `SIMPLE_ACTIVITY_DIAGRAMS.md`
3. Press `Ctrl+Shift+V`
4. View all diagrams

---

## Color Legend

| Color | Meaning |
|-------|---------|
| 🟢 **Green** | Start point |
| 🔴 **Red** | End point |
| 🔵 **Blue** | Important activity |
| 🟠 **Orange** | External service call |
| 🟣 **Purple** | Database operation |

---

## Benefits of These Diagrams

✅ **Simple** - Easy to understand at a glance  
✅ **Clean** - No unnecessary complexity  
✅ **Modular** - Each module separate  
✅ **Complete** - Covers all major flows  
✅ **Professional** - UML standard notation  
✅ **Shareable** - Perfect for presentations  

---

## Quick Stats

| Metric | Value |
|--------|-------|
| **Total Diagrams** | 4 (1 main + 3 modules) |
| **Main Activities** | 8 steps |
| **Admin Activities** | 12 steps |
| **Mobile Activities** | 15 steps |
| **Backend Activities** | 14 steps |

---

**Perfect for**: Documentation, presentations, team training, system understanding!

**Created**: October 8, 2025  
**Format**: UML Activity Diagrams in Mermaid  
**Complexity**: Simple & Clean
