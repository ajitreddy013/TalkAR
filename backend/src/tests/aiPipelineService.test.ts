import { AIPipelineService } from "../services/aiPipelineService";

// Simple test to verify the service can be imported and has the expected methods
describe("AIPipelineService", () => {
  it("should have generateScript method", () => {
    expect(typeof AIPipelineService.generateScript).toBe("function");
  });

  it("should have generateAudio method", () => {
    expect(typeof AIPipelineService.generateAudio).toBe("function");
  });

  it("should have generateLipSync method", () => {
    expect(typeof AIPipelineService.generateLipSync).toBe("function");
  });

  it("should have generateAIPipeline method", () => {
    expect(typeof AIPipelineService.generateAIPipeline).toBe("function");
  });

  it("should have getJobStatus method", () => {
    expect(typeof AIPipelineService.getJobStatus).toBe("function");
  });
});