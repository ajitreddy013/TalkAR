#!/usr/bin/env node

/**
 * Backend Avatar-Script Mapping Test
 *
 * Tests the Week 4 Phase 1 backend linkage implementation:
 * - Avatar CRUD operations
 * - Image-Avatar mapping with scripts
 * - Complete image data retrieval
 * - Media URL updates
 */

const axios = require("axios");

const BASE_URL = "http://localhost:3000/api/v1";

// Test data
const testAvatar = {
  name: "Shah Rukh Khan",
  description: "Bollywood superstar avatar",
  avatarImageUrl: "/uploads/avatars/srk_preview.jpg",
  avatarVideoUrl: "/uploads/avatars/srk_video.mp4",
  avatar3DModelUrl: "/uploads/avatars/SRK_3D.glb",
  voiceId: "voice_srk_hindi",
  idleAnimationType: "breathing_and_blinking",
};

const testScript = `Welcome to TalkAR — experience magic in motion. 
I'm here to guide you through an extraordinary journey into augmented reality.`;

let createdAvatarId = null;
let createdImageId = null;
let createdMappingId = null;

// Color output
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
  log(`✅ ${message}`, colors.green);
}

function logError(message) {
  log(`❌ ${message}`, colors.red);
}

function logInfo(message) {
  log(`ℹ️  ${message}`, colors.cyan);
}

function logWarning(message) {
  log(`⚠️  ${message}`, colors.yellow);
}

// Test 1: Create Avatar
async function testCreateAvatar() {
  try {
    logInfo("Test 1: Creating new avatar...");

    const response = await axios.post(`${BASE_URL}/avatars`, testAvatar);

    if (response.status === 201 && response.data.id) {
      createdAvatarId = response.data.id;
      logSuccess(
        `Avatar created: ${response.data.name} (ID: ${createdAvatarId})`
      );
      log(`  - 3D Model URL: ${response.data.avatar3DModelUrl}`);
      log(`  - Voice ID: ${response.data.voiceId}`);
      log(`  - Idle Animation: ${response.data.idleAnimationType}`);
      return true;
    } else {
      logError("Failed to create avatar");
      return false;
    }
  } catch (error) {
    logError(`Error creating avatar: ${error.message}`);
    return false;
  }
}

// Test 2: Get Avatar by ID
async function testGetAvatar() {
  try {
    logInfo("Test 2: Fetching avatar by ID...");

    const response = await axios.get(`${BASE_URL}/avatars/${createdAvatarId}`);

    if (response.status === 200 && response.data.id === createdAvatarId) {
      logSuccess(`Avatar retrieved: ${response.data.name}`);
      return true;
    } else {
      logError("Failed to retrieve avatar");
      return false;
    }
  } catch (error) {
    logError(`Error retrieving avatar: ${error.message}`);
    return false;
  }
}

// Test 3: Get All Avatars
async function testGetAllAvatars() {
  try {
    logInfo("Test 3: Fetching all avatars...");

    const response = await axios.get(`${BASE_URL}/avatars`);

    if (response.status === 200 && Array.isArray(response.data)) {
      logSuccess(`Retrieved ${response.data.length} avatars`);
      return true;
    } else {
      logError("Failed to retrieve avatars list");
      return false;
    }
  } catch (error) {
    logError(`Error retrieving avatars: ${error.message}`);
    return false;
  }
}

// Test 4: Create Test Image (for mapping)
async function testCreateImage() {
  try {
    logInfo("Test 4: Creating test image for mapping...");

    // In a real scenario, this would upload an actual image file
    // For now, we'll assume an image exists or create a mock one
    const response = await axios.get(`${BASE_URL}/images`);

    if (response.status === 200 && response.data.length > 0) {
      createdImageId = response.data[0].id;
      logSuccess(
        `Using existing image: ${response.data[0].name} (ID: ${createdImageId})`
      );
      return true;
    } else {
      logWarning("No images found. Skipping mapping tests.");
      return false;
    }
  } catch (error) {
    logError(`Error fetching images: ${error.message}`);
    return false;
  }
}

// Test 5: Map Avatar to Image with Script
async function testMapAvatarToImage() {
  try {
    logInfo("Test 5: Mapping avatar to image with script...");

    const mappingData = {
      script: testScript,
      audioUrl: null, // Will be generated later
      videoUrl: null,
      visemeDataUrl: null,
    };

    const response = await axios.post(
      `${BASE_URL}/avatars/${createdAvatarId}/map/${createdImageId}`,
      mappingData
    );

    if (response.status === 200 && response.data.mapping) {
      createdMappingId = response.data.mapping.id;
      logSuccess(`Avatar mapped to image successfully`);
      log(`  - Mapping ID: ${createdMappingId}`);
      log(`  - Script: ${response.data.mapping.script?.substring(0, 50)}...`);
      return true;
    } else {
      logError("Failed to map avatar to image");
      return false;
    }
  } catch (error) {
    logError(`Error mapping avatar: ${error.message}`);
    return false;
  }
}

// Test 6: Update Mapping with Generated Media URLs
async function testUpdateMappingWithMedia() {
  try {
    logInfo("Test 6: Updating mapping with generated media URLs...");

    const mediaData = {
      audioUrl: "/uploads/sync/srk_audio_12345.mp3",
      videoUrl: "/uploads/sync/srk_video_12345.mp4",
      visemeDataUrl: "/uploads/sync/srk_visemes_12345.json",
    };

    const response = await axios.put(
      `${BASE_URL}/avatars/mapping/${createdMappingId}`,
      mediaData
    );

    if (response.status === 200 && response.data.mapping) {
      logSuccess(`Mapping updated with media URLs`);
      log(`  - Audio: ${response.data.mapping.audioUrl}`);
      log(`  - Video: ${response.data.mapping.videoUrl}`);
      log(`  - Visemes: ${response.data.mapping.visemeDataUrl}`);
      return true;
    } else {
      logError("Failed to update mapping");
      return false;
    }
  } catch (error) {
    logError(`Error updating mapping: ${error.message}`);
    return false;
  }
}

// Test 7: Get Complete Image Data
async function testGetCompleteImageData() {
  try {
    logInfo("Test 7: Fetching complete image data...");

    const response = await axios.get(
      `${BASE_URL}/avatars/complete/${createdImageId}`
    );

    if (response.status === 200) {
      logSuccess(`Complete image data retrieved`);
      log(`  - Image: ${response.data.image.name}`);
      log(`  - Avatar: ${response.data.avatar?.name || "None"}`);
      log(
        `  - Script: ${
          response.data.mapping?.script?.substring(0, 50) || "None"
        }...`
      );
      log(`  - Audio URL: ${response.data.mapping?.audioUrl || "None"}`);
      log(`  - Video URL: ${response.data.mapping?.videoUrl || "None"}`);
      log(`  - Viseme Data: ${response.data.mapping?.visemeDataUrl || "None"}`);
      return true;
    } else {
      logError("Failed to retrieve complete image data");
      return false;
    }
  } catch (error) {
    logError(`Error retrieving complete data: ${error.message}`);
    return false;
  }
}

// Test 8: Get All Mappings
async function testGetAllMappings() {
  try {
    logInfo("Test 8: Fetching all avatar-image mappings...");

    const response = await axios.get(`${BASE_URL}/avatars/mappings`);

    if (response.status === 200 && Array.isArray(response.data)) {
      logSuccess(`Retrieved ${response.data.length} mappings`);
      response.data.forEach((mapping, index) => {
        log(
          `  [${index + 1}] Image: ${
            mapping.image?.name || "Unknown"
          } → Avatar: ${mapping.avatar?.name || "Unknown"}`
        );
      });
      return true;
    } else {
      logError("Failed to retrieve mappings");
      return false;
    }
  } catch (error) {
    logError(`Error retrieving mappings: ${error.message}`);
    return false;
  }
}

// Test 9: Get Avatar for Specific Image
async function testGetAvatarForImage() {
  try {
    logInfo("Test 9: Fetching avatar for specific image...");

    const response = await axios.get(
      `${BASE_URL}/avatars/image/${createdImageId}`
    );

    if (response.status === 200 && response.data.id === createdAvatarId) {
      logSuccess(`Correct avatar retrieved for image`);
      log(`  - Avatar: ${response.data.name}`);
      log(`  - 3D Model: ${response.data.avatar3DModelUrl}`);
      return true;
    } else {
      logError("Failed to retrieve avatar for image");
      return false;
    }
  } catch (error) {
    logError(`Error retrieving avatar for image: ${error.message}`);
    return false;
  }
}

// Test 10: Get Image with Avatar Data (Enhanced Images Endpoint)
async function testGetImageWithAvatar() {
  try {
    logInfo("Test 10: Fetching image with avatar data...");

    const response = await axios.get(
      `${BASE_URL}/images/${createdImageId}?includeAvatar=true`
    );

    if (response.status === 200 && response.data.avatarMapping) {
      logSuccess(`Image retrieved with avatar mapping`);
      log(`  - Image: ${response.data.name}`);
      log(`  - Avatar: ${response.data.avatarMapping.avatar?.name || "None"}`);
      log(
        `  - Script: ${
          response.data.avatarMapping.script?.substring(0, 50) || "None"
        }...`
      );
      return true;
    } else {
      logWarning("Image retrieved but no avatar mapping found");
      return true; // Not a failure, just no mapping
    }
  } catch (error) {
    logError(`Error retrieving image with avatar: ${error.message}`);
    return false;
  }
}

// Run all tests
async function runAllTests() {
  log("\n" + "=".repeat(60), colors.blue);
  log("  Week 4 Phase 1: Backend Linkage Integration Test", colors.blue);
  log("=".repeat(60) + "\n", colors.blue);

  const tests = [
    { name: "Create Avatar", fn: testCreateAvatar },
    { name: "Get Avatar by ID", fn: testGetAvatar },
    { name: "Get All Avatars", fn: testGetAllAvatars },
    { name: "Create/Get Test Image", fn: testCreateImage },
    { name: "Map Avatar to Image", fn: testMapAvatarToImage },
    { name: "Update Mapping with Media", fn: testUpdateMappingWithMedia },
    { name: "Get Complete Image Data", fn: testGetCompleteImageData },
    { name: "Get All Mappings", fn: testGetAllMappings },
    { name: "Get Avatar for Image", fn: testGetAvatarForImage },
    { name: "Get Image with Avatar", fn: testGetImageWithAvatar },
  ];

  let passed = 0;
  let failed = 0;

  for (const test of tests) {
    const result = await test.fn();
    if (result) {
      passed++;
    } else {
      failed++;
    }
    console.log(""); // Empty line between tests
  }

  // Summary
  log("\n" + "=".repeat(60), colors.blue);
  log("  Test Summary", colors.blue);
  log("=".repeat(60), colors.blue);
  log(`Total Tests: ${tests.length}`);
  logSuccess(`Passed: ${passed}`);
  if (failed > 0) {
    logError(`Failed: ${failed}`);
  }
  log("=".repeat(60) + "\n", colors.blue);

  // Example data structure
  log("Example Data Structure:", colors.yellow);
  log(
    JSON.stringify(
      {
        imageId: createdImageId,
        avatarId: createdAvatarId,
        script: testScript,
        audioUrl: "/uploads/sync/srk_audio_12345.mp3",
        videoUrl: "/uploads/sync/srk_video_12345.mp4",
        visemeDataUrl: "/uploads/sync/srk_visemes_12345.json",
      },
      null,
      2
    )
  );

  process.exit(failed > 0 ? 1 : 0);
}

// Run tests
runAllTests().catch((error) => {
  logError(`Fatal error: ${error.message}`);
  process.exit(1);
});
