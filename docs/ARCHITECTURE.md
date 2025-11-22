# TalkAR System Architecture

## Overview
TalkAR is an Augmented Reality application that brings static posters to life using Generative AI. It consists of a mobile Android app, a React-based admin dashboard, and a Node.js backend that orchestrates the AI pipeline.

## High-Level Architecture

```mermaid
graph TD
    User[User (Android App)] -->|Scan Poster| AR[AR Module (ARCore)]
    AR -->|Image ID| Backend[Node.js Backend]
    Backend -->|Get Metadata| DB[(PostgreSQL/Supabase)]
    Backend -->|Generate Script| OpenAI[OpenAI GPT-4o]
    Backend -->|Generate Audio| TTS[ElevenLabs/Google TTS]
    Backend -->|Sync Lip| Sync[Sync.so / SadTalker]
    Backend -->|Video URL| User
    
    Admin[Admin (React Dashboard)] -->|Manage Posters| Backend
    Admin -->|View Analytics| Backend
```

## Components

### 1. Mobile App (Android)
- **Tech Stack**: Kotlin, ARCore, Retrofit.
- **Role**: Scans posters, displays AR overlay, captures user feedback.
- **Key Features**:
  - Image Tracking (ARCore)
  - Video Playback (ExoPlayer)
  - Real-time API communication

### 2. Backend API (Node.js)
- **Tech Stack**: Express.js, TypeScript, PostgreSQL (Supabase).
- **Role**: Central orchestration layer.
- **Key Services**:
  - `OptimizedScriptService`: Generates ad scripts using OpenAI.
  - `LipSyncService`: Orchestrates video generation.
  - `AnalyticsService`: Tracks user interactions.

### 3. Admin Dashboard (React)
- **Tech Stack**: React, Material UI, Vite.
- **Role**: Content management and analytics visualization.

### 4. AI Pipeline
- **Script Generation**: OpenAI GPT-4o-mini.
- **Text-to-Speech**: External TTS provider.
- **Lip Sync**: Sync.so or internal model.

## Data Flow
1. **Scan**: App recognizes poster image.
2. **Request**: App sends `image_id` to Backend.
3. **Process**: Backend checks cache -> generates script -> generates video.
4. **Response**: Backend returns video URL.
5. **Display**: App overlays video on physical poster.
