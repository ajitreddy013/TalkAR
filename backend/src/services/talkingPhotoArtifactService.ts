import crypto from "crypto";
import axios from "axios";
import { Dialogue, Image } from "../models/Image";
import {
  ArtifactStatus,
  TalkingPhotoArtifact,
} from "../models/TalkingPhotoArtifact";
import { generateSyncVideo, getSyncStatus } from "./syncService";
import { randomUUID } from "crypto";
import { uploadBufferToS3 } from "./uploadService";
import { getPosterPreprocessResult } from "./posterPreprocessService";
import { cleanupArtifactVersionsInS3 } from "./talkingPhotoLifecycleService";
import { getEffectiveRuntimeFlags } from "./talkingPhotoRuntimeService";

export type ArtifactErrorCode =
  | "ARTIFACT_NOT_READY"
  | "NO_FACE_IN_POSTER"
  | "NO_DEFAULT_SCRIPT"
  | "PROVIDER_TIMEOUT"
  | "PROVIDER_FAILED"
  | "NETWORK_UNAVAILABLE";

type LandmarkShape = {
  lipX: number;
  lipY: number;
  lipWidth: number;
  lipHeight: number;
};

type FaceBoxShape = {
  x: number;
  y: number;
  width: number;
  height: number;
};

const DEFAULT_VIDEO =
  "https://assets.sync.so/docs/example-talking-head.mp4";
function managedProviderEnabled(): boolean {
  return process.env.TALKING_PHOTO_REAL_PROVIDER === "true" && !!process.env.SYNC_API_KEY;
}

function selfHostedFallbackEnabled(): boolean {
  const effective = getEffectiveRuntimeFlags();
  return effective.enableFallback;
}
const processingStartedAt = new Map<string, number>();
const idempotencyMap = new Map<string, { imageId: string; version: number; queuedAt: number }>();
const IDEMPOTENCY_TTL_MS = 10 * 60 * 1000;

type ProviderName = "managed" | "self_hosted";
type RetrySource = "image_upsert_or_script_update" | "manual_admin" | "mobile_runtime";

export type QueueJob = {
  imageId: string;
  dialogueId: string;
  text: string;
  language: string;
  version: number;
  correlationId: string;
};

function buildWorkerAuthHeaders(payload: unknown) {
  const authToken = process.env.WAV2LIP_WORKER_AUTH_TOKEN;
  const signingSecret = process.env.WAV2LIP_WORKER_SIGNING_SECRET;
  const timestamp = Date.now().toString();
  const nonce = randomUUID();
  const payloadJson = JSON.stringify(payload);

  const headers: Record<string, string> = {
    "Content-Type": "application/json",
  };

  if (authToken) {
    headers["x-worker-auth-token"] = authToken;
  }

  if (signingSecret) {
    const signature = crypto
      .createHmac("sha256", signingSecret)
      .update(`${timestamp}.${nonce}.${payloadJson}`)
      .digest("hex");
    headers["x-worker-timestamp"] = timestamp;
    headers["x-worker-nonce"] = nonce;
    headers["x-worker-signature"] = signature;
  }

  return headers;
}

function clamp01(v: number): number {
  return Math.max(0, Math.min(1, v));
}

function computeDeterministicLandmarks(seed: string): {
  landmarks: LandmarkShape;
  faceBox: FaceBoxShape;
  confidence: number;
} {
  const digest = crypto.createHash("sha256").update(seed).digest();
  const n = (idx: number) => digest[idx] / 255;

  const faceX = 0.25 + n(0) * 0.1;
  const faceY = 0.18 + n(1) * 0.1;
  const faceW = 0.45 + n(2) * 0.12;
  const faceH = 0.55 + n(3) * 0.15;

  const lipW = 0.18 + n(4) * 0.08;
  const lipH = 0.05 + n(5) * 0.05;
  const lipX = faceX + faceW * (0.5 - lipW * 0.5);
  const lipY = faceY + faceH * (0.62 + n(6) * 0.08);

  const confidence = clamp01(0.78 + n(7) * 0.2);
  return {
    landmarks: {
      lipX: clamp01(lipX),
      lipY: clamp01(lipY),
      lipWidth: clamp01(lipW),
      lipHeight: clamp01(lipH),
    },
    faceBox: {
      x: clamp01(faceX),
      y: clamp01(faceY),
      width: clamp01(faceW),
      height: clamp01(faceH),
    },
    confidence,
  };
}

function pruneIdempotencyMap(now: number) {
  for (const [key, entry] of idempotencyMap.entries()) {
    if (now - entry.queuedAt > IDEMPOTENCY_TTL_MS) {
      idempotencyMap.delete(key);
    }
  }
}

async function markArtifactFailed(
  imageId: string,
  errorCode: ArtifactErrorCode,
  errorMessage: string,
  provider: ProviderName | "none" = "none",
  stageTimings?: Record<string, number>,
  correlationId?: string
) {
  await TalkingPhotoArtifact.update(
    {
      status: "failed",
      errorCode,
      errorMessage,
      provider,
      stageTimings: stageTimings ? JSON.stringify(stageTimings) : null,
      lastCorrelationId: correlationId || null,
    },
    { where: { imageId } }
  );
}

async function runSelfHostedFallback(job: QueueJob): Promise<string | null> {
  if (!selfHostedFallbackEnabled()) return null;
  try {
    const workerUrl = process.env.WAV2LIP_WORKER_URL;
    if (!workerUrl) return null;
    const endpoint = `${workerUrl.replace(/\/$/, "")}/generate`;
    const timeoutMs = Number(process.env.WAV2LIP_WORKER_TIMEOUT_MS || 60000);
    const requestPayload = {
      posterId: job.imageId,
      dialogueId: job.dialogueId,
      text: job.text,
      language: job.language,
      version: job.version,
      correlationId: job.correlationId,
    };
    const authHeaders = buildWorkerAuthHeaders(requestPayload);
    const response = await axios.post(
      endpoint,
      requestPayload,
      {
        timeout: timeoutMs,
        headers: {
          ...authHeaders,
          "x-correlation-id": job.correlationId,
        },
      }
    );

    const data = response.data || {};
    if (data.success === false) {
      return null;
    }
    const artifactUrl =
      data.videoUrl || data.artifactUrl || data.outputUrl || data.url || null;
    if (typeof artifactUrl === "string" && artifactUrl.trim() !== "") {
      return artifactUrl;
    }
    return null;
  } catch {
    return null;
  }
}

async function mirrorArtifactToS3IfNeeded(
  providerVideoUrl: string,
  imageId: string,
  version: number
): Promise<string> {
  const shouldMirror =
    process.env.TALKING_PHOTO_MIRROR_TO_S3 === "true" &&
    !!process.env.AWS_S3_BUCKET;
  if (!shouldMirror) return providerVideoUrl;

  try {
    const response = await axios.get<ArrayBuffer>(providerVideoUrl, {
      responseType: "arraybuffer",
      timeout: Number(process.env.TALKING_PHOTO_DOWNLOAD_TIMEOUT_MS || 45000),
    });

    const contentType =
      (response.headers["content-type"] as string | undefined) || "video/mp4";
    const extension = contentType.includes("webm") ? "webm" : "mp4";
    const key = `talking-photo/${imageId}/v${version}/artifact.${extension}`;
    const uploadedUrl = await uploadBufferToS3(
      Buffer.from(response.data),
      key,
      contentType
    );
    return uploadedUrl;
  } catch (error) {
    console.warn("[talking-photo] S3 mirroring failed; keeping provider URL", {
      imageId,
      version,
      message: (error as Error).message,
    });
    return providerVideoUrl;
  }
}

export async function processTalkingPhotoJob(job: QueueJob): Promise<void> {
  const { imageId, dialogueId, text, language, version, correlationId } = job;
  const stageStartedAt = {
    queuedToProcessingMs: 0,
    providerGenerateMs: 0,
    mirrorToS3Ms: 0,
    totalMs: 0,
  };
  try {
    const startedAt = Date.now();
    processingStartedAt.set(imageId, startedAt);
    await TalkingPhotoArtifact.update(
      {
        status: "processing" as ArtifactStatus,
        errorCode: null,
        errorMessage: null,
        provider: null,
        stageTimings: null,
        lastProcessingDurationMs: null,
        lastCorrelationId: correlationId,
      },
      { where: { imageId } }
    );
    stageStartedAt.queuedToProcessingMs = Date.now() - startedAt;

    const preprocess = await getPosterPreprocessResult(imageId);
    if (!preprocess || !preprocess.eligibleForTalkingPhoto) {
      const totalMs = Date.now() - startedAt;
      await TalkingPhotoArtifact.update(
        {
          status: "failed",
          errorCode: preprocess?.errorCode || "NO_FACE_IN_POSTER",
          errorMessage:
            preprocess?.errorMessage ||
            "Poster is not eligible for talking photo generation.",
          confidence: preprocess?.confidence ?? null,
          provider: "none",
          stageTimings: JSON.stringify({ ...stageStartedAt, totalMs }),
          lastProcessingDurationMs: totalMs,
          lastCorrelationId: correlationId,
        },
        { where: { imageId } }
      );
      processingStartedAt.delete(imageId);
      return;
    }

    const faceBox = safeParseJson<FaceBoxShape>(preprocess.faceBox) || null;
    const landmarks = safeParseJson<LandmarkShape>(preprocess.lipRoi) || null;
    const confidence = preprocess.confidence ?? 0;
    if (!faceBox || !landmarks) {
      const totalMs = Date.now() - startedAt;
      await TalkingPhotoArtifact.update(
        {
          status: "failed",
          errorCode: "NO_FACE_IN_POSTER",
          errorMessage: "Preprocess metadata is incomplete for this poster.",
          confidence: preprocess.confidence ?? null,
          provider: "none",
          stageTimings: JSON.stringify({ ...stageStartedAt, totalMs }),
          lastProcessingDurationMs: totalMs,
          lastCorrelationId: correlationId,
        },
        { where: { imageId } }
      );
      processingStartedAt.delete(imageId);
      return;
    }

    let provider: ProviderName = "managed";
    const providerStartedAt = Date.now();
    let providerVideoUrl = await resolveVideoUrl(text, language);
    stageStartedAt.providerGenerateMs = Date.now() - providerStartedAt;

    if (!providerVideoUrl) {
      provider = "self_hosted";
      const fallbackStartedAt = Date.now();
      providerVideoUrl = await runSelfHostedFallback(job);
      stageStartedAt.providerGenerateMs += Date.now() - fallbackStartedAt;
    }

    if (!providerVideoUrl) {
      await markArtifactFailed(
        imageId,
        "PROVIDER_FAILED",
        "All configured providers failed to generate artifact.",
        provider,
        {
          ...stageStartedAt,
          totalMs: Date.now() - startedAt,
        },
        correlationId
      );
      processingStartedAt.delete(imageId);
      return;
    }
    const mirrorStartedAt = Date.now();
    const artifactVideoUrl = await mirrorArtifactToS3IfNeeded(
      providerVideoUrl,
      imageId,
      version
    );
    stageStartedAt.mirrorToS3Ms = Date.now() - mirrorStartedAt;
    const totalDurationMs = Date.now() - startedAt;

    await TalkingPhotoArtifact.update(
      {
        dialogueId,
        status: "ready",
        videoUrl: artifactVideoUrl || DEFAULT_VIDEO,
        lipLandmarks: JSON.stringify(landmarks),
        posterFaceBox: JSON.stringify(faceBox),
        errorCode: null,
        errorMessage: null,
        confidence,
        provider,
        stageTimings: JSON.stringify({
          ...stageStartedAt,
          totalMs: totalDurationMs,
        }),
        lastProcessingDurationMs: totalDurationMs,
        lastCorrelationId: correlationId,
      },
      { where: { imageId } }
    );

    const keepLatestN = Number(process.env.TALKING_PHOTO_KEEP_LATEST_N || 3);
    if (keepLatestN > 0 && process.env.TALKING_PHOTO_MIRROR_TO_S3 === "true") {
      try {
        await cleanupArtifactVersionsInS3(imageId, keepLatestN);
      } catch (cleanupError) {
        console.warn("[talking-photo] artifact lifecycle cleanup failed", {
          imageId,
          version,
          message: (cleanupError as Error).message,
        });
      }
    }

    const metricsStart = processingStartedAt.get(imageId) || Date.now();
    console.info("[talking-photo] ready", {
      imageId,
      version,
      provider,
      correlationId,
      processingDurationMs: Date.now() - metricsStart,
      confidence,
    });
    processingStartedAt.delete(imageId);
  } catch (err: any) {
    await markArtifactFailed(
      imageId,
      "PROVIDER_FAILED",
      err?.message || "Provider failed while generating talking photo artifact.",
      "none",
      undefined,
      correlationId
    );
    processingStartedAt.delete(imageId);
  }
}

async function publishGenerationJob(job: QueueJob): Promise<void> {
  const runtime = getEffectiveRuntimeFlags();
  const drainMode = process.env.TALKING_PHOTO_DRAIN_MODE === "true";
  if (runtime.disableEnqueue || runtime.forceReadyOnly || drainMode) {
    console.info("[talking-photo] enqueue skipped by runtime mode", {
      imageId: job.imageId,
      runtimeMode: runtime.runtimeMode,
      drainMode,
    });
    return;
  }

  // SQS path can be enabled in production; fallback remains async in-process.
  // Keeping fallback ensures local/dev/test compatibility.
  if (process.env.TALKING_PHOTO_QUEUE_MODE === "sqs") {
    try {
      const AWS = await import("aws-sdk");
      const queueUrl = process.env.TALKING_PHOTO_SQS_URL;
      if (!queueUrl) throw new Error("Missing TALKING_PHOTO_SQS_URL");
      const sqs = new AWS.SQS({ region: process.env.AWS_REGION || "us-east-1" });
      await sqs
        .sendMessage({
          QueueUrl: queueUrl,
          MessageBody: JSON.stringify(job),
          MessageGroupId: job.imageId,
          MessageDeduplicationId: `${job.imageId}:${job.version}`,
        })
        .promise();
      return;
    } catch (error) {
      console.warn("[talking-photo] SQS publish failed, using local async fallback", {
        message: (error as Error).message,
      });
    }
  }

  setImmediate(() => {
    processTalkingPhotoJob(job).catch((err) => {
      console.error("[talking-photo] local job execution failure", err);
    });
  });
}

export async function enqueueTalkingPhotoGeneration(
  imageId: string,
  options?: {
    idempotencyKey?: string;
    source?: RetrySource;
    correlationId?: string;
  }
) {
  const image = await Image.findByPk(imageId, {
    include: [{ model: Dialogue, as: "dialogues" }],
  });
  if (!image) return null;

  const defaultDialogue = (image.dialogues || []).find((d) => d.isDefault);
  if (!defaultDialogue || !defaultDialogue.text?.trim()) {
    await TalkingPhotoArtifact.upsert({
      imageId,
      dialogueId: defaultDialogue?.id || null,
      status: "failed",
      errorCode: "NO_DEFAULT_SCRIPT",
      errorMessage:
        "Default script is required before generating talking photo artifact.",
    });
    console.info("[talking-photo] failed:NO_DEFAULT_SCRIPT", { imageId });
    return null;
  }

  const now = Date.now();
  pruneIdempotencyMap(now);
  if (options?.idempotencyKey) {
    const existingByKey = idempotencyMap.get(options.idempotencyKey);
    if (existingByKey && existingByKey.imageId === imageId) {
      return {
        imageId,
        version: existingByKey.version,
        deduplicated: true,
      };
    }
  }

  const existing = await TalkingPhotoArtifact.findOne({ where: { imageId } });
  const nextVersion = (existing?.version || 0) + 1;

  await TalkingPhotoArtifact.upsert({
    imageId,
    dialogueId: defaultDialogue.id,
    status: "queued",
    errorCode: null,
    errorMessage: null,
    version: nextVersion,
  });
  console.info("[talking-photo] queued", {
    imageId,
    version: nextVersion,
    source: options?.source || "image_upsert_or_script_update",
    correlationId: options?.correlationId || null,
  });

  if (options?.idempotencyKey) {
    idempotencyMap.set(options.idempotencyKey, {
      imageId,
      version: nextVersion,
      queuedAt: now,
    });
  }

  await publishGenerationJob({
    imageId,
    dialogueId: defaultDialogue.id,
    text: defaultDialogue.text,
    language: defaultDialogue.language || "en",
    version: nextVersion,
    correlationId: options?.correlationId || randomUUID(),
  });

  return { imageId, version: nextVersion };
}

async function resolveVideoUrl(text: string, language: string): Promise<string | null> {
  if (!managedProviderEnabled()) return null;
  try {
    const job = await generateSyncVideo({
      text,
      language,
      voiceId: "voice-2",
    });
    const jobId = job.jobId;
    for (let i = 0; i < 8; i++) {
      const status = await getSyncStatus(jobId);
      if (status.status === "completed" && status.videoUrl) {
        return status.videoUrl;
      }
      if (status.status === "failed") {
        return null;
      }
      await new Promise((r) => setTimeout(r, 1200));
    }
  } catch {
    console.warn("[talking-photo] provider error while resolving video url", {
      language,
    });
    return null;
  }
  return null;
}

export async function getTalkingPhotoArtifact(imageId: string) {
  return TalkingPhotoArtifact.findOne({ where: { imageId } });
}

export function safeParseJson<T>(json?: string | null): T | null {
  if (!json) return null;
  try {
    return JSON.parse(json) as T;
  } catch {
    return null;
  }
}
