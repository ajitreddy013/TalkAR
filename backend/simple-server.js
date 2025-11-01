const express = require("express");
const sqlite3 = require("sqlite3").verbose();
const path = require("path");
const cors = require("cors");
const fs = require("fs");

const app = express();
const port = 4000;

// Middleware
app.use(cors());
app.use(express.json());

// Serve static files from uploads directory
app.use("/uploads", express.static(path.join(__dirname, "uploads")));

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

          const localImageUrl = `http://10.136.91.236:4000/uploads/${imageFile}`;

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

// Start server
app.listen(port, "0.0.0.0", () => {
  console.log(`Simple server running on port ${port}`);
  console.log(`Server accessible at: http://0.0.0.0:${port}`);
  console.log(`Local access: http://localhost:${port}`);
  console.log(`Images will be served from: http://10.136.91.236:4000/uploads/`);
});
