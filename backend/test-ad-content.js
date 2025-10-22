const axios = require("axios");

// Test the generate_ad_content endpoint
async function testAdContentGeneration() {
  const baseUrl = "http://localhost:3000/api/v1/ai-pipeline";

  try {
    console.log("Testing ad content generation endpoint...\n");

    // Test 1: Successful ad content generation
    console.log("1. Testing successful ad content generation...");
    const adContentResponse = await axios.post(
      `${baseUrl}/generate_ad_content`,
      {
        product: "Sunrich Water Bottle",
      }
    );

    console.log("✓ Ad content generation successful");
    console.log("  Script:", adContentResponse.data.script);
    console.log("  Audio URL:", adContentResponse.data.audio_url);
    console.log("  Video URL:", adContentResponse.data.video_url);
    console.log("");

    // Test 2: Another product
    console.log("2. Testing ad content generation with another product...");
    const adContentResponse2 = await axios.post(
      `${baseUrl}/generate_ad_content`,
      {
        product: "Eco-Friendly Backpack",
      }
    );

    console.log("✓ Ad content generation successful");
    console.log("  Script:", adContentResponse2.data.script);
    console.log("  Audio URL:", adContentResponse2.data.audio_url);
    console.log("  Video URL:", adContentResponse2.data.video_url);
    console.log("");

    // Test 3: Error handling - missing product parameter
    console.log("3. Testing error handling for missing product parameter...");
    try {
      await axios.post(`${baseUrl}/generate_ad_content`, {});
      console.log("✗ Expected error but request succeeded");
    } catch (error) {
      if (error.response && error.response.status === 400) {
        console.log(
          "✓ Error handling for missing product parameter works correctly"
        );
        console.log("  Error message:", error.response.data.error);
      } else {
        console.log("✗ Unexpected error:", error.message);
      }
    }
    console.log("");

    // Test 4: Error handling - empty product name
    console.log("4. Testing error handling for empty product name...");
    try {
      await axios.post(`${baseUrl}/generate_ad_content`, {
        product: "",
      });
      console.log("✗ Expected error but request succeeded");
    } catch (error) {
      if (error.response && error.response.status === 400) {
        console.log("✓ Error handling for empty product name works correctly");
        console.log("  Error message:", error.response.data.error);
      } else {
        console.log("✗ Unexpected error:", error.message);
      }
    }
    console.log("");

    // Test 5: Error handling - product name too long
    console.log("5. Testing error handling for product name too long...");
    try {
      const longProductName = "A".repeat(150);
      await axios.post(`${baseUrl}/generate_ad_content`, {
        product: longProductName,
      });
      console.log("✗ Expected error but request succeeded");
    } catch (error) {
      if (error.response && error.response.status === 400) {
        console.log("✓ Error handling for long product name works correctly");
        console.log("  Error message:", error.response.data.error);
      } else {
        console.log("✗ Unexpected error:", error.message);
      }
    }
    console.log("");

    console.log("All ad content generation tests completed successfully!");
  } catch (error) {
    console.error("Test failed:", error.response?.data || error.message);
  }
}

// Run the test
testAdContentGeneration();
