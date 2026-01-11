const express = require("express");
const sqlite3 = require("sqlite3").verbose();
const path = require("path");
const cors = require("cors");
const fs = require("fs");
const multer = require("multer");
const { v4: uuidv4 } = require("uuid");


const app = express();
const port = 4000;

// Middleware
app.use(cors());
app.use(express.json());

// Serve static files from uploads directory
app.use("/uploads", express.static(path.join(__dirname, "uploads")));

// Configure multer for file uploads
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    const uploadPath = path.join(__dirname, "uploads");
    if (!fs.existsSync(uploadPath)) {
      fs.mkdirSync(uploadPath, { recursive: true });
    }
    cb(null, uploadPath);
  },
  filename: (req, file, cb) => {
    const uniqueName = `${uuidv4()}${path.extname(file.originalname)}`;
    cb(null, uniqueName);
  },
});

const upload = multer({
  storage,
  limits: { fileSize: 5 * 1024 * 1024 }, // 5MB limit
  fileFilter: (req, file, cb) => {
    const allowedTypes = /jpeg|jpg|png|gif|webp/;
    const extname = allowedTypes.test(
      path.extname(file.originalname).toLowerCase()
    );
    const mimetype = allowedTypes.test(file.mimetype);
    if (mimetype && extname) {
      return cb(null, true);
    }
    cb(new Error("Only images are allowed"));
  },
});

// Database connection
const dbPath = path.join(__dirname, "data", "database.sqlite");
const db = new sqlite3.Database(dbPath, (err) => {
  if (err) {
    console.error("Error opening database:", err.message);
  } else {
    console.log("Connected to the SQLite database.");
  }
});

// API endpoint to get images
app.get("/api/v1/images", (req, res) => {
  db.all(
    "SELECT id, name, description, imageUrl FROM images WHERE isActive = 1",
    [],
    (err, rows) => {
      if (err) {
        console.error("Error fetching images:", err.message);
        res.status(500).json({ error: "Failed to fetch images" });
      } else {
        // Get list of JPG files in uploads directory
        const uploadsDir = path.join(__dirname, "uploads");
        let uploadFiles = [];
        try {
          uploadFiles = fs
            .readdirSync(uploadsDir)
            .filter((file) => file.endsWith(".jpg"));
        } catch (error) {
          console.error("Error reading uploads directory:", error);
        }

        // Transform the data to use local image URLs
        const images = rows.map((row, index) => {
          // Map each product to a specific test image
          const imageMap = {
            "Sunrich Water Bottle": "water-bottle.jpg",
            "Amul Butter": "butter.jpg",
            "Professional Wireless Headphones": "headphones.jpg",
            "Nike Running Shoes": "shoes.jpg",
            "Eco-Friendly Backpack": "backpack.jpg",
          };

          // Find the appropriate image file for this product
          let imageFile = imageMap[row.name];
          if (!imageFile || !uploadFiles.includes(imageFile)) {
            // Fallback to using files in order
            imageFile = uploadFiles[index % uploadFiles.length];
          }

          const localImageUrl = `http://localhost:4000/uploads/${imageFile}`;

          return {
            id: row.id,
            name: row.name,
            description: row.description,
            imageUrl: localImageUrl,
            thumbnailUrl: localImageUrl, // Using local image as thumbnail
            isActive: true,
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString(),
          };
        });
        res.json(images);
      }
    }
  );
});

// API endpoint to upload image
app.post("/api/v1/images", upload.single("image"), (req, res) => {
  try {
    const { name, description } = req.body;
    const file = req.file;

    if (!file || !name) {
      return res.status(400).json({ error: "Image file and name are required" });
    }

    const id = uuidv4();
    const imageUrl = `http://localhost:4000/uploads/${file.filename}`;
    const now = new Date().toISOString();

    db.run(
      "INSERT INTO images (id, name, description, imageUrl, isActive, createdAt, updatedAt) VALUES (?, ?, ?, ?, ?, ?, ?)",
      [id, name, description || "", imageUrl, 1, now, now],
      function (err) {
        if (err) {
          console.error("Error saving image to DB:", err.message);
          return res.status(500).json({ error: "Failed to save image to database" });
        }

        res.status(201).json({
          id,
          name,
          description,
          imageUrl,
          isActive: true,
          createdAt: now,
          updatedAt: now,
        });
      }
    );
  } catch (error) {
    console.error("Upload error:", error);
    res.status(500).json({ error: "Internal server error during upload" });
  }
});

// Start server
app.listen(port, "0.0.0.0", () => {
  console.log(`Simple server running on port ${port}`);
  console.log(`Server accessible at: http://0.0.0.0:${port}`);
  console.log(`Local access: http://localhost:${port}`);
  console.log(`Images will be served from: http://localhost:4000/uploads/`);
});
