import express from "express";
import { Image, Dialogue } from "../models/Image";
import { Avatar } from "../models/Avatar";
import { ImageAvatarMapping } from "../models/ImageAvatarMapping";
import { ScriptService } from "../services/scriptService";
import { AnalyticsService } from "../services/analyticsService";

const router = express.Router();

// Get dynamic script chunk for detected image - Enhanced version
router.get("/getScriptForImage/:imageId", async (req, res, next) => {
  try {
    const { imageId } = req.params;
    const { scriptIndex, chunkIndex } = req.query;
    
    // Get user agent and IP for analytics
    const userAgent = req.get("User-Agent");
    const ipAddress = req.ip || req.connection.remoteAddress;

    // Use enhanced ScriptService for dynamic script mapping
    const result = await ScriptService.getScriptForImage(
      imageId,
      chunkIndex ? parseInt(chunkIndex as string, 10) : undefined,
      userAgent,
      ipAddress
    );

    if (!result.success) {
      return res.status(404).json({
        success: false,
        message: result.message || "Failed to retrieve script",
        availableScripts: result.availableScripts,
        totalChunks: result.totalChunks,
      });
    }

    // Get associated avatar for the image
    const avatarMapping = await ImageAvatarMapping.findOne({
      where: { imageId, isActive: true },
      include: [
        {
          model: Avatar,
          as: "avatar",
        },
      ],
    });

    // Log analytics data if available
    if (result.analytics && result.script) {
      // Fetch image name for better analytics labeling
      const imageRecord = await Image.findByPk(imageId);

      AnalyticsService.logImageTrigger({
        imageId: result.analytics.imageId,
        imageName: imageRecord?.name || imageId,
        scriptId: result.analytics.scriptId,
        scriptText: result.script.text,
        voiceId: result.script.voiceId || "unknown",
        userAgent,
        ipAddress,
      });
    }

    const response = {
      success: true,
      image: {
        id: imageId,
        // Additional image details can be fetched if needed
      },
      script: result.script,
      avatar: (avatarMapping as any)?.avatar || null,
      availableScripts: result.availableScripts,
      totalChunks: result.totalChunks,
      message: result.message,
    };

    return res.json(response);
  } catch (error) {
    console.error("Error in getScriptForImage endpoint:", error);
    return next(error);
  }
});

// Get all scripts for an image
router.get("/getAllScriptsForImage/:imageId", async (req, res, next) => {
  try {
    const { imageId } = req.params;

    const image = await Image.findByPk(imageId, {
      include: [
        {
          model: Dialogue,
          as: "dialogues",
          where: { isActive: true },
          required: false,
        },
      ],
    });

    if (!image) {
      return res.status(404).json({ error: "Image not found" });
    }

    const response = {
      image: {
        id: image.id,
        name: image.name,
        description: image.description,
      },
      scripts:
        (image as any).dialogues?.map((dialogue: any) => ({
          id: dialogue.id,
          text: dialogue.text,
          language: dialogue.language,
          voiceId: dialogue.voiceId,
          isDefault: dialogue.isDefault,
        })) || [],
      totalScripts: (image as any).dialogues?.length || 0,
    };

    return res.json(response);
  } catch (error) {
    return next(error);
  }
});

// Get next script in sequence
router.get(
  "/getNextScript/:imageId/:currentScriptId",
  async (req, res, next) => {
    try {
      const { imageId, currentScriptId } = req.params;

      const image = await Image.findByPk(imageId, {
        include: [
          {
            model: Dialogue,
            as: "dialogues",
            where: { isActive: true },
            required: false,
          },
        ],
      });

      if (!image) {
        return res.status(404).json({ error: "Image not found" });
      }

      const dialogues = (image as any).dialogues || [];
      const currentIndex = dialogues.findIndex(
        (d: any) => d.id === currentScriptId
      );

      if (currentIndex === -1) {
        return res.status(404).json({ error: "Current script not found" });
      }

      // Get next script (cycle back to first if at end)
      const nextIndex = (currentIndex + 1) % dialogues.length;
      const nextScript = dialogues[nextIndex];

      const response = {
        image: {
          id: image.id,
          name: image.name,
        },
        script: {
          id: nextScript.id,
          text: nextScript.text,
          language: nextScript.language,
          voiceId: nextScript.voiceId,
          isDefault: nextScript.isDefault,
        },
        currentIndex: nextIndex,
        totalScripts: dialogues.length,
      };

      // Log script progression
      console.log(
        `[ANALYTICS] Script progression for ${image.name}: ${currentIndex} -> ${nextIndex}`
      );

      return res.json(response);
    } catch (error) {
      return next(error);
    }
  }
);

/**
 * POST /api/v1/scripts/image/:imageId/chunks
 * Create multiple script chunks for an image
 */
router.post("/image/:imageId/chunks", async (req, res, next) => {
  try {
    const { imageId } = req.params;
    const { scripts } = req.body;

    if (!scripts || !Array.isArray(scripts) || scripts.length === 0) {
      return res.status(400).json({
        success: false,
        message: "Scripts array is required and cannot be empty",
      });
    }

    // Validate script structure
    const validScripts = scripts.every((script) => 
      script.text && 
      script.language && 
      typeof script.orderIndex === "number"
    );

    if (!validScripts) {
      return res.status(400).json({
        success: false,
        message: "Each script must have text, language, and orderIndex",
      });
    }

    const result = await ScriptService.createScriptChunks(imageId, scripts);

    if (!result.success) {
      return res.status(400).json({
        success: false,
        message: result.message || "Failed to create script chunks",
      });
    }

    res.status(201).json({
      success: true,
      createdScripts: result.createdScripts,
      message: result.message,
    });
  } catch (error) {
    console.error("Error in createScriptChunks endpoint:", error);
    next(error);
  }
});

/**
 * GET /api/v1/scripts/analytics/triggers
 * Get script trigger analytics
 */
router.get("/analytics/triggers", async (req, res, next) => {
  try {
    const analytics = AnalyticsService.getAnalytics();
    
    res.json({
      success: true,
      analytics: analytics.imageTriggers,
    });
  } catch (error) {
    console.error("Error in analytics endpoint:", error);
    next(error);
  }
});

/**
 * GET /api/v1/scripts/stats
 * Get script usage statistics
 */
router.get("/stats", async (req, res, next) => {
  try {
    const analytics = AnalyticsService.getAnalytics();
    
    res.json({
      success: true,
      stats: {
        imageTriggers: analytics.imageTriggers,
        avatarPlays: analytics.avatarPlays,
        performance: analytics.performance,
      },
    });
  } catch (error) {
    console.error("Error in stats endpoint:", error);
    next(error);
  }
});

export default router;
