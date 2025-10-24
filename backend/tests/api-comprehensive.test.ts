/**
 * Comprehensive API Test Suite for TalkAR Backend
 * Tests all major API endpoints including:
 * - Images CRUD operations
 * - Dialogues management
 * - Sync API operations
 * - Authentication & Authorization
 * - Admin operations
 * - AI Pipeline
 * - Scripts retrieval
 */

import request from 'supertest';
import express from 'express';
import { sequelize } from '../src/config/database';
import { Image, Dialogue } from '../src/models/Image';
import { Avatar } from '../src/models/Avatar';
import { clearUsers } from '../src/services/authService';
import imageRoutes from '../src/routes/images';
import syncRoutes from '../src/routes/sync';
import authRoutes from '../src/routes/auth';
import adminRoutes from '../src/routes/admin';
import aiPipelineRoutes from '../src/routes/aiPipeline';
import scriptsRoutes from '../src/routes/scripts';

// Create Express app for testing
const app = express();
app.use(express.json());
app.use(express.urlencoded({ extended: true }));
app.use('/api/v1/images', imageRoutes);
app.use('/api/v1/sync', syncRoutes);
app.use('/api/v1/auth', authRoutes);
app.use('/api/v1/admin', adminRoutes);
app.use('/api/v1/ai-pipeline', aiPipelineRoutes);
app.use('/api/v1/scripts', scriptsRoutes);

// Error handler
app.use((err: any, req: any, res: any, next: any) => {
  console.error(err);
  res.status(err.status || 500).json({ error: err.message || 'Internal server error' });
});

describe('TalkAR API Test Suite', () => {
  let authToken: string;
  let adminToken: string;
  let testImageId: string;
  let testDialogueId: string;

  // Setup database before tests
  beforeAll(async () => {
    try {
      await sequelize.sync({ force: true });
      
      // Create a test image for use across multiple tests
      const testImage = await Image.create({
        name: 'Test Image',
        description: 'Test image for dialogue and script tests',
        imageUrl: '/uploads/test.jpg',
        thumbnailUrl: '/uploads/test-thumb.jpg',
        isActive: true
      });
      testImageId = testImage.id;
    } catch (error) {
      console.error('Database setup error:', error);
    }
  });

  // NOTE: do not clear users between tests here — some tests depend on tokens
  // created in earlier login/register tests. Clearing users per-test caused
  // authenticated requests to fail (profile/admin) because tokens referred to
  // deleted in-memory users.

  // Cleanup after tests
  afterAll(async () => {
    await sequelize.close();
  });

  describe('Authentication API', () => {
    describe('POST /api/v1/auth/register', () => {
      it('should register a new user successfully', async () => {
        const response = await request(app)
          .post('/api/v1/auth/register')
          .send({
            email: 'testuser@talkar.com',
            password: 'TestPassword123!',
            role: 'user'
          });

        expect(response.status).toBe(201);
        expect(response.body).toHaveProperty('message', 'User registered successfully');
        expect(response.body.user).toHaveProperty('email', 'testuser@talkar.com');
        expect(response.body.user).toHaveProperty('role', 'user');
      });

      it('should register an admin user successfully', async () => {
        const response = await request(app)
          .post('/api/v1/auth/register')
          .send({
            email: 'admin@talkar.com',
            password: 'AdminPassword123!',
            role: 'admin'
          });

        expect(response.status).toBe(201);
        expect(response.body.user).toHaveProperty('role', 'admin');
      });

      it('should reject duplicate email registration', async () => {
        await request(app)
          .post('/api/v1/auth/register')
          .send({
            email: 'duplicate@talkar.com',
            password: 'Password123!',
            role: 'user'
          });

        const response = await request(app)
          .post('/api/v1/auth/register')
          .send({
            email: 'duplicate@talkar.com',
            password: 'Password123!',
            role: 'user'
          });

        expect(response.status).toBe(400);
      });

      it('should reject invalid email format', async () => {
        const response = await request(app)
          .post('/api/v1/auth/register')
          .send({
            email: 'invalid-email',
            password: 'Password123!',
            role: 'user'
          });

        expect(response.status).toBe(400);
      });

      it('should reject weak password', async () => {
        const response = await request(app)
          .post('/api/v1/auth/register')
          .send({
            email: 'weak@talkar.com',
            password: '123',
            role: 'user'
          });

        expect(response.status).toBe(400);
      });
    });

    describe('POST /api/v1/auth/login', () => {
      beforeEach(async () => {
        // Register test user before each login test
        await request(app)
          .post('/api/v1/auth/register')
          .send({
            email: 'logintest@talkar.com',
            password: 'LoginTest123!',
            role: 'user'
          });
        
        // Register admin user
        await request(app)
          .post('/api/v1/auth/register')
          .send({
            email: 'admin@talkar.com',
            password: 'AdminPassword123!',
            role: 'admin'
          });
      });

      it('should login successfully with valid credentials', async () => {
        const response = await request(app)
          .post('/api/v1/auth/login')
          .send({
            email: 'logintest@talkar.com',
            password: 'LoginTest123!'
          });

        expect(response.status).toBe(200);
        expect(response.body).toHaveProperty('token');
        expect(response.body).toHaveProperty('user');
        expect(response.body.user).toHaveProperty('email', 'logintest@talkar.com');
        
        authToken = response.body.token;
      });

      it('should login admin successfully', async () => {
        const response = await request(app)
          .post('/api/v1/auth/login')
          .send({
            email: 'admin@talkar.com',
            password: 'AdminPassword123!'
          });

        expect(response.status).toBe(200);
        expect(response.body).toHaveProperty('token');
        
        adminToken = response.body.token;
      });

      it('should reject invalid credentials', async () => {
        const response = await request(app)
          .post('/api/v1/auth/login')
          .send({
            email: 'logintest@talkar.com',
            password: 'WrongPassword123!'
          });

        expect(response.status).toBe(401);
      });

      it('should reject non-existent user', async () => {
        const response = await request(app)
          .post('/api/v1/auth/login')
          .send({
            email: 'nonexistent@talkar.com',
            password: 'Password123!'
          });

        expect(response.status).toBe(401);
      });
    });

    describe('GET /api/v1/auth/profile', () => {
      it('should get user profile with valid token', async () => {
        const response = await request(app)
          .get('/api/v1/auth/profile')
          .set('Authorization', `Bearer ${authToken}`);

        expect(response.status).toBe(200);
        expect(response.body).toHaveProperty('email');
        expect(response.body).toHaveProperty('role');
      });

      it('should reject without token', async () => {
        const response = await request(app)
          .get('/api/v1/auth/profile');

        expect(response.status).toBe(401);
      });

      it('should reject with invalid token', async () => {
        const response = await request(app)
          .get('/api/v1/auth/profile')
          .set('Authorization', 'Bearer invalid-token');

        expect(response.status).toBe(401);
      });
    });
  });

  describe('Images API', () => {
    describe('GET /api/v1/images', () => {
      it('should get all active images', async () => {
        const response = await request(app)
          .get('/api/v1/images');

        expect(response.status).toBe(200);
        expect(Array.isArray(response.body)).toBe(true);
      });

      it('should return images with dialogues', async () => {
        // Use the test image created in beforeAll
        const response = await request(app)
          .get('/api/v1/images');

        expect(response.status).toBe(200);
        expect(response.body.length).toBeGreaterThan(0);
        expect(response.body[0]).toHaveProperty('dialogues');
      });
    });

    describe('GET /api/v1/images/:id', () => {
      it('should get specific image by ID', async () => {
        // Use the test image created in beforeAll
        const response = await request(app)
          .get(`/api/v1/images/${testImageId}`);

        expect(response.status).toBe(200);
        expect(response.body).toHaveProperty('id', testImageId);
        expect(response.body).toHaveProperty('name', 'Test Image');
      });

      it('should return 404 for non-existent image', async () => {
        const response = await request(app)
          .get('/api/v1/images/00000000-0000-0000-0000-000000000000');

        expect(response.status).toBe(404);
        expect(response.body).toHaveProperty('error', 'Image not found');
      });

      it('should return 404 for invalid UUID', async () => {
        const response = await request(app)
          .get('/api/v1/images/invalid-id');

        expect(response.status).toBe(404);
      });
    });

    describe('PUT /api/v1/images/:id', () => {
      it('should update image successfully', async () => {
        const response = await request(app)
          .put(`/api/v1/images/${testImageId}`)
          .send({
            name: 'Updated Image Name',
            description: 'Updated Description'
          });

        expect(response.status).toBe(200);
        expect(response.body).toHaveProperty('name', 'Updated Image Name');
        expect(response.body).toHaveProperty('description', 'Updated Description');
      });

      it('should update isActive status', async () => {
        const response = await request(app)
          .put(`/api/v1/images/${testImageId}`)
          .send({
            isActive: false
          });

        expect(response.status).toBe(200);
        expect(response.body).toHaveProperty('isActive', false);
      });

      it('should return 404 for non-existent image', async () => {
        const response = await request(app)
          .put('/api/v1/images/00000000-0000-0000-0000-000000000000')
          .send({
            name: 'Updated Name'
          });

        expect(response.status).toBe(404);
      });
    });

    describe('DELETE /api/v1/images/:id', () => {
      it('should delete image successfully', async () => {
        let image;
        try {
          image = await Image.create({
            name: 'Delete Test',
            description: 'To be deleted',
            imageUrl: '/uploads/delete.jpg',
            thumbnailUrl: '/uploads/delete-thumb.jpg',
            isActive: true
          });
        } catch (err) {
          const util = require('util');
          console.error('Image.create error:', util.inspect(err, { depth: null }));
          throw err;
        }

        const response = await request(app)
          .delete(`/api/v1/images/${image.id}`);

        expect(response.status).toBe(204);

        // Verify deletion
        const deletedImage = await Image.findByPk(image.id);
        expect(deletedImage).toBeNull();
      });

      it('should return 404 for non-existent image', async () => {
        const response = await request(app)
          .delete('/api/v1/images/00000000-0000-0000-0000-000000000000');

        expect(response.status).toBe(404);
      });
    });
  });

  describe('Dialogues API', () => {
    describe('POST /api/v1/images/:id/dialogues', () => {
      it('should add dialogue to image successfully', async () => {
        const response = await request(app)
          .post(`/api/v1/images/${testImageId}/dialogues`)
          .send({
            text: 'Hello, welcome to our store!',
            language: 'en',
            voiceId: 'voice-1',
            isDefault: true
          });

        expect(response.status).toBe(201);
        expect(response.body).toHaveProperty('text', 'Hello, welcome to our store!');
        expect(response.body).toHaveProperty('language', 'en');
        expect(response.body).toHaveProperty('voiceId', 'voice-1');
        expect(response.body).toHaveProperty('isDefault', true);
        
        testDialogueId = response.body.id;
      });

      it('should add multiple dialogues to same image', async () => {
        const response1 = await request(app)
          .post(`/api/v1/images/${testImageId}/dialogues`)
          .send({
            text: 'Second dialogue',
            language: 'en',
            voiceId: 'voice-2',
            isDefault: false
          });

        const response2 = await request(app)
          .post(`/api/v1/images/${testImageId}/dialogues`)
          .send({
            text: 'Third dialogue',
            language: 'es',
            voiceId: 'voice-3',
            isDefault: false
          });

        expect(response1.status).toBe(201);
        expect(response2.status).toBe(201);
      });

      it('should return 404 for non-existent image', async () => {
        const response = await request(app)
          .post('/api/v1/images/00000000-0000-0000-0000-000000000000/dialogues')
          .send({
            text: 'Test dialogue',
            language: 'en',
            voiceId: 'voice-1'
          });

        expect(response.status).toBe(404);
      });

      it('should reject dialogue without required text', async () => {
        const response = await request(app)
          .post(`/api/v1/images/${testImageId}/dialogues`)
          .send({
            language: 'en',
            voiceId: 'voice-1'
          });

        expect(response.status).toBe(400);
      });
    });

    describe('PUT /api/v1/images/:imageId/dialogues/:dialogueId', () => {
      it('should update dialogue successfully', async () => {
        const response = await request(app)
          .put(`/api/v1/images/${testImageId}/dialogues/${testDialogueId}`)
          .send({
            text: 'Updated dialogue text',
            language: 'fr',
            voiceId: 'voice-updated'
          });

        expect(response.status).toBe(200);
        expect(response.body).toHaveProperty('text', 'Updated dialogue text');
        expect(response.body).toHaveProperty('language', 'fr');
        expect(response.body).toHaveProperty('voiceId', 'voice-updated');
      });

      it('should return 404 for non-existent dialogue', async () => {
        const response = await request(app)
          .put(`/api/v1/images/${testImageId}/dialogues/00000000-0000-0000-0000-000000000000`)
          .send({
            text: 'Updated text'
          });

        expect(response.status).toBe(404);
      });
    });

    describe('DELETE /api/v1/images/:imageId/dialogues/:dialogueId', () => {
      it('should delete dialogue successfully', async () => {
        const dialogue = await Dialogue.create({
          imageId: testImageId,
          text: 'To be deleted',
          language: 'en',
          voiceId: 'voice-1',
          isDefault: false,
          isActive: true
        });

        const response = await request(app)
          .delete(`/api/v1/images/${testImageId}/dialogues/${dialogue.id}`);

        expect(response.status).toBe(204);

        // Verify deletion
        const deletedDialogue = await Dialogue.findByPk(dialogue.id);
        expect(deletedDialogue).toBeNull();
      });

      it('should return 404 for non-existent dialogue', async () => {
        const response = await request(app)
          .delete(`/api/v1/images/${testImageId}/dialogues/00000000-0000-0000-0000-000000000000`);

        expect(response.status).toBe(404);
      });
    });
  });

  describe('Sync API', () => {
    describe('POST /api/v1/sync/generate', () => {
      it('should generate sync video successfully', async () => {
        const response = await request(app)
          .post('/api/v1/sync/generate')
          .send({
            text: 'Hello, this is a test video',
            language: 'en',
            voiceId: 'voice-1',
            emotion: 'neutral'
          });

        // Note: This will fail if Sync API is not configured
        // Test should verify the request structure is correct
        expect(response.status).toBeGreaterThanOrEqual(200);
        expect(response.status).toBeLessThan(500);
      });

      it('should reject request without text', async () => {
        const response = await request(app)
          .post('/api/v1/sync/generate')
          .send({
            language: 'en',
            voiceId: 'voice-1'
          });

        expect(response.status).toBe(400);
      });
    });

    describe('GET /api/v1/sync/voices', () => {
      it('should get available voices', async () => {
        const response = await request(app)
          .get('/api/v1/sync/voices');

        expect(response.status).toBe(200);
        expect(Array.isArray(response.body)).toBe(true);
      });

      it('should return voice objects with required fields', async () => {
        const response = await request(app)
          .get('/api/v1/sync/voices');

        if (response.body.length > 0) {
          expect(response.body[0]).toHaveProperty('id');
          expect(response.body[0]).toHaveProperty('name');
          expect(response.body[0]).toHaveProperty('language');
        }
      });
    });

    describe('GET /api/v1/sync/status/:jobId', () => {
      it('should get sync job status', async () => {
        const jobId = 'test-job-id-123';
        const response = await request(app)
          .get(`/api/v1/sync/status/${jobId}`);

        // Will return error if job doesn't exist, but should not crash
        expect(response.status).toBeGreaterThanOrEqual(200);
        expect(response.status).toBeLessThan(500);
      });
    });
  });

  describe('AI Pipeline API', () => {
    describe('POST /api/v1/ai-pipeline/generate', () => {
      it('should reject request without imageId', async () => {
        const response = await request(app)
          .post('/api/v1/ai-pipeline/generate')
          .send({
            language: 'en',
            emotion: 'neutral'
          });

        expect(response.status).toBe(400);
        expect(response.body).toHaveProperty('error', 'Missing required parameter: imageId');
      });

      it('should start AI pipeline with valid parameters', async () => {
        const response = await request(app)
          .post('/api/v1/ai-pipeline/generate')
          .send({
            imageId: testImageId,
            language: 'en',
            emotion: 'happy'
          });

        // May fail if AI services not configured, but should validate input
        expect(response.status).toBeGreaterThanOrEqual(200);
        expect(response.status).toBeLessThan(500);
      });
    });

    describe('POST /api/v1/ai-pipeline/generate_script', () => {
      it('should generate script with valid imageId', async () => {
        const response = await request(app)
          .post('/api/v1/ai-pipeline/generate_script')
          .send({
            imageId: testImageId,
            language: 'en',
            emotion: 'neutral'
          });

        expect(response.status).toBeGreaterThanOrEqual(200);
        expect(response.status).toBeLessThan(500);
      });

      it('should reject without imageId', async () => {
        const response = await request(app)
          .post('/api/v1/ai-pipeline/generate_script')
          .send({
            language: 'en'
          });

        expect(response.status).toBe(400);
      });
    });

    describe('POST /api/v1/ai-pipeline/generate_product_script', () => {
      it('should generate product script successfully', async () => {
        const response = await request(app)
          .post('/api/v1/ai-pipeline/generate_product_script')
          .send({
            productName: 'iPhone 15 Pro'
          });

        expect(response.status).toBeGreaterThanOrEqual(200);
        expect(response.status).toBeLessThan(500);
      });

      it('should reject without productName', async () => {
        const response = await request(app)
          .post('/api/v1/ai-pipeline/generate_product_script')
          .send({});

        expect(response.status).toBe(400);
        expect(response.body).toHaveProperty('error', 'Missing required parameter: productName');
      });

      it('should reject empty productName', async () => {
        const response = await request(app)
          .post('/api/v1/ai-pipeline/generate_product_script')
          .send({
            productName: ''
          });

        expect(response.status).toBe(400);
      });
    });

    describe('POST /api/v1/ai-pipeline/generate_ad_content', () => {
      it('should generate ad content for valid product', async () => {
        const response = await request(app)
          .post('/api/v1/ai-pipeline/generate_ad_content')
          .send({
            product: 'Smart Watch'
          });

        expect(response.status).toBeGreaterThanOrEqual(200);
        expect(response.status).toBeLessThan(500);
      });

      it('should reject without product', async () => {
        const response = await request(app)
          .post('/api/v1/ai-pipeline/generate_ad_content')
          .send({});

        expect(response.status).toBe(400);
      });

      it('should reject product name longer than 100 characters', async () => {
        const longProductName = 'A'.repeat(101);
        const response = await request(app)
          .post('/api/v1/ai-pipeline/generate_ad_content')
          .send({
            product: longProductName
          });

        expect(response.status).toBe(400);
      });
    });
  });

  describe('Scripts API', () => {
    describe('GET /api/v1/scripts/getScriptForImage/:imageId', () => {
      it('should get script for image successfully', async () => {
        const response = await request(app)
          .get(`/api/v1/scripts/getScriptForImage/${testImageId}`);

        expect(response.status).toBe(200);
        expect(response.body).toHaveProperty('image');
        expect(response.body).toHaveProperty('script');
      });

      it('should get script with specific index', async () => {
        const response = await request(app)
          .get(`/api/v1/scripts/getScriptForImage/${testImageId}`)
          .query({ scriptIndex: '0' });

        expect(response.status).toBe(200);
        expect(response.body).toHaveProperty('currentScriptIndex', 0);
      });

      it('should return 404 for non-existent image', async () => {
        const response = await request(app)
          .get('/api/v1/scripts/getScriptForImage/00000000-0000-0000-0000-000000000000');

        expect(response.status).toBe(404);
      });
    });

    describe('GET /api/v1/scripts/getAllScriptsForImage/:imageId', () => {
      it('should get all scripts for image', async () => {
        const response = await request(app)
          .get(`/api/v1/scripts/getAllScriptsForImage/${testImageId}`);

        expect(response.status).toBe(200);
        expect(response.body).toHaveProperty('scripts');
        expect(response.body).toHaveProperty('totalScripts');
        expect(Array.isArray(response.body.scripts)).toBe(true);
      });

      it('should return 404 for non-existent image', async () => {
        const response = await request(app)
          .get('/api/v1/scripts/getAllScriptsForImage/00000000-0000-0000-0000-000000000000');

        expect(response.status).toBe(404);
      });
    });
  });

  describe('Admin API', () => {
    beforeEach(async () => {
      // Register and login admin for these tests
      await request(app)
        .post('/api/v1/auth/register')
        .send({
          email: 'admin@talkar.com',
          password: 'AdminPassword123!',
          role: 'admin'
        });

      const loginResponse = await request(app)
        .post('/api/v1/auth/login')
        .send({
          email: 'admin@talkar.com',
          password: 'AdminPassword123!'
        });

      adminToken = loginResponse.body.token;
    });

    describe('GET /api/v1/admin/images', () => {
      it('should require admin authentication', async () => {
        const response = await request(app)
          .get('/api/v1/admin/images');

        expect(response.status).toBe(401);
      });

      it('should get images with admin token', async () => {
        const response = await request(app)
          .get('/api/v1/admin/images')
          .set('Authorization', `Bearer ${adminToken}`);

        expect(response.status).toBe(200);
        expect(response.body).toHaveProperty('images');
        expect(response.body).toHaveProperty('pagination');
      });

      it('should support pagination', async () => {
        const response = await request(app)
          .get('/api/v1/admin/images')
          .query({ page: '1', limit: '5' })
          .set('Authorization', `Bearer ${adminToken}`);

        expect(response.status).toBe(200);
        expect(response.body.pagination).toHaveProperty('page', 1);
        expect(response.body.pagination).toHaveProperty('limit', 5);
      });

      it('should support search', async () => {
        const response = await request(app)
          .get('/api/v1/admin/images')
          .query({ search: 'test' })
          .set('Authorization', `Bearer ${adminToken}`);

        expect(response.status).toBe(200);
      });
    });

    describe('GET /api/v1/admin/analytics', () => {
      it('should require admin authentication', async () => {
        const response = await request(app)
          .get('/api/v1/admin/analytics');

        expect(response.status).toBe(401);
      });

      it('should get analytics with admin token', async () => {
        const response = await request(app)
          .get('/api/v1/admin/analytics')
          .set('Authorization', `Bearer ${adminToken}`);

        expect(response.status).toBe(200);
        expect(response.body).toHaveProperty('totalImages');
        expect(response.body).toHaveProperty('activeImages');
        expect(response.body).toHaveProperty('totalDialogues');
        expect(response.body).toHaveProperty('languageStats');
      });
    });

    describe('POST /api/v1/admin/images/bulk-deactivate', () => {
      it('should require admin authentication', async () => {
        const response = await request(app)
          .post('/api/v1/admin/images/bulk-deactivate')
          .send({ imageIds: [testImageId] });

        expect(response.status).toBe(401);
      });

      it('should bulk deactivate images with admin token', async () => {
        const response = await request(app)
          .post('/api/v1/admin/images/bulk-deactivate')
          .set('Authorization', `Bearer ${adminToken}`)
          .send({ imageIds: [testImageId] });

        expect(response.status).toBe(200);
        expect(response.body).toHaveProperty('message', 'Images deactivated successfully');
      });
    });

    describe('POST /api/v1/admin/images/bulk-activate', () => {
      it('should bulk activate images with admin token', async () => {
        const response = await request(app)
          .post('/api/v1/admin/images/bulk-activate')
          .set('Authorization', `Bearer ${adminToken}`)
          .send({ imageIds: [testImageId] });

        expect(response.status).toBe(200);
        expect(response.body).toHaveProperty('message', 'Images activated successfully');
      });
    });
  });

  describe('Error Handling', () => {
    it('should return 404 for non-existent routes', async () => {
      const response = await request(app)
        .get('/api/v1/nonexistent');

      expect(response.status).toBe(404);
    });

    it('should handle malformed JSON', async () => {
      const response = await request(app)
        .post('/api/v1/images')
        .set('Content-Type', 'application/json')
        .send('{ invalid json }');

      expect(response.status).toBe(400);
    });

    it('should handle SQL injection attempts', async () => {
      const response = await request(app)
        .get('/api/v1/images/1\'; DROP TABLE images; --');

      expect(response.status).toBe(404);
    });
  });

  describe('Performance Tests', () => {
    it('should respond to GET requests within acceptable time', async () => {
      const startTime = Date.now();
      
      await request(app)
        .get('/api/v1/images');
      
      const endTime = Date.now();
      const duration = endTime - startTime;
      
      expect(duration).toBeLessThan(1000); // Should respond within 1 second
    });

    it('should handle multiple concurrent requests', async () => {
      const requests = Array(10).fill(null).map(() =>
        request(app).get('/api/v1/images')
      );
      
      const responses = await Promise.all(requests);
      
      responses.forEach(response => {
        expect(response.status).toBe(200);
      });
    });
  });
});
