import express from "express";
import {
  EnhancedLipSyncService,
  LipSyncRequest,
} from "../services/enhancedLipSyncService";

const router = express.Router();

// Generate lip-sync video
router.post("/generate", async (req, res, next) => {
  try {
    const { text, voiceId, language, imageId, scriptId } = req.body;

    if (!text || !voiceId || !imageId || !scriptId) {
      return res.status(400).json({
        success: false,
        message: "Missing required fields: text, voiceId, imageId, scriptId",
      });
    }

    const request: LipSyncRequest = {
      text,
      voiceId,
      language: language || "en-US",
      imageId,
      scriptId,
    };

    const result = await EnhancedLipSyncService.generateLipSyncVideo(request);

    // Log the generation request
    console.log(
      `[ANALYTICS] Lip-sync generation requested for image ${imageId}, script ${scriptId}, voice ${voiceId}`,
    );

    return res.json(result);
  } catch (error) {
    return next(error);
  }
});

// Get video status
router.get("/status/:videoId", async (req, res, next) => {
  try {
    const { videoId } = req.params;
    const result = await EnhancedLipSyncService.getVideoStatus(videoId);

    if (result.status === "completed") {
      console.log(
        `[ANALYTICS] Video ${videoId} accessed - processing time: ${result.processingTime}ms`,
      );
    }

    return res.json(result);
  } catch (error) {
    return next(error);
  }
});

// Get all videos for an image
router.get("/videos/:imageId", async (req, res, next) => {
  try {
    const { imageId } = req.params;
    const videos = await EnhancedLipSyncService.getVideosForImage(imageId);

    console.log(
      `[ANALYTICS] Retrieved ${videos.length} videos for image ${imageId}`,
    );

    res.json({
      success: true,
      imageId,
      videos: videos.map((v) => ({
        videoId: v.videoId,
        scriptId: v.scriptId,
        text: v.text,
        voiceId: v.voiceId,
        videoUrl: v.videoUrl,
        status: v.status,
        createdAt: v.createdAt,
        processingTime: v.processingTime,
      })),
      totalVideos: videos.length,
    });
  } catch (error) {
    next(error);
  }
});

// Get analytics
router.get("/analytics", async (req, res, next) => {
  try {
    const analytics = await EnhancedLipSyncService.getAnalytics();

    console.log(
      `[ANALYTICS] Analytics requested - ${analytics.totalVideos} total videos`,
    );

    res.json({
      success: true,
      analytics,
      timestamp: new Date().toISOString(),
    });
  } catch (error) {
    next(error);
  }
});

// Cleanup expired videos (admin endpoint)
router.post("/cleanup", async (req, res, next) => {
  try {
    const cleanedCount = await EnhancedLipSyncService.cleanupExpiredVideos();

    console.log(
      `[ANALYTICS] Manual cleanup performed - ${cleanedCount} videos removed`,
    );

    res.json({
      success: true,
      message: `Cleaned up ${cleanedCount} expired videos`,
      cleanedCount,
    });
  } catch (error) {
    next(error);
  }
});

/**
 * POST /api/v1/enhanced-lipsync/generate-for-image
 * Generate lip-sync video for an image with dynamic script mapping
 */
router.post("/generate-for-image", async (req, res, next) => {
  try {
    const { imageId, chunkIndex, userId, sessionId } = req.body;
    const userAgent = req.headers["user-agent"] || "";
    const ip = req.ip || req.connection.remoteAddress || "";

    if (!imageId) {
      return res.status(400).json({
        success: false,
        error: "imageId is required",
      });
    }

    console.log(
      `[ENHANCED-LIPSYNC] Generating video for image: ${imageId}, chunk: ${chunkIndex}`,
    );

    const result = await EnhancedLipSyncService.generateLipSyncVideoForImage(
      imageId,
      chunkIndex,
      userId,
      sessionId,
      { userAgent, ip },
    );

    if (result.success) {
      return res.json({
        success: true,
        videoId: result.videoId,
        videoUrl: result.videoUrl,
        scriptChunk: result.scriptChunk,
        analyticsId: result.analyticsId,
        message: result.videoUrl
          ? "Video ready"
          : "Video generation in progress",
      });
    } else {
      return res.status(400).json({
        success: false,
        error: result.error,
        analyticsId: result.analyticsId,
      });
    }
  } catch (error) {
    console.error("Error in enhanced lip-sync generation:", error);
    return next(error);
  }
});

/**
 * POST /api/v1/enhanced-lipsync/playback/:videoId
 * Log avatar playback events for analytics
 */
router.post("/playback/:videoId", async (req, res, next) => {
  try {
    const { videoId } = req.params;
    const { event, metadata } = req.body;

    if (!["start", "pause", "resume", "complete", "error"].includes(event)) {
      return res.status(400).json({
        success: false,
        error:
          "Invalid event type. Must be: start, pause, resume, complete, or error",
      });
    }

    await EnhancedLipSyncService.logAvatarPlayback(videoId, event, metadata);

    return res.json({
      success: true,
      message: `Logged ${event} event for video ${videoId}`,
    });
  } catch (error) {
    console.error("Error logging playback event:", error);
    return next(error);
  }
});

/**
 * GET /api/v1/enhanced-lipsync/enhanced-analytics
 * Get enhanced analytics data including script usage and playback patterns
 */
router.get("/enhanced-analytics", async (req, res, next) => {
  try {
    const { imageId } = req.query;

    const analytics = await EnhancedLipSyncService.getEnhancedAnalytics(
      imageId as string,
    );

    res.json({
      success: true,
      analytics,
    });
  } catch (error) {
    console.error("Error getting enhanced analytics:", error);
    next(error);
  }
});

/**
 * POST /api/v1/enhanced-lipsync/image/:imageId/pre-generate
 * Pre-generate lip-sync videos for all scripts of an image
 * Note: This is a placeholder endpoint for future batch pre-generation feature
 */
router.post("/image/:imageId/pre-generate", async (req, res, next) => {
  try {
    const { imageId } = req.params;

    console.log(
      `[ENHANCED-LIPSYNC] Pre-generation requested for image: ${imageId}`,
    );

    // Get existing videos for the image
    const existingVideos =
      await EnhancedLipSyncService.getVideosForImage(imageId);

    // TODO: Implement batch pre-generation when needed
    // This would involve:
    // 1. Fetching all scripts/dialogues for the image
    // 2. Generating videos for each script in the background
    // 3. Returning job IDs for tracking progress

    res.json({
      success: true,
      message: `Pre-generation endpoint - ${existingVideos.length} videos already exist for image ${imageId}`,
      existingVideos: existingVideos.length,
      note: "Batch pre-generation feature is planned for future implementation",
    });
  } catch (error) {
    console.error("Error in pre-generation endpoint:", error);
    next(error);
  }
});

export default router;
