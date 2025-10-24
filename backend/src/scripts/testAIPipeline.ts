import { AIPipelineService } from "../services/aiPipelineService";

async function testAIPipeline() {
  console.log("Testing AI Pipeline Service...");
  
  try {
    // Test script generation
    console.log("\n1. Testing script generation...");
    const scriptResult = await AIPipelineService.generateScript({
      imageId: "test-image-123",
      language: "en",
      emotion: "happy"
    });
    console.log("Script result:", scriptResult);
    
    // Test audio generation
    console.log("\n2. Testing audio generation...");
    const audioResult = await AIPipelineService.generateAudio({
      text: scriptResult.text,
      language: scriptResult.language,
      emotion: scriptResult.emotion
    });
    console.log("Audio result:", audioResult);
    
    // Test lip-sync generation
    console.log("\n3. Testing lip-sync generation...");
    const lipSyncResult = await AIPipelineService.generateLipSync({
      imageId: "test-image-123",
      audioUrl: audioResult.audioUrl,
      emotion: scriptResult.emotion
    });
    console.log("Lip-sync result:", lipSyncResult);
    
    // Test complete pipeline
    console.log("\n4. Testing complete AI pipeline...");
    const pipelineResult = await AIPipelineService.generateAIPipeline(
      "test-image-123",
      "en",
      "neutral"
    );
    console.log("Pipeline result:", pipelineResult);
    
    // Wait a bit and check job status
    console.log("\n5. Checking job status...");
    setTimeout(async () => {
      const jobStatus = await AIPipelineService.getJobStatus(pipelineResult.jobId);
      console.log("Job status:", jobStatus);
    }, 2000);
    
  } catch (error) {
    console.error("Test failed:", error);
  }
}

// Run the test
testAIPipeline();