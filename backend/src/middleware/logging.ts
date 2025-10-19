import { Request, Response, NextFunction } from "express";
import { v4 as uuidv4 } from "uuid";

export interface RequestLog {
  id: string;
  method: string;
  url: string;
  userAgent?: string;
  ip: string;
  startTime: Date;
  endTime?: Date;
  duration?: number;
  statusCode?: number;
  error?: string;
  requestSize?: number;
  responseSize?: number;
  userId?: string;
  sessionId?: string;
}

// In-memory storage for demo (in production, use database)
const requestLogs = new Map<string, RequestLog>();
const performanceMetrics = {
  totalRequests: 0,
  successfulRequests: 0,
  failedRequests: 0,
  averageResponseTime: 0,
  responseTimes: [] as number[],
  endpointStats: new Map<
    string,
    { count: number; avgTime: number; errors: number }
  >(),
};

/**
 * Logging middleware for performance metrics and debugging
 */
export class LoggingMiddleware {
  /**
   * Main middleware function
   */
  static logRequests(req: Request, res: Response, next: NextFunction): void {
    const startTime = Date.now();
    const requestId = uuidv4();

    // Get request size
    const requestSize = req.headers["content-length"]
      ? parseInt(req.headers["content-length"] as string)
      : undefined;

    // Create request log
    const log: RequestLog = {
      id: requestId,
      method: req.method,
      url: req.originalUrl || req.url,
      userAgent: req.headers["user-agent"],
      ip: req.ip || req.connection.remoteAddress || "unknown",
      startTime: new Date(startTime),
      requestSize,
      userId: (req as any).user?.id,
      sessionId: (req as any).session?.id,
    };

    // Store initial log
    requestLogs.set(requestId, log);

    // Override res.end to capture response data
    const originalEnd = res.end.bind(res);
    const chunks: Buffer[] = [];

    res.end = function (chunk?: any, encoding?: any, callback?: any): Response {
      if (chunk) {
        chunks.push(
          Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk, encoding),
        );
      }

      const endTime = Date.now();
      const duration = endTime - startTime;
      const responseSize = chunks.reduce(
        (total, chunk) => total + chunk.length,
        0,
      );

      // Update log with response data
      log.endTime = new Date(endTime);
      log.duration = duration;
      log.statusCode = res.statusCode;
      log.responseSize = responseSize;

      if (res.statusCode >= 400) {
        log.error = `HTTP ${res.statusCode}`;
      }

      requestLogs.set(requestId, log);

      // Update performance metrics
      LoggingMiddleware.updatePerformanceMetrics(log);

      // Log the request
      LoggingMiddleware.logRequest(log);

      // Call original end and return the response
      return originalEnd(chunk, encoding, callback);
    } as any;

    next();
  }

  /**
   * Update performance metrics
   */
  private static updatePerformanceMetrics(log: RequestLog): void {
    performanceMetrics.totalRequests++;

    if (log.statusCode && log.statusCode < 400) {
      performanceMetrics.successfulRequests++;
    } else {
      performanceMetrics.failedRequests++;
    }

    if (log.duration) {
      performanceMetrics.responseTimes.push(log.duration);

      // Keep only last 1000 response times for memory efficiency
      if (performanceMetrics.responseTimes.length > 1000) {
        performanceMetrics.responseTimes =
          performanceMetrics.responseTimes.slice(-1000);
      }

      // Update average response time
      performanceMetrics.averageResponseTime =
        performanceMetrics.responseTimes.reduce((sum, time) => sum + time, 0) /
        performanceMetrics.responseTimes.length;
    }

    // Update endpoint stats
    const endpoint = `${log.method} ${log.url.split("?")[0]}`;
    const existing = performanceMetrics.endpointStats.get(endpoint) || {
      count: 0,
      avgTime: 0,
      errors: 0,
    };

    existing.count++;
    if (log.duration) {
      existing.avgTime =
        (existing.avgTime * (existing.count - 1) + log.duration) /
        existing.count;
    }
    if (log.statusCode && log.statusCode >= 400) {
      existing.errors++;
    }

    performanceMetrics.endpointStats.set(endpoint, existing);
  }

  /**
   * Log request details
   */
  private static logRequest(log: RequestLog): void {
    const status = log.statusCode || 0;
    const duration = log.duration || 0;
    const method = log.method.padEnd(6);
    const statusStr = status.toString().padStart(3);
    const durationStr = `${duration}ms`.padStart(6);

    let logLevel = "info";
    let emoji = "✅";

    if (status >= 500) {
      logLevel = "error";
      emoji = "🔥";
    } else if (status >= 400) {
      logLevel = "warn";
      emoji = "⚠️";
    } else if (duration > 1000) {
      logLevel = "warn";
      emoji = "🐌";
    }

    const logMessage = `${emoji} ${method} ${log.url} → ${statusStr} (${durationStr})`;

    console.log(`[${logLevel.toUpperCase()}] ${logMessage}`);

    // Log additional details for slow or error requests
    if (duration > 1000 || status >= 400) {
      console.log(
        `[DETAILS] Request ID: ${log.id}, IP: ${log.ip}, User Agent: ${log.userAgent}`,
      );
      if (log.error) {
        console.log(`[ERROR] ${log.error}`);
      }
    }
  }

  /**
   * Get performance metrics
   */
  static getPerformanceMetrics() {
    const now = new Date();
    const oneHourAgo = new Date(now.getTime() - 60 * 60 * 1000);

    // Get recent logs (last hour)
    const recentLogs = Array.from(requestLogs.values())
      .filter((log) => log.startTime > oneHourAgo)
      .sort((a, b) => b.startTime.getTime() - a.startTime.getTime());

    // Get slow requests (duration > 1 second)
    const slowRequests = recentLogs.filter((log) => (log.duration || 0) > 1000);

    // Get error requests (status >= 400)
    const errorRequests = recentLogs.filter(
      (log) => (log.statusCode || 0) >= 400,
    );

    // Get top endpoints by usage
    const topEndpoints = Array.from(performanceMetrics.endpointStats.entries())
      .map(([endpoint, stats]) => ({
        endpoint,
        count: stats.count,
        avgTime: Math.round(stats.avgTime),
        errorRate: Math.round((stats.errors / stats.count) * 100),
      }))
      .sort((a, b) => b.count - a.count)
      .slice(0, 10);

    return {
      summary: {
        totalRequests: performanceMetrics.totalRequests,
        successfulRequests: performanceMetrics.successfulRequests,
        failedRequests: performanceMetrics.failedRequests,
        successRate:
          Math.round(
            (performanceMetrics.successfulRequests /
              performanceMetrics.totalRequests) *
              100,
          ) || 0,
        averageResponseTime: Math.round(performanceMetrics.averageResponseTime),
      },
      recent: {
        totalRequests: recentLogs.length,
        slowRequests: slowRequests.length,
        errorRequests: errorRequests.length,
        averageResponseTime:
          recentLogs.length > 0
            ? Math.round(
                recentLogs.reduce((sum, log) => sum + (log.duration || 0), 0) /
                  recentLogs.length,
              )
            : 0,
      },
      topEndpoints,
      slowRequests: slowRequests.slice(0, 10).map((log) => ({
        id: log.id,
        method: log.method,
        url: log.url,
        duration: log.duration,
        statusCode: log.statusCode,
        timestamp: log.startTime,
      })),
      errorRequests: errorRequests.slice(0, 10).map((log) => ({
        id: log.id,
        method: log.method,
        url: log.url,
        statusCode: log.statusCode,
        error: log.error,
        timestamp: log.startTime,
      })),
    };
  }

  /**
   * Get detailed logs with filtering
   */
  static getLogs(
    filters: {
      method?: string;
      endpoint?: string;
      statusCode?: number;
      minDuration?: number;
      maxDuration?: number;
      startDate?: Date;
      endDate?: Date;
      limit?: number;
    } = {},
  ) {
    let logs = Array.from(requestLogs.values());

    // Apply filters
    if (filters.method) {
      logs = logs.filter(
        (log) => log.method.toLowerCase() === filters.method!.toLowerCase(),
      );
    }

    if (filters.endpoint) {
      logs = logs.filter((log) => log.url.includes(filters.endpoint!));
    }

    if (filters.statusCode) {
      logs = logs.filter((log) => log.statusCode === filters.statusCode);
    }

    if (filters.minDuration !== undefined) {
      logs = logs.filter((log) => (log.duration || 0) >= filters.minDuration!);
    }

    if (filters.maxDuration !== undefined) {
      logs = logs.filter((log) => (log.duration || 0) <= filters.maxDuration!);
    }

    if (filters.startDate) {
      logs = logs.filter((log) => log.startTime >= filters.startDate!);
    }

    if (filters.endDate) {
      logs = logs.filter((log) => log.startTime <= filters.endDate!);
    }

    // Sort by timestamp (newest first)
    logs.sort((a, b) => b.startTime.getTime() - a.startTime.getTime());

    // Apply limit
    const limit = filters.limit || 100;
    logs = logs.slice(0, limit);

    return logs;
  }

  /**
   * Clean up old logs (keep last 24 hours)
   */
  static cleanupOldLogs(): number {
    const now = new Date();
    const oneDayAgo = new Date(now.getTime() - 24 * 60 * 60 * 1000);

    let cleanedCount = 0;

    for (const [id, log] of requestLogs.entries()) {
      if (log.startTime < oneDayAgo) {
        requestLogs.delete(id);
        cleanedCount++;
      }
    }

    if (cleanedCount > 0) {
      console.log(`[LOGGING] Cleaned up ${cleanedCount} old request logs`);
    }

    return cleanedCount;
  }
}

// Cleanup old logs every 6 hours
setInterval(
  () => {
    LoggingMiddleware.cleanupOldLogs();
  },
  6 * 60 * 60 * 1000,
);

export default LoggingMiddleware;
