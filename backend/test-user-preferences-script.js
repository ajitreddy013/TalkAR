const axios = require("axios");

// Test user preferences-based script generation
async function testUserPreferencesScriptGeneration() {
  const baseUrl = "http://localhost:3000/api/v1/ai-pipeline";

  try {
    console.log("Testing user preferences-based script generation...\n");

    // Test script generation with user preferences
    console.log("1. Testing script generation with user preferences...");
    const scriptResponse1 = await axios.post(`${baseUrl}/generate_script`, {
      imageId: "poster_01",
      language: "en",
      emotion: "neutral",
      userPreferences: {
        language: "English",
        preferred_tone: "casual",
      },
    });
    console.log("Script response:", scriptResponse1.data);

    console.log(
      "\n2. Testing script generation with different user preferences..."
    );
    const scriptResponse2 = await axios.post(`${baseUrl}/generate_script`, {
      imageId: "poster_02",
      language: "en",
      emotion: "neutral",
      userPreferences: {
        language: "English",
        preferred_tone: "professional",
      },
    });
    console.log("Script response:", scriptResponse2.data);

    console.log("\n3. Testing script generation without user preferences...");
    const scriptResponse3 = await axios.post(`${baseUrl}/generate_script`, {
      imageId: "poster_03",
      language: "en",
      emotion: "excited",
      // No userPreferences provided - should fall back to emotion
    });
    console.log("Script response:", scriptResponse3.data);

    console.log(
      "\nAll user preferences-based script generation tests completed successfully!"
    );
  } catch (error) {
    console.error("Test failed:", error.response?.data || error.message);
  }
}

// Run the test
testUserPreferencesScriptGeneration();
