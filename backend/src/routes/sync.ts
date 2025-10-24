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
    const { text, language, voiceId, emotion, imageUrl } = req.body;

    const result = await generateSyncVideo({
      text,
      language,
      voiceId,
      emotion, // Include emotion parameter
      imageUrl, // Include the recognized image URL
    });

    return res.json(result);
  } catch (error: any) {
    // If sync service fails due to missing API key or API error, return accepted status with fallback
    if (error.message === 'Failed to generate sync video') {
      return res.status(202).json({ 
        message: 'Request accepted. Sync service is unavailable; using fallback.',
        jobId: 'mock-job-id',
        status: 'pending'
      });
    }
    return next(error);
  }
});

// Get sync status
router.get("/status/:jobId", async (req, res, next) => {
  try {
    const { jobId } = req.params;

    const status = await getSyncStatus(jobId);

    return res.json(status);
  } catch (error) {
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
    const { language, emotion } = req.query;

    // Get the talking head video for this specific image
    const talkingHeadVideo = await getTalkingHeadVideo(imageId, language as string, emotion as string);

    return res.json(talkingHeadVideo);
  } catch (error) {
    return next(error);
  }
});

export default router;
