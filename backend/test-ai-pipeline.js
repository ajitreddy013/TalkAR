const axios = require("axios");

// Test the AI pipeline endpoints
async function testAIPipeline() {
  const baseUrl = "http://localhost:3000/api/v1/ai-pipeline";

  try {
    console.log("Testing AI Pipeline API endpoints...\n");

    // Test generate script endpoint
    console.log("1. Testing generate script endpoint...");
    const scriptResponse = await axios.post(`${baseUrl}/generate_script`, {
      imageId: "test-image-123",
      language: "en",
      emotion: "happy",
    });
    console.log("Script response:", scriptResponse.data);

    // Test generate audio endpoint
    console.log("\n2. Testing generate audio endpoint...");
    const audioResponse = await axios.post(`${baseUrl}/generate_audio`, {
      text: "Welcome to our exhibition!",
      language: "en",
      emotion: "happy",
    });
    console.log("Audio response:", audioResponse.data);

    // Test generate lip-sync endpoint
    console.log("\n3. Testing generate lip-sync endpoint...");
    const lipSyncResponse = await axios.post(`${baseUrl}/generate_lipsync`, {
      imageId: "test-image-123",
      audioUrl: "https://example.com/audio.mp3",
      emotion: "happy",
    });
    console.log("Lip-sync response:", lipSyncResponse.data);

    // Test complete pipeline endpoint
    console.log("\n4. Testing complete pipeline endpoint...");
    const pipelineResponse = await axios.post(`${baseUrl}/generate`, {
      imageId: "test-image-123",
      language: "en",
      emotion: "neutral",
    });
    console.log("Pipeline response:", pipelineResponse.data);

    // Test job status endpoint
    console.log("\n5. Testing job status endpoint...");
    // Wait a bit for processing
    await new Promise((resolve) => setTimeout(resolve, 3000));

    const statusResponse = await axios.get(
      `${baseUrl}/status/${pipelineResponse.data.jobId}`
    );
    console.log("Status response:", statusResponse.data);

    console.log("\nAll tests completed successfully!");
  } catch (error) {
    console.error("Test failed:", error.response?.data || error.message);
  }
}

// Run the test
testAIPipeline();
