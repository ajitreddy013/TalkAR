import jwt from "jsonwebtoken";
import { authenticateAdmin } from "../src/middleware/auth";

function buildRes() {
  const res: any = {};
  res.statusCode = 200;
  res.body = null;
  res.status = jest.fn((code: number) => {
    res.statusCode = code;
    return res;
  });
  res.json = jest.fn((payload: any) => {
    res.body = payload;
    return res;
  });
  return res;
}

describe("Phase 2 ops auth middleware behavior", () => {
  const secret = process.env.JWT_SECRET || "fallback-secret";

  it("rejects requests without token", () => {
    const req: any = { header: jest.fn(() => undefined) };
    const res = buildRes();
    const next = jest.fn();

    authenticateAdmin(req, res as any, next);
    expect(res.status).toHaveBeenCalledWith(401);
    expect(next).not.toHaveBeenCalled();
  });

  it("rejects non-admin token", () => {
    const userToken = jwt.sign({ id: "u1", role: "user" }, secret, { expiresIn: "1h" });
    const req: any = {
      header: jest.fn((name: string) => (name === "Authorization" ? `Bearer ${userToken}` : undefined)),
    };
    const res = buildRes();
    const next = jest.fn();

    authenticateAdmin(req, res as any, next);
    expect(res.status).toHaveBeenCalledWith(403);
    expect(next).not.toHaveBeenCalled();
  });

  it("accepts admin token", () => {
    const adminToken = jwt.sign({ id: "a1", role: "admin" }, secret, { expiresIn: "1h" });
    const req: any = {
      header: jest.fn((name: string) => (name === "Authorization" ? `Bearer ${adminToken}` : undefined)),
    };
    const res = buildRes();
    const next = jest.fn();

    authenticateAdmin(req, res as any, next);
    expect(next).toHaveBeenCalledTimes(1);
  });
});

