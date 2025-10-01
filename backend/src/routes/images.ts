import express from "express";
import { Image, Dialogue } from "../models/Image";
import { uploadImage } from "../services/uploadService";
import { validateImageUpload } from "../middleware/validation";

const router = express.Router();

// Get all images
router.get("/", async (req, res, next) => {
  try {
    const images = await Image.findAll({
      where: { isActive: true },
      include: [
        {
          model: Dialogue,
          as: "dialogues",
        },
      ],
      order: [["createdAt", "DESC"]],
    });

    res.json(images);
  } catch (error) {
    next(error);
  }
});

// Get image by ID
router.get("/:id", async (req, res, next) => {
  try {
    const { id } = req.params;
    const image = await Image.findByPk(id, {
      include: [
        {
          model: Dialogue,
          as: "dialogues",
        },
      ],
    });

    if (!image) {
      return res.status(404).json({ error: "Image not found" });
    }

    return res.json(image);
  } catch (error) {
    return next(error);
  }
});

// Create new image
router.post(
  "/",
  uploadImage.single("image"),
  validateImageUpload,
  async (req, res, next) => {
    try {
      const { name, description } = req.body;
      const imageUrl = req.file?.path; // Use path instead of location for local storage

      if (!imageUrl) {
        return res.status(400).json({ error: "Image upload failed" });
      }

      const image = await Image.create({
        name,
        description,
        imageUrl,
        thumbnailUrl: imageUrl, // In production, generate thumbnail
        isActive: true,
      });

      return res.status(201).json(image);
    } catch (error) {
      return next(error);
    }
  }
);

// Update image
router.put("/:id", async (req, res, next) => {
  try {
    const { id } = req.params;
    const { name, description, isActive } = req.body;

    const image = await Image.findByPk(id);
    if (!image) {
      return res.status(404).json({ error: "Image not found" });
    }

    await image.update({
      name: name || image.name,
      description: description !== undefined ? description : image.description,
      isActive: isActive !== undefined ? isActive : image.isActive,
    });

    return res.json(image);
  } catch (error) {
    return next(error);
  }
});

// Delete image
router.delete("/:id", async (req, res, next) => {
  try {
    const { id } = req.params;

    const image = await Image.findByPk(id);
    if (!image) {
      return res.status(404).json({ error: "Image not found" });
    }

    await image.destroy();
    return res.status(204).send();
  } catch (error) {
    return next(error);
  }
});

// Add dialogue to image
router.post("/:id/dialogues", async (req, res, next) => {
  try {
    const { id } = req.params;
    const { text, language, voiceId, isDefault } = req.body;

    const image = await Image.findByPk(id);
    if (!image) {
      return res.status(404).json({ error: "Image not found" });
    }

    const dialogue = await Dialogue.create({
      imageId: id,
      text,
      language,
      voiceId,
      isDefault: isDefault || false,
    });

    return res.status(201).json(dialogue);
  } catch (error) {
    return next(error);
  }
});

// Update dialogue
router.put("/:imageId/dialogues/:dialogueId", async (req, res, next) => {
  try {
    const { imageId, dialogueId } = req.params;
    const { text, language, voiceId, isDefault } = req.body;

    const dialogue = await Dialogue.findOne({
      where: { id: dialogueId, imageId },
    });

    if (!dialogue) {
      return res.status(404).json({ error: "Dialogue not found" });
    }

    await dialogue.update({
      text: text || dialogue.text,
      language: language || dialogue.language,
      voiceId: voiceId !== undefined ? voiceId : dialogue.voiceId,
      isDefault: isDefault !== undefined ? isDefault : dialogue.isDefault,
    });

    return res.json(dialogue);
  } catch (error) {
    return next(error);
  }
});

// Delete dialogue
router.delete("/:imageId/dialogues/:dialogueId", async (req, res, next) => {
  try {
    const { imageId, dialogueId } = req.params;

    const dialogue = await Dialogue.findOne({
      where: { id: dialogueId, imageId },
    });

    if (!dialogue) {
      return res.status(404).json({ error: "Dialogue not found" });
    }

    await dialogue.destroy();
    return res.status(204).send();
  } catch (error) {
    return next(error);
  }
});

export default router;
