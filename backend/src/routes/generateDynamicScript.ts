import express from "express";
import OpenAI from "openai";
import { getPosterById } from "../utils/posterHelper";
import { getUserPreferences } from "../utils/userHelper";
import { dynamicScriptLogger } from "../utils/dynamicScriptLogger";
import { optimizedScriptService } from "../services/optimizedScriptService";

const router = express.Router();

// Initialize OpenAI client
const openai = new OpenAI({ 
  apiKey: process.env.OPENAI_API_KEY 
});

/**
 * Generate dynamic script based on poster metadata and user preferences
 */
router.post("/", async (req, res, next) => {
  const startTime = Date.now();
  let logEntry: any = {
    timestamp: new Date().toISOString(),
    image_id: '',
    user_id: '',
    script: '',
    language: '',
    tone: '',
    response_time: 0,
    success: false
  };

  try {
    const { image_id, user_id } = req.body;

    // Validate required parameters
    if (!image_id) {
      return res.status(400).json({
        error: "Missing required parameter: image_id"
      });
    }

    // Update log entry
    logEntry.image_id = image_id;
    logEntry.user_id = user_id || 'anonymous';

    // Get poster metadata
    const poster = getPosterById(image_id);
    if (!poster) {
      logEntry.error = `Poster with image_id '${image_id}' not found`;
      logEntry.response_time = Date.now() - startTime;
      dynamicScriptLogger.logRequest(logEntry);
      
      return res.status(404).json({
        error: `Poster with image_id '${image_id}' not found`
      });
    }

    // Get user preferences (for future personalization)
    const userPrefs = getUserPreferences();

    // Determine language and tone (poster metadata takes precedence over user preferences)
    const language = poster.language || userPrefs.language;
    const tone = poster.tone || userPrefs.preferred_tone;

    // Update log entry
    logEntry.language = language;
    logEntry.tone = tone;

    // Create dynamic prompt based on poster metadata
    const prompt = `
    Write 2 short advertisement lines for a ${poster.category} product named "${poster.product_name}".
    
    Product Details:
    - Brand: ${poster.brand}
    - Category: ${poster.category}
    - Key Features: ${poster.features.join(', ')}
    - Description: ${poster.description}
    
    Requirements:
    - Tone: ${tone}
    - Language: ${language}
    - Make it sound catchy and emotional
    - Keep it under 25 words total
    - Focus on the main selling points
    - Make it suitable for AR/visual presentation
    
    Format the response as two separate lines, each ending with a period.
    `;

    // Generate script using OpenAI
    const response = await openai.chat.completions.create({
      model: "gpt-4o-mini",
      messages: [{ role: "user", content: prompt }],
      max_tokens: 100,
      temperature: 0.8
    });

    const script = response.choices[0].message.content?.trim() || "";

    // Update log entry
    logEntry.script = script;
    logEntry.response_time = Date.now() - startTime;
    logEntry.success = true;
    logEntry.metadata = {
      model_used: "gpt-4o-mini",
      word_count: script.split(' ').length,
      poster_category: poster.category,
      poster_brand: poster.brand
    };

    // Log the generation request for analytics
    dynamicScriptLogger.logRequest(logEntry);
    console.log(`[DYNAMIC_SCRIPT] Generated script for ${image_id}: ${script.substring(0, 50)}...`);

    // Return poster metadata with generated script
    res.json({
      success: true,
      image_id: poster.image_id,
      product_name: poster.product_name,
      category: poster.category,
      tone: tone,
      language: language,
      image_url: poster.image_url,
      brand: poster.brand,
      script: script,
      metadata: {
        generated_at: new Date().toISOString(),
        user_id: user_id || "anonymous",
        model_used: "gpt-4o-mini",
        word_count: script.split(' ').length
      }
    });

  } catch (error: any) {
    console.error("Dynamic script generation error:", error);
    
    // Update log entry for error
    logEntry.response_time = Date.now() - startTime;
    logEntry.success = false;
    logEntry.error = error.message;
    dynamicScriptLogger.logRequest(logEntry);
    
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
        error: "Failed to generate dynamic script. Please try again later."
      });
    }
  }
});

/**
 * Get all available posters metadata
 */
router.get("/posters", async (req, res, next) => {
  try {
    const { getAllPosters } = await import("../utils/posterHelper");
    const posters = getAllPosters();
    
    res.json({
      success: true,
      posters: posters.map(poster => ({
        image_id: poster.image_id,
        product_name: poster.product_name,
        category: poster.category,
        tone: poster.tone,
        language: poster.language,
        image_url: poster.image_url,
        brand: poster.brand
      }))
    });
  } catch (error: any) {
    console.error("Error fetching posters:", error);
    res.status(500).json({
      error: "Failed to fetch posters metadata"
    });
  }
});

/**
 * Get specific poster metadata
 */
router.get("/poster/:image_id", async (req, res, next) => {
  try {
    const { image_id } = req.params;
    const poster = getPosterById(image_id);
    
    if (!poster) {
      return res.status(404).json({
        error: `Poster with image_id '${image_id}' not found`
      });
    }
    
    res.json({
      success: true,
      poster: {
        image_id: poster.image_id,
        product_name: poster.product_name,
        category: poster.category,
        tone: poster.tone,
        language: poster.language,
        image_url: poster.image_url,
        brand: poster.brand,
        price: poster.price,
        currency: poster.currency,
        features: poster.features,
        description: poster.description
      }
    });
  } catch (error: any) {
    console.error("Error fetching poster:", error);
    res.status(500).json({
      error: "Failed to fetch poster metadata"
    });
  }
});

/**
 * Get analytics data for dynamic script generation
 */
router.get("/analytics", async (req, res, next) => {
  try {
    const analytics = dynamicScriptLogger.getAnalytics();
    
    if (!analytics) {
      return res.status(404).json({
        error: "No analytics data available"
      });
    }
    
    res.json({
      success: true,
      analytics: analytics
    });
  } catch (error: any) {
    console.error("Error fetching analytics:", error);
    res.status(500).json({
      error: "Failed to fetch analytics data"
    });
  }
});

/**
 * Get analytics report
 */
router.get("/analytics/report", async (req, res, next) => {
  try {
    const report = dynamicScriptLogger.generateReport();
    
    res.json({
      success: true,
      report: report
    });
  } catch (error: any) {
    console.error("Error generating analytics report:", error);
    res.status(500).json({
      error: "Failed to generate analytics report"
    });
  }
});

/**
 * Get recent requests
 */
router.get("/analytics/recent", async (req, res, next) => {
  try {
    const limit = parseInt(req.query.limit as string) || 100;
    const recentRequests = dynamicScriptLogger.getRecentRequests(limit);
    
    res.json({
      success: true,
      requests: recentRequests,
      count: recentRequests.length
    });
  } catch (error: any) {
    console.error("Error fetching recent requests:", error);
    res.status(500).json({
      error: "Failed to fetch recent requests"
    });
  }
});

/**
 * Generate optimized script with caching
 */
router.post("/optimized", async (req, res, next) => {
  const startTime = Date.now();
  let logEntry: any = {
    timestamp: new Date().toISOString(),
    image_id: '',
    user_id: '',
    script: '',
    language: '',
    tone: '',
    response_time: 0,
    success: false
  };

  try {
    const { image_id, user_id, use_cache = true } = req.body;

    // Validate required parameters
    if (!image_id) {
      return res.status(400).json({
        error: "Missing required parameter: image_id"
      });
    }

    // Update log entry
    logEntry.image_id = image_id;
    logEntry.user_id = user_id || 'anonymous';

    // Generate optimized script
    const result = await optimizedScriptService.generateOptimizedScript({
      image_id,
      user_id,
      use_cache
    });

    // Update log entry
    logEntry.script = result.script;
    logEntry.language = result.language;
    logEntry.tone = result.tone;
    logEntry.response_time = Date.now() - startTime;
    logEntry.success = true;
    logEntry.metadata = {
      cached: result.cached,
      generation_time: result.generation_time,
      optimized: true
    };

    // Log the generation request for analytics
    dynamicScriptLogger.logRequest(logEntry);
    console.log(`[OPTIMIZED_SCRIPT] Generated script for ${image_id}: ${result.script.substring(0, 50)}... (${result.cached ? 'cached' : 'generated'} in ${result.generation_time}ms)`);

    // Return optimized result
    res.json({
      success: true,
      image_id: image_id,
      product_name: result.product_name,
      language: result.language,
      tone: result.tone,
      image_url: result.image_url,
      script: result.script,
      metadata: {
        generated_at: new Date().toISOString(),
        user_id: user_id || "anonymous",
        cached: result.cached,
        generation_time: result.generation_time,
        optimized: true
      }
    });

  } catch (error: any) {
    console.error("Optimized script generation error:", error);
    
    // Update log entry for error
    logEntry.response_time = Date.now() - startTime;
    logEntry.success = false;
    logEntry.error = error.message;
    dynamicScriptLogger.logRequest(logEntry);
    
    // Handle specific error cases
    if (error.message.includes("not found")) {
      return res.status(404).json({
        error: error.message
      });
    } else {
      return res.status(500).json({
        error: "Failed to generate optimized script. Please try again later."
      });
    }
  }
});

/**
 * Get performance metrics
 */
router.get("/performance", async (req, res, next) => {
  try {
    const metrics = optimizedScriptService.getPerformanceMetrics();
    
    res.json({
      success: true,
      metrics: metrics
    });
  } catch (error: any) {
    console.error("Error fetching performance metrics:", error);
    res.status(500).json({
      error: "Failed to fetch performance metrics"
    });
  }
});

/**
 * Clear cache
 */
router.post("/cache/clear", async (req, res, next) => {
  try {
    optimizedScriptService.clearCache();
    
    res.json({
      success: true,
      message: "Cache cleared successfully"
    });
  } catch (error: any) {
    console.error("Error clearing cache:", error);
    res.status(500).json({
      error: "Failed to clear cache"
    });
  }
});

/**
 * Preload popular posters
 */
router.post("/cache/preload", async (req, res, next) => {
  try {
    const { poster_ids } = req.body;
    
    if (!poster_ids || !Array.isArray(poster_ids)) {
      return res.status(400).json({
        error: "Missing or invalid poster_ids array"
      });
    }

    await optimizedScriptService.preloadPopularPosters(poster_ids);
    
    res.json({
      success: true,
      message: `Preloaded ${poster_ids.length} posters successfully`
    });
  } catch (error: any) {
    console.error("Error preloading posters:", error);
    res.status(500).json({
      error: "Failed to preload posters"
    });
  }
});

/**
 * Batch generate scripts
 */
router.post("/batch", async (req, res, next) => {
  try {
    const { requests } = req.body;
    
    if (!requests || !Array.isArray(requests)) {
      return res.status(400).json({
        error: "Missing or invalid requests array"
      });
    }

    const results = await optimizedScriptService.batchGenerateScripts(requests);
    
    res.json({
      success: true,
      results: results,
      count: results.length
    });
  } catch (error: any) {
    console.error("Error batch generating scripts:", error);
    res.status(500).json({
      error: "Failed to batch generate scripts"
    });
  }
});

export default router;
