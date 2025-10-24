const axios = require("axios");

// Test emotion support in the AI pipeline
async function testEmotionSupport() {
  const baseUrl = "http://localhost:3000/api/v1/ai-pipeline";

  try {
    console.log("Testing emotion support in AI Pipeline...\n");

    // Test happy emotion
    console.log("1. Testing happy emotion...");
    const happyResponse = await axios.post(`${baseUrl}/generate_script`, {
      imageId: "test-image-123",
      language: "en",
      emotion: "happy",
    });
    console.log("Happy emotion script:", happyResponse.data.script);

    // Test serious emotion
    console.log("\n2. Testing serious emotion...");
    const seriousResponse = await axios.post(`${baseUrl}/generate_script`, {
      imageId: "test-image-123",
      language: "en",
      emotion: "serious",
    });
    console.log("Serious emotion script:", seriousResponse.data.script);

    // Test surprised emotion
    console.log("\n3. Testing surprised emotion...");
    const surprisedResponse = await axios.post(`${baseUrl}/generate_script`, {
      imageId: "test-image-123",
      language: "en",
      emotion: "surprised",
    });
    console.log("Surprised emotion script:", surprisedResponse.data.script);

    // Test neutral emotion
    console.log("\n4. Testing neutral emotion...");
    const neutralResponse = await axios.post(`${baseUrl}/generate_script`, {
      imageId: "test-image-123",
      language: "en",
      emotion: "neutral",
    });
    console.log("Neutral emotion script:", neutralResponse.data.script);

    // Test complete pipeline with emotion
    console.log("\n5. Testing complete pipeline with emotion...");
    const pipelineResponse = await axios.post(`${baseUrl}/generate`, {
      imageId: "test-image-123",
      language: "en",
      emotion: "happy",
    });
    console.log("Pipeline response:", pipelineResponse.data);

    // Wait and check job status
    console.log("\n6. Checking job status with emotion...");
    await new Promise((resolve) => setTimeout(resolve, 3000));

    const statusResponse = await axios.get(
      `${baseUrl}/status/${pipelineResponse.data.jobId}`
    );
    console.log("Job status:", statusResponse.data.job);

    console.log("\nAll emotion tests completed successfully!");
  } catch (error) {
    console.error("Test failed:", error.response?.data || error.message);
  }
}

// Run the test
testEmotionSupport();
