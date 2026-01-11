import express from "express";
import { Image, Dialogue } from "../models/Image";
import { ImageAvatarMapping } from "../models/ImageAvatarMapping";
import { uploadImage, uploadToS3 } from "../services/uploadService";
import { validateImageUpload, validateDialogue } from "../middleware/validation";
import path from "path";

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

// Create new image with optional thumbnail and script
router.post(
  "/",
  uploadImage.fields([
    { name: "image", maxCount: 1 },
    { name: "thumbnail", maxCount: 1 },
  ]),
  validateImageUpload,
  async (req, res, next) => {
    try {
      const { name, description, script } = req.body;
      const files = req.files as { [fieldname: string]: Express.Multer.File[] };
      
      let imageUrl: string;
      let thumbnailUrl: string | undefined;

      if (!files || !files["image"] || files["image"].length === 0) {
        return res.status(400).json({ error: "Main image file is required" });
      }

      const mainImageFile = files["image"][0];
      const thumbnailFile = files["thumbnail"] ? files["thumbnail"][0] : null;

      // Handle main image upload
      if (process.env.NODE_ENV === "production" && process.env.AWS_S3_BUCKET) {
        imageUrl = await uploadToS3(mainImageFile);
      } else {
        imageUrl = `/uploads/${path.basename(mainImageFile.path)}`;
      }

      // Handle thumbnail upload
      if (thumbnailFile) {
        if (process.env.NODE_ENV === "production" && process.env.AWS_S3_BUCKET) {
          thumbnailUrl = await uploadToS3(thumbnailFile);
        } else {
          thumbnailUrl = `/uploads/${path.basename(thumbnailFile.path)}`;
        }
      } else {
        thumbnailUrl = imageUrl; // Fallback to main image
      }

      const image = await Image.create({
        name,
        description,
        imageUrl,
        thumbnailUrl,
        isActive: true,
      });

      // If a script was provided, create an initial dialogue
      if (script && script.trim() !== "") {
        await Dialogue.create({
          imageId: image.id,
          text: script,
          language: "en", // Default to English
          isActive: true,
          isDefault: true,
        });
      }

      const createdImage = await Image.findByPk(image.id, {
        include: [{ model: Dialogue, as: "dialogues" }]
      });

      return res.status(201).json(createdImage);
    } catch (error) {
      return next(error);
    }
  }
);

// Update image with optional file updates
router.put(
  "/:id", 
  uploadImage.fields([
    { name: "image", maxCount: 1 },
    { name: "thumbnail", maxCount: 1 },
  ]),
  async (req, res, next) => {
    try {
      const { id } = req.params;
      const { name, description, isActive, script } = req.body;
      const files = req.files as { [fieldname: string]: Express.Multer.File[] } | undefined;

      const image = await Image.findByPk(id, {
        include: [{ model: Dialogue, as: "dialogues" }]
      });
      
      if (!image) {
        return res.status(404).json({ error: "Image not found" });
      }

      let imageUrl = image.imageUrl;
      let thumbnailUrl = image.thumbnailUrl;

      // Handle file updates
      if (files) {
        if (files["image"] && files["image"].length > 0) {
          const mainFile = files["image"][0];
          if (process.env.NODE_ENV === "production" && process.env.AWS_S3_BUCKET) {
            imageUrl = await uploadToS3(mainFile);
          } else {
            imageUrl = `/uploads/${path.basename(mainFile.path)}`;
          }
        }

        if (files["thumbnail"] && files["thumbnail"].length > 0) {
          const thumbFile = files["thumbnail"][0];
          if (process.env.NODE_ENV === "production" && process.env.AWS_S3_BUCKET) {
            thumbnailUrl = await uploadToS3(thumbFile);
          } else {
            thumbnailUrl = `/uploads/${path.basename(thumbFile.path)}`;
          }
        }
      }

      await image.update({
        name: name || image.name,
        description: description !== undefined ? description : image.description,
        isActive: isActive !== undefined ? isActive : image.isActive,
        imageUrl,
        thumbnailUrl,
      });

      // Update default script if provided
      if (script !== undefined) {
        const defaultDialogue = image.dialogues?.find((d: Dialogue) => d.isDefault);
        if (defaultDialogue) {
          await defaultDialogue.update({ text: script });
        } else if (script.trim() !== "") {
          await Dialogue.create({
            imageId: id,
            text: script,
            language: "en",
            isActive: true,
            isDefault: true,
          });
        }
      }

      const updatedImage = await Image.findByPk(id, {
        include: [{ model: Dialogue, as: "dialogues" }]
      });

      return res.json(updatedImage);
    } catch (error) {
      return next(error);
    }
  }
);

// Delete image
router.delete("/:id", async (req, res, next) => {
  try {
    const { id } = req.params;

    const image = await Image.findByPk(id);
    if (!image) {
      return res.status(404).json({ error: "Image not found" });
    }

    // Manually delete related records to satisfy foreign key constraints in SQLite
    await Dialogue.destroy({ where: { imageId: id } });
    await ImageAvatarMapping.destroy({ where: { imageId: id } });
    
    await image.destroy();
    return res.status(204).send();
  } catch (error) {
    return next(error);
  }
});

// Add dialogue to image
router.post("/:id/dialogues", validateDialogue, async (req, res, next) => {
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
      isActive: true,
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
