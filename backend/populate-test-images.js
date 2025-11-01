const { Image, Dialogue } = require("./dist/models/Image");
const { sequelize } = require("./dist/config/database");
const fs = require("fs");
const path = require("path");

async function populateTestImages() {
  try {
    // Sync database
    await sequelize.sync();

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

    // Add each product as an image in the database
    for (const product of productMetadata) {
      try {
        // Check if image already exists
        const existingImage = await Image.findOne({
          where: { name: product.product_name },
        });
        if (existingImage) {
          console.log(
            `Image ${product.product_name} already exists, skipping...`
          );
          continue;
        }

        // Create image record
        const image = await Image.create({
          name: product.product_name,
          description: product.description,
          imageUrl: product.image_url,
          isActive: true,
        });

        console.log(
          `Created image: ${product.product_name} with ID: ${image.id}`
        );

        // Create a default dialogue for the image
        const dialogue = await Dialogue.create({
          imageId: image.id,
          text: `Check out this amazing ${product.product_name}!`,
          language: product.language || "English",
          emotion: "neutral",
          tone: product.tone || "friendly",
          isActive: true,
          isDefault: true,
        });

        console.log(`Created dialogue for ${product.product_name}`);
      } catch (error) {
        console.error(
          `Error creating image for ${product.product_name}:`,
          error.message
        );
      }
    }

    console.log("Finished populating test images");

    // Verify the images were added
    const images = await Image.findAll({
      include: [{ model: Dialogue, as: "dialogues" }],
    });
    console.log(`\nTotal images in database: ${images.length}`);
    images.forEach((image) => {
      console.log(`- ${image.name} (${image.dialogues.length} dialogues)`);
    });

    process.exit(0);
  } catch (error) {
    console.error("Error populating test images:", error);
    process.exit(1);
  }
}

// Run the function
populateTestImages();
