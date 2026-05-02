import { api } from "./api";

export interface Image {
  id: string;
  name: string;
  description?: string;
  imageUrl: string;
  thumbnailUrl?: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
  dialogues: Dialogue[];
  talkingPhotoArtifact?: TalkingPhotoArtifact;
  preprocessResult?: PosterPreprocessResult;
}

export interface TalkingPhotoArtifact {
  status: "queued" | "processing" | "ready" | "failed";
  version: number;
  errorCode?: string | null;
  errorMessage?: string | null;
  confidence?: number | null;
  provider?: string | null;
  stageTimings?: Record<string, number> | null;
  lastProcessingDurationMs?: number | null;
  lastCorrelationId?: string | null;
  updatedAt?: string;
}

export interface PosterPreprocessResult {
  status: "pending" | "ready" | "failed";
  provider: string;
  faceDetected: boolean;
  confidence?: number | null;
  eligibleForTalkingPhoto: boolean;
  errorCode?: string | null;
  errorMessage?: string | null;
  updatedAt?: string;
}

export interface Dialogue {
  id: string;
  imageId: string;
  text: string;
  language: string;
  voiceId?: string;
  isDefault: boolean;
  createdAt: string;
  updatedAt: string;
}

export type ArtifactStatus = "queued" | "processing" | "ready" | "failed";
export type ImageSortBy = "name" | "createdAt" | "updatedAt" | "confidence" | "artifactStatus";
export type SortDirection = "asc" | "desc";

export interface ImageListQuery {
  search?: string;
  status?: ArtifactStatus;
  sortBy?: ImageSortBy;
  sortDir?: SortDirection;
  page?: number;
  pageSize?: number;
  confidenceMin?: number;
  confidenceMax?: number;
}

export interface PosterOpsMetrics {
  generation: {
    total: number;
    ready: number;
    failed: number;
    queued: number;
    processing: number;
    successRate: number;
    medianGenerationTimeMs: number | null;
  };
  queueBacklog: number;
  providerFailureBreakdown: {
    managed: number;
    self_hosted: number;
    none: number;
    unknown: number;
  };
  preprocess: {
    total: number;
    ready: number;
    failed: number;
    eligible: number;
  };
  generatedAt: string;
}

export interface PosterOpsAlert {
  key: string;
  severity: "info" | "warning" | "error";
  active: boolean;
  message: string;
}

export interface PosterOpsAlertsResponse {
  profile: "internal" | "beta";
  runtimeMode: "normal" | "ready_only" | "enqueue_disabled";
  alerts: PosterOpsAlert[];
  metrics: {
    total: number;
    failed: number;
    failedRatio: number;
    queueBacklog: number;
    providerFailures: {
      managed: number;
      self_hosted: number;
      none: number;
      unknown: number;
    };
  };
  generatedAt: string;
}

export const ImageService = {
  getAllImages: () => api.get("/api/v1/images"),

  listImages: (query: ImageListQuery = {}) => {
    const params = new URLSearchParams();
    if (query.search) params.set("search", query.search);
    if (query.status) params.set("status", query.status);
    if (query.sortBy) params.set("sortBy", query.sortBy);
    if (query.sortDir) params.set("sortDir", query.sortDir);
    if (typeof query.page === "number") params.set("page", String(query.page));
    if (typeof query.pageSize === "number") params.set("pageSize", String(query.pageSize));
    if (typeof query.confidenceMin === "number") params.set("confidenceMin", String(query.confidenceMin));
    if (typeof query.confidenceMax === "number") params.set("confidenceMax", String(query.confidenceMax));

    const queryString = params.toString();
    return api.get(`/api/v1/images${queryString ? `?${queryString}` : ""}`);
  },

  getImageById: (id: string) => api.get(`/api/v1/images/${id}`),

  // Create new image with multi-file support and script
  createImage: (imageData: FormData) => api.post("/api/v1/images", imageData),

  // Update image with multi-file support and metadata
  updateImage: (id: string, data: FormData | Partial<Image>) =>
    api.put(`/api/v1/images/${id}`, data),

  deleteImage: (id: string) => api.delete(`/api/v1/images/${id}`),

  addDialogue: (
    imageId: string,
    dialogueData: Omit<Dialogue, "id" | "imageId" | "createdAt" | "updatedAt">
  ) => api.post(`/api/v1/images/${imageId}/dialogues`, dialogueData),

  updateDialogue: (
    imageId: string,
    dialogueId: string,
    data: Partial<Dialogue>
  ) => api.put(`/api/v1/images/${imageId}/dialogues/${dialogueId}`, data),

  deleteDialogue: (imageId: string, dialogueId: string) =>
    api.delete(`/api/v1/images/${imageId}/dialogues/${dialogueId}`),

  retryTalkingPhoto: (imageId: string) =>
    api.post(
      `/api/v1/posters/${imageId}/talking-photo/retry`,
      {
        source: "admin_dashboard",
        reason: "manual_retry",
      },
      {
        headers: {
          "x-idempotency-key": `admin-retry-${imageId}-${Date.now()}`,
        },
      }
    ),

  bulkRetryTalkingPhoto: async (imageIds: string[]) => {
    await Promise.all(
      imageIds.map((imageId) =>
        api.post(`/api/v1/posters/${imageId}/talking-photo/retry`, {
          source: "admin_dashboard_bulk",
          reason: "bulk_retry",
        })
      )
    );
  },

  setActive: (imageId: string, isActive: boolean) =>
    api.put(`/api/v1/images/${imageId}`, { isActive }),

  getPosterOpsMetrics: () => api.get<PosterOpsMetrics>("/api/v1/posters/ops/metrics"),
  getPosterOpsAlerts: () => api.get<PosterOpsAlertsResponse>("/api/v1/posters/ops/alerts"),
  getPosterOpsToggles: () => api.get("/api/v1/posters/ops/toggles"),
  setPosterOpsToggles: (input: {
    disableEnqueue?: boolean;
    forceReadyOnly?: boolean;
    enableFallback?: boolean;
    actor?: string;
  }) => api.post("/api/v1/posters/ops/toggles", input),
};
