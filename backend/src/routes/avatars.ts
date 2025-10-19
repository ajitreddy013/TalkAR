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

// Get all image-avatar mappings (MUST be before /:id route)
router.get("/mappings", async (req: Request, res: Response) => {
  try {
    const mappings = await ImageAvatarMapping.findAll({
      where: { isActive: true },
      include: [
        {
          model: Avatar,
          as: "avatar",
          where: { isActive: true },
          required: false,
        },
        {
          model: Image,
          as: "image",
          where: { isActive: true },
          required: false,
        },
      ],
      order: [["createdAt", "DESC"]],
    });

    return res.json(mappings);
  } catch (error) {
    console.error("Error fetching mappings:", error);
    return res.status(500).json({ error: "Failed to fetch mappings" });
  }
});

// Get complete image data with avatar and scripts (MUST be before /:id route)
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
          required: false,
        },
      ],
    });

    if (!image) {
      return res.status(404).json({ error: "Image not found" });
    }

    // Get avatar mapping with full details
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
          required: false,
        },
      ],
    });

    const avatar = mapping ? await Avatar.findByPk(mapping.avatarId) : null;

    const response = {
      image: {
        id: image.id,
        name: image.name,
        description: image.description,
        imageUrl: image.imageUrl,
        thumbnailUrl: image.thumbnailUrl,
        dialogues: image.getDialogues ? await image.getDialogues() : [],
      },
      avatar: avatar
        ? {
            id: avatar.id,
            name: avatar.name,
            description: avatar.description,
            avatarImageUrl: avatar.avatarImageUrl,
            avatarVideoUrl: avatar.avatarVideoUrl,
            avatar3DModelUrl: avatar.avatar3DModelUrl,
            voiceId: avatar.voiceId,
            idleAnimationType: avatar.idleAnimationType,
          }
        : null,
      mapping: mapping
        ? {
            id: mapping.id,
            script: mapping.script,
            audioUrl: mapping.audioUrl,
            videoUrl: mapping.videoUrl,
            visemeDataUrl: mapping.visemeDataUrl,
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

// Get avatar for specific image (MUST be before /:id route)
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

// Get avatar by ID (MUST be after specific routes)
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

// Create new avatar
router.post("/", async (req: Request, res: Response) => {
  try {
    const {
      name,
      description,
      avatarImageUrl,
      avatarVideoUrl,
      avatar3DModelUrl,
      voiceId,
      idleAnimationType,
    } = req.body;

    const avatar = await Avatar.create({
      name,
      description,
      avatarImageUrl,
      avatarVideoUrl,
      avatar3DModelUrl,
      voiceId,
      idleAnimationType: idleAnimationType || "breathing",
      isActive: true,
    });

    return res.status(201).json(avatar);
  } catch (error) {
    console.error("Error creating avatar:", error);
    return res.status(500).json({ error: "Failed to create avatar" });
  }
});

// Map avatar to image with script
router.post("/:avatarId/map/:imageId", async (req: Request, res: Response) => {
  try {
    const { avatarId, imageId } = req.params;
    const { script, audioUrl, videoUrl, visemeDataUrl } = req.body;

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
      defaults: {
        imageId,
        avatarId,
        script,
        audioUrl,
        videoUrl,
        visemeDataUrl,
        isActive: true,
      },
    });

    if (!created) {
      // Update existing mapping
      await mapping.update({
        script: script !== undefined ? script : mapping.script,
        audioUrl: audioUrl !== undefined ? audioUrl : mapping.audioUrl,
        videoUrl: videoUrl !== undefined ? videoUrl : mapping.videoUrl,
        visemeDataUrl:
          visemeDataUrl !== undefined ? visemeDataUrl : mapping.visemeDataUrl,
        isActive: true,
      });
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

// Update mapping with generated media URLs
router.put("/mapping/:mappingId", async (req: Request, res: Response) => {
  try {
    const { mappingId } = req.params;
    const { script, audioUrl, videoUrl, visemeDataUrl } = req.body;

    const mapping = await ImageAvatarMapping.findByPk(mappingId);

    if (!mapping) {
      return res.status(404).json({ error: "Mapping not found" });
    }

    await mapping.update({
      script: script !== undefined ? script : mapping.script,
      audioUrl: audioUrl !== undefined ? audioUrl : mapping.audioUrl,
      videoUrl: videoUrl !== undefined ? videoUrl : mapping.videoUrl,
      visemeDataUrl:
        visemeDataUrl !== undefined ? visemeDataUrl : mapping.visemeDataUrl,
    });

    return res.json({
      message: "Mapping updated successfully",
      mapping,
    });
  } catch (error) {
    console.error("Error updating mapping:", error);
    return res.status(500).json({ error: "Failed to update mapping" });
  }
});

export default router;
