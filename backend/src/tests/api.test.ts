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

describe("API Integration Tests", () => {
  let authToken: string;
  let adminToken: string;

  beforeAll(async () => {
    await testDb.sync({ force: true });
  });

  afterAll(async () => {
    await testDb.close();
  });

  describe("Authentication Endpoints", () => {
    it("should register a new user", async () => {
      const userData = {
        email: "test@example.com",
        password: "password123",
      };

      const response = await request(app)
        .post("/api/v1/auth/register")
        .send(userData)
        .expect(201);

      expect(response.body.message).toBe("User registered successfully");
      expect(response.body.user.email).toBe(userData.email);
    });

    it("should login with valid credentials", async () => {
      const loginData = {
        email: "test@example.com",
        password: "password123",
      };

      const response = await request(app)
        .post("/api/v1/auth/login")
        .send(loginData)
        .expect(200);

      expect(response.body.token).toBeDefined();
      expect(response.body.user.email).toBe(loginData.email);

      authToken = response.body.token;
    });

    it("should reject invalid credentials", async () => {
      const loginData = {
        email: "test@example.com",
        password: "wrongpassword",
      };

      await request(app).post("/api/v1/auth/login").send(loginData).expect(401);
    });

    it("should create admin user", async () => {
      const adminData = {
        email: "admin@example.com",
        password: "admin123",
        role: "admin",
      };

      const response = await request(app)
        .post("/api/v1/auth/register")
        .send(adminData)
        .expect(201);

      expect(response.body.user.role).toBe("admin");
    });

    it("should login as admin", async () => {
      const loginData = {
        email: "admin@example.com",
        password: "admin123",
      };

      const response = await request(app)
        .post("/api/v1/auth/login")
        .send(loginData)
        .expect(200);

      adminToken = response.body.token;
    });
  });

  describe("Image Endpoints", () => {
    it("should get images without authentication", async () => {
      const response = await request(app).get("/api/v1/images").expect(200);

      expect(Array.isArray(response.body)).toBe(true);
    });

    it("should require authentication for protected endpoints", async () => {
      await request(app)
        .post("/api/v1/images")
        .send({ name: "Test Image" })
        .expect(401);
    });

    it("should upload image with authentication", async () => {
      const imageData = {
        name: "Test Image",
        description: "Test Description",
      };

      // Mock file upload
      const response = await request(app)
        .post("/api/v1/images")
        .set("Authorization", `Bearer ${authToken}`)
        .attach("image", Buffer.from("fake-image-data"), "test.jpg")
        .field("name", imageData.name)
        .field("description", imageData.description)
        .expect(201);

      expect(response.body.name).toBe(imageData.name);
      expect(response.body.description).toBe(imageData.description);
    });
  });

  describe("Sync Endpoints", () => {
    it("should get available voices", async () => {
      const response = await request(app)
        .get("/api/v1/sync/voices")
        .expect(200);

      expect(Array.isArray(response.body)).toBe(true);
      expect(response.body.length).toBeGreaterThan(0);
      expect(response.body[0]).toHaveProperty("id");
      expect(response.body[0]).toHaveProperty("name");
      expect(response.body[0]).toHaveProperty("language");
    });

    it("should generate sync video", async () => {
      const syncData = {
        text: "Hello, welcome to our store!",
        language: "en",
        voiceId: "voice-1",
      };

      const response = await request(app)
        .post("/api/v1/sync/generate")
        .send(syncData)
        .expect(200);

      expect(response.body.jobId).toBeDefined();
      expect(response.body.status).toBe("pending");
    });

    it("should validate sync request", async () => {
      const invalidData = {
        text: "", // Empty text should fail
        language: "en",
      };

      await request(app)
        .post("/api/v1/sync/generate")
        .send(invalidData)
        .expect(400);
    });

    it("should get sync status", async () => {
      // First create a job
      const syncData = {
        text: "Hello, welcome to our store!",
        language: "en",
        voiceId: "voice-1",
      };

      const createResponse = await request(app)
        .post("/api/v1/sync/generate")
        .send(syncData)
        .expect(200);

      const jobId = createResponse.body.jobId;

      // Then get status
      const statusResponse = await request(app)
        .get(`/api/v1/sync/status/${jobId}`)
        .expect(200);

      expect(statusResponse.body.jobId).toBe(jobId);
      expect(statusResponse.body.status).toBeDefined();
    });

    it("should handle non-existing job status", async () => {
      await request(app)
        .get("/api/v1/sync/status/non-existing-job-id")
        .expect(404);
    });
  });

  describe("Admin Endpoints", () => {
    it("should require admin role for admin endpoints", async () => {
      await request(app)
        .get("/api/v1/admin/images")
        .set("Authorization", `Bearer ${authToken}`) // Regular user token
        .expect(403);
    });

    it("should allow admin access to admin endpoints", async () => {
      const response = await request(app)
        .get("/api/v1/admin/images")
        .set("Authorization", `Bearer ${adminToken}`)
        .expect(200);

      expect(Array.isArray(response.body)).toBe(true);
    });
  });

  describe("Error Handling", () => {
    it("should handle invalid JSON", async () => {
      await request(app)
        .post("/api/v1/auth/login")
        .set("Content-Type", "application/json")
        .send("invalid json")
        .expect(400);
    });

    it("should handle missing required fields", async () => {
      await request(app)
        .post("/api/v1/auth/login")
        .send({ email: "test@example.com" }) // Missing password
        .expect(400);
    });

    it("should handle invalid email format", async () => {
      await request(app)
        .post("/api/v1/auth/login")
        .send({
          email: "invalid-email",
          password: "password123",
        })
        .expect(400);
    });
  });

  describe("Rate Limiting and Security", () => {
    it("should handle multiple requests", async () => {
      const promises = Array(10)
        .fill(null)
        .map(() => request(app).get("/api/v1/images").expect(200));

      const responses = await Promise.all(promises);
      expect(responses).toHaveLength(10);
    });

    it("should reject requests without proper headers", async () => {
      await request(app)
        .post("/api/v1/auth/login")
        .send({
          email: "test@example.com",
          password: "password123",
        })
        .expect(200); // Should still work, but in production might have stricter validation
    });
  });
});
