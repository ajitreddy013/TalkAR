import express from "express";
import Feedback from "../models/Feedback";
import { updateFeedback } from "../utils/memoryHelper";

const router = express.Router();

// POST /api/v1/feedback - Receive feedback data from mobile app
router.post("/", async (req, res, next) => {
  try {
    const { adContentId, productName, isPositive, timestamp } = req.body;

    // Validate required fields
    if (!adContentId || !productName || isPositive === undefined || !timestamp) {
      return res.status(400).json({
        success: false,
        message: "Missing required fields: adContentId, productName, isPositive, timestamp"
      });
    }

    // Create feedback record
    const feedback = await Feedback.create({
      id: `${adContentId}-${Date.now()}`, // Simple ID generation
      adContentId,
      productName,
      isPositive,
      timestamp,
    });

    console.log(`[FEEDBACK] New feedback received - Product: ${productName}, Positive: ${isPositive}`);

    return res.status(201).json({
      success: true,
      message: "Feedback recorded successfully",
      feedbackId: feedback.id,
    });
  } catch (error) {
    console.error("[FEEDBACK] Error recording feedback:", error);
    return next(error);
  }
});

// GET /api/v1/feedback - Get all feedback (for admin dashboard)
router.get("/", async (req, res, next) => {
  try {
    const feedbacks = await Feedback.findAll({
      order: [["createdAt", "DESC"]],
      limit: 100, // Limit to last 100 feedback entries
    });

    return res.json({
      success: true,
      feedbacks,
      count: feedbacks.length,
    });
  } catch (error) {
    console.error("[FEEDBACK] Error fetching feedback:", error);
    return next(error);
  }
});

// GET /api/v1/feedback/stats - Get feedback statistics
router.get("/stats", async (req, res, next) => {
  try {
    // Get total feedback count
    const totalFeedback = await Feedback.count();
    
    // Get positive feedback count
    const positiveFeedback = await Feedback.count({
      where: { isPositive: true }
    });
    
    // Get negative feedback count
    const negativeFeedback = await Feedback.count({
      where: { isPositive: false }
    });
    
    // Get feedback by product
    const feedbackByProduct = await Feedback.findAll({
      attributes: [
        "productName",
        [Feedback.sequelize!.fn("COUNT", Feedback.sequelize!.col("id")), "count"],
        [Feedback.sequelize!.fn("SUM", Feedback.sequelize!.col("isPositive")), "positiveCount"]
      ],
      group: ["productName"],
      order: [[Feedback.sequelize!.fn("COUNT", Feedback.sequelize!.col("id")), "DESC"]],
    });
    
    // Format the product stats
    const productStats = feedbackByProduct.map((item: any) => ({
      productName: item.productName,
      totalCount: parseInt(item.getDataValue("count")),
      positiveCount: parseInt(item.getDataValue("positiveCount")),
      negativeCount: parseInt(item.getDataValue("count")) - parseInt(item.getDataValue("positiveCount")),
      positivePercentage: Math.round((parseInt(item.getDataValue("positiveCount")) / parseInt(item.getDataValue("count"))) * 100)
    }));

    return res.json({
      success: true,
      stats: {
        total: totalFeedback,
        positive: positiveFeedback,
        negative: negativeFeedback,
        positivePercentage: totalFeedback > 0 ? Math.round((positiveFeedback / totalFeedback) * 100) : 0,
        negativePercentage: totalFeedback > 0 ? Math.round((negativeFeedback / totalFeedback) * 100) : 0,
        byProduct: productStats
      }
    });
  } catch (error) {
    console.error("[FEEDBACK] Error fetching feedback stats:", error);
    return next(error);
  }
});

// GET /api/v1/feedback/recent - Get recent feedback
router.get("/recent", async (req, res, next) => {
  try {
    const limit = parseInt(req.query.limit as string) || 10;
    
    const feedbacks = await Feedback.findAll({
      order: [["createdAt", "DESC"]],
      limit: Math.min(limit, 50), // Cap at 50
    });

    return res.json({
      success: true,
      feedbacks,
    });
  } catch (error) {
    console.error("[FEEDBACK] Error fetching recent feedback:", error);
    return next(error);
  }
});

// POST /api/v1/feedback/user-context - Update feedback in user context
router.post("/user-context", async (req, res, next) => {
  try {
    const { poster_id, feedback } = req.body;

    // Validate required fields
    if (!poster_id || !feedback) {
      return res.status(400).json({
        success: false,
        message: "Missing required fields: poster_id, feedback"
      });
    }

    // Validate feedback value
    if (feedback !== "like" && feedback !== "dislike") {
      return res.status(400).json({
        success: false,
        message: "Invalid feedback value. Must be 'like' or 'dislike'"
      });
    }

    // Update feedback in user context
    updateFeedback(poster_id, feedback);

    console.log(`[FEEDBACK] Updated user context feedback - Poster: ${poster_id}, Feedback: ${feedback}`);

    return res.status(200).json({
      success: true,
      message: "Feedback updated in user context successfully"
    });
  } catch (error) {
    console.error("[FEEDBACK] Error updating user context feedback:", error);
    return next(error);
  }
});

export default router;