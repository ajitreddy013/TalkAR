import { AnalyticsService } from "./analyticsService";

export interface PerformanceMetrics {
  // Timing metrics
  audioStartDelay: number; // Time from request to audio start
  videoRenderDelay: number; // Time from audio start to video completion
  totalPipelineTime: number; // End-to-end time
  
  // Component timing
  scriptGenerationTime: number;
  audioGenerationTime: number;
  videoGenerationTime: number;
  
  // Status tracking
  status: 'pending' | 'script_generated' | 'audio_started' | 'video_completed' | 'failed';
  error?: string;
  
  // Request metadata
  requestId: string;
  timestamp: Date;
  productName?: string;
}

export interface PerformanceTarget {
  audioStartDelay: number; // ≤ 1.5s
  videoRenderDelay: number; // ≤ 3s
  totalPipelineTime: number; // ≤ 5s
}

export class PerformanceMetricsService {
  private static metricsStore = new Map<string, PerformanceMetrics>();
  private static readonly TARGETS: PerformanceTarget = {
    audioStartDelay: 1500, // 1.5 seconds
    videoRenderDelay: 3000, // 3 seconds
    totalPipelineTime: 5000  // 5 seconds
  };

  /**
   * Start tracking a new request
   */
  static startTracking(requestId: string, productName?: string): void {
    const metrics: PerformanceMetrics = {
      audioStartDelay: 0,
      videoRenderDelay: 0,
      totalPipelineTime: 0,
      scriptGenerationTime: 0,
      audioGenerationTime: 0,
      videoGenerationTime: 0,
      status: 'pending',
      requestId,
      timestamp: new Date(),
      productName
    };
    
    this.metricsStore.set(requestId, metrics);
    console.log(`[PERFORMANCE] Started tracking request ${requestId} for product: ${productName || 'unknown'}`);
  }

  /**
   * Record script generation completion
   */
  static recordScriptGeneration(requestId: string, duration: number): void {
    const metrics = this.metricsStore.get(requestId);
    if (!metrics) return;
    
    metrics.scriptGenerationTime = duration;
    metrics.status = 'script_generated';
    this.metricsStore.set(requestId, metrics);
    
    console.log(`[PERFORMANCE] Script generation completed for ${requestId} in ${duration}ms`);
  }

  /**
   * Record audio generation start (this marks the audio start delay)
   */
  static recordAudioStart(requestId: string, startTime: number): void {
    const metrics = this.metricsStore.get(requestId);
    if (!metrics) return;
    
    metrics.audioStartDelay = startTime;
    metrics.status = 'audio_started';
    this.metricsStore.set(requestId, metrics);
    
    console.log(`[PERFORMANCE] Audio started for ${requestId} at ${startTime}ms`);
    
    // Log to analytics service
    AnalyticsService.logResponseTime(startTime, true);
  }

  /**
   * Record audio generation completion
   */
  static recordAudioGeneration(requestId: string, duration: number): void {
    const metrics = this.metricsStore.get(requestId);
    if (!metrics) return;
    
    metrics.audioGenerationTime = duration;
    this.metricsStore.set(requestId, metrics);
    
    console.log(`[PERFORMANCE] Audio generation completed for ${requestId} in ${duration}ms`);
  }

  /**
   * Record video generation completion (this marks the end of the pipeline)
   */
  static recordVideoCompletion(requestId: string, duration: number, totalDuration: number): void {
    const metrics = this.metricsStore.get(requestId);
    if (!metrics) return;
    
    metrics.videoGenerationTime = duration;
    metrics.videoRenderDelay = totalDuration - metrics.audioStartDelay;
    metrics.totalPipelineTime = totalDuration;
    metrics.status = 'video_completed';
    this.metricsStore.set(requestId, metrics);
    
    console.log(`[PERFORMANCE] Video completed for ${requestId} in ${duration}ms (total: ${totalDuration}ms)`);
    
    // Log to analytics service
    AnalyticsService.logVideoProcessingTime(duration);
    AnalyticsService.logResponseTime(totalDuration, true);
    
    // Check if targets are met
    this.checkPerformanceTargets(metrics);
  }

  /**
   * Record failure
   */
  static recordFailure(requestId: string, error: string): void {
    const metrics = this.metricsStore.get(requestId);
    if (!metrics) return;
    
    metrics.status = 'failed';
    metrics.error = error;
    this.metricsStore.set(requestId, metrics);
    
    console.log(`[PERFORMANCE] Request ${requestId} failed: ${error}`);
    
    // Log to analytics service
    AnalyticsService.logResponseTime(0, false);
  }

  /**
   * Check if performance targets are met
   */
  private static checkPerformanceTargets(metrics: PerformanceMetrics): void {
    const targets = this.TARGETS;
    const issues: string[] = [];
    
    if (metrics.audioStartDelay > targets.audioStartDelay) {
      issues.push(`Audio start delay ${metrics.audioStartDelay}ms exceeds target ${targets.audioStartDelay}ms`);
    }
    
    if (metrics.videoRenderDelay > targets.videoRenderDelay) {
      issues.push(`Video render delay ${metrics.videoRenderDelay}ms exceeds target ${targets.videoRenderDelay}ms`);
    }
    
    if (metrics.totalPipelineTime > targets.totalPipelineTime) {
      issues.push(`Total pipeline time ${metrics.totalPipelineTime}ms exceeds target ${targets.totalPipelineTime}ms`);
    }
    
    if (issues.length > 0) {
      console.warn(`[PERFORMANCE] Targets not met for request ${metrics.requestId}: ${issues.join(', ')}`);
    } else {
      console.log(`[PERFORMANCE] All targets met for request ${metrics.requestId}`);
    }
  }

  /**
   * Get metrics for a specific request
   */
  static getMetrics(requestId: string): PerformanceMetrics | null {
    return this.metricsStore.get(requestId) || null;
  }

  /**
   * Get all metrics
   */
  static getAllMetrics(): PerformanceMetrics[] {
    return Array.from(this.metricsStore.values());
  }

  /**
   * Get performance summary
   */
  static getPerformanceSummary(): {
    totalRequests: number;
    successfulRequests: number;
    failedRequests: number;
    averageAudioStartDelay: number;
    averageVideoRenderDelay: number;
    averageTotalPipelineTime: number;
    targetsMet: {
      audioStartDelay: number;
      videoRenderDelay: number;
      totalPipelineTime: number;
    };
    targetsMissed: {
      audioStartDelay: number;
      videoRenderDelay: number;
      totalPipelineTime: number;
    };
  } {
    const allMetrics = this.getAllMetrics();
    const completedMetrics = allMetrics.filter(m => m.status === 'video_completed');
    const failedMetrics = allMetrics.filter(m => m.status === 'failed');
    
    // Calculate averages
    const avgAudioStartDelay = completedMetrics.length > 0 
      ? completedMetrics.reduce((sum, m) => sum + m.audioStartDelay, 0) / completedMetrics.length
      : 0;
      
    const avgVideoRenderDelay = completedMetrics.length > 0
      ? completedMetrics.reduce((sum, m) => sum + m.videoRenderDelay, 0) / completedMetrics.length
      : 0;
      
    const avgTotalPipelineTime = completedMetrics.length > 0
      ? completedMetrics.reduce((sum, m) => sum + m.totalPipelineTime, 0) / completedMetrics.length
      : 0;
    
    // Count targets met/missed
    const targetsMet = {
      audioStartDelay: completedMetrics.filter(m => m.audioStartDelay <= this.TARGETS.audioStartDelay).length,
      videoRenderDelay: completedMetrics.filter(m => m.videoRenderDelay <= this.TARGETS.videoRenderDelay).length,
      totalPipelineTime: completedMetrics.filter(m => m.totalPipelineTime <= this.TARGETS.totalPipelineTime).length
    };
    
    const targetsMissed = {
      audioStartDelay: completedMetrics.filter(m => m.audioStartDelay > this.TARGETS.audioStartDelay).length,
      videoRenderDelay: completedMetrics.filter(m => m.videoRenderDelay > this.TARGETS.videoRenderDelay).length,
      totalPipelineTime: completedMetrics.filter(m => m.totalPipelineTime > this.TARGETS.totalPipelineTime).length
    };
    
    return {
      totalRequests: allMetrics.length,
      successfulRequests: completedMetrics.length,
      failedRequests: failedMetrics.length,
      averageAudioStartDelay: Math.round(avgAudioStartDelay),
      averageVideoRenderDelay: Math.round(avgVideoRenderDelay),
      averageTotalPipelineTime: Math.round(avgTotalPipelineTime),
      targetsMet,
      targetsMissed
    };
  }

  /**
   * Clean up old metrics (keep last 24 hours)
   */
  static cleanupOldMetrics(): number {
    const now = new Date();
    const oneDayAgo = new Date(now.getTime() - 24 * 60 * 60 * 1000);
    let cleanedCount = 0;

    for (const [id, metrics] of this.metricsStore.entries()) {
      if (metrics.timestamp < oneDayAgo) {
        this.metricsStore.delete(id);
        cleanedCount++;
      }
    }

    if (cleanedCount > 0) {
      console.log(`[PERFORMANCE] Cleaned up ${cleanedCount} old metrics`);
    }

    return cleanedCount;
  }
}

// Cleanup old metrics every 6 hours
setInterval(() => {
  PerformanceMetricsService.cleanupOldMetrics();
}, 6 * 60 * 60 * 1000);