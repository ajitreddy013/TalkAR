const axios = require("axios");

// Test ad content generation
async function testAdContentGeneration() {
  const baseUrl = "http://localhost:3000/api/v1/ai-pipeline";

  try {
    console.log("Testing ad content generation...\n");

    // Test ad content generation for iPhone
    console.log("1. Testing ad content generation for iPhone...");
    const adContentResponse1 = await axios.post(
      `${baseUrl}/generate_ad_content`,
      {
        product: "iPhone",
      }
    );
    console.log("Ad content response:", adContentResponse1.data);

    // Test ad content generation for MacBook
    console.log("\n2. Testing ad content generation for MacBook...");
    const adContentResponse2 = await axios.post(
      `${baseUrl}/generate_ad_content`,
      {
        product: "MacBook",
      }
    );
    console.log("Ad content response:", adContentResponse2.data);

    // Test ad content generation for Water Bottle
    console.log("\n3. Testing ad content generation for Water Bottle...");
    const adContentResponse3 = await axios.post(
      `${baseUrl}/generate_ad_content`,
      {
        product: "Water Bottle",
      }
    );
    console.log("Ad content response:", adContentResponse3.data);

    // Test error case - missing product
    console.log("\n4. Testing error case - missing product...");
    try {
      await axios.post(`${baseUrl}/generate_ad_content`, {});
    } catch (error) {
      console.log("Error response (expected):", error.response?.data);
    }

    // Test error case - empty product
    console.log("\n5. Testing error case - empty product...");
    try {
      await axios.post(`${baseUrl}/generate_ad_content`, {
        product: "",
      });
    } catch (error) {
      console.log("Error response (expected):", error.response?.data);
    }

    console.log("\nAll ad content generation tests completed successfully!");
  } catch (error) {
    console.error("Test failed:", error.response?.data || error.message);
  }
}

// Run the test
testAdContentGeneration();
