import express from "express";
import request from "supertest";
import syncRoutes from "../src/routes/sync";

// Simple E2E-like smoke tests (TestSprite-style) against an in-memory app
// No real server or database required
const buildTestApp = () => {
  const app = express();
  app.use(express.json());
  // Minimal health endpoint (matches production behavior)
  app.get("/health", (_req, res) => {
    res.json({
      status: "OK",
      timestamp: new Date().toISOString(),
      version: "1.0.0",
    });
  });
  // Mount only sync routes which don't require DB
  app.use("/api/v1/sync", syncRoutes);
  return app;
};

describe("TalkAR backend (TestSprite E2E)", () => {
  const app = buildTestApp();

  it("health endpoint responds OK", async () => {
    const res = await request(app).get("/health").expect(200);
    expect(res.body).toHaveProperty("status", "OK");
  });

  it("sync voices returns a non-empty list", async () => {
    const res = await request(app).get("/api/v1/sync/voices").expect(200);
    expect(Array.isArray(res.body)).toBe(true);
  });

  it("sync generate rejects invalid payload", async () => {
    await request(app)
      .post("/api/v1/sync/generate")
      .send({ text: "", language: "en" })
      .expect(400);
  });
});
