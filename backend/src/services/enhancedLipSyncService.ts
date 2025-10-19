import { v4 as uuidv4 } from "uuid";
import { ScriptService } from "./scriptService";
import { AnalyticsService } from "./analyticsService";

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
  status: "processing" | "completed" | "failed";
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
  status: "processing" | "completed" | "failed";
  createdAt: Date;
  expiresAt: Date;
  processingTime?: number;
}

// In-memory storage for demo (in production, use Redis or database)
const videoStorage = new Map<string, VideoStorage>();

// Mock video URLs for different voices and content
const mockVideoUrls = {
  voice_001: [
    "https://mock-lipsync-videos.com/emma_001.mp4",
    "https://mock-lipsync-videos.com/emma_002.mp4",
    "https://mock-lipsync-videos.com/emma_003.mp4",
    "https://mock-lipsync-videos.com/emma_004.mp4",
  ],
  voice_002: [
    "https://mock-lipsync-videos.com/james_001.mp4",
    "https://mock-lipsync-videos.com/james_002.mp4",
    "https://mock-lipsync-videos.com/james_003.mp4",
    "https://mock-lipsync-videos.com/james_004.mp4",
  ],
  voice_003: [
    "https://mock-lipsync-videos.com/sophie_001.mp4",
    "https://mock-lipsync-videos.com/sophie_002.mp4",
    "https://mock-lipsync-videos.com/sophie_003.mp4",
    "https://mock-lipsync-videos.com/sophie_004.mp4",
  ],
  voice_004: [
    "https://mock-lipsync-videos.com/david_001.mp4",
    "https://mock-lipsync-videos.com/david_002.mp4",
    "https://mock-lipsync-videos.com/david_003.mp4",
    "https://mock-lipsync-videos.com/david_004.mp4",
  ],
};

export class EnhancedLipSyncService {
  /**
   * Generate lip-sync video for given text and voice
   */
  static async generateLipSyncVideo(
    request: LipSyncRequest,
  ): Promise<LipSyncResponse> {
    const startTime = Date.now();
    const videoId = uuidv4();

    console.log(
      `[LIPSYNC] Starting generation for videoId: ${videoId}, text: "${request.text.substring(
        0,
        50,
      )}..."`,
    );

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
        videoUrl: "", // Will be set when completed
        status: "processing",
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
        status: "processing",
        message: "Video generation started",
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
        status: "failed",
        message: "Failed to start video generation",
      };
    }
  }

  /**
   * Complete video generation (simulated)
   */
  private static async completeVideoGeneration(
    videoId: string,
    request: LipSyncRequest,
  ): Promise<void> {
    try {
      const storage = videoStorage.get(videoId);
      if (!storage) {
        console.error(`[LIPSYNC] Video storage not found: ${videoId}`);
        return;
      }

      // Select appropriate mock video URL
      const voiceUrls =
        mockVideoUrls[request.voiceId as keyof typeof mockVideoUrls] ||
        mockVideoUrls["voice_001"];
      const randomUrl = voiceUrls[Math.floor(Math.random() * voiceUrls.length)];

      // Update storage with completed video
      storage.videoUrl = randomUrl;
      storage.status = "completed";
      storage.processingTime = Date.now() - storage.createdAt.getTime();

      videoStorage.set(videoId, storage);

      console.log(
        `[LIPSYNC] Video generation completed: ${videoId} -> ${randomUrl}`,
      );
    } catch (error) {
      console.error(`[LIPSYNC] Error completing video generation: ${error}`);

      const storage = videoStorage.get(videoId);
      if (storage) {
        storage.status = "failed";
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
        status: "failed",
        message: "Video not found",
      };
    }

    // Check if video has expired
    if (storage.expiresAt < new Date()) {
      videoStorage.delete(videoId);
      return {
        success: false,
        videoId,
        status: "failed",
        message: "Video has expired",
      };
    }

    return {
      success: true,
      videoId,
      videoUrl: storage.videoUrl,
      status: storage.status,
      message:
        storage.status === "completed" ? "Video ready" : "Video processing",
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
    const activeVideos = videos.filter((v) => v.expiresAt > now);

    const completed = activeVideos.filter((v) => v.status === "completed");
    const processing = activeVideos.filter((v) => v.status === "processing");
    const failed = activeVideos.filter((v) => v.status === "failed");

    const avgProcessingTime =
      completed.length > 0
        ? completed.reduce((sum, v) => sum + (v.processingTime || 0), 0) /
          completed.length
        : 0;

    return {
      totalVideos: activeVideos.length,
      completedVideos: completed.length,
      processingVideos: processing.length,
      failedVideos: failed.length,
      averageProcessingTime: Math.round(avgProcessingTime),
    };
  }

  /**
   * Generate lip-sync video for a specific image using dynamic script mapping
   * Integrates with ScriptService for dynamic script selection
   */
  static async generateLipSyncVideoForImage(
    imageId: string,
    chunkIndex?: number,
    userId?: string,
    sessionId?: string,
    deviceInfo?: { userAgent: string; ip: string },
  ): Promise<{
    success: boolean;
    videoId?: string;
    videoUrl?: string;
    scriptChunk?: any;
    error?: string;
    analyticsId?: string;
  }> {
    try {
      console.log(
        `[LIPSYNC] Generating video for image ${imageId}, chunk ${chunkIndex}`,
      );

      // Step 1: Get dynamic script chunk for the image
      const scriptResult = await ScriptService.getScriptForImage(
        imageId,
        chunkIndex,
      );

      if (!scriptResult.success || !scriptResult.script) {
        return {
          success: false,
          error: scriptResult.message || "No script available for this image",
        };
      }

      const scriptChunk = scriptResult.script;

      // Step 2: Log analytics for image trigger
      const analyticsId = AnalyticsService.logImageTrigger({
        imageId,
        imageName: `Image-${imageId}`,
        scriptId: scriptChunk.id,
        scriptText: scriptChunk.text,
        voiceId: scriptChunk.voiceId || "voice_001",
        sessionId,
        deviceId: userId,
        userAgent: deviceInfo?.userAgent || "",
        ipAddress: deviceInfo?.ip || "",
      });

      // Step 3: Check if we already have a video for this script
      const existingVideos = await this.getVideosForImage(imageId);
      const matchingVideo = existingVideos.find(
        (v) =>
          v.scriptId === scriptChunk.id &&
          v.status === "completed" &&
          v.expiresAt > new Date(),
      );

      if (matchingVideo) {
        console.log(
          `[LIPSYNC] Found cached video for script ${scriptChunk.id}`,
        );

        // For now, we'll just log that we found a cached video
        // The AnalyticsService doesn't have an update method, so we'll just log a new event
        AnalyticsService.logImageTrigger({
          imageId,
          imageName: `Image-${imageId}`,
          scriptId: scriptChunk.id,
          scriptText: scriptChunk.text,
          voiceId: scriptChunk.voiceId || "voice_001",
          sessionId,
          deviceId: userId,
          userAgent: deviceInfo?.userAgent || "",
          ipAddress: deviceInfo?.ip || "",
          recognitionConfidence: 1.0, // High confidence for cache hit
        });

        return {
          success: true,
          videoId: matchingVideo.videoId,
          videoUrl: matchingVideo.videoUrl,
          scriptChunk,
          analyticsId,
        };
      }

      // Step 4: Generate new lip-sync video
      const lipSyncRequest: LipSyncRequest = {
        text: scriptChunk.text,
        voiceId: scriptChunk.voiceId || "voice_001",
        language: scriptChunk.language || "en",
        imageId: imageId,
        scriptId: scriptChunk.id,
      };

      const result = await this.generateLipSyncVideo(lipSyncRequest);

      if (result.success && result.videoId) {
        return {
          success: true,
          videoId: result.videoId,
          videoUrl: result.status === "completed" ? result.videoUrl : undefined,
          scriptChunk,
          analyticsId,
        };
      } else {
        return {
          success: false,
          error: result.message || "Failed to generate lip-sync video",
          analyticsId,
        };
      }
    } catch (error) {
      console.error(`[LIPSYNC] Error generating video for image:`, error);
      return {
        success: false,
        error:
          error instanceof Error ? error.message : "Unknown error occurred",
      };
    }
  }

  /**
   * Log avatar playback events for analytics
   */
  static async logAvatarPlayback(
    videoId: string,
    event: "start" | "pause" | "resume" | "complete" | "error",
    metadata?: any,
  ): Promise<void> {
    try {
      const storage = videoStorage.get(videoId);
      if (!storage) {
        console.warn(
          `[LIPSYNC] Video not found for playback logging: ${videoId}`,
        );
        return;
      }

      // Use available methods in AnalyticsService
      if (event === "start") {
        AnalyticsService.logAvatarPlayStart({
          imageId: storage.imageId,
          scriptId: storage.scriptId,
          videoId: videoId,
        });
      } else if (event === "complete") {
        // We'll need to track the event ID from start to end it properly
        // For now, we'll just log a new start event as a placeholder
        AnalyticsService.logAvatarPlayStart({
          imageId: storage.imageId,
          scriptId: storage.scriptId,
          videoId: videoId,
        });
      }

      console.log(`[LIPSYNC] Logged ${event} event for video ${videoId}`);
    } catch (error) {
      console.error(`[LIPSYNC] Error logging avatar playback:`, error);
    }
  }

  /**
   * Get enhanced analytics including script usage and playback patterns
   */
  static async getEnhancedAnalytics(imageId?: string): Promise<{
    videoStats: {
      totalVideos: number;
      completedVideos: number;
      processingVideos: number;
      failedVideos: number;
      averageProcessingTime: number;
    };
    scriptUsage?: any[];
    playbackStats?: any;
  }> {
    try {
      const videoStats = await this.getAnalytics();

      // Since AnalyticsService doesn't have these methods, we'll return simplified data
      let scriptUsage: any[] = [];
      let playbackStats: any = null;

      // Return what we can with the available methods
      return {
        videoStats,
        scriptUsage,
        playbackStats,
      };
    } catch (error) {
      console.error(`[LIPSYNC] Error getting enhanced analytics:`, error);
      return {
        videoStats: await this.getAnalytics(),
        scriptUsage: [],
        playbackStats: null,
      };
    }
  }
}

// Cleanup expired videos every hour
setInterval(
  () => {
    EnhancedLipSyncService.cleanupExpiredVideos();
  },
  60 * 60 * 1000,
);
