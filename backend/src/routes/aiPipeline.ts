import express from "express";
import { AIPipelineService } from "../services/aiPipelineService";

const router = express.Router();

// Generate complete ad content with parallel execution for streaming
router.post("/generate", async (req, res, next) => {
  try {
    const { image_id } = req.body;
    
    // Validate required parameters
    if (!image_id) {
      return res.status(400).json({
        error: "Missing required parameter: image_id"
      });
    }

    const poster = {
      image_url: `https://example.com/images/${image_id}.jpg`,
      product_name: `Product ${image_id}`
    };

    // Run script + TTS concurrently
    const [scriptRes, audioRes] = await Promise.all([
      AIPipelineService.generateDynamicScript(image_id),
      // We'll need to get the audio URL from the streaming endpoint
      // For now, we'll simulate this with a mock
      Promise.resolve({
        script: `Check out this amazing ${poster.product_name}!`,
        audioUrl: `http://localhost:3000/audio/mock-audio-${Date.now()}.mp3`
      })
    ]);

    // Fire-and-forget lipsync generation
    const lipsyncPromise = AIPipelineService.generateLipSyncAsync({
      imageId: image_id,
      audioUrl: audioRes.audioUrl,
      avatar: poster.image_url
    }).catch((error: any) => {
      console.error("Background lipsync generation failed:", error);
    });

    // Get the job ID for polling
    const lipsyncResult = await lipsyncPromise;
    const jobId = lipsyncResult?.jobId || "mock-job-id";

    res.json({
      script: scriptRes.script,
      audio_url: audioRes.audioUrl,
      estimated_video: "pending",
      job_id: jobId,
      elapsed: 0 // In a real implementation, we would calculate this
    });
  } catch (error: any) {
    console.error("Parallel ad content generation error:", error);
    
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

// Generate complete ad content from poster: poster → script → audio → lipsync video
router.post("/generate_ad_content_from_poster", async (req, res, next) => {
  try {
    const { image_id, user_id } = req.body;

    // Validate required parameters
    if (!image_id) {
      return res.status(400).json({
        error: "Missing required parameter: image_id"
      });
    }

    // Validate image_id
    if (typeof image_id !== 'string' || image_id.trim().length === 0) {
      return res.status(400).json({
        error: "Invalid image_id: must be a non-empty string"
      });
    }

    // Generate complete ad content from poster
    const result = await AIPipelineService.generateAdContentFromPoster(image_id, user_id);

    return res.json({
      success: true,
      script: result.script,
      audio_url: result.audioUrl,
      video_url: result.videoUrl,
      metadata: result.metadata
    });
  } catch (error: any) {
    console.error("Poster-based ad content generation error:", error);
    
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
    } else if (error.message.includes("not found")) {
      return res.status(404).json({
        error: error.message
      });
    } else {
      return res.status(500).json({
        error: "Failed to generate ad content from poster. Please try again later."
      });
    }
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

// Generate audio stream for real-time playback
router.post("/generate_audio_stream", async (req, res, next) => {
  try {
    const { text_prompt } = req.body;

    // Validate required parameters
    if (!text_prompt) {
      return res.status(400).json({
        error: "Missing required parameter: text_prompt"
      });
    }

    // Set headers for streaming
    res.setHeader("Content-Type", "audio/mpeg");
    res.setHeader("Transfer-Encoding", "chunked");

    // Call ElevenLabs streaming API
    await AIPipelineService.streamAudio(text_prompt, res);
  } catch (error: any) {
    console.error("Audio streaming error:", error);
    
    // Handle specific error cases
    if (!res.headersSent) {
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
          error: "Failed to stream audio. Please try again later."
        });
      }
    }
  }
});

// Generate lip-sync asynchronously
router.post("/generate_lipsync_async", async (req, res, next) => {
  try {
    const { image_url, audio_url } = req.body;

    // Validate required parameters
    if (!audio_url) {
      return res.status(400).json({
        error: "Missing required parameter: audio_url"
      });
    }

    // Call Sync.so API to generate lip-sync video asynchronously
    const result = await AIPipelineService.generateLipSyncAsync({
      imageId: "default",
      audioUrl: audio_url,
      avatar: image_url
    });

    return res.json({
      success: true,
      job_id: result.jobId
    });
  } catch (error: any) {
    console.error("Async lip-sync generation error:", error);
    
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
        error: "Failed to generate lip-sync video. Please try again later."
      });
    }
  }
});

// Get lip-sync job status
router.get("/lipsync_status/:id", async (req, res, next) => {
  try {
    const { id } = req.params;

    // Validate required parameters
    if (!id) {
      return res.status(400).json({
        error: "Missing required parameter: id"
      });
    }

    // Get job status from Sync.so API
    const result = await AIPipelineService.getLipSyncJobStatus(id);

    return res.json({
      success: true,
      ...result
    });
  } catch (error: any) {
    console.error("Lip-sync job status error:", error);
    
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
    } else if (error.message.includes("not found")) {
      return res.status(404).json({
        error: "Job not found."
      });
    } else {
      return res.status(500).json({
        error: "Failed to get job status. Please try again later."
      });
    }
  }
});

export default router;