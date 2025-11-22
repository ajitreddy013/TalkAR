import request from "supertest";
import app from "../index";

describe("Rate Limiting", () => {
  it("should allow 60 requests and block the 61st", async () => {
    // Send 60 requests
    for (let i = 0; i < 60; i++) {
      const res = await request(app).get("/health");
      if (res.status !== 200) {
        console.warn(`Request ${i + 1} failed with status ${res.status}`);
      }
      // We don't strictly expect 200 here because the DB might fail in this context,
      // but we expect it NOT to be 429 yet.
      expect(res.status).not.toBe(429);
    }

    // Send the 61st request
    const res = await request(app).get("/health");
    expect(res.status).toBe(429);
    expect(res.text).toContain("Too many requests");
  }, 30000); // Increase timeout
});
