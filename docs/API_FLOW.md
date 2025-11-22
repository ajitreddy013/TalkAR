# API Flow: Scan to Video

This document outlines the sequence of events when a user scans a poster.

## Sequence Diagram

```mermaid
sequenceDiagram
    participant App as Android App
    participant API as Backend API
    participant DB as Database
    participant AI as OpenAI
    participant TTS as TTS Service
    participant Sync as LipSync Service

    App->>API: POST /generate-dynamic-script (image_id)
    API->>DB: Get Poster Metadata
    DB-->>API: Metadata (Product, Tone, etc.)
    
    alt Cache Hit
        API-->>App: Return Cached Video URL
    else Cache Miss
        API->>AI: Generate Ad Script
        AI-->>API: Script Text
        
        API->>TTS: Generate Audio
        TTS-->>API: Audio File
        
        API->>Sync: Generate LipSync Video
        Sync-->>API: Video URL
        
        API->>DB: Save to Cache
        API-->>App: Return Video URL
    end
    
    App->>App: Play Video Overlay
```

## Error Handling Flow

1. **Network Error**: App retries 3 times -> Shows "Offline Mode" UI.
2. **AI Failure**: Backend catches error -> Returns default static script -> App displays text bubble.
3. **Sync Failure**: Backend returns static avatar image + audio -> App displays static avatar with audio.
