import express from "express";
import { AIPipelineService } from "../services/aiPipelineService";

const router = express.Router();

// Generate complete AI pipeline: script → audio → lipsync
router.post("/generate", async (req, res, next) => {
  try {
    const { imageId, language, emotion } = req.body;

    // Validate required parameters
    if (!imageId) {
      return res.status(400).json({
        error: "Missing required parameter: imageId"
      });
    }

    // Generate AI pipeline
    const result = await AIPipelineService.generateAIPipeline(
      imageId,
      language || "en",
      emotion || "neutral"
    );

    return res.json({
      success: true,
      jobId: result.jobId,
      message: "AI pipeline started successfully"
    });
  } catch (error) {
    return next(error);
  }
});

// Get job status
router.get("/status/:jobId", async (req, res, next) => {
  try {
    const { jobId } = req.params;

    const job = await AIPipelineService.getJobStatus(jobId);

    if (!job) {
      return res.status(404).json({
        error: "Job not found"
      });
    }

    return res.json({
      success: true,
      job
    });
  } catch (error) {
    return next(error);
  }
});

// Get Sync.so job status
router.get("/lipsync/status/:jobId", async (req, res, next) => {
  try {
    const { jobId } = req.params;

    const jobStatus = await AIPipelineService.getSyncJobStatus(jobId);

    return res.json({
      success: true,
      jobStatus
    });
  } catch (error) {
    return next(error);
  }
});

// Generate script only
router.post("/generate_script", async (req, res, next) => {
  try {
    const { imageId, language, emotion } = req.body;

    // Validate required parameters
    if (!imageId) {
      return res.status(400).json({
        error: "Missing required parameter: imageId"
      });
    }

    const result = await AIPipelineService.generateScript({
      imageId,
      language: language || "en",
      emotion: emotion || "neutral"
    });

    return res.json({
      success: true,
      script: result.text,
      language: result.language,
      emotion: result.emotion
    });
  } catch (error) {
    return next(error);
  }
});

// Generate product script
router.post("/generate_product_script", async (req, res, next) => {
  try {
    const { productName } = req.body;

    // Validate required parameters
    if (!productName) {
      return res.status(400).json({
        error: "Missing required parameter: productName"
      });
    }

    const script = await AIPipelineService.generateProductScript(productName);

    return res.json({
      success: true,
      script: script
    });
  } catch (error) {
    return next(error);
  }
});

// Generate audio only
router.post("/generate_audio", async (req, res, next) => {
  try {
    const { text, language, voiceId, emotion } = req.body;

    // Validate required parameters
    if (!text) {
      return res.status(400).json({
        error: "Missing required parameter: text"
      });
    }

    const result = await AIPipelineService.generateAudio({
      text,
      language: language || "en",
      voiceId,
      emotion
    });

    return res.json({
      success: true,
      audioUrl: result.audioUrl,
      duration: result.duration
    });
  } catch (error) {
    return next(error);
  }
});

// Generate lip-sync only (enhanced version matching Sync.so API requirements)
router.post("/generate_lipsync", async (req, res, next) => {
  try {
    // Extract parameters with both naming conventions for compatibility
    const { imageId, audioUrl, audio_url, emotion, avatar } = req.body;
    
    // Use audio_url if provided (matching Sync.so API), otherwise fallback to audioUrl
    const audioUrlToUse = audio_url || audioUrl;

    // Validate required parameters
    if (!audioUrlToUse) {
      return res.status(400).json({
        error: "Missing required parameter: audio_url"
      });
    }

    const result = await AIPipelineService.generateLipSync({
      imageId: imageId || "default",
      audioUrl: audioUrlToUse,
      emotion,
      avatar // Pass avatar if provided
    });

    const response: any = {
      success: true,
      videoUrl: result.videoUrl,
      duration: result.duration
    };

    // Include job ID if available
    if (result.jobId) {
      response.jobId = result.jobId;
    }

    return res.json(response);
  } catch (error) {
    return next(error);
  }
});

export default router;