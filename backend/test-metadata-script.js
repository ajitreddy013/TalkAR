const axios = require("axios");

// Test metadata-based script generation
async function testMetadataScriptGeneration() {
  const baseUrl = "http://localhost:3000/api/v1/ai-pipeline";

  try {
    console.log("Testing metadata-based script generation...\n");

    // Test script generation with metadata (poster_01)
    console.log(
      "1. Testing script generation with metadata (Sunrich Water Bottle)..."
    );
    const scriptResponse1 = await axios.post(`${baseUrl}/generate_script`, {
      imageId: "poster_01",
      language: "en",
      emotion: "excited",
    });
    console.log("Script response:", scriptResponse1.data);

    // Test script generation with metadata (poster_02)
    console.log(
      "\n2. Testing script generation with metadata (Eco-Friendly Backpack)..."
    );
    const scriptResponse2 = await axios.post(`${baseUrl}/generate_script`, {
      imageId: "poster_02",
      language: "en",
      emotion: "enthusiastic",
    });
    console.log("Script response:", scriptResponse2.data);

    // Test script generation without metadata (should fall back to existing behavior)
    console.log("\n3. Testing script generation without metadata...");
    const scriptResponse3 = await axios.post(`${baseUrl}/generate_script`, {
      imageId: "test-image-123",
      language: "en",
      emotion: "happy",
    });
    console.log("Script response:", scriptResponse3.data);

    console.log(
      "\nAll metadata-based script generation tests completed successfully!"
    );
  } catch (error) {
    console.error("Test failed:", error.response?.data || error.message);
  }
}

// Run the test
testMetadataScriptGeneration();
