import express from "express";
import multer from "multer";
import path from "path";
import fs from "fs";
import { v4 as uuidv4 } from "uuid";

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

    // Save image set to database
    // await ImageSet.create(imageSet);

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
    // Get all image sets from database
    // const imageSets = await ImageSet.findAll({
    //   include: [ImageRecognition]
    // });

    // Mock data for now
    const imageSets = [
      {
        id: "1",
        objectName: "TalkAR Logo",
        description: "Main company logo",
        imageCount: 5,
        createdAt: new Date().toISOString(),
        images: [
          {
            id: "1",
            name: "TalkAR Logo Front",
            imageType: "front",
            imageUrl: "/uploads/logo-front.jpg",
          },
          {
            id: "2",
            name: "TalkAR Logo Left",
            imageType: "left_angle",
            imageUrl: "/uploads/logo-left.jpg",
          },
          {
            id: "3",
            name: "TalkAR Logo Right",
            imageType: "right_angle",
            imageUrl: "/uploads/logo-right.jpg",
          },
          {
            id: "4",
            name: "TalkAR Logo Bright",
            imageType: "bright",
            imageUrl: "/uploads/logo-bright.jpg",
          },
          {
            id: "5",
            name: "TalkAR Logo Dim",
            imageType: "dim",
            imageUrl: "/uploads/logo-dim.jpg",
          },
        ],
      },
    ];

    return res.json({
      success: true,
      imageSets,
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

    // Get image set from database
    // const imageSet = await ImageSet.findByPk(id, {
    //   include: [ImageRecognition]
    // });

    // Mock data for now
    const imageSet = {
      id,
      objectName: "TalkAR Logo",
      description: "Main company logo",
      imageCount: 5,
      createdAt: new Date().toISOString(),
      images: [
        {
          id: "1",
          name: "TalkAR Logo Front",
          imageType: "front",
          imageUrl: "/uploads/logo-front.jpg",
        },
        {
          id: "2",
          name: "TalkAR Logo Left",
          imageType: "left_angle",
          imageUrl: "/uploads/logo-left.jpg",
        },
        {
          id: "3",
          name: "TalkAR Logo Right",
          imageType: "right_angle",
          imageUrl: "/uploads/logo-right.jpg",
        },
        {
          id: "4",
          name: "TalkAR Logo Bright",
          imageType: "bright",
          imageUrl: "/uploads/logo-bright.jpg",
        },
        {
          id: "5",
          name: "TalkAR Logo Dim",
          imageType: "dim",
          imageUrl: "/uploads/logo-dim.jpg",
        },
      ],
    };

    if (!imageSet) {
      return res.status(404).json({ error: "Image set not found" });
    }

    return res.json({
      success: true,
      imageSet,
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

    // Get all images for this object set
    // const images = await ImageRecognition.findAll({
    //   where: { objectSetId: id }
    // });

    // Mock data for now
    const images = [
      {
        id: "1",
        name: "TalkAR Logo Front",
        imageType: "front",
        imageUrl: "/uploads/logo-front.jpg",
      },
      {
        id: "2",
        name: "TalkAR Logo Left",
        imageType: "left_angle",
        imageUrl: "/uploads/logo-left.jpg",
      },
      {
        id: "3",
        name: "TalkAR Logo Right",
        imageType: "right_angle",
        imageUrl: "/uploads/logo-right.jpg",
      },
      {
        id: "4",
        name: "TalkAR Logo Bright",
        imageType: "bright",
        imageUrl: "/uploads/logo-bright.jpg",
      },
      {
        id: "5",
        name: "TalkAR Logo Dim",
        imageType: "dim",
        imageUrl: "/uploads/logo-dim.jpg",
      },
    ];

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

    // Get image set and delete all associated images
    // const imageSet = await ImageSet.findByPk(id, {
    //   include: [ImageRecognition]
    // });

    // if (!imageSet) {
    //   return res.status(404).json({ error: 'Image set not found' });
    // }

    // Delete physical files
    // for (const image of imageSet.images) {
    //   if (fs.existsSync(image.filePath)) {
    //     fs.unlinkSync(image.filePath);
    //   }
    // }

    // Delete from database
    // await ImageSet.destroy({ where: { id } });
    // await ImageRecognition.destroy({ where: { objectSetId: id } });

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

    // Get all images for this object
    // const images = await ImageRecognition.findAll({
    //   where: { objectName }
    // });

    // Mock data for now
    const images = [
      {
        id: "1",
        name: "TalkAR Logo Front",
        imageType: "front",
        imageUrl: "/uploads/logo-front.jpg",
      },
      {
        id: "2",
        name: "TalkAR Logo Left",
        imageType: "left_angle",
        imageUrl: "/uploads/logo-left.jpg",
      },
      {
        id: "3",
        name: "TalkAR Logo Right",
        imageType: "right_angle",
        imageUrl: "/uploads/logo-right.jpg",
      },
      {
        id: "4",
        name: "TalkAR Logo Bright",
        imageType: "bright",
        imageUrl: "/uploads/logo-bright.jpg",
      },
      {
        id: "5",
        name: "TalkAR Logo Dim",
        imageType: "dim",
        imageUrl: "/uploads/logo-dim.jpg",
      },
    ];

    if (images.length === 0) {
      return res.status(404).json({ error: "No images found for this object" });
    }

    return res.json({
      success: true,
      objectName,
      imageCount: images.length,
      images: images.map((img) => ({
        id: img.id,
        name: img.name,
        imageType: img.imageType,
        imageUrl: `${req.protocol}://${req.get("host")}${img.imageUrl}`,
        required: [
          "front",
          "left_angle",
          "right_angle",
          "bright",
          "dim",
        ].includes(img.imageType),
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
