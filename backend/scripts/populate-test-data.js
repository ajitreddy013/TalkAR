#!/usr/bin/env node

const { execSync } = require("child_process");
const path = require("path");

console.log("🚀 Starting Week 2 Backend Test Data Population...\n");

try {
  // Compile TypeScript
  console.log("📦 Compiling TypeScript...");
  execSync("npx tsc", { stdio: "inherit" });

  // Run the population script
  console.log("📊 Populating test data...");
  execSync("node dist/scripts/populateTestData.js", { stdio: "inherit" });

  console.log("\n✅ Week 2 Backend Test Data Population Completed!");
  console.log("\n📋 What was created:");
  console.log("   • 8 test images with historical figures");
  console.log("   • 16 dialogue scripts (2 per image)");
  console.log("   • 4 avatar profiles with voice mappings");
  console.log("   • 8 image-avatar mappings");
  console.log("   • Mock lip-sync API endpoints");

  console.log("\n🔗 Available API Endpoints:");
  console.log("   • GET /api/v1/images - Fetch all images with scripts");
  console.log("   • GET /api/v1/avatars - Fetch all avatars");
  console.log(
    "   • GET /api/v1/avatars/image/:imageId - Get avatar for specific image"
  );
  console.log(
    "   • GET /api/v1/avatars/complete/:imageId - Get complete image data"
  );
  console.log("   • POST /api/v1/lipsync/generate - Generate lip-sync video");
  console.log("   • GET /api/v1/lipsync/voices - Get available voices");
  console.log(
    "   • POST /api/v1/lipsync/talking-head - Generate talking head video"
  );

  console.log("\n🎯 Ready for Week 2 Mobile App Development!");
} catch (error) {
  console.error("❌ Error populating test data:", error.message);
  process.exit(1);
}
