/**
 * Performance Optimization Test Suite for TalkAR Backend
 * Tests performance targets for the AI pipeline:
 * - Audio start delay ≤ 1.5s
 * - Video render delay ≤ 3s
 * - Total pipeline time ≤ 5s
 */

import request from 'supertest';
import express from 'express';
import { sequelize } from '../src/config/database';
import aiPipelineRoutes from '../src/routes/aiPipeline';
import { PerformanceMetricsService } from '../src/services/performanceMetricsService';

// Create Express app for testing
const app = express();
app.use(express.json());
app.use(express.urlencoded({ extended: true }));
app.use('/api/v1/ai-pipeline', aiPipelineRoutes);

// Error handler
app.use((err: any, req: any, res: any, next: any) => {
  console.error(err);
  res.status(err.status || 500).json({ error: err.message || 'Internal server error' });
});

describe('Performance Optimization Tests', () => {
  // Test products for performance testing
  const testProducts = [
    'iPhone',
    'MacBook',
    'Tesla',
    'Nike Shoes',
    'Samsung TV',
    'Coffee Maker',
    'Smart Watch',
    'Bluetooth Speaker'
  ];

  beforeAll(async () => {
    try {
      // Ensure database is ready
      await sequelize.authenticate();
    } catch (error) {
      console.error('Database setup error:', error);
    }
  });

  afterAll(async () => {
    await sequelize.close();
  });

  describe('Audio Generation Performance', () => {
    it('should generate audio within 1.5 seconds target', async () => {
      const startTime = Date.now();
      
      const response = await request(app)
        .post('/api/v1/ai-pipeline/generate_audio')
        .send({
          text: 'Welcome to our amazing product showcase!',
          language: 'en',
          emotion: 'neutral'
        });

      const endTime = Date.now();
      const duration = endTime - startTime;
      
      expect(response.status).toBe(200);
      expect(response.body).toHaveProperty('success', true);
      expect(response.body).toHaveProperty('audioUrl');
      expect(duration).toBeLessThanOrEqual(1500); // 1.5 seconds
      
      console.log(`Audio generation time: ${duration}ms (target: ≤1500ms)`);
    }, 10000); // 10 second timeout
  });

  describe('Video Generation Performance', () => {
    it('should generate video within 3 seconds target', async () => {
      // First generate audio
      const audioResponse = await request(app)
        .post('/api/v1/ai-pipeline/generate_audio')
        .send({
          text: 'Welcome to our amazing product showcase!',
          language: 'en',
          emotion: 'neutral'
        });

      expect(audioResponse.status).toBe(200);
      expect(audioResponse.body).toHaveProperty('audioUrl');
      
      const startTime = Date.now();
      
      const response = await request(app)
        .post('/api/v1/ai-pipeline/generate_lipsync')
        .send({
          audio_url: audioResponse.body.audioUrl,
          avatar: 'test_avatar.png'
        });

      const endTime = Date.now();
      const duration = endTime - startTime;
      
      expect(response.status).toBe(200);
      expect(response.body).toHaveProperty('success', true);
      expect(response.body).toHaveProperty('videoUrl');
      expect(duration).toBeLessThanOrEqual(3000); // 3 seconds
      
      console.log(`Video generation time: ${duration}ms (target: ≤3000ms)`);
    }, 15000); // 15 second timeout
  });

  describe('End-to-End Pipeline Performance', () => {
    test.each(testProducts)('should complete ad content generation for "%s" within 5 seconds', async (product) => {
      const startTime = Date.now();
      
      const response = await request(app)
        .post('/api/v1/ai-pipeline/generate_ad_content')
        .send({
          product: product
        });

      const endTime = Date.now();
      const duration = endTime - startTime;
      
      expect(response.status).toBe(200);
      expect(response.body).toHaveProperty('success', true);
      expect(response.body).toHaveProperty('script');
      expect(response.body).toHaveProperty('audio_url');
      expect(response.body).toHaveProperty('video_url');
      expect(duration).toBeLessThanOrEqual(5000); // 5 seconds
      
      console.log(`End-to-end pipeline for "${product}" time: ${duration}ms (target: ≤5000ms)`);
    }, 20000); // 20 second timeout

    test.each(testProducts)('should complete streaming ad content generation for "%s" within 5 seconds', async (product) => {
      const startTime = Date.now();
      
      const response = await request(app)
        .post('/api/v1/ai-pipeline/generate_ad_content_streaming')
        .send({
          product: product
        });

      const endTime = Date.now();
      const duration = endTime - startTime;
      
      expect(response.status).toBe(200);
      expect(response.body).toHaveProperty('success', true);
      expect(response.body).toHaveProperty('script');
      expect(response.body).toHaveProperty('audio_url');
      expect(response.body).toHaveProperty('video_url');
      expect(duration).toBeLessThanOrEqual(5000); // 5 seconds
      
      console.log(`Streaming end-to-end pipeline for "${product}" time: ${duration}ms (target: ≤5000ms)`);
    }, 20000); // 20 second timeout
  });

  describe('Performance Metrics Collection', () => {
    it('should collect and track performance metrics', async () => {
      // Generate ad content to create metrics
      const response = await request(app)
        .post('/api/v1/ai-pipeline/generate_ad_content')
        .send({
          product: 'Test Product'
        });

      expect(response.status).toBe(200);
      
      // Get performance summary
      const summaryResponse = await request(app)
        .get('/api/v1/performance')
        .expect(200);

      expect(summaryResponse.body).toHaveProperty('success', true);
      expect(summaryResponse.body).toHaveProperty('summary');
      expect(summaryResponse.body.summary).toHaveProperty('totalRequests');
      expect(summaryResponse.body.summary).toHaveProperty('successfulRequests');
      
      console.log('Performance summary:', JSON.stringify(summaryResponse.body.summary, null, 2));
    }, 15000);

    it('should meet performance targets consistently', async () => {
      // Run multiple requests to test consistency
      const requests: Promise<any>[] = [];
      for (let i = 0; i < 5; i++) {
        requests.push(
          request(app)
            .post('/api/v1/ai-pipeline/generate_ad_content_streaming')
            .send({
              product: `Product ${i + 1}`
            })
        );
      }
      
      const startTime = Date.now();
      const responses = await Promise.all(requests);
      const endTime = Date.now();
      const totalTime = endTime - startTime;
      
      // Check all responses are successful
      responses.forEach(response => {
        expect(response.status).toBe(200);
        expect(response.body).toHaveProperty('success', true);
      });
      
      // Average time should be within target
      const averageTime = totalTime / requests.length;
      expect(averageTime).toBeLessThanOrEqual(5000);
      
      console.log(`Average time for 5 concurrent requests: ${averageTime}ms (target: ≤5000ms)`);
    }, 30000); // 30 second timeout
  });

  describe('Stress Testing', () => {
    it('should handle concurrent requests without significant performance degradation', async () => {
      // Create 10 concurrent requests
      const concurrentRequests = 10;
      const requests: Promise<any>[] = [];
      
      for (let i = 0; i < concurrentRequests; i++) {
        requests.push(
          request(app)
            .post('/api/v1/ai-pipeline/generate_ad_content_streaming')
            .send({
              product: `Concurrent Product ${i + 1}`
            })
        );
      }
      
      const startTime = Date.now();
      const responses = await Promise.all(requests);
      const endTime = Date.now();
      const totalTime = endTime - startTime;
      
      // Check all responses are successful
      responses.forEach(response => {
        expect(response.status).toBe(200);
        expect(response.body).toHaveProperty('success', true);
      });
      
      // Individual request times should still be within target
      const averageTime = totalTime / concurrentRequests;
      expect(averageTime).toBeLessThanOrEqual(7000); // Allow some overhead for concurrent requests
      
      console.log(`Concurrent stress test - ${concurrentRequests} requests completed in ${totalTime}ms (avg: ${averageTime}ms)`);
    }, 60000); // 60 second timeout
  });
});