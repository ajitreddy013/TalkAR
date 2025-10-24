// Manual test script for script generation functions
const { AIPipelineService } = require("./dist/services/aiPipelineService");

async function testScriptGeneration() {
  console.log("Testing script generation functions...\n");

  try {
    // Test generateProductScript
    console.log("1. Testing generateProductScript...");
    const productScript = await AIPipelineService.generateProductScript(
      "iPhone"
    );
    console.log("Product script:", productScript);
    console.log("Type:", typeof productScript);
    console.log("Length:", productScript.length);

    // Test generateScript with museum guide
    console.log("\n2. Testing generateScript with museum guide...");
    const museumScript = await AIPipelineService.generateScript({
      imageId: "test-image-123",
      language: "en",
      emotion: "happy",
    });
    console.log("Museum script:", museumScript);

    // Test generateScript with different language
    console.log("\n3. Testing generateScript with Spanish...");
    const spanishScript = await AIPipelineService.generateScript({
      imageId: "test-image-456",
      language: "es",
      emotion: "neutral",
    });
    console.log("Spanish script:", spanishScript);

    console.log("\nAll tests passed!");
  } catch (error) {
    console.error("Test failed:", error);
  }
}

// Run the test
testScriptGeneration();
