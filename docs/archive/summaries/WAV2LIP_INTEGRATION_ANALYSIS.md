# Wav2Lip Integration Analysis for TalkAR

## What is Wav2Lip?

**Wav2Lip** is a deep learning model that generates accurate lip-sync for talking face videos from audio.

**Key Features:**
- ✅ Generates realistic lip movements from audio
- ✅ Works with any face image/video
- ✅ High-quality lip-sync accuracy
- ✅ Open-source (MIT License)
- ✅ Pre-trained models available

**GitHub**: https://github.com/Rudrabha/Wav2Lip

---

## Can We Use Wav2Lip? YES! ✅

### Deployment Options

#### Option 1: Backend Integration (Recommended)
**Deploy Wav2Lip on your backend server**

**Pros:**
- ✅ No mobile device performance issues
- ✅ Centralized model updates
- ✅ Works on all devices (even low-end)
- ✅ Can cache generated videos
- ✅ Better quality control

**Cons:**
- ❌ Requires server GPU (for fast generation)
- ❌ Network latency for video generation
- ❌ Server costs for GPU instances

**Architecture:**
```
Mobile App → Backend API → Wav2Lip Service → Generated Video → Mobile App
```

#### Option 2: On-Device (Not Recommended)
**Run Wav2Lip directly on Android**

**Pros:**
- ✅ No network dependency
- ✅ Instant generation
- ✅ Privacy (no data sent to server)

**Cons:**
- ❌ Very slow on mobile (minutes per video)
- ❌ High battery consumption
- ❌ Large model size (~300MB)
- ❌ Requires high-end devices
- ❌ Complex TensorFlow Lite conversion

---

## Recommended Architecture: Backend Integration

### System Flow

```
┌─────────────┐
│ Mobile App  │
│  (Android)  │
└──────┬──────┘
       │ 1. Scan poster
       │ 2. Send image + audio/text
       ▼
┌─────────────────────┐
│   Backend API       │
│   (Node.js/Python)  │
└──────┬──────────────┘
       │ 3. Process request
       ▼
┌─────────────────────┐
│  Wav2Lip Service    │
│  (Python + PyTorch) │
│  - Face detection   │
│  - Lip-sync gen     │
│  - Video encoding   │
└──────┬──────────────┘
       │ 4. Generated video
       ▼
┌─────────────────────┐
│  Video Storage      │
│  (S3/Cloud Storage) │
└──────┬──────────────┘
       │ 5. Video URL
       ▼
┌─────────────┐
│ Mobile App  │
│ Plays video │
└─────────────┘
```

---

## Backend Implementation Plan

### 1. Wav2Lip Service Setup

**Requirements:**
- Python 3.7+
- PyTorch
- CUDA (for GPU acceleration)
- FFmpeg

**Installation:**
```bash
# Clone Wav2Lip
git clone https://github.com/Rudrabha/Wav2Lip.git
cd Wav2Lip

# Install dependencies
pip install -r requirements.txt

# Download pre-trained model
wget 'https://iiitaphyd-my.sharepoint.com/:u:/g/personal/radrabha_m_research_iiit_ac_in/Eb3LEzbfuKlJiR600lQWRxgBIY27JZg80f7V9jtMfbNDaQ?download=1' -O 'checkpoints/wav2lip_gan.pth'
```

### 2. Create Wav2Lip API Service

**File: `backend/src/services/wav2lipService.py`**

```python
import os
import subprocess
import uuid
from pathlib import Path
import torch

class Wav2LipService:
    def __init__(self):
        self.wav2lip_path = os.getenv('WAV2LIP_PATH', '/path/to/Wav2Lip')
        self.checkpoint_path = f'{self.wav2lip_path}/checkpoints/wav2lip_gan.pth'
        self.output_dir = '/tmp/wav2lip_outputs'
        os.makedirs(self.output_dir, exist_ok=True)
        
    def generate_lipsync_video(
        self,
        face_image_path: str,
        audio_path: str,
        output_filename: str = None
    ) -> dict:
        """
        Generate lip-synced video from face image and audio.
        
        Args:
            face_image_path: Path to face image (poster)
            audio_path: Path to audio file (speech/TTS)
            output_filename: Optional output filename
            
        Returns:
            dict with video_path, duration, status
        """
        try:
            # Generate unique output filename
            if not output_filename:
                output_filename = f"{uuid.uuid4()}.mp4"
            
            output_path = os.path.join(self.output_dir, output_filename)
            
            # Run Wav2Lip inference
            cmd = [
                'python',
                f'{self.wav2lip_path}/inference.py',
                '--checkpoint_path', self.checkpoint_path,
                '--face', face_image_path,
                '--audio', audio_path,
                '--outfile', output_path,
                '--pads', '0', '10', '0', '0',  # Padding for better results
                '--resize_factor', '1',
                '--nosmooth'  # Disable smoothing for faster processing
            ]
            
            # Execute Wav2Lip
            result = subprocess.run(
                cmd,
                capture_output=True,
                text=True,
                timeout=60  # 60 second timeout
            )
            
            if result.returncode != 0:
                raise Exception(f"Wav2Lip failed: {result.stderr}")
            
            # Get video duration
            duration = self._get_video_duration(output_path)
            
            return {
                'success': True,
                'video_path': output_path,
                'duration': duration,
                'status': 'completed'
            }
            
        except Exception as e:
            return {
                'success': False,
                'error': str(e),
                'status': 'failed'
            }
    
    def _get_video_duration(self, video_path: str) -> float:
        """Get video duration using ffprobe."""
        cmd = [
            'ffprobe',
            '-v', 'error',
            '-show_entries', 'format=duration',
            '-of', 'default=noprint_wrappers=1:nokey=1',
            video_path
        ]
        result = subprocess.run(cmd, capture_output=True, text=True)
        return float(result.stdout.strip())
```

### 3. Node.js Backend Integration

**File: `backend/src/services/wav2lipIntegrationService.ts`**

```typescript
import { spawn } from 'child_process';
import { v4 as uuidv4 } from 'uuid';
import path from 'path';
import fs from 'fs/promises';

export interface Wav2LipRequest {
  imageId: string;
  imagePath: string;  // Path to poster image
  audioPath: string;  // Path to TTS audio
  text?: string;      // Original text (for logging)
}

export interface Wav2LipResponse {
  success: boolean;
  videoId: string;
  videoUrl?: string;
  videoPath?: string;
  duration?: number;
  status: 'processing' | 'completed' | 'failed';
  error?: string;
}

export class Wav2LipIntegrationService {
  private static pythonScriptPath = process.env.WAV2LIP_SCRIPT_PATH || 
    '/path/to/backend/src/services/wav2lipService.py';
  
  private static outputDir = process.env.WAV2LIP_OUTPUT_DIR || 
    '/tmp/wav2lip_videos';
  
  /**
   * Generate lip-synced video using Wav2Lip
   */
  static async generateLipSyncVideo(
    request: Wav2LipRequest
  ): Promise<Wav2LipResponse> {
    const videoId = uuidv4();
    
    console.log(`[WAV2LIP] Starting generation for videoId: ${videoId}`);
    console.log(`[WAV2LIP] Image: ${request.imagePath}`);
    console.log(`[WAV2LIP] Audio: ${request.audioPath}`);
    
    try {
      // Verify input files exist
      await this.verifyFiles(request.imagePath, request.audioPath);
      
      // Call Python Wav2Lip service
      const result = await this.callWav2LipPython(
        request.imagePath,
        request.audioPath,
        videoId
      );
      
      if (!result.success) {
        throw new Error(result.error || 'Wav2Lip generation failed');
      }
      
      // Upload to cloud storage (optional)
      const videoUrl = await this.uploadToStorage(result.video_path);
      
      console.log(`[WAV2LIP] ✅ Video generated: ${videoId} -> ${videoUrl}`);
      
      return {
        success: true,
        videoId,
        videoUrl,
        videoPath: result.video_path,
        duration: result.duration,
        status: 'completed'
      };
      
    } catch (error) {
      console.error(`[WAV2LIP] ❌ Error: ${error}`);
      
      return {
        success: false,
        videoId,
        status: 'failed',
        error: error.message
      };
    }
  }
  
  /**
   * Call Python Wav2Lip service
   */
  private static async callWav2LipPython(
    imagePath: string,
    audioPath: string,
    videoId: string
  ): Promise<any> {
    return new Promise((resolve, reject) => {
      const python = spawn('python3', [
        this.pythonScriptPath,
        '--face', imagePath,
        '--audio', audioPath,
        '--video_id', videoId
      ]);
      
      let stdout = '';
      let stderr = '';
      
      python.stdout.on('data', (data) => {
        stdout += data.toString();
      });
      
      python.stderr.on('data', (data) => {
        stderr += data.toString();
      });
      
      python.on('close', (code) => {
        if (code !== 0) {
          reject(new Error(`Python process exited with code ${code}: ${stderr}`));
        } else {
          try {
            const result = JSON.parse(stdout);
            resolve(result);
          } catch (e) {
            reject(new Error(`Failed to parse Python output: ${stdout}`));
          }
        }
      });
      
      // Timeout after 2 minutes
      setTimeout(() => {
        python.kill();
        reject(new Error('Wav2Lip generation timeout'));
      }, 120000);
    });
  }
  
  /**
   * Verify input files exist
   */
  private static async verifyFiles(...paths: string[]): Promise<void> {
    for (const filePath of paths) {
      try {
        await fs.access(filePath);
      } catch {
        throw new Error(`File not found: ${filePath}`);
      }
    }
  }
  
  /**
   * Upload video to cloud storage (S3, etc.)
   */
  private static async uploadToStorage(videoPath: string): Promise<string> {
    // TODO: Implement S3/cloud storage upload
    // For now, return local path
    return `http://your-server.com/videos/${path.basename(videoPath)}`;
  }
}
```

### 4. Update Backend API Route

**File: `backend/src/routes/lipsync.ts`**

```typescript
import { Router } from 'express';
import { Wav2LipIntegrationService } from '../services/wav2lipIntegrationService';
import { TTSService } from '../services/ttsService';

const router = Router();

/**
 * POST /api/lipsync/generate-talking-photo
 * Generate talking photo with Wav2Lip
 */
router.post('/generate-talking-photo', async (req, res) => {
  try {
    const { imageId, text, voiceId } = req.body;
    
    // 1. Get poster image path
    const imagePath = await getImagePath(imageId);
    
    // 2. Generate TTS audio
    const audioResult = await TTSService.generateAudio(text, voiceId);
    
    if (!audioResult.success) {
      return res.status(500).json({
        success: false,
        error: 'TTS generation failed'
      });
    }
    
    // 3. Generate lip-sync video with Wav2Lip
    const lipSyncResult = await Wav2LipIntegrationService.generateLipSyncVideo({
      imageId,
      imagePath,
      audioPath: audioResult.audioPath,
      text
    });
    
    res.json(lipSyncResult);
    
  } catch (error) {
    console.error('[API] Error generating talking photo:', error);
    res.status(500).json({
      success: false,
      error: error.message
    });
  }
});

export default router;
```

---

## Mobile App Integration

### Update API Client

**File: `mobile-app/app/src/main/java/com/talkar/app/data/api/ApiClient.kt`**

```kotlin
// Add new endpoint for Wav2Lip
@POST("lipsync/generate-talking-photo")
suspend fun generateTalkingPhoto(
    @Body request: TalkingPhotoRequest
): Response<Wav2LipResponse>

data class TalkingPhotoRequest(
    val imageId: String,
    val text: String,
    val voiceId: String
)

data class Wav2LipResponse(
    val success: Boolean,
    val videoId: String,
    val videoUrl: String?,
    val duration: Float?,
    val status: String,
    val error: String?
)
```

### Update ConversationalARService

```kotlin
// In ConversationalARService.kt
suspend fun generateTalkingPhoto(
    posterId: String,
    text: String,
    voiceId: String = "default"
): String? {
    return withContext(Dispatchers.IO) {
        try {
            val response = apiService.generateTalkingPhoto(
                TalkingPhotoRequest(
                    imageId = posterId,
                    text = text,
                    voiceId = voiceId
                )
            )
            
            if (response.isSuccessful && response.body()?.success == true) {
                response.body()?.videoUrl
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating talking photo", e)
            null
        }
    }
}
```

---

## Performance Considerations

### Generation Time
- **With GPU**: 2-5 seconds per video
- **Without GPU (CPU only)**: 30-60 seconds per video

**Recommendation**: Use GPU-enabled server (AWS EC2 with GPU, Google Cloud GPU instances)

### Caching Strategy
```typescript
// Cache generated videos to avoid regeneration
const cacheKey = `${imageId}_${textHash}_${voiceId}`;
const cachedVideo = await videoCache.get(cacheKey);

if (cachedVideo) {
  return cachedVideo; // Return cached video URL
}

// Generate new video
const newVideo = await Wav2LipIntegrationService.generateLipSyncVideo(...);

// Cache for 24 hours
await videoCache.set(cacheKey, newVideo, 86400);
```

---

## Cost Estimation

### Server Requirements
- **GPU Instance**: AWS EC2 g4dn.xlarge (~$0.50/hour)
- **Storage**: S3 for video storage (~$0.023/GB)
- **Bandwidth**: CloudFront for video delivery

### Per-Video Cost
- Generation: ~$0.001 per video (with caching)
- Storage: ~$0.0001 per video per month
- Delivery: ~$0.01 per 100 views

---

## Alternative: Third-Party Services

If you don't want to host Wav2Lip yourself:

### Option 1: D-ID (Recommended)
- **Service**: https://www.d-id.com/
- **Features**: Talking photos, lip-sync, multiple voices
- **Pricing**: Pay-per-use API
- **Quality**: Production-ready

### Option 2: Synthesia
- **Service**: https://www.synthesia.io/
- **Features**: AI avatars, lip-sync
- **Pricing**: Subscription-based

### Option 3: Rephrase.ai
- **Service**: https://www.rephrase.ai/
- **Features**: Talking photos, personalized videos
- **Pricing**: Enterprise

---

## Recommendation

**For TalkAR, I recommend:**

1. ✅ **Use Wav2Lip on backend** (self-hosted)
   - Full control over quality
   - No per-video API costs
   - Can customize for your use case

2. ✅ **Deploy on GPU server**
   - AWS EC2 g4dn.xlarge or similar
   - Fast generation (2-5 seconds)

3. ✅ **Implement caching**
   - Cache generated videos
   - Reduce server load
   - Faster response times

4. ✅ **Update current spec**
   - Modify requirements for Wav2Lip integration
   - Add backend video generation phase
   - Keep Phase 1 foundation (still useful)

---

## Next Steps

**Should we:**
1. Update the spec to include Wav2Lip integration?
2. Add backend video generation phase?
3. Modify mobile app to use backend-generated videos?

**Or would you prefer to:**
- Use a third-party service like D-ID instead?
- Implement a simpler approach first?
