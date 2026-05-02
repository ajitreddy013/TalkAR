type RuntimeMode = "normal" | "ready_only" | "enqueue_disabled";
type ReleaseProfile = "internal" | "beta" | "ga";

type RuntimeOverrides = {
  disableEnqueue: boolean;
  forceReadyOnly: boolean;
  enableFallback: boolean;
};

type RuntimeAuditEvent = {
  id: string;
  action: string;
  actor: string;
  correlationId: string | null;
  metadata: Record<string, any>;
  at: string;
};

const runtimeOverrides: RuntimeOverrides = {
  disableEnqueue: false,
  forceReadyOnly: false,
  enableFallback: false,
};

const runtimeAuditTrail: RuntimeAuditEvent[] = [];

function pushAudit(
  action: string,
  actor: string,
  correlationId: string | null,
  metadata: Record<string, any>
) {
  runtimeAuditTrail.unshift({
    id: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
    action,
    actor,
    correlationId,
    metadata,
    at: new Date().toISOString(),
  });
  if (runtimeAuditTrail.length > 200) {
    runtimeAuditTrail.length = 200;
  }
}

function profileDefaults(): RuntimeOverrides {
  const profile = getReleaseProfile();
  if (profile === "beta" || profile === "ga") {
    return {
      disableEnqueue: false,
      forceReadyOnly: false,
      enableFallback: true,
    };
  }
  return {
    disableEnqueue: false,
    forceReadyOnly: false,
    enableFallback: false,
  };
}

export function getReleaseProfile(): ReleaseProfile {
  const raw = String(process.env.TALKING_PHOTO_RELEASE_PROFILE || "internal").toLowerCase();
  if (raw === "beta") return "beta";
  if (raw === "ga") return "ga";
  return "internal";
}

export function getEffectiveRuntimeFlags() {
  const defaults = profileDefaults();
  const disableEnqueueFromEnv = process.env.TALKING_PHOTO_DISABLE_ENQUEUE === "true";
  const readyOnlyFromEnv = process.env.TALKING_PHOTO_FORCE_READY_ONLY === "true";
  const fallbackFromEnv = process.env.TALKING_PHOTO_ENABLE_WAV2LIP_FALLBACK === "true";

  const disableEnqueue =
    disableEnqueueFromEnv || runtimeOverrides.disableEnqueue || defaults.disableEnqueue;
  const forceReadyOnly =
    readyOnlyFromEnv || runtimeOverrides.forceReadyOnly || defaults.forceReadyOnly;
  const enableFallback =
    runtimeOverrides.enableFallback || fallbackFromEnv || defaults.enableFallback;

  const runtimeMode: RuntimeMode = disableEnqueue
    ? "enqueue_disabled"
    : forceReadyOnly
      ? "ready_only"
      : "normal";

  return {
    profile: getReleaseProfile(),
    disableEnqueue,
    forceReadyOnly,
    enableFallback,
    runtimeMode,
  };
}

export function setRuntimeOverrides(
  input: Partial<RuntimeOverrides>,
  actor: string,
  correlationId: string | null
) {
  if (typeof input.disableEnqueue === "boolean") {
    runtimeOverrides.disableEnqueue = input.disableEnqueue;
  }
  if (typeof input.forceReadyOnly === "boolean") {
    runtimeOverrides.forceReadyOnly = input.forceReadyOnly;
  }
  if (typeof input.enableFallback === "boolean") {
    runtimeOverrides.enableFallback = input.enableFallback;
  }
  pushAudit("runtime_overrides_updated", actor, correlationId, {
    input,
    effective: getEffectiveRuntimeFlags(),
  });
  return getEffectiveRuntimeFlags();
}

export function getRuntimeAuditTrail() {
  return runtimeAuditTrail;
}

export function logStartupReleasePolicy() {
  const effective = getEffectiveRuntimeFlags();
  console.info("[talking-photo] release-policy", effective);
}

export function validateReleasePolicyAtStartup() {
  const profile = getReleaseProfile();
  if (profile === "internal") return;

  const required = [
    "TALKING_PHOTO_SQS_URL",
    "AWS_REGION",
    "AWS_S3_BUCKET",
    "JWT_SECRET",
  ];

  if (profile === "beta" || profile === "ga") {
    required.push("TALKING_PHOTO_ENABLE_WAV2LIP_FALLBACK");
  }
  if (profile === "ga") {
    required.push(
      "TALKING_PHOTO_QUEUE_BACKLOG_THRESHOLD_WARNING",
      "TALKING_PHOTO_QUEUE_BACKLOG_THRESHOLD_CRITICAL",
      "TALKING_PHOTO_DLQ_BACKLOG_THRESHOLD",
      "TALKING_PHOTO_MAX_MESSAGE_AGE_SECONDS_THRESHOLD"
    );
  }

  const missing = required.filter((k) => {
    const value = process.env[k];
    return typeof value !== "string" || value.trim() === "";
  });

  if (missing.length > 0) {
    throw new Error(
      `[talking-photo] invalid ${profile} startup policy: missing env ${missing.join(", ")}`
    );
  }
}
