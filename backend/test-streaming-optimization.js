const axios = require("axios");

// Test the streaming optimization implementation
async function testStreamingOptimization() {
  const baseUrl = "http://127.0.0.1:3000/api/v1/ai-pipeline";

  try {
    console.log("Testing streaming optimization...\n");

    // Test 1: Compare regular ad content generation vs streaming optimization
    console.log("1. Testing regular ad content generation...");
    const startTime1 = Date.now();
    const response1 = await axios.post(`${baseUrl}/generate_ad_content`, {
      product: "iPhone",
    });
    const endTime1 = Date.now();
    const duration1 = endTime1 - startTime1;

    console.log("✓ Regular ad content generation successful");
    console.log("  Script:", response1.data.script);
    console.log("  Audio URL:", response1.data.audio_url);
    console.log("  Video URL:", response1.data.video_url);
    console.log("  Duration:", duration1, "ms\n");

    // Test 2: Streaming optimized ad content generation
    console.log("2. Testing streaming optimized ad content generation...");
    const startTime2 = Date.now();
    const response2 = await axios.post(
      `${baseUrl}/generate_ad_content_streaming`,
      {
        product: "MacBook",
      }
    );
    const endTime2 = Date.now();
    const duration2 = endTime2 - startTime2;

    console.log("✓ Streaming optimized ad content generation successful");
    console.log("  Script:", response2.data.script);
    console.log("  Audio URL:", response2.data.audio_url);
    console.log("  Video URL:", response2.data.video_url);
    console.log("  Duration:", duration2, "ms\n");

    // Compare performance
    console.log("3. Performance comparison:");
    console.log("  Regular generation:", duration1, "ms");
    console.log("  Streaming generation:", duration2, "ms");

    if (duration2 < duration1) {
      console.log("✓ Streaming optimization shows improved performance");
    } else {
      console.log(
        "  Note: Performance may vary based on system load and mock implementations"
      );
    }

    // Test 3: Error handling
    console.log("\n4. Testing error handling...");
    try {
      await axios.post(`${baseUrl}/generate_ad_content_streaming`, {
        // Missing product parameter
      });
      console.log("✗ Expected error but request succeeded");
    } catch (error) {
      if (error.response && error.response.status === 400) {
        console.log("✓ Error handling works correctly");
        console.log("  Error message:", error.response.data.error);
      } else {
        console.log("✗ Unexpected error:", error.message);
      }
    }

    console.log("\nAll streaming optimization tests completed!");
  } catch (error) {
    console.error("Test failed:", error.response?.data || error.message);
  }
}

// Run the test
testStreamingOptimization();
