import express from "express";
import { Settings } from "../models/Settings";

const router = express.Router();

// POST /api/v1/settings/reset - Reset to default settings
router.post("/reset", async (req, res, next) => {
  try {
    // Define default settings
    const defaultSettings = {
      ambientAudioEnabled: true,
      ambientAudioVolume: 30,
      ambientAudioType: "soft_music",
      audioFadeDuration: 2000,
      dynamicLightingEnabled: true,
      shadowIntensity: 30,
      avatarScale: 1.0,
      avatarPositionX: 0,
      avatarPositionY: 0,
      avatarPositionZ: 0,
      maxFrameRate: 30,
      qualitySetting: "medium",
      showDebugInfo: false,
      enableGestures: true
    };
    
    // Update or create each default setting
    const updatedSettings: any[] = [];
    
    for (const [key, value] of Object.entries(defaultSettings)) {
      const stringValue = typeof value === 'string' ? value : JSON.stringify(value);
      
      const [setting, created] = await Settings.findOrCreate({
        where: { key },
        defaults: {
          key,
          value: stringValue,
          description: `Default setting for ${key}`
        }
      });
      
      if (!created) {
        setting.value = stringValue;
        await setting.save();
      }
      
      updatedSettings.push({
        key,
        value: setting.value
      });
    }
    
    console.log("[SETTINGS] Reset to default settings");
    
    return res.json({
      success: true,
      message: "Settings reset to default successfully",
      updatedSettings
    });
  } catch (error) {
    console.error("[SETTINGS] Error resetting settings:", error);
    return next(error);
  }
});

// GET /api/v1/settings - Get all settings
router.get("/", async (req, res, next) => {
  try {
    const settings = await Settings.findAll();
    
    // Convert to key-value pairs for easier frontend consumption
    const settingsObject: Record<string, any> = {};
    settings.forEach(setting => {
      try {
        settingsObject[setting.key] = JSON.parse(setting.value);
      } catch (e) {
        // If JSON parsing fails, store as string
        settingsObject[setting.key] = setting.value;
      }
    });

    return res.json({
      success: true,
      settings: settingsObject,
    });
  } catch (error) {
    console.error("[SETTINGS] Error fetching settings:", error);
    return next(error);
  }
});

// POST /api/v1/settings - Update settings
router.post("/", async (req, res, next) => {
  try {
    const settingsData = req.body;
    
    if (!settingsData || typeof settingsData !== 'object') {
      return res.status(400).json({
        success: false,
        message: "Invalid settings data provided"
      });
    }
    
    // Process each setting
    const updatedSettings: any[] = [];
    
    for (const [key, value] of Object.entries(settingsData)) {
      // Convert value to JSON string for storage
      const stringValue = typeof value === 'string' ? value : JSON.stringify(value);
      
      // Upsert the setting (update if exists, create if not)
      const [setting, created] = await Settings.findOrCreate({
        where: { key },
        defaults: {
          key,
          value: stringValue,
          description: `Setting for ${key}`
        }
      });
      
      if (!created) {
        // Update existing setting
        setting.value = stringValue;
        await setting.save();
      }
      
      updatedSettings.push({
        key,
        value: setting.value
      });
    }
    
    console.log(`[SETTINGS] Updated ${updatedSettings.length} settings`);
    
    return res.json({
      success: true,
      message: "Settings updated successfully",
      updatedSettings
    });
  } catch (error) {
    console.error("[SETTINGS] Error updating settings:", error);
    return next(error);
  }
});

// GET /api/v1/settings/:key - Get specific setting
router.get("/:key", async (req, res, next) => {
  try {
    const { key } = req.params;
    
    const setting = await Settings.findOne({
      where: { key }
    });
    
    if (!setting) {
      return res.status(404).json({
        success: false,
        message: "Setting not found"
      });
    }
    
    let parsedValue;
    try {
      parsedValue = JSON.parse(setting.value);
    } catch (e) {
      // If JSON parsing fails, return as string
      parsedValue = setting.value;
    }
    
    return res.json({
      success: true,
      setting: {
        key: setting.key,
        value: parsedValue,
        description: setting.description
      }
    });
  } catch (error) {
    console.error(`[SETTINGS] Error fetching setting ${req.params.key}:`, error);
    return next(error);
  }
});

// POST /api/v1/settings/:key - Update specific setting
router.post("/:key", async (req, res, next) => {
  try {
    const { key } = req.params;
    const { value, description } = req.body;
    
    if (value === undefined) {
      return res.status(400).json({
        success: false,
        message: "Value is required"
      });
    }
    
    // Convert value to JSON string for storage
    const stringValue = typeof value === 'string' ? value : JSON.stringify(value);
    
    // Upsert the setting
    const [setting, created] = await Settings.findOrCreate({
      where: { key },
      defaults: {
        key,
        value: stringValue,
        description: description || `Setting for ${key}`
      }
    });
    
    if (!created) {
      // Update existing setting
      setting.value = stringValue;
      if (description) {
        setting.description = description;
      }
      await setting.save();
    }
    
    console.log(`[SETTINGS] ${created ? 'Created' : 'Updated'} setting: ${key}`);
    
    return res.json({
      success: true,
      message: `Setting ${created ? 'created' : 'updated'} successfully`,
      setting: {
        key: setting.key,
        value: setting.value,
        description: setting.description
      }
    });
  } catch (error) {
    console.error(`[SETTINGS] Error updating setting ${req.params.key}:`, error);
    return next(error);
  }
});

// DELETE /api/v1/settings/:key - Delete specific setting
router.delete("/:key", async (req, res, next) => {
  try {
    const { key } = req.params;
    
    const deletedCount = await Settings.destroy({
      where: { key }
    });
    
    if (deletedCount === 0) {
      return res.status(404).json({
        success: false,
        message: "Setting not found"
      });
    }
    
    console.log(`[SETTINGS] Deleted setting: ${key}`);
    
    return res.json({
      success: true,
      message: "Setting deleted successfully"
    });
  } catch (error) {
    console.error(`[SETTINGS] Error deleting setting ${req.params.key}:`, error);
    return next(error);
  }
});

export default router;