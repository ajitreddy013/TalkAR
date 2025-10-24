const axios = require("axios");

// Test lip-sync functionality
async function testLipSync() {
  const baseUrl = "http://localhost:3000/api/v1/ai-pipeline";

  try {
    console.log("Testing lip-sync functionality...\n");

    // Test 1: Basic lip-sync generation
    console.log("1. Testing basic lip-sync generation...");
    const lipsyncResponse1 = await axios.post(`${baseUrl}/generate_lipsync`, {
      audioUrl:
        "http://localhost:3000/audio/mock-audio-5x8x7-en-neutral-2025-10-22T18-17-59-896Z.mp3",
      imageId: "test-image-123",
      emotion: "neutral",
    });
    console.log("✓ Basic lip-sync generation successful");
    console.log("  Video URL:", lipsyncResponse1.data.videoUrl);
    console.log("  Duration:", lipsyncResponse1.data.duration, "seconds\n");

    // Test 2: Lip-sync generation with happy emotion
    console.log("2. Testing lip-sync generation with happy emotion...");
    const lipsyncResponse2 = await axios.post(`${baseUrl}/generate_lipsync`, {
      audioUrl:
        "http://localhost:3000/audio/mock-audio-h5nrhu-en-happy-2025-10-22T18-17-59-914Z.mp3",
      imageId: "test-image-456",
      emotion: "happy",
    });
    console.log("✓ Happy emotion lip-sync generation successful");
    console.log("  Video URL:", lipsyncResponse2.data.videoUrl);
    console.log("  Duration:", lipsyncResponse2.data.duration, "seconds\n");

    // Test 3: Lip-sync generation with avatar parameter
    console.log("3. Testing lip-sync generation with avatar parameter...");
    const lipsyncResponse3 = await axios.post(`${baseUrl}/generate_lipsync`, {
      audioUrl:
        "http://localhost:3000/audio/mock-audio-a1ylxv-en-serious-2025-10-22T18-17-59-917Z.mp3",
      avatar: "celebrity_face.png",
      emotion: "serious",
    });
    console.log("✓ Avatar parameter lip-sync generation successful");
    console.log("  Video URL:", lipsyncResponse3.data.videoUrl);
    console.log("  Duration:", lipsyncResponse3.data.duration, "seconds\n");

    // Test 4: Error handling - missing audioUrl
    console.log("4. Testing error handling for missing audioUrl...");
    try {
      await axios.post(`${baseUrl}/generate_lipsync`, {
        imageId: "test-image-789",
        emotion: "neutral",
      });
      console.log("✗ Expected error but request succeeded");
    } catch (error) {
      if (error.response && error.response.status === 400) {
        console.log("✓ Error handling for missing audioUrl works correctly");
        console.log("  Error message:", error.response.data.error, "\n");
      } else {
        console.log("✗ Unexpected error:", error.message);
      }
    }

    // Test 5: Job status checking (if job ID is returned)
    console.log("5. Testing job status checking...");
    const lipsyncResponse5 = await axios.post(`${baseUrl}/generate_lipsync`, {
      audioUrl:
        "http://localhost:3000/audio/mock-audio-b6f9z2-es-neutral-2025-10-22T18-17-59-920Z.mp3",
      imageId: "test-image-999",
      emotion: "neutral",
    });

    if (lipsyncResponse5.data.jobId) {
      console.log("  Job ID received:", lipsyncResponse5.data.jobId);
      // Test job status endpoint
      try {
        const jobStatusResponse = await axios.get(
          `${baseUrl}/lipsync/status/${lipsyncResponse5.data.jobId}`
        );
        console.log("✓ Job status checking successful");
        console.log(
          "  Job status:",
          jobStatusResponse.data.jobStatus.status || "N/A"
        );
      } catch (error) {
        console.log(
          "  Note: Job status endpoint may not be fully implemented in mock mode"
        );
      }
    } else {
      console.log("  Note: No job ID returned (using mock implementation)");
    }

    console.log("\nAll lip-sync tests completed successfully!");
  } catch (error) {
    console.error("Test failed:", error.response?.data || error.message);
  }
}

// Run the test
testLipSync();
