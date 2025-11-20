import express from "express";
import { getPosterById } from "../utils/posterHelper";
import { getUserPreferences } from "../utils/userHelper";
import { dynamicScriptLogger } from "../utils/dynamicScriptLogger";
import { optimizedScriptService } from "../services/optimizedScriptService";
import { getRecentInteractions } from "../utils/memoryHelper";

const router = express.Router();

/**
 * Generate dynamic script based on poster metadata and user preferences
 */
router.post("/", async (req, res, next) => {
  try {
    const { image_id, user_id, script_options } = req.body;
    
    if (!image_id) {
      return res.status(400).json({ error: "image_id is required" });
    }

    // Use optimizedScriptService instead of missing dynamicScriptService
    const result = await optimizedScriptService.generateOptimizedScript({
      image_id,
      user_id,
      use_cache: script_options?.use_cache
    });

    return res.json(result);
  } catch (error: any) {
    console.error("Script generation error:", error);
    return res.status(500).json({ error: "Failed to generate script" });
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
    
    if (!image_id) {
      return res.status(400).json({ error: "image_id is required" });
    }

    const poster = getPosterById(image_id);
    if (!poster) {
      return res.status(404).json({ error: "Poster not found" });
    }

    return res.json(poster);
  } catch (error) {
    console.error("Poster fetch error:", error);
    return res.status(500).json({ error: "Failed to fetch poster" });
  }
});

/**
 * Get analytics data for dynamic script generation
 */
router.get("/analytics", async (req, res, next) => {
  try {
    // Return mock analytics since we don't have the actual service
    return res.json({
      total_requests: 0,
      successful_requests: 0,
      failed_requests: 0,
      average_response_time: 0,
      requests_by_poster: {},
      requests_by_language: {},
      requests_by_tone: {},
      requests_by_hour: {},
      last_updated: new Date().toISOString()
    });
  } catch (error: any) {
    console.error("Analytics fetch error:", error);
    return res.status(500).json({ error: "Failed to fetch analytics" });
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
  try {
    const { image_id, user_id, use_cache } = req.body;
    
    if (!image_id) {
      return res.status(400).json({ error: "image_id is required" });
    }

    const result = await optimizedScriptService.generateOptimizedScript({
      image_id,
      user_id,
      use_cache
    });

    return res.json(result);
  } catch (error) {
    console.error("Optimized script generation error:", error);
    return res.status(500).json({ error: "Failed to generate optimized script" });
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
    const { image_ids } = req.body;
    
    if (!image_ids || !Array.isArray(image_ids)) {
      return res.status(400).json({ error: "image_ids array is required" });
    }

    const results = await Promise.all(
      image_ids.map(async (image_id) => {
        try {
          const result = await optimizedScriptService.generateOptimizedScript({
            image_id,
            use_cache: true
          });
          return { image_id, success: true, result };
        } catch (error: any) {
          return { image_id, success: false, error: error.message };
        }
      })
    );

    return res.json({ 
      message: "Cache preloading completed", 
      results 
    });
  } catch (error: any) {
    console.error("Cache preloading error:", error);
    return res.status(500).json({ error: "Failed to preload cache" });
  }
});

/**
 * Batch generate scripts
 */
router.post("/batch", async (req, res, next) => {
  try {
    const { requests } = req.body;
    
    if (!requests || !Array.isArray(requests)) {
      return res.status(400).json({ error: "requests array is required" });
    }

    const results = await Promise.all(
      requests.map(async (request) => {
        try {
          const result = await optimizedScriptService.generateOptimizedScript(request);
          return { ...request, success: true, result };
        } catch (error: any) {
          return { ...request, success: false, error: error.message };
        }
      })
    );

    return res.json({ 
      message: "Batch processing completed", 
      results 
    });
  } catch (error: any) {
    console.error("Batch processing error:", error);
    return res.status(500).json({ error: "Failed to process batch" });
  }
});

export default router;
