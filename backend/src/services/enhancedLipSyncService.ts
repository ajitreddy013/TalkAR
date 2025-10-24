import { v4 as uuidv4 } from 'uuid';

// Enhanced lip-sync service for Week 3
export interface LipSyncRequest {
  text: string;
  voiceId: string;
  language?: string;
  imageId: string;
  scriptId: string;
}

export interface LipSyncResponse {
  success: boolean;
  videoId: string;
  videoUrl?: string;
  status: 'processing' | 'completed' | 'failed';
  message?: string;
  processingTime?: number;
  estimatedDuration?: number;
}

export interface VideoStorage {
  videoId: string;
  imageId: string;
  scriptId: string;
  text: string;
  voiceId: string;
  videoUrl: string;
  status: 'processing' | 'completed' | 'failed';
  createdAt: Date;
  expiresAt: Date;
  processingTime?: number;
}

// In-memory storage for demo (in production, use Redis or database)
const videoStorage = new Map<string, VideoStorage>();

// Mock video URLs for different voices and content
const mockVideoUrls = {
  'voice_001': [
    'https://mock-lipsync-videos.com/emma_001.mp4',
    'https://mock-lipsync-videos.com/emma_002.mp4',
    'https://mock-lipsync-videos.com/emma_003.mp4',
    'https://mock-lipsync-videos.com/emma_004.mp4',
  ],
  'voice_002': [
    'https://mock-lipsync-videos.com/james_001.mp4',
    'https://mock-lipsync-videos.com/james_002.mp4',
    'https://mock-lipsync-videos.com/james_003.mp4',
    'https://mock-lipsync-videos.com/james_004.mp4',
  ],
  'voice_003': [
    'https://mock-lipsync-videos.com/sophie_001.mp4',
    'https://mock-lipsync-videos.com/sophie_002.mp4',
    'https://mock-lipsync-videos.com/sophie_003.mp4',
    'https://mock-lipsync-videos.com/sophie_004.mp4',
  ],
  'voice_004': [
    'https://mock-lipsync-videos.com/david_001.mp4',
    'https://mock-lipsync-videos.com/david_002.mp4',
    'https://mock-lipsync-videos.com/david_003.mp4',
    'https://mock-lipsync-videos.com/david_004.mp4',
  ],
};

export class EnhancedLipSyncService {
  /**
   * Generate lip-sync video for given text and voice
   */
  static async generateLipSyncVideo(request: LipSyncRequest): Promise<LipSyncResponse> {
    const startTime = Date.now();
    const videoId = uuidv4();
    
    console.log(`[LIPSYNC] Starting generation for videoId: ${videoId}, text: "${request.text.substring(0, 50)}..."`);

    try {
      // Simulate processing time (1-3 seconds)
      const processingTime = Math.random() * 2000 + 1000;
      
      // Store video in processing state
      const videoEntry: VideoStorage = {
        videoId,
        imageId: request.imageId,
        scriptId: request.scriptId,
        text: request.text,
        voiceId: request.voiceId,
        videoUrl: '', // Will be set when completed
        status: 'processing',
        createdAt: new Date(),
        expiresAt: new Date(Date.now() + 24 * 60 * 60 * 1000), // 24 hours
        processingTime: processingTime,
      };

      videoStorage.set(videoId, videoEntry);

      // Simulate async processing
      setTimeout(() => {
        this.completeVideoGeneration(videoId, request);
      }, processingTime);

      const response: LipSyncResponse = {
        success: true,
        videoId,
        status: 'processing',
        message: 'Video generation started',
        processingTime: processingTime,
        estimatedDuration: Math.ceil(request.text.length / 15), // Rough estimate: 15 chars per second
      };

      console.log(`[LIPSYNC] Video generation initiated: ${videoId}`);
      return response;

    } catch (error) {
      console.error(`[LIPSYNC] Error generating video: ${error}`);
      return {
        success: false,
        videoId,
        status: 'failed',
        message: 'Failed to start video generation',
      };
    }
  }

  /**
   * Complete video generation (simulated)
   */
  private static async completeVideoGeneration(videoId: string, request: LipSyncRequest): Promise<void> {
    try {
      const storage = videoStorage.get(videoId);
      if (!storage) {
        console.error(`[LIPSYNC] Video storage not found: ${videoId}`);
        return;
      }

      // Select appropriate mock video URL
      const voiceUrls = mockVideoUrls[request.voiceId as keyof typeof mockVideoUrls] || mockVideoUrls['voice_001'];
      const randomUrl = voiceUrls[Math.floor(Math.random() * voiceUrls.length)];

      // Update storage with completed video
      storage.videoUrl = randomUrl;
      storage.status = 'completed';
      storage.processingTime = Date.now() - storage.createdAt.getTime();

      videoStorage.set(videoId, storage);

      console.log(`[LIPSYNC] Video generation completed: ${videoId} -> ${randomUrl}`);
    } catch (error) {
      console.error(`[LIPSYNC] Error completing video generation: ${error}`);
      
      const storage = videoStorage.get(videoId);
      if (storage) {
        storage.status = 'failed';
        videoStorage.set(videoId, storage);
      }
    }
  }

  /**
   * Get video status and URL
   */
  static async getVideoStatus(videoId: string): Promise<LipSyncResponse> {
    const storage = videoStorage.get(videoId);
    
    if (!storage) {
      return {
        success: false,
        videoId,
        status: 'failed',
        message: 'Video not found',
      };
    }

    // Check if video has expired
    if (storage.expiresAt < new Date()) {
      videoStorage.delete(videoId);
      return {
        success: false,
        videoId,
        status: 'failed',
        message: 'Video has expired',
      };
    }

    return {
      success: true,
      videoId,
      videoUrl: storage.videoUrl,
      status: storage.status,
      message: storage.status === 'completed' ? 'Video ready' : 'Video processing',
      processingTime: storage.processingTime,
    };
  }

  /**
   * Get all videos for an image
   */
  static async getVideosForImage(imageId: string): Promise<VideoStorage[]> {
    const videos: VideoStorage[] = [];
    
    for (const [videoId, storage] of videoStorage.entries()) {
      if (storage.imageId === imageId && storage.expiresAt > new Date()) {
        videos.push(storage);
      }
    }

    return videos.sort((a, b) => b.createdAt.getTime() - a.createdAt.getTime());
  }

  /**
   * Clean up expired videos
   */
  static async cleanupExpiredVideos(): Promise<number> {
    const now = new Date();
    let cleanedCount = 0;

    for (const [videoId, storage] of videoStorage.entries()) {
      if (storage.expiresAt < now) {
        videoStorage.delete(videoId);
        cleanedCount++;
      }
    }

    if (cleanedCount > 0) {
      console.log(`[LIPSYNC] Cleaned up ${cleanedCount} expired videos`);
    }

    return cleanedCount;
  }

  /**
   * Get analytics data
   */
  static async getAnalytics(): Promise<{
    totalVideos: number;
    completedVideos: number;
    processingVideos: number;
    failedVideos: number;
    averageProcessingTime: number;
  }> {
    const videos = Array.from(videoStorage.values());
    const now = new Date();
    const activeVideos = videos.filter(v => v.expiresAt > now);

    const completed = activeVideos.filter(v => v.status === 'completed');
    const processing = activeVideos.filter(v => v.status === 'processing');
    const failed = activeVideos.filter(v => v.status === 'failed');

    const avgProcessingTime = completed.length > 0 
      ? completed.reduce((sum, v) => sum + (v.processingTime || 0), 0) / completed.length
      : 0;

    return {
      totalVideos: activeVideos.length,
      completedVideos: completed.length,
      processingVideos: processing.length,
      failedVideos: failed.length,
      averageProcessingTime: Math.round(avgProcessingTime),
    };
  }
}

// Cleanup expired videos every hour
setInterval(() => {
  EnhancedLipSyncService.cleanupExpiredVideos();
}, 60 * 60 * 1000);
