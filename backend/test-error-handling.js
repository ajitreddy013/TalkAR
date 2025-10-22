const axios = require("axios");

// Test error handling in the AI pipeline
async function testErrorHandling() {
  const baseUrl = "http://localhost:3000/api/v1/ai-pipeline";

  try {
    console.log("Testing error handling in AI Pipeline...\n");

    // Test missing imageId
    console.log("1. Testing missing imageId...");
    try {
      const response = await axios.post(`${baseUrl}/generate`, {
        language: "en",
        emotion: "happy",
      });
      console.log("Unexpected success:", response.data);
    } catch (error) {
      console.log("Expected error for missing imageId:", error.response?.data);
    }

    // Test missing text for audio generation
    console.log("\n2. Testing missing text for audio generation...");
    try {
      const response = await axios.post(`${baseUrl}/generate_audio`, {
        language: "en",
        emotion: "happy",
      });
      console.log("Unexpected success:", response.data);
    } catch (error) {
      console.log("Expected error for missing text:", error.response?.data);
    }

    // Test missing required parameters for lip-sync
    console.log("\n3. Testing missing required parameters for lip-sync...");
    try {
      const response = await axios.post(`${baseUrl}/generate_lipsync`, {
        imageId: "test-image-123",
        // Missing audioUrl
        emotion: "happy",
      });
      console.log("Unexpected success:", response.data);
    } catch (error) {
      console.log("Expected error for missing audioUrl:", error.response?.data);
    }

    // Test job not found
    console.log("\n4. Testing job not found...");
    try {
      const response = await axios.get(`${baseUrl}/status/nonexistent-job-id`);
      console.log("Unexpected success:", response.data);
    } catch (error) {
      console.log("Expected error for job not found:", error.response?.data);
    }

    // Test successful pipeline
    console.log("\n5. Testing successful pipeline...");
    const pipelineResponse = await axios.post(`${baseUrl}/generate`, {
      imageId: "test-image-123",
      language: "en",
      emotion: "happy",
    });
    console.log("Pipeline response:", pipelineResponse.data);

    // Wait and check job status
    console.log("\n6. Checking job status...");
    await new Promise((resolve) => setTimeout(resolve, 3000));

    const statusResponse = await axios.get(
      `${baseUrl}/status/${pipelineResponse.data.jobId}`
    );
    console.log("Job status:", statusResponse.data);

    console.log("\nAll error handling tests completed successfully!");
  } catch (error) {
    console.error("Test failed:", error.response?.data || error.message);
  }
}

// Run the test
testErrorHandling();
