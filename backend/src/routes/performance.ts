import express from "express";
import { PerformanceMetricsService } from "../services/performanceMetricsService";

const router = express.Router();

// Get performance metrics summary
router.get("/", async (req, res, next) => {
  try {
    const summary = PerformanceMetricsService.getPerformanceSummary();
    
    return res.json({
      success: true,
      summary,
      targets: {
        audioStartDelay: "≤ 1.5s",
        videoRenderDelay: "≤ 3s",
        totalPipelineTime: "≤ 5s"
      },
      timestamp: new Date().toISOString(),
    });
  } catch (error) {
    return next(error);
  }
});

// Get detailed metrics for a specific request
router.get("/:requestId", async (req, res, next) => {
  try {
    const { requestId } = req.params;
    const metrics = PerformanceMetricsService.getMetrics(requestId);
    
    if (!metrics) {
      return res.status(404).json({
        success: false,
        message: "Metrics not found for requestId: " + requestId
      });
    }

    return res.json({
      success: true,
      metrics,
      timestamp: new Date().toISOString(),
    });
  } catch (error) {
    return next(error);
  }
});

// Get all metrics (paginated)
router.get("/all", async (req, res, next) => {
  try {
    const allMetrics = PerformanceMetricsService.getAllMetrics();
    const page = parseInt(req.query.page as string) || 1;
    const limit = parseInt(req.query.limit as string) || 50;
    const startIndex = (page - 1) * limit;
    const endIndex = startIndex + limit;
    
    const paginatedMetrics = allMetrics.slice(startIndex, endIndex);
    const totalPages = Math.ceil(allMetrics.length / limit);

    return res.json({
      success: true,
      metrics: paginatedMetrics,
      pagination: {
        currentPage: page,
        totalPages,
        totalMetrics: allMetrics.length,
        limit
      },
      timestamp: new Date().toISOString(),
    });
  } catch (error) {
    return next(error);
  }
});

// Cleanup old metrics (admin endpoint)
router.post("/cleanup", async (req, res, next) => {
  try {
    const cleanedCount = PerformanceMetricsService.cleanupOldMetrics();
    
    console.log(`[PERFORMANCE] Manual cleanup performed - ${cleanedCount} metrics removed`);

    res.json({
      success: true,
      message: `Cleaned up ${cleanedCount} old metrics`,
      cleanedCount,
    });
  } catch (error) {
    next(error);
  }
});

export default router;