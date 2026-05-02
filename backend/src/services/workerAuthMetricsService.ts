export type WorkerAuthFailureReason =
  | "invalid_token"
  | "missing_signature_headers"
  | "invalid_timestamp"
  | "timestamp_expired"
  | "nonce_replay"
  | "signature_mismatch";

type WorkerAuthMetricsSnapshot = {
  totals: {
    requests: number;
    success: number;
    failure: number;
  };
  failuresByReason: Record<WorkerAuthFailureReason, number>;
  lastFailureAt: string | null;
  recentFailures: Array<{
    at: string;
    reason: WorkerAuthFailureReason;
    correlationId: string | null;
  }>;
};

const MAX_RECENT_FAILURES = 200;
const MAX_RECENT_OUTCOMES = 1000;

export class WorkerAuthMetricsService {
  private static totalRequests = 0;
  private static totalSuccess = 0;
  private static totalFailure = 0;
  private static failuresByReason: Record<WorkerAuthFailureReason, number> = {
    invalid_token: 0,
    missing_signature_headers: 0,
    invalid_timestamp: 0,
    timestamp_expired: 0,
    nonce_replay: 0,
    signature_mismatch: 0,
  };
  private static recentFailures: Array<{
    at: string;
    reason: WorkerAuthFailureReason;
    correlationId: string | null;
  }> = [];
  private static recentOutcomes: Array<{
    at: number;
    success: boolean;
  }> = [];

  private static pushOutcome(success: boolean): void {
    this.recentOutcomes.push({
      at: Date.now(),
      success,
    });
    if (this.recentOutcomes.length > MAX_RECENT_OUTCOMES) {
      this.recentOutcomes = this.recentOutcomes.slice(-MAX_RECENT_OUTCOMES);
    }
  }

  static recordSuccess(): void {
    this.totalRequests++;
    this.totalSuccess++;
    this.pushOutcome(true);
  }

  static recordFailure(
    reason: WorkerAuthFailureReason,
    correlationId: string | null
  ): void {
    this.totalRequests++;
    this.totalFailure++;
    this.pushOutcome(false);
    this.failuresByReason[reason] = (this.failuresByReason[reason] || 0) + 1;
    this.recentFailures.push({
      at: new Date().toISOString(),
      reason,
      correlationId,
    });
    if (this.recentFailures.length > MAX_RECENT_FAILURES) {
      this.recentFailures = this.recentFailures.slice(-MAX_RECENT_FAILURES);
    }
  }

  static getSnapshot(): WorkerAuthMetricsSnapshot {
    const lastFailure = this.recentFailures[this.recentFailures.length - 1];
    return {
      totals: {
        requests: this.totalRequests,
        success: this.totalSuccess,
        failure: this.totalFailure,
      },
      failuresByReason: { ...this.failuresByReason },
      lastFailureAt: lastFailure?.at || null,
      recentFailures: [...this.recentFailures],
    };
  }

  static getHealthStatus(): {
    healthy: boolean;
    evaluatedWindowMinutes: number;
    failureRate: number;
    failureRateThreshold: number;
    minimumRequests: number;
    requestsInWindow: number;
    reasonsOverThreshold: Array<{
      reason: WorkerAuthFailureReason;
      count: number;
      threshold: number;
    }>;
  } {
    const windowMinutes = Number(
      process.env.WORKER_AUTH_HEALTH_WINDOW_MINUTES || 5
    );
    const failureRateThreshold = Number(
      process.env.WORKER_AUTH_FAILURE_RATE_THRESHOLD || 0.25
    );
    const minimumRequests = Number(
      process.env.WORKER_AUTH_HEALTH_MIN_REQUESTS || 20
    );
    const reasonThreshold = Number(
      process.env.WORKER_AUTH_REASON_COUNT_THRESHOLD || 20
    );

    const windowMs = Math.max(1, windowMinutes) * 60 * 1000;
    const cutoff = Date.now() - windowMs;
    const outcomes = this.recentOutcomes.filter((o) => o.at >= cutoff);
    const requestsInWindow = outcomes.length;
    const failuresInWindow = outcomes.filter((o) => !o.success).length;
    const failureRate =
      requestsInWindow > 0 ? failuresInWindow / requestsInWindow : 0;

    const reasonCountsInWindow: Record<WorkerAuthFailureReason, number> = {
      invalid_token: 0,
      missing_signature_headers: 0,
      invalid_timestamp: 0,
      timestamp_expired: 0,
      nonce_replay: 0,
      signature_mismatch: 0,
    };
    for (const entry of this.recentFailures) {
      const at = Date.parse(entry.at);
      if (Number.isFinite(at) && at >= cutoff) {
        reasonCountsInWindow[entry.reason] += 1;
      }
    }
    const reasonsOverThreshold = (
      Object.keys(reasonCountsInWindow) as WorkerAuthFailureReason[]
    )
      .filter((key) => reasonCountsInWindow[key] >= reasonThreshold)
      .map((key) => ({
        reason: key,
        count: reasonCountsInWindow[key],
        threshold: reasonThreshold,
      }));

    const enoughTraffic = requestsInWindow >= minimumRequests;
    const failureRateBreached = enoughTraffic && failureRate > failureRateThreshold;
    const reasonBreached = reasonsOverThreshold.length > 0;
    const healthy = !failureRateBreached && !reasonBreached;

    return {
      healthy,
      evaluatedWindowMinutes: windowMinutes,
      failureRate,
      failureRateThreshold,
      minimumRequests,
      requestsInWindow,
      reasonsOverThreshold,
    };
  }
}
