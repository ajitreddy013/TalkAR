const sqlite3 = require("sqlite3").verbose();
const { v4: uuidv4 } = require("uuid");
const fs = require("fs");
const path = require("path");

// Open database
const dbPath = path.join(__dirname, "data", "database.sqlite");
const db = new sqlite3.Database(dbPath, (err) => {
  if (err) {
    console.error("Error opening database:", err.message);
  } else {
    console.log("Connected to the SQLite database.");
  }
});

// Read product metadata
const productMetadataPath = path.join(
  __dirname,
  "data",
  "product-metadata.json"
);
const productMetadata = JSON.parse(
  fs.readFileSync(productMetadataPath, "utf8")
);

console.log(`Found ${productMetadata.length} products in metadata`);

// Create tables if they don't exist
db.serialize(() => {
  db.run(
    `CREATE TABLE IF NOT EXISTS images (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    imageUrl TEXT NOT NULL,
    thumbnailUrl TEXT,
    isActive INTEGER DEFAULT 1,
    createdAt DATETIME NOT NULL,
    updatedAt DATETIME NOT NULL
  )`,
    (err) => {
      if (err) {
        console.error("Error creating images table:", err.message);
      } else {
        console.log("Images table ready");
      }
    }
  );

  db.run(
    `CREATE TABLE IF NOT EXISTS dialogues (
    id TEXT PRIMARY KEY,
    imageId TEXT NOT NULL,
    text TEXT NOT NULL,
    language TEXT NOT NULL,
    voiceId TEXT,
    emotion TEXT,
    tone TEXT,
    isActive INTEGER DEFAULT 1,
    isDefault INTEGER DEFAULT 0,
    createdAt DATETIME NOT NULL,
    updatedAt DATETIME NOT NULL,
    FOREIGN KEY (imageId) REFERENCES images (id)
  )`,
    (err) => {
      if (err) {
        console.error("Error creating dialogues table:", err.message);
      } else {
        console.log("Dialogues table ready");
      }
    }
  );
});

// Insert test images
setTimeout(() => {
  productMetadata.forEach((product, index) => {
    const imageId = uuidv4();
    const now = new Date().toISOString();

    // Check if image already exists
    db.get(
      "SELECT id FROM images WHERE name = ?",
      [product.product_name],
      (err, row) => {
        if (err) {
          console.error("Error checking for existing image:", err.message);
          return;
        }

        if (row) {
          console.log(
            `Image ${product.product_name} already exists, skipping...`
          );
          return;
        }

        // Insert image
        const imageStmt = db.prepare(
          "INSERT INTO images (id, name, description, imageUrl, isActive, createdAt, updatedAt) VALUES (?, ?, ?, ?, ?, ?, ?)"
        );
        imageStmt.run(
          imageId,
          product.product_name,
          product.description,
          product.image_url,
          1,
          now,
          now,
          function (err) {
            if (err) {
              console.error("Error inserting image:", err.message);
            } else {
              console.log(`Inserted image: ${product.product_name}`);

              // Insert dialogue
              const dialogueId = uuidv4();
              const dialogueStmt = db.prepare(
                "INSERT INTO dialogues (id, imageId, text, language, emotion, tone, isActive, isDefault, createdAt, updatedAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
              );
              dialogueStmt.run(
                dialogueId,
                imageId,
                `Check out this amazing ${product.product_name}!`,
                product.language || "English",
                "neutral",
                product.tone || "friendly",
                1,
                1,
                now,
                now,
                function (err) {
                  if (err) {
                    console.error("Error inserting dialogue:", err.message);
                  } else {
                    console.log(
                      `Inserted dialogue for ${product.product_name}`
                    );
                  }
                  dialogueStmt.finalize();
                }
              );
            }
            imageStmt.finalize();
          }
        );
      }
    );
  });

  // Close database after a delay to allow inserts to complete
  setTimeout(() => {
    db.close((err) => {
      if (err) {
        console.error("Error closing database:", err.message);
      } else {
        console.log("Database connection closed.");
      }
    });
  }, 2000);
}, 1000);
