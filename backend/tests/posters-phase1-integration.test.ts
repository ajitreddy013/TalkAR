import { sequelize } from "../src/config/database";
import { Dialogue, Image } from "../src/models/Image";
import { TalkingPhotoArtifact } from "../src/models/TalkingPhotoArtifact";
import { PosterPreprocessResult } from "../src/models/PosterPreprocessResult";
import { defineAssociations } from "../src/models/associations";

describe("Posters Phase 1 integration", () => {
  beforeAll(async () => {
    defineAssociations();
    await sequelize.sync({ force: true });
  });

  afterEach(async () => {
    await TalkingPhotoArtifact.destroy({ where: {}, force: true });
    await PosterPreprocessResult.destroy({ where: {}, force: true });
    await Dialogue.destroy({ where: {}, force: true });
    await Image.destroy({ where: {}, force: true });
  });

  afterAll(async () => {
    await sequelize.close();
  });

  it("returns null artifact before generation starts (ARTIFACT_NOT_READY precondition)", async () => {
    const image = await Image.create({
      name: "Poster A",
      description: "test",
      imageUrl: "/uploads/a.jpg",
      thumbnailUrl: "/uploads/a-thumb.jpg",
      isActive: true,
    } as any);

    jest.resetModules();
    const svc = require("../src/services/talkingPhotoArtifactService");
    const artifact = await svc.getTalkingPhotoArtifact(image.id);
    expect(artifact).toBeNull();
  });

  it("moves queued to failed when preprocess result is not eligible", async () => {
    const image = await Image.create({
      name: "Poster B",
      description: "test",
      imageUrl: "/uploads/b.jpg",
      thumbnailUrl: "/uploads/b-thumb.jpg",
      isActive: true,
    } as any);

    const dialogue = await Dialogue.create({
      imageId: image.id,
      text: "Hello from poster",
      language: "en",
      voiceId: "voice-1",
      isDefault: true,
      isActive: true,
    } as any);

    await PosterPreprocessResult.create({
      imageId: image.id,
      status: "failed",
      provider: "test",
      faceDetected: false,
      confidence: 0.12,
      eligibleForTalkingPhoto: false,
      faceBox: null,
      lipRoi: null,
      errorCode: "NO_FACE_IN_POSTER",
      errorMessage: "No face detected",
      processedAt: new Date(),
    } as any);

    process.env.TALKING_PHOTO_REAL_PROVIDER = "false";
    process.env.TALKING_PHOTO_ENABLE_WAV2LIP_FALLBACK = "false";
    jest.resetModules();
    const svc = require("../src/services/talkingPhotoArtifactService");

    const queued = await svc.enqueueTalkingPhotoGeneration(image.id, {
      source: "manual_admin",
      idempotencyKey: `idem-${image.id}`,
    });
    expect(queued).toBeTruthy();

    await svc.processTalkingPhotoJob({
      imageId: image.id,
      dialogueId: dialogue.id,
      text: dialogue.text,
      language: "en",
      version: queued.version,
      correlationId: "corr-test-1",
    });

    const artifact = await TalkingPhotoArtifact.findOne({ where: { imageId: image.id } });
    expect(artifact).toBeTruthy();
    expect(artifact?.status).toBe("failed");
    expect(artifact?.errorCode).toBe("NO_FACE_IN_POSTER");
  });

  it("moves queued to ready when preprocess is eligible and fallback provider returns artifact", async () => {
    const image = await Image.create({
      name: "Poster C",
      description: "test",
      imageUrl: "/uploads/c.jpg",
      thumbnailUrl: "/uploads/c-thumb.jpg",
      isActive: true,
    } as any);

    const dialogue = await Dialogue.create({
      imageId: image.id,
      text: "Talking poster content",
      language: "en",
      voiceId: "voice-1",
      isDefault: true,
      isActive: true,
    } as any);

    await PosterPreprocessResult.create({
      imageId: image.id,
      status: "ready",
      provider: "test",
      faceDetected: true,
      confidence: 0.93,
      eligibleForTalkingPhoto: true,
      faceBox: JSON.stringify({ x: 0.2, y: 0.2, width: 0.5, height: 0.6 }),
      lipRoi: JSON.stringify({ lipX: 0.4, lipY: 0.58, lipWidth: 0.2, lipHeight: 0.08 }),
      errorCode: null,
      errorMessage: null,
      processedAt: new Date(),
    } as any);

    process.env.TALKING_PHOTO_REAL_PROVIDER = "false";
    process.env.TALKING_PHOTO_ENABLE_WAV2LIP_FALLBACK = "true";
    process.env.WAV2LIP_WORKER_URL = "http://localhost:9999";
    jest.resetModules();

    jest.doMock("axios", () => ({
      __esModule: true,
      default: {
        post: jest.fn().mockResolvedValue({
          data: { success: true, artifactUrl: "https://cdn.example.com/poster-c.mp4" },
        }),
        get: jest.fn(),
      },
    }));

    const svc = require("../src/services/talkingPhotoArtifactService");
    const queued = await svc.enqueueTalkingPhotoGeneration(image.id, {
      source: "manual_admin",
      idempotencyKey: `idem-ready-${image.id}`,
    });
    expect(queued).toBeTruthy();

    await svc.processTalkingPhotoJob({
      imageId: image.id,
      dialogueId: dialogue.id,
      text: dialogue.text,
      language: "en",
      version: queued.version,
      correlationId: "corr-test-2",
    });

    const artifact = await TalkingPhotoArtifact.findOne({ where: { imageId: image.id } });
    expect(artifact).toBeTruthy();
    expect(artifact?.status).toBe("ready");
    expect(artifact?.videoUrl).toContain("poster-c.mp4");
    expect(artifact?.confidence).toBeCloseTo(0.93, 2);
    expect(artifact?.errorCode).toBeNull();
  });

  it("deduplicates enqueue by idempotency key", async () => {
    const image = await Image.create({
      name: "Poster D",
      description: "test",
      imageUrl: "/uploads/d.jpg",
      thumbnailUrl: "/uploads/d-thumb.jpg",
      isActive: true,
    } as any);
    await Dialogue.create({
      imageId: image.id,
      text: "Idempotency test",
      language: "en",
      isDefault: true,
      isActive: true,
    } as any);

    jest.resetModules();
    const svc = require("../src/services/talkingPhotoArtifactService");
    const key = `idem-key-${image.id}`;
    const first = await svc.enqueueTalkingPhotoGeneration(image.id, {
      source: "manual_admin",
      idempotencyKey: key,
    });
    const second = await svc.enqueueTalkingPhotoGeneration(image.id, {
      source: "manual_admin",
      idempotencyKey: key,
    });

    expect(first).toBeTruthy();
    expect(second).toBeTruthy();
    expect(second.deduplicated).toBe(true);
    expect(second.version).toBe(first.version);
  });

  it("attempts self-hosted fallback once when managed provider fails", async () => {
    const image = await Image.create({
      name: "Poster E",
      description: "test",
      imageUrl: "/uploads/e.jpg",
      thumbnailUrl: "/uploads/e-thumb.jpg",
      isActive: true,
    } as any);
    const dialogue = await Dialogue.create({
      imageId: image.id,
      text: "Provider fallback test",
      language: "en",
      isDefault: true,
      isActive: true,
    } as any);
    await PosterPreprocessResult.create({
      imageId: image.id,
      status: "ready",
      provider: "test",
      faceDetected: true,
      confidence: 0.9,
      eligibleForTalkingPhoto: true,
      faceBox: JSON.stringify({ x: 0.2, y: 0.2, width: 0.5, height: 0.6 }),
      lipRoi: JSON.stringify({ lipX: 0.4, lipY: 0.58, lipWidth: 0.2, lipHeight: 0.08 }),
      processedAt: new Date(),
    } as any);

    process.env.TALKING_PHOTO_REAL_PROVIDER = "true";
    process.env.SYNC_API_KEY = "test";
    process.env.TALKING_PHOTO_ENABLE_WAV2LIP_FALLBACK = "true";
    process.env.WAV2LIP_WORKER_URL = "http://localhost:9999";
    jest.resetModules();

    jest.doMock("../src/services/syncService", () => ({
      generateSyncVideo: jest.fn().mockResolvedValue({ jobId: "managed-job-1" }),
      getSyncStatus: jest.fn().mockResolvedValue({ status: "failed", videoUrl: null }),
    }));
    const mockPost = jest.fn().mockResolvedValue({
      data: { success: true, artifactUrl: "https://cdn.example.com/poster-e.mp4" },
    });
    jest.doMock("axios", () => ({
      __esModule: true,
      default: {
        post: mockPost,
        get: jest.fn(),
      },
    }));

    const svc = require("../src/services/talkingPhotoArtifactService");
    await TalkingPhotoArtifact.upsert({
      imageId: image.id,
      dialogueId: dialogue.id,
      status: "queued",
      version: 1,
      errorCode: null,
      errorMessage: null,
    });
    await svc.processTalkingPhotoJob({
      imageId: image.id,
      dialogueId: dialogue.id,
      text: dialogue.text,
      language: "en",
      version: 1,
      correlationId: "corr-fallback-once",
    });

    expect(mockPost).toHaveBeenCalledTimes(1);
    const artifact = await TalkingPhotoArtifact.findOne({ where: { imageId: image.id } });
    expect(artifact?.status).toBe("ready");
  });
});
