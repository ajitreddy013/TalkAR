import express from "express";
import { authenticateAdmin } from "../middleware/auth";

const router = express.Router();

// Apply admin authentication to all routes
router.use(authenticateAdmin);

// Get all images (including inactive)
router.get("/images", async (req, res, next) => {
  try {
    const { page = 1, limit = 10, search } = req.query;
    
    // Mock data for testing
    const mockImages = [
      {
        id: 1,
        name: "Test Image 1",
        description: "Test Description 1",
        isActive: true,
        createdAt: new Date(),
        dialogues: []
      },
      {
        id: 2,
        name: "Test Image 2",
        description: "Test Description 2",
        isActive: false,
        createdAt: new Date(),
        dialogues: []
      }
    ];

    res.json({
      images: mockImages,
      pagination: {
        page: Number(page),
        limit: Number(limit),
        total: mockImages.length,
        pages: Math.ceil(mockImages.length / Number(limit)),
      },
    });
  } catch (error) {
    next(error);
  }
});

// Get image analytics
router.get("/analytics", async (req, res, next) => {
  try {
    // Mock analytics data for testing
    res.json({
      totalImages: 10,
      activeImages: 8,
      totalDialogues: 25,
      languageStats: [
        { language: "en", count: 15 },
        { language: "es", count: 10 }
      ],
    });
  } catch (error) {
    next(error);
  }
});

// Bulk operations
router.post("/images/bulk-deactivate", async (req, res, next) => {
  try {
    const { imageIds } = req.body;

    // Mock bulk operation for testing
    res.json({ message: "Images deactivated successfully" });
  } catch (error) {
    next(error);
  }
});

router.post("/images/bulk-activate", async (req, res, next) => {
  try {
    const { imageIds } = req.body;

    // Mock bulk operation for testing
    res.json({ message: "Images activated successfully" });
  } catch (error) {
    next(error);
  }
});

export default router;
