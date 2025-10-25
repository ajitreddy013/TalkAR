const axios = require("axios");

// Test the performance optimizations
async function testPerformanceOptimization() {
  const baseUrl = "http://127.0.0.1:3000/api/v1/ai-pipeline";

  try {
    console.log("Testing performance optimizations...\n");

    // Test products for performance testing
    const testProducts = [
      "iPhone",
      "MacBook",
      "Tesla",
      "Nike Shoes",
      "Samsung TV",
    ];

    console.log("1. Testing audio generation performance...");
    const audioStartTime = Date.now();
    const audioResponse = await axios.post(`${baseUrl}/generate_audio`, {
      text: "Welcome to our amazing product showcase!",
      language: "en",
      emotion: "neutral",
    });
    const audioEndTime = Date.now();
    const audioDuration = audioEndTime - audioStartTime;

    console.log("✓ Audio generation completed");
    console.log("  Audio URL:", audioResponse.data.audioUrl);
    console.log("  Duration:", audioResponse.data.duration, "seconds");
    console.log("  Response time:", audioDuration, "ms");
    console.log("  Target: ≤ 1500ms");
    console.log("  Status:", audioDuration <= 1500 ? "✓ PASSED" : "✗ FAILED");
    console.log("");

    console.log("2. Testing video generation performance...");
    const videoStartTime = Date.now();
    const videoResponse = await axios.post(`${baseUrl}/generate_lipsync`, {
      audio_url: audioResponse.data.audioUrl,
      avatar: "test_avatar.png",
    });
    const videoEndTime = Date.now();
    const videoDuration = videoEndTime - videoStartTime;

    console.log("✓ Video generation completed");
    console.log("  Video URL:", videoResponse.data.videoUrl);
    console.log("  Duration:", videoResponse.data.duration, "seconds");
    console.log("  Response time:", videoDuration, "ms");
    console.log("  Target: ≤ 3000ms");
    console.log("  Status:", videoDuration <= 3000 ? "✓ PASSED" : "✗ FAILED");
    console.log("");

    console.log("3. Testing end-to-end pipeline performance...");
    let totalPipelineTime = 0;
    let passedTests = 0;
    let failedTests = 0;

    for (const product of testProducts) {
      console.log(`\n  Testing product: ${product}`);
      const pipelineStartTime = Date.now();

      const pipelineResponse = await axios.post(
        `${baseUrl}/generate_ad_content`,
        {
          product: product,
        }
      );

      const pipelineEndTime = Date.now();
      const pipelineDuration = pipelineEndTime - pipelineStartTime;
      totalPipelineTime += pipelineDuration;

      console.log("    Response time:", pipelineDuration, "ms");
      console.log("    Target: ≤ 5000ms");

      if (pipelineDuration <= 5000) {
        console.log("    Status: ✓ PASSED");
        passedTests++;
      } else {
        console.log("    Status: ✗ FAILED");
        failedTests++;
      }
    }

    const averagePipelineTime = totalPipelineTime / testProducts.length;
    console.log(`\n3. End-to-end pipeline summary:`);
    console.log(`  Average response time: ${averagePipelineTime.toFixed(2)}ms`);
    console.log(`  Passed tests: ${passedTests}/${testProducts.length}`);
    console.log(`  Failed tests: ${failedTests}/${testProducts.length}`);
    console.log(
      `  Overall status: ${failedTests === 0 ? "✓ PASSED" : "✗ FAILED"}`
    );
    console.log("");

    console.log("4. Testing streaming pipeline performance...");
    let totalStreamingTime = 0;
    let passedStreamingTests = 0;
    let failedStreamingTests = 0;

    for (const product of testProducts) {
      console.log(`\n  Testing streaming for product: ${product}`);
      const streamingStartTime = Date.now();

      const streamingResponse = await axios.post(
        `${baseUrl}/generate_ad_content_streaming`,
        {
          product: product,
        }
      );

      const streamingEndTime = Date.now();
      const streamingDuration = streamingEndTime - streamingStartTime;
      totalStreamingTime += streamingDuration;

      console.log("    Response time:", streamingDuration, "ms");
      console.log("    Target: ≤ 5000ms");

      if (streamingDuration <= 5000) {
        console.log("    Status: ✓ PASSED");
        passedStreamingTests++;
      } else {
        console.log("    Status: ✗ FAILED");
        failedStreamingTests++;
      }
    }

    const averageStreamingTime = totalStreamingTime / testProducts.length;
    console.log(`\n4. Streaming pipeline summary:`);
    console.log(
      `  Average response time: ${averageStreamingTime.toFixed(2)}ms`
    );
    console.log(
      `  Passed tests: ${passedStreamingTests}/${testProducts.length}`
    );
    console.log(
      `  Failed tests: ${failedStreamingTests}/${testProducts.length}`
    );
    console.log(
      `  Overall status: ${
        failedStreamingTests === 0 ? "✓ PASSED" : "✗ FAILED"
      }`
    );
    console.log("");

    console.log("5. Performance comparison:");
    console.log(
      `  Regular pipeline average: ${averagePipelineTime.toFixed(2)}ms`
    );
    console.log(
      `  Streaming pipeline average: ${averageStreamingTime.toFixed(2)}ms`
    );

    if (averageStreamingTime < averagePipelineTime) {
      console.log("  ✓ Streaming optimization shows improved performance");
    } else {
      console.log(
        "  Note: Performance may vary based on system load and mock implementations"
      );
    }
    console.log("");

    console.log("6. Testing performance metrics collection...");
    // Get performance summary
    try {
      const metricsResponse = await axios.get(
        "http://127.0.0.1:3000/api/v1/performance"
      );
      console.log("✓ Performance metrics endpoint accessible");
      console.log(
        "  Total requests tracked:",
        metricsResponse.data.summary.totalRequests
      );
      console.log(
        "  Successful requests:",
        metricsResponse.data.summary.successfulRequests
      );
      console.log(
        "  Failed requests:",
        metricsResponse.data.summary.failedRequests
      );
    } catch (error) {
      console.log("✗ Performance metrics endpoint error:", error.message);
    }

    console.log("\nAll performance optimization tests completed!");
  } catch (error) {
    console.error("Test failed:", error.response?.data || error.message);
  }
}

// Run the test
testPerformanceOptimization();
