/**
 * Performance Metrics Service Test Suite
 * Tests the performance metrics collection and tracking functionality
 */

import { PerformanceMetricsService } from '../src/services/performanceMetricsService';

describe('Performance Metrics Service', () => {
  beforeEach(() => {
    // Clear metrics store before each test
    (PerformanceMetricsService as any).metricsStore.clear();
  });

  describe('Metrics Tracking', () => {
    it('should start tracking a new request', () => {
      const requestId = 'test-request-1';
      const productName = 'Test Product';
      
      PerformanceMetricsService.startTracking(requestId, productName);
      
      const metrics = PerformanceMetricsService.getMetrics(requestId);
      expect(metrics).not.toBeNull();
      expect(metrics?.requestId).toBe(requestId);
      expect(metrics?.productName).toBe(productName);
      expect(metrics?.status).toBe('pending');
    });

    it('should record script generation time', () => {
      const requestId = 'test-request-2';
      PerformanceMetricsService.startTracking(requestId, 'Test Product');
      
      const duration = 150;
      PerformanceMetricsService.recordScriptGeneration(requestId, duration);
      
      const metrics = PerformanceMetricsService.getMetrics(requestId);
      expect(metrics?.scriptGenerationTime).toBe(duration);
      expect(metrics?.status).toBe('script_generated');
    });

    it('should record audio start delay', () => {
      const requestId = 'test-request-3';
      PerformanceMetricsService.startTracking(requestId, 'Test Product');
      
      const startTime = 800;
      PerformanceMetricsService.recordAudioStart(requestId, startTime);
      
      const metrics = PerformanceMetricsService.getMetrics(requestId);
      expect(metrics?.audioStartDelay).toBe(startTime);
      expect(metrics?.status).toBe('audio_started');
    });

    it('should record audio generation time', () => {
      const requestId = 'test-request-4';
      PerformanceMetricsService.startTracking(requestId, 'Test Product');
      
      const duration = 1200;
      PerformanceMetricsService.recordAudioGeneration(requestId, duration);
      
      const metrics = PerformanceMetricsService.getMetrics(requestId);
      expect(metrics?.audioGenerationTime).toBe(duration);
    });

    it('should record video completion', () => {
      const requestId = 'test-request-5';
      PerformanceMetricsService.startTracking(requestId, 'Test Product');
      
      const videoDuration = 2500;
      const totalDuration = 4500;
      PerformanceMetricsService.recordVideoCompletion(requestId, videoDuration, totalDuration);
      
      const metrics = PerformanceMetricsService.getMetrics(requestId);
      expect(metrics?.videoGenerationTime).toBe(videoDuration);
      expect(metrics?.videoRenderDelay).toBe(totalDuration - (metrics?.audioStartDelay || 0));
      expect(metrics?.totalPipelineTime).toBe(totalDuration);
      expect(metrics?.status).toBe('video_completed');
    });

    it('should record failure', () => {
      const requestId = 'test-request-6';
      PerformanceMetricsService.startTracking(requestId, 'Test Product');
      
      const errorMessage = 'Test error occurred';
      PerformanceMetricsService.recordFailure(requestId, errorMessage);
      
      const metrics = PerformanceMetricsService.getMetrics(requestId);
      expect(metrics?.status).toBe('failed');
      expect(metrics?.error).toBe(errorMessage);
    });
  });

  describe('Performance Targets Validation', () => {
    it('should validate audio start delay target', () => {
      const requestId = 'test-request-7';
      PerformanceMetricsService.startTracking(requestId, 'Test Product');
      
      // Record a time that meets the target (≤ 1.5s)
      PerformanceMetricsService.recordAudioStart(requestId, 1200); // 1.2s
      
      const metrics = PerformanceMetricsService.getMetrics(requestId);
      expect(metrics?.audioStartDelay).toBeLessThanOrEqual(1500);
    });

    it('should validate video render delay target', () => {
      const requestId = 'test-request-8';
      PerformanceMetricsService.startTracking(requestId, 'Test Product');
      
      // Record times that meet the target (≤ 3s)
      PerformanceMetricsService.recordAudioStart(requestId, 500); // 0.5s
      PerformanceMetricsService.recordVideoCompletion(requestId, 2000, 2500); // 2.5s total
      
      const metrics = PerformanceMetricsService.getMetrics(requestId);
      expect(metrics?.videoRenderDelay).toBeLessThanOrEqual(3000);
    });

    it('should validate total pipeline time target', () => {
      const requestId = 'test-request-9';
      PerformanceMetricsService.startTracking(requestId, 'Test Product');
      
      // Record times that meet the target (≤ 5s)
      PerformanceMetricsService.recordAudioStart(requestId, 300); // 0.3s
      PerformanceMetricsService.recordVideoCompletion(requestId, 3200, 4500); // 4.5s total
      
      const metrics = PerformanceMetricsService.getMetrics(requestId);
      expect(metrics?.totalPipelineTime).toBeLessThanOrEqual(5000);
    });
  });

  describe('Metrics Summary', () => {
    it('should provide performance summary', () => {
      // Create some test metrics
      PerformanceMetricsService.startTracking('req-1', 'Product 1');
      PerformanceMetricsService.recordAudioStart('req-1', 800);
      PerformanceMetricsService.recordVideoCompletion('req-1', 2000, 3500);
      
      PerformanceMetricsService.startTracking('req-2', 'Product 2');
      PerformanceMetricsService.recordAudioStart('req-2', 1200);
      PerformanceMetricsService.recordVideoCompletion('req-2', 1800, 4200);
      
      PerformanceMetricsService.startTracking('req-3', 'Product 3');
      PerformanceMetricsService.recordFailure('req-3', 'Test error');
      
      const summary = PerformanceMetricsService.getPerformanceSummary();
      
      expect(summary.totalRequests).toBe(3);
      expect(summary.successfulRequests).toBe(2);
      expect(summary.failedRequests).toBe(1);
      expect(summary.averageAudioStartDelay).toBeGreaterThan(0);
      expect(summary.averageVideoRenderDelay).toBeGreaterThan(0);
      expect(summary.averageTotalPipelineTime).toBeGreaterThan(0);
    });

    it('should track targets met vs missed', () => {
      // Create metrics that meet targets
      PerformanceMetricsService.startTracking('good-req', 'Good Product');
      PerformanceMetricsService.recordAudioStart('good-req', 1000); // 1s - meets target
      PerformanceMetricsService.recordVideoCompletion('good-req', 2000, 4000); // 4s - meets target
      
      // Create metrics that miss targets
      PerformanceMetricsService.startTracking('bad-req', 'Bad Product');
      PerformanceMetricsService.recordAudioStart('bad-req', 2000); // 2s - exceeds target
      PerformanceMetricsService.recordVideoCompletion('bad-req', 4000, 6000); // 6s - exceeds target
      
      const summary = PerformanceMetricsService.getPerformanceSummary();
      
      expect(summary.targetsMet.audioStartDelay).toBe(1);
      expect(summary.targetsMissed.audioStartDelay).toBe(1);
      expect(summary.targetsMet.totalPipelineTime).toBe(1);
      expect(summary.targetsMissed.totalPipelineTime).toBe(1);
    });
  });

  describe('Metrics Cleanup', () => {
    it('should cleanup old metrics', () => {
      // Create a metric with old timestamp
      const oldMetric: any = {
        audioStartDelay: 1000,
        videoRenderDelay: 3000,
        totalPipelineTime: 4000,
        scriptGenerationTime: 200,
        audioGenerationTime: 800,
        videoGenerationTime: 2000,
        status: 'video_completed',
        requestId: 'old-request',
        timestamp: new Date(Date.now() - 25 * 60 * 60 * 1000), // 25 hours ago
        productName: 'Old Product'
      };
      
      (PerformanceMetricsService as any).metricsStore.set('old-request', oldMetric);
      
      // Create a recent metric
      PerformanceMetricsService.startTracking('recent-request', 'Recent Product');
      PerformanceMetricsService.recordAudioStart('recent-request', 800);
      PerformanceMetricsService.recordVideoCompletion('recent-request', 2000, 3500);
      
      // Cleanup should remove old metrics
      const cleanedCount = PerformanceMetricsService.cleanupOldMetrics();
      
      expect(cleanedCount).toBe(1);
      expect(PerformanceMetricsService.getMetrics('old-request')).toBeNull();
      expect(PerformanceMetricsService.getMetrics('recent-request')).not.toBeNull();
    });
  });
});