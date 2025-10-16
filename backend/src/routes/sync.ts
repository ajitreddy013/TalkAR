import express from "express";
import {
  generateSyncVideo,
  getSyncStatus,
  getAvailableVoices,
  getTalkingHeadVideo,
} from "../services/syncService";
import { validateSyncRequest } from "../middleware/validation";

const router = express.Router();

// Generate sync video
router.post("/generate", validateSyncRequest, async (req, res, next) => {
  try {
    const { text, language, voiceId, imageUrl } = req.body;

    const result = await generateSyncVideo({
      text,
      language,
      voiceId,
      imageUrl, // Include the recognized image URL
    });

    return res.json(result);
  } catch (error) {
    return next(error);
  }
});

// Get sync status
router.get("/status/:jobId", async (req, res, next) => {
  try {
    const { jobId } = req.params;

    const status = await getSyncStatus(jobId);

    return res.json(status);
  } catch (error: any) {
    if (error.message === "Job not found") {
      return res.status(404).json({ error: "Job not found" });
    }
    return next(error);
  }
});

// Get available voices
router.get("/voices", async (req, res, next) => {
  try {
    const voices = await getAvailableVoices();
    return res.json(voices);
  } catch (error) {
    return next(error);
  }
});

// Get pre-saved talking head video for recognized image
router.get("/talking-head/:imageId", async (req, res, next) => {
  try {
    const { imageId } = req.params;

    // Get the talking head video for this specific image
    const talkingHeadVideo = await getTalkingHeadVideo(imageId);

    return res.json(talkingHeadVideo);
  } catch (error) {
    return next(error);
  }
});

export default router;
