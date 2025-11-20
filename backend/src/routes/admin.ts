import express, { Request, Response, NextFunction } from "express";
import { Op } from "sequelize";
import { Image, Dialogue } from "../models/Image";
import { authenticateAdmin } from "../middleware/auth";
import { sequelize } from "../config/database";
import Interaction from "../models/Interaction";
import Metric from "../models/Metric";
import { AIConfig } from "../models/AIConfig";

const router = express.Router();

// Apply admin authentication to all routes
router.use(authenticateAdmin);

// Get all images (including inactive)
router.get("/images", async (req: Request, res: Response, next: NextFunction) => {
  try {
    const { page = 1, limit = 10, search } = req.query;
    const offset = (Number(page) - 1) * Number(limit);

    const likeOp = sequelize.getDialect() === "sqlite" ? Op.like : (Op as any).iLike || Op.like;
    const whereClause = search
      ? {
          [Op.or]: [
            { name: { [likeOp]: `%${search}%` } },
            { description: { [likeOp]: `%${search}%` } },
          ],
        }
      : {};

    const { count, rows: images } = await (Image as any).findAndCountAll({
      where: whereClause,
      include: [
        {
          model: Dialogue,
          as: "dialogues",
        },
      ],
      order: [["createdAt", "DESC"]],
      limit: Number(limit),
      offset,
    });

    res.json({
      images,
      pagination: {
        page: Number(page),
        limit: Number(limit),
        total: count,
        pages: Math.ceil(count / Number(limit)),
      },
    });
  } catch (error) {
    next(error);
  }
});

// Get image analytics
router.get("/analytics", async (req: Request, res: Response, next: NextFunction) => {
  try {
    const totalImages = await (Image as any).count();
    const activeImages = await (Image as any).count({ where: { isActive: true } });
    const totalDialogues = await (Dialogue as any).count();

    const languageStats = await (Dialogue as any).findAll({
      attributes: [
        "language",
        [sequelize.fn("COUNT", sequelize.col("id")), "count"],
      ],
      group: ["language"],
      raw: true,
    });

    res.json({
      totalImages,
      activeImages,
      totalDialogues,
      languageStats,
    });
  } catch (error) {
    next(error);
  }
});

// Bulk operations
router.post("/images/bulk-deactivate", async (req: Request, res: Response, next: NextFunction) => {
  try {
    const { imageIds } = req.body;

    await (Image as any).update({ isActive: false }, { where: { id: imageIds } });

    res.json({ message: "Images deactivated successfully" });
  } catch (error) {
    next(error);
  }
});

router.post("/images/bulk-activate", async (req: Request, res: Response, next: NextFunction) => {
  try {
    const { imageIds } = req.body;

    await (Image as any).update({ isActive: true }, { where: { id: imageIds } });

    res.json({ message: "Images activated successfully" });
  } catch (error) {
    next(error);
  }
});


// Get aggregated metrics
router.get("/metrics", async (req: Request, res: Response, next: NextFunction) => {
  try {
    const { start, end } = req.query;
    const whereClause: any = {};
    if (start && end) {
      whereClause.date = {
        [Op.between]: [start as string, end as string]
      };
    }

    const metrics = await (Metric as any).findAll({
      where: whereClause,
      order: [['date', 'ASC']]
    });
    res.json(metrics);
  } catch (error) {
    next(error);
  }
});

// Get interactions
router.get("/interactions", async (req: Request, res: Response, next: NextFunction) => {
  try {
    const { page = 1, limit = 100, status, poster_id, startDate, endDate } = req.query;
    const offset = (Number(page) - 1) * Number(limit);
    
    const whereClause: any = {};
    
    if (status && status !== 'all') {
      whereClause.status = status;
    }
    
    if (poster_id) {
      whereClause.poster_id = { [Op.iLike]: `%${poster_id}%` };
    }
    
    if (startDate && endDate) {
      whereClause.created_at = {
        [Op.between]: [startDate, endDate]
      };
    } else if (startDate) {
      whereClause.created_at = {
        [Op.gte]: startDate
      };
    } else if (endDate) {
      whereClause.created_at = {
        [Op.lte]: endDate
      };
    }

    const { count, rows } = await (Interaction as any).findAndCountAll({
      where: whereClause,
      limit: Number(limit),
      offset,
      order: [['created_at', 'DESC']]
    });
    
    res.json({
      interactions: rows,
      pagination: {
        page: Number(page),
        limit: Number(limit),
        total: count,
        pages: Math.ceil(count / Number(limit))
      }
    });
  } catch (error) {
    next(error);
  }
});

// Export interactions as CSV
router.get("/interactions/export", async (req: Request, res: Response, next: NextFunction) => {
  try {
    const { start, end } = req.query;
    const whereClause: any = {};
    if (start && end) {
      whereClause.created_at = {
        [Op.between]: [start as string, end as string]
      };
    }

    const interactions = await (Interaction as any).findAll({
      where: whereClause,
      order: [['created_at', 'DESC']],
      raw: true
    });

    const fields = ['id', 'user_id', 'poster_id', 'script', 'status', 'latency_ms', 'feedback', 'created_at'];
    const csv = [
      fields.join(','),
      ...interactions.map((row: any) => fields.map((field: string) => JSON.stringify(row[field] || '')).join(','))
    ].join('\n');

    res.header('Content-Type', 'text/csv');
    res.attachment('interactions.csv');
    res.send(csv);
  } catch (error) {
    next(error);
  }
});

// Get current AI config
router.get("/config", async (req: Request, res: Response) => {
  try {
    const configs = await (AIConfig as any).findAll();
    const configMap: any = {};
    configs.forEach((c: any) => {
      configMap[c.key] = c.value;
    });
    res.json({ success: true, config: configMap });
  } catch (error) {
    console.error("Error fetching config:", error);
    res.status(500).json({ error: "Failed to fetch config" });
  }
});

// Update config
router.post("/update-config", async (req: Request, res: Response, next: NextFunction) => {
  try {
    const { voiceId, defaultTone, defaultLanguage } = req.body;
    
    if (voiceId) await (AIConfig as any).upsert({ key: 'voiceId', value: voiceId });
    if (defaultTone) await (AIConfig as any).upsert({ key: 'defaultTone', value: defaultTone });
    if (defaultLanguage) await (AIConfig as any).upsert({ key: 'defaultLanguage', value: defaultLanguage });
    
    const io = req.app.get('io');
    if (io) {
      io.emit('config_updated', { voiceId, defaultTone, defaultLanguage });
    }

    res.json({ message: "Configuration updated successfully" });
  } catch (error) {
    next(error);
  }
});

export default router;
