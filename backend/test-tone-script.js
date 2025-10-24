const axios = require("axios");

// Test tone-based script generation
async function testToneScriptGeneration() {
  const baseUrl = "http://localhost:3000/api/v1/ai-pipeline";

  try {
    console.log("Testing tone-based script generation...\n");

    // Test script generation with different tones
    console.log("1. Testing script generation with 'excited' tone...");
    const scriptResponse1 = await axios.post(`${baseUrl}/generate_script`, {
      imageId: "poster_01",
      language: "en",
      emotion: "excited",
    });
    console.log("Script response:", scriptResponse1.data);

    console.log("\n2. Testing script generation with 'professional' tone...");
    const scriptResponse2 = await axios.post(`${baseUrl}/generate_script`, {
      imageId: "poster_03",
      language: "en",
      emotion: "professional",
    });
    console.log("Script response:", scriptResponse2.data);

    console.log("\n3. Testing script generation with 'friendly' tone...");
    const scriptResponse3 = await axios.post(`${baseUrl}/generate_script`, {
      imageId: "poster_02",
      language: "en",
      emotion: "friendly",
    });
    console.log("Script response:", scriptResponse3.data);

    console.log(
      "\nAll tone-based script generation tests completed successfully!"
    );
  } catch (error) {
    console.error("Test failed:", error.response?.data || error.message);
  }
}

// Run the test
testToneScriptGeneration();
