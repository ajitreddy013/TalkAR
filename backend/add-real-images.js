const sqlite3 = require("sqlite3").verbose();
const { v4: uuidv4 } = require("uuid");
const path = require("path");

// Open database
const dbPath = path.join(__dirname, "data", "database.sqlite");
const db = new sqlite3.Database(dbPath);

// Real product images from Unsplash (publicly accessible)
const newProducts = [
  {
    product_name: "Coca-Cola Classic",
    description: "The iconic refreshing cola drink loved worldwide. Perfect for any occasion!",
    image_url: "https://images.unsplash.com/photo-1554866585-cd94860890b7?w=800",
    language: "English",
    tone: "excited",
    script: "Quench your thirst with the refreshing taste of Coca-Cola! The perfect drink for any moment. Grab one today!"
  },
  {
    product_name: "Nike Air Max Shoes",
    description: "Premium athletic footwear designed for peak performance and style.",
    image_url: "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=800",
    language: "English",
    tone: "energetic",
    script: "Step up your game with Nike Air Max! Engineered for comfort, built for performance. Get yours now!"
  },
  {
    product_name: "Sony Wireless Headphones",
    description: "Professional wireless headphones with premium noise cancellation technology.",
    image_url: "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800",
    language: "English",
    tone: "professional",
    script: "Experience crystal-clear audio with Sony's premium wireless headphones. Immerse yourself in pure sound quality."
  },
  {
    product_name: "Hydro Flask Water Bottle",
    description: "Insulated stainless steel water bottle that keeps drinks cold for 24 hours.",
    image_url: "https://images.unsplash.com/photo-1602143407151-7111542de6e8?w=800",
    language: "English",
    tone: "friendly",
    script: "Stay hydrated in style! This premium water bottle keeps your drinks perfectly chilled all day long."
  },
  {
    product_name: "Premium Travel Backpack",
    description: "Durable and stylish backpack perfect for everyday adventures and travel.",
    image_url: "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=800",
    language: "English",
    tone: "enthusiastic",
    script: "Adventure awaits! This premium backpack has everything you need for your next journey. Grab yours today!"
  }
];

console.log(`Adding ${newProducts.length} new products with real image URLs...\n`);

db.serialize(() => {
  newProducts.forEach((product) => {
    const imageId = uuidv4();
    const now = new Date().toISOString();

    // Check if image already exists
    db.get(
      "SELECT id FROM images WHERE name = ?",
      [product.product_name],
      (err, row) => {
        if (err) {
          console.error(`❌ Error checking for ${product.product_name}:`, err.message);
          return;
        }

        if (row) {
          console.log(`⏭️  ${product.product_name} already exists, skipping...`);
          return;
        }

        // Insert image
        db.run(
          "INSERT INTO images (id, name, description, imageUrl, isActive, createdAt, updatedAt) VALUES (?, ?, ?, ?, ?, ?, ?)",
          [imageId, product.product_name, product.description, product.image_url, 1, now, now],
          function (err) {
            if (err) {
              console.error(`❌ Error inserting ${product.product_name}:`, err.message);
            } else {
              console.log(`✅ Added image: ${product.product_name}`);
              console.log(`   URL: ${product.image_url}`);

              // Insert dialogue
              const dialogueId = uuidv4();
              db.run(
                "INSERT INTO dialogues (id, imageId, text, language, emotion, tone, isActive, isDefault, createdAt, updatedAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                [dialogueId, imageId, product.script, product.language, "happy", product.tone, 1, 1, now, now],
                function (err) {
                  if (err) {
                    console.error(`❌ Error inserting dialogue for ${product.product_name}:`, err.message);
                  } else {
                    console.log(`   ✅ Added dialogue\n`);
                  }
                }
              );
            }
          }
        );
      }
    );
  });

  // Close database after a delay
  setTimeout(() => {
    db.close((err) => {
      if (err) {
        console.error("Error closing database:", err.message);
      } else {
        console.log("\n✅ Database updated successfully!");
        console.log("\nTo view all images:");
        console.log('sqlite3 data/database.sqlite "SELECT name, imageUrl FROM images;"');
      }
    });
  }, 2000);
});
