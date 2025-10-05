import { Router, Request, Response } from "express";
import { MockLipSyncService } from "../services/mockLipSyncService";

const router = Router();

// Generate lip-sync video
router.post("/generate", async (req: Request, res: Response) => {
  try {
    const { imageId, text, voiceId, language } = req.body;

    if (!imageId || !text) {
      return res.status(400).json({
        error: "imageId and text are required",
      });
    }

    const result = await MockLipSyncService.generateLipSyncVideo({
      imageId,
      text,
      voiceId,
      language,
    });

    return res.json(result);
  } catch (error) {
    console.error("Error in lip-sync generation:", error);
    return res.status(500).json({
      success: false,
      status: "error",
      message: "Internal server error",
    });
  }
});

// Get lip-sync video status
router.get("/status/:videoId", async (req: Request, res: Response) => {
  try {
    const { videoId } = req.params;

    const result = await MockLipSyncService.getLipSyncStatus(videoId);
    res.json(result);
  } catch (error) {
    console.error("Error getting lip-sync status:", error);
    res.status(500).json({
      success: false,
      status: "error",
      message: "Internal server error",
    });
  }
});

// Get available voices
router.get("/voices", async (req: Request, res: Response) => {
  try {
    const voices = await MockLipSyncService.getAvailableVoices();
    res.json({ voices });
  } catch (error) {
    console.error("Error getting voices:", error);
    res.status(500).json({ error: "Failed to get available voices" });
  }
});

// Generate talking head video
router.post("/talking-head", async (req: Request, res: Response) => {
  try {
    const { imageId, text, voiceId } = req.body;

    if (!imageId || !text) {
      return res.status(400).json({
        error: "imageId and text are required",
      });
    }

    const result = await MockLipSyncService.generateTalkingHeadVideo(
      imageId,
      text,
      voiceId
    );

    return res.json(result);
  } catch (error) {
    console.error("Error generating talking head video:", error);
    return res.status(500).json({
      success: false,
      status: "error",
      message: "Internal server error",
    });
  }
});

export default router;
