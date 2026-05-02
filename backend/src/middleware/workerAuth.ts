import crypto from "crypto";
import { Request, Response, NextFunction } from "express";
import { reserveWorkerNonce } from "../utils/workerNonceStore";
import {
  WorkerAuthFailureReason,
  WorkerAuthMetricsService,
} from "../services/workerAuthMetricsService";
import logger from "../utils/logger";

type WorkerAuthOptions = {
  authToken?: string;
  signingSecret?: string;
  maxSkewMs?: number;
};

type WorkerAuthResult =
  | { ok: true }
  | {
      ok: false;
      reason: string;
      code: WorkerAuthFailureReason;
    };

function safeTimingEquals(a: string, b: string): boolean {
  const aBuf = Buffer.from(a, "utf8");
  const bBuf = Buffer.from(b, "utf8");
  if (aBuf.length !== bBuf.length) return false;
  return crypto.timingSafeEqual(aBuf, bBuf);
}

export function verifyWorkerRequest(
  req: Request,
  options: WorkerAuthOptions = {}
): Promise<WorkerAuthResult> {
  return verifyWorkerRequestInternal(req, options);
}

async function verifyWorkerRequestInternal(
  req: Request,
  options: WorkerAuthOptions = {}
): Promise<WorkerAuthResult> {
  const authToken = options.authToken ?? process.env.WAV2LIP_WORKER_AUTH_TOKEN;
  const signingSecret =
    options.signingSecret ?? process.env.WAV2LIP_WORKER_SIGNING_SECRET;
  const maxSkewMs = options.maxSkewMs ?? Number(process.env.WAV2LIP_WORKER_MAX_SKEW_MS || 5 * 60 * 1000);

  const requestToken = req.header("x-worker-auth-token");
  if (authToken) {
    if (!requestToken || !safeTimingEquals(requestToken, authToken)) {
      return {
        ok: false,
        reason: "Invalid worker auth token.",
        code: "invalid_token",
      };
    }
  }

  if (signingSecret) {
    const timestamp = req.header("x-worker-timestamp");
    const nonce = req.header("x-worker-nonce");
    const signature = req.header("x-worker-signature");
    if (!timestamp || !nonce || !signature) {
      return {
        ok: false,
        reason: "Missing worker signature headers.",
        code: "missing_signature_headers",
      };
    }

    const timestampNum = Number(timestamp);
    if (!Number.isFinite(timestampNum)) {
      return {
        ok: false,
        reason: "Invalid worker timestamp.",
        code: "invalid_timestamp",
      };
    }

    const age = Math.abs(Date.now() - timestampNum);
    if (age > maxSkewMs) {
      return {
        ok: false,
        reason: "Worker signature timestamp expired.",
        code: "timestamp_expired",
      };
    }

    const nonceKey = `${timestamp}:${nonce}`;
    const reserved = await reserveWorkerNonce(nonceKey, maxSkewMs);
    if (!reserved) {
      return {
        ok: false,
        reason: "Replay detected: nonce already used.",
        code: "nonce_replay",
      };
    }

    const payload = req.body ?? {};
    const payloadJson = JSON.stringify(payload);
    const expectedSignature = crypto
      .createHmac("sha256", signingSecret)
      .update(`${timestamp}.${nonce}.${payloadJson}`)
      .digest("hex");

    if (!safeTimingEquals(signature, expectedSignature)) {
      return {
        ok: false,
        reason: "Worker signature mismatch.",
        code: "signature_mismatch",
      };
    }
  }

  return { ok: true };
}

export function workerAuthMiddleware(options: WorkerAuthOptions = {}) {
  return async (req: Request, res: Response, next: NextFunction) => {
    const correlationId = req.header("x-correlation-id") || null;
    const result = await verifyWorkerRequest(req, options);
    if (!result.ok) {
      WorkerAuthMetricsService.recordFailure(result.code, correlationId);
      logger.warn(
        `[worker-auth] verification failed code=${result.code} correlationId=${correlationId || "none"}`
      );
      return res.status(401).json({
        success: false,
        error: "UNAUTHORIZED_WORKER_REQUEST",
        message: result.reason || "Unauthorized worker request.",
      });
    }
    WorkerAuthMetricsService.recordSuccess();
    return next();
  };
}
