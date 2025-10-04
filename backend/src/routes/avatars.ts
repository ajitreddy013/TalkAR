import { Router, Request, Response } from "express";
import { Avatar } from "../models/Avatar";
import { Image, Dialogue } from "../models/Image";
import { ImageAvatarMapping } from "../models/ImageAvatarMapping";

const router = Router();

// Get all avatars
router.get("/", async (req: Request, res: Response) => {
  try {
    const avatars = await Avatar.findAll({
      where: { isActive: true },
      order: [["createdAt", "DESC"]],
    });
    res.json(avatars);
  } catch (error) {
    console.error("Error fetching avatars:", error);
    res.status(500).json({ error: "Failed to fetch avatars" });
  }
});

// Get avatar by ID
router.get("/:id", async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const avatar = await Avatar.findByPk(id);

    if (!avatar) {
      return res.status(404).json({ error: "Avatar not found" });
    }

    return res.json(avatar);
  } catch (error) {
    console.error("Error fetching avatar:", error);
    return res.status(500).json({ error: "Failed to fetch avatar" });
  }
});

// Get avatar for specific image
router.get("/image/:imageId", async (req: Request, res: Response) => {
  try {
    const { imageId } = req.params;

    // Find the mapping between image and avatar
    const mapping = await ImageAvatarMapping.findOne({
      where: {
        imageId,
        isActive: true,
      },
      include: [
        {
          model: Avatar,
          as: "avatar",
          where: { isActive: true },
        },
      ],
    });

    if (!mapping) {
      return res.status(404).json({ error: "No avatar found for this image" });
    }

    // Get the avatar
    const avatar = await Avatar.findByPk(mapping.avatarId);

    return res.json(avatar);
  } catch (error) {
    console.error("Error fetching avatar for image:", error);
    return res.status(500).json({ error: "Failed to fetch avatar for image" });
  }
});

// Create new avatar
router.post("/", async (req: Request, res: Response) => {
  try {
    const { name, description, avatarImageUrl, avatarVideoUrl, voiceId } =
      req.body;

    const avatar = await Avatar.create({
      name,
      description,
      avatarImageUrl,
      avatarVideoUrl,
      voiceId,
      isActive: true,
    });

    return res.status(201).json(avatar);
  } catch (error) {
    console.error("Error creating avatar:", error);
    return res.status(500).json({ error: "Failed to create avatar" });
  }
});

// Map avatar to image
router.post("/:avatarId/map/:imageId", async (req: Request, res: Response) => {
  try {
    const { avatarId, imageId } = req.params;

    // Check if avatar and image exist
    const avatar = await Avatar.findByPk(avatarId);
    const image = await Image.findByPk(imageId);

    if (!avatar) {
      return res.status(404).json({ error: "Avatar not found" });
    }

    if (!image) {
      return res.status(404).json({ error: "Image not found" });
    }

    // Create or update mapping
    const [mapping, created] = await ImageAvatarMapping.findOrCreate({
      where: { imageId, avatarId },
      defaults: { imageId, avatarId, isActive: true },
    });

    if (!created) {
      mapping.isActive = true;
      await mapping.save();
    }

    return res.json({
      message: "Avatar mapped to image successfully",
      mapping,
    });
  } catch (error) {
    console.error("Error mapping avatar to image:", error);
    return res.status(500).json({ error: "Failed to map avatar to image" });
  }
});

// Get complete image data with avatar and scripts
router.get("/complete/:imageId", async (req: Request, res: Response) => {
  try {
    const { imageId } = req.params;

    // Get image with dialogues
    const image = await Image.findByPk(imageId, {
      include: [
        {
          model: Dialogue,
          as: "dialogues",
          where: { isActive: true },
        },
      ],
    });

    if (!image) {
      return res.status(404).json({ error: "Image not found" });
    }

    // Get avatar mapping
    const mapping = await ImageAvatarMapping.findOne({
      where: {
        imageId,
        isActive: true,
      },
      include: [
        {
          model: Avatar,
          as: "avatar",
          where: { isActive: true },
        },
      ],
    });

    const response = {
      image: {
        id: image.id,
        name: image.name,
        description: image.description,
        imageUrl: image.imageUrl,
        thumbnailUrl: image.thumbnailUrl,
        dialogues: image.getDialogues ? await image.getDialogues() : [],
      },
      avatar: mapping ? await Avatar.findByPk(mapping.avatarId) : null,
      mapping: mapping
        ? {
            id: mapping.id,
            isActive: mapping.isActive,
          }
        : null,
    };

    return res.json(response);
  } catch (error) {
    console.error("Error fetching complete image data:", error);
    return res
      .status(500)
      .json({ error: "Failed to fetch complete image data" });
  }
});

export default router;
