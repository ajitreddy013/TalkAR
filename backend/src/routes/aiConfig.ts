import express from "express";
import { AIConfigService } from "../services/aiConfigService";
import { Avatar } from "../models/Avatar";
import { authenticateAdmin } from "../middleware/auth";

const router = express.Router();

// Apply admin authentication to all routes
router.use(authenticateAdmin);

// Get all AI configurations
router.get("/", async (req, res, next) => {
  try {
    const configs = await AIConfigService.getAllConfigs();
    
    // Get default avatar details if avatar ID is set
    let defaultAvatar = null;
    if (configs.default_avatar_id) {
      defaultAvatar = await Avatar.findByPk(configs.default_avatar_id);
    }

    return res.json({
      success: true,
      configs,
      defaultAvatar
    });
  } catch (error) {
    return next(error);
  }
});

// Get default tone
router.get("/defaults/tone", async (req, res, next) => {
  try {
    const tone = await AIConfigService.getDefaultTone();
    
    return res.json({
      success: true,
      tone
    });
  } catch (error) {
    return next(error);
  }
});

// Set default tone
router.post("/defaults/tone", async (req, res, next) => {
  try {
    const { tone } = req.body;

    if (!tone) {
      return res.status(400).json({
        success: false,
        message: "Missing required field: tone"
      });
    }

    const success = await AIConfigService.setDefaultTone(tone);
    
    if (!success) {
      return res.status(500).json({
        success: false,
        message: "Failed to update default tone"
      });
    }

    // Emit real-time update event
    req.app?.get('io')?.emit('config_updated', { key: 'default_tone', value: tone });

    return res.json({
      success: true,
      message: "Default tone updated successfully",
      tone
    });
  } catch (error) {
    return next(error);
  }
});

// Get prompt template
router.get("/prompt-template", async (req, res, next) => {
  try {
    const template = await AIConfigService.getPromptTemplate();
    
    return res.json({
      success: true,
      template
    });
  } catch (error) {
    return next(error);
  }
});

// Set prompt template
router.post("/prompt-template", async (req, res, next) => {
  try {
    const { value } = req.body;

    if (!value) {
      return res.status(400).json({
        success: false,
        message: "Missing required field: value"
      });
    }

    const success = await AIConfigService.setPromptTemplate(value);
    
    if (!success) {
      return res.status(500).json({
        success: false,
        message: "Failed to update prompt template"
      });
    }

    // Emit real-time update event
    req.app?.get('io')?.emit('config_updated', { key: 'prompt_template', value: value });

    return res.json({
      success: true,
      message: "Prompt template updated successfully",
      template: value
    });
  } catch (error) {
    return next(error);
  }
});

// Get default language
router.get("/defaults/language", async (req, res, next) => {
  try {
    const language = await AIConfigService.getDefaultLanguage();
    
    return res.json({
      success: true,
      language
    });
  } catch (error) {
    return next(error);
  }
});

// Set default language
router.post("/defaults/language", async (req, res, next) => {
  try {
    const { language } = req.body;

    if (!language) {
      return res.status(400).json({
        success: false,
        message: "Missing required field: language"
      });
    }

    const success = await AIConfigService.setDefaultLanguage(language);
    
    if (!success) {
      return res.status(500).json({
        success: false,
        message: "Failed to update default language"
      });
    }

    return res.json({
      success: true,
      message: "Default language updated successfully",
      language
    });
  } catch (error) {
    return next(error);
  }
});

// Get default avatar
router.get("/defaults/avatar", async (req, res, next) => {
  try {
    const avatarId = await AIConfigService.getDefaultAvatarId();
    
    if (!avatarId) {
      return res.json({
        success: true,
        avatar: null
      });
    }

    const avatar = await Avatar.findByPk(avatarId);
    
    return res.json({
      success: true,
      avatar
    });
  } catch (error) {
    return next(error);
  }
});

// Set default avatar
router.post("/defaults/avatar", async (req, res, next) => {
  try {
    const { avatarId } = req.body;

    if (!avatarId) {
      return res.status(400).json({
        success: false,
        message: "Missing required field: avatarId"
      });
    }

    // Verify avatar exists
    const avatar = await Avatar.findByPk(avatarId);
    if (!avatar) {
      return res.status(404).json({
        success: false,
        message: "Avatar not found"
      });
    }

    const success = await AIConfigService.setDefaultAvatarId(avatarId);
    
    if (!success) {
      return res.status(500).json({
        success: false,
        message: "Failed to update default avatar"
      });
    }

    return res.json({
      success: true,
      message: "Default avatar updated successfully",
      avatar
    });
  } catch (error) {
    return next(error);
  }
});

// Get specific AI configuration
router.get("/:key", async (req, res, next) => {
  try {
    const { key } = req.params;
    const value = await AIConfigService.getConfig(key);
    
    if (value === null) {
      return res.status(404).json({
        success: false,
        message: `Configuration key '${key}' not found`
      });
    }

    return res.json({
      success: true,
      key,
      value
    });
  } catch (error) {
    return next(error);
  }
});

// Update AI configuration
router.post("/:key", async (req, res, next) => {
  try {
    const { key } = req.params;
    const { value, description } = req.body;

    if (value === undefined) {
      return res.status(400).json({
        success: false,
        message: "Missing required field: value"
      });
    }

    const success = await AIConfigService.setConfig(key, value, description);
    
    if (!success) {
      return res.status(500).json({
        success: false,
        message: "Failed to update configuration"
      });
    }

    return res.json({
      success: true,
      message: "Configuration updated successfully",
      key,
      value
    });
  } catch (error) {
    return next(error);
  }
});

export default router;