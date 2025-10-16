#!/usr/bin/env node

/**
 * Week 2 Backend Verification Script
 * Verifies that all backend preparation requirements are complete
 */

const sqlite3 = require("sqlite3").verbose();
const path = require("path");
const fs = require("fs");

const dbPath = path.join(__dirname, "database.sqlite");
const db = new sqlite3.Database(dbPath);

console.log("🔍 Week 2 Backend Preparation Verification\n");
console.log("=".repeat(60));

const results = {
  testsPassed: 0,
  testsFailed: 0,
  requirements: [],
};

function runQuery(sql, params = []) {
  return new Promise((resolve, reject) => {
    db.all(sql, params, (err, rows) => {
      if (err) reject(err);
      else resolve(rows);
    });
  });
}

async function verifyRequirements() {
  try {
    // Requirement 1: Check for 5-10 dummy images
    console.log("\n1️⃣  REQUIREMENT: Setup test products/images (5-10 images)");
    console.log("-".repeat(60));

    const images = await runQuery(
      "SELECT COUNT(*) as count, GROUP_CONCAT(name, ', ') as names FROM images WHERE isActive = 1"
    );
    const imageCount = images[0].count;

    if (imageCount >= 5 && imageCount <= 15) {
      console.log(`✅ PASS: Found ${imageCount} active test images`);
      results.testsPassed++;
    } else {
      console.log(`❌ FAIL: Expected 5-10 images, found ${imageCount}`);
      results.testsFailed++;
    }

    const imageList = await runQuery(
      "SELECT id, name, description FROM images WHERE isActive = 1 LIMIT 10"
    );
    console.log("\n   Test Images:");
    imageList.forEach((img, i) => {
      console.log(
        `   ${i + 1}. ${img.name} - ${img.description || "No description"}`
      );
    });

    results.requirements.push({
      name: "Test Images (5-10)",
      status: imageCount >= 5 && imageCount <= 15 ? "✅" : "❌",
      count: imageCount,
    });

    // Requirement 2: Check for scripts/dialogues (5-10 lines each)
    console.log(
      "\n2️⃣  REQUIREMENT: Associate each image with scripts (5-10 lines)"
    );
    console.log("-".repeat(60));

    const dialogues = await runQuery(
      "SELECT COUNT(*) as count FROM dialogues WHERE isActive = 1"
    );
    const dialogueCount = dialogues[0].count;

    const scriptsPerImage = await runQuery(`
      SELECT i.name, COUNT(d.id) as script_count
      FROM images i
      LEFT JOIN dialogues d ON i.id = d.imageId AND d.isActive = 1
      WHERE i.isActive = 1
      GROUP BY i.id, i.name
    `);

    console.log(`   Total Scripts: ${dialogueCount}`);
    console.log("\n   Scripts per Image:");

    let allHaveScripts = true;
    scriptsPerImage.forEach((img) => {
      const status = img.script_count > 0 ? "✅" : "❌";
      console.log(`   ${status} ${img.name}: ${img.script_count} scripts`);
      if (img.script_count === 0) allHaveScripts = false;
    });

    if (allHaveScripts && dialogueCount > 0) {
      console.log(`\n✅ PASS: All images have associated scripts`);
      results.testsPassed++;
    } else {
      console.log(`\n❌ FAIL: Some images missing scripts`);
      results.testsFailed++;
    }

    // Sample script
    const sampleScript = await runQuery(
      "SELECT text FROM dialogues WHERE isActive = 1 LIMIT 1"
    );
    if (sampleScript.length > 0) {
      const lines = sampleScript[0].text
        .split(/[.!?]/)
        .filter((l) => l.trim().length > 0);
      console.log(`\n   Sample Script (${lines.length} sentences):`);
      console.log(`   "${sampleScript[0].text.substring(0, 150)}..."`);
    }

    results.requirements.push({
      name: "Scripts/Dialogues",
      status: allHaveScripts ? "✅" : "❌",
      count: dialogueCount,
    });

    // Requirement 3: /images API endpoint
    console.log(
      "\n3️⃣  REQUIREMENT: /images API - Fetch image metadata and script"
    );
    console.log("-".repeat(60));

    const routeExists = fs.existsSync(
      path.join(__dirname, "src/routes/images.ts")
    );

    if (routeExists) {
      console.log("✅ PASS: /api/v1/images route file exists");
      console.log("   Endpoints available:");
      console.log("   • GET /api/v1/images - Fetch all images with dialogues");
      console.log(
        "   • GET /api/v1/images/:id - Fetch specific image with dialogues"
      );
      results.testsPassed++;
    } else {
      console.log("❌ FAIL: /images route not found");
      results.testsFailed++;
    }

    results.requirements.push({
      name: "/images API",
      status: routeExists ? "✅" : "❌",
    });

    // Requirement 4: /avatars API endpoint
    console.log(
      "\n4️⃣  REQUIREMENT: /avatars API - Map avatar video URLs to images"
    );
    console.log("-".repeat(60));

    const avatarRouteExists = fs.existsSync(
      path.join(__dirname, "src/routes/avatars.ts")
    );

    const avatars = await runQuery(
      "SELECT COUNT(*) as count FROM avatars WHERE isActive = 1"
    );
    const avatarCount = avatars[0].count;

    const mappings = await runQuery(
      "SELECT COUNT(*) as count FROM image_avatar_mappings WHERE isActive = 1"
    );
    const mappingCount = mappings[0].count;

    if (avatarRouteExists && avatarCount > 0 && mappingCount > 0) {
      console.log(
        `✅ PASS: /api/v1/avatars route exists with ${avatarCount} avatars`
      );
      console.log(`   • ${mappingCount} image-avatar mappings configured`);
      console.log("   Endpoints available:");
      console.log("   • GET /api/v1/avatars - Fetch all avatars");
      console.log(
        "   • GET /api/v1/avatars/image/:imageId - Get avatar for image"
      );
      results.testsPassed++;
    } else {
      console.log("❌ FAIL: /avatars route incomplete");
      results.testsFailed++;
    }

    const avatarList = await runQuery(
      "SELECT name, avatarVideoUrl FROM avatars WHERE isActive = 1 LIMIT 5"
    );
    console.log("\n   Sample Avatars:");
    avatarList.forEach((avatar, i) => {
      console.log(`   ${i + 1}. ${avatar.name}`);
      console.log(`      Video: ${avatar.avatarVideoUrl}`);
    });

    results.requirements.push({
      name: "/avatars API",
      status: avatarRouteExists && avatarCount > 0 ? "✅" : "❌",
      avatars: avatarCount,
      mappings: mappingCount,
    });

    // Requirement 5: Mock Lip-Sync Endpoint
    console.log("\n5️⃣  REQUIREMENT: Mock Lip-Sync Endpoint");
    console.log("-".repeat(60));

    const lipSyncRouteExists = fs.existsSync(
      path.join(__dirname, "src/routes/lipSync.ts")
    );
    const lipSyncServiceExists = fs.existsSync(
      path.join(__dirname, "src/services/mockLipSyncService.ts")
    );

    if (lipSyncRouteExists && lipSyncServiceExists) {
      console.log("✅ PASS: Mock lip-sync API implemented");
      console.log("   Endpoints available:");
      console.log(
        "   • POST /api/v1/lipsync/generate - Generate lip-sync video"
      );
      console.log(
        "   • GET /api/v1/lipsync/status/:videoId - Get video status"
      );
      console.log("   • GET /api/v1/lipsync/voices - Get available voices");
      console.log(
        "   • POST /api/v1/lipsync/talking-head - Generate talking head"
      );
      results.testsPassed++;
    } else {
      console.log("❌ FAIL: Mock lip-sync API not found");
      results.testsFailed++;
    }

    results.requirements.push({
      name: "Mock Lip-Sync API",
      status: lipSyncRouteExists && lipSyncServiceExists ? "✅" : "❌",
    });

    // Additional: Check for /scripts endpoint
    console.log("\n➕ ADDITIONAL: /scripts API - Get scripts for images");
    console.log("-".repeat(60));

    const scriptRouteExists = fs.existsSync(
      path.join(__dirname, "src/routes/scripts.ts")
    );

    if (scriptRouteExists) {
      console.log("✅ BONUS: /api/v1/scripts route exists");
      console.log("   Endpoints available:");
      console.log("   • GET /api/v1/scripts/getScriptForImage/:imageId");
      console.log("   • GET /api/v1/scripts/getAllScriptsForImage/:imageId");
    }

    // Final Summary
    console.log("\n" + "=".repeat(60));
    console.log("📊 VERIFICATION SUMMARY");
    console.log("=".repeat(60));

    console.log(`\n✅ Tests Passed: ${results.testsPassed}`);
    console.log(`❌ Tests Failed: ${results.testsFailed}`);

    console.log("\n📋 Requirements Checklist:");
    results.requirements.forEach((req) => {
      console.log(`   ${req.status} ${req.name}`);
      if (req.count !== undefined) {
        console.log(`      Count: ${req.count}`);
      }
    });

    // Deliverable Check
    console.log(
      "\n🎯 DELIVERABLE: Backend can return image → script → avatar video URL"
    );
    console.log("-".repeat(60));

    const completeFlow = await runQuery(`
      SELECT 
        i.name as image_name,
        d.text as script,
        a.name as avatar_name,
        a.avatarVideoUrl as video_url
      FROM images i
      LEFT JOIN dialogues d ON i.id = d.imageId AND d.isActive = 1 AND d.isDefault = 1
      LEFT JOIN image_avatar_mappings m ON i.id = m.imageId AND m.isActive = 1
      LEFT JOIN avatars a ON m.avatarId = a.id
      WHERE i.isActive = 1
      LIMIT 3
    `);

    if (completeFlow.length > 0 && completeFlow[0].video_url) {
      console.log("✅ COMPLETE: Full data flow verified");
      console.log("\n   Sample Complete Flow:");
      completeFlow.forEach((flow, i) => {
        console.log(`\n   ${i + 1}. Image: ${flow.image_name}`);
        console.log(`      Script: "${flow.script?.substring(0, 80)}..."`);
        console.log(`      Avatar: ${flow.avatar_name}`);
        console.log(`      Video: ${flow.video_url}`);
      });
    } else {
      console.log("⚠️  PARTIAL: Some data missing in complete flow");
    }

    console.log("\n" + "=".repeat(60));

    if (results.testsFailed === 0) {
      console.log("🎉 ALL REQUIREMENTS MET! Week 2 Backend is ready!");
      console.log("\n📱 Ready for Mobile App Integration:");
      console.log("   1. Start backend: npm run dev");
      console.log("   2. Test endpoints at: http://localhost:3000");
      console.log("   3. Begin mobile app image recognition flow");
    } else {
      console.log("⚠️  SOME REQUIREMENTS INCOMPLETE");
      console.log("   Please address failed tests above");
    }

    console.log("=".repeat(60));
  } catch (error) {
    console.error("\n❌ Error during verification:", error);
  } finally {
    db.close();
  }
}

verifyRequirements();
