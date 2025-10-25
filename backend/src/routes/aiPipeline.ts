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

// Generate script with metadata support
router.post("/generate_script", async (req, res, next) => {
  try {
    const { imageId, language, emotion, userPreferences } = req.body;

    // Validate required parameters
    if (!imageId) {
      return res.status(400).json({
        error: "Missing required parameter: imageId"
      });
    }

    const result = await AIPipelineService.generateScript({
      imageId,
      language: language || "en",
      emotion: emotion || "neutral",
      userPreferences // Pass user preferences if provided
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

// Generate complete ad content: product → script → audio → lipsync video
router.post("/generate_ad_content", async (req, res, next) => {
  try {
    const { product } = req.body;

    // Validate required parameters
    if (!product) {
      return res.status(400).json({
        error: "Missing required parameter: product"
      });
    }

    // Validate product name
    if (typeof product !== 'string' || product.trim().length === 0) {
      return res.status(400).json({
        error: "Invalid product name: must be a non-empty string"
      });
    }

    // Limit product name length
    if (product.length > 100) {
      return res.status(400).json({
        error: "Invalid product name: must be less than 100 characters"
      });
    }

    // Generate complete ad content
    const result = await AIPipelineService.generateAdContent(product);

    return res.json({
      success: true,
      script: result.script,
      audio_url: result.audioUrl,
      video_url: result.videoUrl
    });
  } catch (error: any) {
    console.error("Ad content generation error:", error);
    
    // Handle specific error cases
    if (error.message.includes("API key")) {
      return res.status(401).json({
        error: "API authentication failed. Please check your API keys."
      });
    } else if (error.message.includes("rate limit")) {
      return res.status(429).json({
        error: "API rate limit exceeded. Please try again later."
      });
    } else if (error.message.includes("timeout")) {
      return res.status(408).json({
        error: "API request timeout. Please try again later."
      });
    } else {
      return res.status(500).json({
        error: "Failed to generate ad content. Please try again later."
      });
    }
  }
});

// Generate complete ad content with streaming optimization
router.post("/generate_ad_content_streaming", async (req, res, next) => {
  try {
    const { product } = req.body;

    // Validate required parameters
    if (!product) {
      return res.status(400).json({
        error: "Missing required parameter: product"
      });
    }

    // Validate product name
    if (typeof product !== 'string' || product.trim().length === 0) {
      return res.status(400).json({
        error: "Invalid product name: must be a non-empty string"
      });
    }

    // Limit product name length
    if (product.length > 100) {
      return res.status(400).json({
        error: "Invalid product name: must be less than 100 characters"
      });
    }

    // Generate complete ad content with streaming optimization
    const result = await AIPipelineService.generateAdContentStreaming(product);

    return res.json({
      success: true,
      script: result.script,
      audio_url: result.audioUrl,
      video_url: result.videoUrl
    });
  } catch (error: any) {
    console.error("Ad content generation error:", error);
    
    // Handle specific error cases
    if (error.message.includes("API key")) {
      return res.status(401).json({
        error: "API authentication failed. Please check your API keys."
      });
    } else if (error.message.includes("rate limit")) {
      return res.status(429).json({
        error: "API rate limit exceeded. Please try again later."
      });
    } else if (error.message.includes("timeout")) {
      return res.status(408).json({
        error: "API request timeout. Please try again later."
      });
    } else {
      return res.status(500).json({
        error: "Failed to generate ad content. Please try again later."
      });
    }
  }
});

// Handle conversational context queries
router.post("/conversational_query", async (req, res, next) => {
  try {
    const { query, imageId, context } = req.body;

    // Validate required parameters
    if (!query) {
      return res.status(400).json({
        error: "Missing required parameter: query"
      });
    }

    // Process conversational query
    const result = await AIPipelineService.processConversationalQuery({
      query,
      imageId,
      context
    });

    return res.json({
      success: true,
      response: result.response,
      audioUrl: result.audioUrl,
      emotion: result.emotion
    });
  } catch (error: any) {
    console.error("Conversational query error:", error);
    
    // Handle specific error cases
    if (error.message.includes("API key")) {
      return res.status(401).json({
        error: "API authentication failed. Please check your API keys."
      });
    } else if (error.message.includes("rate limit")) {
      return res.status(429).json({
        error: "API rate limit exceeded. Please try again later."
      });
    } else if (error.message.includes("timeout")) {
      return res.status(408).json({
        error: "API request timeout. Please try again later."
      });
    } else {
      return res.status(500).json({
        error: "Failed to process conversational query. Please try again later."
      });
    }
  }
});

export default router;