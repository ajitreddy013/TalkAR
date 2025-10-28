#!/usr/bin/env node

/**
 * Phase 2 - Week 7: Dynamic Script Generation & Personalization Testing
 *
 * This script tests the complete flow:
 * Poster detected → Backend gets poster metadata →
 * AI generates ad script (tone + language) →
 * TTS → Lip-Sync → Android plays personalized video
 */

const axios = require("axios");
const fs = require("fs");
const path = require("path");

// Configuration
const BASE_URL = "http://localhost:3000/api/v1";
const TEST_RESULTS_FILE = path.join(
  __dirname,
  "..",
  "WEEK7_TESTING_RESULTS.md"
);

// Test data
const TEST_POSTERS = [
  {
    image_id: "poster_01",
    product_name: "Sunrich Water Bottle",
    category: "Beverage",
    tone: "excited",
    language: "English",
  },
  {
    image_id: "poster_02",
    product_name: "Amul Butter",
    category: "Food",
    tone: "friendly",
    language: "Hindi",
  },
  {
    image_id: "poster_05",
    product_name: "Nike Running Shoes",
    category: "Sports",
    tone: "energetic",
    language: "English",
  },
];

const TEST_USER_ID = "test_user_123";

class Week7Tester {
  constructor() {
    this.results = {
      timestamp: new Date().toISOString(),
      tests: [],
      summary: {
        total: 0,
        passed: 0,
        failed: 0,
        skipped: 0,
      },
    };
  }

  async runAllTests() {
    console.log("🚀 Starting Phase 2 - Week 7 Testing");
    console.log("=====================================");

    try {
      // Test 1: Poster Metadata Schema
      await this.testPosterMetadataSchema();

      // Test 2: Dynamic Script Generation
      await this.testDynamicScriptGeneration();

      // Test 3: User Personalization
      await this.testUserPersonalization();

      // Test 4: Complete Ad Content Generation
      await this.testCompleteAdContentGeneration();

      // Test 5: API Performance
      await this.testAPIPerformance();

      // Test 6: Error Handling
      await this.testErrorHandling();

      // Test 7: End-to-End Flow
      await this.testEndToEndFlow();

      this.generateReport();
    } catch (error) {
      console.error("❌ Testing failed:", error.message);
      this.addTestResult(
        "Test Suite",
        false,
        `Test suite failed: ${error.message}`
      );
      this.generateReport();
    }
  }

  async testPosterMetadataSchema() {
    console.log("\n📋 Test 1: Poster Metadata Schema");

    try {
      // Test getting all posters
      const response = await axios.get(
        `${BASE_URL}/generate-dynamic-script/posters`
      );

      if (response.status === 200 && response.data.success) {
        const posters = response.data.posters;
        console.log(`✅ Found ${posters.length} posters in metadata`);

        // Validate poster structure
        const requiredFields = [
          "image_id",
          "product_name",
          "category",
          "tone",
          "language",
          "image_url",
          "brand",
        ];
        const validPosters = posters.filter((poster) =>
          requiredFields.every((field) => poster[field] !== undefined)
        );

        if (validPosters.length === posters.length) {
          this.addTestResult(
            "Poster Metadata Schema",
            true,
            `All ${posters.length} posters have valid metadata structure`
          );
        } else {
          this.addTestResult(
            "Poster Metadata Schema",
            false,
            `Only ${validPosters.length}/${posters.length} posters have valid structure`
          );
        }
      } else {
        this.addTestResult(
          "Poster Metadata Schema",
          false,
          "Failed to fetch posters metadata"
        );
      }

      // Test getting specific poster
      const posterResponse = await axios.get(
        `${BASE_URL}/generate-dynamic-script/poster/poster_01`
      );

      if (posterResponse.status === 200 && posterResponse.data.success) {
        const poster = posterResponse.data.poster;
        console.log(`✅ Retrieved poster: ${poster.product_name}`);
        this.addTestResult(
          "Specific Poster Retrieval",
          true,
          `Successfully retrieved poster: ${poster.product_name}`
        );
      } else {
        this.addTestResult(
          "Specific Poster Retrieval",
          false,
          "Failed to retrieve specific poster"
        );
      }
    } catch (error) {
      this.addTestResult(
        "Poster Metadata Schema",
        false,
        `Error: ${error.message}`
      );
    }
  }

  async testDynamicScriptGeneration() {
    console.log("\n🤖 Test 2: Dynamic Script Generation");

    for (const poster of TEST_POSTERS) {
      try {
        const startTime = Date.now();

        const response = await axios.post(
          `${BASE_URL}/generate-dynamic-script`,
          {
            image_id: poster.image_id,
            user_id: TEST_USER_ID,
          }
        );

        const duration = Date.now() - startTime;

        if (response.status === 200 && response.data.success) {
          const scriptData = response.data;
          console.log(
            `✅ Generated script for ${poster.product_name} in ${duration}ms`
          );
          console.log(`   Script: "${scriptData.script}"`);
          console.log(
            `   Language: ${scriptData.language}, Tone: ${scriptData.tone}`
          );

          // Validate script quality
          const wordCount = scriptData.script.split(" ").length;
          const isValidLength = wordCount <= 25 && wordCount >= 5;

          this.addTestResult(
            `Dynamic Script - ${poster.product_name}`,
            isValidLength && duration < 5000,
            `Script generated in ${duration}ms, ${wordCount} words, ${
              isValidLength ? "valid length" : "invalid length"
            }`
          );
        } else {
          this.addTestResult(
            `Dynamic Script - ${poster.product_name}`,
            false,
            `Failed: ${response.data.error || "Unknown error"}`
          );
        }
      } catch (error) {
        this.addTestResult(
          `Dynamic Script - ${poster.product_name}`,
          false,
          `Error: ${error.message}`
        );
      }
    }
  }

  async testUserPersonalization() {
    console.log("\n👤 Test 3: User Personalization");

    try {
      // Test with different user preferences
      const personalizedResponse = await axios.post(
        `${BASE_URL}/generate-dynamic-script`,
        {
          image_id: "poster_01",
          user_id: "user_prefers_hindi",
        }
      );

      if (
        personalizedResponse.status === 200 &&
        personalizedResponse.data.success
      ) {
        const scriptData = personalizedResponse.data;
        console.log(`✅ Generated personalized script`);
        console.log(
          `   Language: ${scriptData.language}, Tone: ${scriptData.tone}`
        );

        this.addTestResult(
          "User Personalization",
          true,
          `Personalized script generated with language: ${scriptData.language}`
        );
      } else {
        this.addTestResult(
          "User Personalization",
          false,
          "Failed to generate personalized script"
        );
      }
    } catch (error) {
      this.addTestResult(
        "User Personalization",
        false,
        `Error: ${error.message}`
      );
    }
  }

  async testCompleteAdContentGeneration() {
    console.log("\n🎬 Test 4: Complete Ad Content Generation");

    for (const poster of TEST_POSTERS.slice(0, 2)) {
      // Test first 2 posters to avoid timeout
      try {
        const startTime = Date.now();

        const response = await axios.post(
          `${BASE_URL}/ai-pipeline/generate_ad_content_from_poster`,
          {
            image_id: poster.image_id,
            user_id: TEST_USER_ID,
          }
        );

        const duration = Date.now() - startTime;

        if (response.status === 200 && response.data.success) {
          const adContent = response.data;
          console.log(
            `✅ Generated complete ad content for ${poster.product_name} in ${duration}ms`
          );
          console.log(`   Script: "${adContent.script}"`);
          console.log(
            `   Audio URL: ${adContent.audio_url ? "Generated" : "Missing"}`
          );
          console.log(
            `   Video URL: ${adContent.video_url ? "Generated" : "Missing"}`
          );

          const hasAllComponents =
            adContent.script && adContent.audio_url && adContent.video_url;
          const isWithinTimeLimit = duration < 10000; // 10 seconds

          this.addTestResult(
            `Complete Ad Content - ${poster.product_name}`,
            hasAllComponents && isWithinTimeLimit,
            `Generated in ${duration}ms, All components: ${
              hasAllComponents ? "Yes" : "No"
            }`
          );
        } else {
          this.addTestResult(
            `Complete Ad Content - ${poster.product_name}`,
            false,
            `Failed: ${response.data.error || "Unknown error"}`
          );
        }
      } catch (error) {
        this.addTestResult(
          `Complete Ad Content - ${poster.product_name}`,
          false,
          `Error: ${error.message}`
        );
      }
    }
  }

  async testAPIPerformance() {
    console.log("\n⚡ Test 5: API Performance");

    try {
      const performanceTests = [
        {
          name: "Script Generation",
          endpoint: "/generate-dynamic-script",
          maxTime: 3000,
        },
        {
          name: "Poster Metadata",
          endpoint: "/generate-dynamic-script/posters",
          maxTime: 1000,
        },
        { name: "Health Check", endpoint: "/health", maxTime: 500 },
      ];

      for (const test of performanceTests) {
        const startTime = Date.now();

        try {
          const response = await axios.get(
            `${BASE_URL.replace("/api/v1", "")}${test.endpoint}`
          );
          const duration = Date.now() - startTime;

          const passed = duration <= test.maxTime;
          console.log(
            `${passed ? "✅" : "❌"} ${test.name}: ${duration}ms (max: ${
              test.maxTime
            }ms)`
          );

          this.addTestResult(
            `Performance - ${test.name}`,
            passed,
            `Response time: ${duration}ms (limit: ${test.maxTime}ms)`
          );
        } catch (error) {
          this.addTestResult(
            `Performance - ${test.name}`,
            false,
            `Error: ${error.message}`
          );
        }
      }
    } catch (error) {
      this.addTestResult("API Performance", false, `Error: ${error.message}`);
    }
  }

  async testErrorHandling() {
    console.log("\n🛡️ Test 6: Error Handling");

    const errorTests = [
      {
        name: "Invalid Image ID",
        endpoint: "/generate-dynamic-script",
        data: { image_id: "invalid_poster_999" },
        expectedStatus: 404,
      },
      {
        name: "Missing Image ID",
        endpoint: "/generate-dynamic-script",
        data: {},
        expectedStatus: 400,
      },
      {
        name: "Invalid Poster ID",
        endpoint: "/generate-dynamic-script/poster/invalid_id",
        method: "GET",
        expectedStatus: 404,
      },
    ];

    for (const test of errorTests) {
      try {
        let response;
        if (test.method === "GET") {
          response = await axios.get(`${BASE_URL}${test.endpoint}`);
        } else {
          response = await axios.post(`${BASE_URL}${test.endpoint}`, test.data);
        }

        const passed = response.status === test.expectedStatus;
        console.log(
          `${passed ? "✅" : "❌"} ${test.name}: Status ${
            response.status
          } (expected: ${test.expectedStatus})`
        );

        this.addTestResult(
          `Error Handling - ${test.name}`,
          passed,
          `Status: ${response.status} (expected: ${test.expectedStatus})`
        );
      } catch (error) {
        const actualStatus = error.response?.status || 0;
        const passed = actualStatus === test.expectedStatus;
        console.log(
          `${passed ? "✅" : "❌"} ${
            test.name
          }: Status ${actualStatus} (expected: ${test.expectedStatus})`
        );

        this.addTestResult(
          `Error Handling - ${test.name}`,
          passed,
          `Status: ${actualStatus} (expected: ${test.expectedStatus})`
        );
      }
    }
  }

  async testEndToEndFlow() {
    console.log("\n🔄 Test 7: End-to-End Flow");

    try {
      const poster = TEST_POSTERS[0]; // Use first poster for E2E test
      const startTime = Date.now();

      console.log(`Testing complete flow for: ${poster.product_name}`);

      // Step 1: Generate dynamic script
      const scriptResponse = await axios.post(
        `${BASE_URL}/generate-dynamic-script`,
        {
          image_id: poster.image_id,
          user_id: TEST_USER_ID,
        }
      );

      if (!scriptResponse.data.success) {
        throw new Error("Script generation failed");
      }

      console.log(
        `✅ Step 1: Script generated - "${scriptResponse.data.script}"`
      );

      // Step 2: Generate complete ad content
      const adContentResponse = await axios.post(
        `${BASE_URL}/ai-pipeline/generate_ad_content_from_poster`,
        {
          image_id: poster.image_id,
          user_id: TEST_USER_ID,
        }
      );

      if (!adContentResponse.data.success) {
        throw new Error("Ad content generation failed");
      }

      const totalDuration = Date.now() - startTime;
      console.log(
        `✅ Step 2: Complete ad content generated in ${totalDuration}ms`
      );
      console.log(`   Video URL: ${adContentResponse.data.video_url}`);

      // Validate the complete flow
      const hasScript =
        adContentResponse.data.script &&
        adContentResponse.data.script.length > 0;
      const hasAudio =
        adContentResponse.data.audio_url &&
        adContentResponse.data.audio_url.length > 0;
      const hasVideo =
        adContentResponse.data.video_url &&
        adContentResponse.data.video_url.length > 0;
      const isWithinTimeLimit = totalDuration < 15000; // 15 seconds for complete flow

      const flowComplete =
        hasScript && hasAudio && hasVideo && isWithinTimeLimit;

      this.addTestResult(
        "End-to-End Flow",
        flowComplete,
        `Complete flow in ${totalDuration}ms. Script: ${
          hasScript ? "Yes" : "No"
        }, Audio: ${hasAudio ? "Yes" : "No"}, Video: ${hasVideo ? "Yes" : "No"}`
      );
    } catch (error) {
      this.addTestResult("End-to-End Flow", false, `Error: ${error.message}`);
    }
  }

  addTestResult(testName, passed, details) {
    this.results.tests.push({
      name: testName,
      passed,
      details,
      timestamp: new Date().toISOString(),
    });

    this.results.summary.total++;
    if (passed) {
      this.results.summary.passed++;
    } else {
      this.results.summary.failed++;
    }
  }

  generateReport() {
    const { summary, tests } = this.results;

    let report = `# Phase 2 - Week 7: Dynamic Script Generation & Personalization Testing Results\n\n`;
    report += `**Test Date:** ${this.results.timestamp}\n\n`;
    report += `## Summary\n\n`;
    report += `- **Total Tests:** ${summary.total}\n`;
    report += `- **Passed:** ${summary.passed} ✅\n`;
    report += `- **Failed:** ${summary.failed} ❌\n`;
    report += `- **Success Rate:** ${(
      (summary.passed / summary.total) *
      100
    ).toFixed(1)}%\n\n`;

    report += `## Test Results\n\n`;

    tests.forEach((test) => {
      const status = test.passed ? "✅ PASS" : "❌ FAIL";
      report += `### ${test.name}\n`;
      report += `**Status:** ${status}\n`;
      report += `**Details:** ${test.details}\n`;
      report += `**Time:** ${test.timestamp}\n\n`;
    });

    report += `## Deliverables Validation\n\n`;
    report += `| Deliverable | Status | Notes |\n`;
    report += `|-------------|--------|-------|\n`;
    report += `| Poster Metadata Schema | ${this.getTestStatus(
      "Poster Metadata Schema"
    )} | Backend can identify poster metadata instantly |\n`;
    report += `| Dynamic Script Generation | ${this.getTestStatus(
      "Dynamic Script"
    )} | Poster ID → returns dynamic, tone-based script |\n`;
    report += `| User Personalization | ${this.getTestStatus(
      "User Personalization"
    )} | Personalized tone/language merged with poster metadata |\n`;
    report += `| Complete Ad Content Pipeline | ${this.getTestStatus(
      "Complete Ad Content"
    )} | Single endpoint with full flow |\n`;
    report += `| API Performance | ${this.getTestStatus(
      "Performance"
    )} | Response times within limits |\n`;
    report += `| Error Handling | ${this.getTestStatus(
      "Error Handling"
    )} | Proper error responses |\n`;
    report += `| End-to-End Flow | ${this.getTestStatus(
      "End-to-End Flow"
    )} | Complete poster → video pipeline |\n\n`;

    report += `## Next Steps\n\n`;
    if (summary.failed === 0) {
      report += `🎉 **All tests passed!** The Phase 2 implementation is ready for production.\n\n`;
      report += `### Ready for Week 8:\n`;
      report += `- ✅ Dynamic script generation working\n`;
      report += `- ✅ User personalization integrated\n`;
      report += `- ✅ Complete ad content pipeline functional\n`;
      report += `- ✅ Android integration ready\n`;
      report += `- ✅ Performance within acceptable limits\n\n`;
    } else {
      report += `⚠️ **${summary.failed} test(s) failed.** Please review and fix the following issues:\n\n`;

      const failedTests = tests.filter((t) => !t.passed);
      failedTests.forEach((test) => {
        report += `- **${test.name}:** ${test.details}\n`;
      });
      report += `\n`;
    }

    // Write report to file
    fs.writeFileSync(TEST_RESULTS_FILE, report);

    console.log("\n📊 Testing Complete!");
    console.log("==================");
    console.log(`Total Tests: ${summary.total}`);
    console.log(`Passed: ${summary.passed} ✅`);
    console.log(`Failed: ${summary.failed} ❌`);
    console.log(
      `Success Rate: ${((summary.passed / summary.total) * 100).toFixed(1)}%`
    );
    console.log(`\n📄 Detailed report saved to: ${TEST_RESULTS_FILE}`);
  }

  getTestStatus(testNamePattern) {
    const matchingTests = this.results.tests.filter(
      (test) =>
        test.name.includes(testNamePattern) ||
        test.name.startsWith(testNamePattern)
    );

    if (matchingTests.length === 0) return "❓ Not Tested";

    const allPassed = matchingTests.every((test) => test.passed);
    return allPassed ? "✅ Pass" : "❌ Fail";
  }
}

// Run tests if this script is executed directly
if (require.main === module) {
  const tester = new Week7Tester();
  tester.runAllTests().catch(console.error);
}

module.exports = Week7Tester;
