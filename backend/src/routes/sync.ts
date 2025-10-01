import express from "express";
import {
  generateSyncVideo,
  getSyncStatus,
  getAvailableVoices,
} from "../services/syncService";
import { validateSyncRequest } from "../middleware/validation";

const router = express.Router();

// Generate sync video
router.post("/generate", validateSyncRequest, async (req, res, next) => {
  try {
    const { text, language, voiceId } = req.body;

    const result = await generateSyncVideo({
      text,
      language,
      voiceId,
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

export default router;
