import axios from "axios";
import crypto from "crypto";
import { Image } from "../models/Image";
import { PosterPreprocessResult } from "../models/PosterPreprocessResult";

type FaceBox = { x: number; y: number; width: number; height: number };
type LipRoi = { lipX: number; lipY: number; lipWidth: number; lipHeight: number };

function clamp01(v: number): number {
  return Math.max(0, Math.min(1, v));
}

function buildHeuristicFromImage(imageId: string, imageUrl: string): {
  confidence: number;
  faceBox: FaceBox;
  lipRoi: LipRoi;
} {
  const digest = crypto
    .createHash("sha256")
    .update(`${imageId}:${imageUrl}`)
    .digest();
  const n = (i: number) => digest[i] / 255;
  const faceW = 0.42 + n(0) * 0.14;
  const faceH = 0.5 + n(1) * 0.2;
  const faceX = 0.5 - faceW / 2 + (n(2) - 0.5) * 0.06;
  const faceY = 0.18 + n(3) * 0.14;
  const lipW = 0.18 + n(4) * 0.08;
  const lipH = 0.045 + n(5) * 0.05;
  const lipX = faceX + faceW * (0.5 - lipW * 0.5);
  const lipY = faceY + faceH * (0.62 + n(6) * 0.08);
  const confidence = clamp01(0.78 + n(7) * 0.2);
  return {
    confidence,
    faceBox: {
      x: clamp01(faceX),
      y: clamp01(faceY),
      width: clamp01(faceW),
      height: clamp01(faceH),
    },
    lipRoi: {
      lipX: clamp01(lipX),
      lipY: clamp01(lipY),
      lipWidth: clamp01(lipW),
      lipHeight: clamp01(lipH),
    },
  };
}

export async function preprocessPosterImage(
  imageId: string
): Promise<PosterPreprocessResult> {
  const image = await Image.findByPk(imageId);
  if (!image) {
    throw new Error("Image not found");
  }

  const providerUrl = process.env.POSTER_PREPROCESSOR_URL;
  const minConfidence = Number(process.env.POSTER_PREPROCESS_MIN_CONFIDENCE || 0.75);

  let status: "ready" | "failed" = "failed";
  let provider = "none";
  let faceDetected = false;
  let confidence: number | null = null;
  let faceBox: FaceBox | null = null;
  let lipRoi: LipRoi | null = null;
  let errorCode: string | null = null;
  let errorMessage: string | null = null;

  try {
    if (providerUrl) {
      provider = "external_preprocessor";
      const response = await axios.post(
        `${providerUrl.replace(/\/$/, "")}/analyze`,
        {
          imageId: image.id,
          imageUrl: image.imageUrl,
        },
        {
          timeout: Number(process.env.POSTER_PREPROCESS_TIMEOUT_MS || 15000),
        }
      );
      const data = response.data || {};
      faceDetected = !!data.faceDetected;
      confidence = typeof data.confidence === "number" ? clamp01(data.confidence) : null;
      faceBox = data.faceBox || null;
      lipRoi = data.lipRoi || null;
    } else if (process.env.NODE_ENV !== "production") {
      provider = "heuristic_nonprod";
      const heuristic = buildHeuristicFromImage(image.id, image.imageUrl);
      faceDetected = heuristic.confidence >= minConfidence;
      confidence = heuristic.confidence;
      faceBox = heuristic.faceBox;
      lipRoi = heuristic.lipRoi;
    } else {
      provider = "none";
      errorCode = "PROVIDER_FAILED";
      errorMessage = "Poster preprocessor is not configured in production.";
    }

    if (!errorCode) {
      const eligible =
        faceDetected &&
        confidence !== null &&
        confidence >= minConfidence &&
        !!faceBox &&
        !!lipRoi;
      if (!eligible) {
        status = "failed";
        errorCode = "NO_FACE_IN_POSTER";
        errorMessage = "Poster preprocessing did not detect a usable face/lip ROI.";
      } else {
        status = "ready";
      }
    }
  } catch (error: any) {
    status = "failed";
    errorCode = "PROVIDER_FAILED";
    errorMessage = error?.message || "Poster preprocessing failed";
  }

  await PosterPreprocessResult.upsert({
    imageId: image.id,
    status,
    provider,
    faceDetected,
    confidence,
    eligibleForTalkingPhoto: status === "ready",
    faceBox: faceBox ? JSON.stringify(faceBox) : null,
    lipRoi: lipRoi ? JSON.stringify(lipRoi) : null,
    errorCode,
    errorMessage,
    processedAt: new Date(),
  });

  const result = await PosterPreprocessResult.findOne({
    where: { imageId: image.id },
  });
  if (!result) {
    throw new Error("Failed to persist preprocess result");
  }
  return result;
}

export async function getPosterPreprocessResult(imageId: string) {
  return PosterPreprocessResult.findOne({ where: { imageId } });
}
