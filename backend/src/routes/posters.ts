import { Router } from "express";
import rateLimit from "express-rate-limit";
import { Image } from "../models/Image";
import { authenticateAdmin } from "../middleware/auth";
import {
  enqueueTalkingPhotoGeneration,
  getTalkingPhotoArtifact,
  safeParseJson,
} from "../services/talkingPhotoArtifactService";
import { TalkingPhotoArtifact } from "../models/TalkingPhotoArtifact";
import { Dialogue } from "../models/Image";
import { getSignedArtifactUrl } from "../utils/cdn";
import { getPosterPreprocessResult } from "../services/posterPreprocessService";
import { PosterPreprocessResult } from "../models/PosterPreprocessResult";
import { Op } from "sequelize";
import {
  getEffectiveRuntimeFlags,
  getRuntimeAuditTrail,
  setRuntimeOverrides,
} from "../services/talkingPhotoRuntimeService";

const router = Router();
const talkingPhotoRetryLimiter = rateLimit({
  windowMs: 60 * 1000,
  max: Number(process.env.TALKING_PHOTO_RETRY_RATE_LIMIT_MAX || 12),
  standardHeaders: true,
  legacyHeaders: false,
  message: "Too many talking-photo retry requests. Please try again in a minute.",
});

router.get("/index", async (req, res, next) => {
  try {
    const activePosters = await Image.findAll({
      where: { isActive: true },
      include: [
        { model: TalkingPhotoArtifact, as: "talkingPhotoArtifact" },
        { model: Dialogue, as: "dialogues" },
        { model: PosterPreprocessResult, as: "preprocessResult" },
      ],
      order: [["updatedAt", "DESC"]],
    });

    const posters = activePosters.map((poster: any) => {
      const artifact = poster.talkingPhotoArtifact;
      const hasDefaultScript = (poster.dialogues || []).some(
        (d: any) => d.isDefault && d.isActive && typeof d.text === "string" && d.text.trim() !== ""
      );
      const confidence = artifact?.confidence ?? null;
      const preprocessResult = (poster as any).preprocessResult;
      const preprocessEligible = preprocessResult
        ? !!preprocessResult.eligibleForTalkingPhoto
        : false;
      const eligible = hasDefaultScript && preprocessEligible;

      return {
        id: poster.id,
        name: poster.name,
        imageUrl: poster.imageUrl,
        thumbnailUrl: poster.thumbnailUrl,
        updatedAt: poster.updatedAt,
        talkingPhoto: {
          status: artifact?.status || "queued",
          version: artifact?.version || 0,
          confidence,
          eligible,
          errorCode: artifact?.errorCode || null,
        },
        preprocess: preprocessResult
          ? {
              status: preprocessResult.status,
              provider: preprocessResult.provider,
              confidence: preprocessResult.confidence,
              faceDetected: preprocessResult.faceDetected,
            }
          : null,
      };
    });

    return res.json({
      posters,
      count: posters.length,
      generatedAt: new Date().toISOString(),
    });
  } catch (error) {
    return next(error);
  }
});

router.get("/ops/alerts", authenticateAdmin, async (_req, res, next) => {
  try {
    const runtime = getEffectiveRuntimeFlags();
    const [total, failed, queueBacklog] = await Promise.all([
      TalkingPhotoArtifact.count(),
      TalkingPhotoArtifact.count({ where: { status: "failed" } }),
      TalkingPhotoArtifact.count({
        where: { status: { [Op.in]: ["queued", "processing"] } },
      }),
    ]);
    const failedRatio = total > 0 ? failed / total : 0;

    const recentFailures = await TalkingPhotoArtifact.findAll({
      where: { status: "failed" },
      attributes: ["provider"],
      order: [["updatedAt", "DESC"]],
      limit: 100,
    });
    const providerFailures = {
      managed: 0,
      self_hosted: 0,
      none: 0,
      unknown: 0,
    };
    for (const row of recentFailures as any[]) {
      const provider = row.provider;
      if (provider === "managed") providerFailures.managed++;
      else if (provider === "self_hosted") providerFailures.self_hosted++;
      else if (provider === "none") providerFailures.none++;
      else providerFailures.unknown++;
    }

    const failureSpikeThreshold = Number(process.env.PROVIDER_FAILURE_SPIKE_THRESHOLD || 20);
    const queueBacklogWarningThreshold = Number(
      process.env.TALKING_PHOTO_QUEUE_BACKLOG_THRESHOLD_WARNING ||
        process.env.TALKING_PHOTO_QUEUE_BACKLOG_THRESHOLD ||
        50
    );
    const queueBacklogCriticalThreshold = Number(
      process.env.TALKING_PHOTO_QUEUE_BACKLOG_THRESHOLD_CRITICAL || 150
    );
    const failedRatioThreshold = Number(process.env.TALKING_PHOTO_FAILED_RATIO_THRESHOLD || 0.35);
    const maxMessageAgeSeconds = Number(process.env.TALKING_PHOTO_QUEUE_OLDEST_MESSAGE_AGE_SECONDS || 0);
    const maxMessageAgeThreshold = Number(process.env.TALKING_PHOTO_MAX_MESSAGE_AGE_SECONDS_THRESHOLD || 300);
    const dlqBacklog = Number(process.env.TALKING_PHOTO_DLQ_BACKLOG || 0);
    const dlqBacklogThreshold = Number(process.env.TALKING_PHOTO_DLQ_BACKLOG_THRESHOLD || 5);

    const alerts = [
      {
        key: "queue_backlog",
        severity:
          queueBacklog >= queueBacklogCriticalThreshold
            ? "critical"
            : queueBacklog >= queueBacklogWarningThreshold
              ? "warning"
              : "info",
        active: queueBacklog >= queueBacklogWarningThreshold,
        message: `Queue backlog=${queueBacklog}, warning=${queueBacklogWarningThreshold}, critical=${queueBacklogCriticalThreshold}`,
      },
      {
        key: "provider_failure_spike",
        severity:
          (providerFailures.managed + providerFailures.self_hosted) >= failureSpikeThreshold
            ? "error"
            : "info",
        active:
          (providerFailures.managed + providerFailures.self_hosted) >= failureSpikeThreshold,
        message: `Recent provider failures=${
          providerFailures.managed + providerFailures.self_hosted
        }, threshold=${failureSpikeThreshold}`,
      },
      {
        key: "failed_ratio",
        severity: failedRatio >= failedRatioThreshold ? "warning" : "info",
        active: failedRatio >= failedRatioThreshold,
        message: `Failed ratio=${failedRatio.toFixed(3)}, threshold=${failedRatioThreshold}`,
      },
      {
        key: "queue_max_message_age",
        severity: maxMessageAgeSeconds >= maxMessageAgeThreshold ? "warning" : "info",
        active: maxMessageAgeSeconds >= maxMessageAgeThreshold,
        message: `Oldest queue message age=${maxMessageAgeSeconds}s, threshold=${maxMessageAgeThreshold}s`,
      },
      {
        key: "queue_dlq_backlog",
        severity: dlqBacklog >= dlqBacklogThreshold ? "critical" : "info",
        active: dlqBacklog >= dlqBacklogThreshold,
        message: `DLQ backlog=${dlqBacklog}, threshold=${dlqBacklogThreshold}`,
      },
    ];

    return res.json({
      source: "/api/v1/posters/ops/alerts",
      profile: runtime.profile,
      runtimeMode: runtime.runtimeMode,
      alerts,
      metrics: {
        total,
        failed,
        failedRatio,
        queueBacklog,
        queueBacklogWarningThreshold,
        queueBacklogCriticalThreshold,
        maxMessageAgeSeconds,
        dlqBacklog,
        providerFailures,
      },
      generatedAt: new Date().toISOString(),
    });
  } catch (error) {
    return next(error);
  }
});

router.get("/ops/toggles", authenticateAdmin, async (_req, res, next) => {
  try {
    return res.json({
      effective: getEffectiveRuntimeFlags(),
      auditTrail: getRuntimeAuditTrail().slice(0, 20),
    });
  } catch (error) {
    return next(error);
  }
});

router.post("/ops/toggles", authenticateAdmin, async (req, res, next) => {
  try {
    const actor =
      (typeof req.body?.actor === "string" && req.body.actor.trim() !== ""
        ? req.body.actor
        : "admin_dashboard");
    const correlationId =
      (req as any).correlationId ||
      (typeof req.header("x-correlation-id") === "string"
        ? req.header("x-correlation-id")
        : null);

    const effective = setRuntimeOverrides(
      {
        disableEnqueue:
          typeof req.body?.disableEnqueue === "boolean"
            ? req.body.disableEnqueue
            : undefined,
        forceReadyOnly:
          typeof req.body?.forceReadyOnly === "boolean"
            ? req.body.forceReadyOnly
            : undefined,
        enableFallback:
          typeof req.body?.enableFallback === "boolean"
            ? req.body.enableFallback
            : undefined,
      },
      actor,
      correlationId
    );
    return res.json({
      success: true,
      effective,
      updatedAt: new Date().toISOString(),
    });
  } catch (error) {
    return next(error);
  }
});

router.get("/:id/talking-photo", async (req, res, next) => {
  try {
    const runtime = getEffectiveRuntimeFlags();
    const imageId = req.params.id;
    const image = await Image.findByPk(imageId);
    if (!image || !image.isActive) {
      return res.status(404).json({ error: "Poster not found" });
    }

    const artifact = await getTalkingPhotoArtifact(imageId);
    if (runtime.forceReadyOnly) {
      if (!artifact || artifact.status !== "ready") {
        return res.status(409).json({
          imageId,
          status: "failed",
          runtimeMode: "ready_only",
          errorCode: "ARTIFACT_NOT_READY",
          errorMessage:
            "Artifact is not ready and generation is disabled in ready-only mode.",
        });
      }
    }

    if (!artifact) {
      return res.status(404).json({
        status: "failed",
        runtimeMode: runtime.runtimeMode,
        errorCode: "ARTIFACT_NOT_READY",
        errorMessage: "Artifact not created yet.",
      });
    }

    const signedArtifact = artifact.videoUrl
      ? getSignedArtifactUrl(artifact.videoUrl)
      : { url: null, expiresAt: null };

    return res.json({
      imageId,
      status: artifact.status,
      runtimeMode: runtime.runtimeMode,
      version: artifact.version,
      videoUrl: signedArtifact.url,
      videoUrlExpiresAt: signedArtifact.expiresAt,
      lipLandmarks: safeParseJson(artifact.lipLandmarks),
      posterFaceBox: safeParseJson(artifact.posterFaceBox),
      confidence: artifact.confidence ?? null,
      provider: (artifact as any).provider || null,
      stageTimings: safeParseJson((artifact as any).stageTimings),
      lastProcessingDurationMs: (artifact as any).lastProcessingDurationMs ?? null,
      lastCorrelationId: (artifact as any).lastCorrelationId || null,
      errorCode: artifact.errorCode || null,
      errorMessage: artifact.errorMessage || null,
      updatedAt: artifact.updatedAt,
    });
  } catch (error) {
    return next(error);
  }
});

router.post("/:id/talking-photo/retry", talkingPhotoRetryLimiter, async (req, res, next) => {
  try {
    const runtime = getEffectiveRuntimeFlags();
    if (runtime.disableEnqueue || runtime.forceReadyOnly) {
      return res.status(409).json({
        success: false,
        queued: false,
        runtimeMode: runtime.runtimeMode,
        errorCode: "ARTIFACT_NOT_READY",
        message:
          runtime.runtimeMode === "ready_only"
            ? "Retry is disabled in ready-only mode."
            : "Retry is disabled because enqueue is turned off.",
      });
    }

    const imageId = req.params.id;
    const source = typeof req.body?.source === "string" ? req.body.source : "manual_admin";
    const reason = typeof req.body?.reason === "string" ? req.body.reason : "manual_retry";
    const idempotencyKey =
      req.header("x-idempotency-key") ||
      (typeof req.body?.idempotencyKey === "string" ? req.body.idempotencyKey : undefined);
    const correlationId = req.header("x-correlation-id") || undefined;
    const image = await Image.findByPk(imageId);
    if (!image) {
      return res.status(404).json({ error: "Poster not found" });
    }

    await TalkingPhotoArtifact.update(
      {
        status: "queued",
        errorCode: null,
        errorMessage: null,
      },
      { where: { imageId } }
    );

    const queued = await enqueueTalkingPhotoGeneration(imageId, {
      idempotencyKey,
      source: "manual_admin",
      correlationId,
    });
    console.info("[talking-photo] retry requested", {
      imageId,
      source,
      reason,
      queued: !!queued,
      idempotencyKey: idempotencyKey || null,
    });
    return res.json({
      success: true,
      queued: !!queued,
      deduplicated: (queued as any)?.deduplicated || false,
      version: (queued as any)?.version || null,
      message: "Talking photo generation queued.",
    });
  } catch (error) {
    return next(error);
  }
});

router.get("/:id/preprocess-status", async (req, res, next) => {
  try {
    const imageId = req.params.id;
    const image = await Image.findByPk(imageId, {
      include: [{ model: TalkingPhotoArtifact, as: "talkingPhotoArtifact" }],
    });
    if (!image) {
      return res.status(404).json({ error: "Poster not found" });
    }
    const preprocess = await getPosterPreprocessResult(imageId);
    const artifact = (image as any).talkingPhotoArtifact;

    return res.json({
      imageId,
      status: preprocess?.status || "pending",
      provider: preprocess?.provider || "none",
      confidence: preprocess?.confidence ?? null,
      faceDetected: preprocess?.faceDetected ?? false,
      eligibleForTalkingPhoto: preprocess?.eligibleForTalkingPhoto ?? false,
      lipLandmarksAvailable: !!preprocess?.lipRoi,
      posterFaceBoxAvailable: !!preprocess?.faceBox,
      errorCode: preprocess?.errorCode || null,
      errorMessage: preprocess?.errorMessage || null,
      artifactStatus: artifact?.status || "queued",
      updatedAt: preprocess?.updatedAt || image.updatedAt,
    });
  } catch (error) {
    return next(error);
  }
});

router.get("/ops/metrics", async (_req, res, next) => {
  try {
    const runtime = getEffectiveRuntimeFlags();
    const [total, ready, failed, processing, queued] = await Promise.all([
      TalkingPhotoArtifact.count(),
      TalkingPhotoArtifact.count({ where: { status: "ready" } }),
      TalkingPhotoArtifact.count({ where: { status: "failed" } }),
      TalkingPhotoArtifact.count({ where: { status: "processing" } }),
      TalkingPhotoArtifact.count({ where: { status: "queued" } }),
    ]);

    const successRate = total > 0 ? ready / total : 0;

    const readyArtifacts = await TalkingPhotoArtifact.findAll({
      where: {
        status: "ready",
        lastProcessingDurationMs: { [Op.ne]: null },
      },
      attributes: ["lastProcessingDurationMs", "provider", "errorCode"],
      limit: 500,
      order: [["updatedAt", "DESC"]],
    });
    const durations = readyArtifacts
      .map((a: any) => Number(a.lastProcessingDurationMs))
      .filter((v) => Number.isFinite(v) && v > 0)
      .sort((a, b) => a - b);
    const medianGenerationTimeMs =
      durations.length === 0
        ? null
        : durations[Math.floor(durations.length / 2)];

    const providerFailureBreakdown = {
      managed: 0,
      self_hosted: 0,
      none: 0,
      unknown: 0,
    };
    const failedArtifacts = await TalkingPhotoArtifact.findAll({
      where: { status: "failed" },
      attributes: ["provider"],
      limit: 500,
      order: [["updatedAt", "DESC"]],
    });
    for (const row of failedArtifacts as any[]) {
      const provider = row.provider;
      if (provider === "managed") providerFailureBreakdown.managed++;
      else if (provider === "self_hosted") providerFailureBreakdown.self_hosted++;
      else if (provider === "none") providerFailureBreakdown.none++;
      else providerFailureBreakdown.unknown++;
    }

    const preprocessSummary = await PosterPreprocessResult.findAll({
      attributes: ["status", "eligibleForTalkingPhoto"],
      limit: 500,
      order: [["updatedAt", "DESC"]],
    });
    const preprocess = {
      total: preprocessSummary.length,
      ready: preprocessSummary.filter((p: any) => p.status === "ready").length,
      failed: preprocessSummary.filter((p: any) => p.status === "failed").length,
      eligible: preprocessSummary.filter((p: any) => !!p.eligibleForTalkingPhoto)
        .length,
    };

    return res.json({
      runtimeMode: runtime.runtimeMode,
      generation: {
        total,
        ready,
        failed,
        queued,
        processing,
        successRate,
        medianGenerationTimeMs,
      },
      queueBacklog: queued + processing,
      providerFailureBreakdown,
      preprocess,
      generatedAt: new Date().toISOString(),
    });
  } catch (error) {
    return next(error);
  }
});

export default router;
