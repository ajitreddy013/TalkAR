import express from "express";
import { Image, Dialogue } from "../models/Image";
import { Avatar } from "../models/Avatar";
import { ImageAvatarMapping } from "../models/ImageAvatarMapping";

const router = express.Router();

// Get script for detected image
router.get("/getScriptForImage/:imageId", async (req, res, next) => {
  try {
    const { imageId } = req.params;
    const { scriptIndex } = req.query;

    // Find the image with its dialogues
    const image = await Image.findByPk(imageId, {
      include: [
        {
          model: Dialogue,
          as: "dialogues",
          where: { isActive: true },
          required: false,
        },
      ],
    });

    if (!image) {
      return res.status(404).json({ error: "Image not found" });
    }

    const dialogues = (image as any).dialogues || [];
    
    if (dialogues.length === 0) {
      return res.status(404).json({ error: "No scripts found for this image" });
    }

    // Select script based on index or random selection
    let selectedScript;
    if (scriptIndex && !isNaN(Number(scriptIndex))) {
      const index = Number(scriptIndex);
      selectedScript = dialogues[index] || dialogues[0];
    } else {
      // Random selection for variety
      const randomIndex = Math.floor(Math.random() * dialogues.length);
      selectedScript = dialogues[randomIndex];
    }

    // Get associated avatar
    const avatarMapping = await ImageAvatarMapping.findOne({
      where: { imageId, isActive: true },
      include: [
        {
          model: Avatar,
          as: "avatar",
        },
      ],
    });

    const response = {
      image: {
        id: image.id,
        name: image.name,
        description: image.description,
      },
      script: {
        id: selectedScript.id,
        text: selectedScript.text,
        language: selectedScript.language,
        voiceId: selectedScript.voiceId,
        isDefault: selectedScript.isDefault,
      },
      avatar: (avatarMapping as any)?.avatar || null,
      availableScripts: dialogues.length,
      currentScriptIndex: dialogues.findIndex((d: any) => d.id === selectedScript.id),
    };

    // Log the script selection for analytics
    console.log(`[ANALYTICS] Script selected for image ${image.name}: "${selectedScript.text.substring(0, 50)}..."`);

    return res.json(response);
  } catch (error) {
    return next(error);
  }
});

// Get all scripts for an image
router.get("/getAllScriptsForImage/:imageId", async (req, res, next) => {
  try {
    const { imageId } = req.params;

    const image = await Image.findByPk(imageId, {
      include: [
        {
          model: Dialogue,
          as: "dialogues",
          where: { isActive: true },
          required: false,
        },
      ],
    });

    if (!image) {
      return res.status(404).json({ error: "Image not found" });
    }

    const response = {
      image: {
        id: image.id,
        name: image.name,
        description: image.description,
      },
      scripts: (image as any).dialogues?.map((dialogue: any) => ({
        id: dialogue.id,
        text: dialogue.text,
        language: dialogue.language,
        voiceId: dialogue.voiceId,
        isDefault: dialogue.isDefault,
      })) || [],
      totalScripts: (image as any).dialogues?.length || 0,
    };

    return res.json(response);
  } catch (error) {
    return next(error);
  }
});

// Get next script in sequence
router.get("/getNextScript/:imageId/:currentScriptId", async (req, res, next) => {
  try {
    const { imageId, currentScriptId } = req.params;

    const image = await Image.findByPk(imageId, {
      include: [
        {
          model: Dialogue,
          as: "dialogues",
          where: { isActive: true },
          required: false,
        },
      ],
    });

    if (!image) {
      return res.status(404).json({ error: "Image not found" });
    }

    const dialogues = (image as any).dialogues || [];
    const currentIndex = dialogues.findIndex((d: any) => d.id === currentScriptId);
    
    if (currentIndex === -1) {
      return res.status(404).json({ error: "Current script not found" });
    }

    // Get next script (cycle back to first if at end)
    const nextIndex = (currentIndex + 1) % dialogues.length;
    const nextScript = dialogues[nextIndex];

    const response = {
      image: {
        id: image.id,
        name: image.name,
      },
      script: {
        id: nextScript.id,
        text: nextScript.text,
        language: nextScript.language,
        voiceId: nextScript.voiceId,
        isDefault: nextScript.isDefault,
      },
      currentIndex: nextIndex,
      totalScripts: dialogues.length,
    };

    // Log script progression
    console.log(`[ANALYTICS] Script progression for ${image.name}: ${currentIndex} -> ${nextIndex}`);

    return res.json(response);
  } catch (error) {
    return next(error);
  }
});

export default router;
