import express from "express";
import { EnhancedLipSyncService, LipSyncRequest } from "../services/enhancedLipSyncService";

const router = express.Router();

// Generate lip-sync video
router.post("/generate", async (req, res, next) => {
  try {
    const { text, voiceId, language, imageId, scriptId } = req.body;

    if (!text || !voiceId || !imageId || !scriptId) {
      return res.status(400).json({
        success: false,
        message: "Missing required fields: text, voiceId, imageId, scriptId"
      });
    }

    const request: LipSyncRequest = {
      text,
      voiceId,
      language: language || 'en-US',
      imageId,
      scriptId,
    };

    const result = await EnhancedLipSyncService.generateLipSyncVideo(request);

    // Log the generation request
    console.log(`[ANALYTICS] Lip-sync generation requested for image ${imageId}, script ${scriptId}, voice ${voiceId}`);

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

    if (result.status === 'completed') {
      console.log(`[ANALYTICS] Video ${videoId} accessed - processing time: ${result.processingTime}ms`);
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

    console.log(`[ANALYTICS] Retrieved ${videos.length} videos for image ${imageId}`);

    res.json({
      success: true,
      imageId,
      videos: videos.map(v => ({
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
    
    console.log(`[ANALYTICS] Analytics requested - ${analytics.totalVideos} total videos`);

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
    
    console.log(`[ANALYTICS] Manual cleanup performed - ${cleanedCount} videos removed`);

    res.json({
      success: true,
      message: `Cleaned up ${cleanedCount} expired videos`,
      cleanedCount,
    });
  } catch (error) {
    next(error);
  }
});

export default router;
