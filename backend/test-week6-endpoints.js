const axios = require("axios");
const { v4: uuidv4 } = require("uuid");

const BASE_URL = "http://localhost:3000/api/v1/ai-pipeline";

// Color output for better readability
const colors = {
  reset: "\x1b[0m",
  green: "\x1b[32m",
  red: "\x1b[31m",
  yellow: "\x1b[33m",
  blue: "\x1b[34m",
  cyan: "\x1b[36m",
};

function log(message, color = colors.reset) {
  console.log(`${color}${message}${colors.reset}`);
}

function logSuccess(message) {
  log(`✓ ${message}`, colors.green);
}

function logError(message) {
  log(`✗ ${message}`, colors.red);
}

function logInfo(message) {
  log(`ℹ ${message}`, colors.cyan);
}

function logHeader(message) {
  console.log("\n" + "=".repeat(80));
  log(message, colors.blue);
  console.log("=".repeat(80));
}

async function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

/**
 * Test 1: Generate Script Endpoint
 */
async function testGenerateScript() {
  logHeader("TEST 1: Generate Script Endpoint");

  try {
    const response = await axios.post(`${BASE_URL}/generate_script`, {
      imageId: "test-image-123",
      language: "en",
      emotion: "happy",
      productName: "Pepsi",
    });

    if (response.data.success && response.data.script) {
      logSuccess("Generate script endpoint works correctly");
      log(`Script: ${response.data.script}`, colors.yellow);
      return true;
    } else {
      logError("Generate script endpoint returned invalid response");
      return false;
    }
  } catch (error) {
    logError(`Generate script test failed: ${error.message}`);
    if (error.response) {
      console.log("Response:", error.response.data);
    }
    return false;
  }
}

/**
 * Test 2: Generate Audio Endpoint (TTS)
 */
async function testGenerateAudio() {
  logHeader("TEST 2: Generate Audio Endpoint (TTS)");

  try {
    const testText = "Refresh your day with Pepsi — bold taste, cool vibes!";
    const response = await axios.post(`${BASE_URL}/generate_audio`, {
      text: testText,
      language: "en",
      emotion: "happy",
    });

    if (response.data.success && response.data.audioUrl) {
      logSuccess("Generate audio endpoint works correctly");
      log(`Audio URL: ${response.data.audioUrl}`, colors.yellow);
      log(`Duration: ${response.data.duration}s`, colors.yellow);
      return { success: true, audioUrl: response.data.audioUrl };
    } else {
      logError("Generate audio endpoint returned invalid response");
      return { success: false, audioUrl: null };
    }
  } catch (error) {
    logError(`Generate audio test failed: ${error.message}`);
    if (error.response) {
      console.log("Response:", error.response.data);
    }
    return { success: false, audioUrl: null };
  }
}

/**
 * Test 3: Generate Lip-Sync Endpoint
 */
async function testGenerateLipSync(audioUrl) {
  logHeader("TEST 3: Generate Lip-Sync Endpoint");

  try {
    const response = await axios.post(`${BASE_URL}/generate_lipsync`, {
      imageId: "test-image-123",
      audio_url: audioUrl || "https://example.com/audio.mp3",
      emotion: "happy",
    });

    if (response.data.success && response.data.videoUrl) {
      logSuccess("Generate lip-sync endpoint works correctly");
      log(`Video URL: ${response.data.videoUrl}`, colors.yellow);
      log(`Duration: ${response.data.duration}s`, colors.yellow);
      if (response.data.jobId) {
        log(`Job ID: ${response.data.jobId}`, colors.yellow);
      }
      return { success: true, videoUrl: response.data.videoUrl };
    } else {
      logError("Generate lip-sync endpoint returned invalid response");
      return { success: false, videoUrl: null };
    }
  } catch (error) {
    logError(`Generate lip-sync test failed: ${error.message}`);
    if (error.response) {
      console.log("Response:", error.response.data);
    }
    return { success: false, videoUrl: null };
  }
}

/**
 * Test 4: Generate Complete Ad Content (Full Pipeline)
 */
async function testGenerateAdContent() {
  logHeader("TEST 4: Generate Complete Ad Content (Full Pipeline)");

  const testProducts = [
    "Pepsi",
    "iPhone 15 Pro",
    "Tesla Model 3",
    "Nike Running Shoes",
  ];

  let successCount = 0;

  for (const product of testProducts) {
    try {
      logInfo(`Testing with product: ${product}`);
      const response = await axios.post(`${BASE_URL}/generate_ad_content`, {
        product: product,
      });

      if (response.data.success) {
        logSuccess(`Ad content generated for ${product}`);
        log(
          `Script: ${response.data.script?.substring(0, 80)}...`,
          colors.yellow
        );
        log(`Audio URL: ${response.data.audio_url}`, colors.yellow);
        log(`Video URL: ${response.data.video_url}`, colors.yellow);
        successCount++;
      } else {
        logError(`Failed to generate ad content for ${product}`);
      }
    } catch (error) {
      logError(`Error generating ad content for ${product}: ${error.message}`);
      if (error.response) {
        console.log("Response:", error.response.data);
      }
    }
  }

  return successCount === testProducts.length;
}

/**
 * Test 5: Error Handling - Missing Parameters
 */
async function testErrorHandling() {
  logHeader("TEST 5: Error Handling - Missing Parameters");

  let allTestsPassed = true;

  // Test 1: Missing product parameter
  try {
    await axios.post(`${BASE_URL}/generate_ad_content`, {});
    logError("Test failed: Should have rejected missing product parameter");
    allTestsPassed = false;
  } catch (error) {
    if (error.response && error.response.status === 400) {
      logSuccess("Correctly rejected missing product parameter");
      log(`Error message: ${error.response.data.error}`, colors.yellow);
    } else {
      logError("Unexpected error response");
      allTestsPassed = false;
    }
  }

  // Test 2: Empty product name
  try {
    await axios.post(`${BASE_URL}/generate_ad_content`, {
      product: "",
    });
    logError("Test failed: Should have rejected empty product name");
    allTestsPassed = false;
  } catch (error) {
    if (error.response && error.response.status === 400) {
      logSuccess("Correctly rejected empty product name");
      log(`Error message: ${error.response.data.error}`, colors.yellow);
    } else {
      logError("Unexpected error response");
      allTestsPassed = false;
    }
  }

  // Test 3: Product name too long
  try {
    await axios.post(`${BASE_URL}/generate_ad_content`, {
      product: "A".repeat(150),
    });
    logError("Test failed: Should have rejected long product name");
    allTestsPassed = false;
  } catch (error) {
    if (error.response && error.response.status === 400) {
      logSuccess("Correctly rejected long product name");
      log(`Error message: ${error.response.data.error}`, colors.yellow);
    } else {
      logError("Unexpected error response");
      allTestsPassed = false;
    }
  }

  return allTestsPassed;
}

/**
 * Test 6: Streaming Endpoint
 */
async function testStreamingEndpoint() {
  logHeader("TEST 6: Streaming Ad Content Endpoint");

  try {
    const response = await axios.post(
      `${BASE_URL}/generate_ad_content_streaming`,
      {
        product: "Premium Coffee Maker",
      }
    );

    if (response.data.success) {
      logSuccess("Streaming ad content endpoint works correctly");
      log(
        `Script: ${response.data.script?.substring(0, 80)}...`,
        colors.yellow
      );
      log(`Audio URL: ${response.data.audio_url}`, colors.yellow);
      log(`Video URL: ${response.data.video_url}`, colors.yellow);
      return true;
    } else {
      logError("Streaming ad content endpoint returned invalid response");
      return false;
    }
  } catch (error) {
    logError(`Streaming endpoint test failed: ${error.message}`);
    if (error.response) {
      console.log("Response:", error.response.data);
    }
    return false;
  }
}

/**
 * Test 7: Sequential Pipeline Test
 */
async function testSequentialPipeline() {
  logHeader("TEST 7: Sequential Pipeline Test");

  try {
    // Step 1: Generate script
    logInfo("Step 1: Generating script for 'Nike Running Shoes'");
    const scriptResponse = await axios.post(`${BASE_URL}/generate_script`, {
      imageId: "test-nike-shoes",
      language: "en",
      emotion: "happy",
      productName: "Nike Running Shoes",
    });

    if (!scriptResponse.data.success || !scriptResponse.data.script) {
      logError("Failed to generate script");
      return false;
    }

    const script = scriptResponse.data.script;
    logSuccess(`Script generated: ${script}`);

    // Step 2: Generate audio
    logInfo("Step 2: Converting script to audio");
    const audioResponse = await axios.post(`${BASE_URL}/generate_audio`, {
      text: script,
      language: "en",
      emotion: "happy",
    });

    if (!audioResponse.data.success || !audioResponse.data.audioUrl) {
      logError("Failed to generate audio");
      return false;
    }

    logSuccess(`Audio generated: ${audioResponse.data.audioUrl}`);

    // Step 3: Generate lip-sync
    logInfo("Step 3: Generating lip-sync video");
    const lipsyncResponse = await axios.post(`${BASE_URL}/generate_lipsync`, {
      imageId: "test-nike-shoes",
      audio_url: audioResponse.data.audioUrl,
      emotion: "happy",
    });

    if (!lipsyncResponse.data.success || !lipsyncResponse.data.videoUrl) {
      logError("Failed to generate lip-sync video");
      return false;
    }

    logSuccess(`Lip-sync video generated: ${lipsyncResponse.data.videoUrl}`);

    logSuccess("Sequential pipeline test completed successfully!");
    return true;
  } catch (error) {
    logError(`Sequential pipeline test failed: ${error.message}`);
    if (error.response) {
      console.log("Response:", error.response.data);
    }
    return false;
  }
}

/**
 * Main test runner
 */
async function runAllTests() {
  console.log("\n");
  log(
    "╔════════════════════════════════════════════════════════════════════╗",
    colors.blue
  );
  log(
    "║         TalkAR Week 6 - AI Pipeline Endpoint Tests              ║",
    colors.blue
  );
  log(
    "╚════════════════════════════════════════════════════════════════════╝",
    colors.blue
  );

  const results = [];

  try {
    // Test 1: Generate Script
    results.push({
      name: "Generate Script",
      passed: await testGenerateScript(),
    });

    // Test 2: Generate Audio
    const audioResult = await testGenerateAudio();
    results.push({
      name: "Generate Audio",
      passed: audioResult.success,
    });

    // Test 3: Generate Lip-Sync
    results.push({
      name: "Generate Lip-Sync",
      passed: (await testGenerateLipSync(audioResult.audioUrl)).success,
    });

    // Test 4: Complete Ad Content
    results.push({
      name: "Complete Ad Content",
      passed: await testGenerateAdContent(),
    });

    // Test 5: Error Handling
    results.push({
      name: "Error Handling",
      passed: await testErrorHandling(),
    });

    // Test 6: Streaming Endpoint
    results.push({
      name: "Streaming Endpoint",
      passed: await testStreamingEndpoint(),
    });

    // Test 7: Sequential Pipeline
    results.push({
      name: "Sequential Pipeline",
      passed: await testSequentialPipeline(),
    });

    // Print summary
    logHeader("TEST SUMMARY");

    let totalPassed = 0;
    let totalFailed = 0;

    results.forEach((result, index) => {
      if (result.passed) {
        logSuccess(`${index + 1}. ${result.name}`);
        totalPassed++;
      } else {
        logError(`${index + 1}. ${result.name}`);
        totalFailed++;
      }
    });

    console.log("\n");
    log(`Total Tests: ${results.length}`, colors.cyan);
    log(`Passed: ${totalPassed}`, colors.green);
    log(`Failed: ${totalFailed}`, colors.red);
    console.log("\n");

    if (totalFailed === 0) {
      log(
        "╔════════════════════════════════════════════════════════════════════╗",
        colors.green
      );
      log(
        "║              ALL TESTS PASSED! ✓                                    ║",
        colors.green
      );
      log(
        "╚════════════════════════════════════════════════════════════════════╝",
        colors.green
      );
    } else {
      log(
        "╔════════════════════════════════════════════════════════════════════╗",
        colors.red
      );
      log(
        "║            SOME TESTS FAILED. Please review errors above.          ║",
        colors.red
      );
      log(
        "╚════════════════════════════════════════════════════════════════════╝",
        colors.red
      );
    }
  } catch (error) {
    logError(`Fatal error during testing: ${error.message}`);
    console.error(error);
  }
}

// Run tests
runAllTests().catch((error) => {
  console.error("Test execution failed:", error);
  process.exit(1);
});
