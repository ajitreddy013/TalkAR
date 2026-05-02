import { sequelize } from "../src/config/database";
import { defineAssociations } from "../src/models/associations";
import {
  getEffectiveRuntimeFlags,
  getReleaseProfile,
  setRuntimeOverrides,
  validateReleasePolicyAtStartup,
} from "../src/services/talkingPhotoRuntimeService";
import { Dialogue, Image } from "../src/models/Image";
import { TalkingPhotoArtifact } from "../src/models/TalkingPhotoArtifact";
import { enqueueTalkingPhotoGeneration } from "../src/services/talkingPhotoArtifactService";

describe("Phase 2 runtime controls", () => {
  const previousEnv = { ...process.env };

  beforeAll(async () => {
    defineAssociations();
    await sequelize.sync({ force: true });
  });

  afterEach(async () => {
    await TalkingPhotoArtifact.destroy({ where: {}, force: true });
    await Dialogue.destroy({ where: {}, force: true });
    await Image.destroy({ where: {}, force: true });
    setRuntimeOverrides(
      { disableEnqueue: false, forceReadyOnly: false, enableFallback: false },
      "test-reset",
      null
    );
    process.env = { ...previousEnv };
  });

  afterAll(async () => {
    await sequelize.close();
  });

  it("applies runtime override toggles", async () => {
    const initial = getEffectiveRuntimeFlags();
    expect(initial).toHaveProperty("runtimeMode");

    const updated = setRuntimeOverrides(
      { disableEnqueue: true, forceReadyOnly: true, enableFallback: false },
      "test",
      "corr-phase2-1"
    );
    expect(updated.runtimeMode).toBe("enqueue_disabled");
    expect(updated.disableEnqueue).toBe(true);
    expect(updated.forceReadyOnly).toBe(true);
  });

  it("skips enqueue when disableEnqueue is true", async () => {
    const image = await Image.create({
      name: "Poster Runtime",
      description: "phase2",
      imageUrl: "/uploads/runtime.jpg",
      thumbnailUrl: "/uploads/runtime-thumb.jpg",
      isActive: true,
    } as any);
    await Dialogue.create({
      imageId: image.id,
      text: "Runtime switch test",
      language: "en",
      isDefault: true,
      isActive: true,
    } as any);

    setRuntimeOverrides(
      { disableEnqueue: true, forceReadyOnly: false, enableFallback: false },
      "test",
      "corr-phase2-2"
    );

    const queued = await enqueueTalkingPhotoGeneration(image.id, {
      source: "manual_admin",
      idempotencyKey: `phase2-${image.id}`,
    });
    expect(queued).toBeTruthy();

    await new Promise((resolve) => setTimeout(resolve, 50));

    const artifact = await TalkingPhotoArtifact.findOne({ where: { imageId: image.id } });
    expect(artifact).toBeTruthy();
    expect(artifact?.status).toBe("queued");
  });

  it("resolves ga release profile and enforces startup policy env", () => {
    process.env.TALKING_PHOTO_RELEASE_PROFILE = "ga";
    process.env.TALKING_PHOTO_SQS_URL = "https://example-queue";
    process.env.AWS_REGION = "us-east-1";
    process.env.AWS_S3_BUCKET = "bucket";
    process.env.JWT_SECRET = "secret";
    process.env.TALKING_PHOTO_ENABLE_WAV2LIP_FALLBACK = "true";
    process.env.TALKING_PHOTO_QUEUE_BACKLOG_THRESHOLD_WARNING = "50";
    process.env.TALKING_PHOTO_QUEUE_BACKLOG_THRESHOLD_CRITICAL = "150";
    process.env.TALKING_PHOTO_DLQ_BACKLOG_THRESHOLD = "5";
    process.env.TALKING_PHOTO_MAX_MESSAGE_AGE_SECONDS_THRESHOLD = "300";

    expect(getReleaseProfile()).toBe("ga");
    expect(() => validateReleasePolicyAtStartup()).not.toThrow();
  });

  it("fails startup policy validation when required ga env is missing", () => {
    process.env.TALKING_PHOTO_RELEASE_PROFILE = "ga";
    delete process.env.TALKING_PHOTO_SQS_URL;
    process.env.AWS_REGION = "us-east-1";
    process.env.AWS_S3_BUCKET = "bucket";
    process.env.JWT_SECRET = "secret";
    process.env.TALKING_PHOTO_ENABLE_WAV2LIP_FALLBACK = "true";
    process.env.TALKING_PHOTO_QUEUE_BACKLOG_THRESHOLD_WARNING = "50";
    process.env.TALKING_PHOTO_QUEUE_BACKLOG_THRESHOLD_CRITICAL = "150";
    process.env.TALKING_PHOTO_DLQ_BACKLOG_THRESHOLD = "5";
    process.env.TALKING_PHOTO_MAX_MESSAGE_AGE_SECONDS_THRESHOLD = "300";

    expect(() => validateReleasePolicyAtStartup()).toThrow(/missing env/i);
  });
});
