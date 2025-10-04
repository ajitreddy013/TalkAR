import express from "express";
import multer from "multer";
import path from "path";
import fs from "fs";
import { v4 as uuidv4 } from "uuid";

// In-memory storage for image sets (in production, use a database)
let imageSets: any[] = [];

const router = express.Router();

// Configure multer for multiple file uploads
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    const uploadPath = path.join(__dirname, "../../uploads");
    if (!fs.existsSync(uploadPath)) {
      fs.mkdirSync(uploadPath, { recursive: true });
    }
    cb(null, uploadPath);
  },
  filename: (req, file, cb) => {
    const uniqueName = `${uuidv4()}-${file.originalname}`;
    cb(null, uniqueName);
  },
});

const upload = multer({
  storage,
  limits: {
    fileSize: 10 * 1024 * 1024, // 10MB per file
    files: 7, // Maximum 7 files per request
  },
  fileFilter: (req, file, cb) => {
    const allowedTypes = /jpeg|jpg|png|gif|bmp/;
    const extname = allowedTypes.test(
      path.extname(file.originalname).toLowerCase()
    );
    const mimetype = allowedTypes.test(file.mimetype);

    if (mimetype && extname) {
      return cb(null, true);
    } else {
      cb(new Error("Only image files are allowed"));
    }
  },
});

// Interface for multi-image upload
interface MultiImageUpload {
  objectName: string;
  description: string;
  images: {
    type:
      | "front"
      | "left_angle"
      | "right_angle"
      | "bright"
      | "dim"
      | "close"
      | "far";
    file: Express.Multer.File;
    description: string;
    required: boolean;
  }[];
}

// POST /api/multi-images - Upload multiple images for one object
router.post("/", upload.array("images", 7), async (req, res) => {
  try {
    const { objectName, description, imageTypes } = req.body;
    const files = req.files as Express.Multer.File[];

    if (!objectName) {
      return res.status(400).json({ error: "Object name is required" });
    }

    if (!files || files.length === 0) {
      return res.status(400).json({ error: "At least one image is required" });
    }

    // Parse image types from request body
    const types = JSON.parse(imageTypes || "[]");

    if (types.length !== files.length) {
      return res
        .status(400)
        .json({ error: "Number of image types must match number of files" });
    }

    // Validate required image types
    const requiredTypes = [
      "front",
      "left_angle",
      "right_angle",
      "bright",
      "dim",
    ];
    const providedTypes = types.map((t: any) => t.type);
    const missingTypes = requiredTypes.filter(
      (type) => !providedTypes.includes(type)
    );

    if (missingTypes.length > 0) {
      return res.status(400).json({
        error: `Missing required image types: ${missingTypes.join(", ")}`,
      });
    }

    // Create image set record
    const imageSetId = uuidv4();
    const imageSet: any = {
      id: imageSetId,
      objectName,
      description,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      images: [],
    };

    // Process each image
    for (let i = 0; i < files.length; i++) {
      const file = files[i];
      const imageType = types[i];

      // Create image record
      const imageId = uuidv4();
      const imageRecord = {
        id: imageId,
        objectSetId: imageSetId,
        objectName,
        name: `${objectName}_${imageType.type}`,
        description: imageType.description,
        imageType: imageType.type,
        imageUrl: `/uploads/${file.filename}`,
        filePath: file.path,
        fileName: file.filename,
        originalName: file.originalname,
        fileSize: file.size,
        mimeType: file.mimetype,
        required: imageType.required,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };

      // Save to database (you'll need to implement this)
      // await ImageRecognition.create(imageRecord);

      imageSet.images.push(imageRecord);
    }

    // Save image set to in-memory storage
    imageSets.push(imageSet);

    return res.status(201).json({
      success: true,
      message: `Successfully uploaded ${files.length} images for ${objectName}`,
      imageSet: {
        id: imageSetId,
        objectName,
        description,
        imageCount: files.length,
        images: imageSet.images.map((img: any) => ({
          id: img.id,
          name: img.name,
          imageType: img.imageType,
          imageUrl: img.imageUrl,
          required: img.required,
        })),
      },
    });
  } catch (error) {
    console.error("Error uploading multi-images:", error);
    return res.status(500).json({
      error: "Failed to upload images",
      details: error instanceof Error ? error.message : "Unknown error",
    });
  }
});

// GET /api/multi-images - Get all image sets
router.get("/", async (req, res) => {
  try {
    // Return actual image sets from in-memory storage
    const responseData = imageSets.map((set) => ({
      id: set.id,
      objectName: set.objectName,
      description: set.description,
      imageCount: set.images.length,
      createdAt: set.createdAt,
      images: set.images.map((img: any) => ({
        id: img.id,
        name: img.name,
        imageType: img.imageType,
        imageUrl: img.imageUrl,
        required: img.required,
      })),
    }));

    return res.json({
      success: true,
      imageSets: responseData,
    });
  } catch (error) {
    console.error("Error fetching image sets:", error);
    return res.status(500).json({
      error: "Failed to fetch image sets",
      details: error instanceof Error ? error.message : "Unknown error",
    });
  }
});

// GET /api/multi-images/:id - Get specific image set
router.get("/:id", async (req, res) => {
  try {
    const { id } = req.params;

    // Find image set from in-memory storage
    const imageSet = imageSets.find((set) => set.id === id);

    if (!imageSet) {
      return res.status(404).json({ error: "Image set not found" });
    }

    const responseData = {
      id: imageSet.id,
      objectName: imageSet.objectName,
      description: imageSet.description,
      imageCount: imageSet.images.length,
      createdAt: imageSet.createdAt,
      images: imageSet.images.map((img: any) => ({
        id: img.id,
        name: img.name,
        imageType: img.imageType,
        imageUrl: img.imageUrl,
        required: img.required,
      })),
    };

    return res.json({
      success: true,
      imageSet: responseData,
    });
  } catch (error) {
    console.error("Error fetching image set:", error);
    return res.status(500).json({
      error: "Failed to fetch image set",
      details: error instanceof Error ? error.message : "Unknown error",
    });
  }
});

// GET /api/multi-images/:id/images - Get all images for an object
router.get("/:id/images", async (req, res) => {
  try {
    const { id } = req.params;

    // Find image set from in-memory storage
    const imageSet = imageSets.find((set) => set.id === id);

    if (!imageSet) {
      return res.status(404).json({ error: "Image set not found" });
    }

    const images = imageSet.images.map((img: any) => ({
      id: img.id,
      name: img.name,
      imageType: img.imageType,
      imageUrl: img.imageUrl,
      required: img.required,
    }));

    return res.json({
      success: true,
      images,
    });
  } catch (error) {
    console.error("Error fetching images:", error);
    return res.status(500).json({
      error: "Failed to fetch images",
      details: error instanceof Error ? error.message : "Unknown error",
    });
  }
});

// DELETE /api/multi-images/:id - Delete image set
router.delete("/:id", async (req, res) => {
  try {
    const { id } = req.params;

    // Find image set in in-memory storage
    const imageSetIndex = imageSets.findIndex((set) => set.id === id);

    if (imageSetIndex === -1) {
      return res.status(404).json({ error: "Image set not found" });
    }

    const imageSet = imageSets[imageSetIndex];

    // Delete physical files
    for (const image of imageSet.images) {
      if (fs.existsSync(image.filePath)) {
        try {
          fs.unlinkSync(image.filePath);
        } catch (err) {
          console.warn(`Failed to delete file ${image.filePath}:`, err);
        }
      }
    }

    // Remove from in-memory storage
    imageSets.splice(imageSetIndex, 1);

    return res.json({
      success: true,
      message: "Image set deleted successfully",
    });
  } catch (error) {
    console.error("Error deleting image set:", error);
    return res.status(500).json({
      error: "Failed to delete image set",
      details: error instanceof Error ? error.message : "Unknown error",
    });
  }
});

// GET /api/multi-images/download/:objectName - Download all images for mobile app
router.get("/download/:objectName", async (req, res) => {
  try {
    const { objectName } = req.params;

    // Find all image sets for this object name
    const matchingSets = imageSets.filter(
      (set) => set.objectName === objectName
    );

    if (matchingSets.length === 0) {
      return res.status(404).json({ error: "No images found for this object" });
    }

    // Flatten all images from matching sets
    const allImages = matchingSets.flatMap((set) => set.images);

    return res.json({
      success: true,
      objectName,
      imageCount: allImages.length,
      images: allImages.map((img) => ({
        id: img.id,
        name: img.name,
        imageType: img.imageType,
        imageUrl: `${req.protocol}://${req.get("host")}${img.imageUrl}`,
        required: img.required,
      })),
    });
  } catch (error) {
    console.error("Error downloading images:", error);
    return res.status(500).json({
      error: "Failed to download images",
      details: error instanceof Error ? error.message : "Unknown error",
    });
  }
});

export default router;
