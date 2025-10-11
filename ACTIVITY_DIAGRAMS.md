# TalkAR - UML Activity Diagrams

## Table of Contents
1. [Main System Activity Diagram](#1-main-system-activity-diagram)
2. [Admin Content Upload Activity](#2-admin-content-upload-activity)
3. [Mobile AR Experience Activity](#3-mobile-ar-experience-activity)
4. [Authentication Activity](#4-authentication-activity)
5. [Video Generation Activity](#5-video-generation-activity)
6. [Image Recognition Activity](#6-image-recognition-activity)
7. [Content Synchronization Activity](#7-content-synchronization-activity)

---

## 1. Main System Activity Diagram

```mermaid
graph TB
    Start([Start TalkAR System]) --> Choice{User Type?}
    
    Choice -->|Admin| AdminLogin[Admin Login]
    Choice -->|Mobile User| MobileStart[Open Mobile App]
    
    %% Admin Flow
    AdminLogin --> AdminAuth{Authentication<br/>Success?}
    AdminAuth -->|No| AdminLogin
    AdminAuth -->|Yes| AdminDashboard[Access Dashboard]
    AdminDashboard --> AdminAction{Admin Action?}
    
    AdminAction -->|Upload Image| UploadImage[Upload Image to S3]
    AdminAction -->|Create Script| CreateScript[Create Dialogue/Script]
    AdminAction -->|Manage Avatar| ManageAvatar[Configure Avatar Settings]
    AdminAction -->|View Analytics| ViewAnalytics[View System Analytics]
    
    UploadImage --> SaveDB1[(Save to Database)]
    CreateScript --> SaveDB1
    ManageAvatar --> SaveDB1
    SaveDB1 --> AdminDashboard
    ViewAnalytics --> AdminDashboard
    
    AdminAction -->|Logout| EndAdmin([End Admin Session])
    
    %% Mobile User Flow
    MobileStart --> CheckPermission{Camera<br/>Permission?}
    CheckPermission -->|No| RequestPermission[Request Permission]
    RequestPermission --> CheckPermission
    CheckPermission -->|Yes| InitAR[Initialize ARCore]
    
    InitAR --> StartCamera[Start Camera Preview]
    StartCamera --> ScanImage[Scan for Images]
    
    ScanImage --> ImageDetected{Image<br/>Detected?}
    ImageDetected -->|No| ScanImage
    ImageDetected -->|Yes| RecognizeImage[Recognize Image via ARCore]
    
    RecognizeImage --> FetchData[Fetch Image Data from API]
    FetchData --> CheckCache{Content<br/>Cached?}
    
    CheckCache -->|Yes| LoadCache[Load from Local DB]
    CheckCache -->|No| DownloadData[Download from Backend]
    DownloadData --> CacheData[Cache Locally]
    CacheData --> LoadCache
    
    LoadCache --> GenerateVideo[Request Lip-Sync Video]
    GenerateVideo --> VideoReady{Video<br/>Ready?}
    
    VideoReady -->|Processing| WaitVideo[Wait & Poll Status]
    WaitVideo --> VideoReady
    VideoReady -->|Ready| DisplayAR[Display AR Overlay]
    
    DisplayAR --> PlayVideo[Play Talking Head Video]
    PlayVideo --> UserAction{User Action?}
    
    UserAction -->|Change Language| SelectLang[Select New Language]
    SelectLang --> GenerateVideo
    UserAction -->|Continue Scanning| ScanImage
    UserAction -->|Exit| EndMobile([End Mobile Session])
    
    style Start fill:#4CAF50,stroke:#2E7D32,stroke-width:3px,color:#fff
    style EndAdmin fill:#f44336,stroke:#c62828,stroke-width:3px,color:#fff
    style EndMobile fill:#f44336,stroke:#c62828,stroke-width:3px,color:#fff
    style AdminDashboard fill:#2196F3,stroke:#1565C0,stroke-width:2px,color:#fff
    style DisplayAR fill:#FF9800,stroke:#E65100,stroke-width:2px,color:#fff
    style PlayVideo fill:#9C27B0,stroke:#6A1B9A,stroke-width:2px,color:#fff
```

---

## 2. Admin Content Upload Activity

```mermaid
graph TB
    Start([Admin: Upload Content]) --> Login[Login to Dashboard]
    Login --> Auth{Valid<br/>Credentials?}
    
    Auth -->|No| ErrorAuth[Show Error Message]
    ErrorAuth --> Login
    Auth -->|Yes| Dashboard[Access Dashboard]
    
    Dashboard --> SelectAction{Select Action}
    
    %% Upload Image Flow
    SelectAction -->|Upload Image| SelectFile[Select Image File]
    SelectFile --> ValidateFile{Valid Image<br/>Format?}
    
    ValidateFile -->|No| ErrorFile[Show Format Error]
    ErrorFile --> SelectFile
    ValidateFile -->|Yes| CheckSize{Size < 10MB?}
    
    CheckSize -->|No| ErrorSize[Show Size Error]
    ErrorSize --> SelectFile
    CheckSize -->|Yes| UploadS3[Upload to AWS S3]
    
    UploadS3 --> S3Success{Upload<br/>Success?}
    S3Success -->|No| ErrorUpload[Show Upload Error]
    ErrorUpload --> SelectFile
    S3Success -->|Yes| GenerateThumb[Generate Thumbnail]
    
    GenerateThumb --> SaveMeta[Save Image Metadata to DB]
    SaveMeta --> SaveSuccess{Save<br/>Success?}
    
    SaveSuccess -->|No| ErrorSave[Show Save Error]
    ErrorSave --> CleanupS3[Delete from S3]
    CleanupS3 --> SelectFile
    SaveSuccess -->|Yes| ImageSaved[Image Saved Successfully]
    
    %% Create Script Flow
    ImageSaved --> AddScript{Add Script?}
    AddScript -->|No| Complete
    AddScript -->|Yes| EnterScript[Enter Dialogue Text]
    
    EnterScript --> SelectLang[Select Language]
    SelectLang --> SelectVoice[Select Voice ID]
    SelectVoice --> SetDefault{Set as Default?}
    
    SetDefault --> SaveScript[Save Dialogue to DB]
    SaveScript --> ScriptSuccess{Save<br/>Success?}
    
    ScriptSuccess -->|No| ErrorScript[Show Script Error]
    ErrorScript --> EnterScript
    ScriptSuccess -->|Yes| ScriptSaved[Script Saved]
    
    ScriptSaved --> AddMore{Add More<br/>Scripts?}
    AddMore -->|Yes| EnterScript
    AddMore -->|No| MapAvatar{Map Avatar?}
    
    %% Map Avatar Flow
    MapAvatar -->|Yes| SelectAvatar[Select Avatar]
    SelectAvatar --> CreateMapping[Create Image-Avatar Mapping]
    CreateMapping --> MappingSuccess{Mapping<br/>Success?}
    
    MappingSuccess -->|No| ErrorMapping[Show Mapping Error]
    ErrorMapping --> SelectAvatar
    MappingSuccess -->|Yes| MappingSaved[Mapping Saved]
    
    MappingSaved --> MapAvatar
    MapAvatar -->|No| Complete[Content Upload Complete]
    
    Complete --> Continue{Continue<br/>Working?}
    Continue -->|Yes| Dashboard
    Continue -->|No| Logout[Logout]
    Logout --> End([End])
    
    style Start fill:#4CAF50,stroke:#2E7D32,stroke-width:3px,color:#fff
    style End fill:#f44336,stroke:#c62828,stroke-width:3px,color:#fff
    style Complete fill:#9C27B0,stroke:#6A1B9A,stroke-width:2px,color:#fff
    style ImageSaved fill:#2196F3,stroke:#1565C0,stroke-width:2px,color:#fff
    style ScriptSaved fill:#2196F3,stroke:#1565C0,stroke-width:2px,color:#fff
```

---

## 3. Mobile AR Experience Activity

```mermaid
graph TB
    Start([Mobile User: AR Experience]) --> OpenApp[Open TalkAR App]
    OpenApp --> CheckPerm{Camera<br/>Permission<br/>Granted?}
    
    CheckPerm -->|No| ShowPermDialog[Show Permission Dialog]
    ShowPermDialog --> UserResponse{User<br/>Grants?}
    UserResponse -->|No| ShowError[Show Error & Exit]
    ShowError --> End([End])
    UserResponse -->|Yes| CheckPerm
    
    CheckPerm -->|Yes| CheckARCore{ARCore<br/>Available?}
    CheckARCore -->|No| ShowARError[Show AR Not Supported]
    ShowARError --> End
    
    CheckARCore -->|Yes| InitARSession[Initialize AR Session]
    InitARSession --> InitSuccess{Init<br/>Success?}
    
    InitSuccess -->|No| RetryInit[Retry Initialization]
    RetryInit --> InitARSession
    InitSuccess -->|Yes| LoadImages[Load Image Database]
    
    LoadImages --> StartCamera[Start Camera Preview]
    StartCamera --> ShowScanUI[Show Scanning Interface]
    ShowScanUI --> ScanLoop[Scan Environment]
    
    ScanLoop --> DetectImage{Image<br/>Detected?}
    DetectImage -->|No| CheckTimeout{Timeout?}
    CheckTimeout -->|No| ScanLoop
    CheckTimeout -->|Yes| ShowHint[Show Scanning Hint]
    ShowHint --> ScanLoop
    
    DetectImage -->|Yes| TrackImage[Track Image with ARCore]
    TrackImage --> TrackQuality{Tracking<br/>Quality OK?}
    
    TrackQuality -->|Poor| ShowTrackError[Show Tracking Error]
    ShowTrackError --> ScanLoop
    TrackQuality -->|Good| FetchContent[Fetch Image Content from API]
    
    FetchContent --> CheckNetwork{Network<br/>Available?}
    CheckNetwork -->|No| CheckLocal{Content<br/>Cached?}
    CheckLocal -->|No| ShowNetError[Show Offline Error]
    ShowNetError --> ScanLoop
    CheckLocal -->|Yes| LoadLocal[Load from Cache]
    LoadLocal --> HasContent
    
    CheckNetwork -->|Yes| DownloadContent[Download Content]
    DownloadContent --> DownloadSuccess{Download<br/>Success?}
    
    DownloadSuccess -->|No| ShowDownError[Show Download Error]
    ShowDownError --> ScanLoop
    DownloadSuccess -->|Yes| CacheContent[Cache Content Locally]
    CacheContent --> HasContent[Content Ready]
    
    HasContent --> RequestVideo[Request Lip-Sync Video]
    RequestVideo --> CheckVideoCache{Video<br/>Cached?}
    
    CheckVideoCache -->|Yes| LoadVideo[Load Cached Video]
    CheckVideoCache -->|No| GenerateVideo[Generate via Sync API]
    
    GenerateVideo --> VideoStatus{Video<br/>Status?}
    VideoStatus -->|Processing| WaitPoll[Wait & Poll]
    WaitPoll --> VideoStatus
    VideoStatus -->|Failed| ShowVideoError[Show Generation Error]
    ShowVideoError --> ScanLoop
    VideoStatus -->|Completed| DownloadVideo[Download Video]
    
    DownloadVideo --> LoadVideo
    LoadVideo --> CreateAnchor[Create AR Anchor on Image]
    CreateAnchor --> PositionOverlay[Position Video Overlay]
    PositionOverlay --> PlayVideo[Play Lip-Sync Video]
    
    PlayVideo --> MonitorTracking[Monitor Image Tracking]
    MonitorTracking --> StillTracked{Image<br/>Still<br/>Tracked?}
    
    StillTracked -->|No| PauseVideo[Pause Video]
    PauseVideo --> ScanLoop
    StillTracked -->|Yes| UserAction{User<br/>Action?}
    
    UserAction -->|Change Language| ShowLangMenu[Show Language Menu]
    ShowLangMenu --> SelectLang[Select Language]
    SelectLang --> RequestVideo
    
    UserAction -->|Continue Watching| MonitorTracking
    UserAction -->|Scan New Image| ScanLoop
    UserAction -->|Exit| CleanupAR[Cleanup AR Session]
    CleanupAR --> End
    
    style Start fill:#4CAF50,stroke:#2E7D32,stroke-width:3px,color:#fff
    style End fill:#f44336,stroke:#c62828,stroke-width:3px,color:#fff
    style PlayVideo fill:#9C27B0,stroke:#6A1B9A,stroke-width:2px,color:#fff
    style CreateAnchor fill:#FF9800,stroke:#E65100,stroke-width:2px,color:#fff
```

---

## 4. Authentication Activity

```mermaid
graph TB
    Start([Start Authentication]) --> UserType{User Type?}
    
    %% Admin Login Flow
    UserType -->|Admin| AdminLogin[Navigate to Login Page]
    AdminLogin --> EnterCreds[Enter Email & Password]
    EnterCreds --> ValidateInput{Valid<br/>Format?}
    
    ValidateInput -->|No| ShowFormatError[Show Format Error]
    ShowFormatError --> EnterCreds
    ValidateInput -->|Yes| SubmitLogin[Submit Login Request]
    
    SubmitLogin --> CheckDB[Query User from Database]
    CheckDB --> UserExists{User<br/>Exists?}
    
    UserExists -->|No| ShowUserError[Show User Not Found]
    ShowUserError --> EnterCreds
    UserExists -->|Yes| VerifyPassword[Verify Password Hash]
    
    VerifyPassword --> PasswordMatch{Password<br/>Matches?}
    PasswordMatch -->|No| IncrementAttempt[Increment Failed Attempts]
    IncrementAttempt --> CheckAttempts{Attempts<br/> > 3?}
    
    CheckAttempts -->|Yes| LockAccount[Lock Account]
    LockAccount --> ShowLockError[Show Account Locked]
    ShowLockError --> End([End])
    CheckAttempts -->|No| ShowPassError[Show Invalid Password]
    ShowPassError --> EnterCreds
    
    PasswordMatch -->|Yes| CheckRole{User<br/>Role?}
    CheckRole -->|Not Admin| ShowRoleError[Show Unauthorized]
    ShowRoleError --> End
    CheckRole -->|Admin| GenerateToken[Generate JWT Token]
    
    GenerateToken --> SetExpiry[Set Token Expiry]
    SetExpiry --> SendToken[Send Token to Client]
    SendToken --> StoreToken[Store Token in Client]
    StoreToken --> LoginSuccess[Login Successful]
    LoginSuccess --> RedirectDashboard[Redirect to Dashboard]
    RedirectDashboard --> End
    
    %% Mobile User Flow
    UserType -->|Mobile User| MobileAuth[Check for Stored Token]
    MobileAuth --> HasToken{Token<br/>Exists?}
    
    HasToken -->|No| GuestAccess[Allow Guest Access]
    GuestAccess --> End
    HasToken -->|Yes| ValidateToken[Validate Token]
    
    ValidateToken --> TokenValid{Token<br/>Valid?}
    TokenValid -->|No| RefreshToken{Can<br/>Refresh?}
    RefreshToken -->|No| ClearToken[Clear Invalid Token]
    ClearToken --> GuestAccess
    RefreshToken -->|Yes| RequestRefresh[Request Token Refresh]
    RequestRefresh --> NewToken[Get New Token]
    NewToken --> StoreNewToken[Store New Token]
    StoreNewToken --> AuthSuccess[Authentication Success]
    
    TokenValid -->|Yes| AuthSuccess
    AuthSuccess --> End
    
    style Start fill:#4CAF50,stroke:#2E7D32,stroke-width:3px,color:#fff
    style End fill:#f44336,stroke:#c62828,stroke-width:3px,color:#fff
    style LoginSuccess fill:#2196F3,stroke:#1565C0,stroke-width:2px,color:#fff
    style AuthSuccess fill:#2196F3,stroke:#1565C0,stroke-width:2px,color:#fff
```

---

## 5. Video Generation Activity

```mermaid
graph TB
    Start([Start Video Generation]) --> ReceiveRequest[Receive Lip-Sync Request]
    ReceiveRequest --> ValidateRequest{Valid<br/>Request?}
    
    ValidateRequest -->|No| ReturnError[Return Validation Error]
    ReturnError --> End([End])
    
    ValidateRequest -->|Yes| ExtractParams[Extract Parameters]
    ExtractParams --> CheckCache{Video<br/>Already<br/>Generated?}
    
    CheckCache -->|Yes| GetCachedURL[Get Cached Video URL]
    GetCachedURL --> ReturnURL[Return Video URL]
    ReturnURL --> End
    
    CheckCache -->|No| CheckService{Service<br/>Type?}
    
    %% Mock Service Flow
    CheckService -->|Mock/Dev| GenerateID[Generate Unique Video ID]
    GenerateID --> SelectMockURL[Select Mock Video URL]
    SelectMockURL --> SimulateDelay[Simulate Processing Delay]
    SimulateDelay --> StoreInMemory[Store in Memory Cache]
    StoreInMemory --> SetExpiry[Set 24h Expiry]
    SetExpiry --> ReturnMockResponse[Return Mock Response]
    ReturnMockResponse --> End
    
    %% Real Sync API Flow
    CheckService -->|Production| PreparePayload[Prepare API Payload]
    PreparePayload --> AddText[Add Script Text]
    AddText --> AddVoice[Add Voice ID]
    AddVoice --> AddLanguage[Add Language]
    AddLanguage --> AddImage[Add Image URL]
    
    AddImage --> CallSyncAPI[Call Sync API]
    CallSyncAPI --> APIResponse{API<br/>Response?}
    
    APIResponse -->|Error| CheckRetry{Retry<br/>Count < 3?}
    CheckRetry -->|No| LogError[Log Error]
    LogError --> ReturnAPIError[Return API Error]
    ReturnAPIError --> End
    CheckRetry -->|Yes| IncrementRetry[Increment Retry]
    IncrementRetry --> WaitBackoff[Wait Exponential Backoff]
    WaitBackoff --> CallSyncAPI
    
    APIResponse -->|Success| GetJobID[Extract Job ID]
    GetJobID --> SaveJobDB[Save Job to Database]
    SaveJobDB --> ReturnJobID[Return Job ID to Client]
    
    ReturnJobID --> StartPolling[Start Status Polling]
    StartPolling --> PollStatus[Poll Job Status]
    PollStatus --> CheckStatus{Job<br/>Status?}
    
    CheckStatus -->|Processing| WaitInterval[Wait 2 Seconds]
    WaitInterval --> CheckTimeout{Timeout<br/>Reached?}
    CheckTimeout -->|Yes| TimeoutError[Return Timeout Error]
    TimeoutError --> End
    CheckTimeout -->|No| PollStatus
    
    CheckStatus -->|Failed| GetErrorDetails[Get Error Details]
    GetErrorDetails --> LogFailure[Log Failure]
    LogFailure --> ReturnFailure[Return Failure Response]
    ReturnFailure --> End
    
    CheckStatus -->|Completed| GetVideoURL[Extract Video URL]
    GetVideoURL --> ValidateURL{Valid<br/>URL?}
    
    ValidateURL -->|No| URLError[Return URL Error]
    URLError --> End
    ValidateURL -->|Yes| DownloadVideo[Download Video]
    
    DownloadVideo --> UploadS3{Upload to<br/>S3?}
    UploadS3 -->|Yes| StoreS3[Store in AWS S3]
    StoreS3 --> GetS3URL[Get S3 URL]
    GetS3URL --> UpdateDB
    
    UploadS3 -->|No| UpdateDB[Update Database with URL]
    UpdateDB --> CacheVideo[Cache Video URL]
    CacheVideo --> SendNotification[Send Completion Notification]
    SendNotification --> TrackMetrics[Track Analytics Metrics]
    TrackMetrics --> ReturnSuccess[Return Success Response]
    ReturnSuccess --> End
    
    style Start fill:#4CAF50,stroke:#2E7D32,stroke-width:3px,color:#fff
    style End fill:#f44336,stroke:#c62828,stroke-width:3px,color:#fff
    style ReturnSuccess fill:#2196F3,stroke:#1565C0,stroke-width:2px,color:#fff
    style CallSyncAPI fill:#FF9800,stroke:#E65100,stroke-width:2px,color:#fff
```

---

## 6. Image Recognition Activity

```mermaid
graph TB
    Start([Start Image Recognition]) --> InitService[Initialize Recognition Service]
    InitService --> ServiceType{Service<br/>Type?}
    
    %% ARCore Recognition Flow
    ServiceType -->|ARCore| CheckARCore[Check ARCore Availability]
    CheckARCore --> ARAvailable{ARCore<br/>Available?}
    
    ARAvailable -->|No| FallbackMLKit[Fallback to ML Kit]
    ARAvailable -->|Yes| CreateSession[Create AR Session]
    
    CreateSession --> ConfigSession[Configure Session]
    ConfigSession --> LoadDatabase[Load Image Database]
    LoadDatabase --> AddImages[Add Reference Images]
    AddImages --> StartARSession[Start AR Session]
    
    StartARSession --> GetFrame[Get Camera Frame]
    GetFrame --> ProcessFrame[Process Frame]
    ProcessFrame --> DetectImages[Detect Augmented Images]
    
    DetectImages --> ImagesFound{Images<br/>Detected?}
    ImagesFound -->|No| ContinueAR[Continue Scanning]
    ContinueAR --> GetFrame
    
    ImagesFound -->|Yes| ExtractImage[Extract Augmented Image]
    ExtractImage --> CheckTracking{Tracking<br/>State?}
    
    CheckTracking -->|Not Tracking| ContinueAR
    CheckTracking -->|Tracking| GetImageData[Get Image Metadata]
    GetImageData --> MatchDatabase[Match with Database]
    MatchDatabase --> ImageMatched{Match<br/>Found?}
    
    ImageMatched -->|No| ContinueAR
    ImageMatched -->|Yes| CreateAnchor[Create AR Anchor]
    CreateAnchor --> GetPose[Get Image Pose]
    GetPose --> TriggerCallback[Trigger Recognition Callback]
    TriggerCallback --> MonitorTracking[Monitor Tracking]
    
    MonitorTracking --> StillTracking{Still<br/>Tracking?}
    StillTracking -->|Yes| UpdatePose[Update Pose]
    UpdatePose --> MonitorTracking
    StillTracking -->|No| RemoveAnchor[Remove Anchor]
    RemoveAnchor --> ContinueAR
    
    %% ML Kit Recognition Flow
    ServiceType -->|ML Kit| FallbackMLKit
    FallbackMLKit --> InitMLKit[Initialize ML Kit]
    InitMLKit --> GetMLFrame[Get Camera Frame]
    GetMLFrame --> ConvertBitmap[Convert to Bitmap]
    
    ConvertBitmap --> LabelImage[Label Image]
    LabelImage --> DetectObjects[Detect Objects]
    DetectObjects --> GetLabels[Get Labels & Confidence]
    
    GetLabels --> AnalyzeLabels{Confidence<br/> > 0.7?}
    AnalyzeLabels -->|No| ContinueML[Continue Scanning]
    ContinueML --> GetMLFrame
    
    AnalyzeLabels -->|Yes| MatchLabels[Match with Registered Images]
    MatchLabels --> LabelMatch{Match<br/>Found?}
    
    LabelMatch -->|No| ContinueML
    LabelMatch -->|Yes| EstimatePosition[Estimate Position]
    EstimatePosition --> TriggerMLCallback[Trigger Recognition Callback]
    TriggerMLCallback --> ContinueML
    
    TriggerCallback --> End([End Recognition])
    TriggerMLCallback --> End
    
    style Start fill:#4CAF50,stroke:#2E7D32,stroke-width:3px,color:#fff
    style End fill:#f44336,stroke:#c62828,stroke-width:3px,color:#fff
    style CreateAnchor fill:#2196F3,stroke:#1565C0,stroke-width:2px,color:#fff
    style TriggerCallback fill:#9C27B0,stroke:#6A1B9A,stroke-width:2px,color:#fff
```

---

## 7. Content Synchronization Activity

```mermaid
graph TB
    Start([Start Content Sync]) --> CheckTrigger{Sync<br/>Trigger?}
    
    %% Manual Sync
    CheckTrigger -->|Manual| UserInitiated[User Pulls to Refresh]
    UserInitiated --> ShowLoading[Show Loading Indicator]
    
    %% Auto Sync
    CheckTrigger -->|Auto| CheckInterval[Check Last Sync Time]
    CheckInterval --> IntervalPassed{Interval<br/>Passed?}
    IntervalPassed -->|No| SkipSync[Skip Sync]
    SkipSync --> End([End])
    IntervalPassed -->|Yes| ShowLoading
    
    %% App Launch Sync
    CheckTrigger -->|App Launch| CheckConnection[Check Network]
    CheckConnection --> HasConnection{Connected?}
    HasConnection -->|No| LoadCached[Load Cached Data]
    LoadCached --> End
    HasConnection -->|Yes| ShowLoading
    
    %% Sync Process
    ShowLoading --> GetLocalVersion[Get Local DB Version]
    GetLocalVersion --> CallAPI[Call Sync API]
    CallAPI --> APISuccess{API<br/>Success?}
    
    APISuccess -->|No| HandleError{Error<br/>Type?}
    HandleError -->|Network| ShowNetworkError[Show Network Error]
    HandleError -->|Auth| RefreshAuth[Refresh Authentication]
    RefreshAuth --> CallAPI
    HandleError -->|Server| ShowServerError[Show Server Error]
    
    ShowNetworkError --> LoadCached
    ShowServerError --> LoadCached
    
    APISuccess -->|Yes| GetServerVersion[Get Server Version]
    GetServerVersion --> CompareVersions{Versions<br/>Match?}
    
    CompareVersions -->|Yes| NoUpdate[No Updates Available]
    NoUpdate --> HideLoading[Hide Loading]
    HideLoading --> End
    
    CompareVersions -->|No| FetchUpdates[Fetch Updated Content]
    FetchUpdates --> ReceiveData[Receive Data Package]
    ReceiveData --> ValidateData{Data<br/>Valid?}
    
    ValidateData -->|No| ShowDataError[Show Data Error]
    ShowDataError --> LoadCached
    ValidateData -->|Yes| ParseImages[Parse Images]
    
    ParseImages --> ParseDialogues[Parse Dialogues]
    ParseDialogues --> ParseAvatars[Parse Avatars]
    ParseAvatars --> BeginTransaction[Begin Database Transaction]
    
    BeginTransaction --> ClearOld[Clear Old Data]
    ClearOld --> InsertImages[Insert New Images]
    InsertImages --> InsertDialogues[Insert New Dialogues]
    InsertDialogues --> InsertAvatars[Insert New Avatars]
    InsertAvatars --> InsertMappings[Insert Mappings]
    
    InsertMappings --> UpdateVersion[Update Local Version]
    UpdateVersion --> CommitTransaction{Commit<br/>Success?}
    
    CommitTransaction -->|No| Rollback[Rollback Transaction]
    Rollback --> ShowDBError[Show Database Error]
    ShowDBError --> LoadCached
    
    CommitTransaction -->|Yes| DownloadMedia[Download Media Files]
    DownloadMedia --> MediaType{Media<br/>Type?}
    
    MediaType -->|Images| DownloadImages[Download Images]
    MediaType -->|Videos| DownloadVideos[Download Videos]
    
    DownloadImages --> CacheImages[Cache Images Locally]
    DownloadVideos --> CacheVideos[Cache Videos Locally]
    
    CacheImages --> CheckMoreMedia{More<br/>Media?}
    CacheVideos --> CheckMoreMedia
    
    CheckMoreMedia -->|Yes| DownloadMedia
    CheckMoreMedia -->|No| UpdateUI[Update UI with New Data]
    
    UpdateUI --> NotifyUser[Show Success Message]
    NotifyUser --> SetSyncTime[Update Last Sync Time]
    SetSyncTime --> TrackAnalytics[Track Sync Event]
    TrackAnalytics --> HideLoading
    
    style Start fill:#4CAF50,stroke:#2E7D32,stroke-width:3px,color:#fff
    style End fill:#f44336,stroke:#c62828,stroke-width:3px,color:#fff
    style CommitTransaction fill:#2196F3,stroke:#1565C0,stroke-width:2px,color:#fff
    style UpdateUI fill:#9C27B0,stroke:#6A1B9A,stroke-width:2px,color:#fff
```

---

## Activity Diagram Summary

### Overview Table

| Diagram # | Name | Purpose | Key Activities |
|-----------|------|---------|----------------|
| 1 | **Main System** | Overall system flow | Admin & Mobile user paths |
| 2 | **Admin Upload** | Content management | Upload image, create script, map avatar |
| 3 | **Mobile AR** | AR experience | Scan, recognize, display, play video |
| 4 | **Authentication** | User login & security | Login, verify, generate token |
| 5 | **Video Generation** | Lip-sync creation | Request, process, cache video |
| 6 | **Image Recognition** | AR detection | ARCore + ML Kit recognition |
| 7 | **Content Sync** | Data synchronization | Fetch, parse, cache content |

---

## Activity Diagram Legend

### Shapes Explained

| Shape | Symbol | Meaning |
|-------|--------|---------|
| **Start/End** | `([...])` | Activity start or end point |
| **Activity** | `[...]` | An action or process step |
| **Decision** | `{...?}` | Conditional branch (if/else) |
| **Process** | `[...]` | Standard process/action |
| **Database** | `[(...)]` | Database operation |

### Decision Outcomes

```
{Decision?} -->|Yes| NextStep
{Decision?} -->|No| OtherStep
```

---

## How to Use These Diagrams

### 🚀 Quick View (Mermaid Live):
1. Visit: https://mermaid.live
2. Copy any diagram code
3. Paste and view
4. Export as PNG/SVG

### 📂 GitHub:
```bash
git add ACTIVITY_DIAGRAMS.md
git commit -m "Add UML activity diagrams"
git push
# Auto-renders on GitHub!
```

### 💻 VS Code:
1. Install "Markdown Preview Mermaid Support"
2. Open `ACTIVITY_DIAGRAMS.md`
3. Press `Ctrl+Shift+V`

---

## Best Practices Shown

✅ **Clear Flow** - Top to bottom, left to right  
✅ **Decision Points** - Diamond shapes for choices  
✅ **Error Handling** - Alternative paths for failures  
✅ **Loops** - Retry and polling mechanisms  
✅ **Parallel Activities** - Concurrent operations  
✅ **Color Coding** - Visual hierarchy  
✅ **Comprehensive** - All major scenarios covered  

---

**Created**: October 8, 2025  
**Diagrams**: 7 comprehensive activity flows  
**Standard**: UML 2.0 Activity Diagram notation  
**Format**: Mermaid graph syntax
