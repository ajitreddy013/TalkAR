const axios = require("axios");

// Test product script generation
async function testProductScript() {
  const baseUrl = "http://localhost:3000/api/v1/ai-pipeline";

  try {
    console.log("Testing product script generation...\n");

    // Test product script generation
    console.log("1. Testing product script generation...");
    const productResponse = await axios.post(
      `${baseUrl}/generate_product_script`,
      {
        productName: "iPhone",
      }
    );
    console.log("Product script response:", productResponse.data.script);

    // Test another product
    console.log("\n2. Testing another product script generation...");
    const productResponse2 = await axios.post(
      `${baseUrl}/generate_product_script`,
      {
        productName: "MacBook",
      }
    );
    console.log("Product script response:", productResponse2.data.script);

    // Test with a new product (should use mock)
    console.log("\n3. Testing new product script generation...");
    const productResponse3 = await axios.post(
      `${baseUrl}/generate_product_script`,
      {
        productName: "Smart Refrigerator",
      }
    );
    console.log("Product script response:", productResponse3.data.script);

    console.log("\nAll product script tests completed successfully!");
  } catch (error) {
    console.error("Test failed:", error.response?.data || error.message);
  }
}

// Run the test
testProductScript();
