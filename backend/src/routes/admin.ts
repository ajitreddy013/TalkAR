import express from 'express';
import { Image, Dialogue } from '../models/Image';
import { authenticateAdmin } from '../middleware/auth';

const router = express.Router();

// Apply admin authentication to all routes
router.use(authenticateAdmin);

// Get all images (including inactive)
router.get('/images', async (req, res, next) => {
  try {
    const { page = 1, limit = 10, search } = req.query;
    const offset = (Number(page) - 1) * Number(limit);
    
    const whereClause = search ? {
      [Op.or]: [
        { name: { [Op.iLike]: `%${search}%` } },
        { description: { [Op.iLike]: `%${search}%` } }
      ]
    } : {};
    
    const { count, rows: images } = await Image.findAndCountAll({
      where: whereClause,
      include: [{
        model: Dialogue,
        as: 'dialogues'
      }],
      order: [['createdAt', 'DESC']],
      limit: Number(limit),
      offset
    });
    
    res.json({
      images,
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

// Get image analytics
router.get('/analytics', async (req, res, next) => {
  try {
    const totalImages = await Image.count();
    const activeImages = await Image.count({ where: { isActive: true } });
    const totalDialogues = await Dialogue.count();
    
    const languageStats = await Dialogue.findAll({
      attributes: [
        'language',
        [sequelize.fn('COUNT', sequelize.col('id')), 'count']
      ],
      group: ['language'],
      raw: true
    });
    
    res.json({
      totalImages,
      activeImages,
      totalDialogues,
      languageStats
    });
  } catch (error) {
    next(error);
  }
});

// Bulk operations
router.post('/images/bulk-deactivate', async (req, res, next) => {
  try {
    const { imageIds } = req.body;
    
    await Image.update(
      { isActive: false },
      { where: { id: imageIds } }
    );
    
    res.json({ message: 'Images deactivated successfully' });
  } catch (error) {
    next(error);
  }
});

router.post('/images/bulk-activate', async (req, res, next) => {
  try {
    const { imageIds } = req.body;
    
    await Image.update(
      { isActive: true },
      { where: { id: imageIds } }
    );
    
    res.json({ message: 'Images activated successfully' });
  } catch (error) {
    next(error);
  }
});

export default router;

