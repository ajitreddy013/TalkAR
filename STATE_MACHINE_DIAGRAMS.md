# TalkAR - UML State Machine Diagrams

## Table of Contents
1. [User Session State Machine](#1-user-session-state-machine)
2. [Image Upload State Machine](#2-image-upload-state-machine)
3. [AR Recognition State Machine](#3-ar-recognition-state-machine)
4. [Video Generation State Machine](#4-video-generation-state-machine)
5. [Mobile App State Machine](#5-mobile-app-state-machine)
6. [Image Entity State Machine](#6-image-entity-state-machine)

---

## UML State Machine Notation

### Elements
- **Initial State** (●): Starting point
- **Final State** (◉): End point
- **State**: Represents a condition or situation
- **Transition**: Arrow showing state change
- **Event/Trigger**: What causes transition
- **Guard Condition**: [condition] that must be true
- **Action**: /action performed during transition

### Transition Format
```
Event [Guard] / Action
```

---

## 1. User Session State Machine

**Purpose**: Models user authentication and session lifecycle

```mermaid
stateDiagram-v2
    [*] --> NotAuthenticated
    
    NotAuthenticated --> Authenticating : login() / validateCredentials
    
    Authenticating --> Authenticated : validCredentials / generateToken
    Authenticating --> LoginFailed : invalidCredentials / showError
    Authenticating --> AccountLocked : attemptsExceeded / lockAccount
    
    LoginFailed --> NotAuthenticated : retry
    
    AccountLocked --> NotAuthenticated : resetByAdmin / unlockAccount
    AccountLocked --> [*] : timeout
    
    Authenticated --> Active : accessGranted
    
    Active --> Refreshing : tokenExpiring / requestRefresh
    Active --> Idle : noActivity[5min]
    Active --> NotAuthenticated : logout / clearSession
    
    Refreshing --> Active : refreshSuccess / updateToken
    Refreshing --> NotAuthenticated : refreshFailed / forceLogout
    
    Idle --> Active : userActivity / resumeSession
    Idle --> NotAuthenticated : timeout[30min] / expireSession
    
    state Authenticated {
        [*] --> CheckingPermissions
        CheckingPermissions --> Authorized : hasPermissions
        CheckingPermissions --> Unauthorized : lacksPermissions
        Unauthorized --> [*]
        Authorized --> [*]
    }
    
    note right of Authenticating
        Validates email/password
        Checks failed attempts
        Generates JWT tokens
    end note
    
    note right of Active
        User has valid token
        Can access resources
        Token auto-refreshes
    end note
```

---

## 2. Image Upload State Machine

**Purpose**: Models complete image upload workflow

```mermaid
stateDiagram-v2
    [*] --> Idle
    
    Idle --> Selecting : userClicksUpload
    
    Selecting --> Validating : fileSelected / checkFile
    Selecting --> Idle : cancel
    
    Validating --> UploadingToS3 : valid[size<10MB && type=image]
    Validating --> ValidationFailed : invalid / showError
    
    ValidationFailed --> Selecting : retry
    ValidationFailed --> [*] : cancel
    
    UploadingToS3 --> GeneratingThumbnail : uploadComplete / getS3URL
    UploadingToS3 --> UploadFailed : networkError / rollback
    
    UploadFailed --> UploadingToS3 : retry[attempts<3]
    UploadFailed --> [*] : maxRetriesReached / cleanup
    
    GeneratingThumbnail --> SavingMetadata : thumbnailReady
    GeneratingThumbnail --> SavingMetadata : thumbnailFailed / useDefault
    
    SavingMetadata --> CreatingDialogue : metadataSaved
    SavingMetadata --> SaveFailed : databaseError
    
    SaveFailed --> CleaningUp : cleanup / deleteS3Files
    CleaningUp --> [*]
    
    CreatingDialogue --> MappingAvatar : dialogueCreated
    CreatingDialogue --> Completed : skipDialogue
    
    MappingAvatar --> Completed : avatarMapped
    MappingAvatar --> Completed : skipMapping
    
    Completed --> Idle : uploadAnother
    Completed --> [*] : done
    
    state UploadingToS3 {
        [*] --> Uploading
        Uploading --> Progress : chunk[0-100%]
        Progress --> Uploading : continue
        Progress --> [*] : complete
    }
    
    note right of Validating
        Check file type
        Check file size
        Validate format
    end note
    
    note right of Completed
        Image ready for AR
        Can be scanned by mobile
    end note
```

---

## 3. AR Recognition State Machine

**Purpose**: Models image recognition and tracking lifecycle

```mermaid
stateDiagram-v2
    [*] --> Initializing
    
    Initializing --> CheckingARCore : initAR
    
    CheckingARCore --> ARCoreAvailable : available
    CheckingARCore --> FallbackMLKit : notAvailable
    
    ARCoreAvailable --> LoadingDatabase : createSession
    FallbackMLKit --> MLKitReady : initMLKit
    
    LoadingDatabase --> Ready : databaseLoaded
    LoadingDatabase --> InitFailed : loadError
    
    MLKitReady --> Ready : ready
    
    InitFailed --> [*] : showError
    
    Ready --> Scanning : startCamera
    
    Scanning --> Detecting : frameReceived
    
    Detecting --> ImageFound : imageDetected / extractImageData
    Detecting --> Scanning : noImageFound / continueScan
    
    ImageFound --> Tracking : createAnchor / positionOverlay
    
    Tracking --> TrackingGood : trackingQuality[>0.7]
    Tracking --> TrackingPoor : trackingQuality[<0.7]
    
    TrackingGood --> DisplayingContent : fetchContent / showOverlay
    TrackingGood --> TrackingPoor : qualityDrop
    
    TrackingPoor --> TrackingGood : qualityImprove
    TrackingPoor --> TrackingLost : timeout[2s]
    
    DisplayingContent --> PlayingVideo : videoReady
    DisplayingContent --> TrackingLost : imageLost
    
    PlayingVideo --> DisplayingContent : videoLoop
    PlayingVideo --> TrackingLost : imageLost / pauseVideo
    PlayingVideo --> LanguageChange : userChangesLanguage
    
    LanguageChange --> DisplayingContent : newVideoReady
    
    TrackingLost --> Scanning : resumeScan / removeAnchor
    
    Scanning --> Paused : userPause
    Paused --> Scanning : userResume
    Paused --> [*] : userExit
    
    PlayingVideo --> [*] : userExit / cleanup
    
    state DisplayingContent {
        [*] --> FetchingData
        FetchingData --> CheckingCache
        CheckingCache --> LoadingFromCache : cached
        CheckingCache --> DownloadingFromAPI : notCached
        DownloadingFromAPI --> CachingData
        CachingData --> [*]
        LoadingFromCache --> [*]
    }
    
    note right of Tracking
        Continuously monitors
        image position and
        orientation in 3D space
    end note
    
    note right of PlayingVideo
        AR overlay shows
        lip-synced talking head
        anchored to image
    end note
```

---

## 4. Video Generation State Machine

**Purpose**: Models async video generation process

```mermaid
stateDiagram-v2
    [*] --> Idle
    
    Idle --> ValidatingRequest : generateRequest / checkParams
    
    ValidatingRequest --> CheckingCache : valid
    ValidatingRequest --> RequestInvalid : invalid / returnError
    
    RequestInvalid --> [*]
    
    CheckingCache --> ReturningCached : cacheHit / getURL
    CheckingCache --> Queuing : cacheMiss / createJob
    
    ReturningCached --> [*]
    
    Queuing --> Pending : jobQueued / returnJobId
    
    Pending --> Processing : workerPicksJob / startGeneration
    Pending --> Cancelled : userCancel / removeFromQueue
    
    Cancelled --> [*]
    
    Processing --> CallingAPI : sendToSyncAPI
    
    CallingAPI --> Generating : apiAccepted / getExternalJobId
    CallingAPI --> APIError : apiFailed / logError
    
    APIError --> Retrying : canRetry[attempts<3]
    APIError --> Failed : maxRetries / notifyUser
    
    Retrying --> CallingAPI : waitBackoff / retry
    
    Generating --> Polling : startPolling
    
    Polling --> CheckStatus : poll[every 2s]
    
    CheckStatus --> StillProcessing : status=processing
    CheckStatus --> GenerationComplete : status=completed
    CheckStatus --> GenerationFailed : status=failed
    
    StillProcessing --> Polling : wait
    StillProcessing --> Timeout : timeout[60s]
    
    Timeout --> Failed : maxTimeExceeded
    
    GenerationComplete --> Downloading : getVideoURL
    
    Downloading --> StoringS3 : downloadComplete
    Downloading --> DownloadFailed : downloadError
    
    DownloadFailed --> Retrying : retry
    
    StoringS3 --> UpdatingDatabase : s3UploadComplete / getS3URL
    
    UpdatingDatabase --> Caching : dbUpdated
    
    Caching --> Completed : cached / notifyUser
    
    GenerationFailed --> Failed : logError
    
    Failed --> [*]
    Completed --> [*]
    
    state Processing {
        [*] --> PreparingPayload
        PreparingPayload --> AddingText
        AddingText --> AddingVoice
        AddingVoice --> AddingLanguage
        AddingLanguage --> [*]
    }
    
    note right of Generating
        External Sync API
        processes video
        with lip-sync
    end note
    
    note right of Completed
        Video ready
        URL returned to client
        Cached for 24 hours
    end note
```

---

## 5. Mobile App State Machine

**Purpose**: Models overall mobile application lifecycle

```mermaid
stateDiagram-v2
    [*] --> Launching
    
    Launching --> CheckingPermissions : appStart
    
    CheckingPermissions --> PermissionsGranted : hasAllPermissions
    CheckingPermissions --> RequestingPermissions : missingPermissions
    
    RequestingPermissions --> PermissionsGranted : userGrants
    RequestingPermissions --> PermissionsDenied : userDenies
    
    PermissionsDenied --> ShowingError : showPermissionError
    ShowingError --> [*] : userExits
    
    PermissionsGranted --> InitializingServices : setupApp
    
    InitializingServices --> LoadingData : servicesReady
    InitializingServices --> InitFailed : setupError
    
    InitFailed --> Retrying : retry
    InitFailed --> [*] : cancel
    
    Retrying --> InitializingServices : retryInit
    
    LoadingData --> SyncingContent : checkConnection
    
    SyncingContent --> Ready : syncComplete
    SyncingContent --> Ready : syncFailed[useCache]
    
    Ready --> ARMode : userSelectsAR
    Ready --> BrowseMode : userSelectsBrowse
    Ready --> SettingsMode : userSelectsSettings
    
    ARMode --> Ready : backToHome
    BrowseMode --> Ready : backToHome
    SettingsMode --> Ready : backToHome
    
    state ARMode {
        [*] --> ARInitializing
        ARInitializing --> ARScanning
        ARScanning --> ARRecognized
        ARRecognized --> ARDisplaying
        ARDisplaying --> ARScanning : imageLost
        ARDisplaying --> [*] : exit
    }
    
    state BrowseMode {
        [*] --> ShowingList
        ShowingList --> ShowingDetail
        ShowingDetail --> ShowingList
        ShowingDetail --> [*] : exit
    }
    
    Ready --> Background : appMinimized
    ARMode --> Background : appMinimized
    BrowseMode --> Background : appMinimized
    
    Background --> Ready : appResumed
    Background --> [*] : appKilled
    
    Ready --> [*] : userExits / cleanup
    
    note right of Ready
        Main state where user
        can navigate between
        different app modes
    end note
    
    note right of Background
        App in background
        Pauses AR session
        Maintains state
    end note
```

---

## 6. Image Entity State Machine

**Purpose**: Models image lifecycle in the system

```mermaid
stateDiagram-v2
    [*] --> Creating
    
    Creating --> Uploading : startUpload / validateFile
    
    Uploading --> Processing : uploadComplete / processImage
    Uploading --> UploadFailed : uploadError
    
    UploadFailed --> Uploading : retry
    UploadFailed --> Deleted : abandon / cleanup
    
    Processing --> Inactive : processingComplete[noDialogue]
    Processing --> PendingDialogue : needsDialogue
    Processing --> ProcessingFailed : processingError
    
    ProcessingFailed --> Deleted : cleanup
    
    PendingDialogue --> Inactive : dialogueAdded
    
    Inactive --> Active : adminActivates / setActive(true)
    Inactive --> Editing : adminEdits
    Inactive --> Deleted : adminDeletes / removeFromS3
    
    Active --> Inactive : adminDeactivates / setActive(false)
    Active --> InUse : mobileUserScans
    Active --> Editing : adminEdits
    Active --> Deleted : adminDeletes
    
    InUse --> Active : scanComplete
    InUse --> Generating : requestVideo
    
    Generating --> InUse : videoReady
    Generating --> Active : generationFailed
    
    Editing --> Inactive : saveChanges[wasInactive]
    Editing --> Active : saveChanges[wasActive]
    Editing --> Deleted : deleteWhileEditing
    
    Active --> Archived : archive[inactive>90days]
    Inactive --> Archived : archive[inactive>90days]
    
    Archived --> Active : restore / setActive(true)
    Archived --> Deleted : permanentDelete
    
    Deleted --> [*]
    
    state Active {
        [*] --> Available
        Available --> BeingScanned : userScans
        BeingScanned --> Available : scanComplete
        
        state BeingScanned {
            [*] --> Detected
            Detected --> Tracked
            Tracked --> DisplayingOverlay
            DisplayingOverlay --> [*]
        }
    }
    
    note right of Active
        Image is visible in
        mobile app and can
        be scanned for AR
    end note
    
    note right of InUse
        Currently being used
        in an AR session by
        one or more users
    end note
    
    note right of Archived
        Inactive for >90 days
        Not shown in app
        Can be restored
    end note
```

---

## State Machine Summary

| # | State Machine | Purpose | States | Transitions |
|---|--------------|---------|--------|-------------|
| 1 | User Session | Authentication lifecycle | 8 | 14 |
| 2 | Image Upload | Upload workflow | 11 | 16 |
| 3 | AR Recognition | Image detection & tracking | 12 | 18 |
| 4 | Video Generation | Async video processing | 15 | 20 |
| 5 | Mobile App | App lifecycle | 13 | 17 |
| 6 | Image Entity | Image lifecycle | 12 | 19 |

---

## State Types

### Simple State
```
StateName
```
Basic state with no internal structure

### Composite State
```
state ParentState {
    [*] --> ChildState1
    ChildState1 --> ChildState2
}
```
State containing sub-states

### Choice Pseudostate
```
State1 --> State2 : condition1[guard]
State1 --> State3 : condition2[guard]
```
Conditional branching based on guards

---

## Transition Types

### 1. External Transition
```
StateA --> StateB : event / action
```
Exits StateA, performs action, enters StateB

### 2. Internal Transition
```
StateA : event / action
```
Handles event without leaving state

### 3. Self Transition
```
StateA --> StateA : event / action
```
Exits and re-enters same state

### 4. Compound Transition
```
StateA --> StateB : event1
StateB --> StateC : event2
```
Sequential transitions

---

## Guard Conditions

### Examples from Diagrams

| Guard | Meaning |
|-------|---------|
| `[size<10MB && type=image]` | File must be under 10MB and be an image |
| `[attempts<3]` | Retry count must be less than 3 |
| `[trackingQuality>0.7]` | Tracking quality above 70% |
| `[inactive>90days]` | Inactive for more than 90 days |
| `[hasPermissions]` | User has required permissions |

---

## Actions

### Common Actions

| Action | Purpose |
|--------|---------|
| `/validateCredentials` | Check email/password |
| `/generateToken` | Create JWT token |
| `/showError` | Display error message |
| `/checkFile` | Validate file format/size |
| `/getS3URL` | Retrieve AWS S3 URL |
| `/createAnchor` | Create AR anchor |
| `/pauseVideo` | Pause video playback |
| `/cleanup` | Clean up resources |

---

## Events/Triggers

### User Events
- `login()`, `logout()`, `retry`
- `userClicksUpload`, `fileSelected`, `cancel`
- `userChangesLanguage`, `userExit`

### System Events
- `validCredentials`, `uploadComplete`
- `imageDetected`, `imageLost`
- `videoReady`, `timeout`

### Temporal Events
- `timeout[5min]`, `timeout[30min]`
- `poll[every 2s]`
- `archive[inactive>90days]`

---

## Concurrent States

### Example: Multiple AR Sessions
```mermaid
stateDiagram-v2
    state ARSession {
        state fork_state <<fork>>
        [*] --> fork_state
        fork_state --> Recognition
        fork_state --> Tracking
        
        state join_state <<join>>
        Recognition --> join_state
        Tracking --> join_state
        join_state --> [*]
    }
```

---

## How to Use These Diagrams

### 🚀 View in Mermaid Live:
1. Visit: https://mermaid.live
2. Copy any state diagram
3. View state transitions
4. Export as PNG/SVG

### 📂 GitHub:
```bash
git add STATE_MACHINE_DIAGRAMS.md
git commit -m "Add UML state machine diagrams"
git push
```

### 💻 VS Code:
1. Install "Markdown Preview Mermaid Support"
2. Open file
3. Press `Ctrl+Shift+V`
4. View all state machines

---

## Benefits of State Machines

✅ **Clear Lifecycle** - Shows complete object lifecycle  
✅ **Valid States** - Only valid states defined  
✅ **Valid Transitions** - Only valid transitions shown  
✅ **Event Handling** - Clear event response  
✅ **Guard Conditions** - Conditional logic explicit  
✅ **Error Handling** - Error states and recovery paths  
✅ **Documentation** - Self-documenting behavior  

---

## State Machine Validation

### Questions to Validate
1. ✅ Can every state be reached?
2. ✅ Can every state be exited?
3. ✅ Are all events handled?
4. ✅ Are guard conditions complete?
5. ✅ Are error states defined?
6. ✅ Is there a path to final state?

---

## Implementation Notes

### Backend (Node.js/TypeScript)
```typescript
enum UserSessionState {
    NotAuthenticated,
    Authenticating,
    Authenticated,
    Active,
    Idle
}
```

### Mobile (Kotlin)
```kotlin
sealed class ARState {
    object Initializing : ARState()
    object Scanning : ARState()
    data class Tracking(val imageId: String) : ARState()
    data class DisplayingContent(val content: Content) : ARState()
}
```

---

**Created**: October 8, 2025  
**Standard**: UML 2.0 State Machine Diagrams  
**Format**: Mermaid stateDiagram-v2  
**Total Diagrams**: 6 complete state machines
