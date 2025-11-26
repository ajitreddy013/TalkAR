import express from "express";
import BetaFeedback from "../models/BetaFeedback";

const router = express.Router();

// POST /api/v1/beta-feedback - Receive beta feedback data from mobile app
router.post("/", async (req, res, next) => {
  try {
    const { user_id, poster_id, rating, comment, timestamp } = req.body;

    // Validate required fields
    if (!poster_id || !rating || !timestamp) {
      return res.status(400).json({
        success: false,
        message: "Missing required fields: poster_id, rating, timestamp"
      });
    }

    // Create feedback record
    const feedback = await BetaFeedback.create({
      userId: user_id || "anonymous",
      posterId: poster_id,
      rating,
      comment: comment || "",
      timestamp,
    });

    console.log(`[BETA_FEEDBACK] New feedback received - Poster: ${poster_id}, Rating: ${rating}`);

    return res.status(201).json({
      success: true,
      message: "Beta feedback recorded successfully",
      feedbackId: feedback.id,
    });
  } catch (error) {
    console.error("[BETA_FEEDBACK] Error recording feedback:", error);
    return next(error);
  }
});

export default router;
