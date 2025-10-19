import request from "supertest";
import express from "express";
import { testDb } from "./setup";
import authRoutes from "../routes/auth";
import imageRoutes from "../routes/images";
import syncRoutes from "../routes/sync";

// Create test app
const app = express();
app.use(express.json());
app.use("/api/v1/auth", authRoutes);
app.use("/api/v1/images", imageRoutes);
app.use("/api/v1/sync", syncRoutes);

describe("Performance Tests", () => {
  let authToken: string;

  beforeAll(async () => {
    process.env.SYNC_USE_MOCK = "true";
    await testDb.sync({ force: true });

    // Create test user
    const response = await request(app).post("/api/v1/auth/register").send({
      email: "perf@example.com",
      password: "password123",
    });

    const loginResponse = await request(app).post("/api/v1/auth/login").send({
      email: "perf@example.com",
      password: "password123",
    });

    authToken = loginResponse.body.token;
  });

  afterAll(async () => {
    await testDb.close();
  });

  describe("Load Testing", () => {
    it("should handle concurrent image requests", async () => {
      const startTime = Date.now();
      const concurrentRequests = 50;

      const promises = Array(concurrentRequests)
        .fill(null)
        .map(() => request(app).get("/api/v1/images").expect(200));

      const responses = await Promise.all(promises);
      const endTime = Date.now();
      const duration = endTime - startTime;

      expect(responses).toHaveLength(concurrentRequests);
      expect(duration).toBeLessThan(5000); // Should complete within 5 seconds

      console.log(`Concurrent requests (${concurrentRequests}): ${duration}ms`);
    });

    it("should handle concurrent sync requests", async () => {
      const startTime = Date.now();
      const concurrentRequests = 20;

      const promises = Array(concurrentRequests)
        .fill(null)
        .map((_) =>
          request(app)
            .post("/api/v1/sync/generate")
            .send({
              text: `Test message ${Math.random()}`,
              language: "en",
              voiceId: "voice-1",
            })
            .expect(200),
        );

      const responses = await Promise.all(promises);
      const endTime = Date.now();
      const duration = endTime - startTime;

      expect(responses).toHaveLength(concurrentRequests);
      expect(duration).toBeLessThan(10000); // Should complete within 10 seconds

      console.log(
        `Concurrent sync requests (${concurrentRequests}): ${duration}ms`,
      );
    });

    it("should handle mixed workload", async () => {
      const startTime = Date.now();
      const requests = [
        // 30 image requests
        ...Array(30)
          .fill(null)
          .map(() => request(app).get("/api/v1/images")),
        // 10 sync requests
        ...Array(10)
          .fill(null)
          .map((_) =>
            request(app)
              .post("/api/v1/sync/generate")
              .send({
                text: `Mixed workload test ${Math.random()}`,
                language: "en",
                voiceId: "voice-1",
              }),
          ),
        // 10 voice requests
        ...Array(10)
          .fill(null)
          .map(() => request(app).get("/api/v1/sync/voices")),
      ];

      const responses = await Promise.all(requests);
      const endTime = Date.now();
      const duration = endTime - startTime;

      expect(responses).toHaveLength(50);
      expect(duration).toBeLessThan(15000); // Should complete within 15 seconds

      console.log(`Mixed workload (50 requests): ${duration}ms`);
    });
  });

  describe("Memory Usage", () => {
    it("should not leak memory during repeated requests", async () => {
      const initialMemory = process.memoryUsage();

      // Perform 100 requests
      for (let i = 0; i < 100; i++) {
        await request(app).get("/api/v1/images").expect(200);
      }

      // Force garbage collection if available
      if (global.gc) {
        global.gc();
      }

      const finalMemory = process.memoryUsage();
      const memoryIncrease = finalMemory.heapUsed - initialMemory.heapUsed;

      // Memory increase should be reasonable (less than 50MB)
      expect(memoryIncrease).toBeLessThan(50 * 1024 * 1024);

      console.log(
        `Memory increase: ${(memoryIncrease / 1024 / 1024).toFixed(2)}MB`,
      );
    });
  });

  describe("Response Time Benchmarks", () => {
    it("should respond to image requests within acceptable time", async () => {
      const startTime = Date.now();

      await request(app).get("/api/v1/images").expect(200);

      const duration = Date.now() - startTime;
      expect(duration).toBeLessThan(1200); // Should respond within 1.2 seconds

      console.log(`Image request response time: ${duration}ms`);
    });

    it("should respond to sync requests within acceptable time", async () => {
      const startTime = Date.now();

      await request(app)
        .post("/api/v1/sync/generate")
        .send({
          text: "Performance test message",
          language: "en",
          voiceId: "voice-1",
        })
        .expect(200);

      const duration = Date.now() - startTime;
      expect(duration).toBeLessThan(2500); // Should respond within 2.5 seconds

      console.log(`Sync request response time: ${duration}ms`);
    });

    it("should respond to voice requests within acceptable time", async () => {
      const startTime = Date.now();

      await request(app).get("/api/v1/sync/voices").expect(200);

      const duration = Date.now() - startTime;
      expect(duration).toBeLessThan(500); // Should respond within 500ms

      console.log(`Voice request response time: ${duration}ms`);
    });
  });

  describe("Database Performance", () => {
    it("should handle database queries efficiently", async () => {
      const startTime = Date.now();

      // Simulate multiple database operations
      for (let i = 0; i < 50; i++) {
        await request(app).get("/api/v1/images").expect(200);
      }

      const duration = Date.now() - startTime;
      const avgTime = duration / 50;

      expect(avgTime).toBeLessThan(100); // Average should be less than 100ms

      console.log(`Average database query time: ${avgTime.toFixed(2)}ms`);
    });
  });

  describe("Error Handling Performance", () => {
    it("should handle errors efficiently", async () => {
      const startTime = Date.now();

      // Generate multiple errors
      const promises = Array(20)
        .fill(null)
        .map(() =>
          request(app)
            .post("/api/v1/sync/generate")
            .send({}) // Invalid request
            .expect(400),
        );

      await Promise.all(promises);
      const duration = Date.now() - startTime;

      expect(duration).toBeLessThan(5000); // Should handle errors within 5 seconds

      console.log(`Error handling time: ${duration}ms`);
    });
  });

  describe("Authentication Performance", () => {
    it("should authenticate users efficiently", async () => {
      const startTime = Date.now();

      // Test multiple login attempts
      const promises = Array(20)
        .fill(null)
        .map(() =>
          request(app)
            .post("/api/v1/auth/login")
            .send({
              email: "perf@example.com",
              password: "password123",
            })
            .expect(200),
        );

      await Promise.all(promises);
      const duration = Date.now() - startTime;

      expect(duration).toBeLessThan(3000); // Should authenticate within 3 seconds

      console.log(`Authentication time: ${duration}ms`);
    });
  });
});
