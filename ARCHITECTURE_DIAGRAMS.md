# TalkAR Architecture Diagrams

## Table of Contents
1. [System Overview Architecture](#1-system-overview-architecture)
2. [High-Level Component Diagram](#2-high-level-component-diagram)
3. [Data Flow Architecture](#3-data-flow-architecture)
4. [Mobile App Architecture](#4-mobile-app-architecture)
5. [Backend API Architecture](#5-backend-api-architecture)
6. [Database Schema Diagram](#6-database-schema-diagram)
7. [Deployment Architecture](#7-deployment-architecture)
8. [AR Experience Flow](#8-ar-experience-flow)

---

## 1. System Overview Architecture

```mermaid
graph TB
    subgraph "Client Layer"
        Mobile[📱 Mobile App<br/>Android/Kotlin/ARCore]
        Dashboard[💻 Admin Dashboard<br/>React/TypeScript]
    end
    
    subgraph "API Gateway"
        API[🌐 Backend API<br/>Node.js/Express/TypeScript]
    end
    
    subgraph "Data Layer"
        DB[(🗄️ PostgreSQL<br/>Database)]
        S3[☁️ AWS S3<br/>Media Storage]
    end
    
    subgraph "External Services"
        Sync[🎬 Sync API<br/>Lip-Sync Generation]
        ARCore[📷 ARCore<br/>Image Recognition]
        MLKit[🤖 ML Kit<br/>Image Labeling]
    end
    
    Mobile <-->|REST API| API
    Dashboard <-->|REST API| API
    API <-->|Query/Update| DB
    API <-->|Upload/Download| S3
    API <-->|Generate Video| Sync
    Mobile -->|AR Tracking| ARCore
    Mobile -->|Image Recognition| MLKit
    
    style Mobile fill:#4CAF50
    style Dashboard fill:#2196F3
    style API fill:#FF9800
    style DB fill:#9C27B0
    style S3 fill:#00BCD4
    style Sync fill:#E91E63
    style ARCore fill:#3F51B5
    style MLKit fill:#009688
```

---

## 2. High-Level Component Diagram

```mermaid
graph LR
    subgraph "Mobile Application (Android)"
        UI[UI Layer<br/>Jetpack Compose]
        VM[ViewModel Layer<br/>Business Logic]
        REPO[Repository Layer<br/>Data Management]
        LOCAL[Local Storage<br/>Room DB]
        AR[AR Services<br/>ARCore + ML Kit]
    end
    
    subgraph "Backend Server (Node.js)"
        ROUTES[API Routes<br/>Express]
        MIDDLEWARE[Middleware<br/>Auth/Validation]
        SERVICES[Business Services<br/>Sync/Upload/Analytics]
        MODELS[Data Models<br/>Sequelize ORM]
    end
    
    subgraph "Web Dashboard (React)"
        PAGES[Pages<br/>Dashboard/Images]
        COMPONENTS[Components<br/>Dialogs/Forms]
        STORE[State Management<br/>Redux Toolkit]
        API_SERVICE[API Services<br/>Axios]
    end
    
    subgraph "Infrastructure"
        DB[(PostgreSQL)]
        STORAGE[AWS S3]
        CACHE[Redis Cache]
    end
    
    UI --> VM
    VM --> REPO
    REPO --> LOCAL
    REPO --> AR
    
    PAGES --> COMPONENTS
    COMPONENTS --> STORE
    STORE --> API_SERVICE
    
    ROUTES --> MIDDLEWARE
    MIDDLEWARE --> SERVICES
    SERVICES --> MODELS
    MODELS --> DB
    SERVICES --> STORAGE
    SERVICES --> CACHE
    
    REPO -.->|HTTP| ROUTES
    API_SERVICE -.->|HTTP| ROUTES
```

---

## 3. Data Flow Architecture

```mermaid
sequenceDiagram
    participant Admin as 👤 Admin User
    participant Dashboard as 💻 Admin Dashboard
    participant API as 🌐 Backend API
    participant DB as 🗄️ Database
    participant S3 as ☁️ AWS S3
    participant User as 📱 Mobile User
    participant ARCore as 📷 ARCore
    participant Sync as 🎬 Sync API
    
    rect rgb(200, 220, 250)
        Note over Admin,S3: Content Upload Flow
        Admin->>Dashboard: 1. Upload Image + Script
        Dashboard->>API: 2. POST /images (multipart)
        API->>S3: 3. Store image file
        S3-->>API: 4. Return image URL
        API->>DB: 5. Save metadata + URL
        DB-->>API: 6. Confirm save
        API-->>Dashboard: 7. Success response
        Dashboard-->>Admin: 8. Show confirmation
    end
    
    rect rgb(220, 250, 220)
        Note over User,Sync: AR Experience Flow
        User->>ARCore: 9. Point camera at image
        ARCore-->>User: 10. Recognize image
        User->>API: 11. GET /images/:id
        API->>DB: 12. Query image data
        DB-->>API: 13. Return image + script
        API-->>User: 14. Send content
        User->>API: 15. POST /lipsync/generate
        API->>Sync: 16. Request video generation
        Sync-->>API: 17. Return video URL
        API-->>User: 18. Send video URL
        User->>User: 19. Display AR overlay
    end
```

---

## 4. Mobile App Architecture

```mermaid
graph TB
    subgraph "Presentation Layer"
        SCREEN[📱 AR Screens<br/>ARScreen, Week2ARScreen]
        COMP[🎨 UI Components<br/>ARView, CameraView, VideoPlayer]
    end
    
    subgraph "ViewModel Layer"
        VM[🧠 ViewModels<br/>ARViewModel, SimpleARViewModel]
    end
    
    subgraph "Domain Layer"
        AR_SERVICE[📷 AR Services<br/>ARImageRecognitionService<br/>EnhancedARService<br/>MLKitRecognitionService]
    end
    
    subgraph "Data Layer"
        REPO[📦 Repositories<br/>ImageRepository<br/>SyncRepository]
        API_CLIENT[🌐 API Client<br/>Retrofit + OkHttp]
        DAO[💾 Local DAO<br/>ImageDao]
    end
    
    subgraph "Local Storage"
        ROOM[(🗃️ Room Database<br/>ImageDatabase)]
    end
    
    subgraph "External"
        BACKEND[Backend API]
        ARCORE[ARCore SDK]
        MLKIT[ML Kit SDK]
    end
    
    SCREEN --> COMP
    COMP --> VM
    VM --> AR_SERVICE
    AR_SERVICE --> REPO
    REPO --> API_CLIENT
    REPO --> DAO
    DAO --> ROOM
    API_CLIENT --> BACKEND
    AR_SERVICE --> ARCORE
    AR_SERVICE --> MLKIT
    
    style SCREEN fill:#4CAF50
    style VM fill:#2196F3
    style AR_SERVICE fill:#FF9800
    style REPO fill:#9C27B0
    style ROOM fill:#E91E63
```

---

## 5. Backend API Architecture

```mermaid
graph TB
    subgraph "API Gateway Layer"
        EXPRESS[Express.js Server]
        HELMET[Security: Helmet]
        CORS[CORS Protection]
        MORGAN[Logging: Morgan]
    end
    
    subgraph "Routing Layer"
        AUTH_ROUTE[/auth Routes]
        IMAGE_ROUTE[/images Routes]
        AVATAR_ROUTE[/avatars Routes]
        SYNC_ROUTE[/sync Routes]
        LIPSYNC_ROUTE[/lipsync Routes]
        ENHANCED_ROUTE[/enhanced-lipsync Routes]
        ANALYTICS_ROUTE[/analytics Routes]
        SCRIPT_ROUTE[/scripts Routes]
    end
    
    subgraph "Middleware Layer"
        AUTH_MW[🔐 Authentication<br/>JWT Verification]
        VALID_MW[✅ Validation<br/>Joi Schemas]
        ERROR_MW[⚠️ Error Handler<br/>Global Error Handling]
    end
    
    subgraph "Service Layer"
        AUTH_SVC[🔑 Auth Service<br/>Login/Register]
        SYNC_SVC[🎬 Sync Service<br/>Video Generation]
        UPLOAD_SVC[📤 Upload Service<br/>File Processing]
        LIPSYNC_SVC[💋 Enhanced LipSync<br/>Mock/Real Service]
        ANALYTICS_SVC[📊 Analytics Service<br/>Usage Tracking]
    end
    
    subgraph "Data Layer"
        IMAGE_MODEL[Image Model]
        AVATAR_MODEL[Avatar Model]
        USER_MODEL[User Model]
        DIALOGUE_MODEL[Dialogue Model]
        MAPPING_MODEL[ImageAvatarMapping]
        ASSOC[Associations]
    end
    
    subgraph "External Integration"
        DB[(PostgreSQL)]
        S3[AWS S3]
        SYNC_API[Sync API]
    end
    
    EXPRESS --> HELMET
    EXPRESS --> CORS
    EXPRESS --> MORGAN
    EXPRESS --> AUTH_ROUTE
    EXPRESS --> IMAGE_ROUTE
    EXPRESS --> AVATAR_ROUTE
    EXPRESS --> SYNC_ROUTE
    EXPRESS --> LIPSYNC_ROUTE
    EXPRESS --> ENHANCED_ROUTE
    EXPRESS --> ANALYTICS_ROUTE
    EXPRESS --> SCRIPT_ROUTE
    
    AUTH_ROUTE --> AUTH_MW
    IMAGE_ROUTE --> AUTH_MW
    AVATAR_ROUTE --> AUTH_MW
    
    AUTH_MW --> VALID_MW
    VALID_MW --> ERROR_MW
    
    ERROR_MW --> AUTH_SVC
    ERROR_MW --> SYNC_SVC
    ERROR_MW --> UPLOAD_SVC
    ERROR_MW --> LIPSYNC_SVC
    ERROR_MW --> ANALYTICS_SVC
    
    AUTH_SVC --> USER_MODEL
    SYNC_SVC --> IMAGE_MODEL
    UPLOAD_SVC --> IMAGE_MODEL
    LIPSYNC_SVC --> AVATAR_MODEL
    
    IMAGE_MODEL --> ASSOC
    AVATAR_MODEL --> ASSOC
    DIALOGUE_MODEL --> ASSOC
    MAPPING_MODEL --> ASSOC
    
    ASSOC --> DB
    UPLOAD_SVC --> S3
    SYNC_SVC --> SYNC_API
    
    style EXPRESS fill:#4CAF50
    style AUTH_MW fill:#2196F3
    style SYNC_SVC fill:#FF9800
    style DB fill:#9C27B0
```

---

## 6. Database Schema Diagram

```mermaid
erDiagram
    USER ||--o{ IMAGE : creates
    IMAGE ||--o{ DIALOGUE : has
    IMAGE ||--o{ IMAGE_AVATAR_MAPPING : maps_to
    AVATAR ||--o{ IMAGE_AVATAR_MAPPING : used_in
    
    USER {
        uuid id PK
        string email UK
        string password
        enum role
        datetime createdAt
        datetime updatedAt
    }
    
    IMAGE {
        uuid id PK
        string name
        text description
        string imageUrl
        string thumbnailUrl
        boolean isActive
        uuid userId FK
        datetime createdAt
        datetime updatedAt
    }
    
    DIALOGUE {
        uuid id PK
        uuid imageId FK
        text text
        string language
        string voiceId
        boolean isDefault
        datetime createdAt
        datetime updatedAt
    }
    
    AVATAR {
        uuid id PK
        string name
        string voiceId UK
        string language
        string gender
        string avatarImageUrl
        text description
        boolean isActive
        datetime createdAt
        datetime updatedAt
    }
    
    IMAGE_AVATAR_MAPPING {
        uuid id PK
        uuid imageId FK
        uuid avatarId FK
        uuid scriptId
        datetime createdAt
        datetime updatedAt
    }
```

---

## 7. Deployment Architecture

```mermaid
graph TB
    subgraph "Client Tier"
        USERS[👥 End Users<br/>Mobile Devices]
        ADMINS[👤 Admins<br/>Web Browsers]
    end
    
    subgraph "CDN & Load Balancer"
        CF[☁️ CloudFront CDN<br/>Static Assets]
        LB[⚖️ Load Balancer<br/>Traffic Distribution]
    end
    
    subgraph "Kubernetes Cluster"
        subgraph "Backend Pods"
            BE1[Backend Pod 1]
            BE2[Backend Pod 2]
            BE3[Backend Pod 3]
        end
        
        subgraph "Frontend Pods"
            FE1[Dashboard Pod 1]
            FE2[Dashboard Pod 2]
        end
    end
    
    subgraph "Data Tier"
        subgraph "Database Cluster"
            DB_PRIMARY[(Primary DB<br/>PostgreSQL)]
            DB_REPLICA[(Replica DB<br/>Read-Only)]
        end
        
        CACHE[(Redis Cache<br/>Session/Data)]
    end
    
    subgraph "Storage Tier"
        S3[AWS S3<br/>Images/Videos]
        BACKUP[S3 Backup<br/>Daily Snapshots]
    end
    
    subgraph "Monitoring & Logging"
        PROM[📊 Prometheus<br/>Metrics]
        GRAFANA[📈 Grafana<br/>Dashboards]
        ELK[📝 ELK Stack<br/>Logs]
    end
    
    subgraph "External Services"
        SYNC[Sync API<br/>Video Generation]
        ARCORE[Google ARCore<br/>Mobile SDK]
    end
    
    USERS --> LB
    ADMINS --> CF
    CF --> FE1
    CF --> FE2
    LB --> BE1
    LB --> BE2
    LB --> BE3
    
    BE1 --> DB_PRIMARY
    BE2 --> DB_PRIMARY
    BE3 --> DB_REPLICA
    
    BE1 --> CACHE
    BE2 --> CACHE
    BE3 --> CACHE
    
    BE1 --> S3
    BE2 --> S3
    BE3 --> S3
    
    DB_PRIMARY --> BACKUP
    S3 --> BACKUP
    
    BE1 --> SYNC
    USERS --> ARCORE
    
    BE1 -.-> PROM
    BE2 -.-> PROM
    BE3 -.-> PROM
    PROM --> GRAFANA
    BE1 -.-> ELK
    BE2 -.-> ELK
    BE3 -.-> ELK
    
    style USERS fill:#4CAF50
    style LB fill:#2196F3
    style BE1 fill:#FF9800
    style DB_PRIMARY fill:#9C27B0
    style S3 fill:#00BCD4
```

---

## 8. AR Experience Flow

```mermaid
graph TB
    START([User Opens App]) --> PERM{Camera<br/>Permission?}
    PERM -->|No| REQ[Request Permission]
    REQ --> PERM
    PERM -->|Yes| INIT[Initialize ARCore Session]
    
    INIT --> CAMERA[Start Camera Preview]
    CAMERA --> SCAN[Scan for Images]
    
    SCAN --> DETECT{Image<br/>Detected?}
    DETECT -->|No| SCAN
    
    DETECT -->|Yes| TRACK[Track Image with ARCore]
    TRACK --> FETCH[Fetch Image Data from API]
    
    FETCH --> CHECK{Content<br/>Cached?}
    CHECK -->|Yes| LOAD_CACHE[Load from Room DB]
    CHECK -->|No| API_CALL[Call Backend API]
    
    API_CALL --> CACHE[Cache in Room DB]
    CACHE --> VIDEO_REQ[Request Lip-Sync Video]
    LOAD_CACHE --> VIDEO_REQ
    
    VIDEO_REQ --> GEN{Video<br/>Generated?}
    GEN -->|No| GENERATE[Generate via Sync API]
    GENERATE --> WAIT[Wait for Processing]
    WAIT --> POLL[Poll Status]
    POLL --> GEN
    
    GEN -->|Yes| DOWNLOAD[Download Video]
    DOWNLOAD --> ANCHOR[Create AR Anchor on Image]
    ANCHOR --> OVERLAY[Position Video Overlay]
    OVERLAY --> PLAY[Play Lip-Sync Video]
    
    PLAY --> TRACK_ACTIVE{Image Still<br/>Tracked?}
    TRACK_ACTIVE -->|Yes| PLAY
    TRACK_ACTIVE -->|No| PAUSE[Pause Playback]
    PAUSE --> SCAN
    
    style START fill:#4CAF50
    style DETECT fill:#2196F3
    style GEN fill:#FF9800
    style PLAY fill:#E91E63
    style TRACK_ACTIVE fill:#9C27B0
```

---

## Component Interaction Matrix

| Component | Mobile App | Backend API | Admin Dashboard | PostgreSQL | AWS S3 | Sync API | ARCore |
|-----------|-----------|-------------|-----------------|------------|---------|----------|--------|
| **Mobile App** | - | REST API | - | - | - | - | ✓ |
| **Backend API** | ✓ | - | ✓ | ✓ | ✓ | ✓ | - |
| **Admin Dashboard** | - | ✓ | - | - | - | - | - |
| **PostgreSQL** | - | ✓ | - | - | - | - | - |
| **AWS S3** | - | ✓ | - | - | - | - | - |
| **Sync API** | - | ✓ | - | - | - | - | - |
| **ARCore** | ✓ | - | - | - | - | - | - |

---

## Technology Stack Summary

### Mobile Application (Android)
```
┌─────────────────────────────┐
│   Jetpack Compose (UI)      │
├─────────────────────────────┤
│   ViewModel (MVVM)          │
├─────────────────────────────┤
│   Repository Pattern        │
├─────────────────────────────┤
│   ARCore + ML Kit          │
├─────────────────────────────┤
│   Room DB + Retrofit       │
└─────────────────────────────┘
```

### Backend API (Node.js)
```
┌─────────────────────────────┐
│   Express.js Middleware     │
├─────────────────────────────┤
│   API Routes               │
├─────────────────────────────┤
│   Business Services        │
├─────────────────────────────┤
│   Sequelize ORM            │
├─────────────────────────────┤
│   PostgreSQL Database      │
└─────────────────────────────┘
```

### Admin Dashboard (React)
```
┌─────────────────────────────┐
│   Material-UI Components    │
├─────────────────────────────┤
│   React Pages              │
├─────────────────────────────┤
│   Redux Toolkit Store      │
├─────────────────────────────┤
│   Axios API Client         │
└─────────────────────────────┘
```

---

## Network Architecture

```
                    ┌──────────────┐
                    │   Internet   │
                    └──────┬───────┘
                           │
                    ┌──────▼───────┐
                    │ Load Balancer│
                    │   (HTTPS)    │
                    └──────┬───────┘
                           │
            ┌──────────────┴──────────────┐
            │                             │
     ┌──────▼─────┐              ┌───────▼────┐
     │  Backend   │              │   Admin    │
     │   API      │              │  Dashboard │
     │  Port 3000 │              │  Port 3001 │
     └──────┬─────┘              └────────────┘
            │
     ┌──────┴─────────────────────┐
     │                            │
┌────▼─────┐              ┌──────▼──────┐
│PostgreSQL│              │   AWS S3    │
│Port 5432 │              │   Storage   │
└──────────┘              └─────────────┘
```

---

## Security Architecture

```mermaid
graph LR
    subgraph "Security Layers"
        CLIENT[Client Request]
        HTTPS[HTTPS/TLS]
        CORS_CHECK[CORS Validation]
        AUTH[JWT Authentication]
        VALID[Input Validation]
        SANITIZE[Data Sanitization]
        ENCRYPT[DB Encryption]
        PROCESS[Business Logic]
    end
    
    CLIENT --> HTTPS
    HTTPS --> CORS_CHECK
    CORS_CHECK --> AUTH
    AUTH --> VALID
    VALID --> SANITIZE
    SANITIZE --> ENCRYPT
    ENCRYPT --> PROCESS
    
    style HTTPS fill:#4CAF50
    style AUTH fill:#2196F3
    style ENCRYPT fill:#FF9800
```

---

## How to Use These Diagrams

### For GitHub/GitLab
These Mermaid diagrams will automatically render in:
- GitHub README files
- GitLab documentation
- Most modern markdown viewers

### For Documentation Tools
Import this file into:
- **Confluence**: Use Mermaid macro
- **Notion**: Use code blocks with mermaid language
- **Draw.io/Diagrams.net**: Import Mermaid syntax
- **VS Code**: Use Mermaid Preview extension

### For Presentations
1. Copy diagram code to [Mermaid Live Editor](https://mermaid.live)
2. Export as PNG/SVG
3. Use in PowerPoint/Google Slides

### For Development
- Reference these diagrams in code reviews
- Use as onboarding material for new developers
- Include in technical documentation
- Share with stakeholders for system understanding

---

**Last Updated**: October 8, 2025  
**Diagram Version**: 1.0
