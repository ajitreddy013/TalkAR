import express from "express";
import {
  generateSyncVideo,
  getSyncStatus,
  getUserSyncJobs,
  getTalkingHeadVideo,
} from "../services/syncServiceSupabase";
import { validateSyncRequest } from "../middleware/validation";
import { authenticateUser } from "../middleware/auth";

const router = express.Router();

// Generate sync video
router.post("/generate", authenticateUser, validateSyncRequest, async (req, res, next) => {
  try {
    const { text, language, voiceId, imageUrl } = req.body;
    const userId = req.user?.userId;

    if (!userId) {
      return res.status(401).json({ error: "User not authenticated" });
    }

    const result = await generateSyncVideo({
      text,
      language,
      voiceId,
      imageUrl, // Include the recognized image URL
      userId, // Include user ID for Supabase integration
    });

    return res.json(result);
  } catch (error) {
    return next(error);
  }
});

// Get sync status
router.get("/status/:jobId", authenticateUser, async (req, res, next) => {
  try {
    const { jobId } = req.params;
    const userId = req.user?.userId;

    if (!userId) {
      return res.status(401).json({ error: "User not authenticated" });
    }

    // Verify the job belongs to the user
    const userJobs = await getUserSyncJobs(userId);
    const job = userJobs.find(j => j.jobId === jobId);
    
    if (!job) {
      return res.status(404).json({ error: "Job not found" });
    }

    const status = await getSyncStatus(jobId);

    return res.json(status);
  } catch (error) {
    return next(error);
  }
});

// Get user's sync jobs
router.get("/jobs", authenticateUser, async (req, res, next) => {
  try {
    const userId = req.user?.userId;

    if (!userId) {
      return res.status(401).json({ error: "User not authenticated" });
    }

    const jobs = await getUserSyncJobs(userId);

    return res.json(jobs);
  } catch (error) {
    return next(error);
  }
});

// Get available voices (mock implementation)
router.get("/voices", async (req, res, next) => {
  try {
    const voices = [
      { id: "en-female-1", name: "English Female 1", language: "en" },
      { id: "en-male-1", name: "English Male 1", language: "en" },
      { id: "es-female-1", name: "Spanish Female 1", language: "es" },
      { id: "fr-female-1", name: "French Female 1", language: "fr" },
      { id: "de-male-1", name: "German Male 1", language: "de" },
    ];
    return res.json(voices);
  } catch (error) {
    return next(error);
  }
});

// Get pre-saved talking head video for recognized image
router.get("/talking-head/:imageId", authenticateUser, async (req, res, next) => {
  try {
    const { imageId } = req.params;
    const userId = req.user?.userId;

    if (!userId) {
      return res.status(401).json({ error: "User not authenticated" });
    }

    // Get the talking head video for this specific image
    const talkingHeadVideo = await getTalkingHeadVideo(imageId, userId);

    return res.json(talkingHeadVideo);
  } catch (error) {
    return next(error);
  }
});

export default router;