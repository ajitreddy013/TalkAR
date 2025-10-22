const axios = require("axios");

// Test TTS functionality
async function testTTS() {
  const baseUrl = "http://localhost:3000/api/v1/ai-pipeline";

  try {
    console.log("Testing TTS functionality...\n");

    // Test audio generation
    console.log("1. Testing audio generation...");
    const audioResponse = await axios.post(`${baseUrl}/generate_audio`, {
      text: "Welcome to our amazing product showcase. Today we'll explore the latest innovations in technology.",
      language: "en",
      emotion: "happy",
    });
    console.log("Audio response:", audioResponse.data);

    // Test another audio generation with different parameters
    console.log("\n2. Testing another audio generation...");
    const audioResponse2 = await axios.post(`${baseUrl}/generate_audio`, {
      text: "Please pay attention to this important announcement regarding our new product line.",
      language: "en",
      emotion: "serious",
    });
    console.log("Audio response:", audioResponse2.data);

    console.log("\nAll TTS tests completed successfully!");
  } catch (error) {
    console.error("Test failed:", error.response?.data || error.message);
  }
}

// Run the test
testTTS();
