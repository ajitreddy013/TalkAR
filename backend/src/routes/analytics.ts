import express from "express";
import { AnalyticsService } from "../services/analyticsService";

const router = express.Router();

// Get comprehensive analytics
router.get("/", async (req, res, next) => {
  try {
    const analytics = AnalyticsService.getAnalytics();
    
    console.log(`[ANALYTICS] Full analytics requested - ${analytics.imageTriggers.total} triggers, ${analytics.avatarPlays.total} plays`);

    return res.json({
      success: true,
      analytics,
      timestamp: new Date().toISOString(),
    });
  } catch (error) {
    return next(error);
  }
});

// Get image trigger analytics
router.get("/image-triggers", async (req, res, next) => {
  try {
    const analytics = AnalyticsService.getAnalytics();
    
    console.log(`[ANALYTICS] Image trigger analytics requested`);

    res.json({
      success: true,
      imageTriggers: analytics.imageTriggers,
      timestamp: new Date().toISOString(),
    });
  } catch (error) {
    next(error);
  }
});

// Get avatar play analytics
router.get("/avatar-plays", async (req, res, next) => {
  try {
    const analytics = AnalyticsService.getAnalytics();
    
    console.log(`[ANALYTICS] Avatar play analytics requested`);

    res.json({
      success: true,
      avatarPlays: analytics.avatarPlays,
      timestamp: new Date().toISOString(),
    });
  } catch (error) {
    next(error);
  }
});

// Get performance metrics
router.get("/performance", async (req, res, next) => {
  try {
    const analytics = AnalyticsService.getAnalytics();
    
    console.log(`[ANALYTICS] Performance metrics requested`);

    res.json({
      success: true,
      performance: analytics.performance,
      timestamp: new Date().toISOString(),
    });
  } catch (error) {
    next(error);
  }
});

// Log image trigger event
router.post("/log-image-trigger", async (req, res, next) => {
  try {
    const { imageId, imageName, scriptId, scriptText, voiceId, sessionId, deviceId } = req.body;

    if (!imageId || !imageName || !scriptId || !scriptText || !voiceId) {
      return res.status(400).json({
        success: false,
        message: "Missing required fields: imageId, imageName, scriptId, scriptText, voiceId"
      });
    }

    const eventId = AnalyticsService.logImageTrigger({
      imageId,
      imageName,
      scriptId,
      scriptText,
      voiceId,
      sessionId,
      deviceId,
    });

    return res.json({
      success: true,
      eventId,
      message: "Image trigger logged successfully",
    });
  } catch (error) {
    return next(error);
  }
});

// Log avatar play start
router.post("/log-avatar-play-start", async (req, res, next) => {
  try {
    const { imageId, scriptId, videoId, sessionId, deviceId } = req.body;

    if (!imageId || !scriptId || !videoId) {
      return res.status(400).json({
        success: false,
        message: "Missing required fields: imageId, scriptId, videoId"
      });
    }

    const eventId = AnalyticsService.logAvatarPlayStart({
      imageId,
      scriptId,
      videoId,
      sessionId,
      deviceId,
    });

    return res.json({
      success: true,
      eventId,
      message: "Avatar play start logged successfully",
    });
  } catch (error) {
    return next(error);
  }
});

// Log avatar play end
router.post("/log-avatar-play-end", async (req, res, next) => {
  try {
    const { eventId, status } = req.body;

    if (!eventId) {
      return res.status(400).json({
        success: false,
        message: "Missing required field: eventId"
      });
    }

    AnalyticsService.logAvatarPlayEnd(eventId, status || 'completed');

    return res.json({
      success: true,
      message: "Avatar play end logged successfully",
    });
  } catch (error) {
    return next(error);
  }
});

// Log performance metrics
router.post("/log-performance", async (req, res, next) => {
  try {
    const { responseTime, success, videoProcessingTime } = req.body;

    if (responseTime !== undefined) {
      AnalyticsService.logResponseTime(responseTime, success !== false);
    }

    if (videoProcessingTime !== undefined) {
      AnalyticsService.logVideoProcessingTime(videoProcessingTime);
    }

    res.json({
      success: true,
      message: "Performance metrics logged successfully",
    });
  } catch (error) {
    next(error);
  }
});

// Get recent AI pipeline events
router.get("/ai-pipeline-events", async (req, res, next) => {
  try {
    const analytics = AnalyticsService.getAnalytics();
    
    console.log(`[ANALYTICS] AI pipeline events requested`);

    res.json({
      success: true,
      aiPipelineEvents: analytics.aiPipelineEvents,
      timestamp: new Date().toISOString(),
    });
  } catch (error) {
    next(error);
  }
});

// Cleanup old events (admin endpoint)
router.post("/cleanup", async (req, res, next) => {
  try {
    const cleanedCount = AnalyticsService.cleanupOldEvents();
    
    console.log(`[ANALYTICS] Manual cleanup performed - ${cleanedCount} events removed`);

    res.json({
      success: true,
      message: `Cleaned up ${cleanedCount} old events`,
      cleanedCount,
    });
  } catch (error) {
    next(error);
  }
});

export default router;
